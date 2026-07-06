<template>
  <div ref="rootRef" class="onboarding">
    <OnboardingScene v-if="sceneVisible" :scroll-el="scrollRoot" class="onboarding__scene" />
    <div aria-hidden="true" class="onboarding__scrim"></div>

    <div class="onboarding__progress" aria-hidden="true">
      <div class="progress-pill">
        <div class="progress-line-bg"></div>
        <div class="progress-line-fill" :style="{ transform: `scaleX(${progress})` }"></div>
        <div class="progress-dots">
          <span
            v-for="(scene, index) in scenes"
            :key="scene.id"
            :class="{ 'is-active': index <= activeIndex, 'is-current': index === activeIndex }"
            class="progress-dot"
          ></span>
        </div>
      </div>
      <span class="progress-step-text">{{ String(activeIndex + 1).padStart(2, '0') }} / {{ String(scenes.length).padStart(2, '0') }}</span>
    </div>

    <div ref="scrollRoot" class="onboarding__scroll" @scroll.passive="onScroll">
      <section
        v-for="(scene, index) in scenes"
        :id="`onboarding-${scene.id}`"
        :key="scene.id"
        :ref="(el) => setSectionRef(el as HTMLElement, index)"
        class="onboarding__section"
      >
        <component :is="scene.component" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, provide, ref, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import { authApi } from '@/api'
import { setSession, getToken } from '@/utils/auth.ts'
import OnboardingScene from '@/components/site/onboarding/OnboardingScene.vue'
import SceneAct1 from '@/components/site/onboarding/SceneAct1.vue'
import SceneAct2 from '@/components/site/onboarding/SceneAct2.vue'
import SceneAct3 from '@/components/site/onboarding/SceneAct3.vue'
import SceneAct4 from '@/components/site/onboarding/SceneAct4.vue'
import SceneAct5 from '@/components/site/onboarding/SceneAct5.vue'

const route = useRoute()

const rootRef = ref<HTMLElement>()
const scrollRoot = ref<HTMLElement>()
const sectionRefs = shallowRef<HTMLElement[]>([])
const sceneVisible = ref(true)
const killed = ref(false)

function setSectionRef(el: HTMLElement | null, index: number) {
  if (!el) return
  const next = sectionRefs.value.slice()
  next[index] = el
  sectionRefs.value = next
}

const scenes = [
  { id: 'act1', component: markRaw(SceneAct1) },
  { id: 'act2', component: markRaw(SceneAct2) },
  { id: 'act3', component: markRaw(SceneAct3) },
  { id: 'act4', component: markRaw(SceneAct4) },
  { id: 'act5', component: markRaw(SceneAct5) },
]

const activeIndex = ref(0)
const progress = ref(0)
const fadeTargets = ref<HTMLElement[]>([])

const redirectTarget = computed(() => {
  const raw = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect
  return typeof raw === 'string' && raw.startsWith('/') ? raw : '/'
})

function isUserRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

async function finish() {
  sceneVisible.value = false
  await nextTick()
  try {
    const updated = await authApi.completeOnboarding()
    let nextUser = isUserRecord(updated) ? updated : null
    if (!nextUser) {
      const me = await authApi.me().catch(() => null)
      nextUser = isUserRecord(me) ? me : null
    }
    if (nextUser) {
      setSession({ accessToken: getToken() || undefined, user: nextUser })
    }
  } catch (error) {
    sceneVisible.value = true
    ElMessage.error((error as { message?: string })?.message || '激活失败，请重试')
    return
  }
  window.location.assign(redirectTarget.value)
}

provide('onboarding', { finish })

function onScroll() {
  const root = scrollRoot.value
  if (!root) return
  const max = Math.max(1, root.scrollHeight - root.clientHeight)
  progress.value = Math.min(1, root.scrollTop / max)

  const mid = root.scrollTop + root.clientHeight / 2
  let nearest = 0
  let nearestDelta = Number.POSITIVE_INFINITY
  sectionRefs.value.forEach((el, index) => {
    if (!el) return
    const delta = Math.abs(el.offsetTop + el.offsetHeight / 2 - mid)
    if (delta < nearestDelta) {
      nearestDelta = delta
      nearest = index
    }
  })
  activeIndex.value = nearest

  updateFades(root)
}

function updateFades(root: HTMLElement) {
  const viewH = root.clientHeight
  const scrollTop = root.scrollTop
  fadeTargets.value.forEach((el) => {
    if (el.dataset.noFade !== undefined) return
    const rect = el.getBoundingClientRect()
    const rootRect = root.getBoundingClientRect()
    const elTop = rect.top - rootRect.top
    const elBottom = rect.bottom - rootRect.top
    let opacity = 1
    if (elBottom < viewH * 0.15) {
      opacity = Math.max(0.08, elBottom / (viewH * 0.15))
    } else if (elTop > viewH * 0.85) {
      opacity = Math.max(0.08, (viewH - elTop) / (viewH * 0.15))
    }
    el.style.opacity = String(opacity)
  })
}

let resizeObs: ResizeObserver | undefined
let scrollRaf = 0

function rafUpdate() {
  if (killed.value) return
  scrollRaf = 0
  const root = scrollRoot.value
  if (root) updateFades(root)
}

onMounted(async () => {
  await nextTick()
  const root = scrollRoot.value
  if (!root) return
  fadeTargets.value = Array.from(root.querySelectorAll<HTMLElement>('.scene__body > *'))
  onScroll()
  // 监听容器尺寸变化（字体加载完等）
  resizeObs = new ResizeObserver(() => {
    if (scrollRaf) return
    scrollRaf = requestAnimationFrame(rafUpdate)
  })
  resizeObs.observe(root)
})

onBeforeUnmount(() => {
  killed.value = true
  sceneVisible.value = false
  if (scrollRaf) cancelAnimationFrame(scrollRaf)
  resizeObs?.disconnect()
})
</script>

<style scoped>
.onboarding {
  --ob-bg: #05050a;
  --ob-fg: #f5f5f7;
  --ob-fg-soft: rgba(245, 245, 247, 0.92);
  --ob-fg-muted: rgba(245, 245, 247, 0.62);
  --ob-fg-faint: rgba(245, 245, 247, 0.5);
  --ob-scrim: rgba(5, 5, 10, 0.55);
  --ob-scrim-edge: rgba(5, 5, 10, 0.7);
  --ob-progress-track: rgba(255, 255, 255, 0.14);
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: var(--ob-bg);
  color: var(--ob-fg);
  overflow: hidden;
  isolation: isolate;
}

html:not(.dark) .onboarding {
  --ob-bg: #f5f5f7;
  --ob-fg: #1d1d1f;
  --ob-fg-soft: rgba(29, 29, 31, 0.88);
  --ob-fg-muted: rgba(29, 29, 31, 0.56);
  --ob-fg-faint: rgba(29, 29, 31, 0.42);
  --ob-scrim: rgba(245, 245, 247, 0.5);
  --ob-scrim-edge: rgba(245, 245, 247, 0.7);
  --ob-progress-track: rgba(29, 29, 31, 0.12);
}

.onboarding__scene {
  z-index: 0;
}

.onboarding__scrim {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    radial-gradient(ellipse 75% 65% at 50% 45%, var(--ob-scrim) 0%, color-mix(in srgb, var(--ob-scrim) 45%, transparent) 55%, transparent 100%),
    linear-gradient(180deg, var(--ob-scrim-edge) 0%, transparent 14%, transparent 86%, var(--ob-scrim-edge) 100%);
}

.onboarding__scroll {
  position: relative;
  z-index: 2;
  flex: 1;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.onboarding__scroll::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.onboarding__section {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(80px, 12vh, 140px) clamp(20px, 6vw, 96px);
}

/* 顶部进度胶囊指示器 */
.onboarding__progress {
  position: fixed;
  top: clamp(18px, 4.5vh, 40px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 30;
  padding: 8px 16px;
  border-radius: 999px;
  background: rgba(10, 10, 15, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  gap: 14px;
  pointer-events: none;
}

html:not(.dark) .onboarding__progress {
  background: rgba(245, 245, 247, 0.65);
  border: 1px solid rgba(29, 29, 31, 0.06);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.progress-pill {
  position: relative;
  display: flex;
  align-items: center;
  width: clamp(140px, 18vw, 240px);
  height: 12px;
}

.progress-line-bg {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--ob-progress-track);
  z-index: 0;
  border-radius: 999px;
}

.progress-line-fill {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: #0066cc;
  box-shadow: 0 0 8px rgba(0, 102, 204, 0.6);
  transform: scaleX(0);
  transform-origin: left center;
  transition: transform 0.16s ease-out;
  z-index: 1;
  border-radius: 999px;
}

.progress-dots {
  position: relative;
  z-index: 2;
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ob-bg);
  border: 2px solid var(--ob-fg-faint);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-dot.is-active {
  border-color: #0066cc;
  background: #0066cc;
  box-shadow: 0 0 8px rgba(0, 102, 204, 0.8);
}

.progress-dot.is-current {
  border-color: #0066cc;
  background: var(--ob-fg);
  transform: scale(1.3);
  box-shadow: 0 0 10px #0066cc;
}

.progress-step-text {
  font-size: 11px;
  font-family: monospace;
  color: var(--ob-fg-muted);
  letter-spacing: 0.5px;
  font-weight: 600;
  user-select: none;
}
</style>
