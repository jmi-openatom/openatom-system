<template>
  <ViewPage class="admin-page lottery-admin">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-if="clubs.length > 1"
          v-model="selectedClubId"
          filterable
          placeholder="选择社团"
          style="width: 220px"
          @change="handleClubChange"
        >
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
        </el-select>
        <el-select
          v-model="query.status"
          clearable
          placeholder="抽奖状态"
          style="width: 150px"
          @change="fetchList"
        >
          <el-option label="草稿" value="draft" />
          <el-option label="进行中" value="open" />
          <el-option label="已结束" value="closed" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增抽奖</el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="title" label="抽奖名称" min-width="220" />
      <el-table-column prop="formName" label="参与表单" min-width="180">
        <template #default="{ row }">{{ row.formName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="参与/中奖" width="130">
        <template #default="{ row }">{{ row.participantCount || 0 }} / {{ row.winnerCount || 0 }}</template>
      </el-table-column>
      <el-table-column label="奖品剩余" width="130">
        <template #default="{ row }">
          {{ row.remainingPrizeCount || 0 }} / {{ row.totalPrizeCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openConsole(row)">抽奖</el-button>
          <el-button link type="info" @click="openDialog(row)">编辑</el-button>
          <el-button link type="warning" @click="copyFormLink(row)">复制表单</el-button>
          <el-button link type="success" @click="openScreen(row)">大屏</el-button>
          <el-button v-if="row.status !== 'open'" link type="success" @click="publish(row)"
            >开始</el-button
          >
          <el-button v-if="row.status === 'open'" link type="danger" @click="close(row)"
            >结束</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="当前社团暂无抽奖活动" />

    <el-dialog
      v-model="dialogVisible"
      :title="draft.id ? '编辑抽奖活动' : '新增抽奖活动'"
      width="960px"
      @closed="resetDraft"
    >
      <el-form ref="formRef" :model="draft" :rules="rules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="所属社团" prop="clubId">
            <el-select v-model="draft.clubId" filterable placeholder="请选择社团" @change="loadForms">
              <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="参与表单" prop="formId">
            <el-select v-model="draft.formId" filterable placeholder="请选择已发布给用户填写的表单">
              <el-option v-for="form in forms" :key="form.id" :label="form.name" :value="form.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="抽奖名称" prop="title">
            <el-input v-model="draft.title" placeholder="如：开源社迎新抽奖" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="draft.status">
              <el-option label="草稿" value="draft" />
              <el-option label="进行中" value="open" />
              <el-option label="已结束" value="closed" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="抽奖说明">
          <el-input
            v-model="draft.description"
            type="textarea"
            :rows="3"
            placeholder="可填写抽奖规则或现场说明"
          />
        </el-form-item>

        <el-divider content-position="left">奖品配置</el-divider>
        <div class="prize-toolbar">
          <el-button size="small" type="primary" plain @click="addPrize">新增奖品</el-button>
          <span>奖品数量会限制可抽中的人数，已中奖的奖项不能被删除。</span>
        </div>
        <el-table :data="draft.prizes" class="admin-table prize-edit-table" row-key="rowKey">
          <el-table-column label="奖项" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.level" placeholder="如：一等奖" />
            </template>
          </el-table-column>
          <el-table-column label="奖品名称" min-width="200">
            <template #default="{ row }">
              <el-input v-model="row.name" placeholder="如：机械键盘" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" :step="1" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="排序" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.sortOrder" :step="10" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="颜色" width="130">
            <template #default="{ row }">
              <el-color-picker v-model="row.color" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removePrize($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="consoleVisible"
      class="lottery-console-drawer"
      size="960px"
      title="抽奖控制台"
      @closed="stopDetailTimer"
    >
      <div v-if="detail" class="lottery-console">
        <div class="console-header">
          <div>
            <h2>{{ detail.lottery.title }}</h2>
            <p>{{ detail.lottery.formName || '未关联表单' }}</p>
          </div>
          <div class="console-actions">
            <el-button :icon="Refresh" @click="refreshDetail">刷新</el-button>
            <el-button type="warning" @click="copyFormLink(detail.lottery)">复制表单</el-button>
            <el-button type="success" @click="openScreen(detail.lottery)">打开大屏</el-button>
            <el-button type="danger" plain @click="resetWinners">清空中奖</el-button>
          </div>
        </div>

        <div class="console-stats">
          <div>
            <span>参与人数</span>
            <strong>{{ detail.lottery.participantCount || 0 }}</strong>
          </div>
          <div>
            <span>已抽中</span>
            <strong>{{ detail.lottery.winnerCount || 0 }}</strong>
          </div>
          <div>
            <span>奖品剩余</span>
            <strong>{{ detail.lottery.remainingPrizeCount || 0 }}</strong>
          </div>
          <div>
            <span>当前状态</span>
            <strong>{{ statusText(detail.lottery.status) }}</strong>
          </div>
        </div>

        <div v-if="latestDraw" class="latest-result">
          <span>{{ latestDraw.prizeLevel || '奖品' }} · {{ latestDraw.prizeName }}</span>
          <strong>{{ latestDraw.winnerName }}</strong>
          <small>{{ latestDraw.winnerAccount || latestDraw.winnerContact || '匿名提交' }}</small>
        </div>

        <div class="prize-board">
          <div v-for="prize in detail.prizes" :key="prize.id" class="prize-row">
            <span class="prize-swatch" :style="{ backgroundColor: prize.color || '#1677ff' }" />
            <div>
              <strong>{{ prize.level || '奖品' }} · {{ prize.name }}</strong>
              <p>已抽 {{ prize.wonCount || 0 }} / {{ prize.quantity || 0 }}，剩余 {{ prize.remainingCount || 0 }}</p>
            </div>
            <el-button
              type="primary"
              :loading="drawingPrizeId === prize.id"
              :disabled="detail.lottery.status !== 'open' || !prize.remainingCount"
              @click="draw(prize.id)"
            >
              抽取
            </el-button>
          </div>
        </div>

        <el-table :data="detail.winners" class="admin-table">
          <el-table-column prop="prizeLevel" label="奖项" min-width="120" />
          <el-table-column prop="prizeName" label="奖品" min-width="160" />
          <el-table-column prop="winnerName" label="中奖人" min-width="140" />
          <el-table-column prop="winnerAccount" label="账号" min-width="140">
            <template #default="{ row }">{{ row.winnerAccount || '匿名提交' }}</template>
          </el-table-column>
          <el-table-column prop="winnerContact" label="联系方式" min-width="170">
            <template #default="{ row }">{{ row.winnerContact || '-' }}</template>
          </el-table-column>
          <el-table-column prop="wonAt" label="中奖时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.wonAt) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!detail.winners.length" description="还没有中奖记录" />
      </div>
    </el-drawer>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { clubApi, lotteryApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

type PrizeDraft = {
  id?: number
  rowKey: string
  name: string
  level: string
  quantity: number
  sortOrder: number
  color: string
}

type LotteryDraft = {
  id?: number
  clubId: number | string
  formId: number | string
  title: string
  description: string
  status: string
  prizes: PrizeDraft[]
}

type LotteryDetail = {
  lottery: Record<string, any>
  prizes: any[]
  winners: any[]
}

const prizeColors = ['#1677ff', '#13c2c2', '#faad14', '#eb2f96', '#52c41a']

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const consoleVisible = ref(false)
const drawingPrizeId = ref<number | null>(null)

const clubs = ref<any[]>([])
const forms = ref<any[]>([])
const rows = ref<any[]>([])
const selectedClubId = ref<number | string>('')
const query = ref({ status: '' })
const detail = ref<LotteryDetail | null>(null)
const latestDraw = ref<any>(null)
const formRef = ref<any>()
const router = useRouter()

let rowSeed = 0
let detailTimer: number | null = null

const draft = ref<LotteryDraft>(createEmptyDraft())

const rules = {
  clubId: [{ required: true, message: '请选择社团', trigger: 'change' }],
  formId: [{ required: true, message: '请选择参与表单', trigger: 'change' }],
  title: [{ required: true, message: '请输入抽奖名称', trigger: 'blur' }],
}

function createEmptyDraft(): LotteryDraft {
  return {
    clubId: selectedClubId.value || '',
    formId: '',
    title: '',
    description: '',
    status: 'draft',
    prizes: [createPrize()],
  }
}

function createPrize(source: Record<string, any> = {}): PrizeDraft {
  rowSeed += 1
  return {
    id: source.id,
    rowKey: String(source.id || `new-${rowSeed}`),
    level: source.level || `奖项${rowSeed}`,
    name: source.name || '',
    quantity: Number(source.quantity || 1),
    sortOrder: Number(source.sortOrder ?? rowSeed * 10),
    color: source.color || prizeColors[(rowSeed - 1) % prizeColors.length],
  }
}

async function loadClubs() {
  const result = await clubApi.list({ page: 1, pageSize: 100 })
  clubs.value = result?.list || result || []
  if (!selectedClubId.value && clubs.value.length) {
    selectedClubId.value = clubs.value[0].id
  }
  await handleClubChange(selectedClubId.value)
}

async function handleClubChange(clubId: number | string) {
  await loadForms(clubId)
  await fetchList()
}

async function loadForms(clubId: number | string = draft.value.clubId) {
  if (!clubId) {
    forms.value = []
    return
  }
  const result = await clubApi.siteForms(clubId)
  forms.value = result?.list || result || []
  if (draft.value.clubId === clubId && !forms.value.some((item) => item.id === draft.value.formId)) {
    draft.value.formId = forms.value[0]?.id || ''
  }
}

async function fetchList() {
  if (!selectedClubId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    rows.value = await lotteryApi.list({
      clubId: selectedClubId.value,
      status: query.value.status || undefined,
    })
  } finally {
    loading.value = false
  }
}

async function openDialog(row?: Record<string, any>) {
  if (!row) {
    draft.value = createEmptyDraft()
    await loadForms(draft.value.clubId)
    dialogVisible.value = true
    nextTick(() => formRef.value?.clearValidate())
    return
  }
  const result = await lotteryApi.detail(row.id)
  const lottery = result.lottery || row
  draft.value = {
    id: lottery.id,
    clubId: lottery.clubId,
    formId: lottery.formId,
    title: lottery.title || '',
    description: lottery.description || '',
    status: lottery.status || 'draft',
    prizes: (result.prizes || []).map((item: Record<string, any>) => createPrize(item)),
  }
  if (!draft.value.prizes.length) draft.value.prizes = [createPrize()]
  await loadForms(draft.value.clubId)
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function resetDraft() {
  draft.value = createEmptyDraft()
  formRef.value?.clearValidate()
}

function addPrize() {
  draft.value.prizes.push(createPrize())
}

function removePrize(index: number) {
  if (draft.value.prizes.length <= 1) {
    ElMessage.warning('请至少保留一个奖品')
    return
  }
  draft.value.prizes.splice(index, 1)
}

function buildPayload() {
  const prizes = draft.value.prizes.map((prize, index) => ({
    id: prize.id,
    level: prize.level?.trim() || `奖项${index + 1}`,
    name: prize.name?.trim(),
    quantity: Number(prize.quantity || 0),
    sortOrder: Number(prize.sortOrder || index * 10 + 10),
    color: prize.color || '',
  }))
  const invalidPrize = prizes.find((prize) => !prize.name || prize.quantity <= 0)
  if (invalidPrize) {
    ElMessage.warning('请完整填写奖品名称和数量')
    return null
  }
  return {
    formId: draft.value.formId,
    title: draft.value.title.trim(),
    description: draft.value.description?.trim() || '',
    status: draft.value.status,
    prizes,
  }
}

function save() {
  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    const payload = buildPayload()
    if (!payload) return
    saving.value = true
    try {
      if (draft.value.id) {
        await lotteryApi.update(draft.value.id, payload)
        ElMessage.success('抽奖活动已更新')
      } else {
        await lotteryApi.create(draft.value.clubId, payload)
        ElMessage.success('抽奖活动已创建')
      }
      dialogVisible.value = false
      selectedClubId.value = draft.value.clubId
      await fetchList()
    } finally {
      saving.value = false
    }
  })
}

async function publish(row: Record<string, any>) {
  await lotteryApi.publish(row.id)
  ElMessage.success('抽奖活动已开始')
  fetchList()
}

async function close(row: Record<string, any>) {
  await lotteryApi.close(row.id)
  ElMessage.success('抽奖活动已结束')
  fetchList()
}

async function openConsole(row: Record<string, any>) {
  latestDraw.value = null
  consoleVisible.value = true
  await loadDetail(row.id)
  startDetailTimer()
}

async function loadDetail(id: number | string) {
  detail.value = await lotteryApi.detail(id)
}

async function refreshDetail() {
  if (!detail.value?.lottery?.id) return
  await loadDetail(detail.value.lottery.id)
}

async function draw(prizeId: number) {
  if (!detail.value?.lottery?.id) return
  drawingPrizeId.value = prizeId
  try {
    latestDraw.value = await lotteryApi.draw(detail.value.lottery.id, { prizeId })
    await refreshDetail()
    ElMessage.success(`已抽中：${latestDraw.value?.winnerName || '-'}`)
  } finally {
    drawingPrizeId.value = null
  }
}

async function resetWinners() {
  if (!detail.value?.lottery?.id) return
  await ElMessageBox.confirm('会清空当前抽奖活动的所有中奖记录，奖品配置和表单记录不会删除。', '确认清空中奖记录')
  await lotteryApi.reset(detail.value.lottery.id)
  latestDraw.value = null
  await refreshDetail()
  await fetchList()
  ElMessage.success('中奖记录已清空')
}

async function copyFormLink(row: Record<string, any>) {
  const formId = row.formId
  if (!formId) {
    ElMessage.warning('当前抽奖未关联表单')
    return
  }
  const webLink = `${window.location.origin}${router.resolve({ path: `/forms/${formId}` }).href}`
  try {
    await navigator.clipboard.writeText(webLink)
    ElMessage.success('表单链接已复制')
  } catch {
    ElMessage.info(webLink)
  }
}

function openScreen(row: Record<string, any>) {
  const lotteryId = row.id
  if (!lotteryId) return
  const target = router.resolve({ path: `/lottery/${lotteryId}/screen` })
  window.open(target.href, '_blank')
}

function startDetailTimer() {
  stopDetailTimer()
  detailTimer = window.setInterval(() => {
    refreshDetail()
  }, 3000)
}

function stopDetailTimer() {
  if (detailTimer === null) return
  window.clearInterval(detailTimer)
  detailTimer = null
}

function statusText(status: unknown) {
  return (
    {
      draft: '草稿',
      open: '进行中',
      closed: '已结束',
    }[String(status || '').toLowerCase()] ||
    String(status || '-') ||
    '-'
  )
}

onMounted(() => {
  loadClubs()
})

onBeforeUnmount(() => {
  stopDetailTimer()
})
</script>

<style scoped>
.lottery-admin {
  display: grid;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.prize-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--oa-muted);
  font-size: 13px;
}

.prize-edit-table :deep(.el-input-number) {
  width: 100%;
}

.lottery-console {
  display: grid;
  gap: 18px;
}

.console-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--oa-border);
}

.console-header h2 {
  margin: 0 0 6px;
  color: var(--oa-text);
  font-size: 22px;
}

.console-header p {
  margin: 0;
  color: var(--oa-muted);
}

.console-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.console-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.console-stats > div {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
}

.console-stats span {
  color: var(--oa-muted);
  font-size: 13px;
}

.console-stats strong {
  color: var(--oa-text);
  font-size: 24px;
}

.latest-result {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid rgba(22, 119, 255, 0.28);
  border-radius: 8px;
  background: rgba(22, 119, 255, 0.08);
}

.latest-result span {
  color: var(--oa-muted);
  font-size: 13px;
}

.latest-result strong {
  color: var(--oa-text);
  font-size: 30px;
}

.latest-result small {
  color: var(--oa-muted);
}

.prize-board {
  display: grid;
  gap: 10px;
}

.prize-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
}

.prize-swatch {
  width: 14px;
  height: 44px;
  border-radius: 999px;
}

.prize-row strong {
  color: var(--oa-text);
}

.prize-row p {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
}

@media (max-width: 900px) {
  .form-grid,
  .console-stats {
    grid-template-columns: 1fr;
  }

  .console-header,
  .console-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .prize-row {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .prize-row .el-button {
    grid-column: 1 / -1;
  }
}
</style>
