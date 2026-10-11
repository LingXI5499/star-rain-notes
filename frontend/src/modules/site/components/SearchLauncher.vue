<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import { errorMessage } from '../../../shared/http'
import { quickSearch, suggestions } from '../../search/api/searchApi'

const { contentPath } = useViewMode()
const router = useRouter()
const open = ref(false)
const root = ref(null)
const trigger = ref(null)
const panel = ref(null)
const input = ref(null)
const query = ref('')
const hits = ref([])
const prompts = ref([])
const loading = ref(false)
const failure = ref('')
let debounce = null
let request = null

const shortcut = computed(() => (/Mac|iPhone|iPad/.test(navigator.userAgentData?.platform || navigator.platform || '') ? '⌘ K' : 'Ctrl K'))

async function openPanel() {
  open.value = true
  await nextTick()
  input.value?.focus()
}

function close(restoreFocus = false) {
  open.value = false
  request?.abort()
  if (restoreFocus) nextTick(() => trigger.value?.focus())
}

function goSearch() {
  const q = query.value.trim()
  if (q.length < 2) {
    input.value?.focus()
    return
  }
  close()
  router.push({ path: contentPath('/search'), query: { q } })
}

watch(query, (value) => {
  clearTimeout(debounce)
  request?.abort()
  hits.value = []
  prompts.value = []
  failure.value = ''
  loading.value = false
  const q = value.trim()
  if (!open.value || q.length < 2) return
  loading.value = true
  debounce = setTimeout(async () => {
    const current = new AbortController()
    request = current
    try {
      const [results, suggested] = await Promise.all([
        quickSearch(q, current.signal), suggestions(q, current.signal),
      ])
      if (!current.signal.aborted) {
        hits.value = results
        prompts.value = suggested
      }
    } catch (error) {
      if (!current.signal.aborted) failure.value = errorMessage(error)
    } finally {
      if (!current.signal.aborted) loading.value = false
    }
  }, 280)
})

function onDocumentKeydown(event) {
  const combo = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k'
  if (combo) {
    event.preventDefault()
    if (open.value) close(true)
    else void openPanel()
    return
  }
  if (event.key === 'Escape' && open.value) {
    event.preventDefault()
    close(true)
  }
}

function onClickOutside(event) {
  if (open.value && root.value && !root.value.contains(event.target)) close()
}

onMounted(() => {
  document.addEventListener('keydown', onDocumentKeydown)
  document.addEventListener('click', onClickOutside)
})

onBeforeUnmount(() => {
  clearTimeout(debounce)
  request?.abort()
  document.removeEventListener('keydown', onDocumentKeydown)
  document.removeEventListener('click', onClickOutside)
})
</script>

<template>
  <div ref="root" class="search-launcher">
    <button
      ref="trigger"
      class="search-launcher__trigger"
      type="button"
      aria-haspopup="dialog"
      :aria-expanded="open"
      aria-label="打开站内搜索"
      @click="openPanel"
    >
      <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-3.6-3.6" />
      </svg>
      <span class="search-launcher__hint">搜索知识…</span>
      <kbd class="search-launcher__kbd">{{ shortcut }}</kbd>
    </button>

    <Transition name="search-pop">
      <div
        v-if="open"
        ref="panel"
        class="search-launcher__panel"
        role="dialog"
        aria-modal="false"
        aria-label="站内搜索"
      >
        <form class="search-launcher__field" @submit.prevent="goSearch">
          <input
            ref="input"
            v-model="query"
            type="search"
            placeholder="搜索教程、英语、博客、作品…"
            aria-label="搜索关键字"
            autocomplete="off"
          />
        </form>
        <div class="search-launcher__pending" role="status">
          <p v-if="query.trim().length < 2" class="search-launcher__pending-text">输入至少两个字符，搜索已公开的内容。</p>
          <p v-else-if="loading" class="search-launcher__pending-text">正在搜索…</p>
          <p v-else-if="failure" class="search-launcher__pending-text" role="alert">{{ failure }}</p>
          <p v-else-if="!hits.length" class="search-launcher__pending-text">没有找到相关内容。</p>
          <ul v-if="hits.length" class="search-launcher__links">
            <li v-for="hit in hits" :key="hit.contentType + ':' + hit.contentId">
              <RouterLink :to="contentPath(hit.routePath)" @click="close()">{{ hit.title }}</RouterLink>
            </li>
          </ul>
          <div v-if="prompts.length" class="search-launcher__suggestions">
            <span>相关建议</span>
            <RouterLink v-for="item in prompts" :key="item.contentType + ':' + item.routePath"
              :to="contentPath(item.routePath)" @click="close()">{{ item.text }}</RouterLink>
          </div>
          <button v-if="query.trim().length >= 2" type="button" class="search-launcher__all" @click="goSearch">查看全部结果 →</button>
        </div>
        <button class="search-launcher__close" type="button" @click="close(true)">关闭</button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.search-launcher { position: relative; display: flex; align-items: center; }
/*
 * 触发器外观对齐 V1 `components/search/GlobalSearch.vue` 的 `.global-search__input-wrap`：
 * 同高（--search-input-height）、同宽（clamp(280px,22vw,340px)）、同内边距与圆角。
 * V2 这里仍然是一个按钮而不是真实 input（面板里才是输入框），
 * 但外形与 V1 一致，顶栏四种元素的水平位置才能和 V1 对齐。
 */
.search-launcher__trigger {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  height: var(--search-input-height, 38px);
  width: clamp(280px, 22vw, 340px);
  padding-inline: var(--space-3) var(--space-2);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  color: var(--text-muted);
  background: var(--bg-surface);
  cursor: pointer;
  transition: border-color var(--motion-fast) var(--ease-standard), box-shadow var(--motion-fast) var(--ease-standard);
}
.search-launcher__trigger:hover,
.search-launcher__trigger[aria-expanded='true'] {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary) 16%, transparent);
}
.search-launcher__hint { flex: 1; min-width: 0; overflow: hidden; font-size: 14px; text-align: left; text-overflow: ellipsis; white-space: nowrap; }
.search-launcher__kbd {
  flex: none;
  padding: 3px 5px;
  border: 1px solid var(--border);
  border-bottom-color: var(--border-strong);
  border-radius: 4px;
  color: var(--text-muted);
  background: var(--bg-surface);
  font: 400 11px/1 var(--font-family);
  white-space: nowrap;
}
.search-launcher__panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: var(--z-search-panel, 60);
  width: min(420px, calc(100vw - 32px));
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
  box-shadow: var(--shadow-floating);
}
.search-launcher__field input {
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  background: var(--bg-page);
  font: inherit;
}
.search-launcher__pending {
  margin-top: var(--space-4);
  padding: var(--space-4);
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--bg-subtle) 60%, transparent);
}
.search-launcher__pending-title { color: var(--text-primary); font-size: 14px; font-weight: 700; }
.search-launcher__pending-text { margin-top: 6px; color: var(--text-secondary); font-size: 12px; line-height: 1.7; }
.search-launcher__links { display: grid; gap: 10px; margin: 12px 0 0; padding: 0; list-style: none; font-size: 13px; }
.search-launcher__links a, .search-launcher__suggestions a { color: var(--text-primary); text-decoration: none; }
.search-launcher__links a:hover, .search-launcher__suggestions a:hover { color: var(--primary); }
.search-launcher__suggestions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; font-size: 12px; }
.search-launcher__suggestions span { width: 100%; color: var(--text-muted); }
.search-launcher__all { margin-top: 16px; padding: 0; border: 0; background: none; color: var(--primary); cursor: pointer; font: inherit; font-size: 13px; }
.search-launcher__close {
  margin-top: var(--space-3);
  padding: 6px 10px;
  border: 0;
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  background: transparent;
  cursor: pointer;
  font-size: 12px;
}
.search-launcher__close:hover { color: var(--primary); background: var(--bg-subtle); }
.search-pop-enter-active, .search-pop-leave-active {
  transition: opacity var(--motion-fast) var(--ease-standard), transform var(--motion-fast) var(--ease-out);
}
.search-pop-enter-from, .search-pop-leave-to { opacity: 0; transform: translateY(-6px); }

@media (max-width: 720px) {
  .search-launcher__hint,
  .search-launcher__kbd { display: none; }
  .search-launcher__trigger { width: 36px; justify-content: center; padding: 0; }
  .search-launcher__panel { right: -8px; }
}</style>
