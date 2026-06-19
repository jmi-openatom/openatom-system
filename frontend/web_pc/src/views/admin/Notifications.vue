<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索通知标题/内容"
          style="width: 260px"
          :prefix-icon="Search"
        />
      </div>
      <div class="toolbar__actions">
        <el-button v-if="canCreate" type="primary" :icon="Plus" @click="openDialog()"
          >发布通知</el-button
        >
      </div>
    </ViewToolbar>

    <el-table v-loading="loading" :data="filteredRows" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发布时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-popconfirm title="确定删除此通知吗？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button v-if="canDelete" link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="发布新通知" width="650px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="通知标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="通知类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="系统通知" value="system" />
            <el-option label="活动通知" value="activity" />
            <el-option label="审批通知" value="approval" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收范围" prop="isAll">
          <el-radio-group v-model="form.isAll">
            <el-radio :label="true">全体用户</el-radio>
            <el-radio :label="false">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!form.isAll" label="选择用户" prop="receiverUserIds">
          <el-select
            v-model="form.receiverUserIds"
            multiple
            filterable
            remote
            reserve-keyword
            placeholder="请输入姓名或学号搜索"
            :remote-method="remoteSearchUsers"
            :loading="searchingUsers"
            style="width: 100%"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="`${item.realName} (${item.studentId || item.userName})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="通知内容" prop="content">
          <div class="notification-editor">
            <el-tabs v-model="editorMode">
              <el-tab-pane label="Markdown 编辑" name="write">
                <el-input
                  v-model="form.content"
                  type="textarea"
                  :rows="10"
                  placeholder="# 通知标题&#10;&#10;支持列表、链接、图片、引用和代码块。"
                />
              </el-tab-pane>
              <el-tab-pane label="预览" name="preview">
                <div class="editor-preview">
                  <MarkdownContent v-if="form.content.trim()" :content="form.content" />
                  <el-empty v-else description="输入 Markdown 后可在这里预览" :image-size="72" />
                </div>
              </el-tab-pane>
            </el-tabs>
            <p class="editor-tip">支持 Markdown，用户通知列表会自动生成纯文本摘要。</p>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">立即发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="通知详情" width="600px">
      <div class="notification-detail" v-if="currentNotification">
        <h3>{{ currentNotification.title }}</h3>
        <div class="meta">
          <el-tag :type="typeTag(currentNotification.type)" size="small">
            {{ typeText(currentNotification.type) }}
          </el-tag>
          <span class="time">{{ formatDateTime(currentNotification.createdAt) }}</span>
        </div>
        <el-divider />
        <MarkdownContent :content="currentNotification.content" />
      </div>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import MarkdownContent from '@/components/common/MarkdownContent.vue'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { computed, onMounted, ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { notificationApi, userApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { hasPermission } from '@/utils/permission.ts'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editorMode = ref('write')
const keyword = ref('')
const rows = ref<any[]>([])
const currentNotification = ref<any>(null)
const formRef = ref<any>()

const canCreate = computed(() => hasPermission('notification:create'))
const canDelete = computed(() => hasPermission('notification:delete'))

const form = ref({
  title: '',
  content: '',
  type: 'system',
  isAll: true,
  receiverUserIds: [],
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  receiverUserIds: [
    {
      validator: (rule, value, callback) => {
        if (!form.value.isAll && (!value || value.length === 0)) {
          callback(new Error('请选择至少一个接收用户'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

const filteredRows = computed(() => {
  if (!keyword.value) return rows.value
  const k = keyword.value.toLowerCase()
  return rows.value.filter(
    (r) => r.title.toLowerCase().includes(k) || r.content.toLowerCase().includes(k),
  )
})

const typeTag = (type: string) => {
  const map = {
    system: 'danger',
    activity: 'success',
    approval: 'warning',
    other: 'info',
  }
  return map[type] || 'info'
}

const typeText = (type: string) => {
  const map = {
    system: '系统通知',
    activity: '活动通知',
    approval: '审批通知',
    other: '其他',
  }
  return map[type] || type
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await notificationApi.adminList()
    rows.value = res || []
  } finally {
    loading.value = false
  }
}

const openDialog = () => {
  form.value = {
    title: '',
    content: '',
    type: 'system',
    isAll: true,
    receiverUserIds: [],
  }
  editorMode.value = 'write'
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    await notificationApi.create(form.value)
    ElMessage.success('发布成功')
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await notificationApi.delete(row.id)
    ElMessage.success('已删除')
    fetchList()
  } catch (e) {}
}

const viewDetail = (row) => {
  currentNotification.value = row
  detailVisible.value = true
}

// User selection logic
const searchingUsers = ref(false)
const userOptions = ref([])
const remoteSearchUsers = async (query: string) => {
  if (query) {
    searchingUsers.value = true
    try {
      const res = await userApi.list({ keyword: query, page: 1, pageSize: 20 })
      userOptions.value = res?.list || res || []
    } finally {
      searchingUsers.value = false
    }
  } else {
    userOptions.value = []
  }
}

onMounted(fetchList)
</script>

<style scoped>
.notification-detail h3 {
  margin: 0 0 12px;
  font-size: 18px;
  color: var(--oa-text);
}

.notification-detail .meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notification-detail .time {
  font-size: 13px;
  color: var(--oa-muted);
}

.notification-editor {
  width: 100%;
}

.editor-preview {
  min-height: 230px;
  max-height: 460px;
  padding: 18px;
  border: 1px solid var(--oa-border);
  border-radius: 12px;
  background: var(--oa-elevated-bg);
  overflow: auto;
}

.editor-tip {
  margin: 8px 0 0;
  color: var(--oa-muted);
  font-size: 12px;
}
</style>
