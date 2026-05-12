<template>
    <view class="empty">
        <text class="empty__title">暂无开放中的招新</text>
        <text class="empty__desc">可以先看看近期活动，或稍后下拉刷新。</text>
        <button class="ghost-btn" @tap="emit('activities')">查看活动</button>
    </view>
</template>

<script setup lang="ts">
const emit = defineEmits<{
    activities: []
}>()
</script>

<style scoped>
.empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 120rpx 40rpx;
    text-align: center;
}

.empty__title {
    font-size: 32rpx;
    font-weight: 700;
    color: #0f172a;
}

.empty__desc {
    margin-top: 12rpx;
    color: #64748b;
    font-size: 26rpx;
}

.ghost-btn {
    margin-top: 28rpx;
    height: 72rpx;
    line-height: 72rpx;
    padding: 0 34rpx;
    border-radius: 999rpx;
    background: #1769e8;
    color: #fff;
    font-size: 26rpx;
}

.ghost-btn::after {
    border: 0;
}
</style>
