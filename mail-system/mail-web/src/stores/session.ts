// ViewModel: session state and auth actions.
import { ref } from 'vue'
import { loadMailboxStatus, loadSession, logout as apiLogout, type MailboxStatusView, type SessionView } from '../api'
import type { MailContext } from '../models'
import { bootstrapMail } from '../mail'

const session = ref<SessionView>({
  authenticated: false,
  displayName: null,
  address: null,
  status: null,
  csrfToken: null,
})
const loading = ref(true)
const mailContext = ref<MailContext | null>(null)
const mailboxStatus = ref<MailboxStatusView | null>(null)

/** Loads the current session and, when authenticated, the mail context. */
export async function initSession(): Promise<void> {
  loading.value = true
  try {
    session.value = await loadSession()
    if (session.value.authenticated) {
      mailboxStatus.value = await loadMailboxStatus()
    }
    // Bootstrap the mail context only when the mailbox is actually active;
    // WAITING/PENDING and SUSPENDED mailboxes are handled by dedicated views.
    if (
      session.value.authenticated &&
      mailboxStatus.value?.provisionStatus === 'ACTIVE' &&
      mailboxStatus.value.status === 'ACTIVE'
    ) {
      try {
        mailContext.value = await bootstrapMail(session.value.address)
      } catch (error) {
        mailContext.value = null
      }
    }
  } catch (error) {
    // status call can fail if mailbox not provisioned yet; keep session
  } finally {
    loading.value = false
  }
}

export async function signOut(): Promise<void> {
  try {
    await apiLogout()
  } finally {
    session.value = {
      authenticated: false,
      displayName: null,
      address: null,
      status: null,
      csrfToken: null,
    }
    mailContext.value = null
    window.location.assign('/')
  }
}

export function useSessionStore() {
  return { session, loading, mailContext, mailboxStatus, initSession, signOut }
}