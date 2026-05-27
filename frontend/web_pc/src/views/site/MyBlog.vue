<template>
  <ViewPage class="my-blog-page" :loading="loading">
    <section class="my-blog-shell home-interactive-section">
      <div class="container">
        <div class="section-heading reveal-block my-blog-heading">
          <span>Member Studio</span>
          <h1>我的博客</h1>
          <p>把项目踩坑、竞赛复盘和技术笔记沉淀下来，正式成员提交审核后展示到社团博客。</p>
        </div>

        <ViewToolbar>
          <div class="toolbar__filters">
            <el-select v-model="query.status" clearable placeholder="文章状态" @change="reload">
              <el-option label="草稿" value="draft" />
              <el-option label="待审核" value="pending" />
              <el-option label="已发布" value="published" />
              <el-option label="已隐藏" value="hidden" />
              <el-option label="已驳回" value="rejected" />
            </el-select>
            <el-button :icon="Refresh" @click="fetchList">刷新</el-button>
          </div>
          <el-button type="primary" :icon="Plus" @click="openDialog()">写文章</el-button>
        </ViewToolbar>

        <el-alert
          v-if="!canWrite"
          class="write-alert"
          type="warning"
          show-icon
          :closable="false"
          title="仅正式成员可以发布文章；如果你已经转正，请重新登录刷新权限信息。"
        />

        <el-table :data="rows" class="admin-table">
          <el-table-column prop="title" label="文章" min-width="240">
            <template #default="{ row }">
              <strong>{{ row.title }}</strong>
              <p class="muted-line">{{ row.summary || '暂无摘要' }}</p>
              <p v-if="row.rejectReason" class="danger-line">原因：{{ row.rejectReason }}</p>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="分类" width="130" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="数据" width="190">
            <template #default="{ row }">
              {{ row.viewCount || 0 }} 阅读 · {{ row.likeCount || 0 }} 赞 · {{ row.favoriteCount || 0 }} 藏 ·
              {{ row.commentCount || 0 }} 评论
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt || row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
              <el-button
                v-if="row.status !== 'pending' && row.status !== 'published' && row.status !== 'hidden'"
                link
                type="success"
                @click="publish(row)"
              >
                提交审核
              </el-button>
              <el-button
                v-if="row.status === 'published'"
                link
                type="success"
                @click="$router.push(`/blog/${row.id}`)"
              >
                查看
              </el-button>
              <el-popconfirm title="确定删除这篇文章？" @confirm="remove(row)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="total > query.pageSize"
          class="pager"
          background
          layout="prev, pager, next"
          :current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑文章' : '写文章'" width="960px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <div class="form-grid">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" maxlength="180" show-word-limit />
          </el-form-item>
          <el-form-item label="分类">
            <el-input v-model="form.category" maxlength="80" placeholder="例如：Java / 前端 / 竞赛" />
          </el-form-item>
          <el-form-item label="封面URL">
            <el-input v-model="form.coverUrl" />
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="tagText" placeholder="多个标签用逗号分隔" />
          </el-form-item>
        </div>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="contentMarkdown">
          <div class="editor-field">
            <div class="markdown-toolbar">
              <el-button-group>
                <el-button :icon="EditPen" @click="insertHeading">标题</el-button>
                <el-button @click="wrapSelection('**', '**', '加粗文字')">B</el-button>
                <el-button @click="wrapSelection('*', '*', '斜体文字')">I</el-button>
                <el-button :icon="Memo" @click="insertBlockquote">引用</el-button>
                <el-button :icon="List" @click="insertList">列表</el-button>
                <el-button :icon="Document" @click="insertCodeBlock">代码</el-button>
                <el-button :icon="Grid" @click="insertTable">表格</el-button>
                <el-button :icon="Link" @click="insertLink">链接</el-button>
                <el-button :icon="Picture" @click="openImageDialog">图片</el-button>
              </el-button-group>
            </div>
            <el-tabs v-model="editorMode" class="markdown-editor-tabs">
              <el-tab-pane label="编辑" name="edit">
                <el-input ref="contentInputRef" v-model="form.contentMarkdown" type="textarea" :rows="18" />
              </el-tab-pane>
              <el-tab-pane label="预览" name="preview">
                <article class="markdown-body editor-preview" v-html="previewHtml"></article>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" @click="save('draft')">保存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="save('pending')">提交审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="imageDialogVisible" title="上传图床图片" width="560px">
      <el-upload
        accept="image/jpeg,image/png,image/gif,image/webp"
        :auto-upload="false"
        drag
        :file-list="imageUploadFiles"
        :limit="1"
        :on-change="handleImageChange"
        :on-remove="handleImageRemove"
      >
        <el-icon class="el-icon--upload">
          <UploadFilled />
        </el-icon>
        <div class="el-upload__text">拖拽图片到这里，或点击选择图片</div>
        <template #tip>
          <div class="el-upload__tip">支持 JPG、PNG、GIF、WebP，单张最大 10MB。</div>
        </template>
      </el-upload>

      <div v-if="imageUploadResult.url" class="image-upload-result">
        <el-image :src="imageUploadResult.url" fit="cover" />
        <el-input :model-value="imageUploadResult.url" readonly />
        <el-input :model-value="imageUploadResult.markdown" readonly />
      </div>

      <template #footer>
        <el-button @click="imageDialogVisible = false">取消</el-button>
        <el-button :loading="imageUploading" @click="uploadImage">上传</el-button>
        <el-button
          type="primary"
          :disabled="!imageUploadResult.url"
          @click="insertUploadedImage"
        >
          插入正文
        </el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { blogApi, imageHostingApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'
import { hasRole } from '@/utils/permission.ts'
import { renderMarkdown } from '@/utils/markdown.ts'
import { ElMessage } from 'element-plus'
import {
  Document,
  EditPen,
  Grid,
  Link,
  List,
  Memo,
  Picture,
  Plus,
  Refresh,
  UploadFilled,
} from '@element-plus/icons-vue'
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const imageDialogVisible = ref(false)
const imageUploading = ref(false)
const editorMode = ref('edit')
const rows = ref<any[]>([])
const total = ref(0)
const form = ref<Record<string, any>>({})
const tagText = ref('')
const imageUploadFiles = ref<any[]>([])
const imageUploadResult = ref<Record<string, any>>({})
const formRef = ref<any>()
const contentInputRef = ref<any>()
const query = ref({
  status: '',
  page: 1,
  pageSize: 10,
})

const canWrite = computed(() => hasRole('formal_member'))
const previewHtml = computed(() => renderMarkdown(form.value.contentMarkdown || ''))
const rules = ref({
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  contentMarkdown: [{ required: true, message: '请输入文章正文', trigger: 'blur' }],
})

function statusText(status: string) {
  return (
    { draft: '草稿', pending: '待审核', published: '已发布', hidden: '已隐藏', rejected: '已驳回' }[status] ||
    status ||
    '-'
  )
}

async function fetchList() {
  loading.value = true
  try {
    const data = await blogApi.myArticles({
      status: query.value.status || undefined,
      page: query.value.page,
      pageSize: query.value.pageSize,
    })
    rows.value = data?.list || []
    total.value = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  query.value.page = 1
  fetchList()
}

function handlePageChange(page: number) {
  query.value.page = page
  fetchList()
}

function openDialog(row?: any) {
  form.value = row
    ? { ...row, contentMarkdown: row.contentMarkdown || '' }
    : {
        title: '',
        summary: '',
        category: '',
        coverUrl: '',
        contentMarkdown: '# 标题\n\n',
      }
  tagText.value = (row?.tags || []).join(', ')
  dialogVisible.value = true
}

function openImageDialog() {
  imageUploadFiles.value = []
  imageUploadResult.value = {}
  imageDialogVisible.value = true
}

function textareaElement() {
  return contentInputRef.value?.textarea as HTMLTextAreaElement | undefined
}

function setMarkdown(value: string, selectionStart?: number, selectionEnd?: number) {
  form.value.contentMarkdown = value
  editorMode.value = 'edit'
  nextTick(() => {
    const textarea = textareaElement()
    if (!textarea) return
    textarea.focus()
    if (selectionStart !== undefined && selectionEnd !== undefined) {
      textarea.setSelectionRange(selectionStart, selectionEnd)
    }
  })
}

function wrapSelection(prefix: string, suffix = prefix, placeholder = '内容') {
  const value = String(form.value.contentMarkdown || '')
  const textarea = textareaElement()
  const start = textarea?.selectionStart ?? value.length
  const end = textarea?.selectionEnd ?? value.length
  const selected = value.slice(start, end) || placeholder
  const nextValue = `${value.slice(0, start)}${prefix}${selected}${suffix}${value.slice(end)}`
  const nextStart = start + prefix.length
  setMarkdown(nextValue, nextStart, nextStart + selected.length)
}

function insertSnippet(snippet: string, selectText?: string) {
  const value = String(form.value.contentMarkdown || '')
  const textarea = textareaElement()
  const start = textarea?.selectionStart ?? value.length
  const end = textarea?.selectionEnd ?? value.length
  const prefix = start > 0 && !value.slice(0, start).endsWith('\n\n') ? '\n\n' : ''
  const suffix = snippet.endsWith('\n\n') ? '' : '\n\n'
  const inserted = `${prefix}${snippet}${suffix}`
  const nextValue = `${value.slice(0, start)}${inserted}${value.slice(end)}`
  const matchedAt = selectText ? nextValue.indexOf(selectText, start) : -1
  const hasMatch = matchedAt >= 0 && Boolean(selectText)
  const selectAt = hasMatch ? matchedAt : start + inserted.length
  const selectEnd = hasMatch && selectText ? selectAt + selectText.length : selectAt
  setMarkdown(nextValue, selectAt, selectEnd)
}

function insertHeading() {
  wrapSelection('## ', '', '小标题')
}

function insertBlockquote() {
  insertSnippet('> 引用内容', '引用内容')
}

function insertList() {
  insertSnippet('- 要点一\n- 要点二\n- 要点三', '要点一')
}

function insertCodeBlock() {
  insertSnippet('```java\nSystem.out.println("Hello OpenAtom");\n```', 'System.out.println("Hello OpenAtom");')
}

function insertTable() {
  insertSnippet('| 字段 | 说明 |\n| --- | --- |\n| 标题 | 内容 |', '字段')
}

function insertLink() {
  insertSnippet('[链接文字](https://example.com)', '链接文字')
}

function save(status: string) {
  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        title: form.value.title,
        summary: form.value.summary,
        contentMarkdown: form.value.contentMarkdown,
        coverUrl: form.value.coverUrl,
        category: form.value.category,
        tags: parseTags(tagText.value),
        status,
      }
      if (form.value.id) await blogApi.update(form.value.id, payload)
      else await blogApi.create(payload)
      ElMessage.success(status === 'pending' ? '文章已提交审核' : '草稿已保存')
      dialogVisible.value = false
      fetchList()
    } finally {
      saving.value = false
    }
  })
}

async function publish(row: any) {
  await blogApi.publish(row.id)
  ElMessage.success('文章已提交审核')
  fetchList()
}

async function remove(row: any) {
  await blogApi.remove(row.id)
  ElMessage.success('文章已删除')
  fetchList()
}

function handleImageChange(_file: any, fileList: any[]) {
  imageUploadFiles.value = fileList.slice(-1)
  imageUploadResult.value = {}
}

function handleImageRemove(_file: any, fileList: any[]) {
  imageUploadFiles.value = fileList
  imageUploadResult.value = {}
}

async function uploadImage() {
  const file = imageUploadFiles.value[0]?.raw
  if (!file) {
    ElMessage.warning('请先选择图片')
    return
  }
  imageUploading.value = true
  try {
    imageUploadResult.value = await imageHostingApi.upload(file)
    ElMessage.success('图片上传成功')
  } finally {
    imageUploading.value = false
  }
}

function insertUploadedImage() {
  const url = imageUploadResult.value.url
  if (!url) return
  const markdown = imageUploadResult.value.markdown || `![图片](${url})`
  insertSnippet(markdown)
  if (!form.value.coverUrl) form.value.coverUrl = url
  imageDialogVisible.value = false
}

function parseTags(value: string) {
  return value
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 8)
}

onMounted(async () => {
  await fetchList()
  if (route.name === 'site-blog-write') {
    await nextTick()
    openDialog()
  }
})
</script>

<style scoped>
.my-blog-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.my-blog-shell {
  position: relative;
  padding: 92px 0 56px;
}

.my-blog-heading {
  max-width: 820px;
  margin-bottom: 24px;
}

.my-blog-heading h1 {
  margin: 10px 0 12px;
  color: var(--oa-text);
  font-size: clamp(36px, 5vw, 64px);
  line-height: 1.08;
  letter-spacing: 0;
}

.my-blog-heading p {
  max-width: 760px;
  color: var(--oa-muted);
  font-size: 17px;
  line-height: 1.7;
}

.write-alert {
  margin: 14px 0;
}

.muted-line,
.danger-line {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
}

.muted-line {
  color: var(--oa-muted);
}

.danger-line {
  color: var(--el-color-danger);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.editor-field {
  display: grid;
  width: 100%;
  gap: 10px;
}

.markdown-toolbar {
  display: flex;
  overflow-x: auto;
  padding-bottom: 2px;
}

.markdown-toolbar :deep(.el-button) {
  min-width: 44px;
}

.markdown-editor-tabs {
  width: 100%;
}

.editor-preview {
  min-height: 438px;
  max-height: 62vh;
  overflow: auto;
  padding: 16px;
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.image-upload-result {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.image-upload-result .el-image {
  width: 100%;
  height: 220px;
  overflow: hidden;
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius);
}

@media (max-width: 760px) {
  .my-blog-shell {
    padding-top: 76px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
