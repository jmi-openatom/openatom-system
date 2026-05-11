<script lang="ts" setup>
import type {ActivityFilter, ActivityStatus} from '../types'

withDefaults(
    defineProps<{
        filters?: ActivityFilter[]
        activeFilter?: ActivityStatus
    }>(),
    {
        filters: () => [],
        activeFilter: '',
    },
)

const emit = defineEmits<{
    change: [value: ActivityStatus]
}>()

function onSelect(value: ActivityStatus) {
    emit('change', value)
}
</script>

<template>
    <view class="filter-bar">
        <scroll-view :show-scrollbar="false" class="filter-scroll" enable-flex scroll-x>
            <view class="filter-inner">
                <view
                    v-for="f in filters"
                    :key="f.value"
                    :class="['filter-pill', { 'filter-pill--active': activeFilter === f.value }]"
                    @tap="onSelect(f.value)"
                >
                    <text>{{ f.label }}</text>
                </view>
            </view>
        </scroll-view>
    </view>
</template>

<style lang="scss" scoped>
.filter-bar {
    position: sticky;
    top: 0;
    z-index: 10;
    padding: 16rpx 0 8rpx;
    //background: linear-gradient(180deg, rgba(248, 250, 252, 0.98), rgba(248, 250, 252, 0.85));
    backdrop-filter: blur(20rpx);
}

.filter-scroll {
    width: 100%;
    white-space: nowrap;
}

.filter-inner {
    display: inline-flex;
    gap: 16rpx;
    padding: 0 24rpx;
}

.filter-pill {
    display: inline-flex;
    align-items: center;
    height: 60rpx;
    padding: 0 28rpx;
    border-radius: 999rpx;
    background: #fff;
    border: 1rpx solid rgba(226, 232, 240, 0.95);
    box-shadow: 0 4rpx 12rpx rgba(31, 55, 88, 0.04);
    font-size: 26rpx;
    font-weight: 500;
    color: #64748b;
    transition: all 0.2s;
}

.filter-pill--active {
    color: #fff;
    background: linear-gradient(135deg, #1769e8, #3a84ff);
    border-color: transparent;
    box-shadow: 0 8rpx 20rpx rgba(23, 105, 232, 0.28);
    font-weight: 600;
}
</style>
