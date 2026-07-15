<template>
    <view
        class="progress-card oa-animate-in"
        :style="{ animationDelay: `${index * 70}ms` }"
    >
        <view class="progress-card__head">
            <view>
                <text class="progress-card__title">{{ application.campaignName || '入会申请' }}</text>
                <text class="progress-card__meta">{{ application.clubName || '-' }} · #{{ application.id }}</text>
            </view>
            <text :class="['status-pill', `status-pill--${statusTone(application.status)}`]">{{ statusText(application.status) }}</text>
        </view>

        <view class="step-row">
            <view v-for="step in 4" :key="step" :class="['step-dot', { active: step <= activeStep(application.status) }]"/>
        </view>

        <view class="info-grid">
            <view class="info-item">
                <text>申请人</text>
                <strong>{{ application.applicantName || '-' }}</strong>
            </view>
            <view class="info-item">
                <text>志愿部门</text>
                <strong>{{ application.preferredDepartment || '未填写' }}</strong>
            </view>
        </view>

        <view v-if="application.interviews && application.interviews.length" class="interviews">
            <text class="section-title">面试安排</text>
            <view v-for="interview in application.interviews" :key="interview.id" class="interview">
                <view class="interview__top">
                    <text>第 {{ interview.round || 1 }} 轮</text>
                    <text>{{ statusText(interview.status) }}</text>
                </view>
                <text class="interview__line">{{ formatRange(interview.scheduledStartAt, interview.scheduledEndAt) }}</text>
                <text class="interview__line">{{ interview.mode === 'online' ? '线上' : '线下' }} · {{ interview.location || '地点待定' }}</text>
            </view>
        </view>
        <text v-else class="no-interview">暂未安排面试，请留意消息通知。</text>
    </view>
</template>

<script setup lang="ts">
import {formatRange, statusText, statusTone} from '@/utils/format'

defineProps<{
    application: Record<string, any>
    index: number
}>()

function activeStep(status?: string) {
    const map: Record<string, number> = {
        draft: 1,
        submitted: 2,
        reviewing: 2,
        approved: 3,
        pre_screen_passed: 3,
        interview_scheduled: 3,
        interviewed: 3,
        final_approved: 4,
        waitlisted: 4,
        rejected: 4,
        cancelled: 4,
        pre_screen_rejected: 4,
    }
    return map[status || ''] || 2
}
</script>

<style scoped>
.progress-card {
    margin: 20rpx 24rpx;
    padding: 30rpx;
    border-radius: 18rpx;
    background: #fff;
    box-shadow: 0 12rpx 30rpx rgba(0, 0, 0, .08);
}

.progress-card__head,
.interview__top {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16rpx;
}

.progress-card__title,
.progress-card__meta,
.no-interview {
    display: block;
}

.progress-card__title {
    color: #1d1d1f;
    font-size: 31rpx;
    font-weight: 780;
}

.progress-card__meta {
    margin-top: 8rpx;
    color: #666668;
    font-size: 24rpx;
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

.step-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12rpx;
    margin: 28rpx 0;
}

.step-dot {
    height: 10rpx;
    border-radius: 16rpx;
    background: #e2e8f0;
}

.step-dot.active {
    background: #1d1d1f;
}

.info-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16rpx;
}

.info-item {
    padding: 18rpx;
    border-radius: 14rpx;
    background: #f5f5f7;
}

.info-item text {
    display: block;
    margin-bottom: 8rpx;
    color: #666668;
    font-size: 22rpx;
}

.info-item strong {
    color: #1d1d1f;
    font-size: 26rpx;
}

.section-title {
    display: block;
    margin: 26rpx 0 14rpx;
    color: #1d1d1f;
    font-size: 27rpx;
    font-weight: 800;
}

.interview {
    padding: 18rpx;
    border-radius: 14rpx;
    background: #f5f5f7;
    margin-top: 12rpx;
}

.interview__top {
    color: #1d1d1f;
    font-size: 25rpx;
    font-weight: 700;
}

.interview__line {
    display: block;
    margin-top: 8rpx;
    color: #666668;
    font-size: 23rpx;
}

.no-interview {
    margin-top: 24rpx;
    color: #666668;
    font-size: 24rpx;
}
</style>
