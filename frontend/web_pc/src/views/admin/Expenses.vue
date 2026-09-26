<template>
  <ViewPage class="admin-page expense-page">
    <ViewToolbar title="经费支出" description="登记已付款费用，保留凭证与修改记录">
      <div class="toolbar-actions">
        <el-button v-if="canExport" :loading="exporting" @click="exportRows">导出 CSV</el-button>
        <el-button v-if="canCreate" type="primary" :icon="Plus" @click="openEditor()">登记支出</el-button>
      </div>
    </ViewToolbar>

    <section class="expense-filters" aria-label="筛选支出记录">
      <el-select v-model="filters.clubId" placeholder="所属社团" style="width: 190px" @change="reload">
        <el-option label="全部可见社团" :value="0" />
        <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
      </el-select>
      <el-date-picker
        v-model="filters.dates"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        range-separator="至"
        style="width: 270px"
        @change="reload"
      />
      <el-select v-model="filters.category" placeholder="支出分类" clearable style="width: 150px" @change="reload">
        <el-option v-for="option in categories" :key="option.value" :label="option.label" :value="option.value" />
      </el-select>
      <el-select v-model="filters.status" style="width: 130px" @change="reload">
        <el-option label="有效记录" value="active" />
        <el-option label="已作废" value="voided" />
        <el-option label="全部状态" value="all" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="搜索事项、经手人、备注"
        style="width: 225px"
        @clear="reload"
        @keyup.enter="reload"
      />
      <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
    </section>

    <section class="expense-summary" aria-label="有效支出汇总">
      <article class="summary-card summary-card--main">
        <span>筛选范围内有效支出</span>
        <strong>{{ money(summary.totalAmount) }}</strong>
        <small>共 {{ summary.count }} 笔；作废记录不计入</small>
      </article>
      <article class="summary-card">
        <span>按分类</span>
        <div v-if="summary.byCategory.length" class="summary-lines">
          <p v-for="item in summary.byCategory.slice(0, 4)" :key="item.category">
            <span>{{ categoryLabel(item.category) }}</span><strong>{{ money(item.amount) }}</strong>
          </p>
        </div>
        <small v-else>暂无支出</small>
      </article>
      <article class="summary-card">
        <span>最近月份</span>
        <div v-if="summary.byMonth.length" class="summary-lines">
          <p v-for="item in summary.byMonth.slice(0, 4)" :key="item.month">
            <span>{{ item.month }}</span><strong>{{ money(item.amount) }}</strong>
          </p>
        </div>
        <small v-else>暂无支出</small>
      </article>
    </section>

    <el-table v-loading="loading" :data="rows" class="admin-table" row-key="id">
      <el-table-column prop="paidOn" label="付款日期" width="120" sortable />
      <el-table-column label="支出事项" min-width="220">
        <template #default="{ row }"><strong>{{ row.title }}</strong></template>
      </el-table-column>
      <el-table-column prop="clubName" label="所属社团" min-width="150" />
      <el-table-column label="分类" width="110">
        <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
      </el-table-column>
      <el-table-column label="金额" width="140" align="right">
        <template #default="{ row }"><strong>{{ money(row.amount) }}</strong></template>
      </el-table-column>
      <el-table-column prop="handledBy" label="经手人" width="120" />
      <el-table-column label="关联活动" min-width="150">
        <template #default="{ row }">{{ row.activityTitle || '—' }}</template>
      </el-table-column>
      <el-table-column label="凭证" width="90" align="center">
        <template #default="{ row }">{{ row.attachmentCount || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'">
            {{ row.status === 'active' ? '有效' : '已作废' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="canUpdate && row.status === 'active'" link type="primary" @click="openEditor(row)">编辑</el-button>
          <el-button v-if="canVoid && row.status === 'active'" link type="danger" @click="voidRow(row)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="没有符合条件的支出记录" />
    <div class="expense-pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @change="fetchData"
      />
    </div>

    <el-dialog v-model="editorVisible" :title="form.id ? '编辑支出' : '登记支出'" width="min(760px, 96vw)" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="expense-form">
        <div class="form-grid">
          <el-form-item label="所属社团" prop="clubId">
            <el-select v-model="form.clubId" placeholder="选择社团" :disabled="Boolean(form.id)" @change="loadActivities">
              <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="付款日期" prop="paidOn">
            <el-date-picker v-model="form.paidOn" type="date" value-format="YYYY-MM-DD" placeholder="选择付款日期" />
          </el-form-item>
        </div>
        <el-form-item label="支出事项" prop="title">
          <el-input v-model="form.title" maxlength="160" show-word-limit placeholder="例如：社团活动物资采购" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="分类" prop="category">
            <el-select v-model="form.category" placeholder="选择分类">
              <el-option v-for="option in categories" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="金额（元）" prop="amount">
            <el-input v-model="form.amount" inputmode="decimal" placeholder="0.00" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="经手人" prop="handledBy">
            <el-input v-model="form.handledBy" maxlength="80" placeholder="实际经手人姓名" />
          </el-form-item>
          <el-form-item label="支付方式">
            <el-select v-model="form.paymentMethod" clearable placeholder="选填">
              <el-option v-for="option in paymentMethods" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="关联活动">
          <el-select v-model="form.activityId" clearable filterable placeholder="选填，仅显示所属社团的活动">
            <el-option v-for="activity in activities" :key="activity.id" :label="activity.title" :value="activity.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="票据凭证">
          <div class="attachment-editor">
            <div v-for="attachment in attachments" :key="attachment.id" class="attachment-row">
              <span>{{ attachment.originalName }}</span>
              <el-button link type="primary" @click="downloadAttachment(form.id!, attachment)">下载</el-button>
              <el-button v-if="canAttach && form.status === 'active'" link type="danger" @click="removeAttachment(attachment)">移除</el-button>
            </div>
            <div v-for="(file, index) in pendingFiles" :key="`${file.name}-${index}`" class="attachment-row">
              <span>{{ file.name }}（待上传）</span>
              <el-button link type="danger" @click="pendingFiles.splice(index, 1)">移除</el-button>
            </div>
            <label v-if="canAttach && form.status === 'active'" class="file-picker">
              选择凭证（JPG、PNG、WebP、PDF，单份不超过 10MB，最多 5 份）
              <input type="file" accept=".jpg,.jpeg,.png,.webp,.pdf" multiple @change="chooseFiles" />
            </label>
            <small v-if="!form.id && canAttach">保存支出时会一并上传已选择的凭证。</small>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="支出详情" size="min(560px, 96vw)">
      <template v-if="selected">
        <div class="detail-heading">
          <strong>{{ selected.title }}</strong>
          <span>{{ money(selected.amount) }}</span>
        </div>
        <dl class="detail-grid">
          <dt>所属社团</dt><dd>{{ selected.clubName }}</dd>
          <dt>付款日期</dt><dd>{{ selected.paidOn }}</dd>
          <dt>分类</dt><dd>{{ categoryLabel(selected.category) }}</dd>
          <dt>经手人</dt><dd>{{ selected.handledBy }}</dd>
          <dt>支付方式</dt><dd>{{ paymentLabel(selected.paymentMethod) }}</dd>
          <dt>关联活动</dt><dd>{{ selected.activityTitle || '—' }}</dd>
          <dt>备注</dt><dd>{{ selected.note || '—' }}</dd>
          <dt>状态</dt><dd>{{ selected.status === 'active' ? '有效' : `已作废：${selected.voidReason || ''}` }}</dd>
        </dl>
        <h3>票据凭证</h3>
        <div v-if="selected.attachments?.length" class="detail-attachments">
          <el-button v-for="attachment in selected.attachments" :key="attachment.id" link type="primary" @click="downloadAttachment(selected.id!, attachment)">
            {{ attachment.originalName }}
          </el-button>
        </div>
        <p v-else class="muted">未上传凭证</p>
        <h3>变更记录</h3>
        <el-timeline v-if="historyRows.length">
          <el-timeline-item v-for="item in historyRows" :key="item.id" :timestamp="formatDateTime(item.createdAt)" placement="top">
            <strong>{{ actionLabel(item.action) }}</strong>
            <p>{{ historyDescription(item) }}</p>
            <small>操作人：{{ item.operatorName || `ID ${item.operatorId}` }}</small>
          </el-timeline-item>
        </el-timeline>
        <p v-else class="muted">暂无变更记录</p>
      </template>
    </el-drawer>
  </ViewPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { expenseApi } from '@/api'
import { hasPermission } from '@/utils/permission.ts'

interface Option { id: number; name: string }
interface Activity { id: number; title: string }
interface Attachment { id: number; originalName: string; mimeType: string; fileSize: number }
interface Expense {
  id?: number
  clubId: number
  clubName?: string
  paidOn: string
  title: string
  category: string
  amount: string | number
  handledBy: string
  paymentMethod: string
  activityId?: number | null
  activityTitle?: string
  note: string
  status: string
  voidReason?: string
  version: number
  attachmentCount?: number
  attachments?: Attachment[]
}
interface History { id: number; action: string; beforeJson?: string; afterJson?: string; reason?: string; operatorId: number; operatorName?: string; createdAt: string }
interface Summary {
  count: number
  totalAmount: string | number
  byCategory: { category: string; count: number; amount: string | number }[]
  byMonth: { month: string; count: number; amount: string | number }[]
}

const categories = [
  { value: 'materials', label: '活动物资' },
  { value: 'venue', label: '场地设备' },
  { value: 'printing', label: '宣传印刷' },
  { value: 'gifts', label: '奖品礼品' },
  { value: 'transport', label: '交通' },
  { value: 'catering', label: '餐饮' },
  { value: 'other', label: '其他' },
]
const paymentMethods = [
  { value: 'wechat', label: '微信' },
  { value: 'alipay', label: '支付宝' },
  { value: 'card', label: '银行卡' },
  { value: 'cash', label: '现金' },
  { value: 'other', label: '其他' },
]
const canCreate = computed(() => hasPermission('expense:create'))
const canUpdate = computed(() => hasPermission('expense:update'))
const canVoid = computed(() => hasPermission('expense:void'))
const canExport = computed(() => hasPermission('expense:export'))
const canAttach = computed(() => hasPermission('expense:attachment'))
const clubs = ref<Option[]>([])
const activities = ref<Activity[]>([])
const rows = ref<Expense[]>([])
const summary = ref<Summary>({ count: 0, totalAmount: 0, byCategory: [], byMonth: [] })
const filters = reactive({ clubId: 0, dates: [] as string[], category: '', status: 'active', keyword: '' })
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const editorVisible = ref(false)
const detailVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Expense>(emptyForm())
const attachments = ref<Attachment[]>([])
const pendingFiles = ref<File[]>([])
const selected = ref<Expense | null>(null)
const historyRows = ref<History[]>([])

const rules: FormRules = {
  clubId: [{ required: true, message: '请选择社团', trigger: 'change' }],
  paidOn: [{ required: true, message: '请选择付款日期', trigger: 'change' }],
  title: [{ required: true, whitespace: true, message: '请填写支出事项', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  handledBy: [{ required: true, whitespace: true, message: '请填写经手人', trigger: 'blur' }],
  amount: [{ validator: (_rule, value: string, callback: (error?: Error) => void) => {
    if (!/^(?:0|[1-9]\d{0,9})(?:\.\d{1,2})?$/.test(String(value || '')) || Number(value) <= 0) {
      callback(new Error('请输入大于 0、最多两位小数的金额'))
    } else callback()
  }, trigger: 'blur' }],
}

function emptyForm(): Expense {
  return { clubId: 0, paidOn: new Date().toLocaleDateString('en-CA'), title: '', category: '', amount: '', handledBy: '', paymentMethod: '', activityId: null, note: '', status: 'active', version: 0 }
}

function params() {
  return {
    clubId: filters.clubId || undefined,
    startDate: filters.dates?.[0] || undefined,
    endDate: filters.dates?.[1] || undefined,
    category: filters.category || undefined,
    status: filters.status,
    keyword: filters.keyword.trim() || undefined,
  }
}

async function fetchData() {
  loading.value = true
  try {
    const [result, totals] = await Promise.all([
      expenseApi.list({ ...params(), page: page.value, pageSize: pageSize.value }),
      expenseApi.summary(params()),
    ])
    rows.value = result?.list || []
    total.value = Number(result?.total || 0)
    summary.value = totals || { count: 0, totalAmount: 0, byCategory: [], byMonth: [] }
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  fetchData()
}

async function loadActivities(clubId: number) {
  form.activityId = null
  activities.value = clubId ? await expenseApi.activities(clubId) : []
}

async function openEditor(row?: Expense) {
  Object.assign(form, emptyForm(), { clubId: filters.clubId || clubs.value[0]?.id || 0 })
  form.id = undefined
  attachments.value = []
  pendingFiles.value = []
  if (row?.id) {
    const detail = (await expenseApi.detail(row.id)) as Expense
    Object.assign(form, detail, { amount: String(detail.amount) })
    attachments.value = detail.attachments || []
  }
  if (form.clubId) {
    const savedActivity = form.activityId
    activities.value = await expenseApi.activities(form.clubId)
    form.activityId = savedActivity
  }
  editorVisible.value = true
}

function chooseFiles(event: Event) {
  const input = event.target as HTMLInputElement
  const selectedFiles = Array.from(input.files || [])
  const available = 5 - attachments.value.length - pendingFiles.value.length
  if (selectedFiles.length > available) ElMessage.warning('每笔支出最多上传 5 份凭证')
  for (const file of selectedFiles.slice(0, Math.max(0, available))) {
    if (file.size > 10 * 1024 * 1024 || !/\.(jpe?g|png|webp|pdf)$/i.test(file.name)) {
      ElMessage.warning(`${file.name} 的格式不支持或超过 10MB`)
      continue
    }
    pendingFiles.value.push(file)
  }
  input.value = ''
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      clubId: form.clubId,
      paidOn: form.paidOn,
      title: form.title.trim(),
      category: form.category,
      amount: String(form.amount).trim(),
      handledBy: form.handledBy.trim(),
      paymentMethod: form.paymentMethod || null,
      activityId: form.activityId || null,
      note: form.note?.trim() || null,
      version: form.version,
    }
    const saved = (form.id ? await expenseApi.update(form.id, payload) : await expenseApi.create(payload)) as Expense
    form.id = saved.id
    form.version = saved.version
    const failed: File[] = []
    for (const file of pendingFiles.value) {
      try { await expenseApi.upload(saved.id!, file) } catch { failed.push(file) }
    }
    pendingFiles.value = failed
    if (failed.length) {
      attachments.value = (await expenseApi.detail(saved.id!)).attachments || []
      ElMessage.warning(`支出已保存，${failed.length} 份凭证上传失败，请重试`)
    } else {
      ElMessage.success('支出已保存')
      editorVisible.value = false
    }
    await fetchData()
  } finally {
    saving.value = false
  }
}

async function removeAttachment(attachment: Attachment) {
  if (!form.id) return
  await ElMessageBox.confirm(`移除凭证“${attachment.originalName}”？`, '移除凭证', { type: 'warning' })
  await expenseApi.removeAttachment(form.id, attachment.id)
  attachments.value = (await expenseApi.detail(form.id)).attachments || []
  ElMessage.success('凭证已移除')
  await fetchData()
}

async function downloadAttachment(expenseId: number, attachment: Attachment) {
  const blob = await expenseApi.downloadAttachment(expenseId, attachment.id)
  saveBlob(blob, attachment.originalName)
}

async function openDetail(row: Expense) {
  const [detail, history] = await Promise.all([expenseApi.detail(row.id!), expenseApi.history(row.id!)])
  selected.value = detail
  historyRows.value = history || []
  detailVisible.value = true
}

async function voidRow(row: Expense) {
  const result = await ElMessageBox.prompt('请输入作废原因，作废后该笔金额不再计入汇总。', '作废支出', {
    inputValidator: (value) => Boolean(value?.trim() && value.trim().length <= 500),
    inputErrorMessage: '请填写 500 字以内的作废原因',
    confirmButtonText: '确认作废',
    cancelButtonText: '取消',
    type: 'warning',
  }).catch(() => null)
  if (!result) return
  await expenseApi.void(row.id!, result.value.trim(), row.version)
  ElMessage.success('支出已作废')
  await fetchData()
}

async function exportRows() {
  exporting.value = true
  try {
    const blob = await expenseApi.export(params())
    saveBlob(blob, `社团支出记录-${new Date().toLocaleDateString('en-CA')}.csv`)
    ElMessage.success('导出已开始')
  } finally {
    exporting.value = false
  }
}

function saveBlob(blob: Blob, name: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function categoryLabel(value: string) { return categories.find((item) => item.value === value)?.label || value || '—' }
function paymentLabel(value?: string) { return paymentMethods.find((item) => item.value === value)?.label || '—' }
function money(value: string | number) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function formatDateTime(value?: string) { return value ? value.replace('T', ' ').slice(0, 19) : '—' }
function actionLabel(action: string) {
  return ({ create: '登记支出', update: '修改支出', void: '作废支出', attachment_add: '添加凭证', attachment_remove: '移除凭证' } as Record<string, string>)[action] || action
}
function historyDescription(item: History) {
  if (item.action === 'void') return item.reason || '已作废'
  if (item.action === 'attachment_add' || item.action === 'attachment_remove') return '凭证列表已变更'
  if (item.action === 'create') return '首次登记'
  try {
    const before = JSON.parse(item.beforeJson || '{}')
    const after = JSON.parse(item.afterJson || '{}')
    const labels: Record<string, string> = { paidOn: '付款日期', title: '事项', category: '分类', amount: '金额', handledBy: '经手人', paymentMethod: '支付方式', activityId: '关联活动', note: '备注' }
    return Object.entries(labels).filter(([key]) => String(before[key] ?? '') !== String(after[key] ?? ''))
      .map(([key, label]) => `${label}：${before[key] ?? '—'} → ${after[key] ?? '—'}`).join('；') || '更新记录'
  } catch { return '更新记录' }
}

onMounted(async () => {
  clubs.value = await expenseApi.clubs()
  await fetchData()
})
</script>

<style scoped>
.toolbar-actions { display: flex; gap: 8px; margin-left: auto; }
.expense-filters { display: flex; flex-wrap: wrap; gap: 10px; margin: 18px 0; align-items: center; }
.expense-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-bottom: 20px; }
.summary-card { min-height: 120px; padding: 18px 20px; border: 1px solid var(--el-border-color-light); border-radius: 14px; background: var(--el-bg-color-overlay); display: flex; flex-direction: column; gap: 9px; }
.summary-card > span { color: var(--el-text-color-secondary); font-size: 13px; }
.summary-card--main > strong { font-size: clamp(24px, 3vw, 32px); font-variant-numeric: tabular-nums; }
.summary-card small, .muted { color: var(--el-text-color-secondary); }
.summary-lines { display: grid; gap: 5px; }
.summary-lines p { display: flex; justify-content: space-between; margin: 0; gap: 12px; font-size: 13px; }
.summary-lines strong { font-variant-numeric: tabular-nums; }
.expense-pagination { display: flex; justify-content: flex-end; padding: 18px 0; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.expense-form :deep(.el-select), .expense-form :deep(.el-date-editor) { width: 100%; }
.attachment-editor { width: 100%; display: grid; gap: 8px; }
.attachment-row { display: flex; align-items: center; gap: 8px; }
.attachment-row span { flex: 1; min-width: 0; overflow-wrap: anywhere; }
.file-picker { display: inline-flex; padding: 10px 12px; border: 1px dashed var(--el-border-color); border-radius: 8px; cursor: pointer; color: var(--el-color-primary); font-size: 13px; }
.file-picker input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.attachment-editor small { color: var(--el-text-color-secondary); }
.detail-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 20px; font-size: 20px; }
.detail-heading span { white-space: nowrap; font-variant-numeric: tabular-nums; }
.detail-grid { display: grid; grid-template-columns: 90px minmax(0, 1fr); gap: 12px 16px; margin: 0 0 24px; }
.detail-grid dt { color: var(--el-text-color-secondary); }
.detail-grid dd { margin: 0; overflow-wrap: anywhere; }
.detail-attachments { display: grid; justify-items: start; }
@media (max-width: 760px) {
  .toolbar-actions { margin-left: 0; width: 100%; }
  .expense-summary { grid-template-columns: 1fr; }
  .expense-filters > * { max-width: 100%; }
  .form-grid { grid-template-columns: 1fr; gap: 0; }
  .expense-pagination { justify-content: flex-start; overflow-x: auto; }
}
</style>
