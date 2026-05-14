<template>
  <div class="site-shell">
    <header class="site-header">
      <div class="container site-header__inner">
        <router-link class="brand" to="/">
            <img alt="徽标" class="site-footer__logo" src="/logo.png" />
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
          <router-link to="/leaves">请假</router-link>
          <router-link to="/calendar">校历</router-link>
          <router-link to="/check-in/scan">扫码签到</router-link>
        </nav>
        <el-button class="mobile-menu-btn" circle :icon="Menu" @click="mobileNavVisible = true" />
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

    <el-drawer
      v-model="mobileNavVisible"
      class="mobile-nav-drawer"
      direction="rtl"
      size="82%"
      title="导航"
    >
      <nav class="mobile-nav">
        <a href="/#overview" @click="mobileNavVisible = false">概览</a>
        <router-link to="/activities" @click="mobileNavVisible = false">活动</router-link>
        <router-link to="/apply" @click="mobileNavVisible = false">入会申请</router-link>
        <router-link to="/progress" @click="mobileNavVisible = false">我的申请</router-link>
        <router-link to="/leaves" @click="mobileNavVisible = false">请假</router-link>
        <router-link to="/calendar" @click="mobileNavVisible = false">校历</router-link>
        <router-link to="/check-in/scan" @click="mobileNavVisible = false">扫码签到</router-link>
        <router-link v-if="isLoggedIn" to="/notifications" @click="mobileNavVisible = false">
          通知
        </router-link>
        <router-link v-if="isLoggedIn" to="/profile" @click="mobileNavVisible = false">
          个人中心
        </router-link>
        <router-link v-else to="/admin/login" @click="mobileNavVisible = false">登录</router-link>
        <router-link v-if="showAdminEntry" to="/admin" @click="mobileNavVisible = false">
          管理员后台
        </router-link>
      </nav>
    </el-drawer>

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

<script setup lang="ts">
import {
  Bell as BellIcon,
  Menu as MenuIcon,
  Setting as SettingIcon,
  UserFilled as UserFilledIcon,
} from '@element-plus/icons-vue'
import { computed, markRaw, onBeforeUnmount, onMounted, ref } from 'vue'
import { getToken } from '@/utils/auth.ts'
import { hasAdminAccess } from '@/utils/permission.ts'
import { notificationApi } from '@/api'
import {LiquidLogo} from "@/components/ui/liquid-logo";

const Setting = markRaw(SettingIcon)

const UserFilled = markRaw(UserFilledIcon)

const Bell = markRaw(BellIcon)

const Menu = markRaw(MenuIcon)

const mobileNavVisible = ref(false)

const unreadCount = ref(0)

const unreadTimer = ref(null as any)

const version = ref(__APP_VERSION__)
const isLoggedIn = computed(() => {
  return Boolean(getToken())
})

const showAdminEntry = computed(() => {
  return hasAdminAccess()
})

async function fetchUnreadCount() {
  try {
    const count = await notificationApi.unreadCount()
    unreadCount.value = typeof count === 'number' ? count : 0
  } catch (e) {
    // ignore
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    fetchUnreadCount()
    unreadTimer.value = setInterval(fetchUnreadCount, 30000)
  }
})

onBeforeUnmount(() => {
  if (unreadTimer.value) clearInterval(unreadTimer.value)
})
</script>

<style scoped>
/* 页面整体布局：Flex 垂直排布 */
.site-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #ffffff;
}

/* Header 样式 */
.site-header {
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  backdrop-filter: saturate(180%) blur(18px);
  animation: navDrop 0.38s ease both;
}

.site-header__inner {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

/* 主体内容：撑开空间 */
.site-main {
  flex: 1;
}

/* --- Footer 完整样式 --- */
.site-footer {
  flex-shrink: 0;
  padding: 64px 0;
  background: #f5f5f7;
  border-top: 0;
  color: #333333;
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
  border-radius: 8px;
  filter: grayscale(1) contrast(1.05);
}

.brand-text .name {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}

.brand-text .slogan {
  font-size: 12px;
  color: #7a7a7a;
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
  color: #7a7a7a;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.version-tag {
  background: #ffffff;
  color: #7a7a7a;
  padding: 2px 6px;
  border-radius: 999px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  border: 1px solid #e0e0e0;
}

/* 通用样式 */
.brand {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 9px;
  text-decoration: none;
}

.brand__mark {
  width: 30px;
  height: 30px;
  border-radius: 5px;
  filter: grayscale(1) contrast(1.08);
}

.brand strong {
  display: block;
  color: #1d1d1f;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0;
  line-height: 1.15;
}

.brand small {
  display: block;
  margin-top: 2px;
  color: #7a7a7a;
  font-size: 10px;
  line-height: 1;
}

.site-nav {
  flex: 1 1 auto;
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.mobile-menu-btn {
  display: none;
  flex: 0 0 auto;
  border-color: #d2d2d7 !important;
  background: #ffffff !important;
  color: #1d1d1f !important;
}

.site-nav a {
  padding: 9px 11px;
  color: #333333;
  text-decoration: none;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.1;
  letter-spacing: 0;
  opacity: 0.86;
  white-space: nowrap;
  transition:
    color 0.16s ease,
    opacity 0.16s ease,
    background-color 0.16s ease;
}

.site-nav a:hover {
  background: #f5f5f7;
  color: #000000;
  opacity: 1;
}

.site-nav a.router-link-active {
  background: #1d1d1f;
  color: #ffffff;
  opacity: 1;
}

.site-header__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.site-header__actions :deep(.el-button) {
  min-height: 36px;
  padding: 8px 13px;
  border-color: #d2d2d7;
  background: #ffffff;
  color: #1d1d1f;
  font-size: 12px;
  line-height: 1;
}

.site-header__actions :deep(.el-button:hover) {
  border-color: #1d1d1f;
  color: #000000;
}

.site-header__actions :deep(.el-button--primary) {
  border-color: #1d1d1f;
  background: #1d1d1f;
  color: #ffffff;
}

.site-header__actions :deep(.el-button--primary:hover) {
  border-color: #000000;
  background: #000000;
  color: #ffffff;
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
  background: #f5f5f7 !important;
  font-size: 20px;
  color: #1d1d1f;
  transition:
    background-color 0.16s ease,
    color 0.16s ease,
    transform 0.16s ease;
}

.notification-btn:hover {
  color: #000000;
  background: #ededf0 !important;
}

.notification-btn :deep(.el-badge__content.is-fixed.is-dot) {
  right: 2px;
  top: 2px;
}

.mobile-nav {
  display: grid;
  gap: 8px;
}

.mobile-nav a {
  display: flex;
  min-height: 48px;
  align-items: center;
  padding: 0 14px;
  color: #1d1d1f;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  font-weight: 400;
}

@keyframes navDrop {
  from {
    opacity: 0;
    transform: translateY(-12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 900px) {
  .site-header__inner {
    min-height: 52px;
    flex-wrap: nowrap;
    gap: 12px;
    padding: 10px 0;
  }

  .brand {
    flex: 1 1 auto;
    min-width: 0;
  }

  .brand__mark {
    width: 28px;
    height: 28px;
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
    display: none;
  }

  .mobile-menu-btn {
    display: inline-flex;
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
  .site-header__actions {
    gap: 6px;
  }

  .site-header__actions :deep(.el-button span) {
    display: none;
  }

  .site-header__actions :deep(.el-button) {
    width: 40px;
    padding: 8px;
  }

  .site-header__actions :deep(.el-button + .el-button) {
    margin-left: 0;
  }

  .brand small {
    display: none;
  }
}
</style>
