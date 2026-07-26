<script lang="ts" setup>
import { cn } from '@/lib/utils'
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'

const props = withDefaults(defineProps<Props>(), {
  morphTime: 1.5,
  coolDownTime: 0.5,
})

const TEXT_CLASSES =
  'absolute left-1/2 top-1/2 inline-block w-max -translate-x-1/2 -translate-y-1/2 whitespace-nowrap text-center'

interface Props {
  class?: string
  texts: string[]
  morphTime?: number
  coolDownTime?: number
}
const rootRef = ref<HTMLElement>()
const text1Ref = ref<HTMLSpanElement>()
const text2Ref = ref<HTMLSpanElement>()
const canAnimateMorph = ref(false)
const isMorphing = ref(false)
const staticText = computed(() => props.texts[0] || '')

let textIndex = 0
let animationFrameId = 0
let coolDownTimer: number | undefined
let scrollStopTimer: number | undefined
let morphStartedAt = 0
let lastRenderedAt = 0
let visibilityObserver: IntersectionObserver | undefined
let inViewport = true
const MORPH_FRAME_INTERVAL_MS = 1000 / 30

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

function supportsMorphFilter() {
  if (typeof CSS === 'undefined' || typeof CSS.supports !== 'function') return false
  return CSS.supports('filter', 'blur(1px)') && typeof SVGFEColorMatrixElement !== 'undefined'
}

function setStyles(fraction: number) {
  if (!text1Ref.value || !text2Ref.value) return

  text2Ref.value.style.filter = `blur(${Math.min(4.8 / fraction - 4.8, 40)}px)`
  text2Ref.value.style.opacity = `${fraction ** 0.4 * 100}%`

  const invertedFraction = 1 - fraction
  text1Ref.value.style.filter = `blur(${Math.min(4.8 / invertedFraction - 4.8, 40)}px)`
  text1Ref.value.style.opacity = `${invertedFraction ** 0.4 * 100}%`

}

function setTextContent() {
  if (!text1Ref.value || !text2Ref.value || !props.texts.length) return
  text1Ref.value.textContent = props.texts[textIndex % props.texts.length]
  text2Ref.value.textContent = props.texts[(textIndex + 1) % props.texts.length]
}

function renderRestingFrame() {
  setTextContent()
  if (text1Ref.value && text2Ref.value) {
    text1Ref.value.style.filter = 'none'
    text1Ref.value.style.opacity = '100%'
    text2Ref.value.style.filter = 'none'
    text2Ref.value.style.opacity = '0%'
  }
}

function canRunAnimation() {
  return canAnimateMorph.value && inViewport && !document.hidden
}

function scheduleMorph() {
  if (!canRunAnimation() || coolDownTimer) return
  coolDownTimer = window.setTimeout(() => {
    coolDownTimer = undefined
    startMorph()
  }, props.coolDownTime * 1000)
}

function animateMorph(now: number) {
  if (!canRunAnimation()) {
    stopAnimation()
    return
  }

  const fraction = Math.min((now - morphStartedAt) / (props.morphTime * 1000), 1)
  if (fraction < 1 && now - lastRenderedAt < MORPH_FRAME_INTERVAL_MS) {
    animationFrameId = window.requestAnimationFrame(animateMorph)
    return
  }
  lastRenderedAt = now
  setStyles(fraction)

  if (fraction < 1) {
    animationFrameId = window.requestAnimationFrame(animateMorph)
    return
  }

  animationFrameId = 0
  isMorphing.value = false
  textIndex += 1
  renderRestingFrame()
  scheduleMorph()
}

function startMorph() {
  if (!canRunAnimation() || animationFrameId) return
  setTextContent()
  isMorphing.value = true
  morphStartedAt = performance.now()
  lastRenderedAt = 0
  animationFrameId = window.requestAnimationFrame(animateMorph)
}

function startAnimation() {
  if (!canRunAnimation()) return
  renderRestingFrame()
  scheduleMorph()
}

function stopAnimation() {
  if (animationFrameId) window.cancelAnimationFrame(animationFrameId)
  if (coolDownTimer) window.clearTimeout(coolDownTimer)
  animationFrameId = 0
  coolDownTimer = undefined
  isMorphing.value = false
}

function handleVisibilityChange() {
  stopAnimation()
  if (!document.hidden && inViewport) startAnimation()
}

function handleScroll() {
  if (!inViewport) return
  stopAnimation()
  renderRestingFrame()
  if (scrollStopTimer) window.clearTimeout(scrollStopTimer)
  scrollStopTimer = window.setTimeout(() => {
    scrollStopTimer = undefined
    startAnimation()
  }, 180)
}

onMounted(async () => {
  canAnimateMorph.value = supportsMorphFilter() && !prefersReducedMotion()
  await nextTick()

  if (text1Ref.value && text2Ref.value) {
    renderRestingFrame()
  }

  if (canAnimateMorph.value) {
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('scroll', handleScroll, { passive: true })
    if ('IntersectionObserver' in window && rootRef.value) {
      visibilityObserver = new IntersectionObserver((entries) => {
        inViewport = entries.some((entry) => entry.isIntersecting)
        handleVisibilityChange()
      })
      visibilityObserver.observe(rootRef.value)
    } else {
      startAnimation()
    }
  }
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('scroll', handleScroll)
  if (scrollStopTimer) window.clearTimeout(scrollStopTimer)
  visibilityObserver?.disconnect()
  stopAnimation()
})
</script>

<template>
  <div
    ref="rootRef"
    :class="
      cn(
        `relative mx-auto flex h-16 w-full items-center justify-center overflow-visible text-center font-sans text-[40pt] leading-none font-bold whitespace-nowrap md:h-24 lg:text-[6rem]`,
        isMorphing ? 'filter-[url(#threshold)_blur(0.6px)]' : '',
        props.class,
      )
    "
  >
    <template v-if="canAnimateMorph">
      <span ref="text1Ref" :class="[TEXT_CLASSES]" />
      <span ref="text2Ref" :class="[TEXT_CLASSES]" />
    </template>
    <span v-else :class="[TEXT_CLASSES]">{{ staticText }}</span>

    <svg id="filters" class="fixed size-0" preserveAspectRatio="xMidYMid slice">
      <defs>
        <filter id="threshold">
          <feColorMatrix
            in="SourceGraphic"
            type="matrix"
            values="1 0 0 0 0
                  0 1 0 0 0
                  0 0 1 0 0
                  0 0 0 255 -140"
          />
        </filter>
      </defs>
    </svg>
  </div>
</template>
