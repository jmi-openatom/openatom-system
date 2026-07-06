<template>
  <div ref="containerRef" class="onboard-scene" aria-hidden="true"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    scrollEl?: HTMLElement | null
  }>(),
  { scrollEl: null },
)

const containerRef = ref<HTMLElement>()

let cleanup: (() => void) | undefined
let scrollYTarget = 0
let scrollYCurrent = 0
let pointerX = 0
let pointerY = 0

function handleScroll() {
  const el = props.scrollEl
  scrollYTarget = el ? el.scrollTop : window.scrollY
}

function handlePointer(event: PointerEvent) {
  pointerX = (event.clientX / window.innerWidth) * 2 - 1
  pointerY = (event.clientY / window.innerHeight) * 2 - 1
}

onMounted(async () => {
  const container = containerRef.value
  if (!container) return
  const prefersReducedMotion =
    window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  if (prefersReducedMotion) {
    container.classList.add('onboard-scene--static')
    return
  }

  const THREE = await import('three')
  const width = container.clientWidth || window.innerWidth
  const height = container.clientHeight || window.innerHeight

  const isDark = document.documentElement.classList.contains('dark')
  const palette = isDark
    ? {
        fog: 0x05050a,
        star: 0x9aa0b8,
        dust: 0xc9ccd2,
        monolith: 0x1d1d1f,
        ring: 0x4a4a52,
        waveBase: 0x2a2a35,
        wavePeak: 0x6a6a78,
        nebulaA: 0x2d3a8a,
        nebulaB: 0x6a2d5a,
      }
    : {
        fog: 0xf5f5f7,
        star: 0x9a9ea8,
        dust: 0x6a6a72,
        monolith: 0x1d1d1f,
        ring: 0x8a8a92,
        waveBase: 0xd0d0d8,
        wavePeak: 0x4a4a52,
        nebulaA: 0x6d7bd8,
        nebulaB: 0xc46d8a,
      }

  const scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(palette.fog, 0.016)

  const world = new THREE.Group()
  scene.add(world)

  const camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 600)
  camera.position.set(0, 0, 70)

  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'high-performance' })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(width, height)
  renderer.setClearColor(0x000000, 0)
  container.appendChild(renderer.domElement)

  // ============ 远景星云（粒子云 + 体积感） ============
  const starCount = 1200
  const starPos = new Float32Array(starCount * 3)
  const starCol = new Float32Array(starCount * 3)
  for (let i = 0; i < starCount; i += 1) {
    const radius = 90 + Math.random() * 240
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    starPos[i * 3] = radius * Math.sin(phi) * Math.cos(theta)
    starPos[i * 3 + 1] = radius * Math.sin(phi) * Math.sin(theta) * 0.7
    starPos[i * 3 + 2] = radius * Math.cos(phi) - 60
    const tint = Math.random()
    const c = new THREE.Color(palette.star).lerp(new THREE.Color(0xffffff), tint * 0.4)
    starCol[i * 3] = c.r
    starCol[i * 3 + 1] = c.g
    starCol[i * 3 + 2] = c.b
  }
  const starGeo = new THREE.BufferGeometry()
  starGeo.setAttribute('position', new THREE.BufferAttribute(starPos, 3))
  starGeo.setAttribute('color', new THREE.BufferAttribute(starCol, 3))
  const starMat = new THREE.PointsMaterial({
    size: 0.7,
    vertexColors: true,
    transparent: true,
    opacity: 0.85,
    sizeAttenuation: true,
    depthWrite: false,
    blending: isDark ? THREE.AdditiveBlending : THREE.NormalBlending,
  })
  const stars = new THREE.Points(starGeo, starMat)
  world.add(stars)

  // ============ 体积星云团（两团巨大柔光球） ============
  const nebulaMat = new THREE.PointsMaterial({
    size: 28,
    color: palette.nebulaA,
    transparent: true,
    opacity: 0.12,
    sizeAttenuation: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  })
  const nebulaA = new THREE.Points(
    new THREE.BufferGeometry().setAttribute(
      'position',
      new THREE.BufferAttribute(new Float32Array([-40, 18, -90, 0, 1, 0]), 3),
    ),
    nebulaMat,
  )
  world.add(nebulaA)

  const nebulaBMat = new THREE.PointsMaterial({
    size: 34,
    color: palette.nebulaB,
    transparent: true,
    opacity: 0.1,
    sizeAttenuation: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  })
  const nebulaB = new THREE.Points(
    new THREE.BufferGeometry().setAttribute(
      'position',
      new THREE.BufferAttribute(new Float32Array([44, -12, -110, 0, 1, 0]), 3),
    ),
    nebulaBMat,
  )
  world.add(nebulaB)

  // ============ 中景漂浮尘埃（多层不同速度） ============
  const dustCount = 500
  const dustPos = new Float32Array(dustCount * 3)
  const dustSpeed = new Float32Array(dustCount)
  const dustDepth = new Float32Array(dustCount)
  for (let i = 0; i < dustCount; i += 1) {
    dustPos[i * 3] = (Math.random() - 0.5) * 160
    dustPos[i * 3 + 1] = (Math.random() - 0.5) * 90
    dustPos[i * 3 + 2] = (Math.random() - 0.5) * 140 - 10
    dustSpeed[i] = 0.04 + Math.random() * 0.18
    dustDepth[i] = dustPos[i * 3 + 2]
  }
  const dustGeo = new THREE.BufferGeometry()
  dustGeo.setAttribute('position', new THREE.BufferAttribute(dustPos, 3))
  const dustMat = new THREE.PointsMaterial({
    size: 0.14,
    color: palette.dust,
    transparent: true,
    opacity: 0.6,
    sizeAttenuation: true,
    depthWrite: false,
    blending: isDark ? THREE.AdditiveBlending : THREE.NormalBlending,
  })
  const dust = new THREE.Points(dustGeo, dustMat)
  world.add(dust)

  // ============ 主视觉：核心巨构（双层线框 + 内核实心半透） ============
  const monolithGroup = new THREE.Group()
  monolithGroup.position.set(0, 0, -60)
  world.add(monolithGroup)

  const monoOuterGeo = new THREE.IcosahedronGeometry(14, 1)
  const monoOuterMat = new THREE.MeshBasicMaterial({
    color: palette.monolith,
    wireframe: true,
    transparent: true,
    opacity: 0.3,
  })
  const monoOuter = new THREE.Mesh(monoOuterGeo, monoOuterMat)
  monolithGroup.add(monoOuter)

  const monoInnerGeo = new THREE.IcosahedronGeometry(8, 0)
  const monoInnerMat = new THREE.MeshBasicMaterial({
    color: palette.monolith,
    wireframe: true,
    transparent: true,
    opacity: 0.45,
  })
  const monoInner = new THREE.Mesh(monoInnerGeo, monoInnerMat)
  monolithGroup.add(monoInner)

  // 内核：半透明实心，呼吸光感
  const monoCoreGeo = new THREE.IcosahedronGeometry(3.5, 0)
  const monoCoreMat = new THREE.MeshBasicMaterial({
    color: palette.monolith,
    transparent: true,
    opacity: 0.18,
  })
  const monoCore = new THREE.Mesh(monoCoreGeo, monoCoreMat)
  monolithGroup.add(monoCore)

  // 双层光环（不同倾角）
  const ring1Geo = new THREE.TorusGeometry(22, 0.06, 8, 200)
  const ring1Mat = new THREE.MeshBasicMaterial({
    color: palette.ring,
    transparent: true,
    opacity: 0.22,
  })
  const ring1 = new THREE.Mesh(ring1Geo, ring1Mat)
  ring1.rotation.x = Math.PI / 2.4
  monolithGroup.add(ring1)

  const ring2Geo = new THREE.TorusGeometry(28, 0.04, 8, 200)
  const ring2Mat = new THREE.MeshBasicMaterial({
    color: palette.ring,
    transparent: true,
    opacity: 0.14,
  })
  const ring2 = new THREE.Mesh(ring2Geo, ring2Mat)
  ring2.rotation.x = Math.PI / 3
  ring2.rotation.y = Math.PI / 4
  monolithGroup.add(ring2)

  // ============ 地平线波面 ============
  const planeGeo = new THREE.PlaneGeometry(260, 260, 90, 90)
  planeGeo.rotateX(-Math.PI / 2)
  const basePlane = Float32Array.from(planeGeo.attributes.position.array as Float32Array)
  const planeColors = new Float32Array((basePlane.length / 3) * 3)
  planeGeo.setAttribute('color', new THREE.BufferAttribute(planeColors, 3))
  const planeMat = new THREE.MeshBasicMaterial({
    vertexColors: true,
    wireframe: true,
    transparent: true,
    opacity: 0.32,
  })
  const wave = new THREE.Mesh(planeGeo, planeMat)
  wave.position.y = -18
  world.add(wave)

  const baseCol = new THREE.Color(palette.waveBase)
  const peakCol = new THREE.Color(palette.wavePeak)

  // ============ 能量流光线（沿波面流动的高亮点） ============
  const flowCount = 60
  const flowPos = new Float32Array(flowCount * 3)
  const flowParam = new Float32Array(flowCount)
  for (let i = 0; i < flowCount; i += 1) {
    flowParam[i] = Math.random()
    flowPos[i * 3] = (Math.random() - 0.5) * 200
    flowPos[i * 3 + 1] = 0
    flowPos[i * 3 + 2] = (Math.random() - 0.5) * 200
  }
  const flowGeo = new THREE.BufferGeometry()
  flowGeo.setAttribute('position', new THREE.BufferAttribute(flowPos, 3))
  const flowMat = new THREE.PointsMaterial({
    size: 1.4,
    color: isDark ? 0xffffff : 0x1d1d1f,
    transparent: true,
    opacity: 0.9,
    sizeAttenuation: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  })
  const flow = new THREE.Points(flowGeo, flowMat)
  flow.position.y = -16
  world.add(flow)

  const scrollTarget = props.scrollEl || window
  scrollTarget.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('pointermove', handlePointer, { passive: true })

  const resize = () => {
    const w = container.clientWidth || window.innerWidth
    const h = container.clientHeight || window.innerHeight
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)
  }
  window.addEventListener('resize', resize)

  let frame = 0
  let running = true
  const clock = new THREE.Clock()

  function animate() {
    if (!running) return
    frame = requestAnimationFrame(animate)
    const t = clock.getElapsedTime()

    scrollYCurrent += (scrollYTarget - scrollYCurrent) * 0.06
    const scrollMax = Math.max(1, (props.scrollEl?.scrollHeight || 1) - (props.scrollEl?.clientHeight || 0))
    const s = Math.min(scrollYCurrent / scrollMax, 1)

    // 雾随滚动浓淡：序幕稀薄 → 中段浓 → 启程散开
    ;(scene.fog as THREE.FogExp2).density = 0.01 + Math.sin(s * Math.PI) * 0.024

    // 波面起伏（随滚动加剧）+ 顶点色
    const ppos = wave.geometry.attributes.position as THREE.BufferAttribute
    const pcol = wave.geometry.attributes.color as THREE.BufferAttribute
    const vCount = basePlane.length / 3
    for (let i = 0; i < vCount; i += 1) {
      const ix = basePlane[i * 3]
      const iz = basePlane[i * 3 + 2]
      const h =
        Math.sin(ix * 0.12 + t * 0.8) * 2.4 +
        Math.cos(iz * 0.14 + t * 0.6) * 2.0 +
        Math.sin((ix + iz) * 0.08 + t * 0.5) * 1.6
      ppos.setY(i, h * (1.6 + s * 4))
      const k = Math.max(0, Math.min(1, (h + 6) / 12))
      pcol.setXYZ(
        i,
        baseCol.r + (peakCol.r - baseCol.r) * k,
        baseCol.g + (peakCol.g - baseCol.g) * k,
        baseCol.b + (peakCol.b - baseCol.b) * k,
      )
    }
    ppos.needsUpdate = true
    pcol.needsUpdate = true

    // 能量流光点沿波面流动
    const fpos = flow.geometry.attributes.position as THREE.BufferAttribute
    for (let i = 0; i < flowCount; i += 1) {
      flowParam[i] += 0.0012 + s * 0.002
      if (flowParam[i] > 1) flowParam[i] = 0
      const angle = flowParam[i] * Math.PI * 2 * 3
      const radius = 30 + flowParam[i] * 80
      const fx = Math.cos(angle) * radius
      const fz = Math.sin(angle) * radius
      const fy =
        Math.sin(fx * 0.12 + t * 0.8) * 2.4 +
        Math.cos(fz * 0.14 + t * 0.6) * 2.0 +
        Math.sin((fx + fz) * 0.08 + t * 0.5) * 1.6
      fpos.setXYZ(i, fx, fy * (1.6 + s * 4) + 2, fz)
    }
    fpos.needsUpdate = true

    // 尘埃上升 + 漂移（近处快、远处慢，形成深度视差）
    const dpos = dust.geometry.attributes.position as THREE.BufferAttribute
    for (let i = 0; i < dustCount; i += 1) {
      const yi = i * 3 + 1
      const speedFactor = 1 + (dustDepth[i] + 70) / 140
      let y = dpos.getY(yi) + dustSpeed[i] * 0.006 * speedFactor
      if (y > 45) y = -45
      dpos.setY(yi, y)
    }
    dpos.needsUpdate = true
    dust.rotation.y = t * 0.008
    stars.rotation.y = t * 0.003
    stars.rotation.x = t * 0.0015

    // 星云缓慢漂移 + 呼吸
    nebulaA.rotation.z = t * 0.01
    nebulaB.rotation.z = -t * 0.008
    nebulaMat.opacity = 0.1 + Math.sin(t * 0.25) * 0.04
    nebulaBMat.opacity = 0.08 + Math.sin(t * 0.2 + 1) * 0.04

    // 巨构：自转 + 呼吸 + 随滚动逼近
    monoOuter.rotation.x = t * 0.025
    monoOuter.rotation.y = t * 0.035
    monoInner.rotation.x = -t * 0.04
    monoInner.rotation.y = -t * 0.03
    const breath = 1 + Math.sin(t * 0.35) * 0.04
    monoCore.scale.setScalar(breath)
    monoCoreMat.opacity = 0.14 + Math.sin(t * 0.35) * 0.08
    ring1.rotation.z = t * 0.02
    ring2.rotation.z = -t * 0.016
    monolithGroup.position.z = -60 + s * 42
    monolithGroup.position.y = Math.sin(t * 0.4) * 2
    monoOuterMat.opacity = 0.28 + Math.sin(s * Math.PI) * 0.2

    // 整个世界随滚动做长镜头：横移 + 下沉 + 偏航 + 倾斜
    world.position.x = s * -22
    world.position.y = s * 10
    world.rotation.y = s * 0.52
    world.rotation.x = s * -0.16

    // 摄像机电影长镜头：推进 + 环绕 + 鼠标视差 + 滚动时轻微横摇
    const drift = Math.sin(t * 0.08) * 1.2
    const sway = Math.sin(s * Math.PI * 2) * 2.5
    camera.position.x += (pointerX * 4 + drift + s * -8 + sway - camera.position.x) * 0.04
    camera.position.y += (pointerY * -3 + s * 6 + Math.sin(t * 0.12) * 1.2 - camera.position.y) * 0.04
    camera.position.z += (70 - s * 54 - camera.position.z) * 0.04
    // 摄像机自身随滚动轻微滚转（电影感 Dutch angle）
    camera.rotation.z = s * 0.05 + Math.sin(t * 0.06) * 0.008
    camera.lookAt(s * -10, s * 3, -60 + s * 42)

    renderer.render(scene, camera)
  }
  animate()
  requestAnimationFrame(resize)

  cleanup = () => {
    running = false
    cancelAnimationFrame(frame)
    scrollTarget.removeEventListener('scroll', handleScroll)
    window.removeEventListener('pointermove', handlePointer)
    window.removeEventListener('resize', resize)
    starGeo.dispose()
    starMat.dispose()
    nebulaA.geometry.dispose()
    nebulaMat.dispose()
    nebulaB.geometry.dispose()
    nebulaBMat.dispose()
    dustGeo.dispose()
    dustMat.dispose()
    monoOuterGeo.dispose()
    monoOuterMat.dispose()
    monoInnerGeo.dispose()
    monoInnerMat.dispose()
    monoCoreGeo.dispose()
    monoCoreMat.dispose()
    ring1Geo.dispose()
    ring1Mat.dispose()
    ring2Geo.dispose()
    ring2Mat.dispose()
    planeGeo.dispose()
    planeMat.dispose()
    flowGeo.dispose()
    flowMat.dispose()
    renderer.dispose()
    if (renderer.domElement.parentNode === container) {
      container.removeChild(renderer.domElement)
    }
  }
})

onBeforeUnmount(() => {
  cleanup?.()
  cleanup = undefined
})
</script>

<style scoped>
.onboard-scene {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.onboard-scene :deep(canvas) {
  display: block;
  width: 100% !important;
  height: 100% !important;
}

.onboard-scene--static {
  background:
    radial-gradient(circle at 50% 50%, rgba(40, 40, 55, 0.5), transparent 60%),
    #05050a;
}

html:not(.dark) .onboard-scene--static {
  background:
    radial-gradient(circle at 50% 50%, rgba(180, 180, 195, 0.4), transparent 60%),
    #f5f5f7;
}
</style>
