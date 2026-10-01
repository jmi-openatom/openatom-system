import buildings from './campusBuildings.json'
import { CAMPUS_HILL } from './campusMap'

export const LIGHTHOUSE_ID = 'campus-lighthouse'
export const CAMPUS_BUILDINGS = [
  ...buildings.features.map((feature) => {
    const ring = feature.geometry.coordinates[0]!.slice(0, -1)
    const center = ring
      .reduce((sum, p) => [sum[0]! + p[0]!, sum[1]! + p[1]!], [0, 0])
      .map((n) => n / ring.length) as [number, number]
    return {
      id: feature.id,
      name: feature.properties.name,
      height: feature.properties.height,
      center,
    }
  }),
  {
    id: LIGHTHOUSE_ID,
    name: '山顶灯塔',
    height: CAMPUS_HILL.rise + 30,
    center: CAMPUS_HILL.center,
  },
]

export const CAMPUS_LABELS: GeoJSON.FeatureCollection<GeoJSON.Point> = {
  type: 'FeatureCollection',
  features: [
    ...CAMPUS_BUILDINGS.map((building) => ({
      type: 'Feature' as const,
      id: building.id,
      properties: { name: building.name, height: building.height + 2 },
      geometry: { type: 'Point' as const, coordinates: building.center },
    })),
    {
      type: 'Feature',
      properties: { name: '半霞湖', height: 1 },
      geometry: { type: 'Point', coordinates: [118.9023, 31.91915] },
    },
  ],
}
