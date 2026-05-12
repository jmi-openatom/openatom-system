export function formatDateTime(value?: string | number | Date | null): string {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(String(value).replace(/-/g, '/'))
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`
}

export function formatRange(start?: string | null, end?: string | null): string {
  const left = formatDateTime(start)
  const right = formatDateTime(end)
  if (left && right) return `${left} 至 ${right}`
  return left || right || '时间待定'
}

export function statusText(status?: string): string {
  const map: Record<string, string> = {
    draft: '草稿',
    published: '已发布',
    open: '收集中',
    closed: '已结束',
    archived: '已归档',
    submitted: '已提交',
    reviewing: '审核中',
    approved: '已通过',
    pre_screen_passed: '初筛通过',
    pre_screen_rejected: '初筛未过',
    interview_scheduled: '已安排面试',
    interviewed: '已面试',
    final_approved: '已录取',
    waitlisted: '候补',
    rejected: '未通过',
    cancelled: '已撤回',
  }
  return (status && map[status]) || status || '-'
}

export function statusTone(status?: string): string {
  if (['open', 'published', 'approved', 'final_approved', 'pre_screen_passed'].includes(status || '')) {
    return 'success'
  }
  if (['closed', 'archived', 'rejected', 'cancelled', 'pre_screen_rejected'].includes(status || '')) {
    return 'muted'
  }
  if (['interview_scheduled', 'interviewed', 'reviewing', 'submitted', 'waitlisted'].includes(status || '')) {
    return 'warning'
  }
  return 'default'
}

export function ensureList<T = any>(value: any): T[] {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.list)) return value.list
  if (Array.isArray(value?.items)) return value.items
  if (Array.isArray(value?.data)) return value.data
  return []
}
