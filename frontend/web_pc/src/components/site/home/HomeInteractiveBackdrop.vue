<template>
  <canvas ref="canvasRef" aria-hidden="true" class="home-section-canvas"></canvas>
</template>

<script setup lang="ts">
import gsap from 'gsap'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useTheme } from '@/composables/useTheme'

const props = withDefaults(
  defineProps<{
    spacing?: number
    radius?: number
    strength?: number
  }>(),
  {
    spacing: 58,
    radius: 180,
    strength: 18,
  },
)

const canvasRef = ref<HTMLCanvasElement>()
const { resolvedTheme } = useTheme()

let host: HTMLElement | null = null
let ctx: CanvasRenderingContext2D | null = null
let resizeObserver: ResizeObserver | null = null
let handleWindowResize: (() => void) | null = null
let animationFrame = 0
let width = 0
let height = 0
let dpr = 1

const pointer = {
  x: 0,
  y: 0,
  active: 0,
}

const mouseXTo = gsap.quickTo(pointer, 'x', {
  duration: 0.42,
  ease: 'power3.out',
})
const mouseYTo = gsap.quickTo(pointer, 'y', {
  duration: 0.42,
  ease: 'power3.out',
})
const activeTo = gsap.quickTo(pointer, 'active', {
  duration: 0.26,
  ease: 'power2.out',
})

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

function colors() {
  if (resolvedTheme.value === 'dark') {
    return {
      dot: 'rgba(228, 228, 231, 0.16)',
      dotActive: 'rgba(245, 245, 247, 0.62)',
      line: 'rgba(228, 228, 231, 0.18)',
      ring: 'rgba(245, 245, 247, 0.28)',
    }
  }

  return {
    dot: 'rgba(15, 23, 42, 0.13)',
    dotActive: 'rgba(15, 23, 42, 0.56)',
    line: 'rgba(15, 23, 42, 0.16)',
    ring: 'rgba(15, 23, 42, 0.24)',
  }
}

function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas || !host) return

  const rect = host.getBoundingClientRect()
  width = Math.max(rect.width, 1)
  height = Math.max(rect.height, 1)
  dpr = Math.min(window.devicePixelRatio || 1, 2)

  canvas.width = Math.round(width * dpr)
  canvas.height = Math.round(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  ctx = canvas.getContext('2d')
  ctx?.setTransform(dpr, 0, 0, dpr, 0, 0)

  if (!pointer.x && !pointer.y) {
    pointer.x = width / 2
    pointer.y = height / 2
  }
}

function draw() {
  if (!ctx) return

  const palette = colors()
  const spacing = props.spacing
  const radius = props.radius
  const strength = props.strength
  const active = pointer.active
  const columns = Math.ceil(width / spacing) + 2
  const rows = Math.ceil(height / spacing) + 2

  ctx.clearRect(0, 0, width, height)

  for (let row = -1; row < rows; row += 1) {
    for (let column = -1; column < columns; column += 1) {
      const baseX = column * spacing + (row % 2 === 0 ? 0 : spacing / 2)
      const baseY = row * spacing
      const dx = baseX - pointer.x
      const dy = baseY - pointer.y
      const distance = Math.hypot(dx, dy)
      const influence = Math.max(0, 1 - distance / radius) * active
      const safeDistance = distance || 1
      const x = baseX + (dx / safeDistance) * influence * strength
      const y = baseY + (dy / safeDistance) * influence * strength
      const dotSize = 1.1 + influence * 1.7

      if (influence > 0.08) {
        ctx.beginPath()
        ctx.moveTo(x, y)
        ctx.lineTo(pointer.x, pointer.y)
        ctx.strokeStyle = palette.line
        ctx.globalAlpha = influence * 0.7
        ctx.lineWidth = 1
        ctx.stroke()
      }

      ctx.beginPath()
      ctx.arc(x, y, dotSize, 0, Math.PI * 2)
      ctx.fillStyle = influence > 0.08 ? palette.dotActive : palette.dot
      ctx.globalAlpha = 1
      ctx.fill()
    }
  }

  if (active > 0.02) {
    ctx.beginPath()
    ctx.arc(pointer.x, pointer.y, 26 + active * 8, 0, Math.PI * 2)
    ctx.strokeStyle = palette.ring
    ctx.lineWidth = 1
    ctx.globalAlpha = active
    ctx.stroke()
  }

  ctx.globalAlpha = 1
  animationFrame = window.requestAnimationFrame(draw)
}

function handlePointerMove(event: PointerEvent) {
  if (!host) return
  const rect = host.getBoundingClientRect()
  mouseXTo(event.clientX - rect.left)
  mouseYTo(event.clientY - rect.top)
  activeTo(1)
}

function handlePointerLeave() {
  activeTo(0)
}

onMounted(() => {
  const canvas = canvasRef.value
  host = canvas?.parentElement || null
  if (!canvas || !host) return

  resizeCanvas()
  if ('ResizeObserver' in window) {
    resizeObserver = new ResizeObserver(resizeCanvas)
    resizeObserver.observe(host)
  } else {
    handleWindowResize = resizeCanvas
    window.addEventListener('resize', handleWindowResize)
  }
  host.addEventListener('pointermove', handlePointerMove)
  host.addEventListener('pointerleave', handlePointerLeave)

  if (prefersReducedMotion()) {
    draw()
    if (animationFrame) {
      window.cancelAnimationFrame(animationFrame)
      animationFrame = 0
    }
    return
  }

  animationFrame = window.requestAnimationFrame(draw)
})

watch(resolvedTheme, () => {
  resizeCanvas()
})

onBeforeUnmount(() => {
  if (animationFrame) window.cancelAnimationFrame(animationFrame)
  resizeObserver?.disconnect()
  if (handleWindowResize) {
    window.removeEventListener('resize', handleWindowResize)
  }
  host?.removeEventListener('pointermove', handlePointerMove)
  host?.removeEventListener('pointerleave', handlePointerLeave)
})
</script>
