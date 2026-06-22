<template>
  <div aria-live="polite" aria-atomic="true" class="app-status">
    <div v-if="busy" class="app-status__progress" role="progressbar">
      <span />
    </div>
    <transition name="app-status-notice">
      <div
        v-if="showNotice"
        :class="{ 'app-status__notice--offline': !state.online }"
        class="app-status__notice"
        role="status"
      >
        <span class="app-status__dot" />
        <span>{{ noticeMessage }}</span>
        <button
          v-if="state.navigationFailed || state.navigationSlow"
          class="app-status__retry"
          type="button"
          @click="retry"
        >
          重试
        </button>
      </div>
    </transition>
  </div>
</template>

<script lang="ts" setup>
import { useRouter } from "vue-router";
import { startNavigation, useAppStatus } from "@/composables/useAppStatus";

const router = useRouter();
const { state, busy, showNotice, noticeMessage } = useAppStatus();

async function retry() {
  if (!state.online) return;
  const target = state.navigationTarget || router.currentRoute.value.fullPath;
  if (state.navigationSlow) {
    window.location.assign(target);
    return;
  }
  const wasCurrentRoute = router.currentRoute.value.fullPath === target;
  startNavigation(target);
  try {
    await router.replace(target);
    if (wasCurrentRoute) {
      window.location.reload();
    }
  } catch {
    window.location.assign(target);
  }
}
</script>

<style scoped>
.app-status {
  position: fixed;
  inset: 0 0 auto;
  z-index: 1900;
  pointer-events: none;
}

.app-status__progress {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  overflow: hidden;
  background: color-mix(in srgb, var(--oa-primary) 12%, transparent);
}

.app-status__progress span {
  display: block;
  width: 42%;
  height: 100%;
  background: var(--oa-primary);
  border-radius: 999px;
  animation: app-status-progress 1.1s ease-in-out infinite;
}

.app-status__notice {
  display: flex;
  position: absolute;
  top: max(10px, env(safe-area-inset-top));
  left: 50%;
  min-height: 36px;
  align-items: center;
  gap: 8px;
  max-width: min(92vw, 560px);
  padding: 8px 12px;
  color: var(--oa-text);
  background: color-mix(in srgb, var(--oa-elevated-bg) 94%, transparent);
  border: 1px solid var(--oa-border);
  border-radius: 999px;
  box-shadow: 0 10px 34px rgba(0, 0, 0, 0.16);
  font-size: 13px;
  line-height: 1.35;
  transform: translateX(-50%);
  backdrop-filter: blur(16px);
  pointer-events: auto;
}

.app-status__dot {
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  background: var(--el-color-warning);
  border-radius: 50%;
}

.app-status__notice--offline .app-status__dot {
  background: var(--el-color-danger);
}

.app-status__retry {
  padding: 2px 8px;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  border: 0;
  border-radius: 999px;
  font: inherit;
  cursor: pointer;
}

.app-status-notice-enter-active,
.app-status-notice-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.app-status-notice-enter-from,
.app-status-notice-leave-to {
  opacity: 0;
  transform: translate(-50%, -8px);
}

@keyframes app-status-progress {
  0% {
    transform: translateX(-110%);
  }
  55% {
    transform: translateX(110%);
  }
  100% {
    transform: translateX(250%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-status__progress span {
    width: 100%;
    animation: none;
  }
}
</style>
