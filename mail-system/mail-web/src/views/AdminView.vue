<template>
  <div class="mail-shell admin-shell">
    <header class="admin-header">
      <div class="admin-header__left">
        <img alt="开放原子邮箱" class="brand-logo" src="/logo.png" />
        <strong>邮箱后台管理</strong>
        <small>{{ session.address }}</small>
      </div>
      <div class="admin-header__right">
        <ThemeToggle />
        <button class="secondary-button" type="button" @click="emit('back')">返回邮箱</button>
        <button class="icon-button" type="button" aria-label="退出登录" @click="emit('logout')"><LogOut :size="18" /></button>
      </div>
    </header>

    <nav aria-label="后台管理导航" class="admin-tabs">
      <button :class="{ active: tab === 'mailboxes' }" type="button" @click="tab = 'mailboxes'">邮箱管理</button>
      <button :class="{ active: tab === 'broadcast' }" type="button" @click="tab = 'broadcast'">群发邮件</button>
      <button :class="{ active: tab === 'logs' }" type="button" @click="tab = 'logs'">发送记录</button>
    </nav>

    <div v-if="tab === 'mailboxes'" class="admin-body">
      <section class="admin-stats" aria-label="统计概览">
        <div class="stat-card"><span>总邮箱</span><strong>{{ stats?.total ?? '—' }}</strong></div>
        <div class="stat-card"><span>已激活</span><strong>{{ stats?.active ?? '—' }}</strong></div>
        <div class="stat-card"><span>Resend 域名</span><strong>{{ resendDomain }}</strong><small>{{ resendStatus }}</small></div>
      </section>

      <section class="admin-table-wrap" aria-label="用户邮箱列表">
        <header class="admin-table-header">
          <h2>用户邮箱 <small v-if="pageData">共 {{ pageData.total }} 个</small></h2>
          <div class="admin-toolbar">
            <label class="admin-search">
              <Search :size="15" aria-hidden="true" />
              <input v-model="keyword" placeholder="搜索姓名/地址/ID" @input="onSearchInput" />
            </label>
            <button class="secondary-button" type="button" :disabled="loading" @click="load">刷新</button>
          </div>
        </header>
        <div v-if="loading" class="email-skeletons">
          <div v-for="i in 5" :key="i" class="email-skeleton"><i></i><span></span><b></b></div>
        </div>
        <div v-else-if="error" class="empty-state" role="alert">
          <CircleAlert :size="30" /><h2>无法加载</h2><p>{{ error }}</p>
          <button class="secondary-button" type="button" @click="load">重新加载</button>
        </div>
        <table v-else class="admin-table">
          <thead>
            <tr>
              <th><button class="sort-btn" type="button" @click="setSort('id')">ID {{ sortIcon('id') }}</button></th>
              <th><button class="sort-btn" type="button" @click="setSort('displayName')">用户 {{ sortIcon('displayName') }}</button></th>
              <th><button class="sort-btn" type="button" @click="setSort('address')">地址 {{ sortIcon('address') }}</button></th>
              <th><button class="sort-btn" type="button" @click="setSort('status')">状态 {{ sortIcon('status') }}</button></th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="mailbox in rows" :key="mailbox.id">
              <td>{{ mailbox.id }}</td>
              <td>{{ mailbox.displayName || mailbox.sub }}</td>
              <td class="admin-address">{{ mailbox.address || '（未分配）' }}</td>
              <td><span :class="'status-badge status-badge--' + mailbox.status.toLowerCase()">{{ mailbox.status }}</span></td>
              <td>
                <button class="secondary-button" type="button" :disabled="busyId === mailbox.id" @click="toggleSuspend(mailbox)">
                  {{ mailbox.status === 'SUSPENDED' ? '启用' : '停用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <footer v-if="pageData && pageData.total > 0" class="admin-pagination">
          <span>第 {{ page }} / {{ totalPages }} 页 · {{ pageData.total }} 条</span>
          <div class="admin-pagination__btns">
            <button class="secondary-button" type="button" :disabled="page <= 1 || loading" @click="goPage(page - 1)">上一页</button>
            <button class="secondary-button" type="button" :disabled="page >= totalPages || loading" @click="goPage(page + 1)">下一页</button>
          </div>
        </footer>
      </section>
    </div>

    <div v-else class="admin-body">
      <section class="admin-table-wrap broadcast-card" aria-label="群发邮件">
        <header class="admin-table-header">
          <h2>群发邮件 <small>选择站内系统邮箱或主站外部邮箱收件人</small></h2>
          <div class="admin-toolbar">
            <label class="admin-search">
              <Search :size="15" aria-hidden="true" />
              <input v-model="recipientKeyword" placeholder="搜索姓名/邮箱" @input="onRecipientSearch" />
            </label>
            <button class="secondary-button" type="button" :disabled="recipientsLoading" @click="loadRecipients">刷新</button>
          </div>
        </header>
        <div v-if="recipientsLoading" class="email-skeletons">
          <div v-for="i in 4" :key="i" class="email-skeleton"><i></i><span></span><b></b></div>
        </div>
        <div v-else-if="recipientsError" class="empty-state" role="alert">
          <CircleAlert :size="30" /><h2>无法加载收件人</h2><p>{{ recipientsError }}</p>
          <button class="secondary-button" type="button" @click="loadRecipients">重新加载</button>
        </div>
        <div v-else class="recipient-panel">
          <div class="recipient-source-switch" role="group" aria-label="收件人来源">
            <button :class="{ active: recipientSource === 'internal' }" type="button" @click="switchRecipientSource('internal')">站内系统邮箱</button>
            <button :class="{ active: recipientSource === 'external' }" type="button" @click="switchRecipientSource('external')">主站外部邮箱</button>
          </div>
          <div class="recipient-select-all">
            <label>
              <input :checked="allSelected" type="checkbox" @change="toggleAll" />
              全选本页
            </label>
            <span>共 {{ recipientPage?.total ?? 0 }} 位收件人，已选 {{ selectedRecipients.size }} 位</span>
          </div>
          <ul v-if="recipients.length" class="recipient-list">
            <li v-for="recipient in recipients" :key="recipient.email">
              <label>
                <input :checked="selectedRecipients.has(recipient.email)" type="checkbox" @change="toggleRecipient(recipient)" />
                <span class="recipient-name">{{ recipient.name || '未命名用户' }}</span>
                <small class="admin-address">{{ recipient.email }}</small>
              </label>
            </li>
          </ul>
          <div v-else class="empty-state">
            <MailOpen :size="30" /><h2>没有匹配的收件人</h2>
            <p v-if="recipientSource === 'internal'">还没有已激活的站内邮箱账号。</p>
            <p v-else>主站中还没有登记非 @jmi-openatom.cn 的邮箱。</p>
          </div>
        </div>
        <div class="broadcast-form">
          <div class="broadcast-template-row">
            <label for="broadcast-template">邮件模板</label>
            <select id="broadcast-template" v-model="selectedTemplateKey">
              <option value="default">简约通知 — 日常通知</option>
              <option value="activity">活动邀请 — 活动报名</option>
              <option value="recruitment">招新宣传 — 纳新季</option>
              <option value="announcement">重要公告 — 紧急通知</option>
              <option value="">不套用模板</option>
            </select>
            <small>正文将嵌入所选模板（含社团 logo），与主站风格一致</small>
          </div>
          <input v-model="broadcastSubject" class="broadcast-subject" maxlength="200" placeholder="邮件主题" type="text" />
          <RichTextEditor v-model="broadcastHtml" placeholder="邮件正文…" />
          <p v-if="broadcastError" class="form-error" role="alert"><CircleAlert :size="16" /> {{ broadcastError }}</p>
          <footer class="broadcast-footer">
            <span>将发送给 <strong>{{ selectedRecipients.size }}</strong> 位收件人</span>
            <button class="primary-button" :disabled="sending || selectedRecipients.size === 0" type="button" @click="onSendBroadcast">
              <span v-if="sending" class="spinner small"></span><Send v-else :size="16" />
              {{ sending ? '正在发送…' : '群发邮件' }}
            </button>
          </footer>
        </div>
      </section>
    </div>

    <div v-else-if="tab === 'logs'" class="admin-body">
      <section class="admin-table-wrap" aria-label="群发邮件发送记录">
        <header class="admin-table-header">
          <h2>发送记录 <small v-if="logPage">共 {{ logPage.total }} 条</small></h2>
          <div class="admin-toolbar">
            <button class="secondary-button" type="button" :disabled="logsLoading" @click="loadLogs">刷新</button>
          </div>
        </header>
        <div v-if="logsLoading" class="email-skeletons">
          <div v-for="i in 5" :key="i" class="email-skeleton"><i></i><span></span><b></b></div>
        </div>
        <div v-else-if="logsError" class="empty-state" role="alert">
          <CircleAlert :size="30" /><h2>无法加载记录</h2><p>{{ logsError }}</p>
          <button class="secondary-button" type="button" @click="loadLogs">重新加载</button>
        </div>
        <table v-else class="admin-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>来源</th>
              <th>类型</th>
              <th>主题</th>
              <th>发件人</th>
              <th>收件人</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logRows" :key="log.id">
              <td>{{ formatLogTime(log.createdAt) }}</td>
              <td><span class="status-badge" :class="log.source === 'auto' ? 'status-badge--auto' : 'status-badge--manual'">{{ log.source === 'auto' ? '自动' : '手动' }}</span></td>
              <td>{{ kindName(log.kind) }}</td>
              <td class="log-subject" :title="log.subject">{{ log.subject || '（无主题）' }}</td>
              <td class="admin-address">{{ log.sender }}</td>
              <td>{{ log.recipients }} 位 / {{ log.batches }} 批</td>
              <td>
                <span :class="'status-badge status-badge--' + (log.status === 'sent' ? 'active' : 'suspended')">{{ log.status === 'sent' ? '成功' : '失败' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
        <footer v-if="logPage && logPage.total > 0" class="admin-pagination">
          <span>第 {{ logPageNum }} / {{ logTotalPages }} 页 · {{ logPage.total }} 条</span>
          <div class="admin-pagination__btns">
            <button class="secondary-button" type="button" :disabled="logPageNum <= 1 || logsLoading" @click="goLogPage(logPageNum - 1)">上一页</button>
            <button class="secondary-button" type="button" :disabled="logPageNum >= logTotalPages || logsLoading" @click="goLogPage(logPageNum + 1)">下一页</button>
          </div>
        </footer>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { CircleAlert, LogOut, MailOpen, Search, Send } from 'lucide-vue-next'
import RichTextEditor from '../components/common/RichTextEditor.vue'
import { broadcastTemplates } from '../broadcast/templates'
import {
  loadAdminMailboxes, loadAdminStats, loadBroadcastLogs, loadExternalRecipients, loadInternalRecipients,
  sendBroadcast, setMailboxSuspended,
  type AdminMailboxPage, type AdminMailboxView, type AdminStats, type BroadcastLogEntry,
  type BroadcastLogPage, type ExternalRecipient,
} from '../api'
import { htmlToPlainText } from '../mail'
import type { SessionView } from '../models'
import ThemeToggle from '../components/common/ThemeToggle.vue'
import { useUiStore } from '../stores/ui'

defineProps<{ session: SessionView }>()
const emit = defineEmits<{ (e: 'logout'): void; (e: 'back'): void }>()

const { showToast } = useUiStore()

const tab = ref<'mailboxes' | 'broadcast' | 'logs'>('mailboxes')

// ===== Mailbox management =====
const rows = ref<AdminMailboxView[]>([])
const pageData = ref<AdminMailboxPage | null>(null)
const stats = ref<AdminStats | null>(null)
const loading = ref(false)
const busyId = ref<number | null>(null)
const error = ref('')
const page = ref(1)
const pageSize = 20
const keyword = ref('')
const sort = ref('id')
const order = ref<'asc' | 'desc'>('desc')
let searchTimer: number | undefined

const totalPages = computed(() => {
  const total = pageData.value?.total ?? 0
  return Math.max(1, Math.ceil(total / pageSize))
})

const resendDomain = computed(() => {
  const r = stats.value?.resend
  return r?.configured && r.domain ? r.domain : '未配置'
})
const resendStatus = computed(() => {
  const r = stats.value?.resend
  if (!r?.configured) return ''
  if (r.error) return r.error
  return r.verified ? '已验证' : '未验证'
})

onMounted(() => {
  void load()
  void loadRecipients()
  void loadLogs()
})
onBeforeUnmount(() => {
  if (searchTimer) window.clearTimeout(searchTimer)
  if (recipientTimer) window.clearTimeout(recipientTimer)
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [boxes, stat] = await Promise.all([
      loadAdminMailboxes({ page: page.value, pageSize, keyword: keyword.value, sort: sort.value, order: order.value }),
      loadAdminStats(),
    ])
    rows.value = boxes.rows
    pageData.value = boxes
    stats.value = stat
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function onSearchInput() {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    page.value = 1
    void load()
  }, 350)
}

function setSort(field: string) {
  if (sort.value === field) {
    order.value = order.value === 'asc' ? 'desc' : 'asc'
  } else {
    sort.value = field
    order.value = 'asc'
  }
  void load()
}

function sortIcon(field: string): string {
  if (sort.value !== field) return '↕'
  return order.value === 'asc' ? '↑' : '↓'
}

function goPage(target: number) {
  if (target < 1 || target > totalPages.value) return
  page.value = target
  void load()
}

async function toggleSuspend(mailbox: AdminMailboxView) {
  busyId.value = mailbox.id
  try {
    const target = mailbox.status !== 'SUSPENDED'
    await setMailboxSuspended(mailbox.id, target)
    showToast(target ? '已停用' : '已启用')
    await load()
  } catch (err) {
    showToast(err instanceof Error ? err.message : '操作失败')
  } finally {
    busyId.value = null
  }
}

// ===== Broadcast logs =====
const logRows = ref<BroadcastLogEntry[]>([])
const logPage = ref<BroadcastLogPage | null>(null)
const logsLoading = ref(false)
const logsError = ref('')
const logPageNum = ref(1)
const logPageSize = 20

const logTotalPages = computed(() => {
  const total = logPage.value?.total ?? 0
  return Math.max(1, Math.ceil(total / logPageSize))
})

async function loadLogs() {
  logsLoading.value = true
  logsError.value = ''
  try {
    const result = await loadBroadcastLogs({ page: logPageNum.value, pageSize: logPageSize })
    logRows.value = result.rows
    logPage.value = result
  } catch (err) {
    logsError.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    logsLoading.value = false
  }
}

function goLogPage(target: number) {
  if (target < 1 || target > logTotalPages.value) return
  logPageNum.value = target
  void loadLogs()
}

function formatLogTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'medium' }).format(date)
}

function kindName(kind: string): string {
  const map: Record<string, string> = {
    notification: '通知',
    activity: '活动',
    recruitment: '招新',
    approval: '审核',
    interview: '面试',
    manual: '手动',
    auto: '自动',
  }
  return map[kind] || kind || '—'
}

// ===== Broadcast =====
const recipientSource = ref<'internal' | 'external'>('internal')
const recipients = ref<ExternalRecipient[]>([])
const recipientPage = ref<{ total: number; page: number; pageSize: number } | null>(null)
const recipientKeyword = ref('')
const recipientsLoading = ref(false)
const recipientsError = ref('')
const selectedRecipients = ref(new Set<string>())
const broadcastSubject = ref('')
const broadcastHtml = ref('')
const broadcastError = ref('')
const sending = ref(false)
const selectedTemplateKey = ref('default')
let recipientTimer: number | undefined

function wrappedHtml(html: string, subject: string): string {
  const template = broadcastTemplates.find((item) => item.key === selectedTemplateKey.value)
  if (!template) return html
  return template.wrap(html, subject, window.location.origin + '/logo.png')
}

const allSelected = computed(() => {
  return recipients.value.length > 0 && recipients.value.every((r) => selectedRecipients.value.has(r.email))
})

async function loadRecipients() {
  recipientsLoading.value = true
  recipientsError.value = ''
  try {
    const options = { page: 1, pageSize: 200, keyword: recipientKeyword.value }
    const result = recipientSource.value === 'internal'
      ? await loadInternalRecipients(options)
      : await loadExternalRecipients(options)
    recipients.value = result.rows
    recipientPage.value = { total: result.total, page: result.page, pageSize: result.pageSize }
  } catch (err) {
    recipientsError.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    recipientsLoading.value = false
  }
}

function switchRecipientSource(source: 'internal' | 'external') {
  if (recipientSource.value === source) return
  recipientSource.value = source
  selectedRecipients.value = new Set()
  void loadRecipients()
}

function onRecipientSearch() {
  if (recipientTimer) window.clearTimeout(recipientTimer)
  recipientTimer = window.setTimeout(() => {
    void loadRecipients()
  }, 350)
}

function toggleRecipient(recipient: ExternalRecipient) {
  const selected = selectedRecipients.value
  if (selected.has(recipient.email)) selected.delete(recipient.email)
  else selected.add(recipient.email)
  selectedRecipients.value = new Set(selected)
}

function toggleAll() {
  const selected = selectedRecipients.value
  if (allSelected.value) {
    for (const recipient of recipients.value) selected.delete(recipient.email)
  } else {
    for (const recipient of recipients.value) selected.add(recipient.email)
  }
  selectedRecipients.value = new Set(selected)
}

async function onSendBroadcast() {
  const emails = Array.from(selectedRecipients.value)
  const subject = broadcastSubject.value.trim()
  const text = htmlToPlainText(broadcastHtml.value)
  if (!emails.length) {
    broadcastError.value = '请至少选择一位收件人。'
    return
  }
  if (!subject || !text) {
    broadcastError.value = '请填写主题和正文。'
    return
  }
  sending.value = true
  broadcastError.value = ''
  try {
    const result = await sendBroadcast({
      recipients: emails,
      subject: subject || '（无主题）',
      htmlBody: wrappedHtml(broadcastHtml.value, subject || '（无主题）'),
      textBody: text,
    })
    showToast(`群发成功，已发送给 ${result.recipients} 位收件人`)
    selectedRecipients.value = new Set()
    broadcastSubject.value = ''
    broadcastHtml.value = ''
  } catch (err) {
    broadcastError.value = err instanceof Error ? err.message : '群发失败，请稍后重试。'
  } finally {
    sending.value = false
  }
}
</script>