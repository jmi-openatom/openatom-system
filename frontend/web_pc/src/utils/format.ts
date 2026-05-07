export function displayText(value: unknown, fallback = '-'): string {
  return value === null || value === undefined || value === '' ? fallback : String(value)
}

export function formatDateTime(value: string | number | Date | null | undefined): string {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

export function statusType(status: string): string {
  const map: Record<string, string> = {
    enabled: 'success',
    active: 'success',
    published: 'success',
    open: 'success',
    approved: 'success',
    pre_screen_passed: 'success',
    final_approved: 'success',
    confirmed: 'success',
    completed: 'success',
    pending: 'warning',
    interviewing: 'warning',
    submitted: 'warning',
    draft: 'info',
    pre_screen_rejected: 'danger',
    waitlisted: 'warning',
    probation: 'warning',
    suspended: 'warning',
    locked: 'warning',
    interview_scheduled: 'primary',
    interviewed: 'primary',
    disabled: 'danger',
    rejected: 'danger',
    cancelled: 'info',
    closed: 'info',
    archived: 'info',
    left: 'info',
    graduated: 'info',
  }
  return map[String(status || '').toLowerCase()] || 'info'
}

export function applicationStatusText(status: unknown): string {
  return (
    {
      draft: '草稿',
      submitted: '已提交',
      reviewing: '审核中',
      approved: '初审通过',
      pre_screen_passed: '初审通过',
      pre_screen_rejected: '初审驳回',
      interview_scheduled: '面试已安排',
      interviewed: '已面试',
      final_approved: '终审通过',
      waitlisted: '候补',
      rejected: '已拒绝',
      cancelled: '已撤回',
    }[String(status || '').toLowerCase()] ||
    String(status || '-') ||
    '-'
  )
}

export function interviewStatusText(status: unknown): string {
  return (
    {
      pending: '待确认',
      confirmed: '已确认',
      completed: '已完成',
      cancelled: '已取消',
    }[String(status || '').toLowerCase()] ||
    String(status || '-') ||
    '-'
  )
}

export function membershipStatusText(status: unknown): string {
  return (
    {
      probation: '试用',
      active: '在会',
      suspended: '暂停',
      left: '退出',
      graduated: '毕业',
    }[String(status || '').toLowerCase()] ||
    String(status || '-') ||
    '-'
  )
}

export function clubStatusText(status: unknown): string {
  return (
    {
      enabled: '启用',
      disabled: '禁用',
    }[String(status || '').toLowerCase()] ||
    String(status || '-') ||
    '-'
  )
}

export function recruitmentStatusText(status: unknown): string {
  return (
    {
      open: '招新中',
      closed: '已关闭',
      published: '已发布',
      draft: '草稿',
      archived: '已归档',
      unknown: '未知',
    }[String(status || '').toLowerCase()] ||
    String(status || '-') ||
    '-'
  )
}
