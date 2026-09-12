<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { uploadMedia } from '@/api/media'
import { resolveVditorEditorHeight, setVditorFullscreenActive } from '@/lib/markdownEditorChrome'
import { useThemeStore } from '@/stores/theme'
import { createCodeGroupMarkdown, type CodeGroupBlock, validateCodeGroupBlocks } from '@/lib/markdownCodeGroup'
import { normalizePastedMath } from '@/lib/markdownMath'
import { prepareMathJax } from '@/lib/mathJax'
import '@/styles/math.css'

/**
 * CSDN-style Markdown editor (Vditor, IR instant-rendering mode) shared by
 * the blog / portfolio / chapter admin editors.
 *
 * - mode "ir": real-time inline preview — markdown renders as you type
 * - stores plain Markdown — the public render pipeline is untouched
 * - image upload goes through the existing media library API
 * - left-side document outline (like the reading-page TOC)
 * - dark theme follows the app theme store
 * - Vditor itself is dynamically imported so the ~1MB library stays in a
 *   lazy chunk loaded only when an editor page opens
 *
 * Admin shell notes:
 * - height MUST be numeric (not "auto"): with "auto", outline clicks call
 *   window.scrollTo, but the admin page scrolls .admin-shell__content.
 * - fullscreen must toggle html.is-vditor-fullscreen so CSS can hide the
 *   sidebar/header stacking context that otherwise covers the editor.
 */
const props = defineProps<{
  modelValue: string
  placeholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const theme = useThemeStore()

const host = ref<HTMLDivElement | null>(null)
const loadingError = ref('')

type VditorInstance = InstanceType<typeof import('vditor').default>

let vditor: VditorInstance | null = null
let suppressing = false
let ready = false
let fullscreenObserver: MutationObserver | null = null
let pasteHost: HTMLElement | null = null

// ---------------------------------------------------------------
// table-size picker (Vditor's built-in table button only inserts a
// fixed 3x3 template; we replace it with a custom size picker)
// ---------------------------------------------------------------
const TABLE_MAX_COLS = 20
const TABLE_MAX_ROWS = 50
const tablePickerOpen = ref(false)
const tableCols = ref(3)
const tableRows = ref(3)
const tablePickerRef = ref<HTMLElement | null>(null)

// ---------------------------------------------------------------
// code-group creator — source remains ordinary portable Markdown
// ---------------------------------------------------------------
const codeGroupOpen = ref(false)
const codeGroupError = ref('')
const codeGroupBlocks = ref<CodeGroupBlock[]>([])

function newCodeGroupBlock(index: number): CodeGroupBlock {
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

function removeCodeGroupBlock(index: number) {
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

watch(tablePickerOpen, (open) => {
  const onOutside = (event: PointerEvent) => {
    if (!tablePickerRef.value?.contains(event.target as Node)) {
      tablePickerOpen.value = false
    }
  }
  if (open) {
    document.addEventListener('pointerdown', onOutside)
  } else {
    document.removeEventListener('pointerdown', onOutside)
  }
})

function insertTable() {
  const cols = Math.min(TABLE_MAX_COLS, Math.max(1, Math.round(tableCols.value) || 3))
  const rows = Math.min(TABLE_MAX_ROWS, Math.max(1, Math.round(tableRows.value) || 3))
  const cell = (content: string) => ` ${content} `
  const header = '|' + Array.from({ length: cols }, (_, i) => cell(`列 ${i + 1}`)).join('|') + '|'
  const separator = '|' + Array.from({ length: cols }, () => cell('---')).join('|') + '|'
  const data = Array.from({ length: rows }, () => '|' + Array.from({ length: cols }, () => cell('')).join('|') + '|')
  const text = [header, separator, ...data].join('\n')
  tablePickerOpen.value = false
  vditor?.insertValue(text)
}

function syncTheme(): void {
  if (!vditor || !ready) return
  const dark = theme.resolved === 'dark'
  vditor.setTheme(dark ? 'dark' : 'classic', dark ? 'dark' : 'light')
}

function syncFullscreenFlag(element: Element): void {
  setVditorFullscreenActive(element.classList.contains('vditor--fullscreen'))
}

function watchFullscreen(element: Element): void {
  fullscreenObserver?.disconnect()
  syncFullscreenFlag(element)
  fullscreenObserver = new MutationObserver(() => syncFullscreenFlag(element))
  fullscreenObserver.observe(element, { attributes: true, attributeFilter: ['class'] })
}

function onEditorPaste(event: ClipboardEvent): void {
  const pasted = event.clipboardData?.getData('text/plain')
  if (!pasted) return
  const normalized = normalizePastedMath(pasted)
  if (normalized == null) return
  event.preventDefault()
  vditor?.insertValue(normalized)
}

function watchFormulaPaste(element: HTMLElement): void {
  pasteHost?.removeEventListener('paste', onEditorPaste, true)
  pasteHost = element
  // Capture phase runs before Vditor turns a formula into editor HTML.
  pasteHost.addEventListener('paste', onEditorPaste, true)
}

onMounted(async () => {
  const { default: Vditor } = await import('vditor')
  await import('vditor/dist/index.css')
  try {
    await prepareMathJax()
  } catch {
    loadingError.value = '公式预览资源加载失败，请刷新页面重试。未保存的正文不会因此被修改。'
    return
  }
  if (!host.value) return

  const editorHeight = resolveVditorEditorHeight(window.innerHeight)

  vditor = new Vditor(host.value, {
    mode: 'ir',
    value: props.modelValue ?? '',
    placeholder: props.placeholder ?? '',
    // Numeric height: outline sets scrollTop on .vditor-ir (not window).
    height: editorHeight,
    lang: 'zh_CN',
    theme: 'classic',
    // Keep the frozen "no raw HTML" rule in the editor preview too.
    // (Vditor's IMarkdownConfig has no html toggle; sanitize is the XSS gate.)
    preview: {
      math: { engine: 'MathJax', inlineDigit: true },
      markdown: { sanitize: true, mathBlockPreview: true },
    },
    cache: { enable: false },
    counter: { enable: false },
    fullscreen: { index: 10000 },
    // Left-side document outline, like the article TOC on the reading pages.
    outline: { enable: true, position: 'left' },
    toolbar: [
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
        icon: '<span aria-hidden="true">∑</span>',
        tip: '插入数学公式',
        click: () => vditor?.insertValue('\n$$\n\\frac{a}{b}\n$$\n'),
      },
      {
        name: 'code-group',
        icon: '<span aria-hidden="true">&lt;/&gt;</span>',
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
        icon: '<svg><use xlink:href="#vditor-icon-table"></use></svg>',
        tip: '表格（自定义行列）',
        click: () => openTablePicker(),
      },
      'upload',
      'link',
      '|',
      'edit-mode',
      'outline',
      'fullscreen',
    ],
    upload: {
      accept: 'image/*,.jpg,.jpeg,.png,.webp',
      max: 10 * 1024 * 1024,
      handler: (async (files: File[]) => {
        try {
          for (const file of files) {
            const asset = await uploadMedia(file)
            vditor?.insertValue(`![${asset.originalName}](${asset.publicUrl})`)
          }
          return null
        } catch {
          return '图片上传失败，请稍后重试。'
        }
      }) as IUpload['handler'],
    },
    input: (value: string) => {
      if (!suppressing) {
        emit('update:modelValue', value)
      }
    },
    after: () => {
      ready = true
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
})

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

watch(() => theme.resolved, syncTheme)

onBeforeUnmount(() => {
  ready = false
  fullscreenObserver?.disconnect()
  fullscreenObserver = null
  pasteHost?.removeEventListener('paste', onEditorPaste, true)
  pasteHost = null
  setVditorFullscreenActive(false)
  vditor?.destroy()
  vditor = null
})
</script>

<template>
  <div class="markdown-editor">
    <div ref="host" class="markdown-editor__host" />
    <p v-if="loadingError" role="alert">{{ loadingError }}</p>
    <div v-if="tablePickerOpen" ref="tablePickerRef" class="markdown-editor__table-picker">
      <label class="markdown-editor__table-field">
        <span>列数</span>
        <input v-model.number="tableCols" type="number" min="1" :max="TABLE_MAX_COLS" @keydown.enter.prevent="insertTable()" />
      </label>
      <label class="markdown-editor__table-field">
        <span>行数（数据行）</span>
        <input v-model.number="tableRows" type="number" min="1" :max="TABLE_MAX_ROWS" @keydown.enter.prevent="insertTable()" />
      </label>
      <button type="button" class="markdown-editor__table-insert" @click="insertTable()">插入</button>
    </div>
    <div v-if="codeGroupOpen" class="markdown-editor__code-dialog-backdrop" @click.self="codeGroupOpen = false">
      <section class="markdown-editor__code-dialog" role="dialog" aria-modal="true" aria-labelledby="code-group-title">
        <header>
          <div>
            <h2 id="code-group-title">创建代码组</h2>
            <p>名称会成为前台切换标签；可添加任意数量的代码块。</p>
          </div>
          <button type="button" class="markdown-editor__dialog-close" aria-label="关闭" @click="codeGroupOpen = false">×</button>
        </header>
        <p v-if="codeGroupError" class="markdown-editor__code-error" role="alert">{{ codeGroupError }}</p>
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
          <span />
          <button type="button" class="markdown-editor__dialog-secondary" @click="codeGroupOpen = false">取消</button>
          <button type="button" class="markdown-editor__table-insert" @click="insertCodeGroup()">插入代码组</button>
        </footer>
      </section>
    </div>
  </div>
</template>

<style scoped>
.markdown-editor {
  position: relative;
}

/*
 * Numeric Vditor height owns the scroll container (.vditor-ir etc.).
 * Do not put overflow on .vditor-content — outline click sets scrollTop
 * on the mode element only.
 */
.markdown-editor__host :deep(.vditor-content) {
  overflow: hidden;
}

.markdown-editor__host :deep(.vditor-ir),
.markdown-editor__host :deep(.vditor-wysiwyg),
.markdown-editor__host :deep(.vditor-sv) {
  overflow-y: auto;
}

/* ---------------------------------------------------------------
   Table-size picker (replaces Vditor's fixed 3x3 table insert)
   --------------------------------------------------------------- */
.markdown-editor__table-picker {
  position: absolute;
  top: 44px;
  left: 8px;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--bg-surface);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  box-shadow: 0 8px 24px rgb(0 0 0 / 0.14);
  white-space: nowrap;
}

.markdown-editor__table-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 11px;
  color: var(--text-muted);
}

.markdown-editor__table-field input {
  width: 64px;
  height: 26px;
  border: 1px solid var(--border-strong);
  border-radius: 4px;
  background: var(--bg-page);
  color: var(--text-primary);
  font-size: 13px;
  padding-inline: var(--space-2);
}

.markdown-editor__table-field input:focus {
  outline: none;
  border-color: var(--primary);
}

.markdown-editor__table-insert {
  border: none;
  background: var(--primary);
  color: var(--on-primary);
  font-size: 12px;
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
}

.markdown-editor__table-insert:hover {
  background: var(--primary-hover);
}

.markdown-editor__code-dialog-backdrop {
  position: fixed;
  z-index: 10020;
  inset: 0;
  display: grid;
  place-items: center;
  padding: var(--space-4);
  background: rgb(0 0 0 / 0.42);
}

.markdown-editor__code-dialog {
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

.markdown-editor__code-dialog header,
.markdown-editor__code-row-head,
.markdown-editor__code-dialog footer {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.markdown-editor__code-dialog header { justify-content: space-between; }
.markdown-editor__code-dialog h2 { margin: 0; font-size: 18px; }
.markdown-editor__code-dialog header p { margin: 4px 0 0; color: var(--text-muted); font-size: 13px; }
.markdown-editor__dialog-close { border: 0; background: transparent; color: var(--text-secondary); font-size: 24px; cursor: pointer; }
.markdown-editor__code-error { margin: var(--space-3) 0 0; color: var(--danger); }
.markdown-editor__code-list { display: grid; gap: var(--space-3); margin: var(--space-4) 0; overflow-y: auto; }
.markdown-editor__code-row { display: grid; grid-template-columns: 1fr 150px; gap: var(--space-3); padding: var(--space-3); border: 1px solid var(--border); border-radius: var(--radius-sm); }
.markdown-editor__code-row-head { grid-column: 1 / -1; justify-content: space-between; }
.markdown-editor__code-row-head button,
.markdown-editor__dialog-secondary { border: 1px solid var(--border-strong); border-radius: 4px; background: var(--bg-page); color: var(--text-primary); padding: 6px 10px; cursor: pointer; }
.markdown-editor__code-row label { display: grid; gap: 4px; color: var(--text-muted); font-size: 12px; }
.markdown-editor__code-row label:last-child { grid-column: 1 / -1; }
.markdown-editor__code-row input,
.markdown-editor__code-row textarea { width: 100%; box-sizing: border-box; border: 1px solid var(--border-strong); border-radius: 4px; background: var(--bg-page); color: var(--text-primary); font: 13px var(--font-mono, monospace); padding: var(--space-2); }
.markdown-editor__code-row textarea { resize: vertical; }
.markdown-editor__code-dialog footer { justify-content: flex-end; }
.markdown-editor__code-dialog footer > span { flex: 1; }

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

  .markdown-editor__host :deep(.vditor-toolbar__item) {
    flex: 0 0 auto;
  }

  .markdown-editor__host :deep(.vditor-outline) {
    display: none !important;
  }

  .markdown-editor__code-row { grid-template-columns: 1fr; }
}
</style>
