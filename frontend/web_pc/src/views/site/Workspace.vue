<template>
  <ViewPage class="workspace-page personal-workspace">
    <WorkspaceHero
      eyebrow="个人工作台"
      :title="`${displayName}，欢迎回来`"
      description="把申请、通知、活动参与和个人工具集中在一个入口，不必再到导航栏里逐项寻找。"
      :metrics="workspaceMetrics"
    >
      <template #actions>
        <el-button type="primary" @click="$router.push('/progress')">查看申请进度</el-button>
        <el-button plain @click="$router.push('/profile')">进入我的主页</el-button>
      </template>
    </WorkspaceHero>

    <section class="workspace-section">
      <div class="container personal-workspace__layout">
        <WorkspacePanel
          eyebrow="Today"
          title="当前待办"
          description="优先处理需要关注的申请状态和未读通知。"
        >
          <div class="workspace-task-grid">
            <router-link class="workspace-task" to="/progress">
              <div class="workspace-task__icon"><DocumentChecked /></div>
              <div>
                <span>入会申请</span>
                <strong>{{
                  activeApplicationCount ? `${activeApplicationCount} 条处理中` : '查看申请记录'
                }}</strong>
                <small>审核、面试和结果统一在这里跟进</small>
              </div>
              <ArrowRight class="workspace-task__arrow" />
            </router-link>

            <router-link class="workspace-task" to="/notifications">
              <div class="workspace-task__icon"><Bell /></div>
              <div>
                <span>通知中心</span>
                <strong>{{ unreadCount ? `${unreadCount} 条未读` : '暂无未读消息' }}</strong>
                <small>查看系统通知和业务状态变化</small>
              </div>
              <ArrowRight class="workspace-task__arrow" />
            </router-link>

            <router-link class="workspace-task" to="/leaves">
              <div class="workspace-task__icon"><Calendar /></div>
              <div>
                <span>我的请假</span>
                <strong>提交或查询进度</strong>
                <small>请假申请、审批状态和历史记录</small>
              </div>
              <ArrowRight class="workspace-task__arrow" />
            </router-link>
          </div>
        </WorkspacePanel>

        <div class="workspace-grid workspace-grid--split personal-workspace__groups">
          <WorkspacePanel eyebrow="Participation" title="参与社团">
            <nav aria-label="参与社团" class="workspace-link-list">
              <router-link v-for="item in participationLinks" :key="item.to" :to="item.to">
                <component :is="item.icon" />
                <span
                  ><strong>{{ item.title }}</strong
                  ><small>{{ item.description }}</small></span
                >
                <ArrowRight />
              </router-link>
            </nav>
          </WorkspacePanel>

          <WorkspacePanel eyebrow="Identity" title="身份与账号">
            <nav aria-label="身份与账号" class="workspace-link-list">
              <router-link v-for="item in identityLinks" :key="item.to" :to="item.to">
                <component :is="item.icon" />
                <span
                  ><strong>{{ item.title }}</strong
                  ><small>{{ item.description }}</small></span
                >
                <ArrowRight />
              </router-link>
            </nav>
          </WorkspacePanel>

          <WorkspacePanel eyebrow="Creation" title="内容与工具">
            <nav aria-label="内容与工具" class="workspace-link-list">
              <router-link v-for="item in creationLinks" :key="item.to" :to="item.to">
                <component :is="item.icon" />
                <span
                  ><strong>{{ item.title }}</strong
                  ><small>{{ item.description }}</small></span
                >
                <ArrowRight />
              </router-link>
            </nav>
          </WorkspacePanel>

          <WorkspacePanel eyebrow="Discover" title="浏览与发现">
            <nav aria-label="浏览与发现" class="workspace-link-list">
              <router-link v-for="item in discoverLinks" :key="item.to" :to="item.to">
                <component :is="item.icon" />
                <span
                  ><strong>{{ item.title }}</strong
                  ><small>{{ item.description }}</small></span
                >
                <ArrowRight />
              </router-link>
            </nav>
          </WorkspacePanel>
        </div>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import WorkspaceHero from '@/components/site/workspace/WorkspaceHero.vue'
import WorkspacePanel from '@/components/site/workspace/WorkspacePanel.vue'
import { notificationApi, siteApi } from '@/api'
import { getCurrentUser } from '@/utils/auth.ts'
import {
  ArrowRight,
  Bell,
  Calendar,
  Coin,
  DocumentChecked,
  EditPen,
  Picture,
  Setting,
  Tickets,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { computed, markRaw, onMounted, ref } from 'vue'

type WorkspaceLink = {
  title: string
  description: string
  to: string
  icon: unknown
}

const user = ref(getCurrentUser())
const unreadCount = ref(0)
const applications = ref<any[]>([])

const activeStatuses = new Set([
  'submitted',
  'reviewing',
  'approved',
  'pre_screen_passed',
  'interview_scheduled',
  'interviewed',
  'waitlisted',
])

const displayName = computed(() => user.value.realName || user.value.userName || '同学')
const activeApplicationCount = computed(
  () => applications.value.filter((item) => activeStatuses.has(item.status)).length,
)
const workspaceMetrics = computed(() => [
  { label: '处理中申请', value: activeApplicationCount.value, note: '入会流程' },
  { label: '未读通知', value: unreadCount.value, note: '待查看' },
  { label: '常用入口', value: 12, note: '集中管理' },
])

const participationLinks: WorkspaceLink[] = [
  {
    title: '活动中心',
    description: '查看活动详情与参与方式',
    to: '/activities',
    icon: markRaw(Calendar),
  },
  { title: '投票', description: '参与当前开放的社团投票', to: '/votes', icon: markRaw(Tickets) },
  { title: '积分中心', description: '积分排行、明细与兑换', to: '/points', icon: markRaw(Coin) },
]

const identityLinks: WorkspaceLink[] = [
  { title: '我的主页', description: '查看公开成员主页', to: '/profile', icon: markRaw(UserFilled) },
  {
    title: '编辑公开主页',
    description: '维护公开展示内容',
    to: '/profile/edit',
    icon: markRaw(User),
  },
  {
    title: '账号与安全',
    description: '头像、密码和账号绑定',
    to: '/settings/account',
    icon: markRaw(Setting),
  },
]

const creationLinks: WorkspaceLink[] = [
  {
    title: '我的博客',
    description: '撰写、编辑和管理文章',
    to: '/blog/my',
    icon: markRaw(EditPen),
  },
  { title: '图床', description: '上传和管理内容图片', to: '/images', icon: markRaw(Picture) },
]

const discoverLinks: WorkspaceLink[] = [
  {
    title: '成员名录',
    description: '查找社团成员与个人主页',
    to: '/members',
    icon: markRaw(UserFilled),
  },
  {
    title: '校历',
    description: '查看学校教学与活动日历',
    to: '/calendar',
    icon: markRaw(Calendar),
  },
]

async function loadWorkspace() {
  const [progressResult, unreadResult] = await Promise.allSettled([
    siteApi.progress(),
    notificationApi.unreadCount(),
  ])
  if (progressResult.status === 'fulfilled') {
    const value = progressResult.value
    applications.value = value?.applications || value?.list || value || []
  }
  if (unreadResult.status === 'fulfilled') {
    unreadCount.value = typeof unreadResult.value === 'number' ? unreadResult.value : 0
  }
}

onMounted(loadWorkspace)
</script>

<style scoped>
.personal-workspace__layout {
  display: grid;
  gap: 28px;
}

.workspace-task-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.workspace-task {
  display: grid;
  min-height: 132px;
  grid-template-columns: 44px minmax(0, 1fr) 20px;
  align-items: start;
  gap: 14px;
  padding: 18px;
  color: var(--oa-text);
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  background: var(--oa-page-soft-bg);
  text-decoration: none;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    transform 0.18s ease;
}

.workspace-task:hover,
.workspace-task:focus-visible,
.workspace-link-list a:hover,
.workspace-link-list a:focus-visible {
  border-color: var(--oa-border-strong);
  background: var(--oa-nav-hover-bg);
  outline: none;
}

.workspace-task:hover {
  transform: translateY(-2px);
}

.workspace-task__icon {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border: 1px solid var(--oa-border);
  border-radius: 13px;
  background: var(--oa-elevated-bg);
}

.workspace-task__icon svg,
.workspace-task__arrow,
.workspace-link-list svg {
  width: 20px;
  height: 20px;
}

.workspace-task span,
.workspace-task strong,
.workspace-task small,
.workspace-link-list strong,
.workspace-link-list small {
  display: block;
}

.workspace-task span {
  color: var(--oa-muted);
  font-size: 12px;
}

.workspace-task strong {
  margin-top: 7px;
  font-size: 16px;
}

.workspace-task small,
.workspace-link-list small {
  margin-top: 6px;
  color: var(--oa-muted);
  line-height: 1.5;
}

.workspace-task__arrow {
  margin-top: 12px;
  color: var(--oa-muted);
}

.personal-workspace__groups {
  align-items: start;
}

.workspace-link-list {
  display: grid;
  gap: 8px;
}

.workspace-link-list a {
  display: grid;
  min-height: 72px;
  grid-template-columns: 28px minmax(0, 1fr) 20px;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  color: var(--oa-text);
  border: 1px solid transparent;
  border-radius: 14px;
  text-decoration: none;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.workspace-link-list strong {
  font-size: 15px;
}

.workspace-link-list > a > svg:last-child {
  color: var(--oa-muted);
}

@media (max-width: 900px) {
  .workspace-task-grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .workspace-task,
  .workspace-link-list a {
    transition: none;
  }

  .workspace-task:hover {
    transform: none;
  }
}
</style>
