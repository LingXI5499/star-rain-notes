import { it, expect, vi } from 'vitest'
import { printMarkdown } from './pdfPrint'
it('prints an isolated sanitized document and releases its frame', async () => {
  const print = vi.fn(),
    win = { print, focus: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn() }
  const getter = vi.spyOn(HTMLIFrameElement.prototype, 'contentWindow', 'get').mockReturnValue(win)
  const cleanup = await printMarkdown(
    '# 打印正文\n\n<script>window.xss=1</script>\n\n::: code-group Java|Python\n\n```java\na\n```\n\n```python\nb\n```\n\n:::',
  )
  const frame = document.querySelector('iframe')
  expect(frame.contentDocument.querySelector('h1').textContent).toBe('打印正文')
  expect(frame.contentDocument.querySelector('script')).toBeNull()
  expect(frame.contentDocument.querySelectorAll('pre')).toHaveLength(2)
  expect(print).toHaveBeenCalledOnce()
  cleanup()
  expect(document.querySelector('iframe')).toBeNull()
  getter.mockRestore()
})
it('does not open a stale print job when its document identity has changed', async () => {
  const print = vi.fn(),
    getter = vi
      .spyOn(HTMLIFrameElement.prototype, 'contentWindow', 'get')
      .mockReturnValue({ print, removeEventListener: vi.fn() })
  await printMarkdown(
    '# A',
    () => {},
    () => false,
  )
  expect(print).not.toHaveBeenCalled()
  expect(document.querySelector('iframe')).toBeNull()
  getter.mockRestore()
})
