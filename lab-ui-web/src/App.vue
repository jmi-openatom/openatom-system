<template>
  <div class="app-shell">
    <div
      v-if="appStatus.navigationPending || appStatus.activeRequests"
      class="global-progress"
      role="progressbar"
    >
      <span />
    </div>
    <div
      v-if="
        !appStatus.online ||
        appStatus.navigationSlow ||
        appStatus.navigationFailed ||
        appStatus.requestSlow ||
        appStatus.requestMessage
      "
      class="network-notice"
      role="status"
    >
      <span>{{ networkMessage }}</span>
      <button
        v-if="appStatus.online && (appStatus.navigationSlow || appStatus.navigationFailed)"
        type="button"
        @click="retryNavigation"
      >
        重试
      </button>
    </div>
    <header v-if="showChrome" class="global-nav">
      <RouterLink class="brand" to="/">OpenAtom Lab</RouterLink>
      <nav class="nav-links">
        <RouterLink to="/problem"><Code2 :size="16" />每日一练</RouterLink>
        <RouterLink to="/checkin"><CalendarCheck :size="16" />签到</RouterLink>
        <RouterLink v-if="isAdminUser" to="/admin"><Settings :size="16" />后台</RouterLink>
      </nav>
      <button class="icon-button" title="退出登录" @click="logout"><LogOut :size="17" /></button>
    </header>
    <section v-if="showChrome" class="sub-nav">
      <span>{{ session.user?.nickname || session.user?.username || '实验室成员' }}</span>
      <span class="sub-nav__meta">信誉分 {{ session.user?.reputationScore ?? '-' }}</span>
    </section>
    <RouterView />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CalendarCheck, Code2, LogOut, Settings } from 'lucide-vue-next'
import { appStatus, startNavigation } from '@/appStatus'
import { clearSession, isAdmin, session } from '@/store/session'

const route = useRoute()
const router = useRouter()
const showChrome = computed(() => route.path !== '/login' && route.path !== '/auth/callback')
const isAdminUser = computed(() => isAdmin())
const networkMessage = computed(() => {
  if (!appStatus.online) return '网络已断开，当前页面仍可继续查看'
  if (appStatus.navigationFailed) return '页面加载失败，请重试'
  if (appStatus.navigationSlow) return '页面加载较慢，正在继续尝试'
  if (appStatus.requestMessage) return appStatus.requestMessage
  return '网络响应较慢，正在继续尝试'
})

function logout() {
  clearSession()
  router.replace('/login')
}

async function retryNavigation() {
  const target = appStatus.navigationTarget || route.fullPath
  if (appStatus.navigationSlow) {
    window.location.assign(target)
    return
  }
  const wasCurrentRoute = route.fullPath === target
  startNavigation(target)
  try {
    await router.replace(target)
    if (wasCurrentRoute) window.location.reload()
  } catch {
    window.location.assign(target)
  }
}
</script>

<style scoped>
.global-progress {
  position: fixed;
  inset: 0 0 auto;
  z-index: 5000;
  height: 3px;
  overflow: hidden;
  pointer-events: none;
}

.global-progress span {
  display: block;
  width: 42%;
  height: 100%;
  background: #111827;
  animation: lab-progress 1.1s ease-in-out infinite;
}

.network-notice {
  display: flex;
  position: fixed;
  top: 10px;
  left: 50%;
  z-index: 5001;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  color: #111827;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #d1d5db;
  border-radius: 999px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.14);
  font-size: 13px;
  transform: translateX(-50%);
}

.network-notice button {
  padding: 3px 9px;
  color: #fff;
  background: #111827;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
}

@keyframes lab-progress {
  from {
    transform: translateX(-110%);
  }
  to {
    transform: translateX(250%);
  }
}
</style>
