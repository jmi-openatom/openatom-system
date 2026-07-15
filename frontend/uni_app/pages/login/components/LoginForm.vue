<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import type {LoginFormData} from '../types'

const props = withDefaults(
    defineProps<{
        initialUsername?: string
        initialPassword?: string
        loading?: boolean
    }>(),
    {
        initialUsername: '',
        initialPassword: '',
        loading: false,
    },
)

const emit = defineEmits<{
    submit: [data: LoginFormData]
}>()

const username = ref('')
const password = ref('')
const showPassword = ref(false)

onMounted(() => {
    username.value = props.initialUsername
    password.value = props.initialPassword
})

function onSubmit() {
    if (props.loading) return
    if (!username.value.trim()) {
        uni.showToast({title: '请输入账号', icon: 'none'})
        return
    }
    if (!password.value) {
        uni.showToast({title: '请输入密码', icon: 'none'})
        return
    }
    emit('submit', {
        username: username.value.trim(),
        password: password.value,
    })
}

function togglePassword() {
    showPassword.value = !showPassword.value
}
</script>

<template>
    <view class="form">
        <view class="form-group">
            <view class="input-wrap">
                <text class="input-icon">
                    <tm-icon name="user-line"></tm-icon>
                </text>
                <input
                    v-model="username"
                    class="input"
                    maxlength="50"
                    placeholder="请输入账号"
                    placeholder-class="input-placeholder"
                    type="text"
                />
            </view>
        </view>

        <view class="form-group">
            <view class="input-wrap">
                <text class="input-icon">
                    <tm-icon name="lock-password-line"></tm-icon>
                </text>
                <input
                    v-model="password"
                    :type="showPassword ? 'text' : 'password'"
                    class="input"
                    maxlength="50"
                    placeholder="请输入密码"
                    placeholder-class="input-placeholder"
                />
                <view class="toggle-pwd" @tap="togglePassword">
                    <text>
                        <tm-icon name="eye-line"></tm-icon>
                    </text>
                </view>
            </view>
        </view>

        <view :class="['submit-btn', { 'submit-btn--loading': loading }]" @tap="onSubmit">
            <text v-if="!loading">登 录</text>
            <text v-else class="submit-btn__loading-text">登录中...</text>
        </view>
    </view>
</template>

<style lang="scss" scoped>
.form {
    padding: 0 48rpx;
}

.form-group {
    margin-bottom: 28rpx;
}

.input-wrap {
    display: flex;
    align-items: center;
    height: 96rpx;
    padding: 0 28rpx;
    border-radius: 20rpx;
    background: #fff;
    border: 1rpx solid rgba(226, 232, 240, 0.95);
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
    transition: border-color 0.2s;
}

.input-wrap:focus-within {
    border-color: #1d1d1f;
    box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.12);
}

.input-icon {
    font-size: 36rpx;
    margin-right: 16rpx;
    flex-shrink: 0;
}

.input {
    flex: 1;
    height: 100%;
    font-size: 30rpx;
    color: #1d1d1f;
}

.input-placeholder {
    color: #8e8e93;
    font-size: 28rpx;
}

.toggle-pwd {
    padding: 8rpx;
    margin-left: 8rpx;
    font-size: 32rpx;
    flex-shrink: 0;
}

.form-extra {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 8rpx;
    margin-bottom: 48rpx;
}

.remember-row {
    display: flex;
    align-items: center;
    gap: 12rpx;
}

.checkbox {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40rpx;
    height: 40rpx;
    border-radius: 10rpx;
    border: 2rpx solid rgba(148, 163, 184, 0.45);
    background: #fff;
    transition: all 0.2s;
}

.checkbox--checked {
    border-color: #1d1d1f;
    background: #1d1d1f;
}

.checkbox-mark {
    font-size: 24rpx;
    color: #fff;
    font-weight: 700;
}

.remember-label {
    font-size: 26rpx;
    color: #666668;
}

.submit-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 96rpx;
    border-radius: 16rpx;
    background: linear-gradient(135deg, #1d1d1f, #4f4f52);
    box-shadow: 0 16rpx 32rpx rgba(0, 0, 0, 0.18);
    font-size: 32rpx;
    font-weight: 700;
    color: #fff;
    transition: opacity 0.2s;
}

.submit-btn:active {
    opacity: 0.88;
}

.submit-btn--loading {
    opacity: 0.72;
}

.submit-btn__loading-text {
    font-size: 28rpx;
}
</style>
