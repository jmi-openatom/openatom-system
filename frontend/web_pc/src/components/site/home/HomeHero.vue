<template>
  <section id="overview" class="hero">
    <div aria-hidden="true" class="hero__mesh"></div>
    <div class="container hero__inner">
      <div class="hero__content">
        <MorphingText
          class="home-hero__morph"
          :cool-down-time="coolDownTime"
          :morph-time="morphTime"
          :texts="texts"
          style="width: 50%"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { MorphingText } from '@/components/ui/morphing-text'

defineProps<{
  coolDownTime?: number
  morphTime?: number
  texts: string[]
}>()
</script>
