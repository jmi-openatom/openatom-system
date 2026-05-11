<script lang="ts" setup>
withDefaults(
    defineProps<{
        title?: string
        subTitle?: string
    }>(),
    {
        title: '暂无活动',
        subTitle: '当前分类下没有活动，换个筛选试试',
    },
)
</script>

<template>
    <view class="empty">
        <view class="empty__icon">
            <view class="empty__ring"/>
            <text class="empty__emoji">
                <tm-icon name="calendar-check-line"></tm-icon>
            </text>
        </view>
        <text class="empty__title">{{ title }}</text>
        <text class="empty__sub">{{ subTitle }}</text>
    </view>
</template>

<style lang="scss" scoped>
.empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 120rpx 48rpx 80rpx;
}

.empty__icon {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 140rpx;
    height: 140rpx;
    margin-bottom: 24rpx;
}

.empty__ring {
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    border: 3rpx dashed rgba(148, 163, 184, 0.35);
}

.empty__emoji {
    font-size: 56rpx;
}

.empty__title {
    font-size: 30rpx;
    font-weight: 600;
    color: #475569;
}

.empty__sub {
    margin-top: 10rpx;
    font-size: 24rpx;
    color: #94a3b8;
}
</style>
