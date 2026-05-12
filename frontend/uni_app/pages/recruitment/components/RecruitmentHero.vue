<template>
    <view class="hero oa-animate-in">
        <view>
            <text class="eyebrow">{{ clubName || '开放原子开源社团' }}</text>
            <text class="title">找到你想投入的方向</text>
            <text class="desc">查看开放中的招新计划，提交申请后可在进度页追踪审核与面试安排。</text>
        </view>
        <view class="hero-mark">
            <text>JOIN</text>
        </view>
    </view>
</template>

<script setup lang="ts">
defineProps<{
    clubName?: string
}>()
</script>

<style scoped>
.hero {
    display: flex;
    justify-content: space-between;
    gap: 20rpx;
    margin: 20rpx 24rpx 18rpx;
    padding: 34rpx;
    border-radius: 20rpx;
    background: #ffffff;
    border: 1rpx solid #dbeafe;
    color: #0f172a;
    box-shadow: 0 12rpx 28rpx rgba(15, 23, 42, 0.06);
}

.eyebrow,
.title,
.desc {
    display: block;
}

.eyebrow {
    font-size: 22rpx;
    color: #1769e8;
    font-weight: 700;
}

.title {
    margin-top: 14rpx;
    font-size: 42rpx;
    line-height: 1.18;
    font-weight: 800;
}

.desc {
    max-width: 470rpx;
    margin-top: 16rpx;
    font-size: 25rpx;
    line-height: 1.55;
    color: #64748b;
}

.hero-mark {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 128rpx;
    height: 128rpx;
    flex-shrink: 0;
    border-radius: 24rpx;
    background: #eff6ff;
    color: #1769e8;
    border: 1rpx solid #bfdbfe;
    font-size: 24rpx;
    font-weight: 800;
}
</style>
