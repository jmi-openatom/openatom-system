import * as THREE from 'three'
import type { CustomLayerInterface, Map as MapboxMap, MapSourceDataEvent } from 'mapbox-gl'
import { MercatorCoordinate } from 'mapbox-gl'
import type { ResolvedTheme } from '@/composables/useTheme'
import { CAMPUS_HILL, campusPalette, campusHillHeight } from './campusMap'

/** Reference-based small hill and lighthouse, sharing the map's depth buffer. */
export function campusLandmarkLayer(theme: ResolvedTheme): CustomLayerInterface {
  const scene = new THREE.Scene()
  const camera = new THREE.Camera()
  const root = new THREE.Group()
  scene.add(root)
  const colors = campusPalette(theme)
  const material = (color: string, options: THREE.MeshStandardMaterialParameters = {}) =>
    new THREE.MeshStandardMaterial({ color, roughness: 0.85, ...options })
  const grass = material(colors.park)
  const stone = material(theme === 'dark' ? '#969e91' : '#eee8d8')
  const white = material(theme === 'dark' ? '#b1c2bd' : '#faf7e9')
  const blue = material('#567d90')
  const glass = material(theme === 'dark' ? '#ffdba0' : '#789da8', {
    emissive: theme === 'dark' ? '#efb66a' : '#000000',
    emissiveIntensity: theme === 'dark' ? 0.8 : 0,
  })
  scene.add(new THREE.AmbientLight(theme === 'dark' ? '#b8d5e1' : '#fff7df', 1.5))
  const sun = new THREE.DirectionalLight('#fff6de', 2)
  sun.position.set(-150, 200, 120)
  scene.add(sun)

  const metresLng = 111320 * Math.cos((CAMPUS_HILL.center[1] * Math.PI) / 180)
  const position = (x: number, z: number): [number, number] => [
    CAMPUS_HILL.center[0] + x / metresLng,
    CAMPUS_HILL.center[1] - z / 111320,
  ]
  const localHeight = (x: number, z: number) => campusHillHeight(position(x, z))
  const hillGeometry = new THREE.PlaneGeometry(
    CAMPUS_HILL.eastRadius * 2,
    CAMPUS_HILL.northRadius * 2,
    64,
    64,
  )
  hillGeometry.rotateX(-Math.PI / 2)
  const vertices = hillGeometry.attributes.position!
  for (let i = 0; i < vertices.count; i++) {
    vertices.setY(i, localHeight(vertices.getX(i), vertices.getZ(i)) + 0.15)
  }
  hillGeometry.computeVertexNormals()
  const hill = new THREE.Mesh(hillGeometry, grass)
  root.add(hill)

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
  let terrainDirty = true
  let originElevation = 0
  const refreshTerrain = (event: MapSourceDataEvent) => {
    if (event.sourceId === 'oa-terrain') terrainDirty = true
  }
  function updateTerrain() {
    originElevation = map.queryTerrainElevation(CAMPUS_HILL.center) ?? 0
    const terrainDelta = (x: number, z: number) =>
      (map.queryTerrainElevation(position(x, z)) ?? originElevation) - originElevation
    for (let i = 0; i < vertices.count; i++) {
      const x = vertices.getX(i),
        z = vertices.getZ(i)
      vertices.setY(i, localHeight(x, z) + terrainDelta(x, z) + 0.15)
    }
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
    onAdd(nextMap, gl) {
      map = nextMap
      renderer = new THREE.WebGLRenderer({ canvas: map.getCanvas(), context: gl })
      renderer.autoClear = false
      // Terrain tiles may arrive after the model; update only when their data changes.
      map.on('sourcedata', refreshTerrain)
    },
    render(_gl, matrix) {
      if (map.getZoom() < 14.2) return
      if (terrainDirty) updateTerrain()
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
      const materials = new Set<THREE.Material>()
      scene.traverse((object) => {
        if (object instanceof THREE.Mesh) {
          geometries.add(object.geometry)
          for (const mat of Array.isArray(object.material) ? object.material : [object.material])
            materials.add(mat)
        }
      })
      geometries.forEach((geometry) => geometry.dispose())
      materials.forEach((mat) => mat.dispose())
      renderer.dispose()
    },
  }
}
