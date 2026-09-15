<template>
  <section id="activities" class="activity-band home-interactive-section">
    <HomeInteractiveBackdrop :radius="230" :spacing="68" :strength="18" />
    <div class="activity-shell section">
      <div class="section-heading reveal-block">
        <span>近期活动</span>
        <h2>一起，把想法变成行动。</h2>
        <p>聚焦正在发生的活动，也留下每一次共同创造的轨迹。</p>
      </div>

      <div v-if="activities.length" ref="showcaseRef" class="activity-showcase">
        <router-link
          class="activity-feature"
          :to="`/activities/${featuredActivity.id}`"
          :aria-label="`查看活动：${featuredActivity.title}`"
        >
          <div
            class="activity-feature__media"
            :class="{ 'is-fallback': !featuredActivity.coverUrl }"
          >
            <img
              v-if="featuredActivity.coverUrl"
              :src="featuredActivity.coverUrl"
              :alt="featuredActivity.title"
              decoding="async"
              loading="lazy"
            />
            <span v-else>{{ featuredActivity.date || 'OPENATOM' }}</span>
          </div>
          <div class="activity-feature__content">
            <div class="activity-feature__meta">
              <time>{{ featuredActivity.date }}</time
              ><span>本期聚焦</span>
            </div>
            <h3>{{ featuredActivity.title }}</h3>
            <p>{{ featuredActivity.description || '查看活动详情与最新进展。' }}</p>
            <strong>查看活动 <span aria-hidden="true">↗</span></strong>
          </div>
        </router-link>

        <div v-if="moreActivities.length" class="activity-timeline" aria-label="更多近期活动">
          <router-link
            v-for="(activity, index) in moreActivities"
            :key="activity.id || activity.title"
            class="activity-timeline__item"
            :to="`/activities/${activity.id}`"
          >
            <span class="activity-timeline__index">{{ formatIndex(index + 2) }}</span>
            <div>
              <time>{{ activity.date }}</time>
              <h3>{{ activity.title }}</h3>
              <p>{{ activity.description || '查看活动详情' }}</p>
            </div>
            <span class="activity-timeline__arrow" aria-hidden="true">→</span>
          </router-link>
          <router-link class="activity-timeline__all" to="/activities"
            >查看全部活动 <span aria-hidden="true">→</span></router-link
          >
        </div>
      </div>
      <el-empty v-if="!activities.length && !loading" description="暂无活动数据" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'

const props = defineProps<{ activities: any[]; loading: boolean }>()
const showcaseRef = ref<HTMLElement>()
const featuredActivity = computed(() => props.activities[0] || {})
const moreActivities = computed(() => props.activities.slice(1, 5))
let observer: IntersectionObserver | undefined

function formatIndex(index: number) {
  return String(index).padStart(2, '0')
}
function observeShowcase() {
  observer?.disconnect()
  const showcase = showcaseRef.value
  if (!showcase || !('IntersectionObserver' in window)) {
    showcase?.classList.add('is-visible')
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        showcase.classList.add('is-visible')
        observer?.disconnect()
      }
    },
    { rootMargin: '80px 0px', threshold: 0.08 },
  )
  observer.observe(showcase)
}
watch(() => props.activities.length, observeShowcase, { flush: 'post' })
onMounted(observeShowcase)
onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.activity-showcase {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.85fr);
  gap: 24px;
  margin-top: 48px;
  opacity: 0;
  transform: translateY(28px);
  transition:
    opacity 0.7s ease,
    transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}
.activity-showcase.is-visible {
  opacity: 1;
  transform: none;
}
.activity-feature {
  display: grid;
  min-height: 560px;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 24px;
  background: var(--oa-elevated-bg);
  color: inherit;
  text-decoration: none;
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
}
.activity-feature:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}
.activity-feature__media {
  min-height: 320px;
  overflow: hidden;
  background: linear-gradient(145deg, var(--oa-elevated-bg), var(--oa-page-soft-bg));
}
.activity-feature__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}
.activity-feature:hover .activity-feature__media img {
  transform: scale(1.025);
}
.activity-feature__media.is-fallback {
  display: grid;
  place-items: center;
  color: var(--oa-muted);
  font-size: clamp(32px, 6vw, 72px);
  font-weight: 700;
  letter-spacing: -0.04em;
}
.activity-feature__content {
  display: grid;
  align-content: start;
  gap: 16px;
  padding: 30px 32px 34px;
}
.activity-feature__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--oa-muted);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.activity-feature h3 {
  margin: 0;
  color: var(--oa-text);
  font-size: clamp(28px, 3.4vw, 48px);
  font-weight: 650;
  line-height: 1.08;
  letter-spacing: -0.025em;
}
.activity-feature p {
  max-width: 680px;
  margin: 0;
  color: var(--oa-muted);
  font-size: 16px;
  line-height: 1.7;
}
.activity-feature strong {
  color: var(--oa-text);
  font-size: 14px;
  font-weight: 600;
}
.activity-timeline {
  display: flex;
  min-width: 0;
  flex-direction: column;
  border-top: 1px solid var(--oa-border);
}
.activity-timeline__item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 24px;
  gap: 14px;
  align-items: start;
  padding: 24px 4px;
  border-bottom: 1px solid var(--oa-border);
  color: inherit;
  text-decoration: none;
  transition:
    padding-left 0.25s ease,
    background-color 0.25s ease;
}
.activity-timeline__item:hover {
  padding-left: 12px;
  background: color-mix(in srgb, var(--oa-elevated-bg) 72%, transparent);
}
.activity-timeline__index,
.activity-timeline time {
  color: var(--oa-faint);
  font-size: 11px;
  letter-spacing: 0.12em;
}
.activity-timeline h3 {
  margin: 7px 0 6px;
  color: var(--oa-text);
  font-size: 18px;
  line-height: 1.3;
}
.activity-timeline p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.activity-timeline__arrow {
  color: var(--oa-muted);
  transition:
    transform 0.2s ease,
    color 0.2s ease;
}
.activity-timeline__item:hover .activity-timeline__arrow {
  color: var(--oa-text);
  transform: translateX(3px);
}
.activity-timeline__all {
  align-self: flex-start;
  margin-top: 24px;
  color: var(--oa-text);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
}
.activity-feature:focus-visible,
.activity-timeline__item:focus-visible,
.activity-timeline__all:focus-visible {
  outline: 2px solid var(--oa-text);
  outline-offset: 4px;
}
@media (max-width: 900px) {
  .activity-showcase {
    grid-template-columns: 1fr;
  }
  .activity-feature {
    min-height: 0;
  }
}
@media (max-width: 640px) {
  .activity-showcase {
    gap: 32px;
    margin-top: 32px;
  }
  .activity-feature {
    border-radius: 18px;
  }
  .activity-feature__media {
    min-height: 230px;
  }
  .activity-feature__content {
    padding: 24px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .activity-showcase,
  .activity-feature,
  .activity-feature__media img,
  .activity-timeline__item,
  .activity-timeline__arrow {
    transition: none;
  }
}
</style>
