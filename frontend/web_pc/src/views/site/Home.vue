<template>
  <ViewPage class="home-page" :loading="loading">
    <HomeHero :cool-down-time="heroMorphCoolDownTime" :morph-time="heroMorphTime" :texts="texts" />
    <HomeOverviewPage :metrics="metrics" :tech-stack="techStack" @scroll-to="scrollTo" />
    <HomeFocusSection :club="club" :focus-areas="focusAreas" :loading="loading" />
    <HomeActivitiesSection :activities="activities" :loading="loading" />
    <HomePeopleSection :people="people" :loading="loading" />
    <HomeAwardsSection :awards="awards" :loading="loading" />
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { siteApi } from '@/api'
import HomeActivitiesSection from '@/components/site/home/HomeActivitiesSection.vue'
import HomeAwardsSection from '@/components/site/home/HomeAwardsSection.vue'
import HomeFocusSection from '@/components/site/home/HomeFocusSection.vue'
import HomeHero from '@/components/site/home/HomeHero.vue'
import HomeOverviewPage from '@/components/site/home/HomeOverviewPage.vue'
import HomePeopleSection from '@/components/site/home/HomePeopleSection.vue'

gsap.registerPlugin(ScrollTrigger)

const loading = ref(false)

const club = ref<Record<string, any>>({})

const metrics = ref<any[]>([])

const techStack = ref<any[]>([])

const focusAreas = ref<any[]>([])

const activities = ref<any[]>([])

const people = ref<any[]>([])

const awards = ref<any[]>([])

const animationContext = ref<any>()

const instance = getCurrentInstance()

const texts = ['开放原子开源社团', 'JMI-OPENATOM','信息工程学院']

const heroMorphTime = 0.86

const heroMorphCoolDownTime = 2.4
async function loadClubHome() {
  loading.value = true
  try {
    const data = await siteApi.clubHome()
    club.value = data.club || {}
    metrics.value = data.metrics || []
    techStack.value = data.techStack || []
    focusAreas.value = data.focusAreas || []
    activities.value = data.activities || []
    people.value = data.people || []
    awards.value = data.awards || []
  } catch (error) {
    club.value = {}
    metrics.value = []
    techStack.value = []
    focusAreas.value = []
    activities.value = []
    people.value = []
    awards.value = []
  } finally {
    loading.value = false
  }
}

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function animateMetricValue(element: HTMLElement) {
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

function createHomeAnimations() {
  animationContext.value = gsap.context(() => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

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
      .from('.hero', {
        clipPath: 'inset(0 0 18% 0)',
        duration: 1.05,
      })
      .from(
        '.hero__map',
        {
          opacity: 0,
          scale: 1.04,
          duration: 1.2,
        },
        0,
      )
      .from(
        '.hero__content',
        {
          y: 48,
          opacity: 0,
          filter: 'blur(14px)',
          duration: 1,
        },
        0.32,
      )

    gsap.to('.home-hero__morph', {
      y: -26,
      ease: 'none',
      scrollTrigger: {
        trigger: '.hero',
        start: 'top top',
        end: 'bottom top',
        scrub: 0.8,
      },
    })

    gsap.from('.hero__actions .el-button', {
      y: 26,
      opacity: 0,
      scale: 0.94,
      duration: 0.72,
      ease: 'back.out(1.7)',
      stagger: 0.09,
      scrollTrigger: {
        trigger: '.home-overview-page',
        start: 'top 68%',
      },
    })

    gsap.from('.command-panel', {
      y: 46,
      opacity: 0,
      filter: 'blur(10px)',
      duration: 0.9,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: '.command-panel',
        start: 'top 82%',
      },
    })

    gsap.utils.toArray<HTMLElement>('.signal-card').forEach((element, index) => {
      gsap.from(element, {
        y: 36,
        opacity: 0,
        scale: 0.92,
        duration: 0.72,
        ease: 'back.out(1.5)',
        delay: index * 0.05,
        scrollTrigger: {
          trigger: element,
          start: 'top 88%',
        },
      })
    })

    gsap.utils.toArray<HTMLElement>('.signal-card__value').forEach(animateMetricValue)

    gsap.utils.toArray<HTMLElement>('.reveal-block').forEach((element) => {
      gsap.from(element, {
        y: 58,
        opacity: 0,
        filter: 'blur(12px)',
        duration: 0.86,
        ease: 'power3.out',
        scrollTrigger: {
          trigger: element,
          start: 'top 82%',
        },
      })
    })

    gsap.utils.toArray<HTMLElement>('.reveal-card').forEach((element, index) => {
      gsap.from(element, {
        y: 64,
        opacity: 0,
        rotateX: -8,
        scale: 0.96,
        transformPerspective: 900,
        transformOrigin: '50% 100%',
        duration: 0.78,
        ease: 'power3.out',
        delay: (index % 4) * 0.05,
        scrollTrigger: {
          trigger: element,
          start: 'top 86%',
        },
      })
    })

    gsap.to('.activity-band', {
      backgroundColor: '#111114',
      scrollTrigger: {
        trigger: '.activity-band',
        start: 'top 72%',
        end: 'top 30%',
        scrub: true,
      },
    })

    ScrollTrigger.refresh()
  }, instance?.proxy?.$el)
}

onMounted(async () => {
  await loadClubHome()
  await nextTick()
  createHomeAnimations()
})

onBeforeUnmount(() => {
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
  background: #f5f5f7;
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
  color: #7a7a7a;
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
  background: #ffffff;
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
  color: #7a7a7a;
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
  background: #f5f5f7;
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
  color: #7a7a7a;
  font-size: 13px;
  line-height: 1.35;
}

.signal-card__value {
  display: block;
  margin: 10px 0 8px;
  color: #1d1d1f;
  font-size: 34px;
  line-height: 1.1;
  letter-spacing: 0;
}

.signal-card__note {
  display: block;
  color: #7a7a7a;
  font-size: 12px;
  line-height: 1.45;
}

.signal-card--members {
  background: #f5f5f7;
}

.signal-card--members .signal-card__icon {
  color: #1d1d1f;
}

.signal-card--activity {
  background: #f5f5f7;
}

.signal-card--activity .signal-card__icon {
  color: #1d1d1f;
}

.signal-card--award {
  background: #f5f5f7;
}

.signal-card--award .signal-card__icon {
  color: #1d1d1f;
}

.signal-card--recruit {
  background: #f5f5f7;
}

.signal-card--recruit .signal-card__icon {
  color: #1d1d1f;
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
  background: #f5f5f7;
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
  color: #1d1d1f;
  font-size: 32px;
  line-height: 1.25;
  letter-spacing: 0;
}

.section-heading p {
  margin: 0;
  color: #7a7a7a;
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
  background: #f5f5f7;
  font-size: 24px;
}

.brief-card h3,
.activity-item h3,
.person-card h3,
.award-card h3 {
  margin: 16px 0 8px;
  color: #1d1d1f;
}

.brief-card p,
.activity-item p,
.person-card p,
.award-card p {
  margin: 0;
  color: #7a7a7a;
  line-height: 1.7;
}

.activity-band {
  background: #f5f5f7;
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
  background: #f5f5f7;
  font-size: 22px;
  font-weight: 600;
}

.person-card small {
  margin-top: auto;
  color: #7a7a7a;
  line-height: 1.6;
}

.award-grid {
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
}

.award-card {
  position: relative;
  min-height: 230px;
  padding: 24px;
  overflow: hidden;
}

.award-card::after {
  position: absolute;
  right: -36px;
  bottom: -42px;
  width: 120px;
  height: 120px;
  content: '';
  border: 1px solid rgba(29, 29, 31, 0.6);
  border-radius: 50%;
}

.award-card__year {
  color: #1d1d1f;
  font-size: 30px;
  font-weight: 600;
}

.award-card strong {
  display: inline-block;
  margin-top: 18px;
  color: var(--oa-primary-dark);
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
  background: #ffffff;
}

.hero {
  min-height: calc(100vh - 44px);
  background: #ffffff;
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
  color: #333333;
  background: #fafafc;
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
  color: #1d1d1f;
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
  color: #7a7a7a;
  font-size: 12px;
}

.signal-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  background: #e0e0e0;
}

.signal-card,
.signal-card--members,
.signal-card--activity,
.signal-card--award,
.signal-card--recruit {
  min-height: 150px;
  padding: 24px;
  border: 0;
  border-radius: 0;
  background: #fafafc;
}

.signal-card:hover {
  border-color: transparent;
}

.signal-card__head {
  justify-content: center;
}

.signal-card__value {
  margin: 14px 0 8px;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 40px;
  font-weight: 600;
}

.terminal-strip {
  justify-content: center;
}

.section {
  width: 100%;
  padding: 80px max(24px, calc((100vw - 1180px) / 2));
}

.club-brief {
  max-width: none;
  background: #f5f5f7;
}

.people-section {
  max-width: none;
  background: #ffffff;
}

.achievements-section {
  max-width: none;
  background: #f5f5f7;
}

.section-heading {
  max-width: 760px;
  margin: 0 auto;
  text-align: center;
}

.section-heading span {
  color: #1d1d1f;
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
  color: #7a7a7a;
  font-size: 17px;
  line-height: 1.47;
}

.brief-grid,
.people-grid,
.award-grid {
  width: min(1180px, 100%);
  margin: 32px auto 0;
  gap: 20px;
}

.brief-card,
.person-card,
.award-card {
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  background: #ffffff;
}

.brief-card:hover,
.person-card:hover,
.award-card:hover {
  border-color: #1d1d1f;
}

.brief-card .el-icon,
.avatar {
  color: #ffffff;
  background: #1d1d1f;
  border-radius: 8px;
}

.activity-band {
  background: #272729;
  color: #ffffff;
}

.activity-shell {
  display: flex;
  width: 100%;
  max-width: none;
  min-height: 560px;
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
  color: #ffffff;
}

.activity-band .section-heading span,
.activity-band .activity-card time {
  color: #ffffff;
}

.activity-band .activity-card p {
  color: #cccccc;
}

.activity-band :deep(.el-empty) {
  width: min(440px, 100%);
  margin: 40px auto 0;
  padding: 28px 24px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.activity-band :deep(.el-empty__image) {
  filter: grayscale(1) invert(1);
  opacity: 0.82;
}

.activity-band :deep(.el-empty__description) {
  margin-top: 16px;
  color: #f5f5f7;
  font-size: 16px;
  line-height: 1.5;
  white-space: nowrap;
}

.activity-carousel {
  width: min(1180px, 100%);
  margin: 32px auto 0;
}

.activity-card {
  overflow: hidden;
  border: 0;
  border-radius: 0;
  background: #2a2a2c;
}

.activity-image {
  width: 100%;
  height: 380px;
  padding: 0;
  margin: 18px 0 0;
}

.activity-image img {
  display: block;
  box-shadow: var(--oa-product-shadow);
}

.award-card::after {
  display: none;
}

.hero {
  min-height: 100svh;
  overflow: hidden;
  color: #0f172a;
  background: #f8fbff;
  isolation: isolate;
}

.hero__map {
  position: absolute;
  inset: 0;
  z-index: 0;
  opacity: 1;
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
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(2px) saturate(1.12);
  pointer-events: none;
}

.hero__inner {
  z-index: 4;
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
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: #334155;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.06);
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
  color: #0f172a;
  filter: none;
  font-size: clamp(44px, 6.8vw, 92px);
  font-weight: 700;
  line-height: 0.98;
}

.home-hero__morph span {
  background: linear-gradient(90deg, #020617 0%, #0f766e 46%, #1d4ed8 100%);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
}

.hero__subtitle {
  max-width: 660px;
  margin: 0 auto;
  color: #475569;
  font-size: 18px;
  line-height: 1.7;
  letter-spacing: 0;
}

.home-overview-page {
  position: relative;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
}

.signal-card,
.brief-card,
.person-card,
.award-card,
.activity-card {
  transition:
    transform 0.26s ease,
    border-color 0.26s ease,
    box-shadow 0.26s ease;
  will-change: transform;
}

.signal-card {
  overflow: hidden;
}

.signal-card::before {
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  content: '';
  background: linear-gradient(90deg, #00d1ff, #7cffb2);
  opacity: 0.78;
}

.signal-card--award::before {
  background: linear-gradient(90deg, #f7c948, #ff8f70);
}

.signal-card--recruit::before {
  background: linear-gradient(90deg, #a78bfa, #00d1ff);
}

.signal-card:hover,
.brief-card:hover,
.person-card:hover,
.award-card:hover,
.activity-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.12);
}

.activity-card {
  border-radius: 8px;
}

@media (max-width: 1040px) {
  .hero__inner,
  .brief-grid,
  .people-grid,
  .award-grid {
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
  .award-grid,
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

  .activity-carousel :deep(.el-carousel__container) {
    height: 430px !important;
  }

  .activity-carousel-item {
    width: 86vw;
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

  .activity-image {
    height: 230px;
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

  .signal-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
