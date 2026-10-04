<template>
  <ViewPage class="auth-callback-page">
    <div class="auth-callback-panel">
      <img class="auth-callback-logo" src="/logo.svg?v=20261005" alt="开放原子开源社团" />
      <h1>正在完成登录</h1>
      <p>{{ message }}</p>
    </div>
  </ViewPage>
</template>

<script setup lang="ts">
import axios from 'axios'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ViewPage from '@/components/common/ViewPage.vue'
import { getToken, setSession } from '@/utils/auth.ts'
import {
  consumeOidcCallbackState,
  getOidcAuthority,
  getOidcClientId,
  verifyOidcIdToken,
} from '@/utils/oidc.ts'

const route = useRoute()
const router = useRouter()
const message = ref('请稍候')

onMounted(async () => {
  const code = Array.isArray(route.query.code) ? route.query.code[0] : route.query.code
  const state = Array.isArray(route.query.state) ? route.query.state[0] : route.query.state
  const oauthError = Array.isArray(route.query.error) ? route.query.error[0] : route.query.error
  if (oauthError) {
    message.value = `登录授权失败：${oauthError}`
    return
  }
  if (!code) {
    message.value = '缺少授权码'
    return
  }
  try {
    const { codeVerifier, nonce, returnTo } = consumeOidcCallbackState(state)
    const redirectUri = `${window.location.origin}/auth/callback`
    const response = await axios.post(
      `${getOidcAuthority()}/oauth/token`,
      new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: getOidcClientId(),
        code,
        redirect_uri: redirectUri,
        code_verifier: codeVerifier,
      }),
      { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } },
    )
    const result = response.data
    await verifyOidcIdToken(result.id_token, nonce)
    // The main site already authenticated through /auth/login before the OAuth
    // redirect. Its API expects that Sa-Token session, not the OIDC access token.
    if (!getToken()) throw new Error('主站登录状态已过期')
    setSession({
      user: result.user,
      roles: result.user?.roles || [],
      permissions: result.user?.permissions || [],
    })
    router.replace(returnTo.startsWith('/') ? returnTo : '/admin/dashboard')
  } catch (_error) {
    message.value = '登录失败，请重新发起授权'
  }
})
</script>

<style scoped>
.auth-callback-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: var(--oa-page-bg);
}

.auth-callback-panel {
  display: grid;
  gap: 12px;
  justify-items: center;
  color: var(--oa-text);
}

.auth-callback-logo {
  display: block;
  width: 54px;
  height: 54px;
  object-fit: contain;
}

.auth-callback-panel h1 {
  margin: 0;
  font-size: 24px;
}

.auth-callback-panel p {
  margin: 0;
  color: var(--oa-text-muted);
}
</style>
