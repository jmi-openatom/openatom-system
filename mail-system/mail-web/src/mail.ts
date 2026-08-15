import { jmap, loadJmapSession, type UploadedAttachment } from './api'

export interface Mailbox {
  id: string
  name: string
  role: string | null
  totalEmails: number
  unreadEmails: number
  sortOrder: number
}

export interface Address {
  name?: string
  email: string
}

export interface EmailSummary {
  id: string
  threadId: string
  mailboxIds: Record<string, boolean>
  keywords: Record<string, boolean>
  receivedAt: string
  from: Address[]
  to: Address[]
  subject: string
  preview: string
  bodyValues?: Record<string, { value: string; isTruncated: boolean }>
  attachments?: UploadedAttachment[]
}

export interface RelayIdentity {
  id: string
  email: string
}

export interface MailContext {
  accountId: string
  mailboxes: Mailbox[]
  identityId: string | null
  /** Identity on the Resend-verified relay domain, used for outbound mail. */
  relayIdentity: RelayIdentity | null
}

export async function bootstrapMail(mailboxAddress: string | null): Promise<MailContext> {
  const session = await loadJmapSession()
  const accountId = session.primaryAccounts['urn:ietf:params:jmap:mail']
  if (!accountId) throw new Error('邮箱账户尚未就绪')
  const response = await jmap([
    ['Mailbox/get', { accountId, ids: null }, 'mailboxes'],
    ['Identity/get', { accountId, ids: null }, 'identities'],
  ])
  const mailboxResult = response.methodResponses.find((item) => item[2] === 'mailboxes')?.[1]
  const identityResult = response.methodResponses.find((item) => item[2] === 'identities')?.[1]
  const identities = ((identityResult?.list as { id: string; email?: string; name?: string }[] | undefined) ?? [])
  const relayIdentity = await ensureRelayIdentity(accountId, identities, mailboxAddress)
  return {
    accountId,
    mailboxes: ((mailboxResult?.list as Mailbox[] | undefined) ?? []).sort(
      (left, right) => left.sortOrder - right.sortOrder,
    ),
    identityId: identities[0]?.id ?? null,
    relayIdentity,
  }
}

/**
 * The Resend relay only accepts senders on the verified domain
 * mailer.jmi-openatom.cn, while the mailbox lives on jmi-openatom.cn.
 * Find (or create) an Identity whose address uses the relay domain so
 * EmailSubmission can send as it.
 */
export async function ensureRelayIdentity(
  accountId: string,
  identities: { id: string; email?: string; name?: string }[],
  mailboxAddress: string | null,
): Promise<RelayIdentity | null> {
  const relayDomain = 'mailer.jmi-openatom.cn'
  const local = (mailboxAddress ?? '').split('@')[0]
  if (!local) return null
  const relayEmail = local + '@' + relayDomain
  const existing = identities.find((it) => it.email?.toLowerCase() === relayEmail.toLowerCase())
  if (existing) return { id: existing.id, email: relayEmail }
  const displayName = identities[0]?.name
  const created = await jmap([
    ['Identity/set', {
      accountId,
      create: { relay: { name: displayName || '成员', email: relayEmail, sortOrder: 100 } },
    }, 'relay'],
  ])
  const relayResult = created.methodResponses[0]?.[1]
  const id = relayResult && typeof relayResult === 'object' && 'created' in relayResult
    ? (relayResult as { created?: { relay?: { id?: string } } }).created?.relay?.id
    : undefined
  return id ? { id, email: relayEmail } : null
}

export async function queryEmails(
  accountId: string,
  mailboxId: string | null,
  search: string,
): Promise<EmailSummary[]> {
  const conditions: Record<string, unknown>[] = []
  if (mailboxId) conditions.push({ inMailbox: mailboxId })
  if (search.trim()) conditions.push({ text: search.trim() })
  const filter = conditions.length > 1 ? { operator: 'AND', conditions } : (conditions[0] ?? {})
  const response = await jmap([
    [
      'Email/query',
      { accountId, filter, sort: [{ property: 'receivedAt', isAscending: false }], limit: 50 },
      'query',
    ],
  ])
  const ids = (response.methodResponses[0]?.[1].ids as string[] | undefined) ?? []
  if (!ids.length) return []
  const details = await jmap([
    [
      'Email/get',
      {
        accountId,
        ids,
        properties: [
          'id', 'threadId', 'mailboxIds', 'keywords', 'receivedAt', 'from', 'to', 'subject', 'preview',
        ],
      },
      'emails',
    ],
  ])
  return (details.methodResponses[0]?.[1].list as EmailSummary[] | undefined) ?? []
}

export async function getEmail(accountId: string, id: string): Promise<EmailSummary> {
  const response = await jmap([
    [
      'Email/get',
      {
        accountId,
        ids: [id],
        properties: [
          'id', 'threadId', 'mailboxIds', 'keywords', 'receivedAt', 'from', 'to', 'subject',
          'preview', 'bodyValues', 'textBody', 'attachments',
        ],
        fetchTextBodyValues: true,
        maxBodyValueBytes: 262144,
      },
      'email',
    ],
  ])
  const email = ((response.methodResponses[0]?.[1].list as EmailSummary[] | undefined) ?? [])[0]
  if (!email) throw new Error('邮件不存在或已被删除')
  if (!email.keywords.$seen) {
    await jmap([
      ['Email/set', { accountId, update: { [id]: { 'keywords/$seen': true } } }, 'seen'],
    ])
    email.keywords.$seen = true
  }
  return email
}

export async function sendEmail(input: {
  accountId: string
  identityId: string
  draftsMailboxId: string
  fromName: string
  fromAddress: string
  to: string[]
  subject: string
  body: string
  attachments: UploadedAttachment[]
}): Promise<void> {
  const draftId = 'draft'
  const response = await jmap([
    [
      'Email/set',
      {
        accountId: input.accountId,
        create: {
          [draftId]: {
            mailboxIds: { [input.draftsMailboxId]: true },
            keywords: { $draft: true },
            from: [{ name: input.fromName, email: input.fromAddress }],
            to: input.to.map((email) => ({ email })),
            subject: input.subject,
            textBody: [{ partId: 'body', type: 'text/plain' }],
            bodyValues: { body: { value: input.body, charset: 'utf-8' } },
            attachments: input.attachments.length
              ? input.attachments.map((attachment) => ({
                  blobId: attachment.blobId,
                  name: attachment.name,
                  type: attachment.type,
                  size: attachment.size,
                  disposition: 'attachment',
                }))
              : undefined,
          },
        },
      },
      'draft',
    ],
    [
      'EmailSubmission/set',
      {
        accountId: input.accountId,
        create: { send: { emailId: `#${draftId}`, identityId: input.identityId } },
        onSuccessDestroyEmail: [`#${draftId}`],
      },
      'submit',
    ],
  ])
  for (const [method, result] of response.methodResponses) {
    if (method === 'error') throw new Error(jmapFailureMessage(String(result.type ?? '')))
    for (const failureField of ['notCreated', 'notUpdated', 'notDestroyed'] as const) {
      const failures = result[failureField]
      if (!failures || typeof failures !== 'object') continue
      const first = Object.values(failures as Record<string, unknown>)[0]
      if (first && typeof first === 'object') {
        const type = String((first as Record<string, unknown>).type ?? '')
        throw new Error(jmapFailureMessage(type))
      }
    }
  }
  const submission = response.methodResponses.find((item) => item[0] === 'EmailSubmission/set')?.[1]
  const created = submission?.created
  if (!created || typeof created !== 'object' || !(created as Record<string, unknown>).send) {
    throw new Error('邮件服务器未确认提交，请刷新“已发送”后重试。')
  }
}

function jmapFailureMessage(type: string): string {
  const messages: Record<string, string> = {
    overQuota: '邮箱空间已满，请清理邮件后重试。',
    tooLarge: '邮件或附件超过服务器允许的大小。',
    rateLimit: '发信过于频繁，请稍后重试。',
    tooManyRecipients: '收件人数超过服务器限制。',
    invalidRecipients: '部分收件人地址不存在或不可投递。',
    invalidEmail: '邮件格式无效，请检查收件人、主题和附件。',
    invalidProperties: '邮件内容不符合服务器要求，请检查后重试。',
    forbidden: '当前邮箱没有发信权限，账号可能已被暂停。',
    notFound: '草稿或发件身份已失效，请刷新页面后重试。',
    serverFail: '邮件服务器暂时无法处理请求，请稍后重试。',
  }
  return messages[type] ?? '邮件未能提交，请检查收件人后重试。'
}
