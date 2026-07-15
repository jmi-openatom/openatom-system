<template>
    <view class="empty">
        <text class="empty__title">暂无消息</text>
        <text class="empty__desc">申请反馈和活动提醒会出现在这里。</text>
    </view>
</template>

<style scoped>
.empty {
    padding: 100rpx 40rpx;
    text-align: center;
    color: #666668;
}

.empty__title,
.empty__desc {
    display: block;
}

.empty__title {
    color: #1d1d1f;
    font-size: 32rpx;
    font-weight: 800;
}

.empty__desc {
    margin-top: 12rpx;
    font-size: 25rpx;
}
</style>
