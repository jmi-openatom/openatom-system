<template>
  <div class="admin-page">
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索社团"
          style="width: 220px"
          @keyup.enter="fetchList"
        />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
          <el-option label="启用" value="enabled" />
          <el-option label="禁用" value="disabled" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增社团</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="社团名称" min-width="160" />
      <el-table-column prop="code" label="编号" width="120" />
      <el-table-column prop="category" label="分类" width="130" />
      <el-table-column prop="recruitmentStatus" label="招新" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.recruitmentStatus)">{{
            row.recruitmentStatus || '-'
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="success" @click="changeRecruitment(row, 'open')"
            >开放招新</el-button
          >
          <el-button link type="warning" @click="changeRecruitment(row, 'closed')"
            >关闭招新</el-button
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑社团' : '新增社团'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="社团名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="社团编号" prop="code">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" />
        </el-form-item>
        <el-form-item label="社长用户ID">
          <el-input-number v-model="form.presidentUserId" :min="1" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveClub">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { clubApi } from '@/api'
import { statusType } from '@/utils/format.ts'

export default {
  name: 'AdminClubs',
  data() {
    return {
      Plus,
      Search,
      loading: false,
      saving: false,
      dialogVisible: false,
      rows: [],
      total: 0,
      query: { keyword: '', status: '', page: 1, pageSize: 10 },
      form: {},
      rules: {
        name: [{ required: true, message: '请输入社团名称', trigger: 'blur' }],
        code: [{ required: true, message: '请输入社团编号', trigger: 'blur' }],
      },
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    statusType,
    async fetchList() {
      this.loading = true
      try {
        const result = await clubApi.list(this.query)
        this.rows = result?.list || result || []
        this.total = result?.total || this.rows.length
      } finally {
        this.loading = false
      }
    },
    openDialog(row) {
      this.form = row
        ? { ...row }
        : { name: '', code: '', category: '', presidentUserId: undefined, description: '' }
      this.dialogVisible = true
    },
    saveClub() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        this.saving = true
        try {
          if (this.form.id) await clubApi.update(this.form.id, this.form)
          else await clubApi.create(this.form)
          ElMessage.success('保存成功')
          this.dialogVisible = false
          this.fetchList()
        } finally {
          this.saving = false
        }
      })
    },
    async changeRecruitment(row, status) {
      await clubApi.updateRecruitmentStatus(row.id, status)
      ElMessage.success('招新状态已更新')
      this.fetchList()
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
