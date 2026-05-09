<template>
    <view class="page">
        <tm-navbar :showNavBack="false" title="首页"/>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        <br>
        <tm-sheet>
            <tm-text class="text-weight-b mb-16 d-block" font-size="36">Welcome</tm-text>
            <view>
                <tm-text>欢迎来到JMI-OPENATOM 系统</tm-text>
            </view>
        </tm-sheet>
        
        <tm-tabbar
            :activeIndex="0"
            :list="tabbarList"
            :showTopBorder="true"
            position="fixed"
        />
    </view>
</template>

<script setup type="ts">
import {ref} from 'vue'
import {tabbarList} from '@/config/tabbar'

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
