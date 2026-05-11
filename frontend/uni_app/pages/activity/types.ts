export interface ActivityItem {
  id?: number
  title?: string
  description?: string
  date?: string
  time?: string
  endDate?: string
  location?: string
  status?: string
  coverUrl?: string
  maxParticipants?: number
  currentParticipants?: number
  tags?: string[]
}

export type ActivityStatus = '' | 'published' | 'closed' | 'cancelled'

export interface ActivityFilter {
  label: string
  value: ActivityStatus
}
