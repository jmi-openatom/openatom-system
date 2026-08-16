<template>
  <div class="mail-shell admin-shell">
    <header class="app-header">
      <div class="app-brand">
        <img alt="开放原子邮箱" class="brand-logo" src="/logo.png" />
        <div><strong>邮箱后台管理</strong><small>{{ session.address }}</small></div>
      </div>
      <div class="header-actions">
        <ThemeToggle />
        <button class="secondary-button" type="button" @click="backToMail">返回邮箱</button>
        <button class="icon-button" type="button" aria-label="退出登录" @click="emit('logout')"><LogOut :size="18" /></button>
      </div>
    </header>

    <div class="admin-body">
      <section class="admin-stats" aria-label="统计概览">
        <div class="stat-card"><span>总邮箱</span><strong>{{ stats?.total ?? '—' }}</strong></div>
        <div class="stat-card"><span>已激活</span><strong>{{ stats?.active ?? '—' }}</strong></div>
        <div class="stat-card"><span>Resend 域名</span><strong>{{ resendDomain }}</strong><small>{{ resendStatus }}</small></div>
      </section>

      <section class="admin-table-wrap" aria-label="用户邮箱列表">
        <header class="admin-table-header">
          <h2>用户邮箱</h2>
          <button class="secondary-button" type="button" :disabled="loading" @click="load">刷新</button>
        </header>
        <div v-if="loading" class="email-skeletons">
          <div v-for="i in 5" :key="i" class="email-skeleton"><i></i><span></span><b></b></div>
        </div>
        <div v-else-if="error" class="empty-state" role="alert">
          <CircleAlert :size="30" /><h2>无法加载</h2><p>{{ error }}</p>
          <button class="secondary-button" type="button" @click="load">重新加载</button>
        </div>
        <table v-else class="admin-table">
          <thead><tr><th>ID</th><th>用户</th><th>地址</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="mailbox in mailboxes" :key="mailbox.id">
              <td>{{ mailbox.id }}</td>
              <td>{{ mailbox.displayName || mailbox.sub }}</td>
              <td class="admin-address">{{ mailbox.address || '（未分配）' }}</td>
              <td><span :class="'status-badge status-badge--' + mailbox.status.toLowerCase()">{{ mailbox.status }}</span></td>
              <td>
                <button
                  class="secondary-button"
                  type="button"
                  :disabled="busyId === mailbox.id"
                  @click="toggleSuspend(mailbox)"
                >
                  {{ mailbox.status === 'SUSPENDED' ? '启用' : '停用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { CircleAlert, LogOut } from 'lucide-vue-next'
import {
  loadAdminMailboxes, loadAdminStats, setMailboxSuspended,
  type AdminMailboxView, type AdminStats,
} from '../api'
import type { SessionView } from '../models'
import ThemeToggle from '../components/common/ThemeToggle.vue'
import { useUiStore } from '../stores/ui'

defineProps<{ session: SessionView }>()
const emit = defineEmits<{ (e: 'logout'): void; (e: 'back'): void }>()

const { showToast } = useUiStore()
const mailboxes = ref<AdminMailboxView[]>([])
const stats = ref<AdminStats | null>(null)
const loading = ref(false)
const busyId = ref<number | null>(null)
const error = ref('')

const resendDomain = computed(() => {
  const r = stats.value?.resend
  return r?.configured && r.domain ? r.domain : '未配置'
})
const resendStatus = computed(() => {
  const r = stats.value?.resend
  if (!r?.configured) return ''
  if (r.error) return r.error
  return r.verified ? '已验证' : '未验证'
})

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [boxes, stat] = await Promise.all([loadAdminMailboxes(), loadAdminStats()])
    mailboxes.value = boxes
    stats.value = stat
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function toggleSuspend(mailbox: AdminMailboxView) {
  busyId.value = mailbox.id
  try {
    const target = mailbox.status !== 'SUSPENDED'
    await setMailboxSuspended(mailbox.id, target)
    showToast(target ? '已停用' : '已启用')
    await load()
  } catch (err) {
    showToast(err instanceof Error ? err.message : '操作失败')
  } finally {
    busyId.value = null
  }
}

function backToMail() { emit('back') }
</script>