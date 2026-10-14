/**
 * authStore.js：账号模块的状态仓库。
 * 职责范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 集中维护跨组件共享状态，并通过仓库动作完成读取或更新。
 * 主要依赖：pinia、../api/accountApi、../api/http。
 */

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
      const { allowDraftWrites } = await import('../../../shared/editor/recovery/draftStore')
      allowDraftWrites(this.currentUser?.id)
      clearCsrf()
      this.initialized = true
    },
    async refresh() {
      this.currentUser = await api.currentAccount()
      this.initialized = true
    },
    async logout() {
      const { flushDrafts,accountDrafts,clearAccountDrafts,allowDraftWrites } = await import('../../../shared/editor/recovery/draftStore')
      const account = this.currentUser?.id
      await flushDrafts()
      let drafts = []
      try { if (account != null) drafts = await accountDrafts(account) } catch { /* Storage failure must not prevent a server logout. */ }
      if (drafts.length && !window.confirm(`当前浏览器有 ${drafts.length} 个字段的本地恢复稿。确认退出并删除这些本地副本？尚未上传的内容将无法恢复。`)) throw new Error('已取消退出，本地恢复稿仍保留。')
      allowDraftWrites(account,false)
      try { await api.logout() } catch(error) { allowDraftWrites(account);throw error }
      this.currentUser = null
      clearCsrf()
      if (drafts.length) await clearAccountDrafts(account).catch(() => {})
    },
  },
})
