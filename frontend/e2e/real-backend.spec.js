/** Real browser → Spring Boot → isolated MySQL, with no API interception. */
import { test, expect, request as requests } from '@playwright/test'
import { readFile } from 'node:fs/promises'
import { resolve, sep } from 'node:path'
import { deflateSync, crc32 } from 'node:zlib'
test.describe.configure({ mode: 'serial' })
const credentials = { identifier: process.env.STAR_RAIN_E2E_USERNAME, password: process.env.STAR_RAIN_E2E_PASSWORD }
async function csrf(page) { return (await (await page.request.get('/api/auth/csrf')).json()).data.token }
async function write(page, method, url, data, version) {
  return page.request.fetch(url, { method, data, headers: { 'X-XSRF-TOKEN': await csrf(page), ...(version == null ? {} : { 'If-Match': `"${version}"` }) } })
}
test.beforeEach(async ({ page }) => {
  expect(credentials.password, 'An isolated test password must be provided').toBeTruthy()
  const login = await write(page, 'POST', '/api/auth/login', credentials)
  expect(login.status()).toBe(200)
  await page.route('https://**/*', async route => {
    const url = new URL(route.request().url()), start = url.pathname.indexOf('/dist/')
    if (!url.pathname.includes('vditor') || start < 0) return route.abort()
    const root = resolve('node_modules/vditor/dist'), file = resolve(root, decodeURIComponent(url.pathname.slice(start + 6)))
    if (!file.startsWith(root + sep)) return route.abort()
    try { await route.fulfill({ body: await readFile(file), contentType: file.endsWith('.js') ? 'application/javascript' : file.endsWith('.css') ? 'text/css' : 'application/octet-stream' }) }
    catch { await route.abort() }
  })
})

test('canonical route refreshes, legacy tokens and public HTTP status retain their boundaries', async ({ page }) => {
  await page.goto('/')
  await expect(page.locator('.public-shell .account-entry')).toBeVisible()
  await expect(page.locator('.admin-shell')).toHaveCount(0)
  for (const path of ['/blog/manage', '/tutorials/manage', '/portfolio/manage']) {
    const response = await page.goto(path)
    expect(response.status()).toBe(200)
    expect(response.headers()['x-robots-tag']).toContain('noindex')
    await expect(page).toHaveURL(new RegExp(path + '$'))
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', 'noindex,nofollow')
  }
  await page.goto('/useradmin/reset-password?token=a%2Bb%3D#reset')
  await expect(page).toHaveURL(/\/reset-password\?token=/)
  expect(new URL(page.url()).searchParams.get('token')).toBe('a+b=')
  expect(new URL(page.url()).hash).toBe('#reset')
  for (const path of ['/blog/posts/missing-route-regression', '/tutorials/missing-route-regression/first', '/portfolio/missing-route-regression']) {
    expect((await page.request.get(path)).status()).toBe(404)
  }
  for (const path of ['/', '/blog', '/tutorials', '/portfolio', '/about', '/messages']) {
    const response = await page.request.get(path)
    expect(response.status()).toBe(200)
    expect(await response.text()).toContain('rel="canonical"')
    expect(await response.text()).not.toContain('href="https://yulanlin.cn/useradmin')
  }
  const sitemap = await (await page.request.get('/sitemap.xml')).text()
  expect(sitemap).not.toContain('/useradmin')
  expect(sitemap).not.toContain('/manage')
  expect((await page.request.get('/assets/missing-route-regression.js')).status()).toBe(404)
})
async function article(page) {
  const slug = `real-audit-${Date.now()}`
  const created = await write(page, 'POST', '/api/admin/blog/posts', { title: 'Real audit article', summary: 'Test summary', slug })
  expect(created.status()).toBe(200)
  const item = (await created.json()).data
  const saved = await write(page, 'PUT', `/api/admin/blog/posts/${item.id}/body`, { bodyMarkdown: '# Real heading\n\nOriginal body' }, item.rowVersion)
  expect(saved.status()).toBe(200)
  return (await saved.json()).data
}
test('two editors reject a stale save and preserve typed text', async ({ page, context }) => {
  const item = await article(page), other = await context.newPage()
  await page.goto(`/blog/editor/${item.id}`); await other.goto(`/blog/editor/${item.id}`)
  await expect(page.getByLabel('文章标题')).toHaveValue(item.title)
  await expect(other.getByLabel('文章标题')).toHaveValue(item.title)
  await page.getByLabel('文章标题').fill('First client saved')
  await page.getByRole('button', { name: '保存文章', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('已保存')
  await other.getByLabel('文章标题').fill('Second client unsaved')
  await other.getByRole('button', { name: '保存文章', exact: true }).click()
  await expect(other.getByRole('alert')).toContainText('其他窗口修改')
  await expect(other.getByLabel('文章标题')).toHaveValue('Second client unsaved')
  const current = (await (await page.request.get(`/api/admin/blog/posts/${item.id}`)).json()).data
  expect(current.title).toBe('First client saved')
  await other.close()
})
test('withdrawal removes HTML, metadata, sitemap and search visibility', async ({ page }) => {
  const item = await article(page)
  expect((await write(page, 'POST', `/api/admin/blog/posts/${item.id}/publish`, undefined, item.rowVersion)).status()).toBe(200)
  const current = (await (await page.request.get(`/api/admin/blog/posts/${item.id}`)).json()).data
  expect((await page.request.get(`/api/public/blog/posts/${item.slug}`)).status()).toBe(200)
  expect((await write(page, 'POST', `/api/admin/blog/posts/${item.id}/withdraw`, undefined, current.rowVersion)).status()).toBe(200)
  expect((await page.request.get(`/api/public/blog/posts/${item.slug}`)).status()).toBe(404)
  const backend = process.env.STAR_RAIN_API_TARGET
  const anonymous = await requests.newContext({ baseURL: backend })
  try {
    expect((await anonymous.get(`/blog/posts/${item.slug}`)).status()).toBe(404)
    expect((await anonymous.get(`/api/public/seo/meta?route=/blog/posts/${item.slug}`)).status()).toBe(404)
    expect(await (await anonymous.get('/sitemap.xml')).text()).not.toContain(`/blog/posts/${item.slug}`)
  } finally { await anonymous.dispose() }
})
test('a later read cannot silently advance the visible editor baseline', async ({ page }) => {
  const item = await article(page)
  await page.goto(`/blog/editor/${item.id}`)
  await expect(page.getByLabel('文章标题')).toHaveValue(item.title)
  let peerSaved = false
  await page.route(`**/api/admin/blog/posts/${item.id}`, async route => {
    if (route.request().method() === 'GET' && !peerSaved) {
      peerSaved = true
      const current = (await (await page.request.get(`/api/admin/blog/posts/${item.id}`)).json()).data
      const saved = await write(page, 'PATCH', `/api/admin/blog/posts/${item.id}`, { title: 'Peer changed after publish', summary: current.summary, tagIds: [] }, current.rowVersion)
      expect(saved.status()).toBe(200)
      expect(saved.headers()['x-content-version']).toBe(String((await saved.json()).data.rowVersion))
    }
    await route.continue()
  })
  await page.getByRole('button', { name: '发布', exact: true }).click()
  await expect(page.getByRole('button', { name: '撤回', exact: true })).toBeVisible()
  expect(peerSaved).toBe(true)
  await page.getByLabel('文章标题').fill('Local unsaved input')
  await page.getByRole('button', { name: '保存文章', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('其他窗口修改')
  await expect(page.getByLabel('文章标题')).toHaveValue('Local unsaved input')
})
function png() {
  const width = 600, height = 400, raw = Buffer.alloc((width * 3 + 1) * height)
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    const index = y * (width * 3 + 1) + x * 3 + 1
    raw[index] = x % 256; raw[index + 1] = y % 256; raw[index + 2] = (x + y) % 256
  }
  const header = Buffer.alloc(13); header.writeUInt32BE(width); header.writeUInt32BE(height, 4); header[8] = 8; header[9] = 2
  const chunk = (type, body) => { const name = Buffer.from(type), size = Buffer.alloc(4), sum = Buffer.alloc(4); size.writeUInt32BE(body.length); sum.writeUInt32BE(crc32(Buffer.concat([name, body]))); return Buffer.concat([size, name, body, sum]) }
  return Buffer.concat([Buffer.from('89504e470d0a1a0a', 'hex'), chunk('IHDR', header), chunk('IDAT', deflateSync(raw, { level: 0 })), chunk('IEND', Buffer.alloc(0))])
}
test('image uploads default to public and authorized image variants remain revocable', async ({ page }) => {
  const bytes = png()
  const uploaded = await page.request.post('/api/admin/media/assets', { headers: { 'X-XSRF-TOKEN': await csrf(page) }, multipart: { file: { name: 'isolated-cover.png', mimeType: 'image/png', buffer: bytes } } })
  expect(uploaded.status()).toBe(200)
  const asset = (await uploaded.json()).data
  expect(asset.accessLevel).toBe('PUBLIC')
  const anonymous = await requests.newContext({ baseURL: process.env.STAR_RAIN_API_TARGET })
  try {
    expect((await anonymous.get(`/api/media/assets/${asset.id}/content`)).status()).toBe(200)
    expect((await anonymous.get(`/api/media/assets/${asset.id}/thumbnail?width=360`)).status()).toBe(200)
    expect((await write(page, 'PATCH', `/api/admin/media/assets/${asset.id}/access-level`, { accessLevel: 'PROTECTED' })).status()).toBe(200)
    expect((await anonymous.get(`/api/media/assets/${asset.id}/content`)).status()).toBe(403)
    expect((await anonymous.get(`/api/media/assets/${asset.id}/thumbnail?width=360`)).status()).toBe(403)
    const publicResult = await write(page, 'PATCH', `/api/admin/media/assets/${asset.id}/access-level`, { accessLevel: 'PUBLIC' })
    expect(publicResult.status()).toBe(200)
    const image = await anonymous.get(`/api/media/assets/${asset.id}/thumbnail?width=360`)
    expect(image.status()).toBe(200); expect((await image.body()).length).toBeLessThan(bytes.length / 2)
    const etag = image.headers().etag
    expect((await anonymous.get(`/api/media/assets/${asset.id}/thumbnail?width=360`, { headers: { 'If-None-Match': etag } })).status()).toBe(304)
    expect((await write(page, 'PATCH', `/api/admin/media/assets/${asset.id}/access-level`, { accessLevel: 'PROTECTED' })).status()).toBe(200)
    expect((await anonymous.get(`/api/media/assets/${asset.id}/thumbnail?width=360`, { headers: { 'If-None-Match': etag } })).status()).toBe(403)
  } finally { await anonymous.dispose() }
})
