export interface MemberCard {
  slug: string
  displayName: string
  headline?: string
  avatarUrl?: string
  cardBackgroundUrl?: string
  cardFocusX?: number
  cardFocusY?: number
  departmentName?: string
  positionName?: string
  skills: string[]
  articleCount: number
  likeCount: number
  liked: boolean
  customized: boolean
}

export interface ProfileArticle {
  id: number
  title: string
  summary?: string
  coverUrl?: string
  publishedAt?: string
}

export interface ProfileModule {
  id?: number
  key: string
  title?: string
  sortOrder: number
  columnSpan: number
  enabled: boolean
  visibility: 'members' | 'private'
  data: Record<string, any>
}

export interface ProfileSocialLink {
  id?: number
  platform: string
  label?: string
  url: string
  sortOrder: number
  enabled: boolean
}

export interface MemberProfile {
  slug: string
  displayName: string
  headline?: string
  bio?: string
  avatarUrl?: string
  bannerUrl?: string
  cardBackgroundUrl?: string
  cardFocusX: number
  cardFocusY: number
  departmentName?: string
  positionName?: string
  skills: string[]
  themeKey: 'minimal' | 'tech' | 'warm' | 'editorial'
  colorMode: 'light' | 'dark' | 'system'
  visibility: 'members' | 'unlisted' | 'private'
  status: 'default' | 'draft' | 'published'
  commentsEnabled: boolean
  showDepartment: boolean
  showPosition: boolean
  version: number
  owner: boolean
  customized: boolean
  likeCount: number
  liked: boolean
  commentCount: number
  publishedAt?: string
  updatedAt?: string
  modules: ProfileModule[]
  socialLinks: ProfileSocialLink[]
}

export interface MemberPage {
  list: MemberCard[]
  page: number
  pageSize: number
  total: number
}

export interface MemberFilters {
  departments: Array<{ id: number; name: string }>
  skills: string[]
}
