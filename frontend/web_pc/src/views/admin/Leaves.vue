<template>
  <div class="admin-page">
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-select v-model="query.status" clearable placeholder="审批状态" style="width: 150px" @change="fetchList">
          <el-option label="待审批" value="submitted" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column label="申请人" min-width="160">
        <template #default="{ row }">
          <strong>{{ row.realName || row.userName || '-' }}</strong>
          <p class="muted-line">{{ row.studentId || '-' }}</p>
        </template>
      </el-table-column>
      <el-table-column label="请假内容" min-width="240">
        <template #default="{ row }">
          <strong>{{ row.title }}</strong>
          <p class="muted-line">{{ row.reason }}</p>
        </template>
      </el-table-column>
      <el-table-column label="时间" min-width="220">
        <template #default="{ row }">{{ formatRange(row.startAt, row.endAt) }}</template>
      </el-table-column>
      <el-table-column label="附件" width="90">
        <template #default="{ row }">{{ (row.attachments || []).length }} 个</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="leaveStatusType(row.status)">{{ leaveStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看流程</el-button>
          <el-button v-if="row.status === 'submitted'" link type="success" @click="openReview(row, 'approve')">通过</el-button>
          <el-button v-if="row.status === 'submitted'" link type="danger" @click="openReview(row, 'reject')">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="暂无请假申请" />

    <el-dialog v-model="detailVisible" title="请假审批流程" width="760px">
      <template v-if="current">
        <div class="detail-head">
          <div>
            <h3>{{ current.title }}</h3>
            <p>{{ current.realName || current.userName || '-' }} / {{ current.studentId || '-' }}</p>
          </div>
          <el-tag :type="leaveStatusType(current.status)">{{ leaveStatusText(current.status) }}</el-tag>
        </div>
        <p class="reason-text">{{ current.reason }}</p>
        <el-steps direction="vertical" :active="flowActive" finish-status="success" process-status="process">
          <el-step
            v-for="step in current.approvalFlow || []"
            :key="step.node"
            :title="step.node"
            :description="stepDescription(step)"
          />
        </el-steps>
        <el-divider content-position="left">附件</el-divider>
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
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" :title="reviewForm.action === 'approve' ? '通过请假' : '驳回请假'" width="520px">
      <el-form label-width="86px">
        <el-form-item label="审批意见">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="4" placeholder="可填写审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button :type="reviewForm.action === 'approve' ? 'success' : 'danger'" :loading="reviewing" @click="submitReview">
          确认{{ reviewForm.action === 'approve' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { leaveApplicationApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'

export default {
  name: 'AdminLeaves',
  data() {
    return {
      Refresh,
      loading: false,
      reviewing: false,
      detailVisible: false,
      reviewVisible: false,
      rows: [],
      current: null,
      timer: null,
      query: { status: '' },
      reviewForm: { id: null, action: 'approve', comment: '' },
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
    this.fetchList()
    this.timer = window.setInterval(this.refreshOpenDetail, 3000)
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
    formatRange(startAt, endAt) {
      return `${formatDateTime(startAt) || '不限'} - ${formatDateTime(endAt) || '不限'}`
    },
    stepDescription(step) {
      const parts = [this.formatDateTime(step.time), step.comment].filter(Boolean)
      return parts.join(' / ')
    },
    async fetchList() {
      this.loading = true
      try {
        this.rows = (await leaveApplicationApi.list(this.query)) || []
      } finally {
        this.loading = false
      }
    },
    async openDetail(row) {
      this.current = await leaveApplicationApi.detail(row.id)
      this.detailVisible = true
    },
    async refreshOpenDetail() {
      await this.fetchList()
      if (this.detailVisible && this.current?.id) {
        this.current = await leaveApplicationApi.detail(this.current.id)
      }
    },
    openReview(row, action) {
      this.reviewForm = { id: row.id, action, comment: '' }
      this.reviewVisible = true
    },
    async submitReview() {
      if (!this.reviewForm.id) return
      this.reviewing = true
      try {
        await leaveApplicationApi.review(this.reviewForm.id, {
          action: this.reviewForm.action,
          comment: this.reviewForm.comment,
        })
        ElMessage.success(this.reviewForm.action === 'approve' ? '已通过' : '已驳回')
        this.reviewVisible = false
        await this.fetchList()
        if (this.current?.id === this.reviewForm.id) {
          this.current = await leaveApplicationApi.detail(this.reviewForm.id)
        }
      } finally {
        this.reviewing = false
      }
    },
  },
}
</script>

<style scoped>
.muted-line {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.detail-head h3 {
  margin: 0 0 6px;
}

.detail-head p,
.reason-text {
  color: var(--oa-muted);
}

.reason-text {
  margin: 12px 0 18px;
  line-height: 1.7;
}

.attachment-list {
  display: grid;
  gap: 8px;
}

.attachment-list a {
  color: var(--oa-primary);
  text-decoration: none;
}
</style>
