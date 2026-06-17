<template>
  <ViewPage class="ai-activity-page">
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
      <aside class="session-panel">
        <div class="panel-title">
          <span>活动会话</span>
          <el-button link :icon="Refresh" @click="loadSessions">刷新</el-button>
        </div>
        <div class="session-scroll">
          <div
            v-for="item in sessions"
            :key="item.id"
            class="session-item"
            :class="{ active: current?.id === item.id }"
          >
            <button class="session-main" type="button" @click="openSession(item.id)">
              <strong>{{ item.title }}</strong>
              <small>{{ statusText(item.status) }}</small>
            </button>
            <div class="session-actions">
              <el-tag size="small" :type="statusTypeOf(item.status)">{{ sessionBadge(item.status) }}</el-tag>
              <el-popconfirm
                title="确定删除这个对话吗？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                width="220"
                @confirm="deleteSession(item)"
              >
                <template #reference>
                  <el-button
                    class="session-delete"
                    link
                    type="danger"
                    :icon="Delete"
                    :disabled="deletingSessionId === item.id || streaming"
                    :loading="deletingSessionId === item.id"
                  />
                </template>
              </el-popconfirm>
            </div>
          </div>
          <div v-if="!sessions.length" class="empty-inline">
            <strong>还没有活动</strong>
            <span>从一个活动想法开始，AI 会帮你往下问。</span>
          </div>
        </div>
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
                {{ canGenerateDocuments ? '生成三份 docx' : '创建活动草稿' }}
              </el-button>
            </div>
          </div>

          <el-tabs v-model="activeTab" class="work-tabs">
            <el-tab-pane label="需求对话" name="chat">
              <div class="chat-surface">
                <div class="chat-scroll">
                  <div
                    v-for="message in current.messages || []"
                    :key="message.id"
                    class="message"
                    :class="`message--${message.role}`"
                  >
                    <div class="message-role">{{ message.role === 'user' ? '我' : 'AI' }}</div>
                    <div v-if="isStructuredAssistant(message)" class="ai-card">
                      <p v-if="structuredMessage(message).summary" class="ai-summary">
                        {{ structuredMessage(message).summary }}
                      </p>

                      <div v-if="structuredMessage(message).suggestions?.length" class="ai-block">
                        <strong>建议</strong>
                        <ul>
                          <li v-for="item in structuredMessage(message).suggestions" :key="item">{{ item }}</li>
                        </ul>
                      </div>

                      <div v-if="structuredMessage(message).questions?.length" class="ai-block">
                        <strong>需要补充</strong>
                        <div class="question-list">
                          <button
                            v-for="question in structuredMessage(message).questions"
                            :key="question"
                            type="button"
                            class="question-item"
                            @click="useQuestion(question)"
                          >
                            {{ question }}
                          </button>
                        </div>
                      </div>

                      <div v-if="structuredMessage(message).missingFields?.length" class="field-tags">
                        <el-tag
                          v-for="field in structuredMessage(message).missingFields"
                          :key="field"
                          size="small"
                          type="info"
                        >
                          {{ field }}
                        </el-tag>
                      </div>
                    </div>
                    <pre v-else>{{ displayMessage(message) }}</pre>
                  </div>
                  <div v-if="aiResponsePending" class="message message--assistant">
                    <div class="message-role">AI</div>
                    <div class="ai-skeleton">
                      <span />
                      <span />
                      <span />
                    </div>
                  </div>
                </div>
                <div class="composer">
                  <el-input
                    v-model="messageText"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 6 }"
                    placeholder="补充活动时间、地点、人数、预算、志愿者要求，或直接让 AI 继续追问..."
                    @keydown.meta.enter.prevent="sendMessage"
                    @keydown.ctrl.enter.prevent="sendMessage"
                  />
                  <el-button type="primary" :loading="sending" :disabled="streaming" @click="sendMessage">发送</el-button>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="策划案" name="plan">
              <div v-if="hasPlanEditorContent" class="plan-editor">
                <div class="plan-toolbar">
                  <div>
                    <strong>{{ latestPlan?.title || 'AI 正在生成策划案' }}</strong>
                    <span>{{ streamPhase || `版本 ${latestPlan?.version || 1} · ${latestPlan?.status || 'draft'}` }}</span>
                  </div>
                  <el-button :loading="revising" :disabled="streaming || !reviseInstruction.trim()" @click="revisePlan">
                    按要求修改
                  </el-button>
                </div>
                <el-input v-model="planText" type="textarea" :rows="20" resize="vertical" />
                <el-input
                  v-model="reviseInstruction"
                  class="revise-input"
                  placeholder="例如：把活动流程写得更细，把志愿者职责独立成一段"
                />
              </div>
              <div v-else class="empty-state">
                <strong>还没有策划案</strong>
                <span>先确认需求，然后点击“生成策划案”。</span>
                <el-button type="primary" :loading="generatingPlan" :disabled="streaming" @click="generatePlan">生成策划案</el-button>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
      </main>

      <aside class="ops-panel">
        <section class="ops-section">
          <div class="panel-title">
            <span>模板状态</span>
            <el-button link @click="templateVisible = true">管理</el-button>
          </div>
          <div class="template-list">
            <div v-for="type in documentTypes" :key="type.value" class="template-row">
              <div>
                <strong>{{ type.label }}</strong>
                <small>{{ templateByType(type.value)?.templateName || '请上传 docx 模板' }}</small>
              </div>
              <el-tag :type="templateByType(type.value) ? 'success' : 'warning'" size="small">
                {{ templateByType(type.value) ? '就绪' : '缺失' }}
              </el-tag>
            </div>
          </div>
        </section>

        <section class="ops-section">
          <div class="panel-title">
            <span>生成材料</span>
            <el-tag size="small" type="info">{{ current?.documents?.length || 0 }} 份</el-tag>
          </div>
          <el-button
            class="full-btn"
            type="primary"
            :disabled="streaming || !canGenerateDocuments"
            :loading="generatingDocs"
            @click="generateDocuments"
          >
            生成三份 docx
          </el-button>
          <div class="doc-list">
            <div v-for="doc in current?.documents || []" :key="doc.id" class="doc-item">
              <span>{{ doc.fileName }}</span>
              <el-button link type="primary" @click="downloadDoc(doc)">下载</el-button>
            </div>
            <div v-if="!current?.documents?.length" class="empty-mini">策划案确认后生成正式材料。</div>
          </div>
        </section>

        <section class="ops-section">
          <div class="panel-title">
            <span>AI 配置</span>
            <el-tag size="small" :type="aiSettings.hasApiKey ? 'success' : 'warning'">
              {{ aiSettings.hasApiKey ? '已配置' : '未配置' }}
            </el-tag>
          </div>
          <p class="config-line">{{ aiSettings.model || 'deepseek-chat' }}</p>
          <p class="config-line">{{ aiSettings.baseUrl || 'https://api.deepseek.com' }}</p>
        </section>
      </aside>
    </div>

    <el-dialog v-model="newSessionVisible" title="新建 AI 活动" width="680px">
      <el-form label-width="96px">
        <el-form-item label="会话标题">
          <el-input v-model="newSession.title" placeholder="可选，例如：新生开源破冰活动" />
        </el-form-item>
        <el-form-item label="活动需求">
          <el-input
            v-model="newSession.initialMessage"
            type="textarea"
            :rows="7"
            placeholder="例如：我想办一个面向新生的开源社团破冰活动，轻松一点，让大家认识社团项目。"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newSessionVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createSession">开始澄清需求</el-button>
      </template>
    </el-dialog>

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

    <el-dialog v-model="supplementVisible" title="补充材料生成信息" width="720px">
      <div class="supplement-dialog">
        <el-alert
          title="这些信息会写入正式申请材料，未填写的字段将继续保留“待补充”。"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-form label-width="128px">
          <el-form-item v-for="field in supplementFields" :key="field.key" :label="field.label">
            <el-date-picker
              v-if="field.control === 'datetimeRange'"
              v-model="field.value"
              class="supplement-date-picker"
              type="datetimerange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              range-separator="至"
              value-format="YYYY-MM-DD HH:mm"
              format="YYYY 年 MM 月 DD 日 HH:mm"
            />
            <el-input
              v-else
              v-model="field.value"
              :type="field.multiline ? 'textarea' : 'text'"
              :rows="field.multiline ? 3 : undefined"
              :placeholder="field.placeholder"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="supplementVisible = false">取消</el-button>
        <el-button type="primary" :loading="generatingDocs" @click="submitSupplementAndGenerate">
          生成正式材料
        </el-button>
      </template>
    </el-dialog>

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
const newSession = ref({ title: '', initialMessage: '' })
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
  activityDateRange: { label: '活动时间', placeholder: '请选择活动开始和结束时间', control: 'datetimeRange' },
  registrationDateRange: { label: '报名时间', placeholder: '请选择报名开始和结束时间', control: 'datetimeRange' },
  location: { label: '活动地点', placeholder: '例如：教学楼 A101 / 学生活动中心报告厅' },
  expectedParticipants: { label: '活动人数', placeholder: '例如：100 人' },
  registrationQuota: { label: '报名名额', placeholder: '例如：100 人' },
  volunteerCount: { label: '志愿者人数', placeholder: '例如：10 人' },
  principalName: { label: '负责人', placeholder: '例如：张三' },
  principalPhone: { label: '负责人电话', placeholder: '例如：13800000000' },
  advisorName: { label: '指导老师', placeholder: '例如：李老师' },
  checkinStudentId: { label: '签到员学号', placeholder: '例如：2026xxxxxx' },
  budgetTotal: { label: '预算总额', placeholder: '例如：300 元' },
  budgetDetails: { label: '预算明细', placeholder: '例如：物资 150 元，奖品 100 元，打印 50 元', multiline: true },
}

const supplementFieldOrder = Object.keys(supplementFieldMeta)

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
    { key: 'docs', title: '生成材料', description: '输出三份 PU docx', index: '04' },
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
  if (status === 'plan_confirmed') return '策划案已确认，可以生成三份 docx 材料。'
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
  if (canGenerateDocuments.value) return '生成三份 docx'
  if (canCreateActivityDraft.value) return '创建活动草稿'
  return '继续补充信息'
})

watch(latestPlan, (plan) => {
  planText.value = plan?.contentMarkdown || ''
  if (plan) activeTab.value = 'plan'
})

function statusText(status: string) {
  return (
    {
      drafting: '澄清中',
      requirement_confirmed: '需求已确认',
      plan_generated: '策划案待确认',
      plan_confirmed: '策划案已确认',
      documents_generated: '文档已生成',
      submitted: '已提交',
      archived: '已归档',
    }[status] || status
  )
}

function sessionBadge(status: string) {
  if (status === 'documents_generated') return '完成'
  if (status === 'plan_confirmed') return '文档'
  if (status === 'plan_generated') return '审稿'
  if (status === 'requirement_confirmed') return '写稿'
  return '对话'
}

function statusTypeOf(status: string) {
  if (status === 'documents_generated' || status === 'plan_confirmed') return 'success'
  if (status === 'plan_generated' || status === 'requirement_confirmed') return 'warning'
  return 'info'
}

function documentTypeLabel(value: string) {
  return documentTypes.find((item) => item.value === value)?.label || value
}

function structuredMessage(message: any) {
  if (!message?.structuredPayload) return {}
  if (typeof message.structuredPayload === 'object') return message.structuredPayload
  try {
    return JSON.parse(message.structuredPayload)
  } catch {
    return {}
  }
}

function isStructuredAssistant(message: any) {
  return message?.role === 'assistant' && Boolean(message?.structuredPayload) && Object.keys(structuredMessage(message)).length > 0
}

function displayMessage(message: any) {
  if (message.structuredPayload) {
    try {
      const parsed = structuredMessage(message)
      return parsed.summary || message.content
    } catch {
      return message.content
    }
  }
  if (message.role === 'assistant') {
    return requirementStreamPreview(message.content) || message.content
  }
  return message.content
}

function requirementStreamPreview(content: string) {
  const text = (content || '').trim()
  if (!text.startsWith('{')) return ''
  try {
    return structuredPreviewText(JSON.parse(text))
  } catch {
    const preview: string[] = []
    const summary = extractJsonString(text, 'summary')
    if (summary) preview.push(summary)
    const suggestions = extractJsonArrayStrings(text, 'suggestions')
    if (suggestions.length) preview.push(`建议：\n${suggestions.map((item) => `- ${item}`).join('\n')}`)
    const questions = extractJsonArrayStrings(text, 'questions')
    if (questions.length) preview.push(`需要补充：\n${questions.map((item) => `- ${item}`).join('\n')}`)
    const missingFields = extractJsonArrayStrings(text, 'missingFields')
    if (missingFields.length) preview.push(`缺失字段：${missingFields.join('、')}`)
    return preview.join('\n\n') || 'AI 正在整理需求...'
  }
}

function structuredPreviewText(payload: any) {
  const preview: string[] = []
  if (payload?.summary) preview.push(String(payload.summary))
  if (Array.isArray(payload?.suggestions) && payload.suggestions.length) {
    preview.push(`建议：\n${payload.suggestions.map((item: string) => `- ${item}`).join('\n')}`)
  }
  if (Array.isArray(payload?.questions) && payload.questions.length) {
    preview.push(`需要补充：\n${payload.questions.map((item: string) => `- ${item}`).join('\n')}`)
  }
  if (Array.isArray(payload?.missingFields) && payload.missingFields.length) {
    preview.push(`缺失字段：${payload.missingFields.join('、')}`)
  }
  return preview.join('\n\n')
}

function extractJsonString(text: string, key: string) {
  const match = text.match(new RegExp(`"${key}"\\s*:\\s*"((?:\\\\.|[^"\\\\])*)"`))
  return match ? unescapeJsonString(match[1]) : ''
}

function extractJsonArrayStrings(text: string, key: string) {
  const match = text.match(new RegExp(`"${key}"\\s*:\\s*\\[([\\s\\S]*?)(?:\\]|$)`))
  if (!match) return []
  const values: string[] = []
  const itemPattern = /"((?:\\.|[^"\\])*)"/g
  let item: RegExpExecArray | null
  while ((item = itemPattern.exec(match[1])) !== null) values.push(unescapeJsonString(item[1]))
  return values
}

function unescapeJsonString(value: string) {
  try {
    return JSON.parse(`"${value}"`)
  } catch {
    return value
  }
}

function useQuestion(question: string) {
  messageText.value = messageText.value ? `${messageText.value}\n${question}：` : `${question}：`
}

function templateByType(type: string) {
  return templates.value.find((item) => item.templateType === type && item.status === 'enabled')
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

async function createSession() {
  if (!newSession.value.initialMessage.trim()) {
    ElMessage.warning('请先输入活动需求')
    return
  }
  creating.value = true
  streaming.value = true
  aiResponsePending.value = true
  streamPhase.value = 'AI 正在澄清活动需求'
  try {
    await runAiStream('/ai/activity/sessions/stream', newSession.value)
    activeTab.value = 'chat'
    newSessionVisible.value = false
    newSession.value = { title: '', initialMessage: '' }
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
    .map((key) => ({
      key,
      value: supplementFieldMeta[key]?.control === 'datetimeRange' ? [] : '',
      label: supplementFieldMeta[key]?.label || key,
      placeholder: supplementFieldMeta[key]?.placeholder || `请输入 ${key}`,
      multiline: supplementFieldMeta[key]?.multiline,
      control: supplementFieldMeta[key]?.control,
    }))
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
}

.status-step strong,
.template-row strong {
  display: block;
  font-size: 14px;
  line-height: 1.4;
}

.status-step small,
.template-row small,
.session-main small {
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
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 700;
}

.status-step.current {
  border-color: var(--el-color-primary);
}

.status-step.current .step-index,
.status-step.done .step-index {
  background: var(--el-color-primary);
  color: var(--el-color-primary-foreground, #fff);
}

.workspace {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 340px;
  gap: 16px;
  min-height: calc(100vh - 300px);
}

.session-panel,
.main-panel,
.ops-panel {
  min-width: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.session-panel,
.ops-panel {
  padding: 14px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 700;
}

.session-scroll {
  display: grid;
  gap: 8px;
  max-height: calc(100vh - 360px);
  overflow: auto;
}

.session-item {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 11px;
  background: transparent;
  color: var(--el-text-color-primary);
  text-align: left;
}

.session-item:hover,
.session-item.active {
  border-color: var(--el-border-color);
  background: var(--el-fill-color-light);
}

.session-main strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.session-main {
  min-width: 0;
  border: 0;
  padding: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.session-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.session-delete {
  width: 24px;
  height: 24px;
}

.main-panel {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  overflow: hidden;
}

.current-head,
.action-bar {
  border-bottom: 1px solid var(--el-border-color-light);
}

.current-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
}

.current-head h3,
.welcome-panel h3 {
  margin: 0;
  font-size: 18px;
  line-height: 1.35;
}

.current-head p,
.welcome-panel p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.welcome-panel {
  align-self: center;
  justify-self: center;
  width: min(520px, calc(100% - 32px));
  padding: 28px;
  text-align: center;
}

.welcome-panel .el-button {
  margin-top: 16px;
}

.action-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 16px;
  background: var(--el-fill-color-light);
}

.next-action {
  display: grid;
  gap: 2px;
  min-width: 150px;
}

.next-action span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.3;
}

.next-action strong {
  font-size: 14px;
  line-height: 1.35;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.action-buttons :deep(.el-button + .el-button) {
  margin-left: 0;
}

.work-tabs {
  min-height: 0;
  padding: 0 16px 16px;
}

.work-tabs :deep(.el-tabs__content) {
  min-height: 0;
}

.chat-surface {
  display: grid;
  grid-template-rows: minmax(360px, 1fr) auto;
  min-height: 560px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.chat-scroll {
  padding: 16px;
  overflow: auto;
}

.message {
  max-width: 82%;
  margin-bottom: 14px;
}

.message--user {
  margin-left: auto;
}

.message-role {
  margin-bottom: 5px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.message pre {
  margin: 0;
  padding: 12px 14px;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  font-family: inherit;
  font-size: 14px;
  line-height: 1.65;
}

.message--user pre {
  background: var(--el-color-primary-light-9);
}

.ai-card {
  display: grid;
  gap: 12px;
  padding: 13px 14px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  font-size: 14px;
  line-height: 1.6;
}

.ai-summary {
  margin: 0;
  color: var(--el-text-color-primary);
}

.ai-block {
  display: grid;
  gap: 8px;
}

.ai-block strong {
  font-size: 13px;
}

.ai-block ul {
  margin: 0;
  padding-left: 18px;
}

.ai-block li + li {
  margin-top: 4px;
}

.question-list {
  display: grid;
  gap: 8px;
}

.question-item {
  width: 100%;
  padding: 9px 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  text-align: left;
  line-height: 1.5;
  cursor: pointer;
}

.question-item:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.field-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ai-skeleton {
  display: grid;
  gap: 8px;
  width: min(360px, 100%);
  padding: 13px 14px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.ai-skeleton span {
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    var(--el-fill-color) 0%,
    var(--el-fill-color-dark) 50%,
    var(--el-fill-color) 100%
  );
  background-size: 200% 100%;
  animation: ai-loading 1.2s ease-in-out infinite;
}

.ai-skeleton span:nth-child(2) {
  width: 86%;
}

.ai-skeleton span:nth-child(3) {
  width: 62%;
}

@keyframes ai-loading {
  from {
    background-position: 100% 0;
  }

  to {
    background-position: -100% 0;
  }
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 88px;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}

.plan-editor {
  display: grid;
  gap: 10px;
}

.plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.plan-toolbar span {
  display: block;
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.revise-input {
  max-width: 760px;
}

.empty-state,
.empty-inline,
.empty-mini {
  display: grid;
  gap: 8px;
  justify-items: start;
  padding: 18px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.empty-state {
  justify-items: center;
  text-align: center;
}

.ops-panel {
  display: grid;
  align-content: start;
  gap: 14px;
}

.ops-section {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.ops-section:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.template-list,
.doc-list {
  display: grid;
  gap: 8px;
}

.template-row,
.doc-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.doc-item span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.full-btn {
  width: 100%;
  margin-bottom: 10px;
}

.config-line {
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.form-tip {
  width: 100%;
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
}

.test-result {
  margin: 12px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.template-dialog,
.supplement-dialog {
  display: grid;
  gap: 12px;
}

.supplement-dialog :deep(.el-alert) {
  align-items: flex-start;
}

.supplement-date-picker {
  width: 100%;
}

@media (max-width: 1280px) {
  .workspace {
    grid-template-columns: 250px minmax(0, 1fr);
  }

  .ops-panel {
    grid-column: 1 / -1;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .ops-section {
    border-bottom: 0;
    padding-bottom: 0;
  }
}

@media (max-width: 860px) {
  .page-head,
  .current-head,
  .plan-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-strip,
  .workspace,
  .ops-panel {
    grid-template-columns: 1fr;
  }

  .session-scroll {
    max-height: none;
  }

  .composer {
    grid-template-columns: 1fr;
  }

  .action-bar,
  .action-buttons {
    align-items: stretch;
    flex-direction: column;
  }

  .action-buttons :deep(.el-button) {
    width: 100%;
  }
}
</style>
