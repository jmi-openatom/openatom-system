import request from './request'

export const siteApi = {
    clubHome(params) {
        return request.get('/site/club-home', {params})
    },
    activities() {
        return request.get('/site/activities')
    },
    activityDetail(id) {
        return request.get(`/site/activities/${id}`)
    },
    recruitment(params) {
        return request.get('/site/recruitment', {params})
    },
    recruitmentDetail(id) {
        return request.get(`/site/recruitment/${id}`)
    },
    progress() {
        return request.get('/site/progress')
    },
    registerEnabled() {
        return request.get('/site/register-enabled')
    },
    formDetail(id) {
        return request.get(`/site/forms/${id}`)
    }
}

export const authApi = {
    login(data) {
        return request.post('/auth/login', data)
    },
    register(data) {
        return request.post('/auth/register', data)
    },
    logout(data = {}) {
        return request.post('/auth/logout', data)
    },
    me() {
        return request.get('/auth/me')
    },
    updateRegisterEnabled(enabled) {
        return request.patch('/auth/register-enabled', null, {params: {enabled}})
    },
    updatePassword(data) {
        return request.patch('/auth/password', data)
    },
}

export const userApi = {
    list(params) {
        return request.get('/users', {params})
    },
    create(data) {
        return request.post('/users', data)
    },
    update(id, data) {
        return request.patch(`/users/${id}`, data)
    },
    updateStatus(id, status) {
        return request.patch(`/users/${id}/status`, {status})
    },
    resetPassword(id, data) {
        return request.post(`/users/${id}/reset-password`, data)
    },
    roles(id) {
        return request.get(`/users/${id}/roles`)
    },
    assignRoles(id, data) {
        return request.post(`/users/${id}/roles`, data)
    }
}

export const clubApi = {
    list(params) {
        return request.get('/clubs', {params})
    },
    create(data) {
        return request.post('/clubs', data)
    },
    update(id, data) {
        return request.patch(`/clubs/${id}`, data)
    },
    updateStatus(id, status) {
        return request.patch(`/clubs/${id}/status`, {status})
    },
    updateRecruitmentStatus(id, recruitmentStatus) {
        return request.patch(`/clubs/${id}/recruitment-status`, {recruitmentStatus})
    },
    departments(clubId) {
        return request.get(`/clubs/${clubId}/departments`)
    },
    positions(clubId) {
        return request.get(`/clubs/${clubId}/positions`)
    },
    campaigns(clubId) {
        return request.get(`/clubs/${clubId}/recruitment-campaigns`)
    },
    siteForms(clubId) {
        return request.get(`/clubs/${clubId}/site-forms`)
    }
}

export const positionApi = {
    create(clubId, data) {
        return request.post(`/clubs/${clubId}/positions`, data)
    },
    update(id, data) {
        return request.patch(`/positions/${id}`, data)
    },
    remove(id) {
        return request.delete(`/positions/${id}`)
    }
}

export const campaignApi = {
    create(clubId, data) {
        return request.post(`/clubs/${clubId}/recruitment-campaigns`, data)
    },
    update(id, data) {
        return request.patch(`/recruitment-campaigns/${id}`, data)
    },
    publish(id) {
        return request.post(`/recruitment-campaigns/${id}/publish`)
    },
    close(id) {
        return request.post(`/recruitment-campaigns/${id}/close`)
    }
}

export const siteFormApi = {
    create(clubId, data) {
        return request.post(`/clubs/${clubId}/site-forms`, data)
    },
    update(id, data) {
        return request.patch(`/site-forms/${id}`, data)
    },
    publish(id) {
        return request.post(`/site-forms/${id}/publish`)
    },
    close(id) {
        return request.post(`/site-forms/${id}/close`)
    }
}

export const formSubmissionApi = {
    create(formId, data) {
        return request.post(`/site/forms/${formId}/submissions`, data)
    },
    list(formId, params) {
        return request.get(`/site-forms/${formId}/submissions`, {params})
    },
    export(formId) {
        return request.get(`/site-forms/${formId}/submissions/export`, {responseType: 'blob'})
    }
}

export const officeDocumentApi = {
    list(params) {
        return request.get('/office-documents', {params})
    },
    userOptions(params) {
        return request.get('/office-documents/user-options', {params})
    },
    create(data) {
        return request.post('/office-documents', data)
    },
    update(id, data) {
        return request.patch(`/office-documents/${id}`, data)
    },
    export(id) {
        return request.get(`/office-documents/${id}/export`, {responseType: 'blob'})
    }
}

export const activityApi = {
    list(params) {
        return request.get('/activities', {params})
    },
    detail(id) {
        return request.get(`/activities/${id}`)
    },
    create(data) {
        return request.post('/activities', data)
    },
    update(id, data) {
        return request.patch(`/activities/${id}`, data)
    },
    remove(id) {
        return request.delete(`/activities/${id}`)
    },
    register(id, data) {
        return request.post(`/activities/${id}/registrations`, data)
    },
    registrations(id) {
        return request.get(`/activities/${id}/registrations`)
    }
}

export const awardApi = {
    list() {
        return request.get('/awards')
    },
    create(data) {
        return request.post('/awards', data)
    },
    update(id, data) {
        return request.patch(`/awards/${id}`, data)
    },
    remove(id) {
        return request.delete(`/awards/${id}`)
    }
}

export const applicationApi = {
    list(params) {
        return request.get('/applications', {params})
    },
    create(data) {
        return request.post('/applications', data)
    },
    update(id, data) {
        return request.patch(`/applications/${id}`, data)
    },
    submit(id) {
        return request.post(`/applications/${id}/submit`)
    },
    withdraw(id) {
        return request.post(`/applications/${id}/withdraw`)
    }
}

export const approvalApi = {
    records(applicationId) {
        return request.get(`/applications/${applicationId}/approval-records`)
    },
    approve(applicationId, data) {
        return request.post(`/applications/${applicationId}/approvals`, data)
    },
    batch(data) {
        return request.post('/applications/batch-approvals', data)
    }
}

export const interviewApi = {
    list(params) {
        return request.get('/interviews', {params})
    },
    create(data) {
        return request.post('/interviews', data)
    },
    update(id, data) {
        return request.patch(`/interviews/${id}`, data)
    },
    confirm(id) {
        return request.post(`/interviews/${id}/confirm`)
    },
    feedback(id, data) {
        return request.post(`/interviews/${id}/feedback`, data)
    },
    complete(id) {
        return request.post(`/interviews/${id}/complete`)
    }
}

export const membershipApi = {
    list(params) {
        return request.get('/memberships', {params})
    },
    create(data) {
        return request.post('/memberships', data)
    },
    update(id, data) {
        return request.patch(`/memberships/${id}`, data)
    },
    assignPosition(id, data) {
        return request.post(`/memberships/${id}/assign-position`, data)
    },
    changeStatus(id, data) {
        return request.post(`/memberships/${id}/change-status`, data)
    },
    finalDecision(applicationId, data) {
        return request.post(`/applications/${applicationId}/final-decisions`, data)
    }
}

export const rbacApi = {
    roles(params) {
        return request.get('/roles', {params})
    },
    createRole(data) {
        return request.post('/roles', data)
    },
    updateRole(id, data) {
        return request.patch(`/roles/${id}`, data)
    },
    deleteRole(id) {
        return request.delete(`/roles/${id}`)
    },
    assignRolePermissions(id, data) {
        return request.post(`/roles/${id}/permissions`, data)
    },
    permissions(params) {
        return request.get('/permissions', {params})
    },
    createPermission(data) {
        return request.post('/permissions', data)
    }
}

export const logApi = {
    operation(params) {
        return request.get('/operation-logs', {params})
    },
    login(params) {
        return request.get('/login-logs', {params})
    }
}
