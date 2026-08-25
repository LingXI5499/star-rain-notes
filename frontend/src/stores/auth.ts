import { defineStore } from 'pinia'
import { http, refreshCsrf } from '@/api/http'

export type AuthStatus = 'unknown' | 'authenticated' | 'anonymous'

interface SessionView {
  authenticated: boolean
  username?: string | null
  email?: string | null
  role?: string | null
  accountStatus?: string | null
  capabilities?: string[]
}

/**
 * V1 session authentication store (TASK-003):
 * fetchSession / login / logout / changePassword, all over the session cookie
 * + CSRF flow. 401 responses clear the store automatically (http interceptor).
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    status: 'unknown' as AuthStatus,
    username: '',
    role: '',
    capabilities: [] as string[],
  }),
  getters: {
    isAuthenticated: (state) => state.status === 'authenticated',
    isSuperAdmin: (state) => state.role === 'SUPER_ADMIN' || state.role === 'ROLE_SUPER_ADMIN'
      || state.capabilities.includes('SUPER_ADMIN'),
    canReview: (state) => state.capabilities.includes('REVIEW') || state.role === 'SUPER_ADMIN'
      || state.role === 'ROLE_SUPER_ADMIN',
  },
  actions: {
    async fetchSession() {
      try {
        const { data } = await http.get<SessionView>('/auth/session')
        if (data.authenticated) {
          this.status = 'authenticated'
          this.username = data.email ?? data.username ?? ''
          this.role = normalizeRole(data.role)
          this.capabilities = data.capabilities ?? []
        } else {
          this.reset()
        }
      } catch {
        this.reset()
      }
    },
    async fetchSetupRequired(): Promise<boolean> {
      const { data } = await http.get<{ setupRequired: boolean }>('/setup/status')
      return data.setupRequired
    },
    async login(email: string, password: string) {
      await http.post('/auth/account/login', { email, password })
      await refreshCsrf()
      await this.fetchSession()
    },
    async logout() {
      try {
        await http.post('/auth/logout')
      } finally {
        await refreshCsrf().catch(() => undefined)
        this.reset()
      }
    },
    async changePassword(currentPassword: string, newPassword: string) {
      await http.put('/auth/account/password', { email: this.username, currentPassword, newPassword })
      await refreshCsrf().catch(() => undefined)
      this.reset()
    },
    reset() {
      this.status = 'anonymous'
      this.username = ''
      this.role = ''
      this.capabilities = []
    },
  },
})

function normalizeRole(role?: string | null): string {
  if (!role) return ''
  return role.startsWith('ROLE_') ? role.slice(5) : role
}
