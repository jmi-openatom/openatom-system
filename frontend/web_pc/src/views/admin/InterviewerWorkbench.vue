<template>
  <div class="workbench-shell">
    <header class="workbench-header">
      <div class="workbench-brand">
        <el-button
          class="exit-button"
          circle
          :icon="Close"
          aria-label="退出面试官工作台"
          title="退出面试官工作台"
          @click="$router.push('/admin/interviews')"
        />
        <div>
          <span>OPENATOM INTERVIEW</span>
          <h1>面试官工作台</h1>
        </div>
      </div>

      <div v-if="current" class="current-candidate" aria-live="polite">
        <span class="current-candidate__number">{{ queueText(current) }}</span>
        <div>
          <strong>{{ current.applicantName }}</strong>
          <small>{{ current.roomName || current.location || '面试间待定' }}</small>
        </div>
        <el-tag :type="feedbackTagType(current)" effect="plain">{{ feedbackText(current) }}</el-tag>
      </div>

      <div class="workbench-header__actions">
        <span v-if="items.length" class="candidate-progress">
          <strong>{{ currentIndex + 1 }}</strong> / {{ items.length }} 位
        </span>
        <el-button :icon="List" @click="candidateListVisible = true">候选人列表</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="fetchList">刷新</el-button>
      </div>
    </header>

    <main id="workbench-content" v-loading="loading" class="workbench-content">
      <el-empty v-if="!loading && !items.length" description="暂时没有分配给你的面试">
        <el-button type="primary" @click="fetchList(false)">重新加载</el-button>
      </el-empty>

      <div v-else-if="current" class="workbench-columns">
        <section class="candidate-profile" aria-labelledby="candidate-profile-title">
          <div class="section-heading">
            <div>
              <span>候选人资料</span>
              <h2 id="candidate-profile-title">{{ current.applicantName }}</h2>
            </div>
            <el-tag size="large">{{ current.roomName || current.location || '面试间待定' }}</el-tag>
          </div>

          <el-descriptions :column="1" border class="profile-descriptions">
            <el-descriptions-item label="学号">{{ current.studentId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="学院">{{ current.college || '-' }}</el-descriptions-item>
            <el-descriptions-item label="专业 / 年级">
              {{ [current.major, current.grade].filter(Boolean).join(' · ') || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="第一志愿">{{ current.firstChoiceDepartmentName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="第二志愿">{{ current.secondChoiceDepartmentName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="面试时间">{{ formatDateTime(current.scheduledStartAt) }}</el-descriptions-item>
          </el-descriptions>

          <div class="profile-section-title">
            <span>APPLICATION</span>
            <h3>报名资料</h3>
          </div>
          <div v-if="profileEntries.length" class="profile-answers">
            <article v-for="entry in profileEntries" :key="entry[0]">
              <span>{{ entry[0] }}</span>
              <p>{{ formatAnswer(entry[1]) }}</p>
            </article>
          </div>
          <el-empty v-else description="没有额外报名资料" :image-size="72" />
        </section>

        <section class="evaluation-panel" aria-labelledby="evaluation-title">
          <div class="evaluation-panel__header">
            <div>
              <span>ASSESSMENT</span>
              <h2 id="evaluation-title">{{ current.template?.name || '面试评价' }}</h2>
              <small v-if="current.template">模板版本 {{ current.template.version }}</small>
            </div>
            <div class="save-state" :class="{ 'save-state--saving': saving }">
              <span class="save-state__dot" />
              {{ saving ? '正在保存…' : draftState }}
            </div>
          </div>

          <div class="team-progress">
            <div>
              <span>面试官提交进度</span>
              <strong>{{ current.submittedCount }}/{{ current.requiredCount }}</strong>
            </div>
            <el-progress
              :percentage="progressPercentage"
              :status="current.submittedCount === current.requiredCount ? 'success' : undefined"
              :show-text="false"
            />
          </div>

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
          <el-form label-position="top" class="detail-form">
            <el-form-item label="面试问答记录">
              <el-input v-model="form.details.questionNotes" type="textarea" :rows="5" placeholder="记录关键问题、候选人的回答和追问结果" />
            </el-form-item>
            <div class="detail-form__row">
              <el-form-item label="突出优势">
                <el-input v-model="form.details.highlights" type="textarea" :rows="3" placeholder="具体事实、项目经历或令人印象深刻的表现" />
              </el-form-item>
              <el-form-item label="风险与待确认项">
                <el-input v-model="form.details.risks" type="textarea" :rows="3" placeholder="能力短板、时间投入或需要二次确认的事项" />
              </el-form-item>
            </div>
            <el-form-item label="录用建议" required>
              <el-radio-group v-model="form.suggestion" class="suggestion-group">
                <el-radio-button value="approve">推荐通过</el-radio-button>
                <el-radio-button value="waitlist">待定观察</el-radio-button>
                <el-radio-button value="reject">不推荐</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="综合评语">
              <el-input v-model="form.comment" type="textarea" :rows="4" placeholder="给终审人员的整体判断和建议" />
            </el-form-item>
          </el-form>

          <template v-if="isSubmitted">
            <el-divider content-position="left">其他面试官评价</el-divider>
            <el-empty v-if="!current.groupFeedbacks?.length" description="等待其他面试官提交" :image-size="60" />
            <el-card v-for="feedback in current.groupFeedbacks" :key="feedback.id" shadow="never" class="group-feedback">
              <div>
                <strong>面试官 #{{ feedback.interviewerId }}</strong>
                <el-tag size="small">{{ suggestionText(feedback.suggestion) }}</el-tag>
              </div>
              <p>{{ feedback.comment || '未填写综合评语' }}</p>
            </el-card>
          </template>
        </section>
      </div>
    </main>

    <footer class="workbench-footer">
      <el-button
        class="candidate-nav-button"
        size="large"
        :icon="ArrowLeft"
        :disabled="!previousCandidate || switching"
        @click="goPrevious"
      >
        <span>上一位</span>
        <small v-if="previousCandidate">{{ previousCandidate.applicantName }}</small>
      </el-button>

      <div v-if="current" class="footer-center">
        <span>{{ queueText(current) }} · {{ current.applicantName }}</span>
        <template v-if="canCallNext">
          <el-button type="primary" size="large" :loading="callingNext" @click="callNextFromWorkbench">
            叫下一位
          </el-button>
        </template>
        <el-tag v-else-if="allInterviewersSubmitted" type="success" effect="plain" size="large">
          评价已完成
        </el-tag>
        <template v-else-if="isSubmitted">
          <el-tag type="warning" effect="plain" size="large">
            等待 {{ Math.max(0, current.requiredCount - current.submittedCount) }} 位面试官
          </el-tag>
          <el-button type="warning" plain size="large" :loading="submitting" @click="withdraw">撤回修改</el-button>
        </template>
        <el-button v-else type="primary" size="large" :loading="submitting" @click="submit">提交评价</el-button>
      </div>

      <el-button
        class="candidate-nav-button candidate-nav-button--next"
        size="large"
        :disabled="!nextCandidate || switching"
        @click="goNext"
      >
        <span>下一位</span>
        <small v-if="nextCandidate">{{ nextCandidate.applicantName }}</small>
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </footer>

    <el-drawer
      v-model="candidateListVisible"
      title="选择候选人"
      size="min(440px, 92vw)"
      append-to-body
      class="candidate-drawer"
    >
      <div class="candidate-drawer__toolbar">
        <el-input
          v-model="candidateSearch"
          :prefix-icon="Search"
          clearable
          placeholder="搜索姓名、学号或编号"
        />
        <el-segmented
          v-model="candidateStatus"
          :options="[
            { label: '全部', value: 'all' },
            { label: '待评价', value: 'pending' },
            { label: '草稿', value: 'draft' },
            { label: '已提交', value: 'submitted' },
          ]"
        />
      </div>

      <div class="candidate-drawer__summary">
        <span>共 {{ filteredCandidates.length }} 位</span>
        <small>刷新后仍会回到最后选择的候选人</small>
      </div>

      <div v-if="filteredCandidates.length" class="candidate-options">
        <button
          v-for="item in filteredCandidates"
          :key="item.interviewId"
          type="button"
          class="candidate-option"
          :class="{ 'candidate-option--active': item.interviewId === selectedId }"
          @click="chooseCandidate(item.interviewId)"
        >
          <span class="candidate-option__number">{{ queueText(item) }}</span>
          <span class="candidate-option__identity">
            <strong>{{ item.applicantName }}</strong>
            <small>{{ item.studentId || '未填写学号' }} · {{ item.roomName || '面试间待定' }}</small>
          </span>
          <el-tag :type="feedbackTagType(item)" size="small">{{ feedbackText(item) }}</el-tag>
        </button>
      </div>
      <el-empty v-else description="没有符合条件的候选人" :image-size="80" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { interviewApi, interviewerWorkbenchApi, interviewSessionApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { ArrowLeft, ArrowRight, Close, List, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type Dimension = { key: string; label: string; description?: string; required?: boolean }
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const switching = ref(false)
const callingNext = ref(false)
const candidateListVisible = ref(false)
const candidateSearch = ref('')
const candidateStatus = ref('all')
const items = ref<any[]>([])
const selectedId = ref<number | null>(null)
const hydrated = ref(false)
const draftState = ref('尚未保存')
let saveTimer: ReturnType<typeof setTimeout> | undefined
const route = useRoute()
const router = useRouter()
const selectionStorageKey = 'openatom:interviewer-workbench:selected-id'

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
const currentIndex = computed(() =>
  items.value.findIndex((item) => item.interviewId === selectedId.value),
)
const previousCandidate = computed(() =>
  currentIndex.value > 0 ? items.value[currentIndex.value - 1] : null,
)
const nextCandidate = computed(() =>
  currentIndex.value >= 0 && currentIndex.value < items.value.length - 1
    ? items.value[currentIndex.value + 1]
    : null,
)
const allInterviewersSubmitted = computed(
  () =>
    !!current.value?.requiredCount &&
    current.value.submittedCount >= current.value.requiredCount,
)
const canCallNext = computed(
  () => allInterviewersSubmitted.value && current.value?.queueStatus === 'called',
)
const filteredCandidates = computed(() => {
  const keyword = candidateSearch.value.trim().toLowerCase()
  return items.value.filter((item) => {
    const matchesStatus = candidateStatus.value === 'all' || feedbackStatus(item) === candidateStatus.value
    const haystack = [item.applicantName, item.studentId, item.roomName, queueText(item)]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return matchesStatus && (!keyword || haystack.includes(keyword))
  })
})
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

function storedSelection() {
  const queryId = Number(route.query.candidate)
  if (Number.isInteger(queryId) && queryId > 0) return queryId
  const localId = Number(localStorage.getItem(selectionStorageKey))
  return Number.isInteger(localId) && localId > 0 ? localId : null
}

function persistSelection(id: number | null) {
  if (id == null) return
  localStorage.setItem(selectionStorageKey, String(id))
  if (String(route.query.candidate || '') !== String(id)) {
    void router.replace({ query: { ...route.query, candidate: String(id) } })
  }
}

async function fetchList(keepSelection = true, rehydrate = false) {
  loading.value = true
  try {
    const data = await interviewerWorkbenchApi.list()
    items.value = Array.isArray(data) ? data : []
    const preferredId = keepSelection ? selectedId.value || storedSelection() : storedSelection()
    const nextId = items.value.some((item) => item.interviewId === preferredId)
      ? preferredId
      : items.value[0]?.interviewId ?? null
    const selectionChanged = nextId !== selectedId.value
    selectedId.value = nextId
    persistSelection(nextId)
    if (selectionChanged || rehydrate || !hydrated.value) hydrate()
  } finally {
    loading.value = false
  }
}
async function selectCandidate(id: number) {
  if (id === selectedId.value || switching.value) return
  switching.value = true
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = undefined
  if (hydrated.value && current.value && !isSubmitted.value && draftState.value === '有未保存修改') {
    await saveDraft()
  }
  selectedId.value = id
  persistSelection(id)
  hydrate()
  await nextTick()
  document.querySelector('.workbench-content')?.scrollTo({ top: 0, behavior: 'smooth' })
  switching.value = false
}

async function chooseCandidate(id: number) {
  candidateListVisible.value = false
  await selectCandidate(id)
}

function goPrevious() {
  if (previousCandidate.value) void selectCandidate(previousCandidate.value.interviewId)
}

function goNext() {
  if (nextCandidate.value) void selectCandidate(nextCandidate.value.interviewId)
}

function handleShortcut(event: KeyboardEvent) {
  if (!event.altKey) return
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    goPrevious()
  }
  if (event.key === 'ArrowRight') {
    event.preventDefault()
    goNext()
  }
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
    await fetchList(true, true)
  } finally {
    submitting.value = false
  }
}
async function withdraw() {
  submitting.value = true
  try {
    await interviewApi.withdrawFeedback(current.value.interviewId)
    ElMessage.success('已撤回，可继续修改')
    await fetchList(true, true)
  } finally {
    submitting.value = false
  }
}
async function callNextFromWorkbench() {
  if (!current.value?.roomId || !canCallNext.value) return
  callingNext.value = true
  try {
    const candidate = await interviewSessionApi.callNext(current.value.roomId)
    ElMessage.success(`已叫号：${candidate?.applicantName || '下一位候选人'}`)
    await fetchList(true, false)
    if (candidate?.interviewId && items.value.some((item) => item.interviewId === candidate.interviewId)) {
      await selectCandidate(candidate.interviewId)
    }
  } finally {
    callingNext.value = false
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
function feedbackStatus(item: any) {
  return item.ownFeedback?.status === 'submitted'
    ? 'submitted'
    : item.ownFeedback?.status === 'draft'
      ? 'draft'
      : 'pending'
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
onMounted(() => {
  window.addEventListener('keydown', handleShortcut)
  fetchList(false, true)
})
onBeforeUnmount(() => {
  if (saveTimer) clearTimeout(saveTimer)
  window.removeEventListener('keydown', handleShortcut)
})
</script>

<style scoped>
.workbench-shell {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
  min-height: 100dvh;
  color: var(--el-text-color-primary);
  background: var(--oa-page-soft-bg);
}

.workbench-header {
  min-height: 72px;
  display: grid;
  grid-template-columns: minmax(250px, 1fr) minmax(320px, auto) minmax(250px, 1fr);
  align-items: center;
  gap: 24px;
  padding: 12px clamp(20px, 3vw, 48px);
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: color-mix(in srgb, var(--el-bg-color) 96%, transparent);
}

.workbench-brand,
.current-candidate,
.workbench-header__actions,
.section-heading,
.evaluation-panel__header,
.team-progress > div,
.group-feedback > div,
.workbench-footer,
.footer-center {
  display: flex;
  align-items: center;
}

.workbench-brand {
  gap: 14px;
}

.exit-button {
  min-width: 44px;
  min-height: 44px;
}

.workbench-brand span,
.section-heading span,
.evaluation-panel__header > div > span,
.profile-section-title span {
  display: block;
  margin-bottom: 3px;
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.13em;
}

.workbench-brand h1,
.section-heading h2,
.evaluation-panel__header h2,
.profile-section-title h3 {
  margin: 0;
  line-height: 1.2;
}

.workbench-brand h1 {
  font-size: 19px;
}

.current-candidate {
  justify-self: center;
  gap: 12px;
  min-width: 0;
  padding: 8px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-fill-color-extra-light);
}

.current-candidate__number {
  color: var(--el-color-primary);
  font-size: 15px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.current-candidate div {
  display: grid;
  min-width: 0;
}

.current-candidate strong,
.current-candidate small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.current-candidate small {
  margin-top: 2px;
  color: var(--oa-muted);
}

.workbench-header__actions {
  justify-self: end;
  justify-content: flex-end;
  gap: 14px;
}

.candidate-progress {
  color: var(--oa-muted);
  font-size: 14px;
  font-variant-numeric: tabular-nums;
}

.candidate-progress strong {
  color: var(--el-text-color-primary);
  font-size: 18px;
}

.workbench-content {
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  scroll-behavior: smooth;
}

.workbench-columns {
  width: min(1600px, 100%);
  min-height: 100%;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(330px, 0.8fr) minmax(560px, 1.35fr);
  align-items: start;
  gap: clamp(20px, 3vw, 44px);
  padding: clamp(24px, 3vw, 48px) clamp(20px, 4vw, 64px) 64px;
}

.candidate-profile,
.evaluation-panel {
  min-width: 0;
  padding: clamp(22px, 2.5vw, 36px);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 20px;
  background: var(--el-bg-color);
}

.candidate-profile {
  position: sticky;
  top: 32px;
}

.section-heading,
.evaluation-panel__header {
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.section-heading h2 {
  font-size: clamp(24px, 2.2vw, 34px);
}

.profile-descriptions :deep(.el-descriptions__label) {
  width: 112px;
  color: var(--oa-muted);
  font-weight: 500;
}

.profile-section-title {
  margin: 32px 0 14px;
}

.profile-section-title h3 {
  font-size: 18px;
}

.profile-answers {
  display: grid;
  gap: 12px;
}

.profile-answers article {
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-fill-color-extra-light);
}

.profile-answers span {
  color: var(--oa-muted);
  font-size: 13px;
}

.profile-answers p,
.group-feedback p {
  margin: 6px 0 0;
  line-height: 1.7;
  white-space: pre-wrap;
}

.evaluation-panel__header h2 {
  font-size: clamp(21px, 1.8vw, 28px);
}

.evaluation-panel__header small {
  display: block;
  margin-top: 6px;
  color: var(--oa-muted);
}

.save-state {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex: 0 0 auto;
  padding: 7px 10px;
  border-radius: 999px;
  color: var(--oa-muted);
  background: var(--el-fill-color-light);
  font-size: 12px;
}

.save-state__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-color-success);
}

.save-state--saving .save-state__dot {
  background: var(--el-color-warning);
}

.team-progress {
  margin-bottom: 22px;
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--el-fill-color-extra-light);
}

.team-progress > div {
  justify-content: space-between;
  margin-bottom: 9px;
  color: var(--oa-muted);
  font-size: 13px;
}

.team-progress strong {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-variant-numeric: tabular-nums;
}

.dimension-list {
  display: grid;
  margin-bottom: 30px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.dimension-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  min-height: 74px;
  padding: 14px 2px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.dimension-item em {
  color: var(--el-color-danger);
  font-style: normal;
}

.dimension-item p {
  margin: 5px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.5;
}

.detail-form__row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.suggestion-group {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  width: 100%;
}

.suggestion-group :deep(.el-radio-button__inner) {
  width: 100%;
  min-height: 44px;
  display: grid;
  place-items: center;
}

.group-feedback {
  margin-top: 12px;
}

.group-feedback > div {
  justify-content: space-between;
  gap: 12px;
}

.workbench-footer {
  z-index: 2;
  justify-content: space-between;
  gap: 20px;
  min-height: 88px;
  padding: 14px clamp(20px, 3vw, 48px) calc(14px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--el-border-color);
  background: color-mix(in srgb, var(--el-bg-color) 97%, transparent);
}

.candidate-nav-button {
  min-width: 180px;
  min-height: 52px;
  display: inline-grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto;
  column-gap: 10px;
  text-align: left;
}

.candidate-nav-button :deep(.el-icon) {
  grid-row: 1 / span 2;
  align-self: center;
}

.candidate-nav-button span {
  font-weight: 700;
}

.candidate-nav-button small {
  max-width: 120px;
  overflow: hidden;
  color: var(--oa-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.candidate-nav-button--next {
  grid-template-columns: 1fr auto;
  text-align: right;
}

.candidate-nav-button--next :deep(.el-icon) {
  grid-column: 2;
}

.footer-center {
  justify-content: center;
  gap: 14px;
  min-width: 0;
}

.footer-center > span {
  max-width: 240px;
  overflow: hidden;
  color: var(--oa-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footer-center .el-button {
  min-width: 132px;
  min-height: 48px;
}

.candidate-drawer__toolbar {
  position: sticky;
  top: 0;
  z-index: 2;
  display: grid;
  gap: 12px;
  padding-bottom: 16px;
  background: var(--el-bg-color);
}

.candidate-drawer__toolbar :deep(.el-segmented) {
  width: 100%;
}

.candidate-drawer__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--oa-muted);
}

.candidate-drawer__summary small {
  text-align: right;
}

.candidate-options {
  display: grid;
  gap: 8px;
  padding-bottom: 24px;
}

.candidate-option {
  width: 100%;
  min-height: 68px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  color: inherit;
  text-align: left;
  background: var(--el-bg-color);
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.candidate-option:hover,
.candidate-option:focus-visible {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
  outline: none;
}

.candidate-option:focus-visible {
  box-shadow: 0 0 0 3px var(--el-color-primary-light-7);
}

.candidate-option--active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.candidate-option__number {
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.candidate-option__identity {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.candidate-option__identity strong,
.candidate-option__identity small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.candidate-option__identity small {
  color: var(--oa-muted);
}

@media (max-width: 1050px) {
  .workbench-header {
    grid-template-columns: 1fr auto;
  }

  .current-candidate {
    display: none;
  }

  .workbench-columns {
    grid-template-columns: 1fr;
    max-width: 820px;
  }

  .candidate-profile {
    position: static;
  }
}

@media (max-width: 700px) {
  .workbench-header {
    min-height: 64px;
    gap: 12px;
    padding: 10px 14px;
  }

  .workbench-brand span,
  .workbench-header__actions .candidate-progress,
  .workbench-header__actions .el-button span {
    display: none;
  }

  .workbench-brand h1 {
    font-size: 16px;
  }

  .workbench-columns {
    gap: 14px;
    padding: 14px 12px 32px;
  }

  .candidate-profile,
  .evaluation-panel {
    padding: 18px 14px;
    border-radius: 14px;
  }

  .section-heading,
  .evaluation-panel__header,
  .dimension-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .dimension-item {
    gap: 12px;
  }

  .detail-form__row {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .suggestion-group {
    grid-template-columns: 1fr;
  }

  .suggestion-group :deep(.el-radio-button:first-child .el-radio-button__inner),
  .suggestion-group :deep(.el-radio-button:last-child .el-radio-button__inner) {
    border-radius: 0;
  }

  .workbench-footer {
    min-height: 76px;
    gap: 8px;
    padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
  }

  .candidate-nav-button {
    min-width: 52px;
    width: 52px;
    padding-inline: 0;
    display: inline-flex;
    justify-content: center;
  }

  .candidate-nav-button span,
  .candidate-nav-button small,
  .footer-center > span {
    display: none;
  }

  .footer-center {
    flex: 1;
  }

  .footer-center .el-button {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .workbench-content {
    scroll-behavior: auto;
  }
}
</style>
