/*
 * 图片尺寸标记 —— 从 V1 `lib/markdownImageSize.ts` 移植。
 *
 * 标记写在 Markdown 图片语法的 title 位（`![图](url "wide")`），而不是新造语法：
 * 这样正文仍然是可移植的普通 Markdown，粘到别处最坏只是多一句 title 文本，
 * 不会像 `::: code-group` 那样在别的渲染器里变成裸文本。
 *
 * 尺寸只影响排版类名，不影响 src，因此这里不做任何地址合法性判断
 * （地址安全由渲染器统一交给 markdown-it 的 validateLink + DOMPurify）。
 */
export const IMAGE_SIZES = ['normal', 'wide', 'full']

/* full 优先于 wide：`"wide full"` 这种写法按 V1 取 full */
export function resolveMarkdownImageSize(title) {
  const raw = String(title ?? '').trim().toLowerCase()
  if (!raw) return 'normal'
  if (/\bfull\b/.test(raw)) return 'full'
  if (/\bwide\b/.test(raw)) return 'wide'
  return 'normal'
}

/*
 * 剥离尺寸标记后的可见 title（图片说明/图注）。
 * 注意 normal 也要剥：作者可能显式写 `"架构图 normal"`，如果不剥，
 * 图注就会把 normal 这个词显示出来。
 */
export function stripMarkdownImageSizeToken(title) {
  if (!title) return ''
  return String(title)
    .replace(/\b(full|wide|normal)\b/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}
