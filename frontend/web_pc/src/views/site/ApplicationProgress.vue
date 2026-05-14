<template>
  <ViewPage class="site-page">
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
              <el-tag :type="statusType(item.status)">{{
                applicationStatusText(item.status)
              }}</el-tag>
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

          <el-steps
            :active="applicationStep(item.status)"
            finish-status="success"
            align-center
            class="progress-steps"
          >
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
                <el-tag :type="statusType(interview.status)">{{
                  interviewStatusText(interview.status)
                }}</el-tag>
              </div>
              <div class="interview-item__meta">
                <span
                  >时间：{{
                    formatRange(interview.scheduledStartAt, interview.scheduledEndAt)
                  }}</span
                >
                <span>方式：{{ interviewModeText(interview.mode) }}</span>
                <span>地点/链接：{{ interview.location || '-' }}</span>
              </div>
              <div v-if="interview.feedbacks?.length" class="interview-feedback">
                <div v-for="fb in interview.feedbacks" :key="fb.id" class="feedback-bubble">
                  <p>{{ fb.suggestion || fb.comment || '面试官未填写具体评价' }}</p>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂未安排面试，请留意后续通知" :image-size="72" />

          <template v-if="item.approvalRecords?.length">
            <el-divider content-position="left">审核意见</el-divider>
            <div class="approval-history">
              <div
                v-for="record in item.approvalRecords"
                :key="record.id"
                class="approval-record"
                :class="`is-${record.action}`"
              >
                <div class="record-dot"></div>
                <div class="record-content">
                  <div class="record-title">
                    <span>{{ approvalActionText(record.action) }}</span>
                    <time>{{ formatDateTime(record.createdAt) }}</time>
                  </div>
                  <p v-if="record.comment" class="record-comment">{{ record.comment }}</p>
                </div>
              </div>
            </div>
          </template>
        </el-card>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { authApi, siteApi } from '@/api'
import { getCurrentUser, getToken, setSession } from '@/utils/auth.ts'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  applicationStatusText,
  formatDateTime,
  interviewStatusText,
  statusType,
} from '@/utils/format.ts'

const loading = ref(false)

const user = ref(getCurrentUser())

const applications = ref<any[]>([])

const router = useRouter()

const route = useRoute()

const isLogin = computed(() => {
  return Boolean(getToken())
})

const displayName = computed(() => {
  return user.value.realName || user.value.userName || '当前用户'
})

async function fetchCurrentUser() {
  const result = await authApi.me()
  user.value = result?.user || result || {}
  setSession({
    accessToken: getToken(),
    user: user.value,
    roles: result?.roles || [],
    permissions: result?.permissions || [],
  })
}

async function fetchProgress() {
  if (!isLogin.value) return
  loading.value = true
  try {
    const result = await siteApi.progress()
    applications.value = result?.applications || []
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push({ path: '/admin/login', query: { redirect: route.fullPath } })
}

function interviewModeText(mode: any) {
  return (
    {
      offline: '线下',
      online: '线上',
    }[mode] ||
    mode ||
    '未设置'
  )
}

function applicationStatusHint(status: any) {
  return (
    {
      draft: '申请还未正式提交',
      submitted: '申请已提交，等待审核',
      reviewing: '管理员正在审核你的申请材料',
      approved: '申请已通过初筛，等待安排面试',
      pre_screen_passed: '申请已通过初筛，等待安排面试',
      pre_screen_rejected: '初审未通过，可留意后续招新批次',
      interview_scheduled: '已安排面试，请按时参加',
      interviewed: '面试已完成，等待最终结果',
      final_approved: '恭喜，你已通过入会流程',
      waitlisted: '你已进入候补，请等待后续通知',
      rejected: '本次申请未通过，可关注后续批次',
      cancelled: '你已主动撤回该申请',
    }[status] || '请留意后续通知'
  )
}

function applicationStep(status: any) {
  return (
    {
      draft: 0,
      submitted: 1,
      reviewing: 1,
      approved: 2,
      pre_screen_passed: 2,
      pre_screen_rejected: 4,
      interview_scheduled: 3,
      interviewed: 3,
      final_approved: 4,
      waitlisted: 4,
      rejected: 4,
      cancelled: 4,
    }[status] ?? 1
  )
}

function formatRange(startAt: any, endAt: any) {
  return `${formatDateTime(startAt) || '-'} 至 ${formatDateTime(endAt) || '-'}`
}

function approvalActionText(action: any) {
  return (
    {
      approve: '审核通过',
      reject: '审核驳回',
      transfer: '申请转交',
      request_more_info: '补充材料',
    }[action] || action
  )
}

onMounted(() => {
  if (isLogin.value) {
    fetchCurrentUser()
    fetchProgress()
  }
})
</script>

<style scoped>
.site-page {
  padding: 64px 0 80px;
  background: #f5f5f7;
}

.progress-page {
  display: grid;
  gap: 20px;
}

.progress-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 48px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  animation: oaFadeUp 0.42s ease both;
}

.progress-header__actions {
  display: flex;
  gap: 10px;
}

.progress-list {
  display: grid;
  gap: 1px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  background: #e0e0e0;
}

.progress-card {
  border: 0;
  border-radius: 0;
  animation: oaFadeUp 0.44s ease both;
}

.progress-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.progress-card__title {
  font-size: 18px;
  font-weight: 600;
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
  background: #f5f5f7;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
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
  background: #f5f5f7;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
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
  color: #7a7a7a;
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

.interview-feedback {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed rgba(224, 224, 224, 0.6);
}

.feedback-bubble {
  background: #fff;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  color: #7a7a7a;
  border: 1px solid #e0e0e0;
}

.approval-history {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 10px 4px;
}

.approval-record {
  display: flex;
  gap: 16px;
  position: relative;
}

.approval-record:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 16px;
  bottom: -20px;
  width: 1px;
  background: #e0e0e0;
}

.record-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #e0e0e0;
  margin-top: 6px;
  flex-shrink: 0;
  z-index: 1;
}

.is-approve .record-dot {
  background: #1d1d1f;
  box-shadow: none;
}

.is-reject .record-dot {
  background: #1d1d1f;
  box-shadow: none;
}

.record-content {
  flex: 1;
}

.record-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.record-title span {
  font-weight: 600;
  font-size: 15px;
}

.record-title time {
  font-size: 12px;
  color: var(--oa-muted);
}

.record-comment {
  font-size: 14px;
  color: #7a7a7a;
  background: #f5f5f7;
  padding: 8px 12px;
  border-radius: 8px;
  margin: 0;
}
</style>
