<template>
    <div class="admin-page">
        <div v-if="visibleStats.length" class="stat-grid">
            <el-card v-for="item in visibleStats" :key="item.label" class="stat-card" shadow="hover">
                <div class="stat-content">
                    <div class="stat-icon-wrapper">
                        <el-icon>
                            <component :is="item.icon"/>
                        </el-icon>
                    </div>
                    <div class="stat-info">
                        <span class="stat-label">{{ item.label }}</span>
                        <strong class="stat-value">{{ item.value }}</strong>
                    </div>
                </div>
            </el-card>
        </div>

        <el-card v-if="canManageRegister" class="config-bar" shadow="never">
            <div class="config-item">
                <div class="config-text">
                    <span class="title">开放注册</span>
                    <span class="desc">控制前端用户是否可以自主注册账号</span>
                </div>
                <el-switch
                    v-model="registerEnabled"
                    :loading="switchLoading"
                    active-text="开启"
                    inactive-text="关闭"
                    inline-prompt
                    @change="handleRegisterSwitch"
                />
            </div>
        </el-card>

        <div v-if="hasDashboardSections" class="dashboard-grid">
            <el-card v-if="canViewApplications" class="data-table-card" shadow="hover">
                <template #header>
                    <div class="card-header">
                        <span>待处理申请</span>
                        <el-tag size="small" type="danger">{{ applications.length }} 条待办</el-tag>
                    </div>
                </template>
                <el-table :data="applications" height="360" stripe>
                    <el-table-column label="申请人" min-width="100" prop="applicantName"/>
                    <el-table-column label="社团" min-width="120" prop="clubName"/>
                    <el-table-column label="状态" prop="status" width="100">
                        <template #default="{ row }">
                            <el-tag :type="statusType(row.status)" effect="light" size="small">
                                {{ row.status || '-' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>

            <el-card v-if="canViewInterviews" class="data-table-card" shadow="hover">
                <template #header>
                    <div class="card-header">
                        <span>近期面试</span>
                    </div>
                </template>
                <el-table :data="interviews" height="360" stripe>
                    <el-table-column label="候选人" min-width="100" prop="candidateName"/>
                    <el-table-column label="时间" min-width="160">
                        <template #default="{ row }">
                            <span class="time-text">{{ formatDateTime(row.scheduledAt || row.interviewTime) }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" prop="status" width="100">
                        <template #default="{ row }">
                            <el-tag :type="statusType(row.status)" effect="plain" size="small">
                                {{ row.status || '-' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>
        </div>

        <el-empty v-else :image-size="200" description="暂无管理概览权限"/>
    </div>
</template>

<script>
import {DocumentChecked, OfficeBuilding, User, UserFilled} from '@element-plus/icons-vue'
import {authApi, siteApi} from '../../api'
import {formatDateTime, statusType} from '../../utils/format'
import {hasAnyPermission} from '../../utils/permission'
import {ElMessage} from "element-plus";

export default {
    name: 'AdminDashboard',
    data() {
        return {
            registerEnabled: false,
            switchLoading: false,
            stats: [
                {key: 'users', label: '用户总数', value: '--', icon: User, permissions: ['user:list']},
                {key: 'clubs', label: '社团总数', value: '--', icon: OfficeBuilding, permissions: ['club:list']},
                {
                    key: 'applications',
                    label: '待审申请',
                    value: '--',
                    icon: DocumentChecked,
                    permissions: ['application:list']
                },
                {key: 'memberships', label: '活跃成员', value: '--', icon: UserFilled, permissions: ['membership:list']}
            ],
            applications: [],
            interviews: []
        }
    },
    computed: {
        visibleStats() {
            return this.stats.filter(item => hasAnyPermission(item.permissions))
        },
        canViewApplications() {
            return hasAnyPermission(['application:list'])
        },
        canViewInterviews() {
            return hasAnyPermission(['interview:list'])
        },
        canManageRegister() {
            return hasAnyPermission(['site:manage', 'admin'])
        }, // 假设权限名
        hasDashboardSections() {
            return this.canViewApplications || this.canViewInterviews
        }
    },
    created() {
        this.loadDashboard()
        if (this.canManageRegister) {
            this.fetchRegisterEnabled()
        }
    },
    methods: {
        formatDateTime,
        statusType,
        async fetchRegisterEnabled() {
            try {
                this.registerEnabled = await siteApi.registerEnabled()
            } catch (e) {
                console.error("获取注册状态失败")
            }
        },
        async handleRegisterSwitch(val) {
            this.switchLoading = true
            try {
                await authApi.updateRegisterEnabled(val)
                ElMessage.success(val ? '注册通道已开启' : '注册通道已关闭')
            } catch (error) {
                this.registerEnabled = !val // 失败时回滚状态
            } finally {
                this.switchLoading = false
            }
        },
        async loadDashboard() {
            // ... (保持原有的获取数据逻辑，此处略)
        }
    }
}
</script>

<style scoped>
.admin-page {
    padding: 20px;
    background-color: #f5f7fa;
    min-height: 100vh;
}

/* 统计卡片样式 */
.stat-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 24px;
}

.stat-card {
    border: none;
    border-radius: 12px;
    transition: transform 0.3s;
}

.stat-card:hover {
    transform: translateY(-4px);
}

.stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
}

.stat-icon-wrapper {
    background: rgba(64, 158, 255, 0.1);
    padding: 12px;
    border-radius: 10px;
    color: #409EFF;
    font-size: 24px;
    display: flex;
}

.stat-label {
    font-size: 14px;
    color: #909399;
}

.stat-value {
    display: block;
    font-size: 24px;
    font-weight: bold;
    color: #303133;
}

/* 注册控制栏 */
.config-bar {
    margin-bottom: 24px;
    border-radius: 12px;
}

.config-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.config-text .title {
    display: block;
    font-weight: bold;
    margin-bottom: 4px;
}

.config-text .desc {
    font-size: 13px;
    color: #909399;
}

/* 数据表格布局 */
.dashboard-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
}

.data-table-card {
    border-radius: 12px;
    border: none;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
}

.time-text {
    font-size: 13px;
    color: #606266;
}

@media (max-width: 1050px) {
    .dashboard-grid {
        grid-template-columns: 1fr;
    }
}
</style>