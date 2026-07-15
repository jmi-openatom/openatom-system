<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="true" title="扫码签到"/>
        <view class="content">
            <view class="panel">
                <text class="title">{{ title }}</text>
                <text class="desc">{{ desc }}</text>
                <button class="primary-btn" :disabled="submitting" @tap="scan">
                    {{ submitting ? '签到中...' : '扫描签到码' }}
                </button>
            </view>
        </view>
    </view>
</template>

<script setup lang="ts">
import {checkInApi} from '@/api'
import {getToken} from '@/utils/auth'
import {computed, ref} from 'vue'
import {onLoad, onShow} from '@dcloudio/uni-app'

const token = ref('')
const submitting = ref(false)
const PENDING_CHECKIN_TOKEN = 'openatom_pending_checkin_token'
const title = computed(() => submitting.value ? '正在签到' : '内部签到')
const desc = ref('请使用小程序扫描后台全屏预览中的签到二维码。')

function normalizeToken(value?: string) {
    const text = (value || '').trim()
    if (!text) return ''
    const urlMatch = text.match(/[?&]t=([^&]+)/)
    if (urlMatch) return decodeURIComponent(urlMatch[1])
    const prefix = 'openatom-checkin:'
    if (text.startsWith(prefix)) return text.slice(prefix.length)
    return text
}

async function submit(value: string) {
    const normalized = normalizeToken(value)
    if (!normalized) {
        uni.showToast({title: '签到码无效', icon: 'none'})
        return
    }
    if (!getToken()) {
        uni.showToast({title: '请先登录后签到', icon: 'none'})
        uni.setStorageSync(PENDING_CHECKIN_TOKEN, normalized)
        uni.navigateTo({url: '/pages/login/index'})
        return
    }
    uni.removeStorageSync(PENDING_CHECKIN_TOKEN)
    submitting.value = true
    try {
        const res: any = await checkInApi.scan(normalized)
        desc.value = `${res?.realName || '你'} 已签到成功`
        uni.showToast({title: '签到成功', icon: 'success'})
    } catch {
        // request interceptor already shows the error
    } finally {
        submitting.value = false
    }
}

function scan() {
    uni.scanCode({
        onlyFromCamera: true,
        success: (res) => submit(res.result),
        fail: () => uni.showToast({title: '未识别到签到码', icon: 'none'}),
    })
}

onLoad((options?: { token?: string }) => {
    token.value = options?.token || ''
    if (token.value) {
        submit(token.value)
    }
})

onShow(() => {
    if (submitting.value || !getToken()) return
    const pending = uni.getStorageSync(PENDING_CHECKIN_TOKEN)
    if (typeof pending === 'string' && pending) {
        token.value = pending
        submit(pending)
    }
})
</script>

<style scoped>
.page {
    min-height: 100vh;
    background: #f5f5f7;
}

.content {
    padding: 64rpx 28rpx;
}

.panel {
    display: flex;
    flex-direction: column;
    gap: 22rpx;
    padding: 42rpx 30rpx;
    border-radius: 18rpx;
    background: #fff;
    box-shadow: 0 14rpx 34rpx rgba(0, 0, 0, .08);
}

.title {
    color: #1d1d1f;
    font-size: 40rpx;
    font-weight: 800;
}

.desc {
    color: #666668;
    font-size: 26rpx;
    line-height: 1.6;
}

.primary-btn {
    margin-top: 18rpx;
    height: 86rpx;
    line-height: 86rpx;
    border-radius: 16rpx;
    color: #fff;
    background: #1d1d1f;
    font-size: 29rpx;
    font-weight: 700;
}

.primary-btn[disabled] {
    background: #d2d2d7;
}
</style>
