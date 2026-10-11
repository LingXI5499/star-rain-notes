<script setup>
/**
 * BlogColumnNav.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、vue-router。
 * 组件输入：columns、activeKey、ariaLabel；默认值与类型见 defineProps。
 * 主要响应式状态：props、row、probe、visibleKeys、open、pill、visible、overflow。
 * 阅读入口：placePill（placePill的处理函数）；measure（measure的处理函数）；gapWidth（gap宽度的处理函数）；closeOnOutside（关闭触发Outside）；closeOnEscape（关闭触发Escape）。
 */


import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

/*
 * 博客列表顶部的导航条（专栏栏 / 标签栏共用）。
 *
 * 需求是「放得下的平铺，放不下的收进更多」，而且平铺的那部分要**正好占满一行**——
 * 所以这里是真的按像素测量，而不是写死「显示前 6 个」：
 *   1. 一个不可见的探针容器把**全部**条目渲染一遍（width:max-content，不受行宽限制），
 *      量出每个条目的真实宽度与「更多」按钮的宽度；
 *   2. 可见行按顺序累加，直到再加一个就放不下「更多」为止，其余进下拉。
 *
 * 为什么不用纯 CSS：`overflow:hidden` 会把第 7 个条目切一半，看起来像坏了；
 * 只在 CSS 里限宽也做不到「刚好填满」。
 *
 * 条目**不跳新页面**：调用方传进来的 to 指向同一个列表页、只带不同的筛选查询
 * （`?topic=` / `?tag=`），点一下就是「原地切换下方的列表」。
 *
 * 选中态是一枚会滑动的底片：位置由当前选中项实测得到，
 * 切换专栏时它从旧位置滑到新位置，而不是硬切背景色。
 */
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  // [{ key, label, to }]，key 用于判断选中态，to 交给 RouterLink
  columns: { type: Array, default: () => [] },
  activeKey: { type: String, default: '' },
  ariaLabel: { type: String, default: '博客专栏' },
})

/**
 * 行；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const row = ref(null)
/**
 * 测试样本；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const probe = ref(null)
/**
 * 可见Keys；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const visibleKeys = ref(['home'])
/**
 * open；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const open = ref(false)
/**
 * pill；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pill = ref({ left: 0, top: 0, width: 0, height: 0, ready: false })

/**
 * 可见；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const visible = computed(() => props.columns.filter((item) => visibleKeys.value.includes(item.key)))
/**
 * 越界；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const overflow = computed(() => props.columns.filter((item) => !visibleKeys.value.includes(item.key)))
// 选中项被收进「更多」时，行内没有底片，改为照亮「更多」按钮
/**
 * 有效内部越界；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const activeInOverflow = computed(() => overflow.value.some((item) => item.key === props.activeKey))

const GAP = 8

/*
 * 底片位置只能实测：条目宽度随文案和字体变化，写死 translate 到第 N 个位置一定会错。
 * 选中项不在可见行（被收进「更多」）时直接藏起来。
 */
function placePill() {
  const rowEl = row.value
  const activeEl = rowEl?.querySelector('.column-nav__item--active')
  if (!rowEl || !activeEl) {
    pill.value = { ...pill.value, ready: false }
    return
  }
  pill.value = {
    left: activeEl.offsetLeft,
    top: activeEl.offsetTop,
    width: activeEl.offsetWidth,
    height: activeEl.offsetHeight,
    ready: true,
  }
}

/**
 * measure的处理函数。
 * 主要协作：nextTick、rowEl.getBoundingClientRect、probeEl.querySelectorAll、probeEl.querySelector、itemEls.map。
 * 更新的响应式状态包括 visibleKeys、open；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function measure() {
  await nextTick()
  const rowEl = row.value
  const probeEl = probe.value
  if (!rowEl || !probeEl || !props.columns.length) return

  const available = rowEl.getBoundingClientRect().width || rowEl.parentElement?.clientWidth || window.innerWidth
  const itemEls = [...probeEl.querySelectorAll('[data-probe-item]')]
  const moreEl = probeEl.querySelector('[data-probe-more]')
  const widths = itemEls.map((el) => el.getBoundingClientRect().width)
  /**
   * gap宽度的处理函数。
   * @param count 数量；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  const gapWidth = (count) => Math.max(0, count - 1) * GAP
  const totalWidth = widths.reduce((sum, width) => sum + width, 0) + gapWidth(widths.length)

  // 全部放得下就不显示「更多」
  if (totalWidth <= available) {
    visibleKeys.value = props.columns.map((item) => item.key)
    open.value = false
    await nextTick()
    placePill()
    return
  }

  const moreWidth = (moreEl ? moreEl.getBoundingClientRect().width : 0) + GAP
  // 空间不足时优先保留精选专题；同级专题依然沿用后台保存的真实顺序。
  // 首页固定在第一位，菜单里则保留所有未显示专题的真实顺序。
  const prioritized = props.columns.map((column, index) => ({ column, index }))
    .sort((a, b) => {
      if (a.index === 0) return -1
      if (b.index === 0) return 1
      return Number(Boolean(b.column.featured)) - Number(Boolean(a.column.featured)) || a.index - b.index
    })
  let used = 0
  const chosen = []
  for (const { column, index } of prioritized) {
    const next = used + widths[index] + (chosen.length > 0 ? GAP : 0)
    if (next + moreWidth > available && chosen.length > 0) continue
    used = next
    chosen.push(column.key)
  }
  visibleKeys.value = chosen
  if (overflow.value.length === 0) open.value = false
  await nextTick()
  placePill()
}

let observer = null
/**
 * 关闭触发Outside。
 * 主要协作：root.contains。
 * 更新的响应式状态包括 open；渲染由 Vue 的依赖跟踪触发。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function closeOnOutside(event) {
  if (!open.value) return
  const root = row.value?.parentElement
  if (root && !root.contains(event.target)) open.value = false
}
/**
 * 关闭触发Escape。
 * 更新的响应式状态包括 open；渲染由 Vue 的依赖跟踪触发。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 */
function closeOnEscape(event) {
  if (event.key === 'Escape') open.value = false
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(async () => {
  await measure()
  if (typeof ResizeObserver !== 'undefined' && row.value) {
    observer = new ResizeObserver(() => { void measure() })
    observer.observe(row.value)
  }
  document.addEventListener('click', closeOnOutside)
  document.addEventListener('keydown', closeOnEscape)
})

/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => {
  observer?.disconnect()
  document.removeEventListener('click', closeOnOutside)
  document.removeEventListener('keydown', closeOnEscape)
})

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => props.columns.map((item) => `${item.key}:${item.label}:${item.featured}`).join('|'), () => { void measure() })
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => props.activeKey, async () => { await nextTick(); placePill() })
</script>

<template>
  <nav class="column-nav" :aria-label="ariaLabel">
    <div ref="row" class="column-nav__row">
      <!--
        滑动底片。只在量到位置后渲染：挂载时就带着正确坐标，因此首帧不会从左上角滑进来。
        aria-hidden：它纯装饰，读屏信息由条目的 aria-current 表达。
      -->
      <span
        v-if="pill.ready"
        class="column-nav__pill"
        aria-hidden="true"
        :style="{
          width: `${pill.width}px`,
          height: `${pill.height}px`,
          transform: `translate(${pill.left}px, ${pill.top}px)`,
        }"
      />

      <RouterLink
        v-for="column in visible"
        :key="column.key"
        :to="column.to"
        class="column-nav__item"
        :class="{ 'column-nav__item--active': activeKey === column.key }"
        :aria-current="activeKey === column.key ? 'true' : undefined"
      >
        {{ column.label }}
      </RouterLink>

      <div v-if="overflow.length" class="column-nav__more">
        <button
          type="button"
          class="column-nav__more-trigger"
          :class="{
            'column-nav__more-trigger--open': open,
            'column-nav__more-trigger--active': activeInOverflow,
          }"
          :aria-expanded="open"
          @click.stop="open = !open"
        >
          <span aria-hidden="true">»</span>
          更多
        </button>
        <ul v-if="open" class="column-nav__menu">
          <li v-for="column in overflow" :key="column.key">
            <RouterLink
              :to="column.to"
              :class="{ 'column-nav__menu-link--active': activeKey === column.key }"
              :aria-current="activeKey === column.key ? 'true' : undefined"
              @click="open = false"
            >
              {{ column.label }}
            </RouterLink>
          </li>
        </ul>
      </div>
    </div>

    <!--
      探针：只为量宽度，不可见也不参与布局。
      必须再套一层 0×0 + overflow:hidden 的裁剪盒：探针自身是 width:max-content 的绝对定位元素，
      直接挂在组件里会撑出文档的横向滚动区（手机上实测把页面撑到 3797px）。
    -->
    <div class="column-nav__probe-clip" aria-hidden="true">
      <div ref="probe" class="column-nav__probe">
        <span v-for="column in columns" :key="column.key" data-probe-item class="column-nav__item">{{ column.label }}</span>
        <span data-probe-more class="column-nav__more-trigger"><span aria-hidden="true">»</span>更多</span>
      </div>
    </div>
  </nav>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.column-nav。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav {
  position: relative;
  margin-bottom: var(--space-6);
  padding: 6px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-surface);
}

/**
 * 样式范围：.column-nav__row。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.column-nav__row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
}

/**
 * 样式范围：.column-nav__pill。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__pill {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 0;
  border-radius: 10px;
  background: var(--primary);
  box-shadow: 0 6px 18px color-mix(in srgb, var(--primary) 24%, transparent);
  transition:
    transform 320ms var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1)),
    width 320ms var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

/**
 * 样式范围：.column-nav__item。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__item {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
  padding: 8px 14px;
  border-radius: 10px;
  color: var(--text-secondary);
  font-size: 14px;
  white-space: nowrap;
  transition: color 160ms ease, background-color 160ms ease;
}

/**
 * 样式范围：.column-nav__item:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__item:hover {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

/**
 * 样式范围：.column-nav__item--active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__item--active {
  color: var(--on-primary);
  font-weight: 600;
}

/* 选中项自己不再铺底色：底色由底片负责，否则底片滑走后会留下一块残影 */
.column-nav__item--active:hover {
  color: var(--on-primary);
  background: transparent;
}

/**
 * 样式范围：.column-nav__more。
 */
.column-nav__more {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
  margin-left: auto;
}

/**
 * 样式范围：.column-nav__more-trigger。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__more-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-secondary);
  background: transparent;
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
  transition: color 160ms ease, border-color 160ms ease, background-color 160ms ease;
}

/**
 * 样式范围：.column-nav__more-trigger:hover, .column-nav__more-trigger--open。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__more-trigger:hover,
.column-nav__more-trigger--open {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

/**
 * 样式范围：.column-nav__more-trigger--active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__more-trigger--active {
  border-color: var(--primary);
  color: var(--primary);
  font-weight: 600;
}

/**
 * 样式范围：.column-nav__menu。
 * 溢出处理：整体 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 30;
  min-width: 168px;
  max-height: 340px;
  overflow: auto;
  margin: 0;
  padding: 6px;
  list-style: none;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-surface);
  box-shadow: 0 18px 40px rgb(15 30 25 / 0.14);
}

/**
 * 样式范围：.column-nav__menu a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__menu a {
  display: block;
  padding: 8px 12px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

/**
 * 样式范围：.column-nav__menu a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__menu a:hover {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

/**
 * 样式范围：.column-nav__menu-link--active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.column-nav__menu-link--active {
  color: var(--primary) !important;
  font-weight: 600;
}

/* 量宽度用：0×0 裁剪盒，保证探针再宽也不会撑出横向滚动 */
.column-nav__probe-clip {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  overflow: hidden;
  pointer-events: none;
}

/**
 * 样式范围：.column-nav__probe。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.column-nav__probe {
  display: flex;
  gap: 8px;
  width: max-content;
  visibility: hidden;
}

/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.column-nav__pill。
   */
  .column-nav__pill { transition: none; }
}

/**
 * 响应式或设备条件：(max-width: 680px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 680px) {
  /**
   * 样式范围：.column-nav__row。
   */
  .column-nav__row { min-height: 36px; }
  /**
   * 样式范围：.column-nav__item,   .column-nav__more-trigger。
   */
  .column-nav__item,
  .column-nav__more-trigger { padding: 7px 11px; font-size: 13px; }
}
</style>
