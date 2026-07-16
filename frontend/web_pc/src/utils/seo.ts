import type { RouteLocationNormalizedLoaded } from 'vue-router'

const SITE_NAME = 'JMI-OPENATOM'
const SCHOOL_NAME = '江苏海事职业技术学院'
const DEFAULT_TITLE = `${SITE_NAME}｜${SCHOOL_NAME}开放原子开源社团`
const DEFAULT_DESCRIPTION = `JMI-OPENATOM 是${SCHOOL_NAME}开放原子开源社团，连接校园开发者，分享开源项目、技术活动、工程实践与社区动态。`
const SITE_URL = String(import.meta.env.VITE_SITE_URL || 'https://www.jmi-openatom.cn').replace(
  /\/$/,
  '',
)
const DEFAULT_IMAGE = `${SITE_URL}/logo.png`

type SeoConfig = {
  title?: string
  description?: string
  image?: string
  type?: 'website' | 'article'
  noindex?: boolean
  structuredData?: Record<string, unknown> | null
}

const ROUTE_SEO: Record<string, SeoConfig> = {
  'site-home': { title: DEFAULT_TITLE, description: DEFAULT_DESCRIPTION },
  about: {
    title: `关于我们｜${SITE_NAME}`,
    description: `了解${SCHOOL_NAME} JMI-OPENATOM 开放原子开源社团、共建方向、成长路径与校园开源实践。`,
  },
  'site-activities': {
    title: `技术活动｜${SITE_NAME}`,
    description: '浏览 JMI-OPENATOM 最新技术分享、工作坊、竞赛与开源社区活动。',
  },
  'site-activity-detail': { title: `活动详情｜${SITE_NAME}` },
  'site-apply': {
    title: `加入我们｜${SITE_NAME}`,
    description: '查看 JMI-OPENATOM 当前招新计划，选择适合你的方向并提交申请。',
  },
  'site-apply-detail': { title: `提交申请｜${SITE_NAME}`, noindex: true },
  'site-form-detail': { title: `在线表单｜${SITE_NAME}`, noindex: true },
  'site-blog': {
    title: `技术博客｜${SITE_NAME}`,
    description: '阅读社团成员的工程实践、竞赛复盘、技术教程与开源笔记。',
  },
  'site-blog-detail': { title: `文章详情｜${SITE_NAME}`, type: 'article' },
  'site-members': { title: `社团成员｜${SITE_NAME}`, noindex: true },
  'site-member-profile': { title: `成员主页｜${SITE_NAME}`, noindex: true },
  'site-profile': { title: `我的主页｜${SITE_NAME}`, noindex: true },
  'site-profile-edit': { title: `编辑主页｜${SITE_NAME}`, noindex: true },
  'site-account-settings': { title: `账号与安全｜${SITE_NAME}`, noindex: true },
  'site-regulations': {
    title: `制度与规范｜${SITE_NAME}`,
    description: '查阅 JMI-OPENATOM 社团章程、管理办法与日常协作规范。',
  },
  'site-regulation-detail': { title: `制度详情｜${SITE_NAME}` },
  'site-open-platform': {
    title: `开放平台｜${SITE_NAME}`,
    description: '了解 JMI-OPENATOM 开放平台能力、接口与校园开源服务。',
  },
  'site-apps': {
    title: `开源应用｜${SITE_NAME}`,
    description: '探索由 JMI-OPENATOM 社团成员开发和维护的校园开源应用。',
  },
  'site-app-detail': { title: `应用详情｜${SITE_NAME}` },
  'site-partners': {
    title: `伙伴社团｜${SITE_NAME}`,
    description: '认识与 JMI-OPENATOM 共同建设校园开源生态的伙伴社团。',
  },
  'site-calendar': {
    title: `校园日历｜${SITE_NAME}`,
    description: '查看校园日历与近期安排，快速了解重要日期。',
  },
  'site-alumni-managers': {
    title: `历届管理团队｜${SITE_NAME}`,
    description: '了解 JMI-OPENATOM 历届管理团队与社团传承。',
  },
  'site-votes': { title: `社区投票｜${SITE_NAME}` },
  'site-vote-detail': { title: `投票详情｜${SITE_NAME}` },
  'site-points': { title: `社区积分｜${SITE_NAME}` },
  'site-workspace': { title: `个人工作台｜${SITE_NAME}`, noindex: true },
  'site-progress': { title: `我的申请｜${SITE_NAME}`, noindex: true },
  'site-notifications': { title: `通知中心｜${SITE_NAME}`, noindex: true },
  'site-leaves': { title: `我的请假｜${SITE_NAME}`, noindex: true },
  'site-next': { title: `加入 JMI-OPENATOM｜${SCHOOL_NAME}开放原子开源社团` },
  'site-check-in-scan': { title: `扫码签到｜${SITE_NAME}`, noindex: true },
  'lottery-screen': { title: `抽奖大屏｜${SITE_NAME}`, noindex: true },
  'qr-screen': { title: `二维码大屏｜${SITE_NAME}`, noindex: true },
}

function upsertMeta(
  selector: string,
  attribute: 'name' | 'property',
  key: string,
  content: string,
) {
  let element = document.head.querySelector<HTMLMetaElement>(selector)
  if (!element) {
    element = document.createElement('meta')
    element.setAttribute(attribute, key)
    document.head.appendChild(element)
  }
  element.content = content
}

function upsertLink(rel: string, href: string) {
  let element = document.head.querySelector<HTMLLinkElement>(`link[rel="${rel}"]`)
  if (!element) {
    element = document.createElement('link')
    element.rel = rel
    document.head.appendChild(element)
  }
  element.href = href
}

function canonicalUrl(path: string) {
  const normalizedPath = path === '/' ? '/' : path.replace(/\/$/, '')
  return `${SITE_URL}${normalizedPath}`
}

function isPrivateRoute(route: RouteLocationNormalizedLoaded) {
  return (
    route.path.startsWith('/admin') ||
    route.matched.some((record) => Boolean(record.meta?.requiresSiteLogin)) ||
    ['login', 'auth-callback', 'site-activation', 'not-found'].includes(String(route.name || ''))
  )
}

export function updateSeo(config: SeoConfig = {}, path = window.location.pathname) {
  const title = config.title || DEFAULT_TITLE
  const description = config.description || DEFAULT_DESCRIPTION
  const image = config.image ? new URL(config.image, SITE_URL).href : DEFAULT_IMAGE
  const canonical = canonicalUrl(path)
  const robots = config.noindex
    ? 'noindex, nofollow, noarchive'
    : 'index, follow, max-image-preview:large'

  document.title = title
  upsertMeta('meta[name="description"]', 'name', 'description', description)
  upsertMeta('meta[name="robots"]', 'name', 'robots', robots)
  upsertMeta('meta[property="og:title"]', 'property', 'og:title', title)
  upsertMeta('meta[property="og:description"]', 'property', 'og:description', description)
  upsertMeta('meta[property="og:type"]', 'property', 'og:type', config.type || 'website')
  upsertMeta('meta[property="og:url"]', 'property', 'og:url', canonical)
  upsertMeta('meta[property="og:image"]', 'property', 'og:image', image)
  upsertMeta('meta[name="twitter:title"]', 'name', 'twitter:title', title)
  upsertMeta('meta[name="twitter:description"]', 'name', 'twitter:description', description)
  upsertMeta('meta[name="twitter:image"]', 'name', 'twitter:image', image)
  upsertLink('canonical', canonical)

  document.querySelector('#page-structured-data')?.remove()
  if (config.structuredData) {
    const script = document.createElement('script')
    script.id = 'page-structured-data'
    script.type = 'application/ld+json'
    script.text = JSON.stringify(config.structuredData).replace(/</g, '\\u003c')
    document.head.appendChild(script)
  }
}

export function updateRouteSeo(route: RouteLocationNormalizedLoaded) {
  const config = ROUTE_SEO[String(route.name || '')] || {}
  updateSeo({ ...config, noindex: config.noindex || isPrivateRoute(route) }, route.path)
}

export { SITE_NAME, SITE_URL }
