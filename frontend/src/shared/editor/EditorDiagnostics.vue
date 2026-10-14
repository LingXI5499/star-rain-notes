<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { diagnoseMarkdown } from './diagnostics/markdownDiagnostics'
const props = defineProps({ source: String, context: Object, getMedia: Function })
const opened = ref(false),
  issues = ref([]),
  notice = ref(''),
  busy = ref(false)
let timer,
  worker,
  generation = 0,
  active = true
async function accept(items, n) {
  if (!active || n !== generation) return
  const ids = [...new Set(items.filter((item) => item.mediaId).map((item) => item.mediaId))].slice(
      0,
      50,
    ),
    statuses = new Map()
  for (const id of ids) {
    try {
      const asset = await props.getMedia?.(id)
      statuses.set(id, asset?.accessLevel || 'UNKNOWN')
    } catch {
      statuses.set(id, 'UNKNOWN')
    }
    if (n !== generation || !active) return
  }
  issues.value = items
    .filter((item) => !item.mediaId || statuses.get(item.mediaId) !== 'PUBLIC')
    .map((item) =>
      item.mediaId
        ? {
            ...item,
            message:
              statuses.get(item.mediaId) === 'PROTECTED'
                ? '素材仍受保护，发布前需到媒体库明确公开。'
                : '素材权限无法确认，请手动核对。',
          }
        : item,
    )
  busy.value = false
}
function check() {
  if (!opened.value) return
  const n = ++generation,
    source = props.source || ''
  busy.value = true
  notice.value = ''
  worker?.terminate()
  worker = null
  if (source.length >= 50000 && typeof Worker !== 'undefined') {
    try {
      worker = new Worker(new URL('./diagnostics/markdownDiagnostics.worker.js', import.meta.url), {
        type: 'module',
      })
      worker.onmessage = (event) => {
        if (event.data.id !== generation) return
        if (event.data.error) {
          notice.value = event.data.error
          busy.value = false
        } else void accept(event.data.issues, n)
      }
      worker.onerror = () => {
        if (n === generation) {
          notice.value = '后台检查不可用，正文未修改。'
          busy.value = false
        }
      }
      worker.postMessage({ id: n, source })
    } catch {
      notice.value = '后台检查不可用，正文未修改。'
      busy.value = false
    }
  } else {
    try {
      void accept(diagnoseMarkdown(source), n)
    } catch {
      notice.value = '文档检查失败，正文未修改。'
      busy.value = false
    }
  }
}
watch(opened, (value) => {
  if (value) check()
  else {
    generation++
    clearTimeout(timer)
    worker?.terminate()
    worker = null
    busy.value = false
  }
})
watch(
  () => props.source,
  () => {
    if (opened.value) {
      generation++
      clearTimeout(timer)
      timer = setTimeout(check, 650)
    }
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
    clearTimeout(timer)
    worker?.terminate()
    worker = null
    issues.value = []
    opened.value = false
  },
)
onBeforeUnmount(() => {
  active = false
  generation++
  clearTimeout(timer)
  worker?.terminate()
})
</script>
<template>
  <section class="editor-diagnostics">
    <button type="button" :aria-expanded="opened" @click="opened = !opened">质量检查</button>
    <div v-if="opened">
      <p role="status">
        {{
          busy
            ? '正在检查当前正文…'
            : notice || `${issues.length} 条建议（最多显示 300 条），不会自动修改正文。`
        }}
      </p>
      <ol>
        <li v-for="(item, index) in issues" :key="index">
          {{ item.rule }} · 第 {{ item.line }} 行：{{ item.message }}
        </li>
      </ol>
    </div>
  </section>
</template>
<style scoped>
.editor-diagnostics {
  padding: 8px;
  font-size: 0.85rem;
}
button {
  color: var(--text-primary);
  background: var(--bg-surface);
  border: 1px solid var(--border);
  padding: 6px;
  border-radius: 5px;
}
ol {
  max-height: 250px;
  overflow: auto;
}
</style>
