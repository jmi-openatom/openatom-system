import { defineStore } from 'pinia'
import { getSession, logout as logoutRequest, type CurrentMember } from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    member: null as CurrentMember | null,
    resolved: false,
  }),
  actions: {
    async resolve(force = false) {
      if (this.resolved && !force) return this.member
      try {
        this.member = await getSession()
      } catch {
        this.member = null
      } finally {
        this.resolved = true
      }
      return this.member
    },
    async logout() {
      const redirect = await logoutRequest()
      this.member = null
      this.resolved = true
      window.location.assign(redirect)
    },
  },
})
