<script lang="ts" setup>
import {computed} from 'vue'

const props = withDefaults(
    defineProps<{
        tags?: string[]
    }>(),
    {tags: () => []},
)

const list = computed(() => (props.tags || []).filter(Boolean).slice(0, 12))
</script>

<template>
    <tm-sheet>
        <view v-if="list.length" class="block">
            <view class="block__head">
                <text class="block__title">技术方向</text>
                <text class="block__sub">来自部门名称</text>
            </view>
            <view class="tags">
                <text v-for="(t, i) in list" :key="`${t}-${i}`" class="tags__item">{{ t }}</text>
            </view>
        </view>
    </tm-sheet>
</template>

<style scoped>
.block {
    margin: 0 24rpx 32rpx;
}

.block__head {
    margin-bottom: 16rpx;
    padding: 0 4rpx;
}

.block__title {
    display: block;
    font-size: 30rpx;
    font-weight: 600;
    color: #1d1d1f;
}

.block__sub {
    display: block;
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #8e8e93;
}

.tags {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
}

.tags__item {
    padding: 10rpx 20rpx;
    border-radius: 16rpx;
    font-size: 22rpx;
    color: #1e3a5f;
    background: rgba(255, 255, 255, 0.92);
    border: 1rpx solid rgba(226, 232, 240, 0.95);
    box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.06);
}
</style>
