<template>
  <ViewPage class="votes-page" :loading="loading">
    <template v-if="!isDetailRoute">
      <section class="votes-hero">
        <div class="container votes-hero__inner">
          <div class="votes-hero__copy">
            <span>社团投票</span>
            <h1>投票中心</h1>
            <p>活动主题、项目方案、社团事项都会在这里发起公开投票。</p>
          </div>
          <div v-if="rows.length" class="votes-hero__metrics">
            <article>
              <strong>{{ openCount }}</strong>
              <span>进行中</span>
            </article>
            <article>
              <strong>{{ rows.length }}</strong>
              <span>投票活动</span>
            </article>
            <article>
              <strong>{{ openVoters }}</strong>
              <span>进行中参与</span>
            </article>
          </div>
        </div>
      </section>

      <section class="votes-list-section">
        <div class="container votes-list">
          <div class="section-heading">
            <span>Vote</span>
            <h2>全部投票</h2>
          </div>
          <div v-if="rows.length" class="vote-grid">
            <article
              v-for="item in rows"
              :key="item.id"
              class="vote-card"
              @click="$router.push(`/votes/${item.id}`)"
            >
              <header>
                <el-tag :type="statusType(item.status)" effect="plain">{{
                  statusText(item.status)
                }}</el-tag>
                <span>{{
                  item.voteType === 'multiple' ? `多选 · ${item.maxChoices}` : '单选'
                }}</span>
              </header>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description || '暂无投票说明' }}</p>
              <footer>
                <span v-if="item.status !== 'closed'">{{ item.voterCount || 0 }} 人参与</span>
                <span v-else>参与人数已隐藏</span>
                <span>{{ formatRange(item.startAt, item.endAt) }}</span>
              </footer>
            </article>
          </div>
          <el-empty v-else-if="!loading" description="暂无投票活动" />
        </div>
      </section>
    </template>

    <template v-else>
      <section class="vote-detail-hero">
        <div class="container vote-detail-hero__inner">
          <el-button :icon="ArrowLeft" plain @click="$router.push('/votes')">返回</el-button>
          <div v-if="detail?.vote" class="vote-detail-hero__copy">
            <span>{{ detail.vote.clubName || '开放原子开源社团' }}</span>
            <h1>{{ detail.vote.title }}</h1>
            <p>{{ detail.vote.description || '暂无投票说明' }}</p>
          </div>
        </div>
      </section>

      <section class="vote-detail-section">
        <div v-if="detail?.vote" class="container vote-detail-layout">
          <div class="vote-panel">
            <div class="vote-panel__header">
              <div>
                <el-tag :type="statusType(detail.vote.status)" effect="plain">{{
                  statusText(detail.vote.status)
                }}</el-tag>
                <span>{{ voteTypeText(detail.vote) }}</span>
              </div>
              <strong v-if="detail.vote.status !== 'closed'">
                {{ detail.vote.voterCount || 0 }} 人参与
              </strong>
              <strong v-else>参与人数已隐藏</strong>
            </div>

            <el-radio-group
              v-if="detail.vote.voteType !== 'multiple'"
              v-model="singleOptionId"
              class="vote-options"
            >
              <label
                v-for="option in detail.options"
                :key="option.id"
                class="vote-option"
                :class="{ 'is-selected': singleOptionId === option.id }"
              >
                <el-radio :label="option.id">
                  <span>{{ option.title }}</span>
                </el-radio>
                <small v-if="option.description">{{ option.description }}</small>
                <div v-if="resultsVisible" class="vote-result-bar">
                  <el-progress :percentage="option.votePercent || 0" :show-text="false" />
                  <span>{{ option.voteCount || 0 }} 票 · {{ option.votePercent || 0 }}%</span>
                </div>
              </label>
            </el-radio-group>

            <el-checkbox-group v-else v-model="multipleOptionIds" class="vote-options">
              <label
                v-for="option in detail.options"
                :key="option.id"
                class="vote-option"
                :class="{ 'is-selected': multipleOptionIds.includes(option.id) }"
              >
                <el-checkbox :label="option.id">
                  <span>{{ option.title }}</span>
                </el-checkbox>
                <small v-if="option.description">{{ option.description }}</small>
                <div v-if="resultsVisible" class="vote-result-bar">
                  <el-progress :percentage="option.votePercent || 0" :show-text="false" />
                  <span>{{ option.voteCount || 0 }} 票 · {{ option.votePercent || 0 }}%</span>
                </div>
              </label>
            </el-checkbox-group>

            <div v-if="requiresAnonymousInfo" class="anonymous-fields">
              <el-input v-model="form.voterName" placeholder="姓名" />
              <el-input v-model="form.voterContact" placeholder="手机号、邮箱或微信" />
            </div>

            <el-input
              v-model="form.remark"
              class="vote-remark"
              type="textarea"
              :rows="3"
              placeholder="备注，可不填"
            />

            <el-alert
              v-if="requiresLogin"
              type="warning"
              show-icon
              :closable="false"
              title="该投票需要登录后参与"
            />
            <el-alert
              v-else-if="hasVoted"
              type="success"
              show-icon
              :closable="false"
              title="你已参与该投票"
            />
            <el-alert
              v-else-if="!canSubmitByTime"
              type="info"
              show-icon
              :closable="false"
              title="当前不在投票时间内"
            />

            <div class="vote-actions">
              <el-button v-if="requiresLogin" type="primary" @click="goLogin">去登录</el-button>
              <el-button
                v-else
                type="primary"
                :loading="submitting"
                :disabled="!canSubmit"
                @click="submitVote"
              >
                提交投票
              </el-button>
              <el-button :icon="Refresh" @click="fetchDetail">刷新</el-button>
            </div>
          </div>

          <aside class="vote-meta">
            <div>
              <span>投票时间</span>
              <strong>{{ formatRange(detail.vote.startAt, detail.vote.endAt) }}</strong>
            </div>
            <div>
              <span>投票方式</span>
              <strong>{{ voteTypeText(detail.vote) }}</strong>
            </div>
            <div>
              <span>参与限制</span>
              <strong>{{
                detail.vote.anonymousAllowed === false ? '登录参与' : '支持匿名'
              }}</strong>
            </div>
            <div>
              <span>结果可见</span>
              <strong>{{
                detail.vote.resultVisible === false ? '投后或结束可见' : '公开可见'
              }}</strong>
            </div>
          </aside>
        </div>
        <el-empty v-else-if="!loading" description="投票不存在或暂未开放" />
      </section>
    </template>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { siteApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'
import { getToken } from '@/utils/auth.ts'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const loading = ref(false)
const submitting = ref(false)
const rows = ref<any[]>([])
const detail = ref<any>(null)
const singleOptionId = ref<number | null>(null)
const multipleOptionIds = ref<number[]>([])
const submittedLocally = ref(false)
const form = reactive({
  voterName: '',
  voterContact: '',
  remark: '',
})

const route = useRoute()
const router = useRouter()

const isDetailRoute = computed(() => Boolean(route.params.id))
const hasToken = computed(() => Boolean(getToken()))
const openCount = computed(() => rows.value.filter((item) => item.status === 'open').length)
const openVoters = computed(() =>
  rows.value
    .filter((item) => item.status === 'open')
    .reduce((total, item) => total + Number(item.voterCount || 0), 0),
)
const resultsVisible = computed(() =>
  Boolean(
    detail.value?.options?.some(
      (option) => option.voteCount !== null && option.voteCount !== undefined,
    ),
  ),
)
const requiresLogin = computed(
  () => detail.value?.vote?.anonymousAllowed === false && !hasToken.value,
)
const requiresAnonymousInfo = computed(
  () => detail.value?.vote?.anonymousAllowed !== false && !hasToken.value && !hasVoted.value,
)
const hasVoted = computed(() => Boolean(detail.value?.voted || submittedLocally.value))
const canSubmitByTime = computed(() => {
  const vote = detail.value?.vote
  if (!vote || vote.status !== 'open') return false
  const now = Date.now()
  if (vote.startAt && new Date(vote.startAt).getTime() > now) return false
  if (vote.endAt && new Date(vote.endAt).getTime() < now) return false
  return true
})
const selectedOptionIds = computed(() => {
  if (detail.value?.vote?.voteType === 'multiple') return multipleOptionIds.value
  return singleOptionId.value ? [singleOptionId.value] : []
})
const canSubmit = computed(() => {
  if (!canSubmitByTime.value || hasVoted.value || requiresLogin.value) return false
  if (!selectedOptionIds.value.length) return false
  if (detail.value?.vote?.voteType === 'multiple') {
    return selectedOptionIds.value.length <= Number(detail.value.vote.maxChoices || 2)
  }
  return selectedOptionIds.value.length === 1
})

async function fetchList() {
  loading.value = true
  try {
    rows.value = await siteApi.votes()
  } finally {
    loading.value = false
  }
}

async function fetchDetail() {
  if (!route.params.id) return
  loading.value = true
  try {
    detail.value = await siteApi.voteDetail(route.params.id as string)
    singleOptionId.value = null
    multipleOptionIds.value = []
  } finally {
    loading.value = false
  }
}

async function submitVote() {
  if (!canSubmit.value) return
  if (requiresAnonymousInfo.value && (!form.voterName.trim() || !form.voterContact.trim())) {
    ElMessage.warning('请填写姓名和联系方式')
    return
  }
  submitting.value = true
  try {
    await siteApi.submitVote(route.params.id as string, {
      optionIds: selectedOptionIds.value,
      voterName: form.voterName,
      voterContact: form.voterContact,
      remark: form.remark,
    })
    submittedLocally.value = true
    ElMessage.success('投票成功')
    await fetchDetail()
  } finally {
    submitting.value = false
  }
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

function statusText(status: string) {
  return (
    {
      draft: '草稿',
      open: '进行中',
      closed: '已结束',
    }[String(status || '').toLowerCase()] ||
    status ||
    '-'
  )
}

function voteTypeText(vote: Record<string, any>) {
  return vote.voteType === 'multiple' ? `多选，最多 ${vote.maxChoices || 2} 项` : '单选'
}

function formatRange(startAt: any, endAt: any) {
  return `${formatDateTime(startAt)} 至 ${formatDateTime(endAt)}`
}

function loadByRoute() {
  submittedLocally.value = false
  if (isDetailRoute.value) fetchDetail()
  else fetchList()
}

watch(
  () => route.fullPath,
  () => loadByRoute(),
)

onMounted(() => {
  loadByRoute()
})
</script>

<style scoped>
.votes-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-elevated-bg);
}

.votes-hero {
  min-height: 54vh;
  display: flex;
  align-items: center;
  background: linear-gradient(180deg, var(--oa-elevated-bg), var(--oa-page-soft-bg));
}

.votes-hero__inner {
  display: grid;
  justify-items: center;
  gap: 36px;
  padding: 88px 0 72px;
  text-align: center;
}

.votes-hero__copy {
  max-width: 780px;
}

.votes-hero__copy span,
.section-heading span {
  color: var(--oa-muted);
  font-size: 14px;
}

.votes-hero h1,
.vote-detail-hero h1 {
  margin: 12px 0 16px;
  font-size: clamp(40px, 5vw, 62px);
  line-height: 1.05;
  letter-spacing: 0;
}

.votes-hero p,
.vote-detail-hero p {
  margin: 0;
  color: var(--oa-muted);
  font-size: 20px;
  line-height: 1.6;
}

.votes-hero__metrics {
  display: grid;
  width: min(720px, 100%);
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid var(--oa-border);
}

.votes-hero__metrics article {
  padding: 22px;
  border-right: 1px solid var(--oa-border);
}

.votes-hero__metrics article:last-child {
  border-right: 0;
}

.votes-hero__metrics strong {
  display: block;
  font-size: 32px;
}

.votes-hero__metrics span,
.vote-card footer,
.vote-card header span {
  color: var(--oa-muted);
  font-size: 13px;
}

.votes-list-section,
.vote-detail-section {
  padding: 64px 0 90px;
  background: var(--oa-page-soft-bg);
}

.section-heading {
  margin-bottom: 24px;
}

.section-heading h2 {
  margin: 8px 0 0;
  font-size: 32px;
}

.vote-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.vote-card {
  display: grid;
  min-height: 260px;
  align-content: space-between;
  gap: 20px;
  padding: 22px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease;
}

.vote-card:hover {
  transform: translateY(-3px);
  border-color: var(--oa-text);
}

.vote-card header,
.vote-card footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.vote-card h3 {
  margin: 0;
  font-size: 24px;
}

.vote-card p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.vote-detail-hero {
  background: var(--oa-elevated-bg);
}

.vote-detail-hero__inner {
  display: grid;
  gap: 32px;
  padding: 44px 0 36px;
}

.vote-detail-hero__copy {
  max-width: 980px;
}

.vote-detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 22px;
  align-items: start;
}

.vote-panel,
.vote-meta {
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
}

.vote-panel {
  padding: 22px;
}

.vote-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.vote-panel__header > div {
  display: flex;
  gap: 10px;
  align-items: center;
}

.vote-options {
  display: grid;
  gap: 12px;
  width: 100%;
}

.vote-option {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-panel);
}

.vote-option.is-selected {
  border-color: var(--oa-text);
}

.vote-option small {
  color: var(--oa-muted);
  line-height: 1.6;
}

.vote-result-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  color: var(--oa-muted);
  font-size: 13px;
}

.anonymous-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.vote-remark,
.vote-actions,
.vote-panel :deep(.el-alert) {
  margin-top: 14px;
}

.vote-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.vote-meta {
  display: grid;
  gap: 0;
  overflow: hidden;
}

.vote-meta > div {
  padding: 18px;
  border-bottom: 1px solid var(--oa-border);
}

.vote-meta > div:last-child {
  border-bottom: 0;
}

.vote-meta span {
  display: block;
  margin-bottom: 8px;
  color: var(--oa-muted);
  font-size: 13px;
}

.vote-meta strong {
  line-height: 1.5;
}

@media (max-width: 980px) {
  .vote-grid,
  .vote-detail-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .votes-hero__metrics,
  .anonymous-fields {
    grid-template-columns: 1fr;
  }

  .votes-hero__metrics article {
    border-right: 0;
    border-bottom: 1px solid var(--oa-border);
  }

  .votes-hero__metrics article:last-child {
    border-bottom: 0;
  }

  .vote-card header,
  .vote-card footer,
  .vote-panel__header,
  .vote-result-bar {
    display: grid;
  }
}
</style>
