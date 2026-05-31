<template>
  <ViewPage class="auth-callback-page">
    <div class="auth-callback-panel">
      <span class="auth-callback-logo">OA</span>
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
import { setSession } from '@/utils/auth.ts'
import { getOidcAuthority, getOidcClientId } from '@/utils/oidc.ts'

const route = useRoute()
const router = useRouter()
const message = ref('请稍候')

onMounted(async () => {
  const code = Array.isArray(route.query.code) ? route.query.code[0] : route.query.code
  if (!code) {
    message.value = '缺少授权码'
    return
  }
	try {
	  const redirectUri = `${window.location.origin}/auth/callback`
	  const response = await axios.post(
	    `${getOidcAuthority()}/oauth/token`,
	    new URLSearchParams({
	      grant_type: 'authorization_code',
	      client_id: getOidcClientId(),
	      code,
	      redirect_uri: redirectUri,
	    }),
	    { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } },
	  )
	  const result = response.data
    setSession({
      accessToken: result.access_token,
      refreshToken: result.refresh_token,
      user: result.user,
      roles: result.user?.roles || [],
      permissions: result.user?.permissions || [],
    })
    const state = Array.isArray(route.query.state) ? route.query.state[0] : route.query.state
    router.replace(state && state.startsWith('/') ? state : '/admin/dashboard')
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
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  border-radius: 16px;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  font-weight: 700;
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
