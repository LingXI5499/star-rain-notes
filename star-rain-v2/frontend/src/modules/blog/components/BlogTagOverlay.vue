<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

/*
 * 全部标签的悬浮卡片 —— 一个「先选条件、再确认」的筛选面板。
 *
 * 用户原话：「点击全部标签给我跳转了一个新页面，不好，应该是给个悬浮在这个核心页面上的，
 * 然后也有这个搜索展示，完整的一个小卡片，悬浮卡片」；
 * 之后又要求：「现在一点击就直接跳转了，可以增加条件选择啊，多标签，或者单标签，
 * 确定后再搜索，直接跳的话有点太突兀的」。
 *
 * 所以卡片里的点击**只改本地勾选**，不立刻筛选：
 *   - 单标签：点一个换一个（等价于原来的 ?tag=），选中后再点一次取消；
 *   - 多标签：点一个加一个（?tags=a,b，命中任一），可以攒几个一起看；
 *   - 「应用筛选」才真正 emit 出去，父组件换掉下方列表并收起卡片；
 *   - 「清空」只清本地勾选；空选项也可通过「应用筛选」取消当前筛选。
 * 打开时会用当前生效的标签预填勾选，再次打开不会「忘掉」上次选了什么。
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  tags: { type: Array, default: () => [] },
  // 当前生效的标签集合（来自 URL）
  activeTags: { type: Array, default: () => [] },
})

const emit = defineEmits(['close', 'apply'])

const keyword = ref('')
const mode = ref('single')
const picked = ref([])
const searchInput = ref(null)

const matched = computed(() => {
  const term = keyword.value.trim().toLocaleLowerCase()
  if (!term) return props.tags
  return props.tags.filter((tag) => tag.name.toLocaleLowerCase().includes(term))
})

// 总引用次数：卡片右上角显示「50 个标签 · 58 次引用」，与标签管理页的口径一致
const totalRelations = computed(() => props.tags.reduce((sum, tag) => sum + (tag.postCount || 0), 0))
const pickedNames = computed(() => picked.value.map((slug) => nameOf(slug)))

function nameOf(slug) {
  return props.tags.find((tag) => tag.slug === slug)?.name || slug
}

function isPicked(slug) {
  return picked.value.includes(slug)
}

/*
 * 勾选逻辑按模式分派：
 *   单标签 = 换一个（再点自己就是取消）；多标签 = 逐个累加。
 * 多标签没有上限，但后端对 IN 列表有 20 个的上限，这里也照同一个数拦住，
 * 免得用户选到第 21 个才被后端拒绝。
 */
const PICK_MAX = 20

function toggle(slug) {
  if (mode.value === 'single') {
    picked.value = isPicked(slug) ? [] : [slug]
    return
  }
  if (isPicked(slug)) {
    picked.value = picked.value.filter((item) => item !== slug)
    return
  }
  if (picked.value.length >= PICK_MAX) return
  picked.value = [...picked.value, slug]
}

function switchMode(next) {
  mode.value = next
  // 从多标签切回单标签时只留第一个，避免出现「单标签模式下选中了 3 个」的矛盾状态
  if (next === 'single' && picked.value.length > 1) picked.value = picked.value.slice(0, 1)
}

function clearPicked() {
  picked.value = []
}

function apply() {
  emit('apply', [...picked.value])
}

function close() {
  emit('close')
}

function onKeydown(event) {
  if (event.key === 'Escape' && props.open) close()
}

watch(() => props.open, async (value) => {
  if (!value) return
  // 打开时用当前生效的标签预填；顺带把模式也定成与选择数量相符的那个
  picked.value = [...props.activeTags]
  if (props.activeTags.length > 1) mode.value = 'multi'
  keyword.value = ''
  await nextTick()
  searchInput.value?.focus()
})

// 只在卡片打开期间挂 Esc：组件常驻在侧栏里，不能一直占着全局键盘事件
watch(() => props.open, (value) => {
  if (value) document.addEventListener('keydown', onKeydown)
  else document.removeEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="tag-overlay" role="presentation" @click.self="close">
      <section class="tag-overlay__card" role="dialog" aria-modal="true" aria-labelledby="tag-overlay-title">
        <header class="tag-overlay__head">
          <div>
            <p class="tag-overlay__eyebrow">TAGS · 标签筛选</p>
            <h2 id="tag-overlay-title">按标签筛选</h2>
            <span>{{ tags.length }} 个标签 · {{ totalRelations }} 次文章引用</span>
          </div>
          <button class="tag-overlay__close" type="button" aria-label="关闭标签筛选" @click="close">×</button>
        </header>

        <div class="tag-overlay__modes" role="group" aria-label="标签组合方式">
          <button type="button" :class="{ 'is-active': mode === 'single' }" @click="switchMode('single')">
            单标签
            <small>选一个，看这个标签下的文章</small>
          </button>
          <button type="button" :class="{ 'is-active': mode === 'multi' }" @click="switchMode('multi')">
            多标签
            <small>选多个，命中任一标签的文章</small>
          </button>
        </div>

        <div class="tag-overlay__search">
          <input
            ref="searchInput"
            v-model="keyword"
            type="search"
            placeholder="搜索标签名称"
            aria-label="搜索标签名称"
          />
          <span>{{ matched.length }} / {{ tags.length }}</span>
        </div>

        <div class="tag-overlay__body">
          <p v-if="!tags.length" class="tag-overlay__empty">还没有可用标签。</p>
          <p v-else-if="!matched.length" class="tag-overlay__empty">没有匹配的标签，换个词试试。</p>
          <div v-else class="tag-overlay__list">
            <button
              v-for="tag in matched"
              :key="tag.id || tag.slug"
              type="button"
              :class="{ 'is-picked': isPicked(tag.slug) }"
              :aria-pressed="isPicked(tag.slug)"
              :aria-label="`${tag.name}，${tag.postCount ?? 0} 篇博客`"
              :title="`${tag.name} · ${tag.postCount ?? 0} 篇博客`"
              @click="toggle(tag.slug)"
            >
              <span class="tag-overlay__mark" aria-hidden="true">{{ isPicked(tag.slug) ? '✓' : '' }}</span>
              <span class="tag-overlay__name">{{ tag.name }}</span>
              <em>{{ tag.postCount || 0 }}</em>
            </button>
          </div>
        </div>

        <footer class="tag-overlay__foot">
          <div class="tag-overlay__picked">
            <span v-if="!picked.length" class="tag-overlay__hint">未选择标签，应用后显示所有文章。</span>
            <template v-else>
              <span>已选 {{ picked.length }} 个：</span>
              <button v-for="slug in picked" :key="slug" type="button" class="tag-overlay__chip" @click="toggle(slug)">
                {{ nameOf(slug) }} ×
              </button>
            </template>
          </div>
          <div class="tag-overlay__actions">
            <button type="button" @click="clearPicked">清空</button>
            <button class="primary-button" type="button" @click="apply">应用筛选</button>
          </div>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.tag-overlay {
  position: fixed;
  z-index: var(--z-search-sheet, 100);
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgb(12 22 18 / 0.42);
  backdrop-filter: blur(2px);
}

.tag-overlay__card {
  display: flex;
  width: min(760px, 100%);
  max-height: min(82vh, 760px);
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: 22px;
  background: var(--bg-surface);
  box-shadow: var(--shadow-floating, 0 28px 80px rgb(20 38 31 / 0.28));
  overflow: hidden;
}

.tag-overlay__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 16px;
  border-bottom: 1px solid var(--border);
}

.tag-overlay__eyebrow {
  margin-bottom: 6px;
  color: var(--accent);
  font: 750 10px/1.4 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  letter-spacing: 0.16em;
}

.tag-overlay__head h2 { margin-bottom: 4px; font-size: 24px; letter-spacing: -0.03em; }

.tag-overlay__head span { color: var(--text-muted); font-size: 12px; }

.tag-overlay__close {
  flex: none;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.tag-overlay__close:hover { border-color: var(--primary); color: var(--primary); }

.tag-overlay__modes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 14px 22px 0;
}

.tag-overlay__modes button {
  display: grid;
  gap: 3px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
  text-align: left;
}

.tag-overlay__modes button small { color: var(--text-muted); font-size: 10px; font-weight: 400; }

.tag-overlay__modes button:hover { border-color: color-mix(in srgb, var(--primary) 45%, var(--border)); }

.tag-overlay__modes button.is-active {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.tag-overlay__search {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 22px;
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg-subtle) 45%, transparent);
}

.tag-overlay__search input { flex: 1; min-width: 0; }

.tag-overlay__search > span { flex: none; color: var(--text-muted); font-size: 11px; }

.tag-overlay__body { min-height: 150px; flex: 1; overflow: auto; padding: 16px 22px; }

.tag-overlay__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(176px, 1fr));
  gap: 8px;
}

.tag-overlay__list button {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 9px 11px;
  border: 1px solid var(--border);
  border-radius: 11px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  cursor: pointer;
  text-align: left;
  transition: color 160ms ease, border-color 160ms ease, background-color 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}

.tag-overlay__list button:hover { border-color: color-mix(in srgb, var(--primary) 45%, var(--border)); }

.tag-overlay__list button.is-picked {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  font-weight: 650;
}

.tag-overlay__mark {
  display: inline-grid;
  place-items: center;
  flex: none;
  width: 16px;
  height: 16px;
  border: 1px solid var(--border-strong);
  border-radius: 5px;
  color: transparent;
  font-size: 11px;
  line-height: 1;
  transition: color 160ms ease, background-color 160ms ease, border-color 160ms ease;
}

.tag-overlay__list button.is-picked .tag-overlay__mark {
  border-color: var(--primary);
  color: var(--on-primary);
  background: var(--primary);
}

.tag-overlay__list button:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }

@media (hover: hover) {
  .tag-overlay__list button:hover {
    transform: translateY(-2px);
    box-shadow: 0 3px 8px color-mix(in srgb, var(--primary) 12%, transparent);
  }
}

.tag-overlay__list button:active { transform: scale(0.98); }

@media (prefers-reduced-motion: reduce) {
  .tag-overlay__list button,
  .tag-overlay__mark { transition: none; }
  .tag-overlay__list button:hover,
  .tag-overlay__list button:active { transform: none; }
}

.tag-overlay__name {
  overflow: hidden;
  min-width: 0;
  flex: 1;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-overlay__list em {
  display: inline-grid;
  place-items: center;
  flex: none;
  min-width: 22px;
  height: 22px;
  padding: 0 5px;
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-subtle);
  font-size: 10px;
  font-style: normal;
  font-variant-numeric: tabular-nums;
}

.tag-overlay__list button.is-picked em {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 14%, var(--bg-surface));
}

.tag-overlay__empty { padding: 40px 0; color: var(--text-muted); font-size: 13px; text-align: center; }

.tag-overlay__foot {
  display: grid;
  gap: 10px;
  padding: 14px 22px;
  border-top: 1px solid var(--border);
}

.tag-overlay__picked {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-height: 26px;
  font-size: 11px;
  color: var(--text-muted);
}

.tag-overlay__chip {
  padding: 3px 8px;
  border: 1px solid color-mix(in srgb, var(--primary) 35%, var(--border));
  border-radius: 999px;
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  font-size: 11px;
  cursor: pointer;
}

.tag-overlay__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.tag-overlay__actions button {
  padding: 8px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  font-size: 13px;
  cursor: pointer;
}

.tag-overlay__actions button:hover { border-color: var(--primary); color: var(--primary); }

.tag-overlay__actions .primary-button {
  border-color: var(--primary);
  color: var(--on-primary);
  background: var(--primary);
  font-weight: 650;
}

.tag-overlay__actions .primary-button:hover { background: var(--primary-hover); color: var(--on-primary); }

.tag-overlay__actions .primary-button:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 680px) {
  .tag-overlay { padding: 12px; }

  .tag-overlay__card { max-height: 88vh; border-radius: 18px; }

  .tag-overlay__search,
  .tag-overlay__body,
  .tag-overlay__head,
  .tag-overlay__foot,
  .tag-overlay__modes { padding-left: 16px; padding-right: 16px; }

  .tag-overlay__modes { grid-template-columns: 1fr; }

  .tag-overlay__list { grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); }

  .tag-overlay__actions { justify-content: stretch; }

  .tag-overlay__actions button { flex: 1 1 40%; }
}
</style>
