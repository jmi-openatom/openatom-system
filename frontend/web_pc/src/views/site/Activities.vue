<template>
  <div class="activities-page" v-loading="loading">
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
  </div>
</template>

<script>
import { siteApi } from '@/api/index.ts'
import { formatDateTime } from '@/utils/format.ts'

export default {
  name: 'SiteActivities',
  data() {
    return {
      loading: false,
      rows: [],
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        this.rows = (await siteApi.activities()) || []
      } finally {
        this.loading = false
      }
    },
    day(value) {
      if (!value) return '--'
      return new Date(value).getDate().toString().padStart(2, '0')
    },
    month(value) {
      if (!value) return '待定'
      return `${new Date(value).getMonth() + 1}月`
    },
    formatDateTime,
  },
}
</script>

<style scoped>
.activities-page {
  min-height: 70vh;
  background: transparent;
}

.activities-hero {
  background:
    radial-gradient(circle at left top, rgba(147, 197, 253, 0.3), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(240, 247, 255, 0.96));
  border-bottom: 1px solid rgba(219, 230, 245, 0.9);
}

.activities-hero__inner {
  min-height: 260px;
  display: flex;
  align-items: center;
}

.activities-hero h1 {
  margin: 18px 0 12px;
  font-size: 48px;
  letter-spacing: -0.03em;
}

.activities-hero p {
  max-width: 720px;
  margin: 0;
  color: #475569;
  line-height: 1.8;
}

.activities-list {
  padding: 34px 0 56px;
}

.activity-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) auto;
  align-items: center;
  gap: 22px;
  padding: 22px;
  margin-bottom: 14px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 22px;
  box-shadow: var(--oa-shadow);
  backdrop-filter: blur(14px);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.activity-row:hover {
  transform: translateY(-3px);
  border-color: rgba(147, 197, 253, 0.95);
  box-shadow: 0 18px 36px rgba(37, 99, 235, 0.12);
}

.activity-row__date {
  display: grid;
  height: 74px;
  place-items: center;
  background: #eff6ff;
  border-radius: 18px;
  color: var(--oa-primary-dark);
}

.activity-row__date strong {
  font-size: 30px;
  line-height: 1;
}

.activity-row__date span {
  color: #475569;
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
  font-size: 22px;
  letter-spacing: -0.02em;
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
