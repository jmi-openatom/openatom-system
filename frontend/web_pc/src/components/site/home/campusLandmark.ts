import * as THREE from 'three'
import type { CustomLayerInterface, Map as MapboxMap, MapSourceDataEvent } from 'mapbox-gl'
import { MercatorCoordinate } from 'mapbox-gl'
import type { ResolvedTheme } from '@/composables/useTheme'
import { CAMPUS_HILL, CAMPUS_BOUNDARY, campusPalette, campusHillHeight } from './campusMap'
import { ARCHITECTURE_MIN_ZOOM, SUN_DIRECTION, campusLighting } from './campusVisuals'
import { createCampusArchitecture } from './campusArchitecture'

/** Reference-based small hill and lighthouse, sharing the map's depth buffer. */
export function campusLandmarkLayer(theme: ResolvedTheme): CustomLayerInterface & {
  setSelected: (id: string | number | null) => void
  pick: (point: { x: number; y: number }) => string | number | undefined
} {
  const scene = new THREE.Scene()
  const camera = new THREE.Camera()
  const root = new THREE.Group()
  scene.add(root)
  const colors = campusPalette(theme)
  const material = (color: string, options: THREE.MeshStandardMaterialParameters = {}) =>
    new THREE.MeshStandardMaterial({ color, roughness: 0.85, ...options })
  const grass = material('#ffffff', { vertexColors: true, roughness: 1 })
  const stone = material(theme === 'dark' ? '#969e91' : '#eee8d8')
  const white = material(theme === 'dark' ? '#b1c2bd' : '#faf7e9')
  const blue = material('#567d90')
  const glass = material(theme === 'dark' ? '#ffdba0' : '#789da8', {
    emissive: theme === 'dark' ? '#efb66a' : '#000000',
    emissiveIntensity: theme === 'dark' ? 0.8 : 0,
  })
  const lighting = campusLighting(theme)
  scene.add(new THREE.AmbientLight(lighting.ambient, theme === 'dark' ? 1.45 : 1.8))
  const sun = new THREE.DirectionalLight(lighting.directional, theme === 'dark' ? 1.5 : 2.2)
  const azimuth = THREE.MathUtils.degToRad(SUN_DIRECTION[0])
  const polar = THREE.MathUtils.degToRad(SUN_DIRECTION[1])
  sun.position
    .set(Math.sin(azimuth) * Math.sin(polar), Math.cos(polar), -Math.cos(azimuth) * Math.sin(polar))
    .multiplyScalar(1800)
  sun.castShadow = true
  sun.shadow.mapSize.set(2048, 2048)
  Object.assign(sun.shadow.camera, {
    left: -950,
    right: 950,
    top: 950,
    bottom: -950,
    near: 100,
    far: 3500,
  })
  sun.shadow.bias = -0.00015
  sun.shadow.normalBias = 0.6
  scene.add(sun)
  const architecture = createCampusArchitecture(theme)
  root.add(architecture.root)

  const metresLng = 111320 * Math.cos((CAMPUS_HILL.center[1] * Math.PI) / 180)
  const position = (x: number, z: number): [number, number] => [
    CAMPUS_HILL.center[0] + x / metresLng,
    CAMPUS_HILL.center[1] - z / 111320,
  ]
  const localHeight = (x: number, z: number) => campusHillHeight(position(x, z))
  // Concentric elliptical rings end at the actual foot of the hill. A square
  // plane previously left a visible rectangular patch around the park.
  const hillGeometry = new THREE.BufferGeometry()
  const hillPositions: number[] = [0, CAMPUS_HILL.rise + 0.08, 0]
  const hillColors: number[] = []
  const hillIndices: number[] = []
  const rings = 32,
    segments = 64
  const grassColor = new THREE.Color(colors.park)
  const tint = (x: number, z: number, r: number) => {
    const variation = Math.sin(x * 0.047) * Math.cos(z * 0.036) * 0.035 * Math.sin(r * Math.PI)
    return grassColor.clone().multiplyScalar(1 + variation)
  }
  hillColors.push(...grassColor.toArray())
  for (let ring = 1; ring <= rings; ring++) {
    const r = ring / rings
    for (let i = 0; i < segments; i++) {
      const angle = (i / segments) * Math.PI * 2
      const x = Math.cos(angle) * r * CAMPUS_HILL.eastRadius
      const z = Math.sin(angle) * r * CAMPUS_HILL.northRadius
      hillPositions.push(x, localHeight(x, z) + 0.08, z)
      hillColors.push(...tint(x, z, r).toArray())
      const current = 1 + (ring - 1) * segments + i
      const next = 1 + (ring - 1) * segments + ((i + 1) % segments)
      if (ring === 1) hillIndices.push(0, next, current)
      else {
        const previous = current - segments,
          previousNext = next - segments
        hillIndices.push(previous, next, current, previous, previousNext, next)
      }
    }
  }
  hillGeometry.setAttribute('position', new THREE.Float32BufferAttribute(hillPositions, 3))
  hillGeometry.setAttribute('color', new THREE.Float32BufferAttribute(hillColors, 3))
  hillGeometry.setIndex(hillIndices)
  const vertices = hillGeometry.attributes.position!
  hillGeometry.computeVertexNormals()
  const hill = new THREE.Mesh(hillGeometry, grass)
  hill.receiveShadow = true
  root.add(hill)

  // One terrain-draped receiver lets custom buildings cast shadows onto the
  // native campus ground. It contains no colour/texture and cannot cover labels.
  const boundary = CAMPUS_BOUNDARY.coordinates[0]!.map((p) => [
    (p[0]! - CAMPUS_HILL.center[0]) * metresLng,
    -(p[1]! - CAMPUS_HILL.center[1]) * 111320,
  ])
  const inside = (x: number, z: number) => {
    let result = false
    for (let i = 0, j = boundary.length - 1; i < boundary.length; j = i++) {
      const a = boundary[i]!,
        b = boundary[j]!
      if (a[1]! > z !== b[1]! > z && x < ((b[0]! - a[0]!) * (z - a[1]!)) / (b[1]! - a[1]!) + a[0]!)
        result = !result
    }
    return result
  }
  const xmin = Math.min(...boundary.map((p) => p[0]!)),
    xmax = Math.max(...boundary.map((p) => p[0]!))
  const zmin = Math.min(...boundary.map((p) => p[1]!)),
    zmax = Math.max(...boundary.map((p) => p[1]!))
  const receiverGeometry = new THREE.PlaneGeometry(xmax - xmin, zmax - zmin, 40, 40)
  receiverGeometry.rotateX(-Math.PI / 2).translate((xmin + xmax) / 2, 0, (zmin + zmax) / 2)
  const receiverVertices = receiverGeometry.attributes.position!
  const receiverIndices: number[] = [],
    originalIndices = receiverGeometry.index!
  for (let i = 0; i < originalIndices.count; i += 3) {
    const ids = [originalIndices.getX(i), originalIndices.getX(i + 1), originalIndices.getX(i + 2)]
    if (
      inside(
        ids.reduce((sum, id) => sum + receiverVertices.getX(id), 0) / 3,
        ids.reduce((sum, id) => sum + receiverVertices.getZ(id), 0) / 3,
      )
    )
      receiverIndices.push(...ids)
  }
  receiverGeometry.setIndex(receiverIndices)
  const receiver = new THREE.Mesh(
    receiverGeometry,
    new THREE.ShadowMaterial({ opacity: theme === 'dark' ? 0.17 : 0.23, depthWrite: false }),
  )
  receiver.receiveShadow = true
  root.add(receiver)

  // A thin ribbon follows the slope, rather than floating above the terrain.
  const pathPoints: THREE.Vector3[] = []
  for (let i = 0; i <= 80; i++) {
    const t = i / 80
    const angle = t * Math.PI * 1.8 - 0.7
    const r = 0.97 * (1 - t) + 0.08
    const x = Math.cos(angle) * r * CAMPUS_HILL.eastRadius
    const z = Math.sin(angle) * r * CAMPUS_HILL.northRadius
    pathPoints.push(new THREE.Vector3(x, localHeight(x, z) + 0.3, z))
  }
  const pathGeometry = new THREE.BufferGeometry()
  const pathVertices: number[] = []
  const pathIndices: number[] = []
  pathPoints.forEach((p, i) => {
    const next = pathPoints[Math.min(i + 1, pathPoints.length - 1)]!
    const prev = pathPoints[Math.max(0, i - 1)]!
    const tangent = next.clone().sub(prev).normalize()
    const side = new THREE.Vector3(-tangent.z, 0, tangent.x).normalize().multiplyScalar(1.8)
    for (const sign of [-1, 1]) {
      const x = p.x + side.x * sign,
        z = p.z + side.z * sign
      pathVertices.push(x, localHeight(x, z) + 0.35, z)
    }
    if (i > 0) pathIndices.push(i * 2 - 2, i * 2, i * 2 - 1, i * 2 - 1, i * 2, i * 2 + 1)
  })
  pathGeometry.setAttribute('position', new THREE.Float32BufferAttribute(pathVertices, 3))
  pathGeometry.setIndex(pathIndices)
  pathGeometry.computeVertexNormals()
  stone.side = THREE.DoubleSide
  const path = new THREE.Mesh(pathGeometry, stone)
  root.add(path)

  const tower = new THREE.Group()
  tower.position.y = CAMPUS_HILL.rise
  root.add(tower)
  function cylinder(top: number, bottom: number, height: number, y: number, mat: THREE.Material) {
    const mesh = new THREE.Mesh(new THREE.CylinderGeometry(top, bottom, height, 32), mat)
    mesh.position.y = y
    tower.add(mesh)
    return mesh
  }
  cylinder(11, 11, 0.5, 0.35, stone)
  cylinder(5, 5.5, 1.8, 1.25, white)
  cylinder(2.8, 3.8, 20, 12, white)
  for (const y of [4, 10, 17, 22])
    cylinder(y === 22 ? 4.3 : 3.6, y === 22 ? 4.3 : 3.6, 0.5, y, stone)
  cylinder(3.1, 3.1, 3.2, 24.2, glass)
  cylinder(4.5, 4.5, 0.45, 22.2, blue)
  cylinder(0.1, 4.2, 2.8, 27.3, blue)
  cylinder(0.12, 0.12, 2, 29.5, blue)
  // Balcony rail, lantern mullions and shaft windows remain legible close up.
  for (let i = 0; i < 16; i++) {
    const a = (i * Math.PI) / 8
    const rail = cylinder(0.07, 0.07, 1.3, 23, blue)
    rail.position.x = Math.cos(a) * 4.1
    rail.position.z = Math.sin(a) * 4.1
    if (i % 2 === 0) {
      const mullion = cylinder(0.08, 0.08, 3.4, 24.2, blue)
      mullion.position.x = Math.cos(a) * 3.12
      mullion.position.z = Math.sin(a) * 3.12
    }
  }
  const ring = new THREE.Mesh(new THREE.TorusGeometry(4.1, 0.09, 8, 48), blue)
  ring.rotation.x = Math.PI / 2
  ring.position.y = 23.65
  tower.add(ring)
  for (const y of [7, 13, 19]) {
    for (let side = 0; side < 4; side++) {
      const window = new THREE.Mesh(new THREE.BoxGeometry(0.7, 1.4, 0.12), blue)
      const a = (side * Math.PI) / 2
      const radius = 3.8 - (y - 2) / 20
      window.position.set(Math.sin(a) * radius, y, Math.cos(a) * radius)
      window.rotation.y = a
      tower.add(window)
    }
  }

  let map: MapboxMap
  let renderer: THREE.WebGLRenderer
  const raycaster = new THREE.Raycaster()
  const inverseProjection = new THREE.Matrix4()
  let terrainDirty = true
  let originElevation = 0
  const refreshTerrain = (event: MapSourceDataEvent) => {
    if (event.sourceId === 'oa-terrain' && event.sourceDataType !== 'metadata') terrainDirty = true
  }
  function updateTerrain() {
    originElevation = map.queryTerrainElevation(CAMPUS_HILL.center) ?? 0
    // Sample the coarse regional DEM just 81 times, then interpolate locally.
    // This replaces thousands of elevation queries per arriving DEM tile.
    const extentX = CAMPUS_HILL.eastRadius * 1.1,
      extentZ = CAMPUS_HILL.northRadius * 1.1
    const grid = Array.from({ length: 81 }, (_, i) => {
      const x = (((i % 9) / 8) * 2 - 1) * extentX,
        z = ((Math.floor(i / 9) / 8) * 2 - 1) * extentZ
      return (map.queryTerrainElevation(position(x, z)) ?? originElevation) - originElevation
    })
    const terrainDelta = (x: number, z: number) => {
      const gx = THREE.MathUtils.clamp((x / extentX + 1) * 4, 0, 7.9999)
      const gz = THREE.MathUtils.clamp((z / extentZ + 1) * 4, 0, 7.9999)
      const ix = Math.floor(gx),
        iz = Math.floor(gz),
        tx = gx - ix,
        tz = gz - iz
      return THREE.MathUtils.lerp(
        THREE.MathUtils.lerp(grid[iz * 9 + ix]!, grid[iz * 9 + ix + 1]!, tx),
        THREE.MathUtils.lerp(grid[(iz + 1) * 9 + ix]!, grid[(iz + 1) * 9 + ix + 1]!, tx),
        tz,
      )
    }
    for (const entry of architecture.entries) {
      entry.group.position.y =
        (map.queryTerrainElevation(entry.center) ?? originElevation) - originElevation
    }
    for (let i = 0; i < vertices.count; i++) {
      const x = vertices.getX(i),
        z = vertices.getZ(i)
      vertices.setY(i, localHeight(x, z) + terrainDelta(x, z) + 0.08)
    }
    for (let i = 0; i < receiverVertices.count; i++) {
      const x = receiverVertices.getX(i),
        z = receiverVertices.getZ(i)
      const elevation = map.queryTerrainElevation(position(x, z)) ?? originElevation
      receiverVertices.setY(i, elevation - originElevation + localHeight(x, z) + 0.09)
    }
    receiverVertices.needsUpdate = true
    receiverGeometry.computeVertexNormals()
    sun.shadow.needsUpdate = true
    vertices.needsUpdate = true
    hillGeometry.computeVertexNormals()
    const ribbon = pathGeometry.attributes.position!
    for (let i = 0; i < ribbon.count; i++) {
      const x = ribbon.getX(i),
        z = ribbon.getZ(i)
      ribbon.setY(i, localHeight(x, z) + terrainDelta(x, z) + 0.35)
    }
    ribbon.needsUpdate = true
    terrainDirty = false
  }
  return {
    id: 'oa-campus-landmark',
    type: 'custom',
    renderingMode: '3d',
    setSelected(id) {
      architecture.setSelected(id)
      map?.triggerRepaint()
    },
    pick(point) {
      if (!map || map.getZoom() < ARCHITECTURE_MIN_ZOOM) return undefined
      const canvas = map.getCanvas()
      const x = (point.x / canvas.clientWidth) * 2 - 1,
        y = 1 - (point.y / canvas.clientHeight) * 2
      inverseProjection.copy(camera.projectionMatrix).invert()
      const near = new THREE.Vector3(x, y, -1).applyMatrix4(inverseProjection)
      const far = new THREE.Vector3(x, y, 1).applyMatrix4(inverseProjection)
      raycaster.set(near, far.sub(near).normalize())
      const hit = raycaster.intersectObjects(
        architecture.entries.flatMap((entry) =>
          entry.group.children.filter((child) => child !== entry.details),
        ),
        false,
      )[0]
      return hit?.object.userData.buildingId
    },
    onAdd(nextMap, gl) {
      map = nextMap
      renderer = new THREE.WebGLRenderer({ canvas: map.getCanvas(), context: gl })
      renderer.autoClear = false
      renderer.shadowMap.enabled = true
      renderer.shadowMap.type = THREE.PCFShadowMap
      renderer.shadowMap.autoUpdate = false
      renderer.shadowMap.needsUpdate = true
      tower.traverse((object) => {
        if (object instanceof THREE.Mesh) object.castShadow = true
      })
      // Terrain tiles may arrive after the model; update only when their data changes.
      map.on('sourcedata', refreshTerrain)
    },
    render(_gl, matrix) {
      if (map.getZoom() < 14.2) return
      // DEM tiles can become queryable a frame after the source notification.
      if (Math.abs((map.queryTerrainElevation(CAMPUS_HILL.center) ?? 0) - originElevation) > 0.01)
        terrainDirty = true
      if (terrainDirty) {
        updateTerrain()
        renderer.shadowMap.needsUpdate = true
      }
      architecture.root.visible = map.getZoom() >= ARCHITECTURE_MIN_ZOOM
      for (const entry of architecture.entries) entry.details.visible = map.getZoom() >= 16.1
      const origin = MercatorCoordinate.fromLngLat(CAMPUS_HILL.center, originElevation)
      const scale = origin.meterInMercatorCoordinateUnits()
      const transform = new THREE.Matrix4()
        .makeTranslation(origin.x, origin.y, origin.z)
        .scale(new THREE.Vector3(scale, -scale, scale))
        .multiply(new THREE.Matrix4().makeRotationX(Math.PI / 2))
      camera.projectionMatrix = new THREE.Matrix4()
        .fromArray(matrix as number[])
        .multiply(transform)
      renderer.resetState()
      renderer.render(scene, camera)
    },
    onRemove() {
      map.off('sourcedata', refreshTerrain)
      const geometries = new Set<THREE.BufferGeometry>()
      const materials = new Set<THREE.Material>(architecture.materials)
      scene.traverse((object) => {
        if (object instanceof THREE.Mesh) {
          geometries.add(object.geometry)
          for (const mat of Array.isArray(object.material) ? object.material : [object.material])
            materials.add(mat)
        }
      })
      geometries.forEach((geometry) => geometry.dispose())
      materials.forEach((mat) => mat.dispose())
      sun.shadow.map?.dispose()
      renderer.dispose()
    },
  }
}
