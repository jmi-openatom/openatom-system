<template>
  <div ref="rootRef" class="onboarding">
    <OnboardingScene v-if="sceneVisible" :scroll-el="scrollRoot" class="onboarding__scene" />
    <div aria-hidden="true" class="onboarding__scrim"></div>

    <div class="onboarding__progress" aria-hidden="true">
      <span :style="{ transform: `scaleX(${progress})` }"></span>
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
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '@/api'
import { setSession, getToken } from '@/utils/auth.ts'
import OnboardingScene from '@/components/site/onboarding/OnboardingScene.vue'
import SceneAct1 from '@/components/site/onboarding/SceneAct1.vue'
import SceneAct2 from '@/components/site/onboarding/SceneAct2.vue'
import SceneAct3 from '@/components/site/onboarding/SceneAct3.vue'
import SceneAct4 from '@/components/site/onboarding/SceneAct4.vue'
import SceneAct5 from '@/components/site/onboarding/SceneAct5.vue'

const router = useRouter()
const route = useRoute()

const rootRef = ref<HTMLElement>()
const scrollRoot = ref<HTMLElement>()
const sectionRefs = shallowRef<HTMLElement[]>([])

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
const sceneVisible = ref(true)

let animationContext: { revert: () => void } | undefined
let isUnmounted = false

const redirectTarget = computed(() => {
  const raw = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect
  return typeof raw === 'string' && raw.startsWith('/') ? raw : '/'
})

async function finish() {
  // 先销毁 3D 场景释放 GPU 资源，再发请求
  sceneVisible.value = false
  try {
    const updated = await authApi.completeOnboarding()
    if (updated) {
      setSession({ accessToken: getToken() || undefined, user: updated })
    }
  } catch (error) {
    sceneVisible.value = true
    ElMessage.error((error as { message?: string })?.message || '激活失败，请重试')
    throw error
  }
  router.replace(redirectTarget.value)
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
}

async function createAnimations() {
  const root = rootRef.value
  const scrollEl = scrollRoot.value
  if (!root || !scrollEl) return
  const [{ default: gsap }, { ScrollTrigger }] = await Promise.all([
    import('gsap'),
    import('gsap/ScrollTrigger'),
  ])
  gsap.registerPlugin(ScrollTrigger)
  if (isUnmounted) return

  animationContext = gsap.context(() => {
    ScrollTrigger.defaults({ scroller: scrollEl })

    // 章节大数字轻微视差位移
    gsap.utils.toArray<HTMLElement>('[data-parallax]').forEach((el) => {
      const speed = Number(el.dataset.parallax || '0')
      if (!speed) return
      const section = el.closest('.onboarding__section') || el
      gsap.fromTo(
        el,
        { yPercent: -speed * 100 },
        {
          yPercent: speed * 100,
          ease: 'none',
          scrollTrigger: {
            trigger: section,
            start: 'top bottom',
            end: 'bottom top',
            scrub: 1,
          },
        },
      )
    })

    // 正文：整个 .scene__body 作为一个整体做透明度淡入淡出
    // 用单一 ScrollTrigger + tween 配 keyframes 覆盖"进→中→出"全周期
    // 避免给每个子元素创建 2 个 ScrollTrigger 导致实例爆炸
    gsap.utils.toArray<HTMLElement>('.scene__body').forEach((body) => {
      const children = Array.from(body.children).filter(
        (child) => (child as HTMLElement).dataset.noFade === undefined,
      ) as HTMLElement[]
      if (!children.length) return

      // 按滚动进度分三段：淡入 → 完全清晰 → 淡出
      gsap.fromTo(
        children,
        { opacity: 0.1 },
        {
          opacity: 1,
          ease: 'none',
          stagger: 0.08,
          scrollTrigger: {
            trigger: body,
            scroller: scrollEl,
            start: 'top 85%',
            end: 'top 45%',
            scrub: 1,
          },
        },
      )
      gsap.to(children, {
        opacity: 0.1,
        ease: 'none',
        stagger: 0.06,
        scrollTrigger: {
          trigger: body,
          scroller: scrollEl,
          start: 'bottom 55%',
          end: 'bottom 15%',
          scrub: 1,
        },
      })
    })

    ScrollTrigger.refresh()
  }, root)
}

onMounted(async () => {
  await nextTick()
  onScroll()
  void createAnimations()
})

onBeforeUnmount(() => {
  isUnmounted = true
  sceneVisible.value = false
  animationContext?.revert()
  animationContext = undefined
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

/* 浅色版本 */
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

/* 文字可读性遮罩：暗角 + 上下渐隐 */
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

/* 顶部进度线 */
.onboarding__progress {
  position: fixed;
  top: clamp(22px, 7vh, 70px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 30;
  width: clamp(120px, 18vw, 240px);
  height: 2px;
  background: var(--ob-progress-track);
  border-radius: 999px;
  overflow: hidden;
}

.onboarding__progress span {
  display: block;
  height: 100%;
  width: 100%;
  background: var(--ob-fg);
  transform: scaleX(0);
  transform-origin: left center;
  transition: transform 0.12s linear;
}
</style>
