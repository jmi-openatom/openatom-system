<template>
  <section id="activities" class="activity-band home-interactive-section">
    <HomeInteractiveBackdrop :radius="230" :spacing="68" :strength="22" />
    <div class="activity-shell section">
      <div class="section-heading reveal-block">
        <span>近期活动</span>
        <h2>最新活动</h2>
      </div>

      <div
        v-if="activities.length"
        ref="stageRef"
        class="activity-stage"
        @pointerenter="pauseAutoPlay"
        @pointerleave="resumeAutoPlay"
        @pointermove="handlePointerMove"
      >
        <article
          v-for="(activity, index) in activities"
          :key="activity.id || activity.title"
          :class="stageClass(index)"
          :style="stageStyle(index)"
          class="activity-stage__card"
          @click="openActivity(index)"
        >
          <div class="activity-stage__media">
            <img :alt="activity.title" :src="activity.coverUrl" />
          </div>

          <div class="activity-stage__wipe"></div>
          <div class="activity-stage__veil"></div>

          <div class="activity-stage__content">
            <div class="activity-stage__meta">
              <time>{{ activity.date }}</time>
              <span>{{ formatIndex(index) }}</span>
            </div>
            <h3>{{ activity.title }}</h3>
            <p>{{ activity.description }}</p>
          </div>
        </article>

        <div class="activity-stage__controls">
          <button aria-label="上一个活动" type="button" @click="previous">
            <span>←</span>
          </button>
          <div class="activity-stage__progress">
            <button
              v-for="(activity, index) in activities"
              :key="`dot-${activity.id || activity.title}`"
              :class="{ 'is-active': index === activeIndex }"
              :aria-label="`切换到第 ${index + 1} 个活动`"
              type="button"
              @click="setActive(index)"
            ></button>
          </div>
          <button aria-label="下一个活动" type="button" @click="next">
            <span>→</span>
          </button>
        </div>
      </div>

      <el-empty v-if="!activities.length && !loading" description="暂无活动数据" />
    </div>
  </section>
</template>

<script setup lang="ts">
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'

gsap.registerPlugin(ScrollTrigger)

const props = defineProps<{
  activities: any[]
  loading: boolean
}>()

const router = useRouter()
const stageRef = ref<HTMLElement>()
const activeIndex = ref(0)

let autoPlayTimer: number | undefined
let animationContext: gsap.Context | undefined
let tiltXTo: gsap.QuickToFunc | undefined
let tiltYTo: gsap.QuickToFunc | undefined
let switching = false

const total = computed(() => props.activities.length)

function normalizeIndex(index: number) {
  if (!total.value) return 0
  return (index + total.value) % total.value
}

function relativeOffset(index: number) {
  if (!total.value) return 0

  const rawOffset = index - activeIndex.value
  const wrappedOffset =
    ((rawOffset + Math.floor(total.value / 2) + total.value) % total.value) -
    Math.floor(total.value / 2)

  return total.value === 2 && index !== activeIndex.value ? 1 : wrappedOffset
}

function stageClass(index: number) {
  const offset = relativeOffset(index)
  return {
    'is-active': offset === 0,
    'is-near': Math.abs(offset) === 1,
    'is-far': Math.abs(offset) > 1,
    'is-left': offset < 0,
    'is-right': offset > 0,
  }
}

function stageStyle(index: number) {
  const offset = relativeOffset(index)
  const absoluteOffset = Math.abs(offset)
  const x = offset * 38
  const rotate = offset * -8
  const scale = absoluteOffset === 0 ? 1 : Math.max(0.76, 0.9 - absoluteOffset * 0.06)
  const opacity = absoluteOffset > 2 ? 0 : 1
  const zIndex = 10 - absoluteOffset

  return {
    '--card-x': `${x}%`,
    '--card-rotate': `${rotate}deg`,
    '--card-scale': scale,
    '--card-opacity': opacity,
    '--card-z': zIndex,
  }
}

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

function setupStageMotion() {
  tiltXTo?.tween.kill()
  tiltYTo?.tween.kill()
  tiltXTo = undefined
  tiltYTo = undefined

  const stage = stageRef.value
  if (!stage || prefersReducedMotion()) return

  gsap.set(stage, {
    '--stage-tilt-x': '0deg',
    '--stage-tilt-y': '0deg',
  })

  tiltXTo = gsap.quickTo(stage, '--stage-tilt-x', {
    duration: 0.56,
    ease: 'power3.out',
  })
  tiltYTo = gsap.quickTo(stage, '--stage-tilt-y', {
    duration: 0.56,
    ease: 'power3.out',
  })
}

function animateSwitch(index: number) {
  const stage = stageRef.value
  const nextIndex = normalizeIndex(index)

  if (!stage || switching || nextIndex === activeIndex.value) {
    activeIndex.value = nextIndex
    return
  }

  switching = true

  const direction = relativeOffset(nextIndex) < 0 ? -1 : 1
  const currentCard = stage.querySelector<HTMLElement>('.activity-stage__card.is-active')
  const nextCard = stage.querySelectorAll<HTMLElement>('.activity-stage__card')[nextIndex]

  const timeline = gsap.timeline({
    defaults: {
      ease: 'power3.inOut',
      overwrite: 'auto',
    },
    onComplete: () => {
      gsap.set(stage.querySelectorAll('.activity-stage__card'), { clearProps: 'transform' })
      gsap.set(stage.querySelectorAll('.activity-stage__wipe'), { clearProps: 'transform' })
      gsap.set(
        stage.querySelectorAll(
          '.activity-stage__media img, .activity-stage__content, .activity-stage__content > *',
        ),
        {
          clearProps: 'transform,opacity,visibility',
        },
      )
      switching = false
    },
  })

  if (currentCard) {
    timeline
      .to(
        currentCard.querySelector('.activity-stage__content'),
        {
          y: -22,
          autoAlpha: 0,
          duration: 0.3,
        },
        0,
      )
      .to(
        currentCard.querySelector('.activity-stage__wipe'),
        {
          scaleX: 1,
          transformOrigin: 'left center',
          duration: 0.36,
        },
        0,
      )
      .to(
        currentCard,
        {
          x: -24 * direction,
          rotateY: -16 * direction,
          scale: 0.97,
          duration: 0.44,
        },
        0,
      )
      .to(
        currentCard.querySelector('.activity-stage__media img'),
        {
          scale: 1.08,
          duration: 0.44,
        },
        0,
      )
  }

  timeline.add(() => {
    activeIndex.value = nextIndex
  }, 0.2)

  if (nextCard) {
    const nextContent = nextCard.querySelector<HTMLElement>('.activity-stage__content')
    const nextMeta = nextCard.querySelector<HTMLElement>('.activity-stage__meta')
    const nextTitle = nextCard.querySelector<HTMLElement>('h3')
    const nextCopy = nextCard.querySelector<HTMLElement>('p')
    const nextImage = nextCard.querySelector<HTMLElement>('.activity-stage__media img')
    const nextPieces = [nextMeta, nextTitle, nextCopy].filter(Boolean) as HTMLElement[]

    timeline
      .set(
        nextCard,
        {
          x: 26 * direction,
          rotateY: 12 * direction,
          scale: 0.98,
        },
        0.2,
      )
      .set(nextContent, { y: 0, autoAlpha: 1 }, 0.2)
      .set(nextPieces, { y: 26, autoAlpha: 0 }, 0.2)
      .set(nextImage, { scale: 1.08 }, 0.2)
      .fromTo(
        nextCard.querySelector('.activity-stage__wipe'),
        {
          scaleX: 1,
          transformOrigin: 'right center',
        },
        {
          scaleX: 0,
          duration: 0.46,
        },
        0.22,
      )
      .to(
        nextCard,
        {
          x: 0,
          rotateY: 0,
          scale: 1,
          duration: 0.64,
          ease: 'expo.out',
        },
        0.24,
      )
      .to(
        nextImage,
        {
          scale: 1.025,
          duration: 0.78,
          ease: 'power3.out',
        },
        0.24,
      )
      .to(
        nextPieces,
        {
          y: 0,
          autoAlpha: 1,
          duration: 0.46,
          ease: 'power3.out',
          stagger: 0.065,
        },
        0.38,
      )
  }
}

function setActive(index: number) {
  animateSwitch(index)
}

function next() {
  setActive(activeIndex.value + 1)
}

function previous() {
  setActive(activeIndex.value - 1)
}

function openActivity(index: number) {
  if (index !== activeIndex.value) {
    setActive(index)
    return
  }

  const activity = props.activities[index]
  if (activity?.id) router.push(`/activities/${activity.id}`)
}

function formatIndex(index: number) {
  return `${String(index + 1).padStart(2, '0')} / ${String(total.value).padStart(2, '0')}`
}

function startAutoPlay() {
  window.clearInterval(autoPlayTimer)
  if (total.value <= 1 || prefersReducedMotion()) return
  autoPlayTimer = window.setInterval(next, 4800)
}

function pauseAutoPlay() {
  window.clearInterval(autoPlayTimer)
}

function resumeAutoPlay() {
  resetTilt()
  startAutoPlay()
}

function handlePointerMove(event: PointerEvent) {
  const stage = stageRef.value
  if (!stage || !tiltXTo || !tiltYTo || prefersReducedMotion()) return

  const rect = stage.getBoundingClientRect()
  const xRatio = (event.clientX - rect.left) / rect.width - 0.5
  const yRatio = (event.clientY - rect.top) / rect.height - 0.5
  tiltXTo(yRatio * -6.5)
  tiltYTo(xRatio * 8.5)
}

function resetTilt() {
  tiltXTo?.(0)
  tiltYTo?.(0)
}

function animateStage() {
  animationContext?.revert()
  if (!stageRef.value) return
  if (prefersReducedMotion()) {
    ScrollTrigger.refresh()
    return
  }

  animationContext = gsap.context(() => {
    const stageTimeline = gsap.timeline({
      scrollTrigger: {
        trigger: '.activity-stage',
        start: 'top 84%',
        once: true,
      },
    })

    stageTimeline
      .from('.activity-stage__card', {
        y: 72,
        autoAlpha: 0,
        scale: 0.86,
        rotateX: -9,
        transformOrigin: '50% 100%',
        duration: 0.9,
        ease: 'power4.out',
        stagger: {
          each: 0.08,
          from: 'center',
        },
      })
      .from(
        '.activity-stage__controls',
        {
          y: 20,
          autoAlpha: 0,
          duration: 0.42,
          ease: 'power3.out',
        },
        '-=0.36',
      )
      .from(
        '.activity-stage__card.is-active .activity-stage__content > *',
        {
          y: 22,
          autoAlpha: 0,
          duration: 0.46,
          ease: 'power3.out',
          stagger: 0.06,
        },
        '-=0.42',
      )
  }, stageRef.value)

  ScrollTrigger.refresh()
}

watch(
  () => props.activities.length,
  async () => {
    if (activeIndex.value >= total.value) activeIndex.value = 0
    await nextTick()
    setupStageMotion()
    animateStage()
    startAutoPlay()
  },
  { flush: 'post' },
)

onMounted(() => {
  setupStageMotion()
  startAutoPlay()
  animateStage()
})

onBeforeUnmount(() => {
  window.clearInterval(autoPlayTimer)
  animationContext?.revert()
  tiltXTo?.tween.kill()
  tiltYTo?.tween.kill()
  tiltXTo = undefined
  tiltYTo = undefined
})
</script>
