<template>
  <div class="admin-page">
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-input
          v-model="query.applicationId"
          clearable
          placeholder="搜索申请ID"
          style="width: 220px"
          @keyup.enter="fetchList"
        />
        <el-select v-model="query.status" clearable placeholder="面试状态" style="width: 150px">
          <el-option label="待确认" value="pending" />
          <el-option label="已确认" value="confirmed" />
          <el-option label="已完成" value="completed" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="applicationId" label="申请ID" width="110" />
      <el-table-column prop="round" label="轮次" width="90" />
      <el-table-column prop="location" label="地点/链接" min-width="160" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.scheduledStartAt) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.scheduledEndAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="confirmInterview(row)">确认</el-button>
          <el-button link type="primary" @click="openFeedback(row)">反馈</el-button>
          <el-button link type="warning" @click="completeInterview(row)">完成</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="feedbackVisible" title="面试反馈" width="520px">
      <el-form :model="feedbackForm" label-width="78px">
        <el-form-item label="评分">
          <el-input-number v-model="feedbackForm.score" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="建议">
          <el-select v-model="feedbackForm.suggestion" style="width: 180px">
            <el-option label="推荐" value="approve" />
            <el-option label="淘汰" value="reject" />
            <el-option label="待定" value="waitlist" />
          </el-select>
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="feedbackForm.comment" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { interviewApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'

export default {
  name: 'AdminInterviews',
  data() {
    return {
      Search,
      loading: false,
      rows: [],
      query: { applicationId: '', status: '' },
      feedbackVisible: false,
      feedbackForm: { interviewId: '', score: 80, suggestion: 'approve', comment: '' },
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    formatDateTime,
    statusType,
    async fetchList() {
      this.loading = true
      try {
        const applicationId = this.query.applicationId
          ? Number(this.query.applicationId)
          : undefined
        const result = await interviewApi.list({
          applicationId: Number.isNaN(applicationId) ? undefined : applicationId,
          status: this.query.status || undefined,
        })
        this.rows = result?.list || result || []
      } finally {
        this.loading = false
      }
    },
    async confirmInterview(row) {
      await interviewApi.confirm(row.id)
      ElMessage.success('面试已确认')
      this.fetchList()
    },
    openFeedback(row) {
      this.feedbackForm = { interviewId: row.id, score: 80, suggestion: 'approve', comment: '' }
      this.feedbackVisible = true
    },
    async submitFeedback() {
      await interviewApi.feedback(this.feedbackForm.interviewId, {
        scores: { score: this.feedbackForm.score },
        suggestion: this.feedbackForm.suggestion,
        comment: this.feedbackForm.comment,
      })
      ElMessage.success('反馈已提交')
      this.feedbackVisible = false
      this.fetchList()
    },
    async completeInterview(row) {
      await interviewApi.complete(row.id)
      ElMessage.success('面试已完成')
      this.fetchList()
    },
  },
}
</script>
