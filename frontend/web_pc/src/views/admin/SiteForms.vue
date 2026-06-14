<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-if="clubs.length > 1"
          v-model="selectedClubId"
          filterable
          placeholder="选择社团"
          style="width: 240px"
          @change="fetchList"
        >
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
        </el-select>
        <el-select
          v-model="query.status"
          clearable
          placeholder="表单状态"
          style="width: 150px"
          @change="fetchList"
        >
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
          <el-option label="收集中" value="open" />
          <el-option label="已结束" value="closed" />
          <el-option label="已归档" value="archived" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增表单</el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="name" label="表单名称" min-width="220" />
      <el-table-column label="收集时间" min-width="230">
        <template #default="{ row }">{{ formatRange(row.startAt, row.endAt) }}</template>
      </el-table-column>
      <el-table-column prop="loginRequired" label="登录" width="110">
        <template #default="{ row }">
          <el-tag :type="row.loginRequired ? 'success' : 'warning'">{{
            row.loginRequired ? '需要' : '可匿名'
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="success" @click="openSubmissions(row)">记录</el-button>
          <el-button link type="info" @click="previewSchema(row)">预览字段</el-button>
          <el-button link type="warning" @click="copyFormLink(row)">复制链接</el-button>
          <el-button link type="success" @click="publish(row)" v-if="row.status !== 'open'"
            >发布</el-button
          >
          <el-button
            link
            type="danger"
            @click="closeSiteForm(row)"
            v-if="row.status === 'open' || row.status === 'published'"
            >结束
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="当前社团暂无信息收集表单" />

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑表单' : '新增表单'"
      width="920px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="112px">
        <div class="form-grid">
          <el-form-item label="所属社团" prop="clubId">
            <el-select v-model="form.clubId" filterable placeholder="请选择社团">
              <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="表单状态">
            <el-select v-model="form.status">
              <el-option label="草稿" value="draft" />
              <el-option label="已发布" value="published" />
              <el-option label="收集中" value="open" />
              <el-option label="已结束" value="closed" />
              <el-option label="已归档" value="archived" />
            </el-select>
          </el-form-item>
          <el-form-item label="表单名称" prop="name">
            <el-input v-model="form.name" placeholder="如：报名信息收集、活动意向登记" />
          </el-form-item>
          <el-form-item label="开始时间" prop="startAt">
            <el-input v-model="form.startAt" type="datetime-local" />
          </el-form-item>
          <el-form-item label="结束时间" prop="endAt">
            <el-input v-model="form.endAt" type="datetime-local" />
          </el-form-item>
          <el-form-item label="需先登录">
            <el-switch
              v-model="form.loginRequired"
              active-text="需要登录"
              inactive-text="匿名可填"
            />
          </el-form-item>
        </div>

        <el-divider content-position="left">字段设计</el-divider>
        <div class="designer-toolbar">
          <el-button size="small" @click="addField('text')">新增单行输入</el-button>
          <el-button size="small" @click="addField('textarea')">新增多行输入</el-button>
          <el-button size="small" @click="addField('select')">新增下拉选择</el-button>
          <el-button size="small" type="warning" plain @click="useDefaultFields"
            >恢复默认字段</el-button
          >
        </div>
        <el-alert
          type="info"
          show-icon
          :closable="false"
          title="支持拖拽排序、切换必填、配置下拉选项；字段 Key 建议使用英文字母，提交数据会按 Key 保存。"
        />
        <div v-if="schemaFields.length" class="designer-list">
          <div
            v-for="(field, index) in schemaFields"
            :key="field.uid"
            class="designer-card"
            draggable="true"
            @dragstart="handleDragStart(index)"
            @dragend="handleDragEnd"
            @dragover.prevent
            @drop="handleDrop(index)"
          >
            <div class="designer-card__header">
              <div class="designer-card__title">
                <span class="designer-card__drag">::</span>
                <strong>{{ index + 1 }}. {{ field.label || '未命名字段' }}</strong>
              </div>
              <div class="designer-card__actions">
                <el-tag size="small">{{ typeText(field.type) }}</el-tag>
                <el-button link type="danger" @click="removeField(index)">删除</el-button>
              </div>
            </div>
            <div class="designer-card__grid">
              <el-form-item label="字段名称" label-width="84px" required>
                <el-input
                  v-model="field.label"
                  placeholder="如：申请理由"
                  @blur="fillFieldKey(field)"
                />
              </el-form-item>
              <el-form-item label="字段 Key" label-width="84px" required>
                <el-input v-model="field.key" placeholder="如：reason" />
              </el-form-item>
              <el-form-item label="字段类型" label-width="84px">
                <el-select v-model="field.type" @change="handleTypeChange(field)">
                  <el-option
                    v-for="item in fieldTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="是否必填" label-width="84px">
                <el-switch v-model="field.required" />
              </el-form-item>
            </div>
            <el-form-item label="占位提示" label-width="84px">
              <el-input v-model="field.placeholder" placeholder="如：请输入申请理由" />
            </el-form-item>
            <div v-if="field.type === 'select'" class="designer-options">
              <div class="designer-options__header">
                <strong>下拉选项</strong>
                <el-button size="small" @click="addOption(field)">新增选项</el-button>
              </div>
              <div v-if="field.options.length" class="designer-options__list">
                <div
                  v-for="(option, optionIndex) in field.options"
                  :key="option.uid"
                  class="designer-option-row"
                >
                  <el-input v-model="option.label" placeholder="显示文案" />
                  <el-input v-model="option.value" placeholder="提交值" />
                  <el-button link type="danger" @click="removeOption(field, optionIndex)"
                    >删除</el-button
                  >
                </div>
              </div>
              <el-empty v-else description="请至少配置一个下拉选项" :image-size="70" />
            </div>
          </div>
        </div>
        <el-empty v-else description="还没有字段，先点击上方按钮新增" :image-size="80" />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="表单字段预览" width="760px">
      <el-table :data="previewFields">
        <el-table-column prop="key" label="字段Key" min-width="140" />
        <el-table-column prop="label" label="字段名称" min-width="140" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="required" label="必填" width="100">
          <template #default="{ row }">
            <el-tag :type="row.required ? 'danger' : 'info'">{{
              row.required ? '是' : '否'
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="选项/提示" min-width="220">
          <template #default="{ row }">
            {{ row.placeholder || formatOptions(row.options) || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { siteFormApi, clubApi } from '@/api'
import { formatDateTime, statusType, toDateTimeInputValue } from '@/utils/format.ts'
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const defaultFormSchema = [
  { key: 'name', label: '姓名', type: 'text', required: true, placeholder: '请输入姓名' },
  { key: 'studentId', label: '学号', type: 'text', required: false, placeholder: '请输入学号' },
]

function toInputTime(value) {
  return toDateTimeInputValue(value)
}

const loading = ref(false)

const saving = ref(false)

const dialogVisible = ref(false)

const previewVisible = ref(false)

const clubs = ref<any[]>([])

const selectedClubId = ref('')

const rows = ref<any[]>([])

const query = ref({ status: '' })

const form = ref<Record<string, any>>({})

const previewFields = ref<any[]>([])

const schemaFields = ref<any[]>([])

const draggedFieldIndex = ref(-1)

const fieldTypeOptions = ref([
  { label: '单行输入', value: 'text' },
  { label: '多行输入', value: 'textarea' },
  { label: '下拉选择', value: 'select' },
])

const rules = ref({
  clubId: [{ required: true, message: '请选择社团', trigger: 'change' }],
  name: [{ required: true, message: '请输入表单名称', trigger: 'blur' }],
  startAt: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endAt: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
})

const formRef = ref<any>()

const router = useRouter()

const route = useRoute()

async function loadClubs() {
  const result = await clubApi.list({ page: 1, pageSize: 100 })
  clubs.value = result?.list || result || []
  if (!selectedClubId.value && clubs.value.length) {
    selectedClubId.value = clubs.value[0].id
  }
  await fetchList()
}

async function fetchList() {
  if (!selectedClubId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const result = await clubApi.siteForms(selectedClubId.value)
    const list = result?.list || result || []
    rows.value = query.value.status
      ? list.filter((item) => item.status === query.value.status)
      : list
  } finally {
    loading.value = false
  }
}

function statusText(status: any) {
  return (
    {
      draft: '草稿',
      published: '已发布',
      open: '收集中',
      closed: '已结束',
      archived: '已归档',
    }[status] ||
    status ||
    '-'
  )
}

function formatRange(startAt: any, endAt: any) {
  return `${formatDateTime(startAt) || '-'} - ${formatDateTime(endAt) || '-'}`
}

function createEmptyForm() {
  return {
    clubId: selectedClubId.value || '',
    name: '',
    status: 'draft',
    loginRequired: true,
    startAt: '',
    endAt: '',
  }
}

function openDialog(row: any) {
  form.value = row
    ? {
        ...row,
        clubId: row.clubId,
        status: row.status || 'draft',
        loginRequired: row.loginRequired !== false,
        startAt: toInputTime(row.startAt),
        endAt: toInputTime(row.endAt),
      }
    : createEmptyForm()
  schemaFields.value = normalizeSchema(row?.formSchema || defaultFormSchema)
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

function handleDialogClosed() {
  draggedFieldIndex.value = -1
  form.value = createEmptyForm()
  schemaFields.value = []
  formRef.value?.clearValidate()
}

async function save() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!validateSchedule()) {
      return
    }
    const formSchema = buildFormSchema()
    if (!formSchema) {
      return
    }
    const payload = {
      id: form.value.id,
      clubId: form.value.clubId,
      name: form.value.name,
      status: form.value.status,
      loginRequired: form.value.loginRequired,
      startAt: form.value.startAt || null,
      endAt: form.value.endAt || null,
      formSchema,
    }

    saving.value = true
    try {
      if (payload.id) {
        await siteFormApi.update(payload.id, payload)
        ElMessage.success('表单已更新')
      } else {
        await siteFormApi.create(payload.clubId, payload)
        ElMessage.success('表单已创建')
      }
      dialogVisible.value = false
      selectedClubId.value = payload.clubId
      await fetchList()
    } finally {
      saving.value = false
    }
  })
}

async function publish(row: any) {
  await siteFormApi.publish(row.id)
  ElMessage.success('表单已发布')
  fetchList()
}

async function closeSiteForm(row: any) {
  await siteFormApi.close(row.id)
  ElMessage.success('表单已结束')
  fetchList()
}

function openSubmissions(row: any) {
  router.push({
    path: '/admin/form-submissions',
    query: { formId: row.id, clubId: row.clubId },
  })
}

async function copyFormLink(row: any) {
  const webLink = `${window.location.origin}${router.resolve({ path: `/forms/${row.id}` }).href}`
  const mpPath = `/pages/forms/index?id=${row.id}`
  const fullText = `Web链接: ${webLink}\n小程序路径: ${mpPath}`
  try {
    await navigator.clipboard.writeText(fullText)
    ElMessage.success('Web链接和小程序路径已复制')
  } catch {
    ElMessage.info(fullText)
  }
}

function previewSchema(row: any) {
  previewFields.value = normalizeSchema(row?.formSchema || defaultFormSchema)
  previewVisible.value = true
}

function addField(type: any) {
  schemaFields.value.push(createField(type))
}

function removeField(index: any) {
  schemaFields.value.splice(index, 1)
}

function useDefaultFields() {
  schemaFields.value = normalizeSchema(defaultFormSchema)
}

function createField(type: any) {
  return {
    uid: nextUid(),
    key: '',
    label: '',
    type,
    required: false,
    placeholder: '',
    options: type === 'select' ? [createOption()] : [],
  }
}

function createOption(option: any = {}) {
  return {
    uid: nextUid(),
    label: option.label || '',
    value: option.value || '',
  }
}

function addOption(field: any) {
  if (!Array.isArray(field.options)) field.options = []
  field.options.push(createOption())
}

function removeOption(field: any, optionIndex: any) {
  field.options.splice(optionIndex, 1)
}

function handleTypeChange(field: any) {
  if (field.type === 'select') {
    field.options = field.options?.length ? field.options : [createOption()]
    return
  }
  field.options = []
}

function handleDragStart(index: any) {
  draggedFieldIndex.value = index
}

function handleDragEnd() {
  draggedFieldIndex.value = -1
}

function handleDrop(index: any) {
  if (draggedFieldIndex.value < 0 || draggedFieldIndex.value === index) return
  const [moved] = schemaFields.value.splice(draggedFieldIndex.value, 1)
  schemaFields.value.splice(index, 0, moved)
  draggedFieldIndex.value = -1
}

function fillFieldKey(field: any) {
  if (field.key) return
  field.key = generateFieldKey(field.label)
}

function generateFieldKey(label: any) {
  const base =
    String(label || '')
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '') || `field_${schemaFields.value.length + 1}`
  let key = base
  let index = 1
  while (schemaFields.value.some((item) => item.key === key)) {
    index += 1
    key = `${base}_${index}`
  }
  return key
}

function buildFormSchema() {
  if (!schemaFields.value.length) {
    ElMessage.warning('请至少配置一个表单字段')
    return null
  }
  const keys = new Set()
  const schema = []
  for (const [index, field] of schemaFields.value.entries()) {
    if (!field.label?.trim()) {
      ElMessage.warning(`第 ${index + 1} 个字段缺少名称`)
      return null
    }
    if (!field.key?.trim()) {
      ElMessage.warning(`第 ${index + 1} 个字段缺少 Key`)
      return null
    }
    if (keys.has(field.key.trim())) {
      ElMessage.warning(`字段 Key 不能重复：${field.key}`)
      return null
    }
    keys.add(field.key.trim())
    if (field.type === 'select') {
      const options = (field.options || [])
        .map((option) => ({
          label: option.label?.trim() || option.value?.trim(),
          value: option.value?.trim() || option.label?.trim(),
        }))
        .filter((option) => option.label && option.value)
      if (!options.length) {
        ElMessage.warning(`字段“${field.label}”至少需要一个下拉选项`)
        return null
      }
      schema.push({
        key: field.key.trim(),
        label: field.label.trim(),
        type: field.type,
        required: Boolean(field.required),
        placeholder: field.placeholder?.trim() || '',
        options,
      })
      continue
    }
    schema.push({
      key: field.key.trim(),
      label: field.label.trim(),
      type: field.type,
      required: Boolean(field.required),
      placeholder: field.placeholder?.trim() || '',
    })
  }
  return schema
}

function validateSchedule() {
  const startAt = parseTimeValue(form.value.startAt)
  const endAt = parseTimeValue(form.value.endAt)

  if (startAt && endAt && startAt >= endAt) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return false
  }
  return true
}

function parseTimeValue(value: any) {
  if (!value) return null
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? null : time
}

function normalizeSchema(value: any) {
  const parsed = parseJsonMaybe(value, defaultFormSchema)
  return (parsed || []).map((field) => ({
    uid: nextUid(),
    key: field.key || '',
    label: field.label || '',
    type: ['text', 'textarea', 'select'].includes(field.type) ? field.type : 'text',
    required: Boolean(field.required),
    placeholder: field.placeholder || '',
    options:
      field.type === 'select'
        ? (field.options || []).map((option) =>
            createOption(typeof option === 'object' ? option : { label: option, value: option }),
          )
        : [],
  }))
}

function typeText(type: any) {
  return { text: '单行输入', textarea: '多行输入', select: '下拉选择' }[type] || type
}

function formatOptions(options: any) {
  if (!Array.isArray(options) || !options.length) return ''
  return options
    .map((item) => (typeof item === 'object' ? item.label || item.value : item))
    .filter(Boolean)
    .join(' / ')
}

function parseJsonMaybe(value: any, fallback: any) {
  if (Array.isArray(value)) return value
  if (!value) return fallback
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

function nextUid() {
  return `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

onMounted(() => {
  loadClubs()
})
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.designer-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.designer-list {
  display: grid;
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  background: var(--oa-border);
}

.designer-card {
  padding: 18px;
  background: var(--oa-elevated-bg);
  border: 0;
  border-radius: 0;
  animation: oaFadeUp 0.34s ease both;
}

.designer-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.designer-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.designer-card__drag {
  color: var(--oa-muted);
  cursor: grab;
  user-select: none;
}

.designer-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.designer-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.designer-options {
  margin-top: 6px;
}

.designer-options__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.designer-options__list {
  display: grid;
  gap: 10px;
}

.designer-option-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .designer-card__grid,
  .designer-option-row {
    grid-template-columns: 1fr;
  }

  .designer-card__header,
  .designer-options__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
