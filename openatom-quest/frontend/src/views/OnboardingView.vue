<template>
  <main class="onboarding-screen">
    <div class="onboarding-background" aria-hidden="true"></div>

    <div
      class="onboarding-progress"
      role="progressbar"
      aria-label="新人引导进度"
      :aria-valuenow="activeStep + 1"
      aria-valuemin="1"
      :aria-valuemax="steps.length"
    >
      <span :style="{ width: `${((activeStep + 1) / steps.length) * 100}%` }"></span>
    </div>

    <header class="onboarding-header">
      <div class="onboarding-brand">
        <img src="/logo.png" alt="" />
        <span><strong>JMI-OPENATOM-QUEST</strong><small>开放原子开源社团</small></span>
      </div>
      <div class="onboarding-step-count">
        <span>新人引导</span>
        <strong>{{ String(activeStep + 1).padStart(2, '0') }} / {{ String(steps.length).padStart(2, '0') }}</strong>
      </div>
    </header>

    <section class="onboarding-stage" aria-labelledby="onboarding-title">
      <div v-if="loading" class="onboarding-message">正在读取引导进度…</div>
      <div v-else-if="loadError" class="onboarding-message">
        <h1 id="onboarding-title">暂时无法读取引导进度</h1>
        <p>{{ loadError }}</p>
        <el-button type="primary" size="large" @click="loadProgress">重试</el-button>
      </div>
      <Transition v-else name="onboarding-slide" mode="out-in">
        <div :key="activeStep" class="onboarding-scene">
          <div class="onboarding-emblem" aria-hidden="true">
            <img v-if="activeStep === 0" src="/logo.png" alt="" />
            <span v-else>{{ String(activeStep + 1).padStart(2, '0') }}</span>
          </div>

          <p class="onboarding-eyebrow">STEP {{ String(activeStep + 1).padStart(2, '0') }} <span>·</span> {{ steps[activeStep].short }}</p>
          <h1 id="onboarding-title">{{ steps[activeStep].title }}</h1>
          <p class="onboarding-description">{{ steps[activeStep].description }}</p>

          <div v-if="activeStep === 3" class="onboarding-answer">
            <label for="onboarding-skills">写下你目前接触过的技术</label>
            <el-input
              id="onboarding-skills"
              v-model="skillNote"
              type="textarea"
              :rows="5"
              placeholder="例如：熟悉 Java 基础，做过 Vue 项目，刚开始学习 Git…"
            />
          </div>
          <div v-else-if="activeStep === 4" class="onboarding-answer onboarding-answer--choices">
            <p id="onboarding-assessment-label">选择最符合你当前状态的一项</p>
            <el-radio-group v-model="assessment" aria-labelledby="onboarding-assessment-label">
              <el-radio-button label="beginner">刚刚开始</el-radio-button>
              <el-radio-button label="basic">有一些基础</el-radio-button>
              <el-radio-button label="experienced">做过完整项目</el-radio-button>
            </el-radio-group>
          </div>
          <div v-else class="onboarding-note">{{ steps[activeStep].note }}</div>

          <div class="onboarding-actions" :class="{ 'onboarding-actions--first': activeStep === 0 }">
            <el-button v-if="activeStep > 0" size="large" :disabled="saving" @click="activeStep--">上一步</el-button>
            <el-button type="primary" size="large" :loading="saving" @click="next">
              {{ activeStep === steps.length - 1 ? '完成引导，开始 Quest' : '完成并继续' }}
              <span v-if="!saving" aria-hidden="true">&nbsp;→</span>
            </el-button>
          </div>
        </div>
      </Transition>
    </section>

    <footer class="onboarding-footer">OPEN SOURCE · OPEN MIND · OPEN FUTURE</footer>
  </main>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getErrorMessage, http } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const activeStep = ref(0)
const loading = ref(true)
const loadError = ref('')
const saving = ref(false)
const skillNote = ref('')
const assessment = ref('beginner')
const draftKey = `quest-onboarding-draft:${auth.member?.id || 'current'}`
const steps = [
  { short: '认识社团', title: '了解开放原子开源社团', description: '我们通过技术分享、项目实践与开源协作，让学习真正产生价值。', note: '社团鼓励真实、友善、可追溯的协作。每一个任务都会说明目标、提交要求与验收标准。' },
  { short: '行为准则', title: '确认成员行为准则', description: '尊重他人、诚实记录、保护隐私，并对自己的提交负责。', note: '你已经在资料完善时确认行为准则，这一步帮助你再次理解协作边界。' },
  { short: '技术方向', title: '确认你的技术方向', description: '系统会基于所选方向推荐成长路线，后续仍可调整。', note: '你的主方向已经保存。完成引导后可以在个人资料中维护更多兴趣方向。' },
  { short: '技能自评', title: '写下当前的技能基础', description: '没有标准答案，真实的起点才能得到更合适的任务。', note: '' },
  { short: '基础测评', title: '完成基础能力自评', description: '选择最符合当前状态的一项，导师会结合后续成果提供建议。', note: '' },
  { short: '推荐路线', title: '查看推荐的成长路线', description: '你将从 L0 新人报到开始，逐步走向真实项目贡献。', note: 'L0 新人报到 → L1 开源入门 → L2 技术学习 → L3 项目实战 → L4 项目贡献' },
  { short: '首个任务', title: '准备领取第一个新人任务', description: '完成引导后，工作台会把最合适的下一项任务放在最醒目的位置。', note: '建议先完成「主站探索」：在三个页面找到个人标记，提交后会立即自动核验。' },
]

onMounted(() => {
  const storedTheme = localStorage.getItem('quest-theme')
  const dark = storedTheme ? storedTheme === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
  void loadProgress()
})

async function loadProgress() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await http.get('/members/me/onboarding')
    const progress = response.data.data as { currentStep?: number; selfAssessmentJson?: string }
    activeStep.value = Math.max(0, Math.min(steps.length - 1, (progress.currentStep || 1) - 1))
    if (progress.selfAssessmentJson) {
      try {
        const saved = JSON.parse(progress.selfAssessmentJson)
        skillNote.value = saved.skillNote || ''
        assessment.value = saved.assessment || 'beginner'
      } catch { /* 服务端数据异常时保留默认值 */ }
    }
    const draft = localStorage.getItem(draftKey)
    if (draft) {
      try {
        const saved = JSON.parse(draft)
        skillNote.value = saved.skillNote || skillNote.value
        assessment.value = saved.assessment || assessment.value
      } catch { localStorage.removeItem(draftKey) }
    }
  } catch (error) {
    loadError.value = getErrorMessage(error, '请检查网络后重试')
  } finally {
    loading.value = false
  }
}

watch([skillNote, assessment], () => localStorage.setItem(draftKey, JSON.stringify({ skillNote: skillNote.value, assessment: assessment.value })))

async function next() {
  saving.value = true
  try {
    await http.put('/members/me/onboarding', { step: activeStep.value + 1, skillNote: skillNote.value, assessment: assessment.value })
    if (activeStep.value === steps.length - 1) {
      await auth.resolve(true)
      localStorage.removeItem(draftKey)
      ElMessage.success('新人引导已完成，欢迎开始你的 Quest')
      await router.push('/dashboard')
    } else activeStep.value++
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '保存失败，请重试'))
  } finally {
    saving.value = false
  }
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

.onboarding-answer label,
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

@media (max-width: 640px) {
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
