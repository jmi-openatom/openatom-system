<script lang="ts" setup>
import { cn } from '@/lib/utils'
import { computed, onMounted, onUnmounted, ref } from 'vue'

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
const textIndex = ref(0)
const morph = ref(0)
const coolDown = ref(props.coolDownTime)
const time = ref(new Date())

const text1Ref = ref<HTMLSpanElement>()
const text2Ref = ref<HTMLSpanElement>()
const canAnimateMorph = ref(false)
const staticText = computed(() => props.texts[0] || '')

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

  text1Ref.value.textContent = props.texts[textIndex.value % props.texts.length]
  text2Ref.value.textContent = props.texts[(textIndex.value + 1) % props.texts.length]
}

function doMorph() {
  morph.value -= coolDown.value
  coolDown.value = 0

  let fraction = morph.value / props.morphTime

  if (fraction > 1) {
    coolDown.value = props.coolDownTime
    fraction = 1
  }

  setStyles(fraction)

  if (fraction === 1) {
    textIndex.value++
  }
}

function doCoolDown() {
  morph.value = 0

  if (text1Ref.value && text2Ref.value) {
    text2Ref.value.style.filter = 'none'
    text2Ref.value.style.opacity = '100%'
    text1Ref.value.style.filter = 'none'
    text1Ref.value.style.opacity = '0%'
  }
}

let animationFrameId: number = 0
function animate() {
  animationFrameId = requestAnimationFrame(animate)

  const newTime = new Date()
  const dt = (newTime.getTime() - time.value.getTime()) / 1000
  time.value = newTime

  coolDown.value -= dt

  if (coolDown.value <= 0) {
    doMorph()
  } else {
    doCoolDown()
  }
}

onMounted(() => {
  if (text1Ref.value && text2Ref.value) {
    text1Ref.value.textContent = props.texts[0] || ''
    text2Ref.value.textContent = props.texts[0] || ''
  }

  canAnimateMorph.value = supportsMorphFilter() && !prefersReducedMotion()
  if (canAnimateMorph.value) {
    animate()
  }
})

onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
})
</script>

<template>
  <div
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
