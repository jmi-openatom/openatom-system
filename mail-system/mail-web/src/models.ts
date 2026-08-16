// Model layer: shared domain types for the mail client.

export interface SessionView {
  authenticated: boolean
  displayName: string | null
  address: string | null
  status: string | null
  csrfToken: string | null
}

export interface UploadedAttachment {
  blobId: string
  name: string
  type: string
  size: number
}

export interface JmapSession {
  primaryAccounts: Record<string, string>
  accounts: Record<string, { name: string; isReadOnly: boolean }>
}

export interface JmapResponse {
  methodResponses: [string, Record<string, unknown>, string][]
}

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
