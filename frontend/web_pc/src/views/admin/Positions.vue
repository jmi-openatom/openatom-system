<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-model="selectedClubId"
          filterable
          placeholder="选择社团"
          style="width: 240px"
          @change="handleClubChange"
        >
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
        </el-select>
        <el-select
          v-model="query.departmentId"
          clearable
          filterable
          placeholder="选择部门"
          style="width: 220px"
          @change="fetchList"
        >
          <el-option
            v-for="department in departments"
            :key="department.id"
            :label="department.name"
            :value="department.id"
          />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增岗位</el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="所属部门" min-width="160">
        <template #default="{ row }">{{ departmentName(row.departmentId) }}</template>
      </el-table-column>
      <el-table-column prop="name" label="岗位名称" min-width="160" />
      <el-table-column prop="maxCount" label="人数上限" width="110">
        <template #default="{ row }">{{ row.maxCount || '-' }}</template>
      </el-table-column>
      <el-table-column label="关联角色" min-width="220">
        <template #default="{ row }">
          <span>{{ roleNames(row.roleIds).join(' / ') || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除该岗位？" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="当前社团暂无岗位" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑岗位' : '新增岗位'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属社团" prop="clubId">
          <el-select
            v-model="form.clubId"
            filterable
            placeholder="请选择社团"
            @change="loadDepartments"
          >
            <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select
            v-model="form.departmentId"
            clearable
            filterable
            placeholder="不选则为社团通用岗位"
          >
            <el-option
              v-for="department in dialogDepartments"
              :key="department.id"
              :label="department.name"
              :value="department.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="form.maxCount" :min="1" />
        </el-form-item>
        <el-form-item label="关联角色">
          <el-select
            v-model="form.roleIds"
            multiple
            clearable
            filterable
            placeholder="可选，给岗位绑定基础角色"
          >
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.name || role.code"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { clubApi, positionApi, rbacApi } from '@/api'
import { onMounted, ref } from 'vue'

const loading = ref(false)

const saving = ref(false)

const dialogVisible = ref(false)

const clubs = ref<any[]>([])

const departments = ref<any[]>([])

const dialogDepartments = ref<any[]>([])

const roles = ref<any[]>([])

const selectedClubId = ref('')

const query = ref({ departmentId: undefined })

const rows = ref<any[]>([])

const form = ref<Record<string, any>>({})

const rules = ref({
  clubId: [{ required: true, message: '请选择社团', trigger: 'change' }],
  name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
})

const formRef = ref<any>()

async function loadClubs() {
  const result = await clubApi.list({ page: 1, pageSize: 100 })
  clubs.value = result?.list || result || []
  if (!selectedClubId.value && clubs.value.length) {
    selectedClubId.value = clubs.value[0].id
  }
}

async function loadRoles() {
  const result = await rbacApi.roles({ page: 1, pageSize: 100 })
  roles.value = result?.list || result || []
}

async function handleClubChange(clubId: any) {
  await loadDepartments(clubId)
  query.value.departmentId = undefined
  await fetchList()
}

async function loadDepartments(clubId: any) {
  if (!clubId) {
    departments.value = []
    dialogDepartments.value = []
    return
  }
  const result = await clubApi.departments(clubId)
  const departments = result?.list || result || []
  if (clubId === selectedClubId.value) {
    departments.value = departments
  }
  if (clubId === form.value.clubId || clubId === selectedClubId.value) {
    dialogDepartments.value = departments
  }
}

async function fetchList() {
  if (!selectedClubId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const result = await clubApi.positions(selectedClubId.value)
    const list = result?.list || result || []
    rows.value = query.value.departmentId
      ? list.filter((item) => item.departmentId === query.value.departmentId)
      : list
  } finally {
    loading.value = false
  }
}

function openDialog(row: any) {
  form.value = row
    ? {
        ...row,
        clubId: row.clubId,
        departmentId: row.departmentId,
        roleIds: row.roleIds || [],
      }
    : {
        clubId: selectedClubId.value,
        departmentId: undefined,
        name: '',
        maxCount: undefined,
        roleIds: [],
      }
  dialogDepartments.value = form.value.clubId === selectedClubId.value ? departments.value : []
  loadDepartments(form.value.clubId)
  dialogVisible.value = true
}

async function save() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        departmentId: form.value.departmentId,
        name: form.value.name,
        maxCount: form.value.maxCount,
        roleIds: form.value.roleIds,
      }
      if (form.value.id) {
        await positionApi.update(form.value.id, payload)
      } else {
        await positionApi.create(form.value.clubId, payload)
      }
      ElMessage.success('岗位已保存')
      dialogVisible.value = false
      selectedClubId.value = form.value.clubId
      await handleClubChange(selectedClubId.value)
    } finally {
      saving.value = false
    }
  })
}

async function remove(row: any) {
  await positionApi.remove(row.id)
  ElMessage.success('岗位已删除')
  fetchList()
}

function departmentName(departmentId: any) {
  return departments.value.find((item) => item.id === departmentId)?.name || '通用岗位'
}

function roleNames(roleIds: any) {
  return (roleIds || [])
    .map((roleId) => roles.value.find((item) => item.id === roleId)?.name)
    .filter(Boolean)
}

onMounted(async () => {
  await Promise.all([loadClubs(), loadRoles()])
  if (selectedClubId.value) {
    await handleClubChange(selectedClubId.value)
  }
})
</script>
