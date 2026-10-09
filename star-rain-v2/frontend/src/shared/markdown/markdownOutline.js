/*
 * 标题大纲的 slug 规则 —— 从 V1 `lib/markdownOutline.ts` 逐条移植成 JS。
 *
 * 这个文件存在的唯一理由是「渲染器产出的标题 id」与「大纲里的 id」必须是同一套规则算出来的：
 * 两部分一旦各写一份，阅读页的「本页导航」就会点不到标题，而且这种错位在开发时看不出来
 * （大纲条目照样显示，只是点了没反应）。因此渲染器与大纲都只调用这里的 uniqueHeadingId。
 *
 * 三处刻意保留 V1 的行为（不要「顺手优化」）：
 *   1. slug 用 NFKC 归一化后再小写：全角字母/数字（ＡＢ１）必须与半角落到同一个 id，
 *      否则同一篇文章里 Ａ 与 A 会得到两个不同 id；
 *   2. 只保留 `\w`、中划线、中日韩统一表意文字（\u4e00-\u9fa5）：标点、空格转中划线，
 *      其余字符直接删掉。`\w` 在 JS 正则里等价于 [A-Za-z0-9_]，所以下划线合法。
 *   3. 空标题（例如 `#` 后面什么都没有，或整串都被过滤掉）统一落到 'section'，
 *      而不是生成空 id —— 空 id 会让 `document.getElementById('')` 返回 null，
 *      大纲点击直接失效，且 `#` 锚点会跳到页面顶部。
 */

/* 全部 6 级标题都进大纲：V1 的阅读页就是这样，V2 不额外收窄 */
export const OUTLINE_MIN_LEVEL = 1
export const OUTLINE_MAX_LEVEL = 6

/*
 * 取标题的纯文本。
 * markdown-it 把标题内容放在 heading_open 的下一个 inline token 里，children 里才是真正的
 * 文本片段。这里只收 text / code_inline / math_inline 三种：
 *   - 行内代码 `# 用 run() 启动` → 取到「用 run() 启动」，而不是丢掉代码片段；
 *   - 行内公式 `# 复杂度 $O(n)$` → 取到 `O(n)`（这是 V2 新增数学后的必要项，
 *     V1 的列表里也有 math_inline，因为 V1 同样支持公式）；
 *   - 刻意不收 emoji / 图片等节点：图片的 alt 不进大纲，与 V1 一致。
 */
export function headingText(inline) {
  if (!inline || inline.type !== 'inline') return ''
  return (inline.children || [])
    .filter((child) => ['text', 'code_inline', 'math_inline'].includes(child.type))
    .map((child) => child.content)
    .join('')
    .trim()
}

/* 标题文本 → id 基名（同名的第 2 个标题会在此基础上加 -1，见 uniqueHeadingId） */
export function headingSlug(text) {
  const base = String(text)
    .normalize('NFKC')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\w\u4e00-\u9fa5-]/g, '')
  return base || 'section'
}

/*
 * 去重：同名标题依次得到 base、base-1、base-2 …
 * used 由调用方提供（渲染器放在 markdown-it 的 env 里），因此同一篇文档共用一个计数表，
 * 不同文档之间互不影响。返回值必须原样作为 DOM 的 id，且同一顺序下重复调用结果一致。
 */
export function uniqueHeadingId(text, used) {
  const base = headingSlug(text)
  const count = used.get(base) || 0
  used.set(base, count + 1)
  return count === 0 ? base : `${base}-${count}`
}
