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
    trackWindow?: boolean
  }>(),
  {
    spacing: 58,
    radius: 180,
    strength: 18,
    trackWindow: false,
  },
)

const canvasRef = ref<HTMLCanvasElement>()
const { resolvedTheme } = useTheme()

let host: HTMLElement | null = null
let ctx: CanvasRenderingContext2D | null = null
let resizeObserver: ResizeObserver | null = null
let visibilityObserver: IntersectionObserver | null = null
let handleWindowResize: (() => void) | null = null
let animationFrame = 0
let drawing = false
let isVisible = false
let staticBackdrop = false
let width = 0
let height = 0
let dpr = 1

const pointer = {
  x: 0,
  y: 0,
  active: 0,
}

let mouseXTo: gsap.QuickToFunc | undefined
let mouseYTo: gsap.QuickToFunc | undefined
let activeTo: gsap.QuickToFunc | undefined
let gsapCtx: gsap.Context | null = null

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

function shouldUseStaticBackdrop() {
  const connection = (navigator as any).connection
  const saveData = Boolean(connection?.saveData)
  const lowMemory =
    typeof (navigator as any).deviceMemory === 'number' && (navigator as any).deviceMemory <= 4
  const lowCpu =
    typeof navigator.hardwareConcurrency === 'number' && navigator.hardwareConcurrency <= 4
  return prefersReducedMotion() || saveData || lowMemory || lowCpu
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
    dot: 'rgba(29, 29, 31, 0.13)',
    dotActive: 'rgba(29, 29, 31, 0.56)',
    line: 'rgba(29, 29, 31, 0.16)',
    ring: 'rgba(29, 29, 31, 0.24)',
  }
}

function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas || !host) return

  const rect = host.getBoundingClientRect()
  width = Math.max(rect.width, 1)
  height = Math.max(rect.height, 1)
  // These canvases are decorative and there can be several on the home page.
  // A lower DPR keeps their backing stores from consuming tens of MB each.
  dpr = Math.min(window.devicePixelRatio || 1, 1.25)

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

function releaseCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  ctx = null
  canvas.width = 1
  canvas.height = 1
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
  if (drawing) {
    animationFrame = window.requestAnimationFrame(draw)
  }
}

function startDrawing() {
  if (drawing) return
  drawing = true
  animationFrame = window.requestAnimationFrame(draw)
}

function stopDrawing() {
  drawing = false
  if (animationFrame) {
    window.cancelAnimationFrame(animationFrame)
    animationFrame = 0
  }
}

function handlePointerMove(event: PointerEvent) {
  if (!host) return
  const rect = host.getBoundingClientRect()
  mouseXTo?.(event.clientX - rect.left)
  mouseYTo?.(event.clientY - rect.top)
  activeTo?.(1)
}

function handlePointerLeave() {
  activeTo?.(0)
}

onMounted(() => {
  const canvas = canvasRef.value
  host = canvas?.parentElement || null
  if (!canvas || !host) return

  staticBackdrop = shouldUseStaticBackdrop()
  if (!staticBackdrop) {
    gsapCtx = gsap.context(() => {
      mouseXTo = gsap.quickTo(pointer, 'x', {
        duration: 0.42,
        ease: 'power3.out',
      })
      mouseYTo = gsap.quickTo(pointer, 'y', {
        duration: 0.42,
        ease: 'power3.out',
      })
      activeTo = gsap.quickTo(pointer, 'active', {
        duration: 0.26,
        ease: 'power2.out',
      })
    })
  }

  if ('ResizeObserver' in window) {
    resizeObserver = new ResizeObserver(() => {
      if (!isVisible) return
      resizeCanvas()
      if (staticBackdrop) draw()
    })
    resizeObserver.observe(host)
  } else {
    handleWindowResize = () => {
      if (!isVisible) return
      resizeCanvas()
      if (staticBackdrop) draw()
    }
    window.addEventListener('resize', handleWindowResize)
  }

  if (!staticBackdrop) {
    if (props.trackWindow) {
      window.addEventListener('pointermove', handlePointerMove, { passive: true })
      window.addEventListener('pointerleave', handlePointerLeave)
    } else {
      host.addEventListener('pointermove', handlePointerMove)
      host.addEventListener('pointerleave', handlePointerLeave)
    }
  }

  if ('IntersectionObserver' in window) {
    visibilityObserver = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          isVisible = true
          resizeCanvas()
          if (staticBackdrop) draw()
          else startDrawing()
        } else {
          isVisible = false
          stopDrawing()
          releaseCanvas()
        }
      },
      { rootMargin: '80px 0px', threshold: 0.01 },
    )
    visibilityObserver.observe(host)
  } else {
    isVisible = true
    resizeCanvas()
    if (staticBackdrop) draw()
    else startDrawing()
  }
})

watch(resolvedTheme, () => {
  if (!isVisible) return
  resizeCanvas()
  if (!drawing) draw()
})

onBeforeUnmount(() => {
  stopDrawing()
  releaseCanvas()
  resizeObserver?.disconnect()
  visibilityObserver?.disconnect()
  if (handleWindowResize) {
    window.removeEventListener('resize', handleWindowResize)
  }
  if (props.trackWindow) {
    window.removeEventListener('pointermove', handlePointerMove)
    window.removeEventListener('pointerleave', handlePointerLeave)
  } else {
    host?.removeEventListener('pointermove', handlePointerMove)
    host?.removeEventListener('pointerleave', handlePointerLeave)
  }
  gsapCtx?.revert()
})
</script>
