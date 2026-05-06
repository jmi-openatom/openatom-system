<template>
    <div class="admin-page">
        <div v-if="visibleStats.length" class="stat-grid">
            <div v-for="item in visibleStats" :key="item.label" class="stat-card panel">
                <el-icon>
                    <component :is="item.icon"/>
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
                    <el-table-column label="编号" prop="id" width="80"/>
                    <el-table-column label="申请人" min-width="120" prop="applicantName"/>
                    <el-table-column label="社团" min-width="130" prop="clubName"/>
                    <el-table-column label="状态" prop="status" width="110">
                        <template #default="{ row }">
                            <el-tag :type="statusType(row.status)">{{ row.status || '-' }}</el-tag>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>
            <el-card v-if="canViewInterviews" shadow="never">
                <template #header>近期面试</template>
                <el-table :data="interviews" height="360">
                    <el-table-column label="编号" prop="id" width="80"/>
                    <el-table-column label="候选人" min-width="120" prop="candidateName"/>
                    <el-table-column label="状态" prop="status" width="110">
                        <template #default="{ row }">
                            <el-tag :type="statusType(row.status)">{{ row.status || '-' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="时间" min-width="160" prop="scheduledAt">
                        <template #default="{ row }">{{
                                formatDateTime(row.scheduledAt || row.interviewTime)
                            }}
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>
        </div>
        <el-card shadow="never">
            <button @click="handleRegisterSwitch">切换前端注册</button>
        </el-card>
        <el-empty v-else description="当前账号没有可展示的概览数据权限"/>
    </div>
</template>

<script>
import {DocumentChecked, OfficeBuilding, User, UserFilled} from '@element-plus/icons-vue'
import {applicationApi, authApi, clubApi, interviewApi, membershipApi, userApi} from '../../api'
import {formatDateTime, statusType} from '../../utils/format'
import {hasAnyPermission} from '../../utils/permission'
import {ElMessage} from "element-plus";

export default {
    name: 'AdminDashboard',
    data() {
        return {
            stats: [
                {key: 'users', label: '用户数量', value: '--', icon: User, permissions: ['user:list']},
                {key: 'clubs', label: '社团数量', value: '--', icon: OfficeBuilding, permissions: ['club:list']},
                {
                    key: 'applications',
                    label: '申请数量',
                    value: '--',
                    icon: DocumentChecked,
                    permissions: ['application:list']
                },
                {key: 'memberships', label: '成员数量', value: '--', icon: UserFilled, permissions: ['membership:list']}
            ],
            applications: [],
            interviews: []
        }
    },
    computed: {
        visibleStats() {
            return this.stats.filter((item) => hasAnyPermission(item.permissions))
        },
        canViewApplications() {
            return hasAnyPermission(['application:list'])
        },
        canViewInterviews() {
            return hasAnyPermission(['interview:list'])
        },
        hasDashboardSections() {
            return this.canViewApplications || this.canViewInterviews
        }
    },
    created() {
        this.loadDashboard()
    },
    methods: {
        formatDateTime,
        statusType,
        countValue(result) {
            if (!result) {
                return '--'
            }
            return result.total ?? (result.list || result || []).length
        },
        setStatValue(key, value) {
            const target = this.stats.find((item) => item.key === key)
            if (target) {
                target.value = value
            }
        },
        async loadDashboard() {
            // 仪表盘只请求当前账号有权限访问的数据，避免默认页触发 403。
            const [users, clubs, applications, memberships, interviews] = await Promise.all([
                hasAnyPermission(['user:list']) ? userApi.list({page: 1, pageSize: 1}) : Promise.resolve(null),
                hasAnyPermission(['club:list']) ? clubApi.list({page: 1, pageSize: 1}) : Promise.resolve(null),
                this.canViewApplications ? applicationApi.list({page: 1, pageSize: 8}) : Promise.resolve(null),
                hasAnyPermission(['membership:list']) ? membershipApi.list({
                    page: 1,
                    pageSize: 1
                }) : Promise.resolve(null),
                this.canViewInterviews ? interviewApi.list({page: 1, pageSize: 8}) : Promise.resolve(null)
            ])
            this.setStatValue('users', this.countValue(users))
            this.setStatValue('clubs', this.countValue(clubs))
            this.setStatValue('applications', this.countValue(applications))
            this.setStatValue('memberships', this.countValue(memberships))
            this.applications = applications?.list || applications || []
            this.interviews = interviews?.list || interviews || []
        },
        async handleRegisterSwitch(value) {
            if (!this.canManageRegister) {
                await this.fetchRegisterEnabled()
                return
            }
            try {
                await authApi.updateRegisterEnabled(value)
                ElMessage.success(value ? '已开启注册' : '已关闭注册')
            } catch (error) {
                await this.fetchRegisterEnabled()
            }
        },
    }
}
</script>

<style scoped>
.stat-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 16px;
}

.stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 20px;
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
    font-size: 28px;
}

.dashboard-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
}

@media (max-width: 1050px) {
    .stat-grid,
    .dashboard-grid {
        grid-template-columns: 1fr;
    }
}
</style>
