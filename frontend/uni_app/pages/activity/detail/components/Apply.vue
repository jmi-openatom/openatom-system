<script lang="ts" setup>
import {computed, reactive, ref} from "vue"
import {activityApi} from '@/api'
import {requireLogin} from '@/utils/auth'
import {fieldInputType, isOptionField, normalizeFieldType, optionLabel, optionValue, validateField} from '@/utils/formSchema'
import type {FormField} from '@/utils/formSchema'

const props = withDefaults(defineProps<{
    activityId?: string | number
    disabled?: boolean
    title?: string
    fields?: FormField[]
}>(), {
    disabled: false,
    title: '立即报名',
    fields: () => [],
})

const emit = defineEmits<{success: []}>()

const show = ref(false)
const submitting = ref(false)
const fallbackForm = reactive({
    name: '',
    phone: '',
    remark: '',
})
const formData = reactive<Record<string, string | number>>({})
const hasDynamicFields = computed(() => props.fields.length > 0)

function fieldOptions(field: FormField) {
    return (field.options || []).map(optionLabel)
}

function pickField(field: FormField, event: {detail: {value: string | number}}) {
    const option = (field.options || [])[Number(event.detail.value)]
    formData[field.key] = optionValue(option)
}

const open = () => {
    if (props.disabled) {
        uni.showToast({title: '该活动无需报名或暂未开放报名', icon: 'none'})
        return
    }
    if (!requireLogin()) return
    show.value = true
}

const handleSubmit = async () => {
    if (!props.activityId) return
    const invalid = props.fields.map((field) => validateField(field, formData[field.key])).find(Boolean)
    if (invalid) {
        uni.showToast({title: invalid, icon: 'none'})
        return
    }
    if (!hasDynamicFields.value && (!fallbackForm.name || !fallbackForm.phone)) {
        uni.showToast({title: '请填写姓名和手机号', icon: 'none'})
        return
    }
    submitting.value = true
    try {
        const values: Record<string, string | number> = {}
        props.fields.forEach((field) => {
            const value = formData[field.key]
            if (value !== undefined && value !== '') values[field.label || field.key] = value
        })
        await activityApi.register(props.activityId, {
            formData: hasDynamicFields.value ? values : {
                姓名: fallbackForm.name,
                手机号: fallbackForm.phone,
                备注: fallbackForm.remark,
            },
        })
        uni.showToast({title: '报名成功', icon: 'success'})
        show.value = false
        emit('success')
    } finally {
        submitting.value = false
    }
}
</script>

<template>
    <view class="apply">
        <view class="apply__btn-wrap">
            <tm-button :block="true" :disabled="disabled" color="#1d1d1f" size="g" @click="open">
                {{ title }}
            </tm-button>
        </view>

        <tm-drawer
            v-model:show="show"
            :show-cancel="false"
            :show-close="true"
            :show-footer="false"
            position="bottom"
            size="85%"
            title="活动报名"
        >
            <view class="drawer-container">
                <scroll-view class="drawer-content" scroll-y>
                    <view class="apply__form">
                        <view v-for="field in fields" :key="field.key" class="apply__form-item">
                            <text class="apply__label">{{ field.label || field.key }}{{ field.required ? ' *' : '' }}</text>
                            <picker v-if="isOptionField(field)" :range="fieldOptions(field)" @change="pickField(field, $event)">
                                <view class="apply__picker">{{ formData[field.key] || field.placeholder || '请选择' }}</view>
                            </picker>
                            <picker v-else-if="normalizeFieldType(field) === 'date'" mode="date" @change="formData[field.key] = $event.detail.value">
                                <view class="apply__picker">{{ formData[field.key] || field.placeholder || '请选择日期' }}</view>
                            </picker>
                            <tm-input
                                v-else
                                :model-value="formData[field.key] || ''"
                                :placeholder="field.placeholder || '请输入'"
                                :type="normalizeFieldType(field) === 'textarea' ? 'textarea' : fieldInputType(field)"
                                @update:model-value="formData[field.key] = $event"
                            />
                        </view>
                        <template v-if="!hasDynamicFields">
                        <view class="apply__form-item">
                            <text class="apply__label">姓名</text>
                            <tm-input v-model="fallbackForm.name" placeholder="请输入姓名"/>
                        </view>
                        <view class="apply__form-item">
                            <text class="apply__label">手机号</text>
                            <tm-input v-model="fallbackForm.phone" placeholder="请输入手机号" type="number"/>
                        </view>
                        <view class="apply__form-item">
                            <text class="apply__label">备注</text>
                            <tm-input v-model="fallbackForm.remark" :height="200" placeholder="选填（如过敏史、特殊需求等）" type="textarea"/>
                        </view>
                        </template>

                        <view style="height: 100rpx;"></view>
                    </view>
                </scroll-view>

                <view class="drawer-footer">
                    <tm-button
                        block
                        color="#1d1d1f"
                        :loading="submitting"
                        size="g"
                        @click="handleSubmit"
                    >
                        提交报名
                    </tm-button>
                </view>
            </view>
        </tm-drawer>
    </view>
</template>

<style lang="scss" scoped>
/* 页面背景按钮固定在屏幕底部 */
.apply__btn-wrap {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 99;
    padding: 20rpx 24rpx 32rpx;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, #fff 30%);
}

/* 抽屉内部容器：关键布局 */
.drawer-container {
    display: flex;
    flex-direction: column;
    /* 这里的 85vh 需要对应抽屉的 size="85%"，确保容器占满抽屉 */
    height: 85vh;
}

/* 表单内容区：自动撑开并支持滚动 */
.drawer-content {
    flex: 1;
    overflow: hidden; /* 必须，否则内部滚动失效 */
}

.apply__form {
    padding: 32rpx;
}

.apply__form-item {
    margin-bottom: 32rpx;
}

.apply__label {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: #333333;
    margin-bottom: 12rpx;
}

.apply__picker {
    min-height: 88rpx;
    padding: 0 24rpx;
    border: 1rpx solid #d8d8dc;
    border-radius: 16rpx;
    background: #f5f5f7;
    color: #333;
    font-size: 28rpx;
    line-height: 88rpx;
}

/* 抽屉内的固定底部按钮 */
.drawer-footer {
    padding: 24rpx 32rpx 36rpx;
    background-color: #ffffff;
    border-top: 1rpx solid #ececef;
    box-shadow: 0 -4rpx 10rpx rgba(0, 0, 0, 0.02);
}
</style>
