<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索用户名/姓名"
          style="width: 220px"
          @keyup.enter="fetchList"
        />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
          <el-option label="启用" value="active" />
          <el-option label="禁用" value="disabled" />
          <el-option label="锁定" value="locked" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
      </div>
      <div class="toolbar__actions">
        <el-button
          v-if="canBatchJoinClub"
          type="success"
          :disabled="!selection.length"
          @click="openBatchJoinDialog"
          >批量加入社团</el-button
        >
        <el-upload
          v-if="canImportUsers"
          action="/api/v1/users/import"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleImportSuccess"
          :on-error="handleImportError"
          accept=".xlsx, .xls"
          style="display: inline-block; margin-right: 12px"
        >
          <el-button type="success" :icon="Upload">导入用户</el-button>
        </el-upload>
        <el-button v-if="canImportUsers" type="info" link @click="downloadTemplate"
          >下载模板</el-button
        >
        <el-button v-if="canInspectAvatars" plain @click="openAvatarHealthDialog">
          头像巡检
        </el-button>
        <el-button v-if="canCreateUser" type="primary" :icon="Plus" @click="openDialog()"
          >新增用户</el-button
        >
      </div>
    </ViewToolbar>
    <el-table
      v-loading="loading"
      :data="rows"
      class="admin-table"
      @selection-change="selection = $event"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="userName" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="studentId" label="学号" />
      <el-table-column prop="className" label="班级" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column label="微信" width="100">
        <template #default="{ row }">
          <el-tag :type="row.miniappOpenid ? 'success' : 'info'">
            {{ row.miniappOpenid ? '已绑定' : '未绑定' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="绑定 QQ" min-width="150">
        <template #default="{ row }">
          <el-tag :type="row.qqOpenid ? 'success' : 'info'">
            {{ row.qqOpenid || '未绑定' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="userStatus" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.userStatus)">{{ statusText(row.userStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="激活" width="100">
        <template #default="{ row }">
          <el-tag :type="row.activatedAt ? 'success' : 'warning'" size="small">
            {{ row.activatedAt ? '已激活' : '未激活' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="440" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canUpdateUser" link type="primary" @click="openDialog(row)"
            >编辑</el-button
          >
          <el-button v-if="canAssignRole" link type="success" @click="openRoleDialog(row)"
            >分配角色</el-button
          >
          <el-button v-if="canUpdateUserStatus" link type="warning" @click="toggleStatus(row)">
            {{ row.userStatus === 'disabled' ? '启用' : '禁用' }}
          </el-button>
          <el-button
            v-if="canSetActivation"
            link
            :type="row.activatedAt ? 'warning' : 'success'"
            @click="toggleActivation(row)"
          >
            {{ row.activatedAt ? '取消激活' : '激活' }}
          </el-button>
          <el-button v-if="canResetUserPassword" link type="danger" @click="resetPassword(row)"
            >重置密码</el-button
          >
          <el-button v-if="canDeleteUser" link type="danger" @click="removeUser(row)"
            >删除用户</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pager"
      layout="total, prev, pager, next, sizes"
      :total="total"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      @change="fetchList"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="form.studentId" />
        </el-form-item>
        <el-form-item label="班级">
          <el-input v-model="form.className" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="500px">
      <el-form label-width="86px">
        <el-form-item label="用户名">
          <span>{{ currentRoleUser.realName || currentRoleUser.userName }}</span>
        </el-form-item>
        <el-form-item label="选择角色">
          <el-checkbox-group v-model="checkedRoleIds">
            <el-checkbox v-for="item in allRoles" :key="item.id" :label="item.id">
              {{ item.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="saveUserRoles">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchJoinVisible" title="批量加入社团" width="560px">
      <el-form :model="batchJoinForm" label-width="90px">
        <el-form-item label="选择人数">
          <el-input :model-value="String(selection.length)" disabled />
        </el-form-item>
        <el-form-item label="社团">
          <el-select v-model="batchJoinForm.clubId" filterable placeholder="请选择社团">
            <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成员状态">
          <el-select v-model="batchJoinForm.status">
            <el-option label="正式成员" value="active" />
            <el-option label="非正式成员" value="probation" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchJoinVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchJoinSaving" @click="submitBatchJoin"
          >确认加入</el-button
        >
      </template>
    </el-dialog>

    <el-dialog v-model="avatarHealthVisible" title="头像巡检" width="620px">
      <div v-loading="avatarHealthLoading" class="avatar-health">
        <div class="avatar-health__summary">
          <strong>{{ avatarHealth.totalManaged || 0 }}</strong>
          <span>系统托管头像</span>
          <strong :class="{ danger: avatarHealth.invalidCount }">
            {{ avatarHealth.invalidCount || 0 }}
          </strong>
          <span>失效头像</span>
        </div>

        <el-empty
          v-if="!avatarHealthLoading && !avatarHealth.invalidUsers?.length"
          description="当前没有失效头像"
        />

        <el-table v-else :data="avatarHealth.invalidUsers || []" border size="small">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="userName" label="用户名" />
          <el-table-column prop="avatar" label="失效地址" min-width="220" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="avatarHealthVisible = false">关闭</el-button>
        <el-button
          v-if="canCleanupAvatars"
          type="primary"
          :loading="avatarCleanupLoading"
          :disabled="!avatarHealth.invalidCount"
          @click="cleanupInvalidAvatars"
        >
          清理失效头像
        </el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Plus, Search, Upload } from '@element-plus/icons-vue'
import { clubApi, membershipApi, userApi, rbacApi } from '@/api'
import { statusType } from '@/utils/format.ts'
import { getToken } from '@/utils/auth.ts'
import { hasPermission } from '@/utils/permission.ts'
import { computed, onMounted, ref } from 'vue'

const loading = ref(false)

const saving = ref(false)

const dialogVisible = ref(false)

const rows = ref<any[]>([])

const selection = ref<any[]>([])

const clubs = ref<any[]>([])

const total = ref(0)

const query = ref({ keyword: '', status: '', page: 1, pageSize: 10 })

const form = ref<Record<string, any>>({})

const allRoles = ref<any[]>([])

const currentRoleUser = ref<Record<string, any>>({})

const checkedRoleIds = ref<any[]>([])

const roleDialogVisible = ref(false)

const roleSaving = ref(false)

const batchJoinVisible = ref(false)

const batchJoinSaving = ref(false)

const batchJoinForm = ref({ clubId: undefined, status: 'active' })
const avatarHealthVisible = ref(false)
const avatarHealthLoading = ref(false)
const avatarCleanupLoading = ref(false)
const avatarHealth = ref<any>({})

const rules = ref({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
})

const formRef = ref<any>()

const uploadHeaders = computed(() => {
  const token = getToken()
  return {
    Authorization: `Bearer ${token}`,
    jmiopenatom: token,
  }
})

const canImportUsers = computed(() => {
  return hasPermission('user:import')
})

const canCreateUser = computed(() => {
  return hasPermission('user:create')
})

const canAssignRole = computed(() => {
  return hasPermission('user:role:assign')
})

const canUpdateUser = computed(() => {
  return hasPermission('user:update')
})

const canUpdateUserStatus = computed(() => {
  return hasPermission('user:status:update')
})

const canResetUserPassword = computed(() => {
  return hasPermission('user:password:reset')
})

const canDeleteUser = computed(() => {
  return hasPermission('user:delete')
})

const canBatchJoinClub = computed(() => {
  return hasPermission('membership:batch-create')
})

const canInspectAvatars = computed(() => {
  return hasPermission('user:list')
})

const canCleanupAvatars = computed(() => {
  return hasPermission('user:update')
})

const canSetActivation = computed(() => {
  return hasPermission('user:update')
})

function statusText(status: any) {
  return { active: '启用', disabled: '禁用', locked: '锁定' }[status] || status || '-'
}

function handleImportSuccess(res: any) {
  if (res.code === 0) {
    ElMessage.success(res.message || '导入成功')
    fetchList()
  } else {
    ElMessage.error(res.message || '导入失败')
  }
}

function handleImportError(err: any) {
  const errorMsg = err.message || '上传失败'
  ElMessage.error(errorMsg)
}

function downloadTemplate() {
  const token = getToken()
  const url = `/api/v1/users/import/template?jmiopenatom=${token}`
  window.open(url, '_blank')
}

async function fetchList() {
  loading.value = true
  try {
    const result = await userApi.list(query.value)
    rows.value = result?.list || result || []
    total.value = result?.total || rows.value.length
  } finally {
    loading.value = false
  }
}

async function fetchClubs() {
  const result = await clubApi.list({ page: 1, pageSize: 100 })
  clubs.value = result?.list || result || []
  if (!batchJoinForm.value.clubId && clubs.value.length) {
    batchJoinForm.value.clubId = clubs.value[0].id
  }
}

function openDialog(row: any) {
  form.value = row
    ? { ...row, username: row.userName || row.username }
    : {
        username: '',
        realName: '',
        studentId: '',
        className: '',
        phone: '',
        email: '',
        password: '',
      }
  dialogVisible.value = true
}

function saveUser() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (form.value.id) await userApi.update(form.value.id, form.value)
      else await userApi.create(form.value)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchList()
    } finally {
      saving.value = false
    }
  })
}

async function toggleStatus(row: any) {
  const nextStatus = row.userStatus === 'disabled' ? 'active' : 'disabled'
  await userApi.updateStatus(row.id, nextStatus)
  ElMessage.success('状态已更新')
  fetchList()
}

async function toggleActivation(row: any) {
  const action = row.activatedAt ? '取消激活' : '激活'
  try {
    await userApi.setActivation(row.id, !row.activatedAt)
    ElMessage.success(`${action}成功`)
    fetchList()
  } catch {
    ElMessage.error(`${action}失败`)
  }
}

async function resetPassword(row: any) {
  await ElMessageBox.confirm(`确认重置 ${row.realName || row.userName} 的密码？`, '提示', {
    confirmButtonText: '确认重置',
    cancelButtonText: '取消',
  })
  await userApi.resetPassword(row.id, { newPassword: '12345678' })
  ElMessage.success('已重置为 12345678')
}

async function removeUser(row: any) {
  await ElMessageBox.confirm(
    `确认删除用户 ${row.realName || row.userName}？删除后不可恢复。若该用户仍在社团中，请先移出社团。`,
    '提示',
    { type: 'warning' },
  )
  await userApi.remove(row.id)
  ElMessage.success('用户已删除')
  fetchList()
}

async function fetchAllRoles() {
  if (allRoles.value.length === 0) {
    const result = await rbacApi.roles({ page: 1, pageSize: 100 })
    allRoles.value = result?.list || result || []
  }
}

async function openRoleDialog(row: any) {
  currentRoleUser.value = row
  checkedRoleIds.value = []
  roleDialogVisible.value = true
  await fetchAllRoles()
  try {
    const roles = await userApi.roles(row.id)
    checkedRoleIds.value = roles.map((r) => r.id)
  } catch (err) {
    ElMessage.error('获取用户角色失败')
  }
}

async function saveUserRoles() {
  roleSaving.value = true
  try {
    await userApi.assignRoles(currentRoleUser.value.id, { roleIds: checkedRoleIds.value })
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } finally {
    roleSaving.value = false
  }
}

function openBatchJoinDialog() {
  if (!selection.value.length) {
    ElMessage.warning('请先选择用户')
    return
  }
  batchJoinVisible.value = true
}

async function submitBatchJoin() {
  if (!batchJoinForm.value.clubId) {
    ElMessage.error('请选择社团')
    return
  }
  batchJoinSaving.value = true
  try {
    await membershipApi.batchCreate({
      memberships: selection.value.map((user) => ({
        userId: user.id,
        clubId: batchJoinForm.value.clubId,
        status: batchJoinForm.value.status,
      })),
    })
    ElMessage.success(`已提交 ${selection.value.length} 位用户加入社团`)
    batchJoinVisible.value = false
  } finally {
    batchJoinSaving.value = false
  }
}

async function openAvatarHealthDialog() {
  avatarHealthVisible.value = true
  await fetchAvatarHealth()
}

async function fetchAvatarHealth() {
  avatarHealthLoading.value = true
  try {
    avatarHealth.value = (await userApi.avatarHealth()) || {}
  } finally {
    avatarHealthLoading.value = false
  }
}

async function cleanupInvalidAvatars() {
  await ElMessageBox.confirm('确认清理所有失效头像记录？用户将恢复为默认姓氏头像。', '提示', {
    type: 'warning',
  })
  avatarCleanupLoading.value = true
  try {
    const cleaned = await userApi.cleanupInvalidAvatars()
    ElMessage.success(`已清理 ${cleaned || 0} 条失效头像`)
    await fetchAvatarHealth()
    await fetchList()
  } finally {
    avatarCleanupLoading.value = false
  }
}

onMounted(() => {
  fetchList()
  fetchClubs()
})
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.avatar-health {
  min-height: 180px;
}

.avatar-health__summary {
  display: grid;
  grid-template-columns: auto auto auto auto;
  gap: 10px;
  align-items: baseline;
  width: fit-content;
  margin-bottom: 18px;
}

.avatar-health__summary strong {
  font-size: 28px;
}

.avatar-health__summary strong.danger {
  color: #f56c6c;
}
</style>
