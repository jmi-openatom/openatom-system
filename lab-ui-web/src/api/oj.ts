import request from '@/api/request'

export interface Problem {
  id: number
  problemDate: string
  title: string
  difficulty: string
  descriptionMarkdown: string
  timeLimitMs: number
  memoryLimitMb: number
  sampleCases: Array<{ id: number; inputText: string; expectedOutput: string }>
}

export interface Submission {
  id: number
  problemId: number
  language: string
  judgeStatus: string
  scorePassed: number
  totalCases: number
  errorMessage?: string
  submittedAt: string
  judgedAt?: string
}

export function todayProblem() {
  return request.get<Problem>('/oj/today')
}

export function submitCode(problemId: number, language: string, code: string) {
  return request.post<Submission>('/oj/submissions', { problemId, language, code })
}

export function submissions() {
  return request.get<Submission[]>('/oj/submissions')
}
