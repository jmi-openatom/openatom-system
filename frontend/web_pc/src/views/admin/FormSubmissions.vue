<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-if="clubs.length > 1"
          v-model="selectedClubId"
          filterable
          placeholder="选择社团"
          style="width: 220px"
          @change="handleClubChange"
        >
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
        </el-select>
        <el-select
          v-model="selectedFormId"
          filterable
          placeholder="选择表单"
          style="width: 260px"
          @change="fetchList"
        >
          <el-option v-for="item in forms" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索提交人/联系方式"
          style="width: 240px"
          @keyup.enter="fetchList"
        />
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button
        type="success"
        :disabled="!selectedFormId"
        :loading="exporting"
        @click="exportExcel"
        >导出 Excel
      </el-button>
    </ViewToolbar>

    <el-alert
      v-if="currentForm"
      type="info"
      show-icon
      :closable="false"
      :title="`当前表单：${currentForm.name}`"
      :description="`收集时间：${formatRange(currentForm.applyStartAt, currentForm.applyEndAt)}`"
    />

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="submitterName" label="提交人" min-width="140">
        <template #default="{ row }">{{ row.submitterName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="submitterAccount" label="账号" min-width="140">
        <template #default="{ row }">{{ row.submitterAccount || '匿名提交' }}</template>
      </el-table-column>
      <el-table-column prop="contact" label="联系方式" min-width="180">
        <template #default="{ row }">{{ row.contact || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" min-width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看内容</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="当前表单暂无提交记录" />

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="detailVisible" title="提交内容" width="760px">
      <div v-if="detailRow" class="detail-panel">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="提交人">{{
            detailRow.submitterName || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="联系方式">{{
            detailRow.contact || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{
            detailRow.submitterAccount || '匿名提交'
          }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{
            formatDateTime(detailRow.createdAt)
          }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">字段内容</el-divider>
        <el-descriptions :column="1" border>
          <el-descriptions-item
            v-for="field in previewEntries"
            :key="field.key"
            :label="field.label"
          >
            <div class="detail-value">{{ field.value }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { clubApi, formSubmissionApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

const clubs = ref<any[]>([])

const forms = ref<any[]>([])

const selectedClubId = ref('')

const selectedFormId = ref('')

const keyword = ref('')

const loading = ref(false)

const exporting = ref(false)

const rows = ref<any[]>([])

const page = ref(1)

const pageSize = ref(10)

const total = ref(0)

const detailVisible = ref(false)

const detailRow = ref<any>(null)

const route = useRoute()

const currentForm = computed(() => {
  return forms.value.find((item) => item.id === selectedFormId.value) || null
})

const currentSchema = computed(() => {
  return parseSchema(currentForm.value?.formSchema)
})

const previewEntries = computed(() => {
  const values = detailRow.value?.formData || {}
  return currentSchema.value.map((field) => ({
    key: field.key,
    label: field.label || field.key,
    value: formatValue(values[field.key]),
  }))
})

async function loadClubs() {
  const result = await clubApi.list({ page: 1, pageSize: 100 })
  clubs.value = result?.list || result || []
  if (!selectedClubId.value && clubs.value.length) {
    selectedClubId.value = clubs.value[0].id
  }
  await handleClubChange(selectedClubId.value)
}

async function handleClubChange(clubId: any) {
  if (!clubId) {
    forms.value = []
    selectedFormId.value = ''
    rows.value = []
    return
  }
  const result = await clubApi.siteForms(clubId)
  forms.value = result?.list || result || []
  const hasCurrent = forms.value.some((item) => item.id === selectedFormId.value)
  if (!hasCurrent) {
    selectedFormId.value = forms.value[0]?.id || ''
  }
  page.value = 1
  await fetchList()
}

async function fetchList() {
  if (!selectedFormId.value) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const result = await formSubmissionApi.list(selectedFormId.value, {
      keyword: keyword.value || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    rows.value = result?.list || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

async function exportExcel() {
  if (!selectedFormId.value) return
  exporting.value = true
  try {
    const blob = await formSubmissionApi.export(selectedFormId.value)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${currentForm.value?.name || '表单记录'}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('Excel 已开始下载')
  } finally {
    exporting.value = false
  }
}

function handlePageChange(page: any) {
  page.value = page
  fetchList()
}

function openDetail(row: any) {
  detailRow.value = row
  detailVisible.value = true
}

function parseSchema(value: any) {
  if (Array.isArray(value)) return value
  if (!value) return []
  try {
    return JSON.parse(value)
  } catch {
    return []
  }
}

function formatValue(value: any) {
  if (Array.isArray(value)) return value.join(' / ')
  if (value && typeof value === 'object') return JSON.stringify(value)
  return value || '-'
}

function formatRange(startAt: any, endAt: any) {
  return `${formatDateTime(startAt) || '-'} 至 ${formatDateTime(endAt) || '-'}`
}

onMounted(async () => {
  const queryClubId = Number(route.query.clubId || '')
  if (queryClubId) {
    selectedClubId.value = queryClubId
  }
  const queryFormId = Number(route.query.formId || '')
  if (queryFormId) {
    selectedFormId.value = queryFormId
  }
  await loadClubs()
})
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-panel {
  display: grid;
  gap: 16px;
}

.detail-value {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
