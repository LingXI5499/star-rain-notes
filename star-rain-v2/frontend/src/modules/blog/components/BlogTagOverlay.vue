<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

/*
 * 全部标签的悬浮卡片。
 *
 * 用户的要求：「点击全部标签给我跳转了一个新页面，不好，应该是给个悬浮在这个核心页面上的，
 * 然后也有这个搜索展示，完整的一个小卡片」。
 * 所以它不跳路由、不请求数据：标签列表由列表页一次性取好传进来，
 * 这里只负责搜索、展示与「点一个标签就原地筛选下方列表」。
 *
 * 交互要点：
 *   - Teleport 到 body：卡片要盖在整页之上，不能被侧栏的 overflow 裁掉；
 *   - Esc / 点遮罩 / 点右上角关闭；
 *   - 打开时把焦点给搜索框，全站搜索之外再点一次就关；
 *   - 选中标签后由父组件关闭（父组件要负责换列表，卡片留着会挡住结果）。
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  tags: { type: Array, default: () => [] },
  activeTag: { type: String, default: '' },
})

const emit = defineEmits(['close', 'select-tag'])

const keyword = ref('')
const searchInput = ref(null)

const matched = computed(() => {
  const term = keyword.value.trim().toLocaleLowerCase()
  if (!term) return props.tags
  return props.tags.filter((tag) => tag.name.toLocaleLowerCase().includes(term))
})

// 总引用次数：卡片右上角显示「50 个标签 · 58 次引用」，与标签管理页的口径一致
const totalRelations = computed(() => props.tags.reduce((sum, tag) => sum + (tag.postCount || 0), 0))

function close() {
  emit('close')
}

function select(tag) {
  emit('select-tag', props.activeTag === tag.slug ? '' : tag.slug)
}

function onKeydown(event) {
  if (event.key === 'Escape' && props.open) close()
}

watch(() => props.open, async (value) => {
  if (value) {
    keyword.value = ''
    await nextTick()
    searchInput.value?.focus()
  }
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
            <p class="tag-overlay__eyebrow">TAGS · 标签总览</p>
            <h2 id="tag-overlay-title">全部标签</h2>
            <span>{{ tags.length }} 个标签 · {{ totalRelations }} 次文章引用</span>
          </div>
          <button class="tag-overlay__close" type="button" aria-label="关闭全部标签" @click="close">×</button>
        </header>

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
              :class="{ 'is-active': activeTag === tag.slug }"
              @click="select(tag)"
            >
              <span># {{ tag.name }}</span>
              <em>{{ tag.postCount || 0 }}</em>
            </button>
          </div>
        </div>

        <footer class="tag-overlay__foot">
          <button type="button" :class="{ 'is-active': !activeTag }" @click="emit('select-tag', '')">全部（不筛选）</button>
          <span>点任意标签即在该标签下原地筛选，不会离开这一页。</span>
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
  width: min(720px, 100%);
  max-height: min(78vh, 720px);
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

.tag-overlay__body { min-height: 160px; flex: 1; overflow: auto; padding: 16px 22px; }

.tag-overlay__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
  gap: 8px;
}

.tag-overlay__list button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  padding: 9px 11px;
  border: 1px solid var(--border);
  border-radius: 11px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  cursor: pointer;
  text-align: left;
  transition: color 160ms ease, border-color 160ms ease, background-color 160ms ease;
}

.tag-overlay__list button:hover,
.tag-overlay__list button.is-active {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.tag-overlay__list button > span {
  overflow: hidden;
  min-width: 0;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-overlay__list em { color: var(--text-muted); font-size: 10px; font-style: normal; }

.tag-overlay__empty { padding: 40px 0; color: var(--text-muted); font-size: 13px; text-align: center; }

.tag-overlay__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 22px;
  border-top: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 11px;
}

.tag-overlay__foot button {
  padding: 7px 12px;
  border: 1px solid var(--border);
  border-radius: 9px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  font-size: 12px;
  cursor: pointer;
}

.tag-overlay__foot button:hover,
.tag-overlay__foot button.is-active { border-color: var(--primary); color: var(--primary); }

@media (max-width: 680px) {
  .tag-overlay { padding: 12px; }

  .tag-overlay__card { max-height: 86vh; border-radius: 18px; }

  .tag-overlay__search,
  .tag-overlay__body,
  .tag-overlay__head,
  .tag-overlay__foot { padding-left: 16px; padding-right: 16px; }

  .tag-overlay__list { grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); }

  /* 窄屏底部一行放不下「按钮 + 说明」，竖着排比挤成两行好看 */
  .tag-overlay__foot { align-items: flex-start; flex-direction: column; }
}
</style>
