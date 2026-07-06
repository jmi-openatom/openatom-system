<template>
  <canvas ref="canvasRef" aria-hidden="true" class="hero-field"></canvas>
</template>

<script setup lang="ts">
import gsap from 'gsap'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useTheme } from '@/composables/useTheme'

const canvasRef = ref<HTMLCanvasElement>()
const { resolvedTheme } = useTheme()

let ctx: CanvasRenderingContext2D | null = null
let host: HTMLElement | null = null
let resizeObserver: ResizeObserver | null = null
let frame = 0
let width = 0
let height = 0
let dpr = 1

const pointer = { x: 0, y: 0, tx: 0, ty: 0 }
let xTo: gsap.QuickToFunc | undefined
let yTo: gsap.QuickToFunc | undefined
let gsapCtx: gsap.Context | null = null

const routes = [
  [0.08, 0.74, 0.22, 0.56, 0.34, 0.62, 0.48, 0.38, 0.62, 0.44, 0.78, 0.22],
  [0.04, 0.28, 0.18, 0.34, 0.28, 0.18, 0.46, 0.24, 0.58, 0.1, 0.86, 0.18],
  [0.12, 0.9, 0.26, 0.78, 0.42, 0.84, 0.58, 0.66, 0.76, 0.72, 0.94, 0.58],
  [0.3, 0.06, 0.38, 0.24, 0.54, 0.18, 0.66, 0.32, 0.82, 0.26, 0.96, 0.38],
]

const nodes = [
  [0.18, 0.34],
  [0.28, 0.18],
  [0.34, 0.62],
  [0.42, 0.84],
  [0.48, 0.38],
  [0.58, 0.66],
  [0.62, 0.44],
  [0.78, 0.22],
  [0.82, 0.26],
]

function palette() {
  if (resolvedTheme.value === 'dark') {
    return {
      bg: '#050505',
      route: 'rgba(245,245,247,0.13)',
      routeActive: 'rgba(245,245,247,0.34)',
      node: 'rgba(245,245,247,0.42)',
      grid: 'rgba(245,245,247,0.05)',
      ring: 'rgba(245,245,247,0.18)',
      glow: 'rgba(245,245,247,0.08)',
    }
  }
  return {
    bg: '#f7f8fb',
    route: 'rgba(15,23,42,0.11)',
    routeActive: 'rgba(15,23,42,0.24)',
    node: 'rgba(15,23,42,0.38)',
    grid: 'rgba(15,23,42,0.045)',
    ring: 'rgba(15,23,42,0.14)',
    glow: 'rgba(15,23,42,0.06)',
  }
}

function resize() {
  if (!host || !canvasRef.value) return
  const rect = host.getBoundingClientRect()
  width = Math.max(rect.width, 1)
  height = Math.max(rect.height, 1)
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvasRef.value.width = Math.round(width * dpr)
  canvasRef.value.height = Math.round(height * dpr)
  canvasRef.value.style.width = `${width}px`
  canvasRef.value.style.height = `${height}px`
  ctx = canvasRef.value.getContext('2d')
  ctx?.setTransform(dpr, 0, 0, dpr, 0, 0)
  pointer.x = pointer.tx = width / 2
  pointer.y = pointer.ty = height / 2
}

function drawGrid(p: ReturnType<typeof palette>) {
  if (!ctx) return
  const step = 72
  ctx.strokeStyle = p.grid
  ctx.lineWidth = 1
  for (let x = 0; x <= width; x += step) {
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, height)
    ctx.stroke()
  }
  for (let y = 0; y <= height; y += step) {
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(width, y)
    ctx.stroke()
  }
}

function drawRoutes(p: ReturnType<typeof palette>) {
  if (!ctx) return
  routes.forEach((route, index) => {
    ctx.beginPath()
    ctx.moveTo(route[0] * width, route[1] * height)
    for (let i = 2; i < route.length; i += 4) {
      ctx.bezierCurveTo(
        route[i] * width,
        route[i + 1] * height,
        route[i + 2] * width,
        route[i + 3] * height,
        route[Math.min(i + 4, route.length - 2)] * width,
        route[Math.min(i + 5, route.length - 1)] * height,
      )
    }
    ctx.strokeStyle = index === 0 ? p.routeActive : p.route
    ctx.lineWidth = index === 0 ? 1.4 : 1
    ctx.stroke()
  })
}

function drawNodes(p: ReturnType<typeof palette>) {
  if (!ctx) return
  nodes.forEach(([nx, ny], index) => {
    const x = nx * width
    const y = ny * height
    ctx.beginPath()
    ctx.arc(x, y, index === 4 ? 4 : 2.2, 0, Math.PI * 2)
    ctx.fillStyle = p.node
    ctx.fill()
  })
}

function drawFocus(p: ReturnType<typeof palette>) {
  if (!ctx) return
  const cx = width * 0.48 + (pointer.tx - width / 2) * 0.035
  const cy = height * 0.38 + (pointer.ty - height / 2) * 0.035

  ctx.beginPath()
  ctx.arc(cx, cy, 84, 0, Math.PI * 2)
  ctx.strokeStyle = p.ring
  ctx.lineWidth = 1
  ctx.stroke()

  ctx.beginPath()
  ctx.arc(cx, cy, 14, 0, Math.PI * 2)
  ctx.strokeStyle = p.routeActive
  ctx.stroke()

  const glow = ctx.createRadialGradient(cx, cy, 0, cx, cy, 180)
  glow.addColorStop(0, p.glow)
  glow.addColorStop(1, 'transparent')
  ctx.fillStyle = glow
  ctx.beginPath()
  ctx.arc(cx, cy, 180, 0, Math.PI * 2)
  ctx.fill()
}

function render() {
  if (!ctx) return
  pointer.x += (pointer.tx - pointer.x) * 0.08
  pointer.y += (pointer.ty - pointer.y) * 0.08
  const p = palette()
  ctx.clearRect(0, 0, width, height)
  ctx.fillStyle = p.bg
  ctx.fillRect(0, 0, width, height)
  drawGrid(p)
  drawRoutes(p)
  drawFocus(p)
  drawNodes(p)
  frame = window.requestAnimationFrame(render)
}

function handlePointerMove(event: PointerEvent) {
  if (!host) return
  const rect = host.getBoundingClientRect()
  xTo?.(event.clientX - rect.left)
  yTo?.(event.clientY - rect.top)
}

onMounted(() => {
  host = canvasRef.value?.parentElement || null
  if (!host) return

  gsapCtx = gsap.context(() => {
    xTo = gsap.quickTo(pointer, 'tx', { duration: 0.5, ease: 'power3.out' })
    yTo = gsap.quickTo(pointer, 'ty', { duration: 0.5, ease: 'power3.out' })
  })

  resize()
  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(host)
  host.addEventListener('pointermove', handlePointerMove)
  frame = window.requestAnimationFrame(render)
})

watch(resolvedTheme, resize)

onBeforeUnmount(() => {
  if (frame) window.cancelAnimationFrame(frame)
  resizeObserver?.disconnect()
  host?.removeEventListener('pointermove', handlePointerMove)
  gsapCtx?.revert()
})
</script>

<style scoped>
.hero-field {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
</style>
