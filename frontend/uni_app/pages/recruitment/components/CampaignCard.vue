<template>
    <view
        class="campaign-card oa-animate-in"
        :style="{ animationDelay: `${index * 60}ms` }"
        @tap="emit('tap', campaign)"
    >
        <view class="campaign-card__top">
            <text class="campaign-card__title">{{ campaign.name || '招新计划' }}</text>
            <text :class="['status-pill', `status-pill--${statusTone(campaign.status)}`]">
                {{ statusText(campaign.status) }}
            </text>
        </view>
        <text class="campaign-card__range">{{ formatRange(campaign.applyStartAt, campaign.applyEndAt) }}</text>
        <view class="campaign-card__meta">
            <text>无需登录填写</text>
            <text v-if="campaign.maxApplicants">名额 {{ campaign.maxApplicants }}</text>
            <text v-if="campaign.targetGrades">面向 {{ campaign.targetGrades }}</text>
        </view>
        <view class="campaign-card__action">
            <text>填写申请</text>
            <text class="arrow">›</text>
        </view>
    </view>
</template>

<script setup lang="ts">
import {formatRange, statusText, statusTone} from '@/utils/format'

defineProps<{
    campaign: Record<string, any>
    index?: number
}>()

const emit = defineEmits<{
    tap: [campaign: Record<string, any>]
}>()
</script>

<style scoped>
.campaign-card {
    margin-bottom: 18rpx;
    padding: 28rpx;
    border-radius: 16rpx;
    background: #fff;
    box-shadow: 0 12rpx 30rpx rgba(0, 0, 0, .08);
}

.campaign-card:active {
    transform: scale(.985);
}

.campaign-card__top,
.campaign-card__meta,
.campaign-card__action {
    display: flex;
    align-items: center;
}

.campaign-card__top,
.campaign-card__action {
    justify-content: space-between;
}

.campaign-card__title {
    flex: 1;
    min-width: 0;
    padding-right: 18rpx;
    font-size: 32rpx;
    font-weight: 750;
    color: #1d1d1f;
}

.campaign-card__range {
    display: block;
    margin-top: 16rpx;
    color: #666668;
    font-size: 24rpx;
}

.campaign-card__meta {
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: 18rpx;
}

.campaign-card__meta text {
    padding: 6rpx 14rpx;
    border-radius: 16rpx;
    background: #ececef;
    color: #4f4f52;
    font-size: 22rpx;
}

.campaign-card__action {
    margin-top: 22rpx;
    padding-top: 20rpx;
    border-top: 1rpx solid #eef2f7;
    color: #1d1d1f;
    font-size: 26rpx;
    font-weight: 700;
}

.arrow {
    font-size: 42rpx;
    line-height: 1;
}

.status-pill {
    flex-shrink: 0;
    padding: 6rpx 16rpx;
    border-radius: 16rpx;
    font-size: 22rpx;
    font-weight: 700;
}

.status-pill--success { color: #047857; background: #d1fae5; }
.status-pill--warning { color: #a16207; background: #fef3c7; }
.status-pill--muted,
.status-pill--default { color: #666668; background: #ececef; }
</style>
