<template>
  <div class="admin-page">
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索成员/社团"
          style="width: 220px"
          @keyup.enter="fetchList"
        />
        <el-select v-model="query.status" clearable placeholder="成员状态" style="width: 150px">
          <el-option label="在会" value="active" />
          <el-option label="试用" value="probation" />
          <el-option label="暂停" value="suspended" />
          <el-option label="退出" value="left" />
          <el-option label="毕业" value="graduated" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
      </div>
      <div class="toolbar__actions">
        <el-button
          type="success"
          :disabled="!selection.length"
          @click="batchChangeStatus('active')"
          >批量转正式</el-button
        >
        <el-button
          type="warning"
          :disabled="!selection.length"
          @click="batchChangeStatus('suspended')"
          >批量暂停</el-button
        >
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增成员</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="rows" class="admin-table" @selection-change="selection = $event">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="成员" min-width="150">
        <template #default="{ row }">{{
          row.realName || row.userName || `用户 ${row.userId}`
        }}</template>
      </el-table-column>
      <el-table-column prop="clubName" label="社团" min-width="140" />
      <el-table-column prop="departmentName" label="部门" min-width="120" />
      <el-table-column prop="positionName" label="岗位" min-width="120" />
      <el-table-column prop="featured" label="官网主要人员" width="130">
        <template #default="{ row }">
          <el-switch v-model="row.featured" @change="toggleFeatured(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="展示排序" width="150">
        <template #default="{ row }">
          <el-input-number
            v-model="row.sortOrder"
            :min="0"
            size="small"
            @change="updateDisplay(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ membershipStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAssign(row)">分配岗位</el-button>
          <el-button link type="success" @click="changeStatus(row, 'active')">转正式</el-button>
          <el-button link type="warning" @click="changeStatus(row, 'suspended')">暂停</el-button>
          <el-button link type="danger" @click="changeStatus(row, 'left')">退会</el-button>
          <el-button v-if="canForceExit" link type="danger" @click="forceExit(row)"
            >移出社团</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增成员" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户ID">
          <el-input-number v-model="form.userId" :min="1" />
        </el-form-item>
        <el-form-item label="社团">
          <el-input :model-value="currentClubName" disabled />
        </el-form-item>
        <el-form-item label="部门">
          <el-select
            v-model="form.departmentId"
            clearable
            filterable
            placeholder="选择部门"
            @change="form.positionId = undefined"
          >
            <el-option
              v-for="item in departments"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.positionId" clearable filterable placeholder="选择岗位">
            <el-option
              v-for="item in filteredPositions(form.departmentId)"
              :key="item.id"
              :label="positionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="官网展示">
          <el-switch v-model="form.featured" />
        </el-form-item>
        <el-form-item label="展示排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createMembership">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="分配岗位" width="520px">
      <el-form :model="assignForm" label-width="90px">
        <el-form-item label="成员">
          <el-input :model-value="assignForm.memberName" disabled />
        </el-form-item>
        <el-form-item label="部门">
          <el-select
            v-model="assignForm.departmentId"
            clearable
            filterable
            placeholder="选择部门"
            @change="assignForm.positionId = undefined"
          >
            <el-option
              v-for="item in departments"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="assignForm.positionId" clearable filterable placeholder="选择岗位">
            <el-option
              v-for="item in filteredPositions(assignForm.departmentId)"
              :key="item.id"
              :label="positionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { clubApi, membershipApi } from '@/api'
import { membershipStatusText, statusType } from '@/utils/format.ts'
import { hasPermission } from '@/utils/permission.ts'

export default {
  name: 'AdminMemberships',
  data() {
    return {
      Plus,
      Search,
      loading: false,
      rows: [],
      selection: [],
      clubs: [],
      departments: [],
      positions: [],
      currentClubId: undefined,
      currentClubName: '',
      query: { keyword: '', status: '', page: 1, pageSize: 20 },
      dialogVisible: false,
      assignVisible: false,
      form: {
        userId: undefined,
        clubId: undefined,
        departmentId: undefined,
        positionId: undefined,
        featured: false,
        sortOrder: 0,
      },
      assignForm: {
        membershipId: '',
        memberName: '',
        departmentId: undefined,
        positionId: undefined,
      },
    }
  },
  async created() {
    await this.loadOptions()
    this.fetchList()
  },
  computed: {
    canForceExit() {
      return hasPermission('membership:force-exit')
    },
  },
  methods: {
    membershipStatusText,
    statusType,
    async fetchList() {
      this.loading = true
      try {
        const result = await membershipApi.list({ ...this.query, clubId: this.currentClubId })
        this.rows = result?.list || result || []
      } finally {
        this.loading = false
      }
    },
    async loadOptions() {
      const clubResult = await clubApi.list({})
      this.clubs = clubResult?.list || clubResult || []
      const defaultClub = this.clubs.find((item) => item.code === 'JMI-OPENATOM') || this.clubs[0]
      this.currentClubId = defaultClub?.id
      this.currentClubName = defaultClub?.name || '默认社团'
      if (!this.currentClubId) return
      this.departments = await clubApi.departments(this.currentClubId)
      this.positions = await clubApi.positions(this.currentClubId)
    },
    openDialog() {
      this.form = {
        userId: undefined,
        clubId: this.currentClubId,
        departmentId: undefined,
        positionId: undefined,
        featured: false,
        sortOrder: 0,
        status: 'probation',
      }
      this.dialogVisible = true
    },
    async createMembership() {
      await membershipApi.create(this.form)
      ElMessage.success('成员已创建')
      this.dialogVisible = false
      this.fetchList()
    },
    openAssign(row) {
      this.assignForm = {
        membershipId: row.id,
        memberName: row.realName || row.userName || `用户 ${row.userId}`,
        departmentId: row.departmentId,
        positionId: row.positionId,
      }
      this.assignVisible = true
    },
    async submitAssign() {
      await membershipApi.update(this.assignForm.membershipId, {
        departmentId: this.assignForm.departmentId,
        positionId: this.assignForm.positionId,
      })
      ElMessage.success('岗位已更新')
      this.assignVisible = false
      this.fetchList()
    },
    async changeStatus(row, status) {
      await membershipApi.changeStatus(row.id, { status })
      ElMessage.success('成员状态已更新')
      this.fetchList()
    },
    async batchChangeStatus(status) {
      if (!this.selection.length) return
      try {
        await Promise.all(
          this.selection.map((item) => membershipApi.changeStatus(item.id, { status })),
        )
        ElMessage.success(`已批量处理 ${this.selection.length} 条成员状态`)
        this.fetchList()
      } catch {
        ElMessage.error('部分操作失败')
      }
    },
    async forceExit(row) {
      await ElMessageBox.confirm(
        `确认将 ${row.realName || row.userName || `用户 ${row.userId}`} 移出社团？`,
        '提示',
        { type: 'warning' },
      )
      await membershipApi.forceExit(row.id, { reason: '管理员移出社团' })
      ElMessage.success('已移出社团')
      this.fetchList()
    },
    async toggleFeatured(row) {
      await this.updateDisplay(row)
    },
    async updateDisplay(row) {
      await membershipApi.update(row.id, { featured: row.featured, sortOrder: row.sortOrder || 0 })
      ElMessage.success('官网展示已更新')
    },
    filteredPositions(departmentId) {
      if (!departmentId) return this.positions
      return this.positions.filter((item) => item.departmentId === departmentId)
    },
    positionLabel(position) {
      const department = this.departments.find((item) => item.id === position.departmentId)
      return department ? `${department.name} / ${position.name}` : position.name
    },
  },
}
</script>
