export function displayText(value, fallback = '-') {
  return value === null || value === undefined || value === '' ? fallback : value
}

export function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

export function statusType(status) {
  const map = {
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
