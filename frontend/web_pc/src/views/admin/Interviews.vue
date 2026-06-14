<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
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
    </ViewToolbar>
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
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Search } from '@element-plus/icons-vue'
import { interviewApi } from '@/api'
import { formatDateTime, interviewStatusText, statusType } from '@/utils/format.ts'
import { computed, onMounted, ref } from 'vue'

const loading = ref(false)

const rows = ref<any[]>([])

const selection = ref<any[]>([])

const query = ref({ applicationId: '', status: '' })

const batchConfirmSubmitting = ref(false)

const batchConfirmForm = ref<Record<string, any>>({})

const batchFeedbackSubmitting = ref(false)

const batchFeedbackForm = ref({
  scores: { communication: 3, professional: 3, overall: 3 },
  suggestion: 'approve',
  comment: '',
})

const batchCompleteSubmitting = ref(false)

const batchCompleteForm = ref<Record<string, any>>({})

const feedbackSubmitting = ref(false)

const feedbackForm = ref({
  interviewId: '',
  applicationId: '',
  round: 0,
  location: '',
  scores: { communication: 3, professional: 3, overall: 3 },
  suggestion: 'approve',
  comment: '',
})

const feedbacksLoading = ref(false)

const feedbackRecords = ref<any[]>([])

const viewingFeedbacksInterviewId = ref<any>(null)

const viewingFeedbacksStatus = ref('')

const completeSubmitting = ref(false)

const completingInterview = ref<any>(null)

const completingHasFeedback = ref(false)

const batchConfirmVisible = ref<any>()

const batchFeedbackVisible = ref<any>()

const batchCompleteVisible = ref<any>()

const feedbackVisible = ref<any>()

const feedbacksVisible = ref<any>()

const completeConfirmVisible = ref<any>()

const currentFeedbackApplicationId = computed(() => {
  return feedbackForm.value.applicationId || '-'
})

const batchConfirmCandidates = computed(() => {
  return selection.value.filter((item) => item.status === 'pending')
})

const batchFeedbackCandidates = computed(() => {
  return selection.value.filter((item) => item.status !== 'completed')
})

const batchCompleteCandidates = computed(() => {
  return selection.value.filter((item) => item.status !== 'completed')
})

const batchCompleteAlertTitle = computed(() => {
  return '完成面试后，对应申请将进入[已面试]状态，可进行终审决策。'
})

function suggestionText(suggestion: any) {
  if (suggestion === 'approve') return '推荐通过'
  if (suggestion === 'waitlist') return '待定'
  if (suggestion === 'reject') return '淘汰'
  return suggestion || '-'
}

function suggestionType(suggestion: any) {
  if (suggestion === 'approve') return 'success'
  if (suggestion === 'reject') return 'danger'
  if (suggestion === 'waitlist') return 'warning'
  return 'info'
}

async function fetchList() {
  loading.value = true
  try {
    const applicationId = query.value.applicationId ? Number(query.value.applicationId) : undefined
    const result = await interviewApi.list({
      applicationId: Number.isNaN(applicationId) ? undefined : applicationId,
      status: query.value.status || undefined,
    })
    rows.value = result?.list || result || []
  } finally {
    loading.value = false
  }
}

function openBatchConfirm() {
  if (!batchConfirmCandidates.value.length) {
    ElMessage.warning('请选择待确认的面试')
    return
  }
  batchConfirmVisible.value = true
}

async function submitBatchConfirm() {
  if (batchConfirmSubmitting.value) return
  batchConfirmSubmitting.value = true
  try {
    await Promise.all(batchConfirmCandidates.value.map((item) => interviewApi.confirm(item.id)))
    ElMessage.success(`已批量确认 ${batchConfirmCandidates.value.length} 场面试`)
    batchConfirmVisible.value = false
    fetchList()
  } finally {
    batchConfirmSubmitting.value = false
  }
}

function openBatchFeedback() {
  if (!batchFeedbackCandidates.value.length) {
    ElMessage.warning('请选择可反馈的面试')
    return
  }
  batchFeedbackForm.value = {
    scores: { communication: 3, professional: 3, overall: 3 },
    suggestion: 'approve',
    comment: '',
  }
  batchFeedbackVisible.value = true
}

async function submitBatchFeedback() {
  if (batchFeedbackSubmitting.value) return
  batchFeedbackSubmitting.value = true
  try {
    await Promise.all(
      batchFeedbackCandidates.value.map((item) =>
        interviewApi.feedback(item.id, {
          scores: { ...batchFeedbackForm.value.scores },
          suggestion: batchFeedbackForm.value.suggestion,
          comment: batchFeedbackForm.value.comment || undefined,
        }),
      ),
    )
    ElMessage.success(`已批量反馈 ${batchFeedbackCandidates.value.length} 场面试`)
    batchFeedbackVisible.value = false
    fetchList()
  } finally {
    batchFeedbackSubmitting.value = false
  }
}

function openBatchComplete() {
  if (!batchCompleteCandidates.value.length) {
    ElMessage.warning('请选择可完成的面试')
    return
  }
  batchCompleteVisible.value = true
}

async function submitBatchComplete() {
  if (batchCompleteSubmitting.value) return
  batchCompleteSubmitting.value = true
  try {
    await Promise.all(batchCompleteCandidates.value.map((item) => interviewApi.complete(item.id)))
    ElMessage.success(`已批量完成 ${batchCompleteCandidates.value.length} 场面试`)
    batchCompleteVisible.value = false
    fetchList()
  } finally {
    batchCompleteSubmitting.value = false
  }
}

async function confirmInterview(row: any) {
  await interviewApi.confirm(row.id)
  ElMessage.success('面试已确认')
  fetchList()
}

function openFeedback(row: any) {
  feedbackForm.value = {
    interviewId: row.id,
    applicationId: row.applicationId,
    round: row.round,
    location: row.location,
    scores: { communication: 3, professional: 3, overall: 3 },
    suggestion: 'approve',
    comment: '',
  }
  feedbackVisible.value = true
}

async function submitFeedback() {
  if (feedbackSubmitting.value) return
  feedbackSubmitting.value = true
  try {
    await interviewApi.feedback(feedbackForm.value.interviewId, {
      scores: { ...feedbackForm.value.scores },
      suggestion: feedbackForm.value.suggestion,
      comment: feedbackForm.value.comment,
    })
    ElMessage.success('反馈已提交')
    feedbackVisible.value = false
    fetchList()
  } finally {
    feedbackSubmitting.value = false
  }
}

async function viewFeedbacks(row: any) {
  viewingFeedbacksInterviewId.value = row.id
  viewingFeedbacksStatus.value = row.status
  feedbacksVisible.value = true
  feedbacksLoading.value = true
  try {
    const result = await interviewApi.feedbacks(row.id)
    feedbackRecords.value = (result?.list || result || []).map((item) => {
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
    feedbackRecords.value = []
  } finally {
    feedbacksLoading.value = false
  }
}

async function checkHasFeedback(interviewId: any) {
  try {
    const result = await interviewApi.feedbacks(interviewId)
    const list = result?.list || result || []
    return list.length > 0
  } catch (e) {
    return false
  }
}

async function openCompleteConfirm(row: any) {
  completingInterview.value = row
  completeConfirmVisible.value = true
  const hasFeedback = await checkHasFeedback(row.id)
  completingHasFeedback.value = hasFeedback
}

async function submitComplete() {
  if (completeSubmitting.value) return
  completeSubmitting.value = true
  try {
    await interviewApi.complete(completingInterview.value.id)
    ElMessage.success('面试已完成')
    completeConfirmVisible.value = false
    fetchList()
  } finally {
    completeSubmitting.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.toolbar__actions {
  display: flex;
  gap: 12px;
}
</style>
