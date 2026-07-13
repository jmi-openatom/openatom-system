<template>
  <div ref="pageRoot" class="about-page">
    <main>
      <section id="top" class="about-hero">
        <img
          class="about-hero__image"
          src="/bg.png"
          alt="开放原子开源社团成员共同参与校园开源实践"
          decoding="async"
          fetchpriority="high"
        />
        <div class="about-hero__veil" aria-hidden="true"></div>
        <div class="about-shell about-hero__content">
          <p class="apple-kicker">关于我们 · JMI-OPENATOM</p>
          <h1><span>从校园出发。</span><span>向开源前行。</span></h1>
          <p class="about-hero__lead">
            不只是学习技术。我们一起发现问题、完成作品，也把每一次成长留在开放社区里。
          </p>
          <div class="about-hero__actions">
            <button
              class="apple-button apple-button--light"
              type="button"
              @click="scrollTo('routes')"
            >
              认识我们的路线
              <ArrowDown :size="17" />
            </button>
            <button class="apple-link apple-link--light" type="button" @click="goHome">
              返回官网
              <ArrowUpRight :size="17" />
            </button>
          </div>
          <div class="about-hero__metrics" aria-label="社团关键数据">
            <div v-for="item in heroMetrics" :key="item.label">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </div>
        <button class="about-hero__scroll" type="button" @click="scrollTo('story')">
          继续了解
          <ChevronDown :size="16" />
        </button>
      </section>

      <section id="story" class="about-section about-story">
        <div class="about-shell">
          <div class="section-heading section-reveal">
            <p class="apple-kicker">WHY WE STARTED</p>
            <h2>开源筑梦，<br /><span>海事启航。</span></h2>
            <p>让每一位成员在真实项目中成长，在开放协作中找到自己的位置。</p>
          </div>

          <div class="story-grid">
            <article class="story-card story-card--year reveal-card">
              <span>EST.</span>
              <strong>2025</strong>
              <p>一群人，一个社区，从零开始。</p>
            </article>
            <article class="story-card story-card--statement reveal-card">
              <Lightbulb :size="28" stroke-width="1.5" />
              <h3>先做出来，再一起做得更好。</h3>
              <p>我们相信，真正的成长发生在问题被看见、任务被接住、成果被分享的过程里。</p>
            </article>
          </div>
        </div>
      </section>

      <section id="routes" class="about-section routes-section">
        <div class="about-shell route-story">
          <div class="route-story__intro">
            <p class="apple-kicker">FIND YOUR PLACE</p>
            <h2>五条路线。<br /><span>总有一条适合你。</span></h2>
            <p>不必先成为某一种人。继续向下，看看哪一段故事更像你。</p>

            <ol class="route-story__rail" aria-label="五条成长路线">
              <li v-for="(path, index) in paths" :key="path.title" class="route-story__rail-item">
                <span>0{{ index + 1 }}</span>
                <strong>{{ path.title }}</strong>
              </li>
            </ol>
          </div>

          <div class="route-story__stage">
            <div class="route-story__progress" aria-hidden="true"><span></span></div>
            <article v-for="(path, index) in paths" :key="path.title" class="route-scene">
              <span class="route-scene__number" aria-hidden="true">0{{ index + 1 }}</span>
              <div class="route-scene__eyebrow">
                <component :is="path.icon" :size="24" stroke-width="1.5" />
                <span>{{ path.hint }}</span>
              </div>
              <p class="route-scene__opening">{{ path.opening }}</p>
              <h3>{{ path.title }}</h3>
              <p class="route-scene__description">{{ path.description }}</p>
              <div class="route-scene__next">
                <span>故事从这里开始</span>
                <p>{{ path.mission }}</p>
              </div>
              <p class="route-scene__result"><span>最后，你会留下</span>{{ path.result }}</p>
            </article>
          </div>
        </div>
      </section>

      <section id="projects" class="projects-section">
        <img
          class="projects-section__image"
          src="/recruitment-expedition-project.png"
          alt="社团成员围绕真实项目进行协作开发"
          decoding="async"
          loading="lazy"
        />
        <div class="projects-section__veil" aria-hidden="true"></div>
        <div class="about-shell projects-section__content">
          <div class="section-heading section-heading--dark section-reveal">
            <p class="apple-kicker">REAL WORK. REAL IMPACT.</p>
            <h2>不止是练习。<br /><span>每次提交，都有回声。</span></h2>
            <p>从 issue 到上线，每一步都有真实使用者、协作伙伴和可以被验证的结果。</p>
          </div>

          <div class="projects-grid">
            <article
              v-for="(project, index) in featuredProjects"
              :key="project.title"
              class="project-card reveal-card"
            >
              <span>PROJECT / 0{{ index + 1 }}</span>
              <component :is="project.icon" :size="25" stroke-width="1.5" />
              <h3>{{ project.title }}</h3>
              <p>{{ project.description }}</p>
              <strong>{{ project.impact }}</strong>
              <small>{{ project.category }}</small>
            </article>
          </div>
        </div>
      </section>

      <section id="journey" class="about-section journey-section">
        <div class="about-shell">
          <div class="section-heading section-reveal">
            <p class="apple-kicker">YOUR FIRST 30 DAYS</p>
            <h2>加入，不是进群。<br /><span>是完成一次出发。</span></h2>
            <p>第一个月被拆成四个清晰阶段。每一站都有人同行，也都有看得见的成果。</p>
          </div>

          <ol class="journey-list">
            <li v-for="(item, index) in journey" :key="item.title" class="journey-item reveal-card">
              <div class="journey-item__marker">
                <span>0{{ index + 1 }}</span>
                <i aria-hidden="true"></i>
              </div>
              <small>{{ item.day }}</small>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <strong>{{ item.output }}</strong>
            </li>
          </ol>
        </div>
      </section>

      <section class="about-section gains-section">
        <div class="about-shell">
          <div class="section-heading section-reveal">
            <p class="apple-kicker">WHAT STAYS WITH YOU</p>
            <h2>带走的，<br /><span>远不止一项技能。</span></h2>
          </div>
          <div class="gains-grid">
            <article v-for="gain in gains" :key="gain.title" class="gain-card reveal-card">
              <component :is="gain.icon" :size="27" stroke-width="1.5" />
              <h3>{{ gain.title }}</h3>
              <p>{{ gain.description }}</p>
            </article>
          </div>
        </div>
      </section>

      <section class="community-section">
        <img
          class="community-section__image"
          src="/recruitment-expedition-community.png"
          alt="社团伙伴在开放社区中交流、协作与互相支持"
          decoding="async"
          loading="lazy"
        />
        <div class="community-section__veil" aria-hidden="true"></div>
        <div class="about-shell community-section__content section-reveal">
          <p class="apple-kicker">GROW TOGETHER</p>
          <h2>一个人走得快。<br /><span>一群人，走得更远。</span></h2>
          <blockquote>
            “第一次提 PR
            时，学长没有替我完成，而是陪我把问题拆开。那一晚，我第一次觉得自己真的在参与开源。”
          </blockquote>
          <p class="community-section__byline">往届成员 · 项目部</p>
        </div>
      </section>

      <section id="process" class="about-section process-section">
        <div class="about-shell process-layout">
          <div class="section-heading section-reveal">
            <p class="apple-kicker">JOIN THE COMMUNITY</p>
            <h2>带上好奇心。<br /><span>其他的，一起学。</span></h2>
            <p>我们不考你已经会多少，更在意你为什么想来，以及是否愿意认真完成一件事。</p>
            <button class="apple-button apple-button--dark" type="button" @click="goToApply">
              填写报名表
              <ArrowUpRight :size="17" />
            </button>
          </div>
          <ol class="process-list">
            <li v-for="(item, index) in process" :key="item.title" class="reveal-card">
              <span>0{{ index + 1 }}</span>
              <div>
                <strong>{{ item.title }}</strong>
                <small>{{ item.description }}</small>
              </div>
              <ArrowRight :size="18" />
            </li>
          </ol>
        </div>
      </section>

      <section id="faq" class="about-section faq-section">
        <div class="about-shell faq-layout">
          <div class="section-heading section-reveal">
            <p class="apple-kicker">FAQ</p>
            <h2>出发前，<br /><span>你可能还想知道。</span></h2>
          </div>
          <div class="faq-list">
            <details v-for="(item, index) in faqs" :key="item.question" class="reveal-card">
              <summary>
                <span>0{{ index + 1 }}</span>
                <strong>{{ item.question }}</strong>
                <Plus :size="20" />
              </summary>
              <p>{{ item.answer }}</p>
            </details>
          </div>
        </div>
      </section>

      <section class="final-section">
        <img src="/logo.png" alt="JMI-OPENATOM 标志" width="72" height="72" loading="lazy" />
        <p class="apple-kicker">YOUR NEXT CHAPTER</p>
        <h2>下一段开源故事，<br /><span>等你来写。</span></h2>
        <p>带上好奇心就够了。剩下的，我们在路上一起学。</p>
        <button class="apple-button apple-button--light" type="button" @click="goToApply">
          立即加入
          <ArrowRight :size="18" />
        </button>
      </section>
    </main>

    <footer class="about-footer">
      <div>
        <img src="/logo.png" alt="" width="32" height="32" loading="lazy" />
        <span>JMI-OPENATOM · 开放原子开源社团</span>
      </div>
      <span>© 2025–2027 JMI-OPENATOM</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowDown,
  ArrowRight,
  ArrowUpRight,
  Boxes,
  CalendarDays,
  ChevronDown,
  Code2,
  Compass,
  GitBranch,
  GitPullRequest,
  HeartHandshake,
  Lightbulb,
  Megaphone,
  Palette,
  Plus,
  Sparkles,
  Trophy,
  Users,
} from 'lucide-vue-next'
import { siteApi } from '@/api'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const router = useRouter()
const pageRoot = ref<HTMLElement>()
const metrics = ref<any[]>([])
const focusAreas = ref<any[]>([])
const showcaseApps = ref<any[]>([])
let motionMatchMedia: gsap.MatchMedia | undefined
let scrollResetFrame: number | undefined

const fallbackPaths = [
  {
    title: '项目部',
    hint: 'ENGINEERING',
    opening: '如果你喜欢把模糊的问题，变成真正能用的答案。',
    description: '做产品、写代码，把真实需求交付上线。',
    mission: '认识项目，认领一个边界清晰的问题，并提交第一次改动。',
    result: '一次被合并的 PR',
    icon: markRaw(Code2),
  },
  {
    title: '宣传组',
    hint: 'DESIGN & CONTENT',
    opening: '如果你擅长发现价值，也愿意把它讲给更多人听。',
    description: '把复杂技术讲清楚，让好作品被更多人看见。',
    mission: '采访项目成员，梳理价值，并完成一次真实发布。',
    result: '一篇完整的项目故事',
    icon: markRaw(Palette),
  },
  {
    title: '活动部',
    hint: 'EVENTS',
    opening: '如果你相信，一次好的相遇能点燃很多次行动。',
    description: '策划分享与工作坊，把学习变成共同经历。',
    mission: '参与一次活动全流程，并负责一段清晰的现场体验。',
    result: '一场完整的技术活动',
    icon: markRaw(CalendarDays),
  },
  {
    title: '外联部',
    hint: 'OUTREACH',
    opening: '如果你愿意走出去，为一群人的想法找到更大的舞台。',
    description: '连接校内外资源，让合作产生更大影响。',
    mission: '研究一个合作对象，准备提案，并推动一次真实对话。',
    result: '一次有效的合作连接',
    icon: markRaw(Megaphone),
  },
  {
    title: '社区部',
    hint: 'COMMUNITY',
    opening: '如果你在意每个人的感受，也愿意让陌生人变成同行者。',
    description: '维护社群体验，让每位成员找到连接与归属。',
    mission: '观察新人需求，策划一次轻量而有温度的社区行动。',
    result: '一套可复用的社区机制',
    icon: markRaw(HeartHandshake),
  },
]

const fallbackProjects = [
  {
    title: 'openatom-system',
    category: 'PLATFORM · OPEN SOURCE',
    description: '支撑社团成员、活动与内容协作的校园开源平台。',
    impact: '让社团日常协作更清晰、更可靠',
    icon: markRaw(Boxes),
  },
  {
    title: '校园网站发布工具',
    category: 'TOOLS · DEVOPS',
    description: '让校园项目从提交代码到上线发布更顺畅。',
    impact: '缩短从想法到真实可用的距离',
    icon: markRaw(GitBranch),
  },
  {
    title: 'AI 实验计划',
    category: 'RESEARCH · AI',
    description: '把前沿技术变成可以理解、复用和验证的实验。',
    impact: '把学习过程沉淀为可复用成果',
    icon: markRaw(Sparkles),
  },
]

const journey = [
  {
    day: 'DAY 01',
    title: '认识社区',
    description: '见到伙伴、了解项目，也说说你想尝试什么。',
    output: '找到你的第一张桌子',
  },
  {
    day: 'WEEK 01',
    title: '完成热身',
    description: '在带教伙伴帮助下，跑通工具与协作流程。',
    output: '完成第一次 commit',
  },
  {
    day: 'WEEK 02',
    title: '进入现场',
    description: '认领一个真实但边界清晰的任务，开始共建。',
    output: '提交第一个成果',
  },
  {
    day: 'DAY 30',
    title: '讲述成果',
    description: '复盘过程，把经验留给下一位出发的人。',
    output: '成为可靠的同行者',
  },
]

const gains = [
  {
    title: '完成事情的能力',
    description: '需求、协作、交付、复盘，不只停在“学过”。',
    icon: markRaw(Trophy),
  },
  {
    title: '看得见的作品',
    description: '真实项目、公开贡献和能够讲清楚的成长证据。',
    icon: markRaw(GitPullRequest),
  },
  {
    title: '一起走的人',
    description: '跨年级、跨方向的伙伴，以及愿意带你的前辈。',
    icon: markRaw(Users),
  },
  {
    title: '更大的视野',
    description: '从校园问题出发，认识真正的开源协作方式。',
    icon: markRaw(Compass),
  },
]

const process = [
  { title: '在线报名', description: '告诉我们你是谁、为什么想来' },
  { title: '开放交流', description: '聊兴趣和期待，不做知识背诵' },
  { title: '方向匹配', description: '认识部门与未来的带教伙伴' },
  { title: '正式出发', description: '加入社区，领取第一份路线图' },
]

const faqs = [
  {
    question: '我没有基础，也可以加入吗？',
    answer:
      '可以。我们更看重好奇心、可靠程度和持续投入。每个方向都有适合新人的第一份任务，也会安排带教伙伴。',
  },
  {
    question: '每周大概要投入多少时间？',
    answer:
      '建议每周留出 3–5 小时参与项目、例会或活动。考试周可以提前沟通，真实协作也包括尊重彼此的节奏。',
  },
  {
    question: '只能选择技术方向吗？',
    answer:
      '不是。项目、内容设计、活动策划和对外合作同样重要。你也可以先选一个主方向，再在项目中跨方向协作。',
  },
  {
    question: '加入后会有人带吗？',
    answer:
      '会。新人阶段有清晰的热身任务、带教伙伴和阶段复盘，目标是让你真正拥有独立做成事情的能力。',
  },
]

const heroMetrics = computed(() => {
  if (metrics.value.length) {
    return metrics.value.slice(0, 3).map((item) => ({
      value: item.value || '—',
      label: item.label || item.note || '开放协作',
    }))
  }
  return [
    { value: '68+', label: '在册成员' },
    { value: '5', label: '成长方向' },
    { value: '∞', label: '开放协作' },
  ]
})

const paths = computed(() =>
  fallbackPaths.map((item, index) => ({
    ...item,
    title: focusAreas.value[index]?.title || item.title,
    description: focusAreas.value[index]?.description || item.description,
  })),
)

const featuredProjects = computed(() =>
  fallbackProjects.map((fallback, index) => ({
    ...fallback,
    title: showcaseApps.value[index]?.name || showcaseApps.value[index]?.title || fallback.title,
    description: showcaseApps.value[index]?.description || fallback.description,
    category: showcaseApps.value[index]?.category || fallback.category,
  })),
)

function scrollTo(id: string) {
  const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  document.getElementById(id)?.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth' })
}

function goHome() {
  router.push('/')
}

function goToApply() {
  router.push('/apply')
}

function resetPageScroll() {
  ScrollTrigger.clearScrollMemory('manual')
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
}

function setupMotion() {
  if (!pageRoot.value) return

  motionMatchMedia = gsap.matchMedia()
  motionMatchMedia.add(
    {
      desktop: '(min-width: 1068px)',
      mobile: '(max-width: 1067px)',
      reduceMotion: '(prefers-reduced-motion: reduce)',
    },
    (context) => {
      const { desktop, reduceMotion } = context.conditions as {
        desktop: boolean
        mobile: boolean
        reduceMotion: boolean
      }

      if (reduceMotion) {
        gsap.set(['.section-reveal', '.reveal-card'], { clearProps: 'all' })
        gsap.set('.route-scene', { position: 'relative', autoAlpha: 1, clearProps: 'transform' })
        gsap.set('.route-story__rail-item', { autoAlpha: 1 })
        return
      }

      const intro = gsap.timeline({ defaults: { ease: 'power3.out' } })
      intro
        .from('.about-hero .apple-kicker', { y: 24, autoAlpha: 0, duration: 0.65 }, 0.08)
        .from('.about-hero h1 > span', { y: 72, autoAlpha: 0, stagger: 0.12, duration: 0.92 }, 0.16)
        .from('.about-hero__lead', { y: 32, autoAlpha: 0, duration: 0.72 }, 0.48)
        .from('.about-hero__actions', { y: 24, autoAlpha: 0, duration: 0.62 }, 0.58)
        .from(
          '.about-hero__metrics > div',
          { y: 18, autoAlpha: 0, stagger: 0.08, duration: 0.55 },
          0.68,
        )
        .from('.about-hero__scroll', { y: -10, autoAlpha: 0, duration: 0.5 }, 0.84)

      gsap.fromTo(
        '.about-hero__image',
        { scale: 1.12, xPercent: 0, yPercent: 0 },
        {
          scale: 1.1,
          xPercent: desktop ? 2 : 0,
          yPercent: desktop ? 3 : 2,
          ease: 'none',
          scrollTrigger: {
            trigger: '.about-hero',
            start: 'top top',
            end: 'bottom top',
            scrub: 0.8,
          },
        },
      )

      gsap.to('.about-hero__content', {
        y: desktop ? -72 : -34,
        autoAlpha: 0.45,
        ease: 'none',
        scrollTrigger: {
          trigger: '.about-hero',
          start: '35% top',
          end: 'bottom top',
          scrub: 0.7,
        },
      })

      const routeScenes = gsap.utils.toArray<HTMLElement>('.route-scene')
      const routeRailItems = gsap.utils.toArray<HTMLElement>('.route-story__rail-item')
      const routeProgress = pageRoot.value?.querySelector<HTMLElement>(
        '.route-story__progress span',
      )

      if (routeScenes.length) {
        gsap.set(routeScenes, { autoAlpha: 0, y: 70 })
        gsap.set(routeScenes[0], { autoAlpha: 1, y: 0 })
        gsap.set(routeRailItems, { opacity: 0.28 })
        gsap.set(routeRailItems[0], { opacity: 1 })

        const chapterLength = 1.5
        const routeTimeline = gsap.timeline({
          defaults: { ease: 'power3.inOut' },
          scrollTrigger: {
            trigger: '.routes-section',
            start: 'top top',
            end: () => `+=${window.innerHeight * routeScenes.length}`,
            pin: true,
            scrub: 0.75,
            snap: {
              snapTo: 'labelsDirectional',
              duration: { min: 0.18, max: 0.45 },
              delay: 0.06,
              ease: 'power2.inOut',
            },
            anticipatePin: 1,
            invalidateOnRefresh: true,
          },
        })

        if (routeProgress) {
          routeTimeline.fromTo(
            routeProgress,
            { scaleY: 0 },
            {
              scaleY: 1,
              duration: routeScenes.length * chapterLength,
              ease: 'none',
              transformOrigin: 'top',
            },
            0,
          )
        }

        routeScenes.forEach((scene, index) => {
          const label = `route-${index + 1}`
          const at = index * chapterLength
          const number = scene.querySelector('.route-scene__number')
          const eyebrow = scene.querySelector('.route-scene__eyebrow')
          const opening = scene.querySelector('.route-scene__opening')
          const title = scene.querySelector('h3')
          const description = scene.querySelector('.route-scene__description')
          const next = scene.querySelector('.route-scene__next')
          const result = scene.querySelector('.route-scene__result')

          routeTimeline.addLabel(label, at)
          routeTimeline.to(routeRailItems, { opacity: 0.28, duration: 0.18 }, label)
          routeTimeline.to(routeRailItems[index], { opacity: 1, duration: 0.28 }, label)
          routeTimeline.fromTo(
            scene,
            { autoAlpha: index === 0 ? 1 : 0, y: index === 0 ? 0 : 70 },
            { autoAlpha: 1, y: 0, duration: 0.38 },
            label,
          )
          routeTimeline.fromTo(
            number,
            { x: 80, autoAlpha: 0 },
            { x: 0, autoAlpha: 0.1, duration: 0.58 },
            `${label}+=0.04`,
          )
          routeTimeline.fromTo(
            eyebrow,
            { x: 24, autoAlpha: 0 },
            { x: 0, autoAlpha: 1, duration: 0.32 },
            `${label}+=0.08`,
          )
          routeTimeline.fromTo(
            opening,
            { y: 26, autoAlpha: 0 },
            { y: 0, autoAlpha: 1, duration: 0.42 },
            `${label}+=0.14`,
          )
          routeTimeline.fromTo(
            title,
            { y: 46, autoAlpha: 0 },
            { y: 0, autoAlpha: 1, duration: 0.5 },
            `${label}+=0.2`,
          )
          routeTimeline.fromTo(
            [description, next, result],
            { y: 24, autoAlpha: 0 },
            { y: 0, autoAlpha: 1, duration: 0.42, stagger: 0.08 },
            `${label}+=0.3`,
          )

          if (index < routeScenes.length - 1) {
            routeTimeline.to(
              scene,
              { y: -58, autoAlpha: 0, duration: 0.34, ease: 'power2.in' },
              `${label}+=1.08`,
            )
          }
        })
      }

      const animatedSections = gsap.utils.toArray<HTMLElement>(
        '.about-section:not(.routes-section), .projects-section, .community-section',
      )

      animatedSections.forEach((section) => {
        const heading = section.querySelector<HTMLElement>(
          '.section-heading, .community-section__content',
        )
        const kicker = heading?.querySelector('.apple-kicker')
        const title = heading?.querySelector('h2')
        const copy = heading?.querySelector('p:last-child, blockquote')
        const cards = section.querySelectorAll<HTMLElement>('.reveal-card')
        const sectionTimeline = gsap.timeline({
          scrollTrigger: {
            trigger: section,
            start: 'top 76%',
            once: true,
          },
        })

        if (kicker) {
          sectionTimeline.from(kicker, { x: -28, autoAlpha: 0, duration: 0.52 })
        }
        if (title) {
          sectionTimeline.from(
            title,
            {
              y: desktop ? 68 : 38,
              scale: 0.96,
              autoAlpha: 0,
              duration: 0.82,
              ease: 'power3.out',
              transformOrigin: 'left bottom',
            },
            '<0.08',
          )
        }
        if (copy) {
          sectionTimeline.from(
            copy,
            { y: 28, autoAlpha: 0, duration: 0.62, ease: 'power2.out' },
            '<0.22',
          )
        }
        if (cards.length) {
          sectionTimeline.from(
            cards,
            {
              x: (index) => (desktop ? (index % 2 === 0 ? -32 : 32) : 0),
              y: desktop ? 52 : 30,
              rotationY: desktop ? 5 : 0,
              scale: 0.975,
              autoAlpha: 0,
              stagger: 0.09,
              duration: 0.76,
              ease: 'power3.out',
              transformPerspective: 900,
              clearProps: 'transform,opacity,visibility',
            },
            '<0.18',
          )
        }
      })

      gsap.fromTo(
        '.projects-section__image',
        { scale: 1.14, xPercent: -2, yPercent: -2 },
        {
          scale: 1.08,
          xPercent: 2,
          yPercent: 2,
          ease: 'none',
          scrollTrigger: {
            trigger: '.projects-section',
            start: 'top bottom',
            end: 'bottom top',
            scrub: 0.8,
          },
        },
      )

      gsap.fromTo(
        '.community-section__image',
        { scale: 1.12, xPercent: 2 },
        {
          scale: 1.08,
          xPercent: -2,
          ease: 'none',
          scrollTrigger: {
            trigger: '.community-section',
            start: 'top bottom',
            end: 'bottom top',
            scrub: 0.8,
          },
        },
      )

      gsap.from('.journey-item__marker i', {
        scale: 0,
        duration: 0.6,
        stagger: 0.12,
        ease: 'back.out(1.7)',
        transformOrigin: 'center',
        scrollTrigger: {
          trigger: '.journey-list',
          start: 'top 78%',
          once: true,
        },
      })

      gsap
        .timeline({
          scrollTrigger: {
            trigger: '.final-section',
            start: 'top 76%',
            once: true,
          },
        })
        .from('.final-section > img', { scale: 0.6, rotation: -12, autoAlpha: 0, duration: 0.7 })
        .from('.final-section .apple-kicker', { y: 18, autoAlpha: 0, duration: 0.45 }, '<0.18')
        .from('.final-section h2', { y: 64, scale: 0.95, autoAlpha: 0, duration: 0.85 }, '<0.12')
        .from(
          '.final-section > p:not(.apple-kicker)',
          { y: 24, autoAlpha: 0, duration: 0.55 },
          '<0.2',
        )
        .from('.final-section .apple-button', { y: 20, autoAlpha: 0, duration: 0.5 }, '<0.16')
    },
    pageRoot.value,
  )

  ScrollTrigger.refresh()
}

onMounted(async () => {
  resetPageScroll()
  document.documentElement.classList.add('about-page-active')

  const [homeResult, appsResult] = await Promise.allSettled([
    siteApi.clubHome(),
    siteApi.showcaseApps({ page: 1, pageSize: 3 }),
  ])
  if (homeResult.status === 'fulfilled') {
    metrics.value = homeResult.value?.metrics || []
    focusAreas.value = homeResult.value?.focusAreas || []
  }
  if (appsResult.status === 'fulfilled') {
    showcaseApps.value = appsResult.value?.list || appsResult.value || []
  }

  await nextTick()
  resetPageScroll()
  setupMotion()
  scrollResetFrame = window.requestAnimationFrame(() => {
    resetPageScroll()
    ScrollTrigger.refresh()
  })
})

onBeforeUnmount(() => {
  if (scrollResetFrame !== undefined) window.cancelAnimationFrame(scrollResetFrame)
  motionMatchMedia?.revert()
  document.documentElement.classList.remove('about-page-active')
})
</script>

<style scoped>
:global(html.about-page-active) {
  scrollbar-width: none;
}

:global(html.about-page-active::-webkit-scrollbar) {
  display: none;
}

.about-page {
  --apple-bg: #ffffff;
  --apple-bg-soft: #f5f5f7;
  --apple-bg-elevated: #fbfbfd;
  --apple-ink: #1d1d1f;
  --apple-muted: #6e6e73;
  --apple-faint: #86868b;
  --apple-blue: #0066cc;
  --apple-blue-hover: #0077ed;
  --apple-line: rgba(29, 29, 31, 0.12);
  --apple-on-dark-primary: #f5f5f7;
  --apple-on-dark-secondary: rgba(245, 245, 247, 0.78);
  --apple-on-dark-tertiary: rgba(245, 245, 247, 0.58);
  --apple-on-dark-accent: #79bfff;
  --apple-radius: 24px;
  --apple-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 18px 50px rgba(0, 0, 0, 0.08);
  min-height: 100vh;
  overflow: clip;
  background: var(--apple-bg);
  color: var(--apple-ink);
  font-family: 'SF Pro Text', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.about-page *,
.about-page *::before,
.about-page *::after {
  box-sizing: border-box;
}

.about-page button,
.about-page summary {
  font: inherit;
}

.about-shell {
  width: min(1180px, calc(100% - 48px));
  margin: 0 auto;
}

.about-hero {
  position: relative;
  min-height: 100svh;
  display: flex;
  align-items: center;
  overflow: hidden;
  isolation: isolate;
  background: #050506;
  color: #fff;
}

.about-hero__image,
.projects-section__image,
.community-section__image {
  position: absolute;
  z-index: 0;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.about-hero__veil {
  position: absolute;
  z-index: 1;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(
      90deg,
      rgba(5, 5, 6, 0.82) 0%,
      rgba(5, 5, 6, 0.58) 38%,
      rgba(5, 5, 6, 0.2) 68%,
      rgba(5, 5, 6, 0.08) 100%
    ),
    linear-gradient(180deg, rgba(5, 5, 6, 0.08) 42%, rgba(5, 5, 6, 0.56) 100%);
}

.about-hero__content {
  position: relative;
  z-index: 2;
  padding: 64px 0 48px;
}

.apple-kicker {
  margin: 0 0 22px;
  color: var(--apple-faint);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.about-hero .apple-kicker {
  color: rgba(255, 255, 255, 0.66);
}

.about-hero h1,
.section-heading h2,
.route-story__intro h2,
.community-section h2,
.final-section h2 {
  margin: 0;
  font-family: 'SF Pro Display', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.about-hero h1 {
  max-width: 850px;
  font-size: clamp(58px, 7.6vw, 112px);
  line-height: 0.98;
}

.about-hero h1 span {
  display: block;
}

.about-hero h1 span:last-child {
  color: rgba(255, 255, 255, 0.62);
}

.about-hero__lead {
  max-width: 620px;
  margin: 32px 0 0;
  color: rgba(255, 255, 255, 0.74);
  font-size: clamp(18px, 2vw, 22px);
  line-height: 1.6;
}

.about-hero__actions {
  display: flex;
  align-items: center;
  gap: 22px;
  margin-top: 38px;
}

.apple-button,
.apple-link {
  display: inline-flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
}

.apple-button {
  padding: 0 23px;
  border-radius: 999px;
  transition:
    transform 180ms ease,
    background 180ms ease;
}

.apple-button:hover {
  transform: translateY(-2px);
}

.apple-button--light {
  background: #fff;
  color: #1d1d1f;
}

.apple-button--dark {
  background: var(--apple-ink);
  color: #fff;
}

.apple-link {
  padding: 0;
  background: transparent;
  color: var(--apple-blue);
}

.apple-link--light {
  color: rgba(255, 255, 255, 0.86);
}

.about-page button:focus-visible,
.about-page summary:focus-visible {
  outline: 3px solid #2997ff;
  outline-offset: 3px;
}

.about-hero__metrics {
  display: flex;
  gap: clamp(28px, 5vw, 68px);
  width: min(620px, 100%);
  margin-top: 56px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.about-hero__metrics div {
  display: grid;
  gap: 4px;
}

.about-hero__metrics strong {
  font-size: 28px;
  letter-spacing: -0.03em;
}

.about-hero__metrics span {
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
}

.about-hero__scroll {
  position: absolute;
  z-index: 2;
  bottom: 24px;
  left: 50%;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px;
  transform: translateX(-50%);
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.56);
  cursor: pointer;
  font-size: 11px;
}

.about-section {
  min-height: 100svh;
  display: flex;
  align-items: center;
  padding: 128px 0;
}

.about-story,
.journey-section,
.faq-section {
  background: var(--apple-bg);
}

.routes-section,
.gains-section,
.process-section {
  background: var(--apple-bg-soft);
}

.section-heading {
  max-width: 780px;
  margin-bottom: 64px;
}

.section-heading h2,
.community-section h2,
.final-section h2 {
  font-size: clamp(44px, 5.4vw, 76px);
  line-height: 1.05;
}

.section-heading h2 span,
.community-section h2 span,
.final-section h2 span {
  color: var(--apple-faint);
}

.section-heading > p:last-child {
  max-width: 650px;
  margin: 24px 0 0;
  color: var(--apple-muted);
  font-size: 19px;
  line-height: 1.62;
}

.story-grid {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 20px;
}

.story-card {
  min-height: 420px;
  padding: 40px;
  border-radius: var(--apple-radius);
  overflow: hidden;
}

.story-card--year {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background: var(--apple-ink);
  color: #fff;
}

.story-card--year span {
  color: #a1a1a6;
  font-size: 13px;
  letter-spacing: 0.15em;
}

.story-card--year strong {
  margin: auto 0;
  font-size: clamp(86px, 12vw, 150px);
  line-height: 1;
  letter-spacing: -0.06em;
}

.story-card p {
  margin: 0;
  color: inherit;
  line-height: 1.65;
}

.story-card--statement {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background: linear-gradient(145deg, #e8f2ff, #f6f8ff 62%, #eee7ff);
}

.story-card--statement svg {
  margin-bottom: auto;
  color: #1877d2;
}

.story-card--statement h3 {
  max-width: 500px;
  margin: 0 0 18px;
  font-size: clamp(28px, 3vw, 42px);
  line-height: 1.1;
  letter-spacing: -0.035em;
}

.story-card--statement p {
  max-width: 520px;
  color: var(--apple-muted);
}

.route-story {
  min-height: 100svh;
  display: grid;
  grid-template-columns: minmax(320px, 0.82fr) minmax(520px, 1.18fr);
  align-items: center;
  gap: clamp(56px, 8vw, 130px);
  padding: 52px 0;
}

.route-story__intro {
  position: relative;
  z-index: 2;
}

.route-story__intro h2 {
  font-size: clamp(48px, 5vw, 72px);
  line-height: 1.04;
}

.route-story__intro h2 span {
  color: var(--apple-faint);
}

.route-story__intro > p:last-of-type {
  max-width: 410px;
  margin: 24px 0 0;
  color: var(--apple-muted);
  font-size: 17px;
  line-height: 1.65;
}

.route-story__rail {
  display: grid;
  gap: 10px;
  margin: 48px 0 0;
  padding: 0;
  list-style: none;
}

.route-story__rail-item {
  display: flex;
  align-items: baseline;
  gap: 14px;
  color: var(--apple-ink);
  will-change: opacity;
}

.route-story__rail-item span {
  width: 20px;
  color: var(--apple-blue);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.route-story__rail-item strong {
  font-size: 14px;
  font-weight: 600;
}

.route-story__stage {
  position: relative;
  min-height: min(70svh, 590px);
  border-left: 1px solid rgba(29, 29, 31, 0.12);
}

.route-story__progress {
  position: absolute;
  top: 0;
  bottom: 0;
  left: -1px;
  width: 2px;
  overflow: hidden;
}

.route-story__progress span {
  width: 100%;
  height: 100%;
  display: block;
  transform: scaleY(0);
  transform-origin: top;
  background: var(--apple-blue);
  will-change: transform;
}

.route-scene {
  position: absolute;
  inset: 0 0 0 clamp(42px, 5vw, 76px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  opacity: 0;
  visibility: hidden;
  will-change: transform, opacity;
}

.route-scene:first-of-type {
  opacity: 1;
  visibility: visible;
}

.route-scene__number {
  position: absolute;
  z-index: -1;
  top: -0.12em;
  right: 0;
  color: var(--apple-blue);
  font-size: clamp(150px, 17vw, 244px);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.09em;
  opacity: 0.1;
  pointer-events: none;
}

.route-scene__eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--apple-blue);
}

.route-scene__eyebrow span,
.project-card > span,
.project-card > small {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.09em;
}

.route-scene__opening {
  max-width: 590px;
  margin: 34px 0 0;
  color: var(--apple-muted);
  font-size: clamp(19px, 2vw, 24px);
  line-height: 1.55;
}

.route-scene h3,
.project-card h3,
.gain-card h3,
.journey-item h3 {
  margin: 12px 0 0;
  font-size: 28px;
  letter-spacing: -0.035em;
}

.route-scene h3 {
  margin-top: 14px;
  font-size: clamp(58px, 7vw, 92px);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.055em;
}

.route-scene__description {
  max-width: 560px;
  margin: 20px 0 0;
  color: var(--apple-muted);
  line-height: 1.65;
}

.route-scene__next {
  display: grid;
  grid-template-columns: 128px 1fr;
  gap: 22px;
  max-width: 590px;
  margin-top: 38px;
  padding-top: 22px;
  border-top: 1px solid var(--apple-line);
}

.route-scene__next span,
.route-scene__result span {
  color: var(--apple-faint);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.route-scene__next p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
}

.route-scene__result {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 20px 0 0;
  color: var(--apple-blue);
  font-size: 14px;
  font-weight: 600;
}

.projects-section,
.community-section,
.final-section {
  position: relative;
  min-height: 100svh;
  display: flex;
  align-items: center;
  overflow: hidden;
  isolation: isolate;
  color: #f5f5f7;
}

.projects-section__veil,
.community-section__veil {
  position: absolute;
  z-index: 1;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(
    90deg,
    rgba(0, 0, 0, 0.9),
    rgba(0, 0, 0, 0.52) 62%,
    rgba(0, 0, 0, 0.24)
  );
}

.projects-section__content {
  position: relative;
  z-index: 2;
  padding: 128px 0;
}

.projects-section__veil {
  background:
    linear-gradient(90deg, rgba(4, 5, 7, 0.82), rgba(4, 5, 7, 0.58) 58%, rgba(4, 5, 7, 0.3)),
    linear-gradient(180deg, rgba(4, 5, 7, 0.1), rgba(4, 5, 7, 0.5));
}

.section-heading--dark {
  color: var(--apple-on-dark-primary);
}

.section-heading--dark .apple-kicker {
  color: var(--apple-on-dark-accent);
}

.section-heading--dark h2 {
  color: var(--apple-on-dark-primary);
}

.section-heading--dark h2 span {
  color: rgba(245, 245, 247, 0.64);
}

.section-heading--dark > p:last-child,
.community-section blockquote {
  color: var(--apple-on-dark-secondary);
}

.projects-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.project-card {
  min-height: 340px;
  display: flex;
  flex-direction: column;
  padding: 30px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(20, 22, 27, 0.78), rgba(9, 10, 13, 0.68));
  box-shadow: 0 20px 55px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(20px) saturate(1.15);
  transition:
    transform 260ms ease,
    border-color 260ms ease,
    background 260ms ease;
}

.project-card:hover {
  transform: translateY(-6px);
  border-color: rgba(255, 255, 255, 0.32);
  background: linear-gradient(145deg, rgba(28, 31, 38, 0.84), rgba(12, 13, 17, 0.78));
}

.project-card > span,
.project-card > small {
  color: var(--apple-on-dark-tertiary);
}

.project-card > svg {
  color: var(--apple-on-dark-accent);
}

.project-card > svg {
  margin-top: 36px;
}

.project-card p {
  margin: 14px 0 0;
  color: var(--apple-on-dark-secondary);
  line-height: 1.65;
}

.project-card h3,
.project-card strong {
  color: var(--apple-on-dark-primary);
}

.project-card strong {
  margin-top: 22px;
  font-size: 13px;
}

.project-card small {
  margin-top: auto;
}

.journey-list {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin: 0;
  padding: 0;
  list-style: none;
}

.journey-list::before {
  content: '';
  position: absolute;
  top: 24px;
  right: 10%;
  left: 10%;
  height: 1px;
  background: var(--apple-line);
}

.journey-item {
  position: relative;
  padding: 0 28px 12px 0;
}

.journey-item__marker {
  display: flex;
  height: 50px;
  align-items: center;
  gap: 14px;
  color: var(--apple-faint);
  font-size: 11px;
}

.journey-item__marker i {
  position: relative;
  z-index: 1;
  width: 12px;
  height: 12px;
  border: 3px solid #fff;
  border-radius: 50%;
  background: var(--apple-blue);
  box-shadow: 0 0 0 1px var(--apple-blue);
}

.journey-item > small {
  color: var(--apple-blue);
  font-size: 11px;
  font-weight: 600;
}

.journey-item p,
.gain-card p {
  margin: 14px 0 0;
  color: var(--apple-muted);
  line-height: 1.65;
}

.journey-item > strong {
  display: block;
  margin-top: 24px;
  font-size: 13px;
}

.gains-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

.gain-card {
  min-height: 290px;
  padding: 34px;
  border-radius: var(--apple-radius);
  background: var(--apple-bg);
}

.gain-card svg {
  color: var(--apple-blue);
}

.gain-card h3 {
  margin-top: 76px;
}

.community-section__veil {
  background: linear-gradient(
    90deg,
    rgba(0, 0, 0, 0.86),
    rgba(0, 0, 0, 0.32) 72%,
    rgba(0, 0, 0, 0.12)
  );
}

.community-section__content {
  position: relative;
  z-index: 2;
  padding: 128px 0;
}

.community-section h2 {
  max-width: 760px;
}

.community-section h2 span {
  color: rgba(255, 255, 255, 0.58);
}

.community-section blockquote {
  max-width: 600px;
  margin: 42px 0 0;
  padding-left: 22px;
  border-left: 2px solid rgba(255, 255, 255, 0.72);
  font-size: 18px;
  line-height: 1.75;
}

.community-section__byline {
  margin-top: 18px;
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
}

.process-layout,
.faq-layout {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 88px;
  align-items: start;
}

.process-layout .section-heading,
.faq-layout .section-heading {
  position: sticky;
  top: 100px;
  margin-bottom: 0;
}

.process-layout .apple-button {
  margin-top: 32px;
}

.process-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.process-list li {
  min-height: 112px;
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 26px;
  border-radius: 20px;
  background: #fff;
}

.process-list li > span {
  color: var(--apple-blue);
  font-size: 12px;
}

.process-list li div {
  display: grid;
  gap: 7px;
}

.process-list li strong {
  font-size: 18px;
}

.process-list li small {
  color: var(--apple-muted);
}

.process-list li > svg {
  margin-left: auto;
  color: var(--apple-faint);
}

.faq-list {
  border-top: 1px solid var(--apple-line);
}

.faq-list details {
  border-bottom: 1px solid var(--apple-line);
}

.faq-list summary {
  display: grid;
  grid-template-columns: 38px 1fr 24px;
  align-items: center;
  gap: 12px;
  min-height: 82px;
  cursor: pointer;
  list-style: none;
}

.faq-list summary::-webkit-details-marker {
  display: none;
}

.faq-list summary > span {
  color: var(--apple-blue);
  font-size: 11px;
}

.faq-list summary strong {
  font-size: 17px;
}

.faq-list summary svg {
  transition: transform 220ms ease;
}

.faq-list details[open] summary svg {
  transform: rotate(45deg);
}

.faq-list details p {
  margin: -2px 36px 26px 50px;
  color: var(--apple-muted);
  line-height: 1.7;
}

.final-section {
  min-height: 100svh;
  flex-direction: column;
  justify-content: center;
  padding: 120px 24px;
  background: linear-gradient(180deg, #1d1d1f, #000);
  text-align: center;
}

.final-section > img {
  margin-bottom: 28px;
  border-radius: 18px;
  background: #fff;
}

.final-section h2 span {
  color: #a1a1a6;
}

.final-section > p:not(.apple-kicker) {
  margin: 26px 0 34px;
  color: #a1a1a6;
  font-size: 18px;
}

.about-footer {
  display: flex;
  min-height: 88px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px max(24px, calc((100vw - 1180px) / 2));
  border-top: 1px solid var(--apple-line);
  background: #fff;
  color: var(--apple-faint);
  font-size: 12px;
}

.about-footer div {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--apple-ink);
  font-weight: 600;
}

@media (min-width: 1068px) {
  .about-section,
  .projects-section__content,
  .community-section__content {
    padding-top: 58px;
    padding-bottom: 58px;
  }

  .section-heading {
    margin-bottom: 34px;
  }

  .routes-section {
    padding: 0;
  }

  .section-heading h2,
  .route-story__intro h2,
  .community-section h2,
  .final-section h2 {
    font-size: clamp(44px, 4.4vw, 64px);
  }

  .section-heading > p:last-child {
    margin-top: 18px;
    font-size: 17px;
  }

  .story-card {
    min-height: 350px;
  }

  .projects-section__content {
    padding-top: 58px;
    padding-bottom: 58px;
  }

  .project-card {
    min-height: 286px;
    padding: 24px;
  }

  .project-card > svg {
    margin-top: 22px;
  }

  .gains-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;
  }

  .gain-card {
    min-height: 270px;
    padding: 28px;
  }

  .gain-card h3 {
    margin-top: 54px;
  }
}

@media (max-width: 1067px) {
  .about-section,
  .projects-section__content,
  .community-section__content {
    padding-top: 96px;
    padding-bottom: 96px;
  }

  .routes-section {
    min-height: 100svh;
    padding: 0;
  }

  .route-story {
    grid-template-columns: minmax(260px, 0.72fr) minmax(400px, 1.28fr);
    gap: 44px;
    padding: 48px 0;
  }

  .route-story__intro h2 {
    font-size: clamp(42px, 6vw, 58px);
  }

  .route-story__rail {
    margin-top: 34px;
  }

  .route-story__stage {
    min-height: 590px;
  }

  .route-scene {
    left: 38px;
  }

  .route-scene h3 {
    font-size: clamp(54px, 8vw, 76px);
  }

  .projects-grid {
    grid-template-columns: 1fr;
  }

  .project-card {
    min-height: 280px;
  }

  .journey-list {
    grid-template-columns: repeat(2, 1fr);
    gap: 44px 20px;
  }

  .journey-list::before {
    display: none;
  }

  .process-layout,
  .faq-layout {
    grid-template-columns: 1fr;
    gap: 52px;
  }

  .process-layout .section-heading,
  .faq-layout .section-heading {
    position: static;
  }
}

@media (max-width: 733px) {
  .about-shell {
    width: min(100% - 40px, 620px);
  }

  .about-hero {
    min-height: 760px;
    align-items: flex-end;
  }

  .about-hero__image {
    object-position: 58% center;
  }

  .about-hero__veil {
    background: linear-gradient(180deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.9) 68%);
  }

  .about-hero__content {
    padding: 130px 0 86px;
  }

  .about-hero h1 {
    font-size: clamp(48px, 14vw, 68px);
  }

  .about-hero__lead {
    font-size: 17px;
  }

  .about-hero__actions {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .about-hero__metrics {
    gap: 22px;
    margin-top: 36px;
  }

  .about-hero__metrics strong {
    font-size: 23px;
  }

  .about-hero__scroll {
    display: none;
  }

  .about-section,
  .projects-section__content,
  .community-section__content {
    padding-top: 76px;
    padding-bottom: 76px;
  }

  .section-heading {
    margin-bottom: 42px;
  }

  .section-heading h2,
  .route-story__intro h2,
  .community-section h2,
  .final-section h2 {
    font-size: 42px;
  }

  .section-heading > p:last-child {
    font-size: 16px;
  }

  .story-grid,
  .gains-grid,
  .journey-list {
    grid-template-columns: 1fr;
  }

  .story-card {
    min-height: 340px;
    padding: 28px;
  }

  .route-story {
    min-height: 100svh;
    display: block;
    padding: 56px 0 42px;
  }

  .route-story__intro h2 {
    max-width: 360px;
    font-size: clamp(40px, 11vw, 54px);
  }

  .route-story__intro > p:last-of-type {
    max-width: 340px;
    margin-top: 16px;
    font-size: 15px;
  }

  .route-story__rail {
    display: flex;
    gap: 18px;
    margin-top: 24px;
    overflow: hidden;
  }

  .route-story__rail-item {
    flex: 0 0 auto;
  }

  .route-story__rail-item strong {
    display: none;
  }

  .route-story__stage {
    min-height: 490px;
    margin-top: 24px;
  }

  .route-scene {
    inset: 0 0 0 24px;
    justify-content: flex-start;
    padding-top: 18px;
  }

  .route-scene__number {
    top: 0;
    font-size: 132px;
  }

  .route-scene__opening {
    max-width: 330px;
    margin-top: 24px;
    font-size: 17px;
  }

  .route-scene h3 {
    font-size: clamp(50px, 16vw, 66px);
  }

  .route-scene__description {
    margin-top: 14px;
    font-size: 14px;
  }

  .route-scene__next {
    display: block;
    margin-top: 24px;
    padding-top: 16px;
  }

  .route-scene__next p {
    margin-top: 8px;
    font-size: 13px;
  }

  .route-scene__result {
    align-items: flex-start;
    flex-direction: column;
    gap: 5px;
    margin-top: 14px;
    font-size: 13px;
  }

  .projects-section,
  .community-section {
    min-height: auto;
  }

  .projects-section__image,
  .community-section__image {
    object-position: center;
  }

  .projects-section__veil,
  .community-section__veil {
    background: rgba(0, 0, 0, 0.72);
  }

  .journey-list {
    gap: 32px;
  }

  .journey-item {
    padding-right: 0;
  }

  .gain-card {
    min-height: 250px;
  }

  .community-section blockquote {
    font-size: 16px;
  }

  .faq-list summary {
    grid-template-columns: 28px 1fr 22px;
  }

  .faq-list details p {
    margin-left: 40px;
  }

  .about-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .about-page *,
  .about-page *::before,
  .about-page *::after {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }

  .routes-section {
    min-height: auto;
  }

  .route-story {
    min-height: auto;
    grid-template-columns: 1fr;
    padding-top: 96px;
    padding-bottom: 96px;
  }

  .route-story__rail,
  .route-story__progress {
    display: none;
  }

  .route-story__stage {
    min-height: 0;
    display: grid;
    gap: 56px;
    border-left: 0;
  }

  .route-scene {
    position: relative !important;
    inset: auto;
    padding-top: 32px;
    border-top: 1px solid var(--apple-line);
    opacity: 1 !important;
    visibility: visible !important;
    will-change: auto;
  }
}
</style>
