<template>
    <view class="form-head oa-animate-in">
        <text class="form-head__club">{{ clubName || '开放原子开源社团' }}</text>
        <text class="form-head__title">{{ campaignName || '入会申请表' }}</text>
        <text class="form-head__time">{{ range }}</text>
    </view>
</template>

<script setup lang="ts">
defineProps<{
    clubName?: string
    campaignName?: string
    range: string
}>()
</script>

<style scoped>
.form-head {
    margin: 20rpx 24rpx;
    padding: 30rpx;
    border-radius: 18rpx;
    color: #1d1d1f;
    background: #ffffff;
    border: 1rpx solid #dbeafe;
    box-shadow: 0 12rpx 30rpx rgba(0, 0, 0, .06);
}

.form-head__club,
.form-head__title,
.form-head__time {
    display: block;
}

.form-head__club {
    font-size: 23rpx;
    color: #1d1d1f;
    font-weight: 700;
}

.form-head__title {
    margin-top: 12rpx;
    font-size: 38rpx;
    font-weight: 800;
}

.form-head__time {
    margin-top: 14rpx;
    font-size: 24rpx;
    color: #666668;
}
</style>
