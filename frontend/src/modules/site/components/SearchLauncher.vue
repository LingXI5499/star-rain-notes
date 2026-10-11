<script setup>
/**
 * SearchLauncher.vue：站点模块的视图组件。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、vue-router、../../../shared/viewMode、../../../shared/http、../../search/api/searchApi。
 * 主要响应式状态：router、open、root、trigger、panel、input、query、hits。
 * 阅读入口：openPanel（打开Panel）；close（关闭当前操作）；goSearch（go搜索的处理函数）；onDocumentKeydown（响应文档Keydown）；onClickOutside（响应ClickOutside）。
 */


import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import { errorMessage } from '../../../shared/http'
import { quickSearch, suggestions } from '../../search/api/searchApi'

const { contentPath } = useViewMode()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * open；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const open = ref(false)
/**
 * root；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const root = ref(null)
/**
 * trigger；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const trigger = ref(null)
/**
 * panel；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const panel = ref(null)
/**
 * input；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const input = ref(null)
/**
 * 查询；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const query = ref('')
/**
 * hits；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const hits = ref([])
/**
 * prompts；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const prompts = ref([])
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 失败；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const failure = ref('')
let debounce = null
let request = null

/**
 * shortcut；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const shortcut = computed(() => (/Mac|iPhone|iPad/.test(navigator.userAgentData?.platform || navigator.platform || '') ? '⌘ K' : 'Ctrl K'))

/**
 * 打开Panel。
 * 主要协作：nextTick、input.value.focus。
 * 更新的响应式状态包括 open；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function openPanel() {
  open.value = true
  await nextTick()
  input.value?.focus()
}

/**
 * 关闭当前操作。
 * 主要协作：request.abort、nextTick、trigger.value.focus。
 * 更新的响应式状态包括 open；渲染由 Vue 的依赖跟踪触发。
 * 使用取消信号终止不再需要的异步工作，调用方仍需区分取消和真实失败。
 * @param restoreFocus = false restoreFocusfalse；默认值、解构字段与可选参数见函数签名。
 */
function close(restoreFocus = false) {
  open.value = false
  request?.abort()
  if (restoreFocus) nextTick(() => trigger.value?.focus())
}

/**
 * go搜索的处理函数。
 * 主要协作：query.value.trim、input.value.focus、close、router.push、contentPath。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function goSearch() {
  const q = query.value.trim()
  if (q.length < 2) {
    input.value?.focus()
    return
  }
  close()
  router.push({ path: contentPath('/search'), query: { q } })
}

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
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

/**
 * 响应文档Keydown。
 * 主要协作：event.key.toLowerCase、event.preventDefault、close、openPanel。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
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

/**
 * 响应ClickOutside。
 * 主要协作：root.value.contains、close。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function onClickOutside(event) {
  if (open.value && root.value && !root.value.contains(event.target)) close()
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  document.addEventListener('keydown', onDocumentKeydown)
  document.addEventListener('click', onClickOutside)
})

/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.search-launcher。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
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
/**
 * 样式范围：.search-launcher__trigger:hover, .search-launcher__trigger[aria-expanded='true']。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-launcher__trigger:hover,
.search-launcher__trigger[aria-expanded='true'] {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary) 16%, transparent);
}
/**
 * 样式范围：.search-launcher__hint。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 */
.search-launcher__hint { flex: 1; min-width: 0; overflow: hidden; font-size: 14px; text-align: left; text-overflow: ellipsis; white-space: nowrap; }
/**
 * 样式范围：.search-launcher__kbd。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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
/**
 * 样式范围：.search-launcher__panel。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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
/**
 * 样式范围：.search-launcher__field input。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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
/**
 * 样式范围：.search-launcher__pending。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-launcher__pending {
  margin-top: var(--space-4);
  padding: var(--space-4);
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--bg-subtle) 60%, transparent);
}
/**
 * 样式范围：.search-launcher__pending-title。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-launcher__pending-title { color: var(--text-primary); font-size: 14px; font-weight: 700; }
/**
 * 样式范围：.search-launcher__pending-text。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-launcher__pending-text { margin-top: 6px; color: var(--text-secondary); font-size: 12px; line-height: 1.7; }
/**
 * 样式范围：.search-launcher__links。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 10px 控制。
 */
.search-launcher__links { display: grid; gap: 10px; margin: 12px 0 0; padding: 0; list-style: none; font-size: 13px; }
/**
 * 样式范围：.search-launcher__links a, .search-launcher__suggestions a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-launcher__links a, .search-launcher__suggestions a { color: var(--text-primary); text-decoration: none; }
/**
 * 样式范围：.search-launcher__links a:hover, .search-launcher__suggestions a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-launcher__links a:hover, .search-launcher__suggestions a:hover { color: var(--primary); }
/**
 * 样式范围：.search-launcher__suggestions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.search-launcher__suggestions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; font-size: 12px; }
/**
 * 样式范围：.search-launcher__suggestions span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-launcher__suggestions span { width: 100%; color: var(--text-muted); }
/**
 * 样式范围：.search-launcher__all。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-launcher__all { margin-top: 16px; padding: 0; border: 0; background: none; color: var(--primary); cursor: pointer; font: inherit; font-size: 13px; }
/**
 * 样式范围：.search-launcher__close。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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
/**
 * 样式范围：.search-launcher__close:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-launcher__close:hover { color: var(--primary); background: var(--bg-subtle); }
/**
 * 样式范围：.search-pop-enter-active, .search-pop-leave-active。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.search-pop-enter-active, .search-pop-leave-active {
  transition: opacity var(--motion-fast) var(--ease-standard), transform var(--motion-fast) var(--ease-out);
}
/**
 * 样式范围：.search-pop-enter-from, .search-pop-leave-to。
 */
.search-pop-enter-from, .search-pop-leave-to { opacity: 0; transform: translateY(-6px); }

/**
 * 响应式或设备条件：(max-width: 720px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 720px) {
  /**
   * 样式范围：.search-launcher__hint,   .search-launcher__kbd。
   */
  .search-launcher__hint,
  .search-launcher__kbd { display: none; }
  /**
   * 样式范围：.search-launcher__trigger。
   */
  .search-launcher__trigger { width: 36px; justify-content: center; padding: 0; }
  /**
   * 样式范围：.search-launcher__panel。
   */
  .search-launcher__panel { right: -8px; }
}</style>
