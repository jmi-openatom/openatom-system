<template>
    <div class="login-page">
        <div class="login-panel">
            <div class="login-copy">
                <span class="login-logo">OA</span>
                <h1>OpenAtom 登录</h1>
                <p>JMI-OPENATOM</p>
            </div>
            <el-card class="login-card" shadow="never">
                <el-tabs v-model="activeTab">
                    <el-tab-pane label="账号登录" name="login">
                        <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top">
                            <el-form-item label="用户名" prop="username">
                                <el-input v-model="loginForm.username" placeholder="请输入用户名"/>
                            </el-form-item>
                            <el-form-item label="密码" prop="password">
                                <el-input v-model="loginForm.password" placeholder="请输入密码" show-password
                                          type="password" @keyup.enter="handleLogin"/>
                            </el-form-item>
                            <div class="login-options">
                                <el-checkbox v-model="rememberPassword">记住密码</el-checkbox>
                            </div>
                            <el-button :loading="loading" class="full-btn" type="primary" @click="handleLogin">登录
                            </el-button>
                        </el-form>
                    </el-tab-pane>
                    <el-tab-pane v-if="registerEnabled" label="注册账号" name="register">
                        <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-position="top">
                            <el-form-item label="用户名" prop="username">
                                <el-input v-model="registerForm.username"/>
                            </el-form-item>
                            <el-form-item label="姓名" prop="realName">
                                <el-input v-model="registerForm.realName"/>
                            </el-form-item>
                            <el-form-item label="手机号">
                                <el-input v-model="registerForm.phone"/>
                            </el-form-item>
                            <el-form-item label="邮箱">
                                <el-input v-model="registerForm.email"/>
                            </el-form-item>
                            <el-form-item label="密码" prop="password">
                                <el-input v-model="registerForm.password" show-password type="password"/>
                            </el-form-item>
                            <el-button :loading="loading" class="full-btn" type="primary" @click="handleRegister">
                                注册并登录
                            </el-button>
                        </el-form>
                    </el-tab-pane>
                </el-tabs>
            </el-card>
        </div>
    </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { authApi, siteApi } from '../../api'
import { clearRememberedLogin, getRememberedLogin, setRememberedLogin, setSession } from '../../utils/auth'

export default {
    name: 'AdminLogin',
    data() {
        return {
            activeTab: 'login',
            registerEnabled: false,
            loading: false,
            rememberPassword: false,
            loginForm: {
                username: '',
                password: ''
            },
            registerForm: {
                username: '',
                password: '',
                realName: '',
                phone: '',
                email: ''
            },
            loginRules: {
                username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
                password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
            },
            registerRules: {
                username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
                realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
                password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
            }
        }
    },
    async created() {
        const rememberedLogin = getRememberedLogin()
        this.loginForm.username = rememberedLogin.username || ''
        this.loginForm.password = rememberedLogin.password || ''
        this.rememberPassword = Boolean(rememberedLogin.remember)
        await this.fetchRegisterEnabled()
    },
    methods: {
        async fetchRegisterEnabled() {
            try {
                this.registerEnabled = Boolean(await siteApi.registerEnabled())
                if (!this.registerEnabled && this.activeTab === 'register') {
                    this.activeTab = 'login'
                }
            } catch (e) {
                this.registerEnabled = false
            }
        },

        // 核心跳转逻辑封装
        completeAuth(sessionData, message) {
            // 1. 处理记住密码逻辑
            if (this.rememberPassword) {
                setRememberedLogin({
                    username: this.loginForm.username,
                    password: this.loginForm.password,
                    remember: true
                })
            } else {
                clearRememberedLogin()
            }

            // 2. 设置 Session
            setSession(sessionData)
            ElMessage.success(message)

            // 3. 智能跳转
            // 情况A: URL 带有 redirect 参数 (通常来自拦截器)
            const redirectPath = this.$route.query.redirect
            if (redirectPath) {
                this.$router.replace(redirectPath)
            }
            // 情况B: 浏览器有历史记录
            else if (window.history.length > 1) {
                this.$router.back()
            }
            // 情况C: 兜底跳转首页
            else {
                this.$router.replace('/')
            }
        },

        handleLogin() {
            this.$refs.loginRef.validate(async (valid) => {
                if (!valid) return
                this.loading = true
                try {
                    const result = await authApi.login(this.loginForm)
                    this.completeAuth(result, '登录成功')
                } catch (err) {
                    // 错误处理通常在拦截器中完成
                } finally {
                    this.loading = false
                }
            })
        },

        handleRegister() {
            if (!this.registerEnabled) {
                ElMessage.warning('当前已关闭注册')
                this.activeTab = 'login'
                return
            }
            this.$refs.registerRef.validate(async (valid) => {
                if (!valid) return
                this.loading = true
                try {
                    await authApi.register(this.registerForm)
                    // 注册后自动执行登录
                    const result = await authApi.login({
                        username: this.registerForm.username,
                        password: this.registerForm.password
                    })
                    this.completeAuth(result, '注册并登录成功')
                } catch (err) {
                    // 错误处理
                } finally {
                    this.loading = false
                }
            })
        }
    }
}
</script>

<style scoped>
/* 保持你原有的 CSS 代码，此处略 */
.login-page {
    min-height: 100vh;
    display: grid;
    place-items: center;
    padding: 32px;
    background: radial-gradient(circle at left top, rgba(147, 197, 253, 0.34), transparent 34%),
    radial-gradient(circle at right center, rgba(191, 219, 254, 0.56), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #edf4ff 100%);
}

.login-panel {
    display: grid;
    width: min(980px, 100%);
    grid-template-columns: 1fr 390px;
    gap: 28px;
    align-items: center;
}

.login-copy {
    color: #1e293b;
}

.login-logo {
    display: grid;
    width: 54px;
    height: 54px;
    place-items: center;
    color: #fff;
    background: linear-gradient(135deg, #60a5fa, #2563eb);
    border-radius: 16px;
    font-weight: 800;
    box-shadow: 0 14px 28px rgba(37, 99, 235, 0.18);
}

.login-copy h1 {
    margin: 18px 0 12px;
    font-size: 42px;
    letter-spacing: -0.03em;
}

.login-copy p {
    max-width: 520px;
    color: #475569;
    line-height: 1.8;
}

.login-card {
    border: 1px solid rgba(219, 230, 245, 0.95);
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.84);
    box-shadow: 0 24px 60px rgba(37, 99, 235, 0.12);
    backdrop-filter: blur(16px);
}

.full-btn {
    width: 100%;
}

.login-options {
    display: flex;
    justify-content: flex-end;
    margin: -4px 0 16px;
}

.login-card :deep(.el-card__body) {
    padding: 24px 24px 18px;
}

.login-card :deep(.el-tabs__nav-wrap::after) {
    background-color: rgba(219, 230, 245, 0.9);
}

@media (max-width: 860px) {
    .login-panel {
        grid-template-columns: 1fr;
    }
}
</style>