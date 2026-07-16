export interface PartnerClub {
  id: number
  name: string
  logoUrl: string
  description: string
  websiteUrl?: string
  organization?: string
  category?: string
  presidentUserId?: number
  presidentName?: string
  presidentAvatarUrl?: string
  tags?: string[]
  sortOrder: number
  featured: boolean
}
