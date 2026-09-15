<template>
  <div class="page-container onboarding-page">
    <header class="page-heading">
      <p class="eyebrow">ONBOARDING · {{ activeStep + 1 }}/7</p>
      <h1>新人引导</h1>
      <p>用几分钟了解社团、确认方向并领取你的第一个任务。</p>
    </header>

    <section class="content-card onboarding-card">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step v-for="item in steps" :key="item.title" :title="item.short" />
      </el-steps>
      <div class="onboarding-content">
        <p class="eyebrow">STEP {{ String(activeStep + 1).padStart(2, '0') }}</p>
        <h2>{{ steps[activeStep].title }}</h2>
        <p>{{ steps[activeStep].description }}</p>
        <el-input v-if="activeStep === 3" v-model="skillNote" type="textarea" :rows="5" placeholder="例如：熟悉 Java 基础，做过 Vue 项目，刚开始学习 Git…" />
        <el-radio-group v-else-if="activeStep === 4" v-model="assessment">
          <el-radio-button label="beginner">刚刚开始</el-radio-button>
          <el-radio-button label="basic">有一些基础</el-radio-button>
          <el-radio-button label="experienced">做过完整项目</el-radio-button>
        </el-radio-group>
        <div v-else class="onboarding-note">{{ steps[activeStep].note }}</div>
      </div>
      <footer class="onboarding-actions">
        <el-button :disabled="activeStep === 0" @click="activeStep--">上一步</el-button>
        <el-button type="primary" :loading="saving" @click="next">{{ activeStep === 6 ? '完成引导' : '完成并继续' }}</el-button>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const activeStep = ref(0)
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
  { short: '首个任务', title: '准备领取第一个新人任务', description: '完成引导后，工作台会把最合适的下一项任务放在最醒目的位置。', note: '建议首个任务：完善个人成长档案并熟悉 OpenAtom Quest 的提交与反馈流程。' },
]

onMounted(async () => {
  const response = await http.get('/members/me/onboarding')
  const progress = response.data.data as { currentStep?: number; selfAssessmentJson?: string }
  activeStep.value = Math.max(0, Math.min(6, (progress.currentStep || 1) - 1))
  if (progress.selfAssessmentJson) {
    try { const saved = JSON.parse(progress.selfAssessmentJson); skillNote.value = saved.skillNote || ''; assessment.value = saved.assessment || 'beginner' } catch { /* 服务端会拒绝损坏的数据，页面保留默认值 */ }
  }
  const draft = localStorage.getItem(draftKey)
  if (draft) {
    try { const saved = JSON.parse(draft); skillNote.value = saved.skillNote || skillNote.value; assessment.value = saved.assessment || assessment.value } catch { localStorage.removeItem(draftKey) }
  }
})

watch([skillNote, assessment], () => localStorage.setItem(draftKey, JSON.stringify({ skillNote: skillNote.value, assessment: assessment.value })))

async function next() {
  saving.value = true
  try {
    await http.put('/members/me/onboarding', { step: activeStep.value + 1, skillNote: skillNote.value, assessment: assessment.value })
    if (activeStep.value === 6) {
      await auth.resolve(true)
      localStorage.removeItem(draftKey)
      ElMessage.success('新人引导已完成，欢迎开始你的 Quest')
      await router.push('/dashboard')
    } else activeStep.value++
  } finally { saving.value = false }
}
</script>
