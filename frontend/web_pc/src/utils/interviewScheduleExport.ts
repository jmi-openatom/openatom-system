import { interviewSessionApi } from '@/api'

export async function downloadInterviewSchedule(sessionId: number, sessionName?: string) {
  const blob = await interviewSessionApi.exportSchedule(sessionId)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `面试时间安排-${sessionName || sessionId}.csv`.replace(/[\\/:*?"<>|]/g, '_')
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
