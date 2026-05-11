<script lang="ts" setup>
import {ref} from "vue"

const show = ref(false)
</script>

<template>
    <view class="apply">
        <view class="apply__btn-wrap">
            <tm-button :block="true" color="primary" size="g" @click="show = true">
                立即报名
            </tm-button>
        </view>

        <tm-drawer
            v-model:show="show"
            :show-close="true"
            :show-footer="true"
            confirm-text="提交报名"
            position="bottom"
            size="65%"
            title="活动报名"
        >
            <view class="apply__form">
                <view class="apply__form-item">
                    <text class="apply__label">姓名</text>
                    <tm-input placeholder="请输入姓名"/>
                </view>
                <view class="apply__form-item">
                    <text class="apply__label">手机号</text>
                    <tm-input placeholder="请输入手机号" type="number"/>
                </view>
                <view class="apply__form-item">
                    <text class="apply__label">备注</text>
                    <tm-input placeholder="选填"/>
                </view>
            </view>
        </tm-drawer>
    </view>
</template>

<style lang="scss" scoped>
.apply__btn-wrap {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 99;
    padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
    background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, #fff 30%);
}

.apply__form {
    padding: 16rpx 0;
}

.apply__form-item {
    margin-bottom: 24rpx;
}

.apply__label {
    display: block;
    font-size: 26rpx;
    font-weight: 600;
    color: #334155;
    margin-bottom: 10rpx;
}
</style>
