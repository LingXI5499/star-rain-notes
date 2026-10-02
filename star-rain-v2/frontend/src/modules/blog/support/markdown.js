/*
 * 博客正文 Markdown 渲染（自己实现，不引入 markdown-it 等库）。
 *
 * 安全策略与 V1 `components/MarkdownRenderer.vue` 一致，也是这个文件存在的理由：
 * **先把文本整体转义，再做结构替换**。任何来自正文的文本在拼进 HTML 之前都经过
 * escapeHtml，之后只追加本文件自己生成的标签，源文本永远不会回到未转义状态，
 * 所以正文里写 <script> 只会显示成字面量。
 * 链接与图片地址另外过一遍 safeUrl：只允许 http(s) 与站内相对路径，
 * `javascript:` / `data:` 一律降级为纯文本。
 *
 * 支持：标题(h1–h6) / 段落 / 无序与有序列表（含嵌套）/ 引用 / 围栏代码块 /
 *       分隔线 / 表格 / 加粗 / 斜体 / 删除线 / 行内代码 / 链接 / 图片。
 * 不支持（与 V1 的 markdown-it 管线相比，这里如实列出）：
 * 内联 HTML、脚注、数学公式、`::: code-group` 多语言页签、裸链接自动 linkify、
 * 引用块内嵌列表。这些语法会按普通文本渲染，而不会报错。
 *
 * 标题会生成稳定 id（供右侧「本页导航」与锚点使用），并由 renderMarkdownDocument
 * 一并返回大纲；V1 把这部分放在 lib/markdownOutline.ts，V2 合并进同一个文件，
 * 因为两者必须共用同一套 slug 规则，分开就一定会漂移。
 */

// 语言标签：与 V1 lib/markdownCodeGroup.ts 的 LANG_LABELS 保持同一份对照
const LANG_LABELS = {
  c: 'C',
  cpp: 'C++',
  csharp: 'C#',
  cs: 'C#',
  java: 'Java',
  javascript: 'JavaScript',
  js: 'JavaScript',
  typescript: 'TypeScript',
  ts: 'TypeScript',
  python: 'Python',
  py: 'Python',
  go: 'Go',
  rust: 'Rust',
  sql: 'SQL',
  bash: 'Bash',
  sh: 'Bash',
  shell: 'Bash',
  json: 'JSON',
  html: 'HTML',
  css: 'CSS',
  xml: 'XML',
  yaml: 'YAML',
  yml: 'YAML',
  markdown: 'Markdown',
  md: 'Markdown',
}

const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }

// 只允许站内相对地址与 http(s) 外链；其余（javascript:、data: 等）一律降级为纯文本
const SAFE_URL = /^(https?:\/\/|\/|\.\/|\.\.\/)/i

const LIST_ITEM = /^(\s*)(?:(\d+)[.)]|[-*+])\s+(.*)$/
const HEADING = /^(#{1,6})\s+(.*)$/
const FENCE = /^\s*(```|~~~)\s*([\w+#.-]*)\s*$/
const HORIZONTAL_RULE = /^\s*(?:-{3,}|\*{3,}|_{3,})\s*$/
const TABLE_DELIMITER = /^\s*\|?\s*:?-{2,}:?\s*(?:\|\s*:?-{2,}:?\s*)*\|?\s*$/
const QUOTE = /^\s*>\s?(.*)$/

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ESCAPES[character])
}

function safeUrl(value) {
  const trimmed = String(value).trim()
  return SAFE_URL.test(trimmed) ? trimmed : null
}

export function languageLabel(lang) {
  const key = String(lang || '').trim().toLowerCase()
  if (!key) return 'Code'
  return LANG_LABELS[key] || key.toUpperCase()
}

// 标题 id：与 V1 lib/markdownOutline.ts 的 headingSlug 同一套规则
function headingSlug(text) {
  const base = String(text)
    .normalize('NFKC')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\w\u4e00-\u9fa5-]/g, '')
  return base || 'section'
}

function uniqueHeadingId(text, used) {
  const base = headingSlug(text)
  const count = used.get(base) || 0
  used.set(base, count + 1)
  return count === 0 ? base : `${base}-${count}`
}

/*
 * 行内标记。入参必须已经是转义过的文本：这里只做标记替换，
 * 绝不把内容拼回未转义状态（行内代码先取出、最后按占位符放回）。
 */
function inline(escaped) {
  const codes = []
  let out = escaped.replace(/`([^`]+)`/g, (match, code) => {
    codes.push(code)
    return `\u0000${codes.length - 1}\u0000`
  })
  out = out.replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (match, alt, url) => {
    const target = safeUrl(url)
    return target ? `<img src="${target}" alt="${alt}" loading="lazy" decoding="async" />` : alt
  })
  out = out.replace(/\[([^\]]*)\]\(([^)\s]+)\)/g, (match, label, url) => {
    const target = safeUrl(url)
    if (!target) return label
    // 站内相对地址不开新窗口，外链才开，并补 rel="noopener"
    const external = /^https?:/i.test(target)
    return external
      ? `<a href="${target}" target="_blank" rel="noopener noreferrer">${label}</a>`
      : `<a href="${target}">${label}</a>`
  })
  out = out.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  out = out.replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>')
  out = out.replace(/~~([^~]+)~~/g, '<del>$1</del>')
  out = out.replace(/\u0000(\d+)\u0000/g, (match, index) => `<code>${codes[Number(index)]}</code>`)
  return out
}

// 一段普通文本：先转义，再做行内替换
const paragraphInline = (raw) => inline(escapeHtml(raw))

function renderCodeBlock(language, codeLines) {
  const label = languageLabel(language)
  const className = language ? ` class="language-${escapeHtml(language.toLowerCase())}"` : ''
  return [
    '<div class="code-block">',
    '<div class="code-block__head">',
    `<span class="code-block__label">${escapeHtml(label)}</span>`,
    '<button class="code-block__copy" type="button" data-code-copy aria-label="复制代码">复制</button>',
    '</div>',
    `<pre><code${className}>${escapeHtml(codeLines.join('\n'))}</code></pre>`,
    '</div>',
  ].join('')
}

function splitCells(row) {
  const trimmed = row.trim().replace(/^\|/, '').replace(/\|$/, '')
  return trimmed.split('|').map((cell) => cell.trim())
}

function renderTable(headerCells, alignments, rows) {
  const align = (index) => (alignments[index] ? ` style="text-align:${alignments[index]}"` : '')
  const head = headerCells
    .map((cell, index) => `<th${align(index)}>${paragraphInline(cell)}</th>`)
    .join('')
  const body = rows
    .map((row) => `<tr>${row.map((cell, index) => `<td${align(index)}>${paragraphInline(cell)}</td>`).join('')}</tr>`)
    .join('')
  return `<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`
}

function renderList(ordered, items) {
  const tag = ordered ? 'ol' : 'ul'
  const body = items
    .map((item) => {
      const text = item.content ? paragraphInline(item.content) : ''
      const nested = item.children.join('')
      return `<li>${text}${nested}</li>`
    })
    .join('')
  return `<${tag}>${body}</${tag}>`
}

/*
 * 列表解析：按缩进递归。
 * 同一缩进是同级项，缩进更深的行归到上一个项内部（嵌套列表），
 * 这些行本身也可能是列表，于是再递归一次。
 */
function parseList(lines, start) {
  const baseIndent = LIST_ITEM.exec(lines[start])[1].length
  const ordered = Boolean(LIST_ITEM.exec(lines[start])[2])
  const items = []
  let index = start

  while (index < lines.length) {
    const matched = LIST_ITEM.exec(lines[index])
    if (!matched || matched[1].length < baseIndent) break

    if (matched[1].length > baseIndent) {
      const [nested, next] = parseList(lines, index)
      if (items.length) items[items.length - 1].children.push(nested)
      index = next
      continue
    }

    const content = [matched[3]]
    index += 1
    // 列表项的续行（缩进 ≥2 且不是新项）并入同一项文本
    while (index < lines.length) {
      const raw = lines[index]
      if (raw.trim() === '' || LIST_ITEM.test(raw)) break
      if (!/^\s{2,}/.test(raw)) break
      if (FENCE.test(raw) || HEADING.test(raw.trim()) || QUOTE.test(raw)) break
      content.push(raw.trim())
      index += 1
    }
    items.push({ content: content.join(' '), children: [] })
  }

  return [renderList(ordered, items), index]
}

function isTableStart(lines, index) {
  if (index + 1 >= lines.length || !lines[index].includes('|')) return false
  return TABLE_DELIMITER.test(lines[index + 1]) && lines[index + 1].includes('-')
}

function renderBlocks(lines, outline) {
  const html = []
  const usedIds = new Map()
  let index = 0

  while (index < lines.length) {
    const raw = lines[index]

    // 空行
    if (raw.trim() === '') {
      index += 1
      continue
    }

    // 围栏代码块
    const fence = FENCE.exec(raw)
    if (fence) {
      const marker = fence[1]
      const language = fence[2]
      const code = []
      index += 1
      while (index < lines.length && !new RegExp(`^\\s*${marker}\\s*$`).test(lines[index])) {
        code.push(lines[index])
        index += 1
      }
      index += 1 // 跳过结束围栏
      html.push(renderCodeBlock(language, code))
      continue
    }

    const trimmed = raw.trim()

    // 标题
    const heading = HEADING.exec(trimmed)
    if (heading) {
      const level = heading[1].length
      const text = heading[2].trim()
      const id = uniqueHeadingId(text, usedIds)
      if (outline) outline.push({ level, id, text })
      html.push(`<h${level} id="${escapeHtml(id)}">${paragraphInline(text)}</h${level}>`)
      index += 1
      continue
    }

    // 分隔线
    if (HORIZONTAL_RULE.test(trimmed)) {
      html.push('<hr />')
      index += 1
      continue
    }

    // 表格：表头 + 分隔行之后是数据行
    if (isTableStart(lines, index)) {
      const headerCells = splitCells(lines[index])
      const alignments = splitCells(lines[index + 1]).map((cell) => {
        const left = cell.startsWith(':')
        const right = cell.endsWith(':')
        if (left && right) return 'center'
        if (right) return 'right'
        if (left) return 'left'
        return ''
      })
      index += 2
      const rows = []
      while (index < lines.length && lines[index].includes('|') && lines[index].trim() !== '') {
        rows.push(splitCells(lines[index]))
        index += 1
      }
      html.push(renderTable(headerCells, alignments, rows))
      continue
    }

    // 引用：连续多行合成一个引用块，段内换行按空格连接（与 V1 的 breaks:false 一致）
    if (QUOTE.test(raw)) {
      const quoted = []
      while (index < lines.length && QUOTE.test(lines[index])) {
        quoted.push(QUOTE.exec(lines[index])[1])
        index += 1
      }
      html.push(`<blockquote><p>${paragraphInline(quoted.join(' '))}</p></blockquote>`)
      continue
    }

    // 列表
    if (LIST_ITEM.test(raw)) {
      const [list, next] = parseList(lines, index)
      html.push(list)
      index = next
      continue
    }

    // 段落：连续的非空、非块级起始行合并为一段。
    // 走到这里时行一定不是空行，也不是任何块级语法的起始行（上面都 continue 掉了），
    // 因此循环至少会收入一行。
    const paragraph = []
    while (index < lines.length) {
      const candidate = lines[index]
      const candidateTrimmed = candidate.trim()
      if (candidateTrimmed === '') break
      if (paragraph.length && (
        FENCE.test(candidate)
        || HEADING.test(candidateTrimmed)
        || HORIZONTAL_RULE.test(candidateTrimmed)
        || QUOTE.test(candidate)
        || LIST_ITEM.test(candidate)
        || isTableStart(lines, index)
      )) break
      paragraph.push(candidateTrimmed)
      index += 1
    }

    html.push(`<p>${paragraphInline(paragraph.join(' '))}</p>`)
  }

  return html.join('\n')
}

function normalize(source) {
  return String(source ?? '').replace(/\r\n?/g, '\n')
}

/*
 * 渲染成 HTML 字符串（后台预览与前台正文共用同一套渲染逻辑，
 * 保证「作者看到的预览」就是「读者看到的正文」）。
 */
export function renderMarkdown(source) {
  if (!source || !String(source).trim()) {
    return '<p class="blog-prose__empty">（正文为空）</p>'
  }
  return renderBlocks(normalize(source).split('\n'), null)
}

/*
 * 渲染并同时给出大纲。前台详情页需要标题 id 与大纲来做「本页导航」，
 * 因此单独提供一个入口，而不是让调用方自己再解析一次 HTML。
 */
export function renderMarkdownDocument(source) {
  if (!source || !String(source).trim()) {
    return { html: '<p class="blog-prose__empty">（正文为空）</p>', outline: [] }
  }
  const outline = []
  const html = renderBlocks(normalize(source).split('\n'), outline)
  return { html, outline }
}
