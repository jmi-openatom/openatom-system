<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索应用、申请人或联系方式"
          style="width: 260px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px" @change="reload">
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="reload">刷新</el-button>
      </div>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table data-open-table">
      <el-table-column label="应用" min-width="230">
        <template #default="{ row }">
          <strong>{{ row.appName }}</strong>
          <p class="muted-line">{{ row.organization || '未填写组织' }}</p>
        </template>
      </el-table-column>
      <el-table-column label="申请人" min-width="190">
        <template #default="{ row }">
          <strong>{{ row.applicantName }}</strong>
          <p class="muted-line">{{ row.applicantContact }}</p>
        </template>
      </el-table-column>
      <el-table-column label="使用场景" min-width="260">
        <template #default="{ row }">
          <p class="purpose-text">{{ row.purpose }}</p>
        </template>
      </el-table-column>
      <el-table-column label="密钥" min-width="280">
        <template #default="{ row }">
          <div v-if="row.apiKey" class="key-cell">
            <code>{{ row.apiKey }}</code>
            <el-button link type="primary" @click="copyText(row.apiKey)">复制</el-button>
          </div>
          <span v-else class="muted-line">审核通过后生成</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="调用" width="130">
        <template #default="{ row }">
          <strong>{{ Number(row.callCount || 0) }}</strong>
          <p class="muted-line">{{ formatDateTime(row.lastUsedAt) || '未调用' }}</p>
        </template>
      </el-table-column>
      <el-table-column label="审核" min-width="180">
        <template #default="{ row }">
          <span>{{ row.reviewerName || '-' }}</span>
          <p class="muted-line">{{ formatDateTime(row.reviewedAt) || row.reviewComment || '-' }}</p>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" :disabled="row.status === 'approved'" @click="openReview(row, 'approved')">
            通过
          </el-button>
          <el-button link type="danger" :disabled="row.status === 'rejected'" @click="openReview(row, 'rejected')">
            驳回
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!rows.length && !loading" description="暂无开放平台申请" />

    <el-pagination
      v-if="total > query.pageSize"
      class="pager"
      background
      layout="prev, pager, next"
      :current-page="query.page"
      :page-size="query.pageSize"
      :total="total"
      @current-change="handlePageChange"
    />

    <el-dialog v-model="reviewDialog.visible" :title="reviewDialog.status === 'approved' ? '通过申请' : '驳回申请'" width="520px">
      <el-form label-position="top">
        <el-form-item label="应用名称">
          <el-input :model-value="reviewDialog.row?.appName" readonly />
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewDialog.reviewComment"
            maxlength="500"
            :rows="4"
            show-word-limit
            type="textarea"
            :placeholder="reviewDialog.status === 'approved' ? '可填写密钥发放说明' : '请填写驳回原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button :type="reviewDialog.status === 'approved' ? 'primary' : 'danger'" :loading="reviewing" @click="submitReview">
          确认{{ reviewDialog.status === 'approved' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { dataOpenApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Refresh } from '@element-plus/icons-vue'
import { onMounted, reactive, ref } from 'vue'

const loading = ref(false)
const reviewing = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const query = ref({
  keyword: '',
  status: '',
  page: 1,
  pageSize: 10,
})

const reviewDialog = reactive({
  visible: false,
  row: null as any,
  status: 'approved',
  reviewComment: '',
})

async function fetchList() {
  loading.value = true
  try {
    const data = await dataOpenApi.adminList({
      keyword: query.value.keyword || undefined,
      status: query.value.status || undefined,
      page: query.value.page,
      pageSize: query.value.pageSize,
    })
    rows.value = data?.list || []
    total.value = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  query.value.page = 1
  fetchList()
}

function handlePageChange(page: number) {
  query.value.page = page
  fetchList()
}

function openReview(row: any, status: string) {
  reviewDialog.visible = true
  reviewDialog.row = row
  reviewDialog.status = status
  reviewDialog.reviewComment = row.reviewComment || ''
}

async function submitReview() {
  if (reviewDialog.status === 'rejected' && !reviewDialog.reviewComment.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  reviewing.value = true
  try {
    await dataOpenApi.review(reviewDialog.row.id, {
      status: reviewDialog.status,
      reviewComment: reviewDialog.reviewComment,
    })
    ElMessage.success(reviewDialog.status === 'approved' ? '已通过并生成密钥' : '已驳回申请')
    reviewDialog.visible = false
    fetchList()
  } finally {
    reviewing.value = false
  }
}

async function copyText(value: string) {
  if (!value) return
  try {
    await navigator.clipboard?.writeText(value)
    ElMessage.success('密钥已复制')
  } catch (_error) {
    ElMessage.warning('当前浏览器不允许直接复制，请手动选择密钥')
  }
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
  }
  return map[status] || status || '-'
}

function statusType(status: string) {
  if (status === 'approved') return 'success'
  if (status === 'rejected') return 'danger'
  return 'warning'
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.data-open-table :deep(.cell) {
  line-height: 1.5;
}

.muted-line {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.purpose-text {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--oa-muted);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.key-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.key-cell code {
  min-width: 0;
  max-width: 210px;
  overflow: hidden;
  padding: 4px 6px;
  color: var(--oa-text);
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: 6px;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
