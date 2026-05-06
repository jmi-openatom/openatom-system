<template>
    <div class="site-page">
        <section class="container profile-grid">
            <el-card shadow="never">
                <template #header>
                    <div class="card-header">
                        <span>我的账号</span>
                        <el-button v-if="!isLogin" type="primary" @click="$router.push('/admin/login')">登录</el-button>
                    </div>
                </template>
                <el-descriptions :column="1" border>
                    <el-descriptions-item label="用户名">{{ user.userName || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="姓名">{{ user.realName || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="手机号">{{ user.phone || '-' }}</el-descriptions-item>
                </el-descriptions>
                <br>
                <el-button @click="logout()">退出登录</el-button>
            </el-card>
            <el-card shadow="never">
                <template #header>
                    <span>我的申请</span>
                </template>
                <el-table :data="applications" border>
                    <el-table-column label="编号" prop="id" width="90"/>
                    <el-table-column label="社团" min-width="140" prop="clubName"/>
                    <el-table-column label="状态" prop="status" width="120">
                        <template #default="{ row }">
                            <el-tag :type="statusType(row.status)">{{ row.status || '-' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="提交时间" min-width="170" prop="createdAt">
                        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
                    </el-table-column>
                </el-table>
            </el-card>
        </section>
    </div>
</template>

<script>
import {authApi, siteApi} from '../../api'
import {getCurrentUser, getToken, setSession} from '../../utils/auth'
import {formatDateTime, statusType} from '../../utils/format'

export default {
    name: 'SiteProfile',
    data() {
        return {
            user: getCurrentUser(),
            applications: []
        }
    },
    computed: {
        authApi() {
            return authApi
        },
        isLogin() {
            return Boolean(getToken())
        }
    },
    created() {
        if (this.isLogin) {
            this.fetchProfile()
            this.fetchApplications()
        }
    },
    methods: {
        formatDateTime,
        statusType,
        async fetchProfile() {
            const result = await authApi.me()
            this.user = result?.user || {}
            setSession({
                accessToken: getToken(),
                user: this.user,
                roles: result?.roles || [],
                permissions: result?.permissions || []
            })
        },
        async fetchApplications() {
            const result = await siteApi.progress()
            this.applications = result?.applications || []
        },
        async logout() {
            await authApi.logout()
            this.$router.push('/admin/login')
        }
    }
}
</script>

<style scoped>
.site-page {
    padding: 34px 0;
}

.profile-grid {
    display: grid;
    grid-template-columns: 360px minmax(0, 1fr);
    gap: 20px;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

@media (max-width: 900px) {
    .profile-grid {
        grid-template-columns: 1fr;
    }
}
</style>
