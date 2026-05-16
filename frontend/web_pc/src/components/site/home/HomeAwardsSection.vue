<template>
  <section id="achievements" class="container section achievements-section home-interactive-section">
    <HomeInteractiveBackdrop :radius="220" :spacing="64" :strength="20" />
    <div class="section-heading reveal-block">
      <span>成果展示</span>
      <h2>比赛获奖展示</h2>
      <p>{{ awards.length ? `当前展示 ${awards.length} 项获奖记录` : '暂无获奖记录' }}</p>
    </div>

    <div v-if="awards.length" class="award-exhibit" @pointerleave="resetFocus">
      <div aria-hidden="true" class="award-exhibit__ghost-year">{{ activeAward?.year }}</div>
      <div aria-hidden="true" class="award-exhibit__rail"></div>
      <div aria-hidden="true" class="award-exhibit__trace" :style="traceStyle"></div>

      <button
        v-for="(award, index) in awards"
        :key="award.id || `${award.year}-${award.title}`"
        :class="{ 'is-active': index === activeIndex }"
        class="award-exhibit__item reveal-card"
        type="button"
        @focus="setActive(index)"
        @pointerenter="setActive(index)"
      >
        <span class="award-exhibit__year">{{ award.year }}</span>
        <span class="award-exhibit__dot">
          <i></i>
        </span>
        <span class="award-exhibit__title">{{ award.title }}</span>
      </button>

      <Transition name="award-copy" mode="out-in">
        <div :key="activeKey" class="award-exhibit__spotlight">
        <span>{{ activeAward?.year }}</span>
        <h3>{{ activeAward?.title }}</h3>
        <p>{{ activeAward?.competitionName }}</p>
        <div>
          <strong>{{ activeAward?.awardLevel }}</strong>
          <small>{{ activeAward?.teamName }}</small>
        </div>
        </div>
      </Transition>
    </div>

    <el-empty v-if="!awards.length && !loading" description="暂无比赛获奖数据" />
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'

const props = defineProps<{
  awards: any[]
  loading: boolean
}>()

const activeIndex = ref(0)
const activeAward = computed(() => props.awards[activeIndex.value])
const activeKey = computed(() => activeAward.value?.id || `${activeAward.value?.year}-${activeAward.value?.title}`)
const traceStyle = computed(() => ({
  '--award-progress': props.awards.length > 1 ? activeIndex.value / (props.awards.length - 1) : 0,
}))

function setActive(index: number) {
  activeIndex.value = index
}

function resetFocus() {
  activeIndex.value = 0
}

watch(
  () => props.awards.length,
  () => {
    if (activeIndex.value >= props.awards.length) activeIndex.value = 0
  },
)
</script>
