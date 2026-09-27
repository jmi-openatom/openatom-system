const mainSite = String(import.meta.env.VITE_MAIN_SITE_URL || (import.meta.env.DEV
  ? 'http://localhost:5173'
  : 'https://www.jmi-openatom.cn')).replace(/\/$/, '')

export const siteExplorationPages = [
  { key: 'about', label: '关于我们', clue: '了解社团与新人成长路线', url: `${mainSite}/about` },
  { key: 'regulations', label: '规章制度', clue: '浏览公开的协作规范', url: `${mainSite}/regulations` },
  { key: 'activities', label: '社团活动', clue: '看看有哪些参与机会', url: `${mainSite}/activities` },
] as const
