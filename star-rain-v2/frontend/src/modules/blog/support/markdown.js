/*
 * 极简 Markdown 渲染，只为博客正文的阅读体验服务。
 *
 * 为什么不装一个 Markdown 库：博客正文此刻只需要标题、段落、列表、引用、代码块与图片/链接，
 * 而任何通用库都要处理内联 HTML —— 那意味着必须再引入一套 HTML 白名单清洗。
 * 这里的策略相反：先把整段文本转义，再做结构替换，因此正文里写什么 HTML 都不会被执行。
 *
 * 支持范围：标题 / 段落 / 无序与有序列表 / 引用 / 代码块 / 分隔线 /
 * 加粗 / 斜体 / 行内代码 / 链接 / 图片。
 * 明确不支持：内联 HTML、表格、脚注。
 */

const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }

// 只允许站内相对地址、本站媒体内容地址与 http(s) 外链；其余（javascript:、data: 等）一律降级为纯文本
const SAFE_URL = /^(https?:\/\/|\/|\.\/|\.\.\/)/i

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ESCAPES[character])
}

function safeUrl(value) {
  const trimmed = String(value).trim()
  return SAFE_URL.test(trimmed) ? trimmed : null
}

// 输入必须是已经转义过的文本：这里只做标记替换，绝不再把内容拼回未转义状态
function inline(escaped) {
  const codes = []
  let out = escaped.replace(/`([^`]+)`/g, (match, code) => {
    codes.push(code)
    return `\u0000${codes.length - 1}\u0000`
  })
  out = out.replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (match, alt, url) => {
    const target = safeUrl(url)
    return target ? `<img src="${target}" alt="${alt}" loading="lazy" />` : alt
  })
  out = out.replace(/\[([^\]]*)\]\(([^)\s]+)\)/g, (match, label, url) => {
    const target = safeUrl(url)
    return target ? `<a href="${target}" target="_blank" rel="noopener">${label}</a>` : label
  })
  out = out.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  out = out.replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>')
  out = out.replace(/\u0000(\d+)\u0000/g, (match, index) => `<code>${codes[Number(index)]}</code>`)
  return out
}

export function renderMarkdown(markdown) {
  if (!markdown || !markdown.trim()) {
    return '<p class="blog-prose__empty">（正文为空）</p>'
  }
  const lines = String(markdown).replace(/\r\n?/g, '\n').split('\n')
  const html = []
  let listType = null
  let paragraph = []

  const flushParagraph = () => {
    if (paragraph.length) {
      html.push(`<p>${inline(paragraph.join(' '))}</p>`)
      paragraph = []
    }
  }
  const closeList = () => {
    if (listType) {
      html.push(`</${listType}>`)
      listType = null
    }
  }

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index].trim()

    if (line.startsWith('```')) {
      flushParagraph()
      closeList()
      const language = line.slice(3).trim()
      const code = []
      index += 1
      while (index < lines.length && !lines[index].trim().startsWith('```')) {
        code.push(lines[index])
        index += 1
      }
      const className = language ? ` class="language-${escapeHtml(language)}"` : ''
      html.push(`<pre><code${className}>${escapeHtml(code.join('\n'))}</code></pre>`)
      continue
    }

    if (!line) {
      flushParagraph()
      closeList()
      continue
    }

    const heading = /^(#{1,6})\s+(.*)$/.exec(line)
    if (heading) {
      flushParagraph()
      closeList()
      const level = heading[1].length
      html.push(`<h${level}>${inline(escapeHtml(heading[2]))}</h${level}>`)
      continue
    }

    if (/^(-{3,}|\*{3,})$/.test(line)) {
      flushParagraph()
      closeList()
      html.push('<hr />')
      continue
    }

    const quote = /^>\s?(.*)$/.exec(line)
    if (quote) {
      flushParagraph()
      closeList()
      html.push(`<blockquote>${inline(escapeHtml(quote[1]))}</blockquote>`)
      continue
    }

    const ordered = /^\d+\.\s+(.*)$/.exec(line)
    const bullet = /^[-*+]\s+(.*)$/.exec(line)
    if (ordered || bullet) {
      flushParagraph()
      const type = ordered ? 'ol' : 'ul'
      if (listType !== type) {
        closeList()
        html.push(`<${type}>`)
        listType = type
      }
      html.push(`<li>${inline(escapeHtml((ordered || bullet)[1]))}</li>`)
      continue
    }

    paragraph.push(escapeHtml(line))
  }

  flushParagraph()
  closeList()
  return html.join('\n')
}
