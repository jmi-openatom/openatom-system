<template>
  <div class="site-shell">
    <header class="site-header">
      <div class="container site-header__inner">
        <router-link to="/" class="brand">
          <img class="brand__mark" src="/logo.png" alt="开放原子开源社团徽标" />
          <span>
            <strong>开放原子开源社团</strong>
            <small>江苏海事职业技术学院</small>
          </span>
        </router-link>
        <nav class="site-nav">
          <a href="/#overview">概览</a>
          <router-link to="/activities">活动</router-link>
          <a href="/#people">主要人员</a>
          <a href="/#achievements">比赛获奖</a>
          <router-link to="/forms">入会申请</router-link>
          <router-link to="/progress">我的申请</router-link>
        </nav>
        <el-button v-if="showAdminEntry" type="primary" :icon="Setting" @click="$router.push('/admin')">管理员后台</el-button>
      </div>
    </header>
    <main>
      <router-view />
    </main>
    <footer class="site-footer">
      <div class="container site-footer__inner">
        <div class="site-footer__brand">
          <img class="site-footer__logo" src="/logo.png" alt="开放原子开源社团徽标" />
          <span>开放原子开源社团</span>
        </div>
        <span>技术分享、项目实践、竞赛训练与开源协作</span>
      </div>
    </footer>
  </div>
</template>

<script>
import { Setting } from '@element-plus/icons-vue'
import { markRaw } from 'vue'
import { hasAdminAccess } from '../utils/permission'

export default {
  name: 'SiteLayout',
  data() {
    return {
      Setting: markRaw(Setting)
    }
  },
  computed: {
    showAdminEntry() {
      return hasAdminAccess()
    }
  }
}
</script>

<style scoped>
.site-shell {
  min-height: 100vh;
  background: transparent;
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(248, 251, 255, 0.86);
  border-bottom: 1px solid rgba(219, 230, 245, 0.9);
  backdrop-filter: blur(18px);
}

.site-header__inner {
  display: flex;
  min-height: 76px;
  align-items: center;
  gap: 26px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 210px;
}

.brand__mark {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.2);
  object-fit: cover;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  color: #0f172a;
}

.brand small {
  margin-top: 2px;
  color: var(--oa-muted);
}

.site-nav {
  display: flex;
  flex: 1;
  gap: 10px;
  color: #334155;
  font-size: 14px;
}

.site-nav a {
  padding: 10px 14px;
  border-radius: 999px;
  white-space: nowrap;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}

.site-nav a:hover,
.site-nav a.router-link-active {
  color: var(--oa-primary-dark);
  background: rgba(37, 99, 235, 0.08);
  font-weight: 600;
}

.site-footer {
  padding: 30px 0 38px;
  border-top: 1px solid rgba(219, 230, 245, 0.86);
  background: rgba(255, 255, 255, 0.58);
}

.site-footer__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--oa-muted);
}

.site-footer__brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #0f172a;
  font-weight: 600;
}

.site-footer__logo {
  width: 28px;
  height: 28px;
  border-radius: 9px;
  object-fit: cover;
}

@media (max-width: 900px) {
  .site-header__inner {
    flex-wrap: wrap;
    padding: 12px 0;
  }

  .site-nav {
    width: 100%;
    order: 3;
    overflow-x: auto;
  }

  .site-footer__inner {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
