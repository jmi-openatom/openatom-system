<template>
  <a class="skip-link" href="#mail-main">跳到主要内容</a>

  <div v-if="loading" aria-live="polite" class="boot-screen">
    <span aria-hidden="true" class="spinner"></span>
    <p>正在连接开放原子邮箱…</p>
  </div>

  <LoginView v-else-if="!session.authenticated" />

  <ActivationWizard
    v-else-if="needsActivation"
    @done="onActivationDone"
  />

  <div v-else-if="mailboxStatus?.status === 'SUSPENDED'" class="boot-screen suspended-screen">
    <ShieldAlert :size="40" />
    <h2>邮箱已停用</h2>
    <p>你的邮箱账号已被管理员停用，如需恢复请联系管理员。</p>
    <button class="secondary-button" type="button" @click="onLogout">退出登录</button>
  </div>

  <AdminView
    v-else-if="view === 'admin' && mailboxStatus?.isAdmin"
    :session="session"
    @logout="onLogout"
    @back="view = 'mail'"
  />

  <MailView
    v-else-if="mailContext"
    :session="session"
    :mail-context="mailContext"
    :is-admin="!!mailboxStatus?.isAdmin"
    @logout="onLogout"
    @admin="view = 'admin'"
  />

  <div v-else class="boot-screen"><span aria-hidden="true" class="spinner"></span><p>邮箱账户尚未就绪…</p></div>

  <ToastRegion />
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { ShieldAlert } from 'lucide-vue-next'
import LoginView from './views/LoginView.vue'
import MailView from './views/MailView.vue'
import AdminView from './views/AdminView.vue'
import ActivationWizard from './components/mail/ActivationWizard.vue'
import ToastRegion from './components/common/ToastRegion.vue'
import { useSessionStore } from './stores/session'
import { useUiStore } from './stores/ui'
import { useComposeStore } from './stores/compose'

const { session, loading, mailContext, mailboxStatus, initSession, signOut } = useSessionStore()
const { initTheme, useGlobalShortcuts } = useUiStore()
const { requestCloseCompose } = useComposeStore()

const view = ref<'mail' | 'admin'>('mail')

/** First-login activation: mailbox exists but has no address yet. */
const needsActivation = computed(() => {
  const status = mailboxStatus.value
  return (
    !!status &&
    status.status !== 'SUSPENDED' &&
    (status.provisionStatus === 'WAITING_PROFILE' || status.provisionStatus === 'PENDING')
  )
})

useGlobalShortcuts(
  () => document.querySelector<HTMLInputElement>('.search-field input')?.focus(),
  () => requestCloseCompose(),
)

onMounted(() => {
  initTheme()
  void initSession()
})

async function onLogout() {
  await signOut()
}

async function onActivationDone() {
  // Re-fetch session + mailbox status instead of a hard reload, so the
  // response-driven views switch to the mail context without a reload loop.
  await initSession()
}
</script>