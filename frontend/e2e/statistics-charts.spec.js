import { test, expect } from './fixtures'

async function statisticsFixture(page, fixture, { analyticsPermission = true } = {}) {
  fixture.user = { ...fixture.user, permissions: [...fixture.user.permissions.filter(code => code !== 'analytics:read' && code !== 'site:dashboard-read'), 'site:dashboard-read', ...(analyticsPermission ? ['analytics:read'] : [])] }
  const state = { requests: [], failHot: false, empty: false }
  await page.route('**/api/admin/site/dashboard', route => route.fulfill({ json: { code: 'OK', data: {
    modules: { TUTORIAL: { metrics: { total: 12, chapters: 96, drafts: 2 } }, BLOG: { metrics: { total: 38, drafts: 4 } }, PORTFOLIO: { metrics: { total: 8 } }, ACCOUNT: { metrics: { total: 124 } }, ANALYTICS: { metrics: { totalPageViews: 19824 } } },
    degradedModules: [], recentContent: [{ id: 1, type: 'BLOG', title: 'Spring 开发笔记', status: 'DRAFT', updatedAt: '2026-10-29T08:00:00Z' }],
  } } }))
  await page.route('**/api/admin/analytics/**', async route => {
    const url = new URL(route.request().url()), kind = url.pathname.split('/').at(-1)
    state.requests.push({ kind, start: url.searchParams.get('startDate'), end: url.searchParams.get('endDate') })
    if (kind === 'hot-content' && state.failHot) return route.fulfill({ status: 503, json: { code: 'UNAVAILABLE', message: 'Unavailable' } })
    let data
    if (kind === 'summary') data = { totalPageViews: 19824, totalContentViews: 12735, todayPageViews: 156, todayContentViews: 93 }
    if (kind === 'trend') {
      data = []; let index = 0
      for (let t = Date.parse(url.searchParams.get('startDate')); t <= Date.parse(url.searchParams.get('endDate')); t += 86400000) {
        if (index++ % 6 !== 0 && !state.empty) data.push({ statDate: new Date(t).toISOString().slice(0, 10), pageViews: 80 + (index * 37) % 140, contentViews: 35 + (index * 23) % 90 })
      }
    }
    if (kind === 'hot-content') data = state.empty ? [] : [ { contentType: 'BLOG', contentId: 1, viewCount: 384 }, { contentType: 'CHAPTER', contentId: 12, viewCount: 276 }, { contentType: 'TUTORIAL', contentId: 3, viewCount: 198 }, { contentType: 'PORTFOLIO', contentId: 8, viewCount: 96 } ]
    if (kind === 'referrers') data = state.empty ? [] : [ { category: 'DIRECT', viewCount: 640 }, { category: 'SEARCH', viewCount: 420 }, { category: 'INTERNAL', viewCount: 310 }, { category: 'SOCIAL', viewCount: 156 }, { category: 'OTHER', viewCount: 84 } ]
    await route.fulfill({ json: { code: 'OK', data } })
  })
  return state
}

test('dashboard and analytics render interactive charts, adapt theme and resize on mobile', async ({ page, fixture }) => {
  const state = await statisticsFixture(page, fixture)
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.setViewportSize({ width: 1440, height: 1100 })
  await page.goto('/dashboard')
  await expect(page.locator('.statistics-canvas svg')).toHaveCount(2)
  const trend = page.getByRole('region', { name: '访问趋势' })
  await trend.getByRole('button', { name: '近 30 天' }).click()
  await expect.poll(() => state.requests.filter(item => item.kind === 'trend').length).toBe(2)
  await expect(trend.locator('svg')).toHaveCount(1)
  await page.screenshot({ path: '../.local/dashboard-charts-desktop.png', fullPage: true })
  await page.getByRole('link', { name: '详细统计' }).click()
  await expect(page).toHaveURL(/\/analytics$/)
  await expect(page.locator('.statistics-canvas svg')).toHaveCount(3)
  await expect(page.getByRole('region', { name: '访问概览' })).toContainText('19,824')
  await page.getByText('查看每日明细', { exact: true }).click()
  await expect(page.getByRole('table').first().locator('tbody tr')).toHaveCount(30)
  await page.getByText('查看每日明细', { exact: true }).click()
  await page.getByRole('button', { name: '近 7 天' }).click()
  await expect(page.getByRole('button', { name: '近 7 天' })).toHaveAttribute('aria-pressed', 'true')
  await expect(page.locator('.statistics-canvas svg')).toHaveCount(3)
  const plot = page.getByRole('img', { name: '访问趋势', exact: true })
  const rect = await plot.boundingBox()
  await page.mouse.move(rect.x + rect.width / 2, rect.y + rect.height / 2)
  await page.screenshot({ path: '../.local/analytics-charts-desktop.png', fullPage: true })
  const before = await plot.locator('svg text').first().getAttribute('fill')
  await page.getByRole('button', { name: '深色主题', exact: true }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await expect(page.locator('html')).not.toHaveClass(/is-theme-revealing|is-theme-fading/)
  await expect.poll(() => plot.locator('svg text').first().getAttribute('fill')).not.toBe(before)
  await page.screenshot({ path: '../.local/analytics-charts-dark.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await expect.poll(async () => {
    const svg = await plot.locator('svg').boundingBox(), panel = await plot.boundingBox()
    return svg.width <= panel.width + 1
  }).toBe(true)
  expect(await page.locator('.analytics-page').evaluate(el => el.scrollWidth <= el.clientWidth + 1)).toBe(true)
  await page.screenshot({ path: '../.local/analytics-charts-mobile.png', fullPage: true })
})

test('failed panel retries independently, validates dates and displays empty results', async ({ page, fixture }) => {
  const state = await statisticsFixture(page, fixture); state.failHot = true
  await page.goto('/analytics')
  const hot = page.getByRole('region', { name: '热门内容 TOP 10' })
  await expect(hot.getByRole('alert')).toBeVisible()
  await expect(page.locator('.statistics-canvas svg')).toHaveCount(2)
  const trendCalls = state.requests.filter(item => item.kind === 'trend').length
  state.failHot = false
  await hot.getByRole('button', { name: '重试', exact: true }).click()
  await expect(hot.locator('svg')).toHaveCount(1)
  expect(state.requests.filter(item => item.kind === 'trend')).toHaveLength(trendCalls)
  await page.getByLabel('开始日期').fill('2026-10-10')
  await page.getByLabel('结束日期').fill('2026-10-01')
  const before = state.requests.length
  await page.getByRole('button', { name: '查询', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('开始日期不能晚于结束日期')
  expect(state.requests).toHaveLength(before)
  state.empty = true
  await page.getByLabel('开始日期').fill('2026-10-01')
  await page.getByLabel('结束日期').fill('2026-10-03')
  await page.getByRole('button', { name: '查询', exact: true }).click()
  await expect(page.getByText('所选时段暂无访问记录。可尝试调整日期范围。')).toBeVisible()
  await expect(page.getByText('所选时段暂无来源数据。')).toBeVisible()
  await expect(page.getByText('所选时段暂无内容访问。')).toBeVisible()
  await expect(page.locator('.statistics-canvas svg')).toHaveCount(0)
  await expect(page.getByRole('region', { name: '访问概览' })).toContainText('19,824')
  await page.screenshot({ path: '../.local/analytics-charts-empty.png', fullPage: true })
})

test('dashboard does not request analytics without the existing permission', async ({ page, fixture }) => {
  const state = await statisticsFixture(page, fixture, { analyticsPermission: false })
  await page.goto('/dashboard')
  await expect(page.getByRole('heading', { name: '内容数量', exact: true })).toBeVisible()
  await expect(page.locator('.statistics-canvas svg')).toHaveCount(1)
  await expect(page.getByRole('region', { name: '访问趋势' })).toHaveCount(0)
  expect(state.requests).toHaveLength(0)
})
