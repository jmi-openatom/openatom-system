<script lang="ts" setup>
import { computed } from 'vue'
import type { ClubHomeMetric } from '../types'

const props = withDefaults(
  defineProps<{
    metrics?: ClubHomeMetric[]
    clubName?: string
  }>(),
  {
    metrics: () => [],
    clubName: '',
  },
)

const list = computed(() =>
  (props.metrics || []).filter((m) => m && (m.label || m.value || m.note)),
)

function tone(label?: string) {
  if (!label) return 'default'
  if (label.includes('成员')) return 'members'
  if (label.includes('活动')) return 'activity'
  if (label.includes('获奖')) return 'award'
  if (label.includes('招新')) return 'recruit'
  return 'default'
}
</script>

<template>
  <view class="metrics">
    <view class="metrics__header">
      <text class="metrics__title">{{ clubName ? `${clubName} · 数据概览` : '社团数据概览' }}</text>
      <text class="metrics__subtitle">来自首页接口的实时统计</text>
    </view>

    <view v-if="list.length" class="metrics__grid">
      <view
        v-for="(item, index) in list"
        :key="`${item.label || 'm'}-${index}`"
        class="metrics__card"
        :class="`metrics__card--${tone(item.label)}`"
      >
        <text class="metrics__label">{{ item.label }}</text>
        <text class="metrics__value">{{ item.value }}</text>
        <text v-if="item.note" class="metrics__note">{{ item.note }}</text>
      </view>
    </view>

    <view v-else class="metrics__empty">
      <text class="metrics__empty-title">暂无统计数据</text>
      <text class="metrics__empty-desc">请确认后端已返回 club-home 的 metrics 字段</text>
    </view>
  </view>
</template>

<style scoped>
.metrics {
  margin: 0 24rpx 32rpx;
  padding: 28rpx 24rpx 32rpx;
  border-radius: 24rpx;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.98));
  box-shadow: 0 16rpx 40rpx rgba(31, 55, 88, 0.08);
  border: 1rpx solid rgba(226, 232, 240, 0.95);
  box-sizing: border-box;
}

.metrics__header {
  margin-bottom: 24rpx;
}

.metrics__title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: 0.02em;
}

.metrics__subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #64748b;
  line-height: 1.5;
}

.metrics__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}

.metrics__card {
  padding: 22rpx 20rpx;
  border-radius: 20rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.95);
  background: linear-gradient(165deg, #ffffff, #f8fafc);
  min-height: 140rpx;
  box-sizing: border-box;
}

.metrics__card--members {
  border-color: rgba(191, 219, 254, 0.85);
  background: linear-gradient(165deg, #ffffff, #eff6ff);
}

.metrics__card--activity {
  border-color: rgba(187, 247, 208, 0.85);
  background: linear-gradient(165deg, #ffffff, #f0fdf4);
}

.metrics__card--award {
  border-color: rgba(254, 215, 170, 0.9);
  background: linear-gradient(165deg, #ffffff, #fffbeb);
}

.metrics__card--recruit {
  border-color: rgba(233, 213, 255, 0.9);
  background: linear-gradient(165deg, #ffffff, #faf5ff);
}

.metrics__label {
  display: block;
  font-size: 24rpx;
  color: #64748b;
  line-height: 1.35;
}

.metrics__value {
  display: block;
  margin-top: 12rpx;
  font-size: 44rpx;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.metrics__note {
  display: block;
  margin-top: 10rpx;
  font-size: 20rpx;
  color: #94a3b8;
  line-height: 1.45;
}

.metrics__empty {
  padding: 36rpx 12rpx;
  text-align: center;
}

.metrics__empty-title {
  display: block;
  font-size: 26rpx;
  color: #64748b;
}

.metrics__empty-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #94a3b8;
  line-height: 1.5;
}
</style>
