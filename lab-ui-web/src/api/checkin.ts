import request from '@/api/request'

export interface CheckInSession {
  id: number
  groupId?: number
  sessionType: string
  title: string
  groupName?: string
  location?: string
  startAt?: string
  endAt?: string
  status: string
  qrPayload: string
  targetCount: number
  signedCount: number
  checkedCount: number
  lateCount: number
  absentCount: number
  pendingCount: number
}

export interface CheckInRecord {
  userId: number
  username?: string
  nickname?: string
  phone?: string
  status: string
  checkinAt?: string
}

export interface CheckInGroup {
  id: number
  name: string
  memberCount: number
  userIds: number[]
}

export interface LabUserOption {
  id: number
  username?: string
  nickname?: string
  phone?: string
  labRole: number
  reputationScore: number
}

export interface EveningStudyToday {
  date: string
  sessionCount: number
  targetCount: number
  signedCount: number
  lateCount: number
  absentCount: number
  sessions: CheckInSession[]
}

export function scanCheckIn(token: string) {
  return request.post<CheckInRecord>('/check-ins/scan', { token })
}

export function eveningStudyToday() {
  return request.get<EveningStudyToday>('/evening-study/today')
}

export function adminCheckIns() {
  return request.get<CheckInSession[]>('/admin/check-ins')
}

export function adminGroups() {
  return request.get<CheckInGroup[]>('/admin/check-in-groups')
}

export function userOptions(keyword = '') {
  return request.get<LabUserOption[]>('/admin/check-ins/user-options', { params: { keyword } })
}

export function createGroup(payload: { name: string; userIds: number[] }) {
  return request.post<number>('/admin/check-in-groups', payload)
}

export function createCheckIn(payload: Record<string, unknown>) {
  return request.post<number>('/admin/check-ins', payload)
}

export function generateEveningStudy() {
  return request.post<EveningStudyToday>('/admin/evening-study/sessions/generate')
}
