<template>
    <view class="menu-group">
        <view v-for="item in menus" :key="item.label" class="menu-item" @tap="emit('select', item)">
            <text class="menu-icon">{{ item.icon }}</text>
            <text class="menu-label">{{ item.label }}</text>
            <text v-if="item.badge" class="menu-badge">{{ item.badge }}</text>
            <text class="menu-arrow">›</text>
        </view>
    </view>
</template>

<script setup lang="ts">
export interface ProfileMenuItem {
    icon: string
    label: string
    path?: string
    badge?: string | number
    action?: string
}

defineProps<{
    menus: ProfileMenuItem[]
}>()

const emit = defineEmits<{
    select: [item: ProfileMenuItem]
}>()
</script>

<style scoped>
.menu-group {
    margin: 24rpx;
    background: #fff;
    border-radius: 18rpx;
    overflow: hidden;
    box-shadow: 0 12rpx 30rpx rgba(31, 55, 88, .08);
}

.menu-item {
    display: flex;
    align-items: center;
    padding: 28rpx 30rpx;
    border-bottom: 1rpx solid #f0f0f0;
}

.menu-item:last-child {
    border-bottom: 0;
}

.menu-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 52rpx;
    height: 52rpx;
    border-radius: 14rpx;
    background: #eef6ff;
    color: #1769e8;
    font-size: 24rpx;
    font-weight: 800;
    margin-right: 20rpx;
    flex-shrink: 0;
}

.menu-label {
    flex: 1;
    font-size: 30rpx;
    color: #333;
}

.menu-arrow {
    font-size: 36rpx;
    color: #ccc;
}

.menu-badge {
    min-width: 34rpx;
    height: 34rpx;
    line-height: 34rpx;
    margin-right: 14rpx;
    padding: 0 8rpx;
    border-radius: 999rpx;
    text-align: center;
    background: #ef4444;
    color: #fff;
    font-size: 20rpx;
}
</style>
