<template>
  <ViewPage class="site-page">
    <section class="container profile-grid">
      <el-card class="profile-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>我的账号</span>
            <el-tag v-if="isLogin" size="small" type="success">已登录</el-tag>
          </div>
        </template>

        <div class="profile-avatar">
          <UserAvatar :name="displayName" :size="88" :src="String(user.avatar || '')" />
          <div>
            <strong>{{ displayName }}</strong>
            <span>未上传头像时，将显示姓氏默认头像</span>
          </div>
        </div>

        <input
          ref="avatarInputRef"
          accept="image/jpeg,image/png"
          hidden
          type="file"
          @change="handleAvatarChange"
        />

        <div class="avatar-actions">
          <el-button :loading="avatarUploading" plain type="primary" @click="chooseAvatar">
            上传头像
          </el-button>
          <el-button
            v-if="user.avatar"
            :loading="avatarUploading"
            plain
            @click="removeAvatar"
          >
            恢复默认
          </el-button>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ user.userName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ user.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ user.phone || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="btn-group">
          <el-button class="w-full" @click="logout">退出登录</el-button>
          <el-button class="w-full" plain type="primary" @click="passwordDialogVisible = true"
            >修改密码
          </el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <span>我的申请进度</span>
        </template>
        <el-table :data="applications" border stripe>
          <el-table-column label="申请编号" prop="id" width="100" />
          <el-table-column label="社团名称" min-width="150" prop="clubName" />
          <el-table-column label="当前状态" prop="status" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{
                applicationStatusText(row.status || 'reviewing')
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" min-width="180" prop="createdAt">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>

    <el-dialog
      v-model="passwordDialogVisible"
      append-to-body
      title="安全设置 - 修改密码"
      width="420px"
      @close="resetForm"
    >
      <el-form ref="pwdFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="当前旧密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            placeholder="请输入您当前的登录密码"
            show-password
            type="password"
          />
        </el-form-item>
        <el-form-item label="设置新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            placeholder="建议包含字母与数字"
            show-password
            type="password"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button :loading="submitting" type="primary" @click="submitPassword"
            >确认修改</el-button
          >
        </div>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { authApi, siteApi } from '@/api'
import { clearSession, getCurrentUser, getToken, setSession } from '@/utils/auth.ts'
import { applicationStatusText, formatDateTime, statusType } from '@/utils/format.ts'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const user = ref(getCurrentUser() || {})

const applications = ref<any[]>([])

const submitting = ref(false)
const avatarUploading = ref(false)
const avatarInputRef = ref<HTMLInputElement>()

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
})

const passwordRules = ref({
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
  ],
})

const passwordDialogVisible = ref<any>()

const pwdFormRef = ref<any>()

const router = useRouter()

const route = useRoute()

const isLogin = computed(() => {
  return Boolean(getToken())
})

const displayName = computed(() => String(user.value.realName || user.value.userName || '用户'))

async function fetchProfile() {
  const result = await authApi.me()
  if (result?.user) {
    user.value = result.user
    setSession({ accessToken: getToken(), user: user.value })
  }
}

async function fetchApplications() {
  const result = await siteApi.progress()
  applications.value = result?.applications || []
}

async function logout() {
  await authApi.logout()
  clearSession()
  router.push('/admin/login')
}

function chooseAvatar() {
  avatarInputRef.value?.click()
}

async function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG 或 PNG 头像')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 2MB')
    return
  }
  try {
    avatarUploading.value = true
    await authApi.uploadAvatar(file)
    await fetchProfile()
    setSession({ accessToken: getToken() || undefined, user: user.value })
    if (user.value.avatar && !(await canLoadImage(String(user.value.avatar)))) {
      ElMessage.warning('头像已保存，但图片暂时无法访问，请检查后端头像文件服务')
      return
    }
    ElMessage.success('头像更新成功')
  } finally {
    avatarUploading.value = false
  }
}

async function removeAvatar() {
  try {
    avatarUploading.value = true
    const nextUser = await authApi.removeAvatar()
    user.value = nextUser || user.value
    setSession({ accessToken: getToken() || undefined, user: user.value })
    ElMessage.success('已恢复默认头像')
  } finally {
    avatarUploading.value = false
  }
}

async function submitPassword() {
  pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      submitting.value = true
      await authApi.updatePassword(passwordForm.value)
      ElMessage.success('密码更新成功，请使用新密码重新登录')
      passwordDialogVisible.value = false
      logout() // 强制重新登录以保证安全
    } catch (error) {
      console.error('修改失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  pwdFormRef.value?.resetFields()
}

function canLoadImage(src: string) {
  return new Promise<boolean>((resolve) => {
    const image = new Image()
    image.onload = () => resolve(true)
    image.onerror = () => resolve(false)
    image.src = src
  })
}

onMounted(() => {
  if (isLogin.value) {
    fetchProfile()
    fetchApplications()
  }
})
</script>

<style scoped>
.site-page {
  padding: 64px 0 80px;
  background: #f5f5f7;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.82fr) minmax(0, 1.18fr);
  gap: 1px;
  align-items: start;
  overflow: hidden;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  background: #e0e0e0;
}

.profile-grid :deep(.el-card) {
  min-height: 100%;
  border: 0 !important;
  border-radius: 0 !important;
  animation: oaFadeUp 0.44s ease both;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-avatar {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 18px;
}

.profile-avatar div {
  display: grid;
  gap: 6px;
}

.profile-avatar strong {
  color: #1d1d1f;
  font-size: 20px;
}

.profile-avatar span {
  color: #7a7a7a;
  font-size: 13px;
}

.avatar-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 22px;
}

.btn-group {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.w-full {
  width: 100%;
  margin-left: 0 !important;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
