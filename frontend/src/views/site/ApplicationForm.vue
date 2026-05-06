<template>
  <div class="site-page">
    <section class="container apply-grid">
      <div>
        <h1 class="page-title">入会申请表</h1>
        <p class="section-subtitle">请认真填写以下信息，提交后可以在“我的申请”中查看后续进度。</p>
        <div class="apply-note panel">
          <h3>申请说明</h3>
          <ol>
            <li>确认申请仍处于开放时间内</li>
            <li>按要求填写基础信息和自定义字段</li>
            <li>提交后请保持手机/邮箱畅通，等待面试通知</li>
          </ol>
        </div>
        <div class="apply-note panel" v-if="formMeta.id">
          <h3>当前表单</h3>
          <p class="campaign-line"><strong>{{ formMeta.name || '信息收集表单' }}</strong></p>
          <p class="campaign-line">提交方式：{{ requiresLogin ? '需要登录账号' : '支持匿名填写' }}</p>
          <p class="campaign-line">收集时间：{{ formatRange(formMeta.applyStartAt, formMeta.applyEndAt) }}</p>
        </div>
      </div>
      <el-card shadow="never">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
          <el-alert v-if="!formMeta.id" type="warning" show-icon :closable="false" title="表单不存在或暂未开放" />
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
          <template v-if="hasToken">
            <el-divider content-position="left">意向选择</el-divider>
            <el-form-item label="第一志愿" prop="firstChoiceDepartmentId">
              <el-select v-model="form.firstChoiceDepartmentId" filterable placeholder="请选择意向部门">
                <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="第二志愿" prop="secondChoiceDepartmentId">
              <el-select v-model="form.secondChoiceDepartmentId" filterable placeholder="请选择意向部门（可选）">
                <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
              </el-select>
            </el-form-item>
          </template>
          <el-form-item v-if="!requiresLogin && !hasApplicantNameField" label="联系人" prop="formData.applicantName">
            <el-input v-model="form.formData.applicantName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item v-if="!requiresLogin && !hasContactField" label="联系方式" prop="formData.contact">
            <el-input v-model="form.formData.contact" placeholder="请填写手机号、邮箱或微信" />
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
            <el-button type="primary" :disabled="!formMeta.id || !canSubmit" :loading="submitting" @click="submitForm">提交表单</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { applicationApi, clubApi, siteApi } from '../../api'
import { getToken } from '../../utils/auth'
import { formatDateTime } from '../../utils/format'

export default {
  name: 'SiteApplicationForm',
  data() {
    return {
      club: {},
      formMeta: {},
      departments: [],
      submitting: false,
      form: {
        firstChoiceDepartmentId: undefined,
        secondChoiceDepartmentId: undefined,
        formData: {}
      },
      rules: {}
    }
  },
  computed: {
    hasToken() {
      return Boolean(getToken())
    },
    requiresLogin() {
      return this.formMeta?.loginRequired !== false
    },
    canSubmit() {
      if (!this.formMeta?.id) return false
      if (['closed', 'archived'].includes(this.formMeta.status)) return false
      if (this.formMeta.applyStartAt && new Date(this.formMeta.applyStartAt).getTime() > Date.now()) return false
      if (!this.formMeta.applyEndAt) return true
      return new Date(this.formMeta.applyEndAt).getTime() >= Date.now()
    },
    dynamicFields() {
      return this.parseSchema(this.formMeta?.formSchema)
    },
    hasApplicantNameField() {
      return this.dynamicFields.some((field) => field.key === 'applicantName')
    },
    hasContactField() {
      return this.dynamicFields.some((field) => field.key === 'contact')
    }
  },
  created() {
    this.loadFormDetail()
  },
  methods: {
    formatDateTime,
    async loadFormDetail() {
      const result = await siteApi.recruitmentDetail(this.$route.params.id)
      this.club = result?.club || {}
      this.formMeta = {
        ...(result?.campaign || {}),
        loginRequired: result?.campaign?.loginRequired !== false
      }
      if (this.formMeta.id && this.requiresLogin && !this.hasToken) {
        ElMessage.warning('当前表单需要登录后填写，正在跳转到登录页')
        this.$router.replace({ path: '/admin/login', query: { redirect: this.$route.fullPath } })
        return
      }
      this.departments = result?.departments || []
      if (!this.departments.length && this.club.id) {
        const deptResult = await clubApi.departments(this.club.id)
        this.departments = deptResult?.list || deptResult || []
      }
      this.resetForm()
    },
    buildRules() {
      return {
        ...(this.requiresLogin
          ? {
              firstChoiceDepartmentId: [{ required: true, message: '请选择第一志愿', trigger: 'change' }]
            }
          : {
              ...(!this.hasApplicantNameField
                ? { 'formData.applicantName': [{ required: true, message: '请填写联系人', trigger: 'blur' }] }
                : {}),
              ...(!this.hasContactField
                ? { 'formData.contact': [{ required: true, message: '请填写联系方式', trigger: 'blur' }] }
                : {})
            }),
        ...this.dynamicFields.reduce((rules, field) => {
          if (field.required) {
            rules[`formData.${field.key}`] = [{ required: true, message: `请填写${field.label || field.key}`, trigger: 'blur' }]
          }
          return rules
        }, {})
      }
    },
    parseSchema(value) {
      if (Array.isArray(value)) return value.filter((item) => item && item.key)
      if (!value) return []
      try {
        const parsed = JSON.parse(value)
        return Array.isArray(parsed) ? parsed.filter((item) => item && item.key) : []
      } catch {
        return []
      }
    },
    submitForm() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        if (this.requiresLogin && !getToken()) {
          ElMessage.warning('请先登录后再提交表单')
          this.$router.push({ path: '/admin/login', query: { redirect: this.$route.fullPath } })
          return
        }
        this.submitting = true
        try {
          await applicationApi.create({
            campaignId: this.formMeta.id,
            clubId: this.club.id,
            firstChoiceDepartmentId: this.hasToken ? this.form.firstChoiceDepartmentId : undefined,
            secondChoiceDepartmentId: this.hasToken ? this.form.secondChoiceDepartmentId : undefined,
            profile: this.form.formData
          })
          if (this.hasToken) {
            ElMessage.success('入会申请已提交，请在“我的申请”中关注进度')
          } else {
            ElMessage.success('入会申请已提交')
          }
          this.resetForm()
          if (this.hasToken) {
            this.$router.push('/progress')
          }
        } finally {
          this.submitting = false
        }
      })
    },
    resetForm() {
      const formData = {}
      this.dynamicFields.forEach((field) => {
        formData[field.key] = ''
      })
      if (!this.hasApplicantNameField) {
        formData.applicantName = ''
      }
      if (!this.hasContactField) {
        formData.contact = ''
      }
      this.form = {
        firstChoiceDepartmentId: undefined,
        secondChoiceDepartmentId: undefined,
        formData
      }
      this.rules = this.buildRules()
    },
    formatRange(startAt, endAt) {
      return `${formatDateTime(startAt) || '-'} 至 ${formatDateTime(endAt) || '-'}`
    }
  }
}
</script>

<style scoped>
.site-page {
  padding: 34px 0;
}

.apply-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 24px;
}

.apply-note {
  margin-top: 24px;
  padding: 20px;
  border-radius: 22px;
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

.apply-grid :deep(.el-card) {
  border-radius: 24px;
}

.apply-grid :deep(.el-card__body) {
  padding: 26px;
}

@media (max-width: 900px) {
  .apply-grid {
    grid-template-columns: 1fr;
  }
}
</style>
