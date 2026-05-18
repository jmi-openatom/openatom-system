<template>
  <ViewPage class="workspace-page application-workspace">
    <WorkspaceHero
      eyebrow="个人工作台"
      title="我的入会申请"
      description="查看你提交的所有申请、面试安排和当前处理阶段。"
      :metrics="workspaceMetrics"
    >
      <template #actions>
        <div class="progress-header__actions">
          <el-button v-if="!isLogin" type="primary" @click="goLogin">登录后查看</el-button>
          <el-button v-else plain @click="fetchProgress">刷新进度</el-button>
        </div>
      </template>
    </WorkspaceHero>

    <section class="workspace-section">
      <div class="container workspace-layout progress-page">
        <div class="workspace-grid workspace-grid--split">
          <WorkspacePanel
            class="status-console site-reveal"
            eyebrow="Account"
            title="当前账号"
            description="申请流转、面试安排和最终结果都会回流到这里。"
          >
            <div class="account-console">
              <span class="workspace-chip">{{ isLogin ? '已接入工作台' : '访客模式' }}</span>
              <strong>{{ isLogin ? displayName : '尚未登录' }}</strong>
              <p>
                {{
                  isLogin
                    ? '系统会持续同步你提交过的申请与后续安排。'
                    : '登录后可查看个人入会进度、面试通知和审核结果。'
                }}
              </p>
            </div>
          </WorkspacePanel>

          <WorkspacePanel
            class="pipeline-console site-reveal"
            eyebrow="Pipeline"
            title="流程总览"
            description="把最新状态、待处理事项和最近提交时间集中放在一屏。"
          >
            <div class="pipeline-console__grid">
              <div class="workspace-inline-stat">
                <span>最新阶段</span>
                <strong>{{ latestStatusLabel }}</strong>
              </div>
              <div class="workspace-inline-stat">
                <span>下一步</span>
                <strong>{{ latestStatusHint }}</strong>
              </div>
              <div class="workspace-inline-stat">
                <span>最近提交</span>
                <strong>{{ latestSubmittedAt }}</strong>
              </div>
            </div>
          </WorkspacePanel>
        </div>

        <WorkspacePanel
          class="application-records site-reveal"
          eyebrow="Records"
          title="申请记录"
          description="每一张卡片都对应一条完整申请链路。"
        >
          <el-empty
            v-if="!isLogin"
            class="site-system-empty"
            description="登录后可查看个人入会进度"
          />
          <el-empty
            v-else-if="!loading && !applications.length"
            class="site-system-empty"
            description="你还没有提交入会申请"
          />

          <div v-else v-loading="loading" class="progress-list">
            <el-card
              v-for="item in applications"
              :key="item.id"
              shadow="never"
              class="progress-card"
            >
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
                <div
                  v-for="interview in item.interviews"
                  :key="interview.id"
                  class="interview-item"
                >
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
        </WorkspacePanel>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import WorkspaceHero from '@/components/site/workspace/WorkspaceHero.vue'
import WorkspacePanel from '@/components/site/workspace/WorkspacePanel.vue'
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

const activeStatuses = new Set([
  'submitted',
  'reviewing',
  'approved',
  'pre_screen_passed',
  'interview_scheduled',
  'interviewed',
  'waitlisted',
])

const archivedStatuses = new Set([
  'pre_screen_rejected',
  'final_approved',
  'rejected',
  'cancelled',
])

const activeApplicationCount = computed(() =>
  applications.value.filter((item) => activeStatuses.has(item.status)).length,
)

const archivedApplicationCount = computed(() =>
  applications.value.filter((item) => archivedStatuses.has(item.status)).length,
)

const interviewCount = computed(() =>
  applications.value.reduce((total, item) => total + (item.interviews?.length || 0), 0),
)

const latestApplication = computed(() => {
  return [...applications.value].sort((a, b) => {
    return new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
  })[0]
})

const latestStatusLabel = computed(() => {
  if (!latestApplication.value) return isLogin.value ? '暂无申请' : '等待登录'
  return applicationStatusText(latestApplication.value.status)
})

const latestStatusHint = computed(() => {
  if (!latestApplication.value) return isLogin.value ? '可前往招新页提交申请' : '登录后同步'
  return applicationStatusHint(latestApplication.value.status)
})

const latestSubmittedAt = computed(() => {
  if (!latestApplication.value?.createdAt) return '--'
  return formatDateTime(latestApplication.value.createdAt)
})

const workspaceMetrics = computed(() => [
  {
    label: '申请总数',
    value: applications.value.length,
    note: isLogin.value ? '全部记录' : '登录后同步',
  },
  {
    label: '处理中',
    value: activeApplicationCount.value,
    note: '仍在流转',
  },
  {
    label: '面试安排',
    value: interviewCount.value,
    note: '已创建场次',
  },
  {
    label: '已归档',
    value: archivedApplicationCount.value,
    note: '结果已明确',
  },
])

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
.progress-page {
  gap: 24px;
}

.progress-header__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.account-console {
  display: grid;
  gap: 14px;
}

.account-console strong {
  color: var(--oa-text);
  font-size: clamp(24px, 2vw, 30px);
  font-weight: 600;
}

.account-console p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.8;
}

.pipeline-console__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
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
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
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
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
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
  color: var(--oa-muted);
}

@media (max-width: 900px) {
  .pipeline-console__grid {
    grid-template-columns: 1fr;
  }

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
  background: var(--oa-elevated-bg);
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  color: var(--oa-muted);
  border: 1px solid var(--oa-border);
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
  background: var(--oa-border);
}

.record-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: var(--oa-border);
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
  color: var(--oa-muted);
  background: var(--oa-page-soft-bg);
  padding: 8px 12px;
  border-radius: 8px;
  margin: 0;
}
</style>
