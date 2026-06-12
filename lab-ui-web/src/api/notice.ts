import request from '@/api/request'

export interface Notice {
  id: number
  noticeType: string
  title: string
  content: string
  readAt?: string
  createdAt: string
}

export function notices() {
  return request.get<Notice[]>('/notices')
}

export function markNoticeRead(id: number) {
  return request.patch<string>(`/notices/${id}/read`)
}
