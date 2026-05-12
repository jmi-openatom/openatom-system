<template>
    <view class="panel oa-animate-in">
        <text class="panel-title">基础信息</text>
        <view v-if="!hasApplicantNameField" class="field">
            <text class="label">姓名</text>
            <input
                :value="formData.applicantName || ''"
                class="input"
                placeholder="请输入姓名"
                @input="emit('updateField', 'applicantName', $event.detail.value)"
            />
        </view>
        <view v-if="!hasContactField" class="field">
            <text class="label">联系方式</text>
            <input
                :value="formData.contact || ''"
                class="input"
                placeholder="手机号、邮箱或微信"
                @input="emit('updateField', 'contact', $event.detail.value)"
            />
        </view>

        <view v-for="field in fields" :key="field.key" class="field">
            <text class="label">{{ field.label || field.key }}{{ field.required ? ' *' : '' }}</text>
            <picker
                v-if="field.type === 'select'"
                :range="fieldOptions(field)"
                @change="(event) => emit('pickField', field, event)"
            >
                <view class="picker-value">{{ formData[field.key] || field.placeholder || '请选择' }}</view>
            </picker>
            <textarea
                v-else-if="field.type === 'textarea'"
                :value="formData[field.key] || ''"
                class="textarea"
                :placeholder="field.placeholder || '请输入'"
                @input="emit('updateField', field.key, $event.detail.value)"
            />
            <input
                v-else
                :value="formData[field.key] || ''"
                class="input"
                :placeholder="field.placeholder || '请输入'"
                @input="emit('updateField', field.key, $event.detail.value)"
            />
        </view>
    </view>
</template>

<script setup lang="ts">
import type {FormField} from '@/utils/formSchema'
import {optionLabel} from '@/utils/formSchema'

defineProps<{
    fields: FormField[]
    formData: Record<string, any>
    hasApplicantNameField: boolean
    hasContactField: boolean
}>()

const emit = defineEmits<{
    pickField: [field: FormField, event: any]
    updateField: [key: string, value: string]
}>()

function fieldOptions(field: FormField) {
    return (field.options || []).map(optionLabel)
}
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

.input,
.textarea,
.picker-value {
    width: 100%;
    box-sizing: border-box;
    border-radius: 14rpx;
    background: #f8fafc;
    border: 1rpx solid #e2e8f0;
    color: #0f172a;
    font-size: 26rpx;
}

.input,
.picker-value {
    height: 82rpx;
    line-height: 82rpx;
    padding: 0 22rpx;
}

.textarea {
    min-height: 180rpx;
    padding: 20rpx 22rpx;
}
</style>
