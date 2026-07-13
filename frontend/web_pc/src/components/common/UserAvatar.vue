<template>
  <span class="user-avatar" :style="avatarStyle">
    <img
      v-if="showImage"
      :alt="altText"
      :src="imageSrc"
      decoding="async"
      loading="lazy"
      @error="handleImageError"
    />
    <span v-else>{{ initial }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string
    fallbackSrc?: string
    qqOpenid?: string
    name?: string
    size?: number
  }>(),
  {
    src: '',
    fallbackSrc: '',
    qqOpenid: '',
    name: '',
    size: 56,
  },
)

const failedSources = ref<Set<string>>(new Set())

watch([() => props.src, () => props.fallbackSrc, () => props.qqOpenid], () => {
  failedSources.value = new Set()
})

const uploadedSrc = computed(() => normalizeSource(props.src))
const fallbackSrc = computed(() => normalizeSource(props.fallbackSrc))
const qqSrc = computed(() => qqAvatarUrl(props.qqOpenid))
const imageSrc = computed(() => {
  if (uploadedSrc.value && !failedSources.value.has(uploadedSrc.value)) return uploadedSrc.value
  if (fallbackSrc.value && !failedSources.value.has(fallbackSrc.value)) return fallbackSrc.value
  if (qqSrc.value && !failedSources.value.has(qqSrc.value)) return qqSrc.value
  return ''
})
const showImage = computed(() => Boolean(imageSrc.value))
const initial = computed(() => Array.from((props.name || '').trim())[0]?.toUpperCase() || '?')
const altText = computed(() => (props.name ? `${props.name}的头像` : '用户头像'))
const avatarStyle = computed(() => ({
  '--avatar-size': `${props.size}px`,
}))

function handleImageError() {
  if (!imageSrc.value) return
  failedSources.value = new Set([...failedSources.value, imageSrc.value])
}

function normalizeSource(value?: string) {
  return String(value || '').trim()
}

function qqAvatarUrl(value?: string) {
  const qq = normalizeSource(value)
  if (!/^\d{5,15}$/.test(qq)) return ''
  return `https://q1.qlogo.cn/g?b=qq&nk=${encodeURIComponent(qq)}&s=640`
}
</script>

<style scoped>
.user-avatar {
  display: inline-grid;
  width: var(--avatar-size);
  height: var(--avatar-size);
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(29, 29, 31, 0.12);
  border-radius: 999px;
  background: #1d1d1f;
  color: #ffffff;
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: calc(var(--avatar-size) * 0.42);
  font-weight: 600;
  line-height: 1;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
