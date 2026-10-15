<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { appendMarkdown, downloadBlob, exportName, readMarkdownFile } from './export/markdownFiles'
import { outlineSections } from './commands/editorStructure'
import { normalizeVditorMarkdown } from './commands/editorDocument'
const props = defineProps({ adapter: Object, context: Object, ready: Boolean, readOnly: Boolean, compact: Boolean })
const emit = defineEmits(['apply'])
const input = ref(null),
  notice = ref(''),
  incoming = ref(null),
  original = ref(''),
  choice = ref('replace'),
  busy = ref(false),
  filename = ref('')
const importedHeadings = computed(() =>
  incoming.value === null ? 0 : outlineSections(incoming.value).length,
)
let generation = 0,
  active = true,
  cleanupPrint = null
const current = () => normalizeVditorMarkdown(props.adapter.markdown())
const explanation=ref(false)
function openImport(){if(props.ready&&!props.readOnly&&!busy.value)input.value?.click()}
defineExpose({openImport,exportFile,busy,showHelp:()=>{explanation.value=true}})
async function selected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const n = ++generation
  busy.value = true
  notice.value = ''
  try {
    const source = await readMarkdownFile(file)
    if (n !== generation || !active) return
    original.value = current()
    incoming.value = source
    filename.value = file.name
    choice.value = 'replace'
  } catch (error) {
    if (n === generation) notice.value = error.message
  } finally {
    if (n === generation) busy.value = false
  }
}
function apply() {
  if (props.readOnly || !props.ready) return
  if (current() !== original.value) {
    notice.value = '当前正文在预览期间发生变化，请重新选择文件并核对；未导入。'
    return
  }
  emit(
    'apply',
    choice.value === 'append' ? appendMarkdown(original.value, incoming.value) : incoming.value,
  )
  incoming.value = null
}
async function exportFile(kind) {
  if (busy.value || !props.ready) return
  busy.value = true
  notice.value = ''
  const n = generation,
    source = current()
  try {
    if (kind === 'md')
      downloadBlob(new Blob([source], { type: 'text/markdown;charset=utf-8' }), exportName(source))
    else if (kind === 'copy') {
      await navigator.clipboard.writeText(source)
      notice.value = '已复制当前 Markdown。'
    } else if (kind === 'word') {
      const { createWordBlob } = await import('./export/wordExport')
      const blob = await createWordBlob(source)
      if (n === generation && active) downloadBlob(blob, exportName(source, 'docx'))
    } else {
      const { printMarkdown } = await import('./export/pdfPrint')
      if (n !== generation || !active) return
      cleanupPrint?.()
      cleanupPrint = await printMarkdown(
        source,
        (message) => {
          if (active && n === generation) notice.value = message
        },
        () => active && n === generation,
      )
      if (active && n === generation) notice.value ||= '请在浏览器打印对话框选择「另存为 PDF」。'
    }
  } catch (error) {
    if (n === generation) notice.value = `导出失败：${error.message || '请重试'}。正文未修改。`
  } finally {
    if (active && n === generation) busy.value = false
  }
}
watch(
  () =>
    JSON.stringify([
      props.context?.accountId,
      props.context?.module,
      props.context?.documentId,
      props.context?.field,
    ]),
  () => {
    generation++
    incoming.value = null
    busy.value = false
    notice.value = ''
    explanation.value = false
    cleanupPrint?.()
    cleanupPrint = null
  },
)
onBeforeUnmount(() => {
  active = false
  generation++
  cleanupPrint?.()
})
</script>
<template>
  <section v-show="!compact || incoming!==null || notice || busy || explanation" class="editor-file-tools">
      <input
        ref="input"
        hidden
        type="file"
        accept=".md,.markdown,text/markdown"
        @change="selected"
      />
    <nav v-if="!compact" aria-label="文档导入导出">
      <button type="button" :disabled="!ready || readOnly || busy" @click="openImport">
        导入 Markdown</button
      ><button
        v-for="[kind, label] in [
          ['md', '导出 Markdown'],
          ['copy', '复制 Markdown'],
          ['pdf', '导出 PDF'],
          ['word', '导出 Word'],
        ]"
        :key="kind"
        type="button"
        :disabled="!ready || busy"
        @click="exportFile(kind)"
      >
        {{ label }}
      </button>
    </nav>
    <details v-if="!compact || explanation" :open="explanation"><summary>导出说明</summary><p>PDF 通过浏览器打印对话框另存为文件；Word 中公式保留源码，图片保留说明与链接。导出不会改变素材公开权限。</p><button v-if="compact" type="button" @click="explanation=false">关闭说明</button></details>
    <p v-if="notice" role="status">{{ notice }}</p>
    <dialog :open="incoming !== null" aria-label="Markdown 导入确认">
      <h3>导入 {{ filename }}</h3>
      <p>
        {{ incoming?.length || 0 }} 字符 · {{ importedHeadings }} 个标题；仅限 UTF-8、2
        MiB。行尾统一为 LF，正文不会自动保存到服务器。
      </p>
      <label
        >导入方式<select v-model="choice">
          <option value="replace">替换当前正文</option>
          <option value="append">追加到正文末尾</option>
        </select></label
      >
      <div class="compare">
        <label>当前正文<textarea readonly rows="7" :value="original" /></label
        ><label>待导入正文<textarea readonly rows="7" :value="incoming" /></label>
      </div>
      <button type="button" :disabled="readOnly" @click="apply">确认导入到编辑器</button
      ><button type="button" @click="incoming = null">取消导入</button>
    </dialog>
  </section>
</template>
<style scoped>
.editor-file-tools {
  padding: 8px;
  font-size: 0.85rem;
}
nav {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
button,
select,
textarea {
  font: inherit;
  color: var(--text-primary);
  background: var(--bg-surface);
  border: 1px solid var(--border);
  padding: 6px;
  border-radius: 5px;
}
button:disabled {
  opacity: 0.5;
}
dialog[open] {
  position: relative;
  inset: auto;
  box-sizing: border-box;
  width: 100%;
  color: var(--text-primary);
  background: var(--bg-surface);
  border: 1px solid var(--border);
  padding: 16px;
  z-index: 1;
}
.compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
label {
  display: grid;
  gap: 6px;
}
textarea {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
}
@media (max-width: 640px) {
  .compare {
    grid-template-columns: 1fr;
  }
}
</style>
