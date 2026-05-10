<script lang="ts" setup>
import {computed} from 'vue'
import type {ClubHomeActivity} from '../types'

const props = withDefaults(
    defineProps<{
        activities?: ClubHomeActivity[]
    }>(),
    {activities: () => []},
)

const list = computed(() => (props.activities || []).filter((a) => a?.title))

function statusText(s?: string) {
    const m: Record<string, string> = {
        draft: '草稿',
        published: '已发布',
        closed: '已关闭',
        cancelled: '已取消',
    }
    return (s && m[s]) || s || ''
}

function onTap(item: ClubHomeActivity) {
    const lines = [item.title, item.date, item.location, item.description].filter(Boolean).join('\n')
    const content = (lines || '暂无更多信息').slice(0, 480)
    uni.showModal({
        title: '活动详情',
        content,
        showCancel: false,
    })
}
</script>

<template>
    <tm-sheet>
        <view id="section-activities" class="block">
            <view class="block__head">
                <text class="block__title">近期活动</text>
                <text class="block__sub">左右滑动查看更多</text>
            </view>

            <scroll-view v-if="list.length" class="scroller" enable-flex scroll-x>
                <view class="scroller__inner">
                    <view
                        v-for="(item, i) in list"
                        :key="item.id || `${item.title}-${i}`"
                        class="card"
                        @tap="onTap(item)"
                    >
                        <image
                            v-if="item.coverUrl"
                            :src="item.coverUrl"
                            class="card__cover"
                            mode="aspectFill"
                        />
                        <view v-else class="card__cover card__cover--ph"/>
                        <view class="card__body">
                            <text v-if="item.date" class="card__date">{{ item.date }}</text>
                            <text class="card__title">{{ item.title }}</text>
                            <text v-if="item.status" class="card__tag">{{ statusText(item.status) }}</text>
                        </view>
                    </view>
                </view>
            </scroll-view>

            <view v-else class="empty">
                <text>暂无活动</text>
            </view>
        </view>
    </tm-sheet>
</template>

<style scoped>
.block {
    margin: 0 0 32rpx;
}

.block__head {
    margin: 0 28rpx 16rpx;
    padding: 0 4rpx;
}

.block__title {
    display: block;
    font-size: 30rpx;
    font-weight: 600;
    color: #0f172a;
}

.block__sub {
    display: block;
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #94a3b8;
}

.scroller {
    width: 100%;
    white-space: nowrap;
}

.scroller__inner {
    display: inline-flex;
    gap: 16rpx;
    padding: 0 24rpx 8rpx;
}

.card {
    display: inline-flex;
    flex-direction: column;
    width: 300rpx;
    vertical-align: top;
    border-radius: 20rpx;
    overflow: hidden;
    background: #fff;
    border: 1rpx solid rgba(226, 232, 240, 0.95);
    box-shadow: 0 12rpx 28rpx rgba(31, 55, 88, 0.08);
}

.card__cover {
    width: 100%;
    height: 160rpx;
    background: #e8eef5;
}

.card__cover--ph {
    background: linear-gradient(135deg, #e0e7ff, #dbeafe);
}

.card__body {
    padding: 16rpx 18rpx 20rpx;
    white-space: normal;
}

.card__date {
    display: block;
    font-size: 22rpx;
    color: #64748b;
}

.card__title {
    display: block;
    margin-top: 8rpx;
    font-size: 26rpx;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.35;
}

.card__tag {
    display: inline-block;
    margin-top: 12rpx;
    padding: 4rpx 14rpx;
    border-radius: 999rpx;
    font-size: 20rpx;
    color: #1769e8;
    background: rgba(23, 105, 232, 0.1);
}

.empty {
    margin: 0 24rpx;
    padding: 40rpx;
    text-align: center;
    font-size: 24rpx;
    color: #94a3b8;
    background: rgba(255, 255, 255, 0.7);
    border-radius: 20rpx;
}
</style>
