<template>
  <ViewPage class="site-system-page">
    <SitePageHero
      eyebrow="在线表单"
      title="表单填写"
      description="请认真填写以下信息，提交后可以在“我的申请”中查看后续进度。"
      compact
    />

    <section class="site-system-section">
      <div class="container apply-grid site-system-grid site-system-grid--split">
        <div class="apply-copy site-system-surface site-system-copy-card site-reveal">
          <div class="apply-note">
            <h3>申请说明</h3>
            <ol>
              <li>确认申请仍处于开放时间内</li>
              <li>按要求填写基础信息和自定义字段</li>
              <li>提交后请保持手机/邮箱畅通，等待面试通知</li>
            </ol>
          </div>
          <div class="apply-note" v-if="formMeta.id">
            <h3>当前表单</h3>
            <p class="campaign-line">
              <strong>{{ formMeta.name || '信息收集表单' }}</strong>
            </p>
            <p class="campaign-line">
              提交方式：{{ requiresLogin ? '需要登录账号' : '支持匿名填写' }}
            </p>
            <p class="campaign-line">
              收集时间：{{ formatRange(formMeta.startAt, formMeta.endAt) }}
            </p>
          </div>
        </div>
        <el-card class="apply-form-card site-reveal" shadow="never">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
            <el-alert
              v-if="!formMeta.id"
              type="warning"
              show-icon
              :closable="false"
              title="表单不存在或暂未开放"
            />
            <el-alert
              v-else-if="requiresLogin && !hasToken"
              type="info"
              show-icon
              :closable="false"
              title="当前表单要求登录后提交，正在跳转到登录页。"
            />
            <el-alert
              v-else-if="!requiresLogin"
              type="success"
              show-icon
              :closable="false"
              title="当前表单支持匿名提交，请确保联系方式填写准确。"
            />
            <el-alert
              v-if="formMeta.id && !canSubmit"
              type="warning"
              show-icon
              :closable="false"
              title="当前表单已结束收集，不能继续提交。"
            />
            <el-form-item label="所属社团">
              <el-input :model-value="club.name || '默认社团'" disabled />
            </el-form-item>
            <el-form-item label="表单名称">
              <el-input :model-value="formMeta.name || '-'" disabled />
            </el-form-item>
            <el-form-item
              v-if="!requiresLogin && !hasApplicantNameField"
              label="联系人"
              prop="formData.anonymousName"
            >
              <el-input v-model="form.formData.anonymousName" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item
              v-if="!requiresLogin && !hasContactField"
              label="联系方式"
              prop="formData.anonymousContact"
            >
              <el-input
                v-model="form.formData.anonymousContact"
                placeholder="请填写手机号、邮箱或微信"
              />
            </el-form-item>
            <el-divider v-if="dynamicFields.length" content-position="left">详细信息</el-divider>
            <template v-for="field in dynamicFields" :key="field.key">
              <el-form-item :label="field.label || field.key" :prop="`formData.${field.key}`">
                <el-select
                  v-if="field.type === 'select'"
                  v-model="form.formData[field.key]"
                  clearable
                  :placeholder="field.placeholder || `请选择${field.label || ''}`"
                >
                  <el-option
                    v-for="option in field.options || []"
                    :key="String(option.value || option)"
                    :label="option.label || option"
                    :value="option.value || option"
                  />
                </el-select>
                <el-input
                  v-else-if="field.type === 'textarea'"
                  v-model="form.formData[field.key]"
                  type="textarea"
                  :rows="4"
                  :placeholder="field.placeholder || `请输入${field.label || ''}`"
                />
                <el-input
                  v-else
                  v-model="form.formData[field.key]"
                  :placeholder="field.placeholder || `请输入${field.label || ''}`"
                />
              </el-form-item>
            </template>
            <el-form-item>
              <el-button
                type="primary"
                :disabled="!formMeta.id || !canSubmit"
                :loading="submitting"
                @click="submitForm"
              >
                提交表单
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import SitePageHero from '@/components/site/shell/SitePageHero.vue'
import { ElMessage } from 'element-plus'
import { formSubmissionApi, clubApi, siteApi } from '@/api'
import { getToken } from '@/utils/auth.ts'
import { formatDateTime } from '@/utils/format.ts'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const club = ref<Record<string, any>>({})

const formMeta = ref<Record<string, any>>({})

const departments = ref<any[]>([])

const submitting = ref(false)

const form = ref({
  formData: {},
})

const rules = ref<Record<string, any>>({})

const formRef = ref<any>()

const router = useRouter()

const route = useRoute()

const hasToken = computed(() => {
  return Boolean(getToken())
})

const requiresLogin = computed(() => {
  return formMeta.value?.loginRequired !== false
})

const canSubmit = computed(() => {
  if (!formMeta.value?.id) return false
  if (['closed', 'archived'].includes(formMeta.value.status)) return false
  if (formMeta.value.startAt && new Date(formMeta.value.startAt).getTime() > Date.now())
    return false
  if (!formMeta.value.endAt) return true
  return new Date(formMeta.value.endAt).getTime() >= Date.now()
})

const dynamicFields = computed(() => {
  return parseSchema(formMeta.value?.formSchema)
})

const hasApplicantNameField = computed(() => {
  return Boolean(findApplicantFieldKey())
})

const hasContactField = computed(() => {
  return Boolean(findContactFieldKey())
})

async function loadFormDetail() {
  const result = await siteApi.formDetail(route.params.id)
  club.value = result?.club || {}
  formMeta.value = {
    ...(result?.form || {}),
    loginRequired: result?.form?.loginRequired !== false,
  }
  if (formMeta.value.id && requiresLogin.value && !hasToken.value) {
    ElMessage.warning('当前表单需要登录后填写，正在跳转到登录页')
    router.replace({ path: '/admin/login', query: { redirect: route.fullPath } })
    return
  }
  if (club.value.id) {
    const deptResult = await clubApi.departments(club.value.id)
    departments.value = deptResult?.list || deptResult || []
  }
  resetForm()
}

function buildRules() {
  const applicantFieldKey = findApplicantFieldKey()
  const contactFieldKey = findContactFieldKey()
  return {
    ...(requiresLogin.value
      ? {}
      : {
          ...(applicantFieldKey
            ? {
                [`formData.${applicantFieldKey}`]: [
                  { required: true, message: '请填写联系人', trigger: 'blur' },
                ],
              }
            : {
                'formData.anonymousName': [
                  { required: true, message: '请填写联系人', trigger: 'blur' },
                ],
              }),
          ...(contactFieldKey
            ? {
                [`formData.${contactFieldKey}`]: [
                  { required: true, message: '请填写联系方式', trigger: 'blur' },
                ],
              }
            : {
                'formData.anonymousContact': [
                  { required: true, message: '请填写联系方式', trigger: 'blur' },
                ],
              }),
        }),
    ...dynamicFields.value.reduce((rules, field) => {
      if (field.required) {
        rules[`formData.${field.key}`] = [
          {
            required: true,
            message: `请填写${field.label || field.key}`,
            trigger: 'blur',
          },
        ]
      }
      return rules
    }, {}),
  }
}

function parseSchema(value: any) {
  if (Array.isArray(value)) return value.filter((item) => item && item.key)
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter((item) => item && item.key) : []
  } catch {
    return []
  }
}

function submitForm() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (requiresLogin.value && !getToken()) {
      ElMessage.warning('请先登录后再提交表单')
      router.push({ path: '/admin/login', query: { redirect: route.fullPath } })
      return
    }
    submitting.value = true
    try {
      const anonymousName = resolveAnonymousName()
      const anonymousContact = resolveAnonymousContact()
      await formSubmissionApi.create(formMeta.value.id, {
        anonymousName,
        anonymousContact,
        formData: form.value.formData,
      })
      ElMessage.success('表单已提交')
      resetForm()
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  const formData = {}
  dynamicFields.value.forEach((field) => {
    formData[field.key] = ''
  })
  if (!hasApplicantNameField.value) {
    formData.anonymousName = ''
  }
  if (!hasContactField.value) {
    formData.anonymousContact = ''
  }
  form.value = {
    formData,
  }
  rules.value = buildRules()
}

function formatRange(startAt: any, endAt: any) {
  return `${formatDateTime(startAt) || '-'} 至 ${formatDateTime(endAt) || '-'}`
}

function findApplicantFieldKey() {
  const candidates = ['anonymousName', 'applicantName', 'name', 'realName']
  const field = dynamicFields.value.find((item) => candidates.includes(item.key))
  return field?.key || ''
}

function findContactFieldKey() {
  const candidates = ['anonymousContact', 'contact', 'phone', 'email', 'wechat']
  const field = dynamicFields.value.find((item) => candidates.includes(item.key))
  return field?.key || ''
}

function resolveAnonymousName() {
  const applicantFieldKey = findApplicantFieldKey()
  return (form.value.formData.anonymousName || form.value.formData[applicantFieldKey] || '').trim()
}

function resolveAnonymousContact() {
  const contactFieldKey = findContactFieldKey()
  return (form.value.formData.anonymousContact || form.value.formData[contactFieldKey] || '').trim()
}

onMounted(() => {
  loadFormDetail()
})
</script>

<style scoped>
.apply-grid {
  align-items: start;
}

.apply-copy {
  display: grid;
  gap: 18px;
}

.apply-note {
  padding: 20px 0;
  border-top: 1px solid var(--oa-border);
}

.apply-note:first-child {
  padding-top: 0;
  border-top: 0;
}

.apply-note h3 {
  margin: 0 0 12px;
}

.campaign-line {
  margin: 6px 0 0;
  color: var(--oa-muted);
}

.apply-note ol {
  margin: 0;
  padding-left: 20px;
  color: var(--oa-muted);
  line-height: 2;
}

.apply-form-card {
  border-radius: 24px !important;
}

.apply-form-card :deep(.el-card__body) {
  padding: clamp(22px, 3vw, 32px);
}

@media (max-width: 900px) {
  .apply-grid {
    grid-template-columns: 1fr;
  }
}
</style>
