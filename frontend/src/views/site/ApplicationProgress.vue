<template>
  <div class="site-page">
    <section class="container progress-page">
      <div class="progress-header">
        <div>
          <h1 class="page-title">我的入会申请</h1>
          <p class="section-subtitle">查看你提交的所有申请、面试安排和当前处理阶段。</p>
        </div>
        <div class="progress-header__actions">
          <el-button v-if="!isLogin" type="primary" @click="goLogin">登录后查看</el-button>
          <el-button v-else @click="fetchProgress">刷新进度</el-button>
        </div>
      </div>

      <el-alert
        v-if="isLogin"
        type="info"
        show-icon
        :closable="false"
        :title="`当前账号：${displayName}`"
        description="申请进度、面试时间和结果会在这里持续更新。"
      />

      <el-empty v-if="!isLogin" description="登录后可查看个人入会进度" />
      <el-empty v-else-if="!loading && !applications.length" description="你还没有提交入会申请" />

      <div v-else v-loading="loading" class="progress-list">
        <el-card v-for="item in applications" :key="item.id" shadow="never" class="progress-card">
          <template #header>
            <div class="progress-card__header">
              <div>
                <div class="progress-card__title">{{ item.campaignName || '入会申请' }}</div>
                <div class="progress-card__meta">
                  <span>社团：{{ item.clubName || '-' }}</span>
                  <span>申请编号：{{ item.id }}</span>
                  <span>提交时间：{{ formatDateTime(item.createdAt) }}</span>
                </div>
              </div>
              <el-tag :type="statusType(item.status)">{{ applicationStatusText(item.status) }}</el-tag>
            </div>
          </template>

          <div class="progress-summary">
            <div class="summary-item">
              <span class="summary-item__label">申请人</span>
              <strong>{{ item.applicantName || '-' }}</strong>
            </div>
            <div class="summary-item">
              <span class="summary-item__label">志愿部门</span>
              <strong>{{ item.preferredDepartment || '未填写' }}</strong>
            </div>
            <div class="summary-item">
              <span class="summary-item__label">当前说明</span>
              <strong>{{ applicationStatusHint(item.status) }}</strong>
            </div>
          </div>

          <el-steps :active="applicationStep(item.status)" finish-status="success" align-center class="progress-steps">
            <el-step title="已提交" />
            <el-step title="审核中" />
            <el-step title="面试阶段" />
            <el-step title="结果通知" />
          </el-steps>

          <el-divider content-position="left">面试安排</el-divider>
          <div v-if="item.interviews?.length" class="interview-list">
            <div v-for="interview in item.interviews" :key="interview.id" class="interview-item">
              <div class="interview-item__top">
                <strong>第 {{ interview.round || 1 }} 轮面试</strong>
                <el-tag :type="statusType(interview.status)">{{ interviewStatusText(interview.status) }}</el-tag>
              </div>
              <div class="interview-item__meta">
                <span>时间：{{ formatRange(interview.scheduledStartAt, interview.scheduledEndAt) }}</span>
                <span>方式：{{ interviewModeText(interview.mode) }}</span>
                <span>地点/链接：{{ interview.location || '-' }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂未安排面试，请留意后续通知" :image-size="72" />
        </el-card>
      </div>
    </section>
  </div>
</template>

<script>
import { authApi, siteApi } from '../../api'
import { getCurrentUser, getToken, setSession } from '../../utils/auth'
import { formatDateTime, statusType } from '../../utils/format'

export default {
  name: 'SiteApplicationProgress',
  data() {
    return {
      loading: false,
      user: getCurrentUser(),
      applications: []
    }
  },
  computed: {
    isLogin() {
      return Boolean(getToken())
    },
    displayName() {
      return this.user.realName || this.user.userName || '当前用户'
    }
  },
  created() {
    if (this.isLogin) {
      this.fetchCurrentUser()
      this.fetchProgress()
    }
  },
  methods: {
    formatDateTime,
    statusType,
    async fetchCurrentUser() {
      const result = await authApi.me()
      this.user = result?.user || result || {}
      setSession({
        accessToken: getToken(),
        user: this.user,
        roles: result?.roles || [],
        permissions: result?.permissions || []
      })
    },
    async fetchProgress() {
      if (!this.isLogin) return
      this.loading = true
      try {
        const result = await siteApi.progress()
        this.applications = result?.applications || []
      } finally {
        this.loading = false
      }
    },
    goLogin() {
      this.$router.push({ path: '/admin/login', query: { redirect: this.$route.fullPath } })
    },
    applicationStatusText(status) {
      return (
        {
          draft: '草稿',
          submitted: '已提交',
          reviewing: '审核中',
          approved: '初审通过',
          interview_scheduled: '已安排面试',
          interviewed: '已完成面试',
          final_approved: '已通过',
          rejected: '未通过',
          cancelled: '已撤回'
        }[status] || status || '-'
      )
    },
    interviewStatusText(status) {
      return (
        {
          pending: '待确认',
          confirmed: '已确认',
          completed: '已完成'
        }[status] || status || '-'
      )
    },
    interviewModeText(mode) {
      return (
        {
          offline: '线下',
          online: '线上'
        }[mode] || mode || '未设置'
      )
    },
    applicationStatusHint(status) {
      return (
        {
          draft: '申请还未正式提交',
          submitted: '申请已提交，等待审核',
          reviewing: '管理员正在审核你的申请材料',
          approved: '申请已通过初筛，等待安排面试',
          interview_scheduled: '已安排面试，请按时参加',
          interviewed: '面试已完成，等待最终结果',
          final_approved: '恭喜，你已通过入会流程',
          rejected: '本次申请未通过，可关注后续批次',
          cancelled: '你已主动撤回该申请'
        }[status] || '请留意后续通知'
      )
    },
    applicationStep(status) {
      return (
        {
          draft: 0,
          submitted: 1,
          reviewing: 1,
          approved: 2,
          interview_scheduled: 3,
          interviewed: 3,
          final_approved: 4,
          rejected: 4,
          cancelled: 4
        }[status] ?? 1
      )
    },
    formatRange(startAt, endAt) {
      return `${formatDateTime(startAt) || '-'} 至 ${formatDateTime(endAt) || '-'}`
    }
  }
}
</script>

<style scoped>
.site-page {
  padding: 34px 0;
}

.progress-page {
  display: grid;
  gap: 18px;
}

.progress-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.progress-header__actions {
  display: flex;
  gap: 10px;
}

.progress-list {
  display: grid;
  gap: 18px;
}

.progress-card {
  border-radius: 22px;
}

.progress-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.progress-card__title {
  font-size: 18px;
  font-weight: 700;
}

.progress-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-top: 8px;
  color: var(--oa-muted);
  font-size: 13px;
}

.progress-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.summary-item {
  padding: 14px 16px;
  background: #f7fbff;
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 16px;
}

.summary-item__label {
  display: block;
  margin-bottom: 8px;
  color: var(--oa-muted);
  font-size: 13px;
}

.progress-steps {
  margin: 6px 0 18px;
}

.interview-list {
  display: grid;
  gap: 12px;
}

.interview-item {
  padding: 16px;
  background: #f7fbff;
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 16px;
}

.interview-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.interview-item__meta {
  display: grid;
  gap: 8px;
  color: #475569;
}

@media (max-width: 900px) {
  .progress-header,
  .progress-card__header {
    flex-direction: column;
  }

  .progress-summary {
    grid-template-columns: 1fr;
  }
}
</style>
