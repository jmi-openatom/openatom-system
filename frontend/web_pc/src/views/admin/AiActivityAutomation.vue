<template>
  <ViewPage class="admin-page ai-activity-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">AI 活动自动化</p>
        <h2>从一个想法到三份 PU 材料</h2>
        <p class="subline">先和 AI 补齐活动要素，再确认策划案，最后生成申请书、申请表和志愿者申请书。</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Setting" @click="settingsVisible = true">AI 配置</el-button>
        <el-button :icon="Upload" @click="templateVisible = true">模板</el-button>
        <el-button type="primary" :icon="Plus" @click="newSessionVisible = true">新建活动</el-button>
      </div>
    </section>

    <section class="status-strip">
      <div v-for="item in progressItems" :key="item.key" class="status-step" :class="item.state">
        <span class="step-index">{{ item.index }}</span>
        <div>
          <strong>{{ item.title }}</strong>
          <small>{{ item.description }}</small>
        </div>
      </div>
    </section>

    <div class="workspace">
      <aside class="session-panel-wrap">
        <AiSessionPanel
          :sessions="sessions"
          :current-id="current?.id"
          :streaming="streaming"
          :deleting-session-id="deletingSessionId"
          @open="openSession"
          @delete="deleteSession"
          @refresh="loadSessions"
        />
      </aside>

      <main class="main-panel">
        <div v-if="current" class="current-head">
          <div>
            <h3>{{ current.title }}</h3>
            <p>{{ currentNextAction }}</p>
          </div>
          <el-tag :type="statusTypeOf(current.status)">{{ statusText(current.status) }}</el-tag>
        </div>
        <div v-else class="welcome-panel">
          <h3>创建一个 AI 活动会话</h3>
          <p>输入“我想办一个……”就行。AI 会先追问关键信息，再进入策划案和材料生成。</p>
          <el-button type="primary" :icon="Plus" @click="newSessionVisible = true">开始新活动</el-button>
        </div>

        <template v-if="current">
          <div class="action-bar">
            <div class="next-action">
              <span>当前建议</span>
              <strong>{{ primaryActionLabel }}</strong>
            </div>
            <div class="action-buttons">
              <el-button :disabled="streaming || !canConfirmRequirement" @click="confirmRequirement">确认需求</el-button>
              <el-button
                type="primary"
                :disabled="streaming || !canGeneratePlan"
                :loading="generatingPlan"
                @click="generatePlan"
              >
                生成策划案
              </el-button>
              <el-button type="success" :disabled="!canConfirmPlan" @click="confirmPlan">确认策划案</el-button>
              <el-button
                :disabled="streaming || (!canGenerateDocuments && !canCreateActivityDraft)"
                :loading="generatingDocs"
                @click="canGenerateDocuments ? generateDocuments() : createActivityDraft()"
              >
                {{ canGenerateDocuments ? '生成五份材料' : '创建活动草稿' }}
              </el-button>
            </div>
          </div>

          <el-tabs v-model="activeTab" class="work-tabs">
            <el-tab-pane label="需求对话" name="chat">
              <AiChatSurface
                :messages="current.messages || []"
                :pending="aiResponsePending"
                :streaming="streaming"
                :sending="sending"
                @send="sendMessage"
              />
            </el-tab-pane>

            <el-tab-pane label="策划案" name="plan">
              <AiPlanEditor
                :latest-plan="latestPlan"
                :phase="streamPhase"
                :streaming="streaming || streamingPlan"
                :revising="revising"
                :plan-text="planText"
                :revise-instruction="reviseInstruction"
                @update:plan-text="planText = $event"
                @update:revise-instruction="reviseInstruction = $event"
                @revise="revisePlan"
                @save="savePlanDraft"
                @generate="generatePlan"
              />
            </el-tab-pane>
          </el-tabs>
        </template>
      </main>

      <aside class="ops-panel-wrap">
        <AiOpsPanel
          :document-types="documentTypes"
          :templates="templates"
          :documents="current?.documents"
          :ai-settings="aiSettings"
          :streaming="streaming"
          :can-generate-documents="canGenerateDocuments"
          :can-create-activity-draft="canCreateActivityDraft"
          :generating-docs="generatingDocs"
          @manage-templates="templateVisible = true"
          @manage-settings="settingsVisible = true"
          @generate-documents="generateDocuments"
          @create-activity="createActivityDraft"
          @download="downloadDoc"
        />
      </aside>
    </div>

    <AiNewSessionDialog
      :visible="newSessionVisible"
      :loading="creating"
      @update:visible="newSessionVisible = $event"
      @create="createSession"
    />

    <el-dialog v-model="templateVisible" title="上传 Word 模板" width="780px">
      <div class="template-dialog">
        <el-form label-width="112px">
          <el-form-item label="模板类型">
            <el-select v-model="templateForm.templateType">
              <el-option
                v-for="type in documentTypes"
                :key="type.value"
                :label="type.label"
                :value="type.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="模板名称">
            <el-input v-model="templateForm.templateName" />
          </el-form-item>
          <el-form-item label="docx 文件">
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept=".docx"
              :on-change="handleTemplateFile"
              :on-remove="clearTemplateFile"
            >
              <el-button :icon="Upload">选择文件</el-button>
            </el-upload>
          </el-form-item>
        </el-form>
        <el-table :data="templates" size="small">
          <el-table-column prop="templateName" label="模板" min-width="170" />
          <el-table-column prop="templateType" label="类型" min-width="160">
            <template #default="{ row }">{{ documentTypeLabel(row.templateType) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="templateVisible = false">关闭</el-button>
        <el-button type="primary" :loading="uploadingTemplate" @click="uploadTemplate">上传模板</el-button>
      </template>
    </el-dialog>

    <AiSupplementDialog
      :visible="supplementVisible"
      :fields="supplementFields"
      :loading="generatingDocs"
      @update:visible="supplementVisible = $event"
      @submit="submitSupplementAndGenerate"
    />

    <el-drawer v-model="settingsVisible" title="DeepSeek 配置" size="520px" @open="loadAiSettings">
      <el-form label-width="124px">
        <el-form-item label="启用 AI">
          <el-switch v-model="aiSettings.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="aiSettings.baseUrl" placeholder="https://api.deepseek.com" />
        </el-form-item>
        <el-form-item label="模型">
          <el-input v-model="aiSettings.model" placeholder="deepseek-chat" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input
            v-model="aiSettings.apiKey"
            show-password
            placeholder="留空则保留当前 Key"
            type="password"
          />
          <p class="form-tip">
            当前状态：{{ aiSettings.hasApiKey ? `已配置 ${aiSettings.apiKeyMasked || ''}` : '未配置' }}
          </p>
        </el-form-item>
        <el-form-item label="超时时间">
          <el-input-number v-model="aiSettings.timeoutSeconds" :min="5" :max="180" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button :loading="testingSettings" @click="testAiSettings">测试连接</el-button>
        <el-button type="primary" :loading="savingSettings" @click="saveAiSettings">保存配置</el-button>
      </template>
      <p v-if="testResult" class="test-result">{{ testResult }}</p>
    </el-drawer>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import AiSessionPanel from './ai/AiSessionPanel.vue'
import AiChatSurface from './ai/AiChatSurface.vue'
import AiPlanEditor from './ai/AiPlanEditor.vue'
import AiOpsPanel from './ai/AiOpsPanel.vue'
import AiSupplementDialog from './ai/AiSupplementDialog.vue'
import AiNewSessionDialog from './ai/AiNewSessionDialog.vue'
import { aiActivityApi, aiSettingsApi, documentTemplateApi, postAiStream } from '@/api/index.ts'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Delete, Plus, Refresh, Setting, Upload } from '@element-plus/icons-vue'
import { computed, onMounted, ref, watch } from 'vue'

const documentTypes = [
  { value: 'activity_proposal', label: '活动申请书' },
  { value: 'activity_application_form', label: '活动申请表' },
  { value: 'volunteer_application_form', label: '志愿者申请表' },
]

const sessions = ref<any[]>([])
const current = ref<any>(null)
const templates = ref<any[]>([])
const messageText = ref('')
const planText = ref('')
const reviseInstruction = ref('')
const activeTab = ref('chat')
const newSessionVisible = ref(false)
const templateVisible = ref(false)
const settingsVisible = ref(false)
const supplementVisible = ref(false)
const creating = ref(false)
const sending = ref(false)
const generatingPlan = ref(false)
const revising = ref(false)
const generatingDocs = ref(false)
const uploadingTemplate = ref(false)
const savingSettings = ref(false)
const testingSettings = ref(false)
const deletingSessionId = ref<string | number | null>(null)
const streaming = ref(false)
const streamingPlan = ref(false)
const aiResponsePending = ref(false)
const streamPhase = ref('')
const testResult = ref('')
const streamController = ref<AbortController | null>(null)
const templateFile = ref<File | null>(null)
const templateForm = ref({
  templateType: 'activity_proposal',
  templateName: '',
})
const supplementFields = ref<SupplementField[]>([])
const pendingSupplementVariables = ref<Record<string, string>>({})
const aiSettings = ref<Record<string, any>>({
  enabled: true,
  baseUrl: 'https://api.deepseek.com',
  model: 'deepseek-chat',
  apiKey: '',
  timeoutSeconds: 30,
  hasApiKey: false,
  apiKeyMasked: '',
})

type SupplementField = {
  key: string
  label: string
  value: string | string[]
  placeholder: string
  multiline?: boolean
  control?: 'datetimeRange'
}

const supplementFieldMeta: Record<string, Omit<SupplementField, 'key' | 'value'>> = {
  clubName: { label: '社团名称', placeholder: '例如：开放原子开源社团' },
  activityName: { label: '活动名称', placeholder: '例如：新生开源破冰活动' },
  activityCategory: { label: '活动类别', placeholder: '例如：创新创业' },
  activityLevel: { label: '活动级别', placeholder: '例如：校级' },
  activityDateRange: { label: '活动时间', placeholder: '请选择活动开始和结束时间', control: 'datetimeRange' },
  registrationDateRange: { label: '报名时间', placeholder: '请选择报名开始和结束时间', control: 'datetimeRange' },
  location: { label: '活动地点', placeholder: '例如：教学楼 A101 / 学生活动中心报告厅' },
  targetAudience: { label: '活动对象', placeholder: '例如：全校学生 / 社团新成员' },
  targetCollege: { label: '面向院系', placeholder: '例如：全部' },
  targetGrade: { label: '面向年级', placeholder: '例如：全部' },
  expectedParticipants: { label: '活动人数', placeholder: '例如：100 人' },
  registrationQuota: { label: '报名名额', placeholder: '例如：100 人' },
  practiceHours: { label: '实践学时', placeholder: '例如：3' },
  needCheckout: { label: '是否签退', placeholder: '例如：否' },
  needFieldCheckin: { label: '是否外勤打卡', placeholder: '例如：否' },
  volunteerCount: { label: '志愿者人数', placeholder: '例如：10 人' },
  volunteerCategory: { label: '志愿者类别', placeholder: '例如：志愿公益服务' },
  principalName: { label: '负责人', placeholder: '例如：张三' },
  principalPhone: { label: '负责人电话', placeholder: '例如：13800000000' },
  contactText: { label: '联系方式', placeholder: '例如：张三，联系电话 13800000000' },
  advisorName: { label: '指导老师', placeholder: '例如：李老师' },
  checkinStudentId: { label: '签到员学号', placeholder: '例如：2026xxxxxx' },
  registrationMethod: { label: '报名方式', placeholder: '例如：报名制（报名不需审核，人满截止）' },
  volunteerRegistrationMethod: { label: '志愿者报名方式', placeholder: '例如：报名制（报名需审核，人满截止）' },
  budgetTotal: { label: '预算总额', placeholder: '例如：300 元' },
  budgetDetails: { label: '预算明细', placeholder: '例如：物资 150 元，奖品 100 元，打印 50 元', multiline: true },
  activitySummary: { label: '活动简介', placeholder: '请输入适合申请书的活动简介', multiline: true },
  activityIntroduction: { label: '活动介绍', placeholder: '请输入活动背景或介绍', multiline: true },
  activityHighlights: { label: '活动亮点', placeholder: '请输入活动亮点', multiline: true },
  activityContentFull: { label: '活动内容', placeholder: '请输入申请表活动内容', multiline: true },
  volunteerActivitySummary: { label: '志愿者活动简介', placeholder: '请输入志愿者申请书简介', multiline: true },
  volunteerResponsibilities: { label: '志愿者职责', placeholder: '请输入志愿者职责', multiline: true },
}

const supplementFieldOrder = [
  'activityDateRange',
  'registrationDateRange',
  'location',
  'expectedParticipants',
  'registrationQuota',
  'volunteerCount',
  'principalName',
  'principalPhone',
  'advisorName',
  'checkinStudentId',
  'budgetTotal',
  'budgetDetails',
]

const latestPlan = computed(() => {
  const plans = current.value?.plans || []
  return plans.length ? plans[0] : null
})

const hasPlanEditorContent = computed(() => Boolean(latestPlan.value || streamingPlan.value || planText.value.trim()))

const stepActive = computed(() => {
  const status = current.value?.status
  if (status === 'documents_generated' || status === 'submitted') return 4
  if (status === 'plan_confirmed') return 3
  if (status === 'plan_generated') return 2
  if (status === 'requirement_confirmed') return 1
  return 0
})

const progressItems = computed(() => {
  const items = [
    { key: 'chat', title: '澄清需求', description: '补齐时间、地点、人数、预算', index: '01' },
    { key: 'requirement', title: '确认需求', description: '锁定活动关键信息', index: '02' },
    { key: 'plan', title: '确认策划案', description: '生成并修订正式策划案', index: '03' },
    { key: 'docs', title: '生成材料', description: '输出三份 docx 与两份 Markdown', index: '04' },
  ]
  return items.map((item, index) => ({
    ...item,
    state: index < stepActive.value ? 'done' : index === stepActive.value ? 'current' : 'pending',
  }))
})

const currentNextAction = computed(() => {
  const status = current.value?.status
  if (status === 'drafting') return '继续补齐活动信息，确认后再生成策划案。'
  if (status === 'requirement_confirmed') return '需求已锁定，可以生成活动策划案。'
  if (status === 'plan_generated') return '检查策划案内容，必要时让 AI 局部修改。'
  if (status === 'plan_confirmed') return '策划案已确认，可以生成三份 docx 与两份 Markdown。'
  if (status === 'documents_generated') return '材料已生成，可下载或创建活动草稿。'
  return '选择下一步操作。'
})

const canConfirmRequirement = computed(() => current.value?.status === 'drafting')
const canGeneratePlan = computed(() => current.value?.status === 'requirement_confirmed')
const canConfirmPlan = computed(() =>
  ['requirement_confirmed', 'plan_generated', 'plan_confirmed'].includes(current.value?.status || '')
  && Boolean(latestPlan.value || planText.value.trim())
)
const canGenerateDocuments = computed(() => current.value?.status === 'plan_confirmed')
const canCreateActivityDraft = computed(() =>
  ['documents_generated', 'submitted'].includes(current.value?.status || '')
)

const primaryActionLabel = computed(() => {
  if (!current.value) return '新建活动'
  if (canConfirmRequirement.value) return '确认需求'
  if (canGeneratePlan.value) return '生成策划案'
  if (canConfirmPlan.value) return '确认策划案'
  if (canGenerateDocuments.value) return '生成五份材料'
  if (canCreateActivityDraft.value) return '创建活动草稿'
  return '继续补充信息'
})

watch(latestPlan, (plan) => {
  planText.value = plan?.contentMarkdown || ''
  if (plan) activeTab.value = 'plan'
})




function documentTypeLabel(value: string) {
  return documentTypes.find((item) => item.value === value)?.label || value
}

function statusText(status: string) {
  return (
    {
      drafting: '需求澄清中',
      requirement_confirmed: '需求已确认',
      plan_generated: '策划案已生成',
      plan_confirmed: '策划案已确认',
      documents_generated: '材料已生成',
    }[status] || status
  )
}

function statusTypeOf(status: string) {
  return (
    {
      drafting: 'info',
      requirement_confirmed: 'warning',
      plan_generated: 'warning',
      plan_confirmed: 'primary',
      documents_generated: 'success',
    } as const
  )[status] || 'info'
}

async function savePlanDraft() {
  if (!current.value) return
  if (!planText.value.trim()) {
    ElMessage.warning('策划案内容不能为空')
    return
  }
  await aiActivityApi.savePlan(current.value.id, { contentMarkdown: planText.value })
  ElMessage.success('草稿已保存')
  await loadSessions()
}











async function loadSessions() {
  sessions.value = (await aiActivityApi.sessions()) || []
}

async function loadTemplates() {
  templates.value = (await documentTemplateApi.list({ status: 'enabled' })) || []
}

async function loadAiSettings() {
  const data = await aiSettingsApi.get()
  aiSettings.value = {
    enabled: data?.enabled !== false,
    baseUrl: data?.baseUrl || 'https://api.deepseek.com',
    model: data?.model || 'deepseek-chat',
    apiKey: '',
    timeoutSeconds: Number(data?.timeoutSeconds || 30),
    hasApiKey: Boolean(data?.hasApiKey),
    apiKeyMasked: data?.apiKeyMasked || '',
  }
}

async function openSession(id: string | number) {
  current.value = await aiActivityApi.detail(id)
  planText.value = current.value?.plans?.[0]?.contentMarkdown || ''
  activeTab.value = latestPlan.value ? 'plan' : 'chat'
}

async function deleteSession(item: any) {
  if (!item?.id || streaming.value) return
  deletingSessionId.value = item.id
  try {
    await aiActivityApi.deleteSession(item.id)
    if (current.value?.id === item.id) {
      current.value = null
      planText.value = ''
      reviseInstruction.value = ''
      activeTab.value = 'chat'
    }
    ElMessage.success('对话已删除')
    await loadSessions()
  } finally {
    deletingSessionId.value = null
  }
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

async function syncSavedPlan(sessionId: string | number, fallbackText: string) {
  let refreshed: any = null
  for (let index = 0; index < 5; index += 1) {
    refreshed = await aiActivityApi.detail(sessionId)
    if (refreshed?.plans?.[0]) break
    await wait(250)
  }
  if (!refreshed?.plans?.[0] && fallbackText.trim()) {
    refreshed = await aiActivityApi.savePlan(sessionId, { contentMarkdown: fallbackText })
  }
  current.value = refreshed || current.value
  const savedPlanText = refreshed?.plans?.[0]?.contentMarkdown
  if (savedPlanText) {
    planText.value = savedPlanText
    return true
  }
  planText.value = fallbackText
  return false
}

function ensureMessageList() {
  if (!current.value) return []
  if (!Array.isArray(current.value.messages)) current.value.messages = []
  return current.value.messages
}

function appendTempMessage(role: string, content = '') {
  const messages = ensureMessageList()
  const message = {
    id: `temp-${Date.now()}-${Math.random()}`,
    role,
    content,
  }
  messages.push(message)
  return message
}

function applyStreamEvent(event: string, data: any, assistantMessage: any | null) {
  if (event === 'phase') {
    streamPhase.value = data?.message || ''
    return assistantMessage
  }
  if (event === 'session' || event === 'message') {
    current.value = data?.detail || current.value
    return null
  }
  if (event === 'delta') {
    aiResponsePending.value = false
    if (!assistantMessage) assistantMessage = appendTempMessage('assistant')
    assistantMessage.content += data?.content || ''
    return assistantMessage
  }
  if (event === 'complete') {
    current.value = data?.detail || current.value
    aiResponsePending.value = false
    streamPhase.value = ''
    return null
  }
  if (event === 'error') {
    throw new Error(data?.message || 'AI 流式输出失败')
  }
  return assistantMessage
}

async function runAiStream(
  path: string,
  body: Record<string, unknown>,
  onDelta?: (content: string) => void,
  options: { hideDelta?: boolean } = {},
) {
  let assistantMessage: any | null = null
  const controller = new AbortController()
  streamController.value = controller
  try {
    await postAiStream(path, body, ({ event, data }) => {
      if (event === 'delta' && options.hideDelta) return
      if (event === 'delta' && onDelta) {
        onDelta(data?.content || '')
        return
      }
      assistantMessage = applyStreamEvent(event, data, assistantMessage)
    }, controller.signal)
  } finally {
    if (streamController.value === controller) streamController.value = null
  }
}

function stopCurrentStream() {
  if (streamController.value) {
    streamController.value.abort()
    streamController.value = null
  }
  streaming.value = false
  streamingPlan.value = false
  aiResponsePending.value = false
  streamPhase.value = ''
}

async function createSession(payload: { title: string; initialMessage: string }) {
  if (!payload?.initialMessage.trim()) {
    ElMessage.warning('请先输入活动需求')
    return
  }
  creating.value = true
  streaming.value = true
  aiResponsePending.value = true
  streamPhase.value = 'AI 正在澄清活动需求'
  try {
    await runAiStream('/ai/activity/sessions/stream', payload)
    activeTab.value = 'chat'
    newSessionVisible.value = false
    await loadSessions()
  } finally {
    creating.value = false
    streaming.value = false
    aiResponsePending.value = false
    streamPhase.value = ''
  }
}

async function sendMessage() {
  if (!current.value || !messageText.value.trim()) return
  const message = messageText.value.trim()
  messageText.value = ''
  appendTempMessage('user', message)
  sending.value = true
  streaming.value = true
  aiResponsePending.value = true
  streamPhase.value = 'AI 正在回复'
  try {
    await runAiStream(`/ai/activity/sessions/${current.value.id}/messages/stream`, {
      message,
      mode: 'requirement_clarification',
    })
    await loadSessions()
  } finally {
    sending.value = false
    streaming.value = false
    aiResponsePending.value = false
    streamPhase.value = ''
  }
}

async function confirmRequirement() {
  if (!current.value) return
  current.value = await aiActivityApi.confirmRequirement(current.value.id)
  ElMessage.success('需求已确认')
  await loadSessions()
}

async function generatePlan() {
  if (!current.value) return
  const sessionId = current.value.id
  generatingPlan.value = true
  streaming.value = true
  streamingPlan.value = true
  streamPhase.value = '正在生成策划案'
  planText.value = ''
  activeTab.value = 'plan'
  try {
    await runAiStream(`/ai/activity/sessions/${sessionId}/generate-plan/stream`, {}, (content) => {
      planText.value += content
    })
    const streamedText = planText.value
    const synced = await syncSavedPlan(sessionId, streamedText)
    if (!synced) {
      ElMessage.warning('策划案已生成，但暂未读到保存记录，已保留当前正文')
    }
    activeTab.value = 'plan'
    ElMessage.success('策划案已生成')
    await loadSessions()
  } catch (error: any) {
    if (error?.name !== 'AbortError') throw error
  } finally {
    generatingPlan.value = false
    streaming.value = false
    streamingPlan.value = false
    streamPhase.value = ''
  }
}

async function revisePlan() {
  if (!current.value || !reviseInstruction.value.trim()) return
  const sessionId = current.value.id
  const instruction = `${reviseInstruction.value}\n\n用户当前编辑稿：\n${planText.value}`
  const previousPlanText = planText.value
  let hasRevisionDelta = false
  revising.value = true
  streaming.value = true
  streamingPlan.value = true
  streamPhase.value = '正在修改策划案'
  try {
    await runAiStream(`/ai/activity/sessions/${sessionId}/revise-plan/stream`, {
      instruction,
    }, (content) => {
      if (!hasRevisionDelta) {
        planText.value = ''
        hasRevisionDelta = true
      }
      planText.value += content
    })
    const synced = await syncSavedPlan(sessionId, hasRevisionDelta ? planText.value : previousPlanText)
    if (!synced) {
      ElMessage.warning('策划案已修改，但暂未读到保存记录，已保留当前正文')
    }
    reviseInstruction.value = ''
    ElMessage.success('策划案已修改')
  } catch (error: any) {
    if (error?.name !== 'AbortError') throw error
  } finally {
    revising.value = false
    streaming.value = false
    streamingPlan.value = false
    streamPhase.value = ''
  }
}

async function confirmPlan() {
  if (!current.value) return
  const sessionId = current.value.id
  if (streamingPlan.value || streaming.value) stopCurrentStream()
  if (planText.value.trim()) {
    await syncSavedPlan(sessionId, planText.value)
  }
  current.value = await aiActivityApi.confirmPlan(sessionId)
  current.value = await aiActivityApi.detail(sessionId)
  planText.value = current.value?.plans?.[0]?.contentMarkdown || planText.value
  ElMessage.success('策划案已确认')
  await loadSessions()
}

async function generateDocuments() {
  if (!current.value) return
  const missingFields = collectSupplementFields()
  if (missingFields.length) {
    pendingSupplementVariables.value = {}
    supplementFields.value = missingFields
    supplementVisible.value = true
    return
  }
  await doGenerateDocuments({})
}

async function submitSupplementAndGenerate() {
  const variables: Record<string, string> = { ...pendingSupplementVariables.value }
  for (const field of supplementFields.value) {
    const value = supplementFieldValue(field)
    if (value) variables[field.key] = value
  }
  if (variables.expectedParticipants && !variables.registrationQuota) {
    variables.registrationQuota = variables.expectedParticipants
  }
  if (variables.registrationQuota && !variables.expectedParticipants) {
    variables.expectedParticipants = variables.registrationQuota
  }
  if (variables.principalName || variables.principalPhone) {
    variables.contactText = `${variables.principalName || '待补充'}，联系电话 ${variables.principalPhone || '待补充'}`
  }
  supplementVisible.value = false
  pendingSupplementVariables.value = variables
  await doGenerateDocuments(variables)
}

async function doGenerateDocuments(variables: Record<string, unknown>) {
  if (!current.value) return
  generatingDocs.value = true
  try {
    current.value.documents = await aiActivityApi.generateDocuments(current.value.id, { variables })
    current.value = await aiActivityApi.detail(current.value.id)
    pendingSupplementVariables.value = {}
    ElMessage.success('文档已生成')
    await loadSessions()
  } catch (error: any) {
    const missingFields = collectMissingFieldsFromError(error?.message || '')
    if (missingFields.length) {
      pendingSupplementVariables.value = Object.fromEntries(
        Object.entries(variables).map(([key, value]) => [key, String(value ?? '')]),
      )
      supplementFields.value = missingFields
      supplementVisible.value = true
    } else {
      throw error
    }
  } finally {
    generatingDocs.value = false
  }
}

function collectSupplementFields() {
  const fields = planStructuredFields()
  return supplementFieldOrder
    .filter((key) => needsSupplement(fields[key]))
    .map((key) => ({
      key,
      value: supplementFieldMeta[key]?.control === 'datetimeRange' ? [] : '',
      ...supplementFieldMeta[key],
    }))
}

function supplementFieldValue(field: SupplementField) {
  if (Array.isArray(field.value)) {
    const [start, end] = field.value
    if (!start || !end) return ''
    return `${formatDateTimeText(start)} 至 ${formatDateTimeText(end)}`
  }
  return field.value.trim()
}

function formatDateTimeText(value: string) {
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})$/)
  if (!match) return value
  const [, year, month, day, hour, minute] = match
  return `${year} 年 ${Number(month)} 月 ${Number(day)} 日 ${hour}:${minute}`
}

function collectMissingFieldsFromError(message: string) {
  const match = message.match(/缺少变量[:：]\s*(.+)$/)
  if (!match) return []
  return match[1]
    .split(',')
    .map((key) => key.trim())
    .filter(Boolean)
    .filter((key) => needsSupplement(pendingSupplementVariables.value[key]))
    .map((key) => {
      const meta = supplementFieldMeta[key]
      const label = meta?.label || '补充信息'
      return {
        key,
        value: meta?.control === 'datetimeRange' ? [] : '',
        label,
        placeholder: meta?.placeholder || `请输入${label}`,
        multiline: meta?.multiline,
        control: meta?.control,
      }
    })
}

function planStructuredFields() {
  const raw = latestPlan.value?.structuredFields
  if (!raw) return {}
  if (typeof raw === 'object') return raw as Record<string, unknown>
  try {
    return JSON.parse(raw) as Record<string, unknown>
  } catch {
    return {}
  }
}

function needsSupplement(value: unknown) {
  if (value === null || value === undefined) return true
  const text = String(value).trim()
  return !text || text.includes('待补充')
}

async function createActivityDraft() {
  if (!current.value) return
  const id = await aiActivityApi.createActivity(current.value.id)
  ElMessage.success(`活动草稿已创建：#${id}`)
  current.value = await aiActivityApi.detail(current.value.id)
}

function handleTemplateFile(uploadFile: any) {
  templateFile.value = uploadFile.raw || null
}

function clearTemplateFile() {
  templateFile.value = null
}

async function uploadTemplate() {
  if (!templateFile.value) {
    ElMessage.warning('请选择 docx 模板')
    return
  }
  if (!templateForm.value.templateName.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  uploadingTemplate.value = true
  try {
    await documentTemplateApi.upload({
      templateType: templateForm.value.templateType,
      templateName: templateForm.value.templateName,
      file: templateFile.value,
    })
    templateFile.value = null
    templateForm.value.templateName = ''
    ElMessage.success('模板已上传')
    await loadTemplates()
  } finally {
    uploadingTemplate.value = false
  }
}

async function saveAiSettings() {
  savingSettings.value = true
  testResult.value = ''
  try {
    const payload: Record<string, unknown> = {
      enabled: aiSettings.value.enabled,
      baseUrl: aiSettings.value.baseUrl,
      model: aiSettings.value.model,
      timeoutSeconds: aiSettings.value.timeoutSeconds,
    }
    if (aiSettings.value.apiKey) payload.apiKey = aiSettings.value.apiKey
    const data = await aiSettingsApi.update(payload)
    aiSettings.value.apiKey = ''
    aiSettings.value.hasApiKey = Boolean(data?.hasApiKey)
    aiSettings.value.apiKeyMasked = data?.apiKeyMasked || ''
    ElMessage.success('AI 配置已保存，正在发送测试指令')
    await testAiSettings()
  } finally {
    savingSettings.value = false
  }
}

async function testAiSettings() {
  testingSettings.value = true
  testResult.value = '正在发送测试指令...'
  try {
    const data = await aiSettingsApi.test()
    testResult.value = data?.message || 'DeepSeek 配置测试通过'
    ElMessage.success('DeepSeek 测试通过')
  } catch (error: any) {
    testResult.value = error?.message || 'DeepSeek 测试失败'
  } finally {
    testingSettings.value = false
  }
}

async function downloadDoc(doc: any) {
  const blob = await documentTemplateApi.downloadGenerated(doc.id)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = doc.fileName || 'generated.docx'
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  await Promise.all([loadSessions(), loadTemplates(), loadAiSettings()])
})
</script>

<style scoped>
.ai-activity-page {
  min-height: 100%;
  display: grid;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.eyebrow,
.subline,
.config-line,
.form-tip {
  color: var(--el-text-color-secondary);
}

.eyebrow {
  margin: 0 0 4px;
  font-size: 13px;
}

.page-head h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.25;
  letter-spacing: 0;
}

.subline {
  max-width: 720px;
  margin: 8px 0 0;
  font-size: 14px;
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.status-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.status-step {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 76px;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  transition:
    border-color 0.2s ease,
    background 0.2s ease;
}

.status-step.active {
  border-color: var(--el-color-primary);
  background: var(--el-fill-color-light);
}

.status-step.done {
  border-color: var(--el-color-success);
}

.status-step strong {
  display: block;
  font-size: 14px;
  line-height: 1.4;
}

.status-step small {
  display: block;
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.step-index {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.status-step.active .step-index {
  background: var(--el-color-primary);
  color: #fff;
}

.status-step.done .step-index {
  background: var(--el-color-success);
  color: #fff;
}

.workspace {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: start;
}

.session-panel-wrap,
.main-panel,
.ops-panel-wrap {
  min-height: 480px;
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.session-panel-wrap {
  display: flex;
  min-height: 520px;
  max-height: calc(100vh - 300px);
  position: sticky;
  top: 88px;
}

.main-panel {
  display: grid;
  gap: 14px;
  align-content: start;
}

.ops-panel-wrap {
  position: sticky;
  top: 88px;
  min-height: 0;
  max-height: calc(100vh - 104px);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.current-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.current-head h3 {
  margin: 0;
  font-size: 18px;
  line-height: 1.3;
}

.current-head p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.welcome-panel {
  display: grid;
  gap: 10px;
  justify-items: center;
  padding: 64px 24px;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.welcome-panel h3 {
  margin: 0;
  font-size: 18px;
  color: var(--el-text-color-primary);
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-fill-color-light);
}

.next-action {
  display: grid;
  gap: 2px;
}

.next-action span {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.next-action strong {
  font-size: 14px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.work-tabs {
  width: 100%;
}

.template-dialog {
  display: grid;
  gap: 16px;
}

.form-tip,
.test-result {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin: 6px 0 0;
}

@media (max-width: 1200px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .session-panel-wrap,
  .ops-panel-wrap {
    position: static;
    max-height: none;
  }

  .session-panel-wrap {
    min-height: 0;
  }
}
</style>