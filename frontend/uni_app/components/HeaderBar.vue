<script lang="ts" setup>
defineOptions({
    name: 'HeaderBar'
})
const props = defineProps({
    title: {type: String, required: true},
    showBack: {type: Boolean, default: false}
})

const back = () => {
    if (!props.showBack) {
        return
    }

    uni.navigateBack({
        delta: 1,
        fail: () => {
            uni.switchTab({url: '/pages/home/index'})
        }
    })
}

</script>

<template>
    <view class="header">
        <text v-if="showBack" class="back" @click="back">‹</text>
        <text class="header-title">{{ title }}</text>
    </view>
</template>


<style scoped>
.header {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 88rpx;
    padding: 30rpx;
    background: #ffffff;
    border-bottom: 1px solid #e1e1e1;
}

.back {
    position: absolute;
    left: 30rpx;
    font-size: 52rpx;
    color: #333;
    line-height: 1;
}

.header-title {
    font-size: 36rpx;
    color: #333;
    font-weight: 600;
}
</style>
