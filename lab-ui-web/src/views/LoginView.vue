<template>
  <main class="login-page">
    <section class="login-panel">
      <h1>OpenAtom Lab</h1>
      <p>实验室训练、签到与信誉分系统</p>
      <div class="toolbar" style="margin-top: 22px">
        <button class="button" @click="loginWithCms"><LogIn :size="17" />社团账号登录</button>
        <button class="button secondary" @click="loginDev"><UserRound :size="17" />开发登录</button>
      </div>
      <p v-if="error" class="status bad" style="margin-top: 18px">{{ error }}</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { LogIn, UserRound } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { ref } from 'vue'
import { devLogin, getOauthUrl } from '@/api/auth'
import { setSession } from '@/store/session'

const route = useRoute()
const router = useRouter()
const error = ref('')

async function loginWithCms() {
  error.value = ''
  try {
    const data = await getOauthUrl()
    sessionStorage.setItem('lab_oauth_state', data.state)
    window.location.href = data.authorizeUrl
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'OAuth 初始化失败'
  }
}

async function loginDev() {
  error.value = ''
  try {
    const data = await devLogin({
      clubUserId: 10001,
      username: 'lab-dev',
      nickname: '实验室开发账号',
      labRole: 2,
    })
    setSession(data.token, data.user)
    router.replace(String(route.query.redirect || '/'))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '开发登录失败'
  }
}
</script>
