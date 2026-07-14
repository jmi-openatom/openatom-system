export interface PartnerClub {
  id: number
  name: string
  logoUrl: string
  description: string
  websiteUrl: string
  organization?: string
  category?: string
  tags?: string[]
  sortOrder: number
  featured: boolean
}
