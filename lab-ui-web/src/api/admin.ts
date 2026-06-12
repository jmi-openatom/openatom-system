import request from '@/api/request'
import type { Problem } from '@/api/oj'

export interface MonitorStatus {
  aiEnabled: boolean
  sandboxConfigured: boolean
  mqExchange: string
  scoreRoutingKey: string
  pendingSubmissions: number
}

export interface ScoreboardRow {
  userId: number
  nickname?: string
  username?: string
  labRole: number
  reputationScore: number
}

export function problems() {
  return request.get<Problem[]>('/admin/problems')
}

export function generateTodayProblem() {
  return request.post<Problem>('/admin/problems/generate-today')
}

export function monitorStatus() {
  return request.get<MonitorStatus>('/admin/monitor/status')
}

export function scoreboard() {
  return request.get<ScoreboardRow[]>('/admin/scoreboard')
}
