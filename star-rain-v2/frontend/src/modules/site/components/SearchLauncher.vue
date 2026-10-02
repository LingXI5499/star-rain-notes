<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'

/*
 * 站内搜索入口 —— 只做外壳。
 *
 * 硬约束：`star-rain-search` 后端还不存在，这个组件**不产生任何搜索结果**，
 * 面板里只有一句「搜索模块建设中」和对当前可用浏览方式（按标签 / 归档）的指路。
 * 伪造结果比没有搜索更糟：用户会以为系统坏了，而不是功能没做。
 *
 * 交互对齐 V1 的 GlobalSearch：点击或 Ctrl/⌘ + K 打开，Esc 关闭并把焦点还给入口按钮，
 * 点击面板外部关闭；打开后焦点自动进入面板。
 *
 * 面板里的两条指路链接按当前路径树生成：账号模式下必须落在 /useradmin/blog 上，
 * 否则点一下就把账号外壳丢了。
 */
const { contentPath } = useViewMode()
const blogPath = computed(() => contentPath('/blog'))
const archivePath = computed(() => contentPath('/blog/archive'))
const open = ref(false)
const root = ref(null)
const trigger = ref(null)
const panel = ref(null)
const input = ref(null)

const shortcut = computed(() => (/Mac|iPhone|iPad/.test(navigator.userAgentData?.platform || navigator.platform || '') ? '⌘ K' : 'Ctrl K'))

async function openPanel() {
  open.value = true
  await nextTick()
  input.value?.focus()
}

function close(restoreFocus = false) {
  open.value = false
  if (restoreFocus) nextTick(() => trigger.value?.focus())
}

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
      <span class="search-launcher__hint">搜索</span>
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
        <form class="search-launcher__field" @submit.prevent>
          <input
            ref="input"
            type="search"
            placeholder="搜索文章标题与正文…"
            aria-label="搜索关键字"
            autocomplete="off"
          />
        </form>
        <div class="search-launcher__pending" role="status">
          <p class="search-launcher__pending-title">搜索模块建设中</p>
          <p class="search-launcher__pending-text">
            全文检索还没接入，这里不会返回任何结果。现在可以先按标签或归档浏览已发布的文章。
          </p>
          <ul class="search-launcher__links">
            <li><RouterLink :to="blogPath" @click="close()">博客时间线</RouterLink></li>
            <li><RouterLink :to="archivePath" @click="close()">归档浏览</RouterLink></li>
          </ul>
        </div>
        <button class="search-launcher__close" type="button" @click="close(true)">关闭</button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.search-launcher { position: relative; }
.search-launcher__trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-secondary);
  background: color-mix(in srgb, var(--bg-surface) 82%, transparent);
  cursor: pointer;
}
.search-launcher__trigger:hover,
.search-launcher__trigger[aria-expanded='true'] { border-color: var(--border-strong); color: var(--primary); }
.search-launcher__hint { font-size: 13px; }
.search-launcher__kbd {
  padding: 1px 6px;
  border: 1px solid var(--border);
  border-radius: 6px;
  color: var(--text-muted);
  background: var(--bg-subtle);
  font: 600 10px/1.6 var(--font-mono);
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
.search-launcher__links { display: flex; gap: 14px; margin: 12px 0 0; padding: 0; list-style: none; font-size: 12px; }
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
}
</style>
