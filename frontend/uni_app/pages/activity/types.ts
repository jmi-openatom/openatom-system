export interface ActivityItem {
  id?: number
  title?: string
  summary?: string
  description?: string
  descriptionMarkdown?: string
  activityAt?: string
  endAt?: string
  date?: string
  time?: string
  endDate?: string
  location?: string
  status?: string
  coverUrl?: string
  maxParticipants?: number
  currentParticipants?: number
  registrationRequired?: boolean
  registrationStartAt?: string
  registrationEndAt?: string
  tags?: string[]
}

export interface ActivityDetail extends ActivityItem {
  content?: string
}

export type ActivityStatus = '' | 'published' | 'closed' | 'cancelled'

export interface ActivityFilter {
  label: string
  value: ActivityStatus
}
