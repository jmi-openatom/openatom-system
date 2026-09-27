<template>
  <aside class="exploration-flag" :aria-label="`OpenAtom Quest 探索标记 ${index}/3`">
    <div class="exploration-flag__heading">
      <span class="exploration-flag__mark" aria-hidden="true">✦</span>
      <span>OPENATOM QUEST · {{ index }} / 03</span>
    </div>
    <p>读到这里，你找到了第 {{ index }} 枚探索标记。</p>
    <template v-if="flag">
      <code>{{ flag }}</code>
      <button type="button" class="exploration-flag__action" @click="copyFlag">
        {{ copied ? '已复制' : '复制我的标记' }}
      </button>
      <small>这枚标记属于你的 Quest 账号，完成探索后回到 Quest 提交。</small>
    </template>
    <template v-else-if="loading"><small role="status">正在读取你的个人标记…</small></template>
    <template v-else-if="loginRequired">
      <small>登录 Quest 后，才能看到属于你的标记。</small>
      <a class="exploration-flag__action" :href="`${questUrl}/login`" target="_blank" rel="noopener noreferrer">登录 Quest ↗</a>
      <button type="button" class="exploration-flag__action exploration-flag__action--secondary" @click="loadFlag">已登录，重新加载</button>
    </template>
    <template v-else>
      <small role="alert">标记暂时无法加载，请检查 Quest 登录状态后重试。</small>
      <button type="button" class="exploration-flag__action" @click="loadFlag">重新加载</button>
    </template>
  </aside>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

const props = defineProps<{ index: number; pageKey: 'about' | 'regulations' | 'activities' }>()
const flag = ref('')
const loading = ref(true)
const loginRequired = ref(false)
const copied = ref(false)
const apiBase = String(import.meta.env.VITE_QUEST_API_URL || 'https://quest.jmi-openatom.cn/api').replace(/\/$/, '')
const questUrl = String(import.meta.env.VITE_QUEST_SITE_URL || 'https://quest.jmi-openatom.cn').replace(/\/$/, '')

async function loadFlag() {
  loading.value = true
  loginRequired.value = false
  copied.value = false
  try {
    const response = await fetch(`${apiBase}/site-exploration/flags/${props.pageKey}`, {
      credentials: 'include',
      headers: { Accept: 'application/json' },
    })
    if (response.status === 401 || response.status === 403) {
      loginRequired.value = true
      return
    }
    if (!response.ok) throw new Error('标记加载失败')
    const body = (await response.json()) as { data?: { flag?: string } }
    if (!body.data?.flag) throw new Error('标记为空')
    flag.value = body.data.flag
  } catch {
    flag.value = ''
  } finally {
    loading.value = false
  }
}

async function copyFlag() {
  try {
    await navigator.clipboard.writeText(flag.value)
    copied.value = true
  } catch {
    copied.value = false
  }
}

onMounted(loadFlag)
</script>

<style scoped>
.exploration-flag {
  width: min(100%, 480px);
  margin: clamp(28px, 4vw, 48px) 0 0 auto;
  padding: clamp(20px, 3vw, 28px);
  border: 1px solid rgba(83, 105, 145, 0.2);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 12px 36px rgba(24, 42, 70, 0.06);
  color: #26354d;
}

.exploration-flag__heading {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #5c6c88;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.exploration-flag__mark {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: #e7eefb;
  color: #345caa;
  font-size: 17px;
}

.exploration-flag p {
  margin: 16px 0 8px;
  font-size: 14px;
  line-height: 1.6;
}

.exploration-flag code {
  display: block;
  overflow-wrap: anywhere;
  color: #183d87;
  font-size: clamp(16px, 3.5vw, 21px);
  font-weight: 700;
  letter-spacing: 0.03em;
  user-select: all;
}

.exploration-flag__action {
  display: inline-block;
  margin-top: 12px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #2255af;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
}

.exploration-flag__action:focus-visible {
  outline: 2px solid #2255af;
  outline-offset: 4px;
}

.exploration-flag__action--secondary {
  margin-left: 16px;
}

.exploration-flag small {
  display: block;
  margin-top: 12px;
  color: #63718a;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 600px) {
  .exploration-flag {
    margin-right: auto;
    border-radius: 14px;
  }
}
</style>
