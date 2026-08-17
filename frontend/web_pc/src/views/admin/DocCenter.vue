<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索名称"
          style="width: 220px"
          @clear="fetchList"
          @keyup.enter="fetchList"
        />
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <div class="toolbar__actions">
        <el-button :icon="FolderAdd" @click="openCreateDir">新建目录</el-button>
        <input ref="fileInput" hidden type="file" @change="onUpload" />
        <el-button :disabled="uploading" type="primary" :icon="Upload" @click="fileInput?.click()">
          {{ uploading ? '上传中…' : '上传文件' }}
        </el-button>
      </div>
    </ViewToolbar>

    <el-breadcrumb class="crumbs" separator="/">
      <el-breadcrumb-item><el-link type="primary" @click="goRoot">文件架</el-link></el-breadcrumb-item>
      <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.id">
        <el-link type="primary" @click="goTo(item.id)">{{ item.name }}</el-link>
      </el-breadcrumb-item>
    </el-breadcrumb>

    <el-table v-loading="loading" :data="filteredRows" class="admin-table">
      <el-table-column label="名称" min-width="300">
        <template #default="{ row }">
          <span class="file-cell">
            <span class="file-icon"><component :is="iconOf(row)" /></span>
            <span class="file-copy">
              <el-link v-if="row.dir" type="primary" @click="enterDir(row)">{{ row.name }}</el-link>
              <strong v-else>{{ row.name }}</strong>
              <small class="muted-line">{{ row.dir ? '目录' : typeName(row.extension) + ' · ' + formatSize(row.sizeBytes) }}</small>
            </span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="密码" width="80" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.hasPassword" color="#f0a020"><Lock /></el-icon>
          <span v-else class="muted-line">—</span>
        </template>
      </el-table-column>
      <el-table-column label="修改时间" width="170">
        <template #default="{ row }">{{ formatTime(row.updatedAt || row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <template v-if="!row.dir">
            <el-button link type="primary" @click="preview(row)">预览</el-button>
            <el-button v-if="isOffice(row)" link type="primary" @click="edit(row)">在线编辑</el-button>
          </template>
          <el-button link type="primary" @click="download(row)">下载</el-button>
          <el-button link type="primary" @click="openRename(row)">重命名</el-button>
          <el-button link type="primary" @click="openSetPassword(row)">
            {{ row.hasPassword ? '改密码' : '设密码' }}
          </el-button>
          <el-popconfirm title="删除后不可恢复，确定删除？" @confirm="remove(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="这里还没有内容，上传文件或新建目录" />

    <!-- 新建目录 -->
    <el-dialog v-model="createDirOpen" title="新建目录" width="420px">
      <el-form label-width="70px">
        <el-form-item label="目录名"><el-input v-model="dirForm.name" maxlength="200" /></el-form-item>
        <el-form-item label="访问密码"><el-input v-model="dirForm.password" type="password" show-password placeholder="选填，设置后需密码才能访问" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDirOpen = false">取消</el-button>
        <el-button type="primary" @click="createDir">创建</el-button>
      </template>
    </el-dialog>

    <!-- 重命名 -->
    <el-dialog v-model="renameOpen" title="重命名" width="420px">
      <el-input v-model="renameForm.name" maxlength="200" />
      <template #footer>
        <el-button @click="renameOpen = false">取消</el-button>
        <el-button type="primary" @click="doRename">保存</el-button>
      </template>
    </el-dialog>

    <!-- 设置密码 -->
    <el-dialog v-model="passwordOpen" title="访问密码" width="420px">
      <el-form label-width="70px">
        <el-form-item :label="passwordTarget?.hasPassword ? '新密码' : '密码'">
          <el-input v-model="passwordForm.value" type="password" show-password placeholder="留空则清除密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordOpen = false">取消</el-button>
        <el-button type="primary" @click="doSetPassword">保存</el-button>
      </template>
    </el-dialog>

    <!-- 输入访问密码（预览/下载/编辑） -->
    <el-dialog v-model="askPasswordOpen" :title="`「${askPasswordTarget?.name}」需要访问密码`" width="420px">
      <el-input v-model="askPasswordValue" type="password" show-password placeholder="请输入访问密码" @keyup.enter="confirmAskPassword" />
      <template #footer>
        <el-button @click="askPasswordOpen = false">取消</el-button>
        <el-button type="primary" @click="confirmAskPassword">确定</el-button>
      </template>
    </el-dialog>

    <!-- 预览 -->
    <el-dialog v-model="previewOpen" class="preview-dialog" :title="previewTitle" width="80%" top="4vh">
      <div v-if="previewKind === 'image'" class="preview-body preview-image">
        <img :src="previewUrl" alt="预览" />
      </div>
      <iframe v-else-if="previewKind === 'pdf'" :src="previewUrl" class="preview-frame"></iframe>
      <div v-else-if="previewKind === 'md'" class="preview-body preview-md markdown-body" v-html="previewHtml"></div>
      <div v-else class="preview-body preview-none">
        <p>该文件类型暂不支持预览，请下载后查看。</p>
        <el-button type="primary" @click="download(previewTarget)">下载文件</el-button>
      </div>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import {
  Document, Files, Folder, FolderAdd, Grid, Lock, Picture, Refresh, Upload, VideoCamera,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { sharedFilesApi } from '@/api'
import { renderMarkdown } from '@/utils/markdown.ts'

const loading = ref(false)
const uploading = ref(false)
const rows = ref<any[]>([])
const query = ref({ keyword: '' })
const fileInput = ref<HTMLInputElement | null>(null)
const currentParentId = ref<number | null>(null)
const breadcrumbs = ref<{ id: number; name: string }[]>([])

const createDirOpen = ref(false)
const dirForm = ref({ name: '', password: '' })
const renameOpen = ref(false)
const renameForm = ref({ id: 0, name: '' })
const passwordOpen = ref(false)
const passwordForm = ref({ value: '' })
const passwordTarget = ref<any>(null)
const askPasswordOpen = ref(false)
const askPasswordValue = ref('')
const askPasswordTarget = ref<any>(null)
let askPasswordAction: (password?: string) => void = () => {}

const previewOpen = ref(false)
const previewKind = ref<'image' | 'pdf' | 'md' | 'none'>('none')
const previewUrl = ref('')
const previewHtml = ref('')
const previewTitle = ref('')
const previewTarget = ref<any>(null)
let previewObjectUrl = ''

const filteredRows = computed(() => {
  const keyword = query.value.keyword.trim().toLowerCase()
  if (!keyword) return rows.value
  return rows.value.filter((row) => row.name?.toLowerCase().includes(keyword))
})

const IMAGE_EXTS = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp', 'ico', 'avif']
const TEXT_EXTS = ['md', 'markdown', 'txt', 'text']
const OFFICE_EXTS = ['docx', 'xlsx', 'pptx']

function isImage(row: any) { return IMAGE_EXTS.includes(row.extension) }
function isText(row: any) { return TEXT_EXTS.includes(row.extension) }
function isOffice(row: any) { return OFFICE_EXTS.includes(row.extension) }
function isPdf(row: any) { return row.extension === 'pdf' }

async function fetchList() {
  loading.value = true
  try {
    rows.value = (await sharedFilesApi.list(currentParentId.value)) || []
    breadcrumbs.value = (await sharedFilesApi.path(currentParentId.value)) || []
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function goRoot() {
  currentParentId.value = null
  void fetchList()
}

function enterDir(row: any) {
  currentParentId.value = row.id
  void fetchList()
}

function goTo(id: number) {
  currentParentId.value = id
  void fetchList()
}

function openCreateDir() {
  dirForm.value = { name: '', password: '' }
  createDirOpen.value = true
}

async function createDir() {
  if (!dirForm.value.name.trim()) {
    ElMessage.warning('请输入目录名')
    return
  }
  try {
    await sharedFilesApi.createDir({
      parentId: currentParentId.value,
      name: dirForm.value.name,
      password: dirForm.value.password || undefined,
    })
    ElMessage.success('目录已创建')
    createDirOpen.value = false
    await fetchList()
  } catch (error: any) {
    ElMessage.error(error?.message || '创建失败')
  }
}

async function onUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const proceed = async (password?: string) => {
    uploading.value = true
    try {
      await sharedFilesApi.upload(file, currentParentId.value, password)
      ElMessage.success('上传成功')
      await fetchList()
    } catch (error: any) {
      ElMessage.error(error?.message || '上传失败')
    } finally {
      uploading.value = false
    }
  }
  if (askPasswordTarget.value) {
    // 目录密码场景：上传到带密码的目录时校验目录密码
    askPasswordTarget.value = null
  }
  await proceed()
}

function openRename(row: any) {
  renameForm.value = { id: row.id, name: row.name }
  renameOpen.value = true
}

async function doRename() {
  if (!renameForm.value.name.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  try {
    await sharedFilesApi.rename(renameForm.value.id, renameForm.value.name)
    ElMessage.success('已重命名')
    renameOpen.value = false
    await fetchList()
  } catch (error: any) {
    ElMessage.error(error?.message || '重命名失败')
  }
}

function openSetPassword(row: any) {
  passwordTarget.value = row
  passwordForm.value = { value: '' }
  passwordOpen.value = true
}

async function doSetPassword() {
  try {
    await sharedFilesApi.setPassword(passwordTarget.value.id, passwordForm.value.value)
    ElMessage.success('密码已更新')
    passwordOpen.value = false
    await fetchList()
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败')
  }
}

async function remove(row: any) {
  try {
    await sharedFilesApi.remove(row.id)
    ElMessage.success('已删除')
    await fetchList()
  } catch {
    ElMessage.error('删除失败')
  }
}

function requirePassword(row: any, action: (password?: string) => void) {
  if (row.hasPassword) {
    askPasswordTarget.value = row
    askPasswordValue.value = ''
    askPasswordAction = action
    askPasswordOpen.value = true
  } else {
    action()
  }
}

function confirmAskPassword() {
  const action = askPasswordAction
  askPasswordOpen.value = false
  action(askPasswordValue.value || undefined)
}

function preview(row: any) {
  previewTarget.value = row
  requirePassword(row, (password) => {
    if (isImage(row) || isPdf(row)) {
      openBinaryPreview(row, password)
    } else if (isText(row)) {
      openTextPreview(row, password)
    } else if (isOffice(row)) {
      openEdit(row, password)
    } else {
      previewKind.value = 'none'
      previewTitle.value = row.name
      previewOpen.value = true
    }
  })
}

async function openBinaryPreview(row: any, password?: string) {
  try {
    const blob = await sharedFilesApi.download(row.id, password)
    if (previewObjectUrl) URL.revokeObjectURL(previewObjectUrl)
    previewObjectUrl = URL.createObjectURL(blob)
    previewKind.value = isPdf(row) ? 'pdf' : 'image'
    previewUrl.value = previewObjectUrl
    previewTitle.value = row.name
    previewOpen.value = true
  } catch {
    ElMessage.error('预览失败（密码错误或文件不存在）')
  }
}

async function openTextPreview(row: any, password?: string) {
  try {
    const text = await sharedFilesApi.text(row.id, password)
    previewKind.value = 'md'
    previewHtml.value = row.extension === 'md' || row.extension === 'markdown'
      ? renderMarkdown(text)
      : `<pre style="white-space:pre-wrap">${escapeHtml(text)}</pre>`
    previewTitle.value = row.name
    previewOpen.value = true
  } catch {
    ElMessage.error('预览失败（密码错误或文件不存在）')
  }
}

function edit(row: any) {
  requirePassword(row, (password) => openEdit(row, password))
}

function openEdit(row: any, password?: string) {
  const query = password ? `?password=${encodeURIComponent(password)}` : ''
  window.open(`/shared-edit/${row.id}${query}`, '_blank')
}

function download(row: any) {
  requirePassword(row, async (password) => {
    try {
      const blob = await sharedFilesApi.download(row.id, password)
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = row.name
      anchor.hidden = true
      document.body.append(anchor)
      anchor.click()
      anchor.remove()
      URL.revokeObjectURL(url)
    } catch {
      ElMessage.error('下载失败（密码错误或文件不存在）')
    }
  })
}

function iconOf(row: any) {
  if (row.dir) return Folder
  if (row.extension === 'xlsx') return Grid
  if (row.extension === 'pptx') return VideoCamera
  if (isImage(row)) return Picture
  if (row.extension === 'pdf') return Document
  return Files
}

function typeName(extension: string) {
  const map: Record<string, string> = {
    docx: 'Word', xlsx: 'Excel', pptx: 'PPT', pdf: 'PDF', md: 'Markdown',
    png: '图片', jpg: '图片', jpeg: '图片', gif: '图片', webp: '图片', svg: '图片',
    txt: '文本', zip: '压缩包', rar: '压缩包', mp4: '视频', mp3: '音频',
  }
  return map[extension] || extension.toUpperCase() || '文件'
}

function formatSize(bytes: number) {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KiB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MiB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GiB`
}

function formatTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short' }).format(date)
}

function escapeHtml(value: string) {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

onMounted(() => {
  void fetchList()
})
</script>

<style scoped>
.crumbs {
  margin: 0 0 14px;
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 10px;
  color: var(--oa-primary);
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  font-size: 18px;
}

.file-copy {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.file-copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted-line {
  margin: 0;
  color: var(--oa-muted);
  font-size: 12px;
}

.preview-body {
  min-height: 60vh;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 12px;
}

.preview-image img {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 8px;
}

.preview-frame {
  width: 100%;
  height: 70vh;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.preview-md {
  max-height: 70vh;
  overflow-y: auto;
  justify-items: start;
  padding: 8px 4px;
}

.preview-none {
  color: var(--oa-muted);
}
</style>
