<template>
  <ViewPage class="site-system-page">
    <SitePageHero
      eyebrow="在线表单"
      title="表单填写"
      description="请按实际情况填写以下信息，提交成功后请留意页面提示。"
      compact
    />

    <section class="site-system-section">
      <div class="container apply-grid site-system-grid site-system-grid--split">
        <aside class="apply-copy site-system-surface site-reveal" aria-label="表单信息">
          <div class="apply-copy__eyebrow"><ClipboardCheck :size="18" /> ONLINE FORM</div>
          <h2>{{ formMeta.name || '信息收集表单' }}</h2>
          <p class="apply-copy__intro">
            请按实际情况完成填写，你提供的信息将用于本次申请与后续联系。
          </p><br>
          <div v-if="formMeta.id" class="apply-meta">
            <div>
              <CalendarDays :size="18" aria-hidden="true" /><span
                ><small>收集时间</small>{{ formatRange(formMeta.startAt, formMeta.endAt) }}</span
              >
            </div>
            <div>
              <ShieldCheck :size="18" aria-hidden="true" /><span
                ><small>提交方式</small
                >{{ requiresLogin ? '登录账号后提交' : '支持免登录填写' }}</span
              >
            </div>
          </div>
          <ol class="apply-steps" aria-label="填写步骤">
            <li>
              <span>1</span>
              <div><strong>填写信息</strong><small>完成所有标记为必填的项目</small></div>
            </li>
            <li>
              <span>2</span>
              <div><strong>检查内容</strong><small>确认联系方式等信息准确</small></div>
            </li>
            <li>
              <span>3</span>
              <div><strong>完成提交</strong><small>留意后续通知与申请进度</small></div>
            </li>
          </ol>
          <p class="privacy-note"><LockKeyhole :size="16" /> 你的信息会得到妥善保护</p>
        </aside>
        <el-card class="apply-form-card site-reveal" shadow="never">
          <div class="form-heading">
            <span>FORM DETAILS</span>
            <h2>填写表单内容</h2>
            <p><i>*</i> 为必填项，请确保提交内容准确有效。</p>
          </div>
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="application-form"
          >
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
            <div class="form-section form-section--summary">
              <div class="summary-item">
                <small>所属社团</small><strong>{{ club.name || '默认社团' }}</strong>
              </div>
              <div class="summary-item">
                <small>表单名称</small><strong>{{ formMeta.name || '-' }}</strong>
              </div>
            </div>
            <div
              v-if="!requiresLogin && (!hasApplicantNameField || !hasContactField)"
              class="form-section-heading"
            >
              <span>01</span>
              <div>
                <h3>联系信息</h3>
                <p>用于确认身份与后续沟通</p>
              </div>
            </div>
            <div
              v-if="!requiresLogin && (!hasApplicantNameField || !hasContactField)"
              class="form-section form-fields-grid"
            >
              <el-form-item
                v-if="!requiresLogin && !hasApplicantNameField"
                label="联系人"
                prop="formData.anonymousName"
              >
                <el-input
                  v-model="form.formData.anonymousName"
                  placeholder="请输入姓名"
                  autocomplete="name"
                />
              </el-form-item>
              <el-form-item
                v-if="!requiresLogin && !hasContactField"
                label="联系方式"
                prop="formData.anonymousContact"
              >
                <el-input
                  v-model="form.formData.anonymousContact"
                  placeholder="请填写手机号、邮箱或微信"
                  autocomplete="tel"
                />
              </el-form-item>
            </div>
            <div v-if="dynamicFields.length" class="form-section-heading">
              <span>{{
                !requiresLogin && (!hasApplicantNameField || !hasContactField) ? '02' : '01'
              }}</span>
              <div>
                <h3>详细信息</h3>
                <p>带 * 的项目需要完成填写</p>
              </div>
            </div>
            <div v-if="dynamicFields.length" class="form-section form-fields-grid">
              <template v-for="field in dynamicFields" :key="field.key">
                <el-form-item
                  :label="field.label || field.key"
                  :prop="`formData.${field.key}`"
                  :required="field.required"
                >
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
            </div>
            <div class="form-actions">
              <p><CheckCircle2 :size="16" /> 提交前请再次检查信息是否准确</p>
              <div>
                <el-button class="reset-button" @click="resetForm">重置</el-button>
                <el-button
                  class="submit-button"
                  type="primary"
                  :disabled="!formMeta.id || !canSubmit"
                  :loading="submitting"
                  @click="submitForm"
                >
                  提交表单 <ArrowRight :size="18" />
                </el-button>
              </div>
            </div>
          </el-form>
        </el-card>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import SitePageHero from '@/components/site/shell/SitePageHero.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import {
  ArrowRight,
  CalendarDays,
  CheckCircle2,
  ClipboardCheck,
  LockKeyhole,
  ShieldCheck,
} from 'lucide-vue-next'
import { formSubmissionApi, clubApi, siteApi } from '@/api'
import {
  COLLEGE_FIELD_KEY,
  ensureCollegeFormField,
  resolveCollegeValue,
} from '@/constants/colleges'
import { getCurrentUser, getToken } from '@/utils/auth.ts'
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
    router.replace({ path: '/login', query: { redirect: route.fullPath } })
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
            message: `${field.type === 'select' ? '请选择' : '请填写'}${field.label || field.key}`,
            trigger: field.type === 'select' ? 'change' : 'blur',
          },
        ]
      }
      return rules
    }, {}),
  }
}

function parseSchema(value: any) {
  if (Array.isArray(value)) return ensureCollegeFormField(value).filter((item) => item && item.key)
  if (!value) return ensureCollegeFormField([])
  try {
    const parsed = JSON.parse(value)
    return ensureCollegeFormField(parsed).filter((item) => item && item.key)
  } catch {
    return ensureCollegeFormField([])
  }
}

function submitForm() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (requiresLogin.value && !getToken()) {
      ElMessage.warning('请先登录后再提交表单')
      router.push({ path: '/login', query: { redirect: route.fullPath } })
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
  const currentUser = getCurrentUser()
  dynamicFields.value.forEach((field) => {
    formData[field.key] =
      field.key === COLLEGE_FIELD_KEY ? resolveCollegeValue(currentUser?.college) : ''
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

<style>
@import '@/styles/application-form.css';
</style>
