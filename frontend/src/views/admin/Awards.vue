<template>
  <div class="admin-page">
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增获奖</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="awardYear" label="年份" width="100" />
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="competitionName" label="比赛名称" min-width="220" />
      <el-table-column prop="awardLevel" label="奖项" width="130" />
      <el-table-column prop="teamName" label="团队/成员" width="160" />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除该获奖经历？" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑获奖经历' : '新增获奖经历'" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="比赛名称" prop="competitionName"><el-input v-model="form.competitionName" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="奖项等级"><el-input v-model="form.awardLevel" /></el-form-item>
          <el-form-item label="获奖年份"><el-input-number v-model="form.awardYear" :min="2000" :max="2100" /></el-form-item>
          <el-form-item label="团队/成员"><el-input v-model="form.teamName" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        </div>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
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
import { awardApi } from '../../api'

export default {
  name: 'AdminAwards',
  data() {
    return {
      Plus,
      Refresh,
      loading: false,
      saving: false,
      dialogVisible: false,
      rows: [],
      form: {},
      rules: {
        title: [{ required: true, message: '请输入获奖标题', trigger: 'blur' }],
        competitionName: [{ required: true, message: '请输入比赛名称', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        this.rows = (await awardApi.list()) || []
      } finally {
        this.loading = false
      }
    },
    openDialog(row) {
      this.form = row ? { ...row } : { title: '', competitionName: '', awardLevel: '', awardYear: new Date().getFullYear(), teamName: '', description: '', sortOrder: 0 }
      this.dialogVisible = true
    },
    save() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        this.saving = true
        try {
          if (this.form.id) await awardApi.update(this.form.id, this.form)
          else await awardApi.create(this.form)
          ElMessage.success('保存成功')
          this.dialogVisible = false
          this.fetchList()
        } finally {
          this.saving = false
        }
      })
    },
    async remove(row) {
      await awardApi.remove(row.id)
      ElMessage.success('获奖经历已删除')
      this.fetchList()
    }
  }
}
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}
</style>
