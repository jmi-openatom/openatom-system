<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="true" title="入会申请"/>
        <scroll-view class="main-scroll" scroll-y>
            <view v-if="loading" class="loading">加载中...</view>

            <template v-else-if="campaign.id">
                <ApplyHeader
                    :campaign-name="campaign.name"
                    :club-name="club.name"
                    :range="formatRange(campaign.applyStartAt, campaign.applyEndAt)"
                />

                <DepartmentPickerPanel
                    :department-names="departmentNames"
                    :first-department-name="firstDepartmentName"
                    :optional-department-names="optionalDepartmentNames"
                    :second-department-name="secondDepartmentName"
                    @pick-first="onPickFirst"
                    @pick-second="onPickSecond"
                />

                <DynamicFormPanel
                    :fields="dynamicFields"
                    :form-data="formData"
                    :has-applicant-name-field="hasApplicantNameField"
                    :has-contact-field="hasContactField"
                    @pick-field="onPickField"
                    @update-field="updateField"
                />

                <view class="tips">
                    <text>无需登录即可提交申请，请确保姓名和联系方式填写准确。</text>
                    <text v-if="isLogin" class="tips__warn">已登录提交会自动关联当前账号。</text>
                </view>
            </template>

            <view v-else class="empty">
                <text class="empty__title">表单不存在或暂未开放</text>
                <button class="primary-btn" @tap="goBack">返回招新</button>
            </view>

            <view class="bottom-pad"/>
        </scroll-view>

        <view v-if="campaign.id" class="submit-bar">
            <button class="submit-btn" :disabled="submitting || !canSubmit" @tap="submit">
                {{ submitting ? '提交中...' : canSubmit ? '提交申请' : '当前不可提交' }}
            </button>
        </view>
    </view>
</template>

<script setup lang="ts">
import {applicationApi, clubApi, siteApi} from '@/api'
import {formatRange} from '@/utils/format'
import {getToken} from '@/utils/auth'
import {optionValue, parseFormSchema} from '@/utils/formSchema'
import type {FormField} from '@/utils/formSchema'
import ApplyHeader from './components/ApplyHeader.vue'
import DepartmentPickerPanel from './components/DepartmentPickerPanel.vue'
import DynamicFormPanel from './components/DynamicFormPanel.vue'
import {computed, reactive, ref} from 'vue'
import {onLoad} from '@dcloudio/uni-app'

const campaignId = ref('')
const loading = ref(true)
const submitting = ref(false)
const club = ref<Record<string, any>>({})
const campaign = ref<Record<string, any>>({})
const departments = ref<Record<string, any>[]>([])
const formData = reactive<Record<string, any>>({})
const firstChoiceDepartmentId = ref<number | string>('')
const secondChoiceDepartmentId = ref<number | string>('')
const isLogin = computed(() => Boolean(getToken()))

const dynamicFields = computed(() => parseFormSchema(campaign.value.formSchema))
const departmentNames = computed(() => departments.value.map((item) => item.name || `部门 ${item.id}`))
const optionalDepartmentNames = computed(() => ['不选择', ...departmentNames.value])
const firstDepartmentName = computed(() => departments.value.find((item) => item.id === firstChoiceDepartmentId.value)?.name || '')
const secondDepartmentName = computed(() => departments.value.find((item) => item.id === secondChoiceDepartmentId.value)?.name || '')
const hasApplicantNameField = computed(() => dynamicFields.value.some((field) => field.key === 'applicantName'))
const hasContactField = computed(() => dynamicFields.value.some((field) => field.key === 'contact'))
const requiresLogin = computed(() => false)
const canSubmit = computed(() => {
    if (!campaign.value.id) return false
    if (['closed', 'archived'].includes(campaign.value.status)) return false
    const start = campaign.value.applyStartAt ? new Date(campaign.value.applyStartAt).getTime() : 0
    const end = campaign.value.applyEndAt ? new Date(campaign.value.applyEndAt).getTime() : 0
    const now = Date.now()
    return (!start || start <= now) && (!end || end >= now)
})

function initForm() {
    Object.keys(formData).forEach((key) => delete formData[key])
    dynamicFields.value.forEach((field) => {
        formData[field.key] = ''
    })
    formData.applicantName = ''
    formData.contact = ''
}

async function load() {
    loading.value = true
    try {
        const res: any = await siteApi.recruitmentDetail(campaignId.value)
        club.value = res?.club || {}
        campaign.value = res?.campaign || {}
        departments.value = res?.departments || []
        if (!departments.value.length && club.value.id) {
            const deptRes: any = await clubApi.departments(club.value.id)
            departments.value = Array.isArray(deptRes) ? deptRes : deptRes?.list || []
        }
        initForm()
    } finally {
        loading.value = false
    }
}

function onPickFirst(event: any) {
    const index = Number(event.detail.value)
    firstChoiceDepartmentId.value = departments.value[index]?.id || ''
}

function onPickSecond(event: any) {
    const index = Number(event.detail.value)
    secondChoiceDepartmentId.value = index <= 0 ? '' : departments.value[index - 1]?.id || ''
}

function onPickField(field: FormField, event: any) {
    const index = Number(event.detail.value)
    const option = (field.options || [])[index]
    formData[field.key] = optionValue(option)
}

function updateField(key: string, value: string) {
    formData[key] = value
}

function validate() {
    if (!firstChoiceDepartmentId.value && departments.value.length) {
        uni.showToast({title: '请选择第一志愿部门', icon: 'none'})
        return false
    }
    const requiredFields = dynamicFields.value.filter((field) => field.required)
    const missing = requiredFields.find((field) => !(formData[field.key] || '').trim())
    if (missing) {
        uni.showToast({title: `请填写${missing.label || missing.key}`, icon: 'none'})
        return false
    }
    if (!(formData.applicantName || '').trim()) {
        uni.showToast({title: '请填写姓名', icon: 'none'})
        return false
    }
    if (!(formData.contact || '').trim()) {
        uni.showToast({title: '请填写联系方式', icon: 'none'})
        return false
    }
    return true
}

function buildProfile() {
    const profile: Record<string, any> = {}
    dynamicFields.value.forEach((field) => {
        const val = formData[field.key]
        if (val !== undefined && val !== null && val !== '') {
            profile[field.key] = val
        }
    })
    if (!hasApplicantNameField.value && formData.applicantName) {
        profile.applicantName = formData.applicantName
    }
    if (!hasContactField.value && formData.contact) {
        profile.contact = formData.contact
    }
    return profile
}

async function submit() {
    if (!validate()) return
    submitting.value = true
    try {
        await applicationApi.create({
            campaignId: Number(campaign.value.id),
            clubId: Number(club.value.id),
            firstChoiceDepartmentId: firstChoiceDepartmentId.value ? Number(firstChoiceDepartmentId.value) : undefined,
            secondChoiceDepartmentId: secondChoiceDepartmentId.value ? Number(secondChoiceDepartmentId.value) : undefined,
            profile: buildProfile(),
        })
        uni.showToast({title: '申请已提交', icon: 'success'})
        const target = getToken() ? '/pages/progress/index' : '/pages/recruitment/index'
        setTimeout(() => uni.redirectTo({url: target}), 600)
    } catch (e: any) {
        const msg = e?.message || e?.data?.message || '提交失败，请稍后重试'
        uni.showToast({title: msg, icon: 'none'})
    } finally {
        submitting.value = false
    }
}

function goBack() {
    uni.redirectTo({url: '/pages/recruitment/index'})
}

onLoad((options?: { id?: string }) => {
    campaignId.value = options?.id || ''
    load()
})
</script>

<style scoped>
.page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background: #f7fafc;
}

.main-scroll {
    flex: 1;
    height: 0;
}

.tips {
    margin: 0 24rpx;
    color: #64748b;
    font-size: 23rpx;
    line-height: 1.6;
}

.tips__warn {
    display: block;
    margin-bottom: 10rpx;
    color: #b45309;
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
    border-radius: 999rpx;
    background: #1769e8;
    color: #fff;
    font-size: 28rpx;
    font-weight: 700;
}

.submit-btn[disabled] {
    background: #cbd5e1;
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
    color: #64748b;
}

.empty__title {
    display: block;
    margin-bottom: 24rpx;
    color: #0f172a;
    font-size: 32rpx;
    font-weight: 800;
}

.bottom-pad {
    height: 150rpx;
}
</style>
