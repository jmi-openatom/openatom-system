<template>
  <div class="site-shell">
    <header class="site-header">
      <div class="container site-header__inner">
        <router-link class="brand" to="/">
          <img alt="徽标" class="brand__mark" src="/logo.png" />
          <span>
            <strong>开放原子开源社团</strong>
            <small>江苏海事职业技术学院</small>
          </span>
        </router-link>
        <nav class="site-nav">
          <a href="/#overview">概览</a>
          <router-link to="/activities">活动</router-link>
          <router-link to="/apply">入会申请</router-link>
          <router-link to="/progress">我的申请</router-link>
          <router-link to="/check-in/scan">扫码签到</router-link>
        </nav>
        <div class="site-header__actions">
          <el-button
            v-if="isLoggedIn"
            circle
            class="notification-btn"
            @click="$router.push('/notifications')"
          >
            <el-badge :hidden="unreadCount === 0" is-dot>
              <el-icon>
                <Bell />
              </el-icon>
            </el-badge>
          </el-button>
          <el-button v-if="isLoggedIn" :icon="UserFilled" plain @click="$router.push('/profile')"
            >个人中心
          </el-button>
          <el-button v-else :icon="UserFilled" plain @click="$router.push('/admin/login')"
            >登录
          </el-button>
          <el-button
            v-if="showAdminEntry"
            :icon="Setting"
            type="primary"
            @click="$router.push('/admin')"
          >
            管理员后台
          </el-button>
        </div>
      </div>
    </header>

    <main class="site-main">
      <router-view />
    </main>

    <footer class="site-footer">
      <div class="container site-footer__inner">
        <div class="site-footer__brand">
          <img alt="徽标" class="site-footer__logo" src="/logo.png" />
          <div class="brand-text">
            <span class="name">开放原子开源社团</span>
            <span class="slogan">Open Atom Open Source Club</span>
          </div>
        </div>
        <div class="site-footer__info">
          <p>技术分享 · 项目实践 · 竞赛训练 · 开源协作</p>
          <p class="copyright">
            © 2026 JMI-OPENATOM. All rights reserved.
            <span class="version-tag">{{ version }}</span>
          </p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script lang="ts">
import { Bell, Setting, UserFilled } from '@element-plus/icons-vue'
import { markRaw } from 'vue'
import { getToken } from '@/utils/auth.ts'
import { hasAdminAccess } from '@/utils/permission.ts'
import { notificationApi } from '@/api'

export default {
  name: 'SiteLayout',
  data() {
    return {
      Setting: markRaw(Setting),
      UserFilled: markRaw(UserFilled),
      Bell: markRaw(Bell),
      unreadCount: 0,
      unreadTimer: null as any,
      version: __APP_VERSION__,
    }
  },
  computed: {
    isLoggedIn() {
      return Boolean(getToken())
    },
    showAdminEntry() {
      return hasAdminAccess()
    },
  },
  created() {
    if (this.isLoggedIn) {
      this.fetchUnreadCount()
      this.unreadTimer = setInterval(this.fetchUnreadCount, 30000)
    }
  },
  beforeUnmount() {
    if (this.unreadTimer) clearInterval(this.unreadTimer)
  },
  methods: {
    async fetchUnreadCount() {
      try {
        const count = await notificationApi.unreadCount()
        this.unreadCount = typeof count === 'number' ? count : 0
      } catch (e) {
        // ignore
      }
    },
  },
}
</script>

<style scoped>
/* 页面整体布局：Flex 垂直排布 */
.site-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f8fafc;
}

/* Header 样式 */
.site-header {
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.8);
  border-bottom: 1px solid #e2e8f0;
  backdrop-filter: blur(12px);
}

.site-header__inner {
  display: flex;
  min-height: 76px;
  align-items: center;
  gap: 26px;
}

/* 主体内容：撑开空间 */
.site-main {
  flex: 1;
}

/* --- Footer 完整样式 --- */
.site-footer {
  flex-shrink: 0;
  padding: 48px 0 32px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  color: #64748b;
}

.site-footer__inner {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.site-footer__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.site-footer__logo {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  filter: grayscale(0.2);
}

.brand-text .name {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.brand-text .slogan {
  font-size: 12px;
  color: #94a3b8;
}

.site-footer__info {
  text-align: right;
  font-size: 14px;
}

.site-footer__info p {
  margin: 0 0 8px;
}

.copyright {
  font-size: 12px;
  color: #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.version-tag {
  background: #f1f5f9;
  color: #94a3b8;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  border: 1px solid #e2e8f0;
}

/* 通用样式 */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.brand__mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
}

.brand strong {
  display: block;
  color: #0f172a;
}

.brand small {
  color: #64748b;
}

.site-nav {
  flex: 1;
  display: flex;
  gap: 8px;
}

.site-nav a {
  padding: 8px 16px;
  color: #334155;
  text-decoration: none;
  border-radius: 20px;
  transition: 0.3s;
}

.site-nav a:hover {
  background: #eff6ff;
  color: #2563eb;
}

.nav-notification {
  display: flex !important;
  align-items: center;
  justify-content: center;
  padding: 8px !important;
  width: 40px;
  height: 40px;
}

.notification-btn {
  border: none !important;
  background: transparent !important;
  font-size: 20px;
  color: #64748b;
  transition: all 0.3s;
}

.notification-btn:hover {
  color: #2563eb;
  background: #eff6ff !important;
}

.notification-btn :deep(.el-badge__content.is-fixed.is-dot) {
  right: 2px;
  top: 2px;
}

/* 响应式 */
@media (max-width: 900px) {
  .site-header__inner {
    min-height: auto;
    flex-wrap: wrap;
    gap: 12px;
    padding: 12px 0;
  }

  .brand {
    flex: 1 1 calc(100% - 132px);
    min-width: 0;
  }

  .brand strong,
  .brand small {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .site-header__actions {
    display: flex;
    flex: 0 0 auto;
    gap: 8px;
  }

  .site-header__actions :deep(.el-button) {
    padding: 8px 10px;
  }

  .site-nav {
    order: 3;
    flex: 1 0 100%;
    gap: 6px;
    overflow-x: auto;
    padding-bottom: 2px;
    scrollbar-width: none;
  }

  .site-nav::-webkit-scrollbar {
    display: none;
  }

  .site-nav a {
    flex: 0 0 auto;
    padding: 8px 12px;
    background: rgba(239, 246, 255, 0.72);
  }

  .site-footer__inner {
    flex-direction: column;
    align-items: center;
    gap: 24px;
    text-align: center;
  }

  .site-footer__info {
    text-align: center;
  }
}

@media (max-width: 560px) {
  .site-header__actions :deep(.el-button span) {
    display: none;
  }
}
</style>
