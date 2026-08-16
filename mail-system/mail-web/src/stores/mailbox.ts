// ViewModel: mailbox navigation, email list, reader selection and actions.
import { computed, ref } from 'vue'
import type { EmailSummary, MailContext } from '../models'
import { destroyEmail, getEmail, moveEmail, queryEmails, setEmailSeen, type MailFilter } from '../mail'

const selectedMailboxId = ref<string | null>(null)
const emails = ref<EmailSummary[]>([])
const selectedEmail = ref<EmailSummary | null>(null)
const search = ref('')
const mailFilter = ref<MailFilter>('all')
const mailLoading = ref(false)
const detailLoading = ref(false)
const actionBusy = ref(false)
const moreMenuOpen = ref(false)
const errorMessage = ref('')

export function visibleFolders(mailContext: MailContext) {
  return mailContext.mailboxes.filter(
    (item) => item.role !== 'all' && item.role !== 'important',
  )
}

export function activeFolderName(mailContext: MailContext) {
  const folder = visibleFolders(mailContext).find((item) => item.id === selectedMailboxId.value)
  return folder ? folderName(folder) : '全部邮件'
}

const selectedBody = computed(() => {
  const values = Object.values(selectedEmail.value?.bodyValues ?? {})
  return (
    values.map((item) => item.value).join('\n\n') ||
    selectedEmail.value?.preview ||
    '这封邮件没有可显示的纯文本内容。'
  )
})

export async function loadMailbox(mailContext: MailContext): Promise<void> {
  mailLoading.value = true
  errorMessage.value = ''
  try {
    selectedMailboxId.value =
      mailContext.mailboxes.find((item) => item.role === 'inbox')?.id ??
      mailContext.mailboxes[0]?.id ??
      null
    await loadEmails(mailContext)
  } finally {
    mailLoading.value = false
  }
}

export async function loadEmails(mailContext: MailContext): Promise<void> {
  mailLoading.value = true
  errorMessage.value = ''
  try {
    emails.value = await queryEmails(
      mailContext.accountId,
      selectedMailboxId.value,
      search.value,
      mailFilter.value,
    )
  } catch (error) {
    errorMessage.value = messageOf(error)
  } finally {
    mailLoading.value = false
  }
}

export async function refresh(mailContext: MailContext): Promise<void> {
  await loadEmails(mailContext)
}

export async function selectFolder(id: string, mailContext: MailContext): Promise<void> {
  selectedMailboxId.value = id
  selectedEmail.value = null
  mailFilter.value = 'all'
  await loadEmails(mailContext)
}

export function setFilter(filter: MailFilter, mailContext: MailContext): void {
  mailFilter.value = filter
  selectedEmail.value = null
  void loadEmails(mailContext)
}

export async function selectEmail(id: string, mailContext: MailContext): Promise<void> {
  detailLoading.value = true
  try {
    selectedEmail.value = await getEmail(mailContext.accountId, id)
  } catch (error) {
    errorMessage.value = messageOf(error)
  } finally {
    detailLoading.value = false
  }
}

export function scheduleSearch(mailContext: MailContext): void {
  window.setTimeout(() => void loadEmails(mailContext), 350)
}

export async function archiveSelected(mailContext: MailContext, toast: (msg: string) => void): Promise<void> {
  const email = selectedEmail.value
  const archiveId = mailboxIdByRole(mailContext, 'archive')
  if (!email || !archiveId) return
  if (email.mailboxIds?.[archiveId]) {
    moreMenuOpen.value = false
    return
  }
  actionBusy.value = true
  try {
    await moveEmail(mailContext.accountId, email.id, archiveId)
    await loadEmails(mailContext)
    selectedEmail.value = null
    toast('已归档')
  } catch (error) {
    errorMessage.value = messageOf(error)
  } finally {
    actionBusy.value = false
    moreMenuOpen.value = false
  }
}

export async function deleteSelected(mailContext: MailContext, toast: (msg: string) => void): Promise<void> {
  const email = selectedEmail.value
  const trashId = mailboxIdByRole(mailContext, 'trash')
  if (!email || !trashId) return
  if (email.mailboxIds?.[trashId]) {
    moreMenuOpen.value = false
    return
  }
  actionBusy.value = true
  try {
    await moveEmail(mailContext.accountId, email.id, trashId)
    await loadEmails(mailContext)
    selectedEmail.value = null
    toast('已移到废纸篓')
  } catch (error) {
    errorMessage.value = messageOf(error)
  } finally {
    actionBusy.value = false
    moreMenuOpen.value = false
  }
}

export async function deleteForever(mailContext: MailContext, toast: (msg: string) => void): Promise<void> {
  const email = selectedEmail.value
  if (!email) return
  if (!window.confirm('彻底删除后无法恢复，确定删除这封邮件吗？')) return
  actionBusy.value = true
  try {
    await destroyEmail(mailContext.accountId, email.id)
    await loadEmails(mailContext)
    selectedEmail.value = null
    moreMenuOpen.value = false
    toast('已彻底删除')
  } catch (error) {
    errorMessage.value = messageOf(error)
  } finally {
    actionBusy.value = false
    moreMenuOpen.value = false
  }
}

export async function toggleSelectedSeen(mailContext: MailContext): Promise<void> {
  const email = selectedEmail.value
  if (!email) return
  const next = !email.keywords?.$seen
  try {
    await setEmailSeen(mailContext.accountId, email.id, next)
    email.keywords = { ...(email.keywords ?? {}), $seen: next }
    moreMenuOpen.value = false
  } catch (error) {
    errorMessage.value = messageOf(error)
  }
}

function mailboxIdByRole(mailContext: MailContext, role: string): string | null {
  return mailContext.mailboxes.find((item) => item.role === role)?.id ?? null
}

export function folderIcon(role: string | null) {
  const icons: Record<string, unknown> = {
    inbox: 'Inbox',
    sent: 'Send',
    drafts: 'FilePenLine',
    trash: 'Trash2',
    junk: 'ShieldCheck',
    archive: 'Archive',
  }
  return icons[role ?? ''] ?? 'Mail'
}

export function folderName(folder: { role: string | null; name: string }): string {
  return (
    ({
      inbox: '收件箱',
      sent: '已发送',
      drafts: '草稿箱',
      trash: '废纸篓',
      junk: '垃圾邮件',
      archive: '归档',
    } as Record<string, string>)[folder.role ?? ''] ?? folder.name
  )
}

export function senderName(email: EmailSummary): string {
  return email.from?.[0]?.name || email.from?.[0]?.email || '未知发件人'
}
export function senderAddress(email: EmailSummary): string {
  return email.from?.[0]?.email || ''
}
export function senderInitial(email: EmailSummary): string {
  return senderName(email).trim().slice(0, 1).toUpperCase()
}
export function recipientText(email: EmailSummary): string {
  return email.to?.map((item) => item.name || item.email).join('、') || '我'
}
export function compactNumber(value: number): string {
  return value > 99 ? '99+' : String(value)
}
export function formatListDate(value: string): string {
  const date = new Date(value)
  const today = new Date()
  return date.toDateString() === today.toDateString()
    ? new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(date)
    : new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric' }).format(date)
}
export function formatFullDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'long', timeStyle: 'short' }).format(
    new Date(value),
  )
}
export function formatBytes(value: number): string {
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KiB`
  return `${(value / 1024 / 1024).toFixed(1)} MiB`
}

export function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : '发生未知错误，请稍后重试。'
}

export function useMailboxStore() {
  return {
    selectedMailboxId,
    emails,
    selectedEmail,
    search,
    mailFilter,
    mailLoading,
    detailLoading,
    actionBusy,
    moreMenuOpen,
    errorMessage,
    visibleFolders,
    activeFolderName,
    selectedBody,
    loadMailbox,
    loadEmails,
    refresh,
    selectFolder,
    setFilter,
    selectEmail,
    scheduleSearch,
    archiveSelected,
    deleteSelected,
    deleteForever,
    toggleSelectedSeen,
  }
}
