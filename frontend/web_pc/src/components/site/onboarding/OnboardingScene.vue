<template>
  <div ref="containerRef" class="onboard-scene" aria-hidden="true">
    <canvas ref="canvasRef" class="onboard-scene__canvas"></canvas>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import gsap from 'gsap'

const props = withDefaults(
  defineProps<{
    scrollEl?: HTMLElement | null
  }>(),
  { scrollEl: null },
)

const containerRef = ref<HTMLElement>()
const canvasRef = ref<HTMLCanvasElement>()

let canvasCtx: CanvasRenderingContext2D | null = null
let resizeObserver: ResizeObserver | null = null
let width = 0
let height = 0
let dpr = 1

let scrollYTarget = 0
let scrollYCurrent = 0
let pointerX = 0
let pointerY = 0

// Camera coordinate properties
const camera = {
  x: 0,
  y: 0,
  z: 70,
  rx: 0,
  ry: 0,
  rz: 0,
}

// Center of the atomic model
const monolithPos = { x: 0, y: 0, z: -60 }

// GSAP context & quickTo animators
let gsapCtx: gsap.Context | null = null
let camXTo: gsap.QuickToFunc | undefined
let camYTo: gsap.QuickToFunc | undefined
let camZTo: gsap.QuickToFunc | undefined
let camRzTo: gsap.QuickToFunc | undefined

function handleScroll() {
  const el = props.scrollEl
  scrollYTarget = el ? el.scrollTop : window.scrollY
}

function handlePointer(event: PointerEvent) {
  pointerX = (event.clientX / window.innerWidth) * 2 - 1
  pointerY = (event.clientY / window.innerHeight) * 2 - 1
}

// 3D perspective projection helper
function project(x: number, y: number, z: number, w: number, h: number) {
  const dx = x - camera.x
  const dy = y - camera.y
  const dz = z - camera.z

  const cosY = Math.cos(camera.ry)
  const sinY = Math.sin(camera.ry)
  const x1 = dx * cosY - dz * sinY
  const z1 = dx * sinY + dz * cosY

  const cosX = Math.cos(camera.rx)
  const sinX = Math.sin(camera.rx)
  const y2 = dy * cosX - z1 * sinX
  const z2 = dy * sinX + z1 * cosX

  const cosZ = Math.cos(camera.rz)
  const sinZ = Math.sin(camera.rz)
  const x3 = x1 * cosZ - y2 * sinZ
  const y3 = x1 * sinZ + y2 * cosZ

  const fov = Math.max(w, h) * 0.8
  if (z2 <= 2) return null // Behind camera clipping

  const px = w / 2 + x3 * (fov / z2)
  const py = h / 2 + y3 * (fov / z2)
  return { x: px, y: py, depth: z2 }
}

// World transform for scroll-driven space tilt/translation
function transformWorld(point: number[], s: number) {
  const [x, y, z] = point

  // World rotation Y (yaw)
  const ry = s * 0.52
  const cosY = Math.cos(ry)
  const sinY = Math.sin(ry)
  const x1 = x * cosY - z * sinY
  const z1 = x * sinY + z * cosY

  // World rotation X (pitch)
  const rx = s * -0.16
  const cosX = Math.cos(rx)
  const sinX = Math.sin(rx)
  const y2 = y * cosX - z1 * sinX
  const z2 = y * sinX + z1 * cosX

  // World translation
  const tx = s * -22
  const ty = s * 10
  const tz = 0

  return [x1 + tx, y2 + ty, z2 + tz]
}

// Rotation helpers
function rotateX(point: number[], angle: number) {
  const [x, y, z] = point
  const cos = Math.cos(angle)
  const sin = Math.sin(angle)
  return [x, y * cos - z * sin, y * sin + z * cos]
}

function rotateY(point: number[], angle: number) {
  const [x, y, z] = point
  const cos = Math.cos(angle)
  const sin = Math.sin(angle)
  return [x * cos + z * sin, y, -x * sin + z * cos]
}

function rotateZ(point: number[], angle: number) {
  const [x, y, z] = point
  const cos = Math.cos(angle)
  const sin = Math.sin(angle)
  return [x * cos - y * sin, x * sin + y * cos, z]
}

// 3D Atomic nucleus particles: Proton/Neutron cluster
const nucleusParticles = [
  { x: -1.2, y: 1.0, z: -0.6, color: '#0066cc', size: 3.2 },
  { x: 1.2, y: -0.8, z: 1.0, color: '#4a90e2', size: 3.0 },
  { x: -0.8, y: -1.2, z: -1.0, color: '#0066cc', size: 3.4 },
  { x: 1.0, y: 1.2, z: 0.8, color: '#4a90e2', size: 3.1 },
  { x: -1.0, y: -0.4, z: 1.2, color: '#0066cc', size: 3.3 },
  { x: 0.8, y: -1.0, z: -1.2, color: '#4a90e2', size: 2.9 },
  { x: 0, y: 0, z: 0, color: '#ffffff', size: 4.0 }
]

// Stars setup
const stars: { x: number; y: number; z: number; size: number }[] = []
const starCount = 80
for (let i = 0; i < starCount; i++) {
  stars.push({
    x: (Math.random() - 0.5) * 400,
    y: (Math.random() - 0.5) * 260,
    z: -80 + Math.random() * 450,
    size: 0.5 + Math.random() * 1.2
  })
}

// Dust setup
const dust: { x: number; y: number; z: number; size: number; speed: number }[] = []
const dustCount = 40
for (let i = 0; i < dustCount; i++) {
  dust.push({
    x: (Math.random() - 0.5) * 260,
    y: (Math.random() - 0.5) * 160,
    z: -30 + Math.random() * 150,
    size: 0.6 + Math.random() * 1.0,
    speed: 0.08 + Math.random() * 0.16
  })
}

// Flow streams setup
const flowCount = 12
const flowParam = new Float32Array(flowCount)
const flowPoints: { x: number; z: number }[] = []
for (let i = 0; i < flowCount; i++) {
  flowParam[i] = Math.random()
  flowPoints.push({
    x: (Math.random() - 0.5) * 160,
    z: (Math.random() - 0.5) * 160
  })
}

function gridAlpha(gx: number, gz: number, depth: number) {
  const halfSize = 130
  const distFromCenter = Math.sqrt(gx * gx + gz * gz) / halfSize
  const radialFade = Math.max(0, 1 - distFromCenter * distFromCenter)
  const depthFade = Math.max(0, Math.min(1, (180 - depth) / 140))
  return radialFade * depthFade * 0.35
}

function waveHeight(gx: number, gz: number, t: number) {
  return Math.sin(gx * 0.1 + t * 0.7) * 2.8 +
         Math.cos(gz * 0.12 + t * 0.55) * 2.2 +
         Math.sin((gx + gz) * 0.07 + t * 0.42) * 1.8
}

function getPalette() {
  const isDark = document.documentElement.classList.contains('dark')
  return isDark
    ? {
        bg: '#05050a',
        star: 'rgba(154, 160, 184, 0.72)',
        dust: 'rgba(201, 204, 210, 0.44)',
        ring1: 'rgba(0, 102, 204, 0.42)', // Theme blue
        ring2: 'rgba(74, 144, 226, 0.38)', // Soft blue
        ring3: 'rgba(109, 123, 216, 0.32)',
        waveLine: 'rgba(106, 106, 120, 0.24)',
        flow: 'rgba(255, 255, 255, 0.68)',
      }
    : {
        bg: '#f5f5f7',
        star: 'rgba(154, 158, 168, 0.62)',
        dust: 'rgba(106, 106, 114, 0.32)',
        ring1: 'rgba(0, 102, 204, 0.34)',
        ring2: 'rgba(74, 144, 226, 0.3)',
        ring3: 'rgba(138, 138, 146, 0.24)',
        waveLine: 'rgba(74, 74, 82, 0.18)',
        flow: 'rgba(29, 29, 31, 0.52)',
      }
}

function resize() {
  const container = containerRef.value
  const canvas = canvasRef.value
  if (!container || !canvas) return
  width = container.clientWidth || window.innerWidth
  height = container.clientHeight || window.innerHeight
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvas.width = Math.round(width * dpr)
  canvas.height = Math.round(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  canvasCtx = canvas.getContext('2d')
  canvasCtx?.scale(dpr, dpr)
}

// 1. Draw 3D Nucleus spherical cluster
function drawNucleus(ctx: CanvasRenderingContext2D, s: number, isDark: boolean) {
  const breath = 1 + Math.sin(animTime * 0.45) * 0.05
  const rotXAngle = animTime * 0.22
  const rotYAngle = animTime * 0.32

  const projectedParticles: { x: number; y: number; depth: number; size: number; color: string }[] = []

  nucleusParticles.forEach(p => {
    // local rotation of the nucleus sphere coordinates
    const [rxX, ryY, rzZ] = rotateX([p.x, p.y, p.z], rotXAngle)
    const [rxY, ryX, rzZ2] = rotateY([rxX, ryY, rzZ], rotYAngle)

    // translate relative to monolith center
    const x = rxY * breath * 4.5 + monolithPos.x
    const y = ryX * breath * 4.5 + monolithPos.y
    const z = rzZ2 * breath * 4.5 + monolithPos.z

    const transformed = transformWorld([x, y, z], s)
    const proj = project(transformed[0], transformed[1], transformed[2], width, height)
    if (proj) {
      let displayColor = p.color
      if (!isDark && p.color === '#ffffff') {
        displayColor = '#1d1d1f'
      }
      projectedParticles.push({
        x: proj.x,
        y: proj.y,
        depth: proj.depth,
        size: p.size * (150 / proj.depth),
        color: displayColor
      })
    }
  })

  // Simple Z-sorting for overlaps
  projectedParticles.sort((a, b) => b.depth - a.depth)

  // Render spheres
  projectedParticles.forEach(p => {
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
    ctx.fillStyle = p.color
    ctx.fill()

    // Sphere highlight shine
    ctx.beginPath()
    ctx.arc(p.x - p.size * 0.28, p.y - p.size * 0.28, p.size * 0.26, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255, 255, 255, 0.42)'
    ctx.fill()
  })
}

// 2. Draw 3D Electron Orbits and orbiting electrons with tails
function drawElectronOrbit(
  ctx: CanvasRenderingContext2D,
  radiusX: number,
  radiusY: number,
  rxInit: number,
  ryInit: number,
  electronAngle: number,
  s: number,
  orbitColor: string,
  electronColor: string,
  isDark: boolean
) {
  // Draw Orbit Line
  ctx.beginPath()
  let first = true
  const segmentCount = 64

  for (let i = 0; i <= segmentCount; i++) {
    const theta = (i / segmentCount) * Math.PI * 2
    const x = radiusX * Math.cos(theta)
    const y = radiusY * Math.sin(theta)
    const z = 0

    // Tilt translation
    const [rx1, ry1, rz1] = rotateX([x, y, z], rxInit)
    const [rx2, ry2, rz2] = rotateY([rx1, ry1, rz1], ryInit)

    const px = rx2 + monolithPos.x
    const py = ry2 + monolithPos.y
    const pz = rz2 + monolithPos.z

    const transformedPt = transformWorld([px, py, pz], s)
    const proj = project(transformedPt[0], transformedPt[1], transformedPt[2], width, height)
    if (proj) {
      if (first) {
        ctx.moveTo(proj.x, proj.y)
        first = false
      } else {
        ctx.lineTo(proj.x, proj.y)
      }
    }
  }
  ctx.strokeStyle = orbitColor
  ctx.lineWidth = 0.8
  ctx.stroke()

  // Draw electron with fading physical trails
  const trailLength = 8
  for (let j = 0; j < trailLength; j++) {
    const trailAngle = electronAngle - (j * 0.05)
    const ex = radiusX * Math.cos(trailAngle)
    const ey = radiusY * Math.sin(trailAngle)
    const ez = 0

    const [erx1, ery1, erz1] = rotateX([ex, ey, ez], rxInit)
    const [erx2, ery2, erz2] = rotateY([erx1, ery1, erz1], ryInit)

    const epx = erx2 + monolithPos.x
    const epy = ery2 + monolithPos.y
    const epz = erz2 + monolithPos.z

    const eTransformed = transformWorld([epx, epy, epz], s)
    const eProj = project(eTransformed[0], eTransformed[1], eTransformed[2], width, height)
    if (eProj) {
      ctx.beginPath()
      const alpha = (trailLength - j) / trailLength
      const size = (3.6 - j * 0.25) * (120 / eProj.depth)
      ctx.arc(eProj.x, eProj.y, Math.max(0.5, size), 0, Math.PI * 2)
      ctx.fillStyle = j === 0
        ? electronColor
        : (isDark ? `rgba(245, 245, 247, ${alpha * 0.4})` : `rgba(29, 29, 31, ${alpha * 0.4})`)
      ctx.fill()

      if (j === 0) {
        // Outer glowing halo
        ctx.beginPath()
        ctx.arc(eProj.x, eProj.y, size * 2.2, 0, Math.PI * 2)
        ctx.fillStyle = orbitColor.replace('0.42', '0.18').replace('0.38', '0.18').replace('0.32', '0.18')
        ctx.fill()
      }
    }
  }
}

// 3. Draw Nebula ambient background glows
function drawNebula(ctx: CanvasRenderingContext2D, isDark: boolean, s: number) {
  // Nebula A
  const ptA = transformWorld([-45, 20, -85], s)
  const projA = project(ptA[0], ptA[1], ptA[2], width, height)
  if (projA) {
    const size = 330 * (100 / projA.depth)
    const grad = ctx.createRadialGradient(projA.x, projA.y, 0, projA.x, projA.y, size)
    const color = isDark ? 'rgba(45, 58, 138, 0.16)' : 'rgba(109, 123, 216, 0.14)'
    grad.addColorStop(0, color)
    grad.addColorStop(1, 'transparent')
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(projA.x, projA.y, size, 0, Math.PI * 2)
    ctx.fill()
  }

  // Nebula B
  const ptB = transformWorld([45, -15, -105], s)
  const projB = project(ptB[0], ptB[1], ptB[2], width, height)
  if (projB) {
    const size = 390 * (100 / projB.depth)
    const grad = ctx.createRadialGradient(projB.x, projB.y, 0, projB.x, projB.y, size)
    const color = isDark ? 'rgba(106, 45, 90, 0.14)' : 'rgba(196, 109, 138, 0.12)'
    grad.addColorStop(0, color)
    grad.addColorStop(1, 'transparent')
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(projB.x, projB.y, size, 0, Math.PI * 2)
    ctx.fill()
  }
}

let animTime = 0
let killed = false

function render() {
  if (killed) return
  const canvas = canvasRef.value
  const ctx = canvasCtx
  if (!canvas || !ctx) return

  animTime += 0.012

  const isDark = document.documentElement.classList.contains('dark')
  const palette = getPalette()

  // Clear and fill canvas
  ctx.fillStyle = palette.bg
  ctx.fillRect(0, 0, width, height)

  // Smooth scroll interpolation
  scrollYCurrent += (scrollYTarget - scrollYCurrent) * 0.06
  const scrollEl = props.scrollEl
  const scrollMax = Math.max(1, (scrollEl?.scrollHeight || 1) - (scrollEl?.clientHeight || 0))
  const s = Math.min(scrollYCurrent / scrollMax, 1)

  // Mouse tilt camera drift
  const drift = Math.sin(animTime * 0.08) * 1.2
  const sway = Math.sin(s * Math.PI * 2) * 2.5

  const tx = pointerX * 4 + drift + s * -8 + sway
  const ty = pointerY * -3 + s * 6 + Math.sin(animTime * 0.12) * 1.2
  const tz = 70 - s * 54
  const trz = s * 0.05 + Math.sin(animTime * 0.06) * 0.008

  // Drive camera coordinates via GSAP quickTo lag
  camXTo?.(tx)
  camYTo?.(ty)
  camZTo?.(tz)
  camRzTo?.(trz)

  // Camera lookAt calculations
  const targetX = s * -10
  const targetY = s * 3
  const targetZ = -60 + s * 42

  const dx = targetX - camera.x
  const dy = targetY - camera.y
  const dz = targetZ - camera.z

  camera.ry = Math.atan2(dx, dz)
  camera.rx = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))

  monolithPos.z = -60 + s * 42
  monolithPos.y = Math.sin(animTime * 0.4) * 2

  // 1. Draw Nebula Background Glows
  drawNebula(ctx, isDark, s)

  // 2. Draw Stars Background with scroll parallax
  ctx.fillStyle = palette.star
  for (let i = 0; i < stars.length; i++) {
    const star = stars[i]
    const starRot = rotateY([star.x, star.y, star.z], animTime * 0.002)
    const starTransformed = transformWorld(starRot, s)
    const proj = project(starTransformed[0], starTransformed[1], starTransformed[2], width, height)
    if (proj) {
      ctx.beginPath()
      ctx.arc(proj.x, proj.y, star.size * (150 / proj.depth), 0, Math.PI * 2)
      ctx.fill()
    }
  }

  // 3. Draw Floating Space Dust
  ctx.fillStyle = palette.dust
  for (let i = 0; i < dust.length; i++) {
    const d = dust[i]
    d.y += d.speed
    if (d.y > 100) d.y = -100

    const dustRot = rotateY([d.x, d.y, d.z], animTime * 0.005)
    const dustTransformed = transformWorld(dustRot, s)
    const proj = project(dustTransformed[0], dustTransformed[1], dustTransformed[2], width, height)
    if (proj) {
      ctx.beginPath()
      ctx.arc(proj.x, proj.y, d.size * (100 / proj.depth), 0, Math.PI * 2)
      ctx.fill()
    }
  }

  // 4. Draw Premium Holographic Wave Grid
  const gridHalfSize = 130
  const gridStep = 32
  const curveResolution = 6

  // Draw smooth flowing curves along X direction
  for (let gz = -gridHalfSize; gz <= gridHalfSize; gz += gridStep) {
    const points: { x: number; y: number; depth: number; gx: number; gz: number }[] = []
    for (let gx = -gridHalfSize; gx <= gridHalfSize; gx += curveResolution) {
      const h = waveHeight(gx, gz, animTime)
      const gy = -18 + h * (1.6 + s * 4.0)
      const transformedPt = transformWorld([gx, gy, gz], s)
      const proj = project(transformedPt[0], transformedPt[1], transformedPt[2], width, height)
      if (proj) {
        points.push({ x: proj.x, y: proj.y, depth: proj.depth, gx, gz })
      }
    }
    if (points.length < 3) continue

    ctx.beginPath()
    ctx.moveTo(points[0].x, points[0].y)
    for (let i = 1; i < points.length - 1; i++) {
      const cpx = (points[i].x + points[i + 1].x) / 2
      const cpy = (points[i].y + points[i + 1].y) / 2
      ctx.quadraticCurveTo(points[i].x, points[i].y, cpx, cpy)
    }
    const last = points[points.length - 1]
    ctx.lineTo(last.x, last.y)

    const midPt = points[Math.floor(points.length / 2)]
    const alpha = gridAlpha(0, gz, midPt.depth)
    ctx.strokeStyle = isDark
      ? `rgba(80, 130, 200, ${alpha})`
      : `rgba(40, 80, 160, ${alpha})`
    ctx.lineWidth = 0.7
    ctx.stroke()
  }

  // Draw smooth flowing curves along Z direction
  for (let gx = -gridHalfSize; gx <= gridHalfSize; gx += gridStep) {
    const points: { x: number; y: number; depth: number; gx: number; gz: number }[] = []
    for (let gz = -gridHalfSize; gz <= gridHalfSize; gz += curveResolution) {
      const h = waveHeight(gx, gz, animTime)
      const gy = -18 + h * (1.6 + s * 4.0)
      const transformedPt = transformWorld([gx, gy, gz], s)
      const proj = project(transformedPt[0], transformedPt[1], transformedPt[2], width, height)
      if (proj) {
        points.push({ x: proj.x, y: proj.y, depth: proj.depth, gx, gz })
      }
    }
    if (points.length < 3) continue

    ctx.beginPath()
    ctx.moveTo(points[0].x, points[0].y)
    for (let i = 1; i < points.length - 1; i++) {
      const cpx = (points[i].x + points[i + 1].x) / 2
      const cpy = (points[i].y + points[i + 1].y) / 2
      ctx.quadraticCurveTo(points[i].x, points[i].y, cpx, cpy)
    }
    const last = points[points.length - 1]
    ctx.lineTo(last.x, last.y)

    const midPt = points[Math.floor(points.length / 2)]
    const alpha = gridAlpha(gx, 0, midPt.depth)
    ctx.strokeStyle = isDark
      ? `rgba(80, 130, 200, ${alpha})`
      : `rgba(40, 80, 160, ${alpha})`
    ctx.lineWidth = 0.7
    ctx.stroke()
  }

  // Draw small glowing intersection dots at grid vertices
  for (let gz = -gridHalfSize; gz <= gridHalfSize; gz += gridStep) {
    for (let gx = -gridHalfSize; gx <= gridHalfSize; gx += gridStep) {
      const h = waveHeight(gx, gz, animTime)
      const gy = -18 + h * (1.6 + s * 4.0)
      const transformedPt = transformWorld([gx, gy, gz], s)
      const proj = project(transformedPt[0], transformedPt[1], transformedPt[2], width, height)
      if (proj) {
        const alpha = gridAlpha(gx, gz, proj.depth) * 1.6
        if (alpha > 0.03) {
          const dotSize = 1.2 * (100 / proj.depth)
          ctx.beginPath()
          ctx.arc(proj.x, proj.y, dotSize, 0, Math.PI * 2)
          ctx.fillStyle = isDark
            ? `rgba(120, 170, 255, ${alpha})`
            : `rgba(0, 102, 204, ${alpha})`
          ctx.fill()
        }
      }
    }
  }

  // 5. Draw Flow Streams
  ctx.fillStyle = palette.flow
  for (let i = 0; i < flowCount; i++) {
    flowParam[i] += 0.0012 + s * 0.002
    if (flowParam[i] > 1) {
      flowParam[i] = 0
      flowPoints[i].x = (Math.random() - 0.5) * 200
      flowPoints[i].z = (Math.random() - 0.5) * 200
    }
    const pPercent = flowParam[i]
    const angle = pPercent * Math.PI * 2 * 3
    const radius = 30 + pPercent * 80
    const fx = Math.cos(angle) * radius
    const fz = Math.sin(angle) * radius
    const h = Math.sin(fx * 0.12 + animTime * 0.8) * 2.4 +
              Math.cos(fz * 0.14 + animTime * 0.6) * 2.0 +
              Math.sin((fx + fz) * 0.08 + animTime * 0.5) * 1.6
    const fy = -16 + h * (1.6 + s * 4.0)

    const transformedPt = transformWorld([fx, fy, fz], s)
    const proj = project(transformedPt[0], transformedPt[1], transformedPt[2], width, height)
    if (proj) {
      ctx.beginPath()
      ctx.arc(proj.x, proj.y, 2.5 * (100 / proj.depth), 0, Math.PI * 2)
      ctx.fill()
    }
  }

  // 6. Draw Core Atom Nucleus Cluster
  drawNucleus(ctx, s, isDark)

  // 7. Draw 3 tilted Orbit Tracks and Orbiting Electrons
  // Electron 1 Position
  const eAngle1 = animTime * 1.8
  drawElectronOrbit(ctx, 23, 20, Math.PI / 4.5, Math.PI / 6, eAngle1, s, palette.ring1, isDark ? '#ffffff' : '#0066cc', isDark)

  // Electron 2 Position
  const eAngle2 = -animTime * 2.2 + Math.PI / 3
  drawElectronOrbit(ctx, 29, 24, -Math.PI / 4, Math.PI / 3.5, eAngle2, s, palette.ring2, isDark ? '#ffffff' : '#4a90e2', isDark)

  // Electron 3 Position
  const eAngle3 = animTime * 1.4 + Math.PI * 0.7
  drawElectronOrbit(ctx, 35, 28, Math.PI / 3.2, -Math.PI / 4.5, eAngle3, s, palette.ring3, isDark ? '#ffffff' : '#6d7bd8', isDark)
}

onMounted(() => {
  resize()
  resizeObserver = new ResizeObserver(resize)
  if (containerRef.value) {
    resizeObserver.observe(containerRef.value)
  }

  gsapCtx = gsap.context(() => {
    camera.x = 0
    camera.y = 0
    camera.z = 70
    camera.rz = 0

    camXTo = gsap.quickTo(camera, 'x', { duration: 0.8, ease: 'power2.out' })
    camYTo = gsap.quickTo(camera, 'y', { duration: 0.8, ease: 'power2.out' })
    camZTo = gsap.quickTo(camera, 'z', { duration: 0.8, ease: 'power2.out' })
    camRzTo = gsap.quickTo(camera, 'rz', { duration: 0.8, ease: 'power2.out' })

    gsap.ticker.add(render)
  })

  const scrollTarget = props.scrollEl || window
  scrollTarget.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('pointermove', handlePointer, { passive: true })
})

onBeforeUnmount(() => {
  killed = true
  gsap.ticker.remove(render)
  try {
    gsapCtx?.revert()
  } catch {
    // gsapCtx may be in an invalid state during mid-frame teardown
  }

  if (canvasCtx && canvasRef.value) {
    canvasRef.value.width = 0
    canvasRef.value.height = 0
    canvasCtx = null
  }

  resizeObserver?.disconnect()
  const scrollTarget = props.scrollEl || window
  scrollTarget.removeEventListener('scroll', handleScroll)
  window.removeEventListener('pointermove', handlePointer)
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

.onboard-scene__canvas {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
