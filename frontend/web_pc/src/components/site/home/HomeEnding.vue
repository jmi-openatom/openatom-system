<template>
  <section
    id="join"
    aria-labelledby="join-title"
    class="story-ending story-section home-interactive-section"
  >
    <HomeInteractiveBackdrop :radius="230" :spacing="68" :strength="22" />
    <div class="story-container">
      <div class="story-ending__top">
        <span>一个开放的结尾，也是一段新的开始。</span>
      </div>
      <h2 id="join-title" data-story-line>开放，<br /><span>就差一个你。</span></h2>
      <div class="story-ending__bottom">
        <p>不必等到准备好一切。<br />带着好奇心，来做一点真正的改变。</p>
        <RouterLink class="story-button" to="/apply"
          >加入我们，一起创造
          <ArrowUpRight :size="24" aria-hidden="true" />
        </RouterLink>
      </div>
      <div aria-hidden="true" class="story-ending__signature"></div>
    </div>
  </section>
</template>
<script lang="ts" setup>
import { ArrowUpRight } from 'lucide-vue-next'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'
</script>
