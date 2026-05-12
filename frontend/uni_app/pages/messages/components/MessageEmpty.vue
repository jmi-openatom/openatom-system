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
    color: #64748b;
}

.empty__title,
.empty__desc {
    display: block;
}

.empty__title {
    color: #0f172a;
    font-size: 32rpx;
    font-weight: 800;
}

.empty__desc {
    margin-top: 12rpx;
    font-size: 25rpx;
}
</style>
