<template>
    <view class="page">
        <HeaderBar title="首页"/>

        <Banner/>

        <!-- Feature Grid -->
        <view class="grid">
            <view v-for="item in features" :key="item.label" class="grid-item">
                <text class="grid-icon">{{ item.icon }}</text>
                <text class="grid-label">{{ item.label }}</text>
            </view>
        </view>

        <!-- List -->
        <view class="list">
            <view v-for="n in 5" :key="n" class="list-item">
                <text class="list-icon">📝</text>
                <view class="list-body">
                    <text class="list-title">动态标题 {{ n }}</text>
                    <text class="list-desc">这是动态描述内容区域</text>
                </view>
                <text class="list-arrow">›</text>
            </view>
        </view>

        <CustomTabBar current="pages/home/index"/>
    </view>
</template>

<script setup>
import {ref} from 'vue'
import CustomTabBar from '@/components/CustomTabBar.vue'
import HeaderBar from '@/components/HeaderBar.vue'
import Banner from '@/pages/home/components/Banner.vue'

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
