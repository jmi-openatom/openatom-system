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
            :loading="submitting"
            @submit="onLogin"
        />

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
import type {LoginFormData} from './types'

const submitting = ref(false)

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

async function onLogin(data: LoginFormData) {
    submitting.value = true
    try {
        const res: any = await authApi.login({
            username: data.username,
            password: data.password,
        })

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

        setRememberedLogin({
            username: data.username,
            password: data.password,
        })

        uni.showToast({title: '登录成功', icon: 'success'})
        setTimeout(() => {
            uni.navigateBack({
                fail: () => {
                    uni.reLaunch({url: '/pages/home/index'})
                },
            })
        }, 400)
    } catch {
        // error toast handled by request interceptor
    } finally {
        submitting.value = false
    }
}

function goRegister() {
    uni.showToast({title: '注册功能开发中', icon: 'none'})
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
    background: linear-gradient(180deg, #f1f5f9 0%, #f8fafc 48%, #f8fafc 100%);
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
    box-shadow: 0 20rpx 42rpx rgba(35, 61, 91, 0.12);
}

.brand__logo-img {
    width: 200rpx;
    height: 200rpx;
}

.brand__title {
    font-size: 38rpx;
    font-weight: 800;
    color: #0f172a;
    letter-spacing: 1rpx;
}

.brand__sub {
    margin-top: 12rpx;
    font-size: 26rpx;
    color: #64748b;
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
    color: #94a3b8;
}

.footer__link {
    font-size: 26rpx;
    font-weight: 600;
    color: #1769e8;
}
</style>
