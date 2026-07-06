<template>
  <ViewPage class="activation-page" :loading="loading">
    <!-- ============ 1. Hero ============ -->
    <section ref="heroRef" class="act-hero">
      <div aria-hidden="true" class="act-hero__grid"></div>
      <div aria-hidden="true" class="act-hero__orb act-hero__orb--a"></div>
      <div aria-hidden="true" class="act-hero__orb act-hero__orb--b"></div>
      <div class="container act-hero__inner">
        <div class="act-hero__copy">
          <span class="act-mono-label act-hero__label" data-reveal>
            <i class="act-dot"></i>欢迎加入 · WELCOME
          </span>
          <h1 class="act-hero__title">
            <span class="act-hero__word" data-reveal-word style="--dw:0">恭喜你</span>
            <span class="act-hero__word act-hero__word--italic" data-reveal-word style="--dw:1">成为</span>
            <span class="act-hero__word" data-reveal-word style="--dw:2">正式成员</span>
          </h1>
          <p class="act-hero__desc" data-reveal style="--d:3">
            你已成为<strong>{{ info?.club?.name || '开放原子开源社团' }}</strong>的正式成员。
            向下滚动了解你的部门、社团核心负责人以及我们的使命与方向，浏览完成后激活账号。
          </p>
          <div class="act-hero__chip" data-reveal style="--d:4">
            <UserAvatar
              :name="displayName"
              :qq-openid="String(info?.qqOpenid || '')"
              :size="52"
              :src="String(info?.avatar || '')"
            />
            <div class="act-hero__chip-meta">
              <strong>{{ displayName }}</strong>
              <span>{{ info?.membership?.positionName || '正式成员' }}</span>
            </div>
            <i class="act-hero__chip-pulse"></i>
          </div>
        </div>
        <button class="act-scroll-cue" data-reveal style="--d:5" @click="scrollToNext">
          <span>SCROLL</span>
          <i></i>
        </button>
      </div>
      <div class="act-ticker" aria-hidden="true">
        <div class="act-ticker__track">
          <span v-for="n in 2" :key="n" class="act-ticker__group">
            <i v-for="word in tickerWords" :key="word">{{ word }}</i>
          </span>
        </div>
      </div>
    </section>

    <!-- ============ 2. Department ============ -->
    <section ref="sectionRefs" class="act-section act-section--department">
      <div class="container act-section__inner">
        <header class="act-head">
          <span class="act-mono-label act-head__label" data-stagger style="--si:0">Department / 01</span>
          <h2 class="act-head__title" data-stagger style="--si:1">我的部门</h2>
          <p class="act-head__desc" data-stagger style="--si:2">了解你所在的部门与负责人，从这里开始融入团队。</p>
        </header>
        <div class="act-divider" data-line></div>

        <div v-if="info?.membership" class="dept-layout">
          <div class="dept-layout__index" data-stagger style="--si:3">
            <span class="dept-layout__index-num">{{ membershipIndex }}</span>
            <span class="act-mono-label">YOUR DEPT</span>
          </div>
          <div class="dept-layout__main">
            <h3 class="dept-layout__name" data-stagger style="--si:4">
              {{ info.membership.departmentName || '尚未分配部门' }}
              <span v-if="info.membership.positionName" class="dept-layout__position">{{ info.membership.positionName }}</span>
            </h3>
            <p class="dept-layout__desc" data-stagger style="--si:5">
              {{ info.membership.departmentDescription || '部门介绍即将上线。' }}
            </p>
            <div v-if="info?.departmentHead" class="dept-head" data-stagger style="--si:6">
              <UserAvatar
                :name="info.departmentHead.name"
                :size="56"
                :src="String(info.departmentHead.avatar || '')"
                :fallback-src="info.departmentHead.qqAvatar || ''"
              />
              <div class="dept-head__meta">
                <span class="act-mono-label">DEPT LEAD · 部长</span>
                <strong>{{ info.departmentHead.name }}</strong>
              </div>
            </div>
            <div v-else class="dept-head dept-head--empty" data-stagger style="--si:6">
              <span class="act-mono-label">DEPT LEAD</span>
              <p>暂未设置部门部长，如有疑问请联系社长或管理员。</p>
            </div>
            <div v-if="info?.viceDepartmentHead" class="dept-head dept-head--vice" data-stagger style="--si:7">
              <UserAvatar
                :name="info.viceDepartmentHead.name"
                :size="56"
                :src="String(info.viceDepartmentHead.avatar || '')"
                :fallback-src="info.viceDepartmentHead.qqAvatar || ''"
              />
              <div class="dept-head__meta">
                <span class="act-mono-label">VICE LEAD · 副部长</span>
                <strong>{{ info.viceDepartmentHead.name }}</strong>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="dept-empty" data-stagger style="--si:3">
          <span class="act-mono-label">NOT ASSIGNED</span>
          <p>你尚未被分配到任何部门，请联系社长或管理员为你分配。</p>
        </div>

        <div v-if="info?.membership?.departmentWechatQrcode" class="dept-qrcode" data-stagger style="--si:8">
          <span class="act-mono-label">部门微信群</span>
          <img :src="info.membership.departmentWechatQrcode" alt="部门微信群二维码" class="dept-qrcode__img" />
          <p>扫码加入你所在部门的微信群</p>
        </div>
      </div>
    </section>

    <!-- ============ 3. Group Chat ============ -->
    <section v-if="info?.club?.wechatGroupQrcode || info?.club?.qqGroupNumber" class="act-section act-section--groups">
      <div class="container act-section__inner">
        <header class="act-head">
          <span class="act-mono-label act-head__label" data-stagger style="--si:0">Join Us / 03</span>
          <h2 class="act-head__title" data-stagger style="--si:1">加入群聊</h2>
          <p class="act-head__desc" data-stagger style="--si:2">扫码或搜索加入社团群，和大家一起交流。</p>
        </header>
        <div class="act-divider" data-line></div>

        <div class="group-grid">
          <div v-if="info?.club?.wechatGroupQrcode" class="group-cell" data-stagger style="--si:3">
            <span class="act-mono-label group-cell__label">社团总群 · 微信</span>
            <img :src="info.club.wechatGroupQrcode" alt="社团微信群二维码" class="group-cell__qrcode" />
            <p>打开微信扫码加入社团总群</p>
          </div>
          <div v-if="info?.club?.qqGroupNumber" class="group-cell" data-stagger style="--si:4">
            <span class="act-mono-label group-cell__label">社团总群 · QQ</span>
            <div class="group-cell__qq">
              <span class="group-cell__qq-num">{{ info.club.qqGroupNumber }}</span>
            </div>
            <p>搜索群号或保存二维码后扫码加入</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 4. Leadership ============ -->
    <section class="act-section act-section--leaders">
      <div aria-hidden="true" class="act-section__orb"></div>
      <div class="container act-section__inner">
        <header class="act-head">
          <span class="act-mono-label act-head__label" data-stagger style="--si:0">Leadership / 04</span>
          <h2 class="act-head__title" data-stagger style="--si:1">核心管理团队</h2>
          <p class="act-head__desc" data-stagger style="--si:2">社团的核心管理团队，将带你一起探索开源世界。</p>
        </header>
        <div class="act-divider" data-line></div>

        <div class="leaders">
          <article v-if="info?.president" class="leader leader--president" data-stagger style="--si:3">
            <div class="leader__avatar-wrap">
              <UserAvatar
                :name="info.president.name"
                :size="140"
                :src="String(info.president.avatar || '')"
                :fallback-src="info.president.qqAvatar || ''"
              />
              <span class="leader__ring"></span>
              <span class="leader__ring leader__ring--2"></span>
            </div>
            <span class="act-mono-label leader__tag">PRESIDENT · 社长</span>
            <h3 class="leader__name">{{ info.president.name }}</h3>
            <p class="leader__hint">社团负责人</p>
          </article>

          <article v-if="info?.leagueSecretary" class="leader leader--secretary" data-stagger style="--si:4">
            <div class="leader__avatar-wrap">
              <UserAvatar
                :name="info.leagueSecretary.name"
                :size="104"
                :src="String(info.leagueSecretary.avatar || '')"
                :fallback-src="info.leagueSecretary.qqAvatar || ''"
              />
              <span class="leader__ring"></span>
            </div>
            <span class="act-mono-label leader__tag leader__tag--secretary">SECRETARY · 团支书</span>
            <h3 class="leader__name">{{ info.leagueSecretary.name }}</h3>
            <p class="leader__hint">社团团务负责人</p>
          </article>

          <article
            v-for="(vp, idx) in info?.vicePresidents"
            :key="vp.userId"
            class="leader leader--vice"
            data-stagger
            :style="{ '--si': 5 + idx }"
          >
            <div class="leader__avatar-wrap">
              <UserAvatar
                :name="vp.name"
                :size="104"
                :src="String(vp.avatar || '')"
                :fallback-src="vp.qqAvatar || ''"
              />
              <span class="leader__ring"></span>
            </div>
            <span class="act-mono-label leader__tag leader__tag--vice">VICE · 副社长</span>
            <h3 class="leader__name">{{ vp.name }}</h3>
            <p class="leader__hint">社团副负责人</p>
          </article>

          <div
            v-if="!info?.president && !info?.leagueSecretary && (!info?.vicePresidents || !info.vicePresidents.length)"
            class="leader leader--empty"
            data-stagger
            style="--si:3"
          >
            <span class="act-mono-label">NOT SET</span>
            <p>暂未设置管理团队信息，请联系管理员完善。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 4a. About Intro ============ -->
    <section class="act-section act-section--about">
      <div class="container act-section__inner">
        <header class="act-head">
          <span class="act-mono-label act-head__label" data-stagger style="--si:0">About Us / 05</span>
          <h2 class="act-head__title" data-stagger style="--si:1">关于我们</h2>
          <p class="act-head__desc" data-stagger style="--si:2">从一群热爱技术的同学开始，到现在一起把开源做成日常。</p>
        </header>
        <div class="act-divider" data-line></div>

        <div class="about-intro">
          <div class="about-intro__brand" data-stagger style="--si:3">
            <img v-if="info?.club?.logoUrl" :src="info.club.logoUrl" alt="社团徽标" class="about-intro__logo" />
            <div>
              <span class="act-mono-label">CLUB</span>
              <strong>{{ info?.club?.name || '开放原子开源社团' }}</strong>
              <small v-if="info?.club?.category">{{ info.club.category }}</small>
            </div>
          </div>
          <p class="about-intro__lead" data-stagger style="--si:4">
            {{ info?.club?.description || '我们是江苏海事职业技术学院的开放原子开源社团，专注于技术分享、项目实践、竞赛训练与开源协作。' }}
          </p>
          <div class="about-intro__sig" data-stagger style="--si:5">
            <span>JMI · OPENATOM</span><i></i><span>EST. 2025</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 4b. Manifesto ============ -->
    <section class="act-section act-section--manifesto">
      <div aria-hidden="true" class="manifesto__bg"></div>
      <div class="container act-section__inner">
        <div class="manifesto">
          <span class="act-mono-label manifesto__label" data-stagger style="--si:0">Manifesto · 使命</span>
          <h2 class="manifesto__statement">
            <span class="manifesto__word" data-stagger style="--si:1">让</span>
            <span class="manifesto__word manifesto__word--accent" data-stagger style="--si:2">开源</span>
            <span class="manifesto__word" data-stagger style="--si:3">在校园里</span>
            <span class="manifesto__word manifesto__word--accent" data-stagger style="--si:4">真正发生</span>
          </h2>
          <p class="manifesto__detail" data-stagger style="--si:5">
            我们相信开源不仅是一种协作方式，更是一种学习姿态。在这里，每一行代码都被认真审视，
            每一次提交都值得被讨论，每一个项目都从真实需求出发。我们不追求一蹴而就的成果，
            而是在持续的实践与复盘中，把"做出有用的东西"变成日常。
          </p>
          <div class="manifesto__pillars">
            <div class="manifesto__pillar" data-stagger style="--si:6">
              <span class="manifesto__pillar-num">01</span>
              <strong>开放协作</strong>
              <p>代码、文档、思路全部公开，让每个人都能参与和讨论。</p>
            </div>
            <div class="manifesto__pillar" data-stagger style="--si:7">
              <span class="manifesto__pillar-num">02</span>
              <strong>工程实践</strong>
              <p>用真实项目训练工程能力，从需求到上线全流程参与。</p>
            </div>
            <div class="manifesto__pillar" data-stagger style="--si:8">
              <span class="manifesto__pillar-num">03</span>
              <strong>持续成长</strong>
              <p>不与他人比较，只与昨天的自己比较，保持长期主义。</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 4c. What We Do ============ -->
    <section class="act-section act-section--what">
      <div class="container act-section__inner">
        <header class="act-head">
          <span class="act-mono-label act-head__label" data-stagger style="--si:0">What We Do / 04</span>
          <h2 class="act-head__title" data-stagger style="--si:1">我们做什么</h2>
          <p class="act-head__desc" data-stagger style="--si:2">四个方向，构成社团日常的全部内容。</p>
        </header>
        <div class="act-divider" data-line></div>

        <ol class="what">
          <li v-for="(item, idx) in whatWeDo" :key="item.title" class="what__row" data-stagger :style="{ '--si': idx + 3 }">
            <span class="what__num">{{ String(idx + 1).padStart(2, '0') }}</span>
            <div class="what__body">
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
            </div>
            <span class="what__tag act-mono-label">{{ item.tag }}</span>
            <span class="what__line" data-line></span>
          </li>
        </ol>
      </div>
    </section>

    <!-- ============ 4d. Stats ============ -->
    <section class="act-section act-section--stats">
      <div aria-hidden="true" class="stats__bg"></div>
      <div class="container act-section__inner">
        <header class="act-head">
          <span class="act-mono-label act-head__label" data-stagger style="--si:0">By Numbers / 05</span>
          <h2 class="act-head__title" data-stagger style="--si:1">关键数字</h2>
          <p class="act-head__desc" data-stagger style="--si:2">用数字看社团的现在，也期待你成为下一个数字。</p>
        </header>
        <div class="act-divider" data-line></div>

        <div class="stats">
          <div v-for="(stat, idx) in statsItems" :key="stat.label" class="stats__cell" data-stagger :style="{ '--si': idx + 3 }">
            <span class="act-mono-label">{{ stat.label }}</span>
            <strong class="stats__value" :class="{ 'stats__value--accent': stat.accent }" :data-count="stat.count ?? false">{{ stat.display }}</strong>
            <small>{{ stat.note }}</small>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 4e. Departments ============ -->
    <section v-if="info?.club?.focusAreas?.length" class="act-section act-section--focus">
      <div class="container act-section__inner">
        <header class="act-head">
          <span class="act-mono-label act-head__label" data-stagger style="--si:0">Departments / 06</span>
          <h2 class="act-head__title" data-stagger style="--si:1">各部门方向</h2>
          <p class="act-head__desc" data-stagger style="--si:2">社团由若干部门组成，找到你感兴趣的方向。</p>
        </header>
        <div class="act-divider" data-line></div>

        <ol class="focus">
          <li v-for="(area, idx) in info.club.focusAreas" :key="area.title" class="focus__row" data-stagger :style="{ '--si': idx + 3 }">
            <span class="focus__num">{{ String(idx + 1).padStart(2, '0') }}</span>
            <div class="focus__body">
              <h3>{{ area.title }}</h3>
              <p>{{ area.description || '这个部门正在书写自己的故事。' }}</p>
            </div>
            <span class="focus__line" data-line></span>
          </li>
        </ol>
      </div>
    </section>

    <!-- ============ 5. CTA ============ -->
    <section class="act-cta">
      <div aria-hidden="true" class="act-cta__grid"></div>
      <div aria-hidden="true" class="act-cta__orb"></div>
      <div class="container act-cta__inner">
        <span class="act-mono-label act-cta__label" data-stagger style="--si:0">Ready / 07</span>
        <h2 class="act-cta__title" data-stagger style="--si:1">
          {{ info?.activated ? '账号已激活' : '准备好开始了吗？' }}
        </h2>
        <p data-stagger style="--si:2">
          {{
            info?.activated
              ? '你的账号已经激活，可以正常使用所有功能。'
              : '点击下方按钮激活账号，开启你在社团的所有权限与功能。'
          }}
        </p>
        <el-button
          :loading="submitting"
          class="act-cta__btn"
          size="large"
          type="primary"
          data-stagger
          style="--si:3"
          @click="handleActivate"
        >
          {{ info?.activated ? '进入主页' : '激活账号' }}
        </el-button>
        <small class="act-cta__hint" data-stagger style="--si:4">
          点击激活后即代表你已了解社团信息，账号将完成激活并可正常使用所有功能。
        </small>
      </div>
    </section>

    <el-dialog
      v-model="passwordVisible"
      title="激活账号 - 修改密码"
      width="440px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="90px"
        @submit.prevent="submitPassword"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="请设置至少4位的新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">稍后再说</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitPassword">
          确认修改并激活
        </el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script lang="ts" setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ViewPage from '@/components/common/ViewPage.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { authApi, siteApi } from '@/api'
import { setSession, getToken } from '@/utils/auth.ts'

interface Leader {
  userId: number
  name: string
  initial: string
  role: string
  avatar?: string
  qqAvatar?: string
}

interface MembershipInfo {
  clubId: number
  departmentId: number | null
  departmentName: string | null
  departmentDescription: string | null
  positionId: number | null
  positionName: string | null
  status: string
  joinedAt: string
}

interface ClubInfo {
  id: number
  name: string
  code: string
  category?: string
  description?: string
  logoUrl?: string
  focusAreas: Array<{ title: string; description: string }>
}

interface ActivationInfo {
  userId: number
  userName: string
  realName: string
  avatar?: string
  qqOpenid?: string
  activated: boolean
  activatedAt?: string
  membership?: MembershipInfo | null
  departmentHead?: Leader | null
  viceDepartmentHead?: Leader | null
  president?: Leader | null
  vicePresidents?: Leader[]
  leagueSecretary?: Leader | null
  club?: ClubInfo
}

const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const info = ref<ActivationInfo | null>(null)
const heroRef = ref<HTMLElement>()

const passwordVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref<any>()
const passwordForm = ref({ oldPassword: '', newPassword: '' })
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 4, message: '密码至少4位', trigger: 'blur' },
  ],
}
let revealObserver: IntersectionObserver | undefined
let parallaxFrame = 0

const tickerWords = ['OPEN SOURCE', '技术分享', '项目实践', '竞赛训练', '开源协作', 'JMI OPENATOM', 'EST. 2025']

const whatWeDo = [
  { title: '技术分享', desc: '定期内部分享会，从基础工具到前沿话题，由成员轮流主讲。每一次分享都是对自己知识的整理，也是对同伴的馈赠。', tag: 'Sharing' },
  { title: '项目实践', desc: '我们维护若干长期项目，覆盖 Web、Bot、工具链等方向。新成员从一个小 feature 起步，逐步承担更完整的模块。', tag: 'Building' },
  { title: '竞赛训练', desc: '组队参加各类程序设计、开源相关比赛，把日常积累转化为赛场发挥。赛后的复盘与沉淀，比奖牌本身更重要。', tag: 'Competing' },
  { title: '开源协作', desc: '鼓励成员向上游开源项目提交 PR、参与社区讨论，把校园里的练习延伸到更广阔的开源世界。', tag: 'Open Source' },
]

const displayName = computed(() => info.value?.realName || info.value?.userName || '新成员')

const membershipIndex = computed(() => {
  const id = info.value?.membership?.departmentId
  return id ? String(id).padStart(2, '0') : '01'
})

const statsItems = computed(() => [
  { label: 'EST · 成立', display: '2025', note: '从零开始的故事', accent: false },
  { label: 'DIRECTIONS · 方向', display: '4', note: '分享 · 项目 · 竞赛 · 开源', accent: false, count: 4 },
  { label: 'DEPTS · 部门', display: String(info.value?.club?.focusAreas?.length || '—'), note: '各司其职', accent: false, count: info.value?.club?.focusAreas?.length || 0 },
  { label: 'YOU · 你的位置', display: '+1', note: '正是此刻', accent: true },
])

async function fetchActivationInfo() {
  loading.value = true
  try {
    const result = await siteApi.activation()
    info.value = result as ActivationInfo
  } catch (error) {
    console.error('加载激活信息失败', error)
    ElMessage.error('激活信息加载失败，请刷新重试')
  } finally {
    loading.value = false
  }
}

function scrollToNext() {
  const sections = document.querySelectorAll('.act-section')
  const first = sections[0] as HTMLElement | undefined
  first?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function animateCount(el: HTMLElement) {
  const target = Number(el.dataset.count)
  if (!target || !Number.isFinite(target)) return
  const counter = { v: 0 }
  const duration = 1200
  const start = performance.now()
  function tick(now: number) {
    const p = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - p, 3)
    el.textContent = String(Math.round(target * eased))
    if (p < 1) requestAnimationFrame(tick)
    else el.textContent = String(target)
  }
  requestAnimationFrame(tick)
}

function setupReveal() {
  if (revealObserver) revealObserver.disconnect()
  const prefersReduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false

  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        const el = entry.target as HTMLElement
        el.classList.add('act-in')

        if (el.hasAttribute('data-reveal-word')) {
          el.classList.add('act-in')
        }
        if (el.hasAttribute('data-line')) {
          el.classList.add('act-line-in')
        }
        if (el.hasAttribute('data-count') && el.dataset.count !== 'false') {
          animateCount(el)
        }
        revealObserver?.unobserve(el)
      })
    },
    { rootMargin: '0px 0px -10% 0px', threshold: 0.12 },
  )

  const targets = document.querySelectorAll<HTMLElement>(
    '[data-reveal], [data-reveal-word], [data-stagger], [data-line], [data-count]',
  )
  targets.forEach((el) => {
    if (prefersReduced) {
      el.classList.add('act-in', 'act-line-in')
      return
    }
    revealObserver!.observe(el)
  })
}

function onScroll() {
  if (parallaxFrame) return
  parallaxFrame = requestAnimationFrame(() => {
    parallaxFrame = 0
    const y = window.scrollY
    const orbs = document.querySelectorAll<HTMLElement>('.act-hero__orb, .act-cta__orb, .act-section__orb')
    orbs.forEach((orb, i) => {
      orb.style.transform = `translate3d(0, ${y * (0.04 + i * 0.015)}px, 0)`
    })
  })
}

async function handleActivate() {
  if (info.value?.activated) {
    router.replace('/')
    return
  }
  passwordForm.value = { oldPassword: '', newPassword: '' }
  passwordVisible.value = true
}

async function submitPassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    const result = await authApi.activate()
    if (result) setSession({ accessToken: getToken() || undefined, user: result })

    await authApi.updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
    })

    ElMessage.success('密码修改成功，账号已激活，请使用新密码重新登录')
    passwordVisible.value = false
    window.location.href = '/login'
  } catch (error: any) {
    const msg = error?.message || error?.msg || '操作失败，请重试'
    ElMessage.error(msg)
  } finally {
    passwordLoading.value = false
  }
}

onMounted(async () => {
  await fetchActivationInfo()
  requestAnimationFrame(() => {
    setupReveal()
    window.addEventListener('scroll', onScroll, { passive: true })
  })
})

onBeforeUnmount(() => {
  if (revealObserver) revealObserver.disconnect()
  window.removeEventListener('scroll', onScroll)
  if (parallaxFrame) cancelAnimationFrame(parallaxFrame)
})
</script>

<style scoped>
/* ============ Base ============ */
.activation-page {
  min-height: 100vh;
  background: var(--oa-page-bg);
  color: var(--oa-text);
  font-family: 'SF Pro Display', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
}

.act-mono-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.22em;
  color: var(--oa-muted);
  text-transform: uppercase;
}

.act-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px color-mix(in srgb, #22c55e 20%, transparent);
  animation: actPulse 2.4s ease-in-out infinite;
}

@keyframes actPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); opacity: 0.6; }
}

/* ============ Reveal Animations ============ */
[data-reveal],
[data-reveal-word],
[data-stagger] {
  opacity: 0;
  transform: translateY(24px);
  transition:
    opacity 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.7s ease;
  filter: blur(6px);
  transition-delay: calc(var(--d, 0) * 0.08s + var(--si, 0) * 0.06s);
}

[data-reveal-word] {
  display: inline-block;
  transition-delay: calc(var(--dw, 0) * 0.12s);
}

.act-in {
  opacity: 1 !important;
  transform: translateY(0) !important;
  filter: blur(0) !important;
}

[data-line] {
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.9s cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: calc(var(--si, 0) * 0.05s);
}

.act-line-in {
  transform: scaleX(1) !important;
}

/* ============ 1. Hero ============ */
.act-hero {
  position: relative;
  min-height: 100svh;
  display: flex;
  align-items: center;
  overflow: hidden;
  isolation: isolate;
}

.act-hero__grid {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(color-mix(in srgb, var(--oa-text) 4%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--oa-text) 4%, transparent) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(ellipse at 50% 40%, rgba(0, 0, 0, 0.8), transparent 75%);
}

.act-hero__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  z-index: 0;
  will-change: transform;
}

.act-hero__orb--a {
  top: 10%;
  left: 8%;
  width: 340px;
  height: 340px;
  background: color-mix(in srgb, var(--oa-active-bg) 14%, transparent);
}

.act-hero__orb--b {
  bottom: 5%;
  right: 10%;
  width: 280px;
  height: 280px;
  background: color-mix(in srgb, var(--oa-text) 8%, transparent);
}

.act-hero__inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: clamp(100px, 14vh, 160px) 0 80px;
}

.act-hero__copy {
  display: grid;
  justify-items: center;
  gap: 24px;
  text-align: center;
  max-width: 720px;
}

.act-hero__label {
  padding: 6px 14px;
  border: 1px solid var(--oa-border);
  border-radius: 999px;
  background: var(--oa-elevated-bg);
}

.act-hero__title {
  margin: 0;
  font-size: clamp(44px, 8vw, 88px);
  font-weight: 600;
  line-height: 1.02;
  letter-spacing: -0.03em;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0 0.25em;
}

.act-hero__word {
  display: inline-block;
}

.act-hero__word--italic {
  font-style: italic;
  font-weight: 400;
  color: var(--oa-muted);
}

.act-hero__desc {
  margin: 0;
  max-width: 560px;
  color: var(--oa-text-soft);
  font-size: clamp(15px, 1.6vw, 17px);
  line-height: 1.8;
}

.act-hero__desc strong {
  color: var(--oa-text);
  font-weight: 600;
}

.act-hero__chip {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 20px 10px 10px;
  border-radius: 999px;
  background: var(--oa-elevated-bg);
  box-shadow: 0 12px 40px color-mix(in srgb, var(--oa-text) 10%, transparent);
  position: relative;
}

.act-hero__chip-meta {
  display: grid;
  text-align: left;
  gap: 2px;
}

.act-hero__chip-meta strong {
  font-size: 15px;
  color: var(--oa-text);
}

.act-hero__chip-meta span {
  font-size: 11px;
  color: var(--oa-muted);
  letter-spacing: 0.04em;
}

.act-hero__chip-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  margin-left: 4px;
  animation: actPulse 2s ease-in-out infinite;
}

.act-scroll-cue {
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--oa-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  letter-spacing: 0.3em;
  transition: color 0.2s;
}

.act-scroll-cue:hover {
  color: var(--oa-text);
}

.act-scroll-cue i {
  width: 1px;
  height: 32px;
  background: linear-gradient(var(--oa-text), transparent);
  animation: actScrollLine 2s ease-in-out infinite;
}

@keyframes actScrollLine {
  0% { transform: scaleY(0); transform-origin: top; }
  50% { transform: scaleY(1); transform-origin: top; }
  51% { transform: scaleY(1); transform-origin: bottom; }
  100% { transform: scaleY(0); transform-origin: bottom; }
}

/* Ticker */
.act-ticker {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 0;
  overflow: hidden;
  border-top: 1px solid var(--oa-border);
  border-bottom: 1px solid var(--oa-border);
  background: var(--oa-elevated-bg);
  padding: 10px 0;
}

.act-ticker__track {
  display: flex;
  gap: 32px;
  white-space: nowrap;
  animation: actTicker 28s linear infinite;
}

.act-ticker__group {
  display: flex;
  gap: 32px;
  flex-shrink: 0;
}

.act-ticker__group i {
  font-style: normal;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  letter-spacing: 0.2em;
  color: var(--oa-muted);
}

.act-ticker__group i::after {
  content: ' / ';
  color: var(--oa-border-strong);
}

@keyframes actTicker {
  to { transform: translateX(-50%); }
}

/* ============ Generic Section ============ */
.act-section {
  position: relative;
  min-height: 100svh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(80px, 12vh, 140px) 0;
  isolation: isolate;
  overflow: hidden;
}

.act-section--department { background: var(--oa-page-bg); }
.act-section--leaders { background: var(--oa-page-soft-bg); }
.act-section--about { background: var(--oa-page-bg); }
.act-section--manifesto { background: var(--oa-page-soft-bg); }
.act-section--what { background: var(--oa-page-bg); }
.act-section--stats { background: var(--oa-page-soft-bg); }
.act-section--focus { background: var(--oa-page-bg); }

.act-section__orb {
  position: absolute;
  top: 20%;
  right: -100px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--oa-active-bg) 10%, transparent);
  filter: blur(70px);
  z-index: 0;
  will-change: transform;
}

.act-section__inner {
  position: relative;
  z-index: 1;
}

/* Section heading */
.act-head {
  display: grid;
  gap: 14px;
  max-width: 720px;
}

.act-head__label {
  width: fit-content;
}

.act-head__title {
  margin: 0;
  font-size: clamp(36px, 6vw, 64px);
  font-weight: 600;
  line-height: 1.05;
  letter-spacing: -0.025em;
  color: var(--oa-text);
}

.act-head__title em {
  font-style: italic;
  font-weight: 400;
  color: var(--oa-muted);
}

.act-head__desc {
  margin: 0;
  color: var(--oa-text-soft);
  font-size: clamp(15px, 1.6vw, 17px);
  line-height: 1.7;
}

/* Divider */
.act-divider {
  height: 1px;
  background: var(--oa-border);
  margin: clamp(40px, 5vw, 60px) 0;
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.9s cubic-bezier(0.22, 1, 0.36, 1);
}

.act-divider.act-line-in {
  transform: scaleX(1);
}

/* ============ 2. Department ============ */
.dept-layout {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: clamp(32px, 5vw, 64px);
  align-items: start;
}

.dept-layout__index {
  position: sticky;
  top: 120px;
  display: grid;
  gap: 8px;
}

.dept-layout__index-num {
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: clamp(64px, 8vw, 96px);
  font-weight: 500;
  line-height: 0.9;
  color: var(--oa-text);
  letter-spacing: -0.04em;
}

.dept-layout__main {
  display: grid;
  gap: 28px;
}

.dept-layout__name {
  margin: 0;
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 600;
  line-height: 1.15;
  color: var(--oa-text);
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 16px;
}

.dept-layout__position {
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
  letter-spacing: 0.04em;
}

.dept-layout__desc {
  margin: 0;
  max-width: 580px;
  color: var(--oa-text-soft);
  font-size: clamp(15px, 1.7vw, 18px);
  line-height: 1.9;
}

.dept-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 20px;
  border-top: 1px solid var(--oa-border);
}

.dept-head__meta {
  display: grid;
  gap: 4px;
}

.dept-head__meta strong {
  font-size: 18px;
  color: var(--oa-text);
}

.dept-head--empty {
  display: grid;
  gap: 8px;
}

.dept-head--empty p {
  margin: 0;
  color: var(--oa-muted);
  font-size: 14px;
}

.dept-empty {
  display: grid;
  gap: 12px;
  max-width: 480px;
}

.dept-empty p {
  margin: 0;
  color: var(--oa-text-soft);
  font-size: 16px;
  line-height: 1.7;
}

@media (max-width: 720px) {
  .dept-layout { grid-template-columns: 1fr; gap: 24px; }
  .dept-layout__index { position: static; }
  .dept-layout__index-num { font-size: 56px; }
}

/* ============ 3. Leadership ============ */
.leaders {
  display: flex;
  flex-wrap: wrap;
  gap: clamp(40px, 6vw, 80px);
  align-items: flex-start;
}

.leader {
  display: grid;
  gap: 16px;
  justify-items: center;
  text-align: center;
}

.leader--president { flex: 1.3 1 360px; }
.leader--vice { flex: 1 1 280px; padding-top: 40px; }

.leader__avatar-wrap {
  position: relative;
  display: grid;
  place-items: center;
}

.leader__ring {
  position: absolute;
  inset: -16px;
  border: 1px solid var(--oa-border);
  border-radius: 50%;
}

.leader__ring--2 {
  inset: -32px;
  border-style: dashed;
  opacity: 0.4;
  animation: actRingSpin 24s linear infinite;
}

@keyframes actRingSpin {
  to { transform: rotate(360deg); }
}

.leader__tag {
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
}

.leader__tag--vice {
  background: color-mix(in srgb, var(--oa-text) 10%, transparent);
  color: var(--oa-text);
}

.leader__name {
  margin: 0;
  font-size: clamp(22px, 2.8vw, 30px);
  font-weight: 600;
  color: var(--oa-text);
}

.leader__hint {
  margin: 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.leader--empty {
  text-align: left;
  justify-items: start;
}

.leader--empty p {
  margin: 0;
  color: var(--oa-muted);
}

/* ============ 4a. About ============ */
.about-intro {
  display: grid;
  gap: 40px;
  max-width: 760px;
}

.about-intro__brand {
  display: flex;
  align-items: center;
  gap: 20px;
}

.about-intro__logo {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  object-fit: cover;
}

.about-intro__brand > div {
  display: grid;
  gap: 4px;
}

.about-intro__brand strong {
  font-size: clamp(22px, 2.6vw, 28px);
  font-weight: 600;
  color: var(--oa-text);
}

.about-intro__brand small {
  color: var(--oa-muted);
  font-size: 13px;
}

.about-intro__lead {
  margin: 0;
  font-size: clamp(18px, 2.4vw, 26px);
  font-weight: 400;
  line-height: 1.55;
  letter-spacing: -0.005em;
  color: var(--oa-text);
}

.about-intro__sig {
  display: flex;
  align-items: center;
  gap: 16px;
  color: var(--oa-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.2em;
}

.about-intro__sig i {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, var(--oa-border), transparent);
}

/* ============ 4b. Manifesto ============ */
.manifesto__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(ellipse at 25% 30%, color-mix(in srgb, var(--oa-active-bg) 12%, transparent), transparent 50%),
    radial-gradient(ellipse at 75% 70%, color-mix(in srgb, var(--oa-text) 8%, transparent), transparent 50%);
}

.manifesto__bg::before {
  position: absolute;
  inset: 0;
  content: '';
  background:
    linear-gradient(color-mix(in srgb, var(--oa-text) 3%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--oa-text) 3%, transparent) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: radial-gradient(ellipse at 50% 50%, rgba(0, 0, 0, 0.5), transparent 75%);
}

.manifesto {
  position: relative;
  z-index: 1;
  max-width: 880px;
  display: grid;
  gap: 32px;
}

.manifesto__label {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--oa-border);
}

.manifesto__statement {
  margin: 0;
  font-size: clamp(40px, 7vw, 84px);
  font-weight: 600;
  line-height: 1.04;
  letter-spacing: -0.03em;
  display: flex;
  flex-wrap: wrap;
  gap: 0 0.22em;
}

.manifesto__word {
  display: inline-block;
}

.manifesto__word--accent {
  font-style: italic;
  font-weight: 500;
  background: linear-gradient(120deg, var(--oa-text), color-mix(in srgb, var(--oa-text) 35%, var(--oa-active-bg)));
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  padding-right: 0.18em;
  background-origin: padding-box;
}

.manifesto__detail {
  margin: 0;
  max-width: 680px;
  color: var(--oa-text-soft);
  font-size: clamp(15px, 1.7vw, 18px);
  line-height: 1.95;
}

.manifesto__pillars {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-top: 1px solid var(--oa-border);
  border-bottom: 1px solid var(--oa-border);
}

.manifesto__pillar {
  padding: clamp(44px, 6vw, 64px) clamp(28px, 3vw, 40px) clamp(44px, 6vw, 64px) 0;
  display: grid;
  gap: 12px;
  border-right: 1px solid var(--oa-border);
}

.manifesto__pillar:last-child { border-right: none; }

.manifesto__pillar-num {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--oa-muted);
}

.manifesto__pillar strong {
  font-size: 18px;
  font-weight: 600;
}

.manifesto__pillar p {
  margin: 0;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 720px) {
  .manifesto__pillars { grid-template-columns: 1fr; }
  .manifesto__pillar {
    border-right: none;
    border-bottom: 1px solid var(--oa-border);
    padding: clamp(32px, 5vw, 44px) 0;
  }
  .manifesto__pillar:last-child { border-bottom: none; }
}

/* ============ 4c. What We Do ============ */
.what {
  list-style: none;
  margin: 0;
  padding: 0;
}

.what__row {
  display: grid;
  grid-template-columns: auto 1fr auto 64px;
  align-items: center;
  gap: 32px;
  padding: clamp(28px, 4vw, 44px) 0;
  border-bottom: 1px solid var(--oa-border);
  transition: padding-left 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}

.what__row:first-child { border-top: 1px solid var(--oa-border); }
.what__row:hover { padding-left: 16px; }

.what__num {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: clamp(18px, 2.2vw, 24px);
  font-weight: 500;
  color: var(--oa-text);
  letter-spacing: 0.06em;
}

.what__body { display: grid; gap: 10px; }

.what__body h3 {
  margin: 0;
  font-size: clamp(20px, 2.6vw, 26px);
  font-weight: 600;
  color: var(--oa-text);
}

.what__body p {
  margin: 0;
  max-width: 560px;
  color: var(--oa-text-soft);
  font-size: 14px;
  line-height: 1.8;
}

.what__tag {
  white-space: nowrap;
}

.what__line {
  height: 1px;
  background: var(--oa-text);
  transform-origin: left;
}

@media (max-width: 640px) {
  .what__row {
    grid-template-columns: auto 1fr;
    gap: 16px 20px;
  }
  .what__tag, .what__line { grid-column: 2; }
  .what__line { display: none; }
}

/* ============ 4d. Stats ============ */
.stats__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(circle at 20% 50%, color-mix(in srgb, var(--oa-active-bg) 10%, transparent), transparent 40%),
    radial-gradient(circle at 80% 50%, color-mix(in srgb, var(--oa-text) 7%, transparent), transparent 40%);
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid var(--oa-border);
  border-bottom: 1px solid var(--oa-border);
}

.stats__cell {
  display: grid;
  gap: 12px;
  padding: clamp(36px, 5vw, 56px) 24px;
  border-right: 1px solid var(--oa-border);
}

.stats__cell:last-child { border-right: none; }

.stats__value {
  font-size: clamp(40px, 6vw, 64px);
  font-weight: 500;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--oa-text);
}

.stats__value--accent {
  background: linear-gradient(120deg, var(--oa-text), color-mix(in srgb, var(--oa-text) 40%, var(--oa-active-bg)));
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-style: italic;
}

.stats__cell small {
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 760px) {
  .stats { grid-template-columns: repeat(2, 1fr); }
  .stats__cell:nth-child(2n) { border-right: none; }
  .stats__cell:nth-child(-n+2) { border-bottom: 1px solid var(--oa-border); }
}

@media (max-width: 440px) {
  .stats { grid-template-columns: 1fr; }
  .stats__cell {
    border-right: none;
    border-bottom: 1px solid var(--oa-border);
  }
  .stats__cell:last-child { border-bottom: none; }
}

/* ============ 4e. Focus ============ */
.focus {
  list-style: none;
  margin: 0;
  padding: 0;
}

.focus__row {
  display: grid;
  grid-template-columns: auto 1fr 48px;
  align-items: center;
  gap: 28px;
  padding: clamp(24px, 3.5vw, 36px) 0;
  border-bottom: 1px solid var(--oa-border);
  transition: padding-left 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}

.focus__row:first-child { border-top: 1px solid var(--oa-border); }
.focus__row:hover { padding-left: 12px; }

.focus__num {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: clamp(16px, 2vw, 20px);
  font-weight: 500;
  letter-spacing: 0.06em;
  color: var(--oa-text);
}

.focus__body { display: grid; gap: 8px; }

.focus__body h3 {
  margin: 0;
  font-size: clamp(18px, 2.2vw, 22px);
  font-weight: 600;
}

.focus__body p {
  margin: 0;
  max-width: 520px;
  color: var(--oa-text-soft);
  font-size: 14px;
  line-height: 1.8;
}

.focus__line {
  height: 1px;
  background: var(--oa-text);
  transform-origin: left;
}

@media (max-width: 560px) {
  .focus__row { grid-template-columns: auto 1fr; gap: 16px 20px; }
  .focus__line { display: none; }
}

/* ============ 5. CTA ============ */
.act-cta {
  position: relative;
  min-height: 100svh;
  display: flex;
  align-items: center;
  overflow: hidden;
  isolation: isolate;
}

.act-cta__grid {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(color-mix(in srgb, var(--oa-text) 4%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--oa-text) 4%, transparent) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(circle at 50% 50%, rgba(0, 0, 0, 0.6), transparent 70%);
}

.act-cta__orb {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--oa-active-bg) 14%, transparent), transparent 65%);
  filter: blur(40px);
  z-index: 0;
  will-change: transform;
}

.act-cta__inner {
  position: relative;
  z-index: 1;
  display: grid;
  max-width: 580px;
  margin: 0 auto;
  gap: 20px;
  justify-items: center;
  text-align: center;
  padding: clamp(100px, 14vh, 160px) 0;
}

.act-cta__label {
  padding: 6px 14px;
  border: 1px solid var(--oa-border);
  border-radius: 999px;
  background: var(--oa-elevated-bg);
}

.act-cta__title {
  margin: 6px 0 10px;
  font-size: clamp(34px, 5vw, 52px);
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: -0.025em;
}

.act-cta__inner p {
  margin: 0;
  max-width: 460px;
  color: var(--oa-text-soft);
  font-size: 16px;
  line-height: 1.8;
}

.act-cta__btn {
  margin-top: 24px;
  min-width: 220px;
  height: 54px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.02em;
  border-radius: 999px;
}

.act-cta__hint {
  margin-top: 12px;
  max-width: 380px;
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1.6;
}

.dept-qrcode {
  margin-top: 32px;
  padding: 24px;
  border: 1px solid var(--oa-border);
  border-radius: 12px;
  background: var(--oa-surface);
  text-align: center;
}

.dept-qrcode__img {
  display: block;
  width: 280px;
  height: 280px;
  margin: 16px auto;
  border-radius: 12px;
  object-fit: contain;
}

.dept-qrcode p {
  font-size: 13px;
  color: var(--oa-muted);
}

.act-section--groups {
  padding-top: 60px;
  padding-bottom: 80px;
  background: var(--oa-bg);
}

.group-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 40px;
  margin-top: 40px;
}

.group-cell {
  text-align: center;
  padding: 40px 32px;
  border: 1px solid var(--oa-border);
  border-radius: 16px;
  background: var(--oa-surface);
}

.group-cell__label {
  display: inline-block;
  margin-bottom: 24px;
}

.group-cell__qrcode {
  display: block;
  width: 280px;
  height: 280px;
  margin: 0 auto 20px;
  border-radius: 12px;
  object-fit: contain;
}

.group-cell__qq {
  width: 280px;
  height: 140px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f6ff;
  border: 2px dashed #a0c4ff;
  border-radius: 12px;
}

.group-cell__qq-num {
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #2563eb;
  font-family: monospace;
}

.group-cell p {
  font-size: 14px;
  color: var(--oa-muted);
}
</style>
