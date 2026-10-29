import { test, expect, post } from './fixtures'

const title = '一次点击到底发生了什么？Java 全栈请求从浏览器到 MySQL 的完整链路'
const bodyMarkdown = Array.from({ length: 19 }, (_, index) =>
  `## ${index + 1}. Java 全栈请求链路\n\n${'浏览器、HTTP、Spring Boot 与 MySQL 共同完成一次请求。'.repeat(6)}\n\n` +
  `### ${index + 1}.1 请求的处理过程\n\n正文中的目录与内容应各自显示。\n\n`).join('')

async function installReaders(page) {
  const chapters = [{ id: 'first', slug: 'first', title: 'First lesson' }, { id: 'second', slug: 'second', title: 'Second lesson' }]
  const tutorial = { id: 'long', slug: 'long', title: 'Long tutorial', groups: [{ id: 'basics', title: 'Basics', chapters }] }
  await page.route('**/api/public/tutorials/long**', route => {
    const path = new URL(route.request().url()).pathname
    const data = path.endsWith('/long') ? tutorial : { ...chapters.find(chapter => path.endsWith(chapter.slug)), bodyMarkdown, cards: [], questions: [] }
    return route.fulfill({ json: { code: 'OK', data } })
  })
  await page.route('**/api/public/english/grammar**', route => {
    const path = new URL(route.request().url()).pathname
    const data = path.endsWith('/grammar') ? { course: { title: 'Grammar' }, sections: [{ id: 'basics', title: 'Basics', lessons: chapters }] }
      : { ...chapters.find(chapter => path.endsWith(chapter.slug)), bodyMarkdown }
    return route.fulfill({ json: { code: 'OK', data } })
  })
  await page.route('**/api/public/english/content/**', route => {
    const path = new URL(route.request().url()).pathname
    const data = path.endsWith('/long') ? { slug: 'long', title: 'English article',
      bodyMarkdown: `## English section\n\n${'A long reading paragraph. '.repeat(400)}`,
      translationZhMarkdown: `## 中文章节\n\n${'这是中文正文。'.repeat(400)}`, rights: {} } : []
    return route.fulfill({ json: { code: 'OK', data } })
  })
  await page.route('**/api/public/english/library/world/taxonomy', route => route.fulfill({json:{code:'OK',data:[]}}))
  for (const path of ['world/articles', 'essays']) {
    await page.route(`**/api/public/english/${path}/long`, route => route.fulfill({json:{code:'OK',data:{
      slug:'long',title:'English article',bodyMarkdown:`## English section\n\n${'A long reading paragraph. '.repeat(400)}`,
      translationZhMarkdown:`## 中文章节\n\n${'这是中文正文。'.repeat(400)}`,rights:{},
    }}}))
  }
  await page.route('**/api/account/learning/chapters/first/**', route => {
    const path = new URL(route.request().url()).pathname
    return route.fulfill({ json: { code: 'OK', data: path.endsWith('study-state') ? {
      chapterId: 'first', tutorialSlug: 'long', title: 'First lesson', bodyMarkdown, requiredCardIds: [], initialCardIds: [], questions: [], session: null,
    } : { rowVersion: 0, progressRatio: 0 } } })
  })
}

for (const path of ['/tutorials/long/first', '/learning/chapters/first', '/english/grammar/first', '/english/reading/long', '/english/writing/author/long', '/privacy']) {
  test(`global directory remains available throughout reading: ${path}`, async ({ page }, testInfo) => {
    await installReaders(page)
    await page.setViewportSize({ width: 390, height: 844 })
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await page.goto(path)
    const trigger = page.getByRole('button', { name: /本页目录 ·/ })
    const dialog = page.getByRole('dialog', { name: '本页目录', exact: true })
    await expect(trigger).toHaveCount(1)
    await expect(trigger).toBeInViewport()
    for (const ratio of [0.5, 1]) {
      await page.evaluate(ratio => scrollTo(0, (document.documentElement.scrollHeight - innerHeight) * ratio), ratio)
      await expect(trigger).toBeInViewport()
    }
    await trigger.click()
    await expect(dialog).toBeVisible()
    await expect(dialog.getByRole('link').first()).toBeVisible()
    await page.screenshot({ path: testInfo.outputPath('directory.png') })

    if (path.includes('/grammar/') || path === '/tutorials/long/first') {
      await dialog.getByRole('button', { name: '课程目录', exact: true }).click()
      const second = dialog.getByRole('link', { name: /Second lesson/ })
      const expected = path.includes('grammar') ? '/english/grammar/second' : '/tutorials/long/second'
      await expect(second).toHaveAttribute('href', expected)
      await second.click()
      await expect(page).toHaveURL(expected)
      await expect(dialog).toBeHidden()
      await trigger.click()
      await expect(dialog).toContainText('Second lesson')
      await expect(dialog).not.toContainText('First lesson')
    } else if (path.includes('/english/')) {
      await expect(dialog).toContainText('English section')
      await expect(dialog).not.toContainText('中文章节')
      await page.keyboard.press('Escape')
      await page.locator('.library-translation > summary').click()
      await expect(page.locator('.library-translation')).toContainText('中文章节')
      await trigger.click()
      await expect(dialog).toContainText('English section')
      await expect(dialog).not.toContainText('中文章节')
    }
    await page.keyboard.press('Escape')
    await expect(dialog).toBeHidden()
    expect(await page.evaluate(() => document.body.style.overflow)).not.toBe('hidden')
    // SPA navigation must remove the previous reader's directory entirely.
    await page.getByRole('navigation', { name: '移动端主导航' }).getByRole('button', { name: /首页/ }).click()
    await expect(trigger).toHaveCount(0)
    await expect(page.locator('.reading-drawer')).toHaveCount(0)
  })
}

for (const kind of ['blog', 'portfolio']) {
  test(`${kind} reading keeps the mobile directory separate from long content`, async ({ page }, testInfo) => {
    const url = kind === 'blog' ? '/blog/posts/mobile-layout' : '/portfolio/mobile-layout'
    const api = kind === 'blog' ? '**/api/public/blog/posts/mobile-layout' : '**/api/public/portfolio/works/mobile-layout'
    await page.route(api, route => route.fulfill({ json: { code: 'OK', data: {
      ...post('mobile-layout'), title, bodyMarkdown,
      hasSections: false, tags: [], media: [], links: [],
    } } }))
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await page.setViewportSize({ width: 390, height: 844 })
    await page.goto(url)
    const sidebar = page.locator('.reading-layout__sidebar')
    const trigger = page.getByRole('button', { name: /本页目录/ })
    const dialog = page.getByRole('dialog', { name: '本页目录', exact: true })
    const heading = page.getByRole('heading', { name: title, exact: true })
    await expect(heading).toBeVisible()

    for (const width of [360, 390, 430, 720, 768]) {
      await page.setViewportSize({ width, height: 844 })
      await expect(sidebar).toBeHidden()
      await expect(trigger).toBeVisible()
      await expect(dialog).toBeHidden()
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    }

    await page.setViewportSize({ width: 390, height: 844 })
    await trigger.click()
    await expect(dialog).toBeVisible()
    await expect(trigger).toHaveAttribute('aria-expanded', 'true')
    await expect(dialog.getByRole('link')).toHaveCount(39)
    await expect(dialog.locator('.article-outline__index').first()).toHaveText('01')
    await page.keyboard.press('Escape')
    await expect(dialog).toBeHidden()
    await expect(trigger).toBeFocused()

    const contentHeading = page.locator('.markdown-body h2').nth(2)
    await contentHeading.scrollIntoViewIfNeeded()
    await expect(sidebar).toBeHidden()
    await expect(dialog).toBeHidden()
    await expect(contentHeading).toBeVisible()
    await expect(trigger).toBeInViewport()
    const triggerBox = await trigger.boundingBox()
    const navBox = await page.getByRole('navigation', { name: '移动端主导航' }).boundingBox()
    expect(triggerBox.y + triggerBox.height).toBeLessThanOrEqual(navBox.y)
    const unobstructed = await contentHeading.evaluate(element => {
      const rect = element.getBoundingClientRect()
      return element.contains(document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2))
    })
    expect(unobstructed).toBe(true)
    await page.screenshot({ path: testInfo.outputPath('mobile-reading.png') })
    await trigger.click()
    await dialog.getByRole('link', { name: /19\. Java/ }).click()
    await expect(dialog).toBeHidden()
    await expect(page.locator('.markdown-body h2').last()).toBeInViewport()
    await expect(trigger).toBeInViewport()
    expect(await page.evaluate(() => document.body.style.overflow)).not.toBe('hidden')

    for (const width of [769, 1024, 1440]) {
      await page.setViewportSize({ width, height: 950 })
      await page.evaluate(() => scrollTo(0, 0))
      await expect(sidebar).toBeVisible()
      await expect(trigger).toBeHidden()
      await expect(dialog).toBeHidden()
      const asideBox = await sidebar.boundingBox()
      const bodyBox = await page.locator(kind === 'blog' ? '.article__body' : '.work-detail__body').boundingBox()
      expect(asideBox.x + asideBox.width).toBeLessThanOrEqual(bodyBox.x)
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    }

    // Returning to a phone width after visiting desktop must hide the sticky sidebar again.
    await page.setViewportSize({ width: 390, height: 844 })
    await expect(sidebar).toBeHidden()
    await expect(trigger).toBeVisible()
    await expect(heading).toBeVisible()
  })
}
