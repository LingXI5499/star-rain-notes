<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import {
  draftKey,
  getDraft,
  writeDraft,
  removeDraft,
  migrateDraft,
  registerDraftFlusher,
} from './recovery/draftStore'
const props = defineProps({ context: Object, source: String, readOnly: Boolean })
const emit = defineEmits(['restore'])
const notice = ref(''),
  record = ref(null),
  opened = ref(false),
  selected = ref(0),
  saving = ref(false)
const point = computed(() => record.value?.points?.[selected.value]),
  available = computed(() => Boolean(draftKey(props.context)))
const canonical = (value) =>
  String(value ?? '')
    .replace(/\r\n/g, '\n')
    .trimEnd()
let context = null,
  latest = '',
  timer,
  epoch = 0,
  active = true,
  pending = false,
  touched = false,
  queue = Promise.resolve()
function report(error) {
  if (active)
    notice.value = `浏览器本地备份不可用：${error?.message || '存储失败'}。正文仍可继续编辑并手动保存。`
}
function enqueue(operation) {
  const result = queue.then(operation)
  queue = result.catch(() => {})
  return result
}
async function flush() {
  clearTimeout(timer)
  if (!context || !pending || props.readOnly) return
  const owner = { ...context },
    source = latest,
    n = epoch
  pending = false
  saving.value = true
  try {
    await enqueue(() => writeDraft(owner, source))
    if (active && n === epoch) notice.value = '已备份到当前浏览器；尚未保存到服务器。'
  } catch (error) {
    if (active && n === epoch) {
      pending = true
      report(error)
    }
  } finally {
    if (active && n === epoch) saving.value = false
  }
}
async function load(next) {
  // Capture the old owner before changing identity, even when two fields contain identical text.
  void flush()
  const old = context,
    n = ++epoch
  context = next ? { ...next } : null
  latest = props.source || ''
  pending = false
  touched = false
  record.value = null
  opened.value = false
  notice.value = ''
  if (!draftKey(next)) return
  try {
    if (next.previousDocumentId)
      await enqueue(() => migrateDraft({ ...next, documentId: next.previousDocumentId }, next))
    else if (
      old?.documentId?.startsWith('new:') &&
      old.module === next.module &&
      old.field === next.field &&
      String(old.accountId) === String(next.accountId)
    )
      await enqueue(() => migrateDraft(old, next))
    const result = await enqueue(() => getDraft(next))
    if (!active || n !== epoch) return
    record.value = result
    if (result?.points?.some((item) => canonical(item.source) !== canonical(next.baseline))) {
      opened.value = true
      selected.value = 0
      notice.value = '发现本地恢复点，请对比后决定；不会自动覆盖或上传。'
    }
  } catch (error) {
    if (active && n === epoch) report(error)
  }
}
watch(
  () => draftKey(props.context),
  () => load(props.context),
  { immediate: true },
)
watch(
  () => [props.context?.baseline, props.context?.serverVersion],
  async () => {
    context = props.context ? { ...props.context } : null
    if (!context || !draftKey(context) || !touched) return
    if (canonical(latest) === canonical(context.baseline) && !opened.value) {
      const owner = { ...context },
        n = epoch
      pending = false
      clearTimeout(timer)
      try {
        await enqueue(() => removeDraft(owner))
        if (active && n === epoch) {
          record.value = null
          notice.value = '服务器已确认当前正文。'
        }
      } catch (error) {
        if (n === epoch) report(error)
      }
    } else if (canonical(latest) !== canonical(context.baseline)) {
      pending = true
      clearTimeout(timer)
      timer = setTimeout(flush, 1800)
    }
  },
)
watch(
  () => props.source,
  (value) => {
    latest = value || ''
    if (!context || props.readOnly || canonical(latest) === canonical(context.baseline)) return
    touched = true
    pending = true
    clearTimeout(timer)
    timer = setTimeout(flush, 1800)
  },
)
async function inspect() {
  const owner = context ? { ...context } : null,
    n = epoch
  await flush()
  try {
    const result = await enqueue(() => getDraft(owner))
    if (!active || n !== epoch) return
    record.value = result
    selected.value = 0
    opened.value = true
  } catch (error) {
    if (n === epoch) report(error)
  }
}
function restore() {
  if (!point.value || props.readOnly) return
  emit('restore', point.value.source)
  opened.value = false
  notice.value = '恢复内容仅载入当前编辑器；请检查后手动保存到服务器。'
}
async function discard() {
  if (!window.confirm('确认删除本字段在当前浏览器中的所有恢复点？此操作不会删除服务器正文。'))
    return
  const owner = context ? { ...context } : null,
    n = epoch
  pending = false
  touched = false
  clearTimeout(timer)
  try {
    await enqueue(() => removeDraft(owner))
    if (!active || n !== epoch) return
    record.value = null
    opened.value = false
    notice.value = '已删除本字段本地恢复点。'
  } catch (error) {
    if (n === epoch) report(error)
  }
}
function hidden() {
  if (document.visibilityState === 'hidden') void flush()
}
document.addEventListener('visibilitychange', hidden)
const unregister = registerDraftFlusher(flush)
onBeforeUnmount(() => {
  void flush()
  active = false
  epoch++
  clearTimeout(timer)
  unregister()
  document.removeEventListener('visibilitychange', hidden)
})
</script>
<template>
  <section class="editor-recovery">
    <div v-if="available">
      <button type="button" @click="inspect">本地恢复点</button
      ><span role="status">{{ saving ? '正在备份…' : notice }}</span>
    </div>
    <p v-else>当前字段未提供登录身份或文档上下文，本地备份未启用。</p>
    <dialog :open="opened" aria-label="本地草稿对比">
      <h3>本地恢复点与服务器正文</h3>
      <p>本地数据仅保存在此浏览器，未加密。共享电脑请在退出时清理；恢复不会自动保存或发布。</p>
      <template v-if="point"
        ><label
          >恢复时间<select v-model="selected">
            <option v-for="(item, index) in record.points" :key="item.time" :value="index">
              {{ new Date(item.time).toLocaleString() }}
            </option>
          </select></label
        >
        <p>
          本地基于版本 {{ point.serverVersion ?? '未知' }} · 服务器当前版本
          {{ context?.serverVersion ?? '未知' }} ·
          {{
            point.serverVersion !== context?.serverVersion
              ? '版本不同，请逐段核对'
              : '仍需核对正文差异'
          }}
        </p>
        <div class="editor-recovery__compare">
          <label>服务器原文<textarea readonly :value="context?.baseline || ''" rows="9" /></label
          ><label>本地恢复原文<textarea readonly :value="point.source" rows="9" /></label>
        </div>
        <button type="button" :disabled="readOnly" @click="restore">载入此恢复点到编辑器</button
        ><button type="button" @click="discard">删除本地恢复点</button></template
      >
      <p v-else>暂无本字段恢复点。</p>
      <button type="button" @click="opened = false">关闭，保留当前正文</button>
    </dialog>
  </section>
</template>
<style scoped>
.editor-recovery {
  padding: 8px;
  font-size: 0.85rem;
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
span {
  margin-left: 8px;
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
.editor-recovery__compare {
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
  .editor-recovery__compare {
    grid-template-columns: 1fr;
  }
}
</style>
