import { test, expect } from './fixtures'
import { portfolioCases } from './portfolio.fixtures'
import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const clone = value => JSON.parse(JSON.stringify(value))
async function installPortfolio(page) {
 const state = { works: clone(portfolioCases), creates: 0, failBody: false }
 await page.route('**/api/**portfolio/**', async route => {
  const request = route.request(), url = new URL(request.url()), path = url.pathname.replace('/api', ''), method = request.method()
  const ok = (data, version) => route.fulfill({ json: { code: 'OK', data }, headers: version == null ? {} : { 'x-content-version': String(version) } })
  if (path === '/public/portfolio/works') {
   const featured = url.searchParams.get('featured') === 'true'
   const items = featured ? state.works.filter(work => work.featured).slice(0, 3) : state.works
   return ok({ items, total: items.length })
  }
  if (path.startsWith('/public/portfolio/works/')) return ok(state.works.find(work => work.slug === path.split('/').at(-1)))
  if (path === '/admin/portfolio/works' && method === 'POST') {
   state.creates++
   const work = { ...clone(portfolioCases[0]), ...request.postDataJSON(), id: String(100 + state.creates), slug: `created-${state.creates}`,
    rowVersion: 0, bodyMarkdown: '', featured: false, links: [], media: [] }
   state.works.push(work); return ok(work)
  }
  const match = path.match(/^\/admin\/portfolio\/works\/(\d+)(?:\/(body|links|publish|withdraw|restore|media))?$/)
  if (!match) return route.fallback()
  const work = state.works.find(work => work.id === match[1]), action = match[2]
  if (method === 'GET') return ok(work)
  if (request.headers()['if-match'] !== `"${work.rowVersion}"`) return route.fulfill({ status: 409, json: { code: 'CONTENT_VERSION_CONFLICT', message: '作品版本冲突' } })
  if (action === 'body' && state.failBody) {
   state.failBody = false
   return route.fulfill({ status: 503, json: { code: 'TEMPORARY_FAILURE', message: '测试正文保存失败' } })
  }
  if (!action) Object.assign(work, request.postDataJSON())
  if (action === 'body') work.bodyMarkdown = request.postDataJSON().bodyMarkdown
  if (action === 'links') work.links.push({ ...request.postDataJSON(), id: String(500 + work.links.length) })
  if (action === 'media') work.media.push({ ...request.postDataJSON(), id: '800', url: '/portfolio-test-assets/asterway-reading.png' })
  if (action === 'publish' || action === 'restore') work.status = 'PUBLISHED'
  if (action === 'withdraw') work.status = 'WITHDRAWN'
  work.rowVersion++
  return ok(work, work.rowVersion)
 })
 if (process.env.STAR_RAIN_PORTFOLIO_SOURCE) await page.route('**/portfolio-test-assets/asterway-reading.png', route =>
  readFile(resolve(process.env.STAR_RAIN_PORTFOLIO_SOURCE, 'asterway/public/media/reading-demo.png')).then(body => route.fulfill({ body, contentType: 'image/png' })))
 return state
}
async function setMarkdown(page, markdown) {
 const input = page.locator('.vditor-ir [contenteditable="true"]')
 await expect(input).toBeVisible()
 await input.click(); await page.keyboard.press('Control+End'); await page.keyboard.insertText(markdown)
 return input
}

test('new work keeps the created draft after a failed save and uses sequential versions on retry and publish', async ({ page }) => {
 const state = await installPortfolio(page); state.failBody = true
 await page.goto('/useradmin/portfolio/editor/new')
 await page.getByLabel('作品标题', { exact: true }).fill('真实编辑器重试验收')
 await page.getByLabel('简介', { exact: true }).fill('验证未保存输入、版本和重复创建保护。')
 await setMarkdown(page, '# 创作过程\n\n重试后正文仍然存在。')
 await page.getByLabel('链接名称', { exact: true }).fill('源码')
 await page.getByLabel('链接地址', { exact: true }).fill('https://github.com/LingXI5499/ai-coding-portfolio')
 await page.getByRole('button', { name: '添加链接', exact: true }).click()
 await page.getByRole('button', { name: '保存作品', exact: true }).first().click()
 await expect(page.getByRole('alert')).toContainText('Markdown 正文保存失败')
 await expect(page).toHaveURL(/\/editor\/101$/)
 expect(state.creates).toBe(1)
 await page.locator('.work-editor__more>summary').click()
 await page.getByLabel('作品分类', { exact: true }).selectOption('1')
 await page.getByLabel('作品形态', { exact: true }).selectOption('2')
 await page.getByRole('button', { name: '保存并发布', exact: true }).click()
 await expect(page.locator('.work-editor__public a')).toHaveAttribute('href', '/portfolio/created-1')
 expect(state.creates).toBe(1)
 expect(state.works.at(-1).links).toHaveLength(1)
 expect(state.works.at(-1).bodyMarkdown).toContain('重试后正文仍然存在')
 await page.reload()
 await expect(page.getByLabel('作品标题', { exact: true })).toHaveValue('真实编辑器重试验收')
 await expect(page.locator('.vditor-ir')).toContainText('重试后正文仍然存在')
 await page.getByRole('button', { name: '撤回发布', exact: true }).click()
 await expect(page.getByRole('button', { name: '恢复发布', exact: true })).toBeVisible()
 await page.getByRole('button', { name: '恢复发布', exact: true }).click()
 await expect(page.locator('.work-editor__public a')).toBeVisible()
})

for (const sample of portfolioCases) test(`manually authored real project content: ${sample.slug}`, async ({ page }) => {
 const state = await installPortfolio(page)
 const work = state.works.find(work => work.slug === sample.slug)
 if (sample.slug === 'asterway') {
  work.coverUrl = '/portfolio-test-assets/asterway-reading.png'
  work.media = [{ id: 'screenshot', usageType: 'SCREENSHOT', url: work.coverUrl }]
 }
 work.bodyMarkdown = ''
 await page.goto(`/useradmin/portfolio/editor/${work.id}`)
 await expect(page.getByLabel('作品标题', { exact: true })).toHaveValue(sample.title)
 await setMarkdown(page, sample.bodyMarkdown)
 await page.getByRole('button', { name: '保存作品', exact: true }).first().click()
 await expect(page.getByRole('status').filter({ hasText: '作品已保存。' })).toBeVisible()
 expect(work.bodyMarkdown).toContain(sample.bodyMarkdown.slice(0, 10))
 await page.reload()
 await expect(page.locator('.vditor-ir')).toContainText(sample.bodyMarkdown.split('\n')[0].replace(/^#+ /, ''))
 await page.getByRole('button', { name: '保存并发布', exact: true }).click()
 await expect(page.locator('.work-editor__public a')).toBeVisible()
 expect(work.status).toBe('PUBLISHED')
 await page.goto(`/portfolio/${sample.slug}`)
 await expect(page.getByRole('heading', { name: sample.title, exact: true })).toBeVisible()
 await expect(page.locator('.work-detail__links a')).toHaveAttribute('href', sample.links[0].url)
 await expect(page.locator('.work-detail__tags')).toContainText(sample.techStack.split(', ')[0])
 await expect(page.locator('.markdown-body')).toContainText(sample.bodyMarkdown.split('\n')[0].replace(/^#+ /, ''))
 if (sample.slug === 'java-web-lab') await expect(page.locator('.markdown-body pre')).toContainText('SELECT *')
 if (sample.slug === 'asterway' && process.env.STAR_RAIN_PORTFOLIO_SOURCE) {
  const image = page.locator('.markdown-body img').first()
  await expect.poll(() => image.evaluate(image => image.complete && image.naturalWidth > 0)).toBe(true)
  await expect(page.locator('img[src="/portfolio-test-assets/asterway-reading.png"]')).toHaveCount(1)
 }
})

for (const theme of ['light', 'dark']) for (const width of [1440, 390]) test(`gallery, reading and editor layout: ${theme} ${width}`, async ({ page }, testInfo) => {
 await installPortfolio(page)
 await page.addInitScript(theme => { localStorage.setItem('star-rain-theme', theme); localStorage.setItem('admin-theme', theme) }, theme)
 await page.setViewportSize({ width, height: 950 })
 await page.emulateMedia({ reducedMotion: 'reduce' })
 await page.goto('/portfolio')
 await expect(page.locator('html')).toHaveAttribute('data-theme', theme)
 await expect(page.locator('.works-featured .works-card')).toHaveCount(3)
 await expect(page.locator('.works-archive .works-card')).toHaveCount(8)
 const featured = page.locator('.works-featured .works-card').nth(1)
 const cover = await featured.locator('.works-card__cover').boundingBox(), body = await featured.locator('.works-card__body').boundingBox()
 expect(width < 700 ? cover.y < body.y : cover.x > body.x).toBe(true)
 expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
 await page.screenshot({ path: testInfo.outputPath('gallery.png'), fullPage: true })
 await page.goto('/portfolio/java-web-lab')
 await expect(page.locator('html')).toHaveAttribute('data-theme', theme)
 await expect(page.locator('.markdown-body pre')).toBeVisible()
 expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
 await page.screenshot({ path: testInfo.outputPath('detail.png'), fullPage: true })
 await page.goto('/useradmin/portfolio/editor/new')
 await expect(page.locator('html')).toHaveAttribute('data-theme', theme)
 await expect(page.locator('.vditor-ir [contenteditable="true"]')).toBeVisible()
 expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
 await page.screenshot({ path: testInfo.outputPath('editor.png'), fullPage: true })
})
