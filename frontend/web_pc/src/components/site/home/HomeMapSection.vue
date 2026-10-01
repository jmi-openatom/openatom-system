<template>
  <component
    :is="props.background ? 'div' : 'section'"
    :aria-hidden="props.background ? 'true' : undefined"
    :class="[
      'map-section',
      { 'map-section--hero': props.background, 'map-section--space-pan': spacePanActive },
    ]"
  >
    <div
      ref="mapContainer"
      data-lenis-prevent
      :class="[
        'map-canvas',
        { 'is-loaded': mapLoaded, 'map-canvas--background': props.background },
      ]"
    ></div>
    <div aria-hidden="true" class="map-atmosphere"></div>
    <div aria-hidden="true" class="map-grain"></div>
    <div
      aria-hidden="true"
      :class="['map-fallback', { 'is-hidden': mapLoaded && !mapError }]"
    ></div>
    <p v-if="mapError && !props.background" class="map-status" role="status">{{ mapError }}</p>
  </component>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useTheme, type ResolvedTheme } from '@/composables/useTheme'
import { CAMPUS_BOUNDARY, CAMPUS_BOUNDS, CAMPUS_BUILDING_FILTER, campusPalette } from './campusMap'
import campusLandscape from './campusLandscape.json'
import campusBuildings from './campusBuildings.json'
import { CAMPUS_DETAILS } from './campusDetails'
import type { MapMouseEvent } from 'mapbox-gl'
import { CAMPUS_BUILDINGS, CAMPUS_LABELS, LIGHTHOUSE_ID } from './campusLabels'
import type { campusLandmarkLayer } from './campusLandmark'

type MapboxCameraOptions = Record<string, unknown>
type MapboxModule = any
type MapboxMap = any
type MapboxMarker = any
type StyleLayer = { id: string; type: string; layout?: Record<string, unknown> }

const props = withDefaults(
  defineProps<{
    background?: boolean
    static?: boolean
    interactive?: boolean
    showLabels?: boolean
  }>(),
  {
    background: false,
    static: false,
    interactive: false,
    showLabels: false,
  },
)

const mapContainer = ref<HTMLElement>()
const mapError = ref('')
const mapLoaded = ref(false)
const spacePanActive = ref(false)
const emit = defineEmits<{ selectBuilding: [id: string | number] }>()
let makeLandmark: typeof campusLandmarkLayer | undefined
let selectedBuilding: string | number | null = null
const { resolvedTheme } = useTheme()

const mapboxToken =
  import.meta.env.VITE_MAPBOX_ACCESS_TOKEN?.trim() ||
  'pk.eyJ1IjoiYWlydmVuaGUiLCJhIjoiY21vdmE2YTR5MDByczJxb2d1a3VuZzVwbSJ9.Q3Vok4PZRYqICAn6Uk4j8A'
const mapboxLightStyle =
  import.meta.env.VITE_MAPBOX_LIGHT_STYLE?.trim() ||
  import.meta.env.VITE_MAPBOX_STYLE?.trim() ||
  'mapbox://styles/mapbox/light-v11'
const mapboxDarkStyle =
  import.meta.env.VITE_MAPBOX_DARK_STYLE?.trim() || 'mapbox://styles/mapbox/dark-v11'
const mapboxStyle = computed(() => {
  return resolvedTheme.value === 'dark' ? mapboxDarkStyle : mapboxLightStyle
})

let map: MapboxMap | null = null
let campusHoldTimer: number | undefined
let earthRotationTimer: number | undefined
let nextCampusHoldTimer: number | undefined
let rotationFrame: number | undefined
let campusMarker: MapboxMarker | null = null
let visibilityObserver: IntersectionObserver | undefined
let idleHandle: number | undefined
let enhancementIdleHandle: number | undefined
let releaseMapTimer: number | undefined
let scrollResumeTimer: number | undefined
let mapInitStarted = false
let isMapVisible = true
let mapPausedForScroll = false

const CAMPUS_CENTER: [number, number] = [118.9028, 31.9201]
const EARTH_CENTER: [number, number] = [-40, 26]
const CAMPUS_GLOBE_CENTER: [number, number] = [CAMPUS_CENTER[0], 26]
const CAMPUS_HOLD_MS = 45000
const FLY_TO_EARTH_MS = 3600
const ROTATE_TO_CAMPUS_MS = 3400
const FLY_TO_CAMPUS_MS = 4400
const ROTATION_FRAME_INTERVAL_MS = 1000 / 30

const earthCamera = {
  center: EARTH_CENTER,
  zoom: 1.3,
  pitch: 0,
  bearing: 0,
} satisfies MapboxCameraOptions

const campusCamera = {
  center: CAMPUS_CENTER,
  zoom: 16.05,
  pitch: 52,
  bearing: -20,
} satisfies MapboxCameraOptions

function getCampusCamera() {
  if (!map || !mapContainer.value) return campusCamera
  const { clientWidth: width, clientHeight: height } = mapContainer.value
  const overview = map.cameraForBounds(CAMPUS_BOUNDS, {
    pitch: 52,
    bearing: -20,
    maxZoom: 16.1,
    padding: {
      top: height * (props.background ? 0.16 : 0.08),
      bottom: height * 0.08,
      left: width * 0.06,
      right: width * 0.06,
    },
  })
  if (props.interactive)
    return {
      ...overview,
      pitch: 48,
      bearing: -20,
      zoom: Math.min(16.3, (overview?.zoom ?? 15) + 0.6),
    }
  return { ...overview, ...campusCamera, zoom: Math.min(16.35, (overview?.zoom ?? 15.65) + 0.65) }
}

function mapFog(theme: ResolvedTheme) {
  if (theme === 'dark') {
    return {
      color: '#18181b',
      'high-color': '#303035',
      'horizon-blend': 0.18,
      'space-color': '#0c0c0e',
      'star-intensity': 0.18,
    }
  }

  return {
    color: '#f7f7f8',
    'high-color': '#e5e5e7',
    'horizon-blend': 0.2,
    'space-color': '#f2f2f4',
    'star-intensity': 0,
  }
}

function buildingColors(theme: ResolvedTheme) {
  if (theme === 'dark') {
    return {
      low: '#252529',
      middle: '#38383d',
      high: '#55555b',
      opacity: 0.9,
    }
  }

  return {
    low: '#f0f0f2',
    middle: '#e1e1e4',
    high: '#c9c9ce',
    opacity: 0.78,
  }
}

function stylePalette(theme: ResolvedTheme) {
  if (theme === 'dark') {
    return {
      ink: '#f7f7f8',
      background: '#101012',
      water: '#1a1a1d',
      park: '#202024',
      land: '#161618',
      road: '#d7d7da',
      minorRoad: '#8e8e93',
      boundary: '#77777d',
      label: '#f7f7f8',
      labelHalo: '#0c0c0e',
    }
  }

  return {
    ink: '#1d1d1f',
    background: '#f4f4f5',
    water: '#e3e3e6',
    park: '#ececef',
    land: '#f0f0f2',
    road: '#6e6e73',
    minorRoad: '#a1a1a6',
    boundary: '#8e8e93',
    label: '#1d1d1f',
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
  map.setTerrain({ source: 'oa-terrain', exaggeration: 1 })
}

function applyFog() {
  map?.setFog(mapFog(resolvedTheme.value))
}

function addCampusLandscape() {
  if (!map || map.getSource('oa-campus')) return

  const colors = campusPalette(resolvedTheme.value)
  const beforeRoad = map
    .getStyle()
    .layers?.find((layer: StyleLayer) => layer.type === 'line' && layer.id.includes('road'))?.id

  map.addSource('oa-campus', {
    type: 'geojson',
    attribution:
      '© <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a>',
    data: { type: 'Feature', properties: {}, geometry: CAMPUS_BOUNDARY },
  })
  map.addSource('oa-campus-landscape', { type: 'geojson', data: campusLandscape })
  map.addLayer(
    {
      id: 'oa-campus-ground',
      type: 'fill',
      source: 'oa-campus',
      minzoom: 13,
      paint: {
        'fill-color': colors.ground,
        'fill-opacity': ['interpolate', ['linear'], ['zoom'], 13, 0, 15, 0.88],
      },
    },
    beforeRoad,
  )
  // Tracks/courts sit underneath football fields so the running oval retains
  // its terracotta edge when OSM maps the overlapping playing surfaces.
  for (const kind of ['park', 'track', 'court', 'pitch', 'water'] as const) {
    map.addLayer(
      {
        id: `oa-campus-${kind}`,
        type: 'fill',
        source: 'oa-campus-landscape',
        minzoom: 14,
        filter: ['==', ['get', 'kind'], kind],
        paint: {
          'fill-color': colors[kind === 'court' ? 'track' : kind],
          'fill-opacity': ['interpolate', ['linear'], ['zoom'], 14, 0, 15.2, 0.95],
        },
      },
      beforeRoad,
    )
  }
  map.addLayer(
    {
      id: 'oa-campus-paths',
      type: 'line',
      source: 'oa-campus-landscape',
      minzoom: 14,
      filter: ['==', ['get', 'kind'], 'road'],
      paint: {
        'line-color': colors.road,
        'line-opacity': 0.85,
        'line-width': ['interpolate', ['linear'], ['zoom'], 14, 0.7, 17, 4],
      },
    },
    beforeRoad,
  )
  map.addLayer(
    {
      id: 'oa-campus-shore',
      type: 'line',
      source: 'oa-campus-landscape',
      minzoom: 14,
      filter: ['==', ['get', 'kind'], 'water'],
      paint: {
        'line-color': colors.road,
        'line-opacity': 0.75,
        'line-width': ['interpolate', ['linear'], ['zoom'], 14, 0.5, 17, 2],
      },
    },
    beforeRoad,
  )
  map.addLayer(
    {
      id: 'oa-campus-boundary',
      type: 'line',
      source: 'oa-campus',
      minzoom: 13,
      paint: {
        'line-color': colors.boundary,
        'line-opacity': ['interpolate', ['linear'], ['zoom'], 13, 0, 15, 0.4],
        'line-width': ['interpolate', ['linear'], ['zoom'], 13, 0.5, 17, 2],
      },
    },
    beforeRoad,
  )
}

function add3dBuildings() {
  if (!map || map.getLayer('oa-campus-buildings')) return

  const colors = buildingColors(resolvedTheme.value)
  const labelLayerId = map
    .getStyle()
    .layers?.find(
      (layer: StyleLayer) => layer.type === 'symbol' && layer.layout?.['text-field'],
    )?.id

  if (map.getSource('composite'))
    map.addLayer(
      {
        id: 'oa-3d-buildings',
        source: 'composite',
        'source-layer': 'building',
        type: 'fill-extrusion',
        minzoom: 14,
        filter: ['!', CAMPUS_BUILDING_FILTER],
        paint: {
          'fill-extrusion-color': [
            'interpolate',
            ['linear'],
            ['max', ['coalesce', ['get', 'height'], 0], 24],
            0,
            colors.low,
            30,
            colors.middle,
            90,
            colors.high,
          ],
          'fill-extrusion-height': [
            'interpolate',
            ['linear'],
            ['zoom'],
            14,
            0,
            15.2,
            ['max', ['coalesce', ['get', 'height'], 0], 24],
          ],
          'fill-extrusion-base': [
            'interpolate',
            ['linear'],
            ['zoom'],
            14,
            0,
            15.2,
            ['coalesce', ['get', 'min_height'], 0],
          ],
          'fill-extrusion-opacity': colors.opacity,
          'fill-extrusion-vertical-gradient': !props.background,
        },
      },
      labelLayerId,
    )

  const campusColors = campusPalette(resolvedTheme.value)
  // Local campus geometry remains available even when a basemap is missing
  // these buildings. Campus footprints are excluded from the surrounding layer.
  map.addSource('oa-campus-buildings', { type: 'geojson', data: campusBuildings })
  const buildingHeight = ['coalesce', ['get', 'height'], 18]
  const campusHeight = ['interpolate', ['linear'], ['zoom'], 14, 0, 15.2, buildingHeight]
  map.addLayer(
    {
      id: 'oa-campus-buildings',
      source: 'oa-campus-buildings',
      type: 'fill-extrusion',
      minzoom: 14,
      paint: {
        'fill-extrusion-color': [
          'interpolate',
          ['linear'],
          buildingHeight,
          0,
          campusColors.low,
          30,
          campusColors.middle,
          90,
          campusColors.high,
        ],
        'fill-extrusion-height': campusHeight,
        'fill-extrusion-base': [
          'interpolate',
          ['linear'],
          ['zoom'],
          14,
          0,
          15.2,
          ['coalesce', ['get', 'min_height'], 0],
        ],
        'fill-extrusion-opacity': 1,
        'fill-extrusion-vertical-gradient': true,
        'fill-extrusion-ambient-occlusion-intensity': 0.25,
        'fill-extrusion-ambient-occlusion-radius': 3,
      },
    },
    labelLayerId,
  )
  map.addLayer(
    {
      id: 'oa-campus-roofs',
      source: 'oa-campus-buildings',
      type: 'fill-extrusion',
      minzoom: 14,
      paint: {
        'fill-extrusion-color': [
          'case',
          ['boolean', ['feature-state', 'selected'], false],
          '#d5b579',
          ['==', ['get', 'name'], '体育馆'],
          '#a0b9bc',
          ['==', ['get', 'name'], '办公楼'],
          '#c3c3af',
          campusColors.roof,
        ],
        'fill-extrusion-height': [
          'interpolate',
          ['linear'],
          ['zoom'],
          14,
          0,
          15.2,
          ['+', buildingHeight, 0.6],
        ],
        'fill-extrusion-base': campusHeight,
        'fill-extrusion-opacity': 1,
        'fill-extrusion-vertical-gradient': false,
      },
    },
    labelLayerId,
  )
}

function addCampusLabels() {
  if (!map || map.getSource('oa-campus-labels')) return
  const dark = resolvedTheme.value === 'dark'
  map.addSource('oa-campus-labels', { type: 'geojson', data: CAMPUS_LABELS })
  map.addLayer({
    id: 'oa-campus-labels',
    type: 'symbol',
    source: 'oa-campus-labels',
    minzoom: 14,
    layout: {
      visibility: props.showLabels ? 'visible' : 'none',
      'text-field': ['get', 'name'],
      'text-font': ['Arial Unicode MS Regular'],
      'text-size': ['interpolate', ['linear'], ['zoom'], 14, 10, 16, 12, 18, 14],
      'text-max-width': 8,
      'text-padding': 3,
      'text-anchor': 'bottom',
      'symbol-z-elevate': true,
    },
    paint: {
      'text-color': dark ? '#f1eee0' : '#283f44',
      'text-halo-color': dark ? '#22342f' : '#fffdf3',
      'text-halo-width': 1.8,
      'symbol-z-offset': ['coalesce', ['get', 'height'], 1],
      'text-occlusion-opacity': 0.65,
    },
  })
}

function focusBuilding(id: string | number) {
  const building = CAMPUS_BUILDINGS.find((item) => String(item.id) === String(id))
  if (!map || !building) return
  if (selectedBuilding !== null && selectedBuilding !== LIGHTHOUSE_ID)
    map.setFeatureState(
      { source: 'oa-campus-buildings', id: selectedBuilding },
      { selected: false },
    )
  selectedBuilding = building.id
  if (building.id === LIGHTHOUSE_ID) focusLighthouse()
  else {
    map.setFeatureState({ source: 'oa-campus-buildings', id: building.id }, { selected: true })
    map.flyTo({ center: building.center, zoom: 17.5, pitch: 56, duration: 1000, essential: false })
  }
  emit('selectBuilding', building.id)
}

function resetView() {
  if (!map) return
  if (selectedBuilding !== null && selectedBuilding !== LIGHTHOUSE_ID)
    map.setFeatureState(
      { source: 'oa-campus-buildings', id: selectedBuilding },
      { selected: false },
    )
  selectedBuilding = null
  map.flyTo({ ...getCampusCamera(), duration: 900, essential: false })
}

function rotateView(degrees: number) {
  map?.easeTo({ bearing: map.getBearing() + degrees, duration: 350, essential: false })
}

function focusLighthouse() {
  map?.flyTo({
    center: [118.89995, 31.92185],
    zoom: 18,
    pitch: 60,
    bearing: -30,
    duration: 1000,
    essential: false,
  })
}

defineExpose({ focusBuilding, resetView, rotateView })

function addCampusDetails() {
  if (!map || map.getSource('oa-campus-facades')) return
  const colors = campusPalette(resolvedTheme.value)
  for (const [name, data] of Object.entries(CAMPUS_DETAILS)) {
    const id = `oa-campus-${name}`
    map.addSource(id, { type: 'geojson', data })
    if (name === 'sports') {
      map.addLayer({
        id,
        source: id,
        type: 'line',
        minzoom: 14.8,
        paint: { 'line-color': '#fffdf1', 'line-opacity': 0.75, 'line-width': 0.65 },
      })
      continue
    }
    map.addLayer({
      id,
      source: id,
      type: 'fill-extrusion',
      minzoom: name === 'facades' ? 15 : 14.5,
      paint: {
        'fill-extrusion-color':
          name === 'facades'
            ? colors.windows
            : name === 'parapets'
              ? colors.low
              : name === 'trunks'
                ? colors.trunk
                : ['case', ['==', ['get', 'variant'], 1], colors.treeLight, colors.tree],
        'fill-extrusion-base': ['interpolate', ['linear'], ['zoom'], 14, 0, 15.2, ['get', 'base']],
        'fill-extrusion-height': [
          'interpolate',
          ['linear'],
          ['zoom'],
          14,
          0,
          15.2,
          ['get', 'height'],
        ],
        'fill-extrusion-opacity': 1,
        'fill-extrusion-vertical-gradient': name !== 'facades',
        'fill-extrusion-emissive-strength':
          name === 'facades' && resolvedTheme.value === 'dark' ? 0.25 : 0,
      },
    })
  }
}

function tuneStyle(theme: ResolvedTheme) {
  if (!map) return

  const layers = map.getStyle().layers || []
  const palette = stylePalette(theme)

  layers.forEach((layer: StyleLayer) => {
    if (layer.id.startsWith('oa-campus-')) return
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

          map?.setLayoutProperty(layer.id, 'visibility', 'visible')
          map?.setPaintProperty(layer.id, 'line-color', isMajor ? palette.road : palette.minorRoad)
          map?.setPaintProperty(
            layer.id,
            'line-opacity',
            isMajor ? (theme === 'dark' ? 0.46 : 0.28) : theme === 'dark' ? 0.26 : 0.16,
          )
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

        if (layerId.includes('building')) {
          const colors = buildingColors(theme)
          map?.setPaintProperty(layer.id, 'fill-color', colors.low)
          map?.setPaintProperty(layer.id, 'fill-opacity', [
            'case',
            CAMPUS_BUILDING_FILTER,
            0,
            theme === 'dark' ? 0.58 : 0.48,
          ])
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
  applyFog()
  map.setLights([
    {
      id: 'oa-ambient',
      type: 'ambient',
      properties: {
        color: resolvedTheme.value === 'dark' ? '#c0d6e1' : '#fffaf0',
        intensity: 0.62,
      },
    },
    {
      id: 'oa-sun',
      type: 'directional',
      properties: {
        direction: [210, 38],
        color: '#fff5e4',
        intensity: 0.48,
        'cast-shadows': true,
        'shadow-intensity': 0.22,
      },
    },
  ])
  addCampusLandscape()
  add3dBuildings()
  addCampusDetails()
  if (makeLandmark && !map.getLayer('oa-campus-landmark'))
    map.addLayer(makeLandmark(resolvedTheme.value))
  addCampusLabels()
  if (selectedBuilding !== null && selectedBuilding !== LIGHTHOUSE_ID)
    map.setFeatureState({ source: 'oa-campus-buildings', id: selectedBuilding }, { selected: true })
}

function cancelMapEnhancements() {
  if (!enhancementIdleHandle) return
  if (window.cancelIdleCallback) {
    window.cancelIdleCallback(enhancementIdleHandle)
  } else {
    window.clearTimeout(enhancementIdleHandle)
  }
  enhancementIdleHandle = undefined
}

function scheduleMapEnhancements() {
  cancelMapEnhancements()

  const enhance = () => {
    enhancementIdleHandle = undefined
    addTerrain()
  }

  if (window.requestIdleCallback) {
    enhancementIdleHandle = window.requestIdleCallback(enhance, { timeout: 2600 })
    return
  }
  enhancementIdleHandle = window.setTimeout(enhance, 1200)
}

function cancelIdleInit() {
  if (!idleHandle) return
  if (window.cancelIdleCallback) {
    window.cancelIdleCallback(idleHandle)
  } else {
    window.clearTimeout(idleHandle)
  }
  idleHandle = undefined
}

function releaseMap() {
  releaseSpacePan()
  cancelIdleInit()
  cancelMapEnhancements()
  clearMapTimers()
  if (scrollResumeTimer) window.clearTimeout(scrollResumeTimer)
  scrollResumeTimer = undefined
  mapPausedForScroll = false
  campusMarker?.remove()
  campusMarker = null
  map?.remove()
  map = null
  mapLoaded.value = false
  mapInitStarted = false
}

function addCampusMarker(mapboxgl: MapboxModule) {
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
  mapLoaded.value = false
  cancelMapEnhancements()
  map.setStyle(mapboxStyle.value, { diff: false })
  map.once('style.load', () => {
    restoreStyleOverlays()
    map?.once('idle', () => {
      mapLoaded.value = true
      scheduleMapEnhancements()
    })
  })
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
  if (!shouldAnimateMap()) return
  if (campusHoldTimer) window.clearTimeout(campusHoldTimer)
  campusHoldTimer = window.setTimeout(zoomToEarth, delay)
}

function shouldAnimateMap() {
  const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  return (
    !props.static &&
    !props.interactive &&
    !window.matchMedia('(max-width: 699px)').matches &&
    isMapVisible &&
    !document.hidden &&
    !reducedMotion &&
    !mapPausedForScroll
  )
}

function zoomToEarth() {
  if (!map || !shouldAnimateMap()) return

  map.stop()
  map.flyTo({
    ...earthCamera,
    duration: FLY_TO_EARTH_MS,
    curve: 1.12,
    speed: 0.9,
    essential: false,
  })

  earthRotationTimer = window.setTimeout(rotateEarthToCampus, FLY_TO_EARTH_MS + 160)
}

function rotateEarthToCampus() {
  if (!map || !shouldAnimateMap()) return

  const startTime = window.performance.now()
  const startCenter = map.getCenter()
  const startLng = startCenter.lng
  const startLat = startCenter.lat
  const targetLng = CAMPUS_GLOBE_CENTER[0]
  const targetLat = CAMPUS_GLOBE_CENTER[1]
  const shortestDeltaLng = ((targetLng - startLng + 540) % 360) - 180
  const deltaLng = shortestDeltaLng >= 0 ? shortestDeltaLng + 360 : shortestDeltaLng - 360
  const deltaLat = targetLat - startLat
  let lastRenderedAt = 0

  const step = (frameTime: number) => {
    if (!map || !shouldAnimateMap()) return

    const progress = Math.min((frameTime - startTime) / ROTATE_TO_CAMPUS_MS, 1)
    if (progress < 1 && frameTime - lastRenderedAt < ROTATION_FRAME_INTERVAL_MS) {
      rotationFrame = window.requestAnimationFrame(step)
      return
    }
    lastRenderedAt = frameTime
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
  if (!map || !shouldAnimateMap()) return

  map.stop()
  map.flyTo({
    ...getCampusCamera(),
    duration: FLY_TO_CAMPUS_MS,
    curve: 1.28,
    speed: 0.82,
    essential: false,
  })

  nextCampusHoldTimer = window.setTimeout(() => {
    scheduleEarthReturn()
  }, FLY_TO_CAMPUS_MS)
}

function resetCameraAndResume() {
  if (!map || !mapLoaded.value || !shouldAnimateMap()) return
  clearMapTimers()
  map.stop()
  map.jumpTo(getCampusCamera())
  scheduleEarthReturn()
}

function handleDocumentVisibilityChange() {
  if (document.hidden) {
    releaseSpacePan()
    if (scrollResumeTimer) window.clearTimeout(scrollResumeTimer)
    scrollResumeTimer = undefined
    mapPausedForScroll = false
    clearMapTimers()
    map?.stop()
    return
  }
  resetCameraAndResume()
}

function handleSpaceDown(event: KeyboardEvent) {
  if (!props.interactive || !map || !isMapVisible || event.code !== 'Space') return
  const target = event.target
  // Preserve spaces in search and normal keyboard activation of controls.
  if (
    target instanceof Element &&
    target.closest('input, textarea, select, button, a, [contenteditable="true"], [role="button"]')
  )
    return
  event.preventDefault()
  if (spacePanActive.value) return
  spacePanActive.value = true
  map.dragPan.enable()
  map.getCanvas().style.cursor = 'grab'
}

function releaseSpacePan() {
  if (!spacePanActive.value) return
  spacePanActive.value = false
  map?.dragPan.disable()
  if (map) map.getCanvas().style.cursor = ''
}

function handleSpaceUp(event: KeyboardEvent) {
  if (event.code === 'Space') releaseSpacePan()
}

function handleTouchStart() {
  if (props.interactive) map?.dragPan.enable()
}

function handleTouchEnd(event: TouchEvent) {
  if (!event.touches.length && !spacePanActive.value) map?.dragPan.disable()
}

function handleWindowScroll() {
  if (props.interactive) return
  if (!map || !isMapVisible) return

  if (!mapPausedForScroll) {
    mapPausedForScroll = true
    clearMapTimers()
    map.stop()
  }

  if (scrollResumeTimer) window.clearTimeout(scrollResumeTimer)
  scrollResumeTimer = window.setTimeout(() => {
    scrollResumeTimer = undefined
    mapPausedForScroll = false
    resetCameraAndResume()
  }, 180)
}

function shouldSkipInteractiveMap() {
  const connection = (navigator as any).connection
  return Boolean(connection?.saveData)
}

function runWhenIdle(callback: () => void) {
  cancelIdleInit()
  const run = () => {
    idleHandle = undefined
    callback()
  }
  const requestIdle = window.requestIdleCallback
  if (requestIdle) {
    idleHandle = requestIdle(run, { timeout: 1200 })
    return
  }
  idleHandle = window.setTimeout(run, 220)
}

async function initMap() {
  if (mapInitStarted) return
  mapInitStarted = true
  if (!mapboxToken || !mapContainer.value) return

  let mapboxgl: MapboxModule
  try {
    const [mapboxModule, , landmarkModule] = await Promise.all([
      import('mapbox-gl'),
      import('mapbox-gl/dist/mapbox-gl.css'),
      import('./campusLandmark'),
    ])
    mapboxgl = mapboxModule.default
    makeLandmark = landmarkModule.campusLandmarkLayer
  } catch (error) {
    mapError.value = '地图资源加载失败。'
    return
  }

  if (!isMapVisible || !mapContainer.value) {
    mapInitStarted = false
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
    projection: props.interactive ? 'mercator' : 'globe',
    antialias: !props.background,
    attributionControl: { compact: true },
    crossSourceCollisions: false,
    fadeDuration: props.background ? 0 : 300,
    interactive: props.interactive,
    // Mouse panning is enabled temporarily by the Space key. Touch remains direct.
    dragPan: false,
    dragRotate: props.interactive,
    scrollZoom: props.interactive,
    touchZoomRotate: props.interactive,
    touchPitch: props.interactive,
    clickTolerance: 5,
    minZoom: props.interactive ? 13.5 : 0,
    maxZoom: 19,
    maxPitch: 70,
    maxTileCacheSize: props.background ? 48 : undefined,
    refreshExpiredTiles: !props.background,
    renderWorldCopies: false,
    respectPrefersReducedMotion: true,
  })

  if (props.interactive) {
    map.getCanvas().setAttribute('aria-label', '校园地图：按住空格拖动平移，滚轮缩放，右键拖动旋转')
    map.addControl(new mapboxgl.NavigationControl({ visualizePitch: true }), 'bottom-right')
    map.on('click', (event: MapMouseEvent) => {
      if (spacePanActive.value) return
      if (!map?.getLayer('oa-campus-roofs')) return
      const feature = map.queryRenderedFeatures(event.point, {
        layers: ['oa-campus-labels', 'oa-campus-roofs', 'oa-campus-buildings'],
      })[0]
      if (feature?.id !== undefined) focusBuilding(feature.id)
    })
    map.on('mousemove', (event: MapMouseEvent) => {
      if (spacePanActive.value) {
        map.getCanvas().style.cursor = map.isMoving() ? 'grabbing' : 'grab'
        return
      }
      if (!map?.getLayer('oa-campus-roofs')) return
      map.getCanvas().style.cursor = map.queryRenderedFeatures(event.point, {
        layers: ['oa-campus-roofs'],
      }).length
        ? 'pointer'
        : ''
    })
  }

  map.on('load', () => {
    mapLoaded.value = true
    mapError.value = ''
    restoreStyleOverlays()
    map?.jumpTo(getCampusCamera())
    if (!props.interactive) addCampusMarker(mapboxgl)
    scheduleMapEnhancements()
    if (shouldAnimateMap()) scheduleEarthReturn()
  })

  map.on('error', (event: { error?: Error }) => {
    if (!mapLoaded.value) {
      mapError.value = event.error?.message || '请检查 Mapbox Token、样式地址或网络连接。'
    }
  })
}

onMounted(() => {
  if (!mapboxToken || !mapContainer.value || shouldSkipInteractiveMap()) return
  document.addEventListener('visibilitychange', handleDocumentVisibilityChange)
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  if (props.interactive) {
    window.addEventListener('keydown', handleSpaceDown)
    window.addEventListener('keyup', handleSpaceUp)
    window.addEventListener('blur', releaseSpacePan)
    mapContainer.value.addEventListener('touchstart', handleTouchStart, {
      passive: true,
      capture: true,
    })
    mapContainer.value.addEventListener('touchend', handleTouchEnd, { passive: true })
    mapContainer.value.addEventListener('touchcancel', handleTouchEnd, { passive: true })
  }

  if (!('IntersectionObserver' in window)) {
    runWhenIdle(() => void initMap())
    return
  }

  visibilityObserver = new IntersectionObserver(
    (entries) => {
      isMapVisible = entries.some((entry) => entry.isIntersecting)

      if (isMapVisible) {
        if (releaseMapTimer) window.clearTimeout(releaseMapTimer)
        releaseMapTimer = undefined
        if (map) {
          map.resize()
          resetCameraAndResume()
          return
        }
        runWhenIdle(() => void initMap())
        return
      }

      cancelIdleInit()
      clearMapTimers()
      map?.stop()
      if (releaseMapTimer) window.clearTimeout(releaseMapTimer)
      releaseMapTimer = window.setTimeout(() => {
        releaseMapTimer = undefined
        if (!isMapVisible) releaseMap()
      }, 5000)
    },
    { rootMargin: '240px 0px', threshold: 0.01 },
  )
  visibilityObserver.observe(mapContainer.value)
})

watch(mapboxStyle, () => {
  switchMapStyle()
})

watch(
  () => props.showLabels,
  (show) => {
    if (map?.getLayer('oa-campus-labels'))
      map.setLayoutProperty('oa-campus-labels', 'visibility', show ? 'visible' : 'none')
  },
)

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', handleDocumentVisibilityChange)
  window.removeEventListener('scroll', handleWindowScroll)
  window.removeEventListener('keydown', handleSpaceDown)
  window.removeEventListener('keyup', handleSpaceUp)
  window.removeEventListener('blur', releaseSpacePan)
  mapContainer.value?.removeEventListener('touchstart', handleTouchStart, true)
  mapContainer.value?.removeEventListener('touchend', handleTouchEnd)
  mapContainer.value?.removeEventListener('touchcancel', handleTouchEnd)
  visibilityObserver?.disconnect()
  if (releaseMapTimer) window.clearTimeout(releaseMapTimer)
  releaseMap()
})
</script>

<style scoped>
.map-section {
  --map-campus-accent: #328cb8;
  --map-campus-marker-bg: rgba(237, 248, 255, 0.9);
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100svh;
  min-height: 720px;
  overflow: hidden;
  background: var(--oa-map-bg);
}

:global(html.dark) .map-section {
  --map-campus-accent: #85c9e5;
  --map-campus-marker-bg: rgba(28, 48, 59, 0.9);
}

.map-canvas {
  position: absolute;
  z-index: 0;
  inset: 0;
  opacity: 0;
  transition: opacity 420ms ease;
}

.map-canvas.is-loaded {
  opacity: 1;
}

/*
 * Mapbox sizes its backing canvas from the container's layout dimensions. The
 * hero renders at 72% and is composited back to full size, cutting WebGL pixel
 * work by roughly half on Retina displays while keeping labels and controls out
 * of the background presentation.
 */
.map-canvas--background {
  right: auto;
  bottom: auto;
  width: 72%;
  height: 72%;
  transform: scale(1.3889);
  transform-origin: left top;
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
      rgba(29, 29, 31, 0.035) 78%,
      rgba(29, 29, 31, 0.09) 100%
    ),
    linear-gradient(180deg, rgba(255, 255, 255, 0.01), rgba(29, 29, 31, 0.03));
}

.map-grain {
  z-index: 2;
  opacity: 0.1;
  background-image:
    radial-gradient(rgba(29, 29, 31, 0.22) 0.7px, transparent 0.7px),
    radial-gradient(rgba(29, 29, 31, 0.14) 0.7px, transparent 0.7px);
  background-position:
    0 0,
    8px 8px;
  background-size:
    16px 16px,
    16px 16px;
}

.map-section--hero {
  position: absolute;
  inset: 0;
  z-index: 0;
  height: 100%;
  min-height: 0;
  isolation: isolate;
  background: var(--oa-map-bg);
}

.map-fallback {
  position: absolute;
  inset: 0;
  z-index: 4;
  opacity: 1;
  pointer-events: none;
  transition: opacity 420ms ease;
  background:
    radial-gradient(circle at 50% 48%, rgba(255, 255, 255, 0.82), transparent 18%),
    radial-gradient(circle at 50% 48%, rgba(29, 29, 31, 0.12), transparent 54%),
    linear-gradient(135deg, rgba(29, 29, 31, 0.08), transparent 44%), var(--oa-map-fallback);
  background-size: auto, auto, auto, auto;
}

.map-fallback.is-hidden {
  opacity: 0;
}

.map-status {
  position: absolute;
  z-index: 5;
  bottom: 24px;
  left: 24px;
  padding: 10px 16px;
  border-radius: 10px;
  background: var(--color-bg-page, #fff);
  color: var(--color-text-primary, #1d1d1f);
  font-size: 13px;
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
  border: 1px solid var(--map-campus-accent);
  background: var(--map-campus-marker-bg);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.18);
}

:deep(.map-campus-marker span) {
  width: 10px;
  height: 10px;
  background: var(--map-campus-accent);
}

:deep(.mapboxgl-ctrl-group) {
  border: 1px solid rgba(142, 142, 147, 0.26);
  border-radius: 8px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
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
