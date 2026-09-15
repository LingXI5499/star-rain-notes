import { test, expect } from '@playwright/test'

test.describe('mobile public nav retap', () => {
  test.use({ viewport: { width: 390, height: 844 } })

  test('retapping english on an english page opens the module panel', async ({ page }) => {
    await page.goto('/english')
    await expect(page.getByTestId('mobile-public-nav')).toBeVisible()
    await page.getByTestId('mobile-public-nav').getByRole('button', { name: /英语/ }).click()
    const dialog = page.getByRole('dialog', { name: '英语导航' })
    await expect(dialog).toBeVisible()
    await expect(dialog.getByRole('link', { name: /词汇/ })).toHaveAttribute('href', '/english/vocabulary')
    await expect(dialog.getByRole('link', { name: /学习组合/ })).toHaveCount(0)
  })

  test('leaving english for tutorials goes to root without a panel', async ({ page }) => {
    await page.goto('/english')
    await page.getByTestId('mobile-public-nav').getByRole('button', { name: /教程/ }).click()
    await expect(page).toHaveURL(/\/tutorials\/?$/)
    await expect(page.getByRole('dialog', { name: '教程导航' })).toHaveCount(0)
  })
})
