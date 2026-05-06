<template>
    <div class="site-page">
        <section class="container profile-grid">
            <el-card class="profile-card" shadow="never">
                <template #header>
                    <div class="card-header">
                        <span>我的账号</span>
                        <el-tag v-if="isLogin" size="small" type="success">已登录</el-tag>
                    </div>
                </template>

                <el-descriptions :column="1" border>
                    <el-descriptions-item label="用户名">{{ user.userName || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="姓名">{{ user.realName || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="手机号">{{ user.phone || '-' }}</el-descriptions-item>
                </el-descriptions>

                <div class="btn-group">
                    <el-button class="w-full" @click="logout">退出登录</el-button>
                    <el-button class="w-full" plain type="primary" @click="passwordDialogVisible = true">修改密码
                    </el-button>
                </div>
            </el-card>

            <el-card shadow="never">
                <template #header>
                    <span>我的申请进度</span>
                </template>
                <el-table :data="applications" border stripe>
                    <el-table-column label="申请编号" prop="id" width="100"/>
                    <el-table-column label="社团名称" min-width="150" prop="clubName"/>
                    <el-table-column label="当前状态" prop="status" width="120">
                        <template #default="{ row }">
                            <el-tag :type="statusType(row.status)">{{ row.status || '审核中' }}</el-tag>
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
            <el-form
                ref="pwdFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-position="top"
            >
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
                    <el-button :loading="submitting" type="primary" @click="submitPassword">确认修改</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import {authApi, siteApi} from '../../api'
import {getCurrentUser, getToken, setSession} from '../../utils/auth'
import {formatDateTime, statusType} from '../../utils/format'
import {ElMessage} from 'element-plus'

export default {
    name: 'SiteProfile',
    data() {
        return {
            user: getCurrentUser() || {},
            applications: [],
            // 修改密码相关
            passwordDialogVisible: false,
            submitting: false,
            passwordForm: {
                oldPassword: '',
                newPassword: ''
            },
            passwordRules: {
                oldPassword: [{required: true, message: '请输入旧密码', trigger: 'blur'}],
                newPassword: [
                    {required: true, message: '请输入新密码', trigger: 'blur'},
                    {min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur'}
                ]
            }
        }
    },
    computed: {
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
            if (result?.user) {
                this.user = result.user
                setSession({accessToken: getToken(), user: this.user})
            }
        },
        async fetchApplications() {
            const result = await siteApi.progress()
            this.applications = result?.applications || []
        },
        async logout() {
            await authApi.logout()
            localStorage.clear()
            this.$router.push('/admin/login')
        },
        // 提交密码修改
        async submitPassword() {
            this.$refs.pwdFormRef.validate(async (valid) => {
                if (!valid) return
                try {
                    this.submitting = true
                    await authApi.updatePassword(this.passwordForm)
                    ElMessage.success('密码更新成功，请使用新密码重新登录')
                    this.passwordDialogVisible = false
                    this.logout() // 强制重新登录以保证安全
                } catch (error) {
                    console.error('修改失败:', error)
                } finally {
                    this.submitting = false
                }
            })
        },
        resetForm() {
            this.$refs.pwdFormRef?.resetFields()
        }
    }
}
</script>

<style scoped>
.site-page {
    padding: 40px 0;
}

.profile-grid {
    display: grid;
    grid-template-columns: 360px 1fr;
    gap: 24px;
    align-items: start;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
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