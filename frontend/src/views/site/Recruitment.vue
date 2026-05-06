<template>
  <div class="site-page">
    <section class="container">
      <div class="toolbar">
        <div>
          <h1 class="page-title">入会申请</h1>
          <p class="section-subtitle">选择感兴趣的社团并填写招新表单，开启你的社团之旅。</p>
        </div>
        <div class="toolbar__filters">
          <el-select v-if="clubs.length > 1" v-model="selectedClubId" filterable placeholder="选择社团" style="width: 240px" @change="fetchCampaigns">
            <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
          </el-select>
          <el-button type="primary" :icon="Refresh" @click="loadAll">刷新</el-button>
        </div>
      </div>

      <el-empty v-if="!selectedClubId && !loading" description="请先选择社团" />
      <el-empty v-else-if="selectedClubId && !campaigns.length && !loading" :description="clubs.length > 1 ? '该社团暂无开放中的招新表单' : '当前暂无开放中的招新表单'" />
      <el-timeline v-else v-loading="loading">
        <el-timeline-item v-for="item in campaigns" :key="item.id" :timestamp="formatRange(item)" placement="top">
          <div class="campaign-item panel">
            <div>
              <h3>{{ item.name || '招新计划' }}</h3>
              <p>提交方式：{{ item.loginRequired ? '需要登录' : '支持匿名填写' }}</p>
            </div>
            <div class="campaign-item__action">
              <el-tag :type="statusType(item.status)">{{ statusText(item.status) }}</el-tag>
              <el-button type="primary" @click="$router.push(`/apply/${item.id}`)">
                立即填写
              </el-button>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </section>
  </div>
</template>

<script>
import { Refresh } from '@element-plus/icons-vue'
import { clubApi } from '../../api'
import { formatDateTime, statusType } from '../../utils/format'

export default {
  name: 'SiteRecruitment',
  data() {
    return {
      Refresh,
      loading: false,
      clubs: [],
      campaigns: [],
      selectedClubId: ''
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    statusType,
    async loadAll() {
      this.loading = true
      try {
        const result = await clubApi.list({ status: 'active' })
        this.clubs = result?.list || result || []
        if (!this.selectedClubId && this.clubs.length) {
          this.selectedClubId = this.clubs[0].id
        }
        if (this.selectedClubId) {
          await this.fetchCampaigns()
        }
      } finally {
        this.loading = false
      }
    },
    async fetchCampaigns() {
      if (!this.selectedClubId) return
      this.loading = true
      try {
        const result = await clubApi.campaigns(this.selectedClubId)
        const list = result?.list || result || []
        this.campaigns = list.filter((item) => ['open', 'published'].includes(item.status))
      } finally {
        this.loading = false
      }
    },
    formatRange(item) {
      return `${formatDateTime(item.applyStartAt)} - ${formatDateTime(item.applyEndAt)}`
    },
    statusText(status) {
      return { draft: '草稿', published: '已发布', open: '收集中', closed: '已结束', archived: '已归档' }[status] || status || '-'
    }
  }
}
</script>

<style scoped>
.site-page {
  padding: 34px 0;
}

.campaign-item {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 20px;
  border-radius: 22px;
}

.campaign-item h3 {
  margin: 0 0 8px;
}

.campaign-item p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.campaign-item__action {
  display: flex;
  flex: 0 0 150px;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.site-page :deep(.el-timeline-item__node) {
  background: var(--oa-primary);
}

.site-page :deep(.el-timeline-item__tail) {
  border-left-color: rgba(147, 197, 253, 0.8);
}
</style>
