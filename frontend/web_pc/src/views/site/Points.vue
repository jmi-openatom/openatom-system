<template>
  <ViewPage class="points-page" :loading="loading.page">
    <section class="points-hero">
      <div class="container points-hero__inner">
        <div>
          <span>积分中心</span>
          <h1>排行榜与兑换</h1>
          <p>签到、参与活动和后台奖励都会沉淀为积分，可在这里查看排名并提交兑换申请。</p>
        </div>
        <div class="points-summary">
          <article>
            <span>我的余额</span>
            <strong>{{ myAccount?.balance ?? (isLoggedIn ? 0 : '-') }}</strong>
          </article>
          <article>
            <span>当前排名</span>
            <strong>{{ myAccount?.rank ? `#${myAccount.rank}` : '-' }}</strong>
          </article>
          <article>
            <span>累计获得</span>
            <strong>{{ myAccount?.totalEarned ?? '-' }}</strong>
          </article>
        </div>
      </div>
    </section>

    <section class="points-section">
      <div class="container points-grid">
        <div class="points-panel leaderboard-panel">
          <div class="section-heading">
            <span>Leaderboard</span>
            <h2>积分排行榜</h2>
          </div>
          <div class="leaderboard-list">
            <div v-for="item in leaderboard" :key="item.userId" class="leaderboard-row">
              <div class="rank-mark">{{ item.rank }}</div>
              <div>
                <strong>{{ displayName(item) }}</strong>
                <span>{{ item.className || item.studentId || '社团成员' }}</span>
              </div>
              <b>{{ item.balance || 0 }}</b>
            </div>
            <el-empty v-if="!leaderboard.length && !loading.page" description="暂无积分记录" />
          </div>
        </div>

        <div class="points-panel history-panel">
          <div class="section-heading">
            <span>Records</span>
            <h2>我的记录</h2>
          </div>
          <template v-if="isLoggedIn">
            <div class="history-list">
              <div v-for="item in recentTransactions" :key="item.id" class="history-row">
                <div>
                  <strong>{{ item.description || transactionTypeText(item.type) }}</strong>
                  <span>{{ formatDateTime(item.createdAt) }}</span>
                </div>
                <b :class="item.delta >= 0 ? 'is-plus' : 'is-minus'">
                  {{ item.delta > 0 ? '+' : '' }}{{ item.delta }}
                </b>
              </div>
              <el-empty v-if="!recentTransactions.length" description="暂无积分流水" />
            </div>
          </template>
          <div v-else class="login-panel">
            <span>登录后查看余额、流水和兑换记录。</span>
            <el-button type="primary" @click="goLogin">登录</el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="points-section points-section--soft">
      <div class="container">
        <div class="section-heading">
          <span>Exchange</span>
          <h2>积分兑换</h2>
        </div>
        <div class="exchange-grid">
          <article v-for="item in items" :key="item.id" class="exchange-item">
            <div class="exchange-item__media" :class="{ 'is-empty': !item.imageUrl }">
              <img v-if="item.imageUrl" :alt="item.name" :src="item.imageUrl" />
              <span v-else>{{ item.pointCost }}</span>
            </div>
            <div class="exchange-item__body">
              <div>
                <h3>{{ item.name }}</h3>
                <p>{{ item.description || '等待后台补充兑换说明' }}</p>
              </div>
              <div class="exchange-item__meta">
                <strong>{{ item.pointCost }} 积分</strong>
                <span>{{ item.availableStock === null || item.availableStock === undefined ? '库存不限' : `剩余 ${item.availableStock}` }}</span>
              </div>
              <el-button type="primary" :disabled="!canRedeem(item)" @click="openRedeemDialog(item)">
                兑换
              </el-button>
            </div>
          </article>
        </div>
        <el-empty v-if="!items.length && !loading.page" description="暂无可兑换项目" />
      </div>
    </section>

    <section v-if="isLoggedIn" class="points-section">
      <div class="container points-panel">
        <div class="section-heading">
          <span>Orders</span>
          <h2>兑换记录</h2>
        </div>
        <el-table :data="redemptions">
          <el-table-column prop="itemName" label="兑换项" min-width="180" />
          <el-table-column prop="points" label="积分" width="90" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="redemptionStatusType(row.status)">{{ redemptionStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="adminNote" label="后台备注" min-width="180" />
          <el-table-column label="申请时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-dialog v-model="redeemVisible" title="提交兑换" width="520px">
      <div v-if="currentItem" class="redeem-head">
        <strong>{{ currentItem.name }}</strong>
        <span>{{ currentItem.pointCost }} 积分</span>
      </div>
      <el-form :model="redeemForm" label-width="90px">
        <el-form-item label="领取人">
          <el-input v-model="redeemForm.receiverName" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="redeemForm.receiverContact" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="redeemForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="redeemVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading.redeem" @click="submitRedeem">提交</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { pointApi } from '@/api'
import { getToken } from '@/utils/auth.ts'
import { formatDateTime } from '@/utils/format.ts'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref({ page: false, redeem: false })
const leaderboard = ref<any[]>([])
const items = ref<any[]>([])
const summary = ref<any>(null)
const redemptions = ref<any[]>([])
const redeemVisible = ref(false)
const currentItem = ref<any>(null)
const redeemForm = ref({ receiverName: '', receiverContact: '', remark: '' })

const isLoggedIn = computed(() => Boolean(getToken()))
const myAccount = computed(() => summary.value?.account || null)
const recentTransactions = computed(() => summary.value?.recentTransactions || [])

function displayName(row: any) {
  return row.realName || row.userName || `用户 ${row.userId || '-'}`
}

function transactionTypeText(type: string) {
  return (
    {
      checkin: '签到',
      checkin_revoke: '签到撤销',
      activity: '活动',
      activity_revoke: '活动撤销',
      manual_adjust: '手动调整',
      redemption: '兑换扣除',
      redemption_refund: '兑换退回',
    }[type] || type || '-'
  )
}

function redemptionStatusText(status: string) {
  return (
    { pending: '待处理', fulfilled: '已完成', cancelled: '已取消', rejected: '已驳回' }[status] ||
    status ||
    '-'
  )
}

function redemptionStatusType(status: string) {
  return (
    { pending: 'warning', fulfilled: 'success', cancelled: 'info', rejected: 'danger' }[status] ||
    'info'
  )
}

function canRedeem(item: any) {
  const hasStock = item.availableStock === null || item.availableStock === undefined || item.availableStock > 0
  return isLoggedIn.value && hasStock && Number(myAccount.value?.balance || 0) >= Number(item.pointCost || 0)
}

function goLogin() {
  router.push({ path: '/admin/login', query: { redirect: '/points' } })
}

function openRedeemDialog(item: any) {
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
    const tasks = [pointApi.leaderboard({ limit: 50 }), pointApi.siteItems()]
    const [leaderboardResult, itemsResult] = await Promise.all(tasks)
    leaderboard.value = leaderboardResult || []
    items.value = itemsResult || []
    if (isLoggedIn.value) {
      summary.value = await pointApi.mySummary()
      redemptions.value = summary.value?.redemptions || []
    }
  } finally {
    loading.value.page = false
  }
}

onMounted(() => {
  fetchAll()
})
</script>

<style scoped>
.points-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-elevated-bg);
}

.points-hero {
  padding: clamp(78px, 10vw, 122px) 0 48px;
  background: linear-gradient(180deg, var(--oa-elevated-bg) 0%, var(--oa-page-soft-bg) 100%);
}

.points-hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(320px, 0.58fr);
  gap: 36px;
  align-items: end;
}

.points-hero span,
.section-heading span {
  color: var(--oa-muted);
  font-size: 14px;
}

.points-hero h1,
.section-heading h2 {
  margin: 10px 0 0;
  color: var(--oa-text);
  font-size: clamp(32px, 4vw, 56px);
  line-height: 1.05;
  letter-spacing: 0;
}

.points-hero p {
  max-width: 620px;
  margin: 18px 0 0;
  color: var(--oa-text-soft);
  font-size: 16px;
  line-height: 1.8;
}

.points-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.points-summary article,
.points-panel,
.exchange-item {
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
}

.points-summary article {
  padding: 18px;
}

.points-summary strong {
  display: block;
  margin-top: 8px;
  color: var(--oa-text);
  font-size: 28px;
  line-height: 1;
}

.points-section {
  padding: 48px 0;
}

.points-section--soft {
  background: var(--oa-page-soft-bg);
}

.points-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 22px;
  align-items: start;
}

.points-panel {
  padding: 22px;
}

.section-heading {
  margin-bottom: 18px;
}

.section-heading h2 {
  font-size: clamp(24px, 2.6vw, 34px);
}

.leaderboard-list,
.history-list {
  display: grid;
  gap: 10px;
}

.leaderboard-row,
.history-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-page-soft-bg);
}

.history-row {
  grid-template-columns: minmax(0, 1fr) auto;
}

.rank-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 999px;
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
  font-weight: 700;
}

.leaderboard-row strong,
.history-row strong,
.exchange-item h3,
.redeem-head strong {
  color: var(--oa-text);
}

.leaderboard-row span,
.history-row span,
.exchange-item p,
.exchange-item__meta span,
.login-panel span,
.redeem-head span {
  color: var(--oa-muted);
  font-size: 13px;
}

.leaderboard-row b,
.history-row b {
  color: var(--oa-text);
  font-size: 20px;
}

.history-row b.is-plus {
  color: #16a34a;
}

.history-row b.is-minus {
  color: #dc2626;
}

.login-panel {
  display: grid;
  justify-items: start;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-page-soft-bg);
}

.exchange-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.exchange-item {
  overflow: hidden;
}

.exchange-item__media {
  display: grid;
  aspect-ratio: 16 / 9;
  place-items: center;
  background: var(--oa-page-bg);
}

.exchange-item__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.exchange-item__media.is-empty span {
  color: var(--oa-text);
  font-size: 42px;
  font-weight: 700;
}

.exchange-item__body {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.exchange-item h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.exchange-item p {
  min-height: 42px;
  margin: 0;
  line-height: 1.6;
}

.exchange-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.exchange-item__meta strong {
  color: var(--oa-text);
}

.redeem-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-page-soft-bg);
}

@media (max-width: 980px) {
  .points-hero__inner,
  .points-grid,
  .exchange-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .points-summary {
    grid-template-columns: 1fr;
  }

  .leaderboard-row {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .leaderboard-row b {
    grid-column: 2;
  }
}
</style>
