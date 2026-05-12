<template>
  <div class="admin-page">
    <div class="toolbar">
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
        <el-button v-if="canCreateUser" type="primary" :icon="Plus" @click="openDialog()"
          >新增用户</el-button
        >
      </div>
    </div>
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
      <el-table-column prop="userStatus" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.userStatus)">{{ statusText(row.userStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
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
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Upload } from '@element-plus/icons-vue'
import { clubApi, membershipApi, userApi, rbacApi } from '@/api'
import { statusType } from '@/utils/format.ts'
import { getToken } from '@/utils/auth.ts'
import { hasPermission } from '@/utils/permission.ts'

export default {
  name: 'AdminUsers',
  data() {
    return {
      Plus,
      Search,
      Upload,
      loading: false,
      saving: false,
      dialogVisible: false,
      rows: [],
      selection: [],
      clubs: [],
      total: 0,
      query: { keyword: '', status: '', page: 1, pageSize: 10 },
      form: {},
      allRoles: [],
      currentRoleUser: {},
      checkedRoleIds: [],
      roleDialogVisible: false,
      roleSaving: false,
      batchJoinVisible: false,
      batchJoinSaving: false,
      batchJoinForm: { clubId: undefined, status: 'active' },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
      },
    }
  },
  computed: {
    uploadHeaders() {
      const token = getToken()
      return {
        Authorization: `Bearer ${token}`,
        jmiopenatom: token,
      }
    },
    canImportUsers() {
      return hasPermission('user:import')
    },
    canCreateUser() {
      return hasPermission('user:create')
    },
    canAssignRole() {
      return hasPermission('user:role:assign')
    },
    canUpdateUser() {
      return hasPermission('user:update')
    },
    canUpdateUserStatus() {
      return hasPermission('user:status:update')
    },
    canResetUserPassword() {
      return hasPermission('user:password:reset')
    },
    canDeleteUser() {
      return hasPermission('user:delete')
    },
    canBatchJoinClub() {
      return hasPermission('membership:batch-create')
    },
  },
  created() {
    this.fetchList()
    this.fetchClubs()
  },
  methods: {
    statusType,
    statusText(status) {
      return { active: '启用', disabled: '禁用', locked: '锁定' }[status] || status || '-'
    },
    handleImportSuccess(res) {
      if (res.code === 0) {
        ElMessage.success(res.message || '导入成功')
        this.fetchList()
      } else {
        ElMessage.error(res.message || '导入失败')
      }
    },
    handleImportError(err) {
      const errorMsg = err.message || '上传失败'
      ElMessage.error(errorMsg)
    },
    downloadTemplate() {
      const token = getToken()
      const url = `/api/v1/users/import/template?jmiopenatom=${token}`
      window.open(url, '_blank')
    },
    async fetchList() {
      this.loading = true
      try {
        const result = await userApi.list(this.query)
        this.rows = result?.list || result || []
        this.total = result?.total || this.rows.length
      } finally {
        this.loading = false
      }
    },
    async fetchClubs() {
      const result = await clubApi.list({ page: 1, pageSize: 100 })
      this.clubs = result?.list || result || []
      if (!this.batchJoinForm.clubId && this.clubs.length) {
        this.batchJoinForm.clubId = this.clubs[0].id
      }
    },
    openDialog(row) {
      this.form = row
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
      this.dialogVisible = true
    },
    saveUser() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        this.saving = true
        try {
          if (this.form.id) await userApi.update(this.form.id, this.form)
          else await userApi.create(this.form)
          ElMessage.success('保存成功')
          this.dialogVisible = false
          this.fetchList()
        } finally {
          this.saving = false
        }
      })
    },
    async toggleStatus(row) {
      const nextStatus = row.userStatus === 'disabled' ? 'active' : 'disabled'
      await userApi.updateStatus(row.id, nextStatus)
      ElMessage.success('状态已更新')
      this.fetchList()
    },
    async resetPassword(row) {
      await ElMessageBox.confirm(`确认重置 ${row.realName || row.userName} 的密码？`, '提示')
      await userApi.resetPassword(row.id, { newPassword: '12345678' })
      ElMessage.success('已重置为 12345678')
    },
    async removeUser(row) {
      await ElMessageBox.confirm(
        `确认删除用户 ${row.realName || row.userName}？删除后不可恢复。若该用户仍在社团中，请先移出社团。`,
        '提示',
        { type: 'warning' },
      )
      await userApi.remove(row.id)
      ElMessage.success('用户已删除')
      this.fetchList()
    },
    async fetchAllRoles() {
      if (this.allRoles.length === 0) {
        const result = await rbacApi.roles({ page: 1, pageSize: 100 })
        this.allRoles = result?.list || result || []
      }
    },
    async openRoleDialog(row) {
      this.currentRoleUser = row
      this.checkedRoleIds = []
      this.roleDialogVisible = true
      await this.fetchAllRoles()
      try {
        const roles = await userApi.roles(row.id)
        this.checkedRoleIds = roles.map((r) => r.id)
      } catch (err) {
        ElMessage.error('获取用户角色失败')
      }
    },
    async saveUserRoles() {
      this.roleSaving = true
      try {
        await userApi.assignRoles(this.currentRoleUser.id, { roleIds: this.checkedRoleIds })
        ElMessage.success('角色分配成功')
        this.roleDialogVisible = false
      } finally {
        this.roleSaving = false
      }
    },
    openBatchJoinDialog() {
      if (!this.selection.length) {
        ElMessage.warning('请先选择用户')
        return
      }
      this.batchJoinVisible = true
    },
    async submitBatchJoin() {
      if (!this.batchJoinForm.clubId) {
        ElMessage.error('请选择社团')
        return
      }
      this.batchJoinSaving = true
      try {
        await membershipApi.batchCreate({
          memberships: this.selection.map((user) => ({
            userId: user.id,
            clubId: this.batchJoinForm.clubId,
            status: this.batchJoinForm.status,
          })),
        })
        ElMessage.success(`已提交 ${this.selection.length} 位用户加入社团`)
        this.batchJoinVisible = false
      } finally {
        this.batchJoinSaving = false
      }
    },
  },
}
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
