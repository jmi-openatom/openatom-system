<template>
  <main
    :class="['campus-page', { 'campus-page--detail': selectedBuilding }]"
    aria-label="江苏海事职业技术学院三维校园地图"
  >
    <HomeMapSection
      ref="campusMap"
      class="campus-page__map"
      interactive
      static
      :show-labels="showLabels"
      @select-building="onSelectBuilding"
    />
    <aside class="campus-panel">
      <div class="campus-panel__top">
        <span class="campus-panel__eyebrow">JMI · CAMPUS</span
        ><router-link to="/" class="campus-back" aria-label="返回首页"
          ><ArrowLeft :size="16" aria-hidden="true"
        /></router-link>
      </div>
      <h1>漫游校园</h1>
      <p class="campus-panel__school">江苏海事职业技术学院</p>
      <label class="campus-search">
        <Search :size="16" aria-hidden="true" />
        <input v-model="query" type="search" aria-label="搜索校园建筑" placeholder="找一栋楼…" />
      </label>
      <ul v-if="query.trim()" class="campus-results" aria-label="建筑搜索结果">
        <li v-for="building in searchResults" :key="building.id">
          <button @click="chooseBuilding(building.id)">
            {{ building.name }}<ArrowUpRight :size="14" aria-hidden="true" />
          </button>
        </li>
        <li v-if="!searchResults.length" class="campus-results__empty">没有找到这栋楼</li>
      </ul>
      <label v-else class="campus-select">
        <span class="sr-only">选择校园建筑</span>
        <select v-model="selectedId" @change="chooseBuilding(selectedId)">
          <option value="">全部 {{ CAMPUS_BUILDINGS.length }} 栋建筑</option>
          <option
            v-for="building in CAMPUS_BUILDINGS"
            :key="building.id"
            :value="String(building.id)"
          >
            {{ building.name }}
          </option>
        </select>
      </label>
      <p v-if="selectedBuilding" class="campus-selection" role="status">
        <MapPin :size="14" aria-hidden="true" />{{ selectedBuilding.name }}
      </p>
      <div class="campus-actions">
        <button :aria-pressed="showLabels" @click="showLabels = !showLabels">
          <Tags :size="16" aria-hidden="true" />楼名
        </button>
        <button :aria-pressed="isDark" @click="toggleTheme">
          <component :is="isDark ? Moon : Sun" :size="16" aria-hidden="true" />{{
            isDark ? '夜景' : '日景'
          }}
        </button>
        <button @click="reset"><Scan :size="16" aria-hidden="true" />全景</button>
      </div>
    </aside>
    <Transition name="building-card">
      <CampusBuildingCard
        v-if="selectedBuilding"
        :key="selectedBuilding.id"
        :building="selectedBuilding"
        @close="closeBuilding"
      />
    </Transition>
    <div class="campus-orbit" role="group" aria-label="旋转地图">
      <button aria-label="向左旋转地图" @click="campusMap?.rotateView(-30)">
        <RotateCcw :size="18" aria-hidden="true" />
      </button>
      <button aria-label="向右旋转地图" @click="campusMap?.rotateView(30)">
        <RotateCw :size="18" aria-hidden="true" />
      </button>
    </div>
    <div class="campus-caption">
      <p class="campus-caption__gesture">
        按住空格 + 拖动平移 · 滚轮缩放 · 右键旋转<span>触屏可双指缩放、旋转</span>
      </p>
      <p>点击楼宇查看介绍与实拍图 · 部分建筑及小山按参考图示意</p>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArrowUpRight,
  ArrowLeft,
  MapPin,
  Moon,
  RotateCcw,
  RotateCw,
  Scan,
  Search,
  Sun,
  Tags,
} from 'lucide-vue-next'
import HomeMapSection from '@/components/site/home/HomeMapSection.vue'
import CampusBuildingCard from '@/components/site/home/CampusBuildingCard.vue'
import { CAMPUS_BUILDINGS } from '@/components/site/home/campusLabels'
import { useTheme } from '@/composables/useTheme'

const campusMap = ref<InstanceType<typeof HomeMapSection>>()
const route = useRoute()
const showLabels = ref(true)
const selectedId = ref('')
const query = ref('')
const { isDark, toggleTheme } = useTheme()
const searchResults = computed(() =>
  CAMPUS_BUILDINGS.filter((building) => building.name.includes(query.value.trim())),
)
const selectedBuilding = computed(() =>
  CAMPUS_BUILDINGS.find((building) => String(building.id) === selectedId.value),
)
function onSelectBuilding(id: string | number | null) {
  selectedId.value = id === null ? '' : String(id)
}
function chooseBuilding(id: string | number) {
  if (id === '') {
    reset()
    return
  }
  if (!CAMPUS_BUILDINGS.some((building) => String(building.id) === String(id))) return
  selectedId.value = String(id)
  campusMap.value?.focusBuilding(id)
  query.value = ''
}
function reset() {
  selectedId.value = ''
  query.value = ''
  campusMap.value?.resetView()
}
function closeBuilding() {
  selectedId.value = ''
  campusMap.value?.clearSelection()
}
onMounted(() => {
  const id = route.query.building
  if (typeof id === 'string') chooseBuilding(id)
})
</script>

<style scoped>
.campus-page {
  position: fixed;
  inset: 0;
  height: 100svh;
  min-height: 0;
  overflow: hidden;
  background: var(--oa-page-bg, #f4f4f5);
}
.campus-page__map {
  height: 100%;
  min-height: 0;
}
.building-card-enter-active,
.building-card-leave-active {
  transition:
    opacity 180ms ease,
    transform 180ms ease;
}
.building-card-enter-from,
.building-card-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
@media (prefers-reduced-motion: reduce) {
  .building-card-enter-active,
  .building-card-leave-active {
    transition: none;
  }
}
.campus-panel,
.campus-orbit {
  border: 1px solid var(--oa-border, #e4e5e0);
  background: var(--oa-surface, #fff);
  box-shadow: 0 6px 24px #263c3910;
}
.campus-panel {
  position: absolute;
  z-index: 6;
  top: 24px;
  left: 24px;
  width: 272px;
  padding: 22px;
  border-radius: 20px;
}
.campus-panel__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.campus-back {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  color: var(--oa-muted);
  border-radius: 8px;
  text-decoration: none;
}
.campus-back:hover {
  background: var(--oa-page-soft-bg);
}
.campus-back:focus-visible {
  outline: 2px solid #679ba7;
  outline-offset: 2px;
}
.campus-panel__eyebrow {
  color: var(--oa-muted, #707774);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.16em;
}
.campus-panel h1 {
  margin: 6px 0;
  color: var(--oa-text, #243630);
  font-size: 28px;
  line-height: 1.3;
  font-weight: 600;
  letter-spacing: -0.04em;
}
.campus-panel__school {
  margin: 0 0 20px;
  color: var(--oa-muted, #707774);
  font-size: 12px;
}
.campus-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  height: 42px;
  color: var(--oa-muted, #707774);
  border: 1px solid var(--oa-border, #e4e5e0);
  border-radius: 10px;
}
.campus-search:focus-within {
  outline: 2px solid #679ba7;
  outline-offset: 2px;
}
.campus-search input {
  min-width: 0;
  width: 100%;
  background: transparent;
  border: 0;
  outline: 0;
  font: inherit;
  font-size: 13px;
  color: var(--oa-text, #243630);
}
.campus-select {
  display: block;
  margin-top: 10px;
}
.campus-select select {
  width: 100%;
  padding: 8px 6px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--oa-text, #243630);
  font-size: 12px;
  cursor: pointer;
}
.campus-select option {
  background: var(--oa-surface, #fff);
}
.campus-results {
  max-height: 240px;
  overflow-y: auto;
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
}
.campus-results button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 40px;
  background: transparent;
  border: 0;
  text-align: left;
  color: var(--oa-text, #243630);
  cursor: pointer;
  border-radius: 6px;
  padding: 8px;
  font-size: 12px;
}
.campus-results button:hover {
  background: var(--oa-page-soft-bg, #edf1eb);
}
.campus-results__empty {
  font-size: 12px;
  color: var(--oa-muted, #707774);
  padding: 16px 8px;
}
.campus-selection {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 10px 0;
  font-size: 12px;
  color: var(--oa-text, #243630);
}
.campus-actions {
  display: flex;
  gap: 6px;
  padding-top: 12px;
  margin-top: 10px;
  border-top: 1px solid var(--oa-border, #e4e5e0);
}
.campus-actions button {
  flex: 1;
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: var(--oa-text, #243630);
  background: transparent;
  border: 0;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
}
.campus-actions button[aria-pressed='true'],
.campus-actions button:hover {
  background: var(--oa-page-soft-bg, #edf1eb);
}
.campus-orbit {
  position: absolute;
  z-index: 6;
  right: 10px;
  bottom: 140px;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  overflow: hidden;
}
.campus-orbit button {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  background: transparent;
  border: 0;
  color: var(--oa-text, #243630);
  cursor: pointer;
}
.campus-orbit button + button {
  border-top: 1px solid var(--oa-border, #e4e5e0);
}
.campus-orbit button:hover {
  background: var(--oa-page-soft-bg, #edf1eb);
}
.campus-caption {
  position: absolute;
  z-index: 5;
  left: 24px;
  bottom: 28px;
  font-size: 10px;
  line-height: 1.6;
  color: var(--oa-muted, #707774);
  background: var(--oa-surface, #fff);
  padding: 8px 12px;
  border-radius: 10px;
  pointer-events: none;
}
.campus-caption p {
  margin: 0;
}
.campus-caption__gesture {
  color: var(--oa-text, #243630);
  font-size: 11px;
}
.campus-caption__gesture span {
  display: none;
}
button:focus-visible,
select:focus-visible {
  outline: 2px solid #679ba7;
  outline-offset: 2px;
}
@media (max-width: 699px) {
  .campus-panel {
    top: 12px;
    left: 12px;
    width: 228px;
    padding: 16px;
    border-radius: 16px;
  }
  .campus-panel h1 {
    font-size: 23px;
  }
  .campus-panel__school {
    margin-bottom: 12px;
  }
  .campus-actions {
    padding-top: 6px;
    margin-top: 6px;
  }
  .campus-caption {
    left: 12px;
    bottom: 28px;
    max-width: calc(100% - 70px);
  }
  .campus-caption__gesture {
    font-size: 0;
  }
  .campus-caption__gesture span {
    display: block;
    font-size: 11px;
  }
}
@media (max-width: 899px) {
  .campus-page--detail .campus-caption {
    display: none;
  }
}
</style>
