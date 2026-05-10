<script lang="ts" setup>
import {computed} from 'vue'
import type {ClubHomeAward} from '../types'

const props = withDefaults(
    defineProps<{
        awards?: ClubHomeAward[]
    }>(),
    {awards: () => []},
)

const list = computed(() => (props.awards || []).filter((a) => a?.title))
</script>

<template>
    <tm-sheet>
        <view class="block">
            <view class="block__head">
                <text class="block__title">竞赛获奖</text>
                <text class="block__sub">近期荣誉记录</text>
            </view>

            <view v-if="list.length" class="list">
                <view v-for="(a, i) in list" :key="a.id || `${a.year}-${a.title}-${i}`" class="card">
                    <text v-if="a.year" class="card__year">{{ a.year }}</text>
                    <text class="card__title">{{ a.title }}</text>
                    <text v-if="a.competitionName" class="card__comp">{{ a.competitionName }}</text>
                    <view class="card__row">
                        <text v-if="a.awardLevel" class="pill">{{ a.awardLevel }}</text>
                        <text v-if="a.teamName" class="team">{{ a.teamName }}</text>
                    </view>
                </view>
            </view>

            <view v-else class="empty">
                <text>暂无获奖记录</text>
            </view>
        </view>
    </tm-sheet>
</template>

<style scoped>
.block {
    margin: 0 24rpx 48rpx;
}

.block__head {
    margin-bottom: 16rpx;
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

.list {
    display: flex;
    flex-direction: column;
    gap: 14rpx;
}

.card {
    padding: 22rpx 20rpx;
    border-radius: 20rpx;
    background: linear-gradient(165deg, #fffbeb, #ffffff);
    border: 1rpx solid rgba(254, 215, 170, 0.55);
}

.card__year {
    display: block;
    font-size: 22rpx;
    font-weight: 700;
    color: #b45309;
}

.card__title {
    display: block;
    margin-top: 8rpx;
    font-size: 28rpx;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.35;
}

.card__comp {
    display: block;
    margin-top: 8rpx;
    font-size: 24rpx;
    color: #64748b;
}

.card__row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12rpx;
    margin-top: 12rpx;
}

.pill {
    padding: 4rpx 14rpx;
    border-radius: 999rpx;
    font-size: 20rpx;
    color: #92400e;
    background: rgba(251, 191, 36, 0.25);
}

.team {
    font-size: 22rpx;
    color: #94a3b8;
}

.empty {
    padding: 40rpx;
    text-align: center;
    font-size: 24rpx;
    color: #94a3b8;
    background: rgba(255, 255, 255, 0.7);
    border-radius: 20rpx;
}
</style>
