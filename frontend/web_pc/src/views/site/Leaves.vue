<template>
  <div class="site-page">
    <section class="container leave-layout">
      <div class="leave-main">
        <div class="section-head">
          <el-tag effect="plain">请假申请</el-tag>
          <h1>我的请假</h1>
          <p>提交请假理由和附件后，可在这里实时查看审批流程。</p>
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
          <el-form-item label="附件" prop="attachments">
            <el-upload
              :auto-upload="false"
              :file-list="uploadFiles"
              :limit="5"
              :on-change="handleAttachmentChange"
              :on-remove="handleAttachmentRemove"
            >
              <el-button :icon="Upload">上传附件</el-button>
              <template #tip>
                <span class="upload-tip">最多 5 个文件，审批人可在线查看。</span>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item class="submit-row">
            <el-button class="submit-button" type="primary" :loading="saving" @click="submit">提交申请</el-button>
          </el-form-item>
        </el-form>
      </div>

      <aside class="leave-side">
        <div class="side-head">
          <strong>审批流程</strong>
          <el-button link type="primary" :icon="Refresh" @click="fetchMine">刷新</el-button>
        </div>
        <el-empty v-if="!rows.length" :image-size="72" description="暂无请假申请" />
        <div v-for="item in rows" :key="item.id" class="leave-item" :class="{ active: current?.id === item.id }" @click="selectApplication(item)">
          <div class="leave-item__content">
            <strong>{{ item.title }}</strong>
            <p>{{ formatDateTime(item.createdAt) || '-' }}</p>
          </div>
          <el-tag :type="leaveStatusType(item.status)">{{ leaveStatusText(item.status) }}</el-tag>
        </div>
        <div v-if="current" class="flow-panel">
          <h3>{{ current.title }}</h3>
          <el-steps direction="vertical" :active="flowActive" finish-status="success" process-status="process">
            <el-step
              v-for="step in current.approvalFlow || []"
              :key="step.node"
              :title="step.node"
              :description="stepDescription(step)"
            />
          </el-steps>
          <div class="attachment-list">
            <a
              v-for="file in current.attachments || []"
              :key="file.name"
              :href="file.content"
              :download="file.name"
              target="_blank"
            >
              {{ file.name }}
            </a>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import { leaveApplicationApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'

export default {
  name: 'SiteLeaves',
  data() {
    return {
      Refresh,
      Upload,
      rows: [],
      current: null,
      timer: null,
      saving: false,
      uploadFiles: [],
      form: { title: '', reason: '', startAt: '', endAt: '', attachments: [] },
      rules: {
        title: [{ required: true, message: '请输入请假标题', trigger: 'blur' }],
        reason: [{ required: true, message: '请填写请假理由', trigger: 'blur' }],
        attachments: [{ required: true, type: 'array', min: 1, message: '请上传附件', trigger: 'change' }],
      },
    }
  },
  computed: {
    flowActive() {
      if (!this.current) return 0
      if (this.current.status === 'submitted') return 1
      return 3
    },
  },
  created() {
    this.fetchMine()
    this.timer = window.setInterval(this.refreshCurrent, 3000)
  },
  beforeUnmount() {
    if (this.timer) window.clearInterval(this.timer)
  },
  methods: {
    formatDateTime,
    leaveStatusText(status) {
      return { submitted: '待审批', approved: '已通过', rejected: '已驳回' }[status] || status || '-'
    },
    leaveStatusType(status) {
      return { submitted: 'warning', approved: 'success', rejected: 'danger' }[status] || 'info'
    },
    stepDescription(step) {
      const parts = [this.formatDateTime(step.time), step.comment].filter(Boolean)
      return parts.join(' / ')
    },
    async fetchMine() {
      this.rows = (await leaveApplicationApi.mine()) || []
      if (!this.current && this.rows.length) this.current = this.rows[0]
    },
    async refreshCurrent() {
      await this.fetchMine()
      if (!this.current?.id) return
      this.current = await leaveApplicationApi.siteDetail(this.current.id)
    },
    selectApplication(item) {
      this.current = item
      this.refreshCurrent()
    },
    handleAttachmentChange(file, fileList) {
      this.uploadFiles = fileList
      const raw = file.raw
      if (!raw) return false
      const reader = new FileReader()
      reader.onload = () => {
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
          const id = await leaveApplicationApi.create(this.form)
          ElMessage.success('请假申请已提交')
          this.form = { title: '', reason: '', startAt: '', endAt: '', attachments: [] }
          this.uploadFiles = []
          await this.fetchMine()
          this.current = this.rows.find((item) => item.id === id) || this.rows[0] || null
        } finally {
          this.saving = false
        }
      })
    },
  },
}
</script>

<style scoped>
.site-page {
  padding: 42px 0 72px;
}

.leave-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 22px;
  align-items: start;
}

.leave-main,
.leave-side {
  padding: 24px;
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: var(--oa-shadow);
}

.leave-main {
  min-width: 0;
}

.leave-side {
  min-width: 0;
}

.section-head h1 {
  margin: 14px 0 8px;
  font-size: 34px;
}

.section-head p {
  margin: 0;
  line-height: 1.7;
}

.section-head p,
.leave-item p,
.upload-tip {
  color: var(--oa-muted);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.leave-form {
  margin-top: 22px;
}

.leave-form :deep(.el-upload),
.leave-form :deep(.el-upload-dragger),
.leave-form :deep(.el-upload-list) {
  width: 100%;
}

.side-head,
.leave-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.leave-item {
  margin-top: 12px;
  padding: 12px 0;
  cursor: pointer;
  border-bottom: 1px solid #e2e8f0;
}

.leave-item__content {
  min-width: 0;
}

.leave-item strong,
.leave-item p,
.flow-panel h3,
.attachment-list a {
  overflow-wrap: anywhere;
}

.leave-item p {
  margin: 4px 0 0;
}

.leave-item.active strong {
  color: var(--oa-primary);
}

.flow-panel {
  margin-top: 18px;
}

.flow-panel h3 {
  margin: 0 0 14px;
}

.attachment-list {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.attachment-list a {
  color: var(--oa-primary);
  text-decoration: none;
}

@media (max-width: 980px) {
  .leave-layout,
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .site-page {
    padding: 18px 0 40px;
  }

  .leave-layout {
    gap: 14px;
  }

  .leave-main,
  .leave-side {
    padding: 16px;
    border-radius: 14px;
  }

  .section-head h1 {
    margin-top: 10px;
    font-size: 28px;
    line-height: 1.2;
  }

  .leave-form {
    margin-top: 18px;
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

  .submit-row {
    margin-bottom: 0 !important;
  }

  .side-head {
    align-items: center;
  }

  .leave-item {
    align-items: flex-start;
    padding: 14px 0;
  }

  .leave-item :deep(.el-tag) {
    flex: 0 0 auto;
    margin-top: 2px;
  }

  .flow-panel {
    margin-top: 16px;
  }

  .flow-panel :deep(.el-step__description) {
    padding-right: 0;
    line-height: 1.5;
    overflow-wrap: anywhere;
  }

  .attachment-list a {
    padding: 10px 12px;
    border: 1px solid #dbe6f5;
    border-radius: 10px;
    background: #f8fafc;
  }
}
</style>
