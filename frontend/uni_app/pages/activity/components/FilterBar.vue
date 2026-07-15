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
        <view class="filter-track">
            <view
                v-for="f in filters"
                :key="f.value"
                :class="['filter-segment', { 'filter-segment--active': activeFilter === f.value }]"
                @tap="onSelect(f.value)"
            >
                <text class="filter-segment__label">{{ f.label }}</text>
            </view>
        </view>
    </view>
</template>

<style lang="scss" scoped>
.filter-bar {
    position: sticky;
    top: 0;
    z-index: 10;
    padding: 20rpx 24rpx 12rpx;
    backdrop-filter: blur(20rpx);
}

.filter-track {
    display: flex;
    padding: 6rpx;
    border-radius: 20rpx;
    background: #ececef;
    border: 1rpx solid rgba(226, 232, 240, 0.9);
}

.filter-segment {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 64rpx;
    border-radius: 16rpx;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    cursor: pointer;
}

.filter-segment__label {
    font-size: 28rpx;
    font-weight: 500;
    color: #666668;
    transition: color 0.25s;
}

.filter-segment--active {
    background: #fff;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.08), 0 1rpx 2rpx rgba(0, 0, 0, 0.06);

    .filter-segment__label {
        color: #1d1d1f;
        font-weight: 600;
    }
}
</style>
