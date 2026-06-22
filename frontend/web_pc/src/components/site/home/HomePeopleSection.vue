<template>
  <section id="people" class="container section people-section home-interactive-section">
    <HomeInteractiveBackdrop :radius="200" :spacing="60" :strength="16" />
    <div class="section-heading reveal-block">
      <span>成员信息</span>
      <h2>我们的团队</h2>
      <p>来自不同专业背景的成员汇聚于此，共同推动社团发展。</p>
    </div>

    <el-empty v-if="!people.length && !loading" description="暂无主要人员数据" />

    <div v-if="people.length" class="people-content">
      <!-- Role Stats Bar -->
      <div class="people-stats">
        <div class="people-stat-item">
          <div class="people-stat-dot people-stat-dot--blue" />
          <span class="people-stat-value">{{ mappedPeople.length }}</span>
          <span class="people-stat-label">成员总数</span>
        </div>
        <div class="people-stat-divider" />
        <div class="people-stat-item">
          <div class="people-stat-dot people-stat-dot--green" />
          <span class="people-stat-value">{{ uniqueRoles.length }}</span>
          <span class="people-stat-label">职能方向</span>
        </div>
      </div>

      <div class="people-wall">
        <div class="people-wall__marquee" :style="marqueeStyle" role="list">
          <article
            v-for="(person, index) in loopedPeople"
            :key="`${person.copy}-${person.userId || person.name}`"
            class="people-person"
            role="listitem"
            :aria-hidden="person.copy > 0 ? 'true' : undefined"
          >
            <span class="people-person__index">{{ formatMemberIndex(person.sourceIndex) }}</span>

            <div class="people-person__portrait">
              <UserAvatar
                :fallback-src="person.qqAvatar"
                :name="person.name"
                :size="112"
                :src="person.avatar"
              />
            </div>

            <div class="people-person__meta">
              <strong>{{ person.name }}</strong>
              <span>{{ person.username || '成员' }}</span>
              <p>{{ person.body || '共同推动社团发展' }}</p>
            </div>
          </article>
        </div>

        <div class="people-wall__fade people-wall__fade--left" />
        <div class="people-wall__fade people-wall__fade--right" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'

const props = defineProps<{
  people: any[]
  loading: boolean
}>()

const mappedPeople = computed(() =>
  props.people.map((person) => ({
    userId: person.userId,
    name: person.name,
    avatar: person.avatar || '',
    qqAvatar: person.qqAvatar || '',
    username: person.role || '',
    body: person.focus || '',
  })),
)

const uniqueRoles = computed(() => {
  const roles = new Set(props.people.map((p) => p.role).filter(Boolean))
  return [...roles]
})

const loopedPeople = computed(() =>
  Array.from({ length: 3 }, (_, copy) =>
    mappedPeople.value.map((person, sourceIndex) => ({
      ...person,
      copy,
      sourceIndex,
    })),
  ).flat(),
)

const marqueeStyle = computed(() => ({
  '--people-cycle-width': `${mappedPeople.value.length * 232}px`,
}))

function formatMemberIndex(index: number) {
  return String(index + 1).padStart(2, '0')
}
</script>

<style scoped>
.people-content {
  margin-top: 24px;
}

/* --- Stats Bar --- */
.people-stats {
  display: flex;
  width: fit-content;
  max-width: 100%;
  align-items: center;
  justify-content: center;
  gap: 32px;
  margin: 0 auto 40px;
  padding: 18px 28px;
  border: 1px solid var(--oa-border);
  border-radius: 999px;
  background: var(--oa-surface);
}

.people-stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.people-stat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.people-stat-dot--blue {
  background: #1d1d1f;
  box-shadow: 0 0 8px rgba(29, 29, 31, 0.24);
}

.people-stat-dot--green {
  background: #6e6e73;
  box-shadow: 0 0 8px rgba(110, 110, 115, 0.24);
}

.people-stat-dot--purple {
  background: #a1a1a6;
  box-shadow: 0 0 8px rgba(161, 161, 166, 0.22);
}

.people-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--oa-text);
  line-height: 1;
}

.people-stat-label {
  font-size: 13px;
  color: var(--oa-muted);
}

.people-stat-divider {
  width: 1px;
  height: 28px;
  background: var(--oa-border);
  flex-shrink: 0;
}

.people-wall {
  position: relative;
  width: 100%;
  margin-top: 10px;
  overflow: hidden;
}

.people-wall__marquee {
  display: flex;
  align-items: stretch;
  gap: 0;
  width: max-content;
  animation: people-wall-flow 42s linear infinite;
  will-change: transform;
}

.people-wall:hover .people-wall__marquee {
  animation-play-state: paused;
}

.people-person {
  position: relative;
  display: grid;
  min-width: 232px;
  min-height: 360px;
  flex: 0 0 232px;
  align-content: space-between;
  gap: 20px;
  padding: 22px 24px 24px;
  border-right: 1px solid var(--oa-border);
  transition:
    flex-basis 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    min-width 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    background-color 0.28s ease;
}

@keyframes people-wall-flow {
  from {
    transform: translateX(0);
  }

  to {
    transform: translateX(calc(var(--people-cycle-width) * -1));
  }
}

.people-person:first-child {
  border-left: 1px solid var(--oa-border);
}

.people-person::before {
  position: absolute;
  top: 0;
  right: 24px;
  left: 24px;
  height: 1px;
  content: '';
  background: var(--oa-border);
  transition:
    right 0.32s ease,
    left 0.32s ease,
    background-color 0.32s ease;
}

.people-person__index {
  color: var(--oa-faint);
  font-size: 12px;
  letter-spacing: 0.24em;
}

.people-person__portrait {
  position: relative;
  display: grid;
  width: 112px;
  height: 112px;
  place-items: center;
  overflow: hidden;
  border-radius: 999px;
  background: var(--oa-elevated-bg);
  filter: saturate(1.04);
  transition:
    width 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    height 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.28s ease,
    transform 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.people-person__meta {
  display: grid;
  gap: 8px;
}

.people-person__meta strong {
  color: var(--oa-text);
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: 24px;
  font-weight: 600;
  line-height: 1;
}

.people-person__meta span {
  color: var(--oa-muted-strong);
  font-size: 14px;
}

.people-person__meta p {
  max-width: 180px;
  margin: 10px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.7;
}

.people-person:hover {
  min-width: 286px;
  flex-basis: 286px;
  background: color-mix(in srgb, var(--oa-elevated-bg) 72%, transparent);
}

.people-person:hover::before {
  right: 0;
  left: 0;
  background: var(--oa-text);
}

.people-person:hover .people-person__portrait {
  width: 148px;
  height: 148px;
  filter: saturate(1.08) brightness(1.03);
  transform: translateY(-6px);
}

.people-person:hover .people-person__portrait :deep(.user-avatar) {
  width: 148px;
  height: 148px;
}

.people-wall__fade {
  pointer-events: none;
  position: absolute;
  inset-y: 0;
  z-index: 2;
  width: min(18vw, 180px);
}

.people-wall__fade--left {
  left: 0;
  background: linear-gradient(to right, var(--oa-page-soft-bg) 0%, transparent 100%);
}

.people-wall__fade--right {
  right: 0;
  background: linear-gradient(to left, var(--oa-page-soft-bg) 0%, transparent 100%);
}

.people-wall::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 1px;
  content: '';
  background: var(--oa-border);
}

.people-wall::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 1px;
  content: '';
  background: var(--oa-border);
  z-index: 1;
}

/* --- CTA --- */
.people-cta {
  margin-top: 36px;
  text-align: center;
  font-size: 14px;
  color: var(--oa-muted);
}

.people-cta-link {
  color: var(--oa-text);
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s;
}

.people-cta-link:hover {
  color: var(--oa-primary-dark);
  text-decoration: underline;
}

@media (max-width: 640px) {
  .people-stats {
    width: 100%;
    justify-content: space-between;
    gap: 16px;
    padding: 16px;
    border-radius: 18px;
  }

  .people-person,
  .people-person:hover {
    min-width: 220px;
    min-height: 320px;
    flex-basis: 220px;
  }

  .people-person:hover .people-person__portrait {
    width: 112px;
    height: 112px;
    transform: none;
  }

  .people-person:hover .people-person__portrait :deep(.user-avatar) {
    width: 112px;
    height: 112px;
  }

  .people-wall__fade {
    width: 48px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .people-wall__marquee {
    animation: none;
  }
}
</style>
