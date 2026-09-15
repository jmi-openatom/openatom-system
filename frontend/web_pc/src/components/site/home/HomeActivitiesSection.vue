<template>
  <section
    id="activities"
    class="event-gallery home-interactive-section"
    aria-labelledby="events-title"
  >
    <HomeInteractiveBackdrop :radius="230" :spacing="68" :strength="18" />
    <header class="event-gallery__heading">
      <div>
        <span class="event-gallery__eyebrow">近期活动</span>
        <h2 id="events-title">相聚，让热爱发生。</h2>
      </div>
      <router-link class="event-gallery__all" to="/activities"
        >全部活动 <span aria-hidden="true">↗</span></router-link
      >
    </header>

    <div
      v-if="activities.length"
      id="event-gallery-track"
      ref="track"
      class="event-gallery__track"
      tabindex="0"
      role="region"
      aria-label="活动画廊，可左右滑动"
      @scroll.passive="updateNavigation"
      @keydown.left.prevent="move(-1)"
      @keydown.right.prevent="move(1)"
    >
      <router-link
        v-for="(activity, index) in activities"
        :key="activity.id"
        class="event-gallery__card"
        :to="`/activities/${activity.id}`"
        :aria-label="`查看活动：${activity.title}`"
      >
        <div class="event-gallery__image">
          <img
            v-if="activity.coverUrl && !failedImages.has(activity.coverUrl)"
            :src="activity.coverUrl"
            :alt="activity.title"
            loading="lazy"
            decoding="async"
            @error="failedImages.add(activity.coverUrl)"
          />
          <div v-else class="event-gallery__fallback" aria-hidden="true">
            <span>OPEN<br />ATOM.</span><small>连接 · 分享 · 创造</small>
          </div>
          <span class="event-gallery__number" aria-hidden="true">{{
            String(index + 1).padStart(2, '0')
          }}</span>
        </div>
        <div class="event-gallery__caption">
          <time v-if="activity.date">{{ activity.date }}</time>
          <div class="event-gallery__title">
            <h3>{{ activity.title }}</h3>
            <span aria-hidden="true">↗</span>
          </div>
        </div>
      </router-link>
    </div>
    <div v-else class="event-gallery__empty" :aria-busy="loading">
      {{ loading ? '活动加载中…' : '新的相聚，正在酝酿。' }}
    </div>

    <footer v-if="activities.length" class="event-gallery__footer">
      <span>在分享中相遇，在实践中同行。</span>
      <div v-if="canPrevious || canNext" class="event-gallery__controls">
        <button
          type="button"
          aria-label="上一组活动"
          aria-controls="event-gallery-track"
          :disabled="!canPrevious"
          @click="move(-1)"
        >
          ←
        </button>
        <button
          type="button"
          aria-label="下一组活动"
          aria-controls="event-gallery-track"
          :disabled="!canNext"
          @click="move(1)"
        >
          →
        </button>
      </div>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'

defineProps<{
  activities: { id: number | string; title: string; date?: string; coverUrl?: string }[]
  loading: boolean
}>()
const track = ref<HTMLElement>()
const failedImages = ref(new Set<string>())
const canPrevious = ref(false)
const canNext = ref(false)
let resizeObserver: ResizeObserver | undefined

function updateNavigation() {
  const el = track.value
  canPrevious.value = !!el && el.scrollLeft > 2
  canNext.value = !!el && el.scrollWidth - el.clientWidth - el.scrollLeft > 2
}
function move(direction: number) {
  const el = track.value
  if (!el) return
  const card = el.firstElementChild as HTMLElement | null
  const step =
    (card?.offsetWidth || el.clientWidth) + parseFloat(getComputedStyle(el).columnGap || '0')
  el.scrollBy({
    left: direction * step,
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth',
  })
}
watch(
  track,
  (el) => {
    resizeObserver?.disconnect()
    if (el) {
      resizeObserver = new ResizeObserver(updateNavigation)
      resizeObserver.observe(el)
      for (const child of el.children) resizeObserver.observe(child)
    }
    updateNavigation()
  },
  { flush: 'post' },
)
onBeforeUnmount(() => resizeObserver?.disconnect())
</script>

<style scoped>
.event-gallery {
  --gallery-gutter: max(24px, calc((100% - 1200px) / 2));
  position: relative;
  padding: 100px 0 80px;
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
  overflow: hidden;
}
.event-gallery__heading,
.event-gallery__footer {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin: 0 var(--gallery-gutter);
}
.event-gallery__eyebrow {
  display: block;
  margin-bottom: 16px;
  font-size: 14px;
  color: var(--oa-muted);
}
.event-gallery__heading h2 {
  margin: 0;
  font-size: clamp(30px, 4vw, 52px);
  font-weight: 650;
  line-height: 1.2;
  letter-spacing: -0.04em;
}
.event-gallery__all {
  flex-shrink: 0;
  padding-bottom: 5px;
  color: var(--oa-text);
  text-decoration: none;
  font-size: 14px;
}
.event-gallery__all span {
  margin-left: 10px;
}
.event-gallery__track {
  position: relative;
  display: flex;
  gap: 24px;
  overflow-x: auto;
  overscroll-behavior-x: contain;
  scroll-snap-type: x mandatory;
  scroll-padding-inline: var(--gallery-gutter);
  padding: 44px var(--gallery-gutter) 24px;
  scrollbar-width: none;
}
.event-gallery__track::-webkit-scrollbar {
  display: none;
}
.event-gallery__card {
  flex: 0 0 clamp(300px, 39vw, 520px);
  min-width: 0;
  scroll-snap-align: start;
  color: inherit;
  text-decoration: none;
}
.event-gallery__image {
  position: relative;
  aspect-ratio: 5 / 4;
  border-radius: 20px;
  overflow: hidden;
  background: var(--oa-elevated-bg);
}
.event-gallery__image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}
.event-gallery__card:hover img {
  transform: scale(1.035);
}
.event-gallery__number {
  position: absolute;
  top: 18px;
  left: 18px;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 1px solid #ffffff50;
  border-radius: 50%;
  background: #00000050;
  color: #fff;
  font-size: 12px;
  backdrop-filter: blur(12px);
}
.event-gallery__caption {
  padding: 22px 4px 0;
}
.event-gallery__caption time {
  font-size: 13px;
  color: var(--oa-muted);
  font-variant-numeric: tabular-nums;
}
.event-gallery__title {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  justify-content: space-between;
  margin-top: 10px;
}
.event-gallery__title h3 {
  margin: 0;
  font-size: clamp(20px, 2vw, 26px);
  font-weight: 600;
  line-height: 1.4;
  overflow-wrap: anywhere;
}
.event-gallery__title > span {
  flex-shrink: 0;
  font-size: 24px;
  color: var(--oa-muted);
}
.event-gallery__fallback {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 24px;
  height: 100%;
  padding: 40px;
  box-sizing: border-box;
  color: var(--oa-text);
  background: var(--oa-elevated-bg);
}
.event-gallery__fallback > span {
  font-size: clamp(48px, 6vw, 84px);
  font-weight: 750;
  line-height: 0.95;
  letter-spacing: -0.06em;
}
.event-gallery__fallback small {
  color: var(--oa-muted);
  letter-spacing: 0.12em;
}
.event-gallery__footer {
  align-items: center;
  min-height: 44px;
  margin-top: 12px;
}
.event-gallery__footer > span {
  color: var(--oa-muted);
  font-size: 13px;
}
.event-gallery__controls {
  display: flex;
  gap: 10px;
}
.event-gallery__controls button {
  width: 44px;
  height: 44px;
  border: 1px solid var(--oa-border);
  border-radius: 50%;
  background: var(--oa-elevated-bg);
  color: var(--oa-text);
  font: inherit;
  font-size: 22px;
  cursor: pointer;
}
.event-gallery__controls button:disabled {
  opacity: 0.3;
  cursor: default;
}
.event-gallery__all:hover {
  text-decoration: underline;
}
.event-gallery :is(a, button, [tabindex]):focus-visible {
  outline: 2px solid var(--oa-text);
  outline-offset: 4px;
}
.event-gallery__track:focus-visible {
  outline-offset: -4px;
}
.event-gallery__empty {
  position: relative;
  margin: 44px var(--gallery-gutter) 0;
  padding: 64px 24px;
  border: 1px solid var(--oa-border);
  border-radius: 20px;
  text-align: center;
  color: var(--oa-muted);
}
@media (max-width: 640px) {
  .event-gallery {
    --gallery-gutter: 20px;
    padding: 64px 0 48px;
  }
  .event-gallery__heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 20px;
  }
  .event-gallery__track {
    gap: 16px;
    padding-top: 28px;
  }
  .event-gallery__card {
    flex-basis: 82%;
  }
  .event-gallery__image {
    aspect-ratio: 5 / 4;
    border-radius: 16px;
  }
  .event-gallery__caption {
    padding-top: 18px;
  }
  .event-gallery__footer > span {
    max-width: 180px;
    line-height: 1.6;
  }
}
@media (prefers-reduced-motion: reduce) {
  .event-gallery__image img {
    transition: none;
  }
}
</style>
