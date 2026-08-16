<template>
  <main id="mail-main" class="login-page" tabindex="-1">
    <nav aria-label="邮箱站导航" class="public-nav">
      <a class="brand" href="https://www.jmi-openatom.cn">
        <img alt="开放原子开源社团" class="brand-logo" src="/logo.png" />
        <span><strong>开放原子开源社团</strong><small>江苏海事职业技术学院</small></span>
      </a>
      <ThemeToggle />
    </nav>
    <section class="login-hero">
      <div class="login-copy">
        <span class="eyebrow">OPENATOM MAIL</span>
        <h1>欢迎使用社团邮箱</h1>
        <p>使用开放原子账号登录。你的邮箱地址根据姓名拼音自动生成，邮件数据由社团自己的服务器保存。</p>
        <a class="primary-button login-button" href="/api/oauth/login">
          使用 OAuth 安全登录 <ArrowRight :size="18" />
        </a>
        <div class="trust-row" aria-label="安全特性">
          <span><ShieldCheck :size="16" /> OAuth 2.0</span>
          <span><Server :size="16" /> 自建服务</span>
          <span><LockKeyhole :size="16" /> HttpOnly 会话</span>
        </div>
      </div>
    </section>
    <footer class="public-footer">© 2025–2027 JMI-OPENATOM · 数据由本地邮件服务托管</footer>
  </main>
</template>

<script lang="ts" setup>
import { ArrowRight, LockKeyhole, Server, ShieldCheck } from 'lucide-vue-next'
import ThemeToggle from '../components/common/ThemeToggle.vue'
</script>
