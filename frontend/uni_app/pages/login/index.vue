<template>
    <view class="page oa-page-transition">
        <view class="brand">
            <image class="brand__logo-img" mode="aspectFit" src="/static/logo.png"/>
            <text class="brand__title">开放原子开源社团</text>
            <text class="brand__sub">连接校园开发者与开源协作</text>
        </view>

        <LoginForm
            :initial-password="remembered.password"
            :initial-username="remembered.username"
            :loading="accountSubmitting"
            @submit="onLogin"
        />

        <view class="quick-login">
            <view class="divider">
                <view class="divider__line"></view>
                <text class="divider__text">快捷登录</text>
                <view class="divider__line"></view>
            </view>
            <button class="wechat-button" :disabled="wechatSubmitting" @tap="onMiniappLogin">
                {{ wechatSubmitting ? '登录中...' : '微信一键登录' }}
            </button>
        </view>

        <!--        <view class="footer">-->
        <!--            <text class="footer__text">还没有账号？</text>-->
        <!--            <text class="footer__link" @tap="goRegister">立即注册</text>-->
        <!--        </view>-->
    </view>
</template>

<script lang="ts" setup>
import LoginForm from './components/LoginForm.vue'
import {authApi} from '@/api'
import {getRememberedLogin, setRememberedLogin, setSession} from '@/utils/auth'
import store from '@/store'
import {onMounted, reactive, ref} from 'vue'
import {onLoad} from '@dcloudio/uni-app'
import type {LoginFormData} from './types'

const accountSubmitting = ref(false)
const wechatSubmitting = ref(false)
const redirectUrl = ref('/pages/home/index')

const remembered = reactive({
    username: '',
    password: '',
})

onMounted(() => {
    const saved = getRememberedLogin()
    if (saved) {
        remembered.username = saved.username || ''
        remembered.password = saved.password || ''
    }
})

onLoad((options?: {redirect?: string}) => {
    if (!options?.redirect) return
    try {
        const decoded = decodeURIComponent(options.redirect)
        if (decoded.startsWith('/pages/')) redirectUrl.value = decoded
    } catch {
        redirectUrl.value = '/pages/home/index'
    }
})

async function onLogin(data: LoginFormData) {
    if (accountSubmitting.value || wechatSubmitting.value) return
    accountSubmitting.value = true
    try {
        const res: any = await authApi.login({
            username: data.username,
            password: data.password,
        })

        setRememberedLogin({
            username: data.username,
            password: data.password,
        })
        finishLogin(res)
    } catch {
        // error toast handled by request interceptor
    } finally {
        accountSubmitting.value = false
    }
}

async function onMiniappLogin() {
    if (wechatSubmitting.value || accountSubmitting.value) return
    wechatSubmitting.value = true
    try {
        const loginResult: any = await uniLogin()
        const code = loginResult?.code || ''
        if (!code) {
            uni.showToast({title: '微信登录失败，请重试', icon: 'none'})
            return
        }
        const res: any = await authApi.miniappLogin({code})
        finishLogin(res)
    } catch (error: any) {
        uni.showToast({title: error?.message || '微信登录失败，请重试', icon: 'none'})
    } finally {
        wechatSubmitting.value = false
    }
}

function uniLogin() {
    return new Promise((resolve, reject) => {
        uni.login({
            provider: 'weixin',
            success: resolve,
            fail: reject,
        })
    })
}

function finishLogin(res: any) {
    const token = res?.accessToken || res?.token || ''
    const refreshToken = res?.refreshToken || ''
    const user = res?.user || res?.info || null

    setSession({
        accessToken: token,
        refreshToken,
        user,
        roles: res?.roles || [],
        permissions: res?.permissions || [],
    })

    store.setToken(token)
    if (user) {
        store.setUserInfo(user)
    }

    uni.showToast({title: '登录成功', icon: 'success'})
    setTimeout(() => {
        if (needsProfileCompletion(user)) {
            uni.showModal({
                title: '请补全资料',
                content: '系统已为你创建账号，请登录后补全手机号、学号、院系等个人信息。',
                showCancel: false,
                success: () => uni.reLaunch({url: '/pages/profile/index'}),
            })
            return
        }
        const target = redirectUrl.value || '/pages/home/index'
        uni.redirectTo({
            url: target,
            fail: () => uni.reLaunch({url: target}),
        })
    }, 400)
}

function goRegister() {
    uni.showToast({title: '注册功能开发中', icon: 'none'})
}

function needsProfileCompletion(user: Record<string, any> | null) {
    if (!user) return false
    return !user.phone || !user.college || !user.major || !user.className
}
</script>

<style lang="scss" scoped>
.page {
    display: flex;
    flex-direction: column;
    //align-items: center;
    justify-content: center;
    min-height: 100vh;
    padding-bottom: 80rpx;
    gap: 40px;
    background: linear-gradient(180deg, #ececef 0%, #f5f5f7 48%, #f5f5f7 100%);
    box-sizing: border-box;
}

.brand {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 80rpx 90rpx 56rpx;
}

.brand__logo {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 140rpx;
    height: 140rpx;
    margin-bottom: 24rpx;
    border-radius: 34rpx;
    background: rgba(255, 255, 255, 0.78);
    box-shadow: 0 20rpx 42rpx rgba(0, 0, 0, 0.12);
}

.brand__logo-img {
    width: 200rpx;
    height: 200rpx;
}

.brand__title {
    font-size: 38rpx;
    font-weight: 800;
    color: #1d1d1f;
    letter-spacing: 1rpx;
}

.brand__sub {
    margin-top: 12rpx;
    font-size: 26rpx;
    color: #666668;
}

.footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    margin-top: 48rpx;
}

.footer__text {
    font-size: 26rpx;
    color: #8e8e93;
}

.footer__link {
    font-size: 26rpx;
    font-weight: 600;
    color: #1d1d1f;
}

.quick-login {
    padding: 0 48rpx;
}

.divider {
    display: flex;
    align-items: center;
    gap: 18rpx;
    margin: 8rpx 0 28rpx;
}

.divider__line {
    flex: 1;
    height: 1rpx;
    background: #dbe6f5;
}

.divider__text {
    color: #8e8e93;
    font-size: 24rpx;
}

.wechat-button {
    height: 92rpx;
    line-height: 92rpx;
    border-radius: 16rpx;
    color: #fff;
    background: #07c160;
    font-size: 30rpx;
    font-weight: 700;
}

.wechat-button::after {
    border: 0;
}
</style>
