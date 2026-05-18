export function displayText(value: unknown, fallback = '-'): string {
  return value === null || value === undefined || value === '' ? fallback : String(value)
}

export const BUSINESS_TIME_ZONE = 'Asia/Shanghai'

type DateLike = string | number | Date | null | undefined

type BusinessDateParts = {
  year: string
  month: string
  day: string
  hour: string
  minute: string
  second: string
}

function normalizeBusinessDateTimeString(value: string): string {
  const trimmed = value.trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
    return `${trimmed}T00:00:00+08:00`
  }
  if (/^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?$/.test(trimmed)) {
    return `${trimmed.replace(' ', 'T')}+08:00`
  }
  return trimmed
}

function toBusinessDate(value: DateLike): Date | null {
  if (!value) return null
  const date =
    typeof value === 'string' ? new Date(normalizeBusinessDateTimeString(value)) : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function businessDateParts(value: DateLike): BusinessDateParts | null {
  const date = toBusinessDate(value)
  if (!date) return null
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: BUSINESS_TIME_ZONE,
    hourCycle: 'h23',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).formatToParts(date)
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]))
  return {
    year: values.year,
    month: values.month,
    day: values.day,
    hour: values.hour,
    minute: values.minute,
    second: values.second,
  }
}

export function formatDateTime(value: DateLike): string {
  const date = toBusinessDate(value)
  if (!date) return value ? String(value) : '-'
  return date.toLocaleString('zh-CN', { hour12: false, timeZone: BUSINESS_TIME_ZONE })
}

export function toDateTimeInputValue(value: DateLike): string {
  const parts = businessDateParts(value)
  if (!parts) return ''
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`
}

export function todayDateValue(): string {
  const parts = businessDateParts(new Date())
  if (!parts) return ''
  return `${parts.year}-${parts.month}-${parts.day}`
}

export function monthDayParts(value: DateLike): Pick<BusinessDateParts, 'month' | 'day'> | null {
  const parts = businessDateParts(value)
  if (!parts) return null
  return { month: parts.month, day: parts.day }
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
