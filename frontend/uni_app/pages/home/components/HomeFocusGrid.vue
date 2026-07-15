<script lang="ts" setup>
import {computed} from 'vue'
import type {ClubHomeFocusArea} from '../types'

const props = withDefaults(
    defineProps<{
        items?: ClubHomeFocusArea[]
    }>(),
    {items: () => []},
)

const list = computed(() => (props.items || []).filter((x) => x?.title || x?.description))
</script>

<template>
    <tm-sheet>
        <view class="block">
            <view class="block__head">
                <text class="block__title">社团概览</text>
                <text class="block__sub">部门与主要方向</text>
            </view>

            <view v-if="list.length" class="grid">
                <view v-for="(item, i) in list" :key="`${item.title}-${i}`" class="card">
                    <text class="card__title">{{ item.title }}</text>
                    <text class="card__desc">{{ item.description || '暂无描述' }}</text>
                </view>
            </view>

            <view v-else class="empty">
                <text>暂无部门数据</text>
            </view>
        </view>
    </tm-sheet>
</template>

<style scoped>
.block {
    margin: 0 24rpx 32rpx;
}

.block__head {
    margin-bottom: 16rpx;
    padding: 0 4rpx;
}

.block__title {
    display: block;
    font-size: 30rpx;
    font-weight: 600;
    color: #1d1d1f;
}

.block__sub {
    display: block;
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #8e8e93;
}

.grid {
    display: flex;
    flex-direction: column;
    gap: 16rpx;
}

.card {
    padding: 24rpx 22rpx;
    border-radius: 20rpx;
    background: rgba(255, 255, 255, 0.95);
    border: 1rpx solid rgba(226, 232, 240, 0.95);
    box-shadow: 0 12rpx 32rpx rgba(0, 0, 0, 0.06);
}

.card__icon {
    width: 10rpx;
    height: 10rpx;
    margin-bottom: 14rpx;
    border-radius: 50%;
    background: #1d1d1f;
}

.card__title {
    display: block;
    font-size: 28rpx;
    font-weight: 700;
    color: #1d1d1f;
    line-height: 1.3;
}

.card__desc {
    display: block;
    margin-top: 10rpx;
    font-size: 24rpx;
    color: #666668;
    line-height: 1.55;
}

.empty {
    padding: 40rpx;
    text-align: center;
    font-size: 24rpx;
    color: #8e8e93;
    background: rgba(255, 255, 255, 0.7);
    border-radius: 20rpx;
}
</style>
