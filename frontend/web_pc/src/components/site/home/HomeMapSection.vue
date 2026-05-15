<template>
  <component
    :is="props.background ? 'div' : 'section'"
    :aria-hidden="props.background ? 'true' : undefined"
    :class="['map-section', { 'map-section--hero': props.background }]"
  >
    <div ref="mapContainer" class="map-canvas"></div>
    <div v-if="!mapboxToken || mapError" aria-hidden="true" class="map-fallback"></div>
  </component>
</template>

<script setup lang="ts">
import type { CameraOptions, Layer, Map } from 'mapbox-gl'
import { onBeforeUnmount, onMounted, ref } from 'vue'
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

const mapboxToken = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN?.trim() || 'pk.eyJ1IjoiYWlydmVuaGUiLCJhIjoiY21vdmE2YTR5MDByczJxb2d1a3VuZzVwbSJ9.Q3Vok4PZRYqICAn6Uk4j8A'
const mapboxStyle = import.meta.env.VITE_MAPBOX_STYLE?.trim() || 'mapbox://styles/mapbox/light-v11'

let map: Map | null = null
let campusHoldTimer: number | undefined
let earthRotationTimer: number | undefined
let nextCampusHoldTimer: number | undefined
let rotationFrame: number | undefined

const CAMPUS_CENTER: [number, number] = [118.8823, 31.9108]
const EARTH_CENTER: [number, number] = [-40, 26]
const CAMPUS_GLOBE_CENTER: [number, number] = [CAMPUS_CENTER[0], 26]
const CAMPUS_HOLD_MS = 15000
const FLY_TO_EARTH_MS = 4800
const ROTATE_TO_CAMPUS_MS = 4200
const FLY_TO_CAMPUS_MS = 5600

const earthCamera = {
  center: EARTH_CENTER,
  zoom: 1.72,
  pitch: 0,
  bearing: 0,
} satisfies CameraOptions

const campusCamera = {
  center: CAMPUS_CENTER,
  zoom: 16.25,
  pitch: 68,
  bearing: -34,
} satisfies CameraOptions

function addTerrain() {
  if (!map || map.getSource('oa-terrain')) return

  map.addSource('oa-terrain', {
    type: 'raster-dem',
    url: 'mapbox://mapbox.mapbox-terrain-dem-v1',
    tileSize: 512,
    maxzoom: 14,
  })
  map.setTerrain({ source: 'oa-terrain', exaggeration: 1.35 })
  map.setFog({
    color: '#edf7ff',
    'high-color': '#b9dcff',
    'horizon-blend': 0.18,
    'space-color': '#d9ecff',
    'star-intensity': 0,
  })
}

function addBuildings() {
  if (!map || map.getLayer('oa-3d-buildings')) return

  const layers = map.getStyle().layers || []
  const labelLayer = layers.find((layer: any) => {
    return layer.type === 'symbol' && layer.layout?.['text-field']
  }) as Layer | undefined

  try {
    map.addLayer(
      {
        id: 'oa-3d-buildings',
        source: 'composite',
        'source-layer': 'building',
        filter: ['==', 'extrude', 'true'],
        type: 'fill-extrusion',
        minzoom: 14,
        paint: {
          'fill-extrusion-color': [
            'interpolate',
            ['linear'],
            ['get', 'height'],
            0,
            '#dbeafe',
            80,
            '#bae6fd',
            180,
            '#99f6e4',
          ],
          'fill-extrusion-height': [
            'interpolate',
            ['linear'],
            ['zoom'],
            14,
            0,
            15,
            ['coalesce', ['get', 'height'], 18],
          ],
          'fill-extrusion-base': [
            'interpolate',
            ['linear'],
            ['zoom'],
            14,
            0,
            15,
            ['coalesce', ['get', 'min_height'], 0],
          ],
          'fill-extrusion-opacity': 0.74,
        },
      },
      labelLayer?.id,
    )
  } catch (error) {
    // Some custom Mapbox styles do not expose the composite building source.
  }
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

  const mapboxModule = await import('mapbox-gl')
  const mapboxgl = mapboxModule.default

  if (!mapboxgl.supported()) return

  mapboxgl.accessToken = mapboxToken
  map = new mapboxgl.Map({
    container: mapContainer.value,
    style: mapboxStyle,
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
    addTerrain()
    addBuildings()
    scheduleEarthReturn()
  })

  map.on('error', (event) => {
    mapError.value = event.error?.message || '请检查 Mapbox Token、样式地址或网络连接。'
  })
})

onBeforeUnmount(() => {
  clearMapTimers()
  map?.remove()
  map = null
})
</script>

<style scoped>
.map-section {
  position: relative;
  width: 100%;
  height: 100svh;
  min-height: 720px;
  overflow: hidden;
  background: #f8fbff;
}

.map-canvas {
  position: absolute;
  inset: 0;
}

.map-section--hero {
  position: absolute;
  inset: 0;
  z-index: 0;
  height: 100%;
  min-height: 0;
  background: #f8fbff;
}

.map-fallback {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    radial-gradient(circle at 50% 45%, rgba(59, 130, 246, 0.12), transparent 38%),
    radial-gradient(circle at 52% 44%, rgba(20, 184, 166, 0.08), transparent 54%),
    #f8fbff;
  background-size:
    auto,
    auto;
}

:deep(.mapboxgl-ctrl-group) {
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 8px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

:deep(.mapboxgl-ctrl-attrib) {
  border-radius: 8px 0 0 0;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(12px);
}

@media (max-width: 720px) {
  .map-section {
    min-height: 620px;
  }
}
</style>
