import { test, expect } from './fixtures'
async function open(page) {
  await page.goto('/useradmin/blog/editor/1')
  const input = page.locator('.vditor-ir [contenteditable="true"]')
  await expect(input).toBeVisible()
  return input
}
test('Alt headings replace levels, paragraph conversion and undo preserve content', async ({ page, fixture }) => {
  const input = await open(page)
  await input.locator('p').filter({ hasText: 'Original body' }).click()
  for (const level of [1,2,3,4,5,6]) {
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
  await input.click(); await page.keyboard.press('Control+End'); await page.keyboard.press('Enter'); await page.keyboard.type('/h2')
  await expect(page.getByRole('listbox', { name: '段落命令' })).toBeVisible()
  await page.keyboard.press('Enter')
  await expect(input.locator('h2')).toContainText('标题')
  await expect(input).not.toContainText('/h2')
  await page.keyboard.press('Control+End'); await page.keyboard.press('Enter'); await page.keyboard.type('/no-command'); await page.keyboard.press('Escape')
  await expect(page.getByRole('listbox')).toHaveCount(0)
  await expect(input).toContainText('/no-command')
})
test('input fields and composing input never receive heading commands', async ({ page }) => {
  const input = await open(page)
  await page.getByLabel('文章标题').click(); await page.keyboard.press('Alt+2')
  await expect(input.locator('h2')).toHaveCount(0)
  await input.locator('p').filter({ hasText: 'Original body' }).click()
  await input.evaluate(element => {
    element.dispatchEvent(new CompositionEvent('compositionstart', { bubbles: true }))
    element.dispatchEvent(new KeyboardEvent('keydown', { key:'2',code:'Digit2',altKey:true,isComposing:true,bubbles:true }))
    element.dispatchEvent(new CompositionEvent('compositionend', { bubbles: true }))
  })
  await expect(input.locator('h2')).toHaveCount(0)
})
test('floating toolbar restores selection and stays closed after Escape', async ({ page, fixture }) => {
  const input = await open(page)
  await input.locator('p').filter({ hasText: 'Original body' }).evaluate(element => { const range = document.createRange(); range.selectNodeContents(element); const selection=window.getSelection(); selection.removeAllRanges(); selection.addRange(range); element.closest('[contenteditable]').focus() })
  const toolbar = page.getByRole('toolbar', { name: '选区格式' })
  await expect(toolbar).toBeVisible()
  await toolbar.getByRole('button', { name: '加粗', exact: true }).click()
  await expect(input.locator('[data-type="strong"]')).toContainText('Original body')
  await page.keyboard.press('Escape')
  await expect(toolbar).toHaveCount(0)
  await page.getByRole('button', { name: '保存正文', exact: true }).click()
  expect(fixture.savedBodies.at(-1)).toContain('**Original body**')
})
