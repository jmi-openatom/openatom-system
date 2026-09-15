import { http, type ApiResponse } from './http'

export interface TaskSummary {
  id: number
  taskKey: string
  title: string
  summary: string
  taskType: string
  difficulty: string
  estimatedMinutes: number
  points: number
  directionName?: string
  stageKey?: string
  stageName?: string
  assignmentId?: number
  memberStatus: string
  prerequisitesMet: boolean
}

export interface TaskDetail extends TaskSummary {
  learningObjectives: string
  deadlineType: string
  fixedDeadline?: string
  durationHours?: number
  instructions: string
  resources: string
  submissionRequirements: string
  acceptanceCriteria: string
  faq: string
  submissionLimit?: number
  capacity?: number
  requiredInStage: boolean
  ownerName: string
  dueAt?: string
  prerequisites: Array<{ id: number; title: string; completed: boolean }>
}

export interface Assignment {
  id: number
  taskId: number
  title: string
  summary: string
  difficulty: string
  points: number
  status: string
  claimedAt: string
  dueAt?: string
  latestVersion?: number
  latestFeedback?: string
}

export interface SubmissionPayload {
  completionNote: string
  repositoryUrl?: string
  pullRequestUrl?: string
  demoUrl?: string
  videoUrl?: string
  problemsAndLearning?: string
  aiUsed: boolean
  aiUsageDetail?: string
}

export interface DashboardData {
  level: string
  points: number
  assignments: { total: number; inProgress: number; revisionRequired: number; pendingReview: number; passed: number }
  upcoming: Array<{ assignmentId: number; title: string; dueAt: string }>
  feedback: Array<{ title: string; result: string; comment: string; reviewedAt: string; assignmentId: number }>
  unreadNotifications: number
  announcements: Array<{ id: number; title: string; content: string; publishedAt: string }>
}

export async function getDashboard() {
  return (await http.get<ApiResponse<DashboardData>>('/dashboard')).data.data
}

export async function getTasks(params?: { directionId?: number; stageId?: number }) {
  return (await http.get<ApiResponse<TaskSummary[]>>('/tasks', { params })).data.data
}

export async function getTask(id: number) {
  return (await http.get<ApiResponse<TaskDetail>>(`/tasks/${id}`)).data.data
}

export async function claimTask(id: number) {
  return (await http.post<ApiResponse<{ assignmentId: number }>>(`/tasks/${id}/claim`)).data.data
}

export async function getAssignments() {
  return (await http.get<ApiResponse<Assignment[]>>('/assignments/me')).data.data
}

export async function abandonAssignment(id: number) {
  await http.post(`/assignments/${id}/abandon`)
}

export async function submitAssignment(id: number, payload: SubmissionPayload) {
  return (await http.post<ApiResponse<{ submissionId: number; version: number }>>(`/assignments/${id}/submissions`, payload)).data.data
}

export async function getSubmissionHistory(id: number) {
  return (await http.get<ApiResponse<Record<string, unknown>[]>>(`/assignments/${id}/submissions`)).data.data
}

export async function getRoutes() {
  return (await http.get<ApiResponse<Record<string, unknown>[]>>('/routes')).data.data
}

export async function getMemberGrowth() {
  return (await http.get<ApiResponse<Record<string, any>>>('/growth/me')).data.data
}

export async function getRoute(id: number) {
  return (await http.get<ApiResponse<Record<string, unknown>>>(`/routes/${id}`)).data.data
}

export async function enrollRoute(id: number) {
  return (await http.post<ApiResponse<Record<string, unknown>>>(`/routes/${id}/enroll`)).data.data
}

export async function getNotifications(unreadOnly = false) {
  return (await http.get<ApiResponse<Record<string, unknown>[]>>('/notifications', { params: { unreadOnly } })).data.data
}

export async function markNotificationRead(id: number) {
  await http.post(`/notifications/${id}/read`)
}

export async function getReviewQueue() {
  return (await http.get<ApiResponse<Record<string, unknown>[]>>('/reviews/queue')).data.data
}

export async function reviewSubmission(id: number, payload: Record<string, unknown>) {
  return (await http.post<ApiResponse<Record<string, unknown>>>(`/reviews/submissions/${id}`, payload)).data.data
}

export async function createReviewAppeal(reviewId: number, reason: string) {
  return (await http.post<ApiResponse<Record<string, unknown>>>(`/reviews/${reviewId}/appeals`, { reason })).data.data
}

export async function getAppealQueue() {
  return (await http.get<ApiResponse<Record<string, unknown>[]>>('/reviews/appeals/queue')).data.data
}

export async function resolveAppeal(appealId: number, status: 'UPHELD' | 'OVERTURNED', resolution: string) {
  await http.post(`/reviews/appeals/${appealId}/resolve`, { status, resolution })
}

export async function getAdminTasks() {
  return (await http.get<ApiResponse<Record<string, unknown>[]>>('/admin/tasks')).data.data
}

export async function getTaskManagementOptions() {
  return (await http.get<ApiResponse<{ routes: Record<string, any>[]; stages: Record<string, any>[] }>>('/admin/task-options')).data.data
}

export async function getAdminStats() {
  return (await http.get<ApiResponse<Record<string, unknown>>>('/admin/stats')).data.data
}

export async function createTask(payload: Record<string, unknown>) {
  return (await http.post<ApiResponse<Record<string, unknown>>>('/admin/tasks', payload)).data.data
}

export async function changeTaskStatus(id: number, status: string) {
  await http.patch(`/admin/tasks/${id}/status`, undefined, { params: { status } })
}

export async function assignTask(id: number, memberId: number) {
  return (await http.post<ApiResponse<{ assignmentId: number; status: string; dueAt?: string }>>(
    `/admin/tasks/${id}/assignments`,
    { memberId },
  )).data.data
}

export async function getStages() {
  return (await http.get<ApiResponse<Record<string, any>[]>>('/routes/stages')).data.data
}

export async function getDirections() {
  return (await http.get<ApiResponse<Record<string, any>[]>>('/directions')).data.data
}

export async function getAdminRoutes() {
  return (await http.get<ApiResponse<Record<string, any>[]>>('/admin/routes')).data.data
}

export async function createRoute(payload: Record<string, unknown>) {
  return (await http.post<ApiResponse<Record<string, unknown>>>('/admin/routes', payload)).data.data
}

export async function publishRoute(id: number) {
  await http.post(`/admin/routes/${id}/publish`)
}

export async function archiveRoute(id: number) {
  await http.post(`/admin/routes/${id}/archive`)
}

export async function getAdminMembers() {
  return (await http.get<ApiResponse<Record<string, any>[]>>('/admin/members')).data.data
}

export async function updateMemberStatus(id: number, status: string, reason: string) {
  await http.patch(`/admin/members/${id}/status`, { status, reason })
}

export async function updateMemberRoles(id: number, roles: string[]) {
  await http.patch(`/admin/members/${id}/roles`, { roles })
}

export async function getAuditLogs() {
  return (await http.get<ApiResponse<Record<string, any>[]>>('/admin/audit-logs')).data.data
}

export async function getAdminDirections() {
  return (await http.get<ApiResponse<Record<string, any>[]>>('/admin/directions')).data.data
}

export async function createDirection(payload: Record<string, unknown>) {
  return (await http.post<ApiResponse<Record<string, unknown>>>('/admin/directions', payload)).data.data
}

export async function updateDirection(id: number, payload: Record<string, unknown>) {
  await http.patch(`/admin/directions/${id}`, payload)
}

export async function getLevelRules() {
  return (await http.get<ApiResponse<Record<string, any>[]>>('/admin/level-rules')).data.data
}

export async function updateLevelRule(levelKey: string, payload: Record<string, unknown>) {
  await http.patch(`/admin/level-rules/${levelKey}`, payload)
}

export async function getAnnouncements() {
  return (await http.get<ApiResponse<Record<string, any>[]>>('/admin/announcements')).data.data
}

export async function createAnnouncement(payload: Record<string, unknown>) {
  return (await http.post<ApiResponse<Record<string, unknown>>>('/admin/announcements', payload)).data.data
}

export async function publishAnnouncement(id: number) {
  await http.post(`/admin/announcements/${id}/publish`)
}
