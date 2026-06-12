import request from '@/api/request'

export interface ScoreOverview {
  reputationScore: number
  acceptedCount: number
  checkinCount: number
}

export interface CheckinLog {
  id: number
  checkinDate: string
  attendanceStatus: number
  source: string
  localScoreChange: number
  clubScoreChange: number
  clubSyncStatus: number
}

export function scoreOverview() {
  return request.get<ScoreOverview>('/score/me')
}

export function scoreLogs() {
  return request.get<CheckinLog[]>('/score/logs')
}
