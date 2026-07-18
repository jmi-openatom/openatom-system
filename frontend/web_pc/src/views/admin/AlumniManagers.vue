<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索姓名"
          style="width: 200px"
          @keyup.enter="fetchList"
        />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px">
          <el-option label="退出" value="left" />
          <el-option label="毕业" value="graduated" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
      </div>
      <div class="toolbar__actions">
        <el-button :icon="Files" @click="$router.push('/admin/groups?type=alumni')">分组管理</el-button>
        <el-button type="primary" :icon="Plus" @click="openDialog">新增往届管理人员</el-button>
      </div>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="姓名" min-width="120">
        <template #default="{ row }">{{ row.realName || row.userName || `用户 ${row.userId}` }}</template>
      </el-table-column>
      <el-table-column prop="departmentName" label="部门" min-width="120" />
      <el-table-column prop="positionName" label="岗位" min-width="140" />
      <el-table-column prop="alumniGroup" label="分组" min-width="120">
        <template #default="{ row }">
          <el-tag v-if="row.alumniGroup" size="small">{{ row.alumniGroup }}</el-tag>
          <span v-else style="color: var(--oa-muted)">未分组</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'graduated' ? 'success' : 'info'">
            {{ membershipStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="在任时间" min-width="160">
        <template #default="{ row }">
          <span>{{ formatDate(row.joinedAt) }}</span>
          <span v-if="row.leftAt"> ~ {{ formatDate(row.leftAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="featured" label="官网展示" width="100">
        <template #default="{ row }">
          <el-switch v-model="row.featured" @change="toggleFeatured(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="120">
        <template #default="{ row }">
          <el-input-number v-model="row.sortOrder" :min="0" size="small" @change="updateDisplay(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="removeAlumni(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增对话框 -->
    <el-dialog v-model="dialogVisible" title="新增往届管理人员" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户ID">
          <el-input-number v-model="form.userId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select
            v-model="form.departmentId"
            clearable
            filterable
            placeholder="选择部门"
            @change="form.positionId = undefined"
          >
            <el-option
              v-for="item in departments"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.positionId" clearable filterable placeholder="选择岗位">
            <el-option
              v-for="item in filteredPositions(form.departmentId)"
              :key="item.id"
              :label="positionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="离社状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="退出" value="left" />
            <el-option label="毕业" value="graduated" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="form.alumniGroup" clearable filterable placeholder="选择分组" style="width: 100%">
            <el-option v-for="g in alumniGroups" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="官网展示">
          <el-switch v-model="form.featured" />
        </el-form-item>
        <el-form-item label="展示排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createAlumni">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑往届管理人员" width="520px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="成员">
          <el-input :model-value="editForm.memberName" disabled />
        </el-form-item>
        <el-form-item label="部门">
          <el-select
            v-model="editForm.departmentId"
            clearable
            filterable
            placeholder="选择部门"
            @change="editForm.positionId = undefined"
          >
            <el-option
              v-for="item in departments"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="editForm.positionId" clearable filterable placeholder="选择岗位">
            <el-option
              v-for="item in filteredPositions(editForm.departmentId)"
              :key="item.id"
              :label="positionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="退出" value="left" />
            <el-option label="毕业" value="graduated" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="editForm.alumniGroup" clearable filterable placeholder="选择分组" style="width: 100%">
            <el-option v-for="g in alumniGroups" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="官网展示">
          <el-switch v-model="editForm.featured" />
        </el-form-item>
        <el-form-item label="展示排序">
          <el-input-number v-model="editForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Files, Plus, Search } from '@element-plus/icons-vue'
import { clubApi, membershipApi, alumniGroupApi } from '@/api'
import { membershipStatusText } from '@/utils/format.ts'
import { onMounted, ref } from 'vue'

const loading = ref(false)

const rows = ref<any[]>([])

const clubs = ref<any[]>([])

const departments = ref<any[]>([])

const positions = ref<any[]>([])

const currentClubId = ref<number | undefined>(undefined)

const query = ref({ keyword: '', status: '' })

const dialogVisible = ref(false)

const editDialogVisible = ref(false)

const form = ref({
  userId: undefined as number | undefined,
  clubId: undefined as number | undefined,
  departmentId: undefined as number | undefined,
  positionId: undefined as number | undefined,
  status: 'graduated',
  alumniGroup: '',
  featured: true,
  sortOrder: 0,
})

const editForm = ref({
  membershipId: undefined as number | undefined,
  memberName: '',
  departmentId: undefined as number | undefined,
  positionId: undefined as number | undefined,
  status: 'graduated',
  alumniGroup: '',
  featured: false,
  sortOrder: 0,
})

const alumniGroups = ref<string[]>([])

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { clubId: currentClubId.value }
    if (query.value.keyword) params.keyword = query.value.keyword
    if (query.value.status) params.status = query.value.status
    const result = await membershipApi.list(params)
    const allRows = result?.list || result || []
    // 只显示有岗位且状态为 left/graduated 的成员
    rows.value = allRows.filter(
      (r: any) =>
        r.positionName &&
        (r.status === 'left' || r.status === 'graduated'),
    )
    await loadAlumniGroups()
  } finally {
    loading.value = false
  }
}

async function loadAlumniGroups() {
  if (!currentClubId.value) return
  try {
    const groups = await alumniGroupApi.list(currentClubId.value)
    alumniGroups.value = (groups || []).map((g: any) => g.name)
  } catch {
    alumniGroups.value = []
  }
}

async function loadOptions() {
  const clubResult = await clubApi.list({})
  clubs.value = clubResult?.list || clubResult || []
  const defaultClub = clubs.value.find((item) => item.code === 'JMI-OPENATOM') || clubs.value[0]
  currentClubId.value = defaultClub?.id
  if (!currentClubId.value) return
  departments.value = await clubApi.departments(currentClubId.value)
  positions.value = await clubApi.positions(currentClubId.value)
}

function openDialog() {
  form.value = {
    userId: undefined,
    clubId: currentClubId.value,
    departmentId: undefined,
    positionId: undefined,
    status: 'graduated',
    alumniGroup: '',
    featured: true,
    sortOrder: 0,
  }
  dialogVisible.value = true
}

async function createAlumni() {
  if (!form.value.userId) {
    ElMessage.warning('请填写用户ID')
    return
  }
  if (!form.value.positionId) {
    ElMessage.warning('请分配岗位')
    return
  }
  await membershipApi.create(form.value)
  ElMessage.success('往届管理人员已添加')
  dialogVisible.value = false
  fetchList()
}

function openEditDialog(row: any) {
  editForm.value = {
    membershipId: row.id,
    memberName: row.realName || row.userName || `用户 ${row.userId}`,
    departmentId: row.departmentId,
    positionId: row.positionId,
    status: row.status,
    alumniGroup: row.alumniGroup || '',
    featured: row.featured,
    sortOrder: row.sortOrder || 0,
  }
  editDialogVisible.value = true
}

async function submitEdit() {
  await membershipApi.update(editForm.value.membershipId!, {
    departmentId: editForm.value.departmentId,
    positionId: editForm.value.positionId,
    status: editForm.value.status,
    alumniGroup: editForm.value.alumniGroup || null,
    featured: editForm.value.featured,
    sortOrder: editForm.value.sortOrder,
  })
  ElMessage.success('已更新')
  editDialogVisible.value = false
  fetchList()
}

async function removeAlumni(row: any) {
  await ElMessageBox.confirm(
    `确认将 ${row.realName || row.userName || `用户 ${row.userId}`} 从往届管理人员中移除？`,
    '提示',
    { type: 'warning' },
  )
  await membershipApi.remove(row.id)
  ElMessage.success('已删除')
  fetchList()
}

async function toggleFeatured(row: any) {
  await updateDisplay(row)
}

async function updateDisplay(row: any) {
  await membershipApi.update(row.id, { featured: row.featured, sortOrder: row.sortOrder || 0 })
  ElMessage.success('展示信息已更新')
}

function filteredPositions(departmentId: any) {
  if (!departmentId) return positions.value
  return positions.value.filter((item) => item.departmentId === departmentId)
}

function positionLabel(position: any) {
  const department = departments.value.find((item) => item.id === position.departmentId)
  return department ? `${department.name} / ${position.name}` : position.name
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(async () => {
  await loadOptions()
  fetchList()
})
</script>
