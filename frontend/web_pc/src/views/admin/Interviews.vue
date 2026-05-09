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
      <div class="toolbar__actions">
        <el-button
          type="success"
          :disabled="!batchConfirmCandidates.length"
          @click="openBatchConfirm"
          >批量确认</el-button
        >
        <el-button
          type="primary"
          :disabled="!batchFeedbackCandidates.length"
          @click="openBatchFeedback"
          >批量反馈</el-button
        >
        <el-button
          type="warning"
          :disabled="!batchCompleteCandidates.length"
          @click="openBatchComplete"
          >批量完成</el-button
        >
      </div>
    </div>
    <el-table
      v-loading="loading"
      :data="rows"
      class="admin-table"
      @selection-change="selection = $event"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="applicationId" label="申请ID" width="110" />
      <el-table-column prop="round" label="轮次" width="90" />
      <el-table-column prop="location" label="地点/链接" min-width="160" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ interviewStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.scheduledStartAt) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.scheduledEndAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'pending'"
            link
            type="success"
            @click="confirmInterview(row)"
            >确认</el-button
          >
          <el-button link type="primary" @click="openFeedback(row)">反馈</el-button>
          <el-button link type="info" @click="viewFeedbacks(row)">查看反馈</el-button>
          <el-button
            v-if="row.status !== 'completed'"
            link
            type="warning"
            @click="openCompleteConfirm(row)"
            >完成</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 批量确认对话框 -->
    <el-dialog v-model="batchConfirmVisible" title="批量确认面试" width="620px">
      <el-form :model="batchConfirmForm" label-width="98px">
        <el-form-item label="处理人数">
          <el-input :model-value="String(batchConfirmCandidates.length)" disabled />
        </el-form-item>
      </el-form>
      <el-divider content-position="left">涉及面试</el-divider>
      <el-table :data="batchConfirmCandidates" max-height="280">
        <el-table-column prop="id" label="面试ID" width="90" />
        <el-table-column prop="applicationId" label="申请ID" width="90" />
        <el-table-column prop="round" label="轮次" width="72" />
        <el-table-column prop="location" label="地点" min-width="140" />
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.scheduledStartAt) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="batchConfirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchConfirmSubmitting" @click="submitBatchConfirm"
          >确认</el-button
        >
      </template>
    </el-dialog>

    <!-- 批量反馈对话框 -->
    <el-dialog v-model="batchFeedbackVisible" title="批量反馈" width="640px">
      <el-form :model="batchFeedbackForm" label-width="98px">
        <el-form-item label="处理人数">
          <el-input :model-value="String(batchFeedbackCandidates.length)" disabled />
        </el-form-item>
        <el-form-item label="沟通能力">
          <el-rate v-model="batchFeedbackForm.scores.communication" :max="5" show-score />
        </el-form-item>
        <el-form-item label="专业能力">
          <el-rate v-model="batchFeedbackForm.scores.professional" :max="5" show-score />
        </el-form-item>
        <el-form-item label="综合素质">
          <el-rate v-model="batchFeedbackForm.scores.overall" :max="5" show-score />
        </el-form-item>
        <el-form-item label="建议">
          <el-radio-group v-model="batchFeedbackForm.suggestion">
            <el-radio-button value="approve">推荐通过</el-radio-button>
            <el-radio-button value="waitlist">待定</el-radio-button>
            <el-radio-button value="reject">淘汰</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="统一评语">
          <el-input
            v-model="batchFeedbackForm.comment"
            type="textarea"
            :rows="3"
            placeholder="批量反馈的统一点评（可选）"
          />
        </el-form-item>
      </el-form>
      <el-divider content-position="left">涉及面试</el-divider>
      <el-table :data="batchFeedbackCandidates" max-height="240">
        <el-table-column prop="id" label="面试ID" width="80" />
        <el-table-column prop="applicationId" label="申请ID" width="90" />
        <el-table-column prop="round" label="轮次" width="64" />
        <el-table-column prop="location" label="地点" min-width="130" />
        <el-table-column label="时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.scheduledStartAt) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="batchFeedbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchFeedbackSubmitting" @click="submitBatchFeedback"
          >提交</el-button
        >
      </template>
    </el-dialog>

    <!-- 批量完成对话框 -->
    <el-dialog v-model="batchCompleteVisible" title="批量完成面试" width="620px">
      <el-form :model="batchCompleteForm" label-width="98px">
        <el-form-item label="处理人数">
          <el-input :model-value="String(batchCompleteCandidates.length)" disabled />
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        show-icon
        :closable="false"
        :title="batchCompleteAlertTitle"
        style="margin-bottom: 12px"
      />
      <el-divider content-position="left">涉及面试</el-divider>
      <el-table :data="batchCompleteCandidates" max-height="280">
        <el-table-column prop="id" label="面试ID" width="90" />
        <el-table-column prop="applicationId" label="申请ID" width="90" />
        <el-table-column prop="round" label="轮次" width="72" />
        <el-table-column prop="status" label="当前状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{
              interviewStatusText(row.status)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="140" />
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.scheduledStartAt) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="batchCompleteVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchCompleteSubmitting" @click="submitBatchComplete"
          >完成</el-button
        >
      </template>
    </el-dialog>

    <!-- 面试反馈对话框 -->
    <el-dialog v-model="feedbackVisible" title="面试反馈" width="600px">
      <el-descriptions
        v-if="feedbackForm.interviewId"
        :column="2"
        border
        size="small"
        style="margin-bottom: 16px"
      >
        <el-descriptions-item label="面试ID">{{ feedbackForm.interviewId }}</el-descriptions-item>
        <el-descriptions-item label="申请ID">{{
          currentFeedbackApplicationId
        }}</el-descriptions-item>
        <el-descriptions-item label="轮次"
          >第 {{ feedbackForm.round || '-' }} 轮</el-descriptions-item
        >
        <el-descriptions-item label="地点">{{ feedbackForm.location || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="feedbackForm" label-width="78px">
        <el-form-item label="沟通能力">
          <el-rate v-model="feedbackForm.scores.communication" :max="5" show-score />
        </el-form-item>
        <el-form-item label="专业能力">
          <el-rate v-model="feedbackForm.scores.professional" :max="5" show-score />
        </el-form-item>
        <el-form-item label="综合素质">
          <el-rate v-model="feedbackForm.scores.overall" :max="5" show-score />
        </el-form-item>
        <el-form-item label="建议">
          <el-radio-group v-model="feedbackForm.suggestion">
            <el-radio-button value="approve">推荐通过</el-radio-button>
            <el-radio-button value="waitlist">待定</el-radio-button>
            <el-radio-button value="reject">淘汰</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="feedbackForm.comment" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="feedbackSubmitting" @click="submitFeedback"
          >提交反馈</el-button
        >
      </template>
    </el-dialog>

    <!-- 查看反馈列表对话框 -->
    <el-dialog v-model="feedbacksVisible" title="反馈记录" width="700px">
      <el-descriptions
        v-if="viewingFeedbacksInterviewId"
        :column="2"
        border
        size="small"
        style="margin-bottom: 16px"
      >
        <el-descriptions-item label="面试ID">{{
          viewingFeedbacksInterviewId
        }}</el-descriptions-item>
        <el-descriptions-item label="面试状态">{{
          interviewStatusText(viewingFeedbacksStatus)
        }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-loading="feedbacksLoading" :data="feedbackRecords" max-height="360">
        <el-table-column type="index" label="编号" width="72" />
        <el-table-column label="沟通能力" width="100">
          <template #default="{ row }">
            <template v-if="row.scoresObj && row.scoresObj.communication != null">
              <el-rate
                :model-value="Number(row.scoresObj.communication)"
                :max="5"
                disabled
                show-score
                size="small"
              />
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="专业能力" width="100">
          <template #default="{ row }">
            <template v-if="row.scoresObj && row.scoresObj.professional != null">
              <el-rate
                :model-value="Number(row.scoresObj.professional)"
                :max="5"
                disabled
                show-score
                size="small"
              />
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="综合素质" width="100">
          <template #default="{ row }">
            <template v-if="row.scoresObj && row.scoresObj.overall != null">
              <el-rate
                :model-value="Number(row.scoresObj.overall)"
                :max="5"
                disabled
                show-score
                size="small"
              />
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="建议" width="100">
          <template #default="{ row }">
            <el-tag :type="suggestionType(row.suggestion)" size="small">{{
              suggestionText(row.suggestion)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="评语" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="提交时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!feedbacksLoading && !feedbackRecords.length" description="暂无反馈记录" />
      <template #footer>
        <el-button @click="feedbacksVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 完成面试确认对话框 -->
    <el-dialog v-model="completeConfirmVisible" title="确认完成面试" width="480px">
      <el-descriptions
        v-if="completingInterview"
        :column="1"
        border
        size="small"
        style="margin-bottom: 12px"
      >
        <el-descriptions-item label="面试ID">{{ completingInterview.id }}</el-descriptions-item>
        <el-descriptions-item label="申请ID">{{
          completingInterview.applicationId
        }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="statusType(completingInterview.status)" size="small">{{
            interviewStatusText(completingInterview.status)
          }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="!completingHasFeedback"
        type="warning"
        show-icon
        :closable="false"
        title="该面试暂无反馈记录，确认要直接完成面试吗？"
        style="margin-bottom: 12px"
      />
      <p style="color: var(--oa-muted)">完成面试后，申请将进入[已面试]状态，可进行终审决策。</p>
      <template #footer>
        <el-button @click="completeConfirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="completeSubmitting" @click="submitComplete"
          >确认完成</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { interviewApi } from '@/api'
import { formatDateTime, interviewStatusText, statusType } from '@/utils/format.ts'

export default {
  name: 'AdminInterviews',
  data() {
    return {
      Search,
      loading: false,
      rows: [],
      selection: [],
      query: { applicationId: '', status: '' },
      // 批量确认
      batchConfirmVisible: false,
      batchConfirmSubmitting: false,
      batchConfirmForm: {},
      // 批量反馈
      batchFeedbackVisible: false,
      batchFeedbackSubmitting: false,
      batchFeedbackForm: {
        scores: { communication: 3, professional: 3, overall: 3 },
        suggestion: 'approve',
        comment: '',
      },
      // 批量完成
      batchCompleteVisible: false,
      batchCompleteSubmitting: false,
      batchCompleteForm: {},
      // 单个反馈
      feedbackVisible: false,
      feedbackSubmitting: false,
      feedbackForm: {
        interviewId: '',
        applicationId: '',
        round: 0,
        location: '',
        scores: { communication: 3, professional: 3, overall: 3 },
        suggestion: 'approve',
        comment: '',
      },
      // 查看反馈
      feedbacksVisible: false,
      feedbacksLoading: false,
      feedbackRecords: [],
      viewingFeedbacksInterviewId: null,
      viewingFeedbacksStatus: '',
      // 单个完成
      completeConfirmVisible: false,
      completeSubmitting: false,
      completingInterview: null,
      completingHasFeedback: false,
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    formatDateTime,
    statusType,
    interviewStatusText,
    suggestionText(suggestion) {
      if (suggestion === 'approve') return '推荐通过'
      if (suggestion === 'waitlist') return '待定'
      if (suggestion === 'reject') return '淘汰'
      return suggestion || '-'
    },
    suggestionType(suggestion) {
      if (suggestion === 'approve') return 'success'
      if (suggestion === 'reject') return 'danger'
      if (suggestion === 'waitlist') return 'warning'
      return 'info'
    },
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

    // ---- 批量确认 ----
    openBatchConfirm() {
      if (!this.batchConfirmCandidates.length) {
        ElMessage.warning('请选择待确认的面试')
        return
      }
      this.batchConfirmVisible = true
    },
    async submitBatchConfirm() {
      if (this.batchConfirmSubmitting) return
      this.batchConfirmSubmitting = true
      try {
        await Promise.all(this.batchConfirmCandidates.map((item) => interviewApi.confirm(item.id)))
        ElMessage.success(`已批量确认 ${this.batchConfirmCandidates.length} 场面试`)
        this.batchConfirmVisible = false
        this.fetchList()
      } finally {
        this.batchConfirmSubmitting = false
      }
    },

    // ---- 批量反馈 ----
    openBatchFeedback() {
      if (!this.batchFeedbackCandidates.length) {
        ElMessage.warning('请选择可反馈的面试')
        return
      }
      this.batchFeedbackForm = {
        scores: { communication: 3, professional: 3, overall: 3 },
        suggestion: 'approve',
        comment: '',
      }
      this.batchFeedbackVisible = true
    },
    async submitBatchFeedback() {
      if (this.batchFeedbackSubmitting) return
      this.batchFeedbackSubmitting = true
      try {
        await Promise.all(
          this.batchFeedbackCandidates.map((item) =>
            interviewApi.feedback(item.id, {
              scores: { ...this.batchFeedbackForm.scores },
              suggestion: this.batchFeedbackForm.suggestion,
              comment: this.batchFeedbackForm.comment || undefined,
            }),
          ),
        )
        ElMessage.success(`已批量反馈 ${this.batchFeedbackCandidates.length} 场面试`)
        this.batchFeedbackVisible = false
        this.fetchList()
      } finally {
        this.batchFeedbackSubmitting = false
      }
    },

    // ---- 批量完成 ----
    openBatchComplete() {
      if (!this.batchCompleteCandidates.length) {
        ElMessage.warning('请选择可完成的面试')
        return
      }
      this.batchCompleteVisible = true
    },
    async submitBatchComplete() {
      if (this.batchCompleteSubmitting) return
      this.batchCompleteSubmitting = true
      try {
        await Promise.all(
          this.batchCompleteCandidates.map((item) => interviewApi.complete(item.id)),
        )
        ElMessage.success(`已批量完成 ${this.batchCompleteCandidates.length} 场面试`)
        this.batchCompleteVisible = false
        this.fetchList()
      } finally {
        this.batchCompleteSubmitting = false
      }
    },

    // ---- 单个操作 ----
    async confirmInterview(row) {
      await interviewApi.confirm(row.id)
      ElMessage.success('面试已确认')
      this.fetchList()
    },
    openFeedback(row) {
      this.feedbackForm = {
        interviewId: row.id,
        applicationId: row.applicationId,
        round: row.round,
        location: row.location,
        scores: { communication: 3, professional: 3, overall: 3 },
        suggestion: 'approve',
        comment: '',
      }
      this.feedbackVisible = true
    },
    async submitFeedback() {
      if (this.feedbackSubmitting) return
      this.feedbackSubmitting = true
      try {
        await interviewApi.feedback(this.feedbackForm.interviewId, {
          scores: { ...this.feedbackForm.scores },
          suggestion: this.feedbackForm.suggestion,
          comment: this.feedbackForm.comment,
        })
        ElMessage.success('反馈已提交')
        this.feedbackVisible = false
        this.fetchList()
      } finally {
        this.feedbackSubmitting = false
      }
    },
    async viewFeedbacks(row) {
      this.viewingFeedbacksInterviewId = row.id
      this.viewingFeedbacksStatus = row.status
      this.feedbacksVisible = true
      this.feedbacksLoading = true
      try {
        const result = await interviewApi.feedbacks(row.id)
        this.feedbackRecords = (result?.list || result || []).map((item) => {
          let scoresObj = {}
          if (item.scores) {
            try {
              scoresObj = typeof item.scores === 'string' ? JSON.parse(item.scores) : item.scores
            } catch (e) {
              console.error('Failed to parse scores', e)
            }
          }
          return { ...item, scoresObj }
        })
      } catch (e) {
        console.error('Failed to fetch feedbacks', e)
        this.feedbackRecords = []
      } finally {
        this.feedbacksLoading = false
      }
    },
    async checkHasFeedback(interviewId) {
      try {
        const result = await interviewApi.feedbacks(interviewId)
        const list = result?.list || result || []
        return list.length > 0
      } catch (e) {
        return false
      }
    },
    async openCompleteConfirm(row) {
      this.completingInterview = row
      this.completeConfirmVisible = true
      const hasFeedback = await this.checkHasFeedback(row.id)
      this.completingHasFeedback = hasFeedback
    },
    async submitComplete() {
      if (this.completeSubmitting) return
      this.completeSubmitting = true
      try {
        await interviewApi.complete(this.completingInterview.id)
        ElMessage.success('面试已完成')
        this.completeConfirmVisible = false
        this.fetchList()
      } finally {
        this.completeSubmitting = false
      }
    },
  },
  computed: {
    currentFeedbackApplicationId() {
      return this.feedbackForm.applicationId || '-'
    },
    batchConfirmCandidates() {
      return this.selection.filter((item) => item.status === 'pending')
    },
    batchFeedbackCandidates() {
      return this.selection.filter((item) => item.status !== 'completed')
    },
    batchCompleteCandidates() {
      return this.selection.filter((item) => item.status !== 'completed')
    },
    batchCompleteAlertTitle() {
      return '完成面试后，对应申请将进入[已面试]状态，可进行终审决策。'
    },
  },
}
</script>

<style scoped>
.toolbar__actions {
  display: flex;
  gap: 12px;
}
</style>
