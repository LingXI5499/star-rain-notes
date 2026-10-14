<script setup>
/**
 * MediaUploadDialog.vue：媒体模块的视图组件。
 * 职责范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../api/mediaApi、../support/upload、../support/display。
 * 组件输出事件：uploaded；负载由 emit 调用给出。
 * 主要响应式状态：emit、dialog、fileInput、file、busy、error、uploaded。
 * 阅读入口：open（打开当前操作）；close（关闭当前操作）；pick（pick的处理函数）；submit（提交当前操作）。
 */


import { nextTick, ref } from 'vue'
import { uploadMedia } from '../api/mediaApi'
import { mediaUploadError } from '../support/upload'
import { ACCEPT_ATTRIBUTE, SIZE_HINTS, formatSize, mediaTypeLabel } from '../support/display'

/*
 * MED-001 上传对话框。
 *
 * 默认按 PUBLIC 上传：本项目里的媒体都服务于公开内容（教程、博客、作品、
 * 作者页、英语模块），前台访客要能直接通过 contentUrl 加载它们。
 * 如果默认 PROTECTED，前台图片会全部 403，所以这里不再让用户每次选择；
 * 确实需要保密的文件，到媒体详情页改成「受保护」即可。
 *
 * 上传成功后不直接关闭：给一个「打开」入口让用户立刻确认媒体能被正常读取，
 * 这才是上传功能真正要验证的点，而不只是文件写进了磁盘。
 */
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['uploaded'])

/**
 * 弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const dialog = ref(null)
/**
 * 文件Input；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const fileInput = ref(null)
/**
 * 待处理的上传文件；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const file = ref(null)
/**
 * busy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busy = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * 上传；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const uploaded = ref(null)
const publicUpload = ref(false)

/**
 * 打开当前操作。
 * 主要协作：nextTick、dialog.value.showModal。
 * 更新的响应式状态包括 file、uploaded、error、fileInput.value；渲染由 Vue 的依赖跟踪触发。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function open() {
  if (busy.value) { nextTick(() => { if (!dialog.value?.open) dialog.value?.showModal() }); return }
  file.value = null
  uploaded.value = null
  publicUpload.value = false
  error.value = ''
  if (fileInput.value) fileInput.value.value = ''
  nextTick(() => dialog.value?.showModal())
}

/**
 * 关闭当前操作。
 * 主要协作：dialog.value.close。
 */
function close() {
  dialog.value?.close()
}

/**
 * pick的处理函数。
 * 更新的响应式状态包括 error、uploaded、file；渲染由 Vue 的依赖跟踪触发。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function pick(event) {
  const picked = event.target.files?.[0] || null
  error.value = ''
  uploaded.value = null
  file.value = picked
}

/**
 * 提交当前操作。
 * 主要协作：uploadMedia、emit、mediaUploadError。
 * 更新的响应式状态包括 busy、error、uploaded、file、fileInput.value；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function submit() {
  if (!file.value || busy.value) return
  busy.value = true
  error.value = ''
  try {
    const asset = await uploadMedia(file.value, publicUpload.value ? 'PUBLIC' : 'PROTECTED')
    uploaded.value = asset
    file.value = null
    if (fileInput.value) fileInput.value.value = ''
    emit('uploaded', asset)
  } catch (cause) {
    error.value = mediaUploadError(cause)
  } finally {
    busy.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <dialog ref="dialog" class="media-upload-dialog" aria-labelledby="media-upload-title" @cancel.prevent="close">
    <h2 id="media-upload-title">上传媒体</h2>
    <p class="muted">
      支持图片、文档、音频、视频与压缩包。按真实文件内容校验，改名不会绕过类型检查。
    </p>

    <div class="form-stack">
      <label>
        选择文件
        <input ref="fileInput" class="media-file-input" type="file" :accept="ACCEPT_ATTRIBUTE" :disabled="busy" @change="pick" />
      </label>

      <p v-if="file" class="muted">
        已选择：<strong>{{ file.name }}</strong> · {{ formatSize(file.size) }}
      </p>

      <p class="form-hint">
        各类上限：<template v-for="(hint, index) in SIZE_HINTS" :key="hint.label">
          {{ hint.label }} {{ hint.limit }}<template v-if="index < SIZE_HINTS.length - 1"> · </template>
        </template>
      </p>
      <label><input v-model="publicUpload" type="checkbox" :disabled="busy" />允许任何人通过文件链接读取此素材</label>
      <p class="form-hint">默认仅有媒体读取权限的账户可预览。公开文章引用的素材，请在发布前于媒体库确认公开。</p>

      <p v-if="error" class="error" role="alert">{{ error }}</p>

      <div v-if="uploaded" class="notice" role="status">
        <p>上传成功：<strong>{{ uploaded.originalName }}</strong> ·
          {{ mediaTypeLabel(uploaded.mediaType) }} · {{ formatSize(uploaded.sizeBytes) }}</p>
        <p v-if="uploaded.sameSha256Count > 1" class="muted">
          库中已有 {{ uploaded.sameSha256Count }} 份相同内容的媒体，系统不会自动合并。
        </p>
        <a :href="uploaded.contentUrl" target="_blank" rel="noopener">打开已上传的媒体 →</a>
      </div>
    </div>

    <div class="dialog-actions">
      <button type="button" @click="close">关闭</button>
      <button class="primary-button" type="button" :disabled="!file || busy" @click="submit">
        {{ busy ? '上传中…' : '开始上传' }}
      </button>
    </div>
  </dialog>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.media-upload-dialog。
 * 溢出处理：纵向 auto。
 */
.media-upload-dialog { max-height: calc(100dvh - 32px); overflow-y: auto; }
/**
 * 样式范围：.media-file-input。
 */
.media-file-input { width: 100%; min-width: 0; padding: 10px; font: inherit; }
/**
 * 样式范围：.media-file-input::file-selector-button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-file-input::file-selector-button { margin-right: 12px; padding: 9px 14px; border: 1px solid var(--border-strong); border-radius: var(--action-radius,10px); background: var(--bg-subtle); color: var(--primary); font: inherit; font-weight: 600; cursor: pointer; }
/**
 * 样式范围：.media-file-input::file-selector-button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.media-file-input::file-selector-button:hover { border-color: var(--primary); background: color-mix(in srgb,var(--primary) 10%,var(--bg-surface)); }
</style>
