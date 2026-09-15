<template>
  <div class="quest-shell">
    <header class="quest-header">
      <div class="quest-header__inner">
        <router-link class="quest-brand" to="/dashboard" aria-label="OpenAtom Quest 首页">
          <span class=""><img style="width: 45px;margin-top: 5px;" src="/public/logo.png" alt=""></span>
          <span>
            <strong>JMI-OPENATOM-QUEST</strong>
            <small>成员成长与任务实践</small>
          </span>
        </router-link>

        <nav class="quest-nav" aria-label="主导航">
          <router-link to="/dashboard">工作台</router-link>
          <router-link to="/routes">成长路线</router-link>
          <router-link to="/tasks">任务中心</router-link>
          <router-link to="/notifications">消息</router-link>
          <router-link v-if="canReview" to="/reviews">审核</router-link>
          <router-link v-if="canManageTasks && !isAdmin" to="/task-management">任务管理</router-link>
          <router-link v-if="isAdmin" to="/admin">管理</router-link>
        </nav>

        <div class="quest-header__actions">
          <button class="icon-button" type="button" aria-label="切换主题" @click="toggleTheme">
            <Moon v-if="!dark" />
            <Sunny v-else />
          </button>
          <el-dropdown @command="handleAccountCommand">
            <span class="avatar">{{ avatarText }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="assignments">我的任务</el-dropdown-item>
                <el-dropdown-item command="points">积分与等级</el-dropdown-item>
                <el-dropdown-item v-if="canReview" command="reviews">审核工作台</el-dropdown-item>
                <el-dropdown-item v-if="canManageTasks && !isAdmin" command="task-management">任务管理</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin">管理后台</el-dropdown-item>
                <el-dropdown-item divided command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="quest-main">
      <router-view />
    </main>
    <nav class="quest-mobile-nav" aria-label="移动端主导航">
      <router-link to="/dashboard"><House /><span>工作台</span></router-link>
      <router-link to="/routes"><Guide /><span>路线</span></router-link>
      <router-link to="/tasks"><List /><span>任务</span></router-link>
      <router-link to="/notifications"><Bell /><span>消息</span></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { Bell, Guide, House, List, Moon, Sunny } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const dark = ref(false)
const auth = useAuthStore()
const router = useRouter()
const avatarText = computed(() => auth.member?.nickname?.trim().slice(0, 1) || '新')
const canReview = computed(() => auth.member?.permissions.includes('submission:review'))
const canManageTasks = computed(() => auth.member?.permissions.includes('task:manage'))
const isAdmin = computed(() => auth.member?.permissions.includes('stats:global'))

function applyTheme() {
  document.documentElement.classList.toggle('dark', dark.value)
  document.documentElement.dataset.theme = dark.value ? 'dark' : 'light'
  localStorage.setItem('quest-theme', dark.value ? 'dark' : 'light')
}

function toggleTheme() {
  dark.value = !dark.value
  applyTheme()
}

function handleAccountCommand(command: string) {
  if (command === 'logout') void auth.logout()
  else if (command === 'assignments') void router.push('/assignments')
  else if (command === 'points') void router.push('/points')
  else if (command === 'reviews') void router.push('/reviews')
  else if (command === 'task-management') void router.push('/task-management')
  else if (command === 'admin') void router.push('/admin')
  else if (command === 'profile') void router.push('/profile/setup')
}

onMounted(() => {
  const stored = localStorage.getItem('quest-theme')
  dark.value = stored ? stored === 'dark' : matchMedia('(prefers-color-scheme: dark)').matches
  applyTheme()
})
</script>
