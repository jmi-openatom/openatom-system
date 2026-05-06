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
        ElMessage.warning('请先使用已注册的社团账号登录后再报名')
        this.$router.push({ path: '/admin/login', query: { redirect: this.$route.fullPath } })
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
  padding: 44px 0 50px;
  color: #fff;
  background: linear-gradient(135deg, #0f172a, #0f766e);
}

.detail-hero .el-button {
  color: #d1fae5;
}

.detail-hero h1 {
  max-width: 860px;
  margin: 20px 0 12px;
  font-size: 44px;
}

.detail-hero p {
  max-width: 760px;
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.8;
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
  padding: 34px 0 58px;
}

.markdown-body,
.signup-panel {
  background: #fff;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  padding: 26px;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 0;
  color: #0f172a;
}

.markdown-body :deep(p),
.markdown-body :deep(li) {
  color: #475569;
  line-height: 1.9;
}

.markdown-body :deep(code) {
  padding: 2px 6px;
  background: #f1f5f9;
  border-radius: 4px;
}

.signup-panel {
  align-self: start;
  position: sticky;
  top: 88px;
}

.signup-panel h2 {
  margin: 0 0 12px;
}

.signup-panel p {
  color: var(--oa-muted);
  line-height: 1.7;
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
