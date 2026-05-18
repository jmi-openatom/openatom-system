<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-model="query.status"
          clearable
          placeholder="活动状态"
          style="width: 150px"
          @change="fetchList"
        >
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
          <el-option label="已关闭" value="closed" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增活动</el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="title" label="活动" min-width="220">
        <template #default="{ row }">
          <strong>{{ row.title }}</strong>
          <p class="muted-line">{{ row.summary || '暂无摘要' }}</p>
        </template>
      </el-table-column>
      <el-table-column prop="activityAt" label="开始时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.activityAt) }}</template>
      </el-table-column>
      <el-table-column prop="location" label="地点" width="150" />
      <el-table-column prop="registrationRequired" label="报名" width="110">
        <template #default="{ row }">
          <el-tag :type="row.registrationRequired ? 'success' : 'info'">
            {{ row.registrationRequired ? '需要' : '不需要' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="success" @click="openRegistrations(row)">报名</el-button>
          <el-popconfirm title="确定删除该活动？" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑活动' : '新增活动'" width="920px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="112px">
        <div class="form-grid">
          <el-form-item label="活动标题" prop="title">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="草稿" value="draft" />
              <el-option label="发布" value="published" />
              <el-option label="关闭" value="closed" />
              <el-option label="取消" value="cancelled" />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-input v-model="form.activityAt" type="datetime-local" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-input v-model="form.endAt" type="datetime-local" />
          </el-form-item>
          <el-form-item label="活动地点">
            <el-input v-model="form.location" />
          </el-form-item>
          <el-form-item label="封面URL">
            <el-input v-model="form.coverUrl" />
          </el-form-item>
        </div>
        <el-form-item label="活动摘要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="Markdown介绍">
          <el-input v-model="form.descriptionMarkdown" type="textarea" :rows="10" />
        </el-form-item>
        <el-divider content-position="left">报名设置</el-divider>
        <el-form-item label="官网报名">
          <el-switch
            v-model="form.registrationRequired"
            active-text="需要"
            inactive-text="不需要"
          />
        </el-form-item>
        <div class="form-grid" v-if="form.registrationRequired">
          <el-form-item label="报名开始">
            <el-input v-model="form.registrationStartAt" type="datetime-local" />
          </el-form-item>
          <el-form-item label="报名结束">
            <el-input v-model="form.registrationEndAt" type="datetime-local" />
          </el-form-item>
        </div>
        <el-form-item v-if="form.registrationRequired" label="自定义字段">
          <el-input v-model="registrationFieldsText" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="registrationVisible" title="活动报名记录" width="760px">
      <el-table :data="registrations">
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="formData" label="报名内容" min-width="320" />
        <el-table-column prop="createdAt" label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { activityApi } from '@/api/index.ts'
import { formatDateTime, statusType, toDateTimeInputValue } from '@/utils/format.ts'
import { onMounted, ref } from 'vue'

const defaultFields = [
  { label: '姓名', type: 'text', required: true },
  { label: '联系方式', type: 'text', required: true },
  { label: '备注', type: 'textarea', required: false },
]

const loading = ref(false)

const saving = ref(false)

const dialogVisible = ref(false)

const registrationVisible = ref(false)

const rows = ref<any[]>([])

const registrations = ref<any[]>([])

const query = ref({ status: '' })

const form = ref<Record<string, any>>({})

const registrationFieldsText = ref('')

const rules = ref({
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
})

const formRef = ref<any>()

function statusText(status: any) {
  return (
    { draft: '草稿', published: '已发布', closed: '已关闭', cancelled: '已取消' }[status] ||
    status ||
    '-'
  )
}

async function fetchList() {
  loading.value = true
  try {
    rows.value = (await activityApi.list(query.value)) || []
  } finally {
    loading.value = false
  }
}

function openDialog(row: any) {
  form.value = row
    ? {
        ...row,
        activityAt: toInputTime(row.activityAt),
        endAt: toInputTime(row.endAt),
        registrationStartAt: toInputTime(row.registrationStartAt),
        registrationEndAt: toInputTime(row.registrationEndAt),
      }
    : {
        title: '',
        status: 'draft',
        summary: '',
        descriptionMarkdown: '# 活动介绍\n\n',
        registrationRequired: false,
      }
  registrationFieldsText.value = prettyFields(row?.registrationFields || defaultFields)
  dialogVisible.value = true
}

function save() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    let fields = null
    if (form.value.registrationRequired) {
      try {
        fields = JSON.parse(registrationFieldsText.value || '[]')
      } catch {
        ElMessage.warning('自定义字段必须是 JSON 数组')
        return
      }
    }
    saving.value = true
    try {
      const payload = { ...form.value, registrationFields: fields }
      if (payload.id) await activityApi.update(payload.id, payload)
      else await activityApi.create(payload)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchList()
    } finally {
      saving.value = false
    }
  })
}

async function remove(row: any) {
  await activityApi.remove(row.id)
  ElMessage.success('活动已删除')
  fetchList()
}

async function openRegistrations(row: any) {
  registrations.value = await activityApi.registrations(row.id)
  registrationVisible.value = true
}

function toInputTime(value: any) {
  return toDateTimeInputValue(value)
}

function prettyFields(value: any) {
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value || '[]') : value
    return JSON.stringify(parsed || [], null, 2)
  } catch {
    return JSON.stringify(defaultFields, null, 2)
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.muted-line {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.5;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
