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

      <!-- Marquee Carousel -->
      <div class="people-marquee-wrapper">
        <Marquee pause-on-hover class="[--duration:20s] [--gap:6rem]">
          <ReviewCard
            v-for="person in firstRow"
            :key="person.userId || person.name"
            :img="person.img"
            :name="person.name"
            :username="person.username"
            :body="person.body"
            style="margin-left: 10px;"
          />
        </Marquee>
        <Marquee reverse pause-on-hover class="[--duration:20s] [--gap:6rem]">
          <ReviewCard
            v-for="person in secondRow"
            :key="person.userId || person.name"
            :img="person.img"
            :name="person.name"
            :username="person.username"
            :body="person.body"
            style="margin-left: 10px;"
          />
        </Marquee>
        <div class="people-marquee-gradient people-marquee-gradient--left" />
        <div class="people-marquee-gradient people-marquee-gradient--right" />
      </div>

    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Marquee, ReviewCard } from '@/components/ui/marquee'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'

const props = defineProps<{
  people: any[]
  loading: boolean
}>()

const mappedPeople = computed(() =>
  props.people.map((person) => ({
    userId: person.userId,
    name: person.name,
    img: `https://avatar.vercel.sh/${encodeURIComponent(person.name || 'user')}`,
    username: person.role || '',
    body: person.focus || '',
  })),
)

const uniqueRoles = computed(() => {
  const roles = new Set(
    props.people
      .map((p) => p.role)
      .filter(Boolean),
  )
  return [...roles]
})

const advisors = computed(() =>
  props.people.filter(
    (p) => p.role && (p.role.includes('指导') || p.role.includes('老师')),
  ),
)

const firstRow = computed(() =>
  mappedPeople.value.slice(0, Math.ceil(mappedPeople.value.length / 2)),
)
const secondRow = computed(() =>
  mappedPeople.value.slice(Math.ceil(mappedPeople.value.length / 2)),
)
</script>

<style scoped>
.people-content {
  margin-top: 24px;
}

/* --- Stats Bar --- */
.people-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
  margin-bottom: 40px;
  padding: 18px 32px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
  border: 1px solid #e2e8f0;
}

:global(.dark) .people-stats {
  background: linear-gradient(135deg, #1e293b, #0f172a);
  border-color: #334155;
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
  background: #3b82f6;
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.4);
}

.people-stat-dot--green {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.4);
}

.people-stat-dot--purple {
  background: #8b5cf6;
  box-shadow: 0 0 8px rgba(139, 92, 246, 0.4);
}

.people-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1;
}

:global(.dark) .people-stat-value {
  color: #f1f5f9;
}

.people-stat-label {
  font-size: 13px;
  color: #64748b;
}

:global(.dark) .people-stat-label {
  color: #94a3b8;
}

.people-stat-divider {
  width: 1px;
  height: 28px;
  background: #cbd5e1;
  flex-shrink: 0;
}

:global(.dark) .people-stat-divider {
  background: #475569;
}

/* --- Marquee Wrapper --- */
.people-marquee-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 100%;
  margin-top: 8px;
}

.people-marquee-gradient {
  pointer-events: none;
  position: absolute;
  inset-y: 0;
  width: 33.333%;
  z-index: 1;
}

.people-marquee-gradient--left {
  left: 0;
  background: linear-gradient(to right, #f5f5f7, transparent);
}

.people-marquee-gradient--right {
  right: 0;
  background: linear-gradient(to left, #f5f5f7, transparent);
}

:global(.dark) .people-marquee-gradient--left {
  background: linear-gradient(to right, #0a0a0a, transparent);
}

:global(.dark) .people-marquee-gradient--right {
  background: linear-gradient(to left, #0a0a0a, transparent);
}

/* --- CTA --- */
.people-cta {
  margin-top: 36px;
  text-align: center;
  font-size: 14px;
  color: #64748b;
}

:global(.dark) .people-cta {
  color: #94a3b8;
}

.people-cta-link {
  color: #3b82f6;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s;
}

.people-cta-link:hover {
  color: #2563eb;
  text-decoration: underline;
}
</style>
