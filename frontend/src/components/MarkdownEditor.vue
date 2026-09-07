<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { uploadMedia } from '@/api/media'
import { useThemeStore } from '@/stores/theme'

/**
 * CSDN-style Markdown editor (Vditor, IR instant-rendering mode) shared by
 * the blog / portfolio / chapter admin editors.
 *
 * - mode "ir": real-time inline preview — markdown renders as you type
 *   (Typora-style), the most intuitive writing experience
 * - stores plain Markdown — the public render pipeline is untouched
 * - image upload goes through the existing media library API
 * - left-side document outline (like the reading-page TOC)
 * - dark theme follows the app theme store
 * - Vditor itself is dynamically imported so the ~1MB library stays in a
 *   lazy chunk loaded only when an editor page opens
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

type VditorInstance = InstanceType<typeof import('vditor').default>

let vditor: VditorInstance | null = null
let suppressing = false
let ready = false

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

onMounted(async () => {
  const { default: Vditor } = await import('vditor')
  await import('vditor/dist/index.css')
  if (!host.value) return

  vditor = new Vditor(host.value, {
    mode: 'ir',
    value: props.modelValue ?? '',
    placeholder: props.placeholder ?? '',
    height: 'auto',
    lang: 'zh_CN',
    theme: 'classic',
    // Keep the frozen "no raw HTML" rule in the editor preview too.
    // (Vditor's IMarkdownConfig has no html toggle; sanitize is the XSS gate.)
    preview: { math: false as never, markdown: { sanitize: true } },
    cache: { enable: false },
    counter: { enable: false },
    // Fullscreen must float above the admin shell (sidebar/header create
    // stacking contexts with z-index 10/30); Vditor's default index is 90.
    fullscreen: { index: 2000 },
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
  vditor?.destroy()
  vditor = null
})
</script>

<template>
  <div class="markdown-editor">
    <div ref="host" class="markdown-editor__host" />
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
  </div>
</template>

<style scoped>
/* Fill most of the viewport: long on large screens, floored on small.
   Vditor keeps its own internal scroll inside .vditor-content. */
.markdown-editor {
  position: relative;
}

.markdown-editor__host :deep(.vditor) {
  min-height: max(700px, calc(100vh - 300px));
}

/*
 * The scroll container MUST be the edit-mode element (.vditor-ir/.vditor-wysiwyg/
 * .vditor-sv), not .vditor-content: Vditor's outline click sets scrollTop on the
 * mode element, so scrolling anywhere else breaks outline navigation.
 */
.markdown-editor__host :deep(.vditor-content) {
  max-height: none;
}

.markdown-editor__host :deep(.vditor-ir),
.markdown-editor__host :deep(.vditor-wysiwyg),
.markdown-editor__host :deep(.vditor-sv) {
  max-height: max(780px, calc(100vh - 240px));
  overflow-y: auto;
}

/* Fullscreen: hand the layout back to Vditor entirely (it sizes the modes
   itself); our viewport-based caps would leave dead zones and misalign. */
.markdown-editor__host :deep(.vditor--fullscreen) {
  min-height: 0;
}

.markdown-editor__host :deep(.vditor--fullscreen .vditor-ir),
.markdown-editor__host :deep(.vditor--fullscreen .vditor-wysiwyg),
.markdown-editor__host :deep(.vditor--fullscreen .vditor-sv) {
  max-height: none;
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

@media (max-width: 720px) {
  .markdown-editor__host :deep(.vditor) {
    min-height: 540px;
  }

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

  .markdown-editor__host :deep(.vditor-ir),
  .markdown-editor__host :deep(.vditor-wysiwyg),
  .markdown-editor__host :deep(.vditor-sv) {
    max-height: 70vh;
  }
}
</style>
