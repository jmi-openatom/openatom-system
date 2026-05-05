<template>
  <div class="admin-page role-grid">
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
          <el-button type="primary" :disabled="!currentRole.id" @click="savePermissions">保存权限</el-button>
        </div>
      </template>
      <el-alert v-if="!currentRole.id" title="请先选择左侧角色" type="info" show-icon :closable="false" />
      <div v-else>
        <p class="muted">当前角色：{{ currentRole.name }}</p>
        <el-checkbox-group v-model="checkedPermissionIds" class="permission-list">
          <el-checkbox v-for="item in permissions" :key="item.id" :label="item.id">
            {{ item.name || item.code }} <span class="muted">({{ item.code }})</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
    </el-card>

    <el-dialog v-model="roleVisible" :title="roleForm.id ? '编辑角色' : '新增角色'" width="520px">
      <el-form :model="roleForm" label-width="86px">
        <el-form-item label="角色名称"><el-input v-model="roleForm.name" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="roleForm.code" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="roleForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { rbacApi } from '../../api'

export default {
  name: 'AdminRoles',
  data() {
    return {
      Plus,
      roleLoading: false,
      roles: [],
      permissions: [],
      currentRole: {},
      checkedPermissionIds: [],
      roleVisible: false,
      roleForm: {}
    }
  },
  created() {
    this.fetchRoles()
    this.fetchPermissions()
  },
  methods: {
    async fetchRoles() {
      this.roleLoading = true
      try {
        const result = await rbacApi.roles({ page: 1, pageSize: 100 })
        this.roles = result?.list || result || []
      } finally {
        this.roleLoading = false
      }
    },
    async fetchPermissions() {
      const result = await rbacApi.permissions({ page: 1, pageSize: 200 })
      this.permissions = result?.list || result || []
    },
    selectRole(row) {
      this.currentRole = row
      // 如果后端角色详情返回 permissions，可直接回填；否则让管理员重新勾选。
      this.checkedPermissionIds = (row.permissions || []).map((item) => item.id)
    },
    openRole(row) {
      this.roleForm = row ? { ...row } : { name: '', code: '', description: '' }
      this.roleVisible = true
    },
    async saveRole() {
      if (this.roleForm.id) await rbacApi.updateRole(this.roleForm.id, this.roleForm)
      else await rbacApi.createRole(this.roleForm)
      ElMessage.success('角色已保存')
      this.roleVisible = false
      this.fetchRoles()
    },
    async deleteRole(row) {
      await ElMessageBox.confirm(`确认删除角色 ${row.name}？`, '提示')
      await rbacApi.deleteRole(row.id)
      ElMessage.success('角色已删除')
      this.fetchRoles()
    },
    async savePermissions() {
      await rbacApi.assignRolePermissions(this.currentRole.id, {
        permissionIds: this.checkedPermissionIds
      })
      ElMessage.success('权限已保存')
    }
  }
}
</script>

<style scoped>
.role-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 16px;
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

@media (max-width: 980px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
