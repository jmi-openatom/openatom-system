<template>
  <section id="overview" class="hero">
    <HomeMapSection background class="hero__map" />
    <div aria-hidden="true" class="hero__glass"></div>
    <div class="container hero__inner">
      <div class="hero__content">
        <h1 class="sr-only">开放原子开源社团 · JMI-OPENATOM</h1>
        <MorphingText
          aria-hidden="true"
          class="home-hero__morph title"
          :cool-down-time="coolDownTime"
          :morph-time="morphTime"
          :texts="texts"
        />
        <p class="hero__subtitle">在真实项目中协作，在工程实践中成长。让开源，成为校园日常。</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import HomeMapSection from './HomeMapSection.vue'
import { MorphingText } from '@/components/ui/morphing-text'
interface HeroProps {
  text?: string
  revealDelayMs?: number
  coolDownTime?: number
  morphTime?: number
  texts: string[]
}

withDefaults(defineProps<HeroProps>(), {
  text: '技术分享 · 项目实践 · 竞赛训练 · 开源协作',
  revealDelayMs: 100,
})
</script>

<style>
/* 海报感主标题：Orbitron 超粗 + 宽字距 + 纯色 + 投影 */
.home-hero__morph.title {
  font-family: var(--font-family-display);
  font-weight: 900;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

.home-hero__morph.title span {
  color: #16161c;
  filter: drop-shadow(0 10px 26px rgba(0, 0, 0, 0.22));
}

html.dark .home-hero__morph.title span {
  color: #ffffff;
  filter: drop-shadow(0 12px 30px rgba(0, 0, 0, 0.45));
}
</style>
