<template>
  <ViewPage class="scan-page">
    <section class="container scan-shell">
      <div class="scan-panel">
        <div class="scan-heading">
          <el-tag effect="plain" type="success">微信扫码签到</el-tag>
          <h1>{{ result ? '签到完成' : autoSubmitting ? '正在签到' : '现场签到' }}</h1>
          <p>{{ statusText }}</p>
        </div>

        <div class="status-card" :class="{ 'status-card--success': result }">
          <el-icon v-if="result"><Select /></el-icon>
          <el-icon v-else-if="autoSubmitting" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><Link /></el-icon>
          <strong>{{ result ? '已签到' : autoSubmitting ? '提交中' : '等待扫码' }}</strong>
          <span v-if="result">
            {{ result.realName || result.userName || '当前账号' }} 已完成本次签到
          </span>
          <span v-else>请使用管理员大屏上的二维码进入本页</span>
        </div>

        <dl v-if="result" class="result-detail">
          <div>
            <dt>状态</dt>
            <dd>{{ result.status === 'checked' ? '已签到' : result.status }}</dd>
          </div>
          <div>
            <dt>签到时间</dt>
            <dd>{{ formatDateTime(result.checkinAt) || '-' }}</dd>
          </div>
        </dl>

        <div v-if="routeToken && !result" class="retry-actions">
          <el-button
            :icon="Refresh"
            :loading="submitting"
            type="primary"
            @click="submitToken(routeToken)"
          >
            重新提交
          </el-button>
        </div>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { ElMessage } from 'element-plus'
import { Link, Loading, Refresh, Select } from '@element-plus/icons-vue'
import { checkInApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { clearSession, getToken } from '@/utils/auth.ts'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const PENDING_CHECK_IN_TOKEN = 'openatom_pending_checkin_token'

const routeToken = ref('')

const submitting = ref(false)

const autoSubmitting = ref(false)

const result = ref<any>(null)

const router = useRouter()

const route = useRoute()

const statusText = computed(() => {
  if (result.value) return '签到信息已提交，无需重复操作。'
  if (autoSubmitting.value) return '正在通过微信扫码带入的签到信息自动提交。'
  return '微信扫一扫管理员大屏二维码后会自动打开本页并完成签到。'
})

function redirectToLogin() {
  clearSession()
  router.replace({ path: '/admin/login', query: { redirect: route.fullPath } })
}

function loadRouteToken() {
  const rawToken = extractToken(route.query.t || route.query.token)
  const savedToken = localStorage.getItem(PENDING_CHECK_IN_TOKEN) || ''
  const token = rawToken || savedToken
  if (rawToken) {
    localStorage.setItem(PENDING_CHECK_IN_TOKEN, rawToken)
  }
  routeToken.value = token
  if (!token) return
  if (!getToken()) {
    redirectToLogin()
    return
  }
  autoSubmitting.value = true
  submitToken(token)
    .catch(() => undefined)
    .finally(() => {
      autoSubmitting.value = false
    })
}

function hideTokenFromAddressBar() {
  const url = new URL(window.location.href)
  url.searchParams.delete('token')
  url.searchParams.delete('t')
  window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`)
}

function extractToken(value: any) {
  if (Array.isArray(value)) return String(value[0] || '').trim()
  return String(value || '').trim()
}

async function submitToken(value: any) {
  const token = extractToken(value)
  if (!token || submitting.value) return
  submitting.value = true
  try {
    result.value = await checkInApi.scan({ token })
    localStorage.removeItem(PENDING_CHECK_IN_TOKEN)
    hideTokenFromAddressBar()
    ElMessage.success('签到成功')
  } catch (error: any) {
    if ([401, 40100].includes(Number(error?.code || error?.response?.status))) {
      localStorage.setItem(PENDING_CHECK_IN_TOKEN, token)
      redirectToLogin()
      return
    }
    throw error
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadRouteToken()
})

watch(() => [route.query.token, route.query.t], loadRouteToken)
</script>

<style scoped>
.scan-page {
  display: grid;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 0;
  background: var(--oa-page-soft-bg);
  overflow: hidden;
}

.scan-shell {
  display: grid;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  place-items: center;
  padding: 32px;
}

.scan-panel {
  width: min(100%, 620px);
  padding: 48px;
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  background: var(--oa-elevated-bg);
  box-shadow: none;
  backdrop-filter: none;
  animation: oaScaleIn 0.42s ease both;
  text-align: center;
}

.scan-heading h1 {
  margin: 14px 0 10px;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 56px;
  font-weight: 600;
  line-height: 1.07;
  letter-spacing: 0;
}

.scan-heading p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.status-card {
  display: grid;
  gap: 10px;
  justify-items: center;
  margin-top: 24px;
  padding: 42px 18px;
  color: var(--oa-muted);
  text-align: center;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-page-soft-bg);
}

.status-card .el-icon {
  font-size: 48px;
  color: var(--oa-primary);
}

.status-card strong {
  color: var(--oa-text);
  font-size: 22px;
}

.status-card span {
  line-height: 1.7;
}

.status-card--success {
  background: var(--oa-page-soft-bg);
}

.status-card--success .el-icon {
  color: var(--oa-text);
}

.result-detail {
  display: grid;
  gap: 12px;
  margin: 20px 0 0;
}

.result-detail div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--oa-border);
}

.result-detail dt {
  color: var(--oa-muted);
}

.result-detail dd {
  margin: 0;
  font-weight: 600;
}

.retry-actions {
  margin-top: 20px;
}

@media (max-width: 860px) {
  .scan-page {
    overflow-y: auto;
  }

  .scan-shell {
    align-items: stretch;
    padding: 16px;
  }

  .scan-panel {
    display: grid;
    align-content: center;
    min-height: calc(100dvh - 32px);
    padding: 22px;
  }

  .scan-heading h1 {
    font-size: 30px;
  }
}
</style>
