<template>
  <div class="app-route-shell">
    <div ref="transitionPlaneRef" aria-hidden="true" class="route-transition-plane">
      <span class="route-transition-plane__bar route-transition-plane__bar--one"></span>
      <span class="route-transition-plane__bar route-transition-plane__bar--two"></span>
      <span class="route-transition-plane__bar route-transition-plane__bar--three"></span>
      <span class="route-transition-plane__label">JMI OPENATOM</span>
    </div>

    <router-view v-slot="{ Component, route }">
      <transition
        :css="false"
        mode="out-in"
        @enter="handleEnter"
        @leave="handleLeave"
        @before-enter="handleBeforeEnter"
      >
        <component :is="Component" :key="route.matched[0]?.path || route.fullPath" />
      </transition>
    </router-view>
  </div>
</template>

<script lang="ts" setup>
import gsap from 'gsap'
import {onBeforeUnmount, ref} from 'vue'

const transitionPlaneRef = ref<HTMLElement>()

let activeTimeline: gsap.core.Timeline | undefined

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

function transitionElements() {
  const plane = transitionPlaneRef.value
  return {
    plane,
    bars: plane?.querySelectorAll<HTMLElement>('.route-transition-plane__bar') || [],
    label: plane?.querySelector<HTMLElement>('.route-transition-plane__label'),
  }
}

function killActiveTimeline() {
  activeTimeline?.kill()
  activeTimeline = undefined
}

function handleBeforeEnter(element: Element) {
  if (prefersReducedMotion()) {
    gsap.set(element, { autoAlpha: 0 })
    return
  }

  gsap.set(element, {
    autoAlpha: 0,
    y: 22,
    scale: 0.992,
    transformOrigin: '50% 0%',
    willChange: 'transform, opacity',
  })
}

function handleLeave(element: Element, done: () => void) {
  killActiveTimeline()

  if (prefersReducedMotion()) {
    gsap.to(element, {
      autoAlpha: 0,
      duration: 0.12,
      ease: 'power1.out',
      onComplete: done,
    })
    return
  }

  const { plane, bars, label } = transitionElements()

  activeTimeline = gsap.timeline({
    defaults: {
      ease: 'power3.inOut',
      overwrite: 'auto',
    },
    onComplete: done,
  })

  activeTimeline
    .set(plane, {
      autoAlpha: 1,
      pointerEvents: 'auto',
    })
    .set(bars, {
      xPercent: -112,
      skewX: -8,
      transformOrigin: 'left center',
    })
    .set(label, {
      autoAlpha: 0,
      y: 10,
    })
    .to(
      element,
      {
        autoAlpha: 0,
        y: -12,
        scale: 0.992,
        duration: 0.2,
        ease: 'power2.in',
      },
      0,
    )
    .to(
      bars,
      {
        xPercent: 0,
        skewX: 0,
        duration: 0.28,
        ease: 'power3.out',
        stagger: 0.035,
      },
      0.02,
    )
    .to(
      label,
      {
        autoAlpha: 0.82,
        y: 0,
        duration: 0.18,
        ease: 'power2.out',
      },
      0.13,
    )
}

function handleEnter(element: Element, done: () => void) {
  killActiveTimeline()

  if (prefersReducedMotion()) {
    gsap.to(element, {
      autoAlpha: 1,
      duration: 0.16,
      ease: 'power1.out',
      clearProps: 'opacity,visibility',
      onComplete: done,
    })
    return
  }

  const { plane, bars, label } = transitionElements()

  activeTimeline = gsap.timeline({
    defaults: {
      ease: 'power3.out',
      overwrite: 'auto',
    },
    onComplete: () => {
      gsap.set(element, {
        clearProps: 'transform,opacity,visibility,willChange',
      })
      gsap.set(plane, {
        autoAlpha: 0,
        pointerEvents: 'none',
      })
      gsap.set(bars, {
        clearProps: 'transform',
      })
      done()
    },
  })

  activeTimeline
    .to(
      element,
      {
        autoAlpha: 1,
        y: 0,
        scale: 1,
        duration: 0.36,
        ease: 'power3.out',
      },
      0.04,
    )
    .to(
      bars,
      {
        xPercent: 112,
        skewX: 8,
        duration: 0.32,
        ease: 'power3.inOut',
        stagger: {
          each: 0.025,
          from: 'end',
        },
      },
      0,
    )
    .to(
      label,
      {
        autoAlpha: 0,
        y: -8,
        duration: 0.2,
        ease: 'power2.in',
      },
      0,
    )
}

onBeforeUnmount(() => {
  killActiveTimeline()
})
</script>

<style scoped>
.app-route-shell {
  min-height: 100vh;
  background: var(--oa-page-bg);
}

.route-transition-plane {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: grid;
  grid-template-rows: repeat(3, 1fr);
  overflow: hidden;
  visibility: hidden;
  opacity: 0;
  pointer-events: none;
  isolation: isolate;
}

.route-transition-plane::before {
  position: absolute;
  inset: 0;
  z-index: 2;
  content: '';
  border-inline: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
  pointer-events: none;
}

.route-transition-plane__bar {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  transform: translateX(-112%);
  will-change: transform;
}

.route-transition-plane__bar--one {
  background: #8acaff;
}

.route-transition-plane__bar--two {
  background: #1d4ed8;
}

.route-transition-plane__bar--three {
  background: #8acaff;
}

.route-transition-plane__label {
  position: absolute;
  right: clamp(24px, 6vw, 72px);
  bottom: clamp(24px, 7vw, 82px);
  z-index: 3;
  color: rgba(255, 255, 255, 0.86);
  font-size: clamp(22px, 4vw, 58px);
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.12em;
  text-align: right;
  text-transform: uppercase;
  text-shadow: 0 16px 44px rgba(2, 6, 23, 0.45);
  transform: translateY(10px);
  opacity: 0;
  pointer-events: none;
  will-change: transform, opacity;
}

html.dark .route-transition-plane__bar--one {
  background: #030712;
}

html.dark .route-transition-plane__bar--two {
  background: #3730a3;
}

html.dark .route-transition-plane__bar--three {
  background: #0f766e;
}

@media (prefers-reduced-motion: reduce) {
  .route-transition-plane {
    display: none;
  }
}
</style>
