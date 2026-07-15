import type {FormField} from '@/utils/formSchema'

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
  registered?: boolean
  isRegistered?: boolean
  registrationStatus?: string
  registrationCount?: number
  registrationFields?: string | FormField[]
}

export interface ActivityDetail extends ActivityItem {
  content?: string
}

export type ActivityStatus = '' | 'registration_open' | 'upcoming' | 'ended'

export interface ActivityFilter {
  label: string
  value: ActivityStatus
}
