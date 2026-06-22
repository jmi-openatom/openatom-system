<template>
  <ViewPage class="activities-page" :loading="loading">
    <section class="activities-hero">
      <div class="container activities-hero__inner">
        <div class="activities-hero__copy">
          <span>社团活动</span>
          <h1>活动日程</h1>
          <p>
            讲座、工作坊、项目冲刺和竞赛复盘都会在这里发布。你可以先浏览活动，再进入详情查看安排或完成报名。
          </p>
        </div>

        <div v-if="rows.length" class="activities-hero__metrics" aria-label="活动概览">
          <article>
            <strong>{{ rows.length }}</strong>
            <span>已发布活动</span>
          </article>
          <article>
            <strong>{{ registrationCount }}</strong>
            <span>支持官网报名</span>
          </article>
          <article>
            <strong>{{ upcomingCount }}</strong>
            <span>待举办活动</span>
          </article>
        </div>
      </div>
    </section>

    <section v-if="featuredActivity" class="activities-featured-section">
      <div class="container activities-featured">
        <div class="section-heading">
          <span>活动焦点</span>
          <h2>重点活动</h2>
        </div>

        <article
          class="activities-featured-card"
          @click="$router.push(`/activities/${featuredActivity.id}`)"
        >
          <div
            class="activities-featured-card__media"
            :class="{ 'is-fallback': !featuredActivity.coverUrl }"
          >
            <img
              v-if="featuredActivity.coverUrl"
              :alt="featuredActivity.title"
              :src="featuredActivity.coverUrl"
            />
            <div v-else class="activities-featured-card__placeholder">
              <span>{{ monthDay(featuredActivity.activityAt) }}</span>
            </div>
          </div>

          <div class="activities-featured-card__content">
            <div class="activities-featured-card__meta">
              <el-tag
                :type="featuredActivity.registrationRequired ? 'success' : 'info'"
                effect="plain"
              >
                {{ featuredActivity.registrationRequired ? '官网报名' : '无需报名' }}
              </el-tag>
              <el-tag v-if="featuredActivity.participationPoints" type="warning" effect="plain">
                +{{ featuredActivity.participationPoints }} 积分
              </el-tag>
              <span>{{ featuredActivity.location || '地点待定' }}</span>
            </div>
            <h3>{{ featuredActivity.title }}</h3>
            <p>{{ featuredActivity.summary || '暂无活动摘要' }}</p>

            <div class="activities-featured-card__footer">
              <time>{{ formatDateTime(featuredActivity.activityAt) }}</time>
              <el-button type="primary">查看详情</el-button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-if="moreActivities.length || !featuredActivity" class="activities-archive-section">
      <div class="container activities-archive">
        <div class="section-heading">
          <span>{{ moreActivities.length ? '更多活动' : '活动列表' }}</span>
          <h2>{{ moreActivities.length ? '继续浏览' : '全部活动' }}</h2>
        </div>

        <div v-if="moreActivities.length" class="activity-list-grid">
          <article
            v-for="item in moreActivities"
            :key="item.id"
            class="activity-list-card"
            @click="$router.push(`/activities/${item.id}`)"
          >
            <div class="activity-list-card__media" :class="{ 'is-fallback': !item.coverUrl }">
              <img v-if="item.coverUrl" :alt="item.title" :src="item.coverUrl" />
              <div v-else class="activity-list-card__placeholder">
                <strong>{{ day(item.activityAt) }}</strong>
                <span>{{ month(item.activityAt) }}</span>
              </div>
            </div>

            <div class="activity-list-card__body">
              <div class="activity-list-card__meta">
                <el-tag :type="item.registrationRequired ? 'success' : 'info'" effect="plain">
                  {{ item.registrationRequired ? '官网报名' : '无需报名' }}
                </el-tag>
                <el-tag v-if="item.participationPoints" type="warning" effect="plain">
                  +{{ item.participationPoints }} 积分
                </el-tag>
                <span>{{ item.location || '地点待定' }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary || '暂无活动摘要' }}</p>
            </div>

            <footer>
              <time>{{ monthDay(item.activityAt) }}</time>
              <span>查看详情</span>
            </footer>
          </article>
        </div>

        <el-empty v-else-if="!loading" description="暂无已发布活动" />
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { siteApi } from '@/api/index.ts'
import { formatDateTime, monthDayParts } from '@/utils/format.ts'
import { computed, onMounted, ref } from 'vue'

const loading = ref(false)
const rows = ref<any[]>([])

const featuredActivity = computed(() => rows.value[0])
const moreActivities = computed(() => rows.value.slice(1))
const registrationCount = computed(
  () => rows.value.filter((item) => Boolean(item.registrationRequired)).length,
)
const upcomingCount = computed(() => {
  const now = Date.now()
  return rows.value.filter((item) => {
    const activityTime = new Date(item.activityAt).getTime()
    return Number.isFinite(activityTime) && activityTime >= now
  }).length
})

async function fetchList() {
  loading.value = true
  try {
    rows.value = (await siteApi.activities()) || []
  } finally {
    loading.value = false
  }
}

function day(value: any) {
  return monthDayParts(value)?.day || '--'
}

function month(value: any) {
  const parts = monthDayParts(value)
  return parts ? `${Number(parts.month)}月` : '待定'
}

function monthDay(value: any) {
  const parts = monthDayParts(value)
  return parts ? `${Number(parts.month)}月${Number(parts.day)}日` : '时间待定'
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.activities-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-elevated-bg);
}

.activities-hero {
  min-height: min(72vh, 760px);
  display: flex;
  align-items: center;
  background: linear-gradient(180deg, var(--oa-elevated-bg) 0%, var(--oa-page-soft-bg) 100%);
}

.activities-hero__inner {
  display: grid;
  justify-items: center;
  gap: 38px;
  padding: clamp(72px, 9vw, 120px) 0;
  text-align: center;
}

.activities-hero__copy {
  max-width: 860px;
}

.activities-hero__copy span,
.section-heading span {
  display: inline-block;
  color: var(--oa-text);
  font-size: 14px;
}

.activities-hero h1 {
  margin: 12px 0 16px;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: clamp(42px, 5vw, 64px);
  font-weight: 600;
  line-height: 1.05;
  letter-spacing: 0;
}

.activities-hero p {
  max-width: 760px;
  margin: 0;
  color: var(--oa-muted);
  font-size: clamp(18px, 2vw, 24px);
  font-weight: 300;
  line-height: 1.6;
}

.activities-hero__metrics {
  display: grid;
  width: min(760px, 100%);
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  border-top: 1px solid var(--oa-border);
}

.activities-hero__metrics article {
  display: grid;
  gap: 8px;
  min-height: 116px;
  align-content: center;
  padding: 20px 18px 0;
  border-left: 1px solid var(--oa-border);
}

.activities-hero__metrics article:first-child {
  border-left: 0;
}

.activities-hero__metrics strong {
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: clamp(34px, 4vw, 48px);
  font-weight: 600;
  line-height: 1;
}

.activities-hero__metrics span {
  color: var(--oa-muted);
  font-size: 14px;
}

.activities-featured-section {
  background: var(--oa-elevated-bg);
}

.activities-featured,
.activities-archive {
  padding: clamp(72px, 8vw, 96px) 0;
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
  background: linear-gradient(90deg, transparent, var(--oa-text), transparent);
  opacity: 0.78;
}

.section-heading h2 {
  margin: 10px 0 0;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: clamp(30px, 3vw, 40px);
  font-weight: 600;
  line-height: 1.1;
}

.activities-featured-card {
  display: grid;
  width: min(1180px, 100%);
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  min-height: 480px;
  margin: 36px auto 0;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 32px;
  background: var(--oa-elevated-bg);
  cursor: pointer;
}

.activities-featured-card__media {
  min-height: 360px;
  background: var(--oa-page-soft-bg);
}

.activities-featured-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: grayscale(0.08);
}

.activities-featured-card__media.is-fallback {
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 25% 25%, rgba(29, 29, 31, 0.14), transparent 38%),
    linear-gradient(180deg, var(--oa-page-soft-bg) 0%, var(--oa-elevated-bg) 100%);
}

.activities-featured-card__placeholder {
  display: grid;
  width: min(260px, calc(100% - 48px));
  aspect-ratio: 1;
  place-items: center;
  border: 1px solid var(--oa-border);
  border-radius: 999px;
  color: var(--oa-text);
  font-size: clamp(28px, 4vw, 44px);
}

.activities-featured-card__content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  padding: clamp(28px, 4vw, 44px);
}

.activities-featured-card__meta,
.activity-list-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  color: var(--oa-muted);
  font-size: 14px;
}

.activities-featured-card h3 {
  margin: 0;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: clamp(30px, 3.4vw, 44px);
  font-weight: 600;
  line-height: 1.1;
}

.activities-featured-card p,
.activity-list-card p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.8;
}

.activities-featured-card__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 8px;
}

.activities-featured-card time,
.activity-list-card time {
  color: var(--oa-text-soft);
  font-size: 14px;
}

.activities-archive-section {
  background: var(--oa-page-soft-bg);
}

.activity-list-grid {
  display: grid;
  width: min(1180px, 100%);
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin: 36px auto 0;
}

.activity-list-card {
  display: grid;
  min-height: 320px;
  grid-template-rows: 200px minmax(0, 1fr) auto;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 24px;
  background: var(--oa-elevated-bg);
  cursor: pointer;
}

.activity-list-card__media {
  background: var(--oa-button-subtle-bg);
}

.activity-list-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: grayscale(0.08);
}

.activity-list-card__media.is-fallback {
  display: grid;
  place-items: center;
}

.activity-list-card__placeholder {
  display: grid;
  gap: 4px;
  justify-items: center;
}

.activity-list-card__placeholder strong {
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: 40px;
  line-height: 1;
}

.activity-list-card__placeholder span {
  color: var(--oa-muted);
}

.activity-list-card__body {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 22px 22px 12px;
}

.activity-list-card h3 {
  margin: 0;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
}

.activity-list-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 22px 22px;
}

.activity-list-card footer span {
  color: var(--oa-text);
  font-size: 14px;
}

.activities-archive :deep(.el-empty) {
  width: min(440px, 100%);
  margin: 36px auto 0;
  padding: 28px 24px;
  border: 1px solid var(--oa-border);
  border-radius: 24px;
  background: var(--oa-elevated-bg);
}

@media (max-width: 900px) {
  .activities-featured-card {
    grid-template-columns: 1fr;
  }

  .activity-list-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .activities-hero__metrics {
    grid-template-columns: 1fr;
    border-top: 0;
  }

  .activities-hero__metrics article {
    min-height: 0;
    padding: 18px 0;
    border-top: 1px solid var(--oa-border);
    border-left: 0;
  }

  .activities-featured-card {
    border-radius: 24px;
  }

  .activities-featured-card__media {
    min-height: 260px;
  }

  .activities-featured-card__footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
