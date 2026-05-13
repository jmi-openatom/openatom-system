import request from './request'

// ---------- siteApi ----------

export const siteApi = {
  clubHome(params?: Record<string, unknown>) {
    return request.get('/site/club-home', { params })
  },
  activities() {
    return request.get('/site/activities')
  },
  activityDetail(id: string | number) {
    return request.get(`/site/activities/${id}`)
  },
  recruitment(params?: Record<string, unknown>) {
    return request.get('/site/recruitment', { params })
  },
  recruitmentDetail(id: string | number) {
    return request.get(`/site/recruitment/${id}`)
  },
  progress() {
    return request.get('/site/progress')
  },
  registerEnabled() {
    return request.get('/site/register-enabled')
  },
  formDetail(id: string | number) {
    return request.get(`/site/forms/${id}`)
  },
}

// ---------- authApi ----------

export const authApi = {
  login(data: Record<string, unknown>) {
    return request.post('/auth/login', data)
  },
  miniappLogin(data: Record<string, unknown>) {
    return request.post('/auth/miniapp-login', data)
  },
  miniappBind(data: Record<string, unknown>) {
    return request.post('/auth/miniapp-bind', data)
  },
  register(data: Record<string, unknown>) {
    return request.post('/auth/register', data)
  },
  logout(data: Record<string, unknown> = {}) {
    return request.post('/auth/logout', data)
  },
  me() {
    return request.get('/auth/me')
  },
  updateRegisterEnabled(enabled: boolean) {
    return request.patch('/auth/register-enabled', null, { params: { enabled } })
  },
  updatePassword(data: Record<string, unknown>) {
    return request.patch('/auth/password', data)
  },
}

// ---------- userApi ----------

export const userApi = {
  list(params?: Record<string, unknown>) {
    return request.get('/users', { params })
  },
  create(data: Record<string, unknown>) {
    return request.post('/users', data)
  },
  remove(id: string | number) {
    return request.delete(`/users/${id}`)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/users/${id}`, data)
  },
  updateStatus(id: string | number, status: string) {
    return request.patch(`/users/${id}/status`, { status })
  },
  resetPassword(id: string | number, data: Record<string, unknown>) {
    return request.post(`/users/${id}/reset-password`, data)
  },
  roles(id: string | number) {
    return request.get(`/users/${id}/roles`)
  },
  assignRoles(id: string | number, data: Record<string, unknown>) {
    return request.post(`/users/${id}/roles`, data)
  },
}

// ---------- clubApi ----------

export const clubApi = {
  list(params?: Record<string, unknown>) {
    return request.get('/clubs', { params })
  },
  create(data: Record<string, unknown>) {
    return request.post('/clubs', data)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/clubs/${id}`, data)
  },
  updateStatus(id: string | number, status: string) {
    return request.patch(`/clubs/${id}/status`, { status })
  },
  updateRecruitmentStatus(id: string | number, recruitmentStatus: string) {
    return request.patch(`/clubs/${id}/recruitment-status`, { recruitmentStatus })
  },
  departments(clubId: string | number) {
    return request.get(`/clubs/${clubId}/departments`)
  },
  positions(clubId: string | number) {
    return request.get(`/clubs/${clubId}/positions`)
  },
  campaigns(clubId: string | number) {
    return request.get(`/clubs/${clubId}/recruitment-campaigns`)
  },
  siteForms(clubId: string | number) {
    return request.get(`/clubs/${clubId}/site-forms`)
  },
}

// ---------- positionApi ----------

export const positionApi = {
  create(clubId: string | number, data: Record<string, unknown>) {
    return request.post(`/clubs/${clubId}/positions`, data)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/positions/${id}`, data)
  },
  remove(id: string | number) {
    return request.delete(`/positions/${id}`)
  },
}

// ---------- campaignApi ----------

export const campaignApi = {
  create(clubId: string | number, data: Record<string, unknown>) {
    return request.post(`/clubs/${clubId}/recruitment-campaigns`, data)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/recruitment-campaigns/${id}`, data)
  },
  publish(id: string | number) {
    return request.post(`/recruitment-campaigns/${id}/publish`)
  },
  close(id: string | number) {
    return request.post(`/recruitment-campaigns/${id}/close`)
  },
}

// ---------- siteFormApi ----------

export const siteFormApi = {
  create(clubId: string | number, data: Record<string, unknown>) {
    return request.post(`/clubs/${clubId}/site-forms`, data)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/site-forms/${id}`, data)
  },
  publish(id: string | number) {
    return request.post(`/site-forms/${id}/publish`)
  },
  close(id: string | number) {
    return request.post(`/site-forms/${id}/close`)
  },
}

// ---------- formSubmissionApi ----------

export const formSubmissionApi = {
  create(formId: string | number, data: Record<string, unknown>) {
    return request.post(`/site/forms/${formId}/submissions`, data)
  },
  list(formId: string | number, params?: Record<string, unknown>) {
    return request.get(`/site-forms/${formId}/submissions`, { params })
  },
  export(formId: string | number) {
    return request.get(`/site-forms/${formId}/submissions/export`, { responseType: 'blob' })
  },
}

// ---------- officeDocumentApi ----------

export const officeDocumentApi = {
  list(params?: Record<string, unknown>) {
    return request.get('/office-documents', { params })
  },
  userOptions(params?: Record<string, unknown>) {
    return request.get('/office-documents/user-options', { params })
  },
  create(data: Record<string, unknown>) {
    return request.post('/office-documents', data)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/office-documents/${id}`, data)
  },
  export(id: string | number) {
    return request.get(`/office-documents/${id}/export`, { responseType: 'blob' })
  },
}

// ---------- activityApi ----------

export const activityApi = {
  list(params?: Record<string, unknown>) {
    return request.get('/activities', { params })
  },
  detail(id: string | number) {
    return request.get(`/activities/${id}`)
  },
  create(data: Record<string, unknown>) {
    return request.post('/activities', data)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/activities/${id}`, data)
  },
  remove(id: string | number) {
    return request.delete(`/activities/${id}`)
  },
  register(id: string | number, data: Record<string, unknown>) {
    return request.post(`/activities/${id}/registrations`, data)
  },
  registrations(id: string | number) {
    return request.get(`/activities/${id}/registrations`)
  },
}

export const checkInApi = {
  scan(token: string) {
    return request.post('/site/check-ins/scan', { token })
  },
}

// ---------- awardApi ----------

export const awardApi = {
  list() {
    return request.get('/awards')
  },
  create(data: Record<string, unknown>) {
    return request.post('/awards', data)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/awards/${id}`, data)
  },
  remove(id: string | number) {
    return request.delete(`/awards/${id}`)
  },
}

// ---------- applicationApi ----------

export const applicationApi = {
  list(params?: Record<string, unknown>) {
    return request.get('/applications', { params })
  },
  create(data: Record<string, unknown>) {
    return request.post('/applications', data)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/applications/${id}`, data)
  },
  submit(id: string | number) {
    return request.post(`/applications/${id}/submit`)
  },
  withdraw(id: string | number) {
    return request.post(`/applications/${id}/withdraw`)
  },
}

// ---------- approvalApi ----------

export const approvalApi = {
  records(applicationId: string | number) {
    return request.get(`/applications/${applicationId}/approval-records`)
  },
  approve(applicationId: string | number, data: Record<string, unknown>) {
    return request.post(`/applications/${applicationId}/approvals`, data)
  },
  batch(data: Record<string, unknown>) {
    return request.post('/applications/batch-approvals', data)
  },
}

// ---------- interviewApi ----------

export const interviewApi = {
  list(params?: Record<string, unknown>) {
    return request.get('/interviews', { params })
  },
  create(data: Record<string, unknown>) {
    return request.post('/interviews', data)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/interviews/${id}`, data)
  },
  confirm(id: string | number) {
    return request.post(`/interviews/${id}/confirm`)
  },
  feedback(id: string | number, data: Record<string, unknown>) {
    return request.post(`/interviews/${id}/feedback`, data)
  },
  complete(id: string | number) {
    return request.post(`/interviews/${id}/complete`)
  },
  feedbacks(id: string | number) {
    return request.get(`/interviews/${id}/feedbacks`)
  },
}

// ---------- membershipApi ----------

export const membershipApi = {
  list(params?: Record<string, unknown>) {
    return request.get('/memberships', { params })
  },
  create(data: Record<string, unknown>) {
    return request.post('/memberships', data)
  },
  update(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/memberships/${id}`, data)
  },
  assignPosition(id: string | number, data: Record<string, unknown>) {
    return request.post(`/memberships/${id}/assign-position`, data)
  },
  changeStatus(id: string | number, data: Record<string, unknown>) {
    return request.post(`/memberships/${id}/change-status`, data)
  },
  forceExit(id: string | number, data: Record<string, unknown> = {}) {
    return request.post(`/memberships/${id}/force-exit`, data)
  },
  finalDecision(applicationId: string | number, data: Record<string, unknown>) {
    return request.post(`/applications/${applicationId}/final-decisions`, data)
  },
}

// ---------- rbacApi ----------

export const rbacApi = {
  roles(params?: Record<string, unknown>) {
    return request.get('/roles', { params })
  },
  createRole(data: Record<string, unknown>) {
    return request.post('/roles', data)
  },
  updateRole(id: string | number, data: Record<string, unknown>) {
    return request.patch(`/roles/${id}`, data)
  },
  deleteRole(id: string | number) {
    return request.delete(`/roles/${id}`)
  },
  assignRolePermissions(id: string | number, data: Record<string, unknown>) {
    return request.post(`/roles/${id}/permissions`, data)
  },
  permissions(params?: Record<string, unknown>) {
    return request.get('/permissions', { params })
  },
  createPermission(data: Record<string, unknown>) {
    return request.post('/permissions', data)
  },
}

// ---------- logApi ----------

export const logApi = {
  operation(params?: Record<string, unknown>) {
    return request.get('/operation-logs', { params })
  },
  login(params?: Record<string, unknown>) {
    return request.get('/login-logs', { params })
  },
}

// ---------- notificationApi ----------

export const notificationApi = {
  myNotifications() {
    return request.get('/notifications')
  },
  unreadCount() {
    return request.get('/notifications/unread-count')
  },
  markRead(id: string | number) {
    return request.post(`/notifications/${id}/read`)
  },
  adminList() {
    return request.get('/notifications/admin')
  },
  create(data: Record<string, unknown>) {
    return request.post('/notifications/admin', data)
  },
  delete(id: string | number) {
    return request.delete(`/notifications/admin/${id}`)
  },
}
