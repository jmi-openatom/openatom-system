<template>
    <view class="panel oa-animate-in">
        <text class="panel-title">意向选择</text>
        <view class="field">
            <text class="label">第一志愿部门</text>
            <picker :range="departmentNames" @change="emit('pickFirst', $event)">
                <view class="picker-value">{{ firstDepartmentName || '请选择第一志愿' }}</view>
            </picker>
        </view>
        <view class="field">
            <text class="label">第二志愿部门</text>
            <picker :range="optionalDepartmentNames" @change="emit('pickSecond', $event)">
                <view class="picker-value">{{ secondDepartmentName || '可选' }}</view>
            </picker>
        </view>
    </view>
</template>

<script setup lang="ts">
defineProps<{
    departmentNames: string[]
    optionalDepartmentNames: string[]
    firstDepartmentName?: string
    secondDepartmentName?: string
}>()

const emit = defineEmits<{
    pickFirst: [event: any]
    pickSecond: [event: any]
}>()
</script>

<style scoped>
.panel {
    margin: 20rpx 24rpx;
    padding: 30rpx;
    border-radius: 18rpx;
    background: #fff;
    box-shadow: 0 12rpx 30rpx rgba(31, 55, 88, .08);
}

.panel-title,
.label {
    display: block;
}

.panel-title {
    margin-bottom: 20rpx;
    color: #0f172a;
    font-size: 30rpx;
    font-weight: 800;
}

.field {
    margin-bottom: 24rpx;
}

.field:last-child {
    margin-bottom: 0;
}

.label {
    margin-bottom: 12rpx;
    color: #334155;
    font-size: 25rpx;
    font-weight: 700;
}

.picker-value {
    width: 100%;
    height: 82rpx;
    line-height: 82rpx;
    padding: 0 22rpx;
    box-sizing: border-box;
    border-radius: 14rpx;
    background: #f8fafc;
    border: 1rpx solid #e2e8f0;
    color: #0f172a;
    font-size: 26rpx;
}
</style>
