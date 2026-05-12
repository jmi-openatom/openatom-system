<script lang="ts" setup>
import {reactive, ref} from "vue"
import {activityApi} from '@/api'

const props = withDefaults(defineProps<{
    activityId?: string | number
    disabled?: boolean
    title?: string
}>(), {
    disabled: false,
    title: '立即报名',
})

const show = ref(false)
const submitting = ref(false)
const form = reactive({
    name: '',
    phone: '',
    remark: '',
})

const open = () => {
    if (props.disabled) {
        uni.showToast({title: '该活动无需报名或暂未开放报名', icon: 'none'})
        return
    }
    show.value = true
}

const handleSubmit = async () => {
    if (!props.activityId) return
    if (!form.name || !form.phone) {
        uni.showToast({title: '请填写姓名和手机号', icon: 'none'})
        return
    }
    submitting.value = true
    try {
        await activityApi.register(props.activityId, {
            name: form.name,
            phone: form.phone,
            remark: form.remark,
        })
        uni.showToast({title: '报名成功', icon: 'success'})
        show.value = false
    } finally {
        submitting.value = false
    }
}
</script>

<template>
    <view class="apply">
        <view class="apply__btn-wrap">
            <tm-button :block="true" :disabled="disabled" color="primary" size="g" @click="open">
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
                        <view class="apply__form-item">
                            <text class="apply__label">姓名</text>
                            <tm-input v-model="form.name" placeholder="请输入姓名"/>
                        </view>
                        <view class="apply__form-item">
                            <text class="apply__label">手机号</text>
                            <tm-input v-model="form.phone" placeholder="请输入手机号" type="number"/>
                        </view>
                        <view class="apply__form-item">
                            <text class="apply__label">备注</text>
                            <tm-input v-model="form.remark" :height="200" placeholder="选填（如过敏史、特殊需求等）" type="textarea"/>
                        </view>

                        <view style="height: 100rpx;"></view>
                    </view>
                </scroll-view>

                <view class="drawer-footer">
                    <tm-button
                        block
                        color="primary"
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
    color: #334155;
    margin-bottom: 12rpx;
}

/* 抽屉内的固定底部按钮 */
.drawer-footer {
    padding: 24rpx 32rpx 36rpx;
    background-color: #ffffff;
    border-top: 1rpx solid #f1f5f9;
    box-shadow: 0 -4rpx 10rpx rgba(0, 0, 0, 0.02);
}
</style>
