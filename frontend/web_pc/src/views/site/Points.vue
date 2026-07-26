<template>
  <ViewPage :loading="loading.page" class="points-page">
    <section class="points-hero">
      <div class="container points-hero__inner">
        <div class="points-hero__copy">
          <span class="points-eyebrow"><i aria-hidden="true" /> 社区积分计划</span>
          <h1>每一次参与，<br />都在积累价值。</h1>
          <p>
            签到、活动与内容贡献都会转化为积分。查看成长轨迹、社区排名，并把每一份投入兑换成真实回馈。
          </p>
          <div class="points-hero__actions">
            <a class="points-action points-action--primary" href="#points-overview">
              <span>{{ isLoggedIn ? '查看我的账户' : '了解积分体系' }}</span>
              <ArrowRight aria-hidden="true" />
            </a>
            <a class="points-action points-action--secondary" href="#points-rewards">
              浏览兑换好物
            </a>
          </div>
        </div>

        <aside class="account-card" aria-label="我的积分账户">
          <div class="account-card__head">
            <span>我的积分</span>
            <Wallet aria-hidden="true" />
          </div>
          <div class="account-card__balance">
            <strong>{{ isLoggedIn ? formatPoints(myAccount?.balance || 0) : '—' }}</strong>
            <span>POINTS</span>
          </div>
          <div class="account-card__metrics">
            <div>
              <span>社区排名</span>
              <strong>{{ myAccount?.rank ? `#${myAccount.rank}` : '—' }}</strong>
            </div>
            <div>
              <span>累计获得</span>
              <strong>{{ isLoggedIn ? formatPoints(myAccount?.totalEarned || 0) : '—' }}</strong>
            </div>
            <div>
              <span>已兑换</span>
              <strong>{{ isLoggedIn ? formatPoints(myAccount?.totalSpent || 0) : '—' }}</strong>
            </div>
          </div>
          <div v-if="isLoggedIn" class="account-card__foot">
            <span>当前可兑换 {{ affordableItemCount }} 项</span>
            <span :class="netMovement >= 0 ? 'is-positive' : 'is-negative'">
              近期 {{ netMovement > 0 ? '+' : '' }}{{ formatPoints(netMovement) }}
            </span>
          </div>
          <button v-else class="account-card__login" type="button" @click="goLogin">
            登录查看我的积分
            <ArrowRight aria-hidden="true" />
          </button>
        </aside>
      </div>
    </section>

    <section id="points-overview" class="points-section points-overview">
      <div class="container">
        <header class="points-section__heading">
          <div>
            <span class="points-eyebrow">账户概览</span>
            <h2>看见每一次成长</h2>
          </div>
          <p>最近 30 笔记录汇成余额曲线，精确变动同时保留在流水中。</p>
        </header>

        <div class="points-dashboard">
          <article class="points-panel trend-panel">
            <header class="points-panel__head">
              <div>
                <span>积分趋势</span>
                <h3>{{ trendHeadline }}</h3>
              </div>
              <span v-if="isLoggedIn" class="points-tag"
                >最近 {{ recentTransactions.length }} 笔</span
              >
            </header>

            <PointsTrendChart
              v-if="isLoggedIn"
              :current-balance="Number(myAccount?.balance || 0)"
              :transactions="recentTransactions"
            />
            <div v-else class="points-login-state">
              <TrendCharts aria-hidden="true" />
              <strong>登录后查看积分走势</strong>
              <p>你的积分余额、收支变化与累计贡献会集中展示在这里。</p>
              <el-button type="primary" @click="goLogin">立即登录</el-button>
            </div>
          </article>

          <article class="points-panel activity-panel">
            <header class="points-panel__head">
              <div>
                <span>最近变动</span>
                <h3>积分流水</h3>
              </div>
              <Coin aria-hidden="true" />
            </header>

            <template v-if="isLoggedIn">
              <ol v-if="recentTransactions.length" class="activity-list">
                <li v-for="item in recentTransactions.slice(0, 7)" :key="item.id">
                  <span
                    class="activity-list__mark"
                    :class="Number(item.delta) >= 0 ? 'is-positive' : 'is-negative'"
                    aria-hidden="true"
                  >
                    {{ Number(item.delta) >= 0 ? '+' : '−' }}
                  </span>
                  <div>
                    <strong>{{ item.description || transactionTypeText(item.type) }}</strong>
                    <span>{{ formatDateTime(item.createdAt) }}</span>
                  </div>
                  <b :class="Number(item.delta) >= 0 ? 'is-positive' : 'is-negative'">
                    {{ Number(item.delta) > 0 ? '+' : '' }}{{ formatPoints(item.delta || 0) }}
                  </b>
                </li>
              </ol>
              <div v-else class="points-compact-empty">
                <Coin aria-hidden="true" />
                <strong>暂无积分流水</strong>
                <span>参与活动后，记录会出现在这里。</span>
              </div>
            </template>

            <div v-else class="points-compact-empty">
              <Wallet aria-hidden="true" />
              <strong>积分记录仅自己可见</strong>
              <span>登录后即可查看每一笔获得和使用记录。</span>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="points-section points-section--soft">
      <div class="container leaderboard-layout">
        <header class="points-section__heading leaderboard-heading">
          <div>
            <span class="points-eyebrow">社区排行</span>
            <h2>和优秀的贡献者一起前进</h2>
          </div>
          <label class="leaderboard-search">
            <Search aria-hidden="true" />
            <span class="sr-only">搜索成员</span>
            <input
              v-model.trim="leaderboardQuery"
              type="search"
              placeholder="搜索姓名、学号或班级"
            />
          </label>
        </header>

        <div class="points-panel leaderboard-panel">
          <div v-if="!leaderboardQuery && topLeaderboard.length" class="leaderboard-podium">
            <article
              v-for="item in topLeaderboard"
              :key="item.userId"
              :class="[`is-rank-${item.rank}`, { 'is-me': isCurrentUser(item) }]"
            >
              <div class="leaderboard-avatar">{{ displayName(item).slice(0, 1) }}</div>
              <span class="leaderboard-rank"
                ><Trophy v-if="item.rank === 1" aria-hidden="true" />#{{ item.rank }}</span
              >
              <strong>{{ displayName(item) }}</strong>
              <small>{{ item.className || item.studentId || '社团成员' }}</small>
              <b>{{ formatPoints(item.balance || 0) }} <span>积分</span></b>
            </article>
          </div>

          <div class="leaderboard-list" :class="{ 'is-searching': leaderboardQuery }">
            <div
              v-for="item in leaderboardList"
              :key="item.userId"
              class="leaderboard-row"
              :class="{ 'is-me': isCurrentUser(item) }"
            >
              <span class="rank-mark">{{ item.rank }}</span>
              <div class="leaderboard-row__person">
                <strong>{{ displayName(item) }}</strong>
                <span>{{ item.className || item.studentId || '社团成员' }}</span>
              </div>
              <span v-if="isCurrentUser(item)" class="points-tag">我</span>
              <b>{{ formatPoints(item.balance || 0) }}</b>
            </div>

            <div v-if="!leaderboardList.length && !loading.page" class="points-compact-empty">
              <Search aria-hidden="true" />
              <strong>{{ leaderboardQuery ? '没有找到相关成员' : '暂无积分排名' }}</strong>
              <span>{{
                leaderboardQuery
                  ? '试试姓名、学号或班级中的其他关键词。'
                  : '第一位贡献者会出现在这里。'
              }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="points-rewards" class="points-section rewards-section">
      <div class="container">
        <header class="points-section__heading">
          <div>
            <span class="points-eyebrow">积分好物</span>
            <h2>把贡献兑换成回馈</h2>
          </div>
          <p v-if="isLoggedIn">
            当前余额可兑换 {{ affordableItemCount }} 项，提交后可在页面底部跟进处理状态。
          </p>
          <p v-else>登录后即可使用积分提交兑换申请。</p>
        </header>

        <div v-if="items.length" class="rewards-grid">
          <article v-for="item in items" :key="item.id" class="reward-card">
            <div class="reward-card__media" :class="{ 'has-image': item.imageUrl }">
              <img v-if="item.imageUrl" :alt="item.name" :src="item.imageUrl" loading="lazy" />
              <div v-else class="reward-card__fallback">
                <Present aria-hidden="true" />
                <span>{{ formatPoints(item.pointCost || 0) }}</span>
              </div>
              <span class="reward-card__stock">{{ stockLabel(item) }}</span>
            </div>
            <div class="reward-card__body">
              <div>
                <h3>{{ item.name }}</h3>
                <p>
                  {{ item.description || '兑换说明正在完善中，提交前可向管理员确认领取方式。' }}
                </p>
              </div>
              <div class="reward-card__price">
                <strong>{{ formatPoints(item.pointCost || 0) }}</strong>
                <span>积分</span>
              </div>
              <el-button
                :disabled="!canRedeem(item)"
                :aria-label="`${redeemButtonText(item)}：${item.name}`"
                type="primary"
                @click="openRedeemDialog(item)"
              >
                {{ redeemButtonText(item) }}
              </el-button>
            </div>
          </article>
        </div>

        <div v-else-if="!loading.page" class="points-panel rewards-empty">
          <Present aria-hidden="true" />
          <strong>好物正在准备中</strong>
          <p>新的兑换项目上线后会第一时间出现在这里。</p>
        </div>
      </div>
    </section>

    <section v-if="isLoggedIn" class="points-section points-section--soft redemption-section">
      <div class="container">
        <header class="points-section__heading">
          <div>
            <span class="points-eyebrow">兑换进度</span>
            <h2>我的兑换记录</h2>
          </div>
          <p>状态变化会同步更新，如有领取说明请留意后台备注。</p>
        </header>

        <div class="points-panel redemption-panel">
          <el-table v-if="redemptions.length" class="redemption-table" :data="redemptions">
            <el-table-column label="兑换项" min-width="180" prop="itemName" />
            <el-table-column label="所用积分" prop="points" width="110">
              <template #default="{ row }">{{ formatPoints(row.points || 0) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="redemptionStatusType(row.status)">
                  {{ redemptionStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="后台备注" min-width="200">
              <template #default="{ row }">{{ row.adminNote || '—' }}</template>
            </el-table-column>
            <el-table-column label="申请时间" width="180">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>

          <div v-if="redemptions.length" class="redemption-cards">
            <article v-for="row in redemptions" :key="row.id">
              <div>
                <strong>{{ row.itemName }}</strong>
                <el-tag :type="redemptionStatusType(row.status)">
                  {{ redemptionStatusText(row.status) }}
                </el-tag>
              </div>
              <dl>
                <div>
                  <dt>所用积分</dt>
                  <dd>{{ formatPoints(row.points || 0) }}</dd>
                </div>
                <div>
                  <dt>申请时间</dt>
                  <dd>{{ formatDateTime(row.createdAt) }}</dd>
                </div>
                <div>
                  <dt>后台备注</dt>
                  <dd>{{ row.adminNote || '—' }}</dd>
                </div>
              </dl>
            </article>
          </div>

          <div v-else class="points-compact-empty redemption-empty">
            <Tickets aria-hidden="true" />
            <strong>还没有兑换记录</strong>
            <span>选中喜欢的好物并提交申请后，可以在这里跟进状态。</span>
          </div>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="redeemVisible"
      :close-on-click-modal="false"
      title="确认兑换信息"
      width="min(520px, calc(100vw - 32px))"
    >
      <div v-if="currentItem" class="redeem-head">
        <div>
          <span>兑换项目</span>
          <strong>{{ currentItem.name }}</strong>
        </div>
        <div>
          <span>所需积分</span>
          <strong>{{ formatPoints(currentItem.pointCost || 0) }}</strong>
        </div>
      </div>
      <el-form :model="redeemForm" label-position="top">
        <el-form-item label="领取人">
          <el-input
            v-model="redeemForm.receiverName"
            autocomplete="name"
            placeholder="请输入领取人姓名"
          />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input
            v-model="redeemForm.receiverContact"
            autocomplete="tel"
            placeholder="请输入手机或其他联系方式"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="redeemForm.remark"
            :rows="3"
            maxlength="500"
            placeholder="补充领取时间、尺寸等信息（选填）"
            show-word-limit
            type="textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="redeemVisible = false">取消</el-button>
        <el-button :loading="loading.redeem" type="primary" @click="submitRedeem">
          确认提交
        </el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script lang="ts" setup>
import { pointApi } from '@/api'
import ViewPage from '@/components/common/ViewPage.vue'
import PointsTrendChart from '@/components/site/points/PointsTrendChart.vue'
import { getToken } from '@/utils/auth.ts'
import { formatDateTime } from '@/utils/format.ts'
import {
  ArrowRight,
  Coin,
  Present,
  Search,
  Tickets,
  TrendCharts,
  Trophy,
  Wallet,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

type PointItem = {
  id: string | number
  name: string
  description?: string
  imageUrl?: string
  pointCost?: number
  availableStock?: number | null
}

const router = useRouter()
const LEADERBOARD_LIMIT = 200
const numberFormatter = new Intl.NumberFormat('zh-CN')

const loading = ref({ page: false, redeem: false })
const leaderboard = ref<any[]>([])
const leaderboardQuery = ref('')
const items = ref<PointItem[]>([])
const summary = ref<any>(null)
const redemptions = ref<any[]>([])
const redeemVisible = ref(false)
const currentItem = ref<PointItem | null>(null)
const redeemForm = ref({ receiverName: '', receiverContact: '', remark: '' })

const isLoggedIn = computed(() => Boolean(getToken()))
const myAccount = computed(() => summary.value?.account || null)
const recentTransactions = computed<any[]>(() => summary.value?.recentTransactions || [])
const topLeaderboard = computed(() => leaderboard.value.slice(0, 3))
const affordableItemCount = computed(
  () =>
    items.value.filter((item) => {
      const hasStock =
        item.availableStock === null ||
        item.availableStock === undefined ||
        Number(item.availableStock) > 0
      return hasStock && Number(myAccount.value?.balance || 0) >= Number(item.pointCost || 0)
    }).length,
)
const netMovement = computed(() =>
  recentTransactions.value.reduce((total, item) => total + Number(item.delta || 0), 0),
)
const trendHeadline = computed(() => {
  if (!isLoggedIn.value) return '你的积分成长轨迹'
  if (!recentTransactions.value.length) return '等待第一笔积分记录'
  if (netMovement.value > 0) return `近期净增长 ${formatPoints(netMovement.value)}`
  if (netMovement.value < 0) return `近期净使用 ${formatPoints(Math.abs(netMovement.value))}`
  return '近期积分收支平衡'
})
const leaderboardList = computed(() => {
  const keyword = leaderboardQuery.value.toLowerCase()
  if (!keyword) return leaderboard.value.slice(3)
  return leaderboard.value.filter((item) =>
    [item.realName, item.userName, item.studentId, item.className]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword)),
  )
})

function formatPoints(value: number | string) {
  return numberFormatter.format(Number(value || 0))
}

function displayName(row: any) {
  return row.realName || row.userName || `用户 ${row.userId || '—'}`
}

function isCurrentUser(row: any) {
  return Boolean(myAccount.value?.userId && row.userId === myAccount.value.userId)
}

function transactionTypeText(type: string) {
  return (
    {
      checkin: '签到',
      checkin_revoke: '签到撤销',
      checkin_penalty: '签到扣分',
      checkin_penalty_revoke: '签到扣分撤销',
      activity: '参与活动',
      activity_revoke: '活动撤销',
      daily_login: '每日登录',
      blog_publish: '内容贡献',
      manual_adjust: '积分调整',
      redemption: '兑换扣除',
      redemption_refund: '兑换退回',
    }[type] ||
    type ||
    '积分变动'
  )
}

function redemptionStatusText(status: string) {
  return (
    { pending: '待处理', fulfilled: '已完成', cancelled: '已取消', rejected: '已驳回' }[status] ||
    status ||
    '未知状态'
  )
}

function redemptionStatusType(status: string) {
  return (
    { pending: 'warning', fulfilled: 'success', cancelled: 'info', rejected: 'danger' }[status] ||
    'info'
  )
}

function hasStock(item: PointItem) {
  return (
    item.availableStock === null ||
    item.availableStock === undefined ||
    Number(item.availableStock) > 0
  )
}

function stockLabel(item: PointItem) {
  if (!hasStock(item)) return '暂时售罄'
  if (item.availableStock === null || item.availableStock === undefined) return '库存充足'
  return `剩余 ${item.availableStock}`
}

function canRedeem(item: PointItem) {
  return (
    isLoggedIn.value &&
    hasStock(item) &&
    Number(myAccount.value?.balance || 0) >= Number(item.pointCost || 0)
  )
}

function redeemButtonText(item: PointItem) {
  if (!isLoggedIn.value) return '登录后兑换'
  if (!hasStock(item)) return '暂时售罄'
  if (Number(myAccount.value?.balance || 0) < Number(item.pointCost || 0)) return '积分不足'
  return '立即兑换'
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: '/points' } })
}

function openRedeemDialog(item: PointItem) {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  currentItem.value = item
  redeemForm.value = { receiverName: '', receiverContact: '', remark: '' }
  redeemVisible.value = true
}

async function submitRedeem() {
  if (!currentItem.value) return
  loading.value.redeem = true
  try {
    await pointApi.redeem(currentItem.value.id, redeemForm.value)
    ElMessage.success('兑换申请已提交')
    redeemVisible.value = false
    await fetchAll()
  } finally {
    loading.value.redeem = false
  }
}

async function fetchAll() {
  loading.value.page = true
  try {
    const publicTasks = [pointApi.leaderboard({ limit: LEADERBOARD_LIMIT }), pointApi.siteItems()]
    const [leaderboardResult, itemsResult] = await Promise.all(publicTasks)
    leaderboard.value = leaderboardResult || []
    items.value = itemsResult || []

    if (isLoggedIn.value) {
      summary.value = await pointApi.mySummary()
      redemptions.value = summary.value?.redemptions || []
    } else {
      summary.value = null
      redemptions.value = []
    }
  } finally {
    loading.value.page = false
  }
}

onMounted(fetchAll)
</script>

<style scoped>
.points-page {
  min-height: calc(100dvh - var(--oa-site-header-height));
  background: var(--oa-page-bg);
  color: var(--oa-text);
}

.points-hero {
  position: relative;
  overflow: hidden;
  padding: clamp(88px, 11vw, 144px) 0 clamp(64px, 8vw, 104px);
  background:
    radial-gradient(
      circle at 82% 10%,
      color-mix(in srgb, var(--oa-text) 7%, transparent),
      transparent 28%
    ),
    linear-gradient(180deg, var(--oa-elevated-bg), var(--oa-page-soft-bg));
}

.points-hero::after {
  position: absolute;
  right: -8vw;
  bottom: -280px;
  width: 560px;
  height: 560px;
  border: 1px solid color-mix(in srgb, var(--oa-border) 68%, transparent);
  border-radius: 50%;
  content: '';
  pointer-events: none;
}

.points-hero__inner {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(380px, 0.72fr);
  gap: clamp(40px, 7vw, 96px);
  align-items: center;
}

.points-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--oa-muted);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.points-eyebrow i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 0 5px var(--color-success-bg);
}

.points-hero h1 {
  max-width: 720px;
  margin: var(--space-5) 0 0;
  font-family: var(--font-family-sans);
  font-size: clamp(44px, 6.5vw, 84px);
  font-weight: var(--font-weight-semibold);
  line-height: 1.03;
  letter-spacing: -0.055em;
}

.points-hero__copy > p {
  max-width: 620px;
  margin: var(--space-6) 0 0;
  color: var(--oa-text-soft);
  font-size: clamp(16px, 1.6vw, 19px);
  line-height: 1.75;
}

.points-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-8);
}

.points-action {
  display: inline-flex;
  min-height: 46px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 20px;
  border: 1px solid transparent;
  border-radius: var(--radius-round);
  color: var(--oa-text);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  transition:
    background-color var(--duration-base) var(--ease-standard),
    border-color var(--duration-base) var(--ease-standard),
    color var(--duration-base) var(--ease-standard);
}

.points-action svg {
  width: 17px;
  height: 17px;
}

.points-action--primary {
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
}

.points-action--primary:hover {
  background: var(--oa-active-hover-bg);
}

.points-action--secondary {
  border-color: var(--oa-border-strong);
  background: var(--oa-elevated-bg);
}

.points-action--secondary:hover {
  background: var(--oa-button-hover-bg);
}

.points-action:focus-visible,
.account-card__login:focus-visible,
.leaderboard-search:focus-within {
  outline: 2px solid var(--oa-text);
  outline-offset: 3px;
}

.account-card {
  position: relative;
  overflow: hidden;
  padding: clamp(24px, 3vw, 34px);
  border: 1px solid color-mix(in srgb, var(--color-white) 16%, transparent);
  border-radius: var(--radius-xl);
  background: linear-gradient(145deg, var(--color-gray-950), var(--color-gray-700));
  box-shadow: var(--shadow-lg);
  color: var(--color-white);
}

.account-card::after {
  position: absolute;
  top: -100px;
  right: -70px;
  width: 220px;
  height: 220px;
  border: 1px solid color-mix(in srgb, var(--color-white) 14%, transparent);
  border-radius: 50%;
  content: '';
}

.account-card__head,
.account-card__foot,
.account-card__login {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.account-card__head {
  color: var(--color-gray-300);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.account-card__head svg {
  width: 22px;
  height: 22px;
}

.account-card__balance {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin: 38px 0;
}

.account-card__balance strong {
  overflow: hidden;
  font-size: clamp(48px, 6vw, 72px);
  font-variant-numeric: tabular-nums;
  font-weight: var(--font-weight-semibold);
  line-height: 0.86;
  letter-spacing: -0.06em;
  text-overflow: ellipsis;
}

.account-card__balance span {
  padding-bottom: 4px;
  color: var(--color-gray-400);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.12em;
}

.account-card__metrics {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
  padding-top: var(--space-5);
  border-top: 1px solid color-mix(in srgb, var(--color-white) 15%, transparent);
}

.account-card__metrics div {
  min-width: 0;
}

.account-card__metrics span,
.account-card__foot {
  color: var(--color-gray-400);
  font-size: var(--font-size-xs);
}

.account-card__metrics strong {
  display: block;
  overflow: hidden;
  margin-top: 7px;
  color: var(--color-white);
  font-size: clamp(17px, 2vw, 22px);
  font-variant-numeric: tabular-nums;
  text-overflow: ellipsis;
}

.account-card__foot {
  margin-top: var(--space-6);
}

.account-card__foot .is-positive {
  color: var(--color-success);
}

.account-card__foot .is-negative {
  color: var(--color-danger);
}

.account-card__login {
  width: 100%;
  min-height: 46px;
  margin-top: var(--space-6);
  padding: 0 16px;
  border: 1px solid color-mix(in srgb, var(--color-white) 22%, transparent);
  border-radius: var(--radius-round);
  background: color-mix(in srgb, var(--color-white) 10%, transparent);
  color: var(--color-white);
  cursor: pointer;
  font: inherit;
}

.account-card__login:hover {
  background: color-mix(in srgb, var(--color-white) 16%, transparent);
}

.account-card__login svg {
  width: 17px;
  height: 17px;
}

.points-section {
  padding: clamp(64px, 8vw, 104px) 0;
}

.points-section--soft {
  background: var(--oa-page-soft-bg);
}

.points-section__heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-8);
  margin-bottom: clamp(28px, 4vw, 44px);
}

.points-section__heading h2 {
  margin: 10px 0 0;
  font-size: clamp(30px, 4vw, 48px);
  font-weight: var(--font-weight-semibold);
  line-height: 1.12;
  letter-spacing: -0.035em;
}

.points-section__heading > p {
  max-width: 470px;
  margin: 0;
  color: var(--oa-muted);
  font-size: 15px;
  line-height: 1.7;
  text-align: right;
}

.points-dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.75fr);
  gap: var(--space-5);
  align-items: stretch;
}

.points-panel {
  min-width: 0;
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-xl);
  background: var(--oa-elevated-bg);
}

.trend-panel,
.activity-panel {
  padding: clamp(20px, 3vw, 30px);
}

.points-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.points-panel__head > div > span {
  color: var(--oa-muted);
  font-size: var(--font-size-sm);
}

.points-panel__head h3 {
  margin: 7px 0 0;
  font-size: clamp(21px, 2.3vw, 28px);
  font-weight: var(--font-weight-semibold);
  letter-spacing: -0.02em;
}

.points-panel__head > svg {
  width: 22px;
  height: 22px;
  color: var(--oa-muted);
}

.points-tag {
  display: inline-flex;
  min-height: 28px;
  flex: 0 0 auto;
  align-items: center;
  padding: 0 10px;
  border-radius: var(--radius-round);
  background: var(--oa-page-soft-bg);
  color: var(--oa-text-soft);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.points-login-state,
.points-compact-empty,
.rewards-empty {
  display: grid;
  justify-items: center;
  align-content: center;
  text-align: center;
}

.points-login-state {
  min-height: 330px;
  gap: 12px;
  padding: var(--space-8);
  border: 1px dashed var(--oa-border);
  border-radius: var(--radius-lg);
}

.points-login-state > svg,
.points-compact-empty > svg,
.rewards-empty > svg {
  width: 30px;
  height: 30px;
  color: var(--oa-muted);
}

.points-login-state strong,
.points-compact-empty strong,
.rewards-empty strong {
  margin-top: 4px;
  color: var(--oa-text);
  font-size: var(--font-size-lg);
}

.points-login-state p,
.rewards-empty p {
  max-width: 390px;
  margin: 0 0 8px;
  color: var(--oa-muted);
  line-height: 1.65;
}

.activity-list {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.activity-list li {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-3);
  min-height: 62px;
  border-bottom: 1px solid var(--oa-divider);
}

.activity-list li:last-child {
  border-bottom: 0;
}

.activity-list__mark {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
}

.activity-list__mark.is-positive {
  background: var(--color-success-bg);
}

.activity-list__mark.is-negative {
  background: var(--color-danger-bg);
}

.activity-list li > div {
  min-width: 0;
}

.activity-list strong,
.activity-list li > div span {
  display: block;
}

.activity-list strong {
  overflow: hidden;
  color: var(--oa-text);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-list li > div span {
  margin-top: 4px;
  color: var(--oa-muted);
  font-size: var(--font-size-xs);
}

.activity-list b {
  font-size: var(--font-size-base);
  font-variant-numeric: tabular-nums;
  font-weight: var(--font-weight-semibold);
}

.is-positive {
  color: var(--color-success);
}

.is-negative {
  color: var(--color-danger);
}

.points-compact-empty {
  min-height: 250px;
  gap: 9px;
  padding: var(--space-6);
  color: var(--oa-muted);
}

.points-compact-empty span {
  max-width: 360px;
  font-size: var(--font-size-sm);
  line-height: 1.6;
}

.leaderboard-layout {
  display: grid;
}

.leaderboard-heading {
  align-items: center;
}

.leaderboard-search {
  display: flex;
  width: min(360px, 100%);
  min-height: 46px;
  align-items: center;
  gap: 10px;
  padding: 0 15px;
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-round);
  background: var(--oa-elevated-bg);
  transition: border-color var(--duration-base) var(--ease-standard);
}

.leaderboard-search svg {
  width: 17px;
  height: 17px;
  flex: 0 0 auto;
  color: var(--oa-muted);
}

.leaderboard-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--oa-text);
  font: inherit;
  font-size: 16px;
}

.leaderboard-search input::placeholder {
  color: var(--oa-muted);
}

.leaderboard-panel {
  overflow: hidden;
  padding: clamp(20px, 3vw, 32px);
}

.leaderboard-podium {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.leaderboard-podium article {
  display: grid;
  min-width: 0;
  justify-items: center;
  padding: var(--space-6);
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-lg);
  background: var(--oa-page-soft-bg);
  text-align: center;
}

.leaderboard-podium article.is-rank-1 {
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
}

.leaderboard-podium article.is-me {
  box-shadow: inset 0 0 0 2px var(--color-success);
}

.leaderboard-avatar {
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  border: 1px solid var(--oa-border);
  border-radius: 50%;
  background: var(--oa-elevated-bg);
  color: var(--oa-text);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
}

.leaderboard-rank {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 14px;
  color: var(--oa-muted);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.08em;
}

.leaderboard-rank svg {
  width: 15px;
  height: 15px;
}

.leaderboard-podium article.is-rank-1 .leaderboard-rank,
.leaderboard-podium article.is-rank-1 small {
  color: color-mix(in srgb, var(--oa-active-text) 70%, transparent);
}

.leaderboard-podium article > strong {
  overflow: hidden;
  max-width: 100%;
  margin-top: 8px;
  font-size: var(--font-size-lg);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leaderboard-podium article > small {
  overflow: hidden;
  max-width: 100%;
  margin-top: 5px;
  color: var(--oa-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leaderboard-podium article > b {
  margin-top: var(--space-4);
  font-size: clamp(22px, 2.4vw, 30px);
  font-variant-numeric: tabular-nums;
}

.leaderboard-podium article > b span {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
}

.leaderboard-list {
  --leaderboard-row-height: 64px;
  display: grid;
  max-height: calc(var(--leaderboard-row-height) * 8);
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.leaderboard-list.is-searching {
  max-height: calc(var(--leaderboard-row-height) * 10);
}

.leaderboard-row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: var(--space-4);
  min-height: var(--leaderboard-row-height);
  padding: 0 var(--space-4);
  border-bottom: 1px solid var(--oa-divider);
  transition: background-color var(--duration-fast) var(--ease-standard);
}

.leaderboard-row:hover,
.leaderboard-row.is-me {
  background: var(--oa-page-soft-bg);
}

.rank-mark {
  color: var(--oa-muted);
  font-size: var(--font-size-sm);
  font-variant-numeric: tabular-nums;
  font-weight: var(--font-weight-semibold);
}

.leaderboard-row__person {
  min-width: 0;
}

.leaderboard-row__person strong,
.leaderboard-row__person span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leaderboard-row__person strong {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
}

.leaderboard-row__person span {
  margin-top: 3px;
  color: var(--oa-muted);
  font-size: var(--font-size-xs);
}

.leaderboard-row > b {
  min-width: 78px;
  text-align: right;
  font-size: var(--font-size-lg);
  font-variant-numeric: tabular-nums;
}

.rewards-section {
  background: var(--oa-page-bg);
}

.rewards-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-5);
}

.reward-card {
  overflow: hidden;
  min-width: 0;
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-xl);
  background: var(--oa-elevated-bg);
  transition:
    border-color var(--duration-base) var(--ease-standard),
    box-shadow var(--duration-base) var(--ease-standard);
}

.reward-card:hover {
  border-color: var(--oa-border-strong);
  box-shadow: var(--shadow-sm);
}

.reward-card__media {
  position: relative;
  display: grid;
  aspect-ratio: 16 / 9;
  place-items: center;
  overflow: hidden;
  background: var(--oa-page-soft-bg);
}

.reward-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--duration-slow) var(--ease-out);
}

.reward-card:hover .reward-card__media img {
  transform: scale(1.025);
}

.reward-card__fallback {
  display: grid;
  justify-items: center;
  gap: 10px;
  color: var(--oa-muted);
}

.reward-card__fallback svg {
  width: 34px;
  height: 34px;
}

.reward-card__fallback span {
  font-size: var(--font-size-sm);
  font-variant-numeric: tabular-nums;
  font-weight: var(--font-weight-semibold);
}

.reward-card__stock {
  position: absolute;
  top: 12px;
  right: 12px;
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  padding: 0 10px;
  border: 1px solid color-mix(in srgb, var(--oa-border) 70%, transparent);
  border-radius: var(--radius-round);
  background: color-mix(in srgb, var(--oa-elevated-bg) 88%, transparent);
  color: var(--oa-text-soft);
  font-size: var(--font-size-xs);
  backdrop-filter: blur(12px);
}

.reward-card__body {
  display: grid;
  gap: var(--space-5);
  padding: var(--space-5);
}

.reward-card h3 {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  letter-spacing: -0.015em;
}

.reward-card p {
  min-height: 48px;
  margin: 8px 0 0;
  color: var(--oa-muted);
  font-size: var(--font-size-sm);
  line-height: 1.65;
}

.reward-card__price {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.reward-card__price strong {
  font-size: 26px;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.025em;
}

.reward-card__price span {
  color: var(--oa-muted);
  font-size: var(--font-size-xs);
}

.reward-card :deep(.el-button) {
  width: 100%;
  min-height: 44px;
  border-radius: var(--radius-round);
}

.rewards-empty {
  min-height: 280px;
  gap: 10px;
  padding: var(--space-8);
}

.redemption-panel {
  overflow: hidden;
}

.redemption-table {
  width: 100%;
}

.redemption-cards {
  display: none;
}

.redemption-empty {
  min-height: 260px;
}

.redeem-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-5);
  margin-bottom: var(--space-6);
  padding: var(--space-4);
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-lg);
  background: var(--oa-page-soft-bg);
}

.redeem-head > div:last-child {
  text-align: right;
}

.redeem-head span,
.redeem-head strong {
  display: block;
}

.redeem-head span {
  color: var(--oa-muted);
  font-size: var(--font-size-xs);
}

.redeem-head strong {
  margin-top: 6px;
  font-size: var(--font-size-md);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 1080px) {
  .points-hero__inner,
  .points-dashboard {
    grid-template-columns: 1fr;
  }

  .points-hero__copy {
    max-width: 760px;
  }

  .account-card {
    max-width: 680px;
  }

  .rewards-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .points-hero {
    padding-top: 76px;
  }

  .points-hero h1 {
    font-size: clamp(42px, 13vw, 64px);
  }

  .points-hero::after {
    display: none;
  }

  .points-section__heading,
  .leaderboard-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-5);
  }

  .points-section__heading > p {
    max-width: 600px;
    text-align: left;
  }

  .leaderboard-search {
    width: 100%;
  }

  .leaderboard-podium {
    grid-template-columns: 1fr;
  }

  .leaderboard-podium article {
    grid-template-columns: 48px minmax(0, 1fr) auto;
    justify-items: start;
    gap: 3px var(--space-3);
    text-align: left;
  }

  .leaderboard-avatar {
    grid-row: 1 / 4;
    width: 46px;
    height: 46px;
  }

  .leaderboard-rank {
    margin-top: 0;
  }

  .leaderboard-podium article > strong,
  .leaderboard-podium article > small {
    grid-column: 2;
    margin-top: 0;
  }

  .leaderboard-podium article > b {
    grid-column: 3;
    grid-row: 1 / 4;
    align-self: center;
    margin-top: 0;
  }

  .redemption-table {
    display: none;
  }

  .redemption-cards {
    display: grid;
    gap: var(--space-3);
    padding: var(--space-4);
  }

  .redemption-cards article {
    padding: var(--space-4);
    border: 1px solid var(--oa-border);
    border-radius: var(--radius-lg);
    background: var(--oa-page-soft-bg);
  }

  .redemption-cards article > div {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
  }

  .redemption-cards dl {
    display: grid;
    gap: 10px;
    margin: var(--space-4) 0 0;
  }

  .redemption-cards dl > div {
    display: grid;
    grid-template-columns: 90px minmax(0, 1fr);
    gap: var(--space-3);
  }

  .redemption-cards dt {
    color: var(--oa-muted);
  }

  .redemption-cards dd {
    margin: 0;
  }
}

@media (max-width: 640px) {
  .points-hero__actions,
  .points-action {
    width: 100%;
  }

  .account-card {
    padding: var(--space-5);
  }

  .account-card__balance {
    margin: 32px 0;
  }

  .account-card__balance strong {
    font-size: 52px;
  }

  .points-section {
    padding: 64px 0;
  }

  .rewards-grid {
    grid-template-columns: 1fr;
  }

  .leaderboard-row {
    grid-template-columns: 32px minmax(0, 1fr) auto;
    gap: var(--space-3);
    padding-inline: var(--space-2);
  }

  .leaderboard-row .points-tag {
    display: none;
  }

  .leaderboard-row > b {
    min-width: 60px;
    font-size: var(--font-size-base);
  }

  .activity-list li {
    grid-template-columns: 30px minmax(0, 1fr) auto;
    gap: 10px;
  }

  .activity-list__mark {
    width: 28px;
    height: 28px;
  }

  .redeem-head {
    grid-template-columns: 1fr;
  }

  .redeem-head > div:last-child {
    text-align: left;
  }
}

@media (prefers-reduced-motion: reduce) {
  .points-action,
  .reward-card,
  .reward-card__media img,
  .leaderboard-row {
    transition: none;
  }

  .reward-card:hover .reward-card__media img {
    transform: none;
  }
}
</style>
