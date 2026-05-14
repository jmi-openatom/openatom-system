<script lang="ts" setup>
import type { Component } from 'vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import DefaultCursor from './DefaultCursor.vue'

interface Position {
  x: number
  y: number
}

interface SmoothCursorProps {
  cursor?: Component
  ease?: number
}

const props = withDefaults(defineProps<SmoothCursorProps>(), {
  cursor: () => DefaultCursor,
  ease: 0.22,
})

const visible = ref(true)
const target = ref<Position>({ x: 0, y: 0 })
const current = ref<Position>({ x: 0, y: 0 })
const rotation = ref(0)
const scale = ref(1)
let animationFrameId = 0
let hideTimer: number | undefined
let previousBodyCursor = ''

const cursorStyle = computed(() => ({
  opacity: visible.value ? 1 : 0,
  transform: `translate3d(${current.value.x}px, ${current.value.y}px, 0) translate(-50%, -50%) rotate(${rotation.value}deg) scale(${scale.value})`,
}))

function updateTarget(event: MouseEvent) {
  const next = { x: event.clientX, y: event.clientY }
  const dx = next.x - target.value.x
  const dy = next.y - target.value.y

  if (Math.abs(dx) + Math.abs(dy) > 0.5) {
    rotation.value = Math.atan2(dy, dx) * (180 / Math.PI) + 90
    scale.value = 0.94
    if (hideTimer) window.clearTimeout(hideTimer)
    hideTimer = window.setTimeout(() => {
      scale.value = 1
    }, 120)
  }

  target.value = next
  visible.value = true
}

function animate() {
  current.value = {
    x: current.value.x + (target.value.x - current.value.x) * props.ease,
    y: current.value.y + (target.value.y - current.value.y) * props.ease,
  }
  animationFrameId = requestAnimationFrame(animate)
}

function hideCursor() {
  visible.value = true
}

onMounted(() => {
  previousBodyCursor = document.body.style.cursor
  document.body.style.cursor = 'none'

  const startX = window.innerWidth / 2
  const startY = window.innerHeight / 2
  target.value = { x: startX, y: startY }
  current.value = { x: startX, y: startY }

  document.addEventListener('mousemove', updateTarget, { passive: true })
  window.addEventListener('mouseleave', hideCursor)
  animationFrameId = requestAnimationFrame(animate)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', updateTarget)
  window.removeEventListener('mouseleave', hideCursor)
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  if (hideTimer) window.clearTimeout(hideTimer)
  document.body.style.cursor = previousBodyCursor
})
</script>

<template>
  <div class="smooth-cursor" :style="cursorStyle">
    <div class="smooth-cursor__halo"></div>
    <component :is="props.cursor" />
  </div>
</template>

<style scoped>
.smooth-cursor {
  position: fixed;
  left: 0;
  top: 0;
  z-index: 2147483647;
  width: 34px;
  height: 34px;
  color: #111111;
  pointer-events: none;
  transition:
    opacity 0.16s ease,
    transform 0.08s linear;
  will-change: opacity, transform;
  filter: drop-shadow(0 1px 0 #ffffff) drop-shadow(0 0 6px rgba(255, 255, 255, 0.9));
}

.smooth-cursor :deep(svg) {
  position: relative;
  z-index: 2;
  display: block;
}

.smooth-cursor__halo {
  position: absolute;
  left: 4px;
  top: 4px;
  width: 10px;
  height: 10px;
  border: 2px solid #ffffff;
  border-radius: 999px;
  background: #111111;
  box-shadow: 0 0 0 1px rgba(17, 17, 17, 0.3);
}
</style>
