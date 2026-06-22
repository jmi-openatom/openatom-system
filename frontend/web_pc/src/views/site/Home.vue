<template>
  <ViewPage class="home-page" :loading="loading">
    <HomeHero :cool-down-time="heroMorphCoolDownTime" :morph-time="heroMorphTime" :texts="texts" />
    <HomeOverviewPage :metrics="metrics" :tech-stack="techStack" @scroll-to="scrollTo" />
    <template v-if="renderDeferredSections">
      <HomeFocusSection :club="club" :focus-areas="focusAreas" :loading="loading" />
      <HomeActivitiesSection :activities="activities" :loading="loading" />
      <HomeFeaturedBlogsSection :articles="featuredBlogs" />
      <HomePeopleSection :people="people" :loading="loading" />
      <HomeAwardsSection :awards="awards" :loading="loading" />
    </template>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import {
  defineAsyncComponent,
  getCurrentInstance,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
} from 'vue'
import { siteApi } from '@/api'
import HomeHero from '@/components/site/home/HomeHero.vue'
import HomeOverviewPage from '@/components/site/home/HomeOverviewPage.vue'

const HomeActivitiesSection = defineAsyncComponent(
  () => import('@/components/site/home/HomeActivitiesSection.vue'),
)
const HomeAwardsSection = defineAsyncComponent(
  () => import('@/components/site/home/HomeAwardsSection.vue'),
)
const HomeFeaturedBlogsSection = defineAsyncComponent(
  () => import('@/components/site/home/HomeFeaturedBlogsSection.vue'),
)
const HomeFocusSection = defineAsyncComponent(
  () => import('@/components/site/home/HomeFocusSection.vue'),
)
const HomePeopleSection = defineAsyncComponent(
  () => import('@/components/site/home/HomePeopleSection.vue'),
)

const loading = ref(false)

const renderDeferredSections = ref(false)

const club = ref<Record<string, any>>({})

const metrics = ref<any[]>([])

const techStack = ref<any[]>([])

const focusAreas = ref<any[]>([])

const activities = ref<any[]>([])

const featuredBlogs = ref<any[]>([])

const people = ref<any[]>([])

const awards = ref<any[]>([])

const animationContext = ref<any>()

const instance = getCurrentInstance()

const texts = ['开放原子开源社团', 'JMI-OPENATOM']

const heroMorphTime = 0.86

const heroMorphCoolDownTime = 2.4

let isUnmounted = false
let cancelHomeAnimationIdle: (() => void) | undefined
let cancelDeferredSectionsIdle: (() => void) | undefined

async function loadClubHome() {
  loading.value = true
  try {
    const [data, blogData] = await Promise.all([
      siteApi.clubHome(),
      siteApi.blogArticles({ featured: true, page: 1, pageSize: 3 }).catch(() => null),
    ])
    club.value = data.club || {}
    metrics.value = data.metrics || []
    techStack.value = data.techStack || []
    focusAreas.value = data.focusAreas || []
    activities.value = data.activities || []
    featuredBlogs.value = blogData?.list || []
    people.value = data.people || []
    awards.value = data.awards || []
  } catch (error) {
    club.value = {}
    metrics.value = []
    techStack.value = []
    focusAreas.value = []
    activities.value = []
    featuredBlogs.value = []
    people.value = []
    awards.value = []
  } finally {
    loading.value = false
  }
}

function shouldUseReducedHomeMotion() {
  const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  const connection = (navigator as any).connection
  const saveData = Boolean(connection?.saveData)
  const lowMemory =
    typeof (navigator as any).deviceMemory === 'number' && (navigator as any).deviceMemory <= 4
  const lowCpu =
    typeof navigator.hardwareConcurrency === 'number' && navigator.hardwareConcurrency <= 4
  return reducedMotion || saveData || lowMemory || lowCpu
}

function runWhenIdle(callback: () => void) {
  const requestIdle = window.requestIdleCallback
  if (requestIdle) {
    const idleId = requestIdle(callback, { timeout: 900 })
    return () => window.cancelIdleCallback?.(idleId)
  }
  const timeoutId = window.setTimeout(callback, 120)
  return () => window.clearTimeout(timeoutId)
}

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function loadHomeMotion() {
  const [{ default: gsap }, { ScrollTrigger }] = await Promise.all([
    import('gsap'),
    import('gsap/ScrollTrigger'),
  ])
  gsap.registerPlugin(ScrollTrigger)
  return { gsap, ScrollTrigger }
}

function animateMetricValue(element: HTMLElement, gsap: any) {
  const rawText = element.textContent?.trim() || ''
  const match = rawText.match(/\d+(?:\.\d+)?/)
  if (!match) return

  const targetValue = Number(match[0])
  if (!Number.isFinite(targetValue)) return

  const startIndex = match.index || 0
  const decimals = match[0].includes('.') ? match[0].split('.')[1]?.length || 0 : 0
  const prefix = rawText.slice(0, startIndex)
  const suffix = rawText.slice(startIndex + match[0].length)
  const counter = { value: 0 }

  gsap.to(counter, {
    value: targetValue,
    duration: 1.35,
    ease: 'power2.out',
    scrollTrigger: {
      trigger: element,
      start: 'top 88%',
      once: true,
    },
    onUpdate: () => {
      element.textContent = `${prefix}${counter.value.toFixed(decimals)}${suffix}`
    },
  })
}

async function createHomeAnimations() {
  const { gsap, ScrollTrigger } = await loadHomeMotion()
  if (isUnmounted) return

  animationContext.value = gsap.context(() => {
    const prefersReducedMotion =
      window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
    const supportsCss = (property: string, value: string) => {
      return (
        typeof CSS !== 'undefined' &&
        typeof CSS.supports === 'function' &&
        CSS.supports(property, value)
      )
    }
    const supportsBlur = supportsCss('filter', 'blur(1px)')
    const supports3d = supportsCss('transform-style', 'preserve-3d')
    const supportsClipPath =
      supportsCss('clip-path', 'inset(0 0 20% 0 round 0 0 36px 36px)') ||
      supportsCss('-webkit-clip-path', 'inset(0 0 20% 0 round 0 0 36px 36px)')

    if (prefersReducedMotion) {
      ScrollTrigger.refresh()
      return
    }

    const heroTimeline = gsap.timeline({
      defaults: {
        ease: 'power4.out',
      },
    })

    heroTimeline
      .addLabel('intro', 0)
      .from(
        '.hero',
        {
          ...(supportsClipPath
            ? { clipPath: 'inset(0 0 20% 0 round 0 0 36px 36px)' }
            : { opacity: 0.94, y: 18 }),
          duration: 1.12,
          ease: 'expo.out',
        },
        'intro',
      )
      .from(
        '.hero__map',
        {
          autoAlpha: 0,
          y: 36,
          scale: 1.1,
          duration: 1.45,
        },
        'intro',
      )
      .from(
        '.hero__glass',
        {
          autoAlpha: 0,
          duration: 0.82,
        },
        'intro+=0.12',
      )
      .from(
        '.home-hero__morph',
        {
          y: 54,
          autoAlpha: 0,
          scale: 0.96,
          ...(supportsBlur ? { filter: 'blur(16px)' } : {}),
          duration: 1.02,
          ease: 'expo.out',
        },
        'intro+=0.26',
      )
      .from(
        '.hero__subtitle',
        {
          y: 26,
          autoAlpha: 0,
          duration: 0.68,
          ease: 'power3.out',
        },
        '<0.22',
      )

    const heroParallax = gsap.timeline({
      scrollTrigger: {
        trigger: '.hero',
        start: 'top top',
        end: 'bottom top',
        scrub: 0.9,
      },
    })

    heroParallax
      .to('.hero__map', { yPercent: 10, scale: 1.04, ease: 'none' }, 0)
      .to('.home-hero__morph', { y: -34, scale: 0.985, ease: 'none' }, 0)
      .to('.hero__subtitle', { y: -18, autoAlpha: 0.74, ease: 'none' }, 0)

    const overviewTimeline = gsap.timeline({
      defaults: {
        ease: 'power3.out',
      },
      scrollTrigger: {
        trigger: '.home-overview-page',
        start: 'top 72%',
        once: true,
      },
    })

    overviewTimeline
      .from(
        '.hero__actions .el-button',
        {
          y: 28,
          autoAlpha: 0,
          scale: 0.94,
          duration: 0.66,
          ease: 'back.out(1.55)',
          stagger: 0.08,
        },
        0,
      )
      .from(
        '.command-panel',
        {
          y: 48,
          autoAlpha: 0,
          scale: 0.985,
          ...(supportsBlur ? { filter: 'blur(12px)' } : {}),
          duration: 0.9,
        },
        0.08,
      )
      .from(
        '.metric-console__rail',
        {
          scaleX: 0,
          transformOrigin: 'center center',
          duration: 0.72,
        },
        0.34,
      )
      .from(
        '.metric-console__core',
        {
          y: 24,
          autoAlpha: 0,
          duration: 0.68,
        },
        0.3,
      )
      .from(
        '.metric-node',
        {
          y: 40,
          autoAlpha: 0,
          scale: 0.92,
          transformOrigin: '50% 100%',
          duration: 0.72,
          ease: 'back.out(1.35)',
          stagger: {
            each: 0.07,
            from: 'center',
          },
        },
        0.52,
      )
      .from(
        '.terminal-strip span',
        {
          y: 16,
          autoAlpha: 0,
          duration: 0.38,
          stagger: 0.035,
        },
        0.86,
      )

    gsap.utils
      .toArray<HTMLElement>('.metric-console__core strong, .metric-node strong')
      .forEach((element) => animateMetricValue(element, gsap))

    gsap.set('.reveal-block', {
      y: 58,
      autoAlpha: 0,
      ...(supportsBlur ? { filter: 'blur(12px)' } : {}),
    })

    ScrollTrigger.batch('.reveal-block', {
      start: 'top 82%',
      once: true,
      onEnter: (batch) => {
        gsap.to(batch, {
          y: 0,
          autoAlpha: 1,
          ...(supportsBlur ? { filter: 'blur(0px)' } : {}),
          duration: 0.86,
          ease: 'power3.out',
          stagger: 0.08,
          overwrite: 'auto',
          clearProps: supportsBlur
            ? 'transform,opacity,visibility,filter'
            : 'transform,opacity,visibility',
        })
      },
    })

    gsap.set('.reveal-card', {
      y: 64,
      autoAlpha: 0,
      scale: 0.96,
      ...(supports3d ? { rotateX: -8, transformPerspective: 900 } : {}),
      ...(supportsBlur ? { filter: 'blur(10px)' } : {}),
      transformOrigin: '50% 100%',
    })

    ScrollTrigger.batch('.reveal-card', {
      start: 'top 86%',
      once: true,
      onEnter: (batch) => {
        gsap.to(batch, {
          y: 0,
          autoAlpha: 1,
          scale: 1,
          ...(supports3d ? { rotateX: 0 } : {}),
          ...(supportsBlur ? { filter: 'blur(0px)' } : {}),
          duration: 0.78,
          ease: 'power3.out',
          stagger: {
            each: 0.06,
            from: 'start',
          },
          overwrite: 'auto',
          clearProps: supportsBlur
            ? 'transform,opacity,visibility,filter'
            : 'transform,opacity,visibility',
        })
      },
    })

    ScrollTrigger.refresh()
  }, instance?.proxy?.$el)
}

onMounted(async () => {
  await loadClubHome()
  await nextTick()
  cancelDeferredSectionsIdle = runWhenIdle(() => {
    renderDeferredSections.value = true
  })
  if (!shouldUseReducedHomeMotion()) {
    cancelHomeAnimationIdle = runWhenIdle(() => {
      void createHomeAnimations()
    })
  }
})

onBeforeUnmount(() => {
  isUnmounted = true
  cancelHomeAnimationIdle?.()
  cancelDeferredSectionsIdle?.()
  animationContext.value?.revert()
})
</script>

<style>
.home-page {
  animation: none;
  overflow: hidden;
  background: transparent;
}

.hero {
  position: relative;
  min-height: 560px;
  display: flex;
  align-items: center;
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
}

.hero__mesh {
  position: absolute;
  inset: 0;
  opacity: 0.5;
  background-image: none;
  background-size: 54px 54px;
  mask-image: none;
}

.hero__inner {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 44px;
  align-items: center;
  padding: 72px 0;
}

.hero__tag {
  height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(29, 29, 31, 0.12);
  color: var(--oa-primary-dark);
  background: rgba(255, 255, 255, 0.76);
}

.hero h1 {
  margin: 18px 0 16px;
  max-width: 720px;
  font-size: 56px;
  line-height: 1.12;
  letter-spacing: 0;
}

.hero p {
  max-width: 650px;
  margin: 0;
  color: var(--oa-muted);
  font-size: 18px;
  line-height: 1.9;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 30px;
}

.home-overview-page {
  display: flex;
  min-height: 72vh;
  align-items: center;
  background: var(--oa-elevated-bg);
}

.home-overview-page__inner {
  display: grid;
  justify-items: center;
  gap: 28px;
  padding: 80px 0;
}

.command-panel {
  padding: 22px;
  border: 1px solid rgba(224, 224, 224, 0.95);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: none;
  backdrop-filter: none;
}

.panel-topline,
.terminal-strip {
  display: flex;
  align-items: center;
}

.panel-topline {
  justify-content: space-between;
  margin-bottom: 18px;
  color: var(--oa-muted);
  font-size: 13px;
}

.panel-topline strong {
  color: var(--oa-primary-dark);
}

.signal-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.signal-card {
  position: relative;
  min-height: 122px;
  padding: 16px;
  border: 1px solid rgba(224, 224, 224, 0.95);
  border-radius: 18px;
  background: var(--oa-page-soft-bg);
  transition: border-color 0.2s ease;
}

.signal-card:hover {
  border-color: rgba(29, 29, 31, 0.65);
  box-shadow: none;
}

.signal-card__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.signal-card__icon {
  flex-shrink: 0;
  font-size: 22px;
  color: var(--oa-primary-dark);
  opacity: 0.88;
}

.signal-card__label {
  display: block;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.35;
}

.signal-card__value {
  display: block;
  margin: 10px 0 8px;
  color: var(--oa-text);
  font-size: 34px;
  line-height: 1.1;
  letter-spacing: 0;
}

.signal-card__note {
  display: block;
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1.45;
}

.signal-card--members {
  background: var(--oa-page-soft-bg);
}

.signal-card--members .signal-card__icon {
  color: var(--oa-text);
}

.signal-card--activity {
  background: var(--oa-page-soft-bg);
}

.signal-card--activity .signal-card__icon {
  color: var(--oa-text);
}

.signal-card--award {
  background: var(--oa-page-soft-bg);
}

.signal-card--award .signal-card__icon {
  color: var(--oa-text);
}

.signal-card--recruit {
  background: var(--oa-page-soft-bg);
}

.signal-card--recruit .signal-card__icon {
  color: var(--oa-text);
}

.terminal-strip {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.terminal-strip span {
  padding: 6px 10px;
  border: 1px solid rgba(224, 224, 224, 0.9);
  border-radius: 999px;
  color: var(--oa-primary-dark);
  background: var(--oa-page-soft-bg);
  font-size: 12px;
}

.section {
  padding: 72px 0;
}

.section-heading {
  max-width: 660px;
}

.section-heading span {
  color: var(--oa-primary-dark);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.section-heading h2 {
  margin: 10px 0;
  color: var(--oa-text);
  font-size: 32px;
  line-height: 1.25;
  letter-spacing: 0;
}

.section-heading p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.8;
}

.brief-grid,
.people-grid,
.award-grid {
  display: grid;
  gap: 16px;
  margin-top: 28px;
}

.brief-grid {
  width: 1150px;
  grid-template-columns: repeat(2, 1fr);
}

.brief-card,
.person-card,
.award-card,
.activity-item {
  border: 1px solid rgba(224, 224, 224, 0.95);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: none;
  backdrop-filter: none;
}

.activity-item {
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease;
}

.activity-item:hover,
.brief-card:hover,
.person-card:hover,
.award-card:hover {
  transform: none;
  box-shadow: none;
  border-color: rgba(29, 29, 31, 0.95);
}

.brief-card {
  padding: 24px;
}

.brief-card .el-icon {
  width: 42px;
  height: 42px;
  border-radius: 18px;
  color: var(--oa-primary-dark);
  background: var(--oa-page-soft-bg);
  font-size: 24px;
}

.brief-card h3,
.activity-item h3,
.person-card h3,
.award-card h3 {
  margin: 16px 0 8px;
  color: var(--oa-text);
}

.brief-card p,
.activity-item p,
.person-card p,
.award-card p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.activity-band {
  background: var(--oa-page-soft-bg);
}

.activity-timeline {
  display: grid;
  gap: 14px;
  margin-top: 28px;
}

.activity-item {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 18px;
}

.activity-item time {
  color: var(--oa-primary-dark);
  font-size: 26px;
  font-weight: 600;
}

.activity-item h3 {
  margin-top: 0;
}

.people-grid {
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
}

.person-card {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
}

.avatar {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 18px;
  color: #ffffff;
  background: var(--oa-page-soft-bg);
  font-size: 22px;
  font-weight: 600;
}

.person-card small {
  margin-top: auto;
  color: var(--oa-muted);
  line-height: 1.6;
}

.activity-carousel {
  margin-top: 16px;
}

.activity-carousel-item {
  width: 800px;
}

.activity-card {
  background: var(--oa-panel);
  border: 1px solid var(--oa-border);
  border-radius: 12px;
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
}

.activity-card time {
  font-size: 0.8rem;
  color: var(--oa-muted);
}

.activity-card h3 {
  font-size: 1.5rem;
  color: var(--oa-text);
  font-weight: 600;
  margin: 6px 0;
}

:deep(.el-carousel__item.is-active) .activity-card h3 {
  color: var(--oa-primary);
}

.activity-card p {
  font-size: 0.85rem;
  color: var(--oa-muted);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  margin-bottom: 8px;
}

.activity-image {
  width: 95%;
  height: 400px;
  flex-shrink: 0;
  padding: 5px;
  margin: 0 auto;
}

.activity-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

:deep(.el-tag) {
  width: auto;
  height: 40px;
  font-size: 16px;
  font-weight: 600;
  padding: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* DESIGN.md full-page rewrite: product-tile rhythm, no SaaS chrome. */
.home-page {
  background: var(--oa-elevated-bg);
}

.hero {
  min-height: calc(100vh - 44px);
  background: var(--oa-elevated-bg);
}

.hero__mesh {
  display: none;
}

.hero__inner {
  grid-template-columns: minmax(0, 1fr);
  justify-items: center;
  gap: 48px;
  padding: 80px 0;
  text-align: center;
}

.hero__content {
  width: 100%;
  max-width: none;
}

.home-hero__morph {
  width: min(1180px, calc(100vw - 48px));
  max-width: none;
  min-height: 1.1em;
}

.hero__tag {
  height: auto;
  padding: 8px 14px;
  border-color: #f0f0f0;
  border-radius: 999px;
  color: var(--oa-text-soft);
  background: var(--oa-button-subtle-bg);
}

.hero h1 {
  max-width: 960px;
  margin: 18px auto 14px;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 56px;
  font-weight: 600;
  line-height: 1.07;
  letter-spacing: 0;
}

.hero p {
  max-width: 760px;
  margin: 0 auto;
  color: var(--oa-text);
  font-size: 24px;
  font-weight: 300;
  line-height: 1.5;
  letter-spacing: 0;
}

.hero__actions {
  justify-content: center;
  gap: 12px;
  margin-top: 0;
}

.command-panel {
  width: min(960px, 100%);
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.panel-topline {
  justify-content: center;
  gap: 12px;
  margin-bottom: 18px;
  color: var(--oa-muted);
  font-size: 12px;
}

.metric-console {
  --metric-ink: #1d1d1f;
  --metric-muted: rgba(29, 29, 31, 0.56);
  --metric-faint: rgba(29, 29, 31, 0.34);
  --metric-line: rgba(29, 29, 31, 0.12);
  --metric-glow: rgba(29, 29, 31, 0.18);
  position: relative;
  min-height: 360px;
  padding: 34px 0 8px;
  overflow: visible;
}

.metric-console__core {
  position: relative;
  display: grid;
  min-height: 180px;
  place-items: center;
  color: var(--metric-ink);
}

.metric-console__core::before {
  position: absolute;
  width: min(560px, 72%);
  height: 1px;
  top: 50%;
  left: 50%;
  background: linear-gradient(90deg, transparent, var(--metric-glow), transparent);
  content: '';
  transform: translate(-50%, -50%);
}

.metric-console__copy {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 12px;
  text-align: center;
}

.metric-console__core span,
.metric-console__core p,
.metric-console__core small,
.metric-console__core strong {
  position: relative;
  z-index: 1;
}

.metric-console__core span {
  color: var(--metric-faint);
  font-size: 12px;
  letter-spacing: 0.28em;
}

.metric-console__core strong {
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: clamp(70px, 8vw, 110px);
  font-weight: 600;
  line-height: 0.9;
}

.metric-console__core p {
  margin: 0;
  font-size: 20px;
}

.metric-console__core small {
  max-width: 280px;
  color: var(--metric-muted);
  line-height: 1.6;
}

.metric-console__rail {
  position: absolute;
  right: 0;
  bottom: 94px;
  left: 0;
  height: 1px;
  background: var(--metric-line);
}

.metric-console__orbit {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: end;
  gap: 0;
  min-height: 126px;
}

.metric-node {
  position: relative;
  display: grid;
  align-content: end;
  gap: 7px;
  min-height: 126px;
  padding: 18px 18px 8px;
  border: 0;
  background: transparent;
  color: var(--metric-muted);
  text-align: left;
  cursor: pointer;
  transition:
    color 0.28s ease,
    transform 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.metric-node::before {
  position: absolute;
  top: 30px;
  left: 18px;
  width: 1px;
  height: 44px;
  background: currentColor;
  content: '';
  opacity: 0.2;
  transition:
    height 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.28s ease,
    transform 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.metric-node::after {
  position: absolute;
  top: 24px;
  left: 14px;
  width: 9px;
  height: 9px;
  border-radius: 999px;
  content: '';
  background: currentColor;
  opacity: 0.34;
  transition:
    transform 0.28s ease,
    opacity 0.28s ease;
}

.metric-node i {
  display: none;
}

.metric-node span {
  color: inherit;
  font-size: 14px;
}

.metric-node strong {
  font-family: 'SF Pro Display', system-ui, sans-serif;
  color: var(--metric-ink);
  font-size: 34px;
  font-weight: 600;
  line-height: 1;
}

.metric-node small {
  color: var(--metric-faint);
  font-size: 12px;
}

.metric-node:hover,
.metric-node.is-active {
  color: var(--metric-ink);
  transform: translateY(-10px);
}

.metric-node:hover::before,
.metric-node.is-active::before {
  height: 58px;
  opacity: 0.72;
  transform: translateY(-10px);
}

.metric-node:hover::after,
.metric-node.is-active::after {
  opacity: 1;
  transform: scale(1.8);
}

.metric-swap-enter-active,
.metric-swap-leave-active {
  transition:
    opacity 0.22s ease,
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.22s ease;
}

.metric-swap-enter-from {
  opacity: 0;
  filter: blur(10px);
  transform: translateY(18px);
}

.metric-swap-leave-to {
  opacity: 0;
  filter: blur(10px);
  transform: translateY(-14px);
}

.terminal-strip {
  justify-content: center;
  margin-top: 16px;
}

.section {
  width: 100%;
  padding: 80px max(24px, calc((100vw - 1180px) / 2));
}

.club-brief {
  max-width: none;
  display: flex;
  flex-direction: column;
  gap: 22px;
  justify-content: center;
  align-items: center;
  padding-top: clamp(72px, 8vw, 104px);
  padding-bottom: clamp(72px, 8vw, 104px);
  background: var(--oa-page-soft-bg);
}

.focus-carousel {
  margin-top: 4px;
}

.people-section {
  max-width: none;
  min-height: 100svh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: var(--oa-page-soft-bg);
}

.achievements-section {
  max-width: none;
  min-height: 100svh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: var(--oa-elevated-bg);
}

.section-heading {
  position: relative;
  max-width: 760px;
  margin: 0 auto;
  text-align: center;
}

.section-heading::before {
  display: block;
  width: 68px;
  height: 1px;
  margin: 0 auto 18px;
  content: '';
  background: linear-gradient(90deg, transparent, rgba(29, 29, 31, 0.78), transparent);
}

.section-heading span {
  color: var(--oa-text);
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0;
  text-transform: none;
}

.section-heading h2 {
  margin: 10px 0;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 40px;
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: 0;
}

.section-heading p {
  color: var(--oa-muted);
  font-size: 17px;
  line-height: 1.47;
}

.brief-grid,
.people-grid {
  width: min(1180px, 100%);
  margin: 32px auto 0;
  gap: 20px;
}

.brief-card,
.person-card {
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  background: var(--oa-elevated-bg);
}

.brief-card:hover,
.person-card:hover {
  border-color: var(--oa-text);
}

.brief-card .el-icon,
.avatar {
  color: #ffffff;
  background: #1d1d1f;
  border-radius: 8px;
}

.activity-band {
  min-height: 100svh;
  display: flex;
  align-items: center;
  background: var(--oa-elevated-bg);
  color: var(--oa-text);
}

.activity-shell {
  display: flex;
  width: 100%;
  max-width: none;
  min-height: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 96px 24px;
  margin: 0;
  box-sizing: border-box;
}

.activity-band .section-heading {
  width: min(760px, 100%);
  max-width: none;
  margin: 0 auto;
  text-align: center;
}

.activity-band .section-heading span,
.activity-band .section-heading h2 {
  display: block;
  width: 100%;
  white-space: nowrap;
  word-break: keep-all;
}

.activity-band .section-heading h2,
.activity-band .activity-card h3 {
  color: var(--oa-text);
}

.activity-band .section-heading span,
.activity-band .activity-card time {
  color: var(--oa-primary-dark);
}

.activity-band .activity-card p {
  color: var(--oa-muted);
}

.activity-band :deep(.el-empty) {
  width: min(440px, 100%);
  margin: 40px auto 0;
  padding: 28px 24px;
  border: 1px solid rgba(224, 224, 224, 0.95);
  border-radius: 18px;
  background: var(--oa-button-subtle-bg);
}

.activity-band :deep(.el-empty__image) {
  opacity: 0.82;
}

.activity-band :deep(.el-empty__description) {
  margin-top: 16px;
  color: var(--oa-muted);
  font-size: 16px;
  line-height: 1.5;
  white-space: nowrap;
}

.activity-stage {
  --stage-tilt-x: 0deg;
  --stage-tilt-y: 0deg;
  position: relative;
  width: min(1180px, 100%);
  height: 620px;
  margin: 34px auto 0;
  perspective: 1600px;
}

.activity-stage__card {
  --card-x: 0%;
  --card-rotate: 0deg;
  --card-scale: 1;
  --card-opacity: 1;
  --card-z: 1;
  position: absolute;
  top: 0;
  left: 50%;
  z-index: var(--card-z);
  width: min(820px, calc(100% - 120px));
  height: 500px;
  overflow: hidden;
  border: 1px solid rgba(29, 29, 31, 0.1);
  border-radius: 32px;
  background: #1d1d1f;
  opacity: var(--card-opacity);
  cursor: pointer;
  transform: translateX(calc(-50% + var(--card-x)))
    rotateY(calc(var(--card-rotate) + var(--stage-tilt-y))) rotateX(var(--stage-tilt-x))
    scale(var(--card-scale));
  transform-origin: center center;
  transition:
    transform 0.72s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.28s ease,
    filter 0.48s ease,
    border-color 0.32s ease;
  box-shadow: 0 24px 72px rgba(29, 29, 31, 0.18);
  will-change: transform;
}

.activity-stage__card.is-far {
  pointer-events: none;
  filter: saturate(0.9) brightness(0.88);
}

.activity-stage__card.is-near {
  filter: saturate(0.96) brightness(0.94);
}

.activity-stage__card.is-active {
  border-color: rgba(29, 29, 31, 0.2);
}

.activity-stage__media,
.activity-stage__media img,
.activity-stage__wipe,
.activity-stage__veil {
  position: absolute;
  inset: 0;
}

.activity-stage__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(1.04) contrast(1.03);
  transition:
    transform 0.72s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.35s ease;
}

.activity-stage__card.is-active .activity-stage__media img {
  filter: saturate(1.08) contrast(1.06);
  transform: scale(1.025);
}

.activity-stage__wipe {
  z-index: 2;
  background: var(--oa-elevated-bg);
  transform: scaleX(0);
  transform-origin: right center;
}

.activity-stage__veil {
  z-index: 1;
  background:
    linear-gradient(
      180deg,
      rgba(0, 0, 0, 0.04) 0%,
      rgba(0, 0, 0, 0.2) 48%,
      rgba(0, 0, 0, 0.78) 100%
    ),
    linear-gradient(90deg, rgba(0, 0, 0, 0.48) 0%, rgba(0, 0, 0, 0.1) 58%, rgba(0, 0, 0, 0.34) 100%);
}

.activity-stage__content {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: grid;
  gap: 12px;
  z-index: 3;
  padding: 32px;
  color: #ffffff;
}

.activity-stage__meta {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  color: rgba(255, 255, 255, 0.84);
  font-size: 13px;
  letter-spacing: 0.12em;
  text-shadow: 0 1px 14px rgba(0, 0, 0, 0.46);
}

.activity-stage__content h3 {
  margin: 0;
  color: #ffffff;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: clamp(28px, 3vw, 40px);
  line-height: 1.08;
  text-shadow: 0 2px 18px rgba(0, 0, 0, 0.52);
}

.activity-stage__content p {
  max-width: 560px;
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.7;
  text-shadow: 0 1px 16px rgba(0, 0, 0, 0.5);
}

.activity-stage__controls {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
}

.activity-stage__controls button {
  border: 0;
  background: transparent;
  color: inherit;
}

.activity-stage__controls > button {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border: 1px solid rgba(29, 29, 31, 0.14);
  border-radius: 999px;
  background: var(--oa-elevated-bg);
  color: #1d1d1f;
  cursor: pointer;
  transition:
    transform 0.22s ease,
    background-color 0.22s ease,
    color 0.22s ease;
}

.activity-stage__controls > button:hover {
  background: #1d1d1f;
  color: #ffffff;
  transform: translateY(-2px);
}

.activity-stage__progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.activity-stage__progress button {
  width: 28px;
  height: 2px;
  padding: 0;
  background: rgba(29, 29, 31, 0.18);
  cursor: pointer;
  transition:
    width 0.24s ease,
    background-color 0.24s ease;
}

.activity-stage__progress button.is-active {
  width: 52px;
  background: #1d1d1f;
}

.award-exhibit {
  position: relative;
  display: grid;
  width: min(1180px, 100%);
  min-height: 460px;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.86fr);
  gap: 54px;
  align-items: stretch;
  margin: 42px auto 0;
}

.award-exhibit__ghost-year {
  position: absolute;
  right: -8px;
  bottom: -22px;
  color: rgba(29, 29, 31, 0.055);
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: clamp(120px, 19vw, 280px);
  font-weight: 700;
  line-height: 0.78;
  letter-spacing: -0.08em;
  pointer-events: none;
  user-select: none;
}

.award-exhibit__rail {
  position: absolute;
  top: 18px;
  bottom: 18px;
  left: 148px;
  width: 1px;
  background: rgba(29, 29, 31, 0.14);
}

.award-exhibit__trace {
  --award-progress: 0;
  position: absolute;
  top: 18px;
  left: 148px;
  width: 1px;
  height: calc((100% - 36px) * var(--award-progress));
  background: #1d1d1f;
  transition: height 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.award-exhibit__item {
  position: relative;
  display: grid;
  width: 100%;
  min-height: 86px;
  grid-template-columns: 116px 32px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.award-exhibit__item::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 164px;
  height: 1px;
  content: '';
  background: rgba(29, 29, 31, 0.08);
}

.award-exhibit__year {
  color: rgba(29, 29, 31, 0.36);
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: 42px;
  font-weight: 600;
  line-height: 1;
  transition:
    color 0.28s ease,
    transform 0.28s ease;
}

.award-exhibit__dot {
  position: relative;
  display: block;
  width: 12px;
  height: 12px;
  border: 1px solid rgba(29, 29, 31, 0.28);
  border-radius: 999px;
  background: var(--oa-elevated-bg);
  transition:
    width 0.28s ease,
    height 0.28s ease,
    background-color 0.28s ease,
    border-color 0.28s ease,
    transform 0.28s ease;
}

.award-exhibit__dot i {
  position: absolute;
  inset: -1px;
  border: 1px solid #1d1d1f;
  border-radius: inherit;
  opacity: 0;
}

.award-exhibit__title {
  color: rgba(29, 29, 31, 0.48);
  font-size: 18px;
  transition:
    color 0.28s ease,
    transform 0.28s ease;
}

.award-exhibit__item.is-active .award-exhibit__year {
  color: #1d1d1f;
  transform: translateX(8px);
}

.award-exhibit__item.is-active .award-exhibit__dot {
  width: 18px;
  height: 18px;
  border-color: #1d1d1f;
  background: #1d1d1f;
  transform: translateX(-3px);
}

.award-exhibit__item.is-active .award-exhibit__dot i {
  animation: awardPulse 1.2s ease-out infinite;
}

.award-exhibit__item.is-active .award-exhibit__title {
  color: #1d1d1f;
  transform: translateX(8px);
}

.award-exhibit__spotlight {
  position: relative;
  display: grid;
  align-content: end;
  gap: 18px;
  padding: 32px 0 16px;
}

.award-copy-enter-active,
.award-copy-leave-active {
  transition:
    opacity 0.28s ease,
    transform 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.award-copy-enter-from {
  opacity: 0;
  transform: translateY(18px);
}

.award-copy-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

.award-exhibit__spotlight::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 1px;
  content: '';
  background: #1d1d1f;
}

.award-exhibit__spotlight span {
  color: rgba(29, 29, 31, 0.42);
  font-size: 13px;
  letter-spacing: 0.2em;
}

.award-exhibit__spotlight h3 {
  margin: 0;
  color: #1d1d1f;
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: clamp(30px, 3vw, 44px);
  line-height: 1.08;
}

.award-exhibit__spotlight p {
  margin: 0;
  color: rgba(29, 29, 31, 0.58);
  font-size: 18px;
}

.award-exhibit__spotlight div {
  display: grid;
  gap: 8px;
}

.award-exhibit__spotlight strong {
  color: #1d1d1f;
  font-size: 18px;
}

.award-exhibit__spotlight small {
  color: rgba(29, 29, 31, 0.48);
}

@keyframes awardPulse {
  from {
    opacity: 0.55;
    transform: scale(1);
  }

  to {
    opacity: 0;
    transform: scale(2.4);
  }
}

.hero {
  position: relative;
  min-height: 100vh;
  min-height: 100svh;
  overflow: hidden;
  color: #1d1d1f;
  background: #f7f7f8;
  isolation: isolate;
}

.hero__map {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  opacity: 1;
  will-change: transform, opacity;
}

.hero::before {
  position: absolute;
  inset: 0;
  z-index: 2;
  content: '';
  background: transparent;
  pointer-events: none;
}

.hero::after {
  position: absolute;
  inset: auto 0 0;
  z-index: 3;
  height: 14%;
  content: '';
  background: linear-gradient(180deg, transparent 0%, rgba(255, 255, 255, 0.72) 82%, #ffffff 100%);
  pointer-events: none;
}

.hero__glass {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(1px) saturate(1.08);
  pointer-events: none;
  will-change: opacity;
}

.hero__inner {
  z-index: 4;
  min-height: 100vh;
  min-height: 100svh;
  align-content: center;
  place-items: center;
  padding: clamp(72px, 10vh, 112px) 0 clamp(96px, 13vh, 140px);
}

.hero__content {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 22px;
  width: min(100%, 1060px);
  box-sizing: border-box;
  margin: 0 auto;
}

.hero__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  padding: 6px 14px 6px 8px;
  border: 1px solid rgba(29, 29, 31, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: #4a4a4e;
  box-shadow: 0 12px 36px rgba(29, 29, 31, 0.06);
  backdrop-filter: blur(16px);
  font-size: 13px;
}

.hero__logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
}

.home-hero__morph {
  width: min(100%, 980px, calc(100vw - 48px));
  height: clamp(82px, 9vw, 118px);
  min-height: clamp(82px, 9vw, 118px);
  color: #1d1d1f;
  filter: none;
  font-size: clamp(44px, 6.8vw, 92px);
  font-weight: 700;
  line-height: 0.98;
  will-change: transform, opacity;
}

@media (max-width: 720px) {
  .home-hero__morph {
    width: min(100%, calc(100vw - 24px));
    font-size: clamp(34px, 10vw, 52px);
  }
}

.home-hero__morph span {
  color: #000000;
}

.hero__subtitle {
  max-width: 660px;
  margin: 0 auto;
  color: #66666b;
  font-size: 18px;
  line-height: 1.7;
  letter-spacing: 0;
  will-change: transform, opacity;
}

.home-overview-page {
  position: relative;
  min-height: 100vh;
  min-height: 100svh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(180deg, #ffffff 0%, #f5f5f7 100%);
}

.home-interactive-section {
  position: relative;
  overflow: hidden;
  isolation: isolate;
}

.home-section-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.home-interactive-section > :not(.home-section-canvas) {
  position: relative;
  z-index: 1;
}

.brief-card,
.person-card,
.award-card {
  transition:
    transform 0.26s ease,
    border-color 0.26s ease,
    box-shadow 0.26s ease;
  will-change: transform;
}

.brief-card:hover,
.person-card:hover {
  box-shadow: 0 18px 44px rgba(29, 29, 31, 0.12);
}

@media (max-width: 1040px) {
  .hero__inner,
  .brief-grid,
  .people-grid {
    grid-template-columns: 1fr 1fr;
  }

  .hero__content {
    grid-column: 1 / -1;
  }

  .brief-grid {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .hero {
    min-height: 88svh;
  }

  .hero__inner {
    min-height: 88svh;
    padding: 72px 0 108px;
  }

  .home-hero__morph {
    min-height: 96px;
  }

  .hero__inner,
  .brief-grid,
  .people-grid,
  .signal-grid,
  .activity-item {
    grid-template-columns: 1fr;
  }

  .hero h1 {
    font-size: 38px;
  }

  .hero p {
    font-size: 16px;
  }

  .hero__actions .el-button {
    flex: 1 1 160px;
  }

  .activity-shell {
    min-height: 460px;
    padding: 64px 20px;
  }

  .activity-band .section-heading span,
  .activity-band .section-heading h2,
  .activity-band :deep(.el-empty__description) {
    white-space: normal;
  }

  .activity-stage {
    height: 470px;
  }

  .activity-stage__card {
    width: calc(100% - 28px);
    height: 390px;
    border-radius: 22px;
  }

  .activity-stage__content {
    gap: 10px;
    padding: 20px;
  }

  .activity-stage__content p {
    display: -webkit-box;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }

  .award-exhibit {
    grid-template-columns: 1fr;
    gap: 28px;
    min-height: 0;
  }

  .award-exhibit__ghost-year {
    right: 0;
    bottom: auto;
    top: 160px;
  }

  .award-exhibit__rail {
    left: 98px;
  }

  .award-exhibit__item {
    min-height: 76px;
    grid-template-columns: 72px 28px minmax(0, 1fr);
  }

  .award-exhibit__item::after {
    left: 116px;
  }

  .award-exhibit__year {
    font-size: 28px;
  }

  .section {
    padding: 52px 0;
  }

  .section-heading h2 {
    font-size: 26px;
  }

  .activity-item {
    align-items: start;
  }

  .metric-console {
    grid-template-columns: 1fr;
  }

  .metric-console__orbit {
    grid-template-columns: 1fr 1fr;
  }

  .metric-node {
    min-height: 148px;
  }
}
</style>
