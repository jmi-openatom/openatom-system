<template>
  <ViewPage class="ai-activity-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-button type="primary" :icon="Plus" @click="newSessionVisible = true">新建 AI 活动</el-button>
        <el-button :icon="Refresh" @click="loadSessions">刷新</el-button>
      </div>
      <div class="toolbar__actions">
        <el-button :icon="Setting" @click="settingsVisible = true">AI配置</el-button>
        <el-button :icon="Upload" @click="templateVisible = true">上传模板</el-button>
      </div>
    </ViewToolbar>

    <div class="workspace">
      <aside class="session-list">
        <button
          v-for="item in sessions"
          :key="item.id"
          class="session-item"
          :class="{ active: current?.id === item.id }"
          @click="openSession(item.id)"
        >
          <span class="session-title">{{ item.title }}</span>
          <el-tag size="small" :type="statusTypeOf(item.status)">{{ statusText(item.status) }}</el-tag>
        </button>
        <el-empty v-if="!sessions.length" description="暂无 AI 活动会话" />
      </aside>

      <main class="chat-panel">
        <div v-if="current" class="chat-scroll">
          <div
            v-for="message in current.messages || []"
            :key="message.id"
            class="message"
            :class="`message--${message.role}`"
          >
            <div class="message-role">{{ message.role === 'user' ? '我' : 'AI' }}</div>
            <pre>{{ displayMessage(message) }}</pre>
          </div>
        </div>
        <el-empty v-else description="选择或新建一个活动会话" />

        <div class="chat-input">
          <el-input
            v-model="messageText"
            type="textarea"
            :rows="3"
            placeholder="继续补充活动想法、时间地点、预算、志愿者需求，或要求 AI 调整建议..."
          />
          <el-button type="primary" :loading="sending" :disabled="!current" @click="sendMessage">
            发送
          </el-button>
        </div>
      </main>

      <aside class="side-panel">
        <section class="section">
          <h3>流程</h3>
          <el-steps :active="stepActive" direction="vertical" finish-status="success">
            <el-step title="需求澄清" />
            <el-step title="需求确认" />
            <el-step title="策划案" />
            <el-step title="文档生成" />
          </el-steps>
          <div class="actions">
            <el-button :disabled="!current" @click="confirmRequirement">确认需求</el-button>
            <el-button type="primary" :disabled="!current" :loading="generatingPlan" @click="generatePlan">
              生成策划案
            </el-button>
            <el-button type="success" :disabled="!latestPlan" @click="confirmPlan">确认策划案</el-button>
          </div>
        </section>

        <section class="section">
          <h3>策划案</h3>
          <div v-if="latestPlan" class="plan-box">
            <div class="plan-meta">
              <strong>{{ latestPlan.title }}</strong>
              <el-tag size="small">{{ latestPlan.status }}</el-tag>
            </div>
            <el-input v-model="planText" type="textarea" :rows="12" />
            <el-input
              v-model="reviseInstruction"
              class="mt"
              placeholder="输入局部修改要求，例如：预算写得更详细"
            />
            <el-button class="mt" :loading="revising" @click="revisePlan">按要求修改</el-button>
          </div>
          <el-empty v-else description="尚未生成策划案" />
        </section>

        <section class="section">
          <h3>文档</h3>
          <div class="template-status">
            <div v-for="type in documentTypes" :key="type.value">
              <span>{{ type.label }}</span>
              <el-tag :type="templateByType(type.value) ? 'success' : 'warning'" size="small">
                {{ templateByType(type.value) ? '已配置' : '缺模板' }}
              </el-tag>
            </div>
          </div>
          <el-button type="primary" :disabled="!latestPlan" :loading="generatingDocs" @click="generateDocuments">
            生成三份 docx
          </el-button>
          <el-button :disabled="!current" @click="createActivityDraft">创建活动草稿</el-button>
          <div class="doc-list">
            <div v-for="doc in current?.documents || []" :key="doc.id" class="doc-item">
              <span>{{ doc.fileName }}</span>
              <el-button link type="primary" @click="downloadDoc(doc)">下载</el-button>
            </div>
          </div>
        </section>
      </aside>
    </div>

    <el-dialog v-model="newSessionVisible" title="新建 AI 活动" width="640px">
      <el-form label-width="96px">
        <el-form-item label="会话标题">
          <el-input v-model="newSession.title" placeholder="可选，例如：新生开源破冰活动" />
        </el-form-item>
        <el-form-item label="活动需求">
          <el-input
            v-model="newSession.initialMessage"
            type="textarea"
            :rows="6"
            placeholder="例如：我想办一个面向新生的开源社团破冰活动，轻松一点，让大家认识社团项目。"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newSessionVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createSession">开始</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="templateVisible" title="上传 Word 模板" width="760px">
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
        <el-table-column prop="templateName" label="模板" min-width="160" />
        <el-table-column prop="templateType" label="类型" min-width="160">
          <template #default="{ row }">{{ documentTypeLabel(row.templateType) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column prop="createdAt" label="上传时间" width="170" />
      </el-table>
      <template #footer>
        <el-button @click="templateVisible = false">关闭</el-button>
        <el-button type="primary" :loading="uploadingTemplate" @click="uploadTemplate">上传</el-button>
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
        <el-button type="primary" :loading="savingSettings" @click="saveAiSettings">保存配置</el-button>
      </template>
    </el-drawer>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { aiActivityApi, aiSettingsApi, documentTemplateApi } from '@/api/index.ts'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Plus, Refresh, Setting, Upload } from '@element-plus/icons-vue'
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
const newSessionVisible = ref(false)
const templateVisible = ref(false)
const settingsVisible = ref(false)
const creating = ref(false)
const sending = ref(false)
const generatingPlan = ref(false)
const revising = ref(false)
const generatingDocs = ref(false)
const uploadingTemplate = ref(false)
const savingSettings = ref(false)
const templateFile = ref<File | null>(null)
const newSession = ref({ title: '', initialMessage: '' })
const templateForm = ref({
  templateType: 'activity_proposal',
  templateName: '',
})
const aiSettings = ref<Record<string, any>>({
  enabled: true,
  baseUrl: 'https://api.deepseek.com',
  model: 'deepseek-chat',
  apiKey: '',
  timeoutSeconds: 30,
  hasApiKey: false,
  apiKeyMasked: '',
})

const latestPlan = computed(() => {
  const plans = current.value?.plans || []
  return plans.length ? plans[0] : null
})

const stepActive = computed(() => {
  const status = current.value?.status
  if (status === 'documents_generated' || status === 'submitted') return 4
  if (status === 'plan_confirmed' || status === 'plan_generated') return 3
  if (status === 'requirement_confirmed') return 2
  return current.value ? 1 : 0
})

watch(latestPlan, (plan) => {
  planText.value = plan?.contentMarkdown || ''
})

function statusText(status: string) {
  return (
    {
      drafting: '澄清中',
      requirement_confirmed: '需求已确认',
      plan_generated: '已生成策划案',
      plan_confirmed: '策划案已确认',
      documents_generated: '文档已生成',
      submitted: '已提交',
      archived: '已归档',
    }[status] || status
  )
}

function statusTypeOf(status: string) {
  if (status === 'documents_generated' || status === 'plan_confirmed') return 'success'
  if (status === 'plan_generated' || status === 'requirement_confirmed') return 'warning'
  return 'info'
}

function documentTypeLabel(value: string) {
  return documentTypes.find((item) => item.value === value)?.label || value
}

function displayMessage(message: any) {
  if (message.structuredPayload) {
    try {
      return JSON.stringify(JSON.parse(message.structuredPayload), null, 2)
    } catch {
      return message.content
    }
  }
  return message.content
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
}

async function createSession() {
  if (!newSession.value.initialMessage.trim()) {
    ElMessage.warning('请先输入活动需求')
    return
  }
  creating.value = true
  try {
    current.value = await aiActivityApi.createSession(newSession.value)
    newSessionVisible.value = false
    newSession.value = { title: '', initialMessage: '' }
    await loadSessions()
  } finally {
    creating.value = false
  }
}

async function sendMessage() {
  if (!current.value || !messageText.value.trim()) return
  sending.value = true
  try {
    current.value = await aiActivityApi.sendMessage(current.value.id, {
      message: messageText.value,
      mode: 'requirement_clarification',
    })
    messageText.value = ''
    await loadSessions()
  } finally {
    sending.value = false
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
  generatingPlan.value = true
  try {
    current.value = await aiActivityApi.generatePlan(current.value.id)
    ElMessage.success('策划案已生成')
    await loadSessions()
  } finally {
    generatingPlan.value = false
  }
}

async function revisePlan() {
  if (!current.value || !reviseInstruction.value.trim()) return
  revising.value = true
  try {
    current.value = await aiActivityApi.revisePlan(current.value.id, {
      instruction: `${reviseInstruction.value}\n\n用户当前编辑稿：\n${planText.value}`,
    })
    reviseInstruction.value = ''
    ElMessage.success('策划案已修改')
  } finally {
    revising.value = false
  }
}

async function confirmPlan() {
  if (!current.value) return
  current.value = await aiActivityApi.confirmPlan(current.value.id)
  ElMessage.success('策划案已确认')
  await loadSessions()
}

async function generateDocuments() {
  if (!current.value) return
  generatingDocs.value = true
  try {
    current.value.documents = await aiActivityApi.generateDocuments(current.value.id, {})
    current.value = await aiActivityApi.detail(current.value.id)
    ElMessage.success('文档已生成')
    await loadSessions()
  } finally {
    generatingDocs.value = false
  }
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
  try {
    const payload: Record<string, unknown> = {
      enabled: aiSettings.value.enabled,
      baseUrl: aiSettings.value.baseUrl,
      model: aiSettings.value.model,
      timeoutSeconds: aiSettings.value.timeoutSeconds,
    }
    if (aiSettings.value.apiKey) {
      payload.apiKey = aiSettings.value.apiKey
    }
    const data = await aiSettingsApi.update(payload)
    aiSettings.value.apiKey = ''
    aiSettings.value.hasApiKey = Boolean(data?.hasApiKey)
    aiSettings.value.apiKeyMasked = data?.apiKeyMasked || ''
    ElMessage.success('AI 配置已保存')
  } finally {
    savingSettings.value = false
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
}

.workspace {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 420px;
  gap: 16px;
  min-height: calc(100vh - 190px);
}

.session-list,
.chat-panel,
.side-panel {
  min-width: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.session-list {
  padding: 10px;
  overflow: auto;
}

.session-item {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  border: 0;
  border-radius: 6px;
  padding: 10px;
  background: transparent;
  color: var(--el-text-color-primary);
  text-align: left;
  cursor: pointer;
}

.session-item.active,
.session-item:hover {
  background: var(--el-fill-color-light);
}

.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  overflow: hidden;
}

.chat-scroll {
  padding: 16px;
  overflow: auto;
}

.message {
  max-width: 86%;
  margin-bottom: 14px;
}

.message--user {
  margin-left: auto;
}

.message-role {
  margin-bottom: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.message pre {
  margin: 0;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  font-family: inherit;
  line-height: 1.65;
}

.message--user pre {
  background: var(--el-color-primary-light-9);
}

.chat-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--el-border-color-light);
}

.side-panel {
  padding: 14px;
  overflow: auto;
}

.section + .section {
  margin-top: 20px;
}

.section h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.plan-box {
  display: grid;
  gap: 8px;
}

.plan-meta,
.template-status > div,
.doc-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.template-status {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.doc-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.doc-item span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-tip {
  width: 100%;
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.mt {
  margin-top: 8px;
}

@media (max-width: 1180px) {
  .workspace {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .side-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .chat-input {
    grid-template-columns: 1fr;
  }
}
</style>
