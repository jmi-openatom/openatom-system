<script setup lang="ts">
withDefaults(defineProps<{
    title?: string
    description?: string
    actionText?: string
}>(), {
    title: '暂时无法加载',
    description: '请检查网络后重试',
    actionText: '重新加载',
})

defineEmits<{action: []}>()
</script>

<template>
    <view class="state" role="status">
        <view class="state__mark"><view class="state__mark-dot"/></view>
        <text class="state__title">{{ title }}</text>
        <text class="state__description">{{ description }}</text>
        <button v-if="actionText" class="state__action" @tap="$emit('action')">{{ actionText }}</button>
    </view>
</template>

<style scoped>
.state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 120rpx 48rpx;
    text-align: center;
}
.state__mark {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 88rpx;
    height: 88rpx;
    margin-bottom: 26rpx;
    border: 2rpx solid #d2d2d7;
    border-radius: 16rpx;
    background: #fff;
}
.state__mark-dot { width: 16rpx; height: 16rpx; border-radius: 50%; background: #1d1d1f; }
.state__title { color: #1d1d1f; font-size: 32rpx; font-weight: 700; }
.state__description { margin-top: 12rpx; color: #666668; font-size: 26rpx; line-height: 1.6; }
.state__action {
    min-width: 220rpx;
    height: 88rpx;
    margin-top: 30rpx;
    padding: 0 40rpx;
    border-radius: 16rpx;
    color: #fff;
    background: #1d1d1f;
    font-size: 28rpx;
    line-height: 88rpx;
}
.state__action::after { border: 0; }
</style>
