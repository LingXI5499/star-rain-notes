import { test, expect } from './fixtures'
async function menu(page, group, name, scope = page, radio = false) {
  await scope.getByRole('menubar', {name:'文档菜单'}).getByRole('menuitem', {name:group,exact:true}).click()
  await page.getByRole('menu', {name:`${group}菜单`}).getByRole(radio?'menuitemradio':'menuitem', {name:new RegExp(`^${name}`)}).click()
}
async function mode(page, value) {
  await menu(page, '视图', {ir:'即时渲染',sv:'源码',wysiwyg:'所见即所得'}[value], page, true)
}
async function expectMode(page, value) {
  await expect(page.locator(`.vditor-toolbar [data-mode="${value}"].vditor-menu--current`)).toHaveCount(1)
}
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
  await expect(page.getByRole('menubar').getByRole('menuitem', { name: '段落', exact: true })).toBeEnabled()
  return input
}
test('compact menus preserve selection and fullscreen keeps the same editor at the viewport', async ({page,fixture}) => {
  const input = await open(page)
  await expect(page.getByRole('menubar').getByRole('menuitem')).toHaveCount(7)
  await expect(page.locator('.vditor-toolbar')).toBeHidden()
  await expect(page.locator('.markdown-editor__image-actions')).toHaveCount(0)
  await input.locator('p').filter({hasText:'Original body'}).click()
  await menu(page,'段落','标题 2')
  await expect(input.locator('h2')).toContainText('Original body')
  const bar = page.getByRole('menubar')
  await bar.getByRole('menuitem',{name:'文件',exact:true}).focus()
  await page.keyboard.press('ArrowDown')
  await expect(page.getByRole('menuitem',{name:'导入 Markdown…'})).toBeFocused()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('menu')).toHaveCount(0)
  await expect(bar.getByRole('menuitem',{name:'文件',exact:true})).toBeFocused()
  await input.locator('h2').click()
  await input.evaluate(element=>{element.dataset.fullscreenIdentity='same-editor';element.closest('.blog-edit__writing-card').style.transform='translateZ(0)'})
  for (let cycle=0; cycle<3; cycle++) {
    await bar.getByRole('button',{name:'全屏',exact:true}).evaluate(button=>button.addEventListener('click',()=>{window.fullscreenEntryScroll=document.querySelector('.admin-shell__content').scrollTop},{capture:true,once:true}))
    await bar.getByRole('button',{name:'全屏',exact:true}).click()
    const scroll = await page.evaluate(()=>window.fullscreenEntryScroll)
    const full = page.getByRole('dialog',{name:'全屏文档编辑器'})
    await expect(full).toBeVisible()
    expect(await full.evaluate(element=>element.parentElement===document.body)).toBe(true)
    expect(await page.locator('#app').evaluate(element=>element.inert)).toBe(true)
    // Sample consecutive rendered frames to detect jumps or a replaced/hidden document tree.
    const frames=await full.evaluate(async element=>{
      const frames=[]
      for(let frame=0;frame<16;frame++){
        await new Promise(requestAnimationFrame)
        const rect=element.getBoundingClientRect(),editable=element.querySelector('[data-fullscreen-identity]')
        frames.push({x:rect.x,y:rect.y,width:rect.width,height:rect.height,viewport:innerHeight,connected:editable?.isConnected,editorHeight:editable?.getBoundingClientRect().height})
      }
      return frames
    })
    for(const frame of frames){expect(frame.x).toBe(0);expect(frame.y).toBe(0);expect(frame.height).toBe(frame.viewport);expect(frame.connected).toBe(true);expect(frame.editorHeight).toBeGreaterThan(300)}
    await expect(full.locator('[data-fullscreen-identity]')).toContainText('Original body')
    if(cycle===0){
      await menu(page,'格式','上传图片')
      // Cancelling the browser chooser leaves the editor intact; media actions are no longer persistent rows.
      await menu(page,'格式','从媒体库选择图片')
      const picker=page.getByRole('dialog',{name:'选择图片'})
      await expect(picker).toBeVisible()
      await expect(picker.getByRole('button',{name:'关闭媒体选择器'})).toBeEnabled()
      await picker.getByRole('button',{name:'关闭媒体选择器'}).click()
      await expect(picker).toBeHidden()
      await expect(full).toBeVisible()
      await menu(page,'主题','深色主题',page,true)
      await expect(page.locator('html')).toHaveAttribute('data-theme','dark')
      await page.screenshot({path:'../.local/editor-menu-fullscreen-desktop.png'})
      await menu(page,'主题','浅色主题',page,true)
    }
    await full.locator('[data-fullscreen-identity]').click()
    await page.keyboard.press('Escape')
    await expect(full).toHaveCount(0)
    expect(await page.locator('#app').evaluate(element=>element.inert)).toBe(false)
    expect(await page.locator('.admin-shell__content').evaluate(element=>element.scrollTop)).toBe(scroll)
    await expect(page.locator('[data-fullscreen-identity]')).toBeVisible()
  }
  await page.getByRole('button',{name:'保存正文',exact:true}).click()
  await expect.poll(()=>fixture.savedBodies.length).toBe(1)
  expect(fixture.savedBodies[0]).toContain('## Original body')
  await page.locator('.admin-shell__content').evaluate(element=>element.scrollTop=0)
  await page.screenshot({path:'../.local/editor-menu-desktop.png'})
})
test('fullscreen resizes on mobile and closes menus before exiting', async ({page}) => {
  await page.setViewportSize({width:390,height:700})
  const input=await open(page)
  await input.locator('p').filter({hasText:'Original body'}).click()
  await page.getByRole('menubar').getByRole('button',{name:'全屏',exact:true}).click()
  const full=page.getByRole('dialog',{name:'全屏文档编辑器'})
  await page.setViewportSize({width:320,height:560})
  await expect(full).toHaveCSS('height','560px')
  await full.getByRole('menuitem',{name:'格式',exact:true}).click()
  const popup=page.getByRole('menu',{name:'格式菜单'})
  const rect=await popup.boundingBox()
  expect(rect.x).toBeGreaterThanOrEqual(0);expect(rect.x+rect.width).toBeLessThanOrEqual(320)
  expect(rect.y+rect.height).toBeLessThanOrEqual(560)
  await popup.getByRole('menuitem',{name:'加粗',exact:true}).focus()
  await page.keyboard.press('Escape')
  await expect(popup).toHaveCount(0);await expect(full).toBeVisible()
  await page.screenshot({path:'../.local/editor-menu-fullscreen-mobile.png'})
  await page.keyboard.press('Escape')
  await expect(full).toHaveCount(0)
  await input.locator('p').filter({hasText:'Original body'}).click()
  await page.keyboard.press('Control+`')
  await expect(full).toBeVisible()
  await page.goto('/useradmin/blog')
  expect(await page.locator('#app').evaluate(element=>element.inert)).toBe(false)
  await expect(page.locator('html')).not.toHaveClass(/is-vditor-fullscreen/)
})
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
  await expect.poll(() => fixture.savedBodies.length).toBe(1)
  expect(fixture.savedBodies.at(-1)).toContain('**Original body**')
})
test('unsafe native conversion rolls back complex content and real preview does not save or run HTML', async ({
  page,
  fixture,
}) => {
  fixture.article.bodyMarkdown = complex + '\n<script>window.editorXss = 1</script>\n'
  await open(page)
  await mode(page, 'sv')
  await mode(page, 'ir')
  await expectMode(page, 'sv')
  await expect(page.getByRole('status').filter({ hasText: '已恢复原文' })).toBeVisible()
  await menu(page, '视图', '阅读预览')
  const preview = page.getByRole('dialog', { name: '阅读预览' })
  await expect(preview.getByRole('heading', { name: '第一章', exact: true })).toBeVisible()
  await expect(preview.locator('[data-code-group]')).toHaveCount(1)
  await expect(preview.locator('img')).toHaveClass(/markdown-img--wide/)
  expect(await page.evaluate(() => window.editorXss)).toBeUndefined()
  expect(fixture.savedBodies).toEqual([])
  await preview.getByRole('button', { name: '关闭预览' }).click()
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  await expect.poll(() => fixture.savedBodies.length).toBe(1)
  expect(fixture.savedBodies.at(-1)).toContain('#include <iostream>')
  expect(fixture.savedBodies.at(-1)).toContain('::: code-group')
  expect(fixture.savedBodies.at(-1)).toContain('`a\\|b`')
})
test('all three native modes retain ordinary inline formatting', async ({ page, fixture }) => {
  fixture.article.bodyMarkdown = '# 标题\n\n**中文加粗** 与普通正文\n'
  await open(page)
  for (const value of ['sv', 'wysiwyg', 'ir']) {
    await mode(page, value)
    await expectMode(page, value)
  }
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  await expect.poll(() => fixture.savedBodies.length).toBe(1)
  expect(fixture.savedBodies.at(-1)).toContain('**中文加粗**')
})
test('source headings and protected code are scoped and undoable', async ({ page, fixture }) => {
  fixture.article.bodyMarkdown = complex
  await open(page)
  await mode(page, 'sv')
  const input = await sourcePoint(page, '末尾正文')
  await page.keyboard.press('Alt+3')
  await expect(input).toContainText('### 末尾正文')
  await sourcePoint(page, '#include')
  await page.keyboard.press('Alt+2')
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  await expect.poll(() => fixture.savedBodies.length).toBe(1)
  expect(fixture.savedBodies.at(-1)).toContain('#include <iostream>')
  expect(fixture.savedBodies.at(-1)).not.toContain('## #include')
})
test('source table editing preserves pipes and image property changes keep the media URL', async ({
  page,
  fixture,
}) => {
  fixture.article.bodyMarkdown = complex
  await open(page)
  await mode(page, 'sv')
  await sourcePoint(page, '转义')
  await menu(page, '格式', '编辑表格')
  await page.getByRole('button', { name: '下插行', exact: true }).click()
  await sourcePoint(page, '![结构]')
  await menu(page, '格式', '图片属性')
  await page.getByLabel('替代文本').fill('更新的结构图')
  await page.getByLabel('图片尺寸').selectOption('full')
  await page.getByRole('button', { name: '应用图片属性', exact: true }).click()
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  await expect.poll(() => fixture.savedBodies.length).toBe(1)
  const saved = fixture.savedBodies.at(-1)
  expect(saved).toContain('转义\\|字符')
  expect(saved).toContain('`a\\|b`')
  expect(saved).toContain('/api/media/assets/19/content')
  expect(saved).toContain('full')
  expect(saved).toContain('更新的结构图')
})
test('TSV has an explicit preview and inserts only after confirmation at the captured source caret',async({page,fixture})=>{
  await open(page);await mode(page, 'sv');await sourcePoint(page,'Original body')
  await menu(page, '格式', '编辑表格');await page.getByText('粘贴 TSV 转表格',{exact:true}).click()
  await page.getByLabel('TSV 数据').fill('中文\t值\n转义|字符\t1')
  await expect(page.getByLabel('TSV 转换预览')).toContainText('转义\\|字符')
  expect(fixture.savedBodies).toHaveLength(0)
  await page.getByRole('button',{name:'确认转换并插入',exact:true}).click()
  await page.getByRole('button',{name:'保存正文',exact:true}).click()
  await expect.poll(() => fixture.savedBodies.length).toBe(1)
  expect(fixture.savedBodies.at(-1)).toContain('转义\\|字符');expect(fixture.savedBodies.at(-1)).toContain('Original body')
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
  await expect.poll(() => fixture.savedBodies.length).toBe(1)
  expect(fixture.savedBodies.at(-1)).toContain('Local offline text')
})
test('Markdown import can be cancelled, then explicitly applied and exported without saving', async ({
  page,
  fixture,
}) => {
  await open(page)
  const imported = '# 导入标题\n\n新增 **正文**\n'
  const picker = page.locator('.editor-file-tools input[type=file]')
  await picker.setInputFiles({
    name: 'test.md',
    mimeType: 'text/markdown',
    buffer: Buffer.from(imported),
  })
  await expect(page.getByRole('dialog', { name: 'Markdown 导入确认' })).toBeVisible()
  await page.getByRole('button', { name: '取消导入' }).click()
  await expect(page.locator('.vditor-ir p').filter({ hasText: 'Original body' })).toBeVisible()
  await picker.setInputFiles({
    name: 'test.md',
    mimeType: 'text/markdown',
    buffer: Buffer.from(imported),
  })
  await page.getByRole('button', { name: '确认导入到编辑器' }).click()
  await expect(page.locator('.vditor-sv[contenteditable=true]')).toContainText('新增 **正文**')
  expect(fixture.savedBodies).toHaveLength(0)
  const downloaded = page.waitForEvent('download')
  await menu(page, '文件', '导出 Markdown')
  expect((await downloaded).suggestedFilename()).toBe('导入标题.md')
})
test('Word export downloads a real docx and account shortcut settings apply only in the editor', async ({
  page,
  fixture,
}) => {
  const input = await open(page)
  await menu(page, '帮助', '快捷键设置与帮助')
  await page.getByLabel('配置H2快捷键').fill('Alt+7')
  await page.getByRole('button', { name: '保存快捷键', exact: true }).click()
  await input.locator('p').filter({ hasText: 'Original body' }).click()
  await page.keyboard.press('Alt+7')
  await expect(input.locator('h2')).toContainText('Original body')
  const downloaded = page.waitForEvent('download')
  await menu(page, '文件', '导出 Word')
  expect((await downloaded).suggestedFilename()).toMatch(/\.docx$/)
  expect(fixture.savedBodies).toHaveLength(0)
})
test('English and Chinese editors isolate focus and import suspends existing automatic saving', async ({
  page,
  fixture,
}) => {
  await page.goto('/useradmin/english/writing/articles/2/edit')
  const editors = page.locator('.markdown-editor')
  await expect(editors).toHaveCount(2)
  const en = editors.nth(0),
    zh = editors.nth(1)
  await expect(en.getByRole('menubar').getByRole('menuitem', { name: '段落', exact: true })).toBeEnabled()
  await en.locator('.vditor-ir p').click()
  await page.keyboard.press('Alt+2')
  await expect(en.locator('.vditor-ir h2')).toContainText('English body')
  await page.getByText('中文译文 · 可选', { exact: true }).click()
  await expect(zh.locator('.vditor-ir p')).toContainText('中文正文')
  await expect(zh.locator('.vditor-ir h2')).toHaveCount(0)
  await expect.poll(() => fixture.writingSaves.length).toBe(1)
  const count = fixture.writingSaves.length
  await en
    .locator('.editor-file-tools input[type=file]')
    .setInputFiles({
      name: 'recovery.md',
      mimeType: 'text/markdown',
      buffer: Buffer.from('# Restored draft\n\nLocal only\n'),
    })
  await en.getByRole('button', { name: '确认导入到编辑器' }).click()
  await expect(page.getByRole('status').filter({ hasText: '自动保存已暂停' })).toBeVisible()
  await page.waitForTimeout(1800)
  expect(fixture.writingSaves).toHaveLength(count)
  await page.getByRole('button', { name: '确认保存恢复稿', exact: true }).click()
  await expect.poll(() => fixture.writingSaves.length).toBe(count + 1)
  expect(fixture.writingSaves.at(-1).bodyMarkdown).toContain('Restored draft')
  expect(fixture.writingSaves.at(-1).translationZhMarkdown).toContain('中文正文')
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
    await mode(page, 'sv')
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
    await menu(page, '编辑', '质量检查')
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
