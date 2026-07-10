import request from './request'
import { getToken } from '@/utils/auth.ts'
export const siteApi = {
  clubHome(params?: Record<string, unknown>): Promise<any> {
    return request.get('/site/club-home', { params })
  },
  activities(): Promise<any> {
    return request.get('/site/activities')
  },
  clubs(): Promise<any> {
    return request.get('/site/clubs')
  },
  activityDetail(id: string | number): Promise<any> {
    return request.get(`/site/activities/${id}`)
  },
  recruitment(params?: Record<string, unknown>): Promise<any> {
    return request.get('/site/recruitment', { params })
  },
  recruitmentDetail(id: string | number): Promise<any> {
    return request.get(`/site/recruitment/${id}`)
  },
  progress(): Promise<any> {
    return request.get('/site/progress')
  },
  activation(): Promise<any> {
    return request.get('/site/activation')
  },
  registerEnabled(): Promise<any> {
    return request.get('/site/register-enabled')
  },
  activationEnabled(): Promise<any> {
    return request.get('/site/activation-enabled')
  },
  formDetail(id: string | number): Promise<any> {
    return request.get(`/site/forms/${id}`)
  },
  votes(params?: Record<string, unknown>): Promise<any> {
    return request.get('/site/votes', { params })
  },
  voteDetail(id: string | number): Promise<any> {
    return request.get(`/site/votes/${id}`)
  },
  regulations(params?: Record<string, unknown>): Promise<any> {
    return request.get('/site/regulations', { params })
  },
  regulationDetail(id: string | number): Promise<any> {
    return request.get(`/site/regulations/${id}`)
  },
  submitVote(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/site/votes/${id}/records`, data)
  },
  blogArticles(params?: Record<string, unknown>): Promise<any> {
    return request.get('/site/blog/articles', { params })
  },
  blogArticleDetail(id: string | number): Promise<any> {
    return request.get(`/site/blog/articles/${id}`)
  },
  blogCategories(): Promise<any> {
    return request.get('/site/blog/categories')
  },
  showcaseApps(params?: Record<string, unknown>): Promise<any> {
    return request.get('/site/apps', { params })
  },
  showcaseAppDetail(id: string | number): Promise<any> {
    return request.get(`/site/apps/${id}`)
  },
  blogComments(id: string | number): Promise<any> {
    return request.get(`/site/blog/articles/${id}/comments`)
  },
  createBlogComment(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/site/blog/articles/${id}/comments`, data)
  },
  likeBlogArticle(id: string | number, data: Record<string, unknown> = {}): Promise<any> {
    return request.post(`/site/blog/articles/${id}/like`, data)
  },
  favoriteBlogArticle(id: string | number, data: Record<string, unknown> = {}): Promise<any> {
    return request.post(`/site/blog/articles/${id}/favorite`, data)
  },
  shareBlogArticle(id: string | number, data: Record<string, unknown> = {}): Promise<any> {
    return request.post(`/site/blog/articles/${id}/share`, data)
  },
}

export const authApi = {
  login(data: Record<string, unknown>): Promise<any> {
    return request.post('/auth/login', data)
  },
  register(data: Record<string, unknown>): Promise<any> {
    return request.post('/auth/register', data)
  },
  logout(data: Record<string, unknown> = {}): Promise<any> {
    return request.post('/auth/logout', data)
  },
  me(): Promise<any> {
    return request.get('/auth/me')
  },
  oidcToken(data: Record<string, string>): Promise<any> {
    return request.post('/oauth/token', new URLSearchParams(data), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    })
  },
  createQqBindToken(): Promise<any> {
    return request.post('/auth/qq-bind-token')
  },
  createGroupJoinToken(): Promise<any> {
    return request.post('/auth/group-join-token')
  },
  unbindQq(): Promise<any> {
    return request.delete('/auth/qq-bind')
  },
  updateRegisterEnabled(enabled: boolean): Promise<any> {
    return request.patch('/auth/register-enabled', null, { params: { enabled } })
  },
  updateActivationEnabled(enabled: boolean): Promise<any> {
    return request.patch('/auth/activation-enabled', null, { params: { enabled } })
  },
  updatePassword(data: Record<string, unknown>): Promise<any> {
    return request.patch('/auth/password', data)
  },
  uploadAvatar(file: File): Promise<any> {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/auth/avatar', formData)
  },
  removeAvatar(): Promise<any> {
    return request.delete('/auth/avatar')
  },
  updateProfile(data: Record<string, unknown>): Promise<any> {
    return request.patch('/auth/profile', data)
  },
  completeOnboarding(): Promise<any> {
    return request.post('/auth/onboarding/complete')
  },
  activate(): Promise<any> {
    return request.post('/auth/activate')
  },
	}

export const oauthClientApi = {
  list(): Promise<any> {
    return request.get('/oauth/admin/clients')
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/oauth/admin/clients', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/oauth/admin/clients/${id}`, data)
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/oauth/admin/clients/${id}`)
  },
}

export const showcaseAppApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/showcase-apps', { params })
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/showcase-apps', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/showcase-apps/${id}`, data)
  },
  updateStatus(id: string | number, status: string): Promise<any> {
    return request.patch(`/showcase-apps/${id}/status`, null, { params: { status } })
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/showcase-apps/${id}`)
  },
}

export const userApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/users', { params })
  },
  info(id: string | number): Promise<any> {
    return request.get(`/users/${id}`)
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/users', data)
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/users/${id}`)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/users/${id}`, data)
  },
  updateStatus(id: string | number, status: string): Promise<any> {
    return request.patch(`/users/${id}/status`, { status })
  },
  resetPassword(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/users/${id}/reset-password`, data)
  },
  roles(id: string | number): Promise<any> {
    return request.get(`/users/${id}/roles`)
  },
  assignRoles(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/users/${id}/roles`, data)
  },
  avatarHealth(): Promise<any> {
    return request.get('/users/avatars/health')
  },
  cleanupInvalidAvatars(): Promise<any> {
    return request.post('/users/avatars/cleanup')
  },
  setActivation(id: string | number, activated: boolean): Promise<any> {
    return request.patch(`/users/${id}/activation`, null, { params: { activated } })
  },
}

export const clubApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/clubs', { params })
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/clubs', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/clubs/${id}`, data)
  },
  updateStatus(id: string | number, status: string): Promise<any> {
    return request.patch(`/clubs/${id}/status`, { status })
  },
  updateRecruitmentStatus(id: string | number, recruitmentStatus: string): Promise<any> {
    return request.patch(`/clubs/${id}/recruitment-status`, { recruitmentStatus })
  },
  departments(clubId: string | number): Promise<any> {
    return request.get(`/clubs/${clubId}/departments`)
  },
  positions(clubId: string | number): Promise<any> {
    return request.get(`/clubs/${clubId}/positions`)
  },
  campaigns(clubId: string | number): Promise<any> {
    return request.get(`/clubs/${clubId}/recruitment-campaigns`)
  },
  siteForms(clubId: string | number): Promise<any> {
    return request.get(`/clubs/${clubId}/site-forms`)
  },
  vicePresidents(clubId: string | number): Promise<any> {
    return request.get(`/clubs/${clubId}/vice-presidents`)
  },
  addVicePresident(clubId: string | number, userId: string | number): Promise<any> {
    return request.post(`/clubs/${clubId}/vice-presidents?userId=${userId}`)
  },
  removeVicePresident(clubId: string | number, userId: string | number): Promise<any> {
    return request.delete(`/clubs/${clubId}/vice-presidents/${userId}`)
  },
}

export const positionApi = {
  create(clubId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/clubs/${clubId}/positions`, data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/positions/${id}`, data)
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/positions/${id}`)
  },
}

export const departmentApi = {
  list(clubId: string | number): Promise<any> {
    return request.get(`/clubs/${clubId}/departments`)
  },
  create(clubId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/clubs/${clubId}/departments`, data)
  },
  detail(id: string | number): Promise<any> {
    return request.get(`/departments/${id}`)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/departments/${id}`, data)
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/departments/${id}`)
  },
}

export const campaignApi = {
  create(clubId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/clubs/${clubId}/recruitment-campaigns`, data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/recruitment-campaigns/${id}`, data)
  },
  publish(id: string | number): Promise<any> {
    return request.post(`/recruitment-campaigns/${id}/publish`)
  },
  close(id: string | number): Promise<any> {
    return request.post(`/recruitment-campaigns/${id}/close`)
  },
}

export const siteFormApi = {
  create(clubId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/clubs/${clubId}/site-forms`, data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/site-forms/${id}`, data)
  },
  publish(id: string | number): Promise<any> {
    return request.post(`/site-forms/${id}/publish`)
  },
  close(id: string | number): Promise<any> {
    return request.post(`/site-forms/${id}/close`)
  },
  shareInfo(id: string | number): Promise<any> {
    return request.get(`/site-forms/${id}/share-info`)
  },
}

export const formSubmissionApi = {
  create(formId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/site/forms/${formId}/submissions`, data)
  },
  list(formId: string | number, params?: Record<string, unknown>): Promise<any> {
    return request.get(`/site-forms/${formId}/submissions`, { params })
  },
  export(formId: string | number): Promise<any> {
    return request.get(`/site-forms/${formId}/submissions/export`, { responseType: 'blob' })
  },
}

export const lotteryApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/lotteries', { params })
  },
  detail(id: string | number): Promise<any> {
    return request.get(`/lotteries/${id}`)
  },
  create(clubId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/clubs/${clubId}/lotteries`, data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/lotteries/${id}`, data)
  },
  publish(id: string | number): Promise<any> {
    return request.post(`/lotteries/${id}/publish`)
  },
  close(id: string | number): Promise<any> {
    return request.post(`/lotteries/${id}/close`)
  },
  draw(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/lotteries/${id}/draw`, data)
  },
  reset(id: string | number): Promise<any> {
    return request.post(`/lotteries/${id}/reset`)
  },
  screen(id: string | number): Promise<any> {
    return request.get(`/site/lotteries/${id}/screen`)
  },
}

export const voteApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/votes', { params })
  },
  detail(id: string | number): Promise<any> {
    return request.get(`/votes/${id}`)
  },
  create(clubId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/clubs/${clubId}/votes`, data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/votes/${id}`, data)
  },
  publish(id: string | number): Promise<any> {
    return request.post(`/votes/${id}/publish`)
  },
  close(id: string | number): Promise<any> {
    return request.post(`/votes/${id}/close`)
  },
  reset(id: string | number): Promise<any> {
    return request.post(`/votes/${id}/reset`)
  },
}

export const officeDocumentApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/office-documents', { params })
  },
  userOptions(params?: Record<string, unknown>): Promise<any> {
    return request.get('/office-documents/user-options', { params })
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/office-documents', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/office-documents/${id}`, data)
  },
  export(id: string | number): Promise<any> {
    return request.get(`/office-documents/${id}/export`, { responseType: 'blob' })
  },
}

export const imageHostingApi = {
  upload(file: File): Promise<any> {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/image-hosting/images', formData)
  },
  myImages(params?: Record<string, unknown>): Promise<any> {
    return request.get('/image-hosting/images/my', { params })
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/image-hosting/images/${id}`)
  },
  adminList(params?: Record<string, unknown>): Promise<any> {
    return request.get('/image-hosting/admin/images', { params })
  },
  adminRemove(id: string | number): Promise<any> {
    return request.delete(`/image-hosting/admin/images/${id}`)
  },
}

export const dataOpenApi = {
  apply(data: Record<string, unknown>): Promise<any> {
    return request.post('/public/data-open/applications', data)
  },
  publicDetail(id: string | number, applicantContact: string): Promise<any> {
    return request.get(`/public/data-open/applications/${id}`, { params: { applicantContact } })
  },
  adminList(params?: Record<string, unknown>): Promise<any> {
    return request.get('/data-open/admin/applications', { params })
  },
  review(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/data-open/admin/applications/${id}/review`, data)
  },
}

export const blogApi = {
  myArticles(params?: Record<string, unknown>): Promise<any> {
    return request.get('/blog/my/articles', { params })
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/blog/articles', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/blog/articles/${id}`, data)
  },
  publish(id: string | number): Promise<any> {
    return request.post(`/blog/articles/${id}/publish`)
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/blog/articles/${id}`)
  },
  adminList(params?: Record<string, unknown>): Promise<any> {
    return request.get('/blog/admin/articles', { params })
  },
  review(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/blog/admin/articles/${id}/review`, data)
  },
  adminRemove(id: string | number): Promise<any> {
    return request.delete(`/blog/admin/articles/${id}`)
  },
  adminComments(id: string | number): Promise<any> {
    return request.get(`/blog/admin/articles/${id}/comments`)
  },
  updateCommentStatus(id: string | number, status: string): Promise<any> {
    return request.patch(`/blog/admin/comments/${id}/status`, { status })
  },
  adminInteractions(params?: Record<string, unknown>): Promise<any> {
    return request.get('/blog/admin/interactions', { params })
  },
}

export const activityApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/activities', { params })
  },
  detail(id: string | number): Promise<any> {
    return request.get(`/activities/${id}`)
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/activities', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/activities/${id}`, data)
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/activities/${id}`)
  },
  register(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/activities/${id}/registrations`, data)
  },
  registrations(id: string | number): Promise<any> {
    return request.get(`/activities/${id}/registrations`)
  },
}

export const aiActivityApi = {
  sessions(): Promise<any> {
    return request.get('/ai/activity/sessions')
  },
  detail(id: string | number): Promise<any> {
    return request.get(`/ai/activity/sessions/${id}`)
  },
  createSession(data: Record<string, unknown>): Promise<any> {
    return request.post('/ai/activity/sessions', data)
  },
  deleteSession(id: string | number): Promise<any> {
    return request.delete(`/ai/activity/sessions/${id}`)
  },
  sendMessage(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/ai/activity/sessions/${id}/messages`, data)
  },
  confirmRequirement(id: string | number, data: Record<string, unknown> = {}): Promise<any> {
    return request.post(`/ai/activity/sessions/${id}/confirm-requirement`, data)
  },
  generatePlan(id: string | number): Promise<any> {
    return request.post(`/ai/activity/sessions/${id}/generate-plan`)
  },
  revisePlan(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/ai/activity/sessions/${id}/revise-plan`, data)
  },
  savePlan(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/ai/activity/sessions/${id}/plans`, data)
  },
  confirmPlan(id: string | number): Promise<any> {
    return request.post(`/ai/activity/sessions/${id}/confirm-plan`)
  },
  createActivity(id: string | number): Promise<any> {
    return request.post(`/ai/activity/sessions/${id}/create-activity`)
  },
  generateDocuments(id: string | number, data: Record<string, unknown> = {}): Promise<any> {
    return request.post(`/ai/activity/sessions/${id}/documents/generate`, data)
  },
}

export interface AiStreamEvent {
  event: string
  data: any
}

function apiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL || '/api/v1'
}

export async function postAiStream(
  path: string,
  body: Record<string, unknown>,
  onEvent: (event: AiStreamEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  }
  if (token) {
    headers.jmiopenatom = token
    headers.Authorization = `Bearer ${token}`
  }
  const response = await fetch(`${apiBaseUrl()}${path}`, {
    method: 'POST',
    headers,
    credentials: 'include',
    signal,
    body: JSON.stringify(body || {}),
  })
  if (!response.ok || !response.body) {
    throw new Error(`流式请求失败：HTTP ${response.status}`)
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  const flushEvent = (raw: string) => {
    const lines = raw.split(/\r?\n/)
    let event = 'message'
    const dataLines: string[] = []
    for (const line of lines) {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
    }
    if (!dataLines.length) return
    const dataText = dataLines.join('\n')
    try {
      onEvent({ event, data: JSON.parse(dataText) })
    } catch {
      onEvent({ event, data: dataText })
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split(/\r?\n\r?\n/)
    buffer = chunks.pop() || ''
    chunks.forEach(flushEvent)
  }
  buffer += decoder.decode()
  if (buffer.trim()) flushEvent(buffer)
}

export const aiSettingsApi = {
  get(): Promise<any> {
    return request.get('/ai/settings')
  },
  update(data: Record<string, unknown>): Promise<any> {
    return request.put('/ai/settings', data)
  },
  test(): Promise<any> {
    return request.post('/ai/settings/test')
  },
}

export const documentTemplateApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/document-templates', { params })
  },
  upload(data: { templateType: string; templateName: string; file: File }): Promise<any> {
    const formData = new FormData()
    formData.append('templateType', data.templateType)
    formData.append('templateName', data.templateName)
    formData.append('file', data.file)
    return request.post('/document-templates', formData)
  },
  variables(id: string | number): Promise<any> {
    return request.get(`/document-templates/${id}/variables`)
  },
  saveVariables(id: string | number, variables: Array<Record<string, unknown>>): Promise<any> {
    return request.put(`/document-templates/${id}/variables`, { variables })
  },
  downloadGenerated(id: string | number): Promise<any> {
    return request.get(`/generated-documents/${id}/download`, { responseType: 'blob' })
  },
}

export const checkInApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/check-ins', { params })
  },
  userOptions(params?: Record<string, unknown>): Promise<any> {
    return request.get('/check-ins/user-options', { params })
  },
  groups(): Promise<any> {
    return request.get('/check-in-groups')
  },
  createGroup(data: Record<string, unknown>): Promise<any> {
    return request.post('/check-in-groups', data)
  },
  updateGroup(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.put(`/check-in-groups/${id}`, data)
  },
  deleteGroup(id: string | number): Promise<any> {
    return request.delete(`/check-in-groups/${id}`)
  },
  removeGroupMember(id: string | number, userId: string | number): Promise<any> {
    return request.delete(`/check-in-groups/${id}/members/${userId}`)
  },
  eveningSchedules(): Promise<any> {
    return request.get('/evening-study/schedules')
  },
  createEveningSchedule(data: Record<string, unknown>): Promise<any> {
    return request.post('/evening-study/schedules', data)
  },
  updateEveningSchedule(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.put(`/evening-study/schedules/${id}`, data)
  },
  deleteEveningSchedule(id: string | number): Promise<any> {
    return request.delete(`/evening-study/schedules/${id}`)
  },
  generateEveningStudy(params?: Record<string, unknown>): Promise<any> {
    return request.post('/evening-study/sessions/generate', null, { params })
  },
  eveningStudyToday(params?: Record<string, unknown>): Promise<any> {
    return request.get('/evening-study/today', { params })
  },
  siteEveningStudyToday(params?: Record<string, unknown>): Promise<any> {
    return request.get('/site/evening-study/today', { params })
  },
  detail(id: string | number): Promise<any> {
    return request.get(`/check-ins/${id}`)
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/check-ins', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/check-ins/${id}`, data)
  },
  close(id: string | number): Promise<any> {
    return request.post(`/check-ins/${id}/close`)
  },
  delete(id: string | number): Promise<any> {
    return request.delete(`/check-ins/${id}`)
  },
  addTargets(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/check-ins/${id}/targets`, data)
  },
  records(id: string | number): Promise<any> {
    return request.get(`/check-ins/${id}/records`)
  },
  updateRecordStatus(
    id: string | number,
    userId: string | number,
    data: Record<string, unknown>,
  ): Promise<any> {
    return request.patch(`/check-ins/${id}/records/${userId}`, data)
  },
  scan(data: Record<string, unknown>): Promise<any> {
    return request.post('/site/check-ins/scan', data)
  },
}

export const pointApi = {
  leaderboard(params?: Record<string, unknown>): Promise<any> {
    return request.get('/site/points/leaderboard', { params })
  },
  mySummary(): Promise<any> {
    return request.get('/site/points/me')
  },
  siteItems(): Promise<any> {
    return request.get('/site/points/items')
  },
  redeem(itemId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/site/points/items/${itemId}/redeem`, data)
  },
  myRedemptions(): Promise<any> {
    return request.get('/site/points/redemptions')
  },
  adminAccounts(params?: Record<string, unknown>): Promise<any> {
    return request.get('/points/admin/accounts', { params })
  },
  adminTransactions(params?: Record<string, unknown>): Promise<any> {
    return request.get('/points/admin/transactions', { params })
  },
  adjust(data: Record<string, unknown>): Promise<any> {
    return request.post('/points/admin/adjustments', data)
  },
  adminRules(): Promise<any> {
    return request.get('/points/admin/rules')
  },
  updateRules(data: Record<string, unknown>): Promise<any> {
    return request.patch('/points/admin/rules', data)
  },
  adminItems(params?: Record<string, unknown>): Promise<any> {
    return request.get('/points/admin/items', { params })
  },
  createItem(data: Record<string, unknown>): Promise<any> {
    return request.post('/points/admin/items', data)
  },
  updateItem(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/points/admin/items/${id}`, data)
  },
  deleteItem(id: string | number): Promise<any> {
    return request.delete(`/points/admin/items/${id}`)
  },
  adminRedemptions(params?: Record<string, unknown>): Promise<any> {
    return request.get('/points/admin/redemptions', { params })
  },
  updateRedemptionStatus(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/points/admin/redemptions/${id}/status`, data)
  },
}

export const leaveApplicationApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/leave-applications', { params })
  },
  mine(): Promise<any> {
    return request.get('/site/leave-applications')
  },
  detail(id: string | number): Promise<any> {
    return request.get(`/leave-applications/${id}`)
  },
  siteDetail(id: string | number): Promise<any> {
    return request.get(`/site/leave-applications/${id}`)
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/site/leave-applications', data)
  },
  review(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/leave-applications/${id}/review`, data)
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/leave-applications/${id}`)
  },
  siteRemove(id: string | number): Promise<any> {
    return request.delete(`/site/leave-applications/${id}`)
  },
}

export const schoolCalendarApi = {
  detail(): Promise<any> {
    return request.get('/school-calendar')
  },
  siteDetail(): Promise<any> {
    return request.get('/site/school-calendar')
  },
  save(data: Record<string, unknown>): Promise<any> {
    return request.post('/school-calendar', data)
  },
  saveAdjustment(data: Record<string, unknown>): Promise<any> {
    return request.post('/school-calendar/adjustments', data)
  },
  deleteAdjustment(id: string | number): Promise<any> {
    return request.delete(`/school-calendar/adjustments/${id}`)
  },
}

export const awardApi = {
  list(): Promise<any> {
    return request.get('/awards')
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/awards', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/awards/${id}`, data)
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/awards/${id}`)
  },
}

export const applicationApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/applications', { params })
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/applications', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/applications/${id}`, data)
  },
  submit(id: string | number): Promise<any> {
    return request.post(`/applications/${id}/submit`)
  },
  withdraw(id: string | number): Promise<any> {
    return request.post(`/applications/${id}/withdraw`)
  },
  export(params?: Record<string, unknown>): Promise<any> {
    return request.get('/applications/export', { params, responseType: 'blob' })
  },
}

export const approvalApi = {
  records(applicationId: string | number): Promise<any> {
    return request.get(`/applications/${applicationId}/approval-records`)
  },
  approve(applicationId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/applications/${applicationId}/approvals`, data)
  },
  batch(data: Record<string, unknown>): Promise<any> {
    return request.post('/applications/batch-approvals', data)
  },
}

export const interviewApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/interviews', { params })
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/interviews', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/interviews/${id}`, data)
  },
  confirm(id: string | number): Promise<any> {
    return request.post(`/interviews/${id}/confirm`)
  },
  feedback(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/interviews/${id}/feedback`, data)
  },
  complete(id: string | number): Promise<any> {
    return request.post(`/interviews/${id}/complete`)
  },
  feedbacks(id: string | number): Promise<any> {
    return request.get(`/interviews/${id}/feedbacks`)
  },
}

export const membershipApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/memberships', { params })
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/memberships', data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/memberships/${id}`, data)
  },
  assignPosition(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/memberships/${id}/assign-position`, data)
  },
  changeStatus(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/memberships/${id}/change-status`, data)
  },
  batchChangeStatus(data: Record<string, unknown>): Promise<any> {
    return request.post('/memberships/batch-change-status', data)
  },
  batchCreate(data: Record<string, unknown>): Promise<any> {
    return request.post('/memberships/batch-create', data)
  },
  forceExit(id: string | number, data: Record<string, unknown> = {}): Promise<any> {
    return request.post(`/memberships/${id}/force-exit`, data)
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/memberships/${id}`)
  },
  finalDecision(applicationId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/applications/${applicationId}/final-decisions`, data)
  },
}

export const alumniGroupApi = {
  list(clubId: string | number): Promise<any> {
    return request.get(`/clubs/${clubId}/alumni-groups`)
  },
  create(clubId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/clubs/${clubId}/alumni-groups`, data)
  },
  update(groupId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/alumni-groups/${groupId}`, data)
  },
  delete(groupId: string | number): Promise<any> {
    return request.delete(`/alumni-groups/${groupId}`)
  },
}

export const rbacApi = {
  roles(params?: Record<string, unknown>): Promise<any> {
    return request.get('/roles', { params })
  },
  roleDetail(id: string | number): Promise<any> {
    return request.get(`/roles/${id}`)
  },
  createRole(data: Record<string, unknown>): Promise<any> {
    return request.post('/roles', data)
  },
  updateRole(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/roles/${id}`, data)
  },
  deleteRole(id: string | number): Promise<any> {
    return request.delete(`/roles/${id}`)
  },
  assignRolePermissions(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/roles/${id}/permissions`, data)
  },
  permissions(params?: Record<string, unknown>): Promise<any> {
    return request.get('/permissions', { params })
  },
  createPermission(data: Record<string, unknown>): Promise<any> {
    return request.post('/permissions', data)
  },
}

export const logApi = {
  operation(params?: Record<string, unknown>): Promise<any> {
    return request.get('/operation-logs', { params })
  },
  login(params?: Record<string, unknown>): Promise<any> {
    return request.get('/login-logs', { params })
  },
}

export const notificationApi = {
  myNotifications(): Promise<any> {
    return request.get('/notifications')
  },
  unreadCount(): Promise<any> {
    return request.get('/notifications/unread-count')
  },
  markRead(id: string | number): Promise<any> {
    return request.post(`/notifications/${id}/read`)
  },
  adminList(): Promise<any> {
    return request.get('/notifications/admin')
  },
  create(data: Record<string, unknown>): Promise<any> {
    return request.post('/notifications/admin', data)
  },
  delete(id: string | number): Promise<any> {
    return request.delete(`/notifications/admin/${id}`)
  },
}

export const regulationApi = {
  list(params?: Record<string, unknown>): Promise<any> {
    return request.get('/regulations', { params })
  },
  detail(id: string | number): Promise<any> {
    return request.get(`/regulations/${id}`)
  },
  create(clubId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/clubs/${clubId}/regulations`, data)
  },
  update(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/regulations/${id}`, data)
  },
  remove(id: string | number): Promise<any> {
    return request.delete(`/regulations/${id}`)
  },
}

export const botManagementApi = {
  overview(): Promise<any> {
    return request.get('/bot-management/overview')
  },
  accounts(): Promise<any> {
    return request.get('/bot-management/accounts')
  },
  saveAccount(data: Record<string, unknown>): Promise<any> {
    return request.post('/bot-management/accounts', data)
  },
  groups(params?: Record<string, unknown>): Promise<any> {
    return request.get('/bot-management/groups', { params })
  },
  syncGroups(): Promise<any> {
    return request.post('/bot-management/groups/sync')
  },
  batchConfig(data: Record<string, unknown>): Promise<any> {
    return request.post('/bot-management/groups/batch-config', data)
  },
  groupDetail(groupId: string | number): Promise<any> {
    return request.get(`/bot-management/groups/${groupId}`)
  },
  members(groupId: string | number, params?: Record<string, unknown>): Promise<any> {
    return request.get(`/bot-management/groups/${groupId}/members`, { params })
  },
  syncMembers(groupId: string | number): Promise<any> {
    return request.post(`/bot-management/groups/${groupId}/members/sync`)
  },
  updateConfig(groupId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/bot-management/groups/${groupId}/config`, data)
  },
  muteMember(
    groupId: string | number,
    userId: string | number,
    data: Record<string, unknown>,
  ): Promise<any> {
    return request.post(`/bot-management/groups/${groupId}/members/${userId}/mute`, data)
  },
  muteAll(groupId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/bot-management/groups/${groupId}/mute-all`, data)
  },
  groupMessages(groupId: string | number): Promise<any> {
    return request.get(`/bot-management/groups/${groupId}/messages`)
  },
  sendGroupMessage(groupId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/bot-management/groups/${groupId}/messages`, data)
  },
  sendGroupMessageNow(groupId: string | number, messageId: string | number): Promise<any> {
    return request.post(`/bot-management/groups/${groupId}/messages/${messageId}/send-now`)
  },
  deleteGroupMessage(groupId: string | number, messageId: string | number): Promise<any> {
    return request.delete(`/bot-management/groups/${groupId}/messages/${messageId}`)
  },
  announcements(groupId: string | number): Promise<any> {
    return request.get(`/bot-management/groups/${groupId}/announcements`)
  },
  publishAnnouncement(groupId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/bot-management/groups/${groupId}/announcements`, data)
  },
  republishAnnouncement(groupId: string | number, announcementId: string | number): Promise<any> {
    return request.post(
      `/bot-management/groups/${groupId}/announcements/${announcementId}/republish`,
    )
  },
  deleteAnnouncement(groupId: string | number, announcementId: string | number): Promise<any> {
    return request.delete(`/bot-management/groups/${groupId}/announcements/${announcementId}`)
  },
  joinRequests(groupId: string | number, params?: Record<string, unknown>): Promise<any> {
    return request.get(`/bot-management/groups/${groupId}/join-requests`, { params })
  },
  saveJoinRequest(groupId: string | number, data: Record<string, unknown>): Promise<any> {
    return request.post(`/bot-management/groups/${groupId}/join-requests`, data)
  },
  handleJoinRequest(
    groupId: string | number,
    requestId: string | number,
    data: Record<string, unknown>,
  ): Promise<any> {
    return request.post(`/bot-management/groups/${groupId}/join-requests/${requestId}/handle`, data)
  },
  sensitiveWords(): Promise<any> {
    return request.get('/bot-management/sensitive-words')
  },
  saveSensitiveWord(data: Record<string, unknown>): Promise<any> {
    return request.post('/bot-management/sensitive-words', data)
  },
  updateSensitiveWord(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/bot-management/sensitive-words/${id}`, data)
  },
  deleteSensitiveWord(id: string | number): Promise<any> {
    return request.delete(`/bot-management/sensitive-words/${id}`)
  },
  autoReviewRules(params?: Record<string, unknown>): Promise<any> {
    return request.get('/bot-management/auto-review-rules', { params })
  },
  saveAutoReviewRule(data: Record<string, unknown>): Promise<any> {
    return request.post('/bot-management/auto-review-rules', data)
  },
  updateAutoReviewRule(id: string | number, data: Record<string, unknown>): Promise<any> {
    return request.patch(`/bot-management/auto-review-rules/${id}`, data)
  },
  deleteAutoReviewRule(id: string | number): Promise<any> {
    return request.delete(`/bot-management/auto-review-rules/${id}`)
  },
  statistics(params?: Record<string, unknown>): Promise<any> {
    return request.get('/bot-management/statistics', { params })
  },
  activeGroups(params?: Record<string, unknown>): Promise<any> {
    return request.get('/bot-management/statistics/active-groups', { params })
  },
}

export const nextPageApi = {
  getStats(): Promise<any> {
    return request.get('/next-page/stats')
  },
  like(): Promise<any> {
    return request.post('/next-page/like')
  },
  join(data: { name: string; contact: string; direction: string; skills?: string; message?: string }): Promise<any> {
    return request.post('/next-page/join', data)
  },
}

export const fileMigrationApi = {
  stats(): Promise<any> {
    return request.get('/file-migration/stats')
  },
  exportUrl(): string {
    return `${request.defaults.baseURL || '/api/v1'}/file-migration/export`
  },
  // 异步导出 - 启动导出任务
  startExport(): Promise<any> {
    return request.post('/file-migration/export/start', {}, {
      timeout: 30000 // 30秒超时启动任务
    })
  },
  // 查询导出任务状态
  getExportStatus(taskId: string): Promise<any> {
    return request.get(`/file-migration/export/status/${taskId}`)
  },
  // 下载已完成的导出文件（支持进度回调）
  downloadExport(taskId: string, onProgress?: (loaded: number, total: number) => void): Promise<any> {
    return request.get(`/file-migration/export/download/${taskId}`, { 
      responseType: 'blob',
      timeout: 600000, // 10分钟超时下载
      onDownloadProgress: (progressEvent: any) => {
        if (onProgress) {
          onProgress(progressEvent.loaded, progressEvent.total || 0)
        }
      }
    })
  },
  // 保留旧的同步导出方法（向后兼容）
  export(): Promise<any> {
    // 导出文件可能较大，设置较长超时时间（5分钟）
    return request.get('/file-migration/export', { 
      responseType: 'blob',
      timeout: 300000 // 5分钟
    })
  },
  importZip(file: File, overwrite: boolean = false): Promise<any> {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/file-migration/import', formData, {
      params: { overwrite },
    })
  },
  // 手动触发备份
  runBackup(): Promise<any> {
    return request.post('/file-migration/backup/run', {}, {
      timeout: 60000 // 1分钟超时
    })
  },
  // 获取备份文件列表
  listBackups(): Promise<any> {
    return request.get('/file-migration/backup/list')
  },
  // 删除备份文件
  deleteBackup(fileName: string): Promise<any> {
    return request.delete(`/file-migration/backup/${fileName}`)
  },
}
