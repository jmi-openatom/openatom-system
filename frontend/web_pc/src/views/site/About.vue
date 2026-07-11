<template>
  <div ref="pageRoot" class="expedition-page" :class="{ 'is-reduced': reducedMotion }">
<!--    <aside class="chapter-rail" aria-label="章节进度">-->
<!--      <span class="chapter-rail__label">EXPEDITION</span>-->
<!--      <button-->
<!--          v-for="(chapter, index) in chapters"-->
<!--          :key="chapter.id"-->
<!--          type="button"-->
<!--          :class="{ active: activeChapter === index }"-->
<!--          :aria-label="`前往${chapter.label}`"-->
<!--          @click="scrollTo(chapter.id)"-->
<!--      >-->
<!--        <i></i><span>0{{ index + 1 }}</span>-->
<!--      </button>-->
<!--    </aside>-->

<!--    <div class="story-orb" aria-hidden="true">-->
<!--      <span></span>-->
<!--      <b>00</b>-->
<!--    </div>-->

    <main>
      <section id="top" class="hero chapter" data-chapter="0">
        <img src="/bg.png" alt="" class="hero__bg" />
        <div class="hero__shade" aria-hidden="true"></div>
        <div class="topo-texture" aria-hidden="true"></div>

        <div class="hero__content parallax-copy" data-parallax data-speed="-0.12">
          <p class="eyebrow" >
            <Compass :size="15"/>
            关于我们 · 江苏海事职业技术学院 · 开放原子开源社团
          </p>
          <h1>从校园出发，<br/><em>去更大的开源 <br> 世界</em></h1>
          <p class="hero__lead"  >
            在 {{ club.name || 'JMI-OPENATOM' }}，你不只是学习一门技术。你会找到一条路线、完成第一次真实任务，
            然后和一群伙伴把作品带到更远的地方。
          </p>
          <p class="hero__promise" style="color: white">不用先成为高手。会观察、会表达、愿意把一件事做完，就是很好的起点。</p>
          <div class="hero__actions">
            <button class="button button--primary" type="button" @click="goback">
              <ArrowLeft :size="18"/>
              返回官网

            </button>
            <button class="button button--ghost" type="button" @click="scrollTo('routes')">
              查看远征地图
              <ChevronDown :size="17"/>
            </button>
          </div>
          <div class="hero__metrics" aria-label="社团数据">
            <div v-for="item in heroMetrics" :key="item.label">
              <strong >{{ item.value }}</strong><span >{{ item.label }}</span>
            </div>
          </div>
        </div>

        <button class="scroll-hint" type="button" @click="scrollTo('routes')" style="color: #000000">
          滚动以探索
          <ChevronDown :size="16"/>
        </button>
      </section>

      <section class="philosophy">
        <div class="philosophy__orb" aria-hidden="true"></div>
        <div class="philosophy__content parallax-copy" data-parallax data-speed="-0.06">
          <p class="eyebrow">
            <Lightbulb :size="15"/>
            创立理念
          </p>
          <h2 style="color: #ffffff">开源筑梦<br/><em style="color: #ffffff">海事启航</em></h2>
          <p>让每一位成员在真实项目中成长，在开放社区中发光。</p>
        </div>
      </section>

      <section class="founding-year" style="height: 100vh">
        <div class="founding-year__content">
          <span class="founding-year__label">EST.</span>
          <strong class="founding-year__number">2025</strong>
          <p>一群人，一个社区，从零开始。</p>
        </div>
      </section>

      <section id="routes" class="routes chapter" data-chapter="1">
        <div class="section-shell">
          <div class="chapter-heading parallax-copy" data-parallax data-speed="-0.07">
            <span class="chapter-number">02</span>
            <div>
              <p class="eyebrow">
                <Route :size="15"/>
                CHOOSE YOUR ROUTE
              </p>
              <h2>五个路标，<br/>总有一个指向你。</h2>
              <p>继续往下走。每经过一个路标，你都会看到这个方向真实的第一份任务，以及旅程留下的第一个成果。</p>
            </div>
          </div>

          <div class="route-story" aria-label="五条成长路线">
            <section
                v-for="(path, index) in paths"
                :key="path.title"
                :class="['route-story__stop', `route-story__stop--${index + 1}`, 'parallax-copy']"
                data-parallax
                :data-speed="index % 2 ? -0.08 : -0.12"
                :ref="setRouteStoryRef"
                :id="`department-${index + 1}`"
            >
              <div class="route-story__marker">
                <span>ROUTE 0{{ index + 1 }}</span>
                <component :is="path.icon" :size="27" stroke-width="1.5"/>
              </div>
              <div class="route-story__intro">
                <small>{{ path.hint }}</small>
                <h3>{{ path.title }}</h3>
                <p>{{ path.description }}</p>
                <dl class="route-story__details">
                  <div><dt>适合这样的你</dt><dd>{{ path.fit }}</dd></div>
                  <div><dt>你会逐渐拥有</dt><dd>{{ path.growth }}</dd></div>
                </dl>
              </div>
              <div class="route-story__mission">
                <span><Flag :size="14"/> 第一次任务</span>
                <p>{{ path.mission }}</p>
                <ol>
                  <li v-for="(step, stepIndex) in path.steps" :key="step">
                    <b>0{{ stepIndex + 1 }}</b>{{ step }}
                  </li>
                </ol>
              </div>
              <div class="route-story__result">
                <GitPullRequest :size="23"/>
                <span><small>抵达这一站，你会拥有</small><strong>{{ path.result }}</strong></span>
              </div>
              <div class="route-story__nontech">
                <Sparkles :size="16"/>
                <span><b>不会写代码也能参与</b>{{ path.nonTechnical }}</span>
              </div>
            </section>
          </div>
        </div>
      </section>

      <section id="projects" class="project-scene chapter" data-chapter="2">
        <div class="project-scene__image project-scene__canvas parallax-layer" data-speed="0.18" aria-hidden="true"></div>
        <div class="project-scene__shade" aria-hidden="true"></div>
        <div class="section-shell project-scene__content parallax-copy" data-parallax data-speed="-0.08">
          <div class="chapter-heading chapter-heading--light">
            <span class="chapter-number">03</span>
            <div>
              <p class="eyebrow">
                <Laptop2 :size="15"/>
                REAL PROJECT SITE
              </p>
              <h2>真实项目现场</h2>
              <p>从 issue 到上线，每一步都有真实使用者、协作伙伴和可以被验证的结果。</p>
            </div>
          </div>

          <div class="project-list">
            <article v-for="(project, index) in featuredProjects" :key="project.title">
              <span>PROJECT / 0{{ index + 1 }}</span>
              <component :is="project.icon" :size="23" stroke-width="1.6"/>
              <h3>{{ project.title }}</h3>
              <p>{{ project.description }}</p>
              <strong>{{ project.impact }}</strong>
              <small>{{ project.category }}</small>
            </article>
          </div>
        </div>
      </section>

      <section id="journey" class="journey chapter" data-chapter="3">
        <div class="section-shell">
          <div class="chapter-heading parallax-copy" data-parallax data-speed="-0.07">
            <span class="chapter-number">04</span>
            <div>
              <p class="eyebrow">
                <Footprints :size="15"/>
                THE FIRST 30 DAYS
              </p>
              <h2>不是“加入一个群”，<br/>是完成一次真正的出发。</h2>
              <p>我们把最容易迷路的第一个月拆成四段。每一站都有人带，也都有看得见的成果。</p>
            </div>
          </div>

          <ol class="journey-line">
            <li v-for="(item, index) in journey" :key="item.title">
              <span class="journey-line__day">{{ item.day }}</span>
              <i aria-hidden="true"></i>
              <div>
                <small>CHECKPOINT 0{{ index + 1 }}</small>
                <h3>{{ item.title }}</h3>
                <p>{{ item.description }}</p>
                <b>{{ item.output }}</b>
              </div>
            </li>
          </ol>
        </div>
      </section>

      <section class="gains-scene">
        <div class="section-shell">
          <div class="gains">
            <p class="eyebrow">
              <Trophy :size="15"/>
              WHAT YOU GAIN
            </p>
            <h2>你会带走什么</h2>
            <div>
              <article v-for="gain in gains" :key="gain.title">
                <component :is="gain.icon" :size="24" stroke-width="1.6"/>
                <strong>{{ gain.title }}</strong>
                <p>{{ gain.description }}</p>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section class="community-story">
        <div class="community-story__image community-story__canvas parallax-layer" data-speed="0.18" aria-hidden="true"></div>
        <div class="community-story__shade" aria-hidden="true"></div>
        <div class="community-story__copy parallax-copy" data-parallax data-speed="-0.1">
          <p class="eyebrow">
            <Users :size="15"/>
            MEET YOUR MENTORS
          </p>
          <h2>一个人能走得很快，<br/><em>一群人才能走得更远。</em></h2>
          <blockquote>
            “第一次提 PR 时，我连描述都不知道怎么写。学长没有替我完成，而是陪我把问题拆开。那一晚，我第一次觉得自己真的在参与开源。”
          </blockquote>
          <span>— 往届成员 · 项目部</span>
        </div>
      </section>

      <section id="process" class="process">
        <div class="section-shell">
          <p class="eyebrow">
            <Send :size="15"/>
            APPLICATION
          </p>
          <div class="process__heading">
            <h2>报名出发</h2>
            <p>我们不考“你已经会多少”，更关心你为什么想来、愿意怎么投入。</p>
          </div>
          <div class="process__values" aria-label="我们看重的品质">
            <span>保持好奇</span><span>认真回应</span><span>愿意协作</span><span>把事情做完</span>
          </div>
          <ol>
            <li v-for="(item, index) in process" :key="item.title">
              <span>0{{ index + 1 }}</span>
              <div><strong>{{ item.title }}</strong><small>{{ item.description }}</small></div>
              <ArrowRight :size="18"/>
            </li>
          </ol>
          <button class="button button--primary process__cta" type="button" @click="goToApply">
            填写报名表
            <ArrowUpRight :size="18"/>
          </button>
        </div>
      </section>

      <section id="faq" class="faq">
        <div class="section-shell faq__layout">
          <div>
            <p class="eyebrow">
              <CircleHelp :size="15"/>
              FAQ
            </p>
            <h2>出发前，<br/>你可能还想知道</h2>
          </div>
          <div class="faq__list">
            <article v-for="(item, index) in faqs" :key="item.question">
              <div class="faq__question">
                <span>0{{ index + 1 }}</span><strong>{{ item.question }}</strong>
              </div>
              <p>{{ item.answer }}</p>
            </article>
          </div>
        </div>
      </section>

      <section class="final-cta">
       <div class="logo">
         <img src="../../../public/logo.png" alt="" width="100">
       </div>
        <p class="eyebrow">
          <MapPin :size="15"/>
          NEXT DEPARTURE
        </p>
        <h2>下一段开源路线，<br/><em>等你来命名。</em></h2>
        <p>带上好奇心就够了。剩下的，我们在路上一起学。</p>
        <button class="button button--primary" type="button" @click="goToApply">
          立即报名，加入远征
          <ArrowRight :size="18"/>
        </button>
      </section>
    </main>

    <footer>
      <div class="footer-brand">
        <img src="/logo.png" alt="JMI-OPENATOM Logo" />
        <span>JMI-OPENATOM · 开放原子开源社团</span>
      </div>

      <div class="footer-info">
    <span>
      © 2025-2027 JMI-OPENATOM &
      <a href="https://www.ariven.cn/" target="_blank">
        Ariven（软件技术252301 何治皓）
      </a>
      . All rights reserved.
    </span>

        <span class="ai-credit">
      部分图片素材由
      <strong>ChatGPT 5.6</strong>
      AI 辅助完成
    </span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import {computed, markRaw, nextTick, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {
  ArrowRight, ArrowUpRight, Boxes, ChevronDown, CircleHelp, Code2, Compass, Flag,
  Footprints, GitBranch, GitPullRequest, HeartHandshake, Laptop2, Lightbulb, MapPin, Palette,
  Route, Send, Sparkles, Trophy, Users, Megaphone, CalendarDays,ArrowLeft
} from 'lucide-vue-next'
import {siteApi} from '@/api'
import HomeMapSection from '@/components/site/home/HomeMapSection.vue'
import gsap from 'gsap'
import {ScrollTrigger} from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const router = useRouter()
const pageRoot = ref<HTMLElement>()
const club = ref<Record<string, any>>({})
const metrics = ref<any[]>([])
const focusAreas = ref<any[]>([])
const showcaseApps = ref<any[]>([])
const activeChapter = ref(0)
const routeStoryRefs = ref<HTMLElement[]>([])
const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
let scrollFrame = 0
let routeObserver: IntersectionObserver | undefined
let motionContext: gsap.Context | undefined

const chapters = [
  {id: 'top', label: '从校园出发'},
  {id: 'routes', label: '选择路线'},
  {id: 'projects', label: '真实项目'},
  {id: 'journey', label: '加入旅程'},
]

const fallbackPaths = [
  {
    title: '项目部',
    description: '做产品、写代码，把真实需求交付上线。',
    hint: 'ENGINEERING',
    mission: '从一个影响真实用户的小问题开始，理解上下文并提交修复。',
    steps: ['认识项目与协作规范', '认领一个 beginner issue', '提交并讲清第一次改动'],
    result: '一次被合并的 PR',
    nonTechnical: '做需求访谈、测试体验、写清楚文档，帮项目把正确的事做对。',
    fit: '喜欢拆解问题，也愿意耐心验证每一个细节。',
    growth: '产品判断、工程协作与稳定交付能力。',
    icon: markRaw(Code2)
  },
  {
    title: '宣传组',
    description: '把复杂技术讲清楚，让作品被更多人看见。',
    hint: 'DESIGN & CONTENT',
    mission: '为一个正在进行的项目设计内容，让陌生人三分钟看懂它的价值。',
    steps: ['采访项目成员', '梳理核心故事', '完成一篇发布内容'],
    result: '一次真实的项目发布',
    nonTechnical: '写文案、拍照片、做海报、剪短视频，把技术讲成大家听得懂的故事。',
    fit: '对文字、画面或传播有感觉，愿意理解技术背后的价值。',
    growth: '内容策划、视觉表达与完整叙事能力。',
    icon: markRaw(Palette)
  },
  {
    title: '活动部',
    description: '策划分享与工作坊，把学习变成共同经历。',
    hint: 'EVENTS',
    mission: '参与一次技术活动的全流程，让每位到场者都能带走一个成果。',
    steps: ['共创活动议题', '负责一段现场流程', '完成复盘与资料沉淀'],
    result: '一场完整的技术活动',
    nonTechnical: '做主持、场务、签到、摄影和复盘，让每个人都能舒服地参与进来。',
    fit: '享受把人聚在一起，面对变化时仍能保持清晰。',
    growth: '活动策划、现场推进与多人协作能力。',
    icon: markRaw(CalendarDays)
  },
  {
    title: '外联部',
    description: '连接校内外资源，让合作产生更大影响。',
    hint: 'OUTREACH',
    mission: '找到一个值得连接的伙伴，发起一场对双方都有价值的合作。',
    steps: ['研究合作对象', '准备清晰的合作提案', '推动一次真实对话'],
    result: '一次有效的合作连接',
    nonTechnical: '写合作邮件、做关系维护、整理资源，让好项目遇见合适的人。',
    fit: '愿意主动沟通，擅长发现不同人之间可以合作的部分。',
    growth: '商务表达、资源判断与长期关系维护能力。',
    icon: markRaw(Megaphone)
  },
  {
    title: '社区部',
    description: '维护社群体验，让每位成员都能找到连接与归属。',
    hint: 'COMMUNITY',
    mission: '从一次新人共创开始，设计让陌生成员自然认识、持续参与的社区体验。',
    steps: ['观察新人的真实需求', '策划一次社区共创', '沉淀可复用的社区机制'],
    result: '一次有温度的社区行动',
    nonTechnical: '照顾新人感受、组织轻量活动、维护群内秩序，让社区真正有人情味。',
    fit: '在意他人的参与感，希望把陌生人变成可靠的同行者。',
    growth: '社区运营、成员洞察与机制设计能力。',
    icon: markRaw(HeartHandshake)
  },
]

const fallbackProjects = [
  {
    title: 'openatom-system',
    category: 'PLATFORM · OPEN SOURCE',
    description: '支撑社团成员、活动与内容协作的校园开源平台。',
    impact: '让社团日常协作更清晰、更可靠',
    icon: markRaw(Boxes)
  },
  {
    title: '校园网站发布工具',
    category: 'TOOLS · DEVOPS',
    description: '让校园项目从提交代码到上线发布更顺畅。',
    impact: '缩短从想法到真实可用的距离',
    icon: markRaw(GitBranch)
  },
  {
    title: 'AI 实验计划',
    category: 'RESEARCH · AI',
    description: '把前沿技术变成可以理解、复用和验证的实验。',
    impact: '把学习过程沉淀为可复用成果',
    icon: markRaw(Sparkles)
  },
]

const journey = [
  {
    day: 'DAY 01',
    title: '认识社区',
    description: '见到伙伴、了解项目，也说说你想尝试什么。',
    output: '找到你的第一张桌子'
  },
  {day: 'WEEK 01', title: '完成热身', description: '在带教伙伴帮助下，跑通工具与协作流程。', output: '完成第一次 commit'},
  {day: 'WEEK 02', title: '进入现场', description: '认领一个真实但边界清晰的任务，开始共建。', output: '提交第一个成果'},
  {day: 'DAY 30', title: '讲述你的成果', description: '复盘过程，把经验留给下一位出发的人。', output: '成为可靠的同行者'},
]

const gains = [
  {title: '完成事情的能力', description: '需求、协作、交付、复盘，不只停在“学过”。', icon: markRaw(Trophy)},
  {title: '看得见的作品', description: '真实项目、公开贡献和能够讲清楚的成长证据。', icon: markRaw(GitPullRequest)},
  {title: '一起走的人', description: '跨年级、跨方向的伙伴，以及愿意带你的前辈。', icon: markRaw(Users)},
  {title: '更大的视野', description: '从校园问题出发，认识真正的开源协作方式。', icon: markRaw(Compass)},
]

const process = [
  {title: '在线报名', description: '告诉我们你是谁、为什么想来'},
  {title: '开放交流', description: '聊兴趣和期待，不做知识背诵'},
  {title: '方向匹配', description: '认识部门与未来的带教伙伴'},
  {title: '正式出发', description: '加入社区，领取第一份路线图'},
]

const faqs = [
  {
    question: '我没有基础，也可以加入吗？',
    answer: '可以。我们更看重好奇心、可靠程度和持续投入。每个方向都有适合新人的第一份任务，也会安排带教伙伴。'
  },
  {
    question: '每周大概要投入多少时间？',
    answer: '建议每周留出 3–5 小时参与项目、例会或活动。考试周可以提前沟通，真实协作也包括尊重彼此的节奏。'
  },
  {
    question: '只能选择技术方向吗？',
    answer: '不是。项目、内容设计、活动策划和对外合作同样重要。你也可以先选一个主方向，再在项目中跨方向协作。'
  },
  {
    question: '加入后会有人带吗？',
    answer: '会。新人阶段有清晰的热身任务、带教伙伴和阶段复盘，但我们不会替你完成——目标是让你真正拥有独立做成事情的能力。'
  },
]

const heroMetrics = computed(() => {
  if (metrics.value.length) return metrics.value.slice(0, 3).map((item) => ({
    value: item.value || '—',
    label: item.label || item.note || '开放协作'
  }))
  return [{value: '68+', label: '在册成员'}, {value: '5', label: '成长方向'}, {value: '∞', label: '开放协作'}]
})

const paths = computed(() => {
  if (!focusAreas.value.length) return fallbackPaths
  return fallbackPaths.map((item, index) => ({
    ...item,
    title: focusAreas.value[index]?.title || item.title,
    description: focusAreas.value[index]?.description || item.description
  }))
})

const featuredProjects = computed(() => {
  if (!showcaseApps.value.length) return fallbackProjects
  return fallbackProjects.map((fallback, index) => ({
    ...fallback,
    title: showcaseApps.value[index]?.name || showcaseApps.value[index]?.title || fallback.title,
    description: showcaseApps.value[index]?.description || fallback.description,
    category: showcaseApps.value[index]?.category || fallback.category,
  }))
})

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({behavior: reducedMotion ? 'auto' : 'smooth'})
}

function goToApply() {
  router.push('/apply')
}

function  goback(){
  router.push('/')
}

function updateScrollStory() {
  scrollFrame = 0
  const viewportMark = window.scrollY + window.innerHeight * 0.45
  let chapter = 0
  chapters.forEach((item, index) => {
    const section = document.getElementById(item.id)
    if (section && section.offsetTop <= viewportMark) chapter = index
  })
  activeChapter.value = chapter

  const scrollable = Math.max(1, document.documentElement.scrollHeight - window.innerHeight)
  pageRoot.value?.style.setProperty('--story-progress', String(Math.min(1, window.scrollY / scrollable)))

}

function onScroll() {
  if (!scrollFrame) scrollFrame = window.requestAnimationFrame(updateScrollStory)
}

function setRouteStoryRef(element: Element | null) {
  if (element instanceof HTMLElement && !routeStoryRefs.value.includes(element)) routeStoryRefs.value.push(element)
}

function setupGsapMotion() {
  if (reducedMotion || !pageRoot.value) return
  const hero = pageRoot.value.querySelector<HTMLElement>('.hero')
  const routes = pageRoot.value.querySelector<HTMLElement>('.routes')
  const projectScene = pageRoot.value.querySelector<HTMLElement>('.project-scene')
  if (!hero || !routes || !projectScene) return

  motionContext = gsap.context(() => {
    const storyOrbLabel = pageRoot.value?.querySelector<HTMLElement>('.story-orb b')
    const routeStops = Array.from(pageRoot.value?.querySelectorAll<HTMLElement>('.route-story__stop') || [])
    const activateRoute = (index: number) => {
      storyOrbLabel && (storyOrbLabel.textContent = `R0${index + 1}`)
      routeStops.forEach((item, itemIndex) => item.classList.toggle('is-active', itemIndex === index))
    }

    const heroTimeline = gsap.timeline({
      scrollTrigger: {
        trigger: hero,
        start: 'top top',
        end: '+=1350',
        scrub: 1,
        pin: true,
        anticipatePin: 1,
        invalidateOnRefresh: true,
      },
    })
    heroTimeline.from('.hero__shade', {opacity: 0, immediateRender: false}, 0)
    heroTimeline.from('.hero__content', {y: 0, opacity: 1, immediateRender: false}, 0)
    heroTimeline.from('.hero h1', {scale: 1, y: 0, letterSpacing: '-.045em', immediateRender: false}, 0)
    heroTimeline.from('.hero h1 em', {backgroundPosition: '100% center', immediateRender: false}, 0)
    heroTimeline.from('.scroll-hint', {opacity: 1, y: 0, immediateRender: false}, 0)
    heroTimeline.from('.topo-texture', {xPercent: 0, yPercent: 0, rotation: 0, opacity: .18, immediateRender: false}, 0)
    heroTimeline.to('.hero__shade', {opacity: .94, ease: 'none'}, 0)
    heroTimeline.to('.hero__content', {y: -190, opacity: .16, ease: 'none'}, 0)
    heroTimeline.to('.hero h1', {scale: 1.16, y: -28, letterSpacing: '-.06em', ease: 'none'}, 0)
    heroTimeline.to('.hero h1 em', {backgroundPosition: '0% center', ease: 'none'}, 0)
    heroTimeline.to('.scroll-hint', {opacity: 0, y: 40, ease: 'none'}, 0)
    heroTimeline.to('.story-orb span', {rotation: 48, ease: 'none'}, 0)
    heroTimeline.to('.topo-texture', {xPercent: 24, yPercent: -18, rotation: 10, opacity: .42, ease: 'none'}, 0)

    gsap.timeline({
      scrollTrigger: {
        trigger: '.philosophy',
        start: 'top 78%',
        end: 'top 30%',
        scrub: 1,
        invalidateOnRefresh: true,
      },
    })
      .fromTo('.philosophy__content', {y: 90, opacity: .25, immediateRender: false}, {y: 0, opacity: 1, ease: 'none'}, 0)
      .fromTo('.philosophy h2', {scale: .78, y: 60, transformOrigin: 'left center', immediateRender: false}, {scale: 1, y: 0, ease: 'back.out(1.35)'}, .04)
      .fromTo('.philosophy__content > p', {y: 40, opacity: .3, immediateRender: false}, {y: 0, opacity: 1, ease: 'power2.out'}, .1)
      .to('.story-orb span', {rotation: 150, ease: 'none'}, 0)

    gsap.timeline({
      scrollTrigger: {
        trigger: '.founding-year',
        start: 'top 78%',
        end: 'top 28%',
        scrub: 1,
        invalidateOnRefresh: true,
      },
    })
      .fromTo('.founding-year__number', {scale: .5, opacity: .1, immediateRender: false}, {scale: 1, opacity: 1, ease: 'power2.out'}, 0)
      .fromTo('.founding-year__label', {y: 30, opacity: 0, immediateRender: false}, {y: 0, opacity: 1, ease: 'power2.out'}, .06)
      .fromTo('.founding-year__content > p', {y: 24, opacity: 0, immediateRender: false}, {y: 0, opacity: 1, ease: 'power2.out'}, .12)
      .to('.story-orb span', {rotation: 210, ease: 'none'}, 0)

    gsap.timeline({
      scrollTrigger: {
        trigger: routes,
        start: 'top 82%',
        end: 'top 28%',
        scrub: 1,
      },
    })
      .fromTo('.routes .chapter-heading', {y: 110, opacity: .65, immediateRender: false}, {y: 0, opacity: 1, ease: 'none'}, 0)
      .fromTo('.routes .chapter-heading h2', {scale: .82, y: 70, transformOrigin: 'left center', immediateRender: false}, {scale: 1, y: 0, ease: 'back.out(1.35)'}, 0)
      .to('.story-orb span', {rotation: 90, ease: 'none'}, 0)

    pageRoot.value.querySelectorAll<HTMLElement>('.route-story__stop').forEach((stop, index) => {
      const introFrom = index === 1 || index === 3 ? {x: 150, opacity: .45, immediateRender: false} : {x: -150, opacity: .45, immediateRender: false}
      const missionFrom = index === 2 ? {y: 130, opacity: .45, immediateRender: false} : {x: index === 0 ? 180 : -180, opacity: .45, immediateRender: false}
      const scene = gsap.timeline({
        scrollTrigger: {
          trigger: stop,
          start: 'top 78%',
          end: 'top 24%',
          scrub: .8,
          invalidateOnRefresh: true,
        },
      })
      scene
        .fromTo(stop.querySelector('.route-story__marker'), {y: index === 2 ? 140 : 70, opacity: .45, scale: index === 4 ? .72 : .62, rotation: index === 2 ? -18 : 0, immediateRender: false}, {y: 0, opacity: 1, scale: 1, rotation: 0, ease: 'power2.out'}, 0)
        .fromTo(stop.querySelector('.route-story__intro'), introFrom, {x: 0, opacity: 1, ease: 'power2.out'}, .06)
        .fromTo(stop.querySelector('.route-story__intro h3'), {y: 60, scale: .72, opacity: .3, backgroundPosition: '100% center', transformOrigin: 'left center', immediateRender: false}, {y: 0, scale: 1, opacity: 1, backgroundPosition: '0% center', ease: 'back.out(1.5)'}, .08)
        .fromTo(stop.querySelector('.route-story__mission'), missionFrom, {x: 0, y: 0, opacity: 1, ease: 'power2.out'}, .12)
        .fromTo(stop.querySelector('.route-story__nontech'), {y: 80, opacity: .45, immediateRender: false}, {y: 0, opacity: 1, ease: 'power2.out'}, .2)
        .fromTo(stop.querySelector('.route-story__result'), {scale: .72, opacity: .5, immediateRender: false}, {scale: 1, opacity: 1, ease: 'power2.out'}, .2)
        .to('.story-orb span', {rotation: 120 + index * 42, ease: 'none'}, 0)

      ScrollTrigger.create({
        trigger: stop,
        start: 'top 58%',
        end: 'bottom 42%',
        onEnter: () => activateRoute(index),
        onEnterBack: () => activateRoute(index),
        onLeaveBack: () => index > 0 ? activateRoute(index - 1) : storyOrbLabel && (storyOrbLabel.textContent = '00'),
      })
    })

    const projectTimeline = gsap.timeline({
      scrollTrigger: {
        trigger: projectScene,
        start: 'top top',
        end: '+=1050',
        scrub: 1,
        pin: true,
        anticipatePin: 1,
        invalidateOnRefresh: true,
        onEnter: () => storyOrbLabel && (storyOrbLabel.textContent = 'PRJ'),
        onEnterBack: () => storyOrbLabel && (storyOrbLabel.textContent = 'PRJ'),
        onLeaveBack: () => activateRoute(4),
      },
    })
    projectTimeline
      .to('.project-scene__image', {scale: 1.3, xPercent: -8, yPercent: -9, ease: 'none'}, 0)
      .fromTo('.project-scene__content', {y: 160, opacity: .2}, {y: 0, opacity: 1, ease: 'none'}, 0)
      .fromTo('.project-scene .chapter-heading h2', {scale: .7, y: 90, transformOrigin: 'left center'}, {scale: 1, y: 0, ease: 'back.out(1.35)'}, 0)
      .fromTo('.project-list article', {y: 90, opacity: .1}, {y: 0, opacity: 1, stagger: .12, ease: 'power2.out'}, .2)
      .to('.project-scene__shade', {opacity: .42, ease: 'none'}, 0)
      .to('.story-orb span', {rotation: 320, ease: 'none'}, 0)

    gsap.to('.story-orb span', {
      rotation: 360,
      ease: 'none',
      scrollTrigger: {trigger: '.journey', start: 'top 82%', end: 'top 32%', scrub: 1},
    })

    gsap.fromTo('.journey-line li', {y: 90, opacity: .2}, {
      y: 0,
      opacity: 1,
      stagger: .16,
      ease: 'power2.out',
      scrollTrigger: {
        trigger: '.journey-line', start: 'top 80%', end: 'top 24%', scrub: 1,
        onEnter: () => storyOrbLabel && (storyOrbLabel.textContent = '30D'),
        onEnterBack: () => storyOrbLabel && (storyOrbLabel.textContent = '30D'),
      },
    })
    gsap.fromTo('.gains article', {y: 70, opacity: .2}, {
      y: 0,
      opacity: 1,
      stagger: .12,
      ease: 'power2.out',
      scrollTrigger: {
        trigger: '.gains', start: 'top 78%', end: 'top 34%', scrub: 1,
        onEnter: () => storyOrbLabel && (storyOrbLabel.textContent = 'GAIN'),
        onEnterBack: () => storyOrbLabel && (storyOrbLabel.textContent = 'GAIN'),
      },
    })
    gsap.to('.story-orb span', {
      rotation: 410,
      ease: 'none',
      scrollTrigger: {trigger: '.gains-scene', start: 'top 82%', end: 'top 34%', scrub: 1},
    })
    gsap.fromTo('.community-story__image', {scale: 1.14, xPercent: 6}, {
      scale: 1.02,
      xPercent: -4,
      ease: 'none',
      scrollTrigger: {
        trigger: '.community-story', start: 'top bottom', end: 'bottom top', scrub: 1,
        onEnter: () => storyOrbLabel && (storyOrbLabel.textContent = 'TEAM'),
        onEnterBack: () => storyOrbLabel && (storyOrbLabel.textContent = 'TEAM'),
      },
    })
    gsap.fromTo('.community-story h2', {scale: .76, y: 100, opacity: .2, transformOrigin: 'left center'}, {
      scale: 1,
      y: 0,
      opacity: 1,
      ease: 'back.out(1.35)',
      scrollTrigger: {trigger: '.community-story', start: 'top 78%', end: 'top 32%', scrub: .8},
    })
    gsap.to('.story-orb span', {
      rotation: 470,
      ease: 'none',
      scrollTrigger: {trigger: '.community-story', start: 'top 80%', end: 'top 30%', scrub: 1},
    })
    gsap.fromTo('.final-cta h2', {scale: .78, y: 90, opacity: .2}, {
      scale: 1,
      y: 0,
      opacity: 1,
      ease: 'back.out(1.35)',
      scrollTrigger: {
        trigger: '.final-cta', start: 'top 82%', end: 'top 34%', scrub: .8,
        onEnter: () => storyOrbLabel && (storyOrbLabel.textContent = 'JOIN'),
        onEnterBack: () => storyOrbLabel && (storyOrbLabel.textContent = 'JOIN'),
      },
    })
    gsap.to('.story-orb span', {
      rotation: 540,
      ease: 'none',
      scrollTrigger: {trigger: '.final-cta', start: 'top 80%', end: 'top 28%', scrub: 1},
    })
    ScrollTrigger.refresh()
  }, pageRoot.value)
}

onMounted(async () => {
  window.scrollTo(0, 0)
  window.addEventListener('scroll', onScroll, {passive: true})
  updateScrollStory()

  await nextTick()
  ScrollTrigger.getAll().forEach(st => st.kill(false))
  setupGsapMotion()
  ScrollTrigger.refresh()

  const [homeResult, appsResult] = await Promise.allSettled([
    siteApi.clubHome(),
    siteApi.showcaseApps({page: 1, pageSize: 3}),
  ])
  if (homeResult.status === 'fulfilled') {
    club.value = homeResult.value?.club || {}
    metrics.value = homeResult.value?.metrics || []
    focusAreas.value = homeResult.value?.focusAreas || []
  }
  if (appsResult.status === 'fulfilled') showcaseApps.value = appsResult.value?.list || appsResult.value || []
  await nextTick()
  if (!reducedMotion && 'IntersectionObserver' in window) {
    routeObserver = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) entry.target.classList.add('is-visible')
      })
    }, {threshold: 0.22, rootMargin: '0px 0px -12% 0px'})
    routeStoryRefs.value.forEach((item) => routeObserver?.observe(item))
  } else {
    routeStoryRefs.value.forEach((item) => item.classList.add('is-visible'))
  }
  updateScrollStory()

  setTimeout(() => {
    ScrollTrigger.refresh()
  }, 600)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  if (scrollFrame) window.cancelAnimationFrame(scrollFrame)
  routeObserver?.disconnect()
  motionContext?.revert()
})
</script>

<style scoped>
.expedition-page {
  --bg: var(--oa-page-bg);
  --bg-soft: var(--oa-page-soft-bg);
  --surface: var(--oa-elevated-bg);
  --surface-strong: var(--oa-dark-tile);
  --mint: var(--oa-primary);
  --mint-strong: var(--oa-primary-dark);
  --cyan: var(--oa-muted-strong);
  --text: var(--oa-text);
  --muted: var(--oa-muted);
  --line: var(--oa-border);
  min-height: 100vh;
  background: var(--bg);
  color: var(--text);
  font-family: Inter, 'SF Pro Display', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.expedition-page * {
  box-sizing: border-box;
}

.expedition-page button, .expedition-page a {
  font: inherit;
}

.section-shell {
  width: min(1180px, calc(100% - 96px));
  margin: 0 auto;
}

.expedition-header {
  position: fixed;
  z-index: 100;
  top: 20px;
  right: 4vw;
  left: 4vw;
  display: flex;
  align-items: center;
  min-height: 56px;
  padding: 0 12px 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 18px;
  background: rgba(5, 11, 26, 0.88);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.22);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text);
  text-decoration: none;
}

.brand img {
  width: 34px;
  height: 34px;
  object-fit: contain;
}

.brand span {
  display: grid;
  gap: 1px;
}

.brand strong {
  font-size: 12px;
  letter-spacing: 0.12em;
}

.brand small {
  color: var(--muted);
  font-size: 9px;
  letter-spacing: 0.16em;
}

.expedition-header nav {
  display: flex;
  gap: 26px;
  margin-left: auto;
}

.expedition-header nav a {
  color: var(--muted);
  font-size: 12px;
  text-decoration: none;
  transition: color 180ms ease;
}

.expedition-header nav a:hover {
  color: var(--mint);
}

.header-cta, .button {
  border: 0;
  cursor: pointer;
  transition: transform 180ms ease, background 180ms ease, color 180ms ease;
}

.header-cta {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-left: 28px;
  min-height: 38px;
  padding: 0 18px;
  border-radius: 999px;
  background: var(--mint);
  color: #06112c;
  font-size: 12px;
  font-weight: 700;
}

.header-cta:hover, .button:hover {
  transform: translateY(-2px);
}

.header-cta:focus-visible, .button:focus-visible {
  outline: 2px solid var(--mint);
  outline-offset: 4px;
}

.chapter-rail {
  position: fixed;
  z-index: 60;
  top: 50%;
  left: 26px;
  display: grid;
  gap: 13px;
  transform: translateY(-50%);
}

.chapter-rail::before, .chapter-rail::after {
  content: '';
  position: absolute;
  z-index: -1;
  top: 28px;
  left: 3px;
  width: 1px;
  height: 72px;
  background: rgba(255, 255, 255, .12);
}

.chapter-rail::after {
  height: calc(72px * var(--story-progress, 0));
  background: var(--mint);
}

.chapter-rail__label {
  color: rgba(255, 255, 255, .34);
  font: 8px/1 ui-monospace, monospace;
  letter-spacing: .18em;
  writing-mode: vertical-rl;
}

.chapter-rail button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, .32);
  cursor: pointer;
}

.chapter-rail button i {
  width: 7px;
  height: 7px;
  border: 1px solid currentColor;
  border-radius: 50%;
}

.chapter-rail button span {
  font: 9px/1 ui-monospace, monospace;
  opacity: 0;
  transition: opacity 180ms ease;
}

.chapter-rail button.active {
  color: var(--mint);
}

.chapter-rail button.active i {
  background: var(--mint);
  box-shadow: 0 0 0 5px rgba(114, 184, 255, .14);
}

.chapter-rail button.active span {
  opacity: 1;
}

.hero {
  position: relative;
  min-height: 100svh;
  display: flex;
  align-items: center;
  overflow: hidden;
  isolation: isolate;
  background: var(--surface-strong);
}

.hero__bg {
  position: absolute;
  inset: -20px;

  width: calc(100% + 40px);
  height: calc(100% + 40px);

  object-fit: cover;

  transform: scale(1.05);

  z-index:0;
  pointer-events:none;
}

.hero__image, .project-scene__image, .community-story__image {
  position: absolute;
  inset: -8%;
  width: 116%;
  height: 116%;
  object-fit: cover;
  will-change: transform;
}

.parallax-layer {
  transform: translate3d(0, var(--parallax-y, 0px), 0) scale(1.08);
}

.hero__shade, .project-scene__shade, .community-story__shade {
  position: absolute;
  z-index: 5;
  inset: 0;
  pointer-events: none;
  background: rgba(4, 22, 52, 0.52);
}

.topo-texture {
  position: absolute;
  inset: auto -4% -4% auto;
  width: 52%;
  height: 46%;
  opacity: .18;
  background: url('/recruitment-atlas.png') center/cover no-repeat;
  filter: invert(1) hue-rotate(70deg);
  mix-blend-mode: screen;
}

.hero__content {
  position: relative;
  z-index: 999;

  width: min(1200px, 90vw);

  margin-left: 8vw;
  margin-right: 0;

  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.eyebrow {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 0 22px;
  color: var(--mint);
  font: 700 11px/1.3 ui-monospace, SFMono-Regular, monospace;
  letter-spacing: .12em;
}

.hero h1, .chapter-heading h2, .community-story h2, .process h2, .faq h2, .final-cta h2 {
  margin: 0;
  letter-spacing: -.045em;
}

.hero h1 {
  max-width: 750px;
  font-size: clamp(60px, 7vw, 108px);
  line-height: .98;
}

.hero h1 em, .community-story h2 em, .final-cta h2 em {
  color: var(--mint);
  font-style: normal;
}

.hero__lead {
  max-width: 640px;
  margin: 34px 0 0;
  color: #c1cbca;
  font-size: 17px;
  line-height: 1.9;
}

.hero__promise {
  max-width: 620px;
  margin: 18px 0 0;
  padding-top: 18px;
  border-top: 1px solid var(--line);
  color: var(--oa-muted-strong);
  font-size: 13px;
  line-height: 1.7;
}

.hero__actions {
  display: flex;
  gap: 12px;
  margin-top: 36px;
}

.button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-height: 50px;
  padding: 0 24px;
  border-radius: 999px;
  font-weight: 700;
}

.button--primary {
  background: var(--mint);
  color: #06112c;
  box-shadow: 0 16px 40px rgba(64, 221, 136, .18);
}

.button--ghost {
  border: 1px solid rgba(255, 255, 255, .18);
  background: rgba(5, 11, 26, .58);
  color: var(--text);
}

.button--ghost:hover {
  border-color: var(--mint);
  color: var(--mint);
}

.hero__metrics {
  display: flex;
  gap: 50px;
  margin-top: 54px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, .14);
  width: min(620px, 100%);
}

.hero__metrics div {
  display: grid;
  gap: 5px;
}

.hero__metrics strong {
  font-size: 25px;
}

.hero__metrics span {
  color: var(--muted);
  font-size: 11px;
}

.hero__start {
  position: absolute;
  z-index: 3;
  right: 18%;
  bottom: 28%;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--mint);
}

.hero__start > svg {
  padding: 12px;
  width: 46px;
  height: 46px;
  border: 1px solid var(--mint);
  border-radius: 50%;
  background: rgba(5, 11, 26, .78);
  box-shadow: 0 0 0 12px rgba(114, 184, 255, .08);
}

.hero__start span {
  display: grid;
  gap: 3px;
}

.hero__start b {
  font: 700 11px/1 ui-monospace, monospace;
}

.hero__start small {
  color: #c1cbca;
  font-size: 10px;
}

.scroll-hint {
  position: absolute;
  z-index: 3;
  left: 50%;
  bottom: 28px;
  display: flex;
  align-items: center;
  gap: 8px;
  transform: translateX(-50%);
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, .48);
  font: 10px/1 ui-monospace, monospace;
  letter-spacing: .12em;
  cursor: pointer;
}

.routes, .journey {
  min-height: 100svh;
  padding: 150px 0;
  background: var(--bg);
}

.routes {
  padding-bottom: 0;
}

.journey {
  display: flex;
  align-items: center;
}

.routes > .section-shell {
  padding-top: 26px;
}

.chapter-heading {
  display: grid;
  grid-template-columns: 150px minmax(0, 760px);
  align-items: start;
  gap: 30px;
  margin-bottom: 72px;
}

.chapter-number {
  color: rgba(114, 184, 255, .18);
  font: 700 118px/.8 ui-monospace, monospace;
  letter-spacing: -.08em;
}

.chapter-heading h2 {
  font-size: clamp(44px, 4.6vw, 68px);
  line-height: 1.04;
}

.chapter-heading > div > p:last-child {
  max-width: 650px;
  margin: 22px 0 0;
  color: var(--muted);
  font-size: 16px;
  line-height: 1.8;
}

.parallax-copy {
  will-change: transform;
  transform: translate3d(0, var(--parallax-y, 0px), 0);
}

.route-story {
  position: relative;
  border-top: 1px solid var(--line);
}

.route-story::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 43px;
  width: 1px;
  background: var(--line);
}

.route-story__stop {
  position: relative;
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) minmax(360px, .9fr);
  grid-template-areas:
    'marker intro mission'
    'marker nontech result';
  align-items: center;
  gap: 36px;
  min-height: 100svh;
  padding: 120px 28px 100px 0;
  border-bottom: 1px solid var(--line);
  scroll-margin-top: 20px;
  --reveal-y: 0px;
  opacity: 1;
  transform: translate3d(0, var(--parallax-y, 0px), 0);
  transition: opacity 700ms ease, transform 700ms ease;
  isolation: isolate;
  overflow: hidden;
}

.route-story__marker { grid-area: marker; }
.route-story__intro { grid-area: intro; }
.route-story__mission { grid-area: mission; }
.route-story__result { grid-area: result; }
.route-story__nontech { grid-area: nontech; }

.route-story__stop--2 {
  grid-template-columns: minmax(360px, .9fr) 120px minmax(0, 1fr);
  grid-template-areas:
    'mission marker intro'
    'result marker nontech';
}

.route-story__stop--3 {
  grid-template-columns: 180px minmax(0, 1fr) minmax(300px, .7fr);
  grid-template-areas:
    'marker intro mission'
    'marker result nontech';
}

.route-story__stop--4 {
  grid-template-columns: minmax(0, 1fr) 120px minmax(360px, .9fr);
  grid-template-areas:
    'intro marker mission'
    'nontech marker result';
}

.route-story__stop--5 {
  grid-template-columns: 120px minmax(0, .85fr) minmax(360px, 1.1fr);
  grid-template-areas:
    'marker intro mission'
    'marker result nontech';
}

.route-story__stop--2 .route-story__mission {
  border-left-color: var(--cyan);
  padding-left: 28px;
}

.route-story__stop--3 .route-story__mission {
  border-left: 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  padding: 18px 0;
}

.route-story__stop--4 .route-story__result {
  justify-self: end;
  text-align: right;
}

.route-story__stop--5 .route-story__nontech {
  background: transparent;
  border-color: rgba(114, 184, 255, .3);
}

.route-story__stop::after {
  content: '';
  position: absolute;
  z-index: -1;
  inset: 8% -10% -18% 34%;
  background: url('/recruitment-atlas.png') center / cover no-repeat;
  opacity: .055;
  filter: invert(1) hue-rotate(140deg);
  pointer-events: none;
}

.route-story__stop > * {
  position: relative;
  z-index: 1;
}

.route-story__stop:nth-child(odd) {
  background: var(--bg);
}

.route-story__stop:nth-child(even) {
  background: transparent;
}

.route-story__stop.is-visible {
  --reveal-y: 0px;
  opacity: 1;
}

.route-story__stop:nth-child(even) {
  margin-left: 0;
}

.route-story__marker {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 20px;
  color: var(--mint);
}

.route-story__marker::before {
  content: '';
  position: absolute;
  top: 23px;
  width: 34px;
  height: 34px;
  border: 1px solid var(--mint);
  border-radius: 50%;
  background: var(--bg);
  box-shadow: 0 0 0 9px rgba(114, 184, 255, .08);
}

.route-story__marker svg {
  position: relative;
  margin-top: 2px;
}

.route-story__marker span, .route-story__intro small {
  color: var(--mint);
  font: 10px/1 ui-monospace, monospace;
  letter-spacing: .1em;
}

.route-story__intro h3 {
  margin: 13px 0 12px;
  font-size: clamp(44px, 4.5vw, 68px);
  line-height: 1;
  letter-spacing: -.045em;
}

.route-story__intro p, .route-story__mission > p {
  margin: 0;
  color: var(--muted);
  font-size: 15px;
  line-height: 1.75;
}

.route-story__details {
  display: grid;
  gap: 14px;
  margin: 30px 0 0;
}

.route-story__details > div {
  display: grid;
  grid-template-columns: 116px 1fr;
  gap: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.route-story__details dt {
  color: var(--oa-faint);
  font: 9px/1.5 ui-monospace, SFMono-Regular, monospace;
  letter-spacing: .08em;
}

.route-story__details dd {
  margin: 0;
  color: var(--oa-muted-strong);
  font-size: 12px;
  line-height: 1.65;
}

.route-story__mission {
  padding: 18px 0 18px 24px;
  border-left: 2px solid var(--mint);
  background: transparent;
}

.route-story__mission > span {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--mint);
  font: 700 10px/1 ui-monospace, monospace;
  letter-spacing: .08em;
}

.route-story__mission > p {
  margin-top: 16px;
  color: #c7d0ce;
  font-size: 16px;
}

.route-story__mission ol {
  display: grid;
  gap: 9px;
  margin: 19px 0 0;
  padding: 0;
  list-style: none;
}

.route-story__mission li {
  display: flex;
  gap: 10px;
  color: var(--muted);
  font-size: 12px;
}

.route-story__mission li b {
  color: var(--mint);
  font: 9px/1.5 ui-monospace, monospace;
}

.route-story__result {
  display: flex;
  gap: 13px;
  color: var(--mint);
}

.route-story__result span {
  display: grid;
  gap: 7px;
}

.route-story__result small {
  color: var(--muted);
  font-size: 10px;
  line-height: 1.5;
}

.route-story__result strong {
  font-size: 14px;
  line-height: 1.5;
}

.route-story__nontech {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  max-width: 660px;
  padding: 18px 0 0;
  border-top: 1px solid var(--line);
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

.route-story__nontech svg {
  flex: 0 0 auto;
  color: var(--cyan);
  margin-top: 2px;
}

.route-story__nontech span {
  display: grid;
  gap: 5px;
}

.route-story__nontech b {
  color: var(--cyan);
  font-size: 11px;
}

.project-scene {
  position: relative;
  min-height: 100svh;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.project-scene__shade {
  background: rgba(4, 18, 43, .58);
}

.project-scene__content {
  position: relative;
  z-index: 6;
}

.chapter-heading--light {
  margin-bottom: 54px;
}

.chapter-heading--light > div > p:last-child {
  color: #c0cac8;
}

.project-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-left: 180px;
  border-top: 1px solid rgba(255, 255, 255, .14);
  border-bottom: 1px solid rgba(255, 255, 255, .14);
  background: rgba(5, 11, 26, .82);
  backdrop-filter: blur(16px);
}

.project-list article {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 28px;
  border-right: 1px solid rgba(255, 255, 255, .12);
}

.project-list article:last-child {
  border-right: 0;
}

.project-list article > span, .project-list article > small {
  color: var(--mint);
  font: 9px/1.4 ui-monospace, monospace;
  letter-spacing: .08em;
}

.project-list article > svg {
  margin: 34px 0 18px;
  color: var(--mint);
}

.project-list h3 {
  margin: 0 0 12px;
  font-size: 21px;
}

.project-list p {
  margin: 0;
  color: #bac5c3;
  font-size: 13px;
  line-height: 1.75;
}

.project-list article > strong {
  margin: 22px 0 0;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, .16);
  color: #f5f5f7;
  font-size: 12px;
  line-height: 1.6;
}

.project-list article > small {
  margin-top: auto;
}

.journey-line {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin: 0;
  padding: 0;
  list-style: none;
  border-top: 1px solid var(--line);
}

.journey-line li {
  position: relative;
  padding: 28px 28px 34px 0;
}

.journey-line li:not(:last-child) {
  border-right: 1px solid var(--line);
  margin-right: 28px;
}

.journey-line__day {
  color: var(--mint);
  font: 700 11px/1 ui-monospace, monospace;
}

.journey-line i {
  display: block;
  width: 9px;
  height: 9px;
  margin: -34px 0 36px;
  border-radius: 50%;
  background: var(--mint);
  box-shadow: 0 0 0 7px rgba(114, 184, 255, .12);
}

.journey-line small {
  color: var(--muted);
  font: 9px/1 ui-monospace, monospace;
  letter-spacing: .12em;
}

.journey-line h3 {
  margin: 16px 0 12px;
  font-size: 24px;
}

.journey-line p {
  min-height: 72px;
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.75;
}

.journey-line b {
  display: block;
  margin-top: 20px;
  color: var(--mint);
  font-size: 12px;
}

.gains {
  width: 100%;
  margin: 0;
}

.gains-scene {
  min-height: 100svh;
  display: flex;
  align-items: center;
  background: var(--surface);
}

.gains h2 {
  margin: 0;
  font-size: clamp(44px, 4.6vw, 68px);
  letter-spacing: -.045em;
}

.gains > div {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: 30px;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.gains article {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 28px;
  border-right: 1px solid var(--line);
}

.gains article:last-child {
  border-right: 0;
}

.gains svg {
  color: var(--mint);
}

.gains strong {
  margin: 26px 0 10px;
  font-size: 16px;
}

.gains article p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

.community-story {
  position: relative;
  min-height: 100svh;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.community-story__shade {
  background: rgba(4, 18, 43, .42);
}

.community-story__copy {
  position: relative;
  z-index: 6;
  width: min(1180px, calc(100% - 96px));
  margin: 0 auto;
}

.community-story h2 {
  max-width: 700px;
  font-size: clamp(48px, 5vw, 76px);
  line-height: 1.05;
}

.community-story blockquote {
  max-width: 540px;
  margin: 44px 0 16px;
  padding-left: 24px;
  border-left: 2px solid var(--mint);
  color: #d3d9d8;
  font-size: 15px;
  line-height: 1.9;
}

.community-story__copy > span {
  color: var(--mint);
  font: 10px/1 ui-monospace, monospace;
}

.process {
  padding: 140px 0;
  background: var(--surface);
}

.process__heading {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 80px;
  align-items: end;
}

.process h2, .faq h2 {
  font-size: clamp(48px, 5vw, 76px);
}

.process__heading p {
  margin: 0;
  color: var(--muted);
  font-size: 16px;
  line-height: 1.8;
}

.process__values {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: 48px;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.process__values span {
  padding: 18px 0;
  color: var(--oa-muted-strong);
  font-size: 12px;
  text-align: center;
}

.process__values span:not(:last-child) {
  border-right: 1px solid var(--line);
}

.process ol {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  padding: 0;
  margin: 64px 0 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  list-style: none;
}

.process li {
  min-height: 150px;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px;
  border-right: 1px solid var(--line);
}

.process li:last-child {
  border-right: 0;
}

.process li > span {
  color: var(--mint);
  font: 11px/1 ui-monospace, monospace;
}

.process li div {
  display: grid;
  gap: 8px;
}

.process li small {
  color: var(--muted);
  font-size: 11px;
  line-height: 1.5;
}

.process li > svg {
  margin-left: auto;
  color: rgba(255, 255, 255, .3);
}

.process__cta {
  margin-top: 36px;
}

.faq {
  min-height: 100svh;
  display: flex;
  align-items: center;
  padding: 140px 0;
}

.faq__layout {
  display: grid;
  grid-template-columns: .8fr 1.2fr;
  gap: 100px;
}

.faq__list {
  border-top: 1px solid var(--line);
}

.faq article {
  border-bottom: 1px solid var(--line);
}

.faq article {
  padding: 18px 0;
}

.faq__question {
  display: grid;
  grid-template-columns: 48px 1fr;
  align-items: center;
  gap: 12px;
  color: var(--text);
}

.faq__question > span {
  color: var(--mint);
  font: 10px/1 ui-monospace, monospace;
}

.faq__question strong {
  font-size: 16px;
}

.faq article p {
  margin: 10px 0 0;
  padding-left: 60px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.65;
}

.final-cta {
  min-height: 100svh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 150px 24px;
  background: var(--surface-strong);
  text-align: center;
}

.final-cta h2 {
  font-size: clamp(54px, 6vw, 88px);
  line-height: 1.02;
}

.final-cta > p:not(.eyebrow) {
  margin: 28px 0 34px;
  color: var(--muted);
}

footer {
  display: flex;
  justify-content: space-between;
  padding: 24px 5vw;
  border-top: 1px solid rgba(255, 255, 255, .08);
  color: rgba(255, 255, 255, .36);
  font: 9px/1 ui-monospace, monospace;
  letter-spacing: .12em;
}

/* Home visual language: no photo collage and no floating content cards. */
.story-orb {
  position: fixed;
  z-index: 30;
  top: 50%;
  right: clamp(22px, 4vw, 64px);
  width: 112px;
  aspect-ratio: 1;
  border: 1px solid color-mix(in srgb, var(--oa-text) 14%, transparent);
  border-radius: 50%;
  background: color-mix(in srgb, var(--oa-elevated-bg) 82%, transparent);
  box-shadow: 0 18px 45px rgba(0, 0, 0, .1);
  backdrop-filter: blur(18px) saturate(1.2);
  pointer-events: none;
  transform: translateY(-50%);
  will-change: transform, opacity;
}

.story-orb::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  background: conic-gradient(var(--oa-text) calc(var(--story-progress, 0) * 1turn), transparent 0);
  -webkit-mask: radial-gradient(circle, transparent 65%, #000 66% 69%, transparent 70%);
  mask: radial-gradient(circle, transparent 65%, #000 66% 69%, transparent 70%);
}

.story-orb span {
  position: absolute;
  inset: 13px;
  border: 1px solid color-mix(in srgb, var(--oa-text) 22%, transparent);
  border-radius: 50%;
}

.story-orb span::before {
  content: '';
  position: absolute;
  top: -4px;
  left: 50%;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--oa-text);
  transform: translateX(-50%);
}

.story-orb b {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--oa-text);
  font: 700 12px/1 ui-monospace, SFMono-Regular, monospace;
  letter-spacing: .14em;
}

.hero {
  background: var(--oa-hero-gradient);
  color: var(--text);
}

.hero__canvas {
  background: var(--oa-hero-gradient);
}

.hero__canvas::before {
  display: none;
}

.hero__canvas::after {
  display: none;
}

.hero__shade {
  background:
    linear-gradient(180deg, rgba(4, 18, 38, 0.78) 0%, rgba(4, 18, 38, 0.48) 62%, rgba(4, 18, 38, 0.22) 100%),
    linear-gradient(90deg, rgba(4, 18, 38, 0.6) 0%, transparent 52%);
}

.topo-texture {
  display: none;
}

.hero h1 em, .community-story h2 em {
  color: transparent;
  background: linear-gradient(105deg, var(--oa-text) 0 28%, var(--oa-faint) 46%, #b8b8bd 54%, var(--oa-text) 72% 100%);
  background-size: 220% auto;
  background-position: 100% center;
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.route-story__intro h3 {
  color: transparent;
  background: linear-gradient(105deg, var(--oa-text) 0 32%, var(--oa-faint) 48%, var(--oa-text) 66% 100%);
  background-size: 220% auto;
  background-position: 100% center;
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero .eyebrow, .eyebrow {
  color: var(--muted);
}

.hero__lead, .hero__metrics span, .hero__start small {
  color: var(--muted);
}

.button--primary {
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
  box-shadow: none;
}

.button--primary:hover {
  background: var(--oa-active-hover-bg);
}

.button--ghost {
  border-color: var(--oa-button-border);
  background: rgba(255, 255, 255, .62);
  color: var(--oa-button-text);
}

.button--ghost:hover {
  border-color: var(--oa-button-hover-border);
  background: var(--oa-button-hover-bg);
  color: var(--oa-button-hover-text);
}

.hero__metrics {
  border-top-color: var(--line);
}

.hero__start, .hero__start > svg {
  color: var(--text);
  border-color: var(--oa-border-strong);
}

.hero__start > svg {
  background: rgba(255, 255, 255, .76);
  box-shadow: 0 0 0 12px rgba(29, 29, 31, .05);
}

.scroll-hint {
  color: var(--oa-faint);
}

.chapter-rail {
  color: #fff;
  mix-blend-mode: difference;
}

.chapter-number {
  color: color-mix(in srgb, var(--text) 8%, transparent);
}

.route-story__stop::after {
  display: none;
}

.route-story__marker::before {
  box-shadow: 0 0 0 9px color-mix(in srgb, var(--text) 5%, transparent);
}

.route-story__mission > p {
  color: var(--oa-muted-strong);
}

.project-scene {
  background: #1d1d1f;
  color: #f5f5f7;
}

.project-scene__canvas {
  background: #1d1d1f;
}

.project-scene__shade {
  background: linear-gradient(90deg, rgba(29, 29, 31, .94), rgba(29, 29, 31, .28));
}

.project-scene .eyebrow, .project-scene .chapter-number {
  color: #a1a1a6;
}

.chapter-heading--light > div > p:last-child {
  color: #a1a1a6;
}

.project-list {
  border-color: rgba(255, 255, 255, .24);
  background: transparent;
  backdrop-filter: none;
}

.project-list article {
  border-right-color: rgba(255, 255, 255, .18);
  color: #f5f5f7;
}

.project-list article > span, .project-list article > small,
.project-list article > svg, .project-list p {
  color: #a1a1a6;
}

.journey-line i {
  box-shadow: 0 0 0 7px color-mix(in srgb, var(--text) 7%, transparent);
}

.gains-scene {
  background: var(--bg-soft);
}

.community-story {
  background: var(--oa-elevated-bg);
  color: var(--text);
}

.community-story__canvas {
  background: var(--oa-elevated-bg);
}

.community-story__shade {
  background: linear-gradient(90deg, var(--oa-elevated-bg) 0 58%, transparent 82%);
}

.community-story blockquote {
  border-left-color: var(--text);
  color: var(--oa-muted-strong);
}

.community-story__copy > span {
  color: var(--muted);
}

.process li > svg {
  color: var(--oa-faint);
}

.final-cta {
  color: #f5f5f7;
}

.final-cta h2 em {
  color: #a1a1a6;
}

.final-cta > p:not(.eyebrow) {
  color: rgba(255, 255, 255, .66);
}

.final-cta .button--primary {
  background: #fff;
  color: #1d1d1f;
}

footer {
  border-top-color: var(--oa-border);
  background: var(--oa-page-bg);
  color: var(--oa-faint);
}

.philosophy {
  position: relative;
  min-height: 100svh;
  display: flex;
  align-items: center;
  overflow: hidden;
  background: var(--surface-strong);
  isolation: isolate;
}

.philosophy__orb {
  position: absolute;
  z-index: 0;
  top: 50%;
  right: 8%;
  width: 440px;
  height: 440px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(114, 184, 255, 0.12), transparent 70%);
  filter: blur(60px);
  transform: translateY(-50%);
  pointer-events: none;
}

.philosophy__content {
  position: relative;
  z-index: 1;
  width: min(1180px, calc(100% - 96px));
  margin: 0 auto;
}

.philosophy h2 {
  margin: 0;
  font-size: clamp(48px, 5vw, 76px);
  line-height: 1.08;
  letter-spacing: -.045em;
}

.philosophy h2 em {
  color: var(--mint);
  font-style: normal;
}

.philosophy__content > p {
  max-width: 580px;
  margin: 26px 0 0;
  color: var(--muted);
  font-size: 17px;
  line-height: 1.8;
}

.founding-year {
  min-height: 80svh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 120px 24px;
  background: var(--bg-soft);
}

.founding-year__content {
  text-align: center;
}

.founding-year__label {
  color: var(--mint);
  font: 700 13px/1 ui-monospace, SFMono-Regular, monospace;
  letter-spacing: .18em;
}

.founding-year__number {
  display: block;
  margin: 16px 0 18px;
  font-size: clamp(100px, 16vw, 200px);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -.03em;
  color: transparent;
  background: linear-gradient(180deg, var(--text) 0%, var(--oa-muted-strong) 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.founding-year__content > p {
  color: var(--muted);
  font-size: 16px;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .expedition-header nav {
    display: none;
  }

  .chapter-rail {
    display: none;
  }

  .section-shell, .hero__contenthero__content, .community-story__copy {
    width: min(100% - 40px, 720px);
  }

  .hero {
    min-height: 840px;
  }

  .hero__image {
    inset: -4% -42% -4% -42%;
    width: 184%;
  }

  .hero__start {
    right: 10%;
    bottom: 20%;
  }

  .chapter-heading {
    grid-template-columns: 80px 1fr;
  }

  .chapter-number {
    font-size: 72px;
  }

  .journey-line, .gains > div, .process ol {
    grid-template-columns: repeat(2, 1fr);
  }

  .journey-line li:nth-child(2), .gains article:nth-child(2), .process li:nth-child(2) {
    border-right: 0;
  }

  .route-story::before {
    left: 17px;
  }

  .route-story__stop,
  .route-story__stop--2,
  .route-story__stop--3,
  .route-story__stop--4,
  .route-story__stop--5 {
    grid-template-columns: 42px 1fr;
    grid-template-areas:
      'marker intro'
      'marker mission'
      'marker result'
      'marker nontech';
    gap: 20px;
    padding: 60px 0;
  }

  .route-story__marker {
    align-self: start;
  }

  .route-story__marker span {
    writing-mode: vertical-rl;
  }

  .route-story__marker::before {
    top: 42px;
    width: 30px;
    height: 30px;
  }

  .route-story__marker svg {
    margin-top: 21px;
  }

  .route-story__mission, .route-story__result, .route-story__nontech {
    grid-column: 2;
  }

  .route-story__mission {
    padding: 20px 0;
    border-left: 0;
    border-top: 1px solid var(--line);
    border-bottom: 0;
  }

  .route-story__stop--2 .route-story__mission,
  .route-story__stop--3 .route-story__mission {
    border-left-color: transparent;
    padding-left: 0;
  }

  .route-story__result {
    padding-left: 0;
    justify-self: start;
    text-align: left;
  }

  .route-story__nontech {
    grid-column: 2;
  }

  .route-story__stop::after {
    inset: 8% -10% -18% 20%;
  }

  .route-story__details > div {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .project-list {
    margin-left: 0;
    grid-template-columns: 1fr;
  }

  .project-list article {
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid rgba(255, 255, 255, .12);
  }

  .project-list article:last-child {
    border-bottom: 0;
  }

  .project-scene {
    min-height: 1120px;
  }

  .project-scene__image {
    inset: -4% -50% -4% -10%;
    width: 160%;
  }

  .community-story__image {
    inset: -4% -65% -4% -10%;
    width: 175%;
  }

  .faq__layout {
    grid-template-columns: 1fr;
    gap: 56px;
  }
}

@media (max-width: 620px) {
  .expedition-header {
    top: 10px;
    right: 12px;
    left: 12px;
  }

  .brand small {
    display: none;
  }

  .header-cta {
    margin-left: auto;
    padding: 0 13px;
  }

  .hero {
    min-height: 780px;
    align-items: flex-end;
    padding-bottom: 92px;
  }

  .hero__image {
    inset: -3% -98% -3% -15%;
    width: 215%;
  }

  .hero h1 {
    font-size: 48px;
  }

  .hero__lead {
    font-size: 14px;
    line-height: 1.75;
  }

  .hero__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .hero__metrics {
    gap: 24px;
    margin-top: 34px;
  }

  .hero__start, .scroll-hint {
    display: none;
  }

  .routes, .journey, .process, .faq {
    padding: 90px 0;
  }

  .chapter-heading {
    grid-template-columns: 1fr;
    gap: 16px;
    margin-bottom: 44px;
  }

  .chapter-number {
    font-size: 62px;
  }

  .chapter-heading h2, .community-story h2, .process h2, .faq h2 {
    font-size: 42px;
  }

  .journey-line, .gains > div, .process ol, .project-list {
    grid-template-columns: 1fr;
  }

  .project-list article, .journey-line li, .journey-line li:not(:last-child), .gains article, .gains article:nth-child(2), .process li, .process li:nth-child(2) {
    border-right: 0;
  }

  .route-story::before {
    left: 17px;
  }

  .route-story__stop {
    grid-template-columns: 42px 1fr;
    grid-template-areas:
      'marker intro'
      'marker mission'
      'marker result'
      'marker nontech';
    gap: 16px;
    padding: 44px 0;
  }

  .route-story__marker {
    align-self: start;
  }

  .route-story__marker span {
    writing-mode: vertical-rl;
  }

  .route-story__marker::before {
    top: 42px;
    width: 30px;
    height: 30px;
  }

  .route-story__marker svg {
    margin-top: 21px;
  }

  .route-story__mission, .route-story__result, .route-story__nontech { grid-column: 2; }

  .route-story__mission {
    padding: 24px;
    border-left: 0;
  }

  .route-story__result {
    padding-left: 0;
  }

  .route-story__nontech {
    grid-column: 2;
  }

  .project-scene {
    min-height: 1380px;
    align-items: flex-start;
    padding-top: 110px;
  }

  .project-scene__image {
    inset: -2% -140% -2% -35%;
    width: 280%;
    opacity: .72;
  }

  .project-list {
    margin-top: 50px;
  }

  .project-list article {
    min-height: 250px;
    border-bottom: 1px solid rgba(255, 255, 255, .12);
  }

  .journey-line li {
    padding: 28px 0;
    border-bottom: 1px solid var(--line);
  }

  .journey-line i {
    margin: -34px 0 30px;
  }

  .journey-line p {
    min-height: 0;
  }

  .gains {
    margin-top: 70px;
  }

  .gains article {
    min-height: 190px;
    border-bottom: 1px solid var(--line);
  }

  .community-story {
    min-height: 800px;
    align-items: flex-end;
    padding-bottom: 72px;
  }

  .community-story__image {
    inset: -3% -125% -3% -18%;
    width: 250%;
  }

  .process__heading {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .process li {
    min-height: 118px;
    border-bottom: 1px solid var(--line);
  }

  .faq__question {
    grid-template-columns: 32px 1fr;
  }

  .faq article p {
    padding-left: 44px;
  }

  .final-cta h2 {
    font-size: 48px;
  }

  footer {
    flex-direction: column;
    gap: 10px;
  }
}

@media (max-width: 620px) {
  .story-orb {
    top: 50%;
    right: 14px;
    width: 76px;
  }

  .story-orb span {
    inset: 9px;
  }

  .story-orb b {
    font-size: 9px;
  }

  .hero__canvas, .project-scene__canvas, .community-story__canvas {
    inset: -3%;
    width: 106%;
    height: 106%;
    opacity: 1;
  }

  .hero__canvas::before {
    top: 10%;
    right: -18%;
    width: 260px;
    opacity: .88;
  }

  .hero__canvas::after {
    right: -8%;
    bottom: 20%;
    width: 92vw;
  }

  .hero__shade {
    background: linear-gradient(180deg, color-mix(in srgb, var(--oa-page-soft-bg) 70%, transparent), var(--oa-page-soft-bg) 48% 100%);
  }

  .route-story__intro h3 {
    font-size: 48px;
  }

  .route-story__details > div {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .route-story__mission {
    padding: 22px 0 0;
    border-top: 1px solid var(--line);
  }

  .project-scene__shade {
    background: linear-gradient(180deg, rgba(29, 29, 31, .84), rgba(29, 29, 31, .36));
  }

  .community-story__shade {
    background: linear-gradient(180deg, color-mix(in srgb, var(--oa-elevated-bg) 45%, transparent), var(--oa-elevated-bg) 56% 100%);
  }

  .process__values {
    grid-template-columns: repeat(2, 1fr);
  }

  .process__values span:nth-child(2) {
    border-right: 0;
  }

  .process__values span:nth-child(-n + 2) {
    border-bottom: 1px solid var(--line);
  }
}

@media (prefers-reduced-motion: reduce) {
  .expedition-page *, .expedition-page *::before, .expedition-page *::after {
    scroll-behavior: auto !important;
    transition-duration: .01ms !important;
    animation-duration: .01ms !important;
  }

  .parallax-layer, .parallax-copy, .route-story__stop {
    transform: none !important;
  }
}
.logo{
  width: 60px;
  height: 60px;
  border-radius: 20%;
  background-color: #fff;
  margin-bottom: 30px;
}

footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 28px 6%;
  background: rgba(6, 16, 20, .85);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(124,245,172,.15);
  color: #9eaaa8;
  font-size: 14px;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #f3f7f5;
  font-weight: 600;
}

.footer-brand img {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.footer-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

footer a {
  color: #7cf5ac;
  text-decoration: none;
  transition: .3s;
}

footer a:hover {
  color: #42db88;
}

.ai-credit {
  font-size: 13px;
  opacity: .8;
}

.ai-credit strong {
  color: #4ab8c8;
}
</style>
