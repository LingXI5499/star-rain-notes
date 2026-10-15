/** Identity generations guard responses; server login/logout mutations execute in order. */
import { defineStore } from 'pinia'
import * as api from '../api/accountApi'
import { invalidateAccountRequests } from '../../../shared/http'
import * as drafts from '../../../shared/editor/recovery/draftStore'

const mutations = new WeakMap()
function mutate(store, operation) {
  const pending = (mutations.get(store) || Promise.resolve()).catch(() => {}).then(operation)
  mutations.set(store, pending)
  return pending
}
export const useAuthStore = defineStore('account-auth', {
  state: () => ({
    currentUser: null, initialized: false, identityVersion: 0, readVersion: 0,
    loginBusy: false, logoutBusy: false, cleanupAccounts: [], cleanupBusy: false, cleanupError: '',
  }),
  getters: {
    hasPermission: (state) => (code) => state.currentUser?.permissions?.includes(code) ?? false,
    canManage: (state) => (code) => Boolean(state.currentUser?.roles?.includes('SUPER_ADMIN') && state.currentUser?.permissions?.includes(code)),
  },
  actions: {
    invalidateIdentity() {
      this.identityVersion++
      invalidateAccountRequests()
    },
    expireSession() {
      this.invalidateIdentity()
      if (this.currentUser?.id != null) drafts.allowDraftWrites(this.currentUser.id, false)
      this.currentUser = null
      this.initialized = true
    },
    async initialize() { return this.readAccount(true) },
    async refresh() { return this.readAccount(false) },
    async readAccount(anonymousAllowed) {
      const identity = this.identityVersion, read = ++this.readVersion
      try {
        await mutations.get(this)
        if (identity !== this.identityVersion || this.logoutBusy) return false
        const user = await api.currentAccount()
        if (identity !== this.identityVersion || read !== this.readVersion || this.logoutBusy) return false
        this.currentUser = user
        return true
      } catch (error) {
        if (identity !== this.identityVersion || read !== this.readVersion) return false
        if (error.response?.status === 401) this.currentUser = null
        else throw error
        if (!anonymousAllowed) throw error
      } finally {
        if (identity === this.identityVersion && read === this.readVersion) this.initialized = true
      }
    },
    async login(identifier, password) {
      if (this.loginBusy || this.logoutBusy) return false
      this.loginBusy = true
      this.invalidateIdentity()
      const identity = this.identityVersion
      try {
        const user = await mutate(this, () => api.login({ identifier, password }))
        if (identity !== this.identityVersion) return false
        this.currentUser = user
        drafts.allowDraftWrites(user?.id)
        invalidateAccountRequests()
        this.initialized = true
        return true
      } finally { this.loginBusy = false }
    },
    /** A late profile/email response cannot restore an identity that has logged out. */
    async updateCurrent(action) {
      const identity = this.identityVersion, account = this.currentUser?.id
      const user = await action()
      if (identity !== this.identityVersion || account !== this.currentUser?.id || this.logoutBusy) return false
      this.currentUser = user
      return true
    },
    async logout() {
      if (this.logoutBusy) return false
      this.logoutBusy = true
      const account = this.currentUser?.id
      try {
        let records = [], inspectionFailed = false
        try {
          await drafts.flushDrafts()
          if (account != null) records = await drafts.accountDrafts(account)
        } catch { inspectionFailed = true }
        const message = inspectionFailed
          ? '无法检查本浏览器的本地恢复稿。选择“确定”仍然退出（本地备份可能残留）；选择“取消”返回检查。'
          : `当前浏览器有 ${records.length} 个字段的本地恢复稿。确认退出并删除这些本地副本？尚未上传的内容将无法恢复。`
        if ((inspectionFailed || records.length) && !window.confirm(message)) {
          throw Object.assign(new Error('已取消退出，请返回检查本地恢复稿。'), { code: 'LOGOUT_CANCELED' })
        }
        this.invalidateIdentity()
        drafts.allowDraftWrites(account, false)
        try {
          await mutate(this, () => api.logout())
        } catch (error) {
          if (error.response?.status !== 401) {
            drafts.allowDraftWrites(account)
            throw error
          }
        }
        this.expireSession()
        if (account != null) {
          if (!this.cleanupAccounts.includes(account)) this.cleanupAccounts.push(account)
          await this.retryDraftCleanup()
        }
        return true
      } finally { this.logoutBusy = false }
    },
    /** Deletion failures remain visible after navigation and can be retried independently. */
    async retryDraftCleanup() {
      if (this.cleanupBusy) return
      this.cleanupBusy = true
      this.cleanupError = ''
      for (const account of [...this.cleanupAccounts]) {
        try {
          await drafts.clearAccountDrafts(account)
          this.cleanupAccounts = this.cleanupAccounts.filter(item => item !== account)
        } catch {
          this.cleanupError = '服务端已退出，但本浏览器可能仍有本地副本。请重试清理；也可在浏览器站点设置中清除本站数据。公共电脑请勿保留恢复稿。'
        }
      }
      this.cleanupBusy = false
    },
  },
})
