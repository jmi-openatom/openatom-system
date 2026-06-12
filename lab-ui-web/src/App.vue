<template>
  <div class="app-shell">
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
import { clearSession, isAdmin, session } from '@/store/session'

const route = useRoute()
const router = useRouter()
const showChrome = computed(() => route.path !== '/login' && route.path !== '/auth/callback')
const isAdminUser = computed(() => isAdmin())

function logout() {
  clearSession()
  router.replace('/login')
}
</script>
