<script setup>
/**
 * ArticleOutline.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 目录项来自 Markdown 标题；以最后一个越过顶部偏移线的标题作为当前阅读位置。
 * 滚动事件通过 requestAnimationFrame 合并；普通目录和课程目录风格共享同一跳转及高亮逻辑。
 * 主要依赖：vue。
 * 组件输入：items、hideTitle、embedded、navigationStyle；默认值与类型见 defineProps。
 * 主要响应式状态：props、groups、activeId、sections。
 * 阅读入口：onScroll（响应滚动）；refresh（刷新当前操作）；scrollTo（滚动目标）。
 */


import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

/*
 * 文章目录（TOC）—— 从 V1 `components/ArticleOutline.vue` 移植成 JS。
 *
 * 滚动高亮用的是「最后一个越过偏移线的标题」：比 IntersectionObserver 更贴近阅读直觉，
 * 也不会在长标题上抖动。滚动监听用 requestAnimationFrame 节流，避免每帧都重排。
 * 偏移量 104px ≈ 吸顶导航高度 + 一点余量，保证被高亮的正是刚读到的那个标题。
 */
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  items: { type: Array, default: () => [] },
  // 父级已经给了「本页导航」标题时，不再重复渲染自带的「目录」
  hideTitle: { type: Boolean, default: false },
  // 嵌在粘性父级里时，自己不再 sticky（只有最外层需要）
  embedded: { type: Boolean, default: false },
  // 博客和作品的左栏采用教程课程目录的分组、编号和选中背景。
  navigationStyle: { type: String, default: 'outline' },
})

/**
 * 分组列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const groups = computed(() => {
  const baseLevel = Math.min(...props.items.map(item => Number(item.level) || 2))
  const result = []
  for (const item of props.items) {
    if (!result.length || (Number(item.level) || 2) <= baseLevel) {
      result.push({ heading: item, children: [] })
    } else {
      result[result.length - 1].children.push(item)
    }
  }
  return result
})
const flat = computed(() => groups.value.length > 0 && groups.value.every(group => !group.children.length))

/**
 * 有效标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const activeId = ref('')
/**
 * sections；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const sections = ref([])
let ticking = false

/**
 * 响应滚动。
 * 主要协作：requestAnimationFrame、element.getBoundingClientRect。
 * 更新的响应式状态包括 activeId；渲染由 Vue 的依赖跟踪触发。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    const offset = 104
    let current = ''
    for (const element of sections.value) {
      if (element.getBoundingClientRect().top <= offset) current = element.id
      else break
    }
    activeId.value = current
    ticking = false
  })
}

/**
 * 刷新当前操作。
 * 主要协作：nextTick、.filter、props.items.map、document.getElementById、onScroll。
 * 更新的响应式状态包括 sections；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function refresh() {
  await nextTick()
  sections.value = props.items
    .map((item) => document.getElementById(item.id))
    .filter((element) => element !== null)
  onScroll()
}

/**
 * 滚动目标。
 * 主要协作：window.matchMedia、.scrollIntoView、document.getElementById。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 */
function scrollTo(id) {
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  document.getElementById(id)?.scrollIntoView({ behavior, block: 'start' })
}

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => props.items, refresh, { deep: true })

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  void refresh()
  window.addEventListener('scroll', onScroll, { passive: true })
})

/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <nav
    v-if="items.length"
    class="article-outline"
    :class="{ 'article-outline--embedded': embedded, 'article-outline--curriculum': navigationStyle === 'curriculum' }"
    aria-label="文章目录"
  >
    <p v-if="!hideTitle" class="article-outline__title">目录</p>
    <ol v-if="navigationStyle === 'curriculum' && flat" class="article-outline__chapters">
      <li v-for="(item, index) in items" :key="item.id">
        <a class="article-outline__chapter" :class="{ 'is-active': activeId === item.id }" :href="`#${item.id}`"
          :title="item.text" :aria-current="activeId === item.id ? 'location' : undefined" @click.prevent="scrollTo(item.id)">
          <span class="article-outline__index">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="article-outline__chapter-title">{{ item.text }}</span>
        </a>
      </li>
    </ol>
    <div v-else-if="navigationStyle === 'curriculum'" class="article-outline__groups">
      <section v-for="group in groups" :key="group.heading.id" class="article-outline__group">
        <a class="article-outline__group-title" :class="{ 'is-active': activeId === group.heading.id }"
          :href="`#${group.heading.id}`" :title="group.heading.text"
          :aria-current="activeId === group.heading.id ? 'location' : undefined"
          @click.prevent="scrollTo(group.heading.id)">{{ group.heading.text }}</a>
        <ol v-if="group.children.length" class="article-outline__chapters">
          <li v-for="(item, index) in group.children" :key="item.id">
            <a class="article-outline__chapter" :class="{ 'is-active': activeId === item.id }"
              :href="`#${item.id}`" :title="item.text"
              :style="{ '--outline-indent': `${Math.max(0, Number(item.level) - Number(group.heading.level) - 1) * 10}px` }"
              :aria-current="activeId === item.id ? 'location' : undefined"
              @click.prevent="scrollTo(item.id)">
              <span class="article-outline__index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="article-outline__chapter-title">{{ item.text }}</span>
            </a>
          </li>
        </ol>
      </section>
    </div>
    <ul v-else class="article-outline__list">
      <li
        v-for="item in items"
        :key="item.id"
        class="article-outline__item"
        :class="[`article-outline__item--h${item.level}`, { 'is-active': activeId === item.id }]"
      >
        <a
          :href="`#${item.id}`"
          :aria-current="activeId === item.id ? 'location' : undefined"
          @click.prevent="scrollTo(item.id)"
        >{{ item.text }}</a>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.article-outline。
 * 在正常文档流中保留位置，吸附偏移由 top=calc(var(--header-height) + var(--space-6)) 决定；父级高度限制吸附范围。
 * 溢出处理：纵向 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  max-height: calc(100vh - var(--header-height) - var(--space-12));
  overflow-y: auto;
  font-size: 13px;
  scrollbar-width: thin;
  scrollbar-color: var(--border-strong) transparent;
}

/* 滚动条按 V1 收细：目录列只有 165–220px 宽，系统默认滚动条会挡住标题文字 */
.article-outline::-webkit-scrollbar {
  width: 8px;
}

/**
 * 样式范围：.article-outline::-webkit-scrollbar-thumb。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline::-webkit-scrollbar-thumb {
  background: var(--border-strong);
  border-radius: 999px;
  border: 2px solid transparent;
  background-clip: content-box;
}

/**
 * 样式范围：.article-outline::-webkit-scrollbar-track。
 */
.article-outline::-webkit-scrollbar-track {
  background: transparent;
}

/**
 * 样式范围：.article-outline::-webkit-scrollbar-button。
 */
.article-outline::-webkit-scrollbar-button {
  display: none;
  width: 0;
  height: 0;
}

/**
 * 样式范围：.article-outline--embedded。
 */
.article-outline--embedded {
  position: static;
  top: auto;
  max-height: min(52vh, 420px);
}

/**
 * 样式范围：.article-outline__title。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__title {
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: var(--space-3);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

/**
 * 样式范围：.article-outline__list。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__list {
  list-style: none;
  border-left: 1px solid var(--border);
}

/**
 * 样式范围：.article-outline__item a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__item a {
  display: block;
  color: var(--text-secondary);
  padding: 4px var(--space-4);
  border-left: 2px solid transparent;
  margin-left: -1px;
  line-height: 1.5;
}

/**
 * 样式范围：.article-outline__item a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__item a:hover {
  color: var(--primary);
}

/**
 * 样式范围：.article-outline__item.is-active a。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__item.is-active a {
  color: var(--primary);
  border-left-color: var(--primary);
}

/**
 * 样式范围：.article-outline__item--h1 a。
 */
.article-outline__item--h1 a { font-weight: 700; }
/**
 * 样式范围：.article-outline__item--h2 a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__item--h2 a { font-weight: 600; padding-left: calc(var(--space-4) + 12px); }
/**
 * 样式范围：.article-outline__item--h3 a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__item--h3 a { padding-left: calc(var(--space-4) + 24px); }
/**
 * 样式范围：.article-outline__item--h4 a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__item--h4 a { padding-left: calc(var(--space-4) + 36px); }
/**
 * 样式范围：.article-outline__item--h5 a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__item--h5 a { padding-left: calc(var(--space-4) + 48px); }
/**
 * 样式范围：.article-outline__item--h6 a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__item--h6 a { padding-left: calc(var(--space-4) + 60px); color: var(--text-muted); }

/**
 * 样式范围：.article-outline__groups。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 */
.article-outline__groups { display: flex; flex-direction: column; gap: 20px; }
/**
 * 样式范围：.article-outline__group-title。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__group-title {
  display: block;
  padding: 7px 10px;
  border-left: 2px solid transparent;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: .04em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/**
 * 样式范围：.article-outline__chapters。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__chapters { margin: 0; padding: 0; list-style: none; border-left: 1px solid var(--border); }
/**
 * 样式范围：.article-outline__chapter。
 * 使用网格布局组织内容；列定义为 22px minmax(0, 1fr)，间距由 7px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__chapter {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  align-items: center;
  gap: 7px;
  min-height: 34px;
  margin-left: -1px;
  padding: 6px 8px 6px calc(12px + var(--outline-indent, 0px));
  border-left: 2px solid transparent;
  color: var(--text-secondary);
  font-size: 12.5px;
  line-height: 1.45;
  transition: color 150ms ease, background-color 150ms ease, border-color 150ms ease;
}
/**
 * 样式范围：.article-outline__index。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__index { color: var(--text-muted); font: 600 9px/1 ui-monospace, SFMono-Regular, Menlo, monospace; }
/**
 * 样式范围：.article-outline__chapter-title。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 */
.article-outline__chapter-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/**
 * 样式范围：.article-outline--curriculum a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline--curriculum a:hover { color: var(--primary); background: color-mix(in srgb, var(--primary) 5%, transparent); }
/**
 * 样式范围：.article-outline--curriculum a.is-active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline--curriculum a.is-active { border-left-color: var(--primary); color: var(--primary); background: color-mix(in srgb, var(--primary) 8%, transparent); font-weight: 650; }
/**
 * 样式范围：.article-outline__chapter.is-active .article-outline__index。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline__chapter.is-active .article-outline__index { color: var(--primary); }
/**
 * 样式范围：.article-outline--curriculum a:focus-visible。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.article-outline--curriculum a:focus-visible { outline: 2px solid color-mix(in srgb, var(--primary) 40%, transparent); outline-offset: -2px; }
</style>
