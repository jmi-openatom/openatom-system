<template>
    <view class="empty">
        <text class="empty__title">还没有申请记录</text>
        <text class="empty__desc">去招新页提交第一份入会申请。</text>
        <button class="primary-btn" @tap="emit('recruitment')">查看招新</button>
    </view>
</template>

<script setup lang="ts">
const emit = defineEmits<{
    recruitment: []
}>()
</script>

<style scoped>
.empty {
    padding: 100rpx 40rpx;
    text-align: center;
    color: #666668;
}

.empty__title,
.empty__desc {
    display: block;
}

.empty__title {
    color: #1d1d1f;
    font-size: 32rpx;
    font-weight: 800;
}

.empty__desc {
    margin-top: 12rpx;
    color: #666668;
    font-size: 25rpx;
}

.primary-btn {
    margin-top: 24rpx;
    height: 74rpx;
    line-height: 74rpx;
    border-radius: 16rpx;
    color: #fff;
    background: #1d1d1f;
    font-size: 26rpx;
}

.primary-btn::after {
    border: 0;
}
</style>
