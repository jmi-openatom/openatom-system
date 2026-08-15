<template>
  <a class="skip-link" href="#mail-main">跳到主要内容</a>

  <div v-if="loadingSession" class="boot-screen" aria-live="polite">
    <span class="spinner" aria-hidden="true"></span>
    <p>正在连接开放原子邮箱…</p>
  </div>

  <main v-else-if="!session.authenticated" id="mail-main" class="login-page" tabindex="-1">
    <nav class="public-nav" aria-label="邮箱站导航">
      <a class="brand" href="https://www.jmi-openatom.cn">
        <img class="brand-logo" src="/logo.png" alt="开放原子开源社团" />
        <span><strong>开放原子开源社团</strong><small>江苏海事职业技术学院</small></span>
      </a>
      <button class="icon-button" type="button" aria-label="切换深色模式" @click="toggleTheme">
        <Moon v-if="theme === 'light'" :size="18" />
        <Sun v-else :size="18" />
      </button>
    </nav>
    <section class="login-hero">
      <div class="login-copy">
        <span class="eyebrow">OPENATOM MAIL</span>
        <h1>正在前往登录…</h1>
        <p>即将跳转到开放原子统一登录页面，请稍候。</p>
        <a class="primary-button login-button" href="/api/oauth/login" ref="loginLink">
          立即前往登录 <ArrowRight :size="18" />
        </a>
        <p class="login-redirect-hint">若未自动跳转，请点击上方按钮。</p>
      </div>
    </section>
    <footer class="public-footer">© 2025–2027 JMI-OPENATOM · 数据由本地邮件服务托管</footer>
  </main>

  <div v-else class="mail-shell">
    <header class="app-header">
      <div class="app-brand">
        <img class="brand-logo" src="/logo.png" alt="开放原子邮箱" />
        <div><strong>开放原子邮箱</strong><small>{{ session.address }}</small></div>
      </div>
      <label class="search-field">
        <Search :size="17" aria-hidden="true" />
        <span class="sr-only">搜索邮件</span>
        <input v-model="search" type="search" placeholder="搜索主题、发件人或正文" @input="scheduleSearch" />
        <kbd>⌘ K</kbd>
      </label>
      <div class="header-actions">
        <button class="icon-button" type="button" aria-label="刷新邮件" :disabled="mailLoading" @click="refresh">
          <RefreshCw :class="{ rotating: mailLoading }" :size="18" />
        </button>
        <button class="icon-button" type="button" aria-label="切换深色模式" @click="toggleTheme">
          <Moon v-if="theme === 'light'" :size="18" />
          <Sun v-else :size="18" />
        </button>
        <button class="account-button" type="button" :aria-expanded="accountMenu" @click="accountMenu = !accountMenu">
          <span class="avatar">{{ avatarText }}</span>
          <span class="account-copy"><strong>{{ session.displayName || '开放原子成员' }}</strong><small>{{ session.status }}</small></span>
          <ChevronDown :size="16" />
        </button>
        <div v-if="accountMenu" class="account-menu">
          <a href="https://www.jmi-openatom.cn/workspace"><LayoutGrid :size="16" /> 返回主站工作台</a>
          <button type="button" @click="handleLogout"><LogOut :size="16" /> 退出登录</button>
        </div>
      </div>
    </header>

    <div class="mail-workspace">
      <aside class="folder-sidebar" aria-label="邮箱文件夹">
        <button class="compose-button" type="button" @click="openCompose">
          <SquarePen :size="18" /> 写邮件
        </button>
        <nav class="folder-nav">
          <button
            v-for="folder in visibleFolders"
            :key="folder.id"
            :class="{ active: selectedMailboxId === folder.id }"
            type="button"
            @click="selectFolder(folder.id)"
          >
            <component :is="folderIcon(folder.role)" :size="18" />
            <span>{{ folderName(folder) }}</span>
            <b v-if="folder.unreadEmails">{{ compactNumber(folder.unreadEmails) }}</b>
          </button>
        </nav>
        <div class="storage-card">
          <div><HardDrive :size="16" /><span>邮箱空间</span><small>2 GB</small></div>
          <span class="storage-track"><i style="width: 6%"></i></span>
          <p>邮件正文与附件保存在自建服务器</p>
        </div>
      </aside>

      <section :class="{ 'mobile-hidden': selectedEmail }" class="message-list-panel" aria-label="邮件列表">
        <header class="panel-heading">
          <div><p>{{ activeFolderName }}</p><span>{{ emails.length }} 封邮件</span></div>
          <button class="icon-button" type="button" aria-label="更多筛选"><SlidersHorizontal :size="17" /></button>
        </header>
        <div v-if="mailLoading" class="email-skeletons" aria-live="polite" aria-label="正在加载邮件">
          <div v-for="index in 6" :key="index" class="email-skeleton"><i></i><span></span><b></b></div>
        </div>
        <div v-else-if="errorMessage" class="empty-state" role="alert">
          <CircleAlert :size="30" /><h2>暂时无法读取邮件</h2><p>{{ errorMessage }}</p>
          <button class="secondary-button" type="button" @click="refresh">重新加载</button>
        </div>
        <div v-else-if="!emails.length" class="empty-state">
          <MailOpen :size="32" /><h2>这里还没有邮件</h2><p>新邮件到达后会显示在这里。</p>
          <button class="secondary-button" type="button" @click="openCompose">写第一封邮件</button>
        </div>
        <ol v-else class="email-list">
          <li v-for="email in emails" :key="email.id" v-memo="[email.id, email.keywords.$seen, selectedEmail?.id]">
            <button
              :class="{ selected: selectedEmail?.id === email.id, unread: !email.keywords.$seen }"
              type="button"
              @click="selectEmail(email.id)"
            >
              <span class="sender-avatar">{{ senderInitial(email) }}</span>
              <span class="email-copy">
                <span class="email-line"><strong>{{ senderName(email) }}</strong><time>{{ formatListDate(email.receivedAt) }}</time></span>
                <span class="email-subject">{{ email.subject || '（无主题）' }}</span>
                <span class="email-preview">{{ email.preview }}</span>
              </span>
              <i v-if="!email.keywords.$seen" class="unread-dot" aria-label="未读"></i>
            </button>
          </li>
        </ol>
      </section>

      <main id="mail-main" :class="{ 'mobile-visible': selectedEmail }" class="reader-panel" tabindex="-1">
        <div v-if="detailLoading" class="reader-loading"><span class="spinner"></span><p>正在打开邮件…</p></div>
        <article v-else-if="selectedEmail" class="message-reader">
          <header class="reader-toolbar">
            <button class="back-button" type="button" aria-label="返回邮件列表" @click="selectedEmail = null"><ArrowLeft :size="19" /></button>
            <div class="toolbar-group">
              <button class="icon-button" type="button" aria-label="回复" @click="replyToSelected"><Reply :size="18" /></button>
              <button class="icon-button" type="button" aria-label="归档"><Archive :size="18" /></button>
              <button class="icon-button danger" type="button" aria-label="移到废纸篓"><Trash2 :size="18" /></button>
              <button class="icon-button" type="button" aria-label="更多操作"><MoreHorizontal :size="19" /></button>
            </div>
          </header>
          <div class="reader-content">
            <p class="message-kicker">{{ activeFolderName }}</p>
            <h1>{{ selectedEmail.subject || '（无主题）' }}</h1>
            <div class="message-meta">
              <span class="sender-avatar large">{{ senderInitial(selectedEmail) }}</span>
              <div><strong>{{ senderName(selectedEmail) }}</strong><small>{{ senderAddress(selectedEmail) }} → {{ recipientText(selectedEmail) }}</small></div>
              <time>{{ formatFullDate(selectedEmail.receivedAt) }}</time>
            </div>
            <div class="privacy-notice"><ShieldCheck :size="16" /><span>为保护隐私，HTML 与远程图片默认不加载；当前以安全纯文本显示。</span></div>
            <div class="message-body">{{ selectedBody }}</div>
            <section v-if="selectedEmail.attachments?.length" class="reader-attachments" aria-labelledby="reader-attachments-title">
              <h2 id="reader-attachments-title">附件（仅下载，不在线预览）</h2>
              <button
                v-for="attachment in selectedEmail.attachments"
                :key="attachment.blobId"
                type="button"
                :disabled="downloadingAttachmentId === attachment.blobId"
                @click="downloadSelectedAttachment(attachment)"
              >
                <span v-if="downloadingAttachmentId === attachment.blobId" class="spinner small" aria-hidden="true"></span>
                <Paperclip v-else :size="17" />
                <span><strong>{{ attachment.name || '未命名附件' }}</strong><small>{{ formatBytes(attachment.size) }}</small></span>
              </button>
              <div v-if="attachmentDownloadError" class="attachment-error" role="alert">
                <CircleAlert :size="16" /> {{ attachmentDownloadError }}
              </div>
            </section>
            <button class="reply-button" type="button" @click="replyToSelected"><Reply :size="17" /> 回复</button>
          </div>
        </article>
        <div v-else class="reader-empty">
          <div class="reader-empty-icon"><Mail :size="34" /></div>
          <h2>选择一封邮件开始阅读</h2>
          <p>邮件内容将以安全模式显示，远程图片默认关闭。</p>
          <span><Command :size="15" /> 使用 ↑ ↓ 浏览，Enter 打开</span>
        </div>
      </main>
    </div>

    <nav class="mobile-bottom-nav" aria-label="移动端邮箱导航">
      <button class="active" type="button" @click="selectedEmail = null"><Inbox :size="20" /><span>邮件</span></button>
      <button type="button" @click="focusSearch"><Search :size="20" /><span>搜索</span></button>
      <button type="button" @click="openCompose"><SquarePen :size="20" /><span>写信</span></button>
    </nav>
  </div>

  <div v-if="composeOpen" class="modal-backdrop" role="presentation" @mousedown.self="requestCloseCompose">
    <section ref="composeDialog" class="compose-dialog" role="dialog" aria-modal="true" aria-labelledby="compose-title">
      <header><div><span class="status-dot"></span><h2 id="compose-title">新邮件</h2></div><button class="icon-button" type="button" aria-label="关闭写信窗口" @click="requestCloseCompose"><X :size="19" /></button></header>
      <form @submit.prevent="submitCompose">
        <label><span>收件人</span><input v-model="compose.to" type="text" inputmode="email" autocomplete="off" required placeholder="name@example.com，多个地址用逗号分隔" /></label>
        <label><span>主题</span><input v-model="compose.subject" type="text" maxlength="200" placeholder="邮件主题" /></label>
        <label class="body-field"><span class="sr-only">邮件正文</span><textarea v-model="compose.body" required placeholder="写点什么…"></textarea></label>
        <div v-if="composeAttachments.length" class="compose-attachment-list" aria-label="待发送附件">
          <div v-for="attachment in composeAttachments" :key="attachment.blobId">
            <Paperclip :size="16" />
            <span><strong>{{ attachment.name }}</strong><small>{{ formatBytes(attachment.size) }}</small></span>
            <button type="button" :aria-label="`移除附件 ${attachment.name}`" @click="removeAttachment(attachment)"><X :size="16" /></button>
          </div>
        </div>
        <div v-if="composeError" class="form-error" role="alert"><CircleAlert :size="16" /> {{ composeError }}</div>
        <footer>
          <div class="attachment-actions">
            <input ref="attachmentInput" class="sr-only" type="file" multiple @change="handleAttachmentSelection" />
            <button class="attachment-button" type="button" :disabled="sending || uploadingAttachment" @click="attachmentInput?.click()">
              <span v-if="uploadingAttachment" class="spinner small"></span><Paperclip v-else :size="16" />
              {{ uploadingAttachment ? '正在上传' : '添加附件' }}
            </button>
            <small>最多 10 个，总计 20 MiB</small>
          </div>
          <button class="primary-button" type="submit" :disabled="sending || uploadingAttachment">
            <span v-if="sending" class="spinner small"></span><Send v-else :size="17" />
            {{ sending ? '正在发送' : '发送邮件' }}
          </button>
        </footer>
      </form>
    </section>
  </div>

  <div class="toast-region" aria-live="polite"><div v-if="toast" class="toast"><CircleCheck :size="17" />{{ toast }}</div></div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  Archive, ArrowLeft, ArrowRight, ChevronDown, CircleAlert, CircleCheck, Command,
  FilePenLine, HardDrive, Inbox, LayoutGrid, LogOut, Mail, MailOpen,
  MoreHorizontal, Moon, Paperclip, RefreshCw, Reply, Search, Send, ShieldCheck,
  SlidersHorizontal, SquarePen, Sun, Trash2, X,
} from 'lucide-vue-next'
import {
  downloadAttachment, forgetUploadedAttachment, loadSession, logout, redirectToOAuth, uploadAttachment,
  type SessionView, type UploadedAttachment,
} from './api'
import {
  bootstrapMail, getEmail, queryEmails, sendEmail, type EmailSummary, type Mailbox, type MailContext,
} from './mail'

const session = ref<SessionView>({ authenticated: false, displayName: null, address: null, status: null, csrfToken: null })
const loadingSession = ref(true)
const mailLoading = ref(false)
const detailLoading = ref(false)
const errorMessage = ref('')
const mailContext = ref<MailContext | null>(null)
const selectedMailboxId = ref<string | null>(null)
const emails = ref<EmailSummary[]>([])
const selectedEmail = ref<EmailSummary | null>(null)
const search = ref('')
const accountMenu = ref(false)
const composeOpen = ref(false)
const composeDialog = ref<HTMLElement | null>(null)
const attachmentInput = ref<HTMLInputElement | null>(null)
const sending = ref(false)
const uploadingAttachment = ref(false)
const downloadingAttachmentId = ref('')
const attachmentDownloadError = ref('')
const composeError = ref('')
const toast = ref('')
const compose = reactive({ to: '', subject: '', body: '' })
const composeAttachments = ref<UploadedAttachment[]>([])
const theme = ref<'light' | 'dark'>('light')
let searchTimer: number | undefined
let toastTimer: number | undefined

const visibleFolders = computed(() =>
  (mailContext.value?.mailboxes ?? []).filter((item) => item.role !== 'all' && item.role !== 'important'),
)
const activeFolderName = computed(() => {
  const folder = visibleFolders.value.find((item) => item.id === selectedMailboxId.value)
  return folder ? folderName(folder) : '全部邮件'
})
const selectedBody = computed(() => {
  const values = Object.values(selectedEmail.value?.bodyValues ?? {})
  return values.map((item) => item.value).join('\n\n') || selectedEmail.value?.preview || '这封邮件没有可显示的纯文本内容。'
})
const avatarText = computed(() => (session.value.displayName || 'OA').trim().slice(-2))

onMounted(async () => {
  theme.value = (localStorage.getItem('openatom-theme') as 'light' | 'dark') || 'light'
  applyTheme()
  window.addEventListener('keydown', handleGlobalKeydown)
  try {
    session.value = await loadSession()
    if (session.value.authenticated && session.value.status === 'ACTIVE') {
      await loadMailbox()
    } else if (!session.value.authenticated) {
      // 未登录：直接跳转 OAuth 统一登录
      redirectToOAuth()
    }
  } catch (error) {
    errorMessage.value = messageOf(error)
  } finally {
    loadingSession.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
  if (searchTimer) window.clearTimeout(searchTimer)
  if (toastTimer) window.clearTimeout(toastTimer)
})

async function loadMailbox() {
  mailLoading.value = true
  errorMessage.value = ''
  try {
    mailContext.value = await bootstrapMail(session.value.address)
    selectedMailboxId.value =
      mailContext.value.mailboxes.find((item) => item.role === 'inbox')?.id ??
      mailContext.value.mailboxes[0]?.id ?? null
    await loadEmails()
  } catch (error) {
    errorMessage.value = messageOf(error)
  } finally {
    mailLoading.value = false
  }
}

async function loadEmails() {
  if (!mailContext.value) return
  mailLoading.value = true
  errorMessage.value = ''
  try {
    emails.value = await queryEmails(mailContext.value.accountId, selectedMailboxId.value, search.value)
  } catch (error) {
    errorMessage.value = messageOf(error)
  } finally {
    mailLoading.value = false
  }
}

async function refresh() {
  await loadEmails()
}

async function selectFolder(id: string) {
  selectedMailboxId.value = id
  selectedEmail.value = null
  await loadEmails()
}

async function selectEmail(id: string) {
  if (!mailContext.value) return
  detailLoading.value = true
  try {
    selectedEmail.value = await getEmail(mailContext.value.accountId, id)
  } catch (error) {
    errorMessage.value = messageOf(error)
  } finally {
    detailLoading.value = false
  }
}

function scheduleSearch() {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(loadEmails, 350)
}

function focusSearch() {
  document.querySelector<HTMLInputElement>('.search-field input')?.focus()
}

function openCompose() {
  composeOpen.value = true
  accountMenu.value = false
  nextTick(() => composeDialog.value?.querySelector<HTMLInputElement>('input')?.focus())
}

function requestCloseCompose() {
  if (compose.to || compose.subject || compose.body || composeAttachments.value.length) {
    if (!window.confirm('这封邮件尚未发送，确定放弃草稿吗？')) return
  }
  closeCompose()
}

function closeCompose(discardAttachments = true) {
  const discarded = composeAttachments.value
  composeOpen.value = false
  composeError.value = ''
  composeAttachments.value = []
  Object.assign(compose, { to: '', subject: '', body: '' })
  if (discardAttachments) {
    for (const attachment of discarded) void forgetUploadedAttachment(attachment.blobId)
  }
}

function replyToSelected() {
  if (!selectedEmail.value) return
  compose.to = senderAddress(selectedEmail.value)
  compose.subject = selectedEmail.value.subject.startsWith('Re:') ? selectedEmail.value.subject : `Re: ${selectedEmail.value.subject}`
  openCompose()
}

async function submitCompose() {
  if (!mailContext.value || !session.value.address || !mailContext.value.identityId) {
    composeError.value = '邮箱身份尚未就绪，请刷新后重试。'
    return
  }
  const recipients = compose.to.split(/[，,;；]/).map((item) => item.trim()).filter(Boolean)
  if (!recipients.length || recipients.some((item) => !/^\S+@\S+\.\S+$/.test(item))) {
    composeError.value = '请填写有效的收件人地址；多个地址使用逗号分隔。'
    return
  }
  const drafts = mailContext.value.mailboxes.find((item) => item.role === 'drafts')
  if (!drafts) {
    composeError.value = '草稿箱尚未创建，请联系管理员。'
    return
  }
  sending.value = true
  composeError.value = ''
  try {
    await sendEmail({
      accountId: mailContext.value.accountId,
      // Resend relay requires sending as the verified relay domain; use the
      // relay Identity so the envelope sender matches its email address.
      identityId: mailContext.value.relayIdentity?.id ?? mailContext.value.identityId,
      draftsMailboxId: drafts.id,
      fromName: session.value.displayName || '开放原子成员',
      fromAddress: mailContext.value.relayIdentity?.email ?? session.value.address,
      to: recipients,
      subject: compose.subject.trim() || '（无主题）',
      body: compose.body,
      attachments: composeAttachments.value,
    })
    closeCompose(false)
    showToast('邮件已提交发送')
  } catch (error) {
    composeError.value = messageOf(error)
  } finally {
    sending.value = false
  }
}

async function handleAttachmentSelection(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  if (!files.length) return
  uploadingAttachment.value = true
  composeError.value = ''
  try {
    for (const file of files) {
      if (composeAttachments.value.length >= 10) throw new Error('每封邮件最多添加 10 个附件。')
      if (file.size > 20 * 1024 * 1024) throw new Error(`“${file.name}”超过 20 MiB。`)
      const total = composeAttachments.value.reduce((sum, item) => sum + item.size, 0)
      if (total + file.size > 20 * 1024 * 1024) throw new Error('附件总大小不能超过 20 MiB。')
      composeAttachments.value.push(await uploadAttachment(file))
    }
  } catch (error) {
    composeError.value = messageOf(error)
  } finally {
    uploadingAttachment.value = false
  }
}

async function removeAttachment(attachment: UploadedAttachment) {
  composeAttachments.value = composeAttachments.value.filter((item) => item.blobId !== attachment.blobId)
  try {
    await forgetUploadedAttachment(attachment.blobId)
  } catch (error) {
    composeError.value = messageOf(error)
  }
}

async function downloadSelectedAttachment(attachment: NonNullable<EmailSummary['attachments']>[number]) {
  if (downloadingAttachmentId.value) return
  downloadingAttachmentId.value = attachment.blobId
  attachmentDownloadError.value = ''
  try {
    await downloadAttachment(attachment.blobId, attachment.name)
  } catch (error) {
    attachmentDownloadError.value = messageOf(error)
  } finally {
    downloadingAttachmentId.value = ''
  }
}

async function handleLogout() {
  await logout()
  window.location.assign('/api/oauth/login')
}

function toggleTheme() {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
  localStorage.setItem('openatom-theme', theme.value)
  applyTheme()
}

function applyTheme() {
  document.documentElement.dataset.theme = theme.value
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    focusSearch()
  }
  if (event.key === 'Escape' && composeOpen.value) requestCloseCompose()
}

function folderIcon(role: string | null) {
  return ({ inbox: Inbox, sent: Send, drafts: FilePenLine, trash: Trash2, junk: ShieldCheck, archive: Archive } as const)[role ?? ''] ?? Mail
}

function folderName(folder: Mailbox) {
  return ({ inbox: '收件箱', sent: '已发送', drafts: '草稿箱', trash: '废纸篓', junk: '垃圾邮件', archive: '归档' } as Record<string, string>)[folder.role ?? ''] ?? folder.name
}

function senderName(email: EmailSummary) { return email.from?.[0]?.name || email.from?.[0]?.email || '未知发件人' }
function senderAddress(email: EmailSummary) { return email.from?.[0]?.email || '' }
function senderInitial(email: EmailSummary) { return senderName(email).trim().slice(0, 1).toUpperCase() }
function recipientText(email: EmailSummary) { return email.to?.map((item) => item.name || item.email).join('、') || '我' }
function compactNumber(value: number) { return value > 99 ? '99+' : String(value) }
function formatListDate(value: string) {
  const date = new Date(value)
  const today = new Date()
  return date.toDateString() === today.toDateString()
    ? new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(date)
    : new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric' }).format(date)
}
function formatFullDate(value: string) { return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'long', timeStyle: 'short' }).format(new Date(value)) }
function formatBytes(value: number) {
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KiB`
  return `${(value / 1024 / 1024).toFixed(1)} MiB`
}
function messageOf(error: unknown) { return error instanceof Error ? error.message : '发生未知错误，请稍后重试。' }
function showToast(value: string) {
  toast.value = value
  if (toastTimer) window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => { toast.value = '' }, 4000)
}
</script>