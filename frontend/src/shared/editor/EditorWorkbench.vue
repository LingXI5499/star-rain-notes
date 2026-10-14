<script setup>
import { computed, onBeforeUnmount, ref, watch, useId } from 'vue'
import { defaultHeadingKeys, headingHotkey } from './commands/editorHotkeys'
import { markdownBlocks } from './commands/editorStructure'

const props = defineProps({ adapter: { type: Object, required: true }, host: Object, ready: Boolean, modelValue: String, context: Object, readOnly: Boolean, canUpload: Boolean, canPick: Boolean })
const emit = defineEmits(['action'])
const notice = ref(''), help = ref(false), selected = ref(''), popup = ref(null), query = ref(null), current = ref(0)
const keys = ref({ ...defaultHeadingKeys })
const helpId = useId()
watch(() => props.ready, value => { if (value) props.adapter.shortcutTips(keys.value) })
let composing = false, menuRange = null, listenerHost = null, dismissed = false
const commandList = computed(() => [
  ...Array.from({ length: 7 }, (_, level) => ({ id: `heading${level}`, title: level ? `标题 ${level}` : '正文', aliases: `h${level} 标题${level} biaoti 正文 paragraph`, level })),
  ...[['list', '无序列表'], ['ordered-list', '有序列表'], ['check', '任务清单'], ['quote', '引用'], ['line', '分隔线'], ['code', '代码块'], ['inline-code', '行内代码'], ['link', '链接']].map(([id, title]) => ({ id, title, aliases: `${id} ${title}` })),
  { id: 'math', title: '公式', aliases: '公式 math formula' }, { id: 'table', title: '表格', aliases: '表格 table' }, { id: 'code-group', title: '代码组', aliases: '代码组 code group' },
  ...(props.canUpload ? [{ id: 'upload', title: '上传图片', aliases: '图片 image upload' }] : []),
  ...(props.canPick ? [{ id: 'library', title: '媒体库图片', aliases: '图片 媒体库 image library' }] : []),
])
const filtered = computed(() => commandList.value.filter(item => `${item.title} ${item.aliases}`.toLowerCase().includes((query.value || '').toLowerCase())))
function position(range) {
  const rectangle = range.getBoundingClientRect?.() || props.host?.getBoundingClientRect() || { left: 20, top: 100, bottom: 110 }
  return { left: `${Math.max(8, Math.min(rectangle.left, window.innerWidth - 290))}px`, top: `${Math.max(8, Math.min(rectangle.bottom + 8, window.innerHeight - (query.value !== null ? 280 : 70)))}px` }
}
function hide() { query.value = null; popup.value = null; selected.value = ''; menuRange = null }
function update() {
  if (!props.ready || props.readOnly || composing || dismissed) return hide()
  const selection = props.adapter.selection()
  if (!selection) return hide()
  props.adapter.remember()
  const range = selection.range
  selected.value = range.collapsed ? '' : range.toString().trim()
  const block = (range.startContainer.nodeType === 1 ? range.startContainer : range.startContainer.parentElement)?.closest('p,[data-type="p"]')
  const container = block?.closest('pre,table')
  let slash = block && (!container || container === selection.element) ? block.textContent.replace(/\u200b/g, '').match(/^\s*\/([\p{L}\p{N}-]*)$/u) : null
  if (props.adapter.mode() === 'sv') {
    try {
      const point = props.adapter.sourcePosition(), line = point.source.slice(0, point.start).split('\n').at(-1)
      slash = line.match(/^\s*\/([\p{L}\p{N}-]*)$/u)
    } catch { slash = null }
  }
  if (range.collapsed && slash) {
    const { lines, protectedLines } = markdownBlocks(props.adapter.markdown())
    if (lines.some((line, index) => protectedLines.has(index) && line.trim() === slash[0].trim())) return hide()
    query.value = slash[1]; current.value = Math.min(current.value, Math.max(0, filtered.value.length - 1))
    menuRange = range.cloneRange()
    if (props.adapter.mode() === 'sv') {
      const point = props.adapter.sourcePosition(); menuRange = props.adapter.selectOffsets(point.element, point.start - slash[0].length, point.start); props.adapter.restore(range)
    } else { menuRange.selectNodeContents(block) }
  } else { query.value = null; menuRange = null }
  popup.value = selected.value || query.value !== null ? position(range) : null
}
function safe(action) {
  notice.value = ''
  if (!props.ready || props.readOnly) return
  try { action() } catch (error) { notice.value = error.message }
}
function applyHeading(level) { safe(() => { props.adapter.restore(); props.adapter.heading(level); hide() }) }
function format(name) { safe(() => { props.adapter.restore(); props.adapter.command(name); hide() }) }
function execute(item) {
  const range = menuRange?.cloneRange()
  if (!range) return
  safe(() => {
    props.adapter.restore(range)
    const snippets = { list: '- 列表项', 'ordered-list': '1. 列表项', check: '- [ ] 任务', quote: '> 引用', line: '\n---\n', code: '```text\n代码\n```', 'inline-code': '`代码`', link: '[链接文字](https://example.com)', math: '\n$$\n\\frac{a}{b}\n$$\n' }
    if (item.level !== undefined) props.adapter.insert(item.level ? `${'#'.repeat(item.level)} 标题` : '正文', range)
    else if (snippets[item.id]) props.adapter.insert(snippets[item.id], range)
    else emit('action', { name: item.id, range })
    hide()
  })
}
async function copySelection() { try { props.adapter.restore(); await navigator.clipboard.writeText(props.adapter.markdownSelection() || selected.value); notice.value = '已复制 Markdown 选区。' } catch { notice.value = '剪贴板不可用，请使用系统复制。' } }
function keydown(event) {
  if (!props.ready || props.readOnly || !props.adapter.selection()) return
  if (event.key === 'Escape') { dismissed = true; hide(); help.value = false; return }
  if (event.key.length === 1 || ['Backspace','Delete','Enter'].includes(event.key)) dismissed = false
  if (composing || event.isComposing || event.keyCode === 229) return
  const level = headingHotkey(event, keys.value, composing)
  if (level !== null) { event.preventDefault(); event.stopImmediatePropagation(); props.adapter.remember(); applyHeading(level); return }
  if (query.value !== null && ['ArrowDown', 'ArrowUp', 'Enter'].includes(event.key)) {
    event.preventDefault(); event.stopImmediatePropagation()
    if (event.key === 'Enter') { if (filtered.value[current.value]) execute(filtered.value[current.value]); return }
    const count = filtered.value.length; current.value = count ? (current.value + (event.key === 'ArrowDown' ? 1 : -1) + count) % count : 0
  }
}
function compositionStart() { composing = true; hide() }
function compositionEnd() { composing = false }
function pointer() { dismissed = false }
function outside(event) { if (!props.host?.contains(event.target) && !event.target.closest('.editor-workbench')) hide() }
watch(() => props.host, host => {
  cleanupHost(); listenerHost = host
  host?.addEventListener('keydown', keydown, true); host?.addEventListener('keyup', update)
  host?.addEventListener('compositionstart', compositionStart); host?.addEventListener('compositionend', compositionEnd)
  host?.addEventListener('pointerdown', pointer)
}, { immediate: true })
function cleanupHost() {
  listenerHost?.removeEventListener('keydown', keydown, true); listenerHost?.removeEventListener('keyup', update)
  listenerHost?.removeEventListener('compositionstart', compositionStart); listenerHost?.removeEventListener('compositionend', compositionEnd)
  listenerHost?.removeEventListener('pointerdown', pointer)
}
document.addEventListener('selectionchange', update)
document.addEventListener('pointerdown', outside)
window.addEventListener('resize', update)
window.addEventListener('scroll', update, true)
onBeforeUnmount(() => { cleanupHost(); document.removeEventListener('selectionchange', update); document.removeEventListener('pointerdown', outside); window.removeEventListener('resize', update); window.removeEventListener('scroll', update, true) })
defineExpose({ hide, applyHeading })
</script>
<template>
  <div class="editor-workbench" @keydown.esc="help = false">
    <div class="editor-workbench__bar"><span>写作工具</span><button v-for="level in 7" :key="level" type="button" :disabled="!ready || readOnly" @pointerdown.prevent @click="applyHeading(level - 1)">{{ level === 1 ? '正文' : `H${level - 1}` }}</button><button type="button" :aria-expanded="help" :aria-controls="helpId" @click="help = !help">快捷键 ?</button></div>
    <p v-if="notice" role="status">{{ notice }}</p>
    <div v-if="popup && query !== null" class="editor-workbench__popup" :style="popup" role="listbox" aria-label="段落命令"><button v-for="(item, index) in filtered" :key="item.id" type="button" role="option" :aria-selected="current === index" @pointerdown.prevent @click="execute(item)">{{ item.title }}</button><p v-if="!filtered.length">没有匹配的命令</p></div>
    <div v-else-if="popup && selected" class="editor-workbench__popup editor-workbench__floating" :style="popup" role="toolbar" aria-label="选区格式"><button v-for="[id, label] in [['bold','加粗'],['italic','斜体'],['strike','删除线'],['inline-code','行内代码'],['link','链接']]" :key="id" type="button" @pointerdown.prevent @click="format(id)">{{ label }}</button><label>标题<select aria-label="选区标题级别" @pointerdown="adapter.remember()" @change="applyHeading(Number($event.target.value)); $event.target.value = ''"><option value="">选择</option><option v-for="level in 7" :key="level" :value="level-1">{{ level === 1 ? '正文' : `H${level-1}` }}</option></select></label><button type="button" @pointerdown.prevent @click="copySelection">复制</button></div>
    <section v-if="help" :id="helpId" class="editor-workbench__panel" aria-label="快捷键帮助"><button type="button" @click="help = false">关闭</button><p v-for="level in 7" :key="level">{{ level === 1 ? '正文' : `H${level - 1}` }}：{{ keys[`heading${level - 1}`] }}</p><p>Ctrl+B 加粗 · Ctrl+I 斜体 · Ctrl+Z 撤销 · Ctrl+Y 重做。段落行首输入 / 打开命令菜单。快捷键仅作用于当前编辑区域；系统或输入法可能占用组合键，可使用上方按钮。</p></section>
  </div>
</template>
<style scoped>
.editor-workbench { color: var(--text-primary, inherit); font-size: .85rem; }
.editor-workbench__bar { display:flex; flex-wrap:wrap; gap:6px; align-items:center; padding:8px; border:1px solid var(--border); border-bottom:0; }
button { cursor:pointer; color:inherit; border:1px solid var(--border); border-radius:5px; padding:5px 9px; background:var(--bg-surface); }
button:disabled { opacity:.5; cursor:default; }
.editor-workbench__popup { position:fixed; z-index:11000; width:min(280px, calc(100vw - 16px)); max-height:260px; overflow:auto; padding:6px; background:var(--bg-surface); color:var(--text-primary); box-shadow:0 5px 24px #0003; border:1px solid var(--border); border-radius:8px; }
.editor-workbench__popup [role=option] { display:block; width:100%; text-align:left; border:0; }
.editor-workbench__popup [aria-selected=true] { outline:2px solid var(--primary); outline-offset:-2px; }
.editor-workbench__floating { display:flex; flex-wrap:wrap; gap:4px; }
.editor-workbench__panel { padding:12px; border:1px solid var(--border); background:var(--bg-surface); }
</style>
