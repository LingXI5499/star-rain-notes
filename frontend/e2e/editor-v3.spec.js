import { test, expect } from './fixtures'
const complex =
  '# 第一章\n\n**中文加粗** 与链接 [站点](https://example.com)\n\n$$\nx^2\n$$\n\n::: code-group C++|Java\n\n```cpp\n#include <iostream>\n```\n\n```java\nclass Test {}\n```\n\n:::\n\n| 中文 | 值 |\n| :--- | ---: |\n| 转义\\|字符 | `a\\|b` |\n\n![结构](/api/media/assets/19/content "图 wide")\n\n# 第二章\n\n末尾正文\n'
async function sourcePoint(page, text) {
  const input = page.locator('.vditor-sv[contenteditable="true"]')
  await expect(input).toBeVisible({ timeout: 20000 })
  await input.evaluate((element, text) => {
    element.focus()
    const offset = element.textContent.indexOf(text)
    if (offset < 0) throw new Error('Fixture text missing')
    const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT)
    let node,
      remaining = offset
    while ((node = walker.nextNode())) {
      if (remaining < node.textContent.length) {
        const range = document.createRange()
        range.setStart(node, remaining)
        range.collapse(true)
        const selection = window.getSelection()
        selection.removeAllRanges()
        selection.addRange(range)
        return
      }
      remaining -= node.textContent.length
    }
    throw new Error('Fixture offset missing')
  }, text)
  return input
}
async function open(page) {
  await page.goto('/useradmin/blog/editor/1')
  const input = page.locator(
    '.vditor-ir [contenteditable="true"]:visible,.vditor-sv[contenteditable="true"]:visible',
  )
  await expect(input).toBeVisible({ timeout: 20000 })
  await expect(page.getByRole('button', { name: 'H2', exact: true })).toBeEnabled()
  return input
}
test('Alt headings replace levels, paragraph conversion and undo preserve content', async ({
  page,
  fixture,
}) => {
  const input = await open(page)
  await input.locator('p').filter({ hasText: 'Original body' }).click()
  for (const level of [1, 2, 3, 4, 5, 6]) {
    await page.keyboard.press(`Alt+${level}`)
    await expect(input.locator(`h${level}`).filter({ hasText: 'Original body' })).toBeVisible()
    await page.waitForTimeout(350)
  }
  await page.keyboard.press('Alt+0')
  await expect(input.locator('p').filter({ hasText: 'Original body' })).toBeVisible()
  await page.waitForTimeout(350)
  await page.keyboard.press('Control+z')
  await expect(input.locator('h6').filter({ hasText: 'Original body' })).toBeVisible()
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  await expect.poll(() => fixture.savedBodies.length).toBe(1)
  expect(fixture.savedBodies[0]).toContain('###### Original body')
})
test('slash commands consume only the query and cancel leaves raw text', async ({ page }) => {
  const input = await open(page)
  await input.click()
  await page.keyboard.press('Control+End')
  await page.keyboard.press('Enter')
  await page.keyboard.type('/h2')
  await expect(page.getByRole('listbox', { name: '段落命令' })).toBeVisible()
  await page.keyboard.press('Enter')
  await expect(input.locator('h2')).toContainText('标题')
  await expect(input).not.toContainText('/h2')
  await page.keyboard.press('Control+End')
  await page.keyboard.press('Enter')
  await page.keyboard.type('/no-command')
  await page.keyboard.press('Escape')
  await expect(page.getByRole('listbox')).toHaveCount(0)
  await expect(input).toContainText('/no-command')
})
test('input fields and composing input never receive heading commands', async ({ page }) => {
  const input = await open(page)
  await page.getByLabel('文章标题').click()
  await page.keyboard.press('Alt+2')
  await expect(input.locator('h2')).toHaveCount(0)
  await input.locator('p').filter({ hasText: 'Original body' }).click()
  await input.evaluate((element) => {
    element.dispatchEvent(new CompositionEvent('compositionstart', { bubbles: true }))
    element.dispatchEvent(
      new KeyboardEvent('keydown', {
        key: '2',
        code: 'Digit2',
        altKey: true,
        isComposing: true,
        bubbles: true,
      }),
    )
    element.dispatchEvent(new CompositionEvent('compositionend', { bubbles: true }))
  })
  await expect(input.locator('h2')).toHaveCount(0)
})
test('floating toolbar restores selection and stays closed after Escape', async ({
  page,
  fixture,
}) => {
  const input = await open(page)
  await input
    .locator('p')
    .filter({ hasText: 'Original body' })
    .evaluate((element) => {
      element.closest('[contenteditable]').focus()
      const range = document.createRange()
      range.selectNodeContents(element)
      const selection = window.getSelection()
      selection.removeAllRanges()
      selection.addRange(range)
    })
  const toolbar = page.getByRole('toolbar', { name: '选区格式' })
  await expect(toolbar).toBeVisible()
  await toolbar.getByRole('button', { name: '加粗', exact: true }).click()
  await expect(input.locator('[data-type="strong"]')).toContainText('Original body')
  await page.keyboard.press('Escape')
  await expect(toolbar).toHaveCount(0)
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  expect(fixture.savedBodies.at(-1)).toContain('**Original body**')
})
test('unsafe native conversion rolls back complex content and real preview does not save or run HTML', async ({
  page,
  fixture,
}) => {
  fixture.article.bodyMarkdown = complex + '\n<script>window.editorXss = 1</script>\n'
  await open(page)
  await page.getByLabel('编辑视图').selectOption('sv')
  await page.getByLabel('编辑视图').selectOption('ir')
  await expect(page.getByLabel('编辑视图')).toHaveValue('sv')
  await expect(page.getByRole('status').filter({ hasText: '已恢复原文' })).toBeVisible()
  await page.getByRole('button', { name: '阅读预览', exact: true }).click()
  const preview = page.getByRole('dialog', { name: '阅读预览' })
  await expect(preview.getByRole('heading', { name: '第一章', exact: true })).toBeVisible()
  await expect(preview.locator('[data-code-group]')).toHaveCount(1)
  await expect(preview.locator('img')).toHaveClass(/markdown-img--wide/)
  expect(await page.evaluate(() => window.editorXss)).toBeUndefined()
  expect(fixture.savedBodies).toEqual([])
  await preview.getByRole('button', { name: '关闭预览' }).click()
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  expect(fixture.savedBodies.at(-1)).toContain('#include <iostream>')
  expect(fixture.savedBodies.at(-1)).toContain('::: code-group')
  expect(fixture.savedBodies.at(-1)).toContain('`a\\|b`')
})
test('all three native modes retain ordinary inline formatting', async ({ page, fixture }) => {
  fixture.article.bodyMarkdown = '# 标题\n\n**中文加粗** 与普通正文\n'
  await open(page)
  for (const mode of ['sv', 'wysiwyg', 'ir']) {
    await page.getByLabel('编辑视图').selectOption(mode)
    await expect(page.getByLabel('编辑视图')).toHaveValue(mode)
  }
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  expect(fixture.savedBodies.at(-1)).toContain('**中文加粗**')
})
test('source headings and protected code are scoped and undoable', async ({ page, fixture }) => {
  fixture.article.bodyMarkdown = complex
  await open(page)
  await page.getByLabel('编辑视图').selectOption('sv')
  const input = await sourcePoint(page, '末尾正文')
  await page.keyboard.press('Alt+3')
  await expect(input).toContainText('### 末尾正文')
  await sourcePoint(page, '#include')
  await page.keyboard.press('Alt+2')
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  expect(fixture.savedBodies.at(-1)).toContain('#include <iostream>')
  expect(fixture.savedBodies.at(-1)).not.toContain('## #include')
})
test('source table editing preserves pipes and image property changes keep the media URL', async ({
  page,
  fixture,
}) => {
  fixture.article.bodyMarkdown = complex
  await open(page)
  await page.getByLabel('编辑视图').selectOption('sv')
  await sourcePoint(page, '转义')
  await page.getByRole('button', { name: '编辑表格', exact: true }).click()
  await page.getByRole('button', { name: '下插行', exact: true }).click()
  await sourcePoint(page, '![结构]')
  await page.getByRole('button', { name: '图片属性', exact: true }).click()
  await page.getByLabel('替代文本').fill('更新的结构图')
  await page.getByLabel('图片尺寸').selectOption('full')
  await page.getByRole('button', { name: '应用图片属性', exact: true }).click()
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  const saved = fixture.savedBodies.at(-1)
  expect(saved).toContain('转义\\|字符')
  expect(saved).toContain('`a\\|b`')
  expect(saved).toContain('/api/media/assets/19/content')
  expect(saved).toContain('full')
  expect(saved).toContain('更新的结构图')
})
test('browser recovery survives reload and never saves until the user confirms server saving', async ({
  page,
  fixture,
}) => {
  const input = await open(page)
  await input.click()
  await page.keyboard.press('Control+End')
  await page.keyboard.type('Local offline text')
  await expect(page.getByRole('status').filter({ hasText: '已备份到当前浏览器' })).toBeVisible()
  await page.reload()
  await expect(page.getByRole('dialog', { name: '本地草稿对比' })).toBeVisible()
  expect(fixture.savedBodies).toHaveLength(0)
  await page.getByRole('button', { name: '载入此恢复点到编辑器' }).click()
  await expect(page.locator('.vditor-sv[contenteditable=true]')).toContainText('Local offline text')
  expect(fixture.savedBodies).toHaveLength(0)
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  expect(fixture.savedBodies.at(-1)).toContain('Local offline text')
})
test('HTTP 409 retains the local backup for a deliberate recovery after reload', async ({
  page,
  fixture,
}) => {
  const input = await open(page)
  await input.click()
  await page.keyboard.press('Control+End')
  await page.keyboard.type('Conflict draft')
  fixture.bodyFailure = true
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('CONTENT_VERSION_CONFLICT')
  await expect(page.getByRole('status').filter({ hasText: '已备份到当前浏览器' })).toBeVisible()
  await page.reload()
  await expect(page.getByRole('dialog', { name: '本地草稿对比' })).toBeVisible()
  await expect(page.getByRole('textbox', { name: '本地恢复原文' })).toHaveValue(/Conflict draft/)
  expect(fixture.savedBodies).toHaveLength(0)
})
for (const size of [5000, 50000, 100000, 200000])
  test(`structured document ${size} characters keeps input and worker diagnostics responsive`, async ({
    page,
    fixture,
  }, testInfo) => {
    test.setTimeout(90000)
    const part =
      '# 章节\n\n' +
      '这是结构化长文档，保留段落和普通标点。'.repeat(15) +
      '\n\n```java\nclass Example {}\n```\n\n'
    const body = part.repeat(Math.floor(size / part.length))
    fixture.article.bodyMarkdown = body + '补'.repeat(size - body.length) + '\n\n末段\n'
    await open(page)
    await page.getByLabel('编辑视图').selectOption('sv')
    const input = await sourcePoint(page, '末段')
    await page.evaluate(() => {
      window.editorInputStart = performance.now()
    })
    await page.keyboard.press('x')
    const elapsed = await page.evaluate(
      () =>
        new Promise((resolve) =>
          requestAnimationFrame(() => resolve(performance.now() - window.editorInputStart)),
        ),
    )
    expect(
      await input.evaluate((element) =>
        element.textContent.replace(/\u200b/g, '').includes('x末段'),
      ),
    ).toBe(true)
    await page.getByRole('button', { name: '质量检查', exact: true }).click()
    await expect(page.getByRole('status').filter({ hasText: '条建议' })).toBeVisible({
      timeout: 15000,
    })
    const memory = await page.evaluate(() => performance.memory?.usedJSHeapSize ?? null)
    const evidence = {
      characters: size,
      inputToFrameMs: Number(elapsed.toFixed(1)),
      heapBytes: memory,
    }
    console.log('EDITOR_PERF', JSON.stringify(evidence))
    await testInfo.attach('editor-performance.json', {
      body: JSON.stringify(evidence),
      contentType: 'application/json',
    })
    expect(elapsed).toBeLessThan(5000)
  })
