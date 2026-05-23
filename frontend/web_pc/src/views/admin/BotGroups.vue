<template>
  <ViewPage class="admin-page bot-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索群号 / 群名"
          style="width: 220px"
          @keyup.enter="fetchGroups"
        />
        <el-select v-model="query.botEnabled" clearable placeholder="机器人状态" style="width: 150px">
          <el-option label="启用" value="true" />
          <el-option label="禁用" value="false" />
        </el-select>
        <el-select v-model="query.mode" clearable placeholder="响应模式" style="width: 160px">
          <el-option v-for="item in modeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="fetchGroups">查询</el-button>
        <el-button :icon="Refresh" :loading="syncingGroups" @click="syncGroups">同步群列表</el-button>
      </div>
      <div class="toolbar__actions">
        <el-button :disabled="!selectedRows.length" @click="openBatchDialog">
          批量配置
        </el-button>
        <el-button :icon="RefreshRight" @click="fetchAll">刷新</el-button>
      </div>
    </ViewToolbar>

    <section class="summary-grid">
      <div class="summary-item">
        <span>NapCat</span>
        <strong :class="{ online: overview.napCatOnline }">
          {{ overview.napCatOnline ? '已连接' : '未连接' }}
        </strong>
      </div>
      <div class="summary-item">
        <span>已加入群</span>
        <strong>{{ overview.groupCount || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>启用群</span>
        <strong>{{ overview.enabledGroupCount || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>今日消息</span>
        <strong>{{ overview.todayMessageCount || 0 }}</strong>
      </div>
      <div class="summary-item">
        <span>待处理入群</span>
        <strong>{{ overview.pendingJoinRequestCount || 0 }}</strong>
      </div>
    </section>

    <div class="workspace">
      <section class="group-panel">
        <el-table
          v-loading="loading"
          :data="groups"
          class="admin-table"
          row-key="groupId"
          highlight-current-row
          @selection-change="handleSelectionChange"
          @row-click="selectGroup"
        >
          <el-table-column type="selection" width="46" />
          <el-table-column label="QQ群" min-width="220">
            <template #default="{ row }">
              <strong>{{ row.groupName || '未命名群' }}</strong>
              <p class="muted-line">{{ row.groupId }}</p>
            </template>
          </el-table-column>
          <el-table-column label="人数" width="90">
            <template #default="{ row }">{{ row.memberCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="机器人" width="120">
            <template #default="{ row }">
              <el-tag :type="row.botEnabled ? 'success' : 'danger'">
                {{ row.botEnabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="模式" width="130">
            <template #default="{ row }">{{ modeText(row.mode) }}</template>
          </el-table-column>
          <el-table-column label="最后活跃" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.lastActiveAt || row.lastSyncedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openConfig(row)">配置</el-button>
              <el-button link type="success" :loading="syncingMembers === row.groupId" @click.stop="syncMembers(row)">
                同步成员
              </el-button>
              <el-button link type="warning" @click.stop="confirmMuteAll(row, true)">全禁</el-button>
              <el-button link type="info" @click.stop="confirmMuteAll(row, false)">解全禁</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !groups.length" description="暂无 QQ 群，请先同步 NapCat 群列表" />
      </section>

      <section class="detail-panel">
        <template v-if="activeGroup">
          <div class="detail-head">
            <div>
              <h2>{{ activeGroup.groupName || '未命名群' }}</h2>
              <p>{{ activeGroup.groupId }} · {{ modeText(activeGroup.mode) }}</p>
            </div>
            <el-button type="primary" :icon="Setting" @click="openConfig(activeGroup)">群配置</el-button>
          </div>

          <el-tabs v-model="activeTab" class="detail-tabs">
            <el-tab-pane label="成员" name="members">
              <div class="inline-toolbar">
                <el-input
                  v-model="memberQuery.keyword"
                  clearable
                  placeholder="搜索 QQ / 昵称 / 群名片"
                  style="width: 240px"
                  @keyup.enter="fetchMembers"
                />
                <el-select v-model="memberQuery.role" clearable placeholder="身份" style="width: 130px">
                  <el-option label="群主" value="owner" />
                  <el-option label="管理员" value="admin" />
                  <el-option label="成员" value="member" />
                </el-select>
                <el-select v-model="memberQuery.muteStatus" clearable placeholder="禁言状态" style="width: 140px">
                  <el-option label="正常" value="normal" />
                  <el-option label="禁言中" value="muted" />
                </el-select>
                <el-button :icon="Search" @click="fetchMembers">筛选</el-button>
              </div>
              <el-table v-loading="membersLoading" :data="members" class="admin-table compact-table" height="430">
                <el-table-column label="成员" min-width="180">
                  <template #default="{ row }">
                    <strong>{{ row.card || row.nickname || row.userId }}</strong>
                    <p class="muted-line">{{ row.userId }}</p>
                  </template>
                </el-table-column>
                <el-table-column label="身份" width="100">
                  <template #default="{ row }">{{ roleText(row.role) }}</template>
                </el-table-column>
                <el-table-column label="禁言" width="120">
                  <template #default="{ row }">
                    <el-tag :type="row.muted ? 'danger' : 'success'">
                      {{ row.muted ? '禁言中' : '正常' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="最后发言" min-width="150">
                  <template #default="{ row }">{{ formatDateTime(row.lastSentTime) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="220" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="warning" @click="muteMember(row, 600)">禁言10分钟</el-button>
                    <el-button link type="primary" @click="customMute(row)">自定义</el-button>
                    <el-button link type="success" @click="muteMember(row, 0)">解禁</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="公告" name="announcements">
              <el-form class="announce-form" :model="announcementForm" label-width="72px">
                <el-form-item label="标题">
                  <el-input v-model="announcementForm.title" placeholder="公告标题" />
                </el-form-item>
                <el-form-item label="正文">
                  <el-input v-model="announcementForm.content" type="textarea" :rows="4" placeholder="公告正文" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="announcementSaving" @click="publishAnnouncement">
                    发布公告
                  </el-button>
                </el-form-item>
              </el-form>
              <el-table :data="announcements" class="admin-table compact-table" height="300">
                <el-table-column prop="title" label="公告" min-width="180" />
                <el-table-column label="状态" width="110">
                  <template #default="{ row }">
                    <el-tag :type="statusType(row.status)">{{ announcementStatusText(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="发布时间" min-width="160">
                  <template #default="{ row }">{{ formatDateTime(row.publishedAt || row.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="160">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="republishAnnouncement(row)">重发</el-button>
                    <el-button link type="danger" @click="deleteAnnouncement(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="入群申请" name="requests">
              <el-table :data="joinRequests" class="admin-table compact-table" height="460">
                <el-table-column label="申请人" min-width="180">
                  <template #default="{ row }">
                    <strong>{{ row.nickname || row.userId || '未知用户' }}</strong>
                    <p class="muted-line">{{ row.userId || row.requestId }}</p>
                  </template>
                </el-table-column>
                <el-table-column prop="comment" label="申请理由" min-width="220" show-overflow-tooltip />
                <el-table-column label="状态" width="110">
                  <template #default="{ row }">
                    <el-tag :type="statusType(row.status)">{{ requestStatusText(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="申请时间" min-width="160">
                  <template #default="{ row }">{{ formatDateTime(row.requestedAt || row.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="160">
                  <template #default="{ row }">
                    <el-button v-if="row.status === 'pending'" link type="success" @click="handleJoinRequest(row, true)">
                      同意
                    </el-button>
                    <el-button v-if="row.status === 'pending'" link type="danger" @click="handleJoinRequest(row, false)">
                      拒绝
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="增强能力" name="advanced">
              <div class="advanced-grid">
                <section>
                  <h3>敏感词</h3>
                  <div class="inline-toolbar">
                    <el-input v-model="sensitiveForm.word" placeholder="敏感词" />
                    <el-select v-model="sensitiveForm.action" style="width: 120px">
                      <el-option label="提醒" value="warn" />
                      <el-option label="禁言" value="mute" />
                      <el-option label="删除" value="delete" />
                    </el-select>
                    <el-button type="primary" @click="saveSensitiveWord">保存</el-button>
                  </div>
                  <el-table :data="sensitiveWords" height="230">
                    <el-table-column prop="word" label="词" min-width="120" />
                    <el-table-column prop="action" label="动作" width="90" />
                    <el-table-column label="状态" width="90">
                      <template #default="{ row }">{{ row.enabled ? '启用' : '停用' }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="90">
                      <template #default="{ row }">
                        <el-button link type="danger" @click="deleteSensitiveWord(row)">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </section>
                <section>
                  <h3>自动审核</h3>
                  <div class="rule-form">
                    <el-input v-model="ruleForm.name" placeholder="规则名称" />
                    <el-input v-model="ruleForm.keywordsText" placeholder="关键词，用逗号分隔" />
                    <el-switch v-model="ruleForm.approve" active-text="自动同意" inactive-text="自动拒绝" />
                    <el-button type="primary" @click="saveAutoReviewRule">保存</el-button>
                  </div>
                  <el-table :data="autoRules" height="230">
                    <el-table-column prop="name" label="规则" min-width="140" />
                    <el-table-column label="动作" width="100">
                      <template #default="{ row }">{{ row.approve ? '同意' : '拒绝' }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="90">
                      <template #default="{ row }">
                        <el-button link type="danger" @click="deleteAutoReviewRule(row)">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </section>
              </div>
            </el-tab-pane>

            <el-tab-pane label="统计" name="statistics">
              <div class="inline-toolbar">
                <el-date-picker
                  v-model="statsRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                />
                <el-button :icon="Search" @click="fetchStats">查询统计</el-button>
              </div>
              <div class="stats-grid">
                <el-table :data="stats" height="360">
                  <el-table-column prop="statDate" label="日期" min-width="120" />
                  <el-table-column prop="messageCount" label="消息数" min-width="100" />
                  <el-table-column prop="activeMemberCount" label="活跃人数" min-width="100" />
                  <el-table-column prop="commandCount" label="指令数" min-width="100" />
                </el-table>
                <el-table :data="activeGroups" height="360">
                  <el-table-column prop="groupName" label="活跃群" min-width="160" />
                  <el-table-column prop="messageCount" label="消息数" min-width="100" />
                  <el-table-column prop="peakActiveMemberCount" label="峰值活跃" min-width="100" />
                </el-table>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
        <el-empty v-else description="选择一个 QQ 群查看成员、公告、申请和配置" />
      </section>
    </div>

    <el-drawer v-model="configVisible" title="QQ群机器人配置" size="520px">
      <el-form :model="configForm" label-width="120px">
        <el-form-item label="机器人状态">
          <el-switch v-model="configForm.botEnabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="响应模式">
          <el-select v-model="configForm.mode">
            <el-option v-for="item in modeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-divider content-position="left">新人欢迎</el-divider>
        <el-form-item label="启用欢迎语">
          <el-switch v-model="configForm.welcomeEnabled" />
        </el-form-item>
        <el-form-item label="@新成员">
          <el-switch v-model="configForm.atNewMember" />
        </el-form-item>
        <el-form-item label="延迟秒数">
          <el-input-number v-model="configForm.welcomeDelaySeconds" :min="0" :max="3600" />
        </el-form-item>
        <el-form-item label="欢迎语">
          <el-input v-model="configForm.welcomeText" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="欢迎图片">
          <el-input v-model="configForm.welcomeImageUrl" clearable placeholder="图片 URL，可选" />
        </el-form-item>
        <el-form-item label="欢迎附件">
          <el-input v-model="configForm.welcomeAttachmentUrl" clearable placeholder="附件 URL，可选" />
        </el-form-item>
        <el-divider content-position="left">插件开关</el-divider>
        <el-form-item v-for="item in pluginOptions" :key="item.key" :label="item.label">
          <el-switch v-model="configForm.pluginConfig[item.key]" />
        </el-form-item>
        <el-divider content-position="left">增强能力</el-divider>
        <el-form-item label="自动审核">
          <el-switch v-model="configForm.autoReviewEnabled" />
        </el-form-item>
        <el-form-item label="敏感词过滤">
          <el-switch v-model="configForm.sensitiveFilterEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configVisible = false">取消</el-button>
        <el-button type="primary" :loading="configSaving" @click="saveConfig">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="batchVisible" title="批量配置QQ群" width="460px">
      <el-form :model="batchForm" label-width="110px">
        <el-form-item label="已选择">{{ selectedRows.length }} 个群</el-form-item>
        <el-form-item label="机器人状态">
          <el-switch v-model="batchForm.botEnabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="响应模式">
          <el-select v-model="batchForm.mode">
            <el-option v-for="item in modeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="saveBatchConfig">保存</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshRight, Search, Setting } from '@element-plus/icons-vue'
import { botManagementApi } from '@/api'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { formatDateTime, statusType } from '@/utils/format.ts'

type Row = Record<string, any>

const modeOptions = [
  { label: '启用', value: 'enabled' },
  { label: '禁用', value: 'disabled' },
  { label: '仅管理员可用', value: 'admin_only' },
  { label: '仅指令模式', value: 'command_only' },
  { label: '静默监听', value: 'silent' },
]

const pluginOptions = [
  { label: 'AI聊天', key: 'ai_chat' },
  { label: '关键词回复', key: 'keyword_reply' },
  { label: '入群欢迎', key: 'welcome' },
  { label: '群管功能', key: 'group_admin' },
  { label: '娱乐功能', key: 'fun' },
]

const overview = ref<Row>({})
const groups = ref<Row[]>([])
const members = ref<Row[]>([])
const announcements = ref<Row[]>([])
const joinRequests = ref<Row[]>([])
const sensitiveWords = ref<Row[]>([])
const autoRules = ref<Row[]>([])
const stats = ref<Row[]>([])
const activeGroups = ref<Row[]>([])
const selectedRows = ref<Row[]>([])
const activeGroup = ref<Row | null>(null)
const activeTab = ref('members')
const statsRange = ref<[string, string] | ''>('')

const loading = ref(false)
const membersLoading = ref(false)
const syncingGroups = ref(false)
const syncingMembers = ref('')
const configVisible = ref(false)
const configSaving = ref(false)
const batchVisible = ref(false)
const batchSaving = ref(false)
const announcementSaving = ref(false)

const query = reactive({
  keyword: '',
  botEnabled: '',
  mode: '',
})

const memberQuery = reactive({
  keyword: '',
  role: '',
  muteStatus: '',
})

const announcementForm = reactive({
  title: '',
  content: '',
  attachments: [] as unknown[],
})

const configForm = reactive({
  groupId: '',
  botEnabled: true,
  mode: 'enabled',
  welcomeEnabled: false,
  welcomeText: '欢迎 {nickname} 加入本群！',
  atNewMember: true,
  welcomeImageUrl: '',
  welcomeAttachmentUrl: '',
  welcomeDelaySeconds: 0,
  pluginConfig: {
    ai_chat: true,
    keyword_reply: true,
    welcome: true,
    group_admin: true,
    fun: false,
  } as Record<string, boolean>,
  autoReviewEnabled: false,
  sensitiveFilterEnabled: false,
})

const batchForm = reactive({
  botEnabled: true,
  mode: 'enabled',
})

const sensitiveForm = reactive({
  word: '',
  action: 'warn',
  enabled: true,
})

const ruleForm = reactive({
  name: '',
  keywordsText: '',
  approve: true,
  enabled: true,
})

const selectedGroupId = computed(() => activeGroup.value?.groupId || '')

onMounted(fetchAll)

async function fetchAll() {
  await Promise.all([fetchOverview(), fetchGroups(), fetchSensitiveWords(), fetchAutoReviewRules(), fetchStats()])
}

async function fetchOverview() {
  overview.value = await botManagementApi.overview()
}

async function fetchGroups() {
  loading.value = true
  try {
    const params: Row = {}
    if (query.keyword) params.keyword = query.keyword
    if (query.botEnabled) params.botEnabled = query.botEnabled === 'true'
    if (query.mode) params.mode = query.mode
    groups.value = await botManagementApi.groups(params)
    if (!activeGroup.value && groups.value.length) {
      await selectGroup(groups.value[0])
    }
  } finally {
    loading.value = false
  }
}

async function syncGroups() {
  syncingGroups.value = true
  try {
    await botManagementApi.syncGroups()
    ElMessage.success('群列表已同步')
    await Promise.all([fetchOverview(), fetchGroups()])
  } finally {
    syncingGroups.value = false
  }
}

async function selectGroup(row: Row) {
  activeGroup.value = row
  await Promise.all([
    fetchGroupDetail(),
    fetchMembers(),
    fetchAnnouncements(),
    fetchJoinRequests(),
    fetchAutoReviewRules(),
  ])
}

function handleSelectionChange(rows: Row[]) {
  selectedRows.value = rows
}

async function fetchGroupDetail() {
  if (!selectedGroupId.value) return
  const detail = await botManagementApi.groupDetail(selectedGroupId.value)
  activeGroup.value = { ...(activeGroup.value || {}), ...detail }
}

async function fetchMembers() {
  if (!selectedGroupId.value) return
  membersLoading.value = true
  try {
    const params: Row = {}
    if (memberQuery.keyword) params.keyword = memberQuery.keyword
    if (memberQuery.role) params.role = memberQuery.role
    if (memberQuery.muteStatus) params.muteStatus = memberQuery.muteStatus
    members.value = await botManagementApi.members(selectedGroupId.value, params)
  } finally {
    membersLoading.value = false
  }
}

async function syncMembers(row: Row) {
  syncingMembers.value = row.groupId
  try {
    await botManagementApi.syncMembers(row.groupId)
    ElMessage.success('群成员已同步')
    if (selectedGroupId.value === row.groupId) await fetchMembers()
    await fetchGroups()
  } finally {
    syncingMembers.value = ''
  }
}

async function fetchAnnouncements() {
  if (!selectedGroupId.value) return
  announcements.value = await botManagementApi.announcements(selectedGroupId.value)
}

async function fetchJoinRequests() {
  if (!selectedGroupId.value) return
  joinRequests.value = await botManagementApi.joinRequests(selectedGroupId.value)
}

async function fetchSensitiveWords() {
  sensitiveWords.value = await botManagementApi.sensitiveWords()
}

async function fetchAutoReviewRules() {
  const params = selectedGroupId.value ? { groupId: selectedGroupId.value } : undefined
  autoRules.value = await botManagementApi.autoReviewRules(params)
}

async function fetchStats() {
  const params: Row = {}
  if (statsRange.value) {
    params.startDate = statsRange.value[0]
    params.endDate = statsRange.value[1]
  }
  const [statRows, activeRows] = await Promise.all([
    botManagementApi.statistics(params),
    botManagementApi.activeGroups(params),
  ])
  stats.value = statRows
  activeGroups.value = activeRows
}

async function openConfig(row: Row) {
  const source = row.config ? row : await botManagementApi.groupDetail(row.groupId)
  const config = source.config || {}
  const pluginConfig = parsePluginConfig(config.pluginConfig)
  configForm.groupId = source.groupId
  configForm.botEnabled = Boolean(source.botEnabled)
  configForm.mode = source.mode || 'enabled'
  configForm.welcomeEnabled = Boolean(config.welcomeEnabled)
  configForm.welcomeText = config.welcomeText || '欢迎 {nickname} 加入本群！'
  configForm.atNewMember = config.atNewMember !== false
  configForm.welcomeImageUrl = config.welcomeImageUrl || ''
  configForm.welcomeAttachmentUrl = config.welcomeAttachmentUrl || ''
  configForm.welcomeDelaySeconds = Number(config.welcomeDelaySeconds || 0)
  configForm.pluginConfig = {
    ai_chat: pluginConfig.ai_chat !== false,
    keyword_reply: pluginConfig.keyword_reply !== false,
    welcome: pluginConfig.welcome !== false,
    group_admin: pluginConfig.group_admin !== false,
    fun: Boolean(pluginConfig.fun),
  }
  configForm.autoReviewEnabled = Boolean(config.autoReviewEnabled)
  configForm.sensitiveFilterEnabled = Boolean(config.sensitiveFilterEnabled)
  configVisible.value = true
}

async function saveConfig() {
  configSaving.value = true
  try {
    await botManagementApi.updateConfig(configForm.groupId, { ...configForm })
    ElMessage.success('群配置已保存')
    configVisible.value = false
    await Promise.all([fetchGroups(), fetchGroupDetail()])
  } finally {
    configSaving.value = false
  }
}

function openBatchDialog() {
  batchVisible.value = true
}

async function saveBatchConfig() {
  batchSaving.value = true
  try {
    await botManagementApi.batchConfig({
      groupIds: selectedRows.value.map((item) => item.groupId),
      botEnabled: batchForm.botEnabled,
      mode: batchForm.mode,
    })
    ElMessage.success('批量配置已保存')
    batchVisible.value = false
    await fetchGroups()
  } finally {
    batchSaving.value = false
  }
}

async function muteMember(row: Row, duration: number) {
  if (!selectedGroupId.value) return
  const message = duration > 0 ? `确认禁言 ${row.card || row.nickname || row.userId} ${duration} 秒？` : '确认解除该成员禁言？'
  await ElMessageBox.confirm(message, '禁言操作', { type: 'warning' })
  await botManagementApi.muteMember(selectedGroupId.value, row.userId, { duration })
  ElMessage.success(duration > 0 ? '已禁言' : '已解除禁言')
  await fetchMembers()
}

async function customMute(row: Row) {
  const result = await ElMessageBox.prompt('请输入禁言秒数', '自定义禁言', {
    inputValue: '600',
    inputPattern: /^\d+$/,
    inputErrorMessage: '请输入非负整数秒数',
  })
  await muteMember(row, Number(result.value || 0))
}

async function confirmMuteAll(row: Row, enabled: boolean) {
  await ElMessageBox.confirm(
    enabled ? `确认开启 ${row.groupName || row.groupId} 全体禁言？` : `确认关闭 ${row.groupName || row.groupId} 全体禁言？`,
    '全体禁言',
    { type: 'warning' },
  )
  await botManagementApi.muteAll(row.groupId, { enabled })
  ElMessage.success(enabled ? '已开启全体禁言' : '已关闭全体禁言')
}

async function publishAnnouncement() {
  if (!selectedGroupId.value) return
  announcementSaving.value = true
  try {
    await botManagementApi.publishAnnouncement(selectedGroupId.value, { ...announcementForm })
    ElMessage.success('公告发布请求已提交')
    announcementForm.title = ''
    announcementForm.content = ''
    await fetchAnnouncements()
  } finally {
    announcementSaving.value = false
  }
}

async function republishAnnouncement(row: Row) {
  await botManagementApi.republishAnnouncement(selectedGroupId.value, row.id)
  ElMessage.success('已重新发布')
  await fetchAnnouncements()
}

async function deleteAnnouncement(row: Row) {
  await ElMessageBox.confirm('确认删除这条公告记录？', '删除公告', { type: 'warning' })
  await botManagementApi.deleteAnnouncement(selectedGroupId.value, row.id)
  ElMessage.success('公告已删除')
  await fetchAnnouncements()
}

async function handleJoinRequest(row: Row, approve: boolean) {
  let reason = ''
  if (!approve) {
    const result = await ElMessageBox.prompt('请输入拒绝原因', '拒绝入群申请', {
      inputPattern: /.+/,
      inputErrorMessage: '拒绝原因不能为空',
    })
    reason = result.value
  } else {
    await ElMessageBox.confirm('确认同意该入群申请？', '入群申请', { type: 'warning' })
  }
  await botManagementApi.handleJoinRequest(selectedGroupId.value, row.id, { approve, reason })
  ElMessage.success(approve ? '已同意' : '已拒绝')
  await Promise.all([fetchOverview(), fetchJoinRequests()])
}

async function saveSensitiveWord() {
  await botManagementApi.saveSensitiveWord({ ...sensitiveForm })
  ElMessage.success('敏感词已保存')
  sensitiveForm.word = ''
  await fetchSensitiveWords()
}

async function deleteSensitiveWord(row: Row) {
  await botManagementApi.deleteSensitiveWord(row.id)
  ElMessage.success('敏感词已删除')
  await fetchSensitiveWords()
}

async function saveAutoReviewRule() {
  await botManagementApi.saveAutoReviewRule({
    name: ruleForm.name,
    groupId: selectedGroupId.value || null,
    keywords: ruleForm.keywordsText.split(',').map((item) => item.trim()).filter(Boolean),
    approve: ruleForm.approve,
    enabled: ruleForm.enabled,
  })
  ElMessage.success('自动审核规则已保存')
  ruleForm.name = ''
  ruleForm.keywordsText = ''
  await fetchAutoReviewRules()
}

async function deleteAutoReviewRule(row: Row) {
  await botManagementApi.deleteAutoReviewRule(row.id)
  ElMessage.success('自动审核规则已删除')
  await fetchAutoReviewRules()
}

function parsePluginConfig(value: unknown): Row {
  if (!value) return {}
  if (typeof value === 'object') return value as Row
  try {
    return JSON.parse(String(value))
  } catch {
    return {}
  }
}

function modeText(mode: string): string {
  return modeOptions.find((item) => item.value === mode)?.label || mode || '-'
}

function roleText(role: string): string {
  return (
    {
      owner: '群主',
      admin: '管理员',
      member: '成员',
    }[role] || role || '-'
  )
}

function announcementStatusText(status: string): string {
  return (
    {
      published: '已发布',
      failed: '发布失败',
      draft: '草稿',
    }[status] || status || '-'
  )
}

function requestStatusText(status: string): string {
  return (
    {
      pending: '待处理',
      approved: '已同意',
      rejected: '已拒绝',
    }[status] || status || '-'
  )
}
</script>

<style scoped>
.bot-page {
  min-width: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.summary-item {
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-surface);
}

.summary-item span {
  display: block;
  color: var(--oa-muted);
  font-size: 13px;
}

.summary-item strong {
  display: block;
  margin-top: 8px;
  color: var(--oa-text);
  font-size: 24px;
  line-height: 1;
}

.summary-item strong.online {
  color: var(--el-color-success);
}

.workspace {
  display: grid;
  grid-template-columns: minmax(520px, 0.95fr) minmax(0, 1.25fr);
  gap: 14px;
  align-items: start;
}

.group-panel,
.detail-panel {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-surface);
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.detail-head h2 {
  margin: 0;
  font-size: 18px;
}

.detail-head p,
.muted-line {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 12px;
}

.detail-tabs {
  min-width: 0;
}

.inline-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.announce-form {
  margin-bottom: 10px;
}

.advanced-grid,
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.advanced-grid section {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.advanced-grid h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.rule-form {
  display: grid;
  grid-template-columns: minmax(120px, 0.8fr) minmax(160px, 1fr) auto auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.compact-table {
  width: 100%;
}

@media (max-width: 1180px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workspace,
  .advanced-grid,
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .group-panel,
  .detail-panel {
    padding: 10px;
  }

  .rule-form {
    grid-template-columns: 1fr;
  }
}
</style>
