<template>
  <ViewPage class="site-system-page">
    <SitePageHero
      compact
      description="提交请假理由和图片后，可查看审批流程。"
      eyebrow="请假申请"
      title="我的请假"
    />

    <section class="site-system-section">
      <div class="container leave-page">
        <div class="leave-main site-system-surface site-reveal">
          <SiteSectionHeading
            align="left"
            description="按要求填写时间、理由和图片附件。"
            eyebrow="新申请"
            title="提交请假"
          />
          <el-form ref="formRef" :model="form" :rules="rules" class="leave-form" label-width="92px">
            <div class="form-grid">
              <el-form-item label="请假标题" prop="title">
                <el-input v-model="form.title" placeholder="如：例会请假" />
              </el-form-item>
              <el-form-item label="开始时间">
                <el-input v-model="form.startAt" type="datetime-local" />
              </el-form-item>
              <el-form-item label="结束时间">
                <el-input v-model="form.endAt" type="datetime-local" />
              </el-form-item>
            </div>
            <el-form-item label="请假理由" prop="reason">
              <el-input
                v-model="form.reason"
                :rows="4"
                placeholder="请说明具体原因"
                type="textarea"
              />
            </el-form-item>
            <el-form-item label="图片附件" prop="attachments">
              <el-upload
                :auto-upload="false"
                :file-list="uploadFiles"
                :limit="5"
                :on-change="handleAttachmentChange"
                :on-remove="handleAttachmentRemove"
                accept="image/*"
                list-type="picture-card"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
              <p class="upload-tip">支持手机拍照或相册选择，最多 5 张，审批人可直接查看原图。</p>
            </el-form-item>
            <el-form-item class="submit-row">
              <el-button :loading="saving" class="submit-button" type="primary" @click="submit"
                >提交申请</el-button
              >
            </el-form-item>
          </el-form>
        </div>

        <section class="leave-history site-system-surface site-reveal">
          <div class="history-head">
            <div>
              <strong>请假记录</strong>
              <p>按请假日期归档，提交或删除后会更新列表。</p>
            </div>
            <el-button :icon="Refresh" link type="primary" @click="fetchMine">刷新</el-button>
          </div>

          <el-empty v-if="!rows.length" :image-size="78" description="暂无请假申请" />

          <div v-for="group in groupedRows" :key="group.date" class="date-group">
            <div class="date-title">
              <span>{{ group.label }}</span>
              <small>{{ group.items.length }} 条</small>
            </div>

            <article v-for="item in group.items" :key="item.id" class="leave-card">
              <div class="leave-card__top">
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ formatRange(item.startAt, item.endAt) }}</p>
                </div>
                <el-tag :type="leaveStatusType(item.status)">{{
                  leaveStatusText(item.status)
                }}</el-tag>
              </div>

              <p class="reason-text">{{ item.reason }}</p>
              <p v-if="item.status === 'rejected'" class="reject-reason">
                <strong>驳回理由：</strong>{{ rejectionReason(item) }}
              </p>

              <div v-if="imageAttachments(item).length" class="image-grid">
                <el-image
                  v-for="file in imageAttachments(item)"
                  :key="file.name"
                  :preview-src-list="imagePreviewList(item)"
                  :src="file.content"
                  class="leave-image"
                  fit="cover"
                  preview-teleported
                />
              </div>

              <el-steps
                :active="flowActive(item)"
                class="flow-steps"
                direction="vertical"
                finish-status="success"
                process-status="process"
              >
                <el-step
                  v-for="step in item.approvalFlow || []"
                  :key="step.node"
                  :description="stepDescription(step)"
                  :title="step.node"
                />
              </el-steps>
              <div class="card-actions">
                <el-button link type="danger" @click="deleteLeave(item)">删除记录</el-button>
              </div>
            </article>
          </div>
        </section>
      </div>
    </section>
  </ViewPage>
</template>

<script lang="ts" setup>
import ViewPage from '@/components/common/ViewPage.vue'
import SitePageHero from '@/components/site/shell/SitePageHero.vue'
import SiteSectionHeading from '@/components/site/shell/SiteSectionHeading.vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Plus, Refresh} from '@element-plus/icons-vue'
import {leaveApplicationApi} from '@/api'
import {formatDateTime} from '@/utils/format.ts'
import {computed, onMounted, ref} from 'vue'

const rows = ref<any[]>([])

const saving = ref(false)

const uploadFiles = ref<any[]>([])

const form = ref({ title: '', reason: '', startAt: '', endAt: '', attachments: [] })

const rules = ref({
  title: [{ required: true, message: '请输入请假标题', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写请假理由', trigger: 'blur' }],
  attachments: [
    { required: true, type: 'array', min: 1, message: '请上传图片附件', trigger: 'change' },
  ],
})

const formRef = ref<any>()

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

function rejectionReason(item: any) {
  return item?.reviewComment || '未填写驳回理由'
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
  return (item.attachments || []).filter((file: any) => isImage(file))
}

function imagePreviewList(item: any) {
  return imageAttachments(item).map((file: { content: any }) => file.content)
}

function isImage(file: any) {
  return (
    String(file?.type || '').startsWith('image/') ||
    String(file?.content || '').startsWith('data:image/')
  )
}

async function fetchMine() {
  rows.value = (await leaveApplicationApi.mine()) || []
}

function handleAttachmentChange(file: any, fileList: any) {
  const raw = file.raw
  if (!raw) return false
  if (!raw.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    uploadFiles.value = fileList.filter((item) => item.uid !== file.uid)
    return false
  }
  uploadFiles.value = fileList
  const reader = new FileReader()
  reader.onload = () => {
    file.url = reader.result
    uploadFiles.value = fileList
    const next = form.value.attachments.filter((item) => item.name !== raw.name)
    next.push({ name: raw.name, type: raw.type, size: raw.size, content: reader.result })
    form.value.attachments = next
  }
  reader.readAsDataURL(raw)
  return false
}

function handleAttachmentRemove(file: any, fileList: any) {
  uploadFiles.value = fileList
  const name = file.name
  form.value.attachments = form.value.attachments.filter((item) => item.name !== name)
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await leaveApplicationApi.create(form.value)
      ElMessage.success('请假申请已提交')
      form.value = { title: '', reason: '', startAt: '', endAt: '', attachments: [] }
      uploadFiles.value = []
      await fetchMine()
    } finally {
      saving.value = false
    }
  })
}

async function deleteLeave(item: any) {
  await ElMessageBox.confirm(`确定删除“${item.title}”这条请假记录吗？`, '删除请假记录', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await leaveApplicationApi.siteRemove(item.id)
  ElMessage.success('请假记录已删除')
  await fetchMine()
}

onMounted(() => {
  fetchMine()
})
</script>

<style scoped>
.leave-page {
  display: grid;
  gap: 24px;
}

.leave-main,
.leave-history {
  min-width: 0;
  padding: clamp(22px, 3vw, 32px);
}

.history-head p,
.leave-card p,
.upload-tip,
.date-title small {
  color: var(--oa-muted);
}

.history-head p {
  margin: 0;
  line-height: 1.7;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0;
  max-width: 760px;
}

.leave-form {
  margin-top: 22px;
  max-width: 860px;
}

.upload-tip {
  flex-basis: 100%;
  margin: 8px 0 0;
  font-size: 13px;
}

.submit-button {
  min-width: 150px;
}

.history-head,
.leave-card__top,
.date-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.history-head strong,
.date-title span {
  font-size: 18px;
}

.date-group {
  margin-top: 18px;
}

.date-title {
  align-items: center;
  padding: 0 4px 10px;
  border-bottom: 1px solid var(--oa-border);
}

.leave-card {
  margin-top: 12px;
  padding: 16px;
  border: 1px solid var(--oa-border);
  border-radius: 16px;
  background: var(--oa-elevated-bg);
}

.leave-card__top strong,
.flow-steps :deep(.el-step__title),
.attachment-list a {
  overflow-wrap: anywhere;
}

.leave-card__top p,
.reason-text {
  margin: 6px 0 0;
}

.reason-text {
  line-height: 1.7;
}

.reject-reason {
  margin: 10px 0 0;
  padding: 10px 12px;
  border: 1px solid rgba(245, 108, 108, 0.28);
  border-radius: 8px;
  color: var(--el-color-danger);
  background: rgba(245, 108, 108, 0.08);
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.leave-image {
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 10px;
  background: var(--oa-page-soft-bg);
}

.flow-steps {
  margin-top: 16px;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.flow-steps :deep(.el-step__description) {
  padding-right: 0;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

@media (max-width: 760px) {
  .leave-main,
  .leave-history {
    padding: 16px;
  }

  .leave-form :deep(.el-form-item) {
    display: block;
    margin-bottom: 16px;
  }

  .leave-form :deep(.el-form-item__label) {
    display: block;
    width: 100% !important;
    height: auto;
    margin-bottom: 6px;
    padding: 0;
    line-height: 1.4;
    text-align: left;
  }

  .leave-form :deep(.el-form-item__content) {
    display: block;
    margin-left: 0 !important;
    width: 100%;
  }

  .leave-form :deep(.el-input),
  .leave-form :deep(.el-textarea),
  .submit-button {
    width: 100%;
  }

  .leave-form :deep(.el-upload--picture-card),
  .leave-form :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 86px;
    height: 86px;
  }

  .submit-row {
    margin-bottom: 0 !important;
  }

  .history-head,
  .leave-card__top {
    align-items: flex-start;
  }

  .leave-card__top :deep(.el-tag) {
    flex: 0 0 auto;
    margin-top: 2px;
  }

  .image-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
