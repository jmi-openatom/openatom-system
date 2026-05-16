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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'

const props = defineProps<{
  activities: any[]
  loading: boolean
}>()

const router = useRouter()
const stageRef = ref<HTMLElement>()
const activeIndex = ref(0)
const tiltX = ref(0)
const tiltY = ref(0)

let autoPlayTimer: number | undefined
let animationContext: gsap.Context | undefined
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
  const opacity = absoluteOffset === 0 ? 1 : Math.max(0.18, 0.7 - absoluteOffset * 0.22)
  const zIndex = 10 - absoluteOffset

  return {
    '--card-x': `${x}%`,
    '--card-rotate': `${rotate}deg`,
    '--card-scale': scale,
    '--card-opacity': opacity,
    '--card-z': zIndex,
    '--stage-tilt-x': `${tiltX.value}deg`,
    '--stage-tilt-y': `${tiltY.value}deg`,
  }
}

function animateSwitch(index: number) {
  const stage = stageRef.value
  const nextIndex = normalizeIndex(index)

  if (!stage || switching || nextIndex === activeIndex.value) {
    activeIndex.value = nextIndex
    return
  }

  switching = true

  const currentCard = stage.querySelector<HTMLElement>('.activity-stage__card.is-active')
  const nextCard = stage.querySelectorAll<HTMLElement>('.activity-stage__card')[nextIndex]

  const timeline = gsap.timeline({
    defaults: {
      ease: 'power3.inOut',
    },
    onComplete: () => {
      switching = false
    },
  })

  if (currentCard) {
    timeline
      .to(
        currentCard.querySelector('.activity-stage__content'),
        {
          y: -18,
          opacity: 0,
          duration: 0.28,
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
          rotateY: -14,
          duration: 0.42,
        },
        0,
      )
  }

  timeline.add(() => {
    activeIndex.value = nextIndex
  }, 0.2)

  if (nextCard) {
    timeline
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
      .fromTo(
        nextCard.querySelector('.activity-stage__meta'),
        {
          y: 16,
          opacity: 0,
        },
        {
          y: 0,
          opacity: 1,
          duration: 0.34,
        },
        0.34,
      )
      .fromTo(
        nextCard.querySelector('h3'),
        {
          y: 26,
          opacity: 0,
        },
        {
          y: 0,
          opacity: 1,
          duration: 0.42,
        },
        0.39,
      )
      .fromTo(
        nextCard.querySelector('p'),
        {
          y: 18,
          opacity: 0,
        },
        {
          y: 0,
          opacity: 1,
          duration: 0.38,
        },
        0.46,
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
  if (total.value <= 1) return
  window.clearInterval(autoPlayTimer)
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
  if (!stage) return

  const rect = stage.getBoundingClientRect()
  const xRatio = (event.clientX - rect.left) / rect.width - 0.5
  const yRatio = (event.clientY - rect.top) / rect.height - 0.5
  tiltX.value = yRatio * -6
  tiltY.value = xRatio * 8
}

function resetTilt() {
  tiltX.value = 0
  tiltY.value = 0
}

function animateStage() {
  animationContext?.revert()
  if (!stageRef.value) return

  animationContext = gsap.context(() => {
    gsap.from('.activity-stage__card', {
      y: 36,
      opacity: 0,
      duration: 0.8,
      stagger: 0.08,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: '.activity-stage',
        start: 'top 84%',
        once: true,
      },
    })
  }, stageRef.value)
}

watch(
  () => props.activities.length,
  () => {
    if (activeIndex.value >= total.value) activeIndex.value = 0
    startAutoPlay()
  },
)

onMounted(() => {
  startAutoPlay()
  animateStage()
})

onBeforeUnmount(() => {
  window.clearInterval(autoPlayTimer)
  animationContext?.revert()
})
</script>
