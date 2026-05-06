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
    pending: 'warning',
    interviewing: 'warning',
    disabled: 'danger',
    rejected: 'danger',
    closed: 'info'
  }
  return map[String(status || '').toLowerCase()] || 'info'
}
