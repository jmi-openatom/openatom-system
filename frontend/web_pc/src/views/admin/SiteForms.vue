<template>
  <div class="admin-page">
    <div class="toolbar">
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
    </div>

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
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { siteFormApi, clubApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'

const defaultFormSchema = [
  { key: 'name', label: '姓名', type: 'text', required: true, placeholder: '请输入姓名' },
  { key: 'studentId', label: '学号', type: 'text', required: false, placeholder: '请输入学号' },
]

function toInputTime(value) {
  if (!value) return ''
  const date = new Date(value)
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

export default {
  name: 'AdminSiteForms',
  data() {
    return {
      Plus,
      Refresh,
      loading: false,
      saving: false,
      dialogVisible: false,
      previewVisible: false,
      clubs: [],
      selectedClubId: '',
      rows: [],
      query: { status: '' },
      form: {},
      previewFields: [],
      schemaFields: [],
      draggedFieldIndex: -1,
      fieldTypeOptions: [
        { label: '单行输入', value: 'text' },
        { label: '多行输入', value: 'textarea' },
        { label: '下拉选择', value: 'select' },
      ],
      rules: {
        clubId: [{ required: true, message: '请选择社团', trigger: 'change' }],
        name: [{ required: true, message: '请输入表单名称', trigger: 'blur' }],
        startAt: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
        endAt: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
      },
    }
  },
  created() {
    this.loadClubs()
  },
  methods: {
    statusType,
    async loadClubs() {
      const result = await clubApi.list({ page: 1, pageSize: 100 })
      this.clubs = result?.list || result || []
      if (!this.selectedClubId && this.clubs.length) {
        this.selectedClubId = this.clubs[0].id
      }
      await this.fetchList()
    },
    async fetchList() {
      if (!this.selectedClubId) {
        this.rows = []
        return
      }
      this.loading = true
      try {
        const result = await clubApi.siteForms(this.selectedClubId)
        const list = result?.list || result || []
        this.rows = this.query.status
          ? list.filter((item) => item.status === this.query.status)
          : list
      } finally {
        this.loading = false
      }
    },
    statusText(status) {
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
    },
    formatRange(startAt, endAt) {
      return `${formatDateTime(startAt) || '-'} - ${formatDateTime(endAt) || '-'}`
    },
    createEmptyForm() {
      return {
        clubId: this.selectedClubId || '',
        name: '',
        status: 'draft',
        loginRequired: true,
        startAt: '',
        endAt: '',
      }
    },
    openDialog(row) {
      this.form = row
        ? {
            ...row,
            clubId: row.clubId,
            status: row.status || 'draft',
            loginRequired: row.loginRequired !== false,
            startAt: toInputTime(row.startAt),
            endAt: toInputTime(row.endAt),
          }
        : this.createEmptyForm()
      this.schemaFields = this.normalizeSchema(row?.formSchema || defaultFormSchema)
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.formRef?.clearValidate()
      })
    },
    handleDialogClosed() {
      this.draggedFieldIndex = -1
      this.form = this.createEmptyForm()
      this.schemaFields = []
      this.$refs.formRef?.clearValidate()
    },
    async save() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        if (!this.validateSchedule()) {
          return
        }
        const formSchema = this.buildFormSchema()
        if (!formSchema) {
          return
        }
        const payload = {
          id: this.form.id,
          clubId: this.form.clubId,
          name: this.form.name,
          status: this.form.status,
          loginRequired: this.form.loginRequired,
          startAt: this.form.startAt || null,
          endAt: this.form.endAt || null,
          formSchema,
        }

        this.saving = true
        try {
          if (payload.id) {
            await siteFormApi.update(payload.id, payload)
            ElMessage.success('表单已更新')
          } else {
            await siteFormApi.create(payload.clubId, payload)
            ElMessage.success('表单已创建')
          }
          this.dialogVisible = false
          this.selectedClubId = payload.clubId
          await this.fetchList()
        } finally {
          this.saving = false
        }
      })
    },
    async publish(row) {
      await siteFormApi.publish(row.id)
      ElMessage.success('表单已发布')
      this.fetchList()
    },
    async closeSiteForm(row) {
      await siteFormApi.close(row.id)
      ElMessage.success('表单已结束')
      this.fetchList()
    },
    openSubmissions(row) {
      this.$router.push({
        path: '/admin/form-submissions',
        query: { formId: row.id, clubId: row.clubId },
      })
    },
    async copyFormLink(row) {
      const webLink = `${window.location.origin}${this.$router.resolve({ path: `/forms/${row.id}` }).href}`
      const mpPath = `/pages/forms/index?id=${row.id}`
      const fullText = `Web链接: ${webLink}\n小程序路径: ${mpPath}`
      try {
        await navigator.clipboard.writeText(fullText)
        ElMessage.success('Web链接和小程序路径已复制')
      } catch {
        ElMessage.info(fullText)
      }
    },
    previewSchema(row) {
      this.previewFields = this.normalizeSchema(row?.formSchema || defaultFormSchema)
      this.previewVisible = true
    },
    addField(type) {
      this.schemaFields.push(this.createField(type))
    },
    removeField(index) {
      this.schemaFields.splice(index, 1)
    },
    useDefaultFields() {
      this.schemaFields = this.normalizeSchema(defaultFormSchema)
    },
    createField(type = 'text') {
      return {
        uid: this.nextUid(),
        key: '',
        label: '',
        type,
        required: false,
        placeholder: '',
        options: type === 'select' ? [this.createOption()] : [],
      }
    },
    createOption(option = {}) {
      return {
        uid: this.nextUid(),
        label: option.label || '',
        value: option.value || '',
      }
    },
    addOption(field) {
      field.options.push(this.createOption())
    },
    removeOption(field, optionIndex) {
      field.options.splice(optionIndex, 1)
    },
    handleTypeChange(field) {
      if (field.type === 'select') {
        field.options = field.options?.length ? field.options : [this.createOption()]
        return
      }
      field.options = []
    },
    handleDragStart(index) {
      this.draggedFieldIndex = index
    },
    handleDragEnd() {
      this.draggedFieldIndex = -1
    },
    handleDrop(index) {
      if (this.draggedFieldIndex < 0 || this.draggedFieldIndex === index) return
      const [moved] = this.schemaFields.splice(this.draggedFieldIndex, 1)
      this.schemaFields.splice(index, 0, moved)
      this.draggedFieldIndex = -1
    },
    fillFieldKey(field) {
      if (field.key) return
      field.key = this.generateFieldKey(field.label)
    },
    generateFieldKey(label) {
      const base =
        String(label || '')
          .trim()
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, '_')
          .replace(/^_+|_+$/g, '') || `field_${this.schemaFields.length + 1}`
      let key = base
      let index = 1
      while (this.schemaFields.some((item) => item.key === key)) {
        index += 1
        key = `${base}_${index}`
      }
      return key
    },
    buildFormSchema() {
      if (!this.schemaFields.length) {
        ElMessage.warning('请至少配置一个表单字段')
        return null
      }
      const keys = new Set()
      const schema = []
      for (const [index, field] of this.schemaFields.entries()) {
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
    },
    validateSchedule() {
      const applyStart = this.parseTimeValue(this.form.applyStartAt)
      const applyEnd = this.parseTimeValue(this.form.applyEndAt)
      const interviewStart = this.parseTimeValue(this.form.interviewStartAt)
      const interviewEnd = this.parseTimeValue(this.form.interviewEndAt)
      const resultPublishAt = this.parseTimeValue(this.form.resultPublishAt)

      if (applyStart && applyEnd && applyStart >= applyEnd) {
        ElMessage.warning('结束时间必须晚于开始时间')
        return false
      }
      if ((interviewStart && !interviewEnd) || (!interviewStart && interviewEnd)) {
        ElMessage.warning('请完整填写面试开始和结束时间')
        return false
      }
      if (interviewStart && interviewEnd && interviewStart >= interviewEnd) {
        ElMessage.warning('面试结束时间必须晚于面试开始时间')
        return false
      }
      if (applyEnd && interviewStart && interviewStart < applyEnd) {
        ElMessage.warning('面试开始时间不能早于表单结束时间')
        return false
      }
      if (interviewEnd && resultPublishAt && resultPublishAt < interviewEnd) {
        ElMessage.warning('结果公布时间不能早于面试结束时间')
        return false
      }
      return true
    },
    parseTimeValue(value) {
      if (!value) return null
      const time = new Date(value).getTime()
      return Number.isNaN(time) ? null : time
    },
    normalizeSchema(value) {
      const parsed = this.parseJsonMaybe(value, defaultFormSchema)
      return (parsed || []).map((field) => ({
        uid: this.nextUid(),
        key: field.key || '',
        label: field.label || '',
        type: ['text', 'textarea', 'select'].includes(field.type) ? field.type : 'text',
        required: Boolean(field.required),
        placeholder: field.placeholder || '',
        options:
          field.type === 'select'
            ? (field.options || []).map((option) =>
                this.createOption(
                  typeof option === 'object' ? option : { label: option, value: option },
                ),
              )
            : [],
      }))
    },
    typeText(type) {
      return { text: '单行输入', textarea: '多行输入', select: '下拉选择' }[type] || type
    },
    formatOptions(options) {
      if (!Array.isArray(options) || !options.length) return ''
      return options
        .map((item) => (typeof item === 'object' ? item.label || item.value : item))
        .filter(Boolean)
        .join(' / ')
    },
    parseJsonMaybe(value, fallback) {
      if (Array.isArray(value)) return value
      if (!value) return fallback
      try {
        return JSON.parse(value)
      } catch {
        return fallback
      }
    },
    nextUid() {
      return `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    },
  },
}
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
  gap: 16px;
}

.designer-card {
  padding: 16px;
  background: #fafafa;
  border: 1px solid var(--oa-border);
  border-radius: 10px;
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
