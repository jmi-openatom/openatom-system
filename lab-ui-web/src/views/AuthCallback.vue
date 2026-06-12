<template>
  <main class="login-page">
    <section class="login-panel">
      <h1>认证中</h1>
      <p>{{ message }}</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { oauthCallback } from '@/api/auth'
import { setSession } from '@/store/session'

const route = useRoute()
const router = useRouter()
const message = ref('正在完成社团账号登录')

onMounted(async () => {
  const code = String(route.query.code || '')
  const state = String(route.query.state || '')
  const expected = sessionStorage.getItem('lab_oauth_state') || ''
  if (!code || !state || expected !== state) {
    message.value = 'OAuth 回调参数无效'
    return
  }
  try {
    const data = await oauthCallback(code, state)
    setSession(data.token, data.user)
    router.replace('/')
  } catch (err) {
    message.value = err instanceof Error ? err.message : '认证失败'
  }
})
</script>
