import { tex } from '@mdit/plugin-tex'

/*
 * 数学公式解析 —— 从 V1 `lib/markdownMath.ts` 移植。
 *
 * 目标：既支持 `$...$` / `$$...$$` / `\(...\)` / `\[...\]` / ```math 围栏，
 * 又不把「价格 $10」「$variable$name」这类普通文本误判成公式。
 *
 * 做法分两层：
 *   1. 块级与 `\(...\)` 等形态直接交给 @mdit/plugin-tex（delimiters: 'all'）；
 *   2. `$...$` 这一条把插件注册的 math_inline_dollar 规则**换掉**（见 useMarkdownMath 末尾），
 *      用下面的 canOpenDollar / canCloseDollar 自己判断边界。
 *
 * 为什么第 2 层必须自己写：插件的默认规则要求闭合 `$` 后面不能是字母数字，
 * 于是 `$\boxed{\Theta(n^2)}$Princeton`（公式后紧接英文单词，中文技术笔记里极常见）
 * 会被判成非法、退化成字面量。这里放宽为「内容看起来像 TeX 就允许」，
 * 同时保留 pandoc 式的保护：开 `$` 前不能是单词字符、后不能是空白。
 *
 * 渲染产物是 .math-source 占位元素（不是最终公式），真正的排版由 mathJax.js 的
 * typesetMath 在 DOMPurify 之后做 —— 顺序不能反：先排版再消毒会把 MathJax 的 SVG 剪掉。
 */

/* 单词字符：ASCII 字母数字与下划线（pandoc 的判定口径） */
function isWordChar(code) {
  if (code == null) return false
  return (code >= 48 && code <= 57)
    || (code >= 65 && code <= 90)
    || (code >= 97 && code <= 122)
    || code === 95
}

function isSpaceChar(code) {
  if (code == null) return false
  return code === 32 || code === 9 || code === 10 || code === 13
}

/* 内容几乎可以确定是 TeX（有反斜杠/上下标/花括号），而不是货币或占位词 */
function looksLikeTex(content) {
  return /[\\^_{}]/.test(content)
}

function canOpenDollar(src, pos) {
  if (src.charCodeAt(pos) !== 36) return false
  const prev = pos === 0 ? undefined : src.charCodeAt(pos - 1)
  const next = pos + 1 >= src.length ? undefined : src.charCodeAt(pos + 1)
  /* `$$` 交给块级规则，行内不抢 */
  if (prev === 36) return false
  /* 紧跟在单词后面：`price$10` 不是公式 */
  if (isWordChar(prev)) return false
  /* `$ 10` / 行尾 `$` 不是公式（开标记后面不能是空白） */
  if (isSpaceChar(next)) return false
  return true
}

function canCloseDollar(src, pos, content) {
  if (src.charCodeAt(pos) !== 36) return false
  const prev = src.charCodeAt(pos - 1)
  const next = pos + 1 >= src.length ? undefined : src.charCodeAt(pos + 1)
  if (next === 36) return false
  /* `$10 $` 这种闭标记前面是空白的写法不认 */
  if (isSpaceChar(prev)) return false
  /* 插件原本拒绝 `$...$Word`；内容像 TeX 时放行（本例的 \boxed{\Theta(n^2)}） */
  if (isWordChar(next) && !looksLikeTex(content)) return false
  return true
}

/* 数一数 pos 之前的连续反斜杠：奇数表示这个 `$` 被转义了，不能当闭标记 */
function countTrailingBackslashes(src, pos, minPos) {
  let count = 0
  let i = pos - 1
  while (i >= minPos && src.charCodeAt(i) === 92) {
    count += 1
    i -= 1
  }
  return count
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

/*
 * 公式占位元素。内容在这里被转义成纯文本：
 * 一是 DOMPurify 之后 MathJax 从这个元素的 textContent 取源码，
 * 二是 MathJax 没加载成功时，页面显示的就是这段原始 TeX（可读的降级），
 * 而不是空白或 undefined。
 */
function renderMathPlaceholder(source, display) {
  const className = display ? 'math-source math-source--display' : 'math-source math-source--inline'
  const tag = display ? 'div' : 'span'
  return `<${tag} class="${className}" data-display="${display}">${escapeHtml(source)}</${tag}>`
}

/*
 * 自定义的行内 `$...$` 规则。
 * 找不到合法闭标记时按字面量 $ 处理（pending += '$'）并前进一个字符，
 * 这样 `$10 与 $20` 会原样输出，不会吞掉正文。
 */
function mathInlineDollar(state, silent) {
  const { src } = state
  if (src[state.pos] !== '$') return false
  if (!canOpenDollar(src, state.pos)) {
    if (!silent) state.pending += '$'
    state.pos += 1
    return true
  }

  const start = state.pos + 1
  let end = start
  while ((end = src.indexOf('$', end)) !== -1) {
    if (end >= state.posMax) {
      end = -1
      break
    }
    /* 被转义的 `$`（奇数个反斜杠）不是闭标记 */
    if (countTrailingBackslashes(src, end, start) % 2 === 1) {
      end += 1
      continue
    }
    const content = src.slice(start, end)
    if (canCloseDollar(src, end, content)) break
    end = -1
    break
  }

  if (end === -1) {
    if (!silent) state.pending += '$'
    state.pos += 1
    return true
  }
  /* `$$` 空内容：整体当字面量，交给块级规则或原样显示 */
  if (end === start) {
    if (!silent) state.pending += '$$'
    state.pos += 2
    return true
  }

  if (!silent) {
    const token = state.push('math_inline', 'math', 0)
    token.markup = '$'
    token.content = src.slice(start, end)
  }
  state.pos = end + 1
  return true
}

/*
 * 粘贴归一化：把「整段就是一条 TeX」的剪贴板内容包成 `$$...$$`。
 *
 * 场景：从网页公式编辑器 / 论文里复制 `\frac{a}{b}`，直接粘进 Vditor 只会是一串裸文本
 * （`\f` 还可能被 Markdown 的转义规则吃掉）。这里在粘贴的捕获阶段先判断，
 * 只有「整段都是显式公式或整段都是 TeX」才改写并插入，其余一律返回 null 走 Vditor 原生粘贴。
 *
 * 三条刻意保留的拒绝条件（V1 原样）：
 *   - 含反引号 → 是代码片段，不是公式；
 *   - 是列表项（`- ` / `1. `）→ 是正文结构；
 *   - `$10` 这种没有 TeX 特征的美元数字 → 是价格。
 */
const EXPLICIT_FORMULA = /^(?:\$\$[\s\S]*\$\$|\$[^\n$]+\$|\\\([\s\S]*\\\)|\\\[[\s\S]*\\\]|```(?:math|latex)\s*\n[\s\S]*\n```)$/i

export function normalizePastedMath(value) {
  const source = String(value ?? '').replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const trimmed = source.trim()
  if (!trimmed) return null
  /* 已经是合法公式语法：原样插入，不做二次包装 */
  if (EXPLICIT_FORMULA.test(trimmed)) return source
  const looksLikeWholeTex = /\\[a-zA-Z]+|\\[{}()[\]]/.test(trimmed)
    && !/`/.test(trimmed)
    && !/^\s*(?:[-*+]\s|\d+\.\s)/m.test(trimmed)
  return looksLikeWholeTex ? `$$\n${trimmed}\n$$` : null
}

/*
 * 把数学语法挂到 markdown-it 上。
 * 调用顺序有依赖：必须先 md.use(tex) 注册出 math_inline_dollar，才能用 ruler.at 替换它。
 */
export function useMarkdownMath(md) {
  md.use(tex, {
    /* 同时接受 $...$、$$...$$、\(...\)、\[...\] */
    delimiters: 'all',
    /* ```math 围栏也当公式 */
    mathFence: true,
    render: renderMathPlaceholder,
  })
  /* ```latex 围栏与 ```math 等价（插件只认 math，这里补上 latex） */
  const originalFence = md.renderer.rules.fence
  md.renderer.rules.fence = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    if (token.info.trim().toLowerCase() === 'latex') {
      return renderMathPlaceholder(token.content, true)
    }
    return originalFence
      ? originalFence(tokens, idx, options, env, self)
      : self.renderToken(tokens, idx, options)
  }
  md.inline.ruler.at('math_inline_dollar', mathInlineDollar)
  md.renderer.rules.math_inline = (tokens, idx) => renderMathPlaceholder(tokens[idx].content, false)
  md.renderer.rules.math_block = (tokens, idx) => renderMathPlaceholder(tokens[idx].content, true)
}
