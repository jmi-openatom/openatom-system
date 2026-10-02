import buildings from './campusBuildings.json'
import landscape from './campusLandscape.json'
import { CAMPUS_BOUNDARY, campusHillHeight } from './campusMap'

type Position = GeoJSON.Position
type Feature = GeoJSON.Feature<GeoJSON.Geometry, Record<string, string | number>>
const lngMetres = 111320 * Math.cos((31.92 * Math.PI) / 180)
const latMetres = 111320
const collection = (features: Feature[]): GeoJSON.FeatureCollection => ({
  type: 'FeatureCollection',
  features,
})
const metres = (a: Position, b: Position) =>
  Math.hypot((b[0]! - a[0]!) * lngMetres, (b[1]! - a[1]!) * latMetres)
const move = (p: Position, x: number, y: number): Position => [
  p[0]! + x / lngMetres,
  p[1]! + y / latMetres,
]

function inside(point: Position, ring: Position[]) {
  let result = false
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const a = ring[i]!,
      b = ring[j]!
    if (
      a[1]! > point[1]! !== b[1]! > point[1]! &&
      point[0]! < ((b[0]! - a[0]!) * (point[1]! - a[1]!)) / (b[1]! - a[1]!) + a[0]!
    )
      result = !result
  }
  return result
}

function circle(center: Position, radius: number, segments = 10): Position[] {
  const ring = Array.from({ length: segments }, (_, i) => {
    const angle = (i * 2 * Math.PI) / segments
    return move(center, Math.cos(angle) * radius, Math.sin(angle) * radius)
  })
  return [...ring, ring[0]!]
}

function polygon(ring: Position[], properties: Feature['properties']): Feature {
  return { type: 'Feature', properties, geometry: { type: 'Polygon', coordinates: [ring] } }
}

function line(coordinates: Position[], properties: Feature['properties'] = {}): Feature {
  return { type: 'Feature', properties, geometry: { type: 'LineString', coordinates } }
}

const occupied = [
  ...buildings.features,
  ...landscape.features.filter((f) => !['road', 'park'].includes(f.properties.kind)),
]
  .filter((f) => f.geometry.type === 'Polygon')
  .map((f) => (f.geometry.coordinates as Position[][])[0]!)
const treeBases: Feature[] = [],
  treeCrowns: Feature[] = [],
  treeTops: Feature[] = []
const placed: Position[] = []
function addTree(point: Position) {
  if (
    !inside(point, CAMPUS_BOUNDARY.coordinates[0]!) ||
    occupied.some((ring) => inside(point, ring))
  )
    return
  if (placed.some((p) => metres(p, point) < 8)) return
  // Leave clearance around walls and the edge of the playing fields/lakes.
  if (occupied.some((ring) => ring.some((p) => metres(p, point) < 5))) return
  const variant = placed.length % 3
  const height = 6.6 + variant * 0.8
  placed.push(point)
  const elevation = campusHillHeight(point)
  treeBases.push(polygon(circle(point, 0.4, 6), { base: elevation, height: elevation + 4.7 }))
  treeCrowns.push(
    polygon(circle(point, 3.2 + variant * 0.4), {
      base: elevation + 3.8,
      height: elevation + height - 1.4,
      variant,
    }),
  )
  treeTops.push(
    polygon(circle(point, 2.4 + variant * 0.3), {
      base: elevation + height - 1.4,
      height: elevation + height,
      variant,
    }),
  )
}
for (const feature of landscape.features) {
  if (feature.properties.kind !== 'road') continue
  const path = feature.geometry.coordinates as Position[]
  for (let i = 1; i < path.length; i++) {
    const a = path[i - 1]!,
      b = path[i]!,
      length = metres(a, b)
    const ux = ((b[0]! - a[0]!) * lngMetres) / length,
      uy = ((b[1]! - a[1]!) * latMetres) / length
    for (let distance = 12; distance < length; distance += 26) {
      for (const side of [-1, 1])
        addTree(move(a, ux * distance - uy * side * 5, uy * distance + ux * side * 5))
    }
  }
}
for (const feature of landscape.features) {
  if (feature.properties.kind !== 'park') continue
  const ring = (feature.geometry.coordinates as Position[][])[0]!
  const minLng = Math.min(...ring.map((p) => p[0]!)),
    maxLng = Math.max(...ring.map((p) => p[0]!))
  const minLat = Math.min(...ring.map((p) => p[1]!)),
    maxLat = Math.max(...ring.map((p) => p[1]!))
  for (let lng = minLng + 14 / lngMetres; lng < maxLng; lng += 30 / lngMetres) {
    for (let lat = minLat + 14 / latMetres; lat < maxLat; lat += 30 / latMetres) {
      const point = [lng, lat]
      if (inside(point, ring)) addTree(point)
    }
  }
}

const sports: Feature[] = []
const fields: Position[] = []
for (const feature of landscape.features) {
  const kind = feature.properties.kind
  if (feature.properties.name === '游泳馆主池') {
    const ring = (feature.geometry.coordinates as Position[][])[0]!
    const west = Math.min(...ring.map((p) => p[0]!)),
      east = Math.max(...ring.map((p) => p[0]!))
    const north = Math.max(...ring.map((p) => p[1]!)),
      south = Math.min(...ring.map((p) => p[1]!))
    for (let lane = 1; lane < 8; lane++) {
      const lng = west + ((east - west) * lane) / 8
      sports.push(
        line([
          [lng, south],
          [lng, north],
        ]),
      )
    }
  }
  if (!['track', 'pitch', 'court'].includes(kind)) continue
  const ring = (feature.geometry.coordinates as Position[][])[0]!
  const center = [
    ring.slice(0, -1).reduce((sum, p) => sum + p[0]!, 0) / (ring.length - 1),
    ring.slice(0, -1).reduce((sum, p) => sum + p[1]!, 0) / (ring.length - 1),
  ]
  if (kind === 'track') {
    for (const scale of [0.96, 0.92, 0.88])
      sports.push(
        line(
          ring.map((p) => [
            center[0]! + (p[0]! - center[0]!) * scale,
            center[1]! + (p[1]! - center[1]!) * scale,
          ]),
        ),
      )
    continue
  }
  if (fields.some((p) => metres(p, center) < 8)) continue
  fields.push(center)
  sports.push(
    line(
      ring.map((p) => [
        center[0]! + (p[0]! - center[0]!) * 0.9,
        center[1]! + (p[1]! - center[1]!) * 0.9,
      ]),
    ),
  )
  sports.push(line(circle(center, kind === 'pitch' ? 9.15 : 1.8, 28)))
  if (ring.length === 5) {
    const a = ring[0]!,
      b = ring[1]!,
      c = ring[2]!,
      d = ring[3]!
    const mid = (p: Position, q: Position): Position => [(p[0]! + q[0]!) / 2, (p[1]! + q[1]!) / 2]
    sports.push(
      metres(a, b) > metres(b, c) ? line([mid(a, b), mid(c, d)]) : line([mid(b, c), mid(d, a)]),
    )
  }
}

export const CAMPUS_DETAILS = {
  trunks: collection(treeBases),
  crowns: collection(treeCrowns),
  tops: collection(treeTops),
  sports: collection(sports),
}
