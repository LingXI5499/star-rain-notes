/**
 * markdownCodeGroup.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 主要依赖：markdown-it-container。
 * 阅读入口：createCodeGroupBlock（创建代码分组区块）；languageLabel（language标签的处理函数）；parseCodeGroupLabels（解析代码分组Labels）；validateCodeGroupBlocks（校验代码分组Blocks）；safeLanguage（安全Language的处理函数）。
 */

import markdownItContainer from 'markdown-it-container'

/*
 * 代码组（`::: code-group`）—— 从 V1 `lib/markdownCodeGroup.ts` 完整移植。
 *
 * 设计要点（与 V1 逐条对齐，改动会同时影响编辑器插入与前台渲染）：
 *   1. 源码仍然是**可移植的普通 Markdown**：一段 `::: code-group 标签1|标签2` 容器，
 *      里面放若干个普通围栏代码块。作者把这个片段粘到别的 Markdown 工具里，
 *      最坏结果是容器语法原样显示，代码本身不会丢。
 *   2. 显示标签写在容器开头上（`|` 分隔），不由围栏语言推导：
 *      Vditor / 各种格式化工具会改写围栏的 info 字符串，标签放围栏上会被吃掉；
 *      放容器行上则只有本解析器关心，最稳。
 *   3. 标签用 JSON 编码塞进 `data-code-labels`：自定义名称里可能有逗号，
 *      逗号分隔的旧写法会把它拆成两个标签。`data-code-langs` 保留是为了兼容
 *      已经渲染过 / 已被外部消费的旧标记，新代码只读 data-code-labels。
 *   4. 省略标签时（裸 `::: code-group`）不输出 data 属性，由渲染层回退到围栏语言标签。
 */

/* 常见围栏语言 → 显示名（LeetCode 风格）。与渲染器注册的 highlight.js 语言是两份清单：
   这份只管「显示成什么」，那份管「能不能着色」，故意不合并。 */
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

/* 编辑器「创建代码组」弹窗里的一行 */
export
  /**
   * 创建代码分组区块。
   * @param label 标签；默认值、解构字段与可选参数见函数签名。
   * @param language language；默认值、解构字段与可选参数见函数签名。
   * @param content 内容；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function createCodeGroupBlock(label, language, content) {
  return { label, language, content }
}

export
  /**
   * language标签的处理函数。
   * 主要协作：.toLowerCase、.trim。
   * @param lang lang；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function languageLabel(lang) {
  const key = String(lang ?? '').trim().toLowerCase()
  if (!key || key === 'code') return 'Code'
  return LANG_LABELS[key] ?? String(lang).trim()
}

/*
 * 从 `::: code-group C|Java|Python` 解析标签。
 *
 * `|` 是规范分隔符；只有在整串里没有 `|` 时才按逗号切 —— 这是为了兼容
 * 「命名代码组」这个功能上线之前写下的 `::: code-group C,Java` 老文章。
 * 反过来（先切逗号再切竖线）会把 `方案, 一|方案二` 这种带逗号的自定义名称拆坏。
 */
export
  /**
   * 解析代码分组Labels。
   * 主要协作：.match、.trim、matched..includes、.filter、.map。
   * @param info info；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function parseCodeGroupLabels(info) {
  const matched = String(info ?? '').trim().match(/^code-group(?:\s+(.+))?$/)
  if (!matched?.[1]) return []
  const delimiter = matched[1].includes('|') ? '|' : ','
  return matched[1]
    .split(delimiter)
    .map((part) => part.trim())
    .filter(Boolean)
}

/* 兼容旧调用点与旧测试的别名（V1 也保留了这个导出） */
export const parseCodeGroupLangs = parseCodeGroupLabels

/*
 * 校验编辑器里填的代码组。
 * 返回错误文案或 null —— 返回文案而不是抛异常，是为了让弹窗能把它显示在对话框里，
 * 而不是炸掉整个编辑器。
 */
export
  /**
   * 校验代码分组Blocks。
   * 主要协作：.trim、label.includes、label.toLocaleLowerCase、labels.has、labels.add。
   * @param blocks blocks；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function validateCodeGroupBlocks(blocks) {
  if (!blocks || blocks.length === 0) return '请至少添加一个代码块。'
  const labels = new Set()
  for (const block of blocks) {
    const label = String(block.label ?? '').trim()
    if (!label) return '每个代码块都需要名称。'
    /* 竖线是标签分隔符，出现在名称里会把一个标签切成两个，必须挡住而不是转义 */
    if (label.includes('|')) return '代码块名称不能包含竖线（|）。'
    const key = label.toLocaleLowerCase()
    if (labels.has(key)) return `代码块名称“${label}”重复。`
    labels.add(key)
  }
  return null
}

/* 语言串只允许 [\w+-]（含 C++ / C#）：其余一律降级成 text，避免围栏 info 被注入奇怪内容 */
/**
 * 安全Language的处理函数。
 * 主要协作：.trim、.test。
 * @param language language；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function safeLanguage(language) {
  const normalized = String(language ?? '').trim()
  return /^[\w+-]+$/.test(normalized) ? normalized : 'text'
}

/*
 * 计算包裹内容的围栏长度。
 * 内容里如果本身有 ``` （例如讲 Markdown 的文章），三反引号围栏会被提前闭合，
 * 因此围栏必须比内容里最长的一串反引号再长一位。
 */
function fenceFor(content) {
  const runs = String(content).match(/`+/g) ?? []
  const longest = runs.reduce((length, run) => Math.max(length, run.length), 0)
  return '`'.repeat(Math.max(3, longest + 1))
}

/* 生成编辑器要插入的 Markdown 文本（源码仍然是普通 Markdown，见文件头第 1 点） */
export
  /**
   * 创建代码分组Markdown。
   * 主要协作：validateCodeGroupBlocks、.join、blocks.map、.trim、.replace。
   * 显式失败通过异常交给调用方处理，不在此处伪造成功结果。
   * @param blocks blocks；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function createCodeGroupMarkdown(blocks) {
  const error = validateCodeGroupBlocks(blocks)
  if (error) throw new Error(error)
  const labelList = blocks.map((block) => String(block.label).trim()).join('|')
  /*
   * 单个代码块时补一个尾部分隔符 `标签|`：
   * 这样「只有一个块、且名称里有逗号」的情况会走 `|` 分支，
   * 名称里的逗号才不会被当成标签分隔符。（V1 的这个细节必须保留。）
   */
  const labels = blocks.length === 1 ? `${labelList}|` : labelList
  const panels = blocks.map((block) => {
    const content = String(block.content ?? '').replace(/\r\n/g, '\n').replace(/\r/g, '\n')
    const fence = fenceFor(content)
    return `${fence}${safeLanguage(block.language)}\n${content}\n${fence}`
  })
  return `::: code-group ${labels}\n\n${panels.join('\n\n')}\n\n:::\n`
}

/* data 属性走 HTML 转义后仍是合法 JSON（JSON 里的 " 会变成 &quot;），
   浏览器解析属性时会还原，渲染层 JSON.parse(dataset.codeLabels) 拿到原始数组 */
/**
 * 转义Attr。
 * 主要协作：.replace。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function escapeAttr(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
}

/**
 * 编码Labels。
 * 主要协作：escapeAttr、JSON.stringify。
 * @param labels labels；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function encodeLabels(labels) {
  return escapeAttr(JSON.stringify(labels))
}

/*
 * 把 `::: code-group` 容器渲染成一个带 data 属性的外壳 div。
 *
 * 这里**只产出外壳**，不产出页签：真正的页签、面板切换与复制按钮由
 * markdown.js 的 enhanceMarkdownDom 在 DOM 落地之后构建 —— 页签需要事件与
 * hidden 状态，写在 HTML 字符串里还得再做一遍 DOM 查询，不如一次做完。
 */
export
  /**
   * useMarkdown代码分组的处理函数。
   * 主要协作：md.use、.test、.trim、parseCodeGroupLabels、encodeLabels。
   * @param md md；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function useMarkdownCodeGroup(md) {
  md.use(markdownItContainer, 'code-group', {
    validate: (params) => /^code-group(?:\s+.+)?$/.test(String(params).trim()),
    render: (tokens, idx) => {
      if (tokens[idx].nesting === 1) {
        const labels = parseCodeGroupLabels(tokens[idx].info ?? '')
        const attr = labels.length
          ? ` data-code-labels="${encodeLabels(labels)}" data-code-langs="${escapeAttr(labels.join(','))}"`
          : ''
        return `<div class="code-group" data-code-group="true"${attr}>\n`
      }
      return '</div>\n'
    },
  })
}
