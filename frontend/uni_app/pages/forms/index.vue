<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="true" :title="formName"/>
        <scroll-view class="main-scroll" scroll-y>
            <view v-if="loading" class="loading">加载中...</view>

            <PageState v-else-if="loadFailed" description="表单加载失败，请稍后重试" @action="load"/>

            <template v-else-if="form.id">
                <view class="info-panel">
                    <view class="info-row">
                        <text class="info-label">收集时间</text>
                        <text class="info-value">{{ formatRange(form.startAt, form.endAt) }}</text>
                    </view>
                    <text v-if="form.description" class="info-desc">{{ form.description }}</text>
                </view>

                <view class="form-panel">
                    <view v-if="!form.loginRequired" class="field">
                        <text class="label">姓名</text>
                        <input
                            v-model="anonymousName"
                            class="input"
                            placeholder="请输入姓名"
                        />
                    </view>
                    <view v-if="!form.loginRequired" class="field">
                        <text class="label">联系方式</text>
                        <input
                            v-model="anonymousContact"
                            class="input"
                            placeholder="手机号、邮箱或微信"
                        />
                    </view>

                    <view v-for="field in dynamicFields" :key="field.key" class="field">
                        <text class="label">{{ field.label || field.key }}{{ field.required ? ' *' : '' }}</text>
                        <picker
                            v-if="isOptionField(field)"
                            :range="fieldOptions(field)"
                            @change="(event: any) => onPickField(field, event)"
                        >
                            <view class="picker-value">{{ formData[field.key] || field.placeholder || '请选择' }}</view>
                        </picker>
                        <textarea
                            v-else-if="normalizeFieldType(field) === 'textarea'"
                            :value="formData[field.key] || ''"
                            class="textarea"
                            :placeholder="field.placeholder || '请输入'"
                            @input="updateField(field.key, $event.detail.value)"
                        />
                        <picker
                            v-else-if="normalizeFieldType(field) === 'date'"
                            mode="date"
                            @change="updateField(field.key, $event.detail.value)"
                        >
                            <view class="picker-value">{{ formData[field.key] || field.placeholder || '请选择日期' }}</view>
                        </picker>
                        <input
                            v-else
                            :value="formData[field.key] || ''"
                            class="input"
                            :maxlength="field.maxLength || 140"
                            :placeholder="field.placeholder || '请输入'"
                            :type="fieldInputType(field)"
                            @input="updateField(field.key, $event.detail.value)"
                        />
                    </view>
                </view>
            </template>

            <view v-else class="empty">
                <text class="empty__title">表单不存在或暂未开放</text>
                <button class="primary-btn" @tap="goBack">返回首页</button>
            </view>

            <view class="bottom-pad"/>
        </scroll-view>

        <view v-if="form.id" class="submit-bar">
            <button class="submit-btn" :disabled="submitting || (!canSubmit && !(requiresLogin && !isLogin))" @tap="submitForm">
                {{ submitting ? '提交中...' : requiresLogin && !isLogin ? '登录后提交' : canSubmit ? '提交表单' : '当前不可提交' }}
            </button>
        </view>
    </view>
</template>

<script setup lang="ts">
import { siteApi, formSubmissionApi } from '@/api'
import { getToken, loginUrl } from '@/utils/auth'
import { fieldInputType, isOptionField, normalizeFieldType, parseFormSchema, optionLabel, optionValue, validateField } from '@/utils/formSchema'
import type { FormField } from '@/utils/formSchema'
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import PageState from '@/components/PageState.vue'

const formId = ref('')
const loading = ref(true)
const loadFailed = ref(false)
const submitting = ref(false)
const form = ref<Record<string, any>>({})
const formData = reactive<Record<string, any>>({})
const anonymousName = ref('')
const anonymousContact = ref('')
const isLogin = computed(() => Boolean(getToken()))

const formName = computed(() => form.value.name || '信息收集')
const dynamicFields = computed(() => parseFormSchema(form.value.formSchema))
const requiresLogin = computed(() => form.value.loginRequired === true)

const canSubmit = computed(() => {
    if (!form.value.id) return false
    if (requiresLogin.value && !isLogin.value) return false
    if (!['open', 'published'].includes(form.value.status || '')) return false
    const start = form.value.startAt ? new Date(form.value.startAt).getTime() : 0
    const end = form.value.endAt ? new Date(form.value.endAt).getTime() : 0
    const now = Date.now()
    return (!start || start <= now) && (!end || end >= now)
})

function initForm() {
    Object.keys(formData).forEach((key) => delete formData[key])
    dynamicFields.value.forEach((field) => {
        formData[field.key] = ''
    })
    anonymousName.value = ''
    anonymousContact.value = ''
}

async function load() {
    loading.value = true
    loadFailed.value = false
    try {
        const res: any = await siteApi.formDetail(formId.value)
        form.value = res?.form || {}
        initForm()
    } catch {
        loadFailed.value = true
    } finally {
        loading.value = false
    }
}

function onPickField(field: FormField, event: any) {
    const index = Number(event.detail.value)
    const option = (field.options || [])[index]
    formData[field.key] = optionValue(option)
}

function updateField(key: string, value: string) {
    formData[key] = value
}

function fieldOptions(field: FormField) {
    return (field.options || []).map(optionLabel)
}

function validate() {
    if (!form.value.loginRequired) {
        if (!anonymousName.value.trim()) {
            uni.showToast({ title: '请填写姓名', icon: 'none' })
            return false
        }
        if (!anonymousContact.value.trim()) {
            uni.showToast({ title: '请填写联系方式', icon: 'none' })
            return false
        }
    }
    const invalid = dynamicFields.value
        .map((field) => validateField(field, formData[field.key]))
        .find(Boolean)
    if (invalid) {
        uni.showToast({ title: invalid, icon: 'none' })
        return false
    }
    return true
}

async function submitForm() {
    if (requiresLogin.value && !isLogin.value) {
        uni.navigateTo({url: loginUrl(`/pages/forms/index?id=${formId.value}`)})
        return
    }
    if (!validate()) return
    submitting.value = true
    try {
        await formSubmissionApi.create(Number(formId.value), {
            anonymousName: anonymousName.value || undefined,
            anonymousContact: anonymousContact.value || undefined,
            formData: buildFormData(),
        })
        uni.showToast({ title: '提交成功', icon: 'success' })
        setTimeout(() => uni.reLaunch({ url: '/pages/home/index' }), 800)
    } catch {
        // 错误已在请求拦截器中提示
    } finally {
        submitting.value = false
    }
}

function buildFormData() {
    const data: Record<string, any> = {}
    dynamicFields.value.forEach((field) => {
        const val = formData[field.key]
        if (val !== undefined && val !== null && val !== '') {
            data[field.key] = val
        }
    })
    return data
}

function formatRange(startAt: string, endAt: string) {
    const s = startAt ? startAt.slice(0, 10) : '?'
    const e = endAt ? endAt.slice(0, 10) : '?'
    return `${s} 至 ${e}`
}

function goBack() {
    uni.reLaunch({ url: '/pages/home/index' })
}

onLoad((options?: { id?: string }) => {
    formId.value = options?.id || ''
    load()
})
</script>

<style scoped>
.page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background: #f5f5f7;
}

.main-scroll {
    flex: 1;
    height: 0;
}

.info-panel {
    margin: 20rpx 24rpx;
    padding: 24rpx 30rpx;
    border-radius: 18rpx;
    background: #fff;
    box-shadow: 0 12rpx 30rpx rgba(0, 0, 0, .08);
}

.info-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8rpx;
}

.info-label {
    color: #666668;
    font-size: 24rpx;
}

.info-value {
    color: #1d1d1f;
    font-size: 24rpx;
    font-weight: 600;
}

.info-desc {
    display: block;
    margin-top: 12rpx;
    color: #4f4f52;
    font-size: 25rpx;
    line-height: 1.5;
}

.form-panel {
    margin: 20rpx 24rpx;
    padding: 30rpx;
    border-radius: 18rpx;
    background: #fff;
    box-shadow: 0 12rpx 30rpx rgba(0, 0, 0, .08);
}

.field {
    margin-bottom: 24rpx;
}

.field:last-child {
    margin-bottom: 0;
}

.label {
    display: block;
    margin-bottom: 12rpx;
    color: #333333;
    font-size: 25rpx;
    font-weight: 700;
}

.input,
.textarea,
.picker-value {
    width: 100%;
    box-sizing: border-box;
    border-radius: 14rpx;
    background: #f5f5f7;
    border: 1rpx solid #e2e8f0;
    color: #1d1d1f;
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

.submit-bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 20;
    padding: 18rpx 24rpx 30rpx;
    background: rgba(255, 255, 255, .96);
    box-shadow: 0 -8rpx 24rpx rgba(15, 23, 42, .06);
}

.submit-btn,
.primary-btn {
    height: 82rpx;
    line-height: 82rpx;
    border-radius: 16rpx;
    background: #1d1d1f;
    color: #fff;
    font-size: 28rpx;
    font-weight: 700;
}

.submit-btn[disabled] {
    background: #d2d2d7;
    color: #fff;
}

.submit-btn::after,
.primary-btn::after {
    border: 0;
}

.loading,
.empty {
    padding: 120rpx 40rpx;
    text-align: center;
    color: #666668;
}

.empty__title {
    display: block;
    margin-bottom: 24rpx;
    color: #1d1d1f;
    font-size: 32rpx;
    font-weight: 800;
}

.bottom-pad {
    height: 150rpx;
}
</style>
