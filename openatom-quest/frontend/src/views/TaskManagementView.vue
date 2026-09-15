<template>
  <div class="page-container task-management-page">
    <header class="page-title-row">
      <div class="page-heading">
        <p class="eyebrow">TASK OPERATIONS</p>
        <h1>任务管理</h1>
        <p>创建并维护由你负责的任务。审核队列只会展示这些任务产生的成员提交。</p>
      </div>
      <el-button type="primary" size="large" @click="taskDialog = true">新建任务</el-button>
    </header>

    <el-skeleton v-if="loading" :rows="7" animated />
    <el-result v-else-if="error" icon="error" title="任务管理数据加载失败" :sub-title="error">
      <template #extra><el-button type="primary" @click="load">重新加载</el-button></template>
    </el-result>
    <template v-else>
      <section class="management-summary" aria-label="任务状态概览">
        <article><span>全部任务</span><strong>{{ tasks.length }}</strong></article>
        <article><span>已发布</span><strong>{{ countStatus('PUBLISHED') }}</strong></article>
        <article><span>待发布草稿</span><strong>{{ countStatus('DRAFT') }}</strong></article>
        <article><span>成员提交通过</span><strong>{{ passedTotal }}</strong></article>
      </section>

      <section v-if="tasks.length" class="data-table-wrap" aria-label="我负责的任务">
        <table class="data-table">
          <thead><tr><th>任务</th><th>方向 / 阶段</th><th>领取 / 通过</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in tasks" :key="item.id">
              <td><strong>{{ item.title }}</strong><small>{{ item.taskKey }}</small></td>
              <td>{{ item.directionName || '未指定' }} / {{ item.stageKey || '-' }}</td>
              <td>{{ item.assignmentCount || 0 }} / {{ item.passedCount || 0 }}</td>
              <td><span class="status-chip" :data-status="item.status">{{ statusText[item.status] || item.status }}</span></td>
              <td class="task-actions">
                <el-button v-if="item.status === 'DRAFT' || item.status === 'OFFLINE'" link type="primary" :loading="workingTaskId === item.id" @click="setStatus(item, 'PUBLISHED')">{{ item.status === 'OFFLINE' ? '重新发布' : '发布' }}</el-button>
                <el-button v-if="item.status === 'PUBLISHED'" link :loading="workingTaskId === item.id" @click="setStatus(item, 'OFFLINE')">下架</el-button>
                <el-button v-if="item.status !== 'ARCHIVED'" link type="danger" :loading="workingTaskId === item.id" @click="archiveTask(item)">归档</el-button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>
      <section v-else class="content-card empty-panel">
        <strong>还没有由你负责的任务</strong>
        <p>创建任务草稿，确认内容后再发布给成员。</p>
        <el-button type="primary" @click="taskDialog = true">创建第一个任务</el-button>
      </section>
    </template>

    <el-dialog v-model="taskDialog" title="新建任务" width="min(760px, calc(100vw - 32px))" destroy-on-close="false">
      <p class="dialog-intro">任务负责人将自动设置为当前账号。表单内容会在本机自动保存为草稿。</p>
      <el-form ref="formRef" :model="taskForm" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="任务名称" prop="title"><el-input v-model.trim="taskForm.title" maxlength="160" show-word-limit /></el-form-item>
          <el-form-item label="任务标识" prop="taskKey"><el-input v-model.trim="taskForm.taskKey" placeholder="first-project-task" /></el-form-item>
          <el-form-item label="任务类型" prop="taskType"><el-select v-model="taskForm.taskType" style="width:100%"><el-option v-for="item in taskTypes" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="难度" prop="difficulty"><el-select v-model="taskForm.difficulty" style="width:100%"><el-option label="入门" value="ENTRY"/><el-option label="初级" value="BEGINNER"/><el-option label="中级" value="INTERMEDIATE"/><el-option label="高级" value="ADVANCED"/></el-select></el-form-item>
          <el-form-item label="成长路线" prop="routeId"><el-select v-model="taskForm.routeId" placeholder="选择已发布路线" style="width:100%"><el-option v-for="item in routes" :key="item.id" :label="`${item.directionName} · ${item.name}`" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="成长阶段" prop="stageId"><el-select v-model="taskForm.stageId" :disabled="!taskForm.routeId" placeholder="先选择成长路线" style="width:100%"><el-option v-for="item in availableStages" :key="item.id" :label="`${item.stageKey} ${item.name}`" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="预计用时（分钟）" prop="estimatedMinutes"><el-input-number v-model="taskForm.estimatedMinutes" :min="1" style="width:100%" /></el-form-item>
          <el-form-item label="通过积分" prop="points"><el-input-number v-model="taskForm.points" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="截止规则" prop="deadlineType"><el-select v-model="taskForm.deadlineType" style="width:100%"><el-option label="无固定截止" value="NONE"/><el-option label="固定时间" value="FIXED"/><el-option label="领取后倒计时" value="AFTER_CLAIM"/></el-select></el-form-item>
          <el-form-item v-if="taskForm.deadlineType === 'FIXED'" label="固定截止时间" prop="fixedDeadline"><el-input v-model="taskForm.fixedDeadline" type="datetime-local" /></el-form-item>
          <el-form-item v-if="taskForm.deadlineType === 'AFTER_CLAIM'" label="领取后小时数" prop="durationHours"><el-input-number v-model="taskForm.durationHours" :min="1" style="width:100%" /></el-form-item>
          <el-form-item label="提交次数上限"><el-input-number v-model="taskForm.submissionLimit" :min="1" style="width:100%" /></el-form-item>
          <el-form-item label="领取人数上限"><el-input-number v-model="taskForm.capacity" :min="1" clearable style="width:100%" /></el-form-item>
          <el-form-item label="前置任务"><el-select v-model="taskForm.prerequisiteTaskIds" multiple clearable placeholder="可选" style="width:100%"><el-option v-for="item in publishedTasks" :key="item.id" :label="item.title" :value="item.id" /></el-select></el-form-item>
        </div>
        <el-form-item label="任务简介" prop="summary"><el-input v-model.trim="taskForm.summary" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="学习目标（每行一项）" prop="learningObjectivesText"><el-input v-model="taskForm.learningObjectivesText" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="操作步骤" prop="instructions"><el-input v-model="taskForm.instructions" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="参考资料（每行一个链接）"><el-input v-model="taskForm.resourcesText" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="提交要求" prop="submissionRequirements"><el-input v-model="taskForm.submissionRequirements" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="验收标准" prop="acceptanceCriteria"><el-input v-model="taskForm.acceptanceCriteria" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="常见问题（每行一项）"><el-input v-model="taskForm.faqText" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialog = false">稍后继续</el-button>
        <el-button type="primary" :loading="saving" @click="saveTask">保存草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import 'element-plus/es/components/dialog/style/css'
import { ElDialog, ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { getErrorMessage } from '@/api/http'
import { changeTaskStatus, createTask, getAdminTasks, getTaskManagementOptions } from '@/api/quest'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const taskDialog = ref(false)
const workingTaskId = ref<number>()
const tasks = ref<Record<string, any>[]>([])
const routes = ref<Record<string, any>[]>([])
const stages = ref<Record<string, any>[]>([])
const draftEnabled = ref(true)
const draftKey = `quest-task-management-draft:${auth.member?.id || 'current'}`
const statusText: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下架', ARCHIVED: '已归档' }
const taskTypes = [
  { label: '新人必做', value: 'ONBOARDING' },
  { label: '技术学习', value: 'LEARNING' },
  { label: '实践挑战', value: 'CHALLENGE' },
  { label: '团队协作', value: 'COLLABORATION' },
  { label: '真实项目', value: 'REAL_PROJECT' },
  { label: '限时活动', value: 'LIMITED_EVENT' },
]

const taskForm = reactive(defaultTaskForm())
const availableStages = computed(() => stages.value.filter(item => item.routeId === taskForm.routeId))
const publishedTasks = computed(() => tasks.value.filter(item => item.status === 'PUBLISHED'))
const passedTotal = computed(() => tasks.value.reduce((sum, item) => sum + Number(item.passedCount || 0), 0))
const rules: FormRules = {
  title: [{ required: true, message: '请填写任务名称', trigger: 'blur' }],
  taskKey: [
    { required: true, message: '请填写任务标识', trigger: 'blur' },
    { pattern: /^[a-z0-9-]{2,64}$/, message: '仅支持 2-64 位小写字母、数字和连字符', trigger: 'blur' },
  ],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  routeId: [{ required: true, message: '请选择成长路线', trigger: 'change' }],
  stageId: [{ required: true, message: '请选择成长阶段', trigger: 'change' }],
  estimatedMinutes: [{ required: true, message: '请填写预计用时', trigger: 'change' }],
  points: [{ required: true, message: '请填写通过积分', trigger: 'change' }],
  deadlineType: [{ required: true, message: '请选择截止规则', trigger: 'change' }],
  fixedDeadline: [{ required: true, message: '请选择固定截止时间', trigger: 'change' }],
  durationHours: [{ required: true, message: '请填写领取后小时数', trigger: 'change' }],
  summary: [{ required: true, message: '请填写任务简介', trigger: 'blur' }],
  learningObjectivesText: [{ required: true, message: '请填写至少一项学习目标', trigger: 'blur' }],
  instructions: [{ required: true, message: '请填写操作步骤', trigger: 'blur' }],
  submissionRequirements: [{ required: true, message: '请填写提交要求', trigger: 'blur' }],
  acceptanceCriteria: [{ required: true, message: '请填写验收标准', trigger: 'blur' }],
}

function defaultTaskForm() {
  return {
    title: '', taskKey: '', summary: '', taskType: 'LEARNING', difficulty: 'ENTRY',
    routeId: undefined as number | undefined, stageId: undefined as number | undefined,
    estimatedMinutes: 60, points: 50, learningObjectivesText: '', instructions: '',
    submissionRequirements: '', acceptanceCriteria: '', deadlineType: 'NONE', fixedDeadline: '',
    durationHours: 72, submissionLimit: 3, capacity: undefined as number | undefined,
    prerequisiteTaskIds: [] as number[], resourcesText: '', faqText: '',
  }
}

function countStatus(status: string) { return tasks.value.filter(item => item.status === status).length }
function lines(value: string) { return value.split('\n').map(item => item.trim()).filter(Boolean) }

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [ownedTasks, options] = await Promise.all([getAdminTasks(), getTaskManagementOptions()])
    tasks.value = ownedTasks
    routes.value = options.routes
    stages.value = options.stages
  } catch (reason) {
    error.value = getErrorMessage(reason, '无法加载任务管理数据')
  } finally {
    loading.value = false
  }
}

async function saveTask() {
  if (!await formRef.value?.validate()) return
  const route = routes.value.find(item => item.id === taskForm.routeId)
  if (!route) return ElMessage.error('所选成长路线已失效，请重新选择')
  saving.value = true
  try {
    await createTask({
      ...taskForm,
      directionId: route.directionId,
      ownerMemberId: null,
      learningObjectives: lines(taskForm.learningObjectivesText),
      resources: lines(taskForm.resourcesText),
      faq: lines(taskForm.faqText),
      fixedDeadline: taskForm.deadlineType === 'FIXED' ? taskForm.fixedDeadline : null,
      durationHours: taskForm.deadlineType === 'AFTER_CLAIM' ? taskForm.durationHours : null,
      capacity: taskForm.capacity || null,
      requiredInStage: true,
    })
    taskDialog.value = false
    draftEnabled.value = false
    Object.assign(taskForm, defaultTaskForm())
    localStorage.removeItem(draftKey)
    await nextTick()
    draftEnabled.value = true
    ElMessage.success('任务草稿已创建')
    await load()
  } catch (reason) {
    ElMessage.error(getErrorMessage(reason, '任务创建失败'))
  } finally {
    saving.value = false
  }
}

async function setStatus(item: Record<string, any>, status: string) {
  workingTaskId.value = Number(item.id)
  try {
    await changeTaskStatus(Number(item.id), status)
    ElMessage.success(status === 'PUBLISHED' ? '任务已发布' : '任务已下架')
    await load()
  } catch (reason) {
    ElMessage.error(getErrorMessage(reason, '任务状态更新失败'))
  } finally {
    workingTaskId.value = undefined
  }
}

async function archiveTask(item: Record<string, any>) {
  try {
    await ElMessageBox.confirm(`归档后不可恢复，但会保留历史记录。确认归档“${item.title}”？`, '归档任务', { type: 'warning' })
    await setStatus(item, 'ARCHIVED')
  } catch (reason) {
    if (reason !== 'cancel' && reason !== 'close') ElMessage.error(getErrorMessage(reason, '任务归档失败'))
  }
}

watch(() => taskForm.routeId, () => {
  if (!availableStages.value.some(item => item.id === taskForm.stageId)) taskForm.stageId = undefined
})
watch(taskForm, value => {
  if (draftEnabled.value) localStorage.setItem(draftKey, JSON.stringify(value))
}, { deep: true })

onMounted(async () => {
  const draft = localStorage.getItem(draftKey)
  if (draft) {
    try { Object.assign(taskForm, JSON.parse(draft)) }
    catch { localStorage.removeItem(draftKey) }
  }
  await load()
})
</script>

<style scoped>
.task-management-page { display: grid; gap: var(--space-5); }
.page-title-row .el-button { min-height: 44px; margin-top: var(--space-3); }
.management-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-4); }
.management-summary article { min-height: 112px; padding: var(--space-5); display: flex; flex-direction: column; border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-bg-container); box-shadow: var(--shadow-sm); }
.management-summary span { color: var(--color-text-secondary); font-size: 12px; }
.management-summary strong { margin-top: auto; font-size: 28px; letter-spacing: -.04em; }
.task-actions { white-space: nowrap; }
.task-actions .el-button { min-height: 44px; margin-left: 0; margin-right: var(--space-2); }
.dialog-intro { margin: 0 0 var(--space-5); color: var(--color-text-secondary); font-size: 13px; line-height: 1.7; }
@media (max-width: 767px) {
  .management-summary { grid-template-columns: 1fr 1fr; }
  .page-title-row .el-button { width: 100%; margin: 0; }
}
</style>
