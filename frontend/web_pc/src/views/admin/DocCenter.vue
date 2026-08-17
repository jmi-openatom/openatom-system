<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索文件名"
          style="width: 240px"
          @clear="reload"
          @keyup.enter="reload"
        />
        <el-button type="primary" :icon="Refresh" @click="reload">刷新</el-button>
      </div>
      <div class="toolbar__actions">
        <input ref="fileInput" hidden accept=".docx,.xlsx,.pptx" type="file" @change="onUpload" />
        <el-button :disabled="uploading" type="primary" :icon="Upload" @click="fileInput?.click()">
          {{ uploading ? '上传中…' : '上传文档' }}
        </el-button>
      </div>
    </ViewToolbar>

    <el-table v-loading="loading" :data="filteredRows" class="admin-table">
      <el-table-column label="名称" min-width="300">
        <template #default="{ row }">
          <div class="doc-cell">
            <span class="doc-icon"><component :is="iconOf(row.extension)" /></span>
            <span class="doc-copy">
              <strong>{{ row.name }}</strong>
              <small class="muted-line">{{ typeName(row.extension) }} · {{ formatSize(row.sizeBytes) }}</small>
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="上传者" width="120">
        <template #default="{ row }">{{ row.ownerName || '—' }}</template>
      </el-table-column>
      <el-table-column label="修改时间" width="180">
        <template #default="{ row }">{{ formatTime(row.updatedAt || row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditor(row)">在线编辑</el-button>
          <el-button link type="primary" @click="download(row)">下载</el-button>
          <el-popconfirm title="删除后不可恢复，确定删除？" @confirm="remove(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="还没有文档，点击右上角上传第一个文档" />
  </ViewPage>
</template>

<script setup lang="ts">
import { Document, Files, Grid, Refresh, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { documentCenterApi } from '@/api'

const loading = ref(false)
const uploading = ref(false)
const rows = ref<any[]>([])
const query = ref({ keyword: '' })
const fileInput = ref<HTMLInputElement | null>(null)

const filteredRows = computed(() => {
  const keyword = query.value.keyword.trim().toLowerCase()
  if (!keyword) return rows.value
  return rows.value.filter((row) => row.name?.toLowerCase().includes(keyword))
})

async function fetchList() {
  loading.value = true
  try {
    rows.value = (await documentCenterApi.list()) || []
  } catch {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

function reload() {
  void fetchList()
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
    await fetchList()
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
    await fetchList()
  } catch {
    ElMessage.error('删除失败')
  }
}

function openEditor(row: any) {
  window.open(`/doc-edit/${row.id}`, '_blank')
}

function iconOf(extension: string) {
  if (extension === 'xlsx') return Grid
  if (extension === 'pptx') return Files
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

onMounted(() => {
  void fetchList()
})
</script>

<style scoped>
.doc-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-icon {
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

.doc-copy {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.doc-copy strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted-line {
  margin: 0;
  color: var(--oa-muted);
  font-size: 12px;
}
</style>
