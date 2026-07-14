<template>
  <ViewPage class="admin-page regulation-admin">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-if="clubs.length > 1"
          v-model="selectedClubId"
          filterable
          placeholder="选择社团"
          style="width: 220px"
          @change="reload"
        >
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
        </el-select>
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索制度标题、摘要或正文"
          style="width: 280px"
          :prefix-icon="Search"
          @clear="reload"
          @keyup.enter="reload"
        />
        <el-select
          v-model="query.status"
          clearable
          placeholder="发布状态"
          style="width: 140px"
          @change="reload"
        >
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
        </el-select>
        <el-button :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button v-if="canCreate" type="primary" :icon="Plus" @click="openDialog()">
        新增制度
      </el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="title" label="制度标题" min-width="240">
        <template #default="{ row }">
          <div class="title-cell">
            <strong>{{ row.title }}</strong>
            <p>{{ row.summary || '暂无摘要' }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="clubName" label="所属社团" min-width="160" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'published' ? 'success' : 'info'">
            {{ row.status === 'published' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="发布时间" width="180">
        <template #default="{ row }">
          {{ row.publishedAt ? formatDateTime(row.publishedAt) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="最近更新" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt || row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPreview(row)">预览</el-button>
          <el-button v-if="canUpdate" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button v-if="canUpdate" link type="success" @click="togglePublish(row)">
            {{ row.status === 'published' ? '转为草稿' : '发布' }}
          </el-button>
          <el-popconfirm title="确定删除该制度吗？" @confirm="remove(row)">
            <template #reference>
              <el-button v-if="canDelete" link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="当前社团暂无规章制度" />

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑规章制度' : '新增规章制度'"
      width="min(1040px, 94vw)"
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="所属社团" prop="clubId">
            <el-select
              v-model="form.clubId"
              filterable
              placeholder="请选择社团"
              :disabled="Boolean(form.id)"
            >
              <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布状态">
            <el-select v-model="form.status">
              <el-option label="草稿" value="draft" />
              <el-option label="已发布" value="published" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
          </el-form-item>
        </div>

        <el-form-item label="制度标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="制度摘要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            maxlength="800"
            show-word-limit
            placeholder="用于前台制度目录的简短说明"
          />
        </el-form-item>
        <el-form-item label="制度正文" prop="contentMarkdown">
          <div class="regulation-editor">
            <el-tabs v-model="editorMode">
              <el-tab-pane label="Markdown 编辑" name="write">
                <el-input
                  v-model="form.contentMarkdown"
                  type="textarea"
                  :rows="18"
                  placeholder="# 制度名称&#10;&#10;正文支持 Markdown。Mermaid 图表请使用 ```mermaid 代码块。"
                />
              </el-tab-pane>
              <el-tab-pane label="预览" name="preview">
                <div class="editor-preview">
                  <MarkdownContent
                    v-if="form.contentMarkdown.trim()"
                    :content="form.contentMarkdown"
                  />
                  <el-empty
                    v-else
                    description="输入 Markdown 后可预览正文和 Mermaid 图表"
                    :image-size="72"
                  />
                </div>
              </el-tab-pane>
            </el-tabs>
            <p class="editor-tip">
              Mermaid 示例：在代码块首行写 mermaid，可绘制流程图、时序图和组织结构图。
            </p>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="previewVisible" title="规章制度预览" size="min(860px, 92vw)">
      <div v-if="previewRow" class="regulation-preview">
        <span>{{ previewRow.clubName || clubName(previewRow.clubId) }}</span>
        <h2>{{ previewRow.title }}</h2>
        <p v-if="previewRow.summary" class="preview-summary">{{ previewRow.summary }}</p>
        <el-divider />
        <MarkdownContent :content="previewRow.contentMarkdown" />
      </div>
    </el-drawer>
  </ViewPage>
</template>

<script setup lang="ts">
import MarkdownContent from '@/components/common/MarkdownContent.vue'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { clubApi, regulationApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { hasPermission } from '@/utils/permission.ts'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, onMounted, reactive, ref } from 'vue'

interface RegulationRow {
  id?: number
  clubId: number | string
  clubName?: string
  title: string
  summary: string
  contentMarkdown: string
  status: string
  sortOrder: number
  publishedAt?: string
  createdAt?: string
  updatedAt?: string
}

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const editorMode = ref('write')
const formRef = ref<FormInstance>()
const clubs = ref<any[]>([])
const rows = ref<RegulationRow[]>([])
const selectedClubId = ref<number | string>('')
const previewRow = ref<RegulationRow | null>(null)

const query = reactive({
  keyword: '',
  status: '',
})

const form = reactive<RegulationRow>(emptyForm())

const canCreate = computed(() => hasPermission('regulation:create'))
const canUpdate = computed(() => hasPermission('regulation:update'))
const canDelete = computed(() => hasPermission('regulation:delete'))

const rules: FormRules = {
  clubId: [{ required: true, message: '请选择社团', trigger: 'change' }],
  title: [{ required: true, message: '请输入制度标题', trigger: 'blur' }],
  contentMarkdown: [{ required: true, message: '请输入制度正文', trigger: 'blur' }],
}

function emptyForm(): RegulationRow {
  return {
    clubId: selectedClubId.value || '',
    title: '',
    summary: '',
    contentMarkdown:
      '# 制度名称\n\n## 第一章 总则\n\n请在这里填写制度正文。\n\n```mermaid\nflowchart LR\n  A[制度制定] --> B[内部审议]\n  B --> C[正式发布]\n```\n',
    status: 'draft',
    sortOrder: 0,
  }
}

async function loadClubs() {
  const result = await clubApi.list({ page: 1, pageSize: 100 })
  clubs.value = result?.list || result || []
  if (!selectedClubId.value && clubs.value.length) selectedClubId.value = clubs.value[0].id
}

async function fetchList() {
  if (!selectedClubId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    rows.value =
      (await regulationApi.list({
        clubId: selectedClubId.value,
        status: query.status || undefined,
        keyword: query.keyword.trim() || undefined,
      })) || []
  } finally {
    loading.value = false
  }
}

function reload() {
  fetchList()
}

function resetForm() {
  Object.assign(form, emptyForm())
  editorMode.value = 'write'
}

function openDialog(row?: RegulationRow) {
  resetForm()
  if (row) Object.assign(form, { ...row })
  dialogVisible.value = true
}

function openPreview(row: RegulationRow) {
  previewRow.value = row
  previewVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      summary: form.summary.trim(),
      contentMarkdown: form.contentMarkdown.trim(),
      status: form.status,
      sortOrder: form.sortOrder,
    }
    if (form.id) await regulationApi.update(form.id, payload)
    else await regulationApi.create(form.clubId, payload)
    selectedClubId.value = form.clubId
    ElMessage.success('规章制度已保存')
    dialogVisible.value = false
    await fetchList()
  } finally {
    saving.value = false
  }
}

async function togglePublish(row: RegulationRow) {
  await regulationApi.update(row.id!, {
    title: row.title,
    summary: row.summary || '',
    contentMarkdown: row.contentMarkdown,
    status: row.status === 'published' ? 'draft' : 'published',
    sortOrder: row.sortOrder || 0,
  })
  ElMessage.success(row.status === 'published' ? '已转为草稿' : '制度已发布')
  await fetchList()
}

async function remove(row: RegulationRow) {
  await regulationApi.remove(row.id!)
  ElMessage.success('制度已删除')
  await fetchList()
}

function clubName(clubId: number | string) {
  return clubs.value.find((club) => club.id === clubId)?.name || '未设置社团'
}

onMounted(async () => {
  await loadClubs()
  await fetchList()
})
</script>

<style scoped>
.title-cell {
  display: grid;
  gap: 6px;
}

.title-cell strong {
  color: var(--oa-text);
}

.title-cell p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1.6;
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(160px, 0.7fr) minmax(120px, 0.4fr);
  gap: 16px;
}

.regulation-editor {
  width: 100%;
}

.editor-preview {
  min-height: 430px;
  max-height: 62vh;
  padding: 24px;
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius);
  background: var(--oa-elevated-bg);
  overflow: auto;
}

.editor-tip {
  margin: 8px 0 0;
  color: var(--oa-muted);
  font-size: 12px;
}

.regulation-preview > span {
  color: var(--oa-muted);
  font-size: 13px;
}

.regulation-preview h2 {
  margin: 8px 0 0;
  color: var(--oa-text);
  font-size: 28px;
}

.preview-summary {
  margin: 12px 0 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
