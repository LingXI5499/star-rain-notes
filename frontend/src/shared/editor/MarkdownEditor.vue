<script setup>
/**
 * MarkdownEditor.vue：跨模块基础模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 封装 Vditor 编辑实例、Markdown 值同步、数学公式兼容和媒体插入。
 * 上传图片使用服务端返回的媒体地址；保存正文后由后端建立内容引用，取消或切换文档时阻止旧上传结果回填。
 * 主要依赖：vue、../markdown/markdownEditorChrome、../markdown/markdownCodeGroup、../markdown/markdownMath、../markdown/mathJax、../../modules/media/api/mediaApi。
 * 组件输入：modelValue、placeholder、minHeight、pickImage；默认值与类型见 defineProps。
 * 组件输出事件：update:modelValue；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、host、loadingError、imageInput、imageError、imageStatus、uploadingImages。
 * 阅读入口：newCodeGroupBlock（new代码分组区块的处理函数）；openCodeGroupCreator（打开代码分组Creator）；addCodeGroupBlock（添加代码分组区块）；removeCodeGroupBlock（移除代码分组区块）；insertCodeGroup（写入代码分组）。
 */


import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { resolveVditorEditorHeight, setVditorFullscreenActive } from '../markdown/markdownEditorChrome'
import { createCodeGroupMarkdown, validateCodeGroupBlocks } from '../markdown/markdownCodeGroup'
import { normalizePastedMath } from '../markdown/markdownMath'
import { prepareMathJax } from '../markdown/mathJax'
import { uploadMedia, getMedia } from '../../modules/media/api/mediaApi'
import { IMAGE_ACCEPT, imageMarkdown, mediaUploadError, validateEditorImage } from '../../modules/media/support/upload'
import { useAuthStore } from '../../modules/account/stores/authStore'
import EditorWorkbench from './EditorWorkbench.vue'
import { createVditorAdapter } from './commands/vditorAdapter'
import { normalizeVditorMarkdown } from './commands/editorDocument'
import EditorRecovery from './EditorRecovery.vue'
import EditorDiagnostics from './EditorDiagnostics.vue'

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
 *   - 图片：直接上传、粘贴或拖入，上传后插入媒体地址；也可选媒体库图片
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
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  // 编辑器高度下限（像素）。真实高度取「窗口高度 - 280」，但不低于这个值和 640
  minHeight: { type: Number, default: 460 },
  // () => Promise<{ url, name } | null>；为空时图片按钮退化为插入占位语法
  pickImage: { type: Function, default: null },
  editorContext: { type: Object, default: null },
  readOnly: { type: Boolean, default: false },
})

/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update:modelValue','local-content-applied'])

/**
 * 主机；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const host = ref(null)
const editorReady = ref(false), workbench = ref(null)
const longDocument = computed(() => props.modelValue.length >= 100000)
const adapter = createVditorAdapter(() => vditor, () => host.value)
let workbenchRange = null
function applyLocalContent(value) {
  if (!ready || props.readOnly) return
  try {
    adapter.setMode('sv')
    suppressing = true
    vditor.setValue(value)
    emit('local-content-applied')
    emit('update:modelValue', value)
  } catch (error) { loadingError.value = error.message }
  finally { suppressing = false }
}
function workbenchAction({ name, range }) {
  workbenchRange = ['table','code-group'].includes(name) ? range : null
  if (name === 'table') openTablePicker()
  if (name === 'code-group') openCodeGroupCreator()
  if (name === 'upload') { adapter.restore(range); selectImageFiles() }
  if (name === 'library') { adapter.restore(range); void insertImage() }
}
/**
 * loading错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loadingError = ref('')
/**
 * 图片Input；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const imageInput = ref(null)
/**
 * 图片错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const imageError = ref('')
/**
 * 图片状态；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const imageStatus = ref('')
const imagePercent = ref(null), failedImages = ref([])
/**
 * uploadingImages；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const uploadingImages = ref(false)
/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()
/**
 * can上传Images；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const canUploadImages = computed(() => !props.readOnly && auth.hasPermission('media:upload'))
let savedImageRange = null
let imageUploadController = null
let imageOperation = 0
let disposed = false

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
/**
 * table选择器Open；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tablePickerOpen = ref(false)
/**
 * tableCols；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tableCols = ref(3)
/**
 * table行集合；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tableRows = ref(3)
/**
 * table选择器Ref；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tablePickerRef = ref(null)

/* ---------------------------------------------------------------
   代码组创建器 —— 产出的是可移植的普通 Markdown
   --------------------------------------------------------------- */
/**
 * 代码分组Open；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const codeGroupOpen = ref(false)
/**
 * 代码分组错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const codeGroupError = ref('')
/**
 * 代码分组Blocks；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const codeGroupBlocks = ref([])

/**
 * new代码分组区块的处理函数。
 * @param index 索引；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function newCodeGroupBlock(index) {
  return { label: `方案 ${index}`, language: 'typescript', content: '' }
}

/**
 * 打开代码分组Creator。
 * 主要协作：newCodeGroupBlock。
 * 更新的响应式状态包括 codeGroupError、codeGroupBlocks、codeGroupOpen；渲染由 Vue 的依赖跟踪触发。
 */
function openCodeGroupCreator() {
  codeGroupError.value = ''
  codeGroupBlocks.value = [newCodeGroupBlock(1), newCodeGroupBlock(2)]
  codeGroupOpen.value = true
}

/**
 * 添加代码分组区块。
 * 主要协作：codeGroupBlocks.value.push、newCodeGroupBlock。
 */
function addCodeGroupBlock() {
  codeGroupBlocks.value.push(newCodeGroupBlock(codeGroupBlocks.value.length + 1))
}

/**
 * 移除代码分组区块。
 * 主要协作：codeGroupBlocks.value.splice。
 * @param index 索引；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function removeCodeGroupBlock(index) {
  /* 至少留一个：空代码组是非法输入，与其让用户删到 0 再报错，不如禁止删到 0 */
  if (codeGroupBlocks.value.length <= 1) return
  codeGroupBlocks.value.splice(index, 1)
}

/**
 * 写入代码分组。
 * 主要协作：validateCodeGroupBlocks、vditor.insertValue、createCodeGroupMarkdown。
 * 更新的响应式状态包括 codeGroupError、codeGroupOpen；渲染由 Vue 的依赖跟踪触发。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function insertCodeGroup() {
  const error = validateCodeGroupBlocks(codeGroupBlocks.value)
  if (error) {
    codeGroupError.value = error
    return
  }
  if (workbenchRange) { adapter.insert(`\n${createCodeGroupMarkdown(codeGroupBlocks.value)}\n`, workbenchRange); workbenchRange = null }
  else vditor?.insertValue(`\n${createCodeGroupMarkdown(codeGroupBlocks.value)}\n`)
  codeGroupOpen.value = false
}

/**
 * 打开Table选择器。
 * 更新的响应式状态包括 tablePickerOpen；渲染由 Vue 的依赖跟踪触发。
 */
function openTablePicker() {
  tablePickerOpen.value = true
}

/* 点空白处关闭表格选择器（与 V2 原来的行为一致，避免弹层留在页面上挡住正文） */
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
const onOutside = (event) => {
  if (!tablePickerRef.value?.contains(event.target)) tablePickerOpen.value = false
}
watch(tablePickerOpen, (open) => {
  if (open) document.addEventListener('pointerdown', onOutside)
  else document.removeEventListener('pointerdown', onOutside)
})

/**
 * 写入Table。
 * 主要协作：.join、Array.from、cell、vditor.insertValue。
 * 更新的响应式状态包括 tablePickerOpen；渲染由 Vue 的依赖跟踪触发。
 */
function insertTable() {
  const cols = Math.min(TABLE_MAX_COLS, Math.max(1, Math.round(tableCols.value) || 3))
  const rows = Math.min(TABLE_MAX_ROWS, Math.max(1, Math.round(tableRows.value) || 3))
  /**
   * cell的处理函数。
   * @param content 内容；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  const cell = (content) => ` ${content} `
  const header = `|${Array.from({ length: cols }, (_, i) => cell(`列 ${i + 1}`)).join('|')}|`
  const separator = `|${Array.from({ length: cols }, () => cell('---')).join('|')}|`
  const body = Array.from({ length: rows }, () => `|${Array.from({ length: cols }, () => cell('')).join('|')}|`)
  tablePickerOpen.value = false
  /* 与 V1 一致：表格不加前后换行。加换行会在「行内插入」时把当前段落切断 */
  if (workbenchRange) { adapter.insert([header, separator, ...body].join('\n'), workbenchRange); workbenchRange = null }
  else vditor?.insertValue([header, separator, ...body].join('\n'))
}

/**
 * remember图片范围的处理函数。
 * 主要协作：window.getSelection、selection.getRangeAt、host.value.contains、node.closest、range.cloneRange。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function rememberImageRange() {
  const selection = window.getSelection()
  if (!selection?.rangeCount) return savedImageRange
  const range = selection.getRangeAt(0)
  const node = range.startContainer.nodeType === Node.ELEMENT_NODE ? range.startContainer : range.startContainer.parentElement
  if (host.value?.contains(node) && node?.closest('[contenteditable="true"]')) savedImageRange = range.cloneRange()
  return savedImageRange?.cloneRange() || null
}

/**
 * 写入媒体图片。
 * 主要协作：host.value.contains、.focus、host.value.querySelector、window.getSelection、selection.removeAllRanges。
 * @param asset 资产；默认值、解构字段与可选参数见函数签名。
 * @param range 范围；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function insertMediaImage(asset, range) {
  if (!ready || !vditor || disposed) return
  if (range?.startContainer.isConnected && host.value?.contains(range.startContainer)) {
    host.value.querySelector('[contenteditable="true"]')?.focus()
    const selection = window.getSelection()
    selection.removeAllRanges()
    selection.addRange(range)
  }
  vditor.insertMD(`\n${imageMarkdown(asset)}\n`)
  rememberImageRange()
}

/**
 * 写入图片。
 * 主要协作：rememberImageRange、props.pickImage、insertMediaImage。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function insertImage() {
  if (!props.pickImage || props.readOnly) return
  const range = rememberImageRange()
  const operation = imageOperation
  const asset = await props.pickImage()
  if (!asset?.url || disposed || operation !== imageOperation) return
  insertMediaImage(asset, range)
}

/**
 * 查询图片Files的处理函数。
 * 主要协作：rememberImageRange、imageInput.value.click。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function selectImageFiles() {
  if (!canUploadImages.value || uploadingImages.value) return
  rememberImageRange()
  imageInput.value?.click()
}

/**
 * 上传图片Files。
 * 主要协作：validateEditorImage、failures.push、uploadMedia、insertMediaImage、rememberImageRange。
 * 更新的响应式状态包括 imageError、uploadingImages、imageStatus；渲染由 Vue 的依赖跟踪触发。
 * 使用取消信号终止不再需要的异步工作，调用方仍需区分取消和真实失败。
 * @param files files；默认值、解构字段与可选参数见函数签名。
 * @param range = rememberImageRange() 范围remember图片范围；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function uploadImageFiles(files, range = rememberImageRange()) {
  if (!files.length || !ready) return
  if (!canUploadImages.value) { imageError.value = '当前账号没有媒体上传权限。'; return }
  if (uploadingImages.value) { imageError.value = '图片正在上传，请等待完成后再添加。'; return }
  const operation = ++imageOperation
  imageUploadController = new AbortController()
  uploadingImages.value = true
  imageError.value = ''
  let completed = 0
  const failures = []
  const retryFiles = []
  failedImages.value = []
  for (const file of files) {
    if (disposed || operation !== imageOperation) break
    const invalid = validateEditorImage(file)
    if (invalid) { failures.push(`${file.name}：${invalid}`); continue }
    imageStatus.value = `正在上传 ${completed + 1} / ${files.length}：${file.name}`
    imagePercent.value = null
    try {
      const asset = await uploadMedia(file, 'PROTECTED', { signal: imageUploadController.signal, onUploadProgress: event => {
        if (operation === imageOperation && event.total > 0) imagePercent.value = Math.min(99, Math.round(event.loaded / event.total * 100))
      } })
      if (disposed || operation !== imageOperation) break
      insertMediaImage(asset, range)
      range = rememberImageRange()
      completed += 1
    } catch (cause) {
      if (disposed || operation !== imageOperation) break
      failures.push(`${file.name}：${mediaUploadError(cause)}`)
      retryFiles.push(file)
    }
  }
  if (disposed || operation !== imageOperation) return
  imageError.value = failures.join('\n')
  failedImages.value = retryFiles
  imagePercent.value = null
  imageStatus.value = completed ? `已插入 ${completed} 张图片并加入媒体库；保存正文后会建立内容引用。` : ''
  uploadingImages.value = false
  imageUploadController = null
}

/**
 * 响应图片Files。
 * 主要协作：Array.from、uploadImageFiles。
 * 更新的响应式状态包括 event.target；渲染由 Vue 的依赖跟踪触发。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function onImageFiles(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  void uploadImageFiles(files)
}

/**
 * 响应EditorDrop。
 * 主要协作：Array.from、event.preventDefault、event.stopImmediatePropagation、document.caretRangeFromPoint、uploadImageFiles。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function onEditorDrop(event) {
  const files = Array.from(event.dataTransfer?.files || [])
  if (!files.length) return
  event.preventDefault()
  event.stopImmediatePropagation()
  const range = document.caretRangeFromPoint?.(event.clientX, event.clientY)
  void uploadImageFiles(files, host.value?.contains(range?.startContainer) ? range : rememberImageRange())
}

/* ---------------------------------------------------------------
   主题 / 全屏
   --------------------------------------------------------------- */

/**
 * 判断是否Dark主题。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
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

/**
 * 同步Fullscreen标记。
 * 主要协作：setVditorFullscreenActive、element.classList.contains。
 * @param element element；默认值、解构字段与可选参数见函数签名。
 */
function syncFullscreenFlag(element) {
  setVditorFullscreenActive(element.classList.contains('vditor--fullscreen'))
}

/* Vditor 全屏只是给它自己加 class，没有事件回调，因此用 MutationObserver 盯 class */
/**
 * watchFullscreen的处理函数。
 * 主要协作：fullscreenObserver.disconnect、syncFullscreenFlag、fullscreenObserver.observe。
 * @param element element；默认值、解构字段与可选参数见函数签名。
 */
function watchFullscreen(element) {
  fullscreenObserver?.disconnect()
  syncFullscreenFlag(element)
  fullscreenObserver = new MutationObserver(() => syncFullscreenFlag(element))
  fullscreenObserver.observe(element, { attributes: true, attributeFilter: ['class'] })
}

/* 控制台的主题开关写的是 <html data-theme>，这里跟着它走，不额外引主题 store */
/**
 * watch主题的处理函数。
 * 主要协作：themeObserver.disconnect、themeObserver.observe。
 */
function watchTheme() {
  themeObserver?.disconnect()
  themeObserver = new MutationObserver(syncTheme)
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
}

/* ---------------------------------------------------------------
   粘贴归一化：从网页 / 其它编辑器复制来的公式要能变成合法语法
   --------------------------------------------------------------- */

/**
 * 响应EditorPaste。
 * 主要协作：Array.from、event.preventDefault、event.stopImmediatePropagation、uploadImageFiles、event.clipboardData.getData。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function onEditorPaste(event) {
  const files = Array.from(event.clipboardData?.files || [])
  if (files.length) {
    event.preventDefault()
    event.stopImmediatePropagation()
    void uploadImageFiles(files)
    return
  }
  const pasted = event.clipboardData?.getData('text/plain')
  if (!pasted) return
  const normalized = normalizePastedMath(pasted)
  /* null 表示「不是公式」，原样交给 Vditor 的默认粘贴流程 */
  if (normalized == null) return
  event.preventDefault()
  event.stopImmediatePropagation()
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
  pasteHost.addEventListener('drop', onEditorDrop, true)
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

/**
 * 构建Toolbar。
 * 主要协作：vditor.insertValue、openCodeGroupCreator、openTablePicker、insertImage。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
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
    ...(canUploadImages.value ? [{
      name: 'image-upload',
      icon: ICON_IMAGE,
      tip: '上传图片（也可粘贴或拖入）',
      click: selectImageFiles,
    }] : []),
    ...(props.pickImage ? [{
      name: 'image-library',
      icon: '<span aria-hidden="true" style="font:600 12px/1 var(--font-family)">库</span>',
      tip: '从媒体库插入图片',
      click: () => { void insertImage() },
    }] : []),
    'link',
    '|',
    'outline',
    'edit-mode',
    'fullscreen',
  ]
}

/* 工具栏按钮数量（'|' 是分隔符，不算按钮）：验收脚本读它确认工具栏没有被裁掉过 */
const TOOLBAR_ITEM_COUNT = buildToolbar().filter((item) => item !== '|').length
defineExpose({
  TOOLBAR_ITEM_COUNT,
  getMarkdown: () => (ready && vditor ? normalizeVditorMarkdown(vditor.getValue()) : props.modelValue),
})

/* ---------------------------------------------------------------
   生命周期
   --------------------------------------------------------------- */

/**
 * 挂载Editor。
 * 主要协作：prepareMathJax、resolveVditorEditorHeight、isDarkTheme、buildToolbar、emit。
 * 更新的响应式状态包括 loadingError；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
    cdn: '/assets/vditor-3.11.3',
    // Native SV avoids retaining a large IR tree alongside the source tree.
    mode: longDocument.value ? 'sv' : 'ir',
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
      mode: 'editor',
      math: { engine: 'MathJax', inlineDigit: true },
      markdown: { sanitize: true, mathBlockPreview: true },
    },
    /* 不用 Vditor 的本地草稿缓存：正文的唯一真源是后端的 bodyMarkdown */
    cache: { enable: false },
    // Native formatting operations must reach their own undo checkpoint before the next command.
    undoDelay: 0,
    counter: { enable: false },
    fullscreen: { index: 10000 },
    /* 左侧文档大纲（V1 的「像阅读页目录一样」） */
    outline: { enable: true, position: 'left' },
    toolbar: buildToolbar(),
    input: (value) => {
      if (!suppressing) emit('update:modelValue', normalizeVditorMarkdown(value))
    },
    after: () => {
      ready = true
      editorReady.value = true
      if (props.readOnly) vditor?.disabled()
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

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  document.addEventListener('selectionchange', rememberImageRange)
  watchTheme()
  void mountEditor().catch(() => { loadingError.value = '编辑器资源加载失败，正文未改变，请刷新页面重试。' })
})
watch(() => props.readOnly, value => { if (ready) { if (value) vditor.disabled(); else vditor.enable() } })
watch(() => JSON.stringify([props.editorContext?.accountId,props.editorContext?.module,props.editorContext?.documentId,props.editorContext?.field]), () => {
  imageOperation++;imageUploadController?.abort();uploadingImages.value=false;failedImages.value=[];imagePercent.value=null;imageError.value='';imageStatus.value='';savedImageRange=null;workbenchRange=null
  tablePickerOpen.value=false;codeGroupOpen.value=false;adapter.reset();workbench.value?.hide()
})

/* 外部改值（加载文章、切换文章）→ 写回编辑器，写回期间不回发事件 */
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(
  () => props.modelValue,
  (value) => {
    if (!vditor || !ready) return
    const current = vditor.getValue()
    if (value !== current && value !== normalizeVditorMarkdown(current)) {
      imageOperation += 1
      imageUploadController?.abort()
      uploadingImages.value = false
      imageStatus.value = ''
      savedImageRange = null
      suppressing = true
      if ((value?.length || 0) >= 100000 && adapter.mode() !== 'sv') adapter.setMode('sv')
      vditor.setValue(value ?? '')
      suppressing = false
    }
  },
)

/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => {
  disposed = true
  imageOperation += 1
  imageUploadController?.abort()
  document.removeEventListener('selectionchange', rememberImageRange)
  ready = false
  editorReady.value = false
  document.removeEventListener('pointerdown', onOutside)
  fullscreenObserver?.disconnect()
  fullscreenObserver = null
  themeObserver?.disconnect()
  themeObserver = null
  pasteHost?.removeEventListener('paste', onEditorPaste, true)
  pasteHost?.removeEventListener('drop', onEditorDrop, true)
  pasteHost = null
  setVditorFullscreenActive(false)
  vditor?.destroy()
  vditor = null
})

/* ---------------------------------------------------------------
   底部字数统计（V2 既有能力，保留）
   标题数不在这里算：左侧大纲就是标题清单，再解析一遍正文只会多出一份会漂移的实现。
   --------------------------------------------------------------- */
/**
 * 单词数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const wordCount = computed(() => {
  const text = String(props.modelValue ?? '').trim()
  if (!text) return 0
  /* 中日韩字符按字计，其余按空白分词计 —— 与前台阅读页的字数口径一致 */
  const cjk = (text.match(/[\u4e00-\u9fff\u3400-\u4dbf]/g) || []).length
  const words = (text.replace(/[\u4e00-\u9fff\u3400-\u4dbf]/g, ' ').match(/[A-Za-z0-9_'-]+/g) || []).length
  return cjk + words
})

/**
 * line数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const lineCount = computed(() => (props.modelValue ? String(props.modelValue).split('\n').length : 0))
</script>

<template>
  <div class="markdown-editor">
    <EditorWorkbench ref="workbench" :adapter="adapter" :host="host" :ready="editorReady" :model-value="modelValue" :context="editorContext" :read-only="readOnly" :can-upload="canUploadImages" :can-pick="Boolean(pickImage)" :pick-image="pickImage" :get-media="getMedia" @action="workbenchAction" />
    <EditorRecovery :context="editorContext" :source="modelValue" :read-only="readOnly || !editorReady" @restore="applyLocalContent" />
    <p v-if="longDocument" role="status">正文超过 10 万字符，采用源码视图减少大规模渲染开销；阅读效果请使用预览。保存格式仍为 Markdown。</p>
    <EditorDiagnostics :context="editorContext" :source="modelValue" :get-media="getMedia" />
    <input ref="imageInput" class="markdown-editor__file-input" type="file" :accept="IMAGE_ACCEPT" multiple aria-label="上传正文图片" @change="onImageFiles" />
    <div ref="host" class="markdown-editor__host" />
    <div class="markdown-editor__image-actions">
      <button v-if="canUploadImages" type="button" class="secondary-button" :disabled="uploadingImages" @click="selectImageFiles">{{ uploadingImages ? '图片上传中…' : '上传图片' }}</button>
      <button v-if="pickImage" type="button" class="secondary-button" @click="insertImage">从媒体库选择</button>
      <span v-if="canUploadImages">可粘贴或拖入图片。图片默认仅授权用户可访问；正式发布前请确认素材公开权限。</span>
    </div>
    <p v-if="imageStatus" class="markdown-editor__image-status" role="status">{{ imageStatus }}</p>
    <progress v-if="uploadingImages" :value="imagePercent ?? undefined" max="100" aria-label="图片上传进度" />
    <div v-if="failedImages.length"><p>失败文件保留在当前页面。超时或网络中断时，请先核对媒体库，避免重复上传。</p><button type="button" :disabled="uploadingImages || readOnly" @click="uploadImageFiles([...failedImages])">已核对媒体库，重试失败图片</button></div>
    <p v-if="imageError" class="markdown-editor__error" role="alert">{{ imageError }}</p>
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.markdown-editor__file-input。
 */
.markdown-editor__file-input { display: none; }
/**
 * 样式范围：.markdown-editor__image-actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.markdown-editor__image-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; margin-top: 12px; }
/**
 * 样式范围：.markdown-editor__image-actions span, .markdown-editor__image-status。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.markdown-editor__image-actions span, .markdown-editor__image-status { color: var(--text-muted); font-size: 12px; }
/**
 * 样式范围：.markdown-editor__error。
 */
.markdown-editor__error { white-space: pre-wrap; }
/**
 * 样式范围：.markdown-editor。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.markdown-editor__host :deep(.vditor-ir), .markdown-editor__host :deep(.vditor-wysiwyg), .markdown-editor__host :deep(.vditor-sv)。
 * 溢出处理：纵向 auto。
 */
.markdown-editor__host :deep(.vditor-ir),
.markdown-editor__host :deep(.vditor-wysiwyg),
.markdown-editor__host :deep(.vditor-sv) {
  overflow-y: auto;
}

/**
 * 样式范围：.markdown-editor__error。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.markdown-editor__error {
  padding: var(--space-3) var(--space-4);
  color: var(--danger);
  font-size: 13px;
}

/**
 * 样式范围：.markdown-editor__footer。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.markdown-editor__popover-field。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 2px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.markdown-editor__popover-field {
  display: grid;
  gap: 2px;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 500;
}

/**
 * 样式范围：.markdown-editor__popover-field input。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.markdown-editor__dialog。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.markdown-editor__dialog header, .markdown-editor__code-row-head, .markdown-editor__dialog footer。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.markdown-editor__dialog header,
.markdown-editor__code-row-head,
.markdown-editor__dialog footer {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

/**
 * 样式范围：.markdown-editor__dialog header。
 */
.markdown-editor__dialog header { justify-content: space-between; }
/**
 * 样式范围：.markdown-editor__dialog h2。
 */
.markdown-editor__dialog h2 { margin: 0; font-size: 18px; }
/**
 * 样式范围：.markdown-editor__dialog header p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.markdown-editor__dialog header p { margin: 4px 0 0; color: var(--text-muted); font-size: 13px; }

/**
 * 样式范围：.markdown-editor__dialog-close。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.markdown-editor__dialog-close {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 24px;
  cursor: pointer;
}

/**
 * 样式范围：.markdown-editor__code-list。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 var(--space-3) 控制。
 * 溢出处理：纵向 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.markdown-editor__code-list {
  display: grid;
  gap: var(--space-3);
  margin: var(--space-4) 0;
  overflow-y: auto;
}

/**
 * 样式范围：.markdown-editor__code-row。
 * 使用网格布局组织内容；列定义为 1fr 150px，间距由 var(--space-3) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.markdown-editor__code-row {
  display: grid;
  grid-template-columns: 1fr 150px;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}

/**
 * 样式范围：.markdown-editor__code-row-head。
 */
.markdown-editor__code-row-head { grid-column: 1 / -1; justify-content: space-between; }

/**
 * 样式范围：.markdown-editor__code-row-head button, .markdown-editor__dialog-secondary。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.markdown-editor__code-row-head button,
.markdown-editor__dialog-secondary {
  padding: 6px 10px;
  border: 1px solid var(--border-strong);
  border-radius: 4px;
  background: var(--bg-page);
  color: var(--text-primary);
  cursor: pointer;
}

/**
 * 样式范围：.markdown-editor__code-row-head button:disabled。
 */
.markdown-editor__code-row-head button:disabled { opacity: 0.5; cursor: not-allowed; }

/**
 * 样式范围：.markdown-editor__code-row label。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 4px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.markdown-editor__code-row label {
  display: grid;
  gap: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

/* 代码输入框横跨整行：代码要宽，名称与语言各占一列即可 */
.markdown-editor__code-row label:last-child { grid-column: 1 / -1; }

/**
 * 样式范围：.markdown-editor__code-row input, .markdown-editor__code-row textarea。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.markdown-editor__code-row textarea。
 */
.markdown-editor__code-row textarea { resize: vertical; }

/**
 * 样式范围：.markdown-editor__dialog footer。
 */
.markdown-editor__dialog footer { justify-content: flex-end; }
/**
 * 样式范围：.markdown-editor__dialog-spacer。
 */
.markdown-editor__dialog-spacer { flex: 1; }

/* 窄屏：工具栏横向滚动（不换行，避免吃掉正文高度），大纲收起 */
/**
 * 响应式或设备条件：(max-width: 720px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 720px) {
  /**
   * 样式范围：.markdown-editor__host :deep(.vditor-toolbar)。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 溢出处理：纵向 hidden，横向 auto。
   */
  .markdown-editor__host :deep(.vditor-toolbar) {
    display: flex !important;
    flex-wrap: nowrap !important;
    justify-content: flex-start;
    overflow-x: auto;
    overflow-y: hidden;
    scrollbar-width: thin;
    padding-left: 0 !important;
  }

  /**
   * 样式范围：.markdown-editor__host :deep(.vditor-toolbar__item)。
   */
  .markdown-editor__host :deep(.vditor-toolbar__item) { flex: 0 0 auto; }
  /**
   * 样式范围：.markdown-editor__host :deep(.vditor-outline)。
   */
  .markdown-editor__host :deep(.vditor-outline) { display: none !important; }
  /**
   * 样式范围：.markdown-editor__code-row。
   */
  .markdown-editor__code-row { grid-template-columns: 1fr; }
}
</style>
