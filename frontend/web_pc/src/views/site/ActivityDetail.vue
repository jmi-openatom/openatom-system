<template>
  <div v-loading="loading" class="activity-detail">
    <section class="detail-hero">
      <div class="container">
        <el-button text @click="$router.back()">返回活动</el-button>
        <h1>{{ activity.title || '活动详情' }}</h1>
        <p>{{ activity.summary }}</p>
        <div class="detail-hero__meta">
          <span>{{ formatDateTime(activity.activityAt) }}</span>
          <span>{{ activity.location || '地点待定' }}</span>
          <el-tag :type="activity.registrationRequired ? 'success' : 'info'">
            {{ activity.registrationRequired ? '需要官网报名' : '无需报名' }}
          </el-tag>
        </div>
      </div>
    </section>

    <section class="container detail-grid">
      <article class="markdown-body" v-html="html"></article>
      <aside class="signup-panel">
        <h2>活动报名</h2>
        <p v-if="!activity.registrationRequired">该活动无需官网报名，按活动说明参与即可。</p>
        <template v-else>
          <el-alert
            v-if="signupBlockedMessage"
            type="warning"
            show-icon
            :closable="false"
            :title="signupBlockedMessage"
            class="signup-alert"
          />
          <el-form label-position="top">
            <el-form-item
              v-for="field in fields"
              :key="field.label"
              :label="field.label"
              :required="field.required"
            >
              <el-select
                v-if="field.type === 'select'"
                v-model="form[field.label]"
                placeholder="请选择"
              >
                <el-option
                  v-for="option in field.options || []"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
              <el-radio-group v-else-if="field.type === 'radio'" v-model="form[field.label]">
                <el-radio v-for="option in field.options || []" :key="option" :label="option" />
              </el-radio-group>
              <el-input
                v-else-if="field.type === 'textarea'"
                v-model="form[field.label]"
                :rows="3"
                type="textarea"
              />
              <el-input v-else v-model="form[field.label]" />
            </el-form-item>
          </el-form>
          <el-button :loading="submitting" type="primary" @click="submit">提交报名</el-button>
        </template>
      </aside>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { activityApi, siteApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { getToken } from '@/utils/auth.ts'
import { renderMarkdown } from '@/utils/markdown.ts'
import { hasRole } from '@/utils/permission.ts'

export default {
  name: 'SiteActivityDetail',
  data() {
    return {
      loading: false,
      submitting: false,
      activity: {},
      form: {},
    }
  },
  computed: {
    html() {
      return renderMarkdown(this.activity.descriptionMarkdown || this.activity.summary || '')
    },
    fields() {
      try {
        const parsed =
          typeof this.activity.registrationFields === 'string'
            ? JSON.parse(this.activity.registrationFields || '[]')
            : this.activity.registrationFields
        return Array.isArray(parsed) ? parsed : []
      } catch {
        return []
      }
    },
    signupBlockedMessage() {
      if (!this.activity.registrationRequired) return ''
      if (!getToken()) return '请先登录后再报名'
      if (!hasRole('formal_member')) return '无权限，请先加入社团'
      return ''
    },
  },
  created() {
    this.fetchDetail()
  },
  methods: {
    formatDateTime,
    async fetchDetail() {
      this.loading = true
      try {
        this.activity = await siteApi.activityDetail(this.$route.params.id)
      } finally {
        this.loading = false
      }
    },
    async submit() {
      if (!getToken()) {
        ElMessage.warning('请先登录后再报名')
        this.$router.push({ path: '/admin/login', query: { redirect: this.$route.fullPath } })
        return
      }
      if (!hasRole('formal_member')) {
        ElMessage.warning('无权限，请先加入社团')
        return
      }
      const missing = this.fields.find((field) => field.required && !this.form[field.label])
      if (missing) {
        ElMessage.warning(`请填写${missing.label}`)
        return
      }
      this.submitting = true
      try {
        await activityApi.register(this.activity.id, { formData: this.form })
        ElMessage.success('报名已提交')
      } finally {
        this.submitting = false
      }
    },
  },
}
</script>

<style scoped>
.detail-hero {
  padding: 80px 0;
  color: #ffffff;
  background: #272729;
}

.detail-hero .el-button {
  color: #ffffff;
}

.detail-hero h1 {
  max-width: 860px;
  margin: 20px 0 12px;
  font-family: 'SF Pro Display', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 56px;
  font-weight: 600;
  line-height: 1.07;
  letter-spacing: 0;
}

.detail-hero p {
  max-width: 760px;
  margin: 0;
  color: #cccccc;
  font-size: 24px;
  font-weight: 300;
  line-height: 1.5;
}

.detail-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
  align-items: center;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 28px;
  padding: 48px 0 80px;
}

.markdown-body,
.signup-panel {
  background: #ffffff;
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  padding: 26px;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 0;
  color: #1d1d1f;
}

.markdown-body :deep(p),
.markdown-body :deep(li) {
  color: #7a7a7a;
  line-height: 1.9;
}

.markdown-body :deep(code) {
  padding: 2px 6px;
  background: #fafafc;
  border-radius: 4px;
}

.signup-panel {
  align-self: start;
  position: sticky;
  top: 72px;
}

.signup-panel h2 {
  margin: 0 0 12px;
}

.signup-panel p {
  color: var(--oa-muted);
  line-height: 1.7;
}

.signup-alert {
  margin-bottom: 16px;
}

.signup-panel .el-button {
  width: 100%;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .signup-panel {
    position: static;
  }
}
</style>
