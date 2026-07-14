<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索名称、组织或类型"
          @clear="reload"
          @keyup.enter="reload"
        />
        <el-select v-model="query.status" clearable placeholder="发布状态" @change="reload">
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
          <el-option label="已停用" value="disabled" />
        </el-select>
        <el-select v-model="query.featured" clearable placeholder="首页推荐" @change="reload">
          <el-option label="已推荐" :value="true" />
          <el-option label="未推荐" :value="false" />
        </el-select>
        <el-button :icon="Search" type="primary" @click="reload">筛选</el-button>
        <el-button :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button v-if="canCreate" :icon="Plus" type="primary" @click="openDialog()">
        新增伙伴
      </el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column label="开源伙伴" min-width="260">
        <template #default="{ row }">
          <div class="partner-cell">
            <div class="partner-cell__logo">
              <img v-if="row.logoUrl" :alt="`${row.name} Logo`" :src="row.logoUrl" />
              <span v-else>{{ initial(row.name) }}</span>
            </div>
            <div>
              <strong>{{ row.name }}</strong>
              <p>{{ row.organization || row.category || '未填写所属组织' }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="简介" min-width="260" show-overflow-tooltip />
      <el-table-column label="推荐" width="88">
        <template #default="{ row }">
          <el-tag :type="row.featured ? 'success' : 'info'">
            {{ row.featured ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布状态" width="108">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.websiteUrl" link type="primary" @click="openWebsite(row.websiteUrl)">
            官网
          </el-button>
          <span v-else class="no-website">暂无官网</span>
          <el-button v-if="canUpdate" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button v-if="canUpdateStatus" link type="primary" @click="togglePublish(row)">
            {{ row.status === 'published' ? '停用' : '发布' }}
          </el-button>
          <el-popconfirm v-if="canDelete" title="确定删除该开源伙伴？" @confirm="remove(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > query.pageSize"
      :current-page="query.page"
      :page-size="query.pageSize"
      :total="total"
      background
      class="pager"
      layout="prev, pager, next"
      @current-change="handlePageChange"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑开源伙伴' : '新增开源伙伴'"
      width="720px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="伙伴名称" prop="name">
            <el-input v-model="form.name" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="所属学校或组织">
            <el-input v-model="form.organization" maxlength="160" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="Logo 地址" prop="logoUrl">
            <el-input v-model="form.logoUrl" maxlength="500" placeholder="站内路径或 HTTPS 地址" />
          </el-form-item>
          <el-form-item label="官网地址（可选）" prop="websiteUrl">
            <el-input
              v-model="form.websiteUrl"
              maxlength="500"
              placeholder="可留空，或填写 https://example.org"
            />
          </el-form-item>
        </div>
        <el-form-item label="伙伴简介" prop="description">
          <el-input
            v-model="form.description"
            maxlength="300"
            :rows="4"
            show-word-limit
            type="textarea"
          />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="伙伴类型">
            <el-input v-model="form.category" maxlength="80" placeholder="例如：校园技术社团" />
          </el-form-item>
          <el-form-item label="技术方向标签">
            <el-select
              v-model="form.tags"
              allow-create
              filterable
              multiple
              placeholder="输入后回车添加"
            />
          </el-form-item>
        </div>
        <div class="form-grid form-grid--three">
          <el-form-item label="发布状态">
            <el-select v-model="form.status">
              <el-option label="草稿" value="draft" />
              <el-option label="已发布" value="published" />
              <el-option label="已停用" value="disabled" />
            </el-select>
          </el-form-item>
          <el-form-item label="首页推荐">
            <el-switch v-model="form.featured" active-text="推荐" inactive-text="不推荐" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { partnerClubApi } from '@/api'
import { hasPermission } from '@/utils/permission.ts'

interface PartnerClubRow {
  id?: number
  name: string
  logoUrl: string
  description: string
  websiteUrl: string
  organization?: string
  category?: string
  tags: string[]
  sortOrder: number
  featured: boolean
  status: string
}

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const rows = ref<PartnerClubRow[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()
const canCreate = computed(() => hasPermission('partner-club:create'))
const canUpdate = computed(() => hasPermission('partner-club:update'))
const canUpdateStatus = computed(() => hasPermission('partner-club:status:update'))
const canDelete = computed(() => hasPermission('partner-club:delete'))

const query = reactive({
  keyword: '',
  status: '',
  featured: null as boolean | null,
  page: 1,
  pageSize: 10,
})

const form = reactive<PartnerClubRow>({
  name: '',
  logoUrl: '',
  description: '',
  websiteUrl: '',
  organization: '',
  category: '',
  tags: [],
  sortOrder: 0,
  featured: false,
  status: 'draft',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入伙伴名称', trigger: 'blur' }],
  logoUrl: [{ required: true, message: '请输入 Logo 地址', trigger: 'blur' }],
  description: [{ required: true, message: '请输入伙伴简介', trigger: 'blur' }],
  websiteUrl: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value?.trim() || /^https?:\/\//i.test(value.trim())) callback()
        else callback(new Error('官网地址必须以 http:// 或 https:// 开头'))
      },
      trigger: 'blur',
    },
  ],
}

const listParams = computed(() => ({
  keyword: query.keyword || undefined,
  status: query.status || undefined,
  featured: query.featured === null ? undefined : query.featured,
  page: query.page,
  pageSize: query.pageSize,
}))

async function fetchList() {
  loading.value = true
  try {
    const data = await partnerClubApi.list(listParams.value)
    rows.value = (data?.list || []).map((row: PartnerClubRow & { tags?: string | string[] }) => ({
      ...row,
      websiteUrl: row.websiteUrl || '',
      tags: parseTags(row.tags),
    }))
    total.value = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  fetchList()
}

function handlePageChange(page: number) {
  query.page = page
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    logoUrl: '',
    description: '',
    websiteUrl: '',
    organization: '',
    category: '',
    tags: [],
    sortOrder: 0,
    featured: false,
    status: 'draft',
  })
}

function openDialog(row?: PartnerClubRow) {
  resetForm()
  if (row) Object.assign(form, { ...row, tags: [...row.tags] })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form, tags: [...form.tags] }
    if (payload.id) await partnerClubApi.update(payload.id, payload)
    else await partnerClubApi.create(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await fetchList()
  } finally {
    saving.value = false
  }
}

async function togglePublish(row: PartnerClubRow) {
  if (!row.id) return
  const status = row.status === 'published' ? 'disabled' : 'published'
  await partnerClubApi.updateStatus(row.id, status)
  ElMessage.success(status === 'published' ? '伙伴已发布' : '伙伴已停用')
  await fetchList()
}

async function remove(row: PartnerClubRow) {
  if (!row.id) return
  await partnerClubApi.remove(row.id)
  ElMessage.success('伙伴已删除')
  await fetchList()
}

function openWebsite(url?: string) {
  if (!url) return
  if (/^https?:\/\//i.test(url)) window.open(url, '_blank', 'noopener,noreferrer')
}

function parseTags(tags?: string | string[]) {
  if (Array.isArray(tags)) return tags
  if (!tags) return []
  try {
    const parsed = JSON.parse(tags)
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return []
  }
}

function statusLabel(status: string) {
  if (status === 'published') return '已发布'
  if (status === 'disabled') return '已停用'
  return '草稿'
}

function statusType(status: string) {
  if (status === 'published') return 'success'
  if (status === 'disabled') return 'info'
  return 'warning'
}

function initial(name: string) {
  return String(name || '伙')
    .slice(0, 1)
    .toUpperCase()
}

onMounted(fetchList)
</script>

<style scoped>
.toolbar__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.toolbar__filters .el-input {
  width: 240px;
}

.toolbar__filters .el-select {
  width: 140px;
}

.partner-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.partner-cell__logo {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  place-items: center;
  overflow: hidden;
  padding: 5px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: #fafafa;
  color: #1d1d1f;
  font-weight: 600;
}

.partner-cell__logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.partner-cell strong {
  display: block;
  color: var(--oa-text);
}

.partner-cell p {
  margin: 3px 0 0;
  color: var(--oa-muted);
  font-size: 12px;
}

.no-website {
  display: inline-block;
  padding: 0 8px;
  color: var(--oa-muted);
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.form-grid--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.pager {
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 760px) {
  .form-grid,
  .form-grid--three {
    grid-template-columns: 1fr;
  }

  .toolbar__filters .el-input,
  .toolbar__filters .el-select {
    width: 100%;
  }
}
</style>
