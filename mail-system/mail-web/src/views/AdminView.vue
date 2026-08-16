<template>
  <div class="mail-shell admin-shell">
    <header class="admin-header">
      <div class="admin-header__left">
        <img alt="开放原子邮箱" class="brand-logo" src="/logo.png" />
        <strong>邮箱后台管理</strong>
        <small>{{ session.address }}</small>
      </div>
      <div class="admin-header__right">
        <ThemeToggle />
        <button class="secondary-button" type="button" @click="emit('back')">返回邮箱</button>
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
          <h2>用户邮箱 <small v-if="pageData">共 {{ pageData.total }} 个</small></h2>
          <div class="admin-toolbar">
            <label class="admin-search">
              <Search :size="15" aria-hidden="true" />
              <input v-model="keyword" placeholder="搜索姓名/地址/ID" @input="onSearchInput" />
            </label>
            <button class="secondary-button" type="button" :disabled="loading" @click="load">刷新</button>
          </div>
        </header>
        <div v-if="loading" class="email-skeletons">
          <div v-for="i in 5" :key="i" class="email-skeleton"><i></i><span></span><b></b></div>
        </div>
        <div v-else-if="error" class="empty-state" role="alert">
          <CircleAlert :size="30" /><h2>无法加载</h2><p>{{ error }}</p>
          <button class="secondary-button" type="button" @click="load">重新加载</button>
        </div>
        <table v-else class="admin-table">
          <thead>
            <tr>
              <th><button class="sort-btn" type="button" @click="setSort('id')">ID {{ sortIcon('id') }}</button></th>
              <th><button class="sort-btn" type="button" @click="setSort('displayName')">用户 {{ sortIcon('displayName') }}</button></th>
              <th><button class="sort-btn" type="button" @click="setSort('address')">地址 {{ sortIcon('address') }}</button></th>
              <th><button class="sort-btn" type="button" @click="setSort('status')">状态 {{ sortIcon('status') }}</button></th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="mailbox in rows" :key="mailbox.id">
              <td>{{ mailbox.id }}</td>
              <td>{{ mailbox.displayName || mailbox.sub }}</td>
              <td class="admin-address">{{ mailbox.address || '（未分配）' }}</td>
              <td><span :class="'status-badge status-badge--' + mailbox.status.toLowerCase()">{{ mailbox.status }}</span></td>
              <td>
                <button class="secondary-button" type="button" :disabled="busyId === mailbox.id" @click="toggleSuspend(mailbox)">
                  {{ mailbox.status === 'SUSPENDED' ? '启用' : '停用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <footer v-if="pageData && pageData.total > 0" class="admin-pagination">
          <span>第 {{ page }} / {{ totalPages }} 页 · {{ pageData.total }} 条</span>
          <div class="admin-pagination__btns">
            <button class="secondary-button" type="button" :disabled="page <= 1 || loading" @click="goPage(page - 1)">上一页</button>
            <button class="secondary-button" type="button" :disabled="page >= totalPages || loading" @click="goPage(page + 1)">下一页</button>
          </div>
        </footer>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { CircleAlert, LogOut, Search } from 'lucide-vue-next'
import {
  loadAdminMailboxes, loadAdminStats, setMailboxSuspended,
  type AdminMailboxPage, type AdminMailboxView, type AdminStats,
} from '../api'
import type { SessionView } from '../models'
import ThemeToggle from '../components/common/ThemeToggle.vue'
import { useUiStore } from '../stores/ui'

defineProps<{ session: SessionView }>()
const emit = defineEmits<{ (e: 'logout'): void; (e: 'back'): void }>()

const { showToast } = useUiStore()
const rows = ref<AdminMailboxView[]>([])
const pageData = ref<AdminMailboxPage | null>(null)
const stats = ref<AdminStats | null>(null)
const loading = ref(false)
const busyId = ref<number | null>(null)
const error = ref('')
const page = ref(1)
const pageSize = 20
const keyword = ref('')
const sort = ref('id')
const order = ref<'asc' | 'desc'>('desc')
let searchTimer: number | undefined

const totalPages = computed(() => {
  const total = pageData.value?.total ?? 0
  return Math.max(1, Math.ceil(total / pageSize))
})

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

onMounted(() => void load())

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [boxes, stat] = await Promise.all([
      loadAdminMailboxes({ page: page.value, pageSize, keyword: keyword.value, sort: sort.value, order: order.value }),
      loadAdminStats(),
    ])
    rows.value = boxes.rows
    pageData.value = boxes
    stats.value = stat
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function onSearchInput() {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    page.value = 1
    void load()
  }, 350)
}

function setSort(field: string) {
  if (sort.value === field) {
    order.value = order.value === 'asc' ? 'desc' : 'asc'
  } else {
    sort.value = field
    order.value = 'asc'
  }
  void load()
}

function sortIcon(field: string): string {
  if (sort.value !== field) return '↕'
  return order.value === 'asc' ? '↑' : '↓'
}

function goPage(target: number) {
  if (target < 1 || target > totalPages.value) return
  page.value = target
  void load()
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
</script>
