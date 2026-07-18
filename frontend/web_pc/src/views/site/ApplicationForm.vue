<template>
  <ViewPage class="site-system-page">
    <SitePageHero
      eyebrow="入会申请"
      title="入会申请表"
      description="请认真填写以下信息，提交后可以在“我的申请”中查看后续进度。"
      compact
    />

    <section class="site-system-section">
      <div class="container apply-grid site-system-grid site-system-grid--split">
        <aside class="apply-copy site-system-surface site-reveal" aria-label="申请信息">
          <div class="apply-copy__eyebrow"><ClipboardCheck :size="18" /> APPLICATION</div>
          <h2>{{ formMeta.name || '入会申请' }}</h2>
          <p class="apply-copy__intro">花几分钟介绍自己，我们期待了解你的方向与想法。</p>

          <div v-if="formMeta.id" class="apply-meta">
            <div>
              <CalendarDays :size="18" aria-hidden="true" />
              <span
                ><small>开放时间</small
                >{{ formatRange(formMeta.applyStartAt, formMeta.applyEndAt) }}</span
              >
            </div>
            <div>
              <ShieldCheck :size="18" aria-hidden="true" />
              <span
                ><small>提交方式</small
                >{{ requiresLogin ? '登录账号后提交' : '支持免登录填写' }}</span
              >
            </div>
          </div>

          <ol class="apply-steps" aria-label="申请步骤">
            <li>
              <span>1</span>
              <div><strong>填写资料</strong><small>完成基本信息与意向选择</small></div>
            </li>
            <li>
              <span>2</span>
              <div><strong>提交申请</strong><small>确认内容准确后提交</small></div>
            </li>
            <li>
              <span>3</span>
              <div><strong>等待联系</strong><small>请保持手机或邮箱畅通</small></div>
            </li>
          </ol>

          <p class="privacy-note"><LockKeyhole :size="16" /> 信息仅用于本次招新审核</p>
        </aside>
        <el-card class="apply-form-card site-reveal" shadow="never">
          <div class="form-heading">
            <span>APPLICATION FORM</span>
            <h2>填写申请资料</h2>
            <p><i>*</i> 为必填项，请确保联系方式准确有效。</p>
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
              title="当前表单支持登录用户自动关联身份，也支持匿名填写。"
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
                <small>申请计划</small><strong>{{ formMeta.name || '-' }}</strong>
              </div>
            </div>
            <template v-if="departments.length">
              <div class="form-section-heading">
                <span>01</span>
                <div>
                  <h3>意向选择</h3>
                  <p>告诉我们你更希望加入的方向</p>
                </div>
              </div>
              <div class="form-section form-fields-grid">
                <el-form-item label="第一志愿" prop="firstChoiceDepartmentId" required>
                  <el-select
                    v-model="form.firstChoiceDepartmentId"
                    filterable
                    placeholder="请选择意向部门"
                  >
                    <el-option
                      v-for="dept in departments"
                      :key="dept.id"
                      :label="dept.name"
                      :value="dept.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="第二志愿" prop="secondChoiceDepartmentId">
                  <el-select
                    v-model="form.secondChoiceDepartmentId"
                    filterable
                    placeholder="请选择意向部门（可选）"
                  >
                    <el-option
                      v-for="dept in departments"
                      :key="dept.id"
                      :label="dept.name"
                      :value="dept.id"
                    />
                  </el-select>
                </el-form-item>
              </div>
            </template>
            <div v-if="!hasApplicantNameField || !hasContactField" class="form-section-heading">
              <span>{{ departments.length ? '02' : '01' }}</span>
              <div>
                <h3>个人信息</h3>
                <p>用于审核申请与后续联系</p>
              </div>
            </div>
            <div
              v-if="!hasApplicantNameField || !hasContactField"
              class="form-section form-fields-grid"
            >
              <el-form-item
                v-if="!requiresLogin && !hasApplicantNameField"
                label="联系人"
                prop="formData.applicantName"
              >
                <el-input
                  v-model="form.formData.applicantName"
                  placeholder="请输入姓名"
                  autocomplete="name"
                />
              </el-form-item>
              <el-form-item
                v-if="!requiresLogin && !hasContactField"
                label="联系方式"
                prop="formData.contact"
              >
                <el-input
                  v-model="form.formData.contact"
                  placeholder="请填写手机号、邮箱或微信"
                  autocomplete="tel"
                />
              </el-form-item>
            </div>
            <div v-if="dynamicFields.length" class="form-section-heading">
              <span>{{
                departments.length
                  ? !hasApplicantNameField || !hasContactField
                    ? '03'
                    : '02'
                  : !hasApplicantNameField || !hasContactField
                    ? '02'
                    : '01'
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
                  提交表单
                  <ArrowRight :size="18" />
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
import { applicationApi, siteApi } from '@/api'
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
  firstChoiceDepartmentId: undefined,
  secondChoiceDepartmentId: undefined,
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
  return false
})

const canSubmit = computed(() => {
  if (!formMeta.value?.id) return false
  if (['closed', 'archived'].includes(formMeta.value.status)) return false
  if (formMeta.value.applyStartAt && new Date(formMeta.value.applyStartAt).getTime() > Date.now())
    return false
  if (!formMeta.value.applyEndAt) return true
  return new Date(formMeta.value.applyEndAt).getTime() >= Date.now()
})

const dynamicFields = computed(() => {
  return parseSchema(formMeta.value?.formSchema)
})

const hasApplicantNameField = computed(() => {
  return dynamicFields.value.some((field) => field.key === 'applicantName')
})

const hasContactField = computed(() => {
  return dynamicFields.value.some((field) => field.key === 'contact')
})

async function loadFormDetail() {
  const result = await siteApi.recruitmentDetail(route.params.id)
  club.value = result?.club || {}
  formMeta.value = {
    ...(result?.campaign || {}),
    loginRequired: result?.campaign?.loginRequired !== false,
  }
  departments.value = result?.departments || []
  resetForm()
}

function buildRules() {
  return {
    ...(departments.value.length
      ? {
          firstChoiceDepartmentId: [
            { required: true, message: '请选择第一志愿', trigger: 'change' },
          ],
        }
      : {}),
    ...(!hasApplicantNameField.value
      ? {
          'formData.applicantName': [{ required: true, message: '请填写联系人', trigger: 'blur' }],
        }
      : {}),
    ...(!hasContactField.value
      ? {
          'formData.contact': [{ required: true, message: '请填写联系方式', trigger: 'blur' }],
        }
      : {}),
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
    submitting.value = true
    try {
      await applicationApi.create({
        campaignId: formMeta.value.id,
        clubId: club.value.id,
        firstChoiceDepartmentId: form.value.firstChoiceDepartmentId,
        secondChoiceDepartmentId: form.value.secondChoiceDepartmentId,
        profile: form.value.formData,
      })
      if (hasToken.value) {
        ElMessage.success('入会申请已提交，请在“我的申请”中关注进度')
      } else {
        ElMessage.success('入会申请已提交')
      }
      resetForm()
      if (hasToken.value) {
        router.push('/progress')
      }
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
    formData.applicantName = ''
  }
  if (!hasContactField.value) {
    formData.contact = ''
  }
  form.value = {
    firstChoiceDepartmentId: undefined,
    secondChoiceDepartmentId: undefined,
    formData,
  }
  rules.value = buildRules()
}

function formatRange(startAt: any, endAt: any) {
  return `${formatDateTime(startAt) || '-'} 至 ${formatDateTime(endAt) || '-'}`
}

onMounted(() => {
  loadFormDetail()
})
</script>

<style>
@import '@/styles/application-form.css';
</style>
