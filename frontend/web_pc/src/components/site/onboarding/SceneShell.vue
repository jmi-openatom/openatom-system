<template>
  <div class="scene" :class="[`scene--${tone}`, `scene--${align}`]">
    <span v-if="chapter" class="scene__chapter" data-parallax="-0.25">{{ chapter }}</span>
    <div class="scene__body">
      <p v-if="eyebrow" class="scene__eyebrow">{{ eyebrow }}</p>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    chapter?: string
    eyebrow?: string
    tone?: 'dark' | 'light' | 'dawn'
    align?: 'left' | 'right' | 'center'
  }>(),
  { tone: 'dark', align: 'center' },
)
</script>

<style scoped>
.scene {
  position: relative;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  min-height: 100vh;
  padding: 0 clamp(24px, 8vw, 140px);
  color: #f5f5f7;
}

/* 浅色版本 */
html:not(.dark) .scene {
  color: #1d1d1f;
}

/* 默认居中 */
.scene--center {
  justify-content: center;
  text-align: center;
}
.scene--center .scene__body {
  justify-items: center;
}
.scene--center .scene__chapter {
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

/* 左对齐：内容贴左，章节数字压在右上 */
.scene--left {
  justify-content: flex-start;
  text-align: left;
}
.scene--left .scene__body {
  justify-items: start;
}
.scene--left .scene__chapter {
  right: -2vw;
  top: 18%;
  left: auto;
  transform: none;
}

/* 右对齐：内容贴右，章节数字压在左下 */
.scene--right {
  justify-content: flex-end;
  text-align: right;
}
.scene--right .scene__body {
  justify-items: end;
}
.scene--right .scene__chapter {
  left: -2vw;
  bottom: 12%;
  top: auto;
  transform: none;
}

.scene__chapter {
  position: absolute;
  font-size: clamp(180px, 32vw, 460px);
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1;
  color: #f5f5f7;
  opacity: 0.06;
  pointer-events: none;
  user-select: none;
  font-family: ui-serif, Georgia, 'Times New Roman', serif;
  font-style: italic;
  z-index: 0;
}

html:not(.dark) .scene__chapter {
  color: #1d1d1f;
  opacity: 0.05;
}

.scene__body {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 28px;
  max-width: 760px;
}

.scene__eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: rgba(245, 245, 247, 0.62);
  font-weight: 500;
}

html:not(.dark) .scene__eyebrow {
  color: rgba(29, 29, 31, 0.56);
}

.scene__body :deep(h2) {
  margin: 0;
  font-size: clamp(30px, 5.4vw, 64px);
  line-height: 1.16;
  letter-spacing: -0.02em;
  font-weight: 700;
  text-shadow: 0 2px 30px rgba(0, 0, 0, 0.7);
}

html:not(.dark) .scene__body :deep(h2) {
  text-shadow: 0 2px 30px rgba(255, 255, 255, 0.7);
}

.scene__body :deep(p) {
  margin: 0;
  font-size: clamp(16px, 1.9vw, 21px);
  line-height: 1.95;
  color: rgba(245, 245, 247, 0.92);
  max-width: 620px;
  text-shadow: 0 1px 16px rgba(0, 0, 0, 0.65);
}

html:not(.dark) .scene__body :deep(p) {
  color: rgba(29, 29, 31, 0.88);
  text-shadow: 0 1px 16px rgba(255, 255, 255, 0.6);
}

.scene--center .scene__body :deep(p) {
  max-width: 640px;
}

.scene__body :deep(em) {
  font-style: normal;
  background: linear-gradient(120deg, #ffffff, rgba(245, 245, 247, 0.6));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-weight: 600;
}

html:not(.dark) .scene__body :deep(em) {
  background: linear-gradient(120deg, #1d1d1f, rgba(29, 29, 31, 0.5));
  -webkit-background-clip: text;
  background-clip: text;
}

.scene--dawn .scene__chapter {
  opacity: 0.09;
}

html:not(.dark) .scene--dawn .scene__chapter {
  opacity: 0.07;
}

@media (max-width: 720px) {
  .scene--left,
  .scene--right {
    justify-content: center;
    text-align: center;
    padding: 0 clamp(24px, 6vw, 48px);
  }
  .scene--left .scene__body,
  .scene--right .scene__body {
    justify-items: center;
  }
  .scene--left .scene__chapter,
  .scene--right .scene__chapter {
    left: 50%;
    top: 50%;
    right: auto;
    bottom: auto;
    transform: translate(-50%, -50%);
  }
}
</style>
