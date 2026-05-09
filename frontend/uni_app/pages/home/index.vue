<template>
    <view>
        <tm-sheet>
            <tm-text class=" text-weight-b mb-16 d-block" font-size="32">警告 tmAlert</tm-text>
            <tm-text color="#999999">
                样式丰富，用于警告信息的提醒。
            </tm-text>
        </tm-sheet>
        <tm-alert>默认为thin浅色样式，可以自适应暗黑</tm-alert>
        <tm-alert skin='normal'>也可以把skin改成normal变成纯背景</tm-alert>
        <tm-alert color="#000000" icon="thumb-up-line" skin='normal'>也可以把skin改成normal变成纯背景</tm-alert>
        <tm-alert color="#dee3e7" icon="thumb-up-line" skin='normal'>也可以把skin改成normal变成纯背景</tm-alert>

        <tm-sheet>
            <tm-text class=" text-weight-b " font-size="32">可以使用默认的状态来管理颜色</tm-text>
        </tm-sheet>
        <tm-alert icon="alarm-line" skin='normal' status="success">自定义图标</tm-alert>
        <tm-alert skin='normal' status="error">错误提醒</tm-alert>
        <tm-alert closeIcon="arrow-right-line" skin='normal' status="warn">警告提醒</tm-alert>
        <tm-alert color="#728398" icon="money-cny-circle-fill" skin='normal'>自定义背景色</tm-alert>
        <tm-sheet>
            <tm-text class=" text-weight-b " font-size="32">比如改些圆角，边线啥的,样式丰富请自行设计</tm-text>
        </tm-sheet>
        <tm-alert :round="['6']" skin='normal' status="warn">警告提醒</tm-alert>
        <tm-alert :round="['12', '0']" skin='normal' status="error">改下圆角</tm-alert>
        <tm-alert :border="['2']">加个小边框</tm-alert>
        <tm-alert :border="['2']" :borderColor="['#89c0ff']" :darkBorderColor="['#3b5066']"
                  :showClose="false">加个小边框,隐藏关闭
        </tm-alert>
    </view>
</template>

<script setup type="ts">
import {ref} from 'vue'

const features = ref([
    {icon: '📋', label: '功能A'},
    {icon: '🔍', label: '功能B'},
    {icon: '📊', label: '功能C'},
    {icon: '⚙️', label: '功能D'}
])
</script>

<style scoped>
.page {
    padding-bottom: 140rpx;
}

.kit-title {
    display: flex;
    align-items: center;
}

.kit-title-text {
    flex: 1;
    margin-left: 12rpx;
    font-size: 30rpx;
    font-weight: 600;
    color: #1f2937;
}

.kit-actions {
    display: flex;
    align-items: center;
    gap: 20rpx;
    margin-top: 20rpx;
}

.uni-icon-demo {
    display: flex;
    align-items: center;
    height: 56rpx;
    padding: 0 18rpx;
    background: #fff7ed;
    border-radius: 28rpx;
}

.uni-icon-text {
    margin-left: 8rpx;
    font-size: 24rpx;
    color: #92400e;
}

.grid {
    display: flex;
    flex-wrap: wrap;
    padding: 0 24rpx;
    margin-top: 20rpx;
}

.grid-item {
    width: 25%;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 30rpx 0;
}

.grid-icon {
    font-size: 48rpx;
}

.grid-label {
    font-size: 26rpx;
    color: #333;
    margin-top: 10rpx;
}

.list {
    margin: 0 24rpx;
}

.list-item {
    display: flex;
    align-items: center;
    padding: 30rpx;
    margin-bottom: 16rpx;
    background: #fff;
    border-radius: 12rpx;
}

.list-icon {
    font-size: 36rpx;
    margin-right: 20rpx;
    flex-shrink: 0;
}

.list-body {
    flex: 1;
}

.list-title {
    font-size: 30rpx;
    font-weight: 500;
    color: #333;
}

.list-desc {
    font-size: 26rpx;
    color: #999;
    margin-top: 8rpx;
}

.list-arrow {
    font-size: 36rpx;
    color: #ccc;
}
</style>
