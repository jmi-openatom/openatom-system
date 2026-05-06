<template>
  <div class="admin-page">
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-select
          v-model="selectedClubId"
          filterable
          placeholder="选择社团"
          style="width: 240px"
          @change="handleClubChange"
        >
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
        </el-select>
        <el-select
          v-model="query.departmentId"
          clearable
          filterable
          placeholder="选择部门"
          style="width: 220px"
          @change="fetchList"
        >
          <el-option
            v-for="department in departments"
            :key="department.id"
            :label="department.name"
            :value="department.id"
          />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增岗位</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="所属部门" min-width="160">
        <template #default="{ row }">{{ departmentName(row.departmentId) }}</template>
      </el-table-column>
      <el-table-column prop="name" label="岗位名称" min-width="160" />
      <el-table-column prop="maxCount" label="人数上限" width="110">
        <template #default="{ row }">{{ row.maxCount || '-' }}</template>
      </el-table-column>
      <el-table-column label="关联角色" min-width="220">
        <template #default="{ row }">
          <span>{{ roleNames(row.roleIds).join(' / ') || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除该岗位？" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="当前社团暂无岗位" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑岗位' : '新增岗位'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属社团" prop="clubId">
          <el-select
            v-model="form.clubId"
            filterable
            placeholder="请选择社团"
            @change="loadDepartments"
          >
            <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select
            v-model="form.departmentId"
            clearable
            filterable
            placeholder="不选则为社团通用岗位"
          >
            <el-option
              v-for="department in dialogDepartments"
              :key="department.id"
              :label="department.name"
              :value="department.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="form.maxCount" :min="1" />
        </el-form-item>
        <el-form-item label="关联角色">
          <el-select
            v-model="form.roleIds"
            multiple
            clearable
            filterable
            placeholder="可选，给岗位绑定基础角色"
          >
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.name || role.code"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { clubApi, positionApi, rbacApi } from '@/api'

export default {
  name: 'AdminPositions',
  data() {
    return {
      Plus,
      Refresh,
      loading: false,
      saving: false,
      dialogVisible: false,
      clubs: [],
      departments: [],
      dialogDepartments: [],
      roles: [],
      selectedClubId: '',
      query: { departmentId: undefined },
      rows: [],
      form: {},
      rules: {
        clubId: [{ required: true, message: '请选择社团', trigger: 'change' }],
        name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
      },
    }
  },
  async created() {
    await Promise.all([this.loadClubs(), this.loadRoles()])
    if (this.selectedClubId) {
      await this.handleClubChange(this.selectedClubId)
    }
  },
  methods: {
    async loadClubs() {
      const result = await clubApi.list({ page: 1, pageSize: 100 })
      this.clubs = result?.list || result || []
      if (!this.selectedClubId && this.clubs.length) {
        this.selectedClubId = this.clubs[0].id
      }
    },
    async loadRoles() {
      const result = await rbacApi.roles({ page: 1, pageSize: 100 })
      this.roles = result?.list || result || []
    },
    async handleClubChange(clubId) {
      await this.loadDepartments(clubId)
      this.query.departmentId = undefined
      await this.fetchList()
    },
    async loadDepartments(clubId = this.selectedClubId) {
      if (!clubId) {
        this.departments = []
        this.dialogDepartments = []
        return
      }
      const result = await clubApi.departments(clubId)
      const departments = result?.list || result || []
      if (clubId === this.selectedClubId) {
        this.departments = departments
      }
      if (clubId === this.form.clubId || clubId === this.selectedClubId) {
        this.dialogDepartments = departments
      }
    },
    async fetchList() {
      if (!this.selectedClubId) {
        this.rows = []
        return
      }
      this.loading = true
      try {
        const result = await clubApi.positions(this.selectedClubId)
        const list = result?.list || result || []
        this.rows = this.query.departmentId
          ? list.filter((item) => item.departmentId === this.query.departmentId)
          : list
      } finally {
        this.loading = false
      }
    },
    openDialog(row) {
      this.form = row
        ? {
            ...row,
            clubId: row.clubId,
            departmentId: row.departmentId,
            roleIds: row.roleIds || [],
          }
        : {
            clubId: this.selectedClubId,
            departmentId: undefined,
            name: '',
            maxCount: undefined,
            roleIds: [],
          }
      this.dialogDepartments = this.form.clubId === this.selectedClubId ? this.departments : []
      this.loadDepartments(this.form.clubId)
      this.dialogVisible = true
    },
    async save() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        this.saving = true
        try {
          const payload = {
            departmentId: this.form.departmentId,
            name: this.form.name,
            maxCount: this.form.maxCount,
            roleIds: this.form.roleIds,
          }
          if (this.form.id) {
            await positionApi.update(this.form.id, payload)
          } else {
            await positionApi.create(this.form.clubId, payload)
          }
          ElMessage.success('岗位已保存')
          this.dialogVisible = false
          this.selectedClubId = this.form.clubId
          await this.handleClubChange(this.selectedClubId)
        } finally {
          this.saving = false
        }
      })
    },
    async remove(row) {
      await positionApi.remove(row.id)
      ElMessage.success('岗位已删除')
      this.fetchList()
    },
    departmentName(departmentId) {
      return this.departments.find((item) => item.id === departmentId)?.name || '通用岗位'
    },
    roleNames(roleIds) {
      return (roleIds || [])
        .map((roleId) => this.roles.find((item) => item.id === roleId)?.name)
        .filter(Boolean)
    },
  },
}
</script>
