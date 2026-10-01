import type { ExpressionSpecification } from 'mapbox-gl'
import type { ResolvedTheme } from '@/composables/useTheme'

// WGS84 campus boundary, OpenStreetMap way 92626951 (retrieved 2026-10-01).
// Keep the actual outline: a radius would also highlight neighbouring colleges.
// Source: https://www.openstreetmap.org/way/92626951 (ODbL).
// campusLandscape.json uses the same OSM snapshot for water, greenery, sports
// grounds and paths wholly inside this boundary. No extra requests at runtime.
export const CAMPUS_BOUNDARY: GeoJSON.Polygon = {
  type: 'Polygon',
  coordinates: [
    [
      [118.9035243, 31.9258295],
      [118.9012478, 31.9244254],
      [118.9002217, 31.9237194],
      [118.8997338, 31.9233295],
      [118.8992372, 31.9229382],
      [118.8988122, 31.9226009],
      [118.8985944, 31.9224278],
      [118.8983344, 31.9222308],
      [118.8980291, 31.9220111],
      [118.8977003, 31.9218054],
      [118.8968397, 31.9214406],
      [118.896268, 31.9211988],
      [118.8954155, 31.9207865],
      [118.8954055, 31.9205979],
      [118.8957891, 31.9200128],
      [118.8963202, 31.919364],
      [118.8965742, 31.919293],
      [118.8966767, 31.9191743],
      [118.8968693, 31.9189515],
      [118.8967976, 31.9188631],
      [118.8982598, 31.9172666],
      [118.8990499, 31.916404],
      [118.8992913, 31.9164016],
      [118.8996917, 31.9166229],
      [118.9001638, 31.9168209],
      [118.9005868, 31.9168888],
      [118.9012259, 31.9169712],
      [118.9019815, 31.9169981],
      [118.9045994, 31.9169981],
      [118.9072106, 31.9169952],
      [118.9086066, 31.9169936],
      [118.9081045, 31.9190872],
      [118.9075334, 31.921468],
      [118.9073622, 31.9221818],
      // East grounds confirmed by the user; treated as campus lawn.
      [118.90715, 31.92355],
      [118.90645, 31.92485],
      [118.9053, 31.92535],
      [118.9035243, 31.9258295],
    ],
  ],
}

// `within` only supports points/lines. Buildings are polygons, so use their
// distance to the campus polygon: touching/overlapping footprints measure zero.
export const CAMPUS_BUILDING_FILTER: ExpressionSpecification = [
  '==',
  ['distance', CAMPUS_BOUNDARY],
  0,
]

export const CAMPUS_BOUNDS: [[number, number], [number, number]] = [
  [118.8954055, 31.9164016],
  [118.9086066, 31.9258295],
]

// Small hill identified by the user; the regional DEM cannot resolve it.
// Position and relative height are illustrative pending a campus survey.
export const CAMPUS_HILL = {
  center: [118.89995, 31.92185] as [number, number],
  eastRadius: 95,
  northRadius: 105,
  rise: 15,
}

export function campusHillHeight(point: GeoJSON.Position) {
  const x =
    ((point[0]! - CAMPUS_HILL.center[0]) * 111320 * Math.cos((31.92 * Math.PI) / 180)) /
    CAMPUS_HILL.eastRadius
  const y = ((point[1]! - CAMPUS_HILL.center[1]) * 111320) / CAMPUS_HILL.northRadius
  const radius = Math.hypot(x, y)
  return radius < 1 ? CAMPUS_HILL.rise * Math.cos((radius * Math.PI) / 2) ** 2 : 0
}

export function campusPalette(theme: ResolvedTheme) {
  return theme === 'dark'
    ? {
        ground: '#25392f',
        low: '#8caaa2',
        middle: '#a6beb3',
        high: '#c7d4c5',
        roof: '#5986a3',
        windows: '#dfc48e',
        water: '#3a7b83',
        park: '#3d5d42',
        pitch: '#578465',
        track: '#a27570',
        boundary: '#627e6b',
        road: '#a7b7ad',
        tree: '#45694a',
        treeLight: '#658466',
        trunk: '#807563',
      }
    : {
        ground: '#e9efdf',
        low: '#f1efe5',
        middle: '#e8eddf',
        high: '#d8e6df',
        roof: '#6f9fb9',
        windows: '#7898a6',
        water: '#91d2d8',
        park: '#c2d8a7',
        pitch: '#9cbe8a',
        track: '#ce9b8d',
        boundary: '#a1b495',
        road: '#faf8f0',
        tree: '#71905e',
        treeLight: '#94ac73',
        trunk: '#95856a',
      }
}
