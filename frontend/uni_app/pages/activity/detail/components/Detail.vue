<script lang="ts" setup>
import MarkdownRenderer from './MarkdownRenderer.vue'

defineProps<{
    content?: string
}>()
</script>

<template>
    <tm-sheet>
        <view class="detail">
            <view class="detail__head">
                <text class="detail__head-title">活动详情</text>
            </view>
            <view class="detail__body">
                <MarkdownRenderer :content="content"/>
            </view>
        </view>
    </tm-sheet>


</template>

<style lang="scss" scoped>
.detail {
    padding: 28rpx;
}

.detail__head {
    display: flex;
    align-items: center;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid rgba(226, 232, 240, 0.6);
    margin-bottom: 8rpx;
}

.detail__head-title {
    font-size: 30rpx;
    font-weight: 700;
    color: #1d1d1f;
}

.detail__body {
    padding: 8rpx 0;
}
</style>

