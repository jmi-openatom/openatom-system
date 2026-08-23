<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索名称"
          style="width: 200px"
          @clear="fetchList"
          @keyup.enter="fetchList"
        />
        <el-button :icon="Refresh" circle @click="fetchList" />
        <el-radio-group v-model="viewMode" size="default">
          <el-radio-button value="grid"><el-icon><Menu /></el-icon></el-radio-button>
          <el-radio-button value="list"><el-icon><Expand /></el-icon></el-radio-button>
        </el-radio-group>
      </div>
      <div class="toolbar__actions">
        <el-button :icon="FolderAdd" @click="openCreateDir">新建目录</el-button>
        <input ref="fileInput" hidden type="file" @change="onUpload" />
        <el-button :disabled="uploading" type="primary" :icon="Upload" @click="fileInput?.click()">
          {{ uploading ? '上传中…' : '上传文件' }}
        </el-button>
      </div>
    </ViewToolbar>

    <div class="crumbs-row">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item><el-link type="primary" @click="goRoot">文件架</el-link></el-breadcrumb-item>
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.id">
          <el-link type="primary" @click="goTo(item.id)">{{ item.name }}</el-link>
        </el-breadcrumb-item>
      </el-breadcrumb>
      <transition name="fade">
        <div v-if="selectedRows.length" class="selection-bar">
          <span>已选 <strong>{{ selectedRows.length }}</strong> 项</span>
          <el-button size="small" round :icon="FolderOpened" @click="openMoveDialog">移动到</el-button>
          <el-button v-if="selectedFiles.length" size="small" round :icon="Download" @click="bulkDownload">下载</el-button>
          <el-popconfirm title="确定删除选中的项？" @confirm="bulkDelete">
            <template #reference><el-button size="small" round type="danger" :icon="Delete">删除</el-button></template>
          </el-popconfirm>
          <el-button size="small" text @click="clearSelection">取消</el-button>
        </div>
      </transition>
    </div>

    <div
      class="drop-zone"
      :class="{ 'drop-active': dragOverDepth > 0 }"
      @click="closeContextMenu"
      @contextmenu.prevent="openContextMenu($event, null)"
      @dragenter.prevent="dragEnterZone"
      @dragover.prevent="dragEnterZone"
      @dragleave="dragLeaveZone"
      @drop.prevent="onDropZone"
    >
      <div v-if="dragOverDepth > 0" class="drop-overlay">
        <el-icon :size="36"><component :is="draggingExternal ? UploadFilled : FolderOpened" /></el-icon>
        <p>{{ draggingExternal ? '松开上传到当前目录' : '松开移动到当前目录' }}</p>
      </div>

      <!-- 网格视图 -->
      <div v-if="viewMode === 'grid'" v-loading="loading" class="file-grid">
        <div
          v-for="row in filteredRows"
          :key="row.id"
          class="file-card"
          :class="{ selected: isSelected(row), droppable: dropTargetId === row.id && row.dir }"
          draggable="true"
          @dragstart="onDragStart($event, row)"
          @dragend="onDragEnd"
          @dragover.prevent="dropTargetId = row.dir ? row.id : null"
          @dragleave="dropTargetId = null"
          @drop.prevent.stop="onDropOnRow($event, row)"
          @dblclick.stop="openRow(row)"
          @contextmenu.stop.prevent="openContextMenu($event, row)"
        >
          <el-checkbox
            :model-value="isSelected(row)"
            class="file-card__check"
            @change="toggleSelect(row)"
            @click.stop
          />
          <div class="file-card__icon" :class="'tone-' + toneOf(row)">
            <component :is="iconOf(row)" />
          </div>
          <div class="file-card__name" :title="row.name">
            <el-icon v-if="row.hasPassword" class="lock"><Lock /></el-icon>{{ row.name }}
          </div>
          <div class="file-card__meta">{{ row.dir ? '目录' : typeName(row.extension) + ' · ' + formatSize(row.sizeBytes) }}</div>
          <div class="file-card__actions">
            <el-tooltip content="打开" placement="top"><el-button circle :icon="row.dir ? FolderOpened : View" size="small" @click.stop="openRow(row)" /></el-tooltip>
            <el-tooltip v-if="!row.dir && isOffice(row)" content="在线编辑" placement="top"><el-button circle :icon="EditPen" size="small" @click.stop="edit(row)" /></el-tooltip>
            <el-tooltip content="下载" placement="top"><el-button circle :icon="Download" size="small" @click.stop="download(row)" /></el-tooltip>
            <el-tooltip content="更多" placement="top">
              <el-button circle :icon="MoreFilled" size="small" @click.stop="openCardMenu($event, row)" />
            </el-tooltip>
          </div>
        </div>
        <div v-if="!loading && !filteredRows.length" class="grid-empty">
          <el-empty description="这里还没有内容，上传文件或新建目录" />
        </div>
      </div>

      <!-- 列表视图 -->
      <el-table
        v-else
        v-loading="loading"
        :data="filteredRows"
        class="admin-table"
        @row-contextmenu="onRowContextMenu"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="42" />
        <el-table-column label="名称" min-width="220">
          <template #default="{ row }">
            <span
              class="file-cell"
              draggable="true"
              @dragstart="onDragStart($event, row)"
              @dragend="onDragEnd"
              @click.stop="closeContextMenu"
              @contextmenu.stop.prevent="openContextMenu($event, row)"
            >
              <span class="file-icon"><component :is="iconOf(row)" /></span>
              <span class="file-copy">
                <el-link v-if="row.dir" type="primary" @click="enterDir(row)">{{ row.name }}</el-link>
                <strong v-else>{{ row.name }}</strong>
                <small class="muted-line">{{ row.dir ? '目录' : typeName(row.extension) + ' · ' + formatSize(row.sizeBytes) }}</small>
              </span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="密码" width="70" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.hasPassword" color="#f0a020"><Lock /></el-icon>
            <span v-else class="muted-line">—</span>
          </template>
        </el-table-column>
        <el-table-column label="修改时间" width="160">
          <template #default="{ row }">{{ formatTime(row.updatedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="290">
          <template #default="{ row }">
            <template v-if="!row.dir">
              <el-button link type="primary" @click="preview(row)">预览</el-button>
              <el-button v-if="isOffice(row)" link type="primary" @click="edit(row)">在线编辑</el-button>
            </template>
            <el-button link type="primary" @click="download(row)">下载</el-button>
            <el-button link type="primary" @click="openRename(row)">重命名</el-button>
            <el-button link type="primary" @click="openSetPassword(row)">{{ row.hasPassword ? '改密码' : '设密码' }}</el-button>
            <el-popconfirm title="删除后不可恢复，确定删除？" @confirm="remove(row)">
              <template #reference><el-button link type="danger" @click.stop>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !rows.length && viewMode === 'list'" description="这里还没有内容，上传文件或新建目录" />
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click.stop
    >
      <template v-if="contextMenu.target">
        <div class="context-menu__title">{{ contextMenu.target.name }}</div>
        <button type="button" @click="menuOpen"><component :is="iconOf(contextMenu.target)" /> 打开</button>
        <template v-if="!contextMenu.target.dir">
          <button type="button" @click="menuPreview"><el-icon><View /></el-icon> 预览</button>
          <button v-if="isOffice(contextMenu.target)" type="button" @click="menuEdit"><el-icon><EditPen /></el-icon> 在线编辑</button>
          <button type="button" @click="menuDownload"><el-icon><Download /></el-icon> 下载</button>
        </template>
        <button type="button" @click="menuMove"><el-icon><FolderOpened /></el-icon> 移动到</button>
        <button type="button" @click="menuRename"><el-icon><EditPen /></el-icon> 重命名</button>
        <button type="button" @click="menuPassword"><el-icon><Lock /></el-icon> {{ contextMenu.target.hasPassword ? '修改密码' : '设置密码' }}</button>
        <div class="context-menu__sep"></div>
        <button type="button" class="danger" @click="menuDelete"><el-icon><Delete /></el-icon> 删除</button>
      </template>
      <template v-else>
        <div class="context-menu__title">当前目录</div>
        <button type="button" @click="menuCreateDir"><el-icon><FolderAdd /></el-icon> 新建目录</button>
        <button type="button" @click="menuUpload"><el-icon><Upload /></el-icon> 上传文件</button>
        <button type="button" @click="menuRefresh"><el-icon><Refresh /></el-icon> 刷新</button>
      </template>
    </div>

    <!-- 新建目录 -->
    <el-dialog v-model="createDirOpen" title="新建目录" width="440px">
      <el-form label-width="80px">
        <el-form-item label="目录名"><el-input v-model="dirForm.name" maxlength="200" placeholder="请输入目录名" /></el-form-item>
        <el-form-item label="访问密码"><el-input v-model="dirForm.password" type="password" show-password placeholder="选填，设置后访问需输入密码" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDirOpen = false">取消</el-button>
        <el-button type="primary" @click="createDir">创建</el-button>
      </template>
    </el-dialog>

    <!-- 重命名 -->
    <el-dialog v-model="renameOpen" title="重命名" width="440px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="renameForm.name" maxlength="200" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameOpen = false">取消</el-button>
        <el-button type="primary" @click="doRename">保存</el-button>
      </template>
    </el-dialog>

    <!-- 设置密码 -->
    <el-dialog v-model="passwordOpen" title="访问密码" width="440px">
      <el-form label-width="80px">
        <el-form-item :label="passwordTarget?.hasPassword ? '新密码' : '密码'">
          <el-input v-model="passwordForm.value" type="password" show-password placeholder="留空则清除密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordOpen = false">取消</el-button>
        <el-button type="primary" @click="doSetPassword">保存</el-button>
      </template>
    </el-dialog>

    <!-- 输入访问密码 -->
    <el-dialog v-model="askPasswordOpen" :title="`「${askPasswordTarget?.name}」需要访问密码`" width="440px">
      <el-form label-width="80px">
        <el-form-item label="访问密码">
          <el-input v-model="askPasswordValue" type="password" show-password placeholder="请输入访问密码" @keyup.enter="confirmAskPassword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="askPasswordOpen = false">取消</el-button>
        <el-button type="primary" @click="confirmAskPassword">确定</el-button>
      </template>
    </el-dialog>

    <!-- 移动到 -->
    <el-dialog v-model="moveDialogOpen" title="移动到" width="480px">
      <el-breadcrumb class="move-crumbs" separator="/">
        <el-breadcrumb-item><el-link type="primary" @click="moveGoRoot">文件架根目录</el-link></el-breadcrumb-item>
        <el-breadcrumb-item v-for="item in moveBreadcrumbs" :key="item.id">
          <el-link type="primary" @click="moveGoTo(item.id)">{{ item.name }}</el-link>
        </el-breadcrumb-item>
      </el-breadcrumb>
      <div v-loading="moveLoading" class="move-dir-list">
        <div v-if="!moveDirs.length" class="move-dir-empty">当前目录下没有子目录</div>
        <div v-for="dir in moveDirs" :key="dir.id" class="move-dir-item" @click="moveEnter(dir)">
          <el-icon><Folder /></el-icon>
          <span>{{ dir.name }}</span>
        </div>
      </div>
      <template #footer>
        <span class="move-hint">将移动到：{{ moveTargetName }}</span>
        <el-button @click="moveDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="confirmMove">移动到这里</el-button>
      </template>
    </el-dialog>

    <!-- 预览 -->
    <el-dialog v-model="previewOpen" class="preview-dialog" :title="previewTitle" width="80%" top="4vh">
      <div v-if="previewKind === 'image'" class="preview-body preview-image">
        <img :src="previewUrl" alt="预览" />
      </div>
      <iframe v-else-if="previewKind === 'pdf'" :src="previewUrl" class="preview-frame"></iframe>
      <div v-else-if="previewKind === 'md' && previewIsMarkdown" class="preview-body preview-md markdown-body">
        <MarkdownContent :content="previewText" />
      </div>
      <div
        v-else-if="previewKind === 'md' && !previewIsMarkdown"
        class="preview-body preview-md markdown-body"
        v-html="previewHtml"
      ></div>
      <div v-else class="preview-body preview-none">
        <p>该文件类型暂不支持预览，请下载后查看。</p>
        <el-button type="primary" @click="download(previewTarget)">下载文件</el-button>
      </div>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import {
  Delete, Document, Download, EditPen, Expand, Files, Folder, FolderAdd, FolderOpened,
  Grid, Lock, Menu, MoreFilled, Picture, Refresh, Upload, UploadFilled, View, VideoCamera,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { sharedFilesApi } from '@/api'
import MarkdownContent from '@/components/common/MarkdownContent.vue'

const loading = ref(false)
const uploading = ref(false)
const rows = ref<any[]>([])
const query = ref({ keyword: '' })
const fileInput = ref<HTMLInputElement | null>(null)
const currentParentId = ref<number | null>(null)
const breadcrumbs = ref<{ id: number; name: string }[]>([])
const viewMode = ref<'grid' | 'list'>('grid')

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
const previewText = ref('')
const previewIsMarkdown = ref(false)
const previewTitle = ref('')
const previewTarget = ref<any>(null)
let previewObjectUrl = ''

// ===== 选中 =====
const selectedRows = ref<any[]>([])
const selectedFiles = computed(() => selectedRows.value.filter((row) => !row.dir))

function onSelectionChange(selection: any[]) {
  selectedRows.value = selection
}

function isSelected(row: any) {
  return selectedRows.value.some((item) => item.id === row.id)
}

function toggleSelect(row: any) {
  const index = selectedRows.value.findIndex((item) => item.id === row.id)
  if (index >= 0) selectedRows.value.splice(index, 1)
  else selectedRows.value.push(row)
}

function clearSelection() {
  selectedRows.value = []
}

// ===== 移动 =====
const moveDialogOpen = ref(false)
const moveLoading = ref(false)
const moveTargetId = ref<number | null>(null)
const moveBreadcrumbs = ref<{ id: number; name: string }[]>([])
const moveDirs = ref<any[]>([])
const moveTargetName = ref('')

function openMoveDialog() {
  moveTargetId.value = null
  moveTargetName.value = '文件架根目录'
  moveDialogOpen.value = true
  void loadMoveDirs(null)
}

async function loadMoveDirs(parentId: number | null) {
  moveLoading.value = true
  try {
    const items = (await sharedFilesApi.list(parentId)) || []
    moveDirs.value = items.filter((item: any) => item.dir)
    moveBreadcrumbs.value = (await sharedFilesApi.path(parentId)) || []
  } catch {
    ElMessage.error('加载目录失败')
  } finally {
    moveLoading.value = false
  }
}

function moveGoRoot() {
  moveTargetId.value = null
  moveTargetName.value = '文件架根目录'
  void loadMoveDirs(null)
}

function moveGoTo(id: number) {
  moveTargetId.value = id
  moveTargetName.value = moveBreadcrumbs.value.find((item) => item.id === id)?.name || ''
  void loadMoveDirs(id)
}

function moveEnter(dir: any) {
  moveTargetId.value = dir.id
  moveTargetName.value = dir.name
  void loadMoveDirs(dir.id)
}

async function confirmMove() {
  const ids = selectedRows.value.map((row) => row.id)
  if (!ids.length) {
    moveDialogOpen.value = false
    return
  }
  try {
    for (const id of ids) {
      await sharedFilesApi.move(id, moveTargetId.value)
    }
    ElMessage.success('已移动')
    moveDialogOpen.value = false
    clearSelection()
    await fetchList()
  } catch (error: any) {
    ElMessage.error(error?.message || '移动失败')
  }
}

// ===== 批量操作 =====
async function bulkDownload() {
  for (const row of selectedFiles.value) {
    await download(row)
  }
}

async function bulkDelete() {
  try {
    for (const row of selectedRows.value) {
      await sharedFilesApi.remove(row.id)
    }
    ElMessage.success('已删除')
    clearSelection()
    await fetchList()
  } catch {
    ElMessage.error('删除失败')
  }
}

// ===== 拖拽 =====
const dragOverDepth = ref(0)
const draggingExternal = ref(false)
const dropTargetId = ref<number | null>(null)
let draggedRowId: number | null = null
let dragLeaveTimer: number | undefined

function onDragStart(event: DragEvent, row: any) {
  draggedRowId = row.id
  event.dataTransfer?.setData('text/plain', String(row.id))
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}

function onDragEnd() {
  draggedRowId = null
  dropTargetId.value = null
}

function dragEnterZone(event: DragEvent) {
  window.clearTimeout(dragLeaveTimer)
  if (event.dataTransfer) {
    draggingExternal.value = Array.from(event.dataTransfer.types).includes('Files')
  }
  dragOverDepth.value += 1
}

function dragLeaveZone() {
  dragOverDepth.value = Math.max(0, dragOverDepth.value - 1)
  window.clearTimeout(dragLeaveTimer)
  dragLeaveTimer = window.setTimeout(() => {
    dragOverDepth.value = 0
  }, 80)
}

async function onDropZone(event: DragEvent) {
  dragOverDepth.value = 0
  const files = event.dataTransfer?.files
  if (files && files.length) {
    for (const file of Array.from(files)) {
      await uploadFile(file)
    }
    return
  }
  if (draggedRowId != null) {
    await moveItem(draggedRowId, currentParentId.value)
    draggedRowId = null
  }
}

async function onDropOnRow(_event: DragEvent, row: any) {
  dragOverDepth.value = 0
  dropTargetId.value = null
  const files = _event.dataTransfer?.files
  if (files && files.length) {
    for (const file of Array.from(files)) {
      await uploadFile(file)
    }
    return
  }
  if (draggedRowId != null && row.dir && draggedRowId !== row.id) {
    await moveItem(draggedRowId, row.id)
    draggedRowId = null
  }
}

async function moveItem(id: number, targetParentId: number | null) {
  try {
    await sharedFilesApi.move(id, targetParentId)
    ElMessage.success('已移动')
    await fetchList()
  } catch (error: any) {
    ElMessage.error(error?.message || '移动失败')
  }
}

async function uploadFile(file: File) {
  uploading.value = true
  try {
    await sharedFilesApi.upload(file, currentParentId.value)
    ElMessage.success('上传成功')
    await fetchList()
  } catch (error: any) {
    ElMessage.error(error?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

// ===== 右键菜单 =====
const contextMenu = ref<{ visible: boolean; x: number; y: number; target: any }>({
  visible: false,
  x: 0,
  y: 0,
  target: null,
})

function openContextMenu(event: MouseEvent, row: any) {
  contextMenu.value = {
    visible: true,
    x: Math.min(event.clientX, window.innerWidth - 200),
    y: Math.min(event.clientY, window.innerHeight - 320),
    target: row,
  }
}

function openCardMenu(event: MouseEvent, row: any) {
  openContextMenu(event, row)
}

function closeContextMenu() {
  contextMenu.value.visible = false
}

function onRowContextMenu(row: any, column: any, event: MouseEvent) {
  openContextMenu(event, row)
}

function menuOpen() {
  const target = contextMenu.value.target
  if (target) openRow(target)
  closeContextMenu()
}

function menuPreview() {
  const target = contextMenu.value.target
  if (target) preview(target)
  closeContextMenu()
}

function menuEdit() {
  const target = contextMenu.value.target
  if (target) edit(target)
  closeContextMenu()
}

function menuDownload() {
  const target = contextMenu.value.target
  if (target) download(target)
  closeContextMenu()
}

function menuMove() {
  const target = contextMenu.value.target
  selectedRows.value = target ? [target] : []
  closeContextMenu()
  openMoveDialog()
}

function menuRename() {
  const target = contextMenu.value.target
  if (target) openRename(target)
  closeContextMenu()
}

function menuPassword() {
  const target = contextMenu.value.target
  if (target) openSetPassword(target)
  closeContextMenu()
}

function menuDelete() {
  const target = contextMenu.value.target
  closeContextMenu()
  if (target) {
    void remove(target)
  }
}

function menuCreateDir() {
  closeContextMenu()
  openCreateDir()
}

function menuUpload() {
  closeContextMenu()
  fileInput.value?.click()
}

function menuRefresh() {
  closeContextMenu()
  void fetchList()
}

// ===== 数据加载 =====
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

const filteredRows = computed(() => {
  const keyword = query.value.keyword.trim().toLowerCase()
  if (!keyword) return rows.value
  return rows.value.filter((row) => row.name?.toLowerCase().includes(keyword))
})

// ===== 表单操作 =====
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
  await uploadFile(file)
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

// ===== 预览/编辑/下载 =====
function openRow(row: any) {
  if (row.dir) {
    enterDir(row)
  } else {
    preview(row)
  }
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
    previewIsMarkdown.value = row.extension === 'md' || row.extension === 'markdown'
    previewText.value = text
    previewHtml.value = previewIsMarkdown.value
      ? ''
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

// ===== 展示工具 =====
const IMAGE_EXTS = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp', 'ico', 'avif']
const TEXT_EXTS = ['md', 'markdown', 'txt', 'text']
const OFFICE_EXTS = ['docx', 'doc', 'odt', 'rtf', 'txt', 'xlsx', 'xls', 'ods', 'csv', 'pptx', 'ppt', 'odp']

function isImage(row: any) { return IMAGE_EXTS.includes(row.extension) }
function isText(row: any) { return TEXT_EXTS.includes(row.extension) }
function isOffice(row: any) { return OFFICE_EXTS.includes(row.extension) }
function isPdf(row: any) { return row.extension === 'pdf' }

function iconOf(row: any) {
  if (row.dir) return Folder
  if (['xlsx', 'xls', 'ods'].includes(row.extension)) return Grid
  if (['pptx', 'ppt', 'odp'].includes(row.extension)) return VideoCamera
  if (isImage(row)) return Picture
  if (row.extension === 'pdf') return Document
  return Files
}

function toneOf(row: any) {
  if (row.dir) return 'dir'
  if (['docx', 'doc', 'odt', 'rtf'].includes(row.extension)) return 'word'
  if (['xlsx', 'xls', 'ods', 'csv'].includes(row.extension)) return 'cell'
  if (['pptx', 'ppt', 'odp'].includes(row.extension)) return 'slide'
  if (isImage(row)) return 'image'
  if (row.extension === 'pdf') return 'pdf'
  return 'file'
}

function typeName(extension: string) {
  const map: Record<string, string> = {
    docx: 'Word', doc: 'Word', odt: 'Word', rtf: 'Word',
    xlsx: 'Excel', xls: 'Excel', ods: 'Excel', csv: 'CSV',
    pptx: 'PPT', ppt: 'PPT', odp: 'PPT',
    pdf: 'PDF', md: 'Markdown',
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
.crumbs-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  min-height: 32px;
}

.selection-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px 5px 14px;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 999px;
  background: var(--el-color-primary-light-9);
  font-size: 13px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.drop-zone {
  position: relative;
  min-width: 0;
  max-width: 100%;
}

.drop-overlay {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 10px;
  border: 2px dashed var(--el-color-primary);
  border-radius: 14px;
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 90%, transparent);
  backdrop-filter: blur(2px);
  pointer-events: none;
}

.drop-overlay p {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

/* ===== 网格视图 ===== */
.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(156px, 1fr));
  gap: 14px;
  min-height: 200px;
}

.grid-empty {
  grid-column: 1 / -1;
}

.file-card {
  position: relative;
  padding: 18px 12px 12px;
  display: grid;
  justify-items: center;
  gap: 6px;
  border: 1px solid var(--oa-border);
  border-radius: 14px;
  background: #fff;
  cursor: default;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.file-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08);
  border-color: var(--el-color-primary-light-5);
}

.file-card.selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.file-card.droppable {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 3px var(--el-color-primary-light-7);
}

.file-card__check {
  position: absolute;
  top: 8px;
  left: 8px;
  opacity: 0;
  transition: opacity 0.12s ease;
}

.file-card:hover .file-card__check,
.file-card.selected .file-card__check {
  opacity: 1;
}

.file-card__icon {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  font-size: 26px;
}

.tone-dir { color: #0a4b78; background: #e8f1f8; }
.tone-word { color: #1d5f9e; background: #eaf1fb; }
.tone-cell { color: #16794f; background: #e9f7ef; }
.tone-slide { color: #b35900; background: #fdf2e4; }
.tone-image { color: #7c3aed; background: #f3edfd; }
.tone-pdf { color: #c0392b; background: #fdeceb; }
.tone-file { color: #555; background: var(--oa-primary-soft); }

.file-card__name {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-card__name .lock {
  flex: 0 0 auto;
  color: #f0a020;
}

.file-card__meta {
  color: var(--oa-muted);
  font-size: 11px;
}

.file-card__actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.12s ease;
}

.file-card:hover .file-card__actions {
  opacity: 1;
}

/* ===== 列表视图 ===== */
.file-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
}

.file-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 10px;
  color: var(--oa-primary);
  background: var(--oa-primary-soft);
  border: 1px solid var(--oa-border);
  font-size: 18px;
}

.file-copy {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 2px;
}

.file-copy strong,
.file-copy .el-link {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted-line {
  margin: 0;
  color: var(--oa-muted);
  font-size: 12px;
}

/* ===== 右键菜单 ===== */
.context-menu {
  position: fixed;
  z-index: 3000;
  min-width: 180px;
  padding: 6px;
  border: 1px solid var(--oa-border);
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.14);
}

.context-menu__title {
  padding: 6px 10px 8px;
  overflow: hidden;
  color: var(--oa-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.context-menu button {
  width: 100%;
  min-height: 34px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: 0;
  border-radius: 7px;
  color: var(--oa-text, #1d1d1f);
  background: transparent;
  font: inherit;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.context-menu button:hover {
  background: var(--oa-primary-soft);
}

.context-menu button.danger {
  color: var(--el-color-danger);
}

.context-menu__sep {
  height: 1px;
  margin: 5px 8px;
  background: var(--oa-border);
}

/* ===== 移动对话框 ===== */
.move-crumbs {
  margin-bottom: 12px;
}

.move-dir-list {
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid var(--oa-border);
  border-radius: 10px;
  padding: 4px;
}

.move-dir-item {
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border-radius: 8px;
  cursor: pointer;
}

.move-dir-item:hover {
  background: var(--oa-primary-soft);
}

.move-dir-empty {
  padding: 24px;
  color: var(--oa-muted);
  font-size: 13px;
  text-align: center;
}

.move-hint {
  margin-right: auto;
  color: var(--oa-muted);
  font-size: 13px;
}

/* ===== 预览 ===== */
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
