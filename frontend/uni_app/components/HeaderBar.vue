<script setup>
defineOptions({
  name: 'HeaderBar'
})
defineProps({
    title: { type: String, required: true }
})
</script>

<template>
    <view class="header">
        <text class="header-title">{{title}}</text>
    </view>
</template>

<style scoped>
.header {
    padding: 30rpx;
    background: var(--topbar-color-light);
    border-bottom: 1px solid #e1e1e1;
}
.header-title {
    font-size: 36rpx;
    color: var(--topbar-text-color-light);
    font-weight: 600;
}
</style>