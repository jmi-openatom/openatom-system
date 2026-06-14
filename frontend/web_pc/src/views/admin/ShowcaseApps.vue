<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索应用、开发者、版本"
          @clear="reload"
          @keyup.enter="reload"
        />
        <el-select v-model="query.status" clearable placeholder="发布状态" @change="reload">
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
          <el-option label="已隐藏" value="hidden" />
        </el-select>
        <el-select v-model="query.openSource" clearable placeholder="开源状态" @change="reload">
          <el-option label="已开源" :value="true" />
          <el-option label="未开源" :value="false" />
        </el-select>
        <el-button :icon="Search" type="primary" @click="reload">筛选</el-button>
        <el-button :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button :icon="Plus" type="primary" @click="openDialog()">新增应用</el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column label="应用" min-width="240">
        <template #default="{ row }">
          <div class="app-cell">
            <div class="app-cell__cover">
              <img v-if="row.coverUrl" :alt="row.name" :src="row.coverUrl" />
              <span v-else>{{ appInitial(row.name) }}</span>
            </div>
            <div>
              <strong>{{ row.name }}</strong>
              <p>{{ row.summary || '暂无简介' }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="开源" width="96">
        <template #default="{ row }">
          <el-tag :type="row.openSource ? 'success' : 'info'">
            {{ row.openSource ? '已开源' : '未开源' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="developer" label="开发者" min-width="130" show-overflow-tooltip />
      <el-table-column prop="owner" label="所有者" min-width="130" show-overflow-tooltip />
      <el-table-column prop="licenseName" label="协议" width="120" show-overflow-tooltip />
      <el-table-column prop="version" label="版本" width="110" show-overflow-tooltip />
      <el-table-column label="应用状态" width="116">
        <template #default="{ row }">
          <el-tag v-if="row.appStatus" effect="plain">{{ row.appStatus }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="发布状态" width="108">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPreview(row)">预览</el-button>
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="primary" @click="togglePublish(row)">
            {{ row.status === 'published' ? '下架' : '发布' }}
          </el-button>
          <el-popconfirm title="确定删除该应用？" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
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
      :title="form.id ? '编辑应用' : '新增应用'"
      width="760px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="应用名称" prop="name">
            <el-input v-model="form.name" maxlength="160" />
          </el-form-item>
          <el-form-item label="版本">
            <el-input v-model="form.version" maxlength="80" placeholder="例如 v1.0.0" />
          </el-form-item>
        </div>
        <el-form-item label="应用状态">
          <el-input
            v-model="form.appStatus"
            maxlength="80"
            placeholder="例如：开发中、内测中、已上线、维护中"
          />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.summary" type="textarea" :rows="4" maxlength="800" show-word-limit />
        </el-form-item>
        <el-form-item label="封面/截图 URL">
          <el-input v-model="form.coverUrl" maxlength="500" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="开发者">
            <el-input v-model="form.developer" maxlength="160" />
          </el-form-item>
          <el-form-item label="所有者">
            <el-input v-model="form.owner" maxlength="160" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="开源状态">
            <el-switch v-model="form.openSource" active-text="已开源" inactive-text="未开源" />
          </el-form-item>
          <el-form-item label="开源协议">
            <el-input v-model="form.licenseName" maxlength="120" placeholder="例如 MIT / Apache-2.0" />
          </el-form-item>
        </div>
        <div v-if="form.openSource" class="form-grid">
          <el-form-item label="GitHub 地址">
            <el-input v-model="form.githubUrl" maxlength="500" />
          </el-form-item>
          <el-form-item label="Gitee 地址">
            <el-input v-model="form.giteeUrl" maxlength="500" />
          </el-form-item>
        </div>
        <el-form-item label="下载链接">
          <el-input v-model="form.downloadUrl" maxlength="500" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="发布状态">
            <el-select v-model="form.status">
              <el-option label="草稿" value="draft" />
              <el-option label="已发布" value="published" />
              <el-option label="已隐藏" value="hidden" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="应用预览" width="620px">
      <div v-if="previewApp" class="preview-panel">
        <div class="preview-panel__cover">
          <img v-if="previewApp.coverUrl" :alt="previewApp.name" :src="previewApp.coverUrl" />
          <span v-else>{{ appInitial(previewApp.name) }}</span>
        </div>
        <h2>{{ previewApp.name }}</h2>
        <p>{{ previewApp.summary || '暂无简介' }}</p>
        <dl>
          <div>
            <dt>开源状态</dt>
            <dd>{{ previewApp.openSource ? '已开源' : '未开源' }}</dd>
          </div>
          <div>
            <dt>开发者</dt>
            <dd>{{ previewApp.developer || '-' }}</dd>
          </div>
          <div>
            <dt>所有者</dt>
            <dd>{{ previewApp.owner || '-' }}</dd>
          </div>
          <div>
            <dt>协议</dt>
            <dd>{{ previewApp.licenseName || '-' }}</dd>
          </div>
          <div>
            <dt>版本</dt>
            <dd>{{ previewApp.version || '-' }}</dd>
          </div>
          <div>
            <dt>应用状态</dt>
            <dd>{{ previewApp.appStatus || '-' }}</dd>
          </div>
        </dl>
      </div>
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
import { showcaseAppApi } from '@/api'

interface ShowcaseAppRow {
  id?: number
  name: string
  summary?: string
  coverUrl?: string
  openSource: boolean
  githubUrl?: string
  giteeUrl?: string
  developer?: string
  owner?: string
  licenseName?: string
  version?: string
  appStatus?: string
  downloadUrl?: string
  status: string
  sortOrder: number
}

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const previewApp = ref<ShowcaseAppRow | null>(null)
const rows = ref<ShowcaseAppRow[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()

const query = reactive({
  keyword: '',
  status: '',
  openSource: null as boolean | null,
  page: 1,
  pageSize: 10,
})

const form = reactive<ShowcaseAppRow>({
  name: '',
  summary: '',
  coverUrl: '',
  openSource: true,
  githubUrl: '',
  giteeUrl: '',
  developer: '',
  owner: '',
  licenseName: '',
  version: '',
  appStatus: '',
  downloadUrl: '',
  status: 'draft',
  sortOrder: 0,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
}

const listParams = computed(() => ({
  keyword: query.keyword || undefined,
  status: query.status || undefined,
  openSource: query.openSource === null ? undefined : query.openSource,
  page: query.page,
  pageSize: query.pageSize,
}))

async function fetchList() {
  loading.value = true
  try {
    const data = await showcaseAppApi.list(listParams.value)
    rows.value = data?.list || []
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
    summary: '',
    coverUrl: '',
    openSource: true,
    githubUrl: '',
    giteeUrl: '',
    developer: '',
    owner: '',
    licenseName: '',
    version: '',
    appStatus: '',
    downloadUrl: '',
    status: 'draft',
    sortOrder: 0,
  })
}

function openDialog(row?: ShowcaseAppRow) {
  resetForm()
  if (row) Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (!payload.openSource) {
      payload.githubUrl = ''
      payload.giteeUrl = ''
    }
    if (payload.id) await showcaseAppApi.update(payload.id, payload)
    else await showcaseAppApi.create(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await fetchList()
  } finally {
    saving.value = false
  }
}

async function togglePublish(row: ShowcaseAppRow) {
  if (!row.id) return
  const status = row.status === 'published' ? 'hidden' : 'published'
  await showcaseAppApi.updateStatus(row.id, status)
  ElMessage.success(status === 'published' ? '应用已发布' : '应用已下架')
  await fetchList()
}

async function remove(row: ShowcaseAppRow) {
  if (!row.id) return
  await showcaseAppApi.remove(row.id)
  ElMessage.success('应用已删除')
  await fetchList()
}

function openPreview(row: ShowcaseAppRow) {
  previewApp.value = row
  previewVisible.value = true
}

function statusLabel(status: string) {
  if (status === 'published') return '已发布'
  if (status === 'hidden') return '已隐藏'
  return '草稿'
}

function statusType(status: string) {
  if (status === 'published') return 'success'
  if (status === 'hidden') return 'info'
  return 'warning'
}

function appInitial(name: string) {
  return String(name || 'A').slice(0, 1).toUpperCase()
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

.app-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.app-cell__cover {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  border-radius: 8px;
  font-weight: 600;
}

.app-cell__cover img,
.preview-panel__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-cell strong {
  display: block;
  color: var(--oa-text);
  font-weight: 600;
}

.app-cell p {
  max-width: 420px;
  margin: 4px 0 0;
  overflow: hidden;
  color: var(--oa-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.pager {
  justify-content: flex-end;
  margin-top: 18px;
}

.preview-panel {
  display: grid;
  gap: 14px;
}

.preview-panel__cover {
  display: grid;
  height: 220px;
  place-items: center;
  overflow: hidden;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  border-radius: 8px;
  font-size: 42px;
  font-weight: 700;
}

.preview-panel h2 {
  margin: 0;
  font-size: 24px;
}

.preview-panel p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.preview-panel dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.preview-panel dl div {
  padding: 12px;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.preview-panel dt {
  color: var(--oa-muted);
  font-size: 12px;
}

.preview-panel dd {
  margin: 6px 0 0;
  color: var(--oa-text);
  font-weight: 600;
}

@media (max-width: 760px) {
  .form-grid,
  .preview-panel dl {
    grid-template-columns: 1fr;
  }

  .toolbar__filters .el-input,
  .toolbar__filters .el-select {
    width: 100%;
  }
}
</style>
