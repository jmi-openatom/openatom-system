<!-- ============================================================
View: Home page template
============================================================ -->
<template>
    <view class="page">
        <!-- Header -->
        <HeaderBar title="首页"/>

        <Banner></Banner>

        <CustomTabBar current="pages/home/index"/>
    </view>
</template>

<!-- ============================================================
ViewModel: Home page logic
============================================================ -->
<script setup>
import {ref} from 'vue'
import {BarChartSquare, Clipboard, Cog, Search} from '@boxicons/vue'
import CustomTabBar from '@/components/CustomTabBar.vue'
import HeaderBar from "@/components/HeaderBar.vue";
import Banner from "@/pages/home/components/Banner.vue";

const features = ref([
    {icon: Clipboard, label: '功能A'},
    {icon: Search, label: '功能B'},
    {icon: BarChartSquare, label: '功能C'},
    {icon: Cog, label: '功能D'}
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
</style>
