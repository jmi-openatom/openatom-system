<template>
  <el-container class="admin-shell">
    <el-aside class="admin-aside" width="248px">
      <div class="admin-brand">
        <span class="admin-brand__mark">OA</span>
        <div>
          <strong>OpenAtom</strong>
          <small>管理员后台</small>
        </div>
      </div>
      <el-menu
        router
        :default-active="$route.path"
        class="admin-menu"
        background-color="transparent"
        text-color="#333333"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div class="admin-aside__footer">
        <span class="version-tag">{{ version }}</span>
      </div>
    </el-aside>
    <el-container class="admin-content">
      <el-header class="admin-header">
        <div class="admin-header__title">
          <el-button
            class="admin-mobile-menu-btn"
            circle
            :icon="Menu"
            @click="mobileMenuVisible = true"
          />
          <div>
            <h1>{{ pageTitle }}</h1>
            <p>统一维护社团、用户、审批、面试与权限数据</p>
          </div>
        </div>
        <div class="admin-header__actions">
          <el-button :icon="HomeFilled" @click="$router.push('/')">官网</el-button>
          <el-dropdown @command="handleCommand">
            <el-button type="primary">
              {{ user.realName || user.username || '管理员' }}
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
        <router-view />
      </el-main>
    </el-container>
    <el-drawer
      v-model="mobileMenuVisible"
      class="admin-mobile-drawer"
      direction="ltr"
      size="84%"
      title="管理后台"
    >
      <el-menu
        router
        :default-active="$route.path"
        class="admin-drawer-menu"
        @select="mobileMenuVisible = false"
      >
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Bell,
  Calendar,
  Connection,
  DataAnalysis,
  DocumentChecked,
  HomeFilled,
  List,
  Lock,
  Menu,
  MessageBox,
  OfficeBuilding,
  Tickets,
  Trophy,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { authApi } from '@/api'
import { clearSession, getCurrentUser } from '@/utils/auth.ts'
import { hasAnyPermission } from '@/utils/permission.ts'

const mobileMenuVisible = ref(false)

const user = ref(getCurrentUser())

const version = ref(__APP_VERSION__)

const menus = ref([
  { path: '/admin/dashboard', label: '数据概览', icon: DataAnalysis, permissions: [] },
  { path: '/admin/users', label: '用户管理', icon: User, permissions: ['user:list'] },
  {
    path: '/admin/clubs',
    label: '社团管理',
    icon: OfficeBuilding,
    permissions: ['club:list'],
  },
  { path: '/admin/positions', label: '岗位管理', icon: List, permissions: ['position:list'] },
  {
    path: '/admin/recruitment-campaigns',
    label: '招新计划',
    icon: DocumentChecked,
    permissions: ['recruitment-campaign:list'],
  },
  {
    path: '/admin/site-forms',
    label: '表单管理',
    icon: DocumentChecked,
    permissions: ['site-form:list'],
  },
  {
    path: '/admin/form-submissions',
    label: '表单记录',
    icon: Tickets,
    permissions: ['site-form:detail'],
  },
  {
    path: '/admin/office-documents',
    label: '文书中心',
    icon: DocumentChecked,
    permissions: ['document:list'],
  },
  {
    path: '/admin/leaves',
    label: '请假审批',
    icon: DocumentChecked,
    permissions: ['leave-application:list'],
  },
  {
    path: '/admin/school-calendar',
    label: '校历设置',
    icon: Calendar,
    permissions: ['school-calendar:manage'],
  },
  {
    path: '/admin/activities',
    label: '活动管理',
    icon: Calendar,
    permissions: ['activity:list'],
  },
  {
    path: '/admin/check-ins',
    label: '扫码签到',
    icon: Tickets,
    permissions: ['check-in:list'],
  },
  { path: '/admin/awards', label: '获奖经历', icon: Trophy, permissions: ['award:list'] },
  {
    path: '/admin/applications',
    label: '入会申请',
    icon: DocumentChecked,
    permissions: ['application:list'],
  },
  {
    path: '/admin/interviews',
    label: '面试管理',
    icon: MessageBox,
    permissions: ['interview:list'],
  },
  {
    path: '/admin/memberships',
    label: '成员管理',
    icon: UserFilled,
    permissions: ['membership:list'],
  },
  {
    path: '/admin/roles',
    label: '角色权限',
    icon: Lock,
    permissions: ['role:list', 'permission:list'],
  },
  {
    path: '/admin/logs',
    label: '系统日志',
    icon: List,
    permissions: ['log:operation:list', 'log:login:list'],
  },
  {
    path: '/admin/notifications',
    label: '通知管理',
    icon: Bell,
    permissions: ['notification:list'],
  },
  { path: '/', label: '返回官网', icon: Connection, permissions: [] },
])

const router = useRouter()

const route = useRoute()

const visibleMenus = computed(() => {
  return menus.value.filter((item) => item.path === '/' || hasAnyPermission(item.permissions || []))
})

const pageTitle = computed(() => {
  const current = menus.value.find((item) => item.path === route.path)
  return current ? current.label : '管理后台'
})

async function handleCommand(command: string) {
  if (command !== 'logout') return
  try {
    await authApi.logout()
  } finally {
    clearSession()
    router.replace('/admin/login')
  }
}
</script>

<style scoped>
.admin-shell {
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  background: #f5f5f7;
}

.admin-content {
  min-width: 0;
  height: 100vh;
  overflow: hidden;
}

.admin-aside {
  display: flex;
  position: sticky;
  top: 0;
  align-self: flex-start;
  flex: 0 0 248px;
  height: 100vh;
  flex-direction: column;
  margin: 0;
  border: 0;
  border-right: 1px solid #e0e0e0;
  border-radius: 0;
  background: #ffffff;
  box-shadow: none;
  backdrop-filter: none;
  animation: adminRailIn 0.42s ease both;
}

.admin-brand {
  display: flex;
  min-height: 76px;
  align-items: center;
  gap: 12px;
  padding: 16px 18px 14px;
  color: #1d1d1f;
  border-bottom: 1px solid #f0f0f0;
}

.admin-brand__mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  color: #ffffff;
  background: #1d1d1f;
  border: 1px solid #1d1d1f;
  border-radius: 10px;
  font-weight: 600;
  font-size: 14px;
}

.admin-brand strong {
  display: block;
  color: #1d1d1f;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: 0;
}

.admin-brand small {
  display: block;
  margin-top: 5px;
  color: #7a7a7a;
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
  background: rgba(245, 245, 247, 0.82);
  border: 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
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
  min-height: 0;
  padding: 24px;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: #f5f5f7;
}

.admin-menu :deep(.el-menu-item) {
  height: 42px;
  padding: 0 12px !important;
  margin: 2px 0;
  line-height: 42px;
  color: #7a7a7a;
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

.admin-menu :deep(.el-menu-item:hover) {
  background: #f5f5f7;
  color: #1d1d1f;
  transform: translateX(2px);
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: #1d1d1f;
  color: #ffffff;
  font-weight: 500;
}

.admin-menu :deep(.el-menu-item.is-active:hover) {
  background: #000000;
  color: #ffffff;
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
  justify-content: center;
  padding: 14px 16px 18px;
  border-top: 1px solid #f0f0f0;
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
  background: #f5f5f7;
  color: #7a7a7a;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid #e0e0e0;
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
    overflow: visible;
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
