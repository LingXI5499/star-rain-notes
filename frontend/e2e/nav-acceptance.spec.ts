import { test, expect } from '@playwright/test'

test.describe('acceptance: desktop english nav', () => {
  test.use({ viewport: { width: 1280, height: 800 } })

  test('does not show context bar on english pages', async ({ page }) => {
    await page.goto('/english/vocabulary')
    await expect(page.getByTestId('english-context-bar')).toHaveCount(0)
    await expect(page.getByTestId('mobile-public-nav')).toBeHidden()
  })

  test('hides context bar outside english section', async ({ page }) => {
    await page.goto('/tutorials')
    await expect(page.getByTestId('english-context-bar')).toHaveCount(0)
  })
})

test.describe('acceptance: deep page back links', () => {
  test.use({ viewport: { width: 1280, height: 800 } })

  test('reading and writing hubs link back to english root', async ({ page }) => {
    await page.goto('/english/reading')
    await expect(page.getByTestId('english-page-back')).toHaveAttribute('href', '/english')

    await page.goto('/english/writing')
    await expect(page.getByTestId('english-page-back')).toHaveAttribute('href', '/english')

    await page.goto('/english/listening')
    await expect(page.getByTestId('english-page-back')).toHaveAttribute('href', '/english')
  })

  test('pronunciation list is reachable and backs to listening', async ({ page }) => {
    await page.goto('/english/listening/pronunciation')
    await expect(page).toHaveURL(/\/english\/listening\/pronunciation\/?$/)
    await expect(page.getByTestId('english-page-back')).toHaveAttribute('href', '/english/listening')
  })
})

test.describe('acceptance: mobile nav panels', () => {
  test.use({ viewport: { width: 390, height: 844 } })

  test('english retap opens panel; tutorials cross-jump has no panel', async ({ page }) => {
    await page.goto('/english')
    await expect(page.getByTestId('english-context-bar')).toHaveCount(0)
    await page.getByTestId('mobile-public-nav').getByRole('button', { name: /英语/ }).click()
    const englishDialog = page.getByRole('dialog', { name: '英语导航' })
    await expect(englishDialog).toBeVisible()
    await expect(englishDialog.getByRole('link', { name: /进度/ })).toHaveAttribute('href', '/english/progress')
    await expect(englishDialog.getByRole('link', { name: /学习组合/ })).toHaveCount(0)

    await page.keyboard.press('Escape')
    await expect(englishDialog).toHaveCount(0)

    await page.getByTestId('mobile-public-nav').getByRole('button', { name: /教程/ }).click()
    await expect(page).toHaveURL(/\/tutorials\/?$/)
    await expect(page.getByRole('dialog')).toHaveCount(0)
  })

  test('more menu reaches portfolio and about directly', async ({ page }) => {
    await page.goto('/')
    await page.getByTestId('mobile-public-nav').getByRole('button', { name: /更多/ }).click()
    const more = page.getByRole('dialog', { name: '更多导航' })
    await expect(more).toBeVisible()
    await expect(more.getByRole('link', { name: /^作品/ })).toHaveAttribute('href', '/portfolio')
    await expect(more.getByRole('link', { name: /^关于/ })).toHaveAttribute('href', '/about')
  })
})
