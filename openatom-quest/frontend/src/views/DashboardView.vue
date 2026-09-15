<template>
  <div class="page-container dashboard-page">
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-result v-else-if="error" icon="error" title="工作台加载失败" :sub-title="error"><template #extra><el-button type="primary" @click="load">重新加载</el-button></template></el-result>
    <template v-else-if="dashboard">
      <section class="welcome-card">
        <div><p class="eyebrow eyebrow--inverse">YOUR NEXT STEP</p><h1>{{ greeting }}，{{ auth.member?.nickname || '新同学' }}</h1><p>{{ nextTask ? `建议从「${nextTask.title}」开始，完成后即可推进成长路线。` : '当前任务已完成，前往成长路线查看下一阶段。' }}</p></div>
        <router-link :to="nextTask ? `/tasks/${nextTask.id}` : '/routes'"><el-button type="primary" size="large">{{ nextTask ? '查看下一项任务' : '查看成长路线' }}</el-button></router-link>
      </section>

      <section class="metric-grid" aria-label="成长概览">
        <article><span>当前等级</span><strong>{{ dashboard.level }}</strong><small>持续贡献，逐级成长</small></article>
        <article><span>成长积分</span><strong>{{ dashboard.points }}</strong><small>只记录学习成果与贡献</small></article>
        <article><span>待修改任务</span><strong>{{ Number(dashboard.assignments.revisionRequired || 0) }}</strong><small>优先根据导师反馈修改</small></article>
        <article><span>进行中任务</span><strong>{{ Number(dashboard.assignments.inProgress || 0) }}</strong><small>{{ Number(dashboard.assignments.pendingReview || 0) }} 项等待审核</small></article>
      </section>

      <section class="dashboard-grid">
        <article v-if="dashboard.announcements?.length" class="content-card content-card--wide announcement-card">
          <div class="section-heading"><div><p class="eyebrow">COMMUNITY UPDATE</p><h2>社团公告</h2></div></div>
          <div class="mini-list"><div v-for="item in dashboard.announcements" :key="item.id"><strong>{{ item.title }}</strong><p>{{ item.content }}</p><small>{{ formatDate(item.publishedAt) }}</small></div></div>
        </article>
        <article class="content-card content-card--wide">
          <div class="section-heading"><div><p class="eyebrow">ACTIVE QUESTS</p><h2>当前任务</h2></div><router-link to="/assignments">查看全部</router-link></div>
          <div v-if="assignments.length" class="compact-list"><router-link v-for="item in assignments.slice(0, 5)" :key="item.id" :to="`/assignments/${item.id}`"><span class="status-dot" :data-status="item.status" /><div><strong>{{ item.title }}</strong><small>{{ statusText[item.status] || item.status }} · {{ item.points }} 积分</small></div><span>→</span></router-link></div>
          <div v-else class="empty-state"><span class="empty-state__mark">01</span><div><strong>领取第一个成长任务</strong><p>任务会明确说明目标、操作步骤、提交要求与验收标准。</p></div><router-link to="/tasks"><el-button plain>发现任务</el-button></router-link></div>
        </article>

        <article class="content-card">
          <div class="section-heading"><div><p class="eyebrow">FEEDBACK</p><h2>最近反馈</h2></div></div>
          <div v-if="dashboard.feedback.length" class="mini-list"><router-link v-for="item in dashboard.feedback" :key="`${item.assignmentId}-${item.reviewedAt}`" :to="`/assignments/${item.assignmentId}`"><strong>{{ item.title }}</strong><p>{{ item.comment }}</p></router-link></div>
          <div v-else class="empty-copy">完成首次任务提交后，导师反馈会显示在这里。</div>
        </article>

        <article class="content-card">
          <div class="section-heading"><div><p class="eyebrow">DEADLINES</p><h2>即将截止</h2></div></div>
          <div v-if="dashboard.upcoming.length" class="mini-list"><router-link v-for="item in dashboard.upcoming" :key="item.assignmentId" :to="`/assignments/${item.assignmentId}`"><strong>{{ item.title }}</strong><p>{{ formatDate(item.dueAt) }}</p></router-link></div>
          <div v-else class="empty-copy">未来 7 天没有即将截止的任务。</div>
        </article>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getErrorMessage } from '@/api/http'
import { getAssignments, getDashboard, getTasks, type Assignment, type DashboardData, type TaskSummary } from '@/api/quest'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const dashboard = ref<DashboardData | null>(null)
const assignments = ref<Assignment[]>([])
const nextTask = ref<TaskSummary | null>(null)
const loading = ref(true)
const error = ref('')
const greeting = computed(() => { const hour = new Date().getHours(); return hour < 6 ? '夜深了' : hour < 12 ? '上午好' : hour < 18 ? '下午好' : '晚上好' })
const statusText: Record<string, string> = { IN_PROGRESS: '进行中', PENDING_REVIEW: '待审核', REVISION_REQUIRED: '需修改', PASSED: '已通过', OVERDUE: '已逾期', ABANDONED: '已结束' }
function formatDate(value: string) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
async function load() {
  loading.value = true; error.value = ''
  try { const [summary, owned, tasks] = await Promise.all([getDashboard(), getAssignments(), getTasks()]); dashboard.value = summary; assignments.value = owned; nextTask.value = tasks.find((task) => task.memberStatus === 'REVISION_REQUIRED') || tasks.find((task) => task.memberStatus === 'IN_PROGRESS') || tasks.find((task) => task.memberStatus === 'NOT_STARTED' && task.prerequisitesMet) || null }
  catch (reason) { error.value = getErrorMessage(reason) }
  finally { loading.value = false }
}
onMounted(load)
</script>
