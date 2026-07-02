<template>
  <ViewPage class="admin-page file-migration-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-button 
          type="primary" 
          :icon="Download" 
          :loading="exporting || (exportTask?.status === 'processing')" 
          :disabled="exportTask?.status === 'processing'"
          @click="handleExport"
        >
          {{ exportTask?.status === 'processing' ? '导出中...' : exportTask?.status === 'completed' ? '重新导出' : '导出全部文件' }}
        </el-button>
        <el-button 
          v-if="exportTask?.status === 'completed'" 
          type="success" 
          :icon="Download" 
          :loading="downloading"
          :disabled="downloading"
          @click="downloadExportedFile"
        >
          {{ downloading ? `下载中... ${downloadProgress}%` : '下载 ZIP 文件' }}
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
      <p v-if="exportTask" style="margin-top: 8px;">
        <strong>当前任务：</strong>{{ exportTask.status === 'processing' ? '正在后台生成 ZIP 文件...' : exportTask.status === 'completed' ? '导出完成，点击下载' : exportTask.message }}
        <span v-if="exportTask.progress">（{{ exportTask.progress }}%）</span>
      </p>
    </el-alert>

    <div v-if="downloading" class="download-progress-bar">
      <div class="download-progress-info">
        <span>正在下载...</span>
        <span>{{ formatFileSize(downloadLoaded) }} / {{ formatFileSize(downloadTotal) }}</span>
      </div>
      <el-progress :percentage="downloadProgress" :stroke-width="10" status="success" />
    </div>

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

    <el-divider content-position="left">自动化备份</el-divider>

    <div class="backup-section">
      <div class="backup-toolbar">
        <el-button type="warning" :icon="FolderOpened" :loading="backupRunning" @click="handleRunBackup">
          手动触发备份
        </el-button>
        <el-button :icon="Refresh" @click="fetchBackups">刷新备份列表</el-button>
        <span class="backup-hint">系统每天凌晨 3:00 自动备份当天完成的导出文件</span>
      </div>

      <el-table v-loading="backupsLoading" :data="backups" border style="width: 100%" empty-text="暂无备份文件">
        <el-table-column label="文件名" min-width="280">
          <template #default="{ row }">
            <el-icon class="backup-icon"><Document /></el-icon>
            <span>{{ row.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="140" align="center">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="备份时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="danger" size="small" :icon="Delete" @click="handleDeleteBackup(row.fileName)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Document, Download, Folder, FolderOpened, Refresh, Upload, UploadFilled } from '@element-plus/icons-vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'
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

interface ExportTask {
  taskId: string
  status: 'pending' | 'processing' | 'completed' | 'failed'
  progress?: number
  message?: string
  fileName?: string
  fileSize?: number
  error?: string
}

interface BackupFileInfo {
  fileName: string
  fileSize: number
  createTime: string
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
const exportTask = ref<ExportTask | null>(null)
let pollTimer: number | null = null

// 下载相关
const downloading = ref(false)
const downloadProgress = ref(0)
const downloadLoaded = ref(0)
const downloadTotal = ref(0)

// 备份相关
const backups = ref<BackupFileInfo[]>([])
const backupsLoading = ref(false)
const backupRunning = ref(false)

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

async function fetchBackups() {
  backupsLoading.value = true
  try {
    const data = await fileMigrationApi.listBackups()
    backups.value = data || []
  } catch (error) {
    ElMessage.error('获取备份列表失败')
  } finally {
    backupsLoading.value = false
  }
}

async function handleRunBackup() {
  try {
    await ElMessageBox.confirm('确定要手动触发备份吗？将备份当天完成的导出文件。', '确认备份', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    backupRunning.value = true
    await fileMigrationApi.runBackup()
    ElMessage.success('备份任务已执行，请查看日志获取详细信息')
    fetchBackups()
  } catch (error: any) {
    if (error !== 'cancel' && error?.message !== 'cancel') {
      ElMessage.error('备份失败：' + (error?.message || '未知错误'))
    }
  } finally {
    backupRunning.value = false
  }
}

async function handleDeleteBackup(fileName: string) {
  try {
    await ElMessageBox.confirm(`确定要删除备份文件 "${fileName}" 吗？`, '确认删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await fileMigrationApi.deleteBackup(fileName)
    ElMessage.success('备份文件已删除')
    fetchBackups()
  } catch (error: any) {
    if (error !== 'cancel' && error?.message !== 'cancel') {
      ElMessage.error('删除失败：' + (error?.message || '未知错误'))
    }
  }
}

async function handleExport() {
  if (exportTask.value?.status === 'processing') {
    ElMessage.warning('已有导出任务正在进行中，请稍后')
    return
  }
  
  try {
    const result = await fileMigrationApi.startExport()
    exportTask.value = result
    ElMessage.success('导出任务已启动，正在后台处理...')
    startPolling(result.taskId)
  } catch (error) {
    console.error('启动导出失败:', error)
    ElMessage.error('启动导出任务失败，请稍后重试')
  }
}

function startPolling(taskId: string) {
  if (pollTimer) {
    clearInterval(pollTimer)
  }
  
  pollTimer = window.setInterval(async () => {
    try {
      const task = await fileMigrationApi.getExportStatus(taskId)
      exportTask.value = task
      
      if (task.status === 'completed') {
        stopPolling()
        ElMessage.success('文件资源导出成功，点击下载按钮获取 ZIP 文件')
        fetchStats()
      } else if (task.status === 'failed') {
        stopPolling()
        ElMessage.error(`导出失败：${task.error || '未知错误'}`)
      }
    } catch (error) {
      console.error('查询导出状态失败:', error)
      stopPolling()
    }
  }, 2000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function downloadExportedFile() {
  if (!exportTask.value || exportTask.value.status !== 'completed') {
    ElMessage.warning('没有可下载的文件')
    return
  }
  
  downloading.value = true
  downloadProgress.value = 0
  downloadLoaded.value = 0
  downloadTotal.value = exportTask.value.fileSize || 0
  
  try {
    const blob = await fileMigrationApi.downloadExport(exportTask.value.taskId, (loaded, total) => {
      downloadLoaded.value = loaded
      if (total > 0) {
        downloadTotal.value = total
        downloadProgress.value = Math.min(100, Math.round((loaded / total) * 100))
      } else if (downloadTotal.value > 0) {
        downloadProgress.value = Math.min(100, Math.round((loaded / downloadTotal.value) * 100))
      }
    })
    
    downloadProgress.value = 100
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = exportTask.value.fileName || `openatom-files-${formatTimestamp(new Date())}.zip`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('文件下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败，请稍后重试')
  } finally {
    downloading.value = false
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

function formatDateTime(dateStr: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

onMounted(() => {
  fetchStats()
  fetchBackups()
})

onUnmounted(() => {
  stopPolling()
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

.backup-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.backup-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.backup-hint {
  color: var(--oa-muted);
  font-size: 13px;
}

.backup-icon {
  margin-right: 6px;
  color: var(--oa-muted);
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

.download-progress-bar {
  padding: 12px 16px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.download-progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--oa-muted);
}
</style>
