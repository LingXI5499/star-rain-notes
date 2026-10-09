import { get, patch, post, put } from './http'

export const register = (data) => post('/auth/register', data)
export const sendRegistrationCode = (email) => post('/auth/register/verification-codes', { email })
export const sendAccountEmailCode = () => post('/account/me/email/verification-codes')
export const confirmAccountEmail = (verificationCode) => post('/account/me/email/confirm', { verificationCode })
export const login = (data) => post('/auth/login', data)
export const logout = () => post('/auth/logout')
export const currentAccount = () => get('/account/me')
export const updateAccount = (data) => patch('/account/me', data)
export const changePassword = (data) => put('/account/me/password', data)
export const requestReset = (email) => post('/auth/password-reset/request', { email })
export const confirmReset = (data) => post('/auth/password-reset/confirm', data)
export const acceptInvitation = (token) => post('/account/invitations/accept', { token })

export const listAccounts = (params) => get('/admin/accounts', params)
export const changeAccountStatus = (id, status) =>
  patch(`/admin/accounts/${encodeURIComponent(id)}/status`, { status })
export const createInvitation = (targetAccountId) =>
  post('/admin/account-invitations', { targetAccountId })
export const listInvitations = (params) => get('/admin/account-invitations', params)
export const resendInvitation = (id) => post(`/admin/account-invitations/${encodeURIComponent(id)}/resend`)
export const revokeInvitation = (id) => post(`/admin/account-invitations/${encodeURIComponent(id)}/revoke`)
export const myInvitations = () => get('/account/invitations')
export const acceptMyInvitation = (id) => post(`/account/invitations/${encodeURIComponent(id)}/accept`)
export const revokeAdministrator = (id) => post(`/admin/accounts/${encodeURIComponent(id)}/revoke-admin`)
export const listAudits = (params) => get('/admin/account-audits', params)

