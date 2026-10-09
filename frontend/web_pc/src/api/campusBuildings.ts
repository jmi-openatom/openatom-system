import request from './request'

export interface CampusPhoto {
  id: number
  url: string
  originalName: string
}

export interface CampusBuildingDetail {
  buildingId: string
  buildingName: string
  description: string | null
  photos: CampusPhoto[]
}

export type CampusSubmissionStatus = 'pending' | 'approved' | 'rejected' | 'hidden'
export interface CampusSubmission {
  id: number
  buildingId: string
  buildingName: string
  authorName: string
  description: string | null
  replaceDescription: boolean
  status: CampusSubmissionStatus
  reviewReason: string | null
  createdAt: string
  reviewedAt: string | null
  photos: CampusPhoto[]
  removedPhotos: CampusPhoto[]
}

export interface CampusSubmissionPage {
  list: CampusSubmission[]
  total: number
  page: number
  pageSize: number
}

export const campusBuildingApi = {
  detail(id: string | number): Promise<CampusBuildingDetail> {
    return request.get(`/site/campus-buildings/${encodeURIComponent(id)}`)
  },
  submit(
    id: string | number,
    description: string,
    replaceDescription: boolean,
    photos: File[],
    removedPhotoIds: number[],
  ): Promise<number> {
    const form = new FormData()
    form.append('description', description)
    form.append('replaceDescription', String(replaceDescription))
    photos.forEach((photo) => form.append('photos', photo))
    removedPhotoIds.forEach((id) => form.append('removedPhotoIds', String(id)))
    return request.post(`/campus-buildings/${encodeURIComponent(id)}/submissions`, form, {
      timeout: 120000,
    })
  },
  mine(id: string | number, page = 1): Promise<CampusSubmissionPage> {
    return request.get(`/campus-buildings/${encodeURIComponent(id)}/submissions/my`, {
      params: { page, pageSize: 5 },
    })
  },
  submissions(params: {
    buildingId?: string
    status?: string
    page: number
    pageSize: number
  }): Promise<CampusSubmissionPage> {
    return request.get('/campus-buildings/admin/submissions', { params })
  },
  review(id: number, action: 'approve' | 'reject' | 'hide', reason: string): Promise<void> {
    return request.post(`/campus-buildings/admin/submissions/${id}/review`, { action, reason })
  },
}

export const campusSubmissionLabels: Record<CampusSubmissionStatus, string> = {
  pending: '待审核',
  approved: '已通过',
  rejected: '已驳回',
  hidden: '已下架',
}
