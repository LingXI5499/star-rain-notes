import { test, expect } from './fixtures'

test('root keeps the existing public homepage and the normal account entry', async ({ page, fixture }) => {
  fixture.signedIn = false
  await page.goto('/')
  await expect(page.locator('.public-shell')).toBeVisible()
  await expect(page.locator('.account-entry a[href="/login"]')).toBeVisible()
  await expect(page.locator('.admin-shell')).toHaveCount(0)
  await page.locator('.account-entry a[href="/login"]').click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.locator('a[href="/register"]')).toBeVisible()
})

test('old reset and invitation links retain token, hash and login return target', async ({ page, fixture }) => {
  fixture.signedIn = false
  await page.goto('/useradmin/reset-password?token=a%2Bb%3D#reset')
  await expect(page).toHaveURL(/\/reset-password\?token=a%2Bb=#reset$/)
  await expect(page.locator('input[autocomplete="new-password"]').first()).toBeVisible()
  await page.goto('/useradmin/invitation/accept?token=a%2Bb%3D#accept')
  await expect(page).toHaveURL(/\/login\?redirect=/)
  expect(new URL(page.url()).searchParams.get('redirect')).toBe('/invitation/accept?token=a%2Bb=#accept')
  await page.locator('input[autocomplete="username"]').fill('fixture_admin')
  await page.locator('input[autocomplete="current-password"]').fill('isolated-fixture-password')
  await page.locator('button[type="submit"]').click()
  await expect(page).toHaveURL(/\/invitation\/accept\?token=a%2Bb=#accept$/)
})

test('old homepage and protected bookmarks use canonical URLs after login and refresh', async ({ page, fixture }) => {
  fixture.signedIn = false
  await page.goto('/useradmin?source=bookmark#top')
  await expect(page).toHaveURL(/\/\?source=bookmark#top$/)
  await page.goto('/useradmin/center?tab=profile#details')
  await expect(page).toHaveURL(/\/login\?redirect=/)
  expect(new URL(page.url()).searchParams.get('redirect')).toBe('/center?tab=profile#details')
  await page.locator('input[autocomplete="username"]').fill('fixture_admin')
  await page.locator('input[autocomplete="current-password"]').fill('isolated-fixture-password')
  await page.locator('button[type="submit"]').click()
  await expect(page).toHaveURL(/\/center\?tab=profile#details$/)
  await page.reload()
  await expect(page).toHaveURL(/\/center\?tab=profile#details$/)
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', 'noindex,nofollow')
})

test('mobile menu offers canonical account and content navigation', async ({ page, fixture }) => {
  fixture.signedIn = false
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/')
  await page.locator('.mobile-public-nav').getByRole('button', { name: /更多/ }).click()
  await expect(page.getByRole('dialog').locator('a[href="/center"]')).toBeVisible()
  await expect(page.getByRole('dialog').locator('a[href="/portfolio"]')).toBeVisible()
  await page.getByRole('dialog').locator('a[href="/portfolio"]').click()
  await expect(page).toHaveURL(/\/portfolio$/)
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href', 'https://example.invalid/portfolio')
  expect(await page.locator('a[href^="/useradmin"]').count()).toBe(0)
})
