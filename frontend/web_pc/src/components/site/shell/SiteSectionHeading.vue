<template>
  <header class="site-section-heading" :class="{ 'site-section-heading--left': align === 'left' }">
    <span>{{ eyebrow }}</span>
    <h2>{{ title }}</h2>
    <p v-if="description">{{ description }}</p>
  </header>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow: string
    title: string
    description?: string
    align?: 'center' | 'left'
  }>(),
  {
    description: '',
    align: 'center',
  },
)
</script>

<style scoped>
.site-section-heading {
  position: relative;
  max-width: 760px;
  margin: 0 auto;
  text-align: center;
}

.site-section-heading::before {
  display: block;
  width: 68px;
  height: 1px;
  margin: 0 auto 18px;
  content: '';
  background: linear-gradient(90deg, transparent, var(--oa-text), transparent);
  opacity: 0.78;
}

.site-section-heading span {
  display: inline-block;
  color: var(--oa-text);
  font-size: 14px;
}

.site-section-heading h2 {
  margin: 10px 0 0;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: clamp(28px, 3vw, 40px);
  font-weight: 600;
  line-height: 1.1;
}

.site-section-heading p {
  max-width: 680px;
  margin: 14px auto 0;
  color: var(--oa-muted);
  line-height: 1.8;
}

.site-section-heading--left {
  margin: 0;
  text-align: left;
}

.site-section-heading--left::before {
  margin-left: 0;
}

.site-section-heading--left p {
  margin-left: 0;
}
</style>
