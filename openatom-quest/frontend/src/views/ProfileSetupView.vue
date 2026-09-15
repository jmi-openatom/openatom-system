<template>
  <div class="page-container setup-page">
    <header class="page-heading">
      <p class="eyebrow">MEMBER PROFILE</p>
      <h1>先认识一下你</h1>
      <p>这些资料用于推荐成长路线和安排合适的导师，不会改变你的 OpenAtom 账号信息。</p>
    </header>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="setup-form">
      <section class="content-card form-section">
        <div class="section-heading"><div><p class="eyebrow">BASIC</p><h2>基本资料</h2></div><span>必填项已标注</span></div>
        <div class="form-grid">
          <el-form-item label="姓名或社团昵称" prop="nickname"><el-input v-model="form.nickname" maxlength="64" show-word-limit /></el-form-item>
          <el-form-item label="头像地址"><el-input v-model="form.avatarUrl" placeholder="https://..." /></el-form-item>
          <el-form-item label="学校" prop="school"><el-input v-model="form.school" /></el-form-item>
          <el-form-item label="学院" prop="college"><el-input v-model="form.college" /></el-form-item>
          <el-form-item label="专业" prop="major"><el-input v-model="form.major" /></el-form-item>
          <el-form-item label="年级" prop="grade"><el-input v-model="form.grade" placeholder="例如：2026级" /></el-form-item>
        </div>
      </section>

      <section class="content-card form-section">
        <div class="section-heading"><div><p class="eyebrow">DIRECTION</p><h2>学习方向</h2></div></div>
        <el-form-item label="感兴趣的技术方向" prop="directionIds">
          <el-select v-model="form.directionIds" multiple placeholder="至少选择一个方向" style="width: 100%">
            <el-option v-for="item in directions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="已掌握的技能"><el-select v-model="form.skills" multiple allow-create filterable default-first-option placeholder="输入技能并回车" style="width: 100%" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="GitHub 或 Gitee 主页"><el-input v-model="form.codeProfileUrl" placeholder="https://github.com/..." /></el-form-item>
          <el-form-item label="每周可投入时间" prop="weeklyHours"><el-input-number v-model="form.weeklyHours" :min="0" :max="168" /><span class="field-suffix">小时</span></el-form-item>
        </div>
        <el-form-item label="个人简介"><el-input v-model="form.bio" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item>
        <el-form-item prop="conductAgreed"><el-checkbox v-model="form.conductAgreed">我已阅读并同意社团成员行为准则</el-checkbox></el-form-item>
      </section>

      <div class="form-actions"><el-button type="primary" size="large" :loading="saving" @click="submit">保存并开始新人引导</el-button></div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { http, type ApiResponse } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

interface Direction { id: number; name: string }
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const directions = ref<Direction[]>([])
const draftKey = computedDraftKey()
const form = reactive({ nickname: auth.member?.nickname || '', avatarUrl: auth.member?.avatarUrl || '', school: '', college: '', major: '', grade: '', directionIds: [] as number[], skills: [] as string[], codeProfileUrl: '', weeklyHours: 4, bio: '', conductAgreed: false })
const rules: FormRules = {
  nickname: [{ required: true, message: '请填写姓名或社团昵称', trigger: 'blur' }],
  school: [{ required: true, message: '请填写学校', trigger: 'blur' }],
  college: [{ required: true, message: '请填写学院', trigger: 'blur' }],
  major: [{ required: true, message: '请填写专业', trigger: 'blur' }],
  grade: [{ required: true, message: '请填写年级', trigger: 'blur' }],
  directionIds: [{ type: 'array', required: true, min: 1, message: '至少选择一个技术方向', trigger: 'change' }],
  conductAgreed: [{ validator: (_rule, value, callback) => value ? callback() : callback(new Error('请先同意成员行为准则')), trigger: 'change' }],
}

onMounted(async () => {
  const [directionResponse, profileResponse] = await Promise.all([
    http.get<ApiResponse<Direction[]>>('/directions'),
    http.get<ApiResponse<Record<string, unknown>>>('/members/me/profile'),
  ])
  directions.value = directionResponse.data.data
  Object.assign(form, profileResponse.data.data)
  const draft = localStorage.getItem(draftKey)
  if (draft) {
    try { Object.assign(form, JSON.parse(draft)); ElMessage.info('已恢复未提交的资料草稿') } catch { localStorage.removeItem(draftKey) }
  }
})

watch(form, (value) => localStorage.setItem(draftKey, JSON.stringify(value)), { deep: true })

function computedDraftKey() { return `quest-profile-draft:${auth.member?.id || 'current'}` }

async function submit() {
  if (!await formRef.value?.validate()) return
  saving.value = true
  try {
    await http.put('/members/me/profile', form)
    localStorage.removeItem(draftKey)
    await auth.resolve(true)
    ElMessage.success('资料已保存')
    await router.push('/onboarding')
  } finally { saving.value = false }
}
</script>
