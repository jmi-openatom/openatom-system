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
const staticText = computed(() => props.texts[0] || '')

let textIndex = 0
let morph = 0
let coolDown = props.coolDownTime
let lastTime = 0
let visibilityObserver: IntersectionObserver | undefined
let inViewport = true

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

  text1Ref.value.textContent = props.texts[textIndex % props.texts.length]
  text2Ref.value.textContent = props.texts[(textIndex + 1) % props.texts.length]
}

function doMorph() {
  morph -= coolDown
  coolDown = 0

  let fraction = morph / props.morphTime

  if (fraction > 1) {
    coolDown = props.coolDownTime
    fraction = 1
  }

  setStyles(fraction)

  if (fraction === 1) {
    textIndex++
  }
}

function doCoolDown() {
  morph = 0

  if (text1Ref.value && text2Ref.value) {
    text2Ref.value.style.filter = 'none'
    text2Ref.value.style.opacity = '100%'
    text1Ref.value.style.filter = 'none'
    text1Ref.value.style.opacity = '0%'
  }
}

let animationFrameId: number = 0
function animate(now: number) {
  animationFrameId = requestAnimationFrame(animate)

  const dt = Math.min((now - lastTime) / 1000, 0.1)
  lastTime = now

  coolDown -= dt

  if (coolDown <= 0) {
    doMorph()
  } else {
    doCoolDown()
  }
}

function startAnimation() {
  if (animationFrameId || !canAnimateMorph.value) return
  lastTime = performance.now()
  animationFrameId = requestAnimationFrame(animate)
}

function stopAnimation() {
  if (!animationFrameId) return
  cancelAnimationFrame(animationFrameId)
  animationFrameId = 0
}

function handleVisibilityChange() {
  if (!document.hidden && inViewport) startAnimation()
  else stopAnimation()
}

onMounted(async () => {
  canAnimateMorph.value = supportsMorphFilter() && !prefersReducedMotion()
  await nextTick()

  if (text1Ref.value && text2Ref.value) {
    text1Ref.value.textContent = props.texts[0] || ''
    text2Ref.value.textContent = props.texts[0] || ''
  }

  if (canAnimateMorph.value) {
    document.addEventListener('visibilitychange', handleVisibilityChange)
    if ('IntersectionObserver' in window && rootRef.value) {
      visibilityObserver = new IntersectionObserver((entries) => {
        inViewport = entries.some((entry) => entry.isIntersecting)
        if (inViewport && !document.hidden) startAnimation()
        else stopAnimation()
      })
      visibilityObserver.observe(rootRef.value)
    } else {
      startAnimation()
    }
  }
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
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
        canAnimateMorph ? 'filter-[url(#threshold)_blur(0.6px)]' : '',
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
