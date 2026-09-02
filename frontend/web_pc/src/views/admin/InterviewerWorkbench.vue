<template>
  <ViewPage class="workbench-page">
    <div class="workbench-heading">
      <div>
        <h2>面试官工作台</h2>
        <p>候选人资料、评价填写和组内进度集中在一个页面</p>
      </div>
      <el-button :loading="loading" @click="fetchList">刷新安排</el-button>
    </div>

    <el-empty v-if="!loading && !items.length" description="暂时没有分配给你的面试" />
    <div v-else v-loading="loading" class="workbench-grid">
      <aside class="candidate-list">
        <button
          v-for="item in items"
          :key="item.interviewId"
          type="button"
          class="candidate-card"
          :class="{ active: item.interviewId === selectedId }"
          @click="selectCandidate(item.interviewId)"
        >
          <span class="candidate-card__number">{{ queueText(item) }}</span>
          <strong>{{ item.applicantName }}</strong>
          <small>{{ item.roomName || item.location || '面试间待定' }}</small>
          <el-tag :type="feedbackTagType(item)" size="small">{{ feedbackText(item) }}</el-tag>
        </button>
      </aside>

      <template v-if="current">
        <main class="candidate-profile">
          <el-card shadow="never">
            <template #header>
              <div class="profile-title">
                <div>
                  <span>{{ queueText(current) }}</span>
                  <h2>{{ current.applicantName }}</h2>
                </div>
                <el-tag>{{ current.roomName || current.location || '面试间待定' }}</el-tag>
              </div>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="学号">{{
                current.studentId || '-'
              }}</el-descriptions-item>
              <el-descriptions-item label="学院">{{ current.college || '-' }}</el-descriptions-item>
              <el-descriptions-item label="专业/年级">
                {{ [current.major, current.grade].filter(Boolean).join(' · ') || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="第一志愿">{{
                current.firstChoiceDepartmentName || '-'
              }}</el-descriptions-item>
              <el-descriptions-item label="第二志愿">{{
                current.secondChoiceDepartmentName || '-'
              }}</el-descriptions-item>
              <el-descriptions-item label="时间">{{
                formatDateTime(current.scheduledStartAt)
              }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never">
            <template #header><strong>报名资料</strong></template>
            <div v-if="profileEntries.length" class="profile-answers">
              <div v-for="entry in profileEntries" :key="entry[0]">
                <span>{{ entry[0] }}</span>
                <p>{{ formatAnswer(entry[1]) }}</p>
              </div>
            </div>
            <el-empty v-else description="没有额外报名资料" :image-size="72" />
          </el-card>
        </main>

        <section class="evaluation-panel">
          <div class="evaluation-panel__header">
            <div>
              <h3>{{ current.template?.name || '面试评价' }}</h3>
              <span v-if="current.template">版本 {{ current.template.version }}</span>
            </div>
            <span class="draft-state">{{ saving ? '正在保存…' : draftState }}</span>
          </div>

          <el-progress
            :percentage="progressPercentage"
            :status="current.submittedCount === current.requiredCount ? 'success' : undefined"
            :format="() => `${current.submittedCount}/${current.requiredCount} 人已提交`"
          />

          <div class="dimension-list">
            <div v-for="dimension in dimensions" :key="dimension.key" class="dimension-item">
              <div>
                <strong>{{ dimension.label }} <em v-if="dimension.required">*</em></strong>
                <p>{{ dimension.description }}</p>
              </div>
              <el-rate v-model="form.scores[dimension.key]" :max="5" show-score />
            </div>
          </div>

          <el-divider content-position="left">详细评鉴</el-divider>
          <el-form label-position="top">
            <el-form-item label="面试问答记录">
              <el-input
                v-model="form.details.questionNotes"
                type="textarea"
                :rows="4"
                placeholder="记录关键问题、候选人的回答和追问结果"
              />
            </el-form-item>
            <el-form-item label="突出优势">
              <el-input
                v-model="form.details.highlights"
                type="textarea"
                :rows="2"
                placeholder="具体事实、项目经历或令人印象深刻的表现"
              />
            </el-form-item>
            <el-form-item label="风险与待确认项">
              <el-input
                v-model="form.details.risks"
                type="textarea"
                :rows="2"
                placeholder="能力短板、时间投入或需要二次确认的事项"
              />
            </el-form-item>
            <el-form-item label="录用建议" required>
              <el-radio-group v-model="form.suggestion">
                <el-radio-button value="approve">推荐通过</el-radio-button>
                <el-radio-button value="waitlist">待定</el-radio-button>
                <el-radio-button value="reject">不推荐</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="综合评语">
              <el-input
                v-model="form.comment"
                type="textarea"
                :rows="3"
                placeholder="给终审人员的整体判断和建议"
              />
            </el-form-item>
          </el-form>

          <div class="evaluation-actions">
            <el-button
              v-if="isSubmitted"
              type="warning"
              plain
              :loading="submitting"
              @click="withdraw"
              >撤回修改</el-button
            >
            <el-button v-else type="primary" :loading="submitting" @click="submit"
              >提交评价</el-button
            >
          </div>

          <template v-if="isSubmitted">
            <el-divider content-position="left">其他面试官评价</el-divider>
            <el-empty
              v-if="!current.groupFeedbacks?.length"
              description="等待其他面试官提交"
              :image-size="60"
            />
            <el-card
              v-for="feedback in current.groupFeedbacks"
              :key="feedback.id"
              shadow="never"
              class="group-feedback"
            >
              <div>
                <strong>面试官 #{{ feedback.interviewerId }}</strong
                ><el-tag size="small">{{ suggestionText(feedback.suggestion) }}</el-tag>
              </div>
              <p>{{ feedback.comment || '未填写综合评语' }}</p>
            </el-card>
          </template>
        </section>
      </template>
    </div>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { interviewApi, interviewerWorkbenchApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'

type Dimension = { key: string; label: string; description?: string; required?: boolean }
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const items = ref<any[]>([])
const selectedId = ref<number | null>(null)
const hydrated = ref(false)
const draftState = ref('尚未保存')
let saveTimer: ReturnType<typeof setTimeout> | undefined

const form = reactive<{
  scores: Record<string, number>
  details: Record<string, string>
  suggestion: string
  comment: string
}>({
  scores: {},
  details: { questionNotes: '', highlights: '', risks: '' },
  suggestion: '',
  comment: '',
})
const current = computed(
  () => items.value.find((item) => item.interviewId === selectedId.value) || null,
)
const dimensions = computed<Dimension[]>(() => current.value?.template?.schema?.dimensions || [])
const profileEntries = computed(() =>
  Object.entries(current.value?.profile || {}).filter(
    ([, value]) => value !== null && value !== '',
  ),
)
const isSubmitted = computed(() => current.value?.ownFeedback?.status === 'submitted')
const progressPercentage = computed(() =>
  current.value?.requiredCount
    ? Math.round((current.value.submittedCount / current.value.requiredCount) * 100)
    : 0,
)

function hydrate() {
  hydrated.value = false
  const feedback = current.value?.ownFeedback || {}
  form.scores = { ...(feedback.scores || {}) }
  dimensions.value.forEach((d) => {
    if (form.scores[d.key] == null) form.scores[d.key] = 0
  })
  form.details = { questionNotes: '', highlights: '', risks: '', ...(feedback.details || {}) }
  form.suggestion = feedback.suggestion || ''
  form.comment = feedback.comment || ''
  draftState.value =
    feedback.status === 'draft'
      ? '草稿已保存'
      : feedback.status === 'submitted'
        ? '已提交'
        : '尚未保存'
  requestAnimationFrame(() => {
    hydrated.value = true
  })
}

async function fetchList(keepSelection = true) {
  loading.value = true
  try {
    const data = await interviewerWorkbenchApi.list()
    items.value = Array.isArray(data) ? data : []
    if (!keepSelection || !items.value.some((i) => i.interviewId === selectedId.value))
      selectedId.value = items.value[0]?.interviewId ?? null
    hydrate()
  } finally {
    loading.value = false
  }
}
function selectCandidate(id: number) {
  selectedId.value = id
  hydrate()
}

watch(
  form,
  () => {
    if (!hydrated.value || !current.value || isSubmitted.value) return
    if (saveTimer) clearTimeout(saveTimer)
    draftState.value = '有未保存修改'
    saveTimer = setTimeout(saveDraft, 900)
  },
  { deep: true },
)

function payload() {
  return {
    templateId: current.value?.template?.id,
    scores: form.scores,
    details: form.details,
    suggestion: form.suggestion,
    comment: form.comment,
  }
}
async function saveDraft() {
  if (!current.value || isSubmitted.value) return
  saving.value = true
  try {
    await interviewApi.saveFeedbackDraft(current.value.interviewId, payload())
    draftState.value = '草稿已自动保存'
  } catch {
    draftState.value = '自动保存失败'
  } finally {
    saving.value = false
  }
}
async function submit() {
  const missing = dimensions.value.find((d) => d.required !== false && !form.scores[d.key])
  if (missing) return ElMessage.warning(`请完成“${missing.label}”评分`)
  if (!form.suggestion) return ElMessage.warning('请选择录用建议')
  if (saveTimer) clearTimeout(saveTimer)
  hydrated.value = false
  submitting.value = true
  try {
    await interviewApi.submitFeedback(current.value.interviewId, payload())
    ElMessage.success('评价已提交')
    await fetchList(true)
  } finally {
    submitting.value = false
  }
}
async function withdraw() {
  submitting.value = true
  try {
    await interviewApi.withdrawFeedback(current.value.interviewId)
    ElMessage.success('已撤回，可继续修改')
    await fetchList(true)
  } finally {
    submitting.value = false
  }
}
function queueText(item: any) {
  return item.queueNumber
    ? `#${String(item.queueNumber).padStart(3, '0')}`
    : `面试 ${item.interviewId}`
}
function feedbackText(item: any) {
  return item.ownFeedback?.status === 'submitted'
    ? '已提交'
    : item.ownFeedback?.status === 'draft'
      ? '草稿'
      : '待评价'
}
function feedbackTagType(item: any) {
  return item.ownFeedback?.status === 'submitted'
    ? 'success'
    : item.ownFeedback?.status === 'draft'
      ? 'warning'
      : 'info'
}
function suggestionText(value: string) {
  return (
    ({ approve: '推荐通过', waitlist: '待定', reject: '不推荐' } as Record<string, string>)[
      value
    ] || value
  )
}
function formatAnswer(value: unknown) {
  return Array.isArray(value)
    ? value.join('、')
    : typeof value === 'object'
      ? JSON.stringify(value)
      : String(value)
}
onMounted(() => fetchList(false))
onBeforeUnmount(() => {
  if (saveTimer) clearTimeout(saveTimer)
})
</script>

<style scoped>
.workbench-heading,
.profile-title,
.evaluation-panel__header,
.evaluation-actions,
.group-feedback > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.workbench-heading {
  margin-bottom: 18px;
}
.workbench-heading h2,
.profile-title h2,
.evaluation-panel h3 {
  margin: 0;
}
.workbench-heading p,
.dimension-item p,
.group-feedback p {
  color: var(--oa-muted);
  margin: 6px 0 0;
}
.workbench-grid {
  display: grid;
  grid-template-columns: 220px minmax(300px, 0.9fr) minmax(390px, 1.25fr);
  gap: 16px;
  align-items: start;
}
.candidate-list {
  display: grid;
  gap: 10px;
  position: sticky;
  top: 10px;
}
.candidate-card {
  display: grid;
  gap: 7px;
  text-align: left;
  padding: 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 14px;
  background: var(--el-bg-color);
  cursor: pointer;
  color: inherit;
}
.candidate-card.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}
.candidate-card__number {
  color: var(--el-color-primary);
  font-weight: 700;
}
.candidate-card small,
.evaluation-panel__header span {
  color: var(--oa-muted);
}
.candidate-profile {
  display: grid;
  gap: 16px;
}
.profile-title span {
  font-size: 13px;
  color: var(--el-color-primary);
}
.profile-answers {
  display: grid;
  gap: 15px;
}
.profile-answers span {
  color: var(--oa-muted);
  font-size: 13px;
}
.profile-answers p {
  white-space: pre-wrap;
  margin: 4px 0 0;
}
.evaluation-panel {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  padding: 20px;
}
.dimension-list {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}
.dimension-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 13px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.dimension-item em {
  color: var(--el-color-danger);
  font-style: normal;
}
.dimension-item p {
  font-size: 13px;
}
.draft-state {
  font-size: 12px;
}
.evaluation-actions {
  justify-content: flex-end;
}
.group-feedback {
  margin-top: 10px;
}
.group-feedback p {
  white-space: pre-wrap;
}
@media (max-width: 1100px) {
  .workbench-grid {
    grid-template-columns: 190px 1fr;
  }
  .evaluation-panel {
    grid-column: 2;
  }
  .candidate-list {
    grid-row: 1 / span 2;
  }
}
@media (max-width: 760px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }
  .candidate-list {
    position: static;
    grid-row: auto;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .candidate-profile,
  .evaluation-panel {
    grid-column: 1;
  }
  .dimension-item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
