<template>
  <section
    class="site-page-hero"
    :class="{
      'site-page-hero--compact': compact,
      'site-page-hero--illustrated': lightImage,
    }"
  >
    <div v-if="lightImage" class="site-page-hero__artwork" aria-hidden="true">
      <img class="site-page-hero__image site-page-hero__image--light" :src="lightImage" alt="" />
      <img
        v-if="darkImage"
        class="site-page-hero__image site-page-hero__image--dark"
        :src="darkImage"
        alt=""
      />
    </div>

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
    lightImage?: string
    darkImage?: string
  }>(),
  {
    description: '',
    compact: false,
    lightImage: '',
    darkImage: '',
  },
)
</script>

<style scoped>
.site-page-hero {
  position: relative;
  overflow: hidden;
  isolation: isolate;
  background: var(--oa-page-soft-bg);
  border-bottom: 1px solid var(--oa-border);
}

.site-page-hero--illustrated {
  background: #faf9f7;
}

:global(html.dark .site-page-hero--illustrated) {
  background: #0d0d0f;
}

.site-page-hero__artwork {
  position: absolute;
  z-index: 0;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.site-page-hero__image {
  position: absolute;
  top: 0;
  right: 0;
  display: block;
  width: auto;
  max-width: none;
  height: 100%;
}

.site-page-hero__image--dark {
  display: none;
}

:global(html.dark .site-page-hero__image--light) {
  display: none;
}

:global(html.dark .site-page-hero__image--dark) {
  display: block;
}

.site-page-hero__inner {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: clamp(320px, 38vh, 420px);
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding-top: clamp(48px, 7vw, 80px);
  padding-bottom: clamp(48px, 7vw, 80px);
  text-align: left;
}

.site-page-hero--illustrated .site-page-hero__inner {
  min-height: clamp(440px, 38vw, 560px);
  align-items: flex-start;
  justify-content: center;
  flex-direction: column;
  padding-top: 56px;
  padding-bottom: 56px;
}

.site-page-hero--compact:not(.site-page-hero--illustrated) .site-page-hero__inner {
  min-height: clamp(250px, 30vh, 320px);
}

.site-page-hero__copy {
  width: min(820px, 100%);
}

.site-page-hero--illustrated .site-page-hero__copy {
  width: min(680px, 44%);
}

.site-page-hero__copy span {
  display: inline-block;
  color: var(--oa-muted-strong);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.site-page-hero__copy h1 {
  margin: 12px 0 16px;
  font-family: var(--font-family-display);
  font-size: clamp(36px, 4vw, 52px);
  font-weight: 700;
  line-height: 1.06;
}

.site-page-hero__copy p {
  max-width: 680px;
  margin: 0;
  color: var(--oa-muted);
  font-size: clamp(16px, 1.6vw, 19px);
  font-weight: 400;
  line-height: 1.7;
}

.site-page-hero__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.site-page-hero--illustrated .site-page-hero__actions {
  margin-top: 28px;
  justify-content: flex-start;
}

@media (max-width: 720px) {
  .site-page-hero__inner {
    min-height: auto;
    align-items: flex-start;
    flex-direction: column;
    padding-top: 44px;
    padding-bottom: 44px;
  }

  .site-page-hero__actions {
    width: 100%;
    justify-content: flex-start;
  }

  .site-page-hero--illustrated .site-page-hero__inner {
    min-height: 680px;
    justify-content: flex-start;
    padding-top: 44px;
    padding-bottom: 320px;
  }

  .site-page-hero--illustrated .site-page-hero__copy {
    width: 100%;
  }

  .site-page-hero--illustrated .site-page-hero__artwork {
    top: auto;
    height: 340px;
  }

  .site-page-hero--illustrated .site-page-hero__image {
    right: 20px;
    bottom: 0;
    top: auto;
    width: 720px;
    max-width: none;
    height: auto;
  }
}
</style>
