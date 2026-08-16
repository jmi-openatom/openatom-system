// ViewModel: compose dialog state, attachment pipeline and send action.
import { nextTick, reactive, ref } from 'vue'
import { forgetUploadedAttachment, uploadAttachment, type UploadedAttachment } from '../api'
import type { MailContext, SessionView } from '../models'
import { htmlToPlainText, sendEmail } from '../mail'

const open = ref(false)
const dialog = ref<HTMLElement | null>(null)
const attachmentInput = ref<HTMLInputElement | null>(null)
const sending = ref(false)
const uploadingAttachment = ref(false)
const error = ref('')
const compose = reactive({ to: '', subject: '', body: '' })
const attachments = ref<UploadedAttachment[]>([])

export function openCompose(replyTo?: { address: string; subject: string }): void {
  if (replyTo) {
    compose.to = replyTo.address
    compose.subject = replyTo.subject.startsWith('Re:')
      ? replyTo.subject
      : `Re: ${replyTo.subject}`
  }
  open.value = true
  nextTick(() => dialog.value?.querySelector<HTMLInputElement>('input')?.focus())
}

export function requestCloseCompose(): void {
  if (
    compose.to ||
    compose.subject ||
    htmlToPlainText(compose.body) ||
    attachments.value.length
  ) {
    if (!window.confirm('这封邮件尚未发送，确定放弃草稿吗？')) return
  }
  closeCompose()
}

export function closeCompose(discardAttachments = true): void {
  const discarded = attachments.value
  open.value = false
  error.value = ''
  attachments.value = []
  Object.assign(compose, { to: '', subject: '', body: '' })
  if (discardAttachments) {
    for (const attachment of discarded) void forgetUploadedAttachment(attachment.blobId)
  }
}

export async function submitCompose(
  mailContext: MailContext,
  session: SessionView,
  toast: (msg: string) => void,
): Promise<void> {
  if (!session.address || !mailContext.identityId) {
    error.value = '邮箱身份尚未就绪，请刷新后重试。'
    return
  }
  const recipients = compose.to
    .split(/[，,;；]/)
    .map((item) => item.trim())
    .filter(Boolean)
  if (!recipients.length || recipients.some((item) => !/^\S+@\S+\.\S+$/.test(item))) {
    error.value = '请填写有效的收件人地址；多个地址使用逗号分隔。'
    return
  }
  const html = compose.body.trim()
  const text = htmlToPlainText(html)
  if (!text) {
    error.value = '请填写邮件正文。'
    return
  }
  const drafts = mailContext.mailboxes.find((item) => item.role === 'drafts')
  if (!drafts) {
    error.value = '草稿箱尚未创建，请联系管理员。'
    return
  }
  sending.value = true
  error.value = ''
  try {
    await sendEmail({
      accountId: mailContext.accountId,
      identityId: mailContext.relayIdentity?.id ?? mailContext.identityId,
      draftsMailboxId: drafts.id,
      fromName: session.displayName || '开放原子成员',
      fromAddress: mailContext.relayIdentity?.email ?? session.address,
      to: recipients,
      subject: compose.subject.trim() || '（无主题）',
      htmlBody: html,
      textBody: text,
      attachments: attachments.value,
    })
    closeCompose(false)
    toast('邮件已提交发送')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '发送失败，请稍后重试。'
  } finally {
    sending.value = false
  }
}

export async function handleAttachmentSelection(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  if (!files.length) return
  uploadingAttachment.value = true
  error.value = ''
  try {
    for (const file of files) {
      if (attachments.value.length >= 10) throw new Error('每封邮件最多添加 10 个附件。')
      if (file.size > 20 * 1024 * 1024) throw new Error(`“${file.name}”超过 20 MiB。`)
      const total = attachments.value.reduce((sum, item) => sum + item.size, 0)
      if (total + file.size > 20 * 1024 * 1024) throw new Error('附件总大小不能超过 20 MiB。')
      attachments.value.push(await uploadAttachment(file))
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '上传附件失败。'
  } finally {
    uploadingAttachment.value = false
  }
}

export async function removeAttachment(attachment: UploadedAttachment): Promise<void> {
  attachments.value = attachments.value.filter((item) => item.blobId !== attachment.blobId)
  try {
    await forgetUploadedAttachment(attachment.blobId)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '移除附件失败。'
  }
}

export function useComposeStore() {
  return {
    open,
    dialog,
    attachmentInput,
    sending,
    uploadingAttachment,
    error,
    compose,
    attachments,
    openCompose,
    requestCloseCompose,
    closeCompose,
    submitCompose,
    handleAttachmentSelection,
    removeAttachment,
  }
}
