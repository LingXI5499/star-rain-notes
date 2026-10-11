<script setup>
/**
 * BlogTagOverlay.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue。
 * 组件输入：open、tags、activeTags；默认值与类型见 defineProps。
 * 组件输出事件：close、apply；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、keyword、mode、picked、searchInput、matched、totalRelations。
 * 阅读入口：nameOf（名称所属的处理函数）；isPicked（判断是否Picked）；toggle（切换当前操作）；switchMode（switchMode的处理函数）；clearPicked（清理Picked）。
 */


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
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  tags: { type: Array, default: () => [] },
  // 当前生效的标签集合（来自 URL）
  activeTags: { type: Array, default: () => [] },
})

/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['close', 'apply'])

/**
 * 关键词；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const keyword = ref('')
/**
 * mode；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const mode = ref('single')
/**
 * picked；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const picked = ref([])
/**
 * 搜索Input；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const searchInput = ref(null)

/**
 * matched；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const matched = computed(() => {
  const term = keyword.value.trim().toLocaleLowerCase()
  if (!term) return props.tags
  return props.tags.filter((tag) => tag.name.toLocaleLowerCase().includes(term))
})

// 总引用次数：卡片右上角显示「50 个标签 · 58 次引用」，与标签管理页的口径一致
/**
 * 总数Relations；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const totalRelations = computed(() => props.tags.reduce((sum, tag) => sum + (tag.postCount || 0), 0))
/**
 * pickedNames；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pickedNames = computed(() => picked.value.map((slug) => nameOf(slug)))

/**
 * 名称所属的处理函数。
 * 主要协作：props.tags.find。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function nameOf(slug) {
  return props.tags.find((tag) => tag.slug === slug)?.name || slug
}

/**
 * 判断是否Picked。
 * 主要协作：picked.value.includes。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
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

/**
 * 切换当前操作。
 * 主要协作：isPicked、picked.value.filter。
 * 更新的响应式状态包括 picked；渲染由 Vue 的依赖跟踪触发。
 * @param slug 公开固定链接的路径标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
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

/**
 * switchMode的处理函数。
 * 主要协作：picked.value.slice。
 * 更新的响应式状态包括 mode、picked；渲染由 Vue 的依赖跟踪触发。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function switchMode(next) {
  mode.value = next
  // 从多标签切回单标签时只留第一个，避免出现「单标签模式下选中了 3 个」的矛盾状态
  if (next === 'single' && picked.value.length > 1) picked.value = picked.value.slice(0, 1)
}

/**
 * 清理Picked。
 * 更新的响应式状态包括 picked；渲染由 Vue 的依赖跟踪触发。
 */
function clearPicked() {
  picked.value = []
}

/**
 * 应用当前操作。
 * 主要协作：emit。
 */
function apply() {
  emit('apply', [...picked.value])
}

/**
 * 关闭当前操作。
 * 主要协作：emit。
 */
function close() {
  emit('close')
}

/**
 * 响应Keydown。
 * 主要协作：close。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function onKeydown(event) {
  if (event.key === 'Escape' && props.open) close()
}

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
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
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => props.open, (value) => {
  if (value) document.addEventListener('keydown', onKeydown)
  else document.removeEventListener('keydown', onKeydown)
})

/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.tag-overlay。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
 * 相对视口定位，用于浮层或固定操作区域；层叠顺序由 z-index 决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.tag-overlay__card。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.tag-overlay__head。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 16px;
  border-bottom: 1px solid var(--border);
}

/**
 * 样式范围：.tag-overlay__eyebrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__eyebrow {
  margin-bottom: 6px;
  color: var(--accent);
  font: 750 10px/1.4 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  letter-spacing: 0.16em;
}

/**
 * 样式范围：.tag-overlay__head h2。
 */
.tag-overlay__head h2 { margin-bottom: 4px; font-size: 24px; letter-spacing: -0.03em; }

/**
 * 样式范围：.tag-overlay__head span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__head span { color: var(--text-muted); font-size: 12px; }

/**
 * 样式范围：.tag-overlay__close。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.tag-overlay__close:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__close:hover { border-color: var(--primary); color: var(--primary); }

/**
 * 样式范围：.tag-overlay__modes。
 * 使用网格布局组织内容；列定义为 repeat(2, minmax(0, 1fr))，间距由 10px 控制。
 */
.tag-overlay__modes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 14px 22px 0;
}

/**
 * 样式范围：.tag-overlay__modes button。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 3px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.tag-overlay__modes button small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__modes button small { color: var(--text-muted); font-size: 10px; font-weight: 400; }

/**
 * 样式范围：.tag-overlay__modes button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__modes button:hover { border-color: color-mix(in srgb, var(--primary) 45%, var(--border)); }

/**
 * 样式范围：.tag-overlay__modes button.is-active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__modes button.is-active {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

/**
 * 样式范围：.tag-overlay__search。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__search {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 22px;
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg-subtle) 45%, transparent);
}

/**
 * 样式范围：.tag-overlay__search input。
 */
.tag-overlay__search input { flex: 1; min-width: 0; }

/**
 * 样式范围：.tag-overlay__search > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__search > span { flex: none; color: var(--text-muted); font-size: 11px; }

/**
 * 样式范围：.tag-overlay__body。
 * 溢出处理：整体 auto。
 */
.tag-overlay__body { min-height: 150px; flex: 1; overflow: auto; padding: 16px 22px; }

/**
 * 样式范围：.tag-overlay__list。
 * 使用网格布局组织内容；列定义为 repeat(auto-fill, minmax(176px, 1fr))，间距由 8px 控制。
 */
.tag-overlay__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(176px, 1fr));
  gap: 8px;
}

/**
 * 样式范围：.tag-overlay__list button。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.tag-overlay__list button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__list button:hover { border-color: color-mix(in srgb, var(--primary) 45%, var(--border)); }

/**
 * 样式范围：.tag-overlay__list button.is-picked。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__list button.is-picked {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  font-weight: 650;
}

/**
 * 样式范围：.tag-overlay__mark。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.tag-overlay__list button.is-picked .tag-overlay__mark。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__list button.is-picked .tag-overlay__mark {
  border-color: var(--primary);
  color: var(--on-primary);
  background: var(--primary);
}

/**
 * 样式范围：.tag-overlay__list button:focus-visible。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__list button:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }

/**
 * 响应式或设备条件：(hover: hover)；仅在该条件成立时覆盖基础样式。
 */
@media (hover: hover) {
  /**
   * 样式范围：.tag-overlay__list button:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .tag-overlay__list button:hover {
    transform: translateY(-2px);
    box-shadow: 0 3px 8px color-mix(in srgb, var(--primary) 12%, transparent);
  }
}

/**
 * 样式范围：.tag-overlay__list button:active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.tag-overlay__list button:active { transform: scale(0.98); }

/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.tag-overlay__list button,   .tag-overlay__mark。
   */
  .tag-overlay__list button,
  .tag-overlay__mark { transition: none; }
  /**
   * 样式范围：.tag-overlay__list button:hover,   .tag-overlay__list button:active。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .tag-overlay__list button:hover,
  .tag-overlay__list button:active { transform: none; }
}

/**
 * 样式范围：.tag-overlay__name。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 */
.tag-overlay__name {
  overflow: hidden;
  min-width: 0;
  flex: 1;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 样式范围：.tag-overlay__list em。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.tag-overlay__list button.is-picked em。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__list button.is-picked em {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 14%, var(--bg-surface));
}

/**
 * 样式范围：.tag-overlay__empty。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__empty { padding: 40px 0; color: var(--text-muted); font-size: 13px; text-align: center; }

/**
 * 样式范围：.tag-overlay__foot。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 10px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__foot {
  display: grid;
  gap: 10px;
  padding: 14px 22px;
  border-top: 1px solid var(--border);
}

/**
 * 样式范围：.tag-overlay__picked。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__picked {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-height: 26px;
  font-size: 11px;
  color: var(--text-muted);
}

/**
 * 样式范围：.tag-overlay__chip。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__chip {
  padding: 3px 8px;
  border: 1px solid color-mix(in srgb, var(--primary) 35%, var(--border));
  border-radius: 999px;
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  font-size: 11px;
  cursor: pointer;
}

/**
 * 样式范围：.tag-overlay__actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tag-overlay__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

/**
 * 样式范围：.tag-overlay__actions button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__actions button {
  padding: 8px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  font-size: 13px;
  cursor: pointer;
}

/**
 * 样式范围：.tag-overlay__actions button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__actions button:hover { border-color: var(--primary); color: var(--primary); }

/**
 * 样式范围：.tag-overlay__actions .primary-button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__actions .primary-button {
  border-color: var(--primary);
  color: var(--on-primary);
  background: var(--primary);
  font-weight: 650;
}

/**
 * 样式范围：.tag-overlay__actions .primary-button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-overlay__actions .primary-button:hover { background: var(--primary-hover); color: var(--on-primary); }

/**
 * 样式范围：.tag-overlay__actions .primary-button:disabled。
 */
.tag-overlay__actions .primary-button:disabled { opacity: 0.5; cursor: not-allowed; }

/**
 * 响应式或设备条件：(max-width: 680px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 680px) {
  /**
   * 样式范围：.tag-overlay。
   */
  .tag-overlay { padding: 12px; }

  /**
   * 样式范围：.tag-overlay__card。
   */
  .tag-overlay__card { max-height: 88vh; border-radius: 18px; }

  /**
   * 样式范围：.tag-overlay__search,   .tag-overlay__body,   .tag-overlay__head,   .tag-overlay__foot,   .tag-overlay__modes。
   */
  .tag-overlay__search,
  .tag-overlay__body,
  .tag-overlay__head,
  .tag-overlay__foot,
  .tag-overlay__modes { padding-left: 16px; padding-right: 16px; }

  /**
   * 样式范围：.tag-overlay__modes。
   */
  .tag-overlay__modes { grid-template-columns: 1fr; }

  /**
   * 样式范围：.tag-overlay__list。
   */
  .tag-overlay__list { grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); }

  /**
   * 样式范围：.tag-overlay__actions。
   */
  .tag-overlay__actions { justify-content: stretch; }

  /**
   * 样式范围：.tag-overlay__actions button。
   */
  .tag-overlay__actions button { flex: 1 1 40%; }
}
</style>
