<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

/*
 * 后台 Markdown 编辑器（对标 V1 components/MarkdownEditor.vue 的工具栏 + 大纲面板）。
 *
 * V1 用的是 Vditor（IR 所见即所得），V2 不引入富文本/编辑器依赖，
 * 因此这里是「原生 textarea + 工具栏插入片段 + H1–H6 大纲」：
 *
 * 1. 插入一律走 document.execCommand('insertText')。
 *    这样插入会进入浏览器自己的撤销栈，工具栏的撤销/重做与 Ctrl+Z 是同一套历史，
 *    不会出现「按了撤销却把工具栏插入的内容留下」这种两套历史打架的情况。
 *    execCommand 已标记为废弃，但它是当前无依赖方案里唯一能做到这一点的 API；
 *    返回 false 时（浏览器不再支持）退回到直接改写 value 的手工插入。
 * 2. 大纲从正文里按 H1–H6 解析，跳过 ``` 围栏代码块内的「#」注释，
 *    点击大纲把光标移到该标题所在行（选区覆盖标题文字，浏览器会把选区滚进可视区），
 *    当前章节按光标位置高亮。
 * 3. 图片按钮不自己实现上传：由父页面通过 pickImage 注入媒体库选择器，
 *    与本模块既有的封面选择共用同一个 MediaPicker。
 */
const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  minHeight: { type: Number, default: 460 },
  // () => Promise<{ url, name } | null>，为空时不显示图片按钮
  pickImage: { type: Function, default: null },
})

const emit = defineEmits(['update:modelValue'])

const wrapRef = ref(null)
const textareaRef = ref(null)
const caret = ref(0)
const outlineVisible = ref(true)
const fullscreen = ref(false)
const tablePickerOpen = ref(false)
const tableCols = ref(3)
const tableRows = ref(3)

const TABLE_MAX_COLS = 20
const TABLE_MAX_ROWS = 50

// ---------------------------------------------------------------------
// 大纲
// ---------------------------------------------------------------------

/*
 * 从 Markdown 源文解析标题。
 * 逐行扫描而不是用全局正则：只有这样才能知道每个标题在源文里的字符偏移量，
 * 点击跳转与「当前章节」都依赖这个偏移量。
 * 围栏代码块（``` / ~~~）内的 # 是注释而不是标题，必须跳过。
 */
function parseHeadings(markdown) {
  const lines = String(markdown ?? '').split('\n')
  const result = []
  let offset = 0
  let fence = ''
  for (const line of lines) {
    const trimmed = line.trim()
    const fenceMatch = /^(```+|~~~+)/.exec(trimmed)
    if (fenceMatch) {
      if (!fence) fence = fenceMatch[1][0]
      else if (fence === fenceMatch[1][0]) fence = ''
    } else if (!fence) {
      const match = /^(#{1,6})\s+(.+?)\s*#*\s*$/.exec(trimmed)
      if (match) {
        const indent = line.length - line.trimStart().length
        result.push({ level: match[1].length, text: match[2], offset: offset + indent })
      }
    }
    offset += line.length + 1
  }
  return result
}

const headings = computed(() => parseHeadings(props.modelValue))

const currentHeadingIndex = computed(() => {
  const list = headings.value
  let index = -1
  for (let i = 0; i < list.length; i += 1) {
    if (list[i].offset <= caret.value) index = i
    else break
  }
  return index
})

function syncCaret() {
  const textarea = textareaRef.value
  if (textarea && document.activeElement === textarea) caret.value = textarea.selectionStart
}

function jumpToHeading(heading) {
  const textarea = textareaRef.value
  if (!textarea) return
  textarea.focus()
  textarea.setSelectionRange(heading.offset, heading.offset + heading.text.length)
  caret.value = heading.offset
}

// ---------------------------------------------------------------------
// 文本操作
// ---------------------------------------------------------------------

function currentText() {
  return textareaRef.value ? textareaRef.value.value : (props.modelValue ?? '')
}

function notify(value) {
  emit('update:modelValue', value)
}

/*
 * 用替换选区的方式写入文本。
 * selection 给出替换后要保留的选区（例如把光标放在 **粗体** 中间）。
 */
function replaceRange(start, end, text, selection) {
  const textarea = textareaRef.value
  if (!textarea) return
  textarea.focus()
  textarea.setSelectionRange(start, end)
  let inserted = false
  if (typeof document.execCommand === 'function') {
    try {
      inserted = document.execCommand('insertText', false, text)
    } catch {
      inserted = false
    }
  }
  if (!inserted) {
    // 退路：直接改写 value 并手工广播，保证功能可用（代价是不进浏览器撤销栈）
    const next = `${currentText().slice(0, start)}${text}${currentText().slice(end)}`
    textarea.value = next
    notify(next)
  }
  const [selStart, selEnd] = selection || [start + text.length, start + text.length]
  nextTick(() => {
    textarea.setSelectionRange(selStart, selEnd)
    caret.value = selStart
  })
}

// 行内包裹：加粗 / 斜体 / 删除线 / 行内代码 / 链接
function wrapSelection(prefix, suffix, placeholder) {
  const textarea = textareaRef.value
  if (!textarea) return
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selected = currentText().slice(start, end)
  const inner = selected || placeholder
  replaceRange(start, end, `${prefix}${inner}${suffix}`, [
    start + prefix.length,
    start + prefix.length + inner.length,
  ])
}

// 行块包裹：引用 / 代码块 —— 选中多行时逐行处理
function prefixLines(marker) {
  const textarea = textareaRef.value
  if (!textarea) return
  const text = currentText()
  const start = text.lastIndexOf('\n', Math.max(0, textarea.selectionStart - 1)) + 1
  let end = text.indexOf('\n', textarea.selectionEnd)
  if (end < 0) end = text.length
  const block = text.slice(start, end)
  const lines = block.split('\n')
  const allMarked = lines.every((line) => line.trim() === '' || line.startsWith(marker))
  const next = lines
    .map((line) => {
      if (allMarked) return line.startsWith(marker) ? line.slice(marker.length) : line
      return line.trim() === '' ? line : `${marker}${line}`
    })
    .join('\n')
  replaceRange(start, end, next, [start, start + next.length])
}

// 列表：无序 / 任务列表（同一前缀再次点击即取消，走 prefixLines 的开关语义）
function orderedList() {
  const textarea = textareaRef.value
  if (!textarea) return
  const text = currentText()
  const start = text.lastIndexOf('\n', Math.max(0, textarea.selectionStart - 1)) + 1
  let end = text.indexOf('\n', textarea.selectionEnd)
  if (end < 0) end = text.length
  const lines = text.slice(start, end).split('\n')
  const numbered = lines.some((line) => /^\d+\.\s/.test(line))
  let counter = 0
  const next = lines
    .map((line) => {
      const stripped = line.replace(/^\d+\.\s/, '')
      if (line.trim() === '') return line
      if (numbered) return stripped
      counter += 1
      return `${counter}. ${stripped}`
    })
    .join('\n')
  replaceRange(start, end, next, [start, start + next.length])
}

// 独立成块插入（分割线 / 表格 / 图片）：前后补空行，避免和相邻段落粘连
function insertBlock(text) {
  const textarea = textareaRef.value
  if (!textarea) return
  const value = currentText()
  const start = textarea.selectionStart
  const needLeading = start > 0 && value[start - 1] !== '\n'
  const needTrailing = value[start] && value[start] !== '\n'
  const payload = `${needLeading ? '\n' : ''}${text}${needTrailing ? '\n' : ''}`
  replaceRange(start, textarea.selectionEnd, payload)
}

function insertTable() {
  const cols = Math.min(TABLE_MAX_COLS, Math.max(1, Math.round(tableCols.value) || 3))
  const rows = Math.min(TABLE_MAX_ROWS, Math.max(1, Math.round(tableRows.value) || 3))
  const cell = (content) => ` ${content} `
  const header = `|${Array.from({ length: cols }, (_, i) => cell(`列 ${i + 1}`)).join('|')}|`
  const separator = `|${Array.from({ length: cols }, () => cell('---')).join('|')}|`
  const body = Array.from({ length: rows }, () => `|${Array.from({ length: cols }, () => cell('')).join('|')}|`)
  tablePickerOpen.value = false
  insertBlock([header, separator, ...body].join('\n'))
}

function insertLink() {
  wrapSelection('[', '](https://)', '链接文字')
}

async function insertImage() {
  if (!props.pickImage) {
    wrapSelection('![', '](https://)', '图片说明')
    return
  }
  const asset = await props.pickImage()
  if (!asset?.url) return
  insertBlock(`![${asset.name || '图片'}](${asset.url})`)
}

function historyStep(direction) {
  const textarea = textareaRef.value
  if (!textarea) return
  textarea.focus()
  if (typeof document.execCommand === 'function') document.execCommand(direction)
  nextTick(syncCaret)
}

async function toggleFullscreen() {
  const element = wrapRef.value
  if (!element) return
  try {
    if (!document.fullscreenElement) await element.requestFullscreen()
    else await document.exitFullscreen()
  } catch {
    // 浏览器拒绝（iframe 限制等）时保持原状，不弹错误打断写作
  }
}

function onFullscreenChange() {
  fullscreen.value = document.fullscreenElement === wrapRef.value
}

onMounted(() => document.addEventListener('fullscreenchange', onFullscreenChange))
onBeforeUnmount(() => document.removeEventListener('fullscreenchange', onFullscreenChange))

function onInput(event) {
  caret.value = event.target.selectionStart ?? 0
  notify(event.target.value)
}

function closeTablePicker(event) {
  if (!tablePickerOpen.value) return
  if (!wrapRef.value?.contains(event.target)) tablePickerOpen.value = false
}

document.addEventListener('pointerdown', closeTablePicker)
onBeforeUnmount(() => document.removeEventListener('pointerdown', closeTablePicker))

// ---------------------------------------------------------------------
// 工具栏定义（图标为内联 SVG，颜色跟随 currentColor）
// ---------------------------------------------------------------------

const ICONS = {
  undo: '<path d="M6 5 3 8l3 3"/><path d="M3 8h6.5a3.5 3.5 0 1 1 0 7H6"/>',
  redo: '<path d="M10 5l3 3-3 3"/><path d="M13 8H6.5a3.5 3.5 0 1 0 0 7H10"/>',
  bold: '<path d="M5 3h3.8a2.4 2.4 0 0 1 0 4.8H5z"/><path d="M5 7.8h4.4a2.6 2.6 0 0 1 0 5.2H5z"/>',
  italic: '<path d="M6.5 3h5"/><path d="M4.5 13h5"/><path d="M9.5 3 6.5 13"/>',
  strike: '<path d="M3 8h10"/><path d="M5.5 5.6C5.9 4.3 6.9 3.5 8.4 3.5c1.5 0 2.4.7 2.6 1.9"/><path d="M10.6 10.4c-.4 1.3-1.4 2.1-2.9 2.1-1.5 0-2.4-.7-2.6-1.9"/>',
  quote: '<path d="M3.5 4v8"/><path d="M6.5 6h6.5"/><path d="M6.5 10h4.5"/>',
  code: '<path d="M6 5 3 8l3 3"/><path d="M10 5l3 3-3 3"/>',
  link: '<path d="M6.8 9.2 9.2 6.8"/><path d="M7.6 4.6 9 3.2a2.6 2.6 0 0 1 3.7 3.7l-1.4 1.4"/><path d="M8.4 11.4 7 12.8a2.6 2.6 0 0 1-3.7-3.7l1.4-1.4"/>',
  list: '<path d="M6 4.5h7"/><path d="M6 8h7"/><path d="M6 11.5h7"/><circle cx="3.4" cy="4.5" r=".9"/><circle cx="3.4" cy="8" r=".9"/><circle cx="3.4" cy="11.5" r=".9"/>',
  ordered: '<path d="M6.5 4.5H13"/><path d="M6.5 8H13"/><path d="M6.5 11.5H13"/><path d="M3 3.2l1-.7v3.1"/><path d="M2.7 7.4c.2-.5.7-.8 1.2-.7.6.1.9.7.6 1.2L2.8 11h2"/>',
  task: '<rect x="2.4" y="2.6" width="11.2" height="10.8" rx="2"/><path d="M5.4 8.2l1.6 1.6 3.4-3.6"/>',
  divider: '<path d="M2.5 8h11"/>',
  image: '<rect x="2.4" y="3.4" width="11.2" height="9.2" rx="2"/><path d="M2.8 10.6 6 7.6l2.4 2.2 2-1.8 2.8 2.6"/><circle cx="6" cy="6" r="1"/>',
  table: '<rect x="2.4" y="3" width="11.2" height="10" rx="1.6"/><path d="M2.4 6.4h11.2"/><path d="M2.4 9.8h11.2"/><path d="M6.6 3v10"/><path d="M9.8 3v10"/>',
  outline: '<path d="M3 4h2"/><path d="M7 4h6"/><path d="M5 8h2"/><path d="M9 8h4"/><path d="M7 12h2"/><path d="M11 12h2"/>',
  fullscreen: '<path d="M6 2.6H2.6V6"/><path d="M10 2.6h3.4V6"/><path d="M6 13.4H2.6V10"/><path d="M10 13.4h3.4V10"/>',
}

const tools = computed(() => [
  { key: 'undo', label: '撤销', run: () => historyStep('undo') },
  { key: 'redo', label: '重做', run: () => historyStep('redo') },
  { divider: true },
  { key: 'bold', label: '加粗', run: () => wrapSelection('**', '**', '粗体文字') },
  { key: 'italic', label: '斜体', run: () => wrapSelection('*', '*', '斜体文字') },
  { key: 'strike', label: '删除线', run: () => wrapSelection('~~', '~~', '删除线文字') },
  { key: 'quote', label: '引用', run: () => prefixLines('> ') },
  { key: 'code', label: '行内代码', run: () => wrapSelection('`', '`', 'code') },
  { key: 'link', label: '链接', run: insertLink },
  { divider: true },
  { key: 'list', label: '无序列表', run: () => prefixLines('- ') },
  { key: 'ordered', label: '有序列表', run: orderedList },
  { key: 'task', label: '任务列表', run: () => prefixLines('- [ ] ') },
  { key: 'divider', label: '分割线', run: () => insertBlock('---') },
  { key: 'image', label: '图片', run: insertImage },
  { key: 'table', label: '表格', run: () => { tablePickerOpen.value = !tablePickerOpen.value } },
])

const wordCount = computed(() => {
  const text = String(props.modelValue ?? '').trim()
  if (!text) return 0
  // 中日韩字符按字计，其余按空白分词计 —— 与前台阅读页的字数口径一致
  const cjk = (text.match(/[\u4e00-\u9fff\u3400-\u4dbf]/g) || []).length
  const words = (text.replace(/[\u4e00-\u9fff\u3400-\u4dbf]/g, ' ').match(/[A-Za-z0-9_'-]+/g) || []).length
  return cjk + words
})

const lineCount = computed(() => (props.modelValue ? String(props.modelValue).split('\n').length : 0))
</script>

<template>
  <div ref="wrapRef" class="markdown-editor" :class="{ 'markdown-editor--no-outline': !outlineVisible }">
    <aside v-if="outlineVisible" class="markdown-editor__outline" aria-label="正文大纲">
      <p class="markdown-editor__outline-head">大纲</p>
      <template v-if="headings.length">
        <button
          v-for="(heading, index) in headings"
          :key="`${heading.offset}-${heading.text}`"
          type="button"
          class="markdown-editor__outline-item"
          :class="{ 'is-current': index === currentHeadingIndex }"
          :style="{ paddingLeft: `${8 + (heading.level - 1) * 12}px` }"
          :title="`H${heading.level} ${heading.text}`"
          @click="jumpToHeading(heading)"
        >{{ heading.text }}</button>
      </template>
      <p v-else class="markdown-editor__outline-empty">正文里还没有 H1–H6 标题。<br />用 ## 二级标题开始分节。</p>
    </aside>

    <div class="markdown-editor__main">
      <div class="markdown-editor__toolbar" role="toolbar" aria-label="Markdown 工具栏">
        <template v-for="(tool, index) in tools" :key="tool.key || `divider-${index}`">
          <span v-if="tool.divider" class="markdown-editor__divider" />
          <button
            v-else
            type="button"
            class="markdown-editor__tool"
            :class="{ 'is-active': tool.key === 'table' && tablePickerOpen }"
            :title="tool.label"
            :aria-label="tool.label"
            @click="tool.run()"
          >
            <svg viewBox="0 0 16 16" aria-hidden="true" v-html="ICONS[tool.key]" />
          </button>
        </template>

        <span class="markdown-editor__toolbar-spacer" />

        <button
          type="button"
          class="markdown-editor__tool"
          :class="{ 'is-active': outlineVisible }"
          title="显示 / 隐藏大纲"
          aria-label="显示或隐藏大纲"
          @click="outlineVisible = !outlineVisible"
        >
          <svg viewBox="0 0 16 16" aria-hidden="true" v-html="ICONS.outline" />
        </button>
        <button
          type="button"
          class="markdown-editor__tool"
          :class="{ 'is-active': fullscreen }"
          :title="fullscreen ? '退出全屏' : '全屏编辑'"
          aria-label="全屏编辑"
          @click="toggleFullscreen()"
        >
          <svg viewBox="0 0 16 16" aria-hidden="true" v-html="ICONS.fullscreen" />
        </button>
      </div>

      <textarea
        ref="textareaRef"
        class="markdown-editor__textarea"
        :value="modelValue"
        :placeholder="placeholder"
        :style="{ minHeight: `${minHeight}px` }"
        spellcheck="false"
        aria-label="Markdown 正文"
        @input="onInput"
        @keyup="syncCaret"
        @mouseup="syncCaret"
        @select="syncCaret"
        @click="syncCaret"
      />

      <div class="markdown-editor__footer">
        <span>{{ wordCount }} 字</span>
        <span>{{ lineCount }} 行</span>
        <span>{{ headings.length }} 个标题</span>
        <span v-if="currentHeadingIndex >= 0">当前：{{ headings[currentHeadingIndex].text }}</span>
      </div>
    </div>

    <div v-if="tablePickerOpen" class="markdown-editor__table-popover">
      <label class="markdown-editor__table-field">
        <span>列数</span>
        <input v-model.number="tableCols" type="number" min="1" :max="TABLE_MAX_COLS" @keydown.enter.prevent="insertTable()" />
      </label>
      <label class="markdown-editor__table-field">
        <span>行数（数据行）</span>
        <input v-model.number="tableRows" type="number" min="1" :max="TABLE_MAX_ROWS" @keydown.enter.prevent="insertTable()" />
      </label>
      <button type="button" class="primary-button" @click="insertTable()">插入</button>
    </div>
  </div>
</template>
