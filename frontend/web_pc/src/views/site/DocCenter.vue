<template>
  <ViewPage class="doc-center-page">
    <div class="doc-center-head">
      <div>
        <h1>文档中心</h1>
        <p>上传 .docx / .xlsx / .pptx，在线编辑与协作，文件保存在社团服务器。</p>
      </div>
      <input ref="fileInput" hidden accept=".docx,.xlsx,.pptx" type="file" @change="onUpload" />
      <el-button :disabled="uploading" type="primary" @click="fileInput?.click()">
        {{ uploading ? '上传中…' : '上传文档' }}
      </el-button>
    </div>

    <el-empty v-if="!loading && !rows.length" description="还没有文档，点击右上角上传第一个文档" />
    <el-table v-else v-loading="loading" :data="rows" class="doc-center-table">
      <el-table-column label="名称" min-width="280">
        <template #default="{ row }">
          <span class="doc-name"><component :is="iconOf(row.extension)" /> {{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ typeName(row.extension) }}</template>
      </el-table-column>
      <el-table-column label="大小" width="120">
        <template #default="{ row }">{{ formatSize(row.sizeBytes) }}</template>
      </el-table-column>
      <el-table-column label="修改时间" width="180">
        <template #default="{ row }">{{ formatTime(row.updatedAt || row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openEditor(row)">在线编辑</el-button>
          <el-button size="small" @click="download(row)">下载</el-button>
          <el-popconfirm title="删除后不可恢复，确定删除？" @confirm="remove(row)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editorOpen" class="doc-editor-dialog" :close-on-click-modal="false" fullscreen>
      <div ref="editorEl" class="doc-editor-host"></div>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import { Document, Grid, Picture as PictureIcon, VideoCamera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { nextTick, onBeforeUnmount, ref } from 'vue'
import ViewPage from '@/components/common/ViewPage.vue'
import { documentCenterApi } from '@/api'

const rows = ref<any[]>([])
const loading = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const editorOpen = ref(false)
const editorEl = ref<HTMLElement | null>(null)
let editorInstance: any = null
let sdkLoaded = false
let sdkLoading: Promise<void> | null = null

async function loadRows() {
  loading.value = true
  try {
    rows.value = (await documentCenterApi.list()) || []
  } catch {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

async function onUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploading.value = true
  try {
    await documentCenterApi.upload(file)
    ElMessage.success('上传成功')
    await loadRows()
  } catch (error: any) {
    ElMessage.error(error?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function download(row: any) {
  try {
    const blob = await documentCenterApi.download(row.id)
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
    ElMessage.error('下载失败')
  }
}

async function remove(row: any) {
  try {
    await documentCenterApi.remove(row.id)
    ElMessage.success('已删除')
    await loadRows()
  } catch {
    ElMessage.error('删除失败')
  }
}

function loadSdk(serverUrl: string): Promise<void> {
  if (sdkLoaded) return Promise.resolve()
  if (sdkLoading) return sdkLoading
  sdkLoading = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `${serverUrl.replace(/\/+$/, '')}/web-apps/apps/api/documents/api.js`
    script.onload = () => {
      sdkLoaded = true
      resolve()
    }
    script.onerror = () => {
      sdkLoading = null
      reject(new Error('加载编辑器失败'))
    }
    document.head.append(script)
  })
  return sdkLoading
}

async function openEditor(row: any) {
  let config: any
  try {
    const result = await documentCenterApi.editConfig(row.id)
    config = result.config
    await loadSdk(result.documentServerUrl)
  } catch (error: any) {
    ElMessage.error(error?.message || '无法打开编辑器')
    return
  }
  editorOpen.value = true
  await nextTick()
  if (editorEl.value) {
    editorInstance = new (window as any).DocsAPI.DocEditor(editorEl.value, config)
  }
}

function iconOf(extension: string) {
  if (extension === 'xlsx') return Grid
  if (extension === 'pptx') return VideoCamera
  return Document
}

function typeName(extension: string) {
  return { docx: 'Word 文档', xlsx: 'Excel 表格', pptx: 'PPT 演示' }[extension] || extension
}

function formatSize(bytes: number) {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KiB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MiB`
}

function formatTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short' }).format(date)
}

onBeforeUnmount(() => {
  if (editorInstance?.destroyEditor) {
    editorInstance.destroyEditor()
  }
  editorInstance = null
})

loadRows()
</script>

<style scoped>
.doc-center-page {
  max-width: 1000px;
}

.doc-center-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.doc-center-head h1 {
  margin: 0 0 4px;
  font-size: 22px;
}

.doc-center-head p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.doc-name {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.doc-editor-host {
  width: 100%;
  height: 100%;
}
</style>
