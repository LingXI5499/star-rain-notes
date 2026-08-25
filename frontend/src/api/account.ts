import { http } from './http'

export interface ActivationStatus {
  configured: boolean
  activated: boolean
  emailMasked: string
}
export interface InvitationStatus {
  email: string
  status: string
  expiresAt: string
}

export async function fetchActivationStatus(): Promise<ActivationStatus> {
  return (await http.get<ActivationStatus>('/auth/super-admin-activation/status')).data
}
export async function sendActivationCode(): Promise<void> {
  await http.post('/auth/super-admin-activation/verification-codes', {})
}
export async function confirmActivation(verificationCode: string, password: string): Promise<void> {
  await http.post('/auth/super-admin-activation/confirm', { verificationCode, password })
}

export async function fetchInvitationStatus(token: string): Promise<InvitationStatus> {
  return (await http.get<InvitationStatus>(`/auth/invitations/${token}`)).data
}
export async function sendInvitationCode(token: string): Promise<void> {
  await http.post(`/auth/invitations/${token}/verification-codes`, {})
}
export async function registerByInvitation(token: string, email: string, verificationCode: string, password: string): Promise<void> {
  await http.post(`/auth/invitations/${token}/register`, { email, verificationCode, password })
}

export async function sendPasswordResetCode(email: string): Promise<void> {
  await http.post('/auth/password-reset/verification-codes', { email })
}
export async function confirmPasswordReset(email: string, verificationCode: string, newPassword: string): Promise<void> {
  await http.post('/auth/password-reset/confirm', { email, verificationCode, newPassword })
}

export interface AccountUser {
  id: number
  email: string
  role: string
  accountStatus: string
  emailVerifiedAt: string | null
  activatedAt: string | null
  lockedUntil: string | null
  lastLoginAt: string | null
  passwordChangedAt: string | null
  disabledAt: string | null
  disabledBy: number | null
  disabledReason: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminInvitation {
  id: number
  email: string
  status: string
  invitedBy: number
  acceptedAccountId: number | null
  expiresAt: string
  sentAt: string | null
  acceptedAt: string | null
  revokedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface AuditLog {
  id: number
  actorId: number | null
  action: string
  targetType: string | null
  targetId: number | null
  result: string
  requestIpHash: string | null
  userAgentSummary: string | null
  metadataJson: Record<string, unknown> | null
  createdAt: string
}

export interface ContentReview {
  id: number
  contentType: string
  contentId: number
  actionType: string
  title: string
  payload: Record<string, unknown>
  status: string
  submittedBy: number | null
  reviewedBy: number | null
  reviewNote: string | null
  createdAt: string
  updatedAt: string
  reviewedAt: string | null
}

export async function fetchSuperAdminUsers(): Promise<AccountUser[]> {
  return (await http.get<AccountUser[]>('/super-admin/users')).data
}

export async function disableAccount(id: number, reason?: string): Promise<void> {
  await http.post(`/super-admin/users/${id}/disable`, { reason: reason || null })
}

export async function enableAccount(id: number): Promise<void> {
  await http.post(`/super-admin/users/${id}/enable`)
}

export async function fetchInvitations(): Promise<AdminInvitation[]> {
  return (await http.get<AdminInvitation[]>('/super-admin/invitations')).data
}

export async function createInvitation(email: string): Promise<AdminInvitation> {
  return (await http.post<AdminInvitation>('/super-admin/invitations', { email })).data
}

export async function resendInvitation(id: number): Promise<void> {
  await http.post(`/super-admin/invitations/${id}/resend`)
}

export async function revokeInvitation(id: number): Promise<void> {
  await http.post(`/super-admin/invitations/${id}/revoke`)
}

export async function fetchAuditLogs(params: { page?: number; pageSize?: number; action?: string }): Promise<AuditLog[]> {
  return (await http.get<AuditLog[]>('/super-admin/audit-logs', { params })).data
}

export async function fetchContentReviews(params: { page?: number; pageSize?: number; status?: string }): Promise<ContentReview[]> {
  return (await http.get<ContentReview[]>('/super-admin/content-reviews', { params })).data
}

export async function approveContentReview(id: number, note?: string): Promise<ContentReview> {
  return (await http.post<ContentReview>(`/super-admin/content-reviews/${id}/approve`, { note: note || null })).data
}

export async function rejectContentReview(id: number, note?: string): Promise<ContentReview> {
  return (await http.post<ContentReview>(`/super-admin/content-reviews/${id}/reject`, { note: note || null })).data
}
