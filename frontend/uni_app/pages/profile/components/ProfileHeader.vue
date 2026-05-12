<template>
    <view class="user-card oa-animate-in">
        <view class="avatar">
            <text class="avatar-icon">{{ avatarText }}</text>
        </view>
        <text class="nickname">{{ userName }}</text>
        <text class="subtitle">{{ isLogin ? '已登录' : '登录后查看申请与消息' }}</text>
        <button v-if="!isLogin" class="login-button" @tap="emit('login')">立即登录</button>
    </view>
</template>

<script setup lang="ts">
defineProps<{
    userName: string
    avatarText: string
    isLogin: boolean
}>()

const emit = defineEmits<{
    login: []
}>()
</script>

<style scoped>
.user-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 60rpx 0 40rpx;
    margin: 20rpx 24rpx;
    border-radius: 20rpx;
    background: #ffffff;
    border: 1rpx solid #dbeafe;
    box-shadow: 0 12rpx 28rpx rgba(15, 23, 42, 0.06);
}

.avatar {
    width: 120rpx;
    height: 120rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #eff6ff;
    border-radius: 28rpx;
    margin-bottom: 16rpx;
    border: 1rpx solid #bfdbfe;
}

.avatar-icon {
    font-size: 44rpx;
    color: #1769e8;
    font-weight: 800;
}

.nickname {
    font-size: 36rpx;
    color: #0f172a;
    font-weight: 600;
}

.subtitle {
    font-size: 26rpx;
    color: #64748b;
    margin-top: 8rpx;
}

.login-button {
    margin-top: 24rpx;
    width: 180rpx;
    height: 64rpx;
    line-height: 64rpx;
    border-radius: 999rpx;
    background: #1769e8;
    color: #fff;
    font-size: 24rpx;
}

.login-button::after {
    border: 0;
}
</style>
