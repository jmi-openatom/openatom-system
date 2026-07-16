<template>
  <div ref="siteShellRef" :class="{ 'site-shell--home': isHomeRoute }" class="site-shell">
    <a class="skip-link" href="#site-main">跳到主要内容</a>
    <header
      :aria-hidden="!headerVisible"
      :class="{ 'site-header--hidden': !headerVisible, 'site-header--home': isHomeRoute }"
      class="site-header"
      v-bind="!headerVisible ? { inert: '' } : {}"
    >
      <div class="container site-header__inner">
        <router-link class="brand" to="/">
          <img alt="徽标" class="site-footer__logo" src="/logo.png" />
          <span>
            <strong>开放原子开源社团</strong>
            <small>江苏海事职业技术学院</small>
          </span>
        </router-link>
        <nav aria-label="主导航" class="site-nav">
          <router-link exact-active-class="router-link-active" to="/">首页</router-link>
          <router-link to="/activities">活动</router-link>
          <router-link to="/blog">博客</router-link>
          <router-link to="/members">成员</router-link>
          <router-link to="/partners">伙伴</router-link>
          <router-link to="/apps">应用</router-link>
          <router-link to="/leaves">请假</router-link>
          <router-link to="/apply">加入我们</router-link>
          <el-dropdown
            popper-class="site-header-dropdown"
            trigger="click"
            @command="handleNavCommand"
          >
            <button
              :class="{ 'is-active': isMoreRouteActive }"
              aria-label="打开更多导航"
              class="site-nav__more"
              type="button"
            >
              更多
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/about">社团介绍</el-dropdown-item>
                <el-dropdown-item command="/regulations">规章制度</el-dropdown-item>
                <el-dropdown-item command="/calendar">校历</el-dropdown-item>
                <el-dropdown-item command="/alumni-managers">往届管理人员</el-dropdown-item>
                <el-dropdown-item divided command="/points">积分中心</el-dropdown-item>
                <el-dropdown-item command="/images">图床</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </nav>
        <el-button
          :icon="Menu"
          aria-label="打开导航"
          circle
          class="mobile-menu-btn"
          title="打开导航"
          @click="mobileNavVisible = true"
        />
        <div class="site-header__actions">
          <ThemeToggle />
          <el-button
            v-if="isLoggedIn"
            circle
            class="notification-btn site-header__action--notification"
            aria-label="查看通知"
            title="查看通知"
            @click="$router.push('/notifications')"
          >
            <el-badge :hidden="unreadCount === 0" is-dot>
              <el-icon>
                <Bell />
              </el-icon>
            </el-badge>
          </el-button>
          <el-button
            v-if="isLoggedIn"
            :icon="Grid"
            class="site-header__action--workspace"
            plain
            @click="$router.push('/workspace')"
          >
            工作台
          </el-button>
          <el-dropdown
            v-if="isLoggedIn"
            class="site-header__account-dropdown"
            popper-class="site-header-dropdown"
            trigger="click"
            @command="handleNavCommand"
          >
            <el-button :icon="UserFilled" class="site-header__action--account" plain>
              我的
              <el-icon class="site-header__account-arrow"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/workspace">个人工作台</el-dropdown-item>
                <el-dropdown-item command="/profile">我的主页</el-dropdown-item>
                <el-dropdown-item command="/settings/account">账号与安全</el-dropdown-item>
                <el-dropdown-item v-if="showAdminEntry" divided command="/admin">
                  管理后台
                </el-dropdown-item>
                <el-dropdown-item divided command="__logout__">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button
            v-else
            :icon="UserFilled"
            class="site-header__action--account"
            plain
            @click="$router.push('/login')"
            >登录
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
      <nav aria-label="移动端导航" class="mobile-nav">
        <section class="mobile-nav__group">
          <p class="mobile-nav__label">主要功能</p>
          <div class="mobile-nav__links">
            <router-link to="/" @click="mobileNavVisible = false">首页</router-link>
            <router-link to="/activities" @click="mobileNavVisible = false">活动</router-link>
            <router-link to="/members" @click="mobileNavVisible = false">成员</router-link>
            <router-link to="/partners" @click="mobileNavVisible = false">伙伴</router-link>
            <router-link to="/apps" @click="mobileNavVisible = false">应用</router-link>
            <router-link to="/blog" @click="mobileNavVisible = false">博客</router-link>
            <router-link to="/leaves" @click="mobileNavVisible = false">请假</router-link>
          </div>
        </section>

        <section class="mobile-nav__group">
          <p class="mobile-nav__label">加入与了解</p>
          <div class="mobile-nav__links">
            <router-link to="/apply" @click="mobileNavVisible = false"> 加入我们 </router-link>
            <router-link to="/about" @click="mobileNavVisible = false">社团介绍</router-link>
            <router-link to="/regulations" @click="mobileNavVisible = false">规章制度</router-link>
            <router-link to="/calendar" @click="mobileNavVisible = false">校历</router-link>
            <router-link to="/alumni-managers" @click="mobileNavVisible = false">
              往届管理人员
            </router-link>
          </div>
        </section>

        <section class="mobile-nav__group">
          <p class="mobile-nav__label">个人工作台</p>
          <div class="mobile-nav__links">
            <template v-if="isLoggedIn">
              <router-link
                class="mobile-nav__primary"
                to="/workspace"
                @click="mobileNavVisible = false"
              >
                工作台首页
              </router-link>
              <router-link to="/progress" @click="mobileNavVisible = false">申请进度</router-link>
              <router-link to="/notifications" @click="mobileNavVisible = false"
                >通知中心</router-link
              >
              <router-link to="/points" @click="mobileNavVisible = false">积分中心</router-link>
              <router-link to="/votes" @click="mobileNavVisible = false">投票</router-link>
              <router-link to="/members" @click="mobileNavVisible = false">成员名录</router-link>
              <router-link to="/blog/my" @click="mobileNavVisible = false">我的博客</router-link>
              <router-link to="/images" @click="mobileNavVisible = false">图床</router-link>
              <router-link to="/profile" @click="mobileNavVisible = false">我的主页</router-link>
              <router-link to="/settings/account" @click="mobileNavVisible = false">
                账号与安全
              </router-link>
              <router-link v-if="showAdminEntry" to="/admin" @click="mobileNavVisible = false">
                管理后台
              </router-link>
            </template>
            <router-link v-else to="/login" @click="mobileNavVisible = false">登录</router-link>
          </div>
        </section>
      </nav>
    </el-drawer>

    <main id="site-main" ref="siteMainRef" class="site-main" tabindex="-1">
      <router-view v-slot="{ Component, route }">
        <transition
          :css="false"
          mode="out-in"
          @enter="routeTransition.enter"
          @leave="routeTransition.leave"
          @before-enter="routeTransition.beforeEnter"
        >
          <component :is="Component" :key="route.fullPath" />
        </transition>
      </router-view>
    </main>

    <footer class="site-footer">
      <div class="container site-footer__inner">
        <div class="site-footer__brand">
          <img alt="徽标" class="site-footer__logo" src="/logo.png" />
          <div class="brand-text">
            <span class="name">开放原子开源社团</span>
            <span class="slogan">JMI - OPENATOM</span>
          </div>
        </div>
        <div class="site-footer__info">
          <p>技术分享 · 项目实践 · 竞赛训练 · 开源协作</p>
          <p class="copyright">
            © 2025-2027 JMI-OPENATOM &
            <a href="http://www.ariven.cn/">Ariven(软件技术252301 何治皓).</a> All rights reserved.
          </p>
          <span class="version-tag">{{ version }}</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script lang="ts" setup>
import {
  ArrowDown as ArrowDownIcon,
  Bell as BellIcon,
  Grid as GridIcon,
  Menu as MenuIcon,
  UserFilled as UserFilledIcon,
} from '@element-plus/icons-vue'
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearSession, getToken } from '@/utils/auth.ts'
import { hasAdminAccess } from '@/utils/permission.ts'
import { authApi, notificationApi } from '@/api'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { useRouteTransition } from '@/composables/useRouteTransition'
import { useSiteShellMotion } from '@/composables/useSiteMotion'

const ArrowDown = markRaw(ArrowDownIcon)

const UserFilled = markRaw(UserFilledIcon)

const Bell = markRaw(BellIcon)

const Grid = markRaw(GridIcon)

const Menu = markRaw(MenuIcon)

const mobileNavVisible = ref(false)

const unreadCount = ref(0)

const unreadTimer = ref(null as any)

const sessionToken = ref(getToken())

const version = ref(__APP_VERSION__)

const route = useRoute()

const router = useRouter()

const siteShellRef = ref<HTMLElement>()

const siteMainRef = ref<HTMLElement>()

const hasScrolledPastHeroTop = ref(false)

const isHomeRoute = computed(() => route.name === 'site-home')

const routeTransition = useRouteTransition({
  enterY: 18,
  leaveY: -8,
  enterDuration: 300,
  leaveDuration: 160,
})

const headerVisible = computed(() => {
  return !isHomeRoute.value || hasScrolledPastHeroTop.value || mobileNavVisible.value
})

const isLoggedIn = computed(() => {
  return Boolean(sessionToken.value)
})

const showAdminEntry = computed(() => {
  return hasAdminAccess()
})

const moreRoutePrefixes = [
  '/about',
  '/regulations',
  '/calendar',
  '/alumni-managers',
  '/points',
  '/images',
]

const isMoreRouteActive = computed(() => {
  return moreRoutePrefixes.some((path) => route.path.startsWith(path))
})

useSiteShellMotion(siteShellRef, siteMainRef, isHomeRoute)

async function fetchUnreadCount() {
  try {
    const count = await notificationApi.unreadCount()
    unreadCount.value = typeof count === 'number' ? count : 0
  } catch (e) {
    // ignore
  }
}

function updateHeaderState() {
  hasScrolledPastHeroTop.value = window.scrollY > 72
}

async function handleNavCommand(command: string) {
  mobileNavVisible.value = false
  if (command === '__logout__') {
    try {
      await authApi.logout()
    } finally {
      clearSession()
      sessionToken.value = null
      unreadCount.value = 0
      if (unreadTimer.value) {
        clearInterval(unreadTimer.value)
        unreadTimer.value = null
      }
      router.replace('/')
    }
    return
  }
  router.push(command)
}

onMounted(() => {
  updateHeaderState()
  window.addEventListener('scroll', updateHeaderState, { passive: true })
  if (isLoggedIn.value) {
    fetchUnreadCount()
    unreadTimer.value = setInterval(fetchUnreadCount, 30000)
  }
})

watch(
  () => route.fullPath,
  async () => {
    sessionToken.value = getToken()
    await nextTick()
    updateHeaderState()
  },
)

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateHeaderState)
  if (unreadTimer.value) clearInterval(unreadTimer.value)
  routeTransition.kill()
})
</script>

<style scoped>
/* 页面整体布局：Flex 垂直排布 */
.site-shell {
  --oa-site-header-height: 60px;
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  isolation: isolate;
  background: var(--oa-page-bg);
  color: var(--oa-text);
}

.site-shell:not(.site-shell--home) .site-main {
  padding-top: var(--oa-site-header-height);
}

.skip-link {
  position: fixed;
  top: 8px;
  left: 12px;
  z-index: 100;
  padding: 10px 14px;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  border-radius: 10px;
  text-decoration: none;
  transform: translateY(-150%);
  transition: transform 0.16s ease;
}

.skip-link:focus {
  transform: translateY(0);
}

/* Header 样式 */
.site-header {
  flex-shrink: 0;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 20;
  overflow: hidden;
  background: var(--oa-header-bg);
  border-bottom: 1px solid var(--oa-header-border);
  box-shadow: var(--oa-header-shadow);
  -webkit-backdrop-filter: blur(24px) saturate(190%);
  backdrop-filter: blur(24px) saturate(190%);
  transform: translateY(0);
  opacity: 1;
  transition:
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.22s ease,
    background-color 0.22s ease,
    box-shadow 0.22s ease;
  will-change: transform, opacity;
}

.site-main,
.site-footer {
  position: relative;
  z-index: 1;
}

.site-header::before {
  position: absolute;
  inset: 0;
  content: '';
  background: var(--oa-header-overlay);
  pointer-events: none;
}

.site-header--home {
  background: var(--oa-header-home-bg);
  box-shadow: var(--oa-header-home-shadow);
}

.site-header--hidden {
  pointer-events: none;
  opacity: 0;
  transform: translateY(calc(-100% - 12px));
}

.site-header__inner {
  position: relative;
  z-index: 1;
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
  background: var(--oa-page-soft-bg);
  border-top: 0;
  color: var(--oa-text-soft);
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
}

.brand-text .name {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: var(--oa-text);
}

.brand-text .slogan {
  letter-spacing: 0.15rem;
  font-size: 12px;
  color: var(--oa-muted);
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
  color: var(--oa-muted);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.version-tag {
  background: var(--oa-elevated-bg);
  color: var(--oa-muted);
  padding: 2px 6px;
  border-radius: 999px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  border: 1px solid var(--oa-border);
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
  color: var(--oa-text);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0;
  line-height: 1.15;
}

.brand small {
  display: block;
  margin-top: 2px;
  color: var(--oa-muted);
  font-size: 12px;
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
  border-color: var(--oa-button-border) !important;
  background: var(--oa-button-bg) !important;
  color: var(--oa-button-text) !important;
}

.site-nav a {
  padding: 9px 11px;
  color: var(--oa-text-soft);
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
  background: var(--oa-nav-hover-bg);
  color: var(--oa-text);
  opacity: 1;
}

.site-nav a.router-link-active {
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
  opacity: 1;
}

.site-nav__more {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  gap: 4px;
  padding: 9px 11px;
  color: var(--oa-text-soft);
  background: transparent;
  border: 0;
  border-radius: 999px;
  font: inherit;
  font-size: 12px;
  line-height: 1.1;
  cursor: pointer;
  opacity: 0.86;
  transition:
    color 0.16s ease,
    opacity 0.16s ease,
    background-color 0.16s ease;
}

.site-nav__more:hover,
.site-nav__more:focus-visible {
  color: var(--oa-text);
  background: var(--oa-nav-hover-bg);
  opacity: 1;
  outline: none;
}

.site-nav__more.is-active {
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  opacity: 1;
}

.site-nav__more .el-icon,
.site-header__account-arrow {
  font-size: 11px;
}

.site-nav .nav-next-link {
  color: #a855f7;
  font-weight: 600;
  opacity: 1;
  text-shadow: 0 0 12px rgba(168, 85, 247, 0.4);
}

.site-nav .nav-next-link:hover {
  background: rgba(168, 85, 247, 0.12);
  color: #c084fc;
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
  border-color: var(--oa-button-border);
  background: var(--oa-button-bg);
  color: var(--oa-button-text);
  font-size: 12px;
  line-height: 1;
}

.site-header__actions :deep(.el-button:hover) {
  border-color: var(--oa-button-hover-border);
  color: var(--oa-button-hover-text);
}

.site-header__account-dropdown {
  display: inline-flex;
}

.site-header__action--account {
  gap: 5px;
}

.site-header__actions :deep(.el-button--primary) {
  border-color: var(--oa-active-bg);
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
}

.site-header__actions :deep(.el-button--primary:hover) {
  border-color: var(--oa-active-hover-bg);
  background: var(--oa-active-hover-bg);
  color: var(--oa-active-text);
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
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  min-height: 40px !important;
  aspect-ratio: 1;
  padding: 0 !important;
  border: none !important;
  border-radius: 50% !important;
  background: var(--oa-button-subtle-bg) !important;
  font-size: 20px;
  color: var(--oa-button-text);
  transition:
    background-color 0.16s ease,
    color 0.16s ease,
    transform 0.16s ease;
}

.notification-btn:hover {
  color: var(--oa-button-hover-text);
  background: var(--oa-button-hover-bg) !important;
}

.notification-btn :deep(.el-badge__content.is-fixed.is-dot) {
  right: 2px;
  top: 2px;
}

.mobile-nav {
  display: grid;
  gap: 28px;
  padding-bottom: 28px;
}

.mobile-nav__group,
.mobile-nav__links {
  display: grid;
}

.mobile-nav__group {
  gap: 10px;
}

.mobile-nav__links {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.mobile-nav__label {
  margin: 0;
  color: var(--oa-muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.mobile-nav a {
  display: flex;
  min-height: 46px;
  align-items: center;
  padding: 0 12px;
  color: var(--oa-text);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 12px;
  font-weight: 400;
  text-decoration: none;
  transition:
    color 0.16s ease,
    background-color 0.16s ease,
    border-color 0.16s ease;
}

.mobile-nav a:hover,
.mobile-nav a:focus-visible,
.mobile-nav a.router-link-active {
  color: var(--oa-text);
  background: var(--oa-nav-hover-bg);
  border-color: var(--oa-border);
  outline: none;
}

.mobile-nav a.mobile-nav__primary {
  border-color: var(--oa-active-bg);
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
  font-weight: 600;
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
  .site-shell {
    --oa-site-header-height: 72px;
  }

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
    width: 44px;
    height: 44px;
    min-height: 44px;
    aspect-ratio: 1;
    padding: 0;
  }

  .mobile-menu-btn :deep(.el-icon),
  .site-header__actions :deep(.el-icon) {
    font-size: 18px;
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
    width: 44px;
    height: 44px;
    min-height: 44px;
    aspect-ratio: 1;
    padding: 0;
  }

  .site-header__actions :deep(.el-button + .el-button) {
    margin-left: 0;
  }

  .brand small {
    display: none;
  }

  .site-header__action--notification,
  .site-header__action--workspace,
  .site-header__action--account,
  .site-header__account-dropdown {
    display: none !important;
  }
}
</style>

<style>
.site-header-dropdown.el-popper {
  min-width: 168px;
  border-color: var(--oa-border);
  border-radius: 14px;
  background: var(--oa-elevated-bg);
  box-shadow: var(--oa-card-shadow);
}

.site-header-dropdown .el-dropdown-menu {
  padding: 6px;
  background: transparent;
}

.site-header-dropdown .el-dropdown-menu__item {
  min-height: 38px;
  padding: 0 12px;
  color: var(--oa-text-soft);
  border-radius: 9px;
  font-size: 13px;
}

.site-header-dropdown .el-dropdown-menu__item:not(.is-disabled):focus,
.site-header-dropdown .el-dropdown-menu__item:not(.is-disabled):hover {
  color: var(--oa-text);
  background: var(--oa-nav-hover-bg);
}
</style>
