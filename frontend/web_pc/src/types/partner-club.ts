export interface PartnerClub {
  id: number
  name: string
  logoUrl: string
  description: string
  websiteUrl?: string
  organization?: string
  category?: string
  presidentName?: string
  presidentAvatarUrl?: string
  tags?: string[]
  sortOrder: number
  featured: boolean
}
