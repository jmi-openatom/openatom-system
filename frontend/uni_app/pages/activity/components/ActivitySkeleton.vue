<script lang="ts" setup>
withDefaults(
    defineProps<{
        count?: number
    }>(),
    {
        count: 3,
    },
)
</script>

<template>
    <view class="skeleton-list">
        <view v-for="n in count" :key="n" class="sk-card">
            <view class="sk sk-cover" />
            <view class="sk-body">
                <view class="sk sk-tag-sm" />
                <view class="sk sk-title" />
                <view class="sk sk-line" />
                <view class="sk sk-line sk-line--short" />
            </view>
        </view>
    </view>
</template>

<style lang="scss" scoped>
.skeleton-list {
    padding: 12rpx 24rpx;
}

.sk-card {
    margin-bottom: 24rpx;
    border-radius: 20rpx;
    overflow: hidden;
    background: #fff;
    border: 1rpx solid rgba(226, 232, 240, 0.95);
    box-shadow: 0 12rpx 28rpx rgba(0, 0, 0, 0.08);
}

.sk {
    border-radius: 12rpx;
    background: linear-gradient(90deg, #e8eef5 0%, #f4f7fb 50%, #e8eef5 100%);
    background-size: 200% 100%;
}

.sk-cover {
    width: 100%;
    height: 280rpx;
    border-radius: 0;
}

.sk-body {
    padding: 24rpx 28rpx;
}

.sk-tag-sm {
    width: 100rpx;
    height: 32rpx;
    border-radius: 8rpx;
    margin-bottom: 16rpx;
}

.sk-title {
    width: 70%;
    height: 36rpx;
    margin-bottom: 16rpx;
}

.sk-line {
    width: 90%;
    height: 24rpx;
    margin-bottom: 12rpx;
}

.sk-line--short {
    width: 50%;
}
</style>
