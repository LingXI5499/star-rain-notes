<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { resolveVditorEditorHeight, setVditorFullscreenActive } from '../markdown/markdownEditorChrome'
import { createCodeGroupMarkdown, validateCodeGroupBlocks } from '../markdown/markdownCodeGroup'
import { normalizePastedMath } from '../markdown/markdownMath'
import { prepareMathJax } from '../markdown/mathJax'

/*
 * 文章正文编辑器 —— 对标 V1 `components/MarkdownEditor.vue`（Vditor，IR 即时渲染）。
 *
 * 为什么必须是 Vditor 而不是「textarea + 预览按钮」：V1 里正文的所见即所得**就是**预览，
 * 没有第二个预览区。IR 模式边写边排版（标题、代码块、表格、公式当场成形），
 * 作者不需要在「源码」和「效果」之间来回切换。
 *
 * 与 V1 逐条对齐的能力：
 *   - IR 即时渲染，存的是普通 Markdown（前台渲染管线完全不感知编辑器）
 *   - 左侧文档大纲（Vditor 自带），跟随正文标题实时更新，点击跳转并高亮当前项
 *   - 代码块：语言选择、语言标签、复制按钮
 *   - 代码组：一次插入多个可切换的代码块，源码仍是普通 Markdown（见 markdownCodeGroup.js）
 *   - 数学公式：$...$ / $$...$$，MathJax 排版；粘贴外部公式时做归一化
 *   - 表格：自定义行列数的尺寸选择器（Vditor 自带按钮只会插固定 3x3）
 *   - 图片：走媒体库选择器（由父页面注入 pickImage）
 *   - 全屏
 *
 * 两个必须遵守的约束（V1 注释里写明的坑）：
 *   1. 高度必须是**数值**，不能用 'auto'。用 auto 时 Vditor 的大纲点击会去滚动 window，
 *      而控制台的可滚动容器是 `.admin-shell__content`，点了大纲不会有任何反应。
 *   2. 全屏要切 `html.is-vditor-fullscreen`。侧栏 z-index 是 30，全屏的编辑器仍是它的
 *      后代节点，不藏外壳就会被侧栏盖住。
 *
 * 懒加载：Vditor（含样式）与 MathJax 全部是动态 import / 运行时注入 script，
 * 主包只多出「什么时候去加载」的几行代码。
 */
const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  // 编辑器高度下限（像素）。真实高度取「窗口高度 - 280」，但不低于这个值和 640
  minHeight: { type: Number, default: 460 },
  // () => Promise<{ url, name } | null>；为空时图片按钮退化为插入占位语法
  pickImage: { type: Function, default: null },
})

const emit = defineEmits(['update:modelValue'])

const host = ref(null)
const loadingError = ref('')

/* Vditor 实例。类型不上 TS，这里刻意保留为普通变量，JS 项目里加 JSDoc 类型反而更难维护 */
let vditor = null
/* 程序化写入（初始化 / 外部改值）期间不回发 update:modelValue，避免与 v-model 互相打环 */
let suppressing = false
let ready = false
let fullscreenObserver = null
let themeObserver = null
let pasteHost = null

/* ---------------------------------------------------------------
   表格尺寸选择器（替换 Vditor 自带的固定 3x3 插入）
   --------------------------------------------------------------- */
const TABLE_MAX_COLS = 20
const TABLE_MAX_ROWS = 50
const tablePickerOpen = ref(false)
const tableCols = ref(3)
const tableRows = ref(3)
const tablePickerRef = ref(null)

/* ---------------------------------------------------------------
   代码组创建器 —— 产出的是可移植的普通 Markdown
   --------------------------------------------------------------- */
const codeGroupOpen = ref(false)
const codeGroupError = ref('')
const codeGroupBlocks = ref([])

function newCodeGroupBlock(index) {
  return { label: `方案 ${index}`, language: 'typescript', content: '' }
}

function openCodeGroupCreator() {
  codeGroupError.value = ''
  codeGroupBlocks.value = [newCodeGroupBlock(1), newCodeGroupBlock(2)]
  codeGroupOpen.value = true
}

function addCodeGroupBlock() {
  codeGroupBlocks.value.push(newCodeGroupBlock(codeGroupBlocks.value.length + 1))
}

function removeCodeGroupBlock(index) {
  /* 至少留一个：空代码组是非法输入，与其让用户删到 0 再报错，不如禁止删到 0 */
  if (codeGroupBlocks.value.length <= 1) return
  codeGroupBlocks.value.splice(index, 1)
}

function insertCodeGroup() {
  const error = validateCodeGroupBlocks(codeGroupBlocks.value)
  if (error) {
    codeGroupError.value = error
    return
  }
  vditor?.insertValue(`\n${createCodeGroupMarkdown(codeGroupBlocks.value)}\n`)
  codeGroupOpen.value = false
}

function openTablePicker() {
  tablePickerOpen.value = true
}

/* 点空白处关闭表格选择器（与 V2 原来的行为一致，避免弹层留在页面上挡住正文） */
watch(tablePickerOpen, (open) => {
  const onOutside = (event) => {
    if (!tablePickerRef.value?.contains(event.target)) tablePickerOpen.value = false
  }
  if (open) document.addEventListener('pointerdown', onOutside)
  else document.removeEventListener('pointerdown', onOutside)
})

function insertTable() {
  const cols = Math.min(TABLE_MAX_COLS, Math.max(1, Math.round(tableCols.value) || 3))
  const rows = Math.min(TABLE_MAX_ROWS, Math.max(1, Math.round(tableRows.value) || 3))
  const cell = (content) => ` ${content} `
  const header = `|${Array.from({ length: cols }, (_, i) => cell(`列 ${i + 1}`)).join('|')}|`
  const separator = `|${Array.from({ length: cols }, () => cell('---')).join('|')}|`
  const body = Array.from({ length: rows }, () => `|${Array.from({ length: cols }, () => cell('')).join('|')}|`)
  tablePickerOpen.value = false
  /* 与 V1 一致：表格不加前后换行。加换行会在「行内插入」时把当前段落切断 */
  vditor?.insertValue([header, separator, ...body].join('\n'))
}

/* 图片：走父页面注入的媒体库选择器；没有注入时插入一段可手填的占位语法 */
async function insertImage() {
  if (!props.pickImage) {
    vditor?.insertValue('![图片说明](https://)')
    return
  }
  const asset = await props.pickImage()
  if (!asset?.url) return
  /* alt 里的方括号/换行会把 Markdown 图片语法拆坏，插入前先抹平 */
  const alt = String(asset.name || '图片').replace(/[[\]\r\n]/g, ' ').trim() || '图片'
  vditor?.insertValue(`![${alt}](${asset.url})`)
}

/* ---------------------------------------------------------------
   主题 / 全屏
   --------------------------------------------------------------- */

function isDarkTheme() {
  return document.documentElement.dataset.theme === 'dark'
}

/*
 * Vditor 的编辑器与内容主题是两个参数：编辑器（工具栏/边框）用 classic/dark，
 * 正文排版用 light/dark。两者必须同时切，否则深色下会出现白底工具栏配深色代码块。
 */
function syncTheme() {
  if (!vditor || !ready) return
  const dark = isDarkTheme()
  vditor.setTheme(dark ? 'dark' : 'classic', dark ? 'dark' : 'light')
}

function syncFullscreenFlag(element) {
  setVditorFullscreenActive(element.classList.contains('vditor--fullscreen'))
}

/* Vditor 全屏只是给它自己加 class，没有事件回调，因此用 MutationObserver 盯 class */
function watchFullscreen(element) {
  fullscreenObserver?.disconnect()
  syncFullscreenFlag(element)
  fullscreenObserver = new MutationObserver(() => syncFullscreenFlag(element))
  fullscreenObserver.observe(element, { attributes: true, attributeFilter: ['class'] })
}

/* 控制台的主题开关写的是 <html data-theme>，这里跟着它走，不额外引主题 store */
function watchTheme() {
  themeObserver?.disconnect()
  themeObserver = new MutationObserver(syncTheme)
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
}

/* ---------------------------------------------------------------
   粘贴归一化：从网页 / 其它编辑器复制来的公式要能变成合法语法
   --------------------------------------------------------------- */

function onEditorPaste(event) {
  const pasted = event.clipboardData?.getData('text/plain')
  if (!pasted) return
  const normalized = normalizePastedMath(pasted)
  /* null 表示「不是公式」，原样交给 Vditor 的默认粘贴流程 */
  if (normalized == null) return
  event.preventDefault()
  vditor?.insertValue(normalized)
}

/*
 * 用捕获阶段监听：必须先于 Vditor 自己的 paste 处理拿到事件，
 * 否则（WYSIWYG 模式下）它已经把内容落进 DOM 了，再 preventDefault 也来不及。
 */
function watchFormulaPaste(element) {
  pasteHost?.removeEventListener('paste', onEditorPaste, true)
  pasteHost = element
  pasteHost.addEventListener('paste', onEditorPaste, true)
}

/* ---------------------------------------------------------------
   工具栏
   --------------------------------------------------------------- */

/*
 * 自定义按钮用**自包含的内联 SVG**，不引用 Vditor 的图标雪碧图：
 * 雪碧图是 Vditor 按 options.icon 从 CDN 动态加载的，离线环境下会缺图标，
 * 自定义按钮如果依赖它就会变成空白方块。
 */
const ICON_MATH = '<span aria-hidden="true" style="font:600 15px/1 var(--font-family);">∑</span>'
const ICON_CODE_GROUP = '<span aria-hidden="true" style="font:600 12px/1 var(--font-family);">&lt;/&gt;</span>'
const ICON_TABLE = '<svg viewBox="0 0 16 16" aria-hidden="true" style="width:16px;height:16px;"><rect x="1.6" y="2.6" width="12.8" height="10.8" rx="1.6" fill="none" stroke="currentColor" stroke-width="1.3"/><path d="M1.6 6.2h12.8M1.6 9.8h12.8M6.2 2.6v10.8" fill="none" stroke="currentColor" stroke-width="1.3"/></svg>'
const ICON_IMAGE = '<svg viewBox="0 0 16 16" aria-hidden="true" style="width:16px;height:16px;"><rect x="1.6" y="3" width="12.8" height="10" rx="1.6" fill="none" stroke="currentColor" stroke-width="1.3"/><path d="M2.4 11.4 5.6 8.2l2.4 2.2 1.9-1.7 2.9 2.6" fill="none" stroke="currentColor" stroke-width="1.3"/><circle cx="5.7" cy="5.9" r="1" fill="currentColor"/></svg>'

function buildToolbar() {
  return [
    'undo',
    'redo',
    '|',
    'headings',
    'bold',
    'italic',
    'strike',
    'code',
    'inline-code',
    {
      name: 'math-formula',
      icon: ICON_MATH,
      tip: '插入数学公式',
      click: () => vditor?.insertValue('\n$$\n\\frac{a}{b}\n$$\n'),
    },
    {
      name: 'code-group',
      icon: ICON_CODE_GROUP,
      tip: '插入可切换代码组',
      click: () => openCodeGroupCreator(),
    },
    '|',
    'list',
    'ordered-list',
    'check',
    'quote',
    'line',
    {
      name: 'table-size',
      icon: ICON_TABLE,
      tip: '表格（自定义行列）',
      click: () => openTablePicker(),
    },
    {
      name: 'image-library',
      icon: ICON_IMAGE,
      tip: '插入图片（媒体库）',
      click: () => { void insertImage() },
    },
    'link',
    '|',
    'outline',
    'fullscreen',
  ]
}

/* 工具栏按钮数量（'|' 是分隔符，不算按钮）：验收脚本读它确认工具栏没有被裁掉过 */
const TOOLBAR_ITEM_COUNT = buildToolbar().filter((item) => item !== '|').length
defineExpose({
  TOOLBAR_ITEM_COUNT,
  getMarkdown: () => (ready && vditor ? vditor.getValue() : props.modelValue),
})

/* ---------------------------------------------------------------
   生命周期
   --------------------------------------------------------------- */

async function mountEditor() {
  /* Vditor 与它的样式表都是动态加载：~1MB 的库不进主包，只有打开编辑器页才下载 */
  const { default: Vditor } = await import('vditor')
  await import('vditor/dist/index.css')
  try {
    /* MathJax 也要先就绪：Vditor 认 protyleMathJaxScript 这个 id，就不再自己去请求 CDN */
    await prepareMathJax()
  } catch {
    /*
     * V1 的取舍：公式资源加载不了就不开编辑器，并明确告诉作者正文没有被修改。
     * 这里保留同样的取舍 —— 一个「公式位置全是原始 TeX」的编辑器比打不开更容易让人误存。
     */
    loadingError.value = '公式资源加载失败，编辑器未启动。请刷新页面重试；未保存的正文不会因此被修改。'
    return
  }
  if (!host.value) return

  /* 数值高度：见文件头约束 1 */
  const editorHeight = Math.max(resolveVditorEditorHeight(window.innerHeight), props.minHeight || 0)

  vditor = new Vditor(host.value, {
    mode: 'ir',
    value: props.modelValue ?? '',
    placeholder: props.placeholder ?? '',
    height: editorHeight,
    lang: 'zh_CN',
    theme: isDarkTheme() ? 'dark' : 'classic',
    /*
     * sanitize: true 是编辑器侧的 XSS 闸门。
     * Vditor 的 IMarkdownConfig 没有 html 开关，无法像 markdown-it 那样直接关掉原生 HTML，
     * 因此编辑器预览里的原生 HTML 靠这里过滤（前台渲染则连解析都不做，见 support/markdown.js）。
     */
    preview: {
      math: { engine: 'MathJax', inlineDigit: true },
      markdown: { sanitize: true, mathBlockPreview: true },
    },
    /* 不用 Vditor 的本地草稿缓存：正文的唯一真源是后端的 bodyMarkdown */
    cache: { enable: false },
    counter: { enable: false },
    fullscreen: { index: 10000 },
    /* 左侧文档大纲（V1 的「像阅读页目录一样」） */
    outline: { enable: true, position: 'left' },
    toolbar: buildToolbar(),
    input: (value) => {
      if (!suppressing) emit('update:modelValue', value)
    },
    after: () => {
      ready = true
      /* 初始化期间 Vditor 可能规范化过正文（例如补上结尾换行），以组件收到的值为准再对齐一次 */
      if (vditor && vditor.getValue() !== props.modelValue) {
        suppressing = true
        vditor.setValue(props.modelValue ?? '')
        suppressing = false
      }
      syncTheme()
      if (host.value) {
        watchFullscreen(host.value)
        watchFormulaPaste(host.value)
      }
    },
  })
}

onMounted(() => {
  watchTheme()
  void mountEditor()
})

/* 外部改值（加载文章、切换文章）→ 写回编辑器，写回期间不回发事件 */
watch(
  () => props.modelValue,
  (value) => {
    if (!vditor || !ready) return
    const current = vditor.getValue()
    if (value !== current) {
      suppressing = true
      vditor.setValue(value ?? '')
      suppressing = false
    }
  },
)

onBeforeUnmount(() => {
  ready = false
  fullscreenObserver?.disconnect()
  fullscreenObserver = null
  themeObserver?.disconnect()
  themeObserver = null
  pasteHost?.removeEventListener('paste', onEditorPaste, true)
  pasteHost = null
  setVditorFullscreenActive(false)
  vditor?.destroy()
  vditor = null
})

/* ---------------------------------------------------------------
   底部字数统计（V2 既有能力，保留）
   标题数不在这里算：左侧大纲就是标题清单，再解析一遍正文只会多出一份会漂移的实现。
   --------------------------------------------------------------- */
const wordCount = computed(() => {
  const text = String(props.modelValue ?? '').trim()
  if (!text) return 0
  /* 中日韩字符按字计，其余按空白分词计 —— 与前台阅读页的字数口径一致 */
  const cjk = (text.match(/[\u4e00-\u9fff\u3400-\u4dbf]/g) || []).length
  const words = (text.replace(/[\u4e00-\u9fff\u3400-\u4dbf]/g, ' ').match(/[A-Za-z0-9_'-]+/g) || []).length
  return cjk + words
})

const lineCount = computed(() => (props.modelValue ? String(props.modelValue).split('\n').length : 0))
</script>

<template>
  <div class="markdown-editor">
    <div ref="host" class="markdown-editor__host" />
    <p v-if="loadingError" class="markdown-editor__error" role="alert">{{ loadingError }}</p>

    <div v-if="tablePickerOpen" ref="tablePickerRef" class="markdown-editor__popover">
      <label class="markdown-editor__popover-field">
        <span>列数</span>
        <input v-model.number="tableCols" type="number" min="1" :max="TABLE_MAX_COLS" @keydown.enter.prevent="insertTable()" />
      </label>
      <label class="markdown-editor__popover-field">
        <span>行数（数据行）</span>
        <input v-model.number="tableRows" type="number" min="1" :max="TABLE_MAX_ROWS" @keydown.enter.prevent="insertTable()" />
      </label>
      <button type="button" class="primary-button" @click="insertTable()">插入</button>
    </div>

    <div v-if="codeGroupOpen" class="markdown-editor__dialog-backdrop" @click.self="codeGroupOpen = false">
      <section class="markdown-editor__dialog" role="dialog" aria-modal="true" aria-labelledby="code-group-title">
        <header>
          <div>
            <h2 id="code-group-title">创建代码组</h2>
            <p>名称会成为前台切换标签；可添加任意数量的代码块，源码仍是普通 Markdown。</p>
          </div>
          <button type="button" class="markdown-editor__dialog-close" aria-label="关闭" @click="codeGroupOpen = false">×</button>
        </header>

        <p v-if="codeGroupError" class="error" role="alert">{{ codeGroupError }}</p>

        <div class="markdown-editor__code-list">
          <article v-for="(block, index) in codeGroupBlocks" :key="index" class="markdown-editor__code-row">
            <div class="markdown-editor__code-row-head">
              <strong>代码块 {{ index + 1 }}</strong>
              <button type="button" :disabled="codeGroupBlocks.length === 1" @click="removeCodeGroupBlock(index)">移除</button>
            </div>
            <label><span>显示名称</span><input v-model="block.label" maxlength="80" placeholder="例如：迭代写法" /></label>
            <label><span>语言</span><input v-model="block.language" maxlength="32" placeholder="例如：typescript" /></label>
            <label><span>代码</span><textarea v-model="block.content" rows="5" spellcheck="false" /></label>
          </article>
        </div>

        <footer>
          <button type="button" class="markdown-editor__dialog-secondary" @click="addCodeGroupBlock()">添加代码块</button>
          <span class="markdown-editor__dialog-spacer" />
          <button type="button" class="markdown-editor__dialog-secondary" @click="codeGroupOpen = false">取消</button>
          <button type="button" class="primary-button" @click="insertCodeGroup()">插入代码组</button>
        </footer>
      </section>
    </div>

    <div v-if="!loadingError" class="markdown-editor__footer">
      <span>{{ wordCount }} 字</span>
      <span>{{ lineCount }} 行</span>
    </div>
  </div>
</template>

<style scoped>
.markdown-editor {
  position: relative;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  overflow: hidden;
}

/*
 * 数值高度由 Vditor 自己持有，滚动条归它内部的模式元素（.vditor-ir / .vditor-wysiwyg / .vditor-sv）。
 * 因此外层不能加 overflow-y —— 否则大纲点击设置的 scrollTop 会落在一个不滚动的元素上。
 */
.markdown-editor__host :deep(.vditor-content) {
  overflow: hidden;
}

.markdown-editor__host :deep(.vditor-ir),
.markdown-editor__host :deep(.vditor-wysiwyg),
.markdown-editor__host :deep(.vditor-sv) {
  overflow-y: auto;
}

.markdown-editor__error {
  padding: var(--space-3) var(--space-4);
  color: var(--danger);
  font-size: 13px;
}

.markdown-editor__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 7px 12px;
  border-top: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 11px;
}

/* --- 表格尺寸选择器（替换 Vditor 的固定 3x3） --- */
.markdown-editor__popover {
  position: absolute;
  top: 44px;
  left: 8px;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--bg-surface);
  box-shadow: 0 8px 24px rgb(0 0 0 / 0.14);
  white-space: nowrap;
}

.markdown-editor__popover-field {
  display: grid;
  gap: 2px;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 500;
}

.markdown-editor__popover-field input {
  width: 64px;
  min-height: 26px;
  padding-inline: var(--space-2);
  border-radius: 4px;
  background: var(--bg-page);
  font-size: 13px;
}

/* --- 代码组创建器 --- */
.markdown-editor__dialog-backdrop {
  position: fixed;
  inset: 0;
  /* 必须高于 Vditor 全屏的 10000：全屏状态下也要能弹出创建器 */
  z-index: 10020;
  display: grid;
  place-items: center;
  padding: var(--space-4);
  background: rgb(0 0 0 / 0.42);
}

.markdown-editor__dialog {
  display: flex;
  flex-direction: column;
  width: min(760px, 100%);
  max-height: min(800px, calc(100vh - 32px));
  padding: var(--space-5);
  overflow: hidden;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  box-shadow: 0 20px 48px rgb(0 0 0 / 0.28);
}

.markdown-editor__dialog header,
.markdown-editor__code-row-head,
.markdown-editor__dialog footer {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.markdown-editor__dialog header { justify-content: space-between; }
.markdown-editor__dialog h2 { margin: 0; font-size: 18px; }
.markdown-editor__dialog header p { margin: 4px 0 0; color: var(--text-muted); font-size: 13px; }

.markdown-editor__dialog-close {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 24px;
  cursor: pointer;
}

.markdown-editor__code-list {
  display: grid;
  gap: var(--space-3);
  margin: var(--space-4) 0;
  overflow-y: auto;
}

.markdown-editor__code-row {
  display: grid;
  grid-template-columns: 1fr 150px;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}

.markdown-editor__code-row-head { grid-column: 1 / -1; justify-content: space-between; }

.markdown-editor__code-row-head button,
.markdown-editor__dialog-secondary {
  padding: 6px 10px;
  border: 1px solid var(--border-strong);
  border-radius: 4px;
  background: var(--bg-page);
  color: var(--text-primary);
  cursor: pointer;
}

.markdown-editor__code-row-head button:disabled { opacity: 0.5; cursor: not-allowed; }

.markdown-editor__code-row label {
  display: grid;
  gap: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

/* 代码输入框横跨整行：代码要宽，名称与语言各占一列即可 */
.markdown-editor__code-row label:last-child { grid-column: 1 / -1; }

.markdown-editor__code-row input,
.markdown-editor__code-row textarea {
  width: 100%;
  box-sizing: border-box;
  padding: var(--space-2);
  border: 1px solid var(--border-strong);
  border-radius: 4px;
  background: var(--bg-page);
  color: var(--text-primary);
  font: 13px / 1.6 ui-monospace, SFMono-Regular, Menlo, monospace;
}

.markdown-editor__code-row textarea { resize: vertical; }

.markdown-editor__dialog footer { justify-content: flex-end; }
.markdown-editor__dialog-spacer { flex: 1; }

/* 窄屏：工具栏横向滚动（不换行，避免吃掉正文高度），大纲收起 */
@media (max-width: 720px) {
  .markdown-editor__host :deep(.vditor-toolbar) {
    display: flex !important;
    flex-wrap: nowrap !important;
    justify-content: flex-start;
    overflow-x: auto;
    overflow-y: hidden;
    scrollbar-width: thin;
    padding-left: 0 !important;
  }

  .markdown-editor__host :deep(.vditor-toolbar__item) { flex: 0 0 auto; }
  .markdown-editor__host :deep(.vditor-outline) { display: none !important; }
  .markdown-editor__code-row { grid-template-columns: 1fr; }
}
</style>
