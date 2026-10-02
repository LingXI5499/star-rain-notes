export const roleLabel = (code) => ({ USER: '普通用户', ADMIN: '管理员', SUPER_ADMIN: '超级管理员' })[code] || code
export const statusLabel = (code) => ({ ACTIVE: '正常', DISABLED: '已停用', ENABLED: '已启用', SUCCESS: '成功', FAILED: '失败' })[code] || code
export function dateLabel(value) {
  if (!value) return '—'
  const date = new Date(value.endsWith('Z') ? value : value + 'Z')
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}
export const utcQueryTime = (value) => value ? new Date(value).toISOString().slice(0, -1) : ''
export const auditActions = {
  ADMIN_INVITATION_RESENT: '重发邀请通知', ADMIN_INVITATION_REVOKED: '撤销管理员邀请', ADMIN_ROLE_REVOKED: '取消管理员身份',
  EMAIL_CODE_SENT: '发送邮箱验证码', EMAIL_CODE_DELIVERY_FAILED: '邮箱验证码发送失败', EMAIL_VERIFIED: '验证账户邮箱',
  REGISTER_SUCCESS: '用户注册', LOGIN_SUCCESS: '登录成功', LOGIN_FAILED: '登录失败', LOGOUT: '退出登录',
  PASSWORD_CHANGED: '修改密码', PASSWORD_RESET_REQUESTED: '请求重置密码', PASSWORD_RESET_SUCCESS: '重置密码成功',
  ADMIN_INVITED: '邀请管理员', ADMIN_INVITATION_ACCEPTED: '接受管理员邀请', ACCOUNT_ENABLED: '启用账户',
  ACCOUNT_DISABLED: '停用账户', ACCOUNT_ROLE_CHANGED: '修改账户角色', ROLE_PERMISSION_CHANGED: '修改角色权限',
  BOOTSTRAP_SUPER_ADMIN_CREATED: '初始化超级管理员',
  SUPER_ADMIN_IDENTITY_CORRECTED: '修正超级管理员初始化邮箱',
}
export const actionLabel = (code) => auditActions[code] || code
