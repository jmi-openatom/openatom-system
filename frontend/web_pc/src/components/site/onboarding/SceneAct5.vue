<template>
  <SceneShell align="right" chapter="V" eyebrow="启程" tone="dawn">
    <img class="depart-logo" src="/logo.png" alt="开放原子开源社团" data-no-fade />
    <h2>把这一刻，<br />当成你在社团的第一个 <em>commit</em>。</h2>
    <p>别怕一开始只是改一个错别字、提一个问题、跑通一次本地启动——<br />所有真正的贡献，都是从这样一小步开始的。</p>
    <button class="depart-cta" data-no-fade :disabled="activating" type="button" @click="activate">
      <span>{{ activating ? '正在激活账号…' : '激活账号并开始探索' }}</span>
      <el-icon v-if="!activating"><Right /></el-icon>
    </button>
    <p class="depart-sig">开放原子开源社团 · 欢迎你，新成员</p>
  </SceneShell>
</template>

<script setup lang="ts">
import { Right } from '@element-plus/icons-vue'
import { inject, ref } from 'vue'
import SceneShell from './SceneShell.vue'

const control = inject<{
  finish: () => Promise<void> | void
}>('onboarding')

const activating = ref(false)

async function activate() {
  if (activating.value) return
  activating.value = true
  try {
    await control?.finish()
  } finally {
    activating.value = false
  }
}
</script>

<style scoped>
.depart-logo {
  width: clamp(72px, 7vw, 104px);
  height: auto;
  border-radius: 20px;
  margin-bottom: 4px;
  filter: drop-shadow(0 0 28px rgba(255, 255, 255, 0.28));
  animation: logo-breathe 4s ease-in-out infinite;
}

html:not(.dark) .depart-logo {
  filter: drop-shadow(0 0 24px rgba(29, 29, 31, 0.18));
}

@keyframes logo-breathe {
  0%, 100% {
    filter: drop-shadow(0 0 20px rgba(255, 255, 255, 0.18));
    transform: scale(1);
  }
  50% {
    filter: drop-shadow(0 0 40px rgba(255, 255, 255, 0.38));
    transform: scale(1.04);
  }
}

html:not(.dark) .depart-logo {
  animation-name: logo-breathe-light;
}

@keyframes logo-breathe-light {
  0%, 100% {
    filter: drop-shadow(0 0 16px rgba(29, 29, 31, 0.12));
    transform: scale(1);
  }
  50% {
    filter: drop-shadow(0 0 32px rgba(29, 29, 31, 0.24));
    transform: scale(1.04);
  }
}

.depart-cta {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 17px 34px;
  border: none;
  border-radius: 999px;
  background: #f5f5f7;
  color: #05050a;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, background-color 0.22s ease, opacity 0.22s ease;
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.45);
}

html:not(.dark) .depart-cta {
  background: #1d1d1f;
  color: #f5f5f7;
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.18);
}

.depart-cta:hover:not(:disabled) {
  transform: translateY(-2px);
  background: #ffffff;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.55);
}

html:not(.dark) .depart-cta:hover:not(:disabled) {
  background: #000000;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.24);
}

.depart-cta:disabled {
  opacity: 0.7;
  cursor: progress;
}

.depart-sig {
  margin: 0 !important;
  font-size: 12px !important;
  letter-spacing: 0.24em;
  color: rgba(245, 245, 247, 0.5) !important;
  text-transform: uppercase;
}

html:not(.dark) .depart-sig {
  color: rgba(29, 29, 31, 0.42) !important;
}
</style>
