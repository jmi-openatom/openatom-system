<template>
  <a class="skip-link" href="#mail-main">跳到主要内容</a>

  <div v-if="loading" aria-live="polite" class="boot-screen">
    <span aria-hidden="true" class="spinner"></span>
    <p>正在连接开放原子邮箱…</p>
  </div>

  <LoginView v-else-if="!session.authenticated" />
  <MailView v-else-if="mailContext" :session="session" :mail-context="mailContext" @logout="onLogout" />
  <div v-else class="boot-screen"><span aria-hidden="true" class="spinner"></span><p>邮箱账户尚未就绪…</p></div>

  <ToastRegion />
</template>

<script lang="ts" setup>
import { onMounted } from 'vue'
import LoginView from './views/LoginView.vue'
import MailView from './views/MailView.vue'
import ToastRegion from './components/common/ToastRegion.vue'
import { useSessionStore } from './stores/session'
import { useUiStore } from './stores/ui'
import { useComposeStore } from './stores/compose'

const { session, loading, mailContext, initSession, signOut } = useSessionStore()
const { initTheme, useGlobalShortcuts } = useUiStore()
const { requestCloseCompose } = useComposeStore()

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
</script>
