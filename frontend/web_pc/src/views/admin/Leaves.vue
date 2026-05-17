<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-model="query.status"
          clearable
          placeholder="审批状态"
          style="width: 150px"
          @change="fetchList"
        >
          <el-option label="待审批" value="submitted" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
    </ViewToolbar>

    <el-empty v-if="!loading && !rows.length" description="暂无请假申请" />

    <div v-loading="loading" class="leave-groups">
      <section v-for="group in groupedRows" :key="group.date" class="date-group">
        <div class="date-title">
          <div>
            <strong>{{ group.label }}</strong>
            <p>{{ group.items.length }} 条请假申请</p>
          </div>
        </div>

        <div class="leave-card-list">
          <article v-for="row in group.items" :key="row.id" class="leave-card">
            <div class="leave-card__top">
              <div>
                <strong>{{ row.title }}</strong>
                <p>{{ row.realName || row.userName || '-' }} / {{ row.studentId || '-' }}</p>
              </div>
              <el-tag :type="leaveStatusType(row.status)">{{ leaveStatusText(row.status) }}</el-tag>
            </div>

            <p class="reason-text">{{ row.reason }}</p>
            <p class="muted-line">{{ formatRange(row.startAt, row.endAt) }}</p>

            <div v-if="imageAttachments(row).length" class="image-grid">
              <el-image
                v-for="file in imageAttachments(row)"
                :key="file.name"
                class="leave-image"
                fit="cover"
                :src="file.content"
                :preview-src-list="imagePreviewList(row)"
                preview-teleported
              />
            </div>

            <div class="flow-strip">
              <span
                v-for="step in row.approvalFlow || []"
                :key="step.node"
                :class="`flow-dot flow-dot--${step.status}`"
              >
                {{ step.node }}
              </span>
            </div>

            <div class="card-actions">
              <el-button link type="primary" @click="openDetail(row)">查看流程</el-button>
              <el-button
                v-if="row.status === 'submitted'"
                link
                type="success"
                @click="openReview(row, 'approve')"
                >通过</el-button
              >
              <el-button
                v-if="row.status === 'submitted'"
                link
                type="danger"
                @click="openReview(row, 'reject')"
                >驳回</el-button
              >
              <el-button link type="danger" @click="deleteLeave(row)">删除</el-button>
            </div>
          </article>
        </div>
      </section>
    </div>

    <el-dialog v-model="detailVisible" title="请假审批流程" width="760px">
      <template v-if="current">
        <div class="detail-head">
          <div>
            <h3>{{ current.title }}</h3>
            <p>
              {{ current.realName || current.userName || '-' }} / {{ current.studentId || '-' }}
            </p>
          </div>
          <el-tag :type="leaveStatusType(current.status)">{{
            leaveStatusText(current.status)
          }}</el-tag>
        </div>
        <p class="reason-text">{{ current.reason }}</p>
        <el-steps
          direction="vertical"
          :active="flowActive(current)"
          finish-status="success"
          process-status="process"
        >
          <el-step
            v-for="step in current.approvalFlow || []"
            :key="step.node"
            :title="step.node"
            :description="stepDescription(step)"
          />
        </el-steps>
        <el-divider content-position="left">图片附件</el-divider>
        <div class="dialog-image-grid">
          <el-image
            v-for="file in imageAttachments(current)"
            :key="file.name"
            class="dialog-image"
            fit="cover"
            :src="file.content"
            :preview-src-list="imagePreviewList(current)"
            preview-teleported
          />
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reviewVisible"
      :title="reviewForm.action === 'approve' ? '通过请假' : '驳回请假'"
      width="520px"
    >
      <el-form label-width="86px">
        <el-form-item label="审批意见">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            placeholder="可填写审批意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button
          :type="reviewForm.action === 'approve' ? 'success' : 'danger'"
          :loading="reviewing"
          @click="submitReview"
        >
          确认{{ reviewForm.action === 'approve' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { leaveApplicationApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { computed, onMounted, ref } from 'vue'

const loading = ref(false)

const reviewing = ref(false)

const detailVisible = ref(false)

const reviewVisible = ref(false)

const rows = ref<any[]>([])

const current = ref<any>(null)

const query = ref({ status: '' })

const reviewForm = ref({ id: null, action: 'approve', comment: '' })

const groupedRows = computed(() => {
  const groups = new Map()
  ;(rows.value || []).forEach((item) => {
    const key = dateKey(item.startAt || item.createdAt)
    if (!groups.has(key))
      groups.set(key, { date: key, label: dateLabel(item.startAt || item.createdAt), items: [] })
    groups.get(key).items.push(item)
  })
  return Array.from(groups.values())
})

function leaveStatusText(status: any) {
  return { submitted: '待审批', approved: '已通过', rejected: '已驳回' }[status] || status || '-'
}

function leaveStatusType(status: any) {
  return { submitted: 'warning', approved: 'success', rejected: 'danger' }[status] || 'info'
}

function flowActive(item: any) {
  if (!item) return 0
  return item.status === 'submitted' ? 1 : 3
}

function formatRange(startAt: any, endAt: any) {
  return `${formatDateTime(startAt) || '不限'} - ${formatDateTime(endAt) || '不限'}`
}

function stepDescription(step: any) {
  const parts = [formatDateTime(step.time), step.comment].filter(Boolean)
  return parts.join(' / ')
}

function dateKey(value: any) {
  if (!value) return 'unknown'
  return String(value).slice(0, 10)
}

function dateLabel(value: any) {
  const key = dateKey(value)
  return key === 'unknown' ? '未设置日期' : key
}

function imageAttachments(item: any) {
  return (item.attachments || []).filter((file) => isImage(file))
}

function imagePreviewList(item: any) {
  return imageAttachments(item).map((file) => file.content)
}

function isImage(file: any) {
  return (
    String(file?.type || '').startsWith('image/') ||
    String(file?.content || '').startsWith('data:image/')
  )
}

async function fetchList() {
  loading.value = true
  try {
    rows.value = (await leaveApplicationApi.list(query.value)) || []
  } finally {
    loading.value = false
  }
}

async function openDetail(row: any) {
  current.value = await leaveApplicationApi.detail(row.id)
  detailVisible.value = true
}

function openReview(row: any, action: any) {
  reviewForm.value = { id: row.id, action, comment: '' }
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewForm.value.id) return
  reviewing.value = true
  try {
    await leaveApplicationApi.review(reviewForm.value.id, {
      action: reviewForm.value.action,
      comment: reviewForm.value.comment,
    })
    ElMessage.success(reviewForm.value.action === 'approve' ? '已通过' : '已驳回')
    reviewVisible.value = false
    await fetchList()
    if (current.value?.id === reviewForm.value.id) {
      current.value = await leaveApplicationApi.detail(reviewForm.value.id)
    }
  } finally {
    reviewing.value = false
  }
}

async function deleteLeave(row: any) {
  await ElMessageBox.confirm(`确定删除“${row.title}”这条请假记录吗？`, '删除请假记录', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await leaveApplicationApi.remove(row.id)
  ElMessage.success('请假记录已删除')
  if (current.value?.id === row.id) {
    detailVisible.value = false
    current.value = null
  }
  await fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.muted-line,
.date-title p,
.detail-head p {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.leave-groups {
  display: grid;
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  background: var(--oa-border);
}

.date-group {
  padding: 24px;
  border: 0;
  border-radius: 0;
  background: var(--oa-elevated-bg);
  box-shadow: none;
  animation: oaFadeUp 0.42s ease both;
}

.date-title strong {
  font-size: 18px;
}

.leave-card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.leave-card {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
  animation: oaFadeUp 0.34s ease both;
}

.leave-card__top,
.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.leave-card__top strong,
.detail-head h3 {
  overflow-wrap: anywhere;
}

.reason-text {
  margin: 12px 0 0;
  color: var(--oa-muted);
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.image-grid,
.dialog-image-grid {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.image-grid {
  grid-template-columns: repeat(auto-fill, minmax(76px, 1fr));
}

.dialog-image-grid {
  grid-template-columns: repeat(auto-fill, minmax(132px, 1fr));
}

.leave-image,
.dialog-image {
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 10px;
  background: var(--oa-page-soft-bg);
}

.flow-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.flow-dot {
  padding: 5px 10px;
  border-radius: 999px;
  color: var(--oa-muted);
  background: var(--oa-button-subtle-bg);
  font-size: 12px;
}

.flow-dot--done {
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
}

.flow-dot--process {
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.detail-head h3 {
  margin: 0 0 6px;
}

@media (max-width: 760px) {
  .date-group {
    padding: 14px;
  }

  .leave-card-list {
    grid-template-columns: 1fr;
  }

  .leave-card__top,
  .detail-head {
    gap: 10px;
  }

  .leave-card__top :deep(.el-tag),
  .detail-head :deep(.el-tag) {
    flex: 0 0 auto;
  }

  .image-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .card-actions {
    justify-content: flex-start;
  }
}
</style>
