<script setup>
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
const props = defineProps({
  // [{ key, label, to }]，key 用于判断选中态，to 交给 RouterLink
  columns: { type: Array, default: () => [] },
  activeKey: { type: String, default: '' },
  ariaLabel: { type: String, default: '博客专栏' },
})

const row = ref(null)
const probe = ref(null)
const fitCount = ref(0)
const open = ref(false)
const pill = ref({ left: 0, top: 0, width: 0, height: 0, ready: false })

const visible = computed(() => props.columns.slice(0, fitCount.value))
const overflow = computed(() => props.columns.slice(fitCount.value))
// 选中项被收进「更多」时，行内没有底片，改为照亮「更多」按钮
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

async function measure() {
  await nextTick()
  const rowEl = row.value
  const probeEl = probe.value
  if (!rowEl || !probeEl || !props.columns.length) return

  const available = rowEl.clientWidth
  const itemEls = [...probeEl.querySelectorAll('[data-probe-item]')]
  const moreEl = probeEl.querySelector('[data-probe-more]')
  const widths = itemEls.map((el) => el.getBoundingClientRect().width)
  const gapWidth = (count) => Math.max(0, count - 1) * GAP
  const totalWidth = widths.reduce((sum, width) => sum + width, 0) + gapWidth(widths.length)

  // 全部放得下就不显示「更多」
  if (totalWidth <= available) {
    fitCount.value = props.columns.length
    open.value = false
    await nextTick()
    placePill()
    return
  }

  const moreWidth = (moreEl ? moreEl.getBoundingClientRect().width : 0) + GAP
  let used = 0
  let count = 0
  for (let index = 0; index < widths.length; index += 1) {
    const next = used + widths[index] + (count > 0 ? GAP : 0)
    // 留出「更多」按钮的位置，否则最后一个条目会顶着它
    if (next + moreWidth > available) break
    used = next
    count += 1
  }
  fitCount.value = Math.max(1, count)
  if (overflow.value.length === 0) open.value = false
  await nextTick()
  placePill()
}

let observer = null
function closeOnOutside(event) {
  if (!open.value) return
  const root = row.value?.parentElement
  if (root && !root.contains(event.target)) open.value = false
}
function closeOnEscape(event) {
  if (event.key === 'Escape') open.value = false
}

onMounted(async () => {
  await measure()
  if (typeof ResizeObserver !== 'undefined' && row.value) {
    observer = new ResizeObserver(() => { void measure() })
    observer.observe(row.value)
  }
  document.addEventListener('click', closeOnOutside)
  document.addEventListener('keydown', closeOnEscape)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  document.removeEventListener('click', closeOnOutside)
  document.removeEventListener('keydown', closeOnEscape)
})

watch(() => props.columns.map((item) => `${item.key}:${item.label}`).join('|'), () => { void measure() })
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
.column-nav {
  position: relative;
  margin-bottom: var(--space-6);
  padding: 6px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-surface);
}

.column-nav__row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
}

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

.column-nav__item:hover {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.column-nav__item--active {
  color: var(--on-primary);
  font-weight: 600;
}

/* 选中项自己不再铺底色：底色由底片负责，否则底片滑走后会留下一块残影 */
.column-nav__item--active:hover {
  color: var(--on-primary);
  background: transparent;
}

.column-nav__more {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
  margin-left: auto;
}

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

.column-nav__more-trigger:hover,
.column-nav__more-trigger--open {
  border-color: var(--primary);
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

.column-nav__more-trigger--active {
  border-color: var(--primary);
  color: var(--primary);
  font-weight: 600;
}

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

.column-nav__menu a {
  display: block;
  padding: 8px 12px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.column-nav__menu a:hover {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}

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

.column-nav__probe {
  display: flex;
  gap: 8px;
  width: max-content;
  visibility: hidden;
}

@media (prefers-reduced-motion: reduce) {
  .column-nav__pill { transition: none; }
}

@media (max-width: 680px) {
  .column-nav__row { min-height: 36px; }
  .column-nav__item,
  .column-nav__more-trigger { padding: 7px 11px; font-size: 13px; }
}
</style>
