<template>
  <main id="mail-main" class="login-page activation-page" tabindex="-1">
    <nav aria-label="邮箱站导航" class="public-nav">
      <a class="brand" href="https://www.jmi-openatom.cn">
        <img alt="开放原子开源社团" class="brand-logo" src="/logo.png" />
        <span><strong>开放原子开源社团</strong><small>江苏海事职业技术学院</small></span>
      </a>
      <ThemeToggle />
    </nav>
    <section class="activation-hero">
      <div class="activation-card">
        <span class="eyebrow">OPENATOM MAIL</span>
        <h1>激活你的邮箱地址</h1>
        <p>选择一种方式生成你的邮箱地址。地址确定后一般不可自行修改。</p>

        <form v-if="!activating" class="activation-form" @submit.prevent="onSubmit">
          <div class="activation-options">
            <label class="activation-option">
              <input v-model="mode" type="radio" value="pinyin" />
              <span>
                <strong>延续拼音</strong>
                <small>根据你的姓名自动生成（例如 zhang san → zhangsan@mailer.jmi-openatom.cn）</small>
              </span>
            </label>
            <label class="activation-option">
              <input v-model="mode" type="radio" value="custom" />
              <span>
                <strong>自定义主机名</strong>
                <small>手动输入你想要的邮箱前缀</small>
              </span>
            </label>
          </div>

          <div v-if="mode === 'custom'" class="activation-custom">
            <label for="local-part">邮箱前缀</label>
            <div class="activation-input-row">
              <input
                id="local-part"
                v-model="localPart"
                placeholder="例如 zhang.san"
                autocomplete="off"
                spellcheck="false"
              />
              <span>@mailer.jmi-openatom.cn</span>
            </div>
            <p class="activation-hint">仅允许小写字母、数字和点号，2–48 位。</p>
          </div>

          <div v-if="error" class="form-error" role="alert">
            <CircleAlert :size="16" /> {{ error }}
          </div>

          <button class="primary-button activation-submit" type="submit" :disabled="busy">
            <span v-if="busy" class="spinner small"></span>
            {{ busy ? '正在激活…' : '确定并进入邮箱' }}
          </button>
        </form>
        <div v-else class="activation-success">
          <CircleCheck :size="40" />
          <h2>激活成功！</h2>
          <p>你的邮箱地址是 <strong>{{ activatedAddress }}</strong></p>
          <button class="primary-button" type="button" @click="emit('done')">进入邮箱</button>
        </div>
      </div>
    </section>
    <footer class="public-footer">© 2025–2027 JMI-OPENATOM · 数据由本地邮件服务托管</footer>
  </main>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { CircleAlert, CircleCheck } from 'lucide-vue-next'
import { activateMailbox } from '../../api'
import ThemeToggle from '../common/ThemeToggle.vue'

const emit = defineEmits<{ (e: 'done'): void }>()

const mode = ref<'pinyin' | 'custom'>('pinyin')
const localPart = ref('')
const error = ref('')
const busy = ref(false)
const activating = ref(false)
const activatedAddress = ref('')

async function onSubmit() {
  error.value = ''
  busy.value = true
  try {
    const result =
      mode.value === 'pinyin'
        ? await activateMailbox({ usePinyin: true })
        : await activateMailbox({ localPart: localPart.value.trim() })
    activatedAddress.value = result.address ?? ''
    activating.value = true
  } catch (err) {
    error.value = err instanceof Error ? err.message : '激活失败，请稍后重试。'
  } finally {
    busy.value = false
  }
}
</script>