<template>
    <view class="skeleton-list">
        <view v-for="i in count" :key="i" class="sk-card"/>
    </view>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ count?: number }>(), {
    count: 3,
})
</script>

<style scoped>
.skeleton-list {
    padding: 0 24rpx;
}

.sk-card {
    height: 190rpx;
    margin-bottom: 18rpx;
    border-radius: 16rpx;
    background: linear-gradient(90deg, #eef2f7, #f8fafc, #eef2f7);
    background-size: 200% 100%;
    animation: shimmer 1.2s ease-in-out infinite;
}

@keyframes shimmer {
    0% { background-position: 120% 0; }
    100% { background-position: -120% 0; }
}
</style>
