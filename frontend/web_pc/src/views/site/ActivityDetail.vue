<template>
  <ViewPage class="activity-detail" :loading="loading">
    <section class="detail-hero">
      <div class="container detail-hero__inner">
        <div class="detail-hero__copy">
          <el-button text @click="$router.back()">返回活动</el-button>
          <span>活动详情</span>
          <h1>{{ activity.title || '活动详情' }}</h1>
          <p>{{ activity.summary || '暂无活动摘要' }}</p>

          <div class="detail-hero__meta">
            <el-tag :type="activity.registrationRequired ? 'success' : 'info'" effect="plain">
              {{ activity.registrationRequired ? '需要官网报名' : '无需报名' }}
            </el-tag>
            <span>{{ formatDateTime(activity.activityAt) }}</span>
            <span>{{ activity.location || '地点待定' }}</span>
          </div>
        </div>

        <div class="detail-hero__visual" :class="{ 'is-fallback': !activity.coverUrl }">
          <img
            v-if="activity.coverUrl"
            :alt="activity.title || '活动封面'"
            :src="activity.coverUrl"
          />
          <div v-else class="detail-hero__placeholder">
            <strong>{{ day(activity.activityAt) }}</strong>
            <span>{{ month(activity.activityAt) }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="detail-body-section">
      <div class="container detail-facts" aria-label="活动信息">
        <article class="detail-fact">
          <span>活动时间</span>
          <strong>{{ formatDateTime(activity.activityAt) }}</strong>
        </article>
        <article class="detail-fact">
          <span>活动地点</span>
          <strong>{{ activity.location || '地点待定' }}</strong>
        </article>
        <article class="detail-fact">
          <span>报名方式</span>
          <strong>{{ activity.registrationRequired ? '官网报名' : '现场参与' }}</strong>
        </article>
      </div>

      <div class="container detail-grid">
        <article class="markdown-body detail-story-card">
          <div v-if="html" v-html="html"></div>
          <div v-else class="detail-story-empty">
            <span>活动介绍</span>
            <p>活动说明稍后补充，当前可先查看时间、地点和报名方式。</p>
          </div>
        </article>

        <aside class="signup-panel">
          <div class="signup-panel__head">
            <span>参与方式</span>
            <h2>活动报名</h2>
          </div>

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
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { ElMessage } from 'element-plus'
import { activityApi, siteApi } from '@/api'
import { formatDateTime, monthDayParts } from '@/utils/format.ts'
import { getToken } from '@/utils/auth.ts'
import { renderMarkdown } from '@/utils/markdown.ts'
import { hasRole } from '@/utils/permission.ts'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const loading = ref(false)
const submitting = ref(false)
const activity = ref<Record<string, any>>({})
const form = ref<Record<string, any>>({})

const router = useRouter()
const route = useRoute()

const html = computed(() => {
  return renderMarkdown(activity.value.descriptionMarkdown || activity.value.summary || '')
})

const fields = computed(() => {
  try {
    const parsed =
      typeof activity.value.registrationFields === 'string'
        ? JSON.parse(activity.value.registrationFields || '[]')
        : activity.value.registrationFields
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

const signupBlockedMessage = computed(() => {
  if (!activity.value.registrationRequired) return ''
  if (!getToken()) return '请先登录后再报名'
  if (!hasRole('formal_member')) return '无权限，请先加入社团'
  return ''
})

async function fetchDetail() {
  loading.value = true
  try {
    activity.value = await siteApi.activityDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!getToken()) {
    ElMessage.warning('请先登录后再报名')
    router.push({ path: '/admin/login', query: { redirect: route.fullPath } })
    return
  }
  if (!hasRole('formal_member')) {
    ElMessage.warning('无权限，请先加入社团')
    return
  }
  const missing = fields.value.find((field) => field.required && !form.value[field.label])
  if (missing) {
    ElMessage.warning(`请填写${missing.label}`)
    return
  }
  submitting.value = true
  try {
    await activityApi.register(activity.value.id, { formData: form.value })
    ElMessage.success('报名已提交')
  } finally {
    submitting.value = false
  }
}

function day(value: any) {
  return monthDayParts(value)?.day || '--'
}

function month(value: any) {
  const parts = monthDayParts(value)
  return parts ? `${Number(parts.month)}月` : '待定'
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.activity-detail {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.detail-hero {
  background: linear-gradient(180deg, var(--oa-elevated-bg) 0%, var(--oa-page-soft-bg) 100%);
}

.detail-hero__inner {
  display: grid;
  min-height: min(72vh, 760px);
  grid-template-columns: minmax(0, 0.9fr) minmax(420px, 1.1fr);
  gap: clamp(28px, 5vw, 72px);
  align-items: center;
  padding: clamp(72px, 9vw, 120px) 0;
}

.detail-hero__copy {
  max-width: 620px;
}

.detail-hero__copy .el-button {
  margin-bottom: 18px;
  padding-left: 0;
}

.detail-hero__copy > span,
.signup-panel__head span {
  display: inline-block;
  color: var(--oa-text);
  font-size: 14px;
}

.detail-hero h1 {
  margin: 12px 0 16px;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: clamp(38px, 4.8vw, 62px);
  font-weight: 600;
  line-height: 1.05;
}

.detail-hero p {
  margin: 0;
  color: var(--oa-muted);
  font-size: clamp(18px, 2vw, 24px);
  font-weight: 300;
  line-height: 1.6;
}

.detail-hero__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 26px;
  color: var(--oa-muted);
  font-size: 14px;
}

.detail-hero__visual {
  min-height: clamp(320px, 34vw, 500px);
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 32px;
  background: var(--oa-elevated-bg);
}

.detail-hero__visual img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: grayscale(0.08);
}

.detail-hero__visual.is-fallback {
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 28% 24%, rgba(15, 23, 42, 0.14), transparent 34%),
    linear-gradient(180deg, var(--oa-page-soft-bg) 0%, var(--oa-elevated-bg) 100%);
}

.detail-hero__placeholder {
  display: grid;
  gap: 4px;
  justify-items: center;
}

.detail-hero__placeholder strong {
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: clamp(58px, 7vw, 88px);
  font-weight: 600;
  line-height: 0.95;
}

.detail-hero__placeholder span {
  color: var(--oa-muted);
}

.detail-body-section {
  padding-bottom: clamp(72px, 8vw, 96px);
}

.detail-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  margin-bottom: 28px;
  border-top: 1px solid var(--oa-border);
}

.detail-fact {
  display: grid;
  gap: 10px;
  min-height: 116px;
  align-content: center;
  padding: 20px 24px 0;
  border-left: 1px solid var(--oa-border);
}

.detail-fact:first-child {
  border-left: 0;
}

.detail-fact span {
  color: var(--oa-muted);
  font-size: 14px;
}

.detail-fact strong {
  color: var(--oa-text);
  font-size: clamp(18px, 2vw, 22px);
  font-weight: 500;
  line-height: 1.45;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 24px;
}

.detail-story-card,
.signup-panel {
  border: 1px solid var(--oa-border);
  border-radius: 24px;
  background: var(--oa-elevated-bg);
}

.detail-story-card {
  min-height: 420px;
  padding: clamp(24px, 3vw, 34px);
}

.detail-story-empty {
  display: grid;
  min-height: 352px;
  align-content: center;
  justify-items: center;
  gap: 10px;
  color: var(--oa-muted);
  text-align: center;
}

.detail-story-empty span {
  color: var(--oa-text);
  font-size: 14px;
}

.detail-story-empty p {
  max-width: 320px;
  margin: 0;
  line-height: 1.8;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  color: var(--oa-text);
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
}

.markdown-body :deep(h1:first-child),
.markdown-body :deep(h2:first-child),
.markdown-body :deep(h3:first-child) {
  margin-top: 0;
}

.markdown-body :deep(p),
.markdown-body :deep(li) {
  color: var(--oa-muted);
  line-height: 1.95;
}

.markdown-body :deep(code) {
  padding: 2px 6px;
  background: var(--oa-button-subtle-bg);
  border-radius: 4px;
}

.signup-panel {
  position: sticky;
  top: calc(var(--oa-site-header-height) + 18px);
  align-self: start;
  padding: 26px;
}

.signup-panel__head {
  margin-bottom: 18px;
}

.signup-panel h2 {
  margin: 10px 0 0;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 28px;
  font-weight: 600;
}

.signup-panel p {
  color: var(--oa-muted);
  line-height: 1.8;
}

.signup-alert {
  margin-bottom: 16px;
}

.signup-panel .el-button {
  width: 100%;
}

@media (max-width: 980px) {
  .detail-hero__inner,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-hero__copy {
    max-width: none;
  }

  .signup-panel {
    position: static;
  }
}

@media (max-width: 720px) {
  .detail-facts {
    grid-template-columns: 1fr;
    border-top: 0;
  }

  .detail-fact {
    min-height: 0;
    padding: 18px 0;
    border-top: 1px solid var(--oa-border);
    border-left: 0;
  }

  .detail-hero__visual,
  .detail-story-card,
  .signup-panel {
    border-radius: 24px;
  }
}
</style>
