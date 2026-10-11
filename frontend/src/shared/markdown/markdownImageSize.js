/**
 * markdownImageSize.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 阅读入口：resolveMarkdownImageSize（解析Markdown图片大小）；stripMarkdownImageSizeToken（stripMarkdown图片大小令牌的处理函数）。
 */

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
export
  /**
   * 解析Markdown图片大小。
   * 主要协作：.toLowerCase、.trim、.test。
   * @param title 标题；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function resolveMarkdownImageSize(title) {
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
export
  /**
   * stripMarkdown图片大小令牌的处理函数。
   * 主要协作：.trim、.replace。
   * @param title 标题；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function stripMarkdownImageSizeToken(title) {
  if (!title) return ''
  return String(title)
    .replace(/\b(full|wide|normal)\b/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}
