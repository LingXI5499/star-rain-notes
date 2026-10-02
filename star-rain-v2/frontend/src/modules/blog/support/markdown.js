import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import csharp from 'highlight.js/lib/languages/csharp'
import css from 'highlight.js/lib/languages/css'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'
import { headingText, OUTLINE_MAX_LEVEL, OUTLINE_MIN_LEVEL, uniqueHeadingId } from './markdownOutline'
import { resolveMarkdownImageSize, stripMarkdownImageSizeToken } from './markdownImageSize'
import { languageLabel, useMarkdownCodeGroup } from './markdownCodeGroup'
import { useMarkdownMath } from './markdownMath'
import { typesetMath } from './mathJax'

/*
 * 博客正文渲染管线 —— 从 V1 `components/MarkdownRenderer.vue` 移植（去掉 Vue 外壳）。
 *
 * 这是**唯一**一份 Markdown → HTML 的实现，后台预览（BlogPreview）与公开阅读页
 * （BlogProse）都调它，因此「作者在后台看到的」与「读者在前台看到的」不可能不一致。
 * 这条约束是 V1 写的（AGENTS.md：共用同一条管线），V2 继续遵守。
 *
 * 管线：Markdown → markdown-it 解析 → DOMPurify 消毒 → 交给浏览器 → 再构建代码页签 / 排版公式。
 * 顺序不能变：
 *   - 消毒必须在 v-html 之前，否则等于没消毒；
 *   - 公式排版必须在消毒**之后**（MathJax 的 SVG/MathML 产物会被消毒器剪掉）；
 *   - 代码页签也在消毒之后建（用 DOM API 建，绕开「字符串拼接 + 白名单」的麻烦）。
 *
 * 安全底线（三条都要成立，缺一条就等于开了 XSS 口子）：
 *   1. markdown-it `html: false` —— 正文里的原始 HTML 标签不会进入词法结果；
 *   2. DOMPurify 再消毒一遍，配置见 SANITIZE_CONFIG，白名单只加了本渲染器自己产出的
 *      data-* 属性，**没有放宽任何标签或属性**；
 *   3. 公式源码在占位元素里是文本节点（markdownMath.js 转义），不是 HTML。
 */

/*
 * 只注册这个技术知识库真正用到的 14 种语言。
 * 引入 highlight.js 默认全量包会把近 200 种语法拖进阅读页，V1 的记录是渲染器 chunk
 * 会因此超过 1.1MB —— 因此这里逐个 import，新增语言必须显式加进来。
 */
const languages = {
  bash,
  c,
  cpp,
  csharp,
  css,
  java,
  javascript,
  json,
  markdown,
  python,
  sql,
  typescript,
  xml,
  yaml,
}

for (const [name, language] of Object.entries(languages)) {
  hljs.registerLanguage(name, language)
}

/* 别名：围栏里写 sh / js / html 等常见写法也要能着色，否则作者会看到「语言不被支持」的灰块 */
hljs.registerAliases(['sh', 'shell'], { languageName: 'bash' })
hljs.registerAliases(['cs'], { languageName: 'csharp' })
hljs.registerAliases(['js', 'jsx'], { languageName: 'javascript' })
hljs.registerAliases(['md'], { languageName: 'markdown' })
hljs.registerAliases(['py'], { languageName: 'python' })
hljs.registerAliases(['ts', 'tsx'], { languageName: 'typescript' })
hljs.registerAliases(['html', 'vue'], { languageName: 'xml' })
hljs.registerAliases(['yml'], { languageName: 'yaml' })

const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ESCAPES[character])
}

/*
 * DOMPurify 配置，逐条照抄 V1（不许放宽）：
 *   - html + mathMl + svg：MathJax 的产物是 SVG（内部包 MathML），剪掉就白排版了；
 *   - ADD_TAGS div/span：html profile 默认就允许，写出来是为了让「这里需要容器标签」显式化；
 *   - ADD_ATTR 里全是本渲染器自己产出的标记：沙箱包裹层、代码组与标签、公式的 display。
 *     data-* 属性 DOMPurify 默认放行（ALLOW_DATA_ATTR），列出来是防止将来有人关掉它。
 *   - id 属性不在白名单里但属于 DOMPurify 默认允许项，**标题 id 依赖它**，
 *     所以绝不能改成自定义白名单（改了就只剩 ALLOWED_ATTR 里那几个，id 会被删掉，
 *     大纲点击会全部失效）。
 */
const SANITIZE_CONFIG = {
  USE_PROFILES: { html: true, mathMl: true, svg: true },
  ADD_TAGS: ['div', 'span'],
  ADD_ATTR: [
    'data-markdown-sandbox',
    'data-code-group',
    'data-code-labels',
    'data-code-langs',
    'data-display',
    'hidden',
  ],
}

function createRenderer() {
  const md = new MarkdownIt({
    /* 正文里的原始 HTML 一律不解析（安全底线第 1 条） */
    html: false,
    linkify: true,
    breaks: false,
    highlight(code, lang) {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return hljs.highlight(code, { language: lang }).value
        } catch {
          /* 语法本身有问题时退回到转义后的纯文本，不能让一整篇文章渲染失败 */
        }
      }
      return escapeHtml(code)
    },
  })

  useMarkdownMath(md)
  useMarkdownCodeGroup(md)

  /*
   * 图片：补 loading=lazy / decoding=async，并把 title 里的尺寸标记
   * （"wide" / "full" / "normal"，见 markdownImageSize.js）转成排版类名。
   * 标记要从 title 里剥掉，否则图注会显示「架构图 wide」。
   */
  const renderImage = md.renderer.rules.image
  md.renderer.rules.image = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    token.attrSet('loading', 'lazy')
    token.attrSet('decoding', 'async')
    const titleAttr = token.attrGet('title')
    const title = typeof titleAttr === 'string' ? titleAttr : titleAttr == null ? null : String(titleAttr)
    const size = resolveMarkdownImageSize(title)
    const className = `markdown-img markdown-img--${size}`
    const existing = token.attrGet('class')
    const existingClass = typeof existing === 'string' ? existing : existing == null ? '' : String(existing)
    token.attrSet('class', existingClass ? `${existingClass} ${className}` : className)
    const cleaned = stripMarkdownImageSizeToken(title)
    if (cleaned) token.attrSet('title', cleaned)
    else token.attrs = (token.attrs ?? []).filter(([name]) => name !== 'title')
    return renderImage(tokens, idx, options, env, self)
  }

  /*
   * 标题：写 id 的同时收集大纲。
   *
   * 收集状态放在 markdown-it 的 env 里（而不是模块级变量）：
   * 渲染器是模块单例，同一个页面上后台预览与正文可能先后渲染，
   * 用 env 才能保证两次渲染的 id 计数互不干扰。
   * id 的唯一性与 markdownOutline.js 共用同一套规则 —— 这是「大纲能点到标题」的前提。
   */
  md.renderer.rules.heading_open = (tokens, idx, options, env) => {
    const token = tokens[idx]
    const text = headingText(tokens[idx + 1])
    const id = uniqueHeadingId(text, env.headingIds)
    const level = Number(token.tag.slice(1))
    if (level >= OUTLINE_MIN_LEVEL && level <= OUTLINE_MAX_LEVEL) {
      env.headings.push({ level, id, text })
    }
    return `<h${level} id="${id}">`
  }

  return md
}

const md = createRenderer()

/* 新建一次渲染的状态容器。两处必须成对出现，因此单独一个函数 */
function createEnv() {
  return { headings: [], headingIds: new Map() }
}

function sanitize(rendered) {
  /*
   * 外面套一层一次性父节点：DOMPurify 会把「只有一个顶层块」的文档直接提升成它的子节点，
   * 于是「整篇只有一个代码组」的文章会丢掉代码组外壳上的 data 属性，页签就建不起来。
   * 有这一层父节点，顶层永远是我们的沙箱 div，子结构原样保留。
   */
  return DOMPurify.sanitize(`<div data-markdown-sandbox="true">${rendered}</div>`, SANITIZE_CONFIG)
}

/* 只渲染 HTML（后台「预览」这类不需要大纲的场景） */
export function renderMarkdown(source) {
  if (!source || !String(source).trim()) {
    return '<p class="blog-prose__empty">（正文为空）</p>'
  }
  return sanitize(md.render(String(source), createEnv()))
}

/*
 * 渲染 HTML 并返回大纲。
 * 大纲的 id 与 HTML 里标题的 id 是同一份数据，调用方不需要（也不应该）再解析一次 DOM。
 */
export function renderMarkdownDocument(source) {
  if (!source || !String(source).trim()) {
    return { html: '<p class="blog-prose__empty">（正文为空）</p>', outline: [] }
  }
  const env = createEnv()
  const html = sanitize(md.render(String(source), env))
  return { html, outline: env.headings }
}

/* 代码组外壳的实例编号。模块级自增，保证同一页面里多篇正文的 id 不重复 */
let codeGroupId = 0

/* 围栏语言：优先取 <code> 上的 language-xxx；highlight.js 也可能把它挂在 <pre> 上 */
function fenceLanguage(pre) {
  const code = pre.querySelector('code')
  const fromClass = code?.className.match(/language-([\w+-]+)/)?.[1]
  if (fromClass) return fromClass
  return (pre.className.match(/language-([\w+-]+)/)?.[1]) ?? 'code'
}

/*
 * 取代码组的显示标签。
 * 优先 data-code-labels（JSON，能保住自定义名称里的逗号），
 * 其次 data-code-langs（逗号分隔的旧标记），最后回退到围栏语言。
 */
function codeGroupLabels(group) {
  const encoded = group.dataset.codeLabels
  if (encoded) {
    try {
      const labels = JSON.parse(encoded)
      if (Array.isArray(labels) && labels.every((label) => typeof label === 'string')) {
        return labels.map((label) => label.trim()).filter(Boolean)
      }
    } catch {
      /* JSON 坏了就退回旧格式，不让一个坏属性毁掉整段代码 */
    }
  }
  return (group.dataset.codeLangs ?? '')
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
}

/*
 * 给独立的代码块加「语言标签 + 复制按钮」外壳。
 * 已经处理过的（data-enhanced）与代码组内部的（组里自己建工具栏）都跳过。
 */
function enhanceStandaloneCodeBlocks(container) {
  container.querySelectorAll('pre').forEach((pre) => {
    if (pre.dataset.enhanced) return
    if (pre.closest('[data-code-group]')) return
    pre.dataset.enhanced = 'true'
    const lang = fenceLanguage(pre)
    const label = document.createElement('span')
    label.className = 'code-block__label'
    label.textContent = languageLabel(lang)
    const head = document.createElement('div')
    head.className = 'code-block__head'
    head.append(label, createCopyButton())
    const wrap = document.createElement('div')
    wrap.className = 'code-block'
    wrap.dataset.enhanced = 'true'
    pre.replaceWith(wrap)
    wrap.append(head, pre)
  })
}

/*
 * 复制按钮。
 * 行为本身不在这里绑定：正文是 v-html 出来的，每次重渲染按钮都会被换掉，
 * 逐个 addEventListener 就得逐个 removeEventListener。改由 useCodeCopy 在正文容器上
 * 做事件委托，按钮只负责带上 data-code-copy 标记（代码组里的再多带一个组编号）。
 */
function createCopyButton(groupId) {
  const button = document.createElement('button')
  button.className = groupId ? 'code-block__copy code-group__copy' : 'code-block__copy'
  button.type = 'button'
  button.textContent = '复制'
  button.setAttribute('aria-label', groupId ? '复制当前语言代码' : '复制代码')
  button.dataset.codeCopy = ''
  if (groupId) button.dataset.copyGroup = groupId
  return button
}

/*
 * 把 `::: code-group` 外壳升级成 LeetCode 风格的语言页签。
 *
 * 页签与面板都带 data-group / data-index，切换由 useCodeCopy 的委托处理器完成
 * （见上一条注释）。面板用 hidden 属性而不是 display:none 类：
 * 这样无样式时（CSS 没加载）也只有一个面板可见，不会出现代码堆叠。
 */
function enhanceCodeGroups(container) {
  container.querySelectorAll('[data-code-group="true"]').forEach((group) => {
    if (group.dataset.enhanced === 'true') return
    const panels = Array.from(group.querySelectorAll(':scope > pre'))
    if (panels.length === 0) {
      /* 空代码组（作者写了 ::: 但没放代码块）：不建页签，也不留一个空壳 */
      group.dataset.enhanced = 'true'
      return
    }

    group.dataset.enhanced = 'true'
    group.classList.add('code-group--tabs')
    group.replaceChildren()

    const instanceId = `code-group-${codeGroupId++}`
    group.dataset.codeGroupId = instanceId

    const toolbar = document.createElement('div')
    toolbar.className = 'code-group__toolbar'
    const tabs = document.createElement('div')
    tabs.className = 'code-group__tabs'
    tabs.setAttribute('role', 'tablist')
    tabs.setAttribute('aria-label', '代码语言')

    const panelHost = document.createElement('div')
    panelHost.className = 'code-group__panels'
    const groupLabels = codeGroupLabels(group)

    panels.forEach((pre, index) => {
      pre.dataset.enhanced = 'true'
      pre.hidden = index !== 0
      /* 标签优先用容器行的显式名称：各种工具会改写围栏 info，围栏语言不可靠 */
      const label = groupLabels[index] || languageLabel(fenceLanguage(pre))
      const tab = document.createElement('button')
      tab.type = 'button'
      tab.className = 'code-group__tab'
      tab.setAttribute('role', 'tab')
      tab.id = `${instanceId}-tab-${index}`
      tab.setAttribute('aria-controls', `${instanceId}-panel-${index}`)
      tab.setAttribute('aria-selected', index === 0 ? 'true' : 'false')
      tab.tabIndex = index === 0 ? 0 : -1
      tab.classList.toggle('is-active', index === 0)
      tab.textContent = label
      tab.dataset.codeGroupTab = instanceId
      tab.dataset.group = instanceId
      tab.dataset.index = String(index)
      tabs.append(tab)

      pre.id = `${instanceId}-panel-${index}`
      pre.setAttribute('role', 'tabpanel')
      pre.setAttribute('aria-labelledby', tab.id)
      pre.dataset.codeGroupPanel = instanceId
      pre.dataset.group = instanceId
      pre.dataset.index = String(index)
      panelHost.append(pre)
    })

    toolbar.append(tabs, createCopyButton(instanceId))
    group.append(toolbar, panelHost)
  })
}

/*
 * DOM 落地后的增强：代码组页签 + 独立代码块的标签/复制按钮。
 * 必须在 v-html 更新之后、且在内层元素存在时调用（组件里 await nextTick() 后调用）。
 * 幂等：重复调用不会重复建外壳。
 */
export function enhanceMarkdownDom(container) {
  if (!container) return
  enhanceCodeGroups(container)
  enhanceStandaloneCodeBlocks(container)
}

/*
 * 排版正文里的公式。
 * 失败（MathJax 资源加载不了）时**不抛出到调用方之外**的取舍在组件侧做：
 * 这里保留 throw，让调用方能区分「没有公式」与「排版失败」。
 */
export function typesetMarkdownMath(container) {
  return typesetMath(container)
}

/* 语言显示名：老调用点（以及 V1 的导出习惯）从这里也能拿到 */
export { languageLabel }
