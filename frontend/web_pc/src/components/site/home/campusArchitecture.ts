import * as THREE from 'three'
import { mergeGeometries } from 'three/examples/jsm/utils/BufferGeometryUtils.js'
import buildings from './campusBuildings.json'
import { CAMPUS_HILL, campusPalette } from './campusMap'
import type { ResolvedTheme } from '@/composables/useTheme'

// Metre-based, footprint-aligned models reconstructed from the supplied campus
// overview / sand-table references. Storeys and unseen facades are estimates;
// the original GeoJSON remains the source of identity, placement and height.
type Point = [number, number]
type Surface = 'wall' | 'stone' | 'roof' | 'glass' | 'lit' | 'trim' | 'metal' | 'dormRoof'
export type CampusArchitecture = ReturnType<typeof createCampusArchitecture>
const radians = Math.PI / 180
const metreLng = 111320 * Math.cos(CAMPUS_HILL.center[1] * radians)
const toLocal = (p: number[], center: number[]): Point => [
  (p[0]! - center[0]!) * metreLng,
  -(p[1]! - center[1]!) * 111320,
]
const boxRing = (x: number, z: number, w: number, d: number): Point[] => [
  [x - w / 2, z - d / 2],
  [x + w / 2, z - d / 2],
  [x + w / 2, z + d / 2],
  [x - w / 2, z + d / 2],
]
function bounds(ring: Point[]) {
  const xs = ring.map((p) => p[0]),
    zs = ring.map((p) => p[1])
  const x0 = Math.min(...xs),
    x1 = Math.max(...xs),
    z0 = Math.min(...zs),
    z1 = Math.max(...zs)
  return { x: (x0 + x1) / 2, z: (z0 + z1) / 2, w: x1 - x0, d: z1 - z0 }
}
function shape(rings: Point[][]) {
  const result = new THREE.Shape(rings[0]!.map((p) => new THREE.Vector2(p[0], -p[1])))
  for (const ring of rings.slice(1))
    result.holes.push(new THREE.Path(ring.map((p) => new THREE.Vector2(p[0], -p[1]))))
  return result
}

// Horizontal clipping keeps each dormitory wing on the original irregular
// footprint, including its stair cores, rather than using a bounding box.
function clipAtZ(ring: Point[], limit: number, keepNorth: boolean): Point[] {
  const result: Point[] = []
  const inside = (p: Point) => (keepNorth ? p[1] <= limit : p[1] >= limit)
  for (let i = 0; i < ring.length; i++) {
    const a = ring[i]!,
      b = ring[(i + 1) % ring.length]!
    if (inside(a)) result.push(a)
    if (inside(a) !== inside(b)) {
      const t = (limit - a[1]) / (b[1] - a[1])
      result.push([a[0] + (b[0] - a[0]) * t, limit])
    }
  }
  return result
}

export function createCampusArchitecture(theme: ResolvedTheme) {
  const colors = campusPalette(theme),
    dark = theme === 'dark'
  const materials: Record<Surface, THREE.MeshStandardMaterial> = {
    wall: new THREE.MeshStandardMaterial({ color: colors.low, roughness: 0.9 }),
    stone: new THREE.MeshStandardMaterial({ color: dark ? '#8e9d9f' : '#c6c7bf', roughness: 0.95 }),
    dormRoof: new THREE.MeshStandardMaterial({
      color: dark ? '#47545c' : '#656059',
      roughness: 0.9,
    }),
    roof: new THREE.MeshStandardMaterial({ color: colors.roof, roughness: 0.52, metalness: 0.12 }),
    glass: new THREE.MeshStandardMaterial({
      color: dark ? '#365363' : '#496f82',
      roughness: 0.3,
      metalness: 0.25,
    }),
    lit: new THREE.MeshStandardMaterial({
      color: dark ? '#dcc7a2' : '#6f8e98',
      roughness: 0.45,
      emissive: dark ? '#e2bb76' : '#000000',
      emissiveIntensity: dark ? 0.48 : 0,
    }),
    trim: new THREE.MeshStandardMaterial({ color: dark ? '#bac6c4' : '#f3f0e5', roughness: 0.78 }),
    metal: new THREE.MeshStandardMaterial({
      color: dark ? '#627883' : '#87999b',
      roughness: 0.48,
      metalness: 0.25,
    }),
  }
  const selectedMaterial = materials.roof.clone()
  selectedMaterial.color.set('#bfa877')
  const root = new THREE.Group()
  const entries: {
    id: string | number
    center: [number, number]
    group: THREE.Group
    details: THREE.Group
    roofs: THREE.Mesh[]
    originals: Map<THREE.Mesh, THREE.Material>
  }[] = []
  for (const feature of buildings.features) {
    const ring = feature.geometry.coordinates[0]!.slice(0, -1)
    const center: [number, number] = [
      ring.reduce((s, p) => s + p[0]!, 0) / ring.length,
      ring.reduce((s, p) => s + p[1]!, 0) / ring.length,
    ]
    const rings = feature.geometry.coordinates.map((r) =>
      r.slice(0, -1).map((p) => toLocal(p, center)),
    )
    const outer = rings[0]!,
      b = bounds(outer),
      height = feature.properties.height
    const name = feature.properties.name
    const group = new THREE.Group(),
      details = new THREE.Group()
    const location = toLocal(center, CAMPUS_HILL.center)
    group.position.set(location[0], 0, location[1])
    group.name = name
    group.userData = {
      buildingId: feature.id,
      center,
      height,
      reference: 'campus-overview-and-sand-table',
      estimated: true,
    }
    root.add(group)
    const buckets = new Map<string, THREE.BufferGeometry[]>()
    function add(geometry: THREE.BufferGeometry, surface: Surface, detail = false) {
      // A single indexed layout lets all repeated windows merge into a handful
      // of draws instead of creating thousands of independent window meshes.
      const g = geometry.index ? geometry.toNonIndexed() : geometry
      if (g !== geometry) geometry.dispose()
      g.deleteAttribute('uv')
      const key = `${detail ? 'detail' : 'body'}:${surface}`
      if (!buckets.has(key)) buckets.set(key, [])
      buckets.get(key)!.push(g)
    }
    function box(
      x: number,
      y: number,
      z: number,
      w: number,
      h: number,
      d: number,
      surface: Surface,
      detail = false,
      angle = 0,
    ) {
      const geometry = new THREE.BoxGeometry(w, h, d)
      geometry.rotateY(angle).translate(x, y, z)
      add(geometry, surface, detail)
    }
    function volume(
      outline: Point[][],
      base: number,
      top: number,
      surface: Surface,
      detail = false,
    ) {
      const g = new THREE.ExtrudeGeometry(shape(outline), {
        depth: top - base,
        bevelEnabled: false,
        steps: 1,
        curveSegments: 24,
      })
      g.rotateX(-Math.PI / 2).translate(0, base, 0)
      add(g, surface, detail)
    }
    function slab(outline: Point[][], y: number, surface: Surface, thickness = 0.3) {
      volume(outline, y, y + thickness, surface)
    }
    function segment(
      a: Point,
      c: Point,
      y: number,
      h: number,
      width: number,
      surface: Surface,
      detail = false,
    ) {
      const dx = c[0] - a[0],
        dz = c[1] - a[1]
      box(
        (a[0] + c[0]) / 2,
        y + h / 2,
        (a[1] + c[1]) / 2,
        Math.hypot(dx, dz),
        h,
        width,
        surface,
        detail,
        -Math.atan2(dz, dx),
      )
    }
    function outlineBands(
      outline: Point[],
      y: number,
      h: number,
      depth: number,
      surface: Surface,
      detail = true,
    ) {
      outline.forEach((a, i) =>
        segment(a, outline[(i + 1) % outline.length]!, y, h, depth, surface, detail),
      )
    }
    function facade(outline: Point[], top: number, storey = 3.5, balcony = false, inner = false) {
      const area = outline.reduce((s, a, i) => {
        const c = outline[(i + 1) % outline.length]!
        return s + a[0] * c[1] - c[0] * a[1]
      }, 0)
      const orientation = Math.sign(area) * (inner ? -1 : 1)
      outline.forEach((a, i) => {
        const c = outline[(i + 1) % outline.length]!,
          dx = c[0] - a[0],
          dz = c[1] - a[1],
          length = Math.hypot(dx, dz)
        if (length < 5) return
        const nx = (dz / length) * orientation,
          nz = (-dx / length) * orientation
        const count = Math.max(1, Math.floor((length - 2) / (balcony ? 4.3 : 4.8)))
        const spacing = (length - 2) / count
        for (let floor = 0, base = 1.5; base + 1.8 < top - 0.6; floor++, base += storey) {
          for (let j = 0; j < count; j++) {
            const t = (1 + spacing * (j + 0.5)) / length
            const x = a[0] + dx * t + nx * 0.1,
              z = a[1] + dz * t + nz * 0.1
            const angle = -Math.atan2(dz, dx)
            const warm = (i * 7 + j * 3 + floor * 11) % 7 < 2
            // Facade panes are two triangles, not six-sided boxes. Their
            // normal follows the outer (or inner courtyard) wall orientation.
            const pane = (width: number, offset: number, surface: Surface) => {
              const g = new THREE.PlaneGeometry(width, 1.8)
              g.rotateY(angle + (orientation > 0 ? Math.PI : 0))
              g.translate(x + nx * offset, base + 0.9, z + nz * offset)
              add(g, surface, true)
            }
            pane(Math.min(2.9, spacing * 0.67), 0, warm ? 'lit' : 'glass')
            pane(0.12, 0.035, 'trim')
            if (balcony && floor > 0 && length > 20) {
              box(
                x + nx * 0.45,
                base - 0.12,
                z + nz * 0.45,
                spacing * 0.84,
                0.18,
                1.15,
                'stone',
                true,
                angle,
              )
              box(
                x + nx * 0.98,
                base + 0.35,
                z + nz * 0.98,
                spacing * 0.84,
                0.13,
                0.12,
                'metal',
                true,
                angle,
              )
            }
          }
          segment(a, c, base - 0.55, 0.22, 0.22, 'trim', true)
        }
      })
    }
    function gable(rect: Point[], eave: number, rise: number, surface: Surface) {
      // For rectangular wings, keep the ridge parallel to the longest wall.
      const area = rect.reduce((sum, a, i) => {
        const b = rect[(i + 1) % rect.length]!
        return sum + a[0] * b[1] - b[0] * a[1]
      }, 0)
      const p = area > 0 ? rect : [...rect].reverse(),
        edgeA = Math.hypot(p[1]![0] - p[0]![0], p[1]![1] - p[0]![1]),
        edgeB = Math.hypot(p[2]![0] - p[1]![0], p[2]![1] - p[1]![1])
      const q = edgeA >= edgeB ? p : [p[1]!, p[2]!, p[3]!, p[0]!]
      const mid = (a: Point, c: Point): Point => [(a[0] + c[0]) / 2, (a[1] + c[1]) / 2]
      const r0 = mid(q[0]!, q[3]!),
        r1 = mid(q[1]!, q[2]!)
      const vertices = [
        ...q.map((v) => [v[0], eave, v[1]]),
        [r0[0], eave + rise, r0[1]],
        [r1[0], eave + rise, r1[1]],
      ].flat()
      const g = new THREE.BufferGeometry()
      g.setAttribute('position', new THREE.Float32BufferAttribute(vertices, 3))
      g.setIndex([0, 4, 5, 0, 5, 1, 4, 3, 2, 4, 2, 5, 0, 3, 4, 1, 5, 2])
      g.computeVertexNormals()
      add(g, surface)
    }
    const ellipse = (x: number, z: number, rx: number, rz: number, count = 64): Point[] =>
      Array.from({ length: count }, (_, i) => {
        const a = (i / count) * Math.PI * 2
        return [x + Math.cos(a) * rx, z + Math.sin(a) * rz]
      })
    function roundBody(
      x: number,
      z: number,
      rx: number,
      rz: number,
      base: number,
      top: number,
      surface: Surface,
    ) {
      volume([ellipse(x, z, rx, rz)], base, top, surface)
    }
    function ringBody(
      x: number,
      z: number,
      rx: number,
      rz: number,
      inner: number,
      base: number,
      top: number,
      surface: Surface,
    ) {
      volume(
        [ellipse(x, z, rx, rz), ellipse(x, z, rx * inner, rz * inner).reverse()],
        base,
        top,
        surface,
      )
    }
    // The foundation extends below the sampled DEM, concealing small differences
    // between a flat floor plate and the regional elevation raster.
    volume(rings, -2.5, 0.5, 'stone')

    if (name === '图书馆') {
      // Southern circular reading wing + northern stepped, glazed fan. The
      // library is not a circular cylinder or a single extruded footprint.
      const cx = (118.903135 - center[0]) * metreLng,
        cz = -(31.92019 - center[1]) * 111320
      const rx = 33,
        rz = 31
      volume(rings, 0.5, 6.2, 'stone')
      roundBody(cx, cz, rx, rz, 5.5, 17.3, 'glass')
      for (const y of [6.2, 9.6, 13.1, 16.8])
        ringBody(cx, cz, rx + 0.8, rz + 0.8, 0.94, y, y + 0.55, 'trim')
      ringBody(cx, cz, rx + 1.2, rz + 1.2, 0.31, 17.35, 18.25, 'roof')
      roundBody(cx, cz, 10, 9.4, 17.4, 18.2, 'glass')
      for (let i = 0; i < 48; i++) {
        const a = (i / 48) * Math.PI * 2,
          x = cx + Math.cos(a) * (rx + 0.08),
          z = cz + Math.sin(a) * (rz + 0.08)
        box(x, 11.8, z, 0.22, 11, 0.32, 'trim', true, -a)
      }
      // Sloping north wing, aligned to the lake on the west. A three-part
      // profile follows the reference's high north edge and low central atrium.
      const wing = outer.slice(16, 29)
      const nb = bounds(wing)
      const roofHeight = (z: number) =>
        18.5 + 3.1 * THREE.MathUtils.clamp((nb.z + nb.d / 2 - z) / nb.d, 0, 1)
      const wingGeometry = new THREE.ExtrudeGeometry(shape([wing]), {
        depth: 1,
        bevelEnabled: false,
      })
      wingGeometry.rotateX(-Math.PI / 2)
      const wp = wingGeometry.attributes.position!
      for (let i = 0; i < wp.count; i++) wp.setY(i, wp.getY(i) > 0.5 ? roofHeight(wp.getZ(i)) : 6.2)
      wingGeometry.computeVertexNormals()
      add(wingGeometry, 'glass')
      const origin = new THREE.Vector3(cx - 5, 18.6, cz - 20)
      // The visible curved north outline is traced from the existing footprint.
      for (let i = 0; i < wing.length - 1; i++) {
        const p = wing[i]!,
          q = wing[i + 1]!
        segment(p, q, Math.min(roofHeight(p[1]), roofHeight(q[1])), 0.35, 0.45, 'trim')
        const count = Math.max(1, Math.ceil(Math.hypot(q[0] - p[0], q[1] - p[1]) / 5))
        for (let j = 0; j < count; j++) {
          const x = THREE.MathUtils.lerp(p[0], q[0], j / count),
            z = THREE.MathUtils.lerp(p[1], q[1], j / count)
          const start = new THREE.Vector3(x, roofHeight(z) + 0.2, z)
          // Short roof mullions, confined to the northern fan's roof surface.
          const end = start.clone().lerp(origin, 0.24)
          end.y = roofHeight(end.z) + 0.2
          const g = new THREE.CylinderGeometry(0.13, 0.13, start.distanceTo(end), 6)
          g.applyQuaternion(
            new THREE.Quaternion().setFromUnitVectors(
              new THREE.Vector3(0, 1, 0),
              end.clone().sub(start).normalize(),
            ),
          )
          g.translate(...start.add(end).multiplyScalar(0.5).toArray())
          add(g, 'trim', true)
        }
      }
      facade(wing, 18.5, 3.5)
      outlineBands(outer, 5.9, 0.5, 0.6, 'trim', false)
    } else if (name === '体育馆') {
      const rx = b.w / 2,
        rz = b.d / 2
      roundBody(b.x, b.z, rx * 0.94, rz * 0.94, 0.5, 12.6, 'stone')
      roundBody(b.x, b.z, rx * 0.96, rz * 0.96, 6, 12.5, 'glass')
      ringBody(b.x, b.z, rx * 0.99, rz * 0.99, 0.92, 12.5, 13.5, 'trim')
      // A shallow oval dome, with a roof rim and radial metal seams.
      const geometry = new THREE.SphereGeometry(1, 64, 16, 0, Math.PI * 2, 0, Math.PI / 2)
      geometry.scale(rx, 4.5, rz).translate(b.x, 13.5, b.z)
      add(geometry, 'trim')
      for (let i = 0; i < 48; i++) {
        const a = (i / 48) * Math.PI * 2
        const x = b.x + Math.cos(a) * rx * 0.96,
          z = b.z + Math.sin(a) * rz * 0.96
        box(x, 8.7, z, 0.6, 8.2, 0.7, 'trim', true, -a)
        const curve = new THREE.CatmullRomCurve3(
          Array.from({ length: 13 }, (_, j) => {
            const t = ((j / 12) * Math.PI) / 2
            return new THREE.Vector3(
              b.x + Math.sin(t) * Math.cos(a) * rx * 1.002,
              13.6 + Math.cos(t) * 4.5,
              b.z + Math.sin(t) * Math.sin(a) * rz * 1.002,
            )
          }),
        )
        add(new THREE.TubeGeometry(curve, 12, 0.08, 3, false), 'metal', true)
      }
    } else if (name === '游泳馆') {
      // The satellite reference shows an open pool, a north service building,
      // and a low west-side canopy. The pool itself remains a native water fill.
      const north = clipAtZ(outer, b.z - b.d / 2 + 15, true)
      const canopy = clipAtZ(outer, b.z - b.d / 2 + 15, false)
      volume([north], 0.5, 8.4, 'wall')
      facade(north, 8.4, 3.8)
      slab([north], 8.4, 'dormRoof', 0.5)
      slab([canopy], 4.7, 'trim', 0.35)
      const cb = bounds(canopy)
      for (let z = cb.z - cb.d / 2 + 3; z < cb.z + cb.d / 2; z += 7) {
        for (const x of [cb.x - cb.w / 2 + 0.5, cb.x + cb.w / 2 - 0.5])
          box(x, 2.5, z, 0.35, 4.5, 0.35, 'trim', true)
      }
    } else if (
      'model_profile' in feature.properties &&
      feature.properties.model_profile === 'qinyuan-courtyard'
    ) {
      // The supplied outline has a western connecting wing and an eastern
      // open courtyard. Keep that C-shaped footprint, including its inner walls.
      const eave = height - 1.4
      volume(rings, 0.5, eave, 'wall')
      facade(outer, eave, (eave - 1.5) / 6, true)
      slab(rings, eave, 'stone', 0.5)
      outlineBands(outer, eave + 0.5, 0.55, 0.5, 'dormRoof')
      group.userData.courtyardOpening = 'east'
    } else if ([1530948743, 1530948739, 1530886401, 1530886392].includes(feature.id as number)) {
      const northEdge = b.z - b.d / 2 + b.d * 0.34
      const southEdge = b.z + b.d / 2 - b.d * 0.34
      const wings = [clipAtZ(outer, northEdge, true), clipAtZ(outer, southEdge, false)]
      const connector = clipAtZ(clipAtZ(outer, northEdge, false), southEdge, true)
      const eave = height - 1.4
      if (connector.length >= 3) {
        volume([connector], 0.5, 4.2, 'wall')
        slab([connector], 4.2, 'stone', 0.3)
        facade(connector, 4.2, 3.5)
      }
      for (const wing of wings) {
        volume([wing], 0.5, eave, 'wall')
        facade(wing, eave, (eave - 1.5) / 6, true)
        slab([wing], eave, 'dormRoof', 0.55)
        outlineBands(wing, eave + 0.5, 0.5, 0.45, 'trim')
      }
      group.userData.wingGapMetres = southEdge - northEdge
      group.userData.connectorHeight = 4.5
    } else if (name === '海涵楼') {
      const podium = height * 0.23
      volume(rings, 0.5, podium, 'wall')
      slab(rings, podium, 'stone')
      const tower = boxRing(b.x - b.w * 0.04, b.z - b.d * 0.18, b.w * 0.84, b.d * 0.58)
      volume([tower], podium, height - 1, 'wall')
      facade(outer, podium, 3.6)
      facade(tower, height - 1, 3.6)
      slab([tower], height - 1, 'stone', 0.65)
      outlineBands(tower, height - 0.35, 0.65, 0.5, 'trim')
      const stair = boxRing(b.x + b.w * 0.27, b.z - b.d * 0.1, b.w * 0.07, b.d * 0.62)
      volume([stair], podium, height - 2.2, 'glass')
    } else if (['躬行楼', '笃行楼', '景行楼'].includes(name)) {
      // Courtyard teaching blocks: three wings, an open inner court, pitched
      // pale roofs. The previous rectangular placeholder filled this courtyard.
      const wings = [
        boxRing(b.x, b.z - b.d * 0.34, b.w, b.d * 0.32),
        boxRing(b.x - b.w * 0.36, b.z + b.d * 0.13, b.w * 0.28, b.d * 0.62),
        boxRing(b.x + b.w * 0.36, b.z + b.d * 0.13, b.w * 0.28, b.d * 0.62),
      ]
      for (const wing of wings) {
        volume([wing], 0.5, height - 3, 'wall')
        facade(wing, height - 3)
        gable(wing, height - 3, 3, 'stone')
      }
    } else {
      const dorm = name.includes('公寓'),
        teaching = /德楼|慧楼|秀楼|行楼/.test(name)
      const roofSurface: Surface =
        name.startsWith('澄园') || name.startsWith('海川') ? 'dormRoof' : 'roof'
      const eave = dorm || teaching ? height - 1.4 : height - 0.6
      volume(rings, 0.5, eave, 'wall')
      slab(rings, eave, 'stone', 0.45)
      for (const [ringIndex, r] of rings.entries()) {
        facade(r, eave, dorm ? (eave - 1.5) / 6 : 3.6, dorm, ringIndex > 0)
        outlineBands(r, eave + 0.35, 0.65, 0.42, 'trim')
      }
      if (outer.length === 4 && (dorm || teaching)) {
        // Pale flat decks, blue facade-side strips and raised white end frames
        // follow the supplied overview rather than making the whole roof blue.
        const edgeLength = (a: Point, c: Point) => Math.hypot(c[0] - a[0], c[1] - a[1])
        const q =
          edgeLength(outer[0]!, outer[1]!) >= edgeLength(outer[1]!, outer[2]!)
            ? outer
            : [outer[1]!, outer[2]!, outer[3]!, outer[0]!]
        const mix = (a: Point, c: Point, t: number): Point => [
          THREE.MathUtils.lerp(a[0], c[0], t),
          THREE.MathUtils.lerp(a[1], c[1], t),
        ]
        const band = [
          mix(q[0]!, q[3]!, 0.07),
          mix(q[1]!, q[2]!, 0.07),
          mix(q[1]!, q[2]!, 0.4),
          mix(q[0]!, q[3]!, 0.4),
        ]
        gable(band, eave + 0.4, 0.9, roofSurface)
        for (const t of [0.04, 0.92]) {
          const frame = [
            mix(q[0]!, q[1]!, t),
            mix(q[0]!, q[1]!, t + 0.04),
            mix(q[3]!, q[2]!, t + 0.04),
            mix(q[3]!, q[2]!, t),
          ]
          volume([frame], eave + 0.45, eave + 1.15, 'trim')
        }
      } else {
        slab(rings, eave + 0.5, name === '办公楼' ? 'stone' : roofSurface, 0.22)
        // Recessed roof equipment is kept small and inside the footprint; no
        // arbitrary towers or decorative skyline changes are introduced.
        if (outer.length === 4 && b.w > 20 && b.d > 12) {
          box(b.x, eave + 0.9, b.z, b.w * 0.27, 0.8, b.d * 0.35, 'stone', true)
        }
      }
      // Long principal wings in the reference have a white raised end frame
      // and a blue roof strip, retaining the measured footprint underneath.
      if (teaching && outer.length === 4) {
        const a = outer[0]!,
          c = outer[1]!
        segment(a, c, eave - 0.5, 1.2, 0.7, 'trim')
      }
    }
    const roofs: THREE.Mesh[] = []
    for (const [key, geometries] of buckets) {
      const [level, surface] = key.split(':') as [string, Surface]
      const merged = mergeGeometries(geometries)
      geometries.forEach((g) => g.dispose())
      if (!merged) throw new Error(`Could not merge campus model: ${name} / ${key}`)
      merged.computeBoundingSphere()
      const mesh = new THREE.Mesh(merged, materials[surface])
      mesh.userData.buildingId = feature.id
      mesh.castShadow = level === 'body'
      mesh.receiveShadow = true
      ;(level === 'detail' ? details : group).add(mesh)
      if (surface === 'roof' || surface === 'dormRoof') roofs.push(mesh)
    }
    group.add(details)
    if (!roofs.length) {
      const paleRoof = group.children.find(
        (child) => child instanceof THREE.Mesh && child.material === materials.trim,
      )
      if (paleRoof instanceof THREE.Mesh) roofs.push(paleRoof)
    }
    const originals = new Map(roofs.map((mesh) => [mesh, mesh.material as THREE.Material]))
    entries.push({ id: feature.id, center, group, details, roofs, originals })
  }
  return {
    root,
    entries,
    setSelected(id: string | number | null) {
      for (const entry of entries)
        for (const mesh of entry.roofs)
          mesh.material =
            String(entry.id) === String(id) ? selectedMaterial : entry.originals.get(mesh)!
    },
    // Hidden materials may not be reachable by traversing the scene on removal.
    materials: [...Object.values(materials), selectedMaterial],
  }
}
