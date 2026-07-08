<template>
  <el-container class="admin-shell">
    <el-aside :class="{ 'is-collapsed': sidebarCollapsed }" :width="sidebarWidth" class="admin-aside">
      <el-button
        :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        :icon="sidebarCollapsed ? Expand : Fold"
        :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        circle
        class="admin-aside__collapse"
        @click="toggleSidebar"
      />
      <div class="admin-brand">
<!--        <span class="admin-brand__mark">OA</span>-->
        <div class="admin-brand__copy">
          <strong>JMI-OPENATOM</strong>
          <small>管理员后台</small>
        </div>
      </div>
      <el-menu
        router
        :default-active="$route.path"
        :collapse="sidebarCollapsed"
        :collapse-transition="false"
        class="admin-menu"
        background-color="transparent"
        text-color="var(--oa-muted)"
        active-text-color="var(--oa-active-text)"
      >
        <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div class="admin-aside__footer">
        <AdminMenuPreferences
          v-model="customVisibleMenuPaths"
          v-model:order-value="customMenuOrderPaths"
          :compact="sidebarCollapsed"
          :items="customizableMenus"
        />
        <span v-if="!sidebarCollapsed" class="version-tag">{{ version }}</span>
      </div>
    </el-aside>
    <el-container class="admin-content">
      <el-header class="admin-header">
        <div class="admin-header__title">
          <el-button class="admin-mobile-menu-btn" circle :icon="Menu" @click="mobileMenuVisible = true" />
          <div>
            <h1>{{ pageTitle }}</h1>
            <p>统一维护社团、用户、审批、面试与权限数据</p>
          </div>
        </div>
        <div class="admin-header__actions">
          <ThemeToggle />
          <el-button :icon="HomeFilled" @click="$router.push('/')">官网</el-button>
          <el-dropdown @command="handleCommand">
            <el-button type="primary">
              {{ displayUserName }}
              <el-icon class="el-icon--right">
                <ArrowDown />
              </el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view v-slot="{ Component, route }">
          <transition
            :css="false"
            mode="out-in"
            @before-enter="routeTransition.beforeEnter"
            @enter="routeTransition.enter"
            @leave="routeTransition.leave"
          >
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
    <el-drawer v-model="mobileMenuVisible" class="admin-mobile-drawer" direction="ltr" size="84%" title="管理后台">
      <div class="admin-mobile-drawer__tools">
        <AdminMenuPreferences
          v-model="customVisibleMenuPaths"
          v-model:order-value="customMenuOrderPaths"
          :items="customizableMenus"
          placement="bottom-start"
        />
      </div>
      <el-menu router :default-active="$route.path" class="admin-drawer-menu" @select="mobileMenuVisible = false">
        <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <span class="version-tag">{{ version }}</span>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Bell,
  Calendar,
  Box,
  ChatDotRound,
  Coin,
  Connection,
  DataAnalysis,
  DocumentChecked,
  EditPen,
  Expand,
  Files,
  FolderOpened,
  Fold,
  Grid,
  HomeFilled,
  List,
  Lock,
  Menu,
  MessageBox,
  Monitor,
  OfficeBuilding,
  Picture,
  Setting,
  Tickets,
  Trophy,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { authApi } from '@/api'
import { clearSession, getCurrentUser } from '@/utils/auth.ts'
import { hasAnyPermission } from '@/utils/permission.ts'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import AdminMenuPreferences from '@/components/admin/AdminMenuPreferences.vue'
import { useRouteTransition } from '@/composables/useRouteTransition'

const mobileMenuVisible = ref(false)

const user = ref<Record<string, unknown>>(getCurrentUser())

const version = ref(__APP_VERSION__)

const displayUserName = computed(() => {
  return String(user.value.realName || user.value.username || '管理员')
})

interface AdminMenuItem {
  path: string
  label: string
  icon: unknown
  permissions: string[]
  pinned?: boolean
}

const menus = ref<AdminMenuItem[]>([
  // ==== 1. 数据概览 ====
  { path: '/admin/dashboard', label: '数据概览', icon: DataAnalysis, permissions: [], pinned: true },

  // ==== 2. 日常最高频：用户与核心业务（活动、签到、请假） ====
  { path: '/admin/users', label: '用户管理', icon: User, permissions: ['user:list'] },
  { path: '/admin/memberships', label: '成员管理', icon: UserFilled, permissions: ['membership:list'] },
  { path: '/admin/alumni-managers', label: '往届管理人员', icon: User, permissions: ['membership:list'] },
  { path: '/admin/alumni-groups', label: '往届分组管理', icon: Files, permissions: ['membership:list'] },
  { path: '/admin/activities', label: '活动管理', icon: Calendar, permissions: ['activity:list'] },
  { path: '/admin/ai-activities', label: 'AI活动自动化', icon: Calendar, permissions: ['activity:create'] },
  { path: '/admin/check-ins', label: '扫码签到', icon: Tickets, permissions: ['check-in:list'] },
  { path: '/admin/leaves', label: '请假审批', icon: DocumentChecked, permissions: ['leave-application:list'] },

  // ==== 3. 表单与数据收集（高频） ====
  { path: '/admin/site-forms', label: '表单管理', icon: DocumentChecked, permissions: ['site-form:list'] },
  { path: '/admin/form-submissions', label: '表单记录', icon: Tickets, permissions: ['site-form:detail'] },

  // ==== 4. 社团基础与招新链路 ====
  { path: '/admin/clubs', label: '社团管理', icon: OfficeBuilding, permissions: ['club:list'] },
  { path: '/admin/positions', label: '岗位管理', icon: List, permissions: ['position:list'] },
  { path: '/admin/departments', label: '部门管理', icon: Grid, permissions: ['department:list'] },
  {
    path: '/admin/activation-settings',
    label: '激活页设置',
    icon: Setting,
    permissions: ['club:update', 'department:update'],
  },
  {
    path: '/admin/recruitment-campaigns',
    label: '招新计划',
    icon: DocumentChecked,
    permissions: ['recruitment-campaign:list'],
  },
  { path: '/admin/applications', label: '入会申请', icon: DocumentChecked, permissions: ['application:list'] },
  { path: '/admin/interviews', label: '面试管理', icon: MessageBox, permissions: ['interview:list'] },

  // ==== 5. 互动营销与运营工具 ====
  { path: '/admin/votes', label: '投票管理', icon: Tickets, permissions: ['vote:list'] },
  { path: '/admin/lotteries', label: '抽奖系统', icon: Trophy, permissions: ['lottery:list'] },
  {
    path: '/admin/points',
    label: '积分兑换',
    icon: Coin,
    permissions: ['point:account:list', 'point:item:list', 'point:redemption:list'],
  },
  { path: '/admin/awards', label: '获奖经历', icon: Trophy, permissions: ['award:list'] },

  // ==== 6. 内容与媒体中心 ====
  { path: '/admin/blogs', label: '博客管理', icon: EditPen, permissions: ['blog:list'] },
  {
    path: '/admin/regulations',
    label: '规章制度',
    icon: DocumentChecked,
    permissions: ['regulation:list'],
  },
  { path: '/admin/office-documents', label: '文书中心', icon: DocumentChecked, permissions: ['document:list'] },
  { path: '/admin/images', label: '图床管理', icon: Picture, permissions: ['image:list'] },

  // ==== 7. 公共通知与基础配置 ====
  { path: '/admin/school-calendar', label: '校历设置', icon: Calendar, permissions: ['school-calendar:manage'] },
  { path: '/admin/notifications', label: '通知管理', icon: Bell, permissions: ['notification:list'] },
  { path: '/admin/qr-center', label: '二维码中心', icon: Monitor, permissions: [] },

  // ==== 8. 后台管理、权限与安全配置（低频操作下沉） ====
  { path: '/admin/roles', label: '角色权限', icon: Lock, permissions: ['role:list', 'permission:list'] },
  { path: '/admin/oauth-clients', label: '认证应用', icon: Lock, permissions: ['oauth-client:list'] },
  { path: '/admin/showcase-apps', label: '应用展示', icon: Box, permissions: ['showcase-app:list'] },
  { path: '/admin/data-open', label: '开放平台', icon: Connection, permissions: ['data-open:list'] },
  { path: '/admin/bot-groups', label: 'QQ机器人', icon: ChatDotRound, permissions: ['bot-management:list'] },
  { path: '/admin/logs', label: '系统日志', icon: List, permissions: ['log:operation:list', 'log:login:list'] },
  { path: '/admin/file-migration', label: '文件迁移', icon: FolderOpened, permissions: ['file:migration:stats'] },

  // ==== 9. 出口 ====
  { path: '/', label: '返回官网', icon: Connection, permissions: [], pinned: true },
])

const router = useRouter()

const route = useRoute()

const routeTransition = useRouteTransition({
  enterY: 16,
  leaveY: -8,
  enterDuration: 280,
  leaveDuration: 140,
})

const sidebarPreferenceKey = `oa:admin-sidebar:${String(user.value.id || user.value.username || 'default')}`

const storedSidebarPreferences = readSidebarPreferences()
const sidebarCollapsed = ref(storedSidebarPreferences.collapsed)
const hiddenMenuPaths = ref(storedSidebarPreferences.hiddenMenuPaths)
const menuOrderPaths = ref(storedSidebarPreferences.menuOrderPaths)

const sidebarWidth = computed(() => (sidebarCollapsed.value ? '76px' : '248px'))

const authorizedMenus = computed(() => {
  return menus.value.filter((item) => item.path === '/' || hasAnyPermission(item.permissions || []))
})

const customizableMenus = computed(() => authorizedMenus.value.filter((item) => !item.pinned))

const orderedCustomizableMenus = computed(() => {
  const itemMap = new Map(customizableMenus.value.map((item) => [item.path, item]))
  const orderedItems = menuOrderPaths.value.map((path) => itemMap.get(path)).filter(Boolean) as AdminMenuItem[]
  const orderedPaths = new Set(orderedItems.map((item) => item.path))
  return [...orderedItems, ...customizableMenus.value.filter((item) => !orderedPaths.has(item.path))]
})

const customMenuOrderPaths = computed({
  get() {
    return orderedCustomizableMenus.value.map((item) => item.path)
  },
  set(paths: string[]) {
    const allowedPaths = new Set(customizableMenus.value.map((item) => item.path))
    const normalizedPaths = paths.filter((path, index) => allowedPaths.has(path) && paths.indexOf(path) === index)
    const includedPaths = new Set(normalizedPaths)
    menuOrderPaths.value = [
      ...normalizedPaths,
      ...customizableMenus.value.filter((item) => !includedPaths.has(item.path)).map((item) => item.path),
    ]
    persistSidebarPreferences()
  },
})

const customVisibleMenuPaths = computed({
  get() {
    const hiddenPaths = new Set(hiddenMenuPaths.value)
    return customizableMenus.value.filter((item) => !hiddenPaths.has(item.path)).map((item) => item.path)
  },
  set(paths: string[]) {
    const visiblePaths = new Set(paths)
    hiddenMenuPaths.value = customizableMenus.value
      .filter((item) => !visiblePaths.has(item.path))
      .map((item) => item.path)
    persistSidebarPreferences()
  },
})

const visibleMenus = computed(() => {
  const hiddenPaths = new Set(hiddenMenuPaths.value)
  const dashboardMenu = authorizedMenus.value.filter((item) => item.path === '/admin/dashboard')
  const websiteMenu = authorizedMenus.value.filter((item) => item.path === '/')
  const customMenus = orderedCustomizableMenus.value.filter((item) => !hiddenPaths.has(item.path))
  return [...dashboardMenu, ...customMenus, ...websiteMenu]
})

const pageTitle = computed(() => {
  const current = menus.value.find((item) => item.path === route.path)
  return current ? current.label : '管理后台'
})

function readSidebarPreferences() {
  const fallback = {
    collapsed: false,
    hiddenMenuPaths: [] as string[],
    menuOrderPaths: [] as string[],
  }
  if (typeof window === 'undefined') return fallback

  try {
    const storedValue = window.localStorage.getItem(sidebarPreferenceKey)
    if (!storedValue) return fallback
    const parsed = JSON.parse(storedValue)
    return {
      collapsed: parsed?.collapsed === true,
      hiddenMenuPaths: Array.isArray(parsed?.hiddenMenuPaths)
        ? parsed.hiddenMenuPaths.map((item: unknown) => String(item))
        : [],
      menuOrderPaths: Array.isArray(parsed?.menuOrderPaths)
        ? parsed.menuOrderPaths.map((item: unknown) => String(item))
        : [],
    }
  } catch {
    return fallback
  }
}

function persistSidebarPreferences() {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(
      sidebarPreferenceKey,
      JSON.stringify({
        collapsed: sidebarCollapsed.value,
        hiddenMenuPaths: hiddenMenuPaths.value,
        menuOrderPaths: menuOrderPaths.value,
      }),
    )
  } catch {
    // 本地存储不可用时仍保留当前会话内的交互状态。
  }
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  persistSidebarPreferences()
}

async function handleCommand(command: string) {
  if (command !== 'logout') return
  try {
    await authApi.logout()
  } finally {
    clearSession()
    router.replace('/login')
  }
}

onBeforeUnmount(() => {
  routeTransition.kill()
})
</script>

<style scoped>
.admin-shell {
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
}

.admin-content {
  min-width: 0;
  height: 100vh;
  overflow: hidden;
}

.admin-aside {
  display: flex;
  position: sticky;
  z-index: 3;
  top: 0;
  align-self: flex-start;
  flex: 0 0 auto;
  height: 100vh;
  flex-direction: column;
  margin: 0;
  border: 0;
  border-right: 1px solid var(--oa-border);
  border-radius: 0;
  background: var(--oa-elevated-bg);
  box-shadow: none;
  backdrop-filter: none;
  animation: adminRailIn 0.42s ease both;
  transition: width 0.22s ease;
}

.admin-aside__collapse {
  position: absolute;
  z-index: 2;
  top: 24px;
  right: -14px;
  width: 28px;
  min-height: 28px;
  height: 28px;
  padding: 0;
  background: var(--oa-elevated-bg);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.16);
}

.admin-brand {
  display: flex;
  min-height: 76px;
  align-items: center;
  gap: 12px;
  padding: 16px 18px 14px;
  color: var(--oa-text);
  border-bottom: 1px solid var(--oa-divider);
}

.admin-brand__copy {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  transition:
    width 0.18s ease,
    opacity 0.14s ease;
}

.admin-aside.is-collapsed .admin-brand {
  justify-content: center;
  padding-right: 10px;
  padding-left: 10px;
}

.admin-aside.is-collapsed .admin-brand__copy {
  width: 0;
  opacity: 0;
}

.admin-brand__mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  border: 1px solid var(--oa-active-bg);
  border-radius: 10px;
  font-weight: 600;
  font-size: 14px;
  flex: 0 0 auto;
}

.admin-brand strong {
  display: block;
  color: var(--oa-text);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: 0;
}

.admin-brand small {
  display: block;
  margin-top: 5px;
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1;
}

.admin-menu {
  flex: 1 1 auto;
  min-height: 0;
  padding: 12px 10px;
  overflow-y: auto;
  border-right: 0;
  scrollbar-width: none;
}

.admin-menu.el-menu--collapse {
  width: 100%;
  padding-right: 8px;
  padding-left: 8px;
}

.admin-menu::-webkit-scrollbar {
  display: none;
}

.admin-header {
  display: flex;
  flex-shrink: 0;
  height: 52px;
  align-items: center;
  justify-content: space-between;
  margin: 0;
  padding: 0 22px;
  background: var(--oa-admin-header-bg);
  border: 0;
  border-bottom: 1px solid var(--oa-header-border);
  border-radius: 0;
  box-shadow: none;
  backdrop-filter: none;
  animation: adminTopIn 0.36s ease both;
}

.admin-header__title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.admin-mobile-menu-btn {
  display: none;
  flex: 0 0 auto;
}

.admin-header h1 {
  margin: 0;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0;
}

.admin-header p {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 12px;
}

.admin-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-main {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  padding: 24px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: var(--oa-page-soft-bg);
}

.admin-menu :deep(.el-menu-item) {
  height: 42px;
  padding: 0 12px !important;
  margin: 2px 0;
  line-height: 42px;
  color: var(--oa-muted);
  border-radius: 12px;
  font-size: 14px;
  letter-spacing: 0;
  transition:
    color 0.16s ease,
    background-color 0.16s ease,
    transform 0.16s ease;
}

.admin-menu :deep(.el-menu-item .el-icon) {
  width: 20px;
  margin-right: 12px;
  color: currentColor;
  font-size: 18px;
}

.admin-menu :deep(.el-menu-item span) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-menu.el-menu--collapse :deep(.el-menu-item) {
  justify-content: center;
  padding: 0 !important;
}

.admin-menu.el-menu--collapse :deep(.el-menu-item .el-icon) {
  margin-right: 0;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: var(--oa-nav-hover-bg);
  color: var(--oa-text);
  transform: translateX(2px);
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
  font-weight: 500;
}

.admin-menu :deep(.el-menu-item.is-active:hover) {
  background: var(--oa-active-hover-bg);
  color: var(--oa-active-text);
}

@keyframes adminRailIn {
  from {
    opacity: 0;
    transform: translateX(-14px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes adminTopIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.admin-aside__footer {
  display: flex;
  margin-top: auto;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  padding: 14px 16px 18px;
  border-top: 1px solid var(--oa-divider);
}

.admin-aside.is-collapsed .admin-aside__footer {
  padding-right: 14px;
  padding-left: 14px;
}

.admin-mobile-drawer__tools {
  margin-bottom: 14px;
}

.admin-drawer-menu {
  margin-bottom: 16px;
  border-right: 0;
}

.admin-drawer-menu :deep(.el-menu-item) {
  height: 48px;
  margin-bottom: 8px;
  border-radius: 18px;
}

.version-tag {
  background: var(--oa-page-soft-bg);
  color: var(--oa-muted);
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid var(--oa-border);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1;
}

@media (max-width: 900px) {
  .admin-shell {
    display: block;
    height: auto;
    overflow: visible;
  }

  .admin-content {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .admin-header {
    margin-left: 0;
    margin-right: 0;
  }

  .admin-aside {
    display: none;
  }

  .admin-header {
    height: auto;
    min-height: 66px;
    flex-wrap: nowrap;
    gap: 12px;
    padding: 14px;
    border-radius: 0;
  }

  .admin-header__title {
    flex: 1 1 auto;
  }

  .admin-mobile-menu-btn {
    display: inline-flex;
  }

  .admin-header h1 {
    font-size: 18px;
  }

  .admin-header p {
    display: none;
  }

  .admin-header__actions {
    flex: 0 0 auto;
    justify-content: flex-end;
  }

  .admin-main {
    padding: 12px;
    overflow-x: hidden;
    overflow-y: auto;
  }
}

@media (max-width: 560px) {
  .admin-header {
    margin: 0;
    padding: 10px;
  }

  .admin-header__actions {
    gap: 6px;
  }

  .admin-header__actions :deep(.el-button span) {
    display: none;
  }

  .admin-header__actions :deep(.el-button) {
    width: 40px;
    padding: 8px;
  }

  .admin-header__actions :deep(.el-button + .el-button) {
    margin-left: 0;
  }

  .admin-main {
    padding: 10px;
  }
}
</style>
