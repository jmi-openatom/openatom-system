<script lang="ts" setup>
import type {ActivityDetail} from '../../types'
import {computed} from 'vue'

const props = defineProps<{
    data?: ActivityDetail | null
}>()

const statusText = computed(() => {
    const m: Record<string, string> = {
        draft: '草稿',
        published: '进行中',
        closed: '已结束',
        cancelled: '已取消',
    }
    return (props.data?.status && m[props.data.status]) || props.data?.status || ''
})

const statusClass = computed(() => {
    const m: Record<string, string> = {
        published: 'badge--active',
        draft: 'badge--draft',
        cancelled: 'badge--danger',
    }
    return (props.data?.status && m[props.data.status]) || 'badge--closed'
})
</script>

<template>
    <tm-sheet>
        <view class="banner">
            <view class="banner__cover-wrap">
                <image
                    v-if="data?.coverUrl"
                    :src="data.coverUrl"
                    class="banner__cover"
                    mode="aspectFill"
                />
                <view v-else class="banner__cover banner__cover--ph">
                    <tm-icon :size="48" name="calendar-check-line"/>
                </view>
                <view :class="['banner__badge', statusClass]">
                    <text>{{ statusText }}</text>
                </view>
            </view>

            <view class="banner__body">
                <text class="banner__title">{{ data?.title || '加载中...' }}</text>

                <view v-if="data?.tags?.length" class="banner__tags">
                    <text v-for="tag in data.tags" :key="tag" class="banner__tag">{{ tag }}</text>
                </view>

                <view class="banner__meta">
                    <view v-if="data?.date" class="banner__meta-item">
                        <tm-icon :size="20" name="calendar-check-line"/>
                        <text class="banner__meta-text">{{ data.date }}</text>
                    </view>
                    <view v-if="data?.location" class="banner__meta-item">
                        <tm-icon :size="20" name="map-pin-line"/>
                        <text class="banner__meta-text">{{ data.location }}</text>
                    </view>
                </view>
            </view>
        </view>
    </tm-sheet>
</template>

<style lang="scss" scoped>
.banner {
    overflow: hidden;
    border-radius: 20rpx;
}

.banner__cover-wrap {
    position: relative;
    width: 100%;
    height: 360rpx;
    overflow: hidden;
}

.banner__cover {
    width: 100%;
    height: 100%;
}

.banner__cover--ph {
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 40%, #e0e7ff 100%);
}

.banner__badge {
    position: absolute;
    top: 20rpx;
    right: 20rpx;
    padding: 8rpx 22rpx;
    border-radius: 999rpx;
    font-size: 24rpx;
    font-weight: 600;
    color: #fff;
}

.badge--active {
    background: rgba(33, 182, 168, 0.88);
}

.badge--closed {
    background: rgba(148, 163, 184, 0.82);
}

.badge--danger {
    background: rgba(238, 10, 36, 0.82);
}

.badge--draft {
    background: rgba(245, 158, 11, 0.82);
}

.banner__body {
    padding: 28rpx;
}

.banner__title {
    display: block;
    font-size: 36rpx;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.4;
}

.banner__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: 18rpx;
}

.banner__tag {
    padding: 4rpx 16rpx;
    border-radius: 8rpx;
    font-size: 22rpx;
    color: #1769e8;
    background: rgba(23, 105, 232, 0.08);
    font-weight: 500;
}

.banner__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 24rpx;
    margin-top: 22rpx;
    padding-top: 22rpx;
    border-top: 1rpx solid rgba(226, 232, 240, 0.6);
}

.banner__meta-item {
    display: flex;
    align-items: center;
    gap: 8rpx;
}

.banner__meta-text {
    font-size: 26rpx;
    color: #64748b;
}
</style>
