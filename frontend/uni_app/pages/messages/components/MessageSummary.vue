<template>
    <view class="top-card oa-animate-in">
        <text class="top-card__label">消息中心</text>
        <text class="top-card__title">{{ unreadCount }} 条未读</text>
        <button v-if="unreadCount" class="read-btn" @tap="emit('readAll')">全部已读</button>
    </view>
</template>

<script setup lang="ts">
defineProps<{
    unreadCount: number
}>()

const emit = defineEmits<{
    readAll: []
}>()
</script>

<style scoped>
.top-card {
    margin: 20rpx 24rpx;
    padding: 32rpx;
    border-radius: 18rpx;
    color: #1d1d1f;
    background: #ffffff;
    border: 1rpx solid #dbeafe;
    box-shadow: 0 12rpx 28rpx rgba(15, 23, 42, 0.06);
}

.top-card__label,
.top-card__title {
    display: block;
}

.top-card__label {
    color: #1d1d1f;
    font-size: 23rpx;
    font-weight: 700;
}

.top-card__title {
    margin-top: 8rpx;
    font-size: 42rpx;
    font-weight: 800;
}

.read-btn {
    margin: 24rpx 0 0;
    width: 180rpx;
    height: 64rpx;
    line-height: 64rpx;
    border-radius: 16rpx;
    background: #1d1d1f;
    color: #fff;
    font-size: 24rpx;
}

.read-btn::after {
    border: 0;
}
</style>
