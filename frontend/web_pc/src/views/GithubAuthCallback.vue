<template>
  <ViewPage class="auth-callback-page">
    <div class="auth-callback-panel">
      <span class="auth-callback-logo">GH</span>
      <h1>正在完成 GitHub 授权</h1>
      <p>{{ message }}</p>
    </div>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { authApi } from '@/api'
import { setSession } from '@/utils/auth.ts'
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const message = ref('请稍候')

onMounted(async () => {
  const code = String(
    Array.isArray(route.query.code) ? route.query.code[0] || '' : route.query.code || '',
  )
  const state = String(
    Array.isArray(route.query.state) ? route.query.state[0] || '' : route.query.state || '',
  )
  if (!code || !state) {
    message.value = 'GitHub 回调参数不完整'
    return
  }
  try {
    const result = await authApi.githubCallback({ code, state })
    if (result.purpose === 'login' && result.login) setSession(result.login)
    window.location.replace(
      result.redirect || (result.purpose === 'bind' ? '/settings/account' : '/progress'),
    )
  } catch {
    message.value = 'GitHub 授权失败，请返回登录页重试'
  }
})
</script>

<style scoped>
.auth-callback-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
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
  color: #fff;
  background: #24292f;
  font-weight: 700;
}
.auth-callback-panel h1,
.auth-callback-panel p {
  margin: 0;
}
.auth-callback-panel p {
  color: var(--oa-muted);
}
</style>
