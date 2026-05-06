<template>
  <el-container class="admin-shell">
    <el-aside class="admin-aside" width="232px">
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
        text-color="#475569"
        active-text-color="#2563eb"
      >
        <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>统一维护社团、用户、审批、面试与权限数据</p>
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
  </el-container>
</template>

<script lang="ts">
import {
  ArrowDown,
  Calendar,
  Connection,
  DataAnalysis,
  DocumentChecked,
  HomeFilled,
  List,
  Lock,
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

export default {
  name: 'AdminLayout',
  data() {
    return {
      ArrowDown,
      HomeFilled,
      user: getCurrentUser(),
      menus: [
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
          path: '/admin/activities',
          label: '活动管理',
          icon: Calendar,
          permissions: ['activity:list'],
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
        { path: '/', label: '返回官网', icon: Connection, permissions: [] },
      ],
    }
  },
  computed: {
    visibleMenus() {
      return this.menus.filter(
        (item) => item.path === '/' || hasAnyPermission(item.permissions || []),
      )
    },
    pageTitle() {
      const current = this.menus.find((item) => item.path === this.$route.path)
      return current ? current.label : '管理后台'
    },
  },
  methods: {
    async handleCommand(command: string) {
      if (command !== 'logout') return
      try {
        await authApi.logout()
      } finally {
        clearSession()
        this.$router.replace('/admin/login')
      }
    },
  },
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  background: transparent;
}

.admin-aside {
  margin: 16px 0 16px 16px;
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--oa-shadow);
  backdrop-filter: blur(18px);
}

.admin-brand {
  display: flex;
  height: 72px;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  color: var(--oa-text);
}

.admin-brand__mark {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  border-radius: 14px;
  font-weight: 800;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.18);
}

.admin-brand small {
  display: block;
  margin-top: 2px;
  color: #64748b;
}

.admin-menu {
  padding: 8px;
  border-right: 0;
}

.admin-header {
  display: flex;
  height: 72px;
  align-items: center;
  justify-content: space-between;
  margin: 16px 16px 0 16px;
  padding: 0 22px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 24px;
  box-shadow: var(--oa-shadow);
  backdrop-filter: blur(18px);
}

.admin-header h1 {
  margin: 0;
  font-size: 20px;
}

.admin-header p {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.admin-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-main {
  padding: 16px;
  background: transparent;
}

.admin-menu :deep(.el-menu-item) {
  margin-bottom: 6px;
  height: 46px;
  line-height: 46px;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(37, 99, 235, 0.08);
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: rgba(37, 99, 235, 0.1);
  font-weight: 600;
}

@media (max-width: 900px) {
  .admin-aside,
  .admin-header {
    margin-left: 12px;
    margin-right: 12px;
  }

  .admin-main {
    padding: 12px;
  }
}
</style>
