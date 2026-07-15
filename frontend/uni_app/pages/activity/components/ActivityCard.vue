<script lang="ts" setup>
import type {ActivityItem} from '../types'

withDefaults(
    defineProps<{
        activity: ActivityItem
    }>(),
    {
        activity: () => ({}),
    },
)

const emit = defineEmits<{
    tap: [item: ActivityItem]
}>()

function statusText(s?: string) {
    const m: Record<string, string> = {
        draft: '草稿',
        published: '进行中',
        closed: '已结束',
        cancelled: '已取消',
    }
    return (s && m[s]) || s || ''
}

function statusColor(s?: string) {
    const m: Record<string, string> = {
        published: 'active',
        closed: 'gray',
        cancelled: 'danger',
    }
    return (s && m[s]) || 'gray'
}
</script>

<template>
    <tm-sheet>
        <view @tap="emit('tap', activity)">
            <view class="card__cover-wrap">
                <image
                    v-if="activity.coverUrl"
                    :src="activity.coverUrl"
                    class="card__cover"
                    mode="aspectFill"
                />
                <view v-else class="card__cover card__cover--ph">
                    <view class="card__cover-icon">
                        <view class="card__cover-dot"/>
                    </view>
                </view>
                <view :class="['card__badge', `card__badge--${statusColor(activity.status)}`]">
                    <text>{{ statusText(activity.status) }}</text>
                </view>
            </view>

            <view class="card__body">
                <text class="card__title">{{ activity.title }}</text>

                <view v-if="activity.tags && activity.tags.length" class="card__tags">
                    <text v-for="tag in activity.tags" :key="tag" class="card__tag">{{ tag }}</text>
                </view>

                <view class="card__meta">
                    <view v-if="activity.date" class="card__meta-item">
                        <text class="card__meta-icon">&#xe63f;</text>
                        <text class="card__meta-text">{{ activity.date }}</text>
                    </view>
                    <view v-if="activity.location" class="card__meta-item">
                        <text class="card__meta-icon">&#xe632;</text>
                        <text class="card__meta-text">{{ activity.location }}</text>
                    </view>
                    <view v-if="activity.maxParticipants" class="card__meta-item">
                        <text class="card__meta-icon">&#xe636;</text>
                        <text class="card__meta-text">{{
                                activity.currentParticipants || 0
                            }}/{{ activity.maxParticipants }} 人
                        </text>
                    </view>
                </view>
            </view>
        </view>
    </tm-sheet>
</template>

<style lang="scss" scoped>
.card {
    margin-bottom: 24rpx;
    border-radius: 20rpx;
    overflow: hidden;
    background: #fff;
    border: 1rpx solid rgba(226, 232, 240, 0.95);
    box-shadow: 0 12rpx 28rpx rgba(0, 0, 0, 0.08);
    transition: transform 0.15s;
}

.card:active {
    transform: scale(0.985);
}

.card__cover-wrap {
    position: relative;
    width: 100%;
    height: 280rpx;
    overflow: hidden;
}

.card__cover {
    width: 100%;
    height: 100%;
}

.card__cover--ph {
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 40%, #e0e7ff 100%);
}

.card__cover-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.55);
    backdrop-filter: blur(8rpx);
}

.card__cover-dot {
    width: 28rpx;
    height: 28rpx;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.28);
}

.card__badge {
    position: absolute;
    top: 16rpx;
    right: 16rpx;
    padding: 6rpx 18rpx;
    border-radius: 16rpx;
    font-size: 22rpx;
    font-weight: 600;
    backdrop-filter: blur(12rpx);
}

.card__badge--active {
    color: #fff;
    background: rgba(33, 182, 168, 0.88);
}

.card__badge--gray {
    color: #fff;
    background: rgba(148, 163, 184, 0.82);
}

.card__badge--danger {
    color: #fff;
    background: rgba(238, 10, 36, 0.82);
}

.card__body {
    padding: 24rpx 28rpx;
}

.card__title {
    display: block;
    font-size: 32rpx;
    font-weight: 700;
    color: #1d1d1f;
    line-height: 1.4;
}

.card__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: 16rpx;
}

.card__tag {
    padding: 4rpx 16rpx;
    border-radius: 8rpx;
    font-size: 22rpx;
    color: #1d1d1f;
    background: rgba(0, 0, 0, 0.08);
    font-weight: 500;
}

.card__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;
    margin-top: 20rpx;
    padding-top: 20rpx;
    border-top: 1rpx solid rgba(226, 232, 240, 0.6);
}

.card__meta-item {
    display: flex;
    align-items: center;
    gap: 8rpx;
}

.card__meta-icon {
    font-size: 24rpx;
    color: #8e8e93;
}

.card__meta-text {
    font-size: 24rpx;
    color: #666668;
}
</style>
