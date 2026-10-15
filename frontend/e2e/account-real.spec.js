/** Browser → real Spring Security/Controller/Service/Mapper/MySQL → loopback SMTP. No API interception. */
import { test, expect } from '@playwright/test'
import { createAccountTestSmtp } from '../../tools/account-test-smtp.mjs'
import { randomBytes } from 'node:crypto'

test.describe.configure({ mode: 'serial' })
const smtp = createAccountTestSmtp(), password = `Isolated-${randomBytes(18).toString('hex')}!`
test.beforeAll(async () => { await smtp.start() })
test.afterAll(async () => { console.log('Isolated SMTP capture counts:', smtp.metrics); await smtp.close() })
const fixture = () => { const name = `browser_v3_${randomBytes(7).toString('hex')}`; return { name, email: `${name}@example.test` } }
async function write(client, method, url, data) {
  const token = (await (await client.get('/api/auth/csrf')).json()).data.token
  return client.fetch(url, { method, data, headers: { 'X-XSRF-TOKEN': token } })
}
async function mail(email, pattern) {
  await expect.poll(() => smtp.latest(email, pattern), { timeout: 20000 }).not.toBe('')
  return smtp.latest(email, pattern)
}
async function register(client, user) {
  expect((await write(client, 'POST', '/api/auth/register/verification-codes', { email: user.email })).status()).toBe(200)
  const verificationCode = await mail(user.email, /(\d{6})/)
  expect((await write(client, 'POST', '/api/auth/register', {
    username: user.name, email: user.email, verificationCode, password, confirmPassword: password,
  })).status()).toBe(201)
}
async function login(page, identifier, secret = password) {
  await page.goto('/useradmin/login')
  await page.getByLabel('用户名或邮箱').fill(identifier)
  await page.locator('input[autocomplete="current-password"]').fill(secret)
  await page.locator('form button[type="submit"]').click()
  await expect(page).toHaveURL(/\/useradmin\/?$/)
}

test('AUTH-01/03/06/13: real email registration, login, profile, password reset and logout', async ({ page }) => {
  const user = fixture()
  await page.goto('/useradmin/register')
  await page.getByLabel('用户名', { exact: true }).fill(user.name)
  await page.getByLabel('邮箱', { exact: true }).fill(user.email)
  await page.getByRole('button', { name: '发送验证码', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('如果邮箱可接收邮件')
  await page.getByLabel('邮箱验证码', { exact: false }).fill(await mail(user.email, /(\d{6})/))
  await page.getByLabel('密码', { exact: true }).fill(password)
  await page.getByLabel('确认密码', { exact: true }).fill(password)
  await page.locator('form button[type="submit"]').click()
  await expect(page).toHaveURL(/\/useradmin\/login\?registered=1$/)
  await login(page, user.email)
  await page.goto('/useradmin/center')
  await page.getByLabel('显示名称').fill('Account V3 browser fixture')
  await page.getByRole('button', { name: '保存资料', exact: true }).click()
  await expect(page.getByRole('status').filter({ hasText: '个人资料已保存' })).toBeVisible()
  await page.goto('/useradmin/forgot-password')
  await page.getByLabel('注册邮箱').fill(user.email)
  await page.locator('form button[type="submit"]').click()
  await expect(page.getByRole('status')).toContainText('请求已受理')
  const token = await mail(user.email, /token=([A-Za-z0-9_-]+)/), nextPassword = `${password}2`
  await page.goto(`/useradmin/reset-password?token=${token}`)
  await page.getByLabel('新密码', { exact: true }).fill(nextPassword)
  await page.getByLabel('确认新密码', { exact: true }).fill(nextPassword)
  await page.locator('form button[type="submit"]').click()
  await expect(page).toHaveURL(/\/useradmin\/login\?reset=1$/)
  await login(page, user.name, nextPassword)
  await page.goto('/useradmin/center')
  await page.getByRole('button', { name: /Account V3 browser fixture/ }).click()
  await page.getByRole('menuitem', { name: '退出登录' }).click()
  await expect(page).toHaveURL(/\/useradmin\/?$/)
  expect((await page.request.get('/api/account/me')).status()).toBe(401)
})

test('AUTH-15/16/18/22: super admin invitation and revocation invalidate the member session', async ({ page, browser }) => {
  const user = fixture(); await register(page.request, user)
  const admin = await browser.newContext({ baseURL: 'http://127.0.0.1:5189' })
  try {
    expect((await write(admin.request, 'POST', '/api/auth/login', {
      identifier: process.env.STAR_RAIN_E2E_USERNAME, password: process.env.STAR_RAIN_E2E_PASSWORD,
    })).status()).toBe(200)
    const list = (await (await admin.request.get(`/api/admin/accounts?keyword=${user.name}`)).json()).data.items
    const account = list.find(item => item.username === user.name)
    expect(account).toBeTruthy()
    const adminPage = await admin.newPage()
    await adminPage.goto('/useradmin/accounts')
    await adminPage.getByLabel('关键词').fill(user.name)
    await adminPage.getByRole('button', { name: '查询', exact: true }).click()
    await expect(adminPage.locator('table')).toContainText(user.name)
    expect((await write(admin.request, 'POST', '/api/admin/account-invitations', { targetAccountId: account.id })).status()).toBe(200)
    await login(page, user.name); await page.goto('/useradmin/center')
    page.on('dialog', dialog => dialog.accept())
    await page.getByRole('button', { name: '接受管理员邀请', exact: true }).click()
    // ConfirmDialog is an in-page accessible dialog.
    const confirm = page.getByRole('dialog')
    await expect(confirm).toBeVisible()
    await confirm.getByRole('button', { name: '确认', exact: true }).click()
    await expect(page).toHaveURL(/\/useradmin\/login\?invited=1$/)
    await login(page, user.name)
    expect((await page.request.get('/api/admin/accounts')).status()).toBe(403)
    expect((await write(admin.request, 'POST', `/api/admin/accounts/${account.id}/revoke-admin`, {})).status()).toBe(200)
    expect((await page.request.get('/api/account/me')).status()).toBe(401)
    expect((await write(admin.request, 'PATCH', `/api/admin/accounts/${account.id}/status`, { status: 'DISABLED' })).status()).toBe(200)
    expect((await write(page.request, 'POST', '/api/auth/login', { identifier: user.name, password })).status()).toBe(401)
  } finally { await admin.close() }
})
