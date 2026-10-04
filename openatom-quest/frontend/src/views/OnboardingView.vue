<template>
  <main class="onboarding-screen">
    <div class="onboarding-background" aria-hidden="true"></div>
    <div class="onboarding-progress" role="progressbar" aria-label="新人引导进度"
      :aria-valuenow="activeStep + 1" aria-valuemin="1" :aria-valuemax="steps.length">
      <span :style="{ width: `${((activeStep + 1) / steps.length) * 100}%` }"></span>
    </div>

    <header class="onboarding-header">
      <div class="onboarding-brand">
        <img src="/logo.png?v=20261005" alt="" />
        <span><strong>JMI-OPENATOM-QUEST</strong><small>开放原子开源社团</small></span>
      </div>
      <div class="onboarding-step-count">
        <span>新人引导</span>
        <strong>{{ String(activeStep + 1).padStart(2, '0') }} / 07</strong>
      </div>
    </header>

    <section class="onboarding-stage" aria-labelledby="onboarding-title">
      <div v-if="loading" class="onboarding-message">正在读取引导进度…</div>
      <div v-else-if="loadError" class="onboarding-message">
        <h1 id="onboarding-title">暂时无法读取引导</h1>
        <p>{{ loadError }}</p>
        <el-button type="primary" size="large" @click="loadProgress">重试</el-button>
      </div>
      <Transition v-else name="onboarding-slide" mode="out-in">
        <div :key="activeStep" class="onboarding-scene">
          <div class="onboarding-emblem" aria-hidden="true">
            <img v-if="activeStep === 0" src="/logo.png?v=20261005" alt="" />
            <span v-else>{{ String(activeStep + 1).padStart(2, '0') }}</span>
          </div>
          <p class="onboarding-eyebrow">STEP {{ String(activeStep + 1).padStart(2, '0') }} <span>·</span> {{ steps[activeStep].short }}</p>
          <h1 id="onboarding-title">{{ steps[activeStep].title }}</h1>
          <p class="onboarding-description">{{ steps[activeStep].description }}</p>

          <div v-if="activeStep === 0" class="onboarding-answer">
            <div class="onboarding-identity">
              <span class="onboarding-avatar">
                <img v-if="form.avatarUrl && !avatarFailed" :src="form.avatarUrl" alt="LMS 头像" @error="avatarFailed = true" />
                <span v-else>{{ form.nickname.trim().slice(0, 1) || '新' }}</span>
              </span>
              <span><strong>你的社团名片</strong><small>头像与学籍信息由 LMS 自动同步</small></span>
            </div>
            <label for="onboarding-nickname">姓名或社团昵称</label>
            <el-input id="onboarding-nickname" v-model="form.nickname" maxlength="64" show-word-limit placeholder="让大家知道如何称呼你" />
            <dl class="onboarding-education">
              <div v-for="field in educationFields" :key="field.key"><dt>{{ field.label }}</dt><dd>{{ form[field.key] || 'LMS 暂无数据' }}</dd></div>
            </dl>
          </div>

          <div v-else-if="activeStep === 1" class="onboarding-answer">
            <ul class="onboarding-conduct">
              <li><strong>友善协作</strong><span>尊重不同经验与观点，讨论围绕问题展开。</span></li>
              <li><strong>真实提交</strong><span>如实记录自己的工作，注明引用与 AI 辅助内容。</span></li>
              <li><strong>保护隐私</strong><span>不公开他人资料、访问凭证或未获授权的项目内容。</span></li>
              <li><strong>对成果负责</strong><span>遵守任务期限，遇到困难及时沟通并补充验证说明。</span></li>
            </ul>
            <el-checkbox v-model="form.conductAgreed">我已阅读并同意社团成员行为准则</el-checkbox>
          </div>

          <div v-else-if="activeStep === 2" class="onboarding-answer onboarding-answer--wide">
            <p>至少选择一个方向。第一个选择的方向作为主方向，可取消后重新选择。</p>
            <div class="onboarding-direction-grid" role="group" aria-label="选择感兴趣的技术方向">
              <button v-for="direction in directions" :key="direction.id" type="button"
                class="onboarding-choice" :class="{ 'is-selected': form.directionIds.includes(direction.id) }"
                :aria-pressed="form.directionIds.includes(direction.id)" @click="toggleDirection(direction.id)">
                <span class="onboarding-choice-heading"><strong>{{ direction.name }}</strong><span v-if="form.directionIds[0] === direction.id" class="onboarding-choice-tag">主方向</span><span v-else-if="form.directionIds.includes(direction.id)" class="onboarding-choice-tag">已选</span></span>
                <span>{{ direction.description || '通过任务实践，逐步建立这一方向的技术基础。' }}</span>
              </button>
            </div>
            <p v-if="!directions.length" class="onboarding-step-error" role="alert">暂时没有可选方向，请联系管理员配置技术方向。</p>
          </div>

          <div v-else-if="activeStep === 3" class="onboarding-answer">
            <el-form label-position="top" :model="form">
              <el-form-item label="已接触或掌握的技能（可选）">
                <el-select v-model="form.skills" multiple allow-create filterable default-first-option placeholder="选择技能，也可输入后回车添加" style="width: 100%">
                  <el-option v-for="skill in skillOptions" :key="skill" :label="skill" :value="skill" />
                </el-select>
              </el-form-item>
              <div class="onboarding-field-grid">
                <el-form-item label="GitHub 或 Gitee 主页（可选）"><el-input v-model="form.codeProfileUrl" maxlength="512" placeholder="https://github.com/..." /></el-form-item>
                <el-form-item label="每周可投入时间"><div class="onboarding-hours"><el-input-number v-model="form.weeklyHours" :min="0" :max="168" /><span>小时</span></div></el-form-item>
              </div>
              <el-form-item label="技能基础与学习经历（可选）"><el-input v-model="skillNote" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="例如：熟悉 Java 基础，刚开始学习 Git…" /></el-form-item>
              <el-form-item label="个人简介（可选）"><el-input v-model="form.bio" type="textarea" :rows="2" maxlength="1000" show-word-limit placeholder="你希望在社团学习或参与什么？" /></el-form-item>
            </el-form>
          </div>

          <div v-else-if="activeStep === 4" class="onboarding-answer onboarding-answer--choices">
            <p id="onboarding-assessment-label">选择最符合你当前状态的一项</p>
            <el-radio-group v-model="assessment" aria-labelledby="onboarding-assessment-label">
              <el-radio-button value="beginner">刚刚开始</el-radio-button>
              <el-radio-button value="basic">有一些基础</el-radio-button>
              <el-radio-button value="experienced">做过完整项目</el-radio-button>
            </el-radio-group>
          </div>

          <div v-else-if="activeStep === 5" class="onboarding-answer onboarding-answer--wide">
            <p v-if="routesLoading">正在根据你的方向推荐路线…</p>
            <div v-else-if="routesError" class="onboarding-inline-error"><p>{{ routesError }}</p><el-button @click="loadRoutes">重新加载路线</el-button></div>
            <div v-else-if="routes.length" class="onboarding-route-grid" role="radiogroup" aria-label="选择成长路线">
              <button v-for="routeItem in routes" :key="routeItem.id" type="button" role="radio"
                class="onboarding-choice" :class="{ 'is-selected': selectedRouteId === routeItem.id }"
                :aria-checked="selectedRouteId === routeItem.id" @click="selectedRouteId = routeItem.id">
                <span class="onboarding-choice-heading"><strong>{{ routeItem.name }}</strong><span v-if="selectedRouteId === routeItem.id" class="onboarding-choice-tag">已选</span></span>
                <span>{{ routeItem.description }}</span>
                <small>{{ routeItem.directionName }} · {{ routeItem.stageCount }} 个阶段 · {{ routeItem.taskCount }} 项任务<span v-if="routeItem.memberStatus !== 'NOT_ENROLLED'"> · 已加入</span></small>
              </button>
            </div>
            <div v-else class="onboarding-note">当前方向的成长路线还在准备中。你可以先完成新人任务，后续在工作台加入路线。</div>
          </div>

          <div v-else class="onboarding-answer">
            <dl class="onboarding-summary">
              <div><dt>社团昵称</dt><dd>{{ form.nickname }}</dd></div>
              <div><dt>技术方向</dt><dd>{{ selectedDirectionNames }}</dd></div>
              <div><dt>每周投入</dt><dd>{{ form.weeklyHours ?? 0 }} 小时</dd></div>
              <div><dt>成长路线</dt><dd>{{ selectedRoute?.name || '稍后加入' }}</dd></div>
            </dl>
            <p v-if="firstTaskLoading">正在准备你的第一项任务…</p>
            <div v-else-if="firstTaskError" class="onboarding-inline-error"><p>{{ firstTaskError }}</p><el-button @click="loadFirstTask">重新加载任务</el-button></div>
            <div v-else-if="firstTask" class="onboarding-note onboarding-first-task"><strong>{{ firstTask.title }}</strong><p>{{ firstTask.summary }}</p><p>{{ firstTask.points }} 积分 · {{ firstTask.memberStatus === 'PASSED' ? '你已完成这项任务' : firstTask.assignmentId ? '已领取，完成引导后即可继续' : '完成引导时一起领取，随后直接进入任务' }}</p></div>
            <div v-else class="onboarding-note">新人任务还在准备中。你可以先进入工作台，后续领取已发布的任务。</div>
          </div>

          <p v-if="stepError" class="onboarding-step-error" role="alert">{{ stepError }}</p>
          <div class="onboarding-actions" :class="{ 'onboarding-actions--first': activeStep === 0 }">
            <el-button v-if="activeStep > 0" size="large" :disabled="saving" @click="activeStep--">上一步</el-button>
            <el-button type="primary" size="large" :loading="saving" :disabled="(activeStep === 5 && (routesLoading || !!routesError)) || (activeStep === 6 && (firstTaskLoading || !!firstTaskError))" @click="next">
              {{ activeStep === steps.length - 1 ? (firstTask && !firstTask.assignmentId && firstTask.memberStatus !== 'PASSED' ? '领取任务，完成引导' : '完成引导，开始 Quest') : '保存并继续' }}
              <span v-if="!saving" aria-hidden="true">&nbsp;→</span>
            </el-button>
          </div>
          <p class="onboarding-save-hint">已完成的步骤自动保存，刷新后可继续。</p>
        </div>
      </Transition>
    </section>
    <footer class="onboarding-footer">OPEN SOURCE · OPEN MIND · OPEN FUTURE</footer>
  </main>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getErrorMessage, http, type ApiResponse } from '@/api/http'
import { claimTask, enrollRoute, getRoutes, getTasks, type TaskSummary } from '@/api/quest'
import { useAuthStore } from '@/stores/auth'

interface Direction { id: number; name: string; description?: string }
interface GrowthRoute { id: number; name: string; description: string; directionName: string; stageCount: number; taskCount: number; memberStatus: string }
interface ProfileForm {
  nickname: string; avatarUrl: string; school: string; college: string; major: string; grade: string;
  directionIds: number[]; skills: string[]; codeProfileUrl: string; weeklyHours: number; bio: string; conductAgreed: boolean;
}
const router = useRouter()
const auth = useAuthStore()
const activeStep = ref(0), loading = ref(true), loadError = ref(''), saving = ref(false), stepError = ref('')
const skillNote = ref(''), assessment = ref('beginner'), avatarFailed = ref(false)
const directions = ref<Direction[]>([]), routes = ref<GrowthRoute[]>([])
const routesLoading = ref(false), routesError = ref(''), selectedRouteId = ref<number | null>(null)
const firstTask = ref<TaskSummary>(), firstTaskLoading = ref(false), firstTaskError = ref('')
const selectedRoute = computed(() => routes.value.find(item => item.id === selectedRouteId.value))
const selectedDirectionNames = computed(() => form.directionIds.map(id => directions.value.find(item => item.id === id)?.name).filter(Boolean).join('、'))
const draftKey = `quest-onboarding-draft:${auth.member?.id || 'current'}`
const profileDraftKey = `quest-profile-draft:${auth.member?.id || 'current'}`
let hydrated = false
const form = reactive<ProfileForm>({ nickname: auth.member?.nickname || '', avatarUrl: '', school: '', college: '', major: '', grade: '', directionIds: [], skills: [], codeProfileUrl: '', weeklyHours: 4, bio: '', conductAgreed: false })
const educationFields = [{ key: 'school', label: '学校' }, { key: 'college', label: '学院' }, { key: 'major', label: '专业' }, { key: 'grade', label: '年级' }] as const
const skillOptions = ['HTML / CSS', 'JavaScript', 'TypeScript', 'Vue', 'React', 'Node.js', 'Java', 'Spring Boot', 'Python', 'FastAPI', 'MySQL', 'Redis', 'Git', 'Linux', 'Docker', 'GitHub Actions', 'REST API', 'OpenHarmony', 'ArkTS', '人工智能', 'UI/UX', 'Figma', '技术写作']
const steps = [
  { short: '认识社团', title: '欢迎加入开放原子开源社团', description: '从这里开始，用七步认识社团、完善资料，并找到适合你的成长路线。' },
  { short: '行为准则', title: '一起建立友善的协作环境', description: '尊重他人、诚实记录、保护隐私，并对自己的提交负责。' },
  { short: '技术方向', title: '选择你想探索的技术方向', description: '从兴趣出发，系统会根据你的选择推荐成长路线，之后仍可调整。' },
  { short: '技能基础', title: '让我们了解你的起点', description: '零基础也可以开始。记录已有技能与可投入的时间，让学习节奏更适合你。' },
  { short: '基础测评', title: '你现在处于哪一个阶段？', description: '如实选择当前状态，导师会结合后续成果提供建议。' },
  { short: '成长路线', title: '选择一条成长路线', description: '根据你刚刚选择的方向，从 L0 新人报到逐步走向真实项目贡献。' },
  { short: '首个任务', title: '用第一项任务开启 Quest', description: '确认你的成长名片，在这里领取新人任务，开启你的开源实践。' },
]

onMounted(() => {
  const storedTheme = localStorage.getItem('quest-theme')
  const dark = storedTheme ? storedTheme === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
  void loadProgress()
})

function profilePayload() {
  return { nickname: form.nickname, conductAgreed: form.conductAgreed, directionIds: [...form.directionIds], skills: [...form.skills], codeProfileUrl: form.codeProfileUrl, weeklyHours: form.weeklyHours, bio: form.bio }
}

function restoreProfile(profile: Partial<ProfileForm>) {
  for (const key of ['nickname', 'codeProfileUrl', 'bio'] as const) if (typeof profile[key] === 'string') form[key] = profile[key]
  if (Array.isArray(profile.directionIds)) form.directionIds = profile.directionIds.map(Number).filter(id => directions.value.some(item => item.id === id))
  if (Array.isArray(profile.skills)) form.skills = profile.skills.filter(value => typeof value === 'string')
  if (typeof profile.weeklyHours === 'number') form.weeklyHours = profile.weeklyHours
  if (typeof profile.conductAgreed === 'boolean') form.conductAgreed = profile.conductAgreed
}

async function loadProgress() {
  loading.value = true
  loadError.value = ''
  hydrated = false
  try {
    const [progressResponse, profileResponse, directionResponse] = await Promise.all([
      http.get<ApiResponse<{ currentStep?: number; selfAssessmentJson?: string }>>('/members/me/onboarding'),
      http.get<ApiResponse<Partial<ProfileForm>>>('/members/me/profile'),
      http.get<ApiResponse<Direction[]>>('/directions'),
    ])
    directions.value = directionResponse.data.data
    const profile = profileResponse.data.data
    restoreProfile(profile)
    for (const key of ['avatarUrl', 'school', 'college', 'major', 'grade'] as const) form[key] = profile[key] || ''
    const progress = progressResponse.data.data
    activeStep.value = Math.max(0, Math.min(6, (progress.currentStep || 1) - 1))
    if (progress.selfAssessmentJson) {
      try { const saved = JSON.parse(progress.selfAssessmentJson); skillNote.value = saved.skillNote || ''; assessment.value = saved.assessment || 'beginner' } catch { /* 保留默认值 */ }
    }
    const oldProfileDraft = localStorage.getItem(profileDraftKey)
    if (oldProfileDraft) {
      try { restoreProfile(JSON.parse(oldProfileDraft)) } catch { localStorage.removeItem(profileDraftKey) }
    }
    const draft = localStorage.getItem(draftKey)
    if (draft) {
      try {
        const saved = JSON.parse(draft)
        if (saved.profile) restoreProfile(saved.profile)
        skillNote.value = saved.skillNote ?? skillNote.value
        assessment.value = saved.assessment || assessment.value
        selectedRouteId.value = typeof saved.selectedRouteId === 'number' ? saved.selectedRouteId : null
      } catch { localStorage.removeItem(draftKey) }
    }
    if (!form.nickname.trim()) activeStep.value = 0
    else if (!form.conductAgreed && activeStep.value > 1) activeStep.value = 1
    else if (!form.directionIds.length && activeStep.value > 2) activeStep.value = 2
    else if (!auth.member?.profileCompleted && activeStep.value > 3) activeStep.value = 3
    hydrated = true
    if (activeStep.value >= 5) await loadRoutes()
    if (activeStep.value === 6) await loadFirstTask()
  } catch (error) {
    loadError.value = getErrorMessage(error, '请检查网络后重试')
  } finally { loading.value = false }
}

watch([form, skillNote, assessment, selectedRouteId], () => {
  if (hydrated) localStorage.setItem(draftKey, JSON.stringify({ profile: profilePayload(), skillNote: skillNote.value, assessment: assessment.value, selectedRouteId: selectedRouteId.value }))
}, { deep: true })
watch(activeStep, () => { stepError.value = '' })
watch(() => [form.nickname, form.conductAgreed, ...form.directionIds, selectedRouteId.value], () => { stepError.value = '' })

function toggleDirection(id: number) {
  form.directionIds = form.directionIds.includes(id) ? form.directionIds.filter(value => value !== id) : [...form.directionIds, id]
  selectedRouteId.value = null
}

async function loadRoutes() {
  routesLoading.value = true
  routesError.value = ''
  try {
    routes.value = (await getRoutes()).map(item => ({ ...item, id: Number(item.id) })) as GrowthRoute[]
    if (!routes.value.some(item => item.id === selectedRouteId.value)) selectedRouteId.value = routes.value.find(item => item.memberStatus !== 'NOT_ENROLLED')?.id ?? null
  } catch (error) { routesError.value = getErrorMessage(error, '推荐路线加载失败，请重试') }
  finally { routesLoading.value = false }
}

async function loadFirstTask() {
  firstTaskLoading.value = true
  firstTaskError.value = ''
  try { firstTask.value = (await getTasks()).find(item => item.taskKey === 'site-exploration-l0') }
  catch (error) { firstTaskError.value = getErrorMessage(error, '新人任务加载失败，请重试') }
  finally { firstTaskLoading.value = false }
}

async function next() {
  stepError.value = ''
  if (activeStep.value === 0 && !form.nickname.trim()) stepError.value = '请填写姓名或社团昵称'
  if (activeStep.value === 1 && !form.conductAgreed) stepError.value = '请先阅读并同意成员行为准则'
  if (activeStep.value === 2 && !form.directionIds.length) stepError.value = '至少选择一个技术方向'
  if (activeStep.value === 5 && routes.value.length && !selectedRoute.value) stepError.value = '请选择一条成长路线'
  if (stepError.value) return
  saving.value = true
  try {
    if (activeStep.value === 5 && selectedRoute.value?.memberStatus === 'NOT_ENROLLED') {
      await enrollRoute(selectedRoute.value.id)
      selectedRoute.value.memberStatus = 'ACTIVE'
    }
    if (activeStep.value === 6 && firstTask.value && !firstTask.value.assignmentId && firstTask.value.memberStatus !== 'PASSED') {
      const claimed = await claimTask(firstTask.value.id)
      firstTask.value.assignmentId = claimed.assignmentId
      firstTask.value.memberStatus = 'IN_PROGRESS'
    }
    await http.put('/members/me/onboarding', { step: activeStep.value + 1, skillNote: skillNote.value, assessment: assessment.value, profile: profilePayload() })
    if (activeStep.value === steps.length - 1) {
      await auth.resolve(true)
      localStorage.removeItem(draftKey)
      localStorage.removeItem(profileDraftKey)
      ElMessage.success('新人引导已完成，欢迎开始你的 Quest')
      await router.push(firstTask.value?.assignmentId && firstTask.value.memberStatus !== 'PASSED' ? `/assignments/${firstTask.value.assignmentId}` : '/dashboard')
    } else {
      if (activeStep.value === 3) await auth.resolve(true)
      activeStep.value++
      if (activeStep.value === 5) await loadRoutes()
      if (activeStep.value === 6) await loadFirstTask()
      window.scrollTo({ top: 0, behavior: 'instant' })
    }
  } catch (error) { stepError.value = getErrorMessage(error, '保存失败，请重试') }
  finally { saving.value = false }
}
</script>

<style scoped>
.onboarding-screen {
  --el-color-primary: var(--color-primary);
  --el-color-primary-dark-2: var(--color-primary-hover);
  --el-color-primary-light-3: color-mix(in srgb, var(--color-primary) 70%, var(--color-bg-container));
  position: relative;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  color: var(--color-text-primary);
  background: var(--color-bg-page);
}

.onboarding-background {
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background:
    radial-gradient(circle at 15% 18%, color-mix(in srgb, var(--color-text-primary) 5%, transparent), transparent 32%),
    radial-gradient(circle at 85% 82%, color-mix(in srgb, var(--color-text-primary) 4%, transparent), transparent 34%),
    linear-gradient(145deg, var(--color-bg-container), var(--color-bg-page));
}

.onboarding-background::after {
  position: absolute;
  inset: 0;
  content: '';
  opacity: .55;
  background-image:
    linear-gradient(var(--color-border-light) 1px, transparent 1px),
    linear-gradient(90deg, var(--color-border-light) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: linear-gradient(to bottom, #000, transparent 90%);
}

.onboarding-progress {
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  background: var(--color-border-light);
}

.onboarding-progress span {
  display: block;
  height: 100%;
  background: var(--color-primary);
  transition: width 400ms var(--ease-standard);
}

.onboarding-header {
  width: min(100% - 64px, 1320px);
  margin: 0 auto;
  padding: 32px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.onboarding-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.onboarding-brand img {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.onboarding-brand span,
.onboarding-step-count {
  display: grid;
  gap: 3px;
}

.onboarding-brand strong {
  font-size: 13px;
  letter-spacing: .04em;
}

.onboarding-brand small,
.onboarding-step-count span {
  color: var(--color-text-secondary);
  font-size: 11px;
}

.onboarding-step-count {
  text-align: right;
}

.onboarding-step-count strong {
  font-size: 14px;
  letter-spacing: .08em;
}

.onboarding-stage {
  width: 100%;
  flex: 1;
  display: grid;
  place-items: center;
  padding: 24px 24px 56px;
}

.onboarding-scene,
.onboarding-message {
  width: min(100%, 780px);
  margin: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.onboarding-emblem {
  width: 80px;
  height: 80px;
  margin-bottom: 30px;
  display: grid;
  place-items: center;
  border: 1px solid var(--color-border);
  border-radius: 24px;
  background: var(--glass-bg);
  box-shadow: 0 16px 44px color-mix(in srgb, var(--color-text-primary) 8%, transparent);
  backdrop-filter: var(--glass-filter);
}

.onboarding-emblem img {
  width: 58px;
  height: 58px;
  object-fit: contain;
}

.onboarding-emblem span {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: -.04em;
}

.onboarding-eyebrow {
  margin: 0 0 18px;
  padding: 7px 12px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  color: var(--color-text-regular);
  background: var(--glass-bg);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: .12em;
}

.onboarding-eyebrow span {
  padding: 0 4px;
  color: var(--color-text-tertiary);
}

.onboarding-scene h1,
.onboarding-message h1 {
  max-width: 760px;
  margin: 0;
  font-size: clamp(34px, 5vw, 60px);
  font-weight: 600;
  letter-spacing: -.045em;
  line-height: 1.13;
}

.onboarding-description,
.onboarding-message p {
  max-width: 640px;
  margin: 22px 0 0;
  color: var(--color-text-secondary);
  font-size: clamp(15px, 1.5vw, 17px);
  line-height: 1.8;
}

.onboarding-note,
.onboarding-answer {
  width: min(100%, 600px);
  margin-top: 32px;
}

.onboarding-note {
  padding: 20px 24px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-text-regular);
  background: var(--glass-bg);
  font-size: 14px;
  line-height: 1.8;
}

.onboarding-answer {
  text-align: left;
}

.onboarding-answer > label,
.onboarding-answer > p {
  display: block;
  margin: 0 0 12px;
  color: var(--color-text-regular);
  font-size: 13px;
  font-weight: 600;
}

.onboarding-answer :deep(.el-textarea__inner) {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-container);
  box-shadow: none;
  line-height: 1.7;
}

.onboarding-answer :deep(.el-textarea__inner:focus) {
  border-color: var(--color-text-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-text-primary) 12%, transparent);
}

.onboarding-answer--choices {
  text-align: center;
}

.onboarding-answer--choices :deep(.el-radio-group) {
  display: flex;
  justify-content: center;
}

.onboarding-answer--choices :deep(.el-radio-button__inner) {
  min-height: 46px;
  display: grid;
  place-items: center;
  padding: 0 22px;
}

.onboarding-actions {
  width: min(100%, 600px);
  margin-top: 42px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.onboarding-actions--first {
  justify-content: center;
}

.onboarding-actions :deep(.el-button) {
  min-height: 46px;
  padding: 0 24px;
  font-weight: 600;
}

.onboarding-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.onboarding-screen :deep(.el-button--primary) {
  --el-button-text-color: var(--color-primary-foreground);
  --el-button-hover-text-color: var(--color-primary-foreground);
  --el-button-active-text-color: var(--color-primary-foreground);
}

.onboarding-screen :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  color: var(--color-primary-foreground);
}

.onboarding-footer {
  padding: 0 24px 28px;
  color: var(--color-text-tertiary);
  font-size: 10px;
  letter-spacing: .18em;
  text-align: center;
}

.onboarding-message {
  gap: 18px;
}

.onboarding-message :deep(.el-button) {
  margin-top: 12px;
}

.onboarding-slide-enter-active,
.onboarding-slide-leave-active {
  transition: opacity 280ms ease, transform 280ms ease;
}

.onboarding-slide-enter-from {
  opacity: 0;
  transform: translateY(16px);
}

.onboarding-slide-leave-to {
  opacity: 0;
  transform: translateY(-16px);
}

.onboarding-answer--wide { width: min(100%, 780px); }
.onboarding-identity { display: flex; align-items: center; gap: 14px; margin-bottom: 24px; }
.onboarding-avatar { width: 52px; height: 52px; flex-shrink: 0; display: grid; place-items: center; overflow: hidden; border-radius: 50%; color: var(--color-primary-foreground); background: var(--color-primary); font-size: 22px; }
.onboarding-avatar img { width: 100%; height: 100%; object-fit: cover; }
.onboarding-identity strong, .onboarding-identity small { display: block; }
.onboarding-identity small { margin-top: 4px; color: var(--color-text-secondary); font-size: 12px; }
.onboarding-education, .onboarding-field-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.onboarding-education { margin: 20px 0 0; padding: 20px; border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--glass-bg); }
.onboarding-education dt, .onboarding-summary dt { color: var(--color-text-secondary); font-size: 12px; }
.onboarding-education dd { margin: 5px 0 0; font-size: 14px; overflow-wrap: anywhere; }
.onboarding-conduct { display: grid; gap: 18px; margin: 0 0 24px; padding: 24px; border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--glass-bg); list-style: none; }
.onboarding-conduct strong, .onboarding-conduct span { display: block; }
.onboarding-conduct strong { font-size: 14px; }
.onboarding-conduct span { margin-top: 4px; color: var(--color-text-secondary); font-size: 13px; line-height: 1.7; }
.onboarding-answer :deep(.el-checkbox) { height: auto; white-space: normal; }
.onboarding-answer :deep(.el-checkbox__label) { line-height: 1.6; white-space: normal; }
.onboarding-direction-grid, .onboarding-route-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.onboarding-choice { display: flex; flex-direction: column; gap: 10px; padding: 20px; border: 1px solid var(--color-border); border-radius: var(--radius-lg); color: var(--color-text-primary); background: var(--color-bg-container); text-align: left; cursor: pointer; font: inherit; }
.onboarding-choice:hover { border-color: var(--color-text-secondary); }
.onboarding-choice:focus-visible { outline: 3px solid var(--color-text-secondary); outline-offset: 3px; }
.onboarding-choice.is-selected { border-color: var(--color-primary); box-shadow: inset 0 0 0 1px var(--color-primary); background: color-mix(in srgb, var(--color-primary) 4%, var(--color-bg-container)); }
.onboarding-choice-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.onboarding-choice strong { font-size: 15px; }
.onboarding-choice > span:not(.onboarding-choice-heading) { color: var(--color-text-secondary); font-size: 13px; line-height: 1.7; }
.onboarding-choice-tag { flex-shrink: 0; padding: 3px 8px; border-radius: 999px; color: var(--color-primary-foreground); background: var(--color-primary); font-size: 10px; }
.onboarding-choice small { margin-top: auto; color: var(--color-text-secondary); font-size: 11px; line-height: 1.7; }
.onboarding-hours { display: flex; align-items: center; gap: 10px; }
.onboarding-hours > span { color: var(--color-text-secondary); font-size: 13px; }
.onboarding-summary { display: grid; gap: 18px; margin: 0; padding: 24px; border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--glass-bg); }
.onboarding-summary > div { display: grid; grid-template-columns: 80px minmax(0, 1fr); align-items: baseline; gap: 16px; }
.onboarding-summary dd { margin: 0; font-size: 14px; overflow-wrap: anywhere; }
.onboarding-first-task { margin-top: 20px; text-align: left; }
.onboarding-first-task p { margin: 8px 0 0; }
.onboarding-step-error { width: min(100%, 600px); margin: 20px 0 0; color: var(--el-color-danger); font-size: 13px; line-height: 1.6; }
.onboarding-save-hint { margin: 18px 0 0; color: var(--color-text-tertiary); font-size: 11px; }
.onboarding-inline-error { color: var(--el-color-danger); text-align: center; }

@media (max-width: 640px) {
  .onboarding-direction-grid, .onboarding-route-grid, .onboarding-field-grid { grid-template-columns: 1fr; }
  .onboarding-field-grid { gap: 0; }
  .onboarding-choice { padding: 18px; }
  .onboarding-summary { padding: 18px; }
  .onboarding-header {
    width: calc(100% - 32px);
    padding: 22px 0;
  }

  .onboarding-brand img {
    width: 36px;
    height: 36px;
  }

  .onboarding-brand strong {
    font-size: 10px;
  }

  .onboarding-brand small,
  .onboarding-step-count span {
    font-size: 10px;
  }

  .onboarding-stage {
    padding: 20px 20px 44px;
  }

  .onboarding-emblem {
    width: 64px;
    height: 64px;
    margin-bottom: 22px;
    border-radius: 19px;
  }

  .onboarding-emblem img {
    width: 48px;
    height: 48px;
  }

  .onboarding-eyebrow {
    margin-bottom: 16px;
  }

  .onboarding-description {
    margin-top: 18px;
  }

  .onboarding-note,
.onboarding-answer {
    margin-top: 26px;
  }

  .onboarding-note {
    padding: 18px;
  }

  .onboarding-answer--choices :deep(.el-radio-group) {
    width: 100%;
    flex-direction: column;
  }

  .onboarding-answer--choices :deep(.el-radio-button),
  .onboarding-answer--choices :deep(.el-radio-button__inner) {
    width: 100%;
  }

  .onboarding-answer--choices :deep(.el-radio-button__inner) {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
  }

  .onboarding-answer--choices :deep(.el-radio-button + .el-radio-button) {
    margin-top: 8px;
  }

  .onboarding-actions {
    margin-top: 32px;
  }

  .onboarding-actions :deep(.el-button) {
    padding: 0 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .onboarding-slide-enter-active,
  .onboarding-slide-leave-active,
  .onboarding-progress span {
    transition: none;
  }
}
</style>
