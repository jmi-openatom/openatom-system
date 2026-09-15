<template>
  <div class="page-container detail-page">
    <el-skeleton v-if="loading" :rows="9" animated />
    <el-result v-else-if="error" icon="error" title="任务记录加载失败" :sub-title="error" />
    <template v-else-if="assignment && task">
      <nav class="breadcrumb"><router-link to="/assignments">我的任务</router-link><span>/</span><span>{{ task.title }}</span></nav>
      <header class="page-title-row assignment-heading">
        <div class="page-heading"><p class="eyebrow">{{ statusText[assignment.status] }}</p><h1>{{ task.title }}</h1><p>{{ task.summary }}</p></div>
        <div class="status-chip status-chip--large" :data-status="assignment.status">{{ statusText[assignment.status] }}</div>
      </header>
      <section class="detail-layout">
        <main class="detail-content">
          <article v-if="assignment.latestFeedback" class="feedback-banner"><strong>导师最新反馈</strong><p>{{ assignment.latestFeedback }}</p></article>
          <article v-if="canSubmit" class="content-card submission-form">
            <div class="section-heading"><div><p class="eyebrow">NEW SUBMISSION</p><h2>{{ assignment.latestVersion ? '重新提交新版本' : '提交成果' }}</h2></div><span>自动保存草稿 · 将创建 v{{ (assignment.latestVersion || 0) + 1 }}</span></div>
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="完成情况说明" required><el-input v-model="form.completionNote" type="textarea" :rows="5" placeholder="说明你完成了什么，以及如何验证结果" /></el-form-item>
              <div class="form-grid">
                <el-form-item label="仓库地址"><el-input v-model="form.repositoryUrl" placeholder="https://github.com/..." /></el-form-item>
                <el-form-item label="Pull Request 地址"><el-input v-model="form.pullRequestUrl" placeholder="https://github.com/.../pull/..." /></el-form-item>
                <el-form-item label="在线演示地址"><el-input v-model="form.demoUrl" placeholder="https://..." /></el-form-item>
                <el-form-item label="视频演示地址"><el-input v-model="form.videoUrl" placeholder="https://..." /></el-form-item>
              </div>
              <el-form-item label="遇到的问题与学习总结"><el-input v-model="form.problemsAndLearning" type="textarea" :rows="4" /></el-form-item>
              <el-form-item><el-checkbox v-model="form.aiUsed">本次成果使用了 AI 工具</el-checkbox></el-form-item>
              <el-form-item v-if="form.aiUsed" label="AI 参与的具体内容" required><el-input v-model="form.aiUsageDetail" type="textarea" :rows="3" placeholder="说明使用了什么工具、用于哪些环节、你如何验证结果" /></el-form-item>
              <div class="inline-actions"><el-button type="primary" size="large" :loading="submitting" @click="submit">提交审核</el-button><el-button @click="abandon">放弃任务</el-button></div>
            </el-form>
          </article>
          <article v-else class="content-card state-panel"><h2>{{ stateTitle }}</h2><p>{{ stateDescription }}</p></article>
        </main>
        <aside class="detail-aside">
          <section class="content-card fact-list">
            <div><span>当前版本</span><strong>v{{ assignment.latestVersion || 0 }}</strong></div>
            <div><span>任务积分</span><strong>{{ assignment.points }}</strong></div>
            <div><span>截止时间</span><strong>{{ formatDate(assignment.dueAt) }}</strong></div>
            <div><span>领取时间</span><strong>{{ formatDate(assignment.claimedAt) }}</strong></div>
          </section>
          <section class="content-card rich-section"><h2>提交历史</h2><div v-if="!history.length" class="empty-copy">还没有提交版本。</div><div v-for="item in history" :key="String(item.id)" class="history-row"><span>v{{ item.version }}</span><div><strong>{{ reviewText[String(item.reviewResult || item.status)] || item.status }}</strong><small>{{ formatDate(String(item.submittedAt)) }}</small><small v-if="item.appealStatus">申诉：{{ appealText[String(item.appealStatus)] || item.appealStatus }}<template v-if="item.appealResolution"> · {{ item.appealResolution }}</template></small><el-button v-else-if="item.reviewId && ['REVISION_REQUIRED','FAILED'].includes(String(item.reviewResult))" link type="primary" @click="appeal(item)">对本次审核发起申诉</el-button></div></div></section>
        </aside>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getErrorMessage } from '@/api/http'
import { abandonAssignment, createReviewAppeal, getAssignments, getSubmissionHistory, getTask, submitAssignment, type Assignment, type TaskDetail } from '@/api/quest'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const assignment = ref<Assignment | null>(null)
const task = ref<TaskDetail | null>(null)
const history = ref<Record<string, unknown>[]>([])
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const form = reactive({ completionNote: '', repositoryUrl: '', pullRequestUrl: '', demoUrl: '', videoUrl: '', problemsAndLearning: '', aiUsed: false, aiUsageDetail: '' })
const draftKey = computed(() => `quest-submission-draft:${auth.member?.id || 'current'}:${route.params.id}`)
const statusText: Record<string, string> = { IN_PROGRESS: '进行中', PENDING_REVIEW: '待审核', REVISION_REQUIRED: '需修改', PASSED: '已通过', OVERDUE: '已逾期', ABANDONED: '已结束' }
const reviewText: Record<string, string> = { SUBMITTED: '等待审核', REVIEWING: '审核中', REVISION_REQUIRED: '需修改', PASSED: '审核通过', FAILED: '未通过', SUPERSEDED: '已被新版本替代' }
const appealText: Record<string, string> = { PENDING: '待复核', UPHELD: '维持原结论', OVERTURNED: '原结论已撤销' }
const canSubmit = computed(() => ['IN_PROGRESS', 'REVISION_REQUIRED'].includes(assignment.value?.status || ''))
const stateTitle = computed(() => assignment.value?.status === 'PENDING_REVIEW' ? '成果正在等待导师审核' : assignment.value?.status === 'PASSED' ? '任务已经通过' : '当前任务不可提交')
const stateDescription = computed(() => assignment.value?.status === 'PENDING_REVIEW' ? '审核结果和修改建议会通过站内通知送达。' : assignment.value?.status === 'PASSED' ? '积分已发放，继续前往成长路线查看下一步。' : '如有疑问，请联系任务负责人。')

watch(() => form.aiUsed, (used) => { if (!used) form.aiUsageDetail = '' })
watch(form, (value) => {
  if (canSubmit.value) localStorage.setItem(draftKey.value, JSON.stringify(value))
}, { deep: true })

function formatDate(value?: string) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '无固定截止' }

async function load() {
  try {
    const id = Number(route.params.id)
    const all = await getAssignments()
    assignment.value = all.find((item) => item.id === id) || null
    if (!assignment.value) throw new Error('任务领取记录不存在')
    ;[task.value, history.value] = await Promise.all([getTask(assignment.value.taskId), getSubmissionHistory(id)])
    if (canSubmit.value) {
      const draft = localStorage.getItem(draftKey.value)
      if (draft) {
        try {
          Object.assign(form, JSON.parse(draft))
          ElMessage.info('已恢复未提交的成果草稿')
        } catch {
          localStorage.removeItem(draftKey.value)
        }
      }
    }
  } catch (reason) { error.value = getErrorMessage(reason) }
  finally { loading.value = false }
}

async function submit() {
  if (!assignment.value || !form.completionNote.trim()) return ElMessage.warning('请填写完成情况说明')
  if (form.aiUsed && !form.aiUsageDetail.trim()) return ElMessage.warning('请说明 AI 参与的具体内容')
  submitting.value = true
  try {
    const result = await submitAssignment(assignment.value.id, form)
    localStorage.removeItem(draftKey.value)
    ElMessage.success(`v${result.version} 已提交审核`)
    loading.value = true
    await load()
  } catch (reason) { ElMessage.error(getErrorMessage(reason, '提交失败')) }
  finally { submitting.value = false }
}

async function abandon() {
  if (!assignment.value) return
  try {
    await ElMessageBox.confirm('放弃后本次任务将结束，历史提交仍会保留。确认放弃吗？', '放弃任务', { type: 'warning' })
    await abandonAssignment(assignment.value.id)
    ElMessage.success('任务已放弃')
    loading.value = true
    await load()
  } catch (reason) { if (reason !== 'cancel' && reason !== 'close') ElMessage.error(getErrorMessage(reason, '放弃任务失败')) }
}

async function appeal(item: Record<string, unknown>) {
  try {
    const { value } = await ElMessageBox.prompt('说明你对审核结论的异议及希望复核的具体内容。', '发起审核申诉', {
      inputType: 'textarea', inputPattern: /\S{5,}/, inputErrorMessage: '请至少填写 5 个字符的申诉理由', confirmButtonText: '提交申诉'
    })
    await createReviewAppeal(Number(item.reviewId), value)
    ElMessage.success('申诉已提交，将由其他导师或管理员复核')
    history.value = await getSubmissionHistory(Number(route.params.id))
  } catch (reason) { if (reason !== 'cancel' && reason !== 'close') ElMessage.error(getErrorMessage(reason, '申诉提交失败')) }
}

onMounted(load)
</script>
