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
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button
        v-if="canCreate"
        type="primary"
        :icon="Plus"
        :disabled="!selectedClubId"
        @click="$router.push({ path: '/admin/groups/create', query: { clubId: selectedClubId, type: 'department' } })"
      >新增部门</el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="部门名称" min-width="140" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '-' }}</template>
      </el-table-column>
      <el-table-column label="部长" min-width="200">
        <template #default="{ row }">
          <div class="dept-leader">
            <template v-if="row._leaderUser">
              <el-avatar :size="26" :src="row._leaderUser.avatar">
                {{ (row._leaderUser.realName || row._leaderUser.userName || '?').charAt(0) }}
              </el-avatar>
              <span class="dept-leader__name">{{ row._leaderUser.realName || row._leaderUser.userName }}</span>
            </template>
            <span v-else class="leader-item__empty">未设置</span>
            <el-button v-if="canUpdate" link type="primary" size="small" @click="openUserPicker('departmentHead', row)">
              {{ row.managerUserId ? '更换' : '设置' }}
            </el-button>
            <el-button v-if="canUpdate && row.managerUserId" link type="danger" size="small" @click="clearDeptField(row, 'manager')">清空</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="副部长" min-width="200">
        <template #default="{ row }">
          <div class="dept-leader">
            <template v-if="row._viceLeaderUser">
              <el-avatar :size="26" :src="row._viceLeaderUser.avatar">
                {{ (row._viceLeaderUser.realName || row._viceLeaderUser.userName || '?').charAt(0) }}
              </el-avatar>
              <span class="dept-leader__name">{{ row._viceLeaderUser.realName || row._viceLeaderUser.userName }}</span>
            </template>
            <span v-else class="leader-item__empty">未设置</span>
            <el-button v-if="canUpdate" link type="primary" size="small" @click="openUserPicker('viceDepartmentHead', row)">
              {{ row.viceManagerUserId ? '更换' : '设置' }}
            </el-button>
            <el-button v-if="canUpdate && row.viceManagerUserId" link type="danger" size="small" @click="clearDeptField(row, 'viceManager')">清空</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="微信群二维码" width="120">
        <template #default="{ row }">
          <el-image
            v-if="row.wechatGroupQrcode"
            :src="row.wechatGroupQrcode"
            style="width:52px;height:52px;border-radius:4px"
            fit="contain"
            :preview-src-list="[row.wechatGroupQrcode]"
          />
          <span v-else class="leader-item__empty">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="canUpdate"
            link
            type="primary"
            @click="$router.push({ path: '/admin/groups', query: { type: 'department', sourceType: 'department', sourceId: row.id } })"
          >统一管理</el-button>
          <el-upload
            v-if="canUpdate"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="(res: any) => onDeptQrcodeUploaded(row, res)"
            accept="image/*"
            style="display:inline-block;margin:0 4px"
          >
            <el-button link type="primary">上传二维码</el-button>
          </el-upload>
          <el-popconfirm v-if="canDelete" title="确定删除该部门？" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" :description="selectedClubId ? '当前社团暂无部门' : '请先选择社团'" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑部门' : '新增部门'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" maxlength="64" show-word-limit placeholder="如：技术部、宣传部" />
        </el-form-item>
        <el-form-item label="部门描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="可选，部门职责简介"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="userPickerVisible" :title="userPickerTitle" width="600px">
      <div class="user-picker">
        <el-input
          v-model="userSearchKeyword"
          clearable
          placeholder="搜索用户名/姓名"
          style="margin-bottom: 16px"
          @keyup.enter="searchUsers"
        >
          <template #append>
            <el-button :icon="Search" @click="searchUsers" />
          </template>
        </el-input>
        <el-table
          v-loading="userSearchLoading"
          :data="userSearchRows"
          highlight-current-row
          max-height="400"
          @current-change="onUserSelect"
        >
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="userName" label="用户名" />
          <el-table-column prop="studentId" label="学号" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="userPickerVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { clubApi, departmentApi, userApi } from '@/api'
import { hasPermission } from '@/utils/permission.ts'
import { getToken } from '@/utils/auth.ts'
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)

const clubs = ref<any[]>([])
const selectedClubId = ref<number | string>('')
const rows = ref<any[]>([])

const form = ref<Record<string, any>>({})
const formRef = ref<any>()
const rules = ref({
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
})

const canCreate = computed(() => hasPermission('department:create'))
const canUpdate = computed(() => hasPermission('department:update'))
const canDelete = computed(() => hasPermission('department:delete'))

const uploadUrl = import.meta.env.VITE_API_BASE_URL
  ? `${import.meta.env.VITE_API_BASE_URL}/image-hosting/images`
  : '/api/v1/image-hosting/images'
const uploadHeaders = computed(() => {
  const token = getToken()
  return { Authorization: `Bearer ${token}`, jmiopenatom: token }
})

const userPickerVisible = ref(false)
const userPickerTarget = ref<'departmentHead' | 'viceDepartmentHead'>('departmentHead')
const userPickerDept = ref<any>(null)
const userSearchKeyword = ref('')
const userSearchLoading = ref(false)
const userSearchRows = ref<any[]>([])

const userPickerTitle = computed(() => {
  return userPickerTarget.value === 'departmentHead' ? '选择部长' : '选择副部长'
})

async function loadClubs() {
  const result = await clubApi.list({ page: 1, pageSize: 100 })
  clubs.value = result?.list || result || []
  if (!selectedClubId.value && clubs.value.length) {
    selectedClubId.value = clubs.value[0].id
  }
}

async function handleClubChange() {
  await fetchList()
}

async function fetchList() {
  if (!selectedClubId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const result = await departmentApi.list(selectedClubId.value)
    const list = result?.list || result || []
    const enriched: any[] = []
    for (const d of list) {
      const [leader, viceLeader] = await Promise.all([
        d.managerUserId ? fetchUser(d.managerUserId).catch(() => null) : null,
        d.viceManagerUserId ? fetchUser(d.viceManagerUserId).catch(() => null) : null,
      ])
      enriched.push({ ...d, _leaderUser: leader, _viceLeaderUser: viceLeader })
    }
    rows.value = enriched
  } finally {
    loading.value = false
  }
}

async function fetchUser(userId: number): Promise<any> {
  try {
    const res = await userApi.info(userId)
    return res || {}
  } catch {
    return {}
  }
}

function openDialog(row?: any) {
  form.value = row
    ? { id: row.id, name: row.name, description: row.description || '' }
    : { name: '', description: '' }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate?.())
}

async function save() {
  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = { name: form.value.name, description: form.value.description }
      if (form.value.id) {
        await departmentApi.update(form.value.id, payload)
        ElMessage.success('部门已更新')
      } else {
        await departmentApi.create(selectedClubId.value, payload)
        ElMessage.success('部门已创建')
      }
      dialogVisible.value = false
      await fetchList()
    } finally {
      saving.value = false
    }
  })
}

async function remove(row: any) {
  await departmentApi.remove(row.id)
  ElMessage.success('部门已删除')
  fetchList()
}

function openUserPicker(target: typeof userPickerTarget.value, dept: any) {
  userPickerTarget.value = target
  userPickerDept.value = dept
  userSearchKeyword.value = ''
  userSearchRows.value = []
  userPickerVisible.value = true
  nextTick(() => searchUsers())
}

async function searchUsers() {
  userSearchLoading.value = true
  try {
    const res = await userApi.list({ keyword: userSearchKeyword.value, page: 1, pageSize: 50 })
    userSearchRows.value = res?.list || res || []
  } finally {
    userSearchLoading.value = false
  }
}

async function onUserSelect(user: any) {
  if (!user || !userPickerDept.value) return
  const dept = userPickerDept.value
  const target = userPickerTarget.value
  if (target === 'departmentHead') {
    await departmentApi.update(dept.id, { managerUserId: user.id })
    dept.managerUserId = user.id
    dept._leaderUser = user
    ElMessage.success(`已将「${user.realName || user.userName}」设为「${dept.name}」部长`)
  } else {
    await departmentApi.update(dept.id, { viceManagerUserId: user.id })
    dept.viceManagerUserId = user.id
    dept._viceLeaderUser = user
    ElMessage.success(`已将「${user.realName || user.userName}」设为「${dept.name}」副部长`)
  }
  userPickerVisible.value = false
}

async function clearDeptField(row: any, field: 'manager' | 'viceManager') {
  const payload =
    field === 'manager' ? { managerUserId: 0 } : { viceManagerUserId: 0 }
  await departmentApi.update(row.id, payload)
  if (field === 'manager') {
    row.managerUserId = null
    row._leaderUser = null
  } else {
    row.viceManagerUserId = null
    row._viceLeaderUser = null
  }
  ElMessage.success('已清空')
}

async function onDeptQrcodeUploaded(row: any, res: any) {
  const url = res?.data?.url || res?.url || res?.data
  if (!url || typeof url !== 'string') {
    ElMessage.error('上传失败：未获取到图片地址')
    return
  }
  await departmentApi.update(row.id, { wechatGroupQrcode: url })
  row.wechatGroupQrcode = url
  ElMessage.success('二维码已上传')
}

onMounted(async () => {
  await loadClubs()
  if (selectedClubId.value) {
    await fetchList()
    const requestedGroupId = Number(route.query.groupId)
    const requestedGroup = rows.value.find((item) => item.id === requestedGroupId)
    if (requestedGroup && canUpdate.value) {
      await router.replace({
        path: '/admin/groups',
        query: { type: 'department', sourceType: 'department', sourceId: requestedGroup.id },
      })
    }
  }
})
</script>

<style scoped>
.dept-leader {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.dept-leader__name {
  font-size: 13px;
}

.leader-item__empty {
  color: var(--oa-muted);
  font-size: 12px;
}
</style>
