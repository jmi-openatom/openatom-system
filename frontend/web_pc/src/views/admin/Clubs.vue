<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
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
    </ViewToolbar>
    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="社团名称" min-width="160" />
      <el-table-column prop="code" label="编号" width="120" />
      <el-table-column prop="category" label="分类" width="130" />
      <el-table-column prop="recruitmentStatus" label="招新" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.recruitmentStatus)">{{
            recruitmentStatusText(row.recruitmentStatus)
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ clubStatusText(row.status) }}</el-tag>
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
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { clubApi } from '@/api'
import { clubStatusText, recruitmentStatusText, statusType } from '@/utils/format.ts'
import { onMounted, ref } from 'vue'

const loading = ref(false)

const saving = ref(false)

const dialogVisible = ref(false)

const rows = ref<any[]>([])

const total = ref(0)

const query = ref({ keyword: '', status: '', page: 1, pageSize: 10 })

const form = ref<Record<string, any>>({})

const rules = ref({
  name: [{ required: true, message: '请输入社团名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入社团编号', trigger: 'blur' }],
})

const formRef = ref<any>()

async function fetchList() {
  loading.value = true
  try {
    const result = await clubApi.list(query.value)
    rows.value = result?.list || result || []
    total.value = result?.total || rows.value.length
  } finally {
    loading.value = false
  }
}

function openDialog(row: any) {
  form.value = row
    ? { ...row }
    : { name: '', code: '', category: '', presidentUserId: undefined, description: '' }
  dialogVisible.value = true
}

function saveClub() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (form.value.id) await clubApi.update(form.value.id, form.value)
      else await clubApi.create(form.value)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchList()
    } finally {
      saving.value = false
    }
  })
}

async function changeRecruitment(row: any, status: any) {
  await clubApi.updateRecruitmentStatus(row.id, status)
  ElMessage.success('招新状态已更新')
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
