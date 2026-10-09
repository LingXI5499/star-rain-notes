<script setup>
import { nextTick, ref } from 'vue'
import { uploadMedia } from '../api/mediaApi'
import { errorMessage } from '../../../shared/http'
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
const emit = defineEmits(['uploaded'])

const dialog = ref(null)
const fileInput = ref(null)
const file = ref(null)
const busy = ref(false)
const error = ref('')
const uploaded = ref(null)

function open() {
  file.value = null
  uploaded.value = null
  error.value = ''
  if (fileInput.value) fileInput.value.value = ''
  nextTick(() => dialog.value?.showModal())
}

function close() {
  dialog.value?.close()
}

function pick(event) {
  const picked = event.target.files?.[0] || null
  error.value = ''
  uploaded.value = null
  file.value = picked
}

async function submit() {
  if (!file.value || busy.value) return
  busy.value = true
  error.value = ''
  try {
    const asset = await uploadMedia(file.value, 'PUBLIC')
    uploaded.value = asset
    file.value = null
    if (fileInput.value) fileInput.value.value = ''
    emit('uploaded', asset)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busy.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <dialog ref="dialog" aria-labelledby="media-upload-title" @cancel.prevent="close">
    <h2 id="media-upload-title">上传媒体</h2>
    <p class="muted">
      支持图片、文档、音频、视频与压缩包。按真实文件内容校验，改名不会绕过类型检查。
    </p>

    <div class="form-stack">
      <label>
        选择文件
        <input ref="fileInput" type="file" :accept="ACCEPT_ATTRIBUTE" @change="pick" />
      </label>

      <p v-if="file" class="muted">
        已选择：<strong>{{ file.name }}</strong> · {{ formatSize(file.size) }}
      </p>

      <p class="form-hint">
        各类上限：<template v-for="(hint, index) in SIZE_HINTS" :key="hint.label">
          {{ hint.label }} {{ hint.limit }}<template v-if="index < SIZE_HINTS.length - 1"> · </template>
        </template>
      </p>
      <p class="form-hint">上传后为公开可读；需要限制读取的文件请在媒体详情页改为「受保护」。</p>

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
