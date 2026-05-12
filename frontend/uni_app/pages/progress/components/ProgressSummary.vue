<template>
    <view class="summary oa-animate-in">
        <text class="summary__label">当前账号</text>
        <text class="summary__title">{{ isLogin ? displayName : '未登录' }}</text>
        <text class="summary__desc">{{ isLogin ? '申请、审核和面试安排会在这里同步。' : '登录后可查看个人入会进度。' }}</text>
        <button v-if="!isLogin" class="primary-btn" @tap="emit('login')">去登录</button>
        <button v-else class="ghost-btn" @tap="emit('refresh')">刷新进度</button>
    </view>
</template>

<script setup lang="ts">
defineProps<{
    isLogin: boolean
    displayName: string
}>()

const emit = defineEmits<{
    login: []
    refresh: []
}>()
</script>

<style scoped>
.summary {
    margin: 20rpx 24rpx;
    padding: 30rpx;
    border-radius: 18rpx;
    background: #fff;
    box-shadow: 0 12rpx 30rpx rgba(31, 55, 88, .08);
}

.summary__label,
.summary__title,
.summary__desc {
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
</style>
