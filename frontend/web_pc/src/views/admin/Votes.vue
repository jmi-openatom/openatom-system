<template>
  <ViewPage class="admin-page vote-admin">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-if="clubs.length > 1"
          v-model="selectedClubId"
          filterable
          placeholder="选择社团"
          style="width: 220px"
          @change="fetchList"
        >
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
        </el-select>
        <el-select
          v-model="query.status"
          clearable
          placeholder="投票状态"
          style="width: 150px"
          @change="fetchList"
        >
          <el-option label="草稿" value="draft" />
          <el-option label="进行中" value="open" />
          <el-option label="已结束" value="closed" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增投票</el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="title" label="投票名称" min-width="220" />
      <el-table-column prop="clubName" label="所属社团" min-width="160">
        <template #default="{ row }">{{ row.clubName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ voteTypeText(row) }}</template>
      </el-table-column>
      <el-table-column label="投票/选项" width="130">
        <template #default="{ row }"
          >{{ row.voterCount || 0 }} / {{ row.optionCount || 0 }}</template
        >
      </el-table-column>
      <el-table-column label="投票时间" min-width="230">
        <template #default="{ row }">{{ formatRange(row.startAt, row.endAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="390" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">结果</el-button>
          <el-button link type="info" @click="openDialog(row)">编辑</el-button>
          <el-button link type="warning" @click="copyVoteLink(row)">复制链接</el-button>
          <el-button v-if="row.status !== 'open'" link type="success" @click="publish(row)"
            >发布</el-button
          >
          <el-button v-if="row.status === 'open'" link type="danger" @click="close(row)"
            >结束</el-button
          >
          <el-button link type="danger" @click="resetRecords(row)">清空</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="当前社团暂无投票活动" />

    <el-dialog
      v-model="dialogVisible"
      :title="draft.id ? '编辑投票活动' : '新增投票活动'"
      width="960px"
      @closed="resetDraft"
    >
      <el-form ref="formRef" :model="draft" :rules="rules" label-width="108px">
        <div class="form-grid">
          <el-form-item label="所属社团" prop="clubId">
            <el-select v-model="draft.clubId" filterable placeholder="请选择社团">
              <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="投票状态">
            <el-select v-model="draft.status">
              <el-option label="草稿" value="draft" />
              <el-option label="进行中" value="open" />
              <el-option label="已结束" value="closed" />
            </el-select>
          </el-form-item>
          <el-form-item label="投票名称" prop="title">
            <el-input v-model="draft.title" placeholder="如：社团活动主题投票" />
          </el-form-item>
          <el-form-item label="投票类型">
            <el-segmented
              v-model="draft.voteType"
              :options="[
                { label: '单选', value: 'single' },
                { label: '多选', value: 'multiple' },
              ]"
            />
          </el-form-item>
          <el-form-item v-if="draft.voteType === 'multiple'" label="最多可选">
            <el-input-number
              v-model="draft.maxChoices"
              :min="2"
              :step="1"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="匿名投票">
            <el-switch v-model="draft.anonymousAllowed" active-text="允许" inactive-text="需登录" />
          </el-form-item>
          <el-form-item label="结果可见">
            <div class="visibility-setting">
              <el-segmented
                v-model="draft.resultVisibility"
                :options="[
                  { label: '公开', value: 'public' },
                  { label: '投后可见', value: 'after_vote' },
                  { label: '不公开', value: 'private' },
                ]"
              />
              <span>{{ resultVisibilityDescription(draft.resultVisibility) }}</span>
            </div>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-input v-model="draft.startAt" type="datetime-local" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-input v-model="draft.endAt" type="datetime-local" />
          </el-form-item>
        </div>
        <el-form-item label="投票说明">
          <el-input
            v-model="draft.description"
            type="textarea"
            :rows="3"
            placeholder="可填写投票背景、规则或现场说明"
          />
        </el-form-item>

        <el-divider content-position="left">投票选项</el-divider>
        <div class="option-toolbar">
          <el-button size="small" type="primary" plain @click="addOption">新增选项</el-button>
          <span>至少保留两个选项，已有投票记录的选项不能删除。</span>
        </div>
        <el-table :data="draft.options" class="admin-table option-edit-table" row-key="rowKey">
          <el-table-column label="选项标题" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.title" placeholder="请输入选项标题" />
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="240">
            <template #default="{ row }">
              <el-input v-model="row.description" placeholder="可选说明" />
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
              <el-button link type="danger" @click="removeOption($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="920px" title="投票结果">
      <div v-if="detail" class="vote-detail">
        <div class="detail-header">
          <div>
            <h2>{{ detail.vote.title }}</h2>
            <p>{{ detail.vote.clubName || '未设置社团' }}</p>
          </div>
          <div class="detail-actions">
            <el-button :icon="Refresh" @click="refreshDetail">刷新</el-button>
            <el-button type="warning" @click="copyVoteLink(detail.vote)">复制链接</el-button>
          </div>
        </div>

        <div class="detail-stats">
          <div>
            <span>投票人数</span>
            <strong>{{ detail.vote.voterCount || 0 }}</strong>
          </div>
          <div>
            <span>累计选择</span>
            <strong>{{ detail.vote.totalVoteCount || 0 }}</strong>
          </div>
          <div>
            <span>选项数量</span>
            <strong>{{ detail.vote.optionCount || 0 }}</strong>
          </div>
          <div>
            <span>状态</span>
            <strong>{{ statusText(detail.vote.status) }}</strong>
          </div>
        </div>

        <div class="option-result-list">
          <div v-for="option in detail.options" :key="option.id" class="option-result">
            <span class="option-swatch" :style="{ backgroundColor: option.color || '#1d1d1f' }" />
            <div class="option-result__main">
              <div class="option-result__title">
                <strong>{{ option.title }}</strong>
                <span>{{ option.voteCount || 0 }} 票 · {{ option.votePercent || 0 }}%</span>
              </div>
              <el-progress :percentage="option.votePercent || 0" :show-text="false" />
              <p v-if="option.description">{{ option.description }}</p>
            </div>
          </div>
        </div>

        <el-table :data="detail.records" class="admin-table">
          <el-table-column prop="voterName" label="投票人" min-width="140" />
          <el-table-column prop="voterContact" label="联系方式" min-width="170">
            <template #default="{ row }">{{ row.voterContact || '-' }}</template>
          </el-table-column>
          <el-table-column label="选择" min-width="220">
            <template #default="{ row }">{{
              (row.optionTitles || []).join(' / ') || '-'
            }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160">
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
          <el-table-column prop="votedAt" label="投票时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.votedAt) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!detail.records.length" description="还没有投票记录" />
      </div>
    </el-drawer>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { clubApi, voteApi } from '@/api'
import { formatDateTime, statusType, toDateTimeInputValue } from '@/utils/format.ts'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { onMounted, ref } from 'vue'

type OptionDraft = {
  id?: number
  rowKey: string
  title: string
  description: string
  sortOrder: number
  color: string
}

type VoteDraft = {
  id?: number
  clubId: number | string
  title: string
  description: string
  status: string
  voteType: string
  maxChoices: number
  anonymousAllowed: boolean
  resultVisibility: string
  startAt: string
  endAt: string
  options: OptionDraft[]
}

const optionColors = ['#1d1d1f', '#4a4a4e', '#6e6e73', '#8e8e93', '#a1a1a6', '#c7c7cc']

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const clubs = ref<any[]>([])
const rows = ref<any[]>([])
const selectedClubId = ref<number | string>('')
const query = ref({ status: '' })
const detail = ref<any>(null)
const currentDetailId = ref<number | string>('')
const formRef = ref<any>()

let rowSeed = 0

const draft = ref<VoteDraft>(createEmptyDraft())

const rules = {
  clubId: [{ required: true, message: '请选择社团', trigger: 'change' }],
  title: [{ required: true, message: '请输入投票名称', trigger: 'blur' }],
}

function createEmptyDraft(): VoteDraft {
  return {
    clubId: selectedClubId.value || '',
    title: '',
    description: '',
    status: 'draft',
    voteType: 'single',
    maxChoices: 2,
    anonymousAllowed: true,
    resultVisibility: 'public',
    startAt: '',
    endAt: '',
    options: [createOption({ title: '选项 A' }), createOption({ title: '选项 B' })],
  }
}

function createOption(source: Record<string, any> = {}): OptionDraft {
  rowSeed += 1
  return {
    id: source.id,
    rowKey: String(source.id || `new-${rowSeed}`),
    title: source.title || '',
    description: source.description || '',
    sortOrder: Number(source.sortOrder ?? rowSeed * 10),
    color: source.color || optionColors[(rowSeed - 1) % optionColors.length],
  }
}

async function loadClubs() {
  const result = await clubApi.list({ page: 1, pageSize: 100 })
  clubs.value = result?.list || result || []
  if (!selectedClubId.value && clubs.value.length) selectedClubId.value = clubs.value[0].id
  await fetchList()
}

async function fetchList() {
  if (!selectedClubId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    rows.value = await voteApi.list({
      clubId: selectedClubId.value,
      status: query.value.status || undefined,
    })
  } finally {
    loading.value = false
  }
}

async function openDialog(row?: Record<string, any>) {
  if (!row?.id) {
    draft.value = createEmptyDraft()
    dialogVisible.value = true
    return
  }
  const result = await voteApi.detail(row.id)
  const vote = result.vote || row
  draft.value = {
    id: vote.id,
    clubId: vote.clubId,
    title: vote.title || '',
    description: vote.description || '',
    status: vote.status || 'draft',
    voteType: vote.voteType || 'single',
    maxChoices: Number(vote.maxChoices || 2),
    anonymousAllowed: vote.anonymousAllowed !== false,
    resultVisibility:
      vote.resultVisibility || (vote.resultVisible === false ? 'after_vote' : 'public'),
    startAt: toDateTimeInputValue(vote.startAt),
    endAt: toDateTimeInputValue(vote.endAt),
    options: (result.options || []).map((option) => createOption(option)),
  }
  if (draft.value.options.length < 2) {
    draft.value.options = [createOption({ title: '选项 A' }), createOption({ title: '选项 B' })]
  }
  dialogVisible.value = true
}

function addOption() {
  draft.value.options.push(createOption({ title: `选项 ${draft.value.options.length + 1}` }))
}

function removeOption(index: number) {
  if (draft.value.options.length <= 2) {
    ElMessage.warning('至少保留两个选项')
    return
  }
  draft.value.options.splice(index, 1)
}

function save() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    const options = draft.value.options.map((option, index) => ({
      id: option.id,
      title: option.title,
      description: option.description,
      sortOrder: option.sortOrder || (index + 1) * 10,
      color: option.color,
    }))
    if (options.some((option) => !option.title?.trim())) {
      ElMessage.warning('请填写完整的选项标题')
      return
    }
    saving.value = true
    try {
      const payload = {
        title: draft.value.title,
        description: draft.value.description,
        status: draft.value.status,
        voteType: draft.value.voteType,
        maxChoices: draft.value.voteType === 'multiple' ? draft.value.maxChoices : 1,
        anonymousAllowed: draft.value.anonymousAllowed,
        resultVisibility: draft.value.resultVisibility,
        startAt: draft.value.startAt,
        endAt: draft.value.endAt,
        options,
      }
      if (draft.value.id) {
        await voteApi.update(draft.value.id, payload)
        ElMessage.success('投票活动已更新')
      } else {
        await voteApi.create(draft.value.clubId, payload)
        ElMessage.success('投票活动已创建')
      }
      dialogVisible.value = false
      await fetchList()
    } finally {
      saving.value = false
    }
  })
}

async function openDetail(row: Record<string, any>) {
  currentDetailId.value = row.id
  detailVisible.value = true
  await refreshDetail()
}

async function refreshDetail() {
  if (!currentDetailId.value) return
  detail.value = await voteApi.detail(currentDetailId.value)
}

async function publish(row: Record<string, any>) {
  await voteApi.publish(row.id)
  ElMessage.success('投票活动已发布')
  await fetchList()
  if (currentDetailId.value === row.id) await refreshDetail()
}

async function close(row: Record<string, any>) {
  await voteApi.close(row.id)
  ElMessage.success('投票活动已结束')
  await fetchList()
  if (currentDetailId.value === row.id) await refreshDetail()
}

async function resetRecords(row: Record<string, any>) {
  await ElMessageBox.confirm(`确认清空“${row.title}”的所有投票记录？`, '清空投票记录', {
    type: 'warning',
  })
  await voteApi.reset(row.id)
  ElMessage.success('投票记录已清空')
  await fetchList()
  if (currentDetailId.value === row.id) await refreshDetail()
}

async function copyVoteLink(row: Record<string, any>) {
  const url = `${window.location.origin}/votes/${row.id}`
  await navigator.clipboard.writeText(url)
  ElMessage.success('投票链接已复制')
}

function resetDraft() {
  draft.value = createEmptyDraft()
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

function voteTypeText(row: Record<string, any>) {
  return row.voteType === 'multiple' ? `多选 · ${row.maxChoices || 2}` : '单选'
}

function resultVisibilityDescription(value: string) {
  return (
    {
      public: '所有人都可以随时查看票数和占比。',
      after_vote: '参与者投票后可查看，投票结束后所有人可查看。',
      private: '前台始终不展示票数、占比和参与人数，仅后台可查看。',
    }[value] || ''
  )
}

function formatRange(startAt: any, endAt: any) {
  return `${formatDateTime(startAt)} 至 ${formatDateTime(endAt)}`
}

onMounted(() => {
  loadClubs()
})
</script>

<style scoped>
.vote-admin :deep(.el-segmented) {
  --el-segmented-item-selected-bg-color: var(--oa-active-bg);
  --el-segmented-item-selected-color: var(--oa-active-text);
}

.option-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--oa-muted);
  font-size: 13px;
}

.option-edit-table :deep(.el-input-number) {
  width: 100%;
}

.visibility-setting {
  display: grid;
  gap: 8px;
}

.visibility-setting > span {
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.5;
}

.vote-detail {
  display: grid;
  gap: 20px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
}

.detail-header p {
  margin: 0;
  color: var(--oa-muted);
}

.detail-actions {
  display: flex;
  gap: 10px;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.detail-stats > div {
  padding: 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
}

.detail-stats span {
  display: block;
  color: var(--oa-muted);
  font-size: 13px;
}

.detail-stats strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
}

.option-result-list {
  display: grid;
  gap: 12px;
}

.option-result {
  display: grid;
  grid-template-columns: 18px 1fr;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-panel);
}

.option-swatch {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  margin-top: 3px;
}

.option-result__main {
  min-width: 0;
}

.option-result__title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.option-result__title span,
.option-result p {
  color: var(--oa-muted);
}

.option-result p {
  margin: 8px 0 0;
  font-size: 13px;
}

@media (max-width: 760px) {
  .detail-header {
    display: grid;
  }

  .detail-actions,
  .option-result__title {
    flex-wrap: wrap;
  }

  .detail-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
