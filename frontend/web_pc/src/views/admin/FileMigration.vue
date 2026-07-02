<template>
  <ViewPage class="admin-page file-migration-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-button type="primary" :icon="Download" :loading="exporting" @click="handleExport">
          导出全部文件
        </el-button>
        <el-button :icon="Refresh" @click="fetchStats">刷新统计</el-button>
      </div>
    </ViewToolbar>

    <el-alert
      class="page-alert"
      type="info"
      :closable="false"
      show-icon
    >
      <template #title>文件资源迁移</template>
      <p>导出会将头像、图床图片、请假附件、文档模板、生成文档等所有上传文件打包为 ZIP，用于服务器迁移。导入时请选择此前导出的 ZIP 文件。</p>
    </el-alert>

    <el-table v-loading="loading" :data="stats" class="admin-table" border>
      <el-table-column label="存储目录" min-width="180">
        <template #default="{ row }">
          <div class="dir-cell">
            <el-icon class="dir-icon"><Folder /></el-icon>
            <div>
              <strong>{{ dirNameMap[row.name] || row.name }}</strong>
              <p class="muted-line">{{ row.path }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="目录标识" width="180">
        <template #default="{ row }">
          <el-tag>{{ row.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件数量" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.exists">{{ row.fileCount }}</span>
          <span v-else class="muted-text">目录不存在</span>
        </template>
      </el-table-column>
      <el-table-column label="总大小" width="140" align="center">
        <template #default="{ row }">
          <span v-if="row.exists">{{ formatFileSize(row.totalSize) }}</span>
          <span v-else class="muted-text">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.exists ? 'success' : 'info'">
            {{ row.exists ? '正常' : '未创建' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div class="summary-bar" v-if="totalFiles > 0">
      <span>合计：<strong>{{ totalFiles }}</strong> 个文件，总大小 <strong>{{ formatFileSize(totalSize) }}</strong></span>
    </div>

    <el-divider content-position="left">导入文件资源</el-divider>

    <div class="import-section">
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".zip"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
        drag
        class="import-upload"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 ZIP 文件拖到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .zip 文件，应为此前通过导出功能生成的文件</div>
        </template>
      </el-upload>

      <div class="import-options">
        <el-checkbox v-model="overwrite">覆盖已存在的文件</el-checkbox>
        <el-button
          type="primary"
          :icon="Upload"
          :loading="importing"
          :disabled="!selectedFile"
          @click="handleImport"
        >
          开始导入
        </el-button>
      </div>

      <el-alert
        v-if="importResult"
        class="import-result-alert"
        :type="importResult.importedFiles > 0 ? 'success' : 'warning'"
        :closable="true"
        show-icon
      >
        <template #title>导入结果</template>
        <p>成功导入 <strong>{{ importResult.importedFiles }}</strong> 个文件，跳过 <strong>{{ importResult.skippedFiles }}</strong> 个，总大小 {{ formatFileSize(importResult.importedSize) }}</p>
      </el-alert>
    </div>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { fileMigrationApi } from '@/api'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Download, Folder, Refresh, Upload, UploadFilled } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import type { UploadFile, UploadFiles, UploadInstance } from 'element-plus'

interface DirectoryStats {
  name: string
  path: string
  fileCount: number
  totalSize: number
  exists: boolean
}

interface ImportResult {
  importedFiles: number
  skippedFiles: number
  importedSize: number
}

const dirNameMap: Record<string, string> = {
  avatars: '用户头像',
  images: '图床图片',
  'leave-attachments': '请假附件',
  'document-templates': '文档模板',
  'generated-documents': '生成文档',
}

const loading = ref(false)
const exporting = ref(false)
const importing = ref(false)
const stats = ref<DirectoryStats[]>([])
const selectedFile = ref<File | null>(null)
const overwrite = ref(false)
const importResult = ref<ImportResult | null>(null)
const uploadRef = ref<UploadInstance>()

const totalFiles = computed(() =>
  stats.value.reduce((sum, item) => sum + (item.exists ? item.fileCount : 0), 0),
)
const totalSize = computed(() =>
  stats.value.reduce((sum, item) => sum + (item.exists ? item.totalSize : 0), 0),
)

async function fetchStats() {
  loading.value = true
  try {
    const data = await fileMigrationApi.stats()
    stats.value = data || []
  } catch (error) {
    ElMessage.error('获取统计信息失败')
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const blob = await fileMigrationApi.export()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `openatom-files-${formatTimestamp(new Date())}.zip`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('文件资源导出成功')
    fetchStats()
  } catch (error) {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

function handleFileChange(file: UploadFile, _files: UploadFiles) {
  if (file.raw) {
    selectedFile.value = file.raw
    importResult.value = null
  }
}

function handleExceed(_files: UploadFile[], uploadFiles: UploadFiles) {
  uploadRef.value?.clearFiles()
  const first = uploadFiles[0]
  if (first) {
    first.raw && (selectedFile.value = first.raw)
  }
}

async function handleImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 ZIP 文件')
    return
  }
  importing.value = true
  try {
    const data = await fileMigrationApi.importZip(selectedFile.value, overwrite.value)
    importResult.value = data
    ElMessage.success('文件资源导入完成')
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    fetchStats()
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '导入失败'
    ElMessage.error(message)
  } finally {
    importing.value = false
  }
}

function formatFileSize(value: number) {
  const size = Number(value || 0)
  if (size >= 1024 * 1024 * 1024) return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  if (size >= 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${size} B`
}

function formatTimestamp(date: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}-${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.file-migration-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-alert {
  margin-bottom: 4px;
}

.page-alert p {
  margin: 4px 0 0;
  line-height: 1.6;
}

.dir-cell {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.dir-icon {
  margin-top: 2px;
  font-size: 20px;
  color: var(--oa-muted);
  flex-shrink: 0;
}

.muted-line {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
  word-break: break-all;
}

.muted-text {
  color: var(--oa-muted);
}

.summary-bar {
  padding: 12px 16px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  font-size: 14px;
}

.summary-bar strong {
  color: var(--oa-active-text);
}

.import-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.import-upload {
  width: 100%;
}

.import-upload :deep(.el-upload-dragger) {
  width: 100%;
}

.import-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.import-result-alert p {
  margin: 4px 0 0;
}

.import-result-alert strong {
  color: var(--oa-active-text);
}
</style>
