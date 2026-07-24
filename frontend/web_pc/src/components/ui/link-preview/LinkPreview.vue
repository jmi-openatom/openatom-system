<script setup lang="ts">
import type { CSSProperties } from 'vue'
import { computed, nextTick, onBeforeUnmount, ref, useAttrs } from 'vue'
import { cn } from '@inspira-ui/plugins'

defineOptions({ inheritAttrs: false })

interface Props {
  class?: string
  linkClass?: string
  width?: number
  height?: number
  isStatic?: boolean
  imageSrc?: string
  url?: string
}

const props = withDefaults(defineProps<Props>(), {
  class: '',
  linkClass: '',
  width: 200,
  height: 125,
  isStatic: false,
  imageSrc: '',
  url: '',
})
const attrs = useAttrs()

const isVisible = ref(false)
const hasPopped = ref(false)
const trigger = ref<HTMLAnchorElement | null>(null)
const left = ref(0)
const top = ref(0)
let popTimer: number | undefined

const previewSrc = computed(() => {
  if (props.isStatic) return props.imageSrc

  const params = new URLSearchParams({
    url: props.url,
    screenshot: 'true',
    meta: 'false',
    embed: 'screenshot.url',
    colorScheme: 'light',
    'viewport.isMobile': 'false',
    'viewport.deviceScaleFactor': '1',
    'viewport.width': String(props.width * 3),
    'viewport.height': String(props.height * 3),
  })

  return `https://api.microlink.io/?${params.toString()}`
})

const previewStyle = computed<CSSProperties>(() => ({
  left: `${left.value}px`,
  top: `${top.value}px`,
  width: `${props.width}px`,
  height: `${props.height}px`,
}))

function updatePosition(pointerX?: number) {
  if (!trigger.value) return

  const rect = trigger.value.getBoundingClientRect()
  const viewportPadding = 12
  const gap = 14
  const centeredLeft = (pointerX ?? rect.left + rect.width / 2) - props.width / 2
  const maxLeft = Math.max(viewportPadding, window.innerWidth - props.width - viewportPadding)

  left.value = Math.min(Math.max(viewportPadding, centeredLeft), maxLeft)
  const desiredTop =
    rect.top >= props.height + gap + viewportPadding
      ? rect.top - props.height - gap
      : Math.min(rect.bottom + gap, window.innerHeight - props.height - viewportPadding)
  top.value = Math.max(viewportPadding, desiredTop)
}

async function showPreview(pointerX?: number) {
  window.clearTimeout(popTimer)
  isVisible.value = true
  await nextTick()
  updatePosition(pointerX)
  popTimer = window.setTimeout(() => {
    hasPopped.value = true
  }, 30)
}

function hidePreview() {
  window.clearTimeout(popTimer)
  isVisible.value = false
  hasPopped.value = false
}

function handleMouseMove(event: MouseEvent) {
  if (isVisible.value) updatePosition(event.clientX)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    hidePreview()
    trigger.value?.blur()
  }
}

onBeforeUnmount(() => window.clearTimeout(popTimer))
</script>

<template>
  <span :class="cn('link-preview', props.class)">
    <a
      ref="trigger"
      v-bind="attrs"
      :class="cn('link-preview__trigger', props.linkClass)"
      :href="url"
      @blur="hidePreview"
      @focus="showPreview()"
      @keydown="handleKeydown"
      @mouseenter="showPreview()"
      @mouseleave="hidePreview"
      @mousemove="handleMouseMove"
    >
      <slot />
    </a>

    <Teleport to="body">
      <div
        v-if="isVisible && previewSrc"
        class="link-preview__floating"
        :class="{ 'is-visible': hasPopped }"
        :style="previewStyle"
        aria-hidden="true"
      >
        <img
          :alt="`网站预览：${url}`"
          class="link-preview__image"
          :height="height"
          :src="previewSrc"
          :width="width"
        />
      </div>
    </Teleport>
  </span>
</template>

<style scoped>
.link-preview {
  display: inline-flex;
}

.link-preview__trigger {
  color: inherit;
}

.link-preview__floating {
  position: fixed;
  z-index: 1000;
  overflow: hidden;
  padding: 4px;
  border: 1px solid color-mix(in srgb, var(--oa-border, #d2d2d7) 88%, transparent);
  border-radius: 14px;
  background: var(--oa-elevated-bg, #fff);
  box-shadow: 0 18px 48px rgb(0 0 0 / 18%);
  opacity: 0;
  pointer-events: none;
  transform: translateY(6px) scale(0.94);
  transform-origin: center bottom;
  transition:
    opacity 180ms ease-out,
    transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.link-preview__floating.is-visible {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.link-preview__image {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 10px;
  background: var(--oa-page-soft-bg, #f5f5f7);
  object-fit: cover;
}

@media (hover: none), (pointer: coarse) {
  .link-preview__floating {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .link-preview__floating {
    transform: none;
    transition: none;
  }
}
</style>
