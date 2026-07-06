import { getCurrentUser } from './auth.ts'

/**
 * 入社介绍是否已完成。
 *
 * 状态由后端 `tb_user.onboarding_completed_at` 字段持久化，前端只读取
 * `/auth/me` 返回并缓存到 localStorage 的用户对象判断，不自行写标记。
 */
export function hasSeenOnboarding(): boolean {
  const user = getCurrentUser() as { onboardingCompletedAt?: unknown }
  const value = user?.onboardingCompletedAt
  return value !== null && value !== undefined && value !== ''
}
