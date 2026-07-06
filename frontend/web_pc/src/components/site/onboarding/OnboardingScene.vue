<template>
  <div ref="containerRef" class="onboard-scene" aria-hidden="true">
    <div class="onboard-scene__fallback" aria-hidden="true"></div>
  </div>
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

/** 探测浏览器是否真的能创建 WebGL 上下文 */
function canCreateWebGL(): boolean {
  try {
    const canvas = document.createElement('canvas')
    const gl = canvas.getContext('webgl2') || canvas.getContext('webgl')
    if (!gl) return false
    // 部分环境能拿到 context 但实际不可用，检查基本能力
    const glCtx = gl as WebGLRenderingContext
    if (typeof glCtx.getParameter !== 'function') return false
    const renderer = glCtx.getParameter(glCtx.RENDERER)
    if (!renderer) return false
    // "SwiftShader" / "llvmpipe" 等纯软件渲染器在服务器环境极易崩溃
    if (/swiftshader|llvmpipe|software/i.test(String(renderer))) return false
    return true
  } catch (_error) {
    return false
  }
}

onMounted(async () => {
  const container = containerRef.value
  if (!container) return
  const prefersReducedMotion =
    window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false

  // 低配设备兜底：CPU 核心少 / 内存低 直接用 CSS 背景
  const lowCpu = typeof navigator.hardwareConcurrency === 'number' && navigator.hardwareConcurrency <= 2
  const lowMem = typeof (navigator as any).deviceMemory === 'number' && (navigator as any).deviceMemory <= 2

  if (prefersReducedMotion || lowCpu || lowMem) {
    container.classList.add('onboard-scene--static')
    return
  }

  // WebGL 预检测：拿不到可用上下文就直接降级
  if (!canCreateWebGL()) {
    container.classList.add('onboard-scene--static')
    return
  }

  let contextLost = false

  try {
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
          waveBase: new THREE.Color(0x2a2a35),
          wavePeak: new THREE.Color(0x6a6a78),
          nebulaA: 0x2d3a8a,
          nebulaB: 0x6a2d5a,
        }
      : {
          fog: 0xf5f5f7,
          star: 0x9a9ea8,
          dust: 0x6a6a72,
          monolith: 0x1d1d1f,
          ring: 0x8a8a92,
          waveBase: new THREE.Color(0xd0d0d8),
          wavePeak: new THREE.Color(0x4a4a52),
          nebulaA: 0x6d7bd8,
          nebulaB: 0xc46d8a,
        }

    const scene = new THREE.Scene()
    scene.fog = new THREE.FogExp2(palette.fog, 0.016)

    const world = new THREE.Group()
    scene.add(world)

    const camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 600)
    camera.position.set(0, 0, 70)

    const renderer = new THREE.WebGLRenderer({
      antialias: false,
      alpha: true,
      powerPreference: 'default',
      failIfMajorPerformanceCaveat: true,
    })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5))
    renderer.setSize(width, height)
    renderer.setClearColor(0x000000, 0)
    container.appendChild(renderer.domElement)

    renderer.domElement.addEventListener(
      'webglcontextlost',
      (e) => {
        e.preventDefault()
        contextLost = true
        running = false
        cancelAnimationFrame(frame)
        container.classList.add('onboard-scene--static')
      },
      { once: false },
    )

    // ============ 远景星河 ============
    const starCount = 600
    const starPos = new Float32Array(starCount * 3)
    const starCol = new Float32Array(starCount * 3)
    for (let i = 0; i < starCount; i += 1) {
      const radius = 90 + Math.random() * 240
      const theta = Math.random() * Math.PI * 2
      const phi = Math.acos(2 * Math.random() - 1)
      starPos[i * 3] = radius * Math.sin(phi) * Math.cos(theta)
      starPos[i * 3 + 1] = radius * Math.sin(phi) * Math.sin(theta) * 0.7
      starPos[i * 3 + 2] = radius * Math.cos(phi) - 60
      const c = new THREE.Color(palette.star).lerp(new THREE.Color(0xffffff), Math.random() * 0.4)
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

    // ============ 体积星云团 ============
    const nebulaAMat = new THREE.PointsMaterial({
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
      nebulaAMat,
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

    // ============ 中景漂浮尘埃 ============
    const dustCount = 300
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

    // ============ 主视觉：核心巨构 ============
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

    const monoCoreGeo = new THREE.IcosahedronGeometry(3.5, 0)
    const monoCoreMat = new THREE.MeshBasicMaterial({
      color: palette.monolith,
      transparent: true,
      opacity: 0.18,
    })
    const monoCore = new THREE.Mesh(monoCoreGeo, monoCoreMat)
    monolithGroup.add(monoCore)

    const ring1Geo = new THREE.TorusGeometry(22, 0.06, 8, 100)
    const ring1Mat = new THREE.MeshBasicMaterial({
      color: palette.ring,
      transparent: true,
      opacity: 0.22,
    })
    const ring1 = new THREE.Mesh(ring1Geo, ring1Mat)
    ring1.rotation.x = Math.PI / 2.4
    monolithGroup.add(ring1)

    const ring2Geo = new THREE.TorusGeometry(28, 0.04, 8, 100)
    const ring2Mat = new THREE.MeshBasicMaterial({
      color: palette.ring,
      transparent: true,
      opacity: 0.14,
    })
    const ring2 = new THREE.Mesh(ring2Geo, ring2Mat)
    ring2.rotation.x = Math.PI / 3
    ring2.rotation.y = Math.PI / 4
    monolithGroup.add(ring2)

    // ============ 地平线波面（ShaderMaterial，GPU 计算） ============
    const waveGeo = new THREE.PlaneGeometry(260, 260, 40, 40)
    waveGeo.rotateX(-Math.PI / 2)

    const waveMat = new THREE.ShaderMaterial({
      transparent: true,
      wireframe: true,
      depthWrite: false,
      uniforms: {
        uTime: { value: 0 },
        uScroll: { value: 0 },
        uBaseColor: { value: palette.waveBase },
        uPeakColor: { value: palette.wavePeak },
        uFogColor: { value: new THREE.Color(palette.fog) },
        uFogDensity: { value: 0.016 },
      },
      vertexShader: `
        uniform float uTime;
        uniform float uScroll;
        varying float vIntensity;
        varying float vFogDepth;

        void main() {
          vec3 pos = position;
          float ix = pos.x;
          float iz = pos.z;
          float h =
            sin(ix * 0.12 + uTime * 0.8) * 2.4 +
            cos(iz * 0.14 + uTime * 0.6) * 2.0 +
            sin((ix + iz) * 0.08 + uTime * 0.5) * 1.6;
          pos.y = h * (1.6 + uScroll * 4.0);
          vIntensity = clamp((h + 6.0) / 12.0, 0.0, 1.0);
          vec4 mvPosition = modelViewMatrix * vec4(pos, 1.0);
          vFogDepth = -mvPosition.z;
          gl_Position = projectionMatrix * mvPosition;
        }
      `,
      fragmentShader: `
        uniform vec3 uBaseColor;
        uniform vec3 uPeakColor;
        uniform vec3 uFogColor;
        uniform float uFogDensity;
        varying float vIntensity;
        varying float vFogDepth;

        void main() {
          vec3 color = mix(uBaseColor, uPeakColor, vIntensity);
          float fogFactor = 1.0 - exp(-uFogDensity * uFogDensity * vFogDepth * vFogDepth);
          color = mix(color, uFogColor, clamp(fogFactor, 0.0, 1.0));
          gl_FragColor = vec4(color, 0.32);
        }
      `,
    })
    const wave = new THREE.Mesh(waveGeo, waveMat)
    wave.position.y = -18
    world.add(wave)

    // ============ 能量流光线 ============
    const flowCount = 30
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

    // 帧率监控：连续低帧则降级为 CSS 背景
    let lowFpsCount = 0
    let lastFrameTime = performance.now()

    function animate() {
      if (!running) return
      frame = requestAnimationFrame(animate)
      if (contextLost) return

      const now = performance.now()
      const delta = now - lastFrameTime
      lastFrameTime = now
      if (delta > 60) {
        lowFpsCount += 1
        if (lowFpsCount > 10) {
          running = false
          contextLost = true
          container.classList.add('onboard-scene--static')
          return
        }
      } else {
        lowFpsCount = 0
      }

      const t = clock.getElapsedTime()

      scrollYCurrent += (scrollYTarget - scrollYCurrent) * 0.06
      const scrollMax = Math.max(1, (props.scrollEl?.scrollHeight || 1) - (props.scrollEl?.clientHeight || 0))
      const s = Math.min(scrollYCurrent / scrollMax, 1)

      const fogDensity = 0.01 + Math.sin(s * Math.PI) * 0.024
      ;(scene.fog as THREE.FogExp2).density = fogDensity
      waveMat.uniforms.uFogDensity.value = fogDensity
      waveMat.uniforms.uTime.value = t
      waveMat.uniforms.uScroll.value = s

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

      nebulaA.rotation.z = t * 0.01
      nebulaB.rotation.z = -t * 0.008

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

      world.position.x = s * -22
      world.position.y = s * 10
      world.rotation.y = s * 0.52
      world.rotation.x = s * -0.16

      const drift = Math.sin(t * 0.08) * 1.2
      const sway = Math.sin(s * Math.PI * 2) * 2.5
      camera.position.x += (pointerX * 4 + drift + s * -8 + sway - camera.position.x) * 0.04
      camera.position.y += (pointerY * -3 + s * 6 + Math.sin(t * 0.12) * 1.2 - camera.position.y) * 0.04
      camera.position.z += (70 - s * 54 - camera.position.z) * 0.04
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
      nebulaAMat.dispose()
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
      waveGeo.dispose()
      waveMat.dispose()
      flowGeo.dispose()
      flowMat.dispose()
      renderer.dispose()
      if (renderer.domElement.parentNode === container) {
        container.removeChild(renderer.domElement)
      }
    }
  } catch (_error) {
    // three.js 加载失败 / WebGL 创建失败 → 降级
    container.classList.add('onboard-scene--static')
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

/* 3D canvas 正常显示时隐藏 fallback */
.onboard-scene:not(.onboard-scene--static) .onboard-scene__fallback {
  display: none;
}

/* CSS 降级背景：多层渐变模拟星云感 */
.onboard-scene__fallback {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 40% 30% at 30% 35%, rgba(80, 90, 160, 0.15), transparent 70%),
    radial-gradient(ellipse 35% 25% at 70% 60%, rgba(120, 60, 100, 0.12), transparent 70%),
    radial-gradient(circle at 50% 50%, rgba(40, 40, 55, 0.4), transparent 65%),
    #05050a;
}

html:not(.dark) .onboard-scene__fallback {
  background:
    radial-gradient(ellipse 40% 30% at 30% 35%, rgba(120, 130, 200, 0.1), transparent 70%),
    radial-gradient(ellipse 35% 25% at 70% 60%, rgba(180, 120, 150, 0.08), transparent 70%),
    radial-gradient(circle at 50% 50%, rgba(200, 200, 215, 0.3), transparent 65%),
    #f5f5f7;
}

.onboard-scene--static .onboard-scene__fallback {
  display: block;
}
</style>
