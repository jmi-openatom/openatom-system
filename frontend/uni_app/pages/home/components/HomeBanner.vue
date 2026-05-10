<script lang="ts" setup>
import type { ClubHomeClub } from '../types'

const props = withDefaults(
  defineProps<{
    club?: ClubHomeClub | null
    loading?: boolean
  }>(),
  {
    club: null,
    loading: false,
  },
)

const emit = defineEmits<{
  join: []
  activities: []
}>()

const eyebrow = () => props.club?.code || 'JMI-OPENATOM'
const title = () => props.club?.name || '开放原子开源社团'
const desc = () =>
  props.club?.description ||
  '连接校园开发者、项目实践与开源协作。'
const logo = () => props.club?.logoUrl || '/static/logo.png'
const recruitHint = () => {
  const s = props.club?.recruitmentStatus
  if (!s) return ''
  const key = String(s).toLowerCase()
  const map: Record<string, string> = {
    open: '招新进行中',
    opening: '招新进行中',
    published: '招新进行中',
    closed: '招新已结束',
    close: '招新已结束',
    paused: '招新暂停',
    draft: '招新筹备中',
  }
  return map[key] || s
}
</script>

<template>
  <view class="banner">
    <view v-if="loading" class="banner__skeleton">
      <view class="sk sk-line sk-line--short" />
      <view class="sk sk-line sk-line--title" />
      <view class="sk sk-line sk-line--mid" />
      <view class="sk sk-line sk-line--long" />
      <view class="sk sk-actions" />
    </view>

    <template v-else>
      <view class="banner__content">
        <view class="banner__eyebrow">
          <view class="banner__dot" />
          <text>{{ eyebrow() }}</text>
        </view>

        <text class="banner__title">{{ title() }}</text>
        <text class="banner__desc">{{ desc() }}</text>

        <view v-if="recruitHint()" class="banner__chip">
          <text>{{ recruitHint() }}</text>
        </view>

        <view class="banner__actions">
          <view class="banner__button banner__button--primary" @tap="emit('join')">
            <text>加入社团</text>
          </view>
          <view class="banner__button banner__button--ghost" @tap="emit('activities')">
            <text>近期活动</text>
          </view>
        </view>
      </view>

      <view class="banner__visual">
        <view class="banner__logo-wrap">
          <image class="banner__logo" mode="aspectFit" :src="logo()" />
        </view>
        <view class="banner__badge banner__badge--top">
          <text class="banner__badge-value">Open</text>
          <text class="banner__badge-label">Source</text>
        </view>
        <view class="banner__badge banner__badge--bottom">
          <text class="banner__badge-value">JMI</text>
          <text class="banner__badge-label">Club</text>
        </view>
      </view>
    </template>
  </view>
</template>

<style scoped>
.banner {
  position: relative;
  display: flex;
  min-height: 360rpx;
  margin: 0 24rpx 24rpx;
  padding: 36rpx 32rpx;
  overflow: hidden;
  border-radius: 24rpx;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.78)),
    linear-gradient(135deg, #e7f6ff 0%, #f5f0ff 48%, #fff4dc 100%);
  box-shadow: 0 24rpx 54rpx rgba(31, 55, 88, 0.12);
  box-sizing: border-box;
}

.banner::before {
  content: '';
  position: absolute;
  right: -130rpx;
  top: -120rpx;
  width: 360rpx;
  height: 360rpx;
  border-radius: 50%;
  background: rgba(58, 132, 255, 0.12);
}

.banner::after {
  content: '';
  position: absolute;
  right: 58rpx;
  bottom: -150rpx;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  background: rgba(255, 181, 62, 0.16);
}

.banner__content {
  position: relative;
  z-index: 1;
  flex: 1;
  min-width: 0;
  padding-right: 18rpx;
}

.banner__eyebrow {
  display: inline-flex;
  align-items: center;
  height: 44rpx;
  padding: 0 18rpx;
  border: 1rpx solid rgba(40, 88, 152, 0.14);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  color: #34506f;
  font-size: 22rpx;
  font-weight: 700;
}

.banner__dot {
  width: 12rpx;
  height: 12rpx;
  margin-right: 10rpx;
  border-radius: 50%;
  background: #21b6a8;
  box-shadow: 0 0 0 8rpx rgba(33, 182, 168, 0.12);
}

.banner__title {
  display: block;
  max-width: 390rpx;
  margin-top: 24rpx;
  color: #142033;
  font-size: 46rpx;
  line-height: 1.16;
  font-weight: 800;
}

.banner__desc {
  display: block;
  max-width: 410rpx;
  margin-top: 18rpx;
  color: #5d6c80;
  font-size: 26rpx;
  line-height: 1.48;
}

.banner__chip {
  display: inline-flex;
  margin-top: 16rpx;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(23, 105, 232, 0.1);
  color: #1769e8;
  font-size: 22rpx;
  font-weight: 600;
}

.banner__actions {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 30rpx;
}

.banner__button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 136rpx;
  height: 58rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 700;
  box-sizing: border-box;
}

.banner__button--primary {
  color: #fff;
  background: #1769e8;
  box-shadow: 0 12rpx 24rpx rgba(23, 105, 232, 0.22);
}

.banner__button--ghost {
  color: #294864;
  border: 1rpx solid rgba(41, 72, 100, 0.16);
  background: rgba(255, 255, 255, 0.62);
}

.banner__visual {
  position: relative;
  z-index: 1;
  width: 200rpx;
  min-height: 260rpx;
  flex-shrink: 0;
}

.banner__logo-wrap {
  position: absolute;
  right: 8rpx;
  top: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 166rpx;
  height: 166rpx;
  border-radius: 38rpx;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 20rpx 42rpx rgba(35, 61, 91, 0.15);
  transform: rotate(6deg);
}

.banner__logo {
  width: 112rpx;
  height: 112rpx;
  transform: rotate(-6deg);
}

.banner__badge {
  position: absolute;
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 108rpx;
  height: 74rpx;
  padding-left: 18rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 12rpx 28rpx rgba(35, 61, 91, 0.12);
  box-sizing: border-box;
}

.banner__badge--top {
  right: 78rpx;
  top: 4rpx;
}

.banner__badge--bottom {
  right: 0;
  bottom: 8rpx;
}

.banner__badge-value {
  color: #17263b;
  font-size: 23rpx;
  line-height: 1.1;
  font-weight: 800;
}

.banner__badge-label {
  margin-top: 4rpx;
  color: #738195;
  font-size: 18rpx;
  line-height: 1.1;
}

.banner__skeleton {
  flex: 1;
  padding: 8rpx 0;
}

.sk {
  border-radius: 12rpx;
  background: linear-gradient(90deg, #e8eef5 0%, #f4f7fb 50%, #e8eef5 100%);
  background-size: 200% 100%;
}

.sk-line {
  height: 28rpx;
  margin-bottom: 20rpx;
}

.sk-line--short {
  width: 160rpx;
}

.sk-line--title {
  width: 320rpx;
  height: 44rpx;
}

.sk-line--mid {
  width: 280rpx;
}

.sk-line--long {
  width: 100%;
  max-width: 360rpx;
}

.sk-actions {
  width: 300rpx;
  height: 58rpx;
  margin-top: 24rpx;
  border-radius: 999rpx;
}

@media screen and (max-width: 360px) {
  .banner {
    padding: 32rpx 28rpx;
  }

  .banner__title {
    max-width: 340rpx;
    font-size: 40rpx;
  }

  .banner__visual {
    width: 152rpx;
  }

  .banner__logo-wrap {
    right: 0;
    width: 132rpx;
    height: 132rpx;
    border-radius: 30rpx;
  }

  .banner__logo {
    width: 88rpx;
    height: 88rpx;
  }

  .banner__badge {
    display: none;
  }
}
</style>
