<template>
  <ViewPage class="admin-page points-admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="accountQuery.keyword"
          clearable
          placeholder="姓名/学号/用户名"
          style="width: 220px"
          @keyup.enter="fetchAccounts"
        />
        <el-button type="primary" :icon="Refresh" @click="refreshCurrent">刷新</el-button>
      </div>
      <div class="toolbar__actions">
        <el-button type="primary" :icon="Plus" @click="openAdjustDialog">调整积分</el-button>
        <el-button :icon="Goods" @click="openItemDialog()">新增兑换项</el-button>
      </div>
    </ViewToolbar>

    <div class="point-metrics">
      <article>
        <span>积分账户</span>
        <strong>{{ accounts.length }}</strong>
      </article>
      <article>
        <span>当前总余额</span>
        <strong>{{ totalBalance }}</strong>
      </article>
      <article>
        <span>待处理兑换</span>
        <strong>{{ pendingRedemptions }}</strong>
      </article>
      <article>
        <span>上架兑换项</span>
        <strong>{{ activeItems }}</strong>
      </article>
    </div>

    <el-tabs v-model="activeTab" class="point-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="账户排行" name="accounts">
        <el-table v-loading="loading.accounts" :data="accounts" class="admin-table">
          <el-table-column prop="rank" label="排名" width="80" />
          <el-table-column label="成员" min-width="180">
            <template #default="{ row }">
              <strong>{{ displayName(row) }}</strong>
              <p class="muted-line">{{ row.studentId || row.userName || '-' }}</p>
            </template>
          </el-table-column>
          <el-table-column prop="className" label="班级" min-width="140" />
          <el-table-column prop="balance" label="余额" width="110" />
          <el-table-column prop="totalEarned" label="累计获得" width="120" />
          <el-table-column prop="totalSpent" label="兑换消耗" width="120" />
          <el-table-column label="更新时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt) || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAdjustDialog(row)">调整</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="积分流水" name="transactions">
        <div class="table-filter-row">
          <el-select v-model="transactionQuery.type" clearable placeholder="流水类型" style="width: 180px" @change="fetchTransactions">
            <el-option label="签到" value="checkin" />
            <el-option label="签到扣分" value="checkin_penalty" />
            <el-option label="签到扣分撤销" value="checkin_penalty_revoke" />
            <el-option label="活动" value="activity" />
            <el-option label="每日登录" value="daily_login" />
            <el-option label="博客发布" value="blog_publish" />
            <el-option label="手动调整" value="manual_adjust" />
            <el-option label="兑换扣除" value="redemption" />
            <el-option label="兑换退回" value="redemption_refund" />
          </el-select>
        </div>
        <el-table v-loading="loading.transactions" :data="transactions" class="admin-table">
          <el-table-column label="成员" min-width="160">
            <template #default="{ row }">
              <strong>{{ displayName(row) }}</strong>
              <p class="muted-line">{{ row.studentId || '-' }}</p>
            </template>
          </el-table-column>
          <el-table-column label="变动" width="100">
            <template #default="{ row }">
              <span :class="['delta-text', row.delta >= 0 ? 'is-plus' : 'is-minus']">
                {{ row.delta > 0 ? '+' : '' }}{{ row.delta }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="balanceAfter" label="变动后" width="110" />
          <el-table-column label="类型" width="130">
            <template #default="{ row }">{{ transactionTypeText(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="220" />
          <el-table-column label="操作人" width="130">
            <template #default="{ row }">{{ row.operatorName || '-' }}</template>
          </el-table-column>
          <el-table-column label="时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="兑换项" name="items">
        <el-table v-loading="loading.items" :data="items" class="admin-table">
          <el-table-column label="兑换项" min-width="220">
            <template #default="{ row }">
              <strong>{{ row.name }}</strong>
              <p class="muted-line">{{ row.description || '暂无说明' }}</p>
            </template>
          </el-table-column>
          <el-table-column prop="pointCost" label="所需积分" width="120" />
          <el-table-column label="库存" width="130">
            <template #default="{ row }">
              {{ row.stock === null || row.stock === undefined ? '不限' : row.availableStock }}
            </template>
          </el-table-column>
          <el-table-column prop="exchangedCount" label="已兑换" width="100" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                {{ row.status === 'active' ? '上架' : '下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="90" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="openItemDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="deleteItem(row)">下架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="兑换记录" name="redemptions">
        <div class="table-filter-row">
          <el-select v-model="redemptionQuery.status" clearable placeholder="兑换状态" style="width: 180px" @change="fetchRedemptions">
            <el-option label="待处理" value="pending" />
            <el-option label="已完成" value="fulfilled" />
            <el-option label="已取消" value="cancelled" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </div>
        <el-table v-loading="loading.redemptions" :data="redemptions" class="admin-table">
          <el-table-column label="成员" min-width="150">
            <template #default="{ row }">
              <strong>{{ displayName(row) }}</strong>
              <p class="muted-line">{{ row.studentId || '-' }}</p>
            </template>
          </el-table-column>
          <el-table-column prop="itemName" label="兑换项" min-width="180" />
          <el-table-column prop="points" label="积分" width="90" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="redemptionStatusType(row.status)">{{ redemptionStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="receiverName" label="领取人" width="120" />
          <el-table-column prop="receiverContact" label="联系方式" width="140" />
          <el-table-column prop="remark" label="备注" min-width="160" />
          <el-table-column label="申请时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRedemptionDialog(row)">处理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="积分规则" name="rules">
        <el-form
          v-loading="loading.rules"
          class="point-rules-form"
          :model="ruleForm"
          label-width="150px"
        >
          <el-form-item label="每日登录积分">
            <el-input-number v-model="ruleForm.dailyLoginPoints" :min="0" :max="POINT_AMOUNT_MAX" :step="1" />
            <span class="rule-tip">每个账号每天首次登录只发放一次，填 0 表示关闭。</span>
          </el-form-item>
          <el-form-item label="博客审核通过积分">
            <el-input-number v-model="ruleForm.blogPublishPoints" :min="0" :max="POINT_AMOUNT_MAX" :step="5" />
            <span class="rule-tip">文章通过管理员审核并展示时发放，单篇文章只发放一次。</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving.rules" @click="saveRules">保存规则</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="adjustVisible" title="调整积分" width="520px">
      <el-form :model="adjustForm" label-width="90px">
        <el-form-item label="成员">
          <el-select v-model="adjustForm.userId" filterable placeholder="选择成员" style="width: 100%">
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="displayUserOption(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="积分变动">
          <el-input-number v-model="adjustForm.delta" :min="-POINT_AMOUNT_MAX" :max="POINT_AMOUNT_MAX" :step="5" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="adjustForm.description" maxlength="120" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving.adjust" @click="saveAdjustment">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemVisible" :title="itemForm.id ? '编辑兑换项' : '新增兑换项'" width="620px">
      <el-form :model="itemForm" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="itemForm.name" />
        </el-form-item>
        <el-form-item label="所需积分">
          <el-input-number v-model="itemForm.pointCost" :min="1" :max="POINT_AMOUNT_MAX" :step="10" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input v-model="itemForm.stock" placeholder="留空或 -1 表示不限" type="number" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="itemForm.status">
            <el-option label="上架" value="active" />
            <el-option label="下架" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sortOrder" :step="1" />
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="itemForm.imageUrl" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="itemForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving.item" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="redemptionVisible" title="处理兑换" width="520px">
      <el-form :model="redemptionForm" label-width="90px">
        <el-form-item label="状态">
          <el-select v-model="redemptionForm.status" style="width: 100%">
            <el-option label="待处理" value="pending" />
            <el-option label="已完成" value="fulfilled" />
            <el-option label="已取消" value="cancelled" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="后台备注">
          <el-input v-model="redemptionForm.adminNote" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="redemptionVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving.redemption" @click="saveRedemption">保存</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { checkInApi, pointApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Goods, Plus, Refresh } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'

const activeTab = ref('accounts')
const POINT_AMOUNT_MAX = 9_000_000_000_000_000

const loading = ref({
  accounts: false,
  transactions: false,
  items: false,
  redemptions: false,
  rules: false,
})

const saving = ref({
  adjust: false,
  item: false,
  redemption: false,
  rules: false,
})

const accountQuery = ref({ keyword: '' })
const transactionQuery = ref({ type: '' })
const redemptionQuery = ref({ status: '' })

const accounts = ref<any[]>([])
const transactions = ref<any[]>([])
const items = ref<any[]>([])
const redemptions = ref<any[]>([])
const userOptions = ref<any[]>([])

const adjustVisible = ref(false)
const itemVisible = ref(false)
const redemptionVisible = ref(false)

const adjustForm = ref<any>({ userId: null, delta: 10, description: '' })
const itemForm = ref<any>({})
const redemptionForm = ref<any>({})
const ruleForm = ref<any>({ dailyLoginPoints: 1, blogPublishPoints: 20 })

const totalBalance = computed(() =>
  accounts.value.reduce((sum, item) => sum + Number(item.balance || 0), 0),
)
const pendingRedemptions = computed(
  () => redemptions.value.filter((item) => item.status === 'pending').length,
)
const activeItems = computed(() => items.value.filter((item) => item.status === 'active').length)

function displayName(row: any) {
  return row.realName || row.userName || `用户 ${row.userId || '-'}`
}

function displayUserOption(row: any) {
  return `${row.realName || row.userName || row.studentId || row.id}${row.studentId ? ` / ${row.studentId}` : ''}`
}

function transactionTypeText(type: string) {
  return (
    {
      checkin: '签到',
      checkin_revoke: '签到撤销',
      checkin_penalty: '签到扣分',
      checkin_penalty_revoke: '签到扣分撤销',
      activity: '活动',
      activity_revoke: '活动撤销',
      daily_login: '每日登录',
      blog_publish: '博客发布',
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

async function fetchAccounts() {
  loading.value.accounts = true
  try {
    accounts.value = (await pointApi.adminAccounts(accountQuery.value)) || []
  } finally {
    loading.value.accounts = false
  }
}

async function fetchTransactions() {
  loading.value.transactions = true
  try {
    transactions.value = (await pointApi.adminTransactions(transactionQuery.value)) || []
  } finally {
    loading.value.transactions = false
  }
}

async function fetchItems() {
  loading.value.items = true
  try {
    items.value = (await pointApi.adminItems({ includeInactive: true })) || []
  } finally {
    loading.value.items = false
  }
}

async function fetchRedemptions() {
  loading.value.redemptions = true
  try {
    redemptions.value = (await pointApi.adminRedemptions(redemptionQuery.value)) || []
  } finally {
    loading.value.redemptions = false
  }
}

async function fetchRules() {
  loading.value.rules = true
  try {
    ruleForm.value = {
      dailyLoginPoints: 1,
      blogPublishPoints: 20,
      ...((await pointApi.adminRules()) || {}),
    }
  } finally {
    loading.value.rules = false
  }
}

async function fetchUserOptions() {
  userOptions.value = (await checkInApi.userOptions()) || []
}

function refreshCurrent() {
  if (activeTab.value === 'accounts') fetchAccounts()
  else if (activeTab.value === 'transactions') fetchTransactions()
  else if (activeTab.value === 'items') fetchItems()
  else if (activeTab.value === 'redemptions') fetchRedemptions()
  else fetchRules()
}

function handleTabChange() {
  refreshCurrent()
}

function openAdjustDialog(row?: any) {
  adjustForm.value = {
    userId: row?.userId || null,
    delta: 10,
    description: '',
  }
  adjustVisible.value = true
}

async function saveAdjustment() {
  if (!adjustForm.value.userId) {
    ElMessage.warning('请选择成员')
    return
  }
  if (!Number(adjustForm.value.delta)) {
    ElMessage.warning('积分变动不能为 0')
    return
  }
  saving.value.adjust = true
  try {
    await pointApi.adjust(adjustForm.value)
    ElMessage.success('积分已调整')
    adjustVisible.value = false
    await Promise.all([fetchAccounts(), fetchTransactions()])
  } finally {
    saving.value.adjust = false
  }
}

function openItemDialog(row?: any) {
  itemForm.value = row
    ? { ...row, stock: row.stock === null || row.stock === undefined ? '' : row.stock }
    : {
        name: '',
        pointCost: 50,
        stock: '',
        status: 'active',
        sortOrder: 0,
        imageUrl: '',
        description: '',
      }
  itemVisible.value = true
}

function itemPayload() {
  const stockValue = itemForm.value.stock
  return {
    ...itemForm.value,
    pointCost: Number(itemForm.value.pointCost || 0),
    stock: stockValue === '' || stockValue === null || stockValue === undefined ? null : Number(stockValue),
    sortOrder: Number(itemForm.value.sortOrder || 0),
  }
}

async function saveItem() {
  if (!itemForm.value.name?.trim()) {
    ElMessage.warning('请输入兑换项名称')
    return
  }
  saving.value.item = true
  try {
    const payload = itemPayload()
    if (payload.id) await pointApi.updateItem(payload.id, payload)
    else await pointApi.createItem(payload)
    ElMessage.success('兑换项已保存')
    itemVisible.value = false
    await fetchItems()
  } finally {
    saving.value.item = false
  }
}

async function deleteItem(row: any) {
  await ElMessageBox.confirm(`确定下架“${row.name}”吗？`, '下架兑换项', { type: 'warning' })
  await pointApi.deleteItem(row.id)
  ElMessage.success('兑换项已下架')
  await fetchItems()
}

function openRedemptionDialog(row: any) {
  redemptionForm.value = { id: row.id, status: row.status, adminNote: row.adminNote || '' }
  redemptionVisible.value = true
}

async function saveRedemption() {
  saving.value.redemption = true
  try {
    await pointApi.updateRedemptionStatus(redemptionForm.value.id, {
      status: redemptionForm.value.status,
      adminNote: redemptionForm.value.adminNote,
    })
    ElMessage.success('兑换状态已更新')
    redemptionVisible.value = false
    await Promise.all([fetchRedemptions(), fetchAccounts(), fetchTransactions(), fetchItems()])
  } finally {
    saving.value.redemption = false
  }
}

async function saveRules() {
  saving.value.rules = true
  try {
    await pointApi.updateRules({
      dailyLoginPoints: Number(ruleForm.value.dailyLoginPoints || 0),
      blogPublishPoints: Number(ruleForm.value.blogPublishPoints || 0),
    })
    ElMessage.success('积分规则已保存')
    await fetchRules()
  } finally {
    saving.value.rules = false
  }
}

onMounted(async () => {
  await Promise.all([fetchAccounts(), fetchItems(), fetchRedemptions(), fetchRules(), fetchUserOptions()])
})
</script>

<style scoped>
.points-admin-page {
  display: grid;
  gap: 18px;
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
}

.point-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  min-width: 0;
  max-width: 100%;
}

.point-metrics article {
  padding: 18px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
}

.point-metrics span {
  color: var(--oa-muted);
  font-size: 13px;
}

.point-metrics strong {
  display: block;
  margin-top: 8px;
  color: var(--oa-text);
  font-size: 28px;
  line-height: 1;
}

.point-tabs {
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  padding: 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
}

.table-filter-row {
  display: flex;
  min-width: 0;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.points-admin-page :deep(.toolbar) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  max-width: 100%;
  min-width: 0;
}

.points-admin-page :deep(.toolbar)::before {
  grid-column: 1 / -1;
}

.points-admin-page :deep(.toolbar__filters),
.points-admin-page :deep(.toolbar__actions),
.points-admin-page :deep(.el-tabs__content),
.points-admin-page :deep(.el-tab-pane),
.points-admin-page :deep(.el-table),
.points-admin-page :deep(.el-table__inner-wrapper) {
  min-width: 0;
  max-width: 100%;
}

.points-admin-page :deep(.el-table) {
  width: 100%;
}

.point-rules-form {
  max-width: 760px;
  padding: 12px 0 4px;
}

.point-rules-form :deep(.el-form-item__content) {
  gap: 12px;
}

.rule-tip {
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.5;
}

.muted-line {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 12px;
}

.delta-text {
  font-weight: 700;
}

.delta-text.is-plus {
  color: #16a34a;
}

.delta-text.is-minus {
  color: #dc2626;
}

@media (max-width: 860px) {
  .point-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .point-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
