<template>
  <div class="site-page">
    <section class="container leave-page">
      <div class="leave-main">
        <div class="section-head">
          <el-tag effect="plain">请假申请</el-tag>
          <h1>我的请假</h1>
          <p>提交请假理由和图片后，可查看审批流程。</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" class="leave-form">
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
            <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请说明具体原因" />
          </el-form-item>
          <el-form-item label="图片附件" prop="attachments">
            <el-upload
              accept="image/*"
              :auto-upload="false"
              :file-list="uploadFiles"
              :limit="5"
              list-type="picture-card"
              :on-change="handleAttachmentChange"
              :on-remove="handleAttachmentRemove"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <p class="upload-tip">支持手机拍照或相册选择，最多 5 张，审批人可直接查看原图。</p>
          </el-form-item>
          <el-form-item class="submit-row">
            <el-button class="submit-button" type="primary" :loading="saving" @click="submit">提交申请</el-button>
          </el-form-item>
        </el-form>
      </div>

      <section class="leave-history">
        <div class="history-head">
          <div>
            <strong>请假记录</strong>
            <p>按请假日期归档，提交或删除后会更新列表。</p>
          </div>
          <el-button link type="primary" :icon="Refresh" @click="fetchMine">刷新</el-button>
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
              <el-tag :type="leaveStatusType(item.status)">{{ leaveStatusText(item.status) }}</el-tag>
            </div>

            <p class="reason-text">{{ item.reason }}</p>

            <div v-if="imageAttachments(item).length" class="image-grid">
              <el-image
                v-for="file in imageAttachments(item)"
                :key="file.name"
                class="leave-image"
                fit="cover"
                :src="file.content"
                :preview-src-list="imagePreviewList(item)"
                preview-teleported
              />
            </div>

            <el-steps class="flow-steps" direction="vertical" :active="flowActive(item)" finish-status="success" process-status="process">
              <el-step
                v-for="step in item.approvalFlow || []"
                :key="step.node"
                :title="step.node"
                :description="stepDescription(step)"
              />
            </el-steps>
            <div class="card-actions">
              <el-button link type="danger" @click="deleteLeave(item)">删除记录</el-button>
            </div>
          </article>
        </div>
      </section>
    </section>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { leaveApplicationApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'

export default {
  name: 'SiteLeaves',
  data() {
    return {
      Plus,
      Refresh,
      rows: [],
      saving: false,
      uploadFiles: [],
      form: { title: '', reason: '', startAt: '', endAt: '', attachments: [] },
      rules: {
        title: [{ required: true, message: '请输入请假标题', trigger: 'blur' }],
        reason: [{ required: true, message: '请填写请假理由', trigger: 'blur' }],
        attachments: [{ required: true, type: 'array', min: 1, message: '请上传图片附件', trigger: 'change' }],
      },
    }
  },
  computed: {
    groupedRows() {
      const groups = new Map()
      ;(this.rows || []).forEach((item) => {
        const key = this.dateKey(item.startAt || item.createdAt)
        if (!groups.has(key)) groups.set(key, { date: key, label: this.dateLabel(item.startAt || item.createdAt), items: [] })
        groups.get(key).items.push(item)
      })
      return Array.from(groups.values())
    },
  },
  created() {
    this.fetchMine()
  },
  methods: {
    formatDateTime,
    leaveStatusText(status) {
      return { submitted: '待审批', approved: '已通过', rejected: '已驳回' }[status] || status || '-'
    },
    leaveStatusType(status) {
      return { submitted: 'warning', approved: 'success', rejected: 'danger' }[status] || 'info'
    },
    flowActive(item) {
      if (!item) return 0
      return item.status === 'submitted' ? 1 : 3
    },
    formatRange(startAt, endAt) {
      return `${formatDateTime(startAt) || '不限'} - ${formatDateTime(endAt) || '不限'}`
    },
    stepDescription(step) {
      const parts = [this.formatDateTime(step.time), step.comment].filter(Boolean)
      return parts.join(' / ')
    },
    dateKey(value) {
      if (!value) return 'unknown'
      return String(value).slice(0, 10)
    },
    dateLabel(value) {
      const key = this.dateKey(value)
      return key === 'unknown' ? '未设置日期' : key
    },
    imageAttachments(item) {
      return (item.attachments || []).filter((file) => this.isImage(file))
    },
    imagePreviewList(item) {
      return this.imageAttachments(item).map((file) => file.content)
    },
    isImage(file) {
      return String(file?.type || '').startsWith('image/') || String(file?.content || '').startsWith('data:image/')
    },
    async fetchMine() {
      this.rows = (await leaveApplicationApi.mine()) || []
    },
    handleAttachmentChange(file, fileList) {
      const raw = file.raw
      if (!raw) return false
      if (!raw.type.startsWith('image/')) {
        ElMessage.error('请上传图片文件')
        this.uploadFiles = fileList.filter((item) => item.uid !== file.uid)
        return false
      }
      this.uploadFiles = fileList
      const reader = new FileReader()
      reader.onload = () => {
        file.url = reader.result
        this.uploadFiles = fileList
        const next = this.form.attachments.filter((item) => item.name !== raw.name)
        next.push({ name: raw.name, type: raw.type, size: raw.size, content: reader.result })
        this.form.attachments = next
      }
      reader.readAsDataURL(raw)
      return false
    },
    handleAttachmentRemove(file, fileList) {
      this.uploadFiles = fileList
      const name = file.name
      this.form.attachments = this.form.attachments.filter((item) => item.name !== name)
    },
    submit() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        this.saving = true
        try {
          await leaveApplicationApi.create(this.form)
          ElMessage.success('请假申请已提交')
          this.form = { title: '', reason: '', startAt: '', endAt: '', attachments: [] }
          this.uploadFiles = []
          await this.fetchMine()
        } finally {
          this.saving = false
        }
      })
    },
    async deleteLeave(item) {
      await ElMessageBox.confirm(`确定删除“${item.title}”这条请假记录吗？`, '删除请假记录', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
      await leaveApplicationApi.siteRemove(item.id)
      ElMessage.success('请假记录已删除')
      await this.fetchMine()
    },
  },
}
</script>

<style scoped>
.site-page {
  padding: 28px 0 64px;
}

.leave-page {
  display: grid;
  gap: 18px;
}

.leave-main,
.leave-history {
  min-width: 0;
  padding: 24px;
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--oa-shadow);
}

.section-head h1 {
  margin: 14px 0 8px;
  font-size: 34px;
  line-height: 1.2;
}

.section-head p,
.history-head p,
.leave-card p,
.upload-tip,
.date-title small {
  color: var(--oa-muted);
}

.section-head p,
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
  border-bottom: 1px solid #e2e8f0;
}

.leave-card {
  margin-top: 12px;
  padding: 16px;
  border: 1px solid #dbe6f5;
  border-radius: 12px;
  background: #fff;
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
  border: 1px solid #dbe6f5;
  border-radius: 10px;
  background: #f8fafc;
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
  .site-page {
    padding: 14px 0 36px;
  }

  .leave-main,
  .leave-history {
    padding: 16px;
    border-radius: 14px;
  }

  .section-head h1 {
    margin-top: 10px;
    font-size: 28px;
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
