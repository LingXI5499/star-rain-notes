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