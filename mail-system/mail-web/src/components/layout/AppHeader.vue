<template>
  <header class="app-header">
    <div class="app-brand">
      <img alt="开放原子邮箱" class="brand-logo" src="/logo.png"/>
      <div><strong>JMI-OPENATOM 邮箱系统</strong><small>{{ session.address }}</small></div>
    </div>
    <label class="search-field">
      <Search :size="17" aria-hidden="true"/>
      <span class="sr-only">搜索邮件</span>
      <input :value="search" placeholder="搜索主题、发件人或正文" type="search" @input="onSearchInput"/>
      <kbd>⌘ K</kbd>
    </label>
    <div class="header-actions">
      <button v-if="isAdmin" aria-label="后台管理" class="secondary-button admin-entry" type="button"
              @click="emit('admin')">
        <Settings :size="16"/>
        后台管理
      </button>
      <button :disabled="mailLoading" aria-label="刷新邮件" class="icon-button" type="button" @click="onRefresh">
        <RefreshCw :class="{ rotating: mailLoading }" :size="18"/>
      </button>
      <ThemeToggle/>
      <button :aria-expanded="accountMenu" class="account-button" type="button" @click="accountMenu = !accountMenu">
        <span class="avatar">{{ avatarText }}</span>
        <span class="account-copy"><strong>{{ session.displayName || '开放原子成员' }}</strong><small>{{
            session.status
          }}</small></span>
        <ChevronDown :size="16"/>
      </button>
      <div v-if="accountMenu" class="account-menu">
        <a href="https://www.jmi-openatom.cn/workspace">
          <LayoutGrid :size="16"/>
          返回主站工作台</a>
        <button type="button" @click="onLogout">
          <LogOut :size="16"/>
          退出登录
        </button>
      </div>
    </div>
  </header>
</template>

<script lang="ts" setup>
import {computed, ref} from 'vue'
import {ChevronDown, LayoutGrid, LogOut, RefreshCw, Search, Settings} from 'lucide-vue-next'
import type {SessionView} from '../../models'
import ThemeToggle from '../common/ThemeToggle.vue'

const props = defineProps<{
  session: SessionView
  search: string
  mailLoading: boolean
  isAdmin: boolean
}>()
const emit = defineEmits<{
  (e: 'update:search', value: string): void
  (e: 'refresh'): void
  (e: 'logout'): void
  (e: 'admin'): void
}>()

const accountMenu = ref(false)
const avatarText = computed(() => (props.session.displayName || 'OA').trim().slice(-2))

function onSearchInput(event: Event) {
  emit('update:search', (event.target as HTMLInputElement).value)
}

function onRefresh() {
  emit('refresh')
}

function onLogout() {
  emit('logout')
}
</script>