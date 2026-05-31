<template>
  <ViewPage class="workspace-page profile-workspace">
    <WorkspaceHero
      :metrics="workspaceMetrics"
      description="管理头像、账号信息和最近的申请进度。"
      eyebrow="个人中心"
      title="我的账号"
    />

    <section class="workspace-section">
      <div class="container workspace-grid workspace-grid--split profile-grid">
        <WorkspacePanel
          class="profile-card site-reveal"
          description="维护基础信息与头像，保持工作台身份一致。"
          eyebrow="Identity"
          title="身份资料"
        >
          <div class="profile-avatar">
            <UserAvatar :name="displayName" :size="88" :src="String(user.avatar || '')" />
            <div>
              <strong>{{ displayName }}</strong>
              <span>未上传头像时，如已经绑定QQ,显示QQ头像,否则将显示姓氏默认头像</span>
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
            <el-button v-if="user.avatar" :loading="avatarUploading" plain @click="removeAvatar">
              恢复默认
            </el-button>
          </div>

          <div class="profile-meta">
            <span class="workspace-chip">{{ isLogin ? '已登录' : '未登录' }}</span>
            <span class="workspace-chip">资料完整度 {{ profileCompletion }}%</span>
          </div>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ user.userName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ user.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ user.phone || '-' }}</el-descriptions-item>
          </el-descriptions>
        </WorkspacePanel>

        <div class="profile-stack">
          <WorkspacePanel
            class="profile-progress-card site-reveal"
            description="最近申请与当前阶段一并汇总。"
            eyebrow="Applications"
            title="申请雷达"
          >
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
          </WorkspacePanel>

          <WorkspacePanel
            class="security-card site-reveal"
            compact
            description="账号操作集中放在这里，避免和资料维护混在一起。"
            eyebrow="Security"
            title="安全控制"
          >
            <div class="security-grid">
              <div class="workspace-inline-stat">
                <span>账号状态</span>
                <strong>{{ isLogin ? '在线' : '离线' }}</strong>
              </div>
              <div class="workspace-inline-stat">
                <span>当前申请</span>
                <strong>{{ activeApplicationCount }}</strong>
              </div>
              <div class="workspace-inline-stat">
                <span>QQ 绑定</span>
                <strong>{{ user.qqOpenid ? '已绑定' : '未绑定' }}</strong>
              </div>
            </div>

            <div class="qq-bind-box">
              <div>
                <strong>QQ 机器人绑定</strong>
                <span>{{ user.qqOpenid ? '当前账号已绑定 QQ，可在需要时解绑后重新绑定。' : '生成一次性绑定码后，发给 QQ 机器人完成绑定。' }}</span>
              </div>
              <el-button
                v-if="!user.qqOpenid"
                :loading="qqTokenLoading"
                plain
                type="primary"
                @click="generateQqBindToken"
              >
                生成绑定码
              </el-button>
              <el-button v-else :loading="qqUnbindLoading" plain type="danger" @click="unbindQq">
                解除绑定
              </el-button>
            </div>

            <div v-if="!user.qqOpenid && qqBindToken" class="qq-token-card">
              <span>绑定码</span>
              <strong>{{ qqBindToken }}</strong>
              <p>请在 {{ qqTokenMinutes }} 分钟内向机器人发送：</p>
              <code>/oa bind-qq {{ qqBindToken }}</code>
              <el-button link type="primary" @click="copyQqBindCommand">复制命令</el-button>
            </div>

            <div class="btn-group">
              <el-button class="w-full" plain type="primary" @click="passwordDialogVisible = true"
                >修改密码
              </el-button>
              <el-button class="w-full" @click="logout">退出登录</el-button>
            </div>
          </WorkspacePanel>
        </div>
      </div>
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

<script lang="ts" setup>
import ViewPage from '@/components/common/ViewPage.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import WorkspaceHero from '@/components/site/workspace/WorkspaceHero.vue'
import WorkspacePanel from '@/components/site/workspace/WorkspacePanel.vue'
import {authApi, siteApi} from '@/api'
import {clearSession, getCurrentUser, getToken, setSession} from '@/utils/auth.ts'
import {applicationStatusText, formatDateTime, statusType} from '@/utils/format.ts'
import {ElMessage} from 'element-plus'
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'

const user = ref(getCurrentUser() || {})

const applications = ref<any[]>([])

const submitting = ref(false)
const avatarUploading = ref(false)
const qqTokenLoading = ref(false)
const qqUnbindLoading = ref(false)
const qqBindToken = ref('')
const qqBindExpiresIn = ref(0)
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

const activeStatuses = new Set([
  'submitted',
  'reviewing',
  'approved',
  'pre_screen_passed',
  'interview_scheduled',
  'interviewed',
  'waitlisted',
])

const activeApplicationCount = computed(() =>
  applications.value.filter((item) => activeStatuses.has(item.status)).length,
)

const qqTokenMinutes = computed(() => Math.max(1, Math.ceil(qqBindExpiresIn.value / 60)))

const profileCompletion = computed(() => {
  const fields = [user.value.realName, user.value.email, user.value.phone, user.value.avatar]
  return Math.round((fields.filter(Boolean).length / fields.length) * 100)
})

const workspaceMetrics = computed(() => [
  {
    label: '账号状态',
    value: isLogin.value ? '在线' : '离线',
    note: isLogin.value ? '会话有效' : '等待登录',
  },
  {
    label: '申请记录',
    value: applications.value.length,
    note: '累计提交',
  },
  {
    label: '处理中',
    value: activeApplicationCount.value,
    note: '仍在流转',
  },
  {
    label: '资料完整度',
    value: `${profileCompletion.value}%`,
    note: '基础信息',
  },
])

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
  router.push('/login')
}

async function generateQqBindToken() {
  try {
    qqTokenLoading.value = true
    const result = await authApi.createQqBindToken()
    qqBindToken.value = result?.token || ''
    qqBindExpiresIn.value = Number(result?.expiresIn || 0)
    if (qqBindToken.value) {
      ElMessage.success('QQ 绑定码已生成')
      await fetchProfile()
    }
  } finally {
    qqTokenLoading.value = false
  }
}

async function unbindQq() {
  try {
    qqUnbindLoading.value = true
    await authApi.unbindQq()
    qqBindToken.value = ''
    qqBindExpiresIn.value = 0
    await fetchProfile()
    ElMessage.success('QQ 绑定已解除')
  } finally {
    qqUnbindLoading.value = false
  }
}

async function copyQqBindCommand() {
  const command = `/oa bind-qq ${qqBindToken.value}`
  await navigator.clipboard?.writeText(command)
  ElMessage.success('已复制绑定命令')
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
.profile-grid {
  align-items: start;
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
  color: var(--oa-text);
  font-size: 20px;
}

.profile-avatar span {
  color: var(--oa-muted);
  font-size: 13px;
}

.avatar-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 22px;
}

.profile-stack {
  display: grid;
  gap: 24px;
}

.security-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.qq-bind-box {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-soft);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.qq-bind-box > div {
  display: grid;
  gap: 6px;
}

.qq-bind-box strong,
.qq-token-card strong {
  color: var(--oa-text);
}

.qq-bind-box span,
.qq-token-card span,
.qq-token-card p {
  color: var(--oa-muted);
  font-size: 13px;
}

.qq-token-card {
  margin-top: 12px;
  padding: 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  display: grid;
  gap: 10px;
}

.qq-token-card > strong {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 24px;
  letter-spacing: 3px;
}

.qq-token-card code {
  width: 100%;
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.06);
  color: var(--oa-text);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  white-space: normal;
  word-break: break-all;
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

@media (max-width: 640px) {
  .security-grid {
    grid-template-columns: 1fr;
  }

  .qq-bind-box {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
