<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-button :icon="ArrowLeft" @click="$router.push('/admin/alumni-managers')">
          返回往届管理人员
        </el-button>
      </div>
      <div class="toolbar__actions">
        <el-button
          type="primary"
          :icon="Plus"
          @click="$router.push({ path: '/admin/groups/create', query: { clubId: currentClubId, type: 'alumni' } })"
        >新增分组</el-button>
      </div>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分组名称" min-width="160">
        <template #default="{ row }">
          <strong>{{ row.name }}</strong>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column prop="sortOrder" label="排序" width="100" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="$router.push({ path: '/admin/groups', query: { type: 'alumni', sourceType: 'alumni', sourceId: row.id } })"
          >统一管理</el-button>
          <el-button link type="danger" @click="removeGroup(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增对话框 -->
    <el-dialog v-model="dialogVisible" title="新增分组" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分组名称">
          <el-input v-model="form.name" placeholder="请输入分组名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="分组描述（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createGroup">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑分组" width="460px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="分组名称">
          <el-input v-model="editForm.name" placeholder="请输入分组名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="分组描述（可选）" />
        </el-form-item>
        <el-form-item label="排序">
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
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { alumniGroupApi, clubApi } from '@/api'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const rows = ref<any[]>([])
const currentClubId = ref<number | undefined>(undefined)

const dialogVisible = ref(false)
const editDialogVisible = ref(false)

const form = ref({
  name: '',
  description: '',
  sortOrder: 0,
})

const editForm = ref({
  id: undefined as number | undefined,
  name: '',
  description: '',
  sortOrder: 0,
})

async function loadClubId() {
  const clubResult = await clubApi.list({})
  const clubs = clubResult?.list || clubResult || []
  const defaultClub = clubs.find((item: any) => item.code === 'JMI-OPENATOM') || clubs[0]
  currentClubId.value = defaultClub?.id
}

async function fetchList() {
  if (!currentClubId.value) return
  loading.value = true
  try {
    const result = await alumniGroupApi.list(currentClubId.value)
    rows.value = result || []
  } finally {
    loading.value = false
  }
}

function openDialog() {
  form.value = { name: '', description: '', sortOrder: 0 }
  dialogVisible.value = true
}

async function createGroup() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }
  await alumniGroupApi.create(currentClubId.value!, form.value)
  ElMessage.success('分组创建成功')
  dialogVisible.value = false
  fetchList()
}

function openEditDialog(row: any) {
  editForm.value = {
    id: row.id,
    name: row.name,
    description: row.description || '',
    sortOrder: row.sortOrder || 0,
  }
  editDialogVisible.value = true
}

async function submitEdit() {
  if (!editForm.value.name.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }
  await alumniGroupApi.update(editForm.value.id!, {
    name: editForm.value.name,
    description: editForm.value.description,
    sortOrder: editForm.value.sortOrder,
  })
  ElMessage.success('分组更新成功')
  editDialogVisible.value = false
  fetchList()
}

async function removeGroup(row: any) {
  await ElMessageBox.confirm(`确认删除分组「${row.name}」？`, '提示', { type: 'warning' })
  await alumniGroupApi.delete(row.id)
  ElMessage.success('分组已删除')
  fetchList()
}

onMounted(async () => {
  await loadClubId()
  await fetchList()
  const requestedGroupId = Number(route.query.groupId)
  const requestedGroup = rows.value.find((item) => item.id === requestedGroupId)
  if (requestedGroup) {
    await router.replace({
      path: '/admin/groups',
      query: { type: 'alumni', sourceType: 'alumni', sourceId: requestedGroup.id },
    })
  }
})
</script>
