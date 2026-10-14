<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, ref, watch } from 'vue'
import { outlineSections } from './commands/editorStructure'
import {
  editTable,
  imageEdit,
  lineOffset,
  markdownImages,
  moveSection,
  normalizeVditorMarkdown,
  tableAt,
  tsvTable,
} from './commands/editorDocument'

const Preview = defineAsyncComponent(() => import('./EditorReadingPreview.vue'))
const props = defineProps({
  adapter: Object,
  host: Object,
  ready: Boolean,
  source: String,
  readOnly: Boolean,
  pickImage: Function,
  getMedia: Function,
  context: Object,
})
const mode = ref('ir'),
  panel = ref(''),
  notice = ref(''),
  preview = ref(null),
  headings = ref([]),
  folded = ref(new Set()),
  current = ref(null)
const tableColumn = ref(1),
  image = ref(null),
  imageFields = ref({}),
  media = ref([]),
  tsv = ref(''),
  imageBaseline = ref('')
const tsvPreview = computed(() => {
  if (!tsv.value) return ''
  try {
    return tsvTable(tsv.value)
  } catch (error) {
    return error.message
  }
})
let timer,
  host = null,
  generation = 0,
  outlineDocument = '',
  active = true
function safe(operation) {
  notice.value = ''
  try {
    operation()
  } catch (error) {
    notice.value = error.message
  }
}
function changeMode(value) {
  safe(() => props.adapter.setMode(value))
  mode.value = props.adapter.mode()
}
function capture() {
  props.adapter.remember()
}
function show(name) {
  panel.value = panel.value === name ? '' : name
  if (panel.value === 'outline') refreshOutline()
  if (panel.value === 'media') void checkMedia()
}
function openPreview() {
  preview.value = normalizeVditorMarkdown(props.adapter.markdown())
}
function sourcePoint() {
  props.adapter.restore()
  return props.adapter.sourcePosition()
}
function table(command) {
  safe(() => {
    const point = sourcePoint()
    props.adapter.replace(
      editTable(tableAt(point.source, point.start), command, tableColumn.value - 1),
    )
  })
}
function convertTsv() {
  safe(() => {
    props.adapter.restore()
    props.adapter.insert(tsvTable(tsv.value))
    panel.value = ''
  })
}
function refreshOutline() {
  headings.value = outlineSections(props.adapter.markdown())
  outlineDocument = props.adapter.markdown()
}
function visible(item) {
  return !headings.value.some(
    (parent) =>
      folded.value.has(parent.start) && parent.start < item.start && item.start < parent.sectionEnd,
  )
}
function fold(item) {
  const next = new Set(folded.value)
  if (next.has(item.start)) next.delete(item.start)
  else next.add(item.start)
  folded.value = next
}
function jump(item) {
  if (props.adapter.markdown() !== outlineDocument) {
    refreshOutline()
    throw new Error('大纲已更新，请重新选择章节。')
  }
  const editor = props.adapter.selection()?.element
  const matches = [...(editor?.querySelectorAll('h1,h2,h3,h4,h5,h6') || [])].filter(
    (node) => node.textContent.replace(/^\s*#{1,6}\s*/, '').trim() === item.title,
  )
  if (mode.value !== 'sv' && matches.length === 1) {
    const range = document.createRange()
    range.selectNodeContents(matches[0])
    range.collapse(false)
    props.adapter.restore(range)
    editor.scrollTo({ top: matches[0].offsetTop - editor.offsetTop, behavior: 'auto' })
    props.adapter.remember()
  } else {
    props.adapter.setMode('sv')
    mode.value = 'sv'
    const element = props.host.querySelector('.vditor-sv[contenteditable="true"]'),
      offset = lineOffset(props.adapter.markdown(), item.start)
    props.adapter.selectOffsets(element, offset, offset)
    props.adapter.remember()
  }
  current.value = item.start
}
function heading(item, direction) {
  safe(() => {
    jump(item)
    props.adapter.heading(Math.max(1, Math.min(6, item.level + direction)))
  })
}
function move(item, direction) {
  safe(() => {
    jump(item)
    props.adapter.setMode('sv')
    mode.value = 'sv'
    const element = props.host.querySelector('.vditor-sv[contenteditable="true"]'),
      source = props.adapter.markdown(),
      target = outlineSections(source).find((heading) => heading.id === item.id)
    if (!target) throw new Error('章节定位已变化，请重新选择。')
    const offset = lineOffset(source, target.start)
    props.adapter.selectOffsets(element, offset, offset)
    props.adapter.replace(moveSection(source, target.start, direction))
    refreshOutline()
  })
}
function pickImageAt(point) {
  safe(() => {
    const source = props.adapter.markdown(),
      images = markdownImages(source)
    const matches =
      typeof point === 'number'
        ? images.filter((item) => item.start <= point && item.end >= point)
        : images.filter((item) => item.url === point)
    if (matches.length !== 1)
      throw new Error('请在源码模式定位一条行内图片；重复地址或引用式图片请直接编辑源码。')
    image.value = matches[0]
    imageFields.value = { ...image.value }
    imageBaseline.value = source
    panel.value = 'image'
  })
}
function openImage() {
  safe(() => pickImageAt(sourcePoint().start))
}
function saveImage() {
  safe(() => {
    if (imageBaseline.value !== props.adapter.markdown())
      throw new Error('正文已变化，请重新选择图片；原文未改变。')
    props.adapter.setMode('sv')
    mode.value = 'sv'
    const matches = markdownImages(props.adapter.markdown()).filter(
      (item) => item.raw === image.value.raw,
    )
    if (matches.length !== 1) throw new Error('图片定位已变化，请在源码模式重新选择。')
    const target = matches[0],
      element = props.host.querySelector('.vditor-sv[contenteditable="true"]')
    props.adapter.selectOffsets(element, target.start, target.start)
    props.adapter.replace(imageEdit(target, imageFields.value))
    panel.value = ''
  })
}
async function replaceImage() {
  const epoch = generation
  try {
    const asset = await props.pickImage?.()
    if (active && epoch === generation && asset) imageFields.value.url = asset.url
  } catch {
    if (active && epoch === generation) notice.value = '未能选择图片，原文未改变。'
  }
}
async function copyImageUrl() {
  try {
    await navigator.clipboard.writeText(imageFields.value.url)
    notice.value = '已复制图片地址。'
  } catch {
    notice.value = '剪贴板不可用，请手动复制图片地址。'
  }
}
async function checkMedia() {
  const epoch = ++generation,
    source = props.adapter.markdown(),
    ids = [
      ...new Set(
        markdownImages(source).flatMap(
          (item) => item.url.match(/\/api\/media\/assets\/(\d+)\/content/)?.[1] || [],
        ),
      ),
    ].slice(0, 50)
  media.value = ids.map((id) => ({ id, accessLevel: '正在核对' }))
  for (const item of media.value) {
    try {
      const asset = await props.getMedia(item.id)
      if (epoch !== generation || !active) return
      item.accessLevel = asset.accessLevel
    } catch {
      if (epoch !== generation || !active) return
      item.accessLevel = '无法确认，请到媒体库核对'
    }
  }
}
function clicked(event) {
  let intercepted = false
  safe(() => {
    intercepted = props.adapter.interceptMode(event)
  })
  if (intercepted || event.cancelBubble) {
    mode.value = props.adapter.mode()
    return
  }
  const picture = event.target.closest('img')
  if (picture) pickImageAt(picture.getAttribute('src'))
  const selection = props.adapter.selection(),
    node =
      selection &&
      (selection.range.startContainer.nodeType === 1
        ? selection.range.startContainer
        : selection.range.startContainer.parentElement
      )?.closest('h1,h2,h3,h4,h5,h6')
  if (node)
    current.value =
      headings.value.find(
        (item) => node.textContent.replace(/^\s*#{1,6}\s*/, '').trim() === item.title,
      )?.start ?? null
  mode.value = props.adapter.mode()
}
watch(
  () => props.host,
  (value) => {
    host?.removeEventListener('click', clicked, true)
    host = value
    host?.addEventListener('click', clicked, true)
  },
)
watch(
  () => props.source,
  () => {
    mode.value = props.adapter.mode()
    clearTimeout(timer)
    if (panel.value === 'outline') timer = setTimeout(refreshOutline, 300)
  },
)
watch(
  () => props.ready,
  (value) => {
    if (value) mode.value = props.adapter.mode()
  },
)
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
    panel.value = ''
    image.value = null
    preview.value = null
    folded.value = new Set()
    props.adapter.reset()
  },
)
onBeforeUnmount(() => {
  active = false
  generation++
  clearTimeout(timer)
  host?.removeEventListener('click', clicked, true)
})
defineExpose({ openPreview })
</script>
<template>
  <section class="editor-document-tools">
    <nav aria-label="文档工具">
      <label
        >编辑视图<select
          aria-label="编辑视图"
          :value="mode"
          :disabled="!ready || readOnly"
          @pointerdown="capture"
          @change="changeMode($event.target.value)"
        >
          <option value="ir">即时渲染</option>
          <option value="sv">源码</option>
          <option value="wysiwyg">所见即所得</option>
        </select></label
      ><button type="button" :disabled="!ready" @pointerdown.prevent="capture" @click="openPreview">
        阅读预览</button
      ><button type="button" @pointerdown.prevent="capture" @click="show('outline')">
        章节管理</button
      ><button type="button" @pointerdown.prevent="capture" @click="show('table')">编辑表格</button
      ><button type="button" @pointerdown.prevent="capture" @click="openImage">图片属性</button
      ><button type="button" @pointerdown.prevent="capture" @click="show('media')">
        素材公开检查
      </button>
    </nav>
    <p v-if="notice" role="status">{{ notice }}</p>
    <div v-if="panel === 'table'" class="editor-document-tools__panel">
      <p>
        已有表格的行列操作在源码模式中执行，使用光标所在行；Tab/Shift+Tab 保留 Vditor 原生行为。
      </p>
      <label>第几列<input v-model.number="tableColumn" type="number" min="1" max="20" /></label
      ><button
        v-for="[id, label] in [
          ['row-before', '上插行'],
          ['row-after', '下插行'],
          ['column-before', '左插列'],
          ['column-after', '右插列'],
          ['remove-row', '删除行'],
          ['remove-column', '删除列'],
          ['left', '左对齐'],
          ['center', '居中'],
          ['right', '右对齐'],
        ]"
        :key="id"
        type="button"
        :disabled="readOnly"
        @pointerdown.prevent
        @click="table(id)"
      >
        {{ label }}
      </button>
      <details>
        <summary>粘贴 TSV 转表格</summary>
        <textarea v-model="tsv" aria-label="TSV 数据" rows="4" />
        <p>首行作为表头，确认后才插入。</p>
        <pre aria-label="TSV 转换预览">{{ tsvPreview }}</pre>
        <button type="button" :disabled="readOnly" @click="convertTsv">确认转换并插入</button>
      </details>
    </div>
    <div v-if="panel === 'outline'" class="editor-document-tools__panel">
      <button type="button" @click="folded = new Set(headings.map((item) => item.start))">
        折叠全部</button
      ><button type="button" @click="folded = new Set()">展开全部</button>
      <ol>
        <template v-for="item in headings" :key="item.id"
          ><li
            v-if="visible(item)"
            :style="{ paddingLeft: `${(item.level - 1) * 12}px` }"
            :class="{ current: current === item.start }"
          >
            <button type="button" :aria-expanded="!folded.has(item.start)" @click="fold(item)">
              {{ folded.has(item.start) ? '▸' : '▾' }}</button
            ><button type="button" @pointerdown.prevent="capture" @click="safe(() => jump(item))">
              H{{ item.level }} {{ item.title }}</button
            ><button
              type="button"
              :disabled="readOnly"
              @pointerdown.prevent
              @click="heading(item, -1)"
            >
              提升</button
            ><button
              type="button"
              :disabled="readOnly"
              @pointerdown.prevent
              @click="heading(item, 1)"
            >
              降低</button
            ><button
              type="button"
              :disabled="readOnly"
              @pointerdown.prevent
              @click="move(item, 'up')"
            >
              整章上移</button
            ><button
              type="button"
              :disabled="readOnly"
              @pointerdown.prevent
              @click="move(item, 'down')"
            >
              整章下移
            </button>
          </li></template
        >
      </ol>
    </div>
    <div v-if="panel === 'image' && image" class="editor-document-tools__panel">
      <p>保存属性使用源码视图，只修改选中图片，权限不会改变。</p>
      <label>替代文本<input v-model="imageFields.alt" /></label
      ><label>说明文字<input v-model="imageFields.title" /></label
      ><label>图片地址<input v-model="imageFields.url" /></label
      ><label
        >图片尺寸<select v-model="imageFields.size">
          <option v-for="size in ['normal', 'wide', 'full']" :key="size">{{ size }}</option>
        </select></label
      ><button type="button" :disabled="readOnly" @click="saveImage">应用图片属性</button
      ><button v-if="pickImage" type="button" @click="replaceImage">从媒体库替换</button
      ><button type="button" @click="copyImageUrl">复制图片地址</button
      ><button type="button" @click="panel = ''">取消</button>
    </div>
    <div v-if="panel === 'media'" class="editor-document-tools__panel">
      <p>不会自动公开素材；发布仍由服务器检查权限。</p>
      <p v-for="item in media" :key="item.id">
        <a :href="`/useradmin/media/${item.id}`" target="_blank" rel="noopener"
          >素材 {{ item.id }}</a
        >：{{ item.accessLevel }}
      </p>
      <p v-if="!media.length">未发现可核对的站内行内图片引用。网络图片和引用式图片请另行核对。</p>
    </div>
    <Preview v-if="preview !== null" :source="preview" @close="preview = null" />
  </section>
</template>
<style scoped>
nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 8px;
  border: 1px solid var(--border);
}
button,
select,
input,
textarea {
  font: inherit;
  color: var(--text-primary);
  border: 1px solid var(--border);
  background: var(--bg-surface);
  border-radius: 5px;
  padding: 5px 8px;
  max-width: 100%;
}
button {
  cursor: pointer;
}
button:disabled {
  opacity: 0.5;
}
.editor-document-tools__panel {
  padding: 12px;
  background: var(--bg-subtle);
  max-height: 400px;
  overflow: auto;
}
.editor-document-tools__panel label {
  display: inline-flex;
  gap: 6px;
  margin: 5px;
}
textarea {
  width: 100%;
}
ol {
  list-style: none;
  padding: 0;
}
li {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin: 4px 0;
}
li.current {
  border-left: 3px solid var(--primary);
}
</style>
