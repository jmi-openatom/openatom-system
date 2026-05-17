<template>
  <ViewPage class="activities-page" :loading="loading">
    <section class="activities-hero">
      <div class="container activities-hero__inner">
        <div>
          <el-tag effect="plain">社团活动</el-tag>
          <h1>活动日程</h1>
          <p>
            讲座、工作坊、项目冲刺和竞赛复盘都会在这里发布；需要报名的活动可直接进入详情填写表单。
          </p>
        </div>
      </div>
    </section>

    <section class="container activities-list">
      <article
        v-for="item in rows"
        :key="item.id"
        class="activity-row"
        @click="$router.push(`/activities/${item.id}`)"
      >
        <div class="activity-row__date">
          <strong>{{ day(item.activityAt) }}</strong>
          <span>{{ month(item.activityAt) }}</span>
        </div>
        <div class="activity-row__body">
          <div class="activity-row__meta">
            <el-tag :type="item.registrationRequired ? 'success' : 'info'" effect="plain">
              {{ item.registrationRequired ? '官网报名' : '无需报名' }}
            </el-tag>
            <span>{{ item.location || '地点待定' }}</span>
          </div>
          <h2>{{ item.title }}</h2>
          <p>{{ item.summary || '暂无活动摘要' }}</p>
        </div>
        <el-button type="primary" plain>查看详情</el-button>
      </article>
      <el-empty v-if="!rows.length && !loading" description="暂无已发布活动" />
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { siteApi } from '@/api/index.ts'
import { formatDateTime } from '@/utils/format.ts'
import { onMounted, ref } from 'vue'

const loading = ref(false)

const rows = ref<any[]>([])

async function fetchList() {
  loading.value = true
  try {
    rows.value = (await siteApi.activities()) || []
  } finally {
    loading.value = false
  }
}

function day(value: any) {
  if (!value) return '--'
  return new Date(value).getDate().toString().padStart(2, '0')
}

function month(value: any) {
  if (!value) return '待定'
  return `${new Date(value).getMonth() + 1}月`
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.activities-page {
  min-height: calc(100vh - 44px);
  background: var(--oa-page-soft-bg);
}

.activities-hero {
  background: var(--oa-elevated-bg);
  border-bottom: 0;
}

.activities-hero__inner {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.activities-hero h1 {
  margin: 18px 0 12px;
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

.activities-hero p {
  max-width: 720px;
  margin: 0;
  color: var(--oa-muted);
  font-size: 24px;
  font-weight: 300;
  line-height: 1.5;
}

.activities-list {
  width: min(980px, calc(100% - 48px));
  padding: 64px 0 80px;
}

.activity-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) auto;
  align-items: center;
  gap: 22px;
  padding: 24px;
  margin-bottom: 1px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  box-shadow: none;
  backdrop-filter: none;
  cursor: pointer;
  animation: oaFadeUp 0.44s ease both;
  transition: border-color 0.2s ease;
}

.activity-row:hover {
  transform: none;
  border-color: rgba(29, 29, 31, 0.95);
  box-shadow: none;
}

.activity-row__date {
  display: grid;
  height: 74px;
  place-items: center;
  background: var(--oa-page-soft-bg);
  border-radius: 8px;
  color: var(--oa-primary-dark);
}

.activity-row__date strong {
  font-size: 30px;
  line-height: 1;
}

.activity-row__date span {
  color: var(--oa-muted);
}

.activity-row__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--oa-muted);
  font-size: 13px;
}

.activity-row h2 {
  margin: 10px 0 8px;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0;
}

.activity-row p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

@media (max-width: 720px) {
  .activity-row {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .activity-row .el-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
