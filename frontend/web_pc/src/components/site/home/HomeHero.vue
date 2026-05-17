<template>
  <section id="overview" class="hero">
    <HomeMapSection background class="hero__map" />
    <div aria-hidden="true" class="hero__glass"></div>
    <div class="container hero__inner">
      <div class="hero__content">
        <MorphingText
          class="home-hero__morph"
          :cool-down-time="coolDownTime"
          :morph-time="morphTime"
          :texts="texts"
        />
        <p class="hero__subtitle">开放协作、工程实践、真实项目，在校园里把开源做成日常。</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import HomeMapSection from './HomeMapSection.vue'
import { MorphingText } from '@/components/ui/morphing-text'
import type { BaseProps } from '../../ui/pattern-background'
import { PATTERN_BACKGROUND_SPEED } from '../../ui/pattern-background'
// 1. Define a single interface for all props
interface HeroProps extends BaseProps {
  text?: string
  revealDelayMs?: number
  coolDownTime?: number
  morphTime?: number
  texts: string[]
}

// 2. Use withDefaults to handle your default values cleanly
withDefaults(defineProps<HeroProps>(), {
  text: '技术分享 · 项目实践 · 竞赛训练 · 开源协作',
  revealDelayMs: 100,
  animate: true,
  direction: 'bottom',
  variant: 'big-dot',
  size: 'md',
  mask: 'ellipse-top',
  speed: () => PATTERN_BACKGROUND_SPEED.Slow,
})
</script>
