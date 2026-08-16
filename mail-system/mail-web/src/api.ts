export interface SessionView {
  authenticated: boolean
  displayName: string | null
  address: string | null
  status: string | null
  csrfToken: string | null
}

let csrfToken = ''

/** 会话失效时跳转到 OAuth 登录（仅一次，避免循环）。 */
export function redirectToOAuth(): void {
  if (window.location.pathname !== '/api/oauth/login') {
    window.location.assign('/api/oauth/login')
  }
}

export interface UploadedAttachment {
  blobId: string
  name: string
  type: string
  size: number
}

export async function loadSession(): Promise<SessionView> {
  const response = await fetch('/api/session', { credentials: 'same-origin' })
  if (!response.ok) throw new Error('无法读取登录状态')
  const session = (await response.json()) as SessionView
  csrfToken = session.csrfToken ?? ''
  return session
}

export async function logout(): Promise<void> {
  const response = await fetch('/api/logout', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'X-Mail-CSRF': csrfToken },
  })
  if (!response.ok) throw new Error('退出登录失败')
}

export async function loadJmapSession(): Promise<JmapSession> {
  const response = await fetch('/api/jmap/session', { credentials: 'same-origin' })
  if (response.status === 401) {
    redirectToOAuth()
    throw new Error('登录已过期')
  }
  if (!response.ok) throw new Error('邮件服务暂不可用')
  return response.json() as Promise<JmapSession>
}

export async function jmap(methodCalls: unknown[][]): Promise<JmapResponse> {
  const response = await fetch('/api/jmap', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', 'X-Mail-CSRF': csrfToken },
    body: JSON.stringify({
      using: ['urn:ietf:params:jmap:core', 'urn:ietf:params:jmap:mail', 'urn:ietf:params:jmap:submission'],
      methodCalls,
    }),
  })
  if (response.status === 401) {
    redirectToOAuth()
    throw new Error('登录已过期，正在跳转到登录页…')
  }
  if (!response.ok) {
    const problem = await response.clone().json().catch(() => ({})) as { code?: string; detail?: string }
    const code = problem.code ?? problem.detail ?? ''
    const messages: Record<string, string> = {
      message_too_large: '邮件超过 25 MiB 限制，请缩短正文后重试。',
      attachment_not_uploaded: '附件状态已失效，请移除后重新上传。',
      attachment_count_exceeded: '每封邮件最多添加 10 个附件。',
      attachment_too_large: '单个附件不能超过 20 MiB。',
      attachments_total_too_large: '附件总大小不能超过 20 MiB。',
      attachment_type_not_allowed: '为保护收件人，不能发送可执行文件或主动网页内容。',
      attachment_malware_detected: '附件被安全扫描判定为恶意文件，已拒绝发送。',
      attachment_scan_unavailable: '附件安全扫描暂不可用，请稍后重试。',
      recipient_limit_exceeded: '每封邮件需包含 1–25 位收件人。',
      send_rate_exceeded: '发信过于频繁，请一分钟后重试。',
      mailbox_not_active: '邮箱已暂停或尚未开通，当前不能发信。',
      oauth_session_expired: '登录已过期，请重新登录。',
    }
    throw new Error(messages[code] ?? (response.status === 401 ? '登录已过期' : '邮件操作失败'))
  }
  return response.json() as Promise<JmapResponse>
}

export async function uploadAttachment(file: File): Promise<UploadedAttachment> {
  const form = new FormData()
  form.append('file', file, file.name)
  const response = await fetch('/api/attachments', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'X-Mail-CSRF': csrfToken },
    body: form,
  })
  if (!response.ok) throw new Error(await attachmentError(response))
  return response.json() as Promise<UploadedAttachment>
}

export async function forgetUploadedAttachment(blobId: string): Promise<void> {
  const response = await fetch(`/api/attachments/${encodeURIComponent(blobId)}`, {
    method: 'DELETE',
    credentials: 'same-origin',
    headers: { 'X-Mail-CSRF': csrfToken },
  })
  if (!response.ok && response.status !== 404) throw new Error('移除附件失败，请稍后重试。')
}

export async function downloadAttachment(blobId: string, name: string): Promise<void> {
  const filename = name || 'attachment.bin'
  const response = await fetch(
    `/api/attachments/${encodeURIComponent(blobId)}?name=${encodeURIComponent(filename)}`,
    { credentials: 'same-origin' },
  )
  if (!response.ok) throw new Error(await attachmentError(response, 'download'))

  const objectUrl = URL.createObjectURL(await response.blob())
  const anchor = document.createElement('a')
  try {
    anchor.href = objectUrl
    anchor.download = filename
    anchor.hidden = true
    document.body.append(anchor)
    anchor.click()
  } finally {
    anchor.remove()
    URL.revokeObjectURL(objectUrl)
  }
}

async function attachmentError(response: Response, operation: 'upload' | 'download' = 'upload'): Promise<string> {
  const problem = await response.clone().json().catch(() => ({})) as { code?: string; detail?: string }
  const code = problem.code ?? problem.detail ?? ''
  const messages: Record<string, string> = {
    attachment_empty: '不能上传空文件。',
    attachment_too_large: '单个附件不能超过 20 MiB。',
    attachments_total_too_large: '附件总大小不能超过 20 MiB。',
    attachment_count_exceeded: '每封邮件最多添加 10 个附件。',
    attachment_type_not_allowed: '为保护收件人，不能发送可执行文件或主动网页内容。',
    attachment_malware_detected: `附件被安全扫描判定为恶意文件，已拒绝${operation === 'download' ? '下载' : '上传'}。`,
    attachment_scan_unavailable: '附件安全扫描暂不可用，请稍后重试。',
    mailbox_not_active: '邮箱已暂停或尚未开通，当前不能上传附件。',
  }
  return messages[code] ?? (
    response.status === 401
      ? '登录已过期，请重新登录。'
      : `附件${operation === 'download' ? '下载' : '上传'}失败，请稍后重试。`
  )
}

export interface JmapSession {
  primaryAccounts: Record<string, string>
  accounts: Record<string, { name: string; isReadOnly: boolean }>
}

export interface JmapResponse {
  methodResponses: [string, Record<string, unknown>, string][]
}

// ===== Mailbox self-service =====
export interface MailboxStatusView {
  status: string
  provisionStatus: string
  address: string | null
  displayName: string
  isAdmin: boolean
}

export async function loadMailboxStatus(): Promise<MailboxStatusView> {
  const response = await fetch('/api/mailbox/status', { credentials: 'same-origin' })
  if (response.status === 401) {
    redirectToOAuth()
    throw new Error('登录已过期')
  }
  if (!response.ok) throw new Error('无法读取邮箱状态')
  return response.json() as Promise<MailboxStatusView>
}

export async function activateMailbox(options: {
  usePinyin?: boolean
  localPart?: string
}): Promise<MailboxStatusView> {
  const response = await fetch('/api/mailbox/activate', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', 'X-Mail-CSRF': csrfToken },
    body: JSON.stringify(options),
  })
  if (!response.ok) {
    const problem = await response.clone().json().catch(() => ({})) as { code?: string }
    throw new Error(mailboxActivationError(problem.code ?? ''))
  }
  return response.json() as Promise<MailboxStatusView>
}

function mailboxActivationError(code: string): string {
  const messages: Record<string, string> = {
    local_part_required: '请填写你想要的主机名。',
    invalid_or_reserved_local_part: '该主机名不可用或已被保留，请换一个。',
    mailbox_address_space_exhausted: '地址空间已用尽，请换一个主机名。',
    cannot_generate_address: '无法根据姓名生成地址，请手动填写。',
    mailbox_not_active: '邮箱尚未开通，请稍后重试。',
  }
  return messages[code] ?? '激活失败，请稍后重试。'
}

// ===== Admin =====
export interface AdminMailboxView {
  id: number
  sub: string
  userId: number
  displayName: string | null
  address: string | null
  mailDomain: string
  status: string
  provisionStatus: string
  lastEventId: string | null
}

export interface AdminStats {
  total: number
  active: number
  resend: { configured: boolean; verified?: boolean; domain?: string; region?: string; error?: string }
}

export interface AdminMailboxPage {
  rows: AdminMailboxView[]
  total: number
  page: number
  pageSize: number
}

export async function loadAdminMailboxes(options: {
  page?: number
  pageSize?: number
  keyword?: string
  sort?: string
  order?: string
} = {}): Promise<AdminMailboxPage> {
  const params = new URLSearchParams()
  params.set('page', String(options.page ?? 1))
  params.set('pageSize', String(options.pageSize ?? 20))
  params.set('sort', options.sort ?? 'id')
  params.set('order', options.order ?? 'desc')
  if (options.keyword) params.set('keyword', options.keyword)
  const response = await fetch('/api/admin/mailboxes?' + params.toString(), { credentials: 'same-origin' })
  if (response.status === 401) {
    redirectToOAuth()
    throw new Error('登录已过期')
  }
  if (!response.ok) throw new Error(response.status === 403 ? '没有管理员权限' : '无法加载邮箱列表')
  return response.json() as Promise<AdminMailboxPage>
}

export async function loadAdminStats(): Promise<AdminStats> {
  const response = await fetch('/api/admin/stats', { credentials: 'same-origin' })
  if (!response.ok) throw new Error('无法加载统计信息')
  return response.json() as Promise<AdminStats>
}

export async function setMailboxSuspended(id: number, suspended: boolean): Promise<void> {
  const response = await fetch('/api/admin/mailboxes/' + id + '/suspend', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', 'X-Mail-CSRF': csrfToken },
    body: JSON.stringify({ suspended }),
  })
  if (!response.ok) throw new Error('操作失败，请稍后重试')
}

export interface ExternalRecipient {
  userId: number
  name: string
  email: string
}

export interface ExternalRecipientPage {
  rows: ExternalRecipient[]
  total: number
  page: number
  pageSize: number
}

export async function loadExternalRecipients(options: {
  page?: number
  pageSize?: number
  keyword?: string
} = {}): Promise<ExternalRecipientPage> {
  const params = new URLSearchParams()
  params.set('page', String(options.page ?? 1))
  params.set('pageSize', String(options.pageSize ?? 200))
  if (options.keyword) params.set('keyword', options.keyword)
  const response = await fetch('/api/admin/external-recipients?' + params.toString(), { credentials: 'same-origin' })
  if (response.status === 401) {
    redirectToOAuth()
    throw new Error('登录已过期')
  }
  if (!response.ok) throw new Error(response.status === 403 ? '没有管理员权限' : '无法加载收件人列表')
  return response.json() as Promise<ExternalRecipientPage>
}

export async function sendBroadcast(input: {
  recipients: string[]
  subject: string
  htmlBody: string
  textBody: string
}): Promise<{ id: string; recipients: number }> {
  const response = await fetch('/api/admin/broadcast', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', 'X-Mail-CSRF': csrfToken },
    body: JSON.stringify(input),
  })
  if (response.status === 401) {
    redirectToOAuth()
    throw new Error('登录已过期')
  }
  if (!response.ok) {
    const problem = await response.clone().json().catch(() => ({})) as { code?: string }
    throw new Error(broadcastError(problem.code ?? ''))
  }
  return response.json() as Promise<{ id: string; recipients: number }>
}

function broadcastError(code: string): string {
  const messages: Record<string, string> = {
    recipients_required: '请至少选择一位收件人。',
    too_many_recipients: '收件人数量超过上限，请分批发送。',
    invalid_recipients: '部分收件人邮箱格式不正确。',
    content_required: '请填写主题和正文。',
    resend_not_configured: '发信服务未配置，请联系运维。',
    resend_not_configured_re_required: '发信服务未配置，请联系运维。',
    main_site_unreachable: '无法连接主站获取用户邮箱，请稍后重试。',
    main_site_not_configured: '主站接口未配置，无法拉取收件人。',
  }
  return messages[code] ?? '群发失败，请稍后重试。'
}