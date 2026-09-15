<template>
  <main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
      <router-link class="quest-brand" to="/">
        <span class="quest-brand__mark">OA</span>
        <span><strong>OpenAtom Quest</strong><small>开放原子开源社团</small></span>
      </router-link>

      <div class="login-panel__copy">
        <p class="eyebrow">START YOUR QUEST</p>
        <h1 id="login-title">把每一步学习，变成真实贡献。</h1>
        <p>沿着清晰的成长路线完成任务，获得导师反馈，逐步加入真实开源项目。</p>
      </div>

      <el-button type="primary" size="large" class="login-button" @click="startLogin">
        使用 OpenAtom 账号登录
      </el-button>

      <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />

      <p class="login-panel__legal">
        登录即表示你同意用户协议、隐私政策和社团成员行为准则。平台不提供独立注册或密码登录。
      </p>
      <a class="text-link" href="mailto:contact@jmi-openatom.cn">登录遇到问题？联系管理员</a>
    </section>

    <aside class="login-story" aria-label="平台能力介绍">
      <div class="login-story__content">
        <p class="eyebrow eyebrow--inverse">OPEN SOURCE JOURNEY</p>
        <h2>从新人报到，到项目贡献者。</h2>
        <ol class="journey-list">
          <li><span>L0</span><strong>新人报到</strong><small>了解社团与协作规则</small></li>
          <li><span>L1</span><strong>开源入门</strong><small>掌握 Git、Issue 与 PR</small></li>
          <li><span>L2</span><strong>技术学习</strong><small>完成方向基础任务</small></li>
          <li><span>L3</span><strong>项目实战</strong><small>解决接近真实的问题</small></li>
          <li><span>L4</span><strong>项目贡献</strong><small>进入真实开源项目</small></li>
        </ol>
      </div>
    </aside>
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const errorMessage = computed(() => {
  const error = typeof route.query.error === 'string' ? route.query.error : ''
  const messages: Record<string, string> = {
    invalid_callback: '登录回调已失效，请重新发起登录。',
    member_disabled: '该成员账号已被禁用，请联系管理员。',
    oauth_exchange_failed: 'OpenAtom 登录服务暂时不可用，请稍后重试。',
    access_denied: '你取消了 OpenAtom 授权。',
  }
  return error ? messages[error] || '登录未完成，请重新尝试。' : ''
})

function startLogin() {
  window.location.assign('/api/auth/login')
}
</script>
