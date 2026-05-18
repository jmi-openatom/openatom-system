<template>
  <section class="site-page-hero" :class="{ 'site-page-hero--compact': compact }">
    <div class="container site-page-hero__inner">
      <div class="site-page-hero__copy">
        <span>{{ eyebrow }}</span>
        <h1>{{ title }}</h1>
        <p v-if="description">{{ description }}</p>
      </div>

      <div v-if="$slots.actions" class="site-page-hero__actions">
        <slot name="actions" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow: string
    title: string
    description?: string
    compact?: boolean
  }>(),
  {
    description: '',
    compact: false,
  },
)
</script>

<style scoped>
.site-page-hero {
  background: linear-gradient(180deg, var(--oa-elevated-bg) 0%, var(--oa-page-soft-bg) 100%);
}

.site-page-hero__inner {
  display: grid;
  justify-items: center;
  gap: 24px;
  min-height: clamp(320px, 42vh, 460px);
  align-content: center;
  padding: clamp(56px, 8vw, 96px) 0;
  text-align: center;
}

.site-page-hero--compact .site-page-hero__inner {
  min-height: clamp(250px, 30vh, 340px);
}

.site-page-hero__copy {
  width: min(820px, 100%);
}

.site-page-hero__copy span {
  display: inline-block;
  color: var(--oa-text);
  font-size: 14px;
}

.site-page-hero__copy h1 {
  margin: 12px 0 16px;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: clamp(36px, 4vw, 56px);
  font-weight: 600;
  line-height: 1.06;
}

.site-page-hero__copy p {
  max-width: 720px;
  margin: 0 auto;
  color: var(--oa-muted);
  font-size: clamp(17px, 1.8vw, 22px);
  font-weight: 300;
  line-height: 1.7;
}

.site-page-hero__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

@media (max-width: 720px) {
  .site-page-hero__inner {
    min-height: auto;
    padding: 44px 0 48px;
  }
}
</style>
