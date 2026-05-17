<template>
  <component
    :is="props.background ? 'div' : 'section'"
    :aria-hidden="props.background ? 'true' : undefined"
    :class="['map-section', { 'map-section--hero': props.background }]"
  >
    <div ref="mapContainer" class="map-canvas"></div>
    <div aria-hidden="true" class="map-atmosphere"></div>
    <div aria-hidden="true" class="map-grain"></div>
    <div v-if="!mapboxToken || mapError" aria-hidden="true" class="map-fallback"></div>
  </component>
</template>

<script setup lang="ts">
import type { CameraOptions, Layer, Map, Marker } from 'mapbox-gl'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useTheme, type ResolvedTheme } from '@/composables/useTheme'
import 'mapbox-gl/dist/mapbox-gl.css'

const props = withDefaults(
  defineProps<{
    background?: boolean
  }>(),
  {
    background: false,
  },
)

const mapContainer = ref<HTMLElement>()
const mapError = ref('')
const mapLoaded = ref(false)
const { resolvedTheme } = useTheme()

const mapboxToken = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN?.trim() || 'pk.eyJ1IjoiYWlydmVuaGUiLCJhIjoiY21vdmE2YTR5MDByczJxb2d1a3VuZzVwbSJ9.Q3Vok4PZRYqICAn6Uk4j8A'
const mapboxLightStyle =
  import.meta.env.VITE_MAPBOX_LIGHT_STYLE?.trim() ||
  import.meta.env.VITE_MAPBOX_STYLE?.trim() ||
  'mapbox://styles/mapbox/light-v11'
const mapboxDarkStyle =
  import.meta.env.VITE_MAPBOX_DARK_STYLE?.trim() || 'mapbox://styles/mapbox/dark-v11'
const mapboxStyle = computed(() => {
  return resolvedTheme.value === 'dark' ? mapboxDarkStyle : mapboxLightStyle
})

let map: Map | null = null
let campusHoldTimer: number | undefined
let earthRotationTimer: number | undefined
let nextCampusHoldTimer: number | undefined
let rotationFrame: number | undefined
let campusMarker: Marker | null = null

const CAMPUS_CENTER: [number, number] = [118.903, 31.920]
const EARTH_CENTER: [number, number] = [-40, 26]
const CAMPUS_GLOBE_CENTER: [number, number] = [CAMPUS_CENTER[0], 26]
const CAMPUS_HOLD_MS = 15000
const FLY_TO_EARTH_MS = 4800
const ROTATE_TO_CAMPUS_MS = 4200
const FLY_TO_CAMPUS_MS = 5600

const earthCamera = {
  center: EARTH_CENTER,
  zoom: 1.3,
  pitch: 0,
  bearing: 0,
} satisfies CameraOptions

const campusCamera = {
  center: CAMPUS_CENTER,
  zoom: 16.25,
  pitch: 52,
  bearing: -18,
} satisfies CameraOptions

function mapFog(theme: ResolvedTheme) {
  if (theme === 'dark') {
    return {
      color: '#121a28',
      'high-color': '#1b2638',
      'horizon-blend': 0.18,
      'space-color': '#0b1220',
      'star-intensity': 0.22,
    }
  }

  return {
    color: '#f7fbff',
    'high-color': '#dbeafe',
    'horizon-blend': 0.2,
    'space-color': '#eef6ff',
    'star-intensity': 0,
  }
}

function buildingColors(theme: ResolvedTheme) {
  if (theme === 'dark') {
    return {
      low: '#1f2937',
      middle: '#283548',
      high: '#334155',
      opacity: 0.82,
    }
  }

  return {
    low: '#f1f5f9',
    middle: '#e8eef5',
    high: '#dbe4ee',
    opacity: 0.78,
  }
}

function stylePalette(theme: ResolvedTheme) {
  if (theme === 'dark') {
    return {
      ink: '#f8fafc',
      background: '#121a28',
      water: '#183049',
      park: '#183125',
      land: '#162233',
      road: '#e5e7eb',
      minorRoad: '#a1a1aa',
      boundary: '#94a3b8',
      label: '#f8fafc',
      labelHalo: '#09090b',
    }
  }

  return {
    ink: '#0f172a',
    background: '#f7fbff',
    water: '#dceeff',
    park: '#dff5e8',
    land: '#edf4fa',
    road: '#64748b',
    minorRoad: '#94a3b8',
    boundary: '#94a3b8',
    label: '#0f172a',
    labelHalo: '#ffffff',
  }
}

function addTerrain() {
  if (!map || map.getSource('oa-terrain')) return

  map.addSource('oa-terrain', {
    type: 'raster-dem',
    url: 'mapbox://mapbox.mapbox-terrain-dem-v1',
    tileSize: 512,
    maxzoom: 14,
  })
  map.setTerrain({ source: 'oa-terrain', exaggeration: 1.35 })
  map.setFog(mapFog(resolvedTheme.value))
}

function tuneStyle(theme: ResolvedTheme) {
  if (!map) return

  const layers = map.getStyle().layers || []
  const palette = stylePalette(theme)

  layers.forEach((layer) => {
    const layerId = layer.id.toLowerCase()

    try {
      if (layer.type === 'symbol') {
        map?.setLayoutProperty(layer.id, 'visibility', 'none')
        return
      }

      if (layer.type === 'background') {
        map?.setPaintProperty(layer.id, 'background-color', palette.background)
        return
      }

      if (layer.type === 'line') {
        if (layerId.includes('road') || layerId.includes('bridge') || layerId.includes('tunnel')) {
          const isMajor =
            layerId.includes('motorway') ||
            layerId.includes('trunk') ||
            layerId.includes('primary') ||
            layerId.includes('secondary')

          map?.setLayoutProperty(layer.id, 'visibility', isMajor ? 'visible' : 'none')
          if (isMajor) {
            map?.setPaintProperty(layer.id, 'line-color', palette.road)
            map?.setPaintProperty(layer.id, 'line-opacity', theme === 'dark' ? 0.22 : 0.18)
          }
          return
        }

        map?.setPaintProperty(layer.id, 'line-opacity', 0)
        return
      }

      if (layer.type === 'fill') {
        if (layerId.includes('water')) {
          map?.setPaintProperty(layer.id, 'fill-color', palette.water)
          map?.setPaintProperty(layer.id, 'fill-opacity', 1)
          return
        }

        if (
          layerId.includes('park') ||
          layerId.includes('landuse') ||
          layerId.includes('landcover') ||
          layerId.includes('vegetation')
        ) {
          map?.setPaintProperty(layer.id, 'fill-color', palette.park)
          map?.setPaintProperty(layer.id, 'fill-opacity', 0.92)
          return
        }

        map?.setPaintProperty(layer.id, 'fill-color', palette.land)
        map?.setPaintProperty(layer.id, 'fill-opacity', 1)
        return
      }
    } catch (error) {
      // Some style layers do not expose every paint/layout property.
    }
  })
}

function restoreStyleOverlays() {
  if (!map) return
  tuneStyle(resolvedTheme.value)
  addTerrain()
}

function addCampusMarker(mapboxgl: typeof import('mapbox-gl').default) {
  if (!map || campusMarker) return

  const markerElement = document.createElement('div')
  markerElement.className = 'map-campus-marker'
  markerElement.innerHTML = '<span></span>'

  campusMarker = new mapboxgl.Marker({
    element: markerElement,
    anchor: 'center',
  })
    .setLngLat(CAMPUS_CENTER)
    .addTo(map)
}

function switchMapStyle() {
  if (!map) return

  mapError.value = ''
  map.setStyle(mapboxStyle.value)
  map.once('style.load', restoreStyleOverlays)
}

function clearMapTimers() {
  if (campusHoldTimer) window.clearTimeout(campusHoldTimer)
  if (earthRotationTimer) window.clearTimeout(earthRotationTimer)
  if (nextCampusHoldTimer) window.clearTimeout(nextCampusHoldTimer)
  if (rotationFrame) window.cancelAnimationFrame(rotationFrame)
  campusHoldTimer = undefined
  earthRotationTimer = undefined
  nextCampusHoldTimer = undefined
  rotationFrame = undefined
}

function scheduleEarthReturn(delay = CAMPUS_HOLD_MS) {
  if (campusHoldTimer) window.clearTimeout(campusHoldTimer)
  campusHoldTimer = window.setTimeout(zoomToEarth, delay)
}

function zoomToEarth() {
  if (!map) return

  map.stop()
  map.flyTo({
    ...earthCamera,
    duration: FLY_TO_EARTH_MS,
    curve: 1.12,
    speed: 0.9,
    essential: true,
  })

  earthRotationTimer = window.setTimeout(rotateEarthToCampus, FLY_TO_EARTH_MS + 160)
}

function rotateEarthToCampus() {
  if (!map) return

  const startTime = window.performance.now()
  const startCenter = map.getCenter()
  const startLng = startCenter.lng
  const startLat = startCenter.lat
  const targetLng = CAMPUS_GLOBE_CENTER[0]
  const targetLat = CAMPUS_GLOBE_CENTER[1]
  const shortestDeltaLng = ((targetLng - startLng + 540) % 360) - 180
  const deltaLng = shortestDeltaLng >= 0 ? shortestDeltaLng + 360 : shortestDeltaLng - 360
  const deltaLat = targetLat - startLat

  const step = (frameTime: number) => {
    if (!map) return

    const progress = Math.min((frameTime - startTime) / ROTATE_TO_CAMPUS_MS, 1)
    const eased = 1 - (1 - progress) ** 3
    const lng = ((startLng + deltaLng * eased + 540) % 360) - 180
    const lat = startLat + deltaLat * eased
    map.setCenter([lng, lat])

    if (progress < 1) {
      rotationFrame = window.requestAnimationFrame(step)
      return
    }

    rotationFrame = undefined
    zoomToCampus()
  }

  rotationFrame = window.requestAnimationFrame(step)
}

function zoomToCampus() {
  if (!map) return

  map.stop()
  map.flyTo({
    ...campusCamera,
    duration: FLY_TO_CAMPUS_MS,
    curve: 1.28,
    speed: 0.82,
    essential: true,
  })

  nextCampusHoldTimer = window.setTimeout(() => {
    scheduleEarthReturn()
  }, FLY_TO_CAMPUS_MS)
}

onMounted(async () => {
  if (!mapboxToken || !mapContainer.value) return

  let mapboxgl: typeof import('mapbox-gl').default
  try {
    const mapboxModule = await import('mapbox-gl')
    mapboxgl = mapboxModule.default
  } catch (error) {
    mapError.value = '地图资源加载失败。'
    return
  }

  if (!mapboxgl.supported({ failIfMajorPerformanceCaveat: true })) {
    mapError.value = '当前浏览器不支持 WebGL 地图。'
    return
  }

  mapboxgl.accessToken = mapboxToken
  map = new mapboxgl.Map({
    container: mapContainer.value,
    style: mapboxStyle.value,
    center: campusCamera.center,
    zoom: campusCamera.zoom,
    pitch: campusCamera.pitch,
    bearing: campusCamera.bearing,
    projection: 'globe',
    antialias: true,
    attributionControl: false,
    interactive: false,
  })

  map.on('load', () => {
    mapLoaded.value = true
    mapError.value = ''
    restoreStyleOverlays()
    addCampusMarker(mapboxgl)
    scheduleEarthReturn()
  })

  map.on('error', (event) => {
    if (!mapLoaded.value) {
      mapError.value = event.error?.message || '请检查 Mapbox Token、样式地址或网络连接。'
    }
  })
})

watch(mapboxStyle, () => {
  switchMapStyle()
})

onBeforeUnmount(() => {
  clearMapTimers()
  campusMarker?.remove()
  campusMarker = null
  map?.remove()
  map = null
})
</script>

<style scoped>
.map-section {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100svh;
  min-height: 720px;
  overflow: hidden;
  background: var(--oa-map-bg);
}

.map-canvas {
  position: absolute;
  inset: 0;
  filter: saturate(0.86) contrast(1.02);
}

.map-atmosphere,
.map-grain {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.map-atmosphere {
  z-index: 1;
  background:
    radial-gradient(
      circle at 50% 56%,
      transparent 0 38%,
      rgba(15, 23, 42, 0.035) 78%,
      rgba(15, 23, 42, 0.09) 100%
    ),
    linear-gradient(180deg, rgba(255, 255, 255, 0.01), rgba(15, 23, 42, 0.03));
}

.map-grain {
  z-index: 2;
  opacity: 0.18;
  background-image:
    radial-gradient(rgba(15, 23, 42, 0.22) 0.7px, transparent 0.7px),
    radial-gradient(rgba(15, 23, 42, 0.14) 0.7px, transparent 0.7px);
  background-position:
    0 0,
    8px 8px;
  background-size:
    16px 16px,
    16px 16px;
  mix-blend-mode: soft-light;
}

.map-section--hero {
  position: absolute;
  inset: 0;
  z-index: 0;
  height: 100%;
  min-height: 0;
  background: var(--oa-map-bg);
}

.map-fallback {
  position: absolute;
  inset: 0;
  z-index: 4;
  background:
    radial-gradient(circle at 50% 48%, rgba(255, 255, 255, 0.82), transparent 18%),
    radial-gradient(circle at 50% 48%, rgba(14, 116, 144, 0.22), transparent 54%),
    linear-gradient(135deg, rgba(15, 23, 42, 0.08), transparent 44%),
    var(--oa-map-fallback);
  background-size:
    auto,
    auto,
    auto,
    auto;
}

:deep(.map-campus-marker) {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
}

:deep(.map-campus-marker::before),
:deep(.map-campus-marker span) {
  grid-area: 1 / 1;
  border-radius: 50%;
}

:deep(.map-campus-marker::before) {
  width: 24px;
  height: 24px;
  content: '';
  border: 1px solid rgba(15, 23, 42, 0.24);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
}

:deep(.map-campus-marker span) {
  width: 10px;
  height: 10px;
  background: #0f172a;
}

:global(html.dark) .map-atmosphere {
  background:
    radial-gradient(
      circle at 50% 56%,
      transparent 0 42%,
      rgba(0, 0, 0, 0.03) 74%,
      rgba(0, 0, 0, 0.08) 100%
    ),
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), rgba(0, 0, 0, 0.05));
}

:global(html.dark) .map-canvas {
  filter: brightness(1.1) saturate(0.88) contrast(1.02);
}

:global(html.dark) .map-grain {
  opacity: 0.12;
  background-image:
    radial-gradient(rgba(255, 255, 255, 0.2) 0.7px, transparent 0.7px),
    radial-gradient(rgba(255, 255, 255, 0.12) 0.7px, transparent 0.7px);
}

:global(html.dark) :deep(.map-campus-marker::before) {
  border-color: rgba(245, 245, 247, 0.22);
  background: rgba(9, 9, 11, 0.8);
}

:global(html.dark) :deep(.map-campus-marker span) {
  background: #f5f5f7;
}

:deep(.mapboxgl-ctrl-group) {
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 8px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

:deep(.mapboxgl-ctrl-attrib) {
  border-radius: 8px 0 0 0;
  background: color-mix(in srgb, var(--oa-elevated-bg) 78%, transparent);
  backdrop-filter: blur(12px);
}

@media (max-width: 720px) {
  .map-section {
    min-height: 620px;
  }
}
</style>
