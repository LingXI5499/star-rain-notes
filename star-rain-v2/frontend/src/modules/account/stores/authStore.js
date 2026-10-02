import { defineStore } from 'pinia'
import * as api from '../api/accountApi'
import { clearCsrf } from '../api/http'

export const useAuthStore = defineStore('account-auth', {
  state: () => ({ currentUser: null, initialized: false }),
  getters: {
    hasPermission: (state) => (code) => state.currentUser?.permissions?.includes(code) ?? false,
    canManage: (state) => (code) => Boolean(state.currentUser?.roles?.includes('SUPER_ADMIN') && state.currentUser?.permissions?.includes(code)),
  },
  actions: {
    async initialize() {
      try {
        this.currentUser = await api.currentAccount()
      } catch (error) {
        this.currentUser = null
        if (error.response?.status !== 401) throw error
      } finally {
        this.initialized = true
      }
    },
    async login(identifier, password) {
      this.currentUser = await api.login({ identifier, password })
      clearCsrf()
      this.initialized = true
    },
    async refresh() {
      this.currentUser = await api.currentAccount()
      this.initialized = true
    },
    async logout() {
      await api.logout()
      this.currentUser = null
      clearCsrf()
    },
  },
})
