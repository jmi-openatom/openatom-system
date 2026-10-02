const { createRequire } = require('node:module')
const { readFileSync, mkdtempSync, rmSync } = require('node:fs')
const { join } = require('node:path')
const { tmpdir } = require('node:os')
const outputDir = mkdtempSync(join(tmpdir(), 'campus-model-check-'))
process.on('exit', () => rmSync(outputDir, { recursive: true, force: true }))
process.chdir(join(__dirname, '..'))
const assert = require('node:assert/strict')
const requireFromVite = createRequire(
  createRequire(process.cwd() + '/package.json').resolve('vite'),
)
const { buildSync } = requireFromVite('esbuild')
buildSync({
  entryPoints: ['src/components/site/home/campusArchitecture.ts'],
  bundle: true,
  platform: 'node',
  format: 'cjs',
  outfile: join(outputDir, 'models.cjs'),
  logLevel: 'silent',
})
const { createCampusArchitecture } = require(join(outputDir, 'models.cjs'))
const source = JSON.parse(readFileSync('src/components/site/home/campusBuildings.json', 'utf8'))
const boundaryText = readFileSync('src/components/site/home/campusMap.ts', 'utf8')
  .split('export const CAMPUS_BOUNDARY')[1]
  .split('export const CAMPUS_BUILDING_FILTER')[0]
const boundary = [...boundaryText.matchAll(/\[(118\.[\d]+), (31\.[\d]+)\]/g)].map((match) => [
  Number(match[1]),
  Number(match[2]),
])
function inside(point, ring) {
  let result = false
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const a = ring[i],
      b = ring[j]
    if (
      a[1] > point[1] !== b[1] > point[1] &&
      point[0] < ((b[0] - a[0]) * (point[1] - a[1])) / (b[1] - a[1]) + a[0]
    )
      result = !result
  }
  return result
}
for (const feature of source.features.filter(
  (f) => f.properties.position_source === 'user-reference-campus-boundary-constrained',
))
  for (const point of feature.geometry.coordinates[0])
    assert(inside(point, boundary), `${feature.properties.name}: footprint outside campus`)
const landscape = JSON.parse(readFileSync('src/components/site/home/campusLandscape.json', 'utf8'))
for (const road of landscape.features.filter((f) => String(f.id).startsWith('reference-road-'))) {
  for (const point of road.geometry.coordinates) {
    assert(inside(point, boundary), `${road.properties.name}: road outside campus`)
    assert(
      !source.features.some(
        (f) =>
          inside(point, f.geometry.coordinates[0]) &&
          !f.geometry.coordinates.slice(1).some((hole) => inside(point, hole)),
      ),
      `${road.properties.name}: road intersects a building`,
    )
  }
}
// The satellite reference confirms a passage between these two buildings.
const haiyi = source.features.find((feature) => feature.id === 1535120628)
const jingde = source.features.find((feature) => feature.id === 'reference-11')
assert(haiyi && jingde)
const latitudeGap =
  Math.min(...haiyi.geometry.coordinates[0].map((point) => point[1])) -
  Math.max(...jingde.geometry.coordinates[0].map((point) => point[1]))
const gapMetres = latitudeGap * 111320
assert(gapMetres >= 10, 'Haiyi and Jingde must retain a clear ground-level passage')
console.log(JSON.stringify({ haiyiJingdeMinimumNorthSouthGapMetres: +gapMetres.toFixed(2) }))
const expectedNames = new Map([
  ['reference-12', '竞慧楼'],
  ['reference-14', '竞秀楼'],
  [1530948739, '澄园2号公寓'],
  [1530948742, '澄园3号公寓'],
  [1530886392, '澄园7号公寓'],
  [1530886393, '海川楼（澄园6号公寓）'],
  ['reference-swimming-center', '游泳馆'],
  ['reference-maker-center', '创客中心'],
])
for (const [id, name] of expectedNames)
  assert.equal(source.features.find((f) => f.id === id)?.properties.name, name)
const THREE = createRequire(process.cwd() + '/package.json')('three')
const metreLng = 111320 * Math.cos((31.92185 * Math.PI) / 180)
function topAt(model, id, lng, lat) {
  const entry = model.entries.find((e) => e.id === id)
  assert(entry)
  model.root.updateMatrixWorld(true)
  const ray = new THREE.Raycaster(
    new THREE.Vector3((lng - 118.89995) * metreLng, 100, -(lat - 31.92185) * 111320),
    new THREE.Vector3(0, -1, 0),
  )
  return ray.intersectObjects(
    entry.group.children.filter((child) => child.isMesh),
    false,
  )[0]?.point.y
}
for (const theme of ['light', 'dark']) {
  const model = createCampusArchitecture(theme)
  assert.equal(model.entries.length, source.features.length)
  for (const id of ['reference-0', 'reference-1', 'reference-2']) {
    const ring = source.features.find((f) => f.id === id).geometry.coordinates[0]
    const midpoint = (a, b) => [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2]
    const courtyard = midpoint(midpoint(ring[2], ring[3]), midpoint(ring[4], ring[5]))
    const courtTop = topAt(model, id, ...courtyard)
    assert(courtTop === undefined || courtTop <= 0.51, 'Qinyuan court must remain open')
    const connection = midpoint(midpoint(ring[0], ring[7]), midpoint(ring[3], ring[4]))
    assert(topAt(model, id, ...connection) > 20, 'Qinyuan western connecting wing missing')
    assert(topAt(model, id, ...midpoint(ring[0], ring[2])) > 20, 'Qinyuan wing missing')
  }
  const connectorTop = topAt(model, 1530948743, 118.89791, 31.920385)
  assert(connectorTop >= 4 && connectorTop <= 5, 'Dormitory bridge must remain low')
  assert.equal(
    topAt(model, 1530948743, 118.8982, 31.920385),
    undefined,
    'Dormitory courtyard must stay open',
  )
  assert(topAt(model, 1530948743, 118.89815, 31.92056) > 15, 'Dormitory north wing is missing')
  assert.equal(
    topAt(model, 'reference-swimming-center', 118.899405, 31.918855),
    undefined,
    'Main swimming pool must remain uncovered',
  )
  assert(
    topAt(model, 'reference-swimming-center', 118.8994, 31.91919) > 8,
    'Pool service building is missing',
  )
  assert(
    topAt(model, 'reference-swimming-center', 118.899145, 31.9187) < 6,
    'Pool-side canopy should be low',
  )

  assert.equal(new Set(model.entries.map((entry) => String(entry.id))).size, source.features.length)
  let triangles = 0,
    bytes = 0,
    meshes = 0,
    bodyMeshes = 0
  for (const entry of model.entries) {
    const feature = source.features.find((f) => String(f.id) === String(entry.id))
    assert(feature)
    entry.group.traverse((object) => {
      if (!object.geometry) return
      const position = object.geometry.attributes.position
      for (let i = 0; i < position.array.length; i++)
        assert(Number.isFinite(position.array[i]), `${entry.group.name}: invalid vertex`)
      object.geometry.computeBoundingBox()
      const box = object.geometry.boundingBox
      assert(box.min.y >= -2.51, `${entry.group.name}: underground geometry`)
      assert(
        box.max.y <= feature.properties.height + 2,
        `${entry.group.name}: roof exceeds label height`,
      )
      triangles += position.count / 3
      meshes++
      if (object.parent === entry.group) bodyMeshes++
      for (const attribute of Object.values(object.geometry.attributes))
        bytes += attribute.array.byteLength
    })
  }
  assert(bodyMeshes < 150, 'Distant view exceeds draw budget')
  assert(triangles < 250000, 'Model triangle budget exceeded')
  const library = model.entries.find((e) => e.group.name === '图书馆')
  const gym = model.entries.find((e) => e.group.name === '体育馆')
  assert(library.roofs.length && gym.roofs.length, 'Landmark selection unavailable')
  const original = gym.roofs[0].material
  model.setSelected(gym.id)
  assert.notEqual(gym.roofs[0].material, original)
  model.setSelected(null)
  assert.equal(gym.roofs[0].material, original)
  console.log(
    JSON.stringify({
      theme,
      buildings: model.entries.length,
      triangles,
      bodyMeshes,
      meshes,
      geometryMiB: (bytes / 1048576).toFixed(2),
    }),
  )
  model.root.traverse((object) => object.geometry?.dispose())
  model.materials.forEach((material) => material.dispose())
}
