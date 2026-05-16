<template>
  <span class="user-avatar" :style="avatarStyle">
    <img v-if="showImage" :alt="altText" :src="src" @error="imageFailed = true" />
    <span v-else>{{ initial }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string
    name?: string
    size?: number
  }>(),
  {
    src: '',
    name: '',
    size: 56,
  },
)

const imageFailed = ref(false)

watch(
  () => props.src,
  () => {
    imageFailed.value = false
  },
)

const showImage = computed(() => Boolean(props.src) && !imageFailed.value)
const initial = computed(() => Array.from((props.name || '').trim())[0]?.toUpperCase() || '?')
const altText = computed(() => (props.name ? `${props.name}的头像` : '用户头像'))
const avatarStyle = computed(() => ({
  '--avatar-size': `${props.size}px`,
}))
</script>

<style scoped>
.user-avatar {
  display: inline-grid;
  width: var(--avatar-size);
  height: var(--avatar-size);
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 999px;
  background: #1d1d1f;
  color: #ffffff;
  font-family:
    'SF Pro Display',
    system-ui,
    sans-serif;
  font-size: calc(var(--avatar-size) * 0.42);
  font-weight: 600;
  line-height: 1;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

:global(html.dark) .user-avatar {
  border-color: rgba(245, 245, 247, 0.16);
  background: #f5f5f7;
  color: #09090b;
}
</style>
