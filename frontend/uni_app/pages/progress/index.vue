<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="true" title="申请进度"/>
        <scroll-view
            :refresher-enabled="isLogin"
            :refresher-triggered="refreshing"
            class="main-scroll"
            scroll-y
            @refresherrefresh="refresh"
        >
            <ProgressSummary :display-name="displayName" :is-login="isLogin" @login="goLogin" @refresh="refresh"/>

            <view v-if="isLogin && loading && !applications.length" class="loading">加载中...</view>

            <view v-else-if="isLogin && applications.length" class="progress-list">
                <ProgressCard
                    v-for="(item, index) in applications"
                    :key="item.id || index"
                    :application="item"
                    :index="index"
                />
            </view>

            <ProgressEmpty v-else-if="isLogin" @recruitment="goRecruitment"/>

            <view class="bottom-pad"/>
        </scroll-view>
    </view>
</template>

<script setup lang="ts">
import {authApi, siteApi} from '@/api'
import {getCurrentUser, getToken, setSession} from '@/utils/auth'
import {computed, onMounted, ref} from 'vue'
import ProgressCard from './components/ProgressCard.vue'
import ProgressEmpty from './components/ProgressEmpty.vue'
import ProgressSummary from './components/ProgressSummary.vue'

const loading = ref(false)
const refreshing = ref(false)
const user = ref<Record<string, any>>(getCurrentUser())
const applications = ref<Record<string, any>[]>([])
const isLogin = computed(() => Boolean(getToken()))
const displayName = computed(() => user.value.realName || user.value.userName || user.value.username || '当前用户')

async function fetchUser() {
    const res: any = await authApi.me()
    user.value = res?.user || res || {}
    setSession({accessToken: getToken() || '', user: user.value, roles: res?.roles || [], permissions: res?.permissions || []})
}

async function load() {
    if (!isLogin.value) return
    loading.value = true
    try {
        const res: any = await siteApi.progress()
        applications.value = res?.applications || []
    } finally {
        loading.value = false
        refreshing.value = false
    }
}

async function refresh() {
    refreshing.value = true
    try {
        await fetchUser()
    } catch {}
    await load()
}

function goLogin() {
    uni.navigateTo({url: '/pages/login/index'})
}

function goRecruitment() {
    uni.navigateTo({url: '/pages/recruitment/index'})
}

onMounted(() => {
    if (isLogin.value) {
        refresh()
    }
})
</script>

<style scoped>
.page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background: #f7fafc;
}

.main-scroll {
    flex: 1;
    height: 0;
}

.summary,
.progress-card {
    margin: 20rpx 24rpx;
    padding: 30rpx;
    border-radius: 18rpx;
    background: #fff;
    box-shadow: 0 12rpx 30rpx rgba(31, 55, 88, .08);
}

.summary__label,
.summary__title,
.summary__desc,
.progress-card__title,
.progress-card__meta,
.no-interview {
    display: block;
}

.summary__label {
    color: #1769e8;
    font-size: 23rpx;
    font-weight: 800;
}

.summary__title {
    margin-top: 10rpx;
    color: #0f172a;
    font-size: 40rpx;
    font-weight: 800;
}

.summary__desc {
    margin-top: 10rpx;
    color: #64748b;
    font-size: 25rpx;
}

.primary-btn,
.ghost-btn {
    margin-top: 24rpx;
    height: 74rpx;
    line-height: 74rpx;
    border-radius: 999rpx;
    font-size: 26rpx;
}

.primary-btn {
    color: #fff;
    background: #1769e8;
}

.ghost-btn {
    color: #1769e8;
    background: #eef6ff;
}

.primary-btn::after,
.ghost-btn::after {
    border: 0;
}

.progress-list {
    padding-bottom: 8rpx;
}

.progress-card__head,
.interview__top {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16rpx;
}

.progress-card__title {
    color: #0f172a;
    font-size: 31rpx;
    font-weight: 780;
}

.progress-card__meta {
    margin-top: 8rpx;
    color: #64748b;
    font-size: 24rpx;
}

.status-pill {
    flex-shrink: 0;
    padding: 6rpx 16rpx;
    border-radius: 999rpx;
    font-size: 22rpx;
    font-weight: 700;
}

.status-pill--success { color: #047857; background: #d1fae5; }
.status-pill--warning { color: #a16207; background: #fef3c7; }
.status-pill--muted,
.status-pill--default { color: #64748b; background: #f1f5f9; }

.step-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12rpx;
    margin: 28rpx 0;
}

.step-dot {
    height: 10rpx;
    border-radius: 999rpx;
    background: #e2e8f0;
}

.step-dot.active {
    background: #1769e8;
}

.info-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16rpx;
}

.info-item {
    padding: 18rpx;
    border-radius: 14rpx;
    background: #f8fafc;
}

.info-item text {
    display: block;
    margin-bottom: 8rpx;
    color: #64748b;
    font-size: 22rpx;
}

.info-item strong {
    color: #0f172a;
    font-size: 26rpx;
}

.section-title {
    display: block;
    margin: 26rpx 0 14rpx;
    color: #0f172a;
    font-size: 27rpx;
    font-weight: 800;
}

.interview {
    padding: 18rpx;
    border-radius: 14rpx;
    background: #f8fafc;
    margin-top: 12rpx;
}

.interview__top {
    color: #0f172a;
    font-size: 25rpx;
    font-weight: 700;
}

.interview__line {
    display: block;
    margin-top: 8rpx;
    color: #64748b;
    font-size: 23rpx;
}

.no-interview {
    margin-top: 24rpx;
    color: #64748b;
    font-size: 24rpx;
}

.loading,
.empty {
    padding: 100rpx 40rpx;
    text-align: center;
    color: #64748b;
}

.empty__title,
.empty__desc {
    display: block;
}

.empty__title {
    color: #0f172a;
    font-size: 32rpx;
    font-weight: 800;
}

.empty__desc {
    margin-top: 12rpx;
    color: #64748b;
    font-size: 25rpx;
}

.bottom-pad {
    height: 80rpx;
}
</style>
