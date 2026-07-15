<script lang="ts" setup>
import {computed} from 'vue'
import type {ClubHomePerson} from '../types'

const props = withDefaults(
    defineProps<{
        people?: ClubHomePerson[]
    }>(),
    {people: () => []},
)

const list = computed(() => (props.people || []).filter((p) => p?.name))
</script>

<template>
    <tm-sheet>
        <view class="block">
            <view class="block__head">
                <text class="block__title">主要成员</text>
                <text class="block__sub">首页精选展示</text>
            </view>

            <view v-if="list.length" class="list">
                <view v-for="(p, i) in list" :key="p.userId || `${p.name}-${i}`" class="row">
                    <view class="avatar">
                        <image v-if="p.avatar" :src="p.avatar" class="avatar__img" mode="aspectFill"/>
                        <text v-else class="avatar__txt">{{ p.initial || (p.name && p.name[0]) || '?' }}</text>
                    </view>
                    <view class="meta">
                        <text class="meta__name">{{ p.name }}</text>
                        <text v-if="p.role" class="meta__role">{{ p.role }}</text>
                        <text v-if="p.focus" class="meta__focus">{{ p.focus }}</text>
                    </view>
                </view>
            </view>

            <view v-else class="empty">
                <text>暂无成员展示</text>
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

.list {
    display: flex;
    flex-direction: column;
    gap: 14rpx;
}

.row {
    display: flex;
    align-items: center;
    gap: 18rpx;
    padding: 18rpx 20rpx;
    border-radius: 20rpx;
    background: rgba(255, 255, 255, 0.95);
    border: 1rpx solid rgba(226, 232, 240, 0.95);
}

.avatar {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #1d1d1f, #21b6a8);
    overflow: hidden;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}

.avatar__img {
    width: 100%;
    height: 100%;
}

.avatar__txt {
    color: #fff;
    font-size: 30rpx;
    font-weight: 800;
}

.meta {
    flex: 1;
    min-width: 0;
}

.meta__name {
    display: block;
    font-size: 28rpx;
    font-weight: 700;
    color: #1d1d1f;
}

.meta__role {
    display: block;
    margin-top: 6rpx;
    font-size: 24rpx;
    color: #666668;
}

.meta__focus {
    display: block;
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #8e8e93;
    line-height: 1.45;
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
