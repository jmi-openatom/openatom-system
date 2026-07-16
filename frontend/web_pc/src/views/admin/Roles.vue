<template>
  <ViewPage class="admin-page role-grid">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" :icon="Plus" @click="openRole()">新增角色</el-button>
        </div>
      </template>
      <el-table v-loading="roleLoading" :data="roles" @row-click="selectRole">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column prop="code" label="编码" min-width="140" />
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openRole(row)">编辑</el-button>
            <el-button link type="danger" @click.stop="deleteRole(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>权限分配</span>
          <el-button type="primary" :disabled="!currentRole.id" @click="savePermissions"
            >保存权限</el-button
          >
        </div>
      </template>
      <el-alert
        v-if="!currentRole.id"
        title="请先选择左侧角色"
        type="info"
        show-icon
        :closable="false"
      />
      <div v-else>
        <p class="muted">当前角色：{{ currentRole.name }}</p>
        <section class="permission-section">
          <div class="permission-section__head">
            <strong>后台入口显示权限</strong>
            <span class="muted">勾选后会自动带上进入该后台菜单所需权限</span>
          </div>
          <el-checkbox-group v-model="checkedMenuKeys" class="permission-list">
            <el-checkbox v-for="item in adminMenuPermissions" :key="item.key" :label="item.key">
              {{ item.label }}
              <span class="muted">({{ item.permissions.join(' / ') || '默认显示' }})</span>
            </el-checkbox>
          </el-checkbox-group>
        </section>

        <section class="permission-section">
          <div class="permission-section__head">
            <strong>接口权限</strong>
            <span class="muted">用于控制具体接口操作，已包含上方入口所需权限</span>
          </div>
          <el-checkbox-group v-model="checkedPermissionIds" class="permission-list">
            <el-checkbox v-for="item in permissions" :key="item.id" :label="item.id">
              {{ item.name || item.code }} <span class="muted">({{ item.code }})</span>
            </el-checkbox>
          </el-checkbox-group>
        </section>
      </div>
    </el-card>

    <el-dialog v-model="roleVisible" :title="roleForm.id ? '编辑角色' : '新增角色'" width="520px">
      <el-form :model="roleForm" label-width="86px">
        <el-form-item label="角色名称">
          <el-input v-model="roleForm.name" />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="roleForm.code" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Plus } from '@element-plus/icons-vue'
import { rbacApi } from '@/api'
import { computed, onMounted, ref, watch } from 'vue'

const roleLoading = ref(false)

const roles = ref<any[]>([])

const permissions = ref<any[]>([])

const currentRole = ref<Record<string, any>>({})

const checkedPermissionIds = ref<any[]>([])

const checkedMenuKeys = ref<string[]>([])

const syncingMenus = ref(false)

const roleVisible = ref(false)

const roleForm = ref<Record<string, any>>({})

const adminMenuPermissions = [
  { key: 'users', label: '用户管理', permissions: ['user:list'] },
  { key: 'memberships', label: '成员管理', permissions: ['membership:list'] },
  { key: 'activities', label: '活动管理', permissions: ['activity:list'] },
  { key: 'ai-activities', label: 'AI 活动自动化', permissions: ['activity:create'] },
  { key: 'check-ins', label: '扫码签到', permissions: ['check-in:list'] },
  { key: 'leaves', label: '请假审批', permissions: ['leave-application:list'] },
  { key: 'site-forms', label: '表单管理', permissions: ['site-form:list'] },
  { key: 'form-submissions', label: '表单记录', permissions: ['site-form:detail'] },
  { key: 'clubs', label: '社团管理', permissions: ['club:list'] },
  { key: 'positions', label: '岗位管理', permissions: ['position:list'] },
  { key: 'recruitment-campaigns', label: '招新计划', permissions: ['recruitment-campaign:list'] },
  { key: 'applications', label: '入会申请', permissions: ['application:list'] },
  { key: 'interviews', label: '面试管理', permissions: ['interview:list'] },
  { key: 'votes', label: '投票管理', permissions: ['vote:list'] },
  { key: 'lotteries', label: '抽奖系统', permissions: ['lottery:list'] },
  {
    key: 'points',
    label: '积分兑换',
    permissions: ['point:account:list', 'point:item:list', 'point:redemption:list'],
  },
  { key: 'awards', label: '获奖经历', permissions: ['award:list'] },
  { key: 'blogs', label: '博客管理', permissions: ['blog:list'] },
  {
    key: 'member-profile-comments',
    label: '主页评论',
    permissions: ['member-profile-comment:list'],
  },
  { key: 'office-documents', label: '文书中心', permissions: ['document:list'] },
  { key: 'images', label: '图床管理', permissions: ['image:list'] },
  { key: 'school-calendar', label: '校历设置', permissions: ['school-calendar:manage'] },
  { key: 'notifications', label: '通知管理', permissions: ['notification:list'] },
  { key: 'roles', label: '角色权限', permissions: ['role:list', 'permission:list'] },
  { key: 'oauth-clients', label: '认证应用', permissions: ['oauth-client:list'] },
  { key: 'showcase-apps', label: '应用展示', permissions: ['showcase-app:list'] },
  { key: 'data-open', label: '开放平台', permissions: ['data-open:list'] },
  { key: 'bot-groups', label: 'QQ 机器人', permissions: ['bot-management:list'] },
  { key: 'logs', label: '系统日志', permissions: ['log:operation:list', 'log:login:list'] },
]

const permissionIdByCode = computed(() => {
  const map = new Map<string, number>()
  for (const permission of permissions.value) {
    if (permission?.code && permission?.id) map.set(permission.code, permission.id)
  }
  return map
})

async function fetchRoles() {
  roleLoading.value = true
  try {
    const result = await rbacApi.roles({ page: 1, pageSize: 100 })
    roles.value = result?.list || result || []
  } finally {
    roleLoading.value = false
  }
}

async function fetchPermissions() {
  const result = await rbacApi.permissions({ page: 1, pageSize: 200 })
  permissions.value = result?.list || result || []
  if (currentRole.value.id) syncMenusFromPermissions()
}

async function selectRole(row: any) {
  currentRole.value = row
  const detail = await rbacApi.roleDetail(row.id)
  currentRole.value = { ...row, ...detail }
  checkedPermissionIds.value = normalizePermissionIds(detail)
  syncMenusFromPermissions()
}

function openRole(row: any) {
  roleForm.value = row ? { ...row } : { name: '', code: '', description: '' }
  roleVisible.value = true
}

async function saveRole() {
  if (roleForm.value.id) await rbacApi.updateRole(roleForm.value.id, roleForm.value)
  else await rbacApi.createRole(roleForm.value)
  ElMessage.success('角色已保存')
  roleVisible.value = false
  fetchRoles()
}

async function deleteRole(row: any) {
  await ElMessageBox.confirm(`确认删除角色 ${row.name}？`, '提示')
  await rbacApi.deleteRole(row.id)
  ElMessage.success('角色已删除')
  fetchRoles()
}

async function savePermissions() {
  applyMenuPermissionsToChecked()
  await rbacApi.assignRolePermissions(currentRole.value.id, {
    permissionIds: Array.from(new Set(checkedPermissionIds.value)),
  })
  ElMessage.success('权限已保存')
  await selectRole(currentRole.value)
}

onMounted(() => {
  fetchRoles()
  fetchPermissions()
})

watch(checkedMenuKeys, () => {
  if (syncingMenus.value) return
  applyMenuPermissionsToChecked()
})

function normalizePermissionIds(detail: any) {
  if (Array.isArray(detail?.permissionIds)) return detail.permissionIds
  if (Array.isArray(detail?.permissions))
    return detail.permissions.map((item: any) => item.id).filter(Boolean)
  return []
}

function syncMenusFromPermissions() {
  const checkedCodes = new Set(
    permissions.value
      .filter((permission) => checkedPermissionIds.value.includes(permission.id))
      .map((permission) => permission.code),
  )
  syncingMenus.value = true
  checkedMenuKeys.value = adminMenuPermissions
    .filter(
      (item) => item.permissions.length && item.permissions.every((code) => checkedCodes.has(code)),
    )
    .map((item) => item.key)
  syncingMenus.value = false
}

function applyMenuPermissionsToChecked() {
  const next = new Set(checkedPermissionIds.value)
  for (const menu of adminMenuPermissions.filter((item) =>
    checkedMenuKeys.value.includes(item.key),
  )) {
    for (const code of menu.permissions) {
      const id = permissionIdByCode.value.get(code)
      if (id) next.add(id)
    }
  }
  checkedPermissionIds.value = Array.from(next)
}
</script>

<style scoped>
.role-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius);
  background: var(--oa-border);
}

.role-grid :deep(.el-card) {
  min-height: 100%;
  border: 0 !important;
  border-radius: 0 !important;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.permission-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.permission-section {
  padding: 14px 0;
  border-top: 1px solid var(--oa-border);
}

.permission-section:first-of-type {
  margin-top: 12px;
}

.permission-section__head {
  display: grid;
  gap: 4px;
}

@media (max-width: 980px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
