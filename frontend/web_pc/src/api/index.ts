import request from './request'
import type {AxiosResponse} from 'axios'

export const siteApi = {
    clubHome(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/site/club-home', {params})
    },
    activities(): Promise<AxiosResponse> {
        return request.get('/site/activities')
    },
    activityDetail(id: string | number): Promise<AxiosResponse> {
        return request.get(`/site/activities/${id}`)
    },
    recruitment(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/site/recruitment', {params})
    },
    recruitmentDetail(id: string | number): Promise<AxiosResponse> {
        return request.get(`/site/recruitment/${id}`)
    },
    progress(): Promise<AxiosResponse> {
        return request.get('/site/progress')
    },
    registerEnabled(): Promise<AxiosResponse> {
        return request.get('/site/register-enabled')
    },
    formDetail(id: string | number): Promise<AxiosResponse> {
        return request.get(`/site/forms/${id}`)
    }
}

export const authApi = {
    login(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/auth/login', data)
    },
    register(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/auth/register', data)
    },
    logout(data: Record<string, unknown> = {}): Promise<AxiosResponse> {
        return request.post('/auth/logout', data)
    },
    me(): Promise<AxiosResponse> {
        return request.get('/auth/me')
    },
    updateRegisterEnabled(enabled: boolean): Promise<AxiosResponse> {
        return request.patch('/auth/register-enabled', null, {params: {enabled}})
    },
    updatePassword(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch('/auth/password', data)
    },
}

export const userApi = {
    list(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/users', {params})
    },
    create(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/users', data)
    },
    remove(id: string | number): Promise<AxiosResponse> {
        return request.delete(`/users/${id}`)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/users/${id}`, data)
    },
    updateStatus(id: string | number, status: string): Promise<AxiosResponse> {
        return request.patch(`/users/${id}/status`, {status})
    },
    resetPassword(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/users/${id}/reset-password`, data)
    },
    roles(id: string | number): Promise<AxiosResponse> {
        return request.get(`/users/${id}/roles`)
    },
    assignRoles(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/users/${id}/roles`, data)
    }
}

export const clubApi = {
    list(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/clubs', {params})
    },
    create(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/clubs', data)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/clubs/${id}`, data)
    },
    updateStatus(id: string | number, status: string): Promise<AxiosResponse> {
        return request.patch(`/clubs/${id}/status`, {status})
    },
    updateRecruitmentStatus(id: string | number, recruitmentStatus: string): Promise<AxiosResponse> {
        return request.patch(`/clubs/${id}/recruitment-status`, {recruitmentStatus})
    },
    departments(clubId: string | number): Promise<AxiosResponse> {
        return request.get(`/clubs/${clubId}/departments`)
    },
    positions(clubId: string | number): Promise<AxiosResponse> {
        return request.get(`/clubs/${clubId}/positions`)
    },
    campaigns(clubId: string | number): Promise<AxiosResponse> {
        return request.get(`/clubs/${clubId}/recruitment-campaigns`)
    },
    siteForms(clubId: string | number): Promise<AxiosResponse> {
        return request.get(`/clubs/${clubId}/site-forms`)
    }
}

export const positionApi = {
    create(clubId: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/clubs/${clubId}/positions`, data)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/positions/${id}`, data)
    },
    remove(id: string | number): Promise<AxiosResponse> {
        return request.delete(`/positions/${id}`)
    }
}

export const campaignApi = {
    create(clubId: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/clubs/${clubId}/recruitment-campaigns`, data)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/recruitment-campaigns/${id}`, data)
    },
    publish(id: string | number): Promise<AxiosResponse> {
        return request.post(`/recruitment-campaigns/${id}/publish`)
    },
    close(id: string | number): Promise<AxiosResponse> {
        return request.post(`/recruitment-campaigns/${id}/close`)
    }
}

export const siteFormApi = {
    create(clubId: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/clubs/${clubId}/site-forms`, data)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/site-forms/${id}`, data)
    },
    publish(id: string | number): Promise<AxiosResponse> {
        return request.post(`/site-forms/${id}/publish`)
    },
    close(id: string | number): Promise<AxiosResponse> {
        return request.post(`/site-forms/${id}/close`)
    }
}

export const formSubmissionApi = {
    create(formId: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/site/forms/${formId}/submissions`, data)
    },
    list(formId: string | number, params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get(`/site-forms/${formId}/submissions`, {params})
    },
    export(formId: string | number): Promise<AxiosResponse> {
        return request.get(`/site-forms/${formId}/submissions/export`, {responseType: 'blob'})
    }
}

export const officeDocumentApi = {
    list(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/office-documents', {params})
    },
    userOptions(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/office-documents/user-options', {params})
    },
    create(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/office-documents', data)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/office-documents/${id}`, data)
    },
    export(id: string | number): Promise<AxiosResponse> {
        return request.get(`/office-documents/${id}/export`, {responseType: 'blob'})
    }
}

export const activityApi = {
    list(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/activities', {params})
    },
    detail(id: string | number): Promise<AxiosResponse> {
        return request.get(`/activities/${id}`)
    },
    create(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/activities', data)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/activities/${id}`, data)
    },
    remove(id: string | number): Promise<AxiosResponse> {
        return request.delete(`/activities/${id}`)
    },
    register(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/activities/${id}/registrations`, data)
    },
    registrations(id: string | number): Promise<AxiosResponse> {
        return request.get(`/activities/${id}/registrations`)
    }
}

export const awardApi = {
    list(): Promise<AxiosResponse> {
        return request.get('/awards')
    },
    create(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/awards', data)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/awards/${id}`, data)
    },
    remove(id: string | number): Promise<AxiosResponse> {
        return request.delete(`/awards/${id}`)
    }
}

export const applicationApi = {
    list(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/applications', {params})
    },
    create(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/applications', data)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/applications/${id}`, data)
    },
    submit(id: string | number): Promise<AxiosResponse> {
        return request.post(`/applications/${id}/submit`)
    },
    withdraw(id: string | number): Promise<AxiosResponse> {
        return request.post(`/applications/${id}/withdraw`)
    }
}

export const approvalApi = {
    records(applicationId: string | number): Promise<AxiosResponse> {
        return request.get(`/applications/${applicationId}/approval-records`)
    },
    approve(applicationId: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/applications/${applicationId}/approvals`, data)
    },
    batch(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/applications/batch-approvals', data)
    }
}

export const interviewApi = {
    list(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/interviews', {params})
    },
    create(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/interviews', data)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/interviews/${id}`, data)
    },
    confirm(id: string | number): Promise<AxiosResponse> {
        return request.post(`/interviews/${id}/confirm`)
    },
    feedback(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/interviews/${id}/feedback`, data)
    },
    complete(id: string | number): Promise<AxiosResponse> {
        return request.post(`/interviews/${id}/complete`)
    }
}

export const membershipApi = {
    list(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/memberships', {params})
    },
    create(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/memberships', data)
    },
    update(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/memberships/${id}`, data)
    },
    assignPosition(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/memberships/${id}/assign-position`, data)
    },
    changeStatus(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/memberships/${id}/change-status`, data)
    },
    forceExit(id: string | number, data: Record<string, unknown> = {}): Promise<AxiosResponse> {
        return request.post(`/memberships/${id}/force-exit`, data)
    },
    finalDecision(applicationId: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/applications/${applicationId}/final-decisions`, data)
    }
}

export const rbacApi = {
    roles(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/roles', {params})
    },
    createRole(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/roles', data)
    },
    updateRole(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.patch(`/roles/${id}`, data)
    },
    deleteRole(id: string | number): Promise<AxiosResponse> {
        return request.delete(`/roles/${id}`)
    },
    assignRolePermissions(id: string | number, data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post(`/roles/${id}/permissions`, data)
    },
    permissions(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/permissions', {params})
    },
    createPermission(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/permissions', data)
    }
}

export const logApi = {
    operation(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/operation-logs', {params})
    },
    login(params?: Record<string, unknown>): Promise<AxiosResponse> {
        return request.get('/login-logs', {params})
    }
}

export const notificationApi = {
    myNotifications(): Promise<AxiosResponse> {
        return request.get('/notifications')
    },
    unreadCount(): Promise<AxiosResponse> {
        return request.get('/notifications/unread-count')
    },
    markRead(id: string | number): Promise<AxiosResponse> {
        return request.post(`/notifications/${id}/read`)
    },
    adminList(): Promise<AxiosResponse> {
        return request.get('/notifications/admin')
    },
    create(data: Record<string, unknown>): Promise<AxiosResponse> {
        return request.post('/notifications/admin', data)
    },
    delete(id: string | number): Promise<AxiosResponse> {
        return request.delete(`/notifications/admin/${id}`)
    }
}
