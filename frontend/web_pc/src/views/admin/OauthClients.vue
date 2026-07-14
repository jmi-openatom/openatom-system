<template>
  <ViewPage class="admin-page">
    <ViewToolbar title="认证应用">
      <el-button type="primary" @click="openCreate">新增应用</el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="clients" class="admin-table">
      <el-table-column prop="clientName" label="应用名称" min-width="150" />
      <el-table-column prop="clientId" label="Client ID" min-width="150" />
      <el-table-column prop="redirectUris" label="回调地址" min-width="260" show-overflow-tooltip />
      <el-table-column prop="scopes" label="Scopes" min-width="180" show-overflow-tooltip />
      <el-table-column prop="grantTypes" label="Grant Types" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="removeClient(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑应用' : '新增应用'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="应用名称" prop="clientName">
          <el-input v-model="form.clientName" />
        </el-form-item>
        <el-form-item label="Client ID" prop="clientId">
          <el-input v-model="form.clientId" />
        </el-form-item>
        <el-form-item label="Client Secret">
          <el-input v-model="form.clientSecret" show-password placeholder="公开客户端可留空；编辑时留空表示不修改" />
        </el-form-item>
        <el-form-item label="回调地址" prop="redirectUris">
          <el-input v-model="form.redirectUris" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="Scopes">
          <el-input v-model="form.scopes" />
        </el-form-item>
        <el-form-item label="Grant Types">
          <el-input v-model="form.grantTypes" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveClient">保存</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { oauthClientApi } from '@/api'

interface OauthClientRow {
  id: number
  clientId: string
  clientName: string
  clientSecret?: string
  redirectUris: string
  scopes: string
  grantTypes: string
  enabled: boolean
}

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const clients = ref<OauthClientRow[]>([])
const formRef = ref<any>()

const form = reactive({
  clientId: '',
  clientSecret: '',
  clientName: '',
  redirectUris: '',
  scopes: 'openid profile email roles permissions',
  grantTypes: 'authorization_code refresh_token',
  enabled: true,
})

const rules = {
  clientId: [{ required: true, message: '请输入 Client ID', trigger: 'blur' }],
  clientName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  redirectUris: [{ required: true, message: '请输入回调地址', trigger: 'blur' }],
}

async function loadClients() {
  loading.value = true
  try {
    clients.value = await oauthClientApi.list()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    clientId: '',
    clientSecret: '',
    clientName: '',
    redirectUris: `${window.location.origin}/auth/callback`,
    scopes: 'openid profile email roles permissions',
    grantTypes: 'authorization_code refresh_token',
    enabled: true,
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: OauthClientRow) {
  editingId.value = row.id
  Object.assign(form, { ...row, clientSecret: '' })
  dialogVisible.value = true
}

function saveClient() {
  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      if (editingId.value) {
        await oauthClientApi.update(editingId.value, form)
      } else {
        await oauthClientApi.create(form)
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      await loadClients()
    } finally {
      saving.value = false
    }
  })
}

async function removeClient(row: OauthClientRow) {
  await ElMessageBox.confirm(`确认删除 ${row.clientName}？`, '删除认证应用', { type: 'warning' })
  await oauthClientApi.remove(row.id)
  ElMessage.success('删除成功')
  await loadClients()
}

onMounted(loadClients)
</script>
