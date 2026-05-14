<template>
  <ViewPage class="admin-page">
    <div v-if="visibleStats.length" class="stat-grid">
      <div v-for="item in visibleStats" :key="item.label" class="stat-card panel">
        <el-icon>
          <component :is="item.icon" />
        </el-icon>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </div>

    <div v-if="hasDashboardSections" class="dashboard-grid">
      <el-card v-if="canViewApplications" shadow="never">
        <template #header>待处理申请</template>
        <el-table :data="applications" height="360">
          <el-table-column label="编号" prop="id" width="80" />
          <el-table-column label="申请人" min-width="120" prop="applicantName" />
          <el-table-column label="社团" min-width="130" prop="clubName" />
          <el-table-column label="状态" prop="status" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{
                applicationStatusText(row.status)
              }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="canViewInterviews" shadow="never">
        <template #header>近期面试</template>
        <el-table :data="interviews" height="360">
          <el-table-column label="编号" prop="id" width="80" />
          <el-table-column label="候选人" min-width="120" prop="candidateName" />
          <el-table-column label="状态" prop="status" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ interviewStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" min-width="160" prop="scheduledAt">
            <template #default="{ row }"
              >{{ formatDateTime(row.scheduledAt || row.interviewTime) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <br />

      <el-card shadow="never" style="grid-column: span 2">
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span>前端用户注册功能</span>
          <el-switch
            v-model="registerEnabled"
            :loading="switchLoading"
            active-text="开启注册"
            inactive-text="关闭注册"
            @change="handleRegisterSwitch"
          />
        </div>
      </el-card>
    </div>

    <el-empty v-else description="当前账号没有可展示的概览数据权限" />
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { DocumentChecked, OfficeBuilding, User, UserFilled } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import {
  applicationApi,
  authApi,
  clubApi,
  interviewApi,
  membershipApi,
  siteApi,
  userApi,
} from '@/api'
import {
  applicationStatusText,
  formatDateTime,
  interviewStatusText,
  statusType,
} from '@/utils/format.ts'
import { hasAnyPermission } from '@/utils/permission.ts'
import { ElMessage } from 'element-plus'

const switchLoading = ref(false)

const applications = ref<any[]>([])

const interviews = ref<any[]>([])

const stats = ref<any>()

const registerEnabled = ref<any>()

const visibleStats = computed(() => {
  return stats.value.filter((item) => hasAnyPermission(item.permissions))
})

const canViewApplications = computed(() => {
  return hasAnyPermission(['application:list'])
})

const canViewInterviews = computed(() => {
  return hasAnyPermission(['interview:list'])
})

const hasDashboardSections = computed(() => {
  return canViewApplications.value || canViewInterviews.value
})

function countValue(result: any) {
  if (!result) {
    return '--'
  }
  return result.total ?? (result.list || result || []).length
}

function setStatValue(key: any, value: any) {
  const target = stats.value.find((item) => item.key === key)
  if (target) {
    target.value = value
  }
}

async function loadDashboard() {
  // 保持你原来的 Promise.all 请求逻辑
  const [users, clubs, applications, memberships, interviews] = await Promise.all([
    hasAnyPermission(['user:list'])
      ? userApi.list({ page: 1, pageSize: 1 })
      : Promise.resolve(null),
    hasAnyPermission(['club:list'])
      ? clubApi.list({ page: 1, pageSize: 1 })
      : Promise.resolve(null),
    canViewApplications.value
      ? applicationApi.list({ page: 1, pageSize: 8 })
      : Promise.resolve(null),
    hasAnyPermission(['membership:list'])
      ? membershipApi.list({
          page: 1,
          pageSize: 1,
        })
      : Promise.resolve(null),
    canViewInterviews.value ? interviewApi.list({ page: 1, pageSize: 8 }) : Promise.resolve(null),
  ])
  setStatValue('users', countValue(users))
  setStatValue('clubs', countValue(clubs))
  setStatValue('applications', countValue(applications))
  setStatValue('memberships', countValue(memberships))
  applications.value = applications?.list || applications || []
  interviews.value = interviews?.list || interviews || []
}

async function fetchRegisterEnabled() {
  try {
    registerEnabled.value = Boolean(await siteApi.registerEnabled())
  } catch (e) {
    console.error('获取注册状态失败', e)
  }
}

async function handleRegisterSwitch(value: any) {
  switchLoading.value = true
  try {
    await authApi.updateRegisterEnabled(value)
    ElMessage.success(value ? '已开启注册' : '已关闭注册')
  } catch (error) {
    // 如果后端更新失败，UI 状态回滚
    registerEnabled.value = !value
    ElMessage.error('操作失败')
  } finally {
    switchLoading.value = false
  }
}

onMounted(() => {
  loadDashboard()
  fetchRegisterEnabled() // 页面加载时获取初始状态
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  overflow: hidden;
  margin-bottom: 0;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  background: #e0e0e0;
}

.stat-card {
  display: flex;
  min-height: 152px;
  align-items: center;
  gap: 14px;
  padding: 24px;
  border: 0;
  border-radius: 0;
  background: #ffffff;
  animation: oaFadeUp 0.42s ease both;
}

.stat-card .el-icon {
  color: var(--oa-primary);
  font-size: 30px;
}

.stat-card span {
  display: block;
  color: var(--oa-muted);
}

.stat-card strong {
  display: block;
  margin-top: 4px;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 40px;
  font-weight: 600;
  line-height: 1.1;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  background: #e0e0e0;
}

.dashboard-grid > br {
  display: none;
}

.dashboard-grid :deep(.el-card) {
  border: 0 !important;
  border-radius: 0 !important;
}

@media (max-width: 1050px) {
  .stat-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
