<script setup>
/**
 * MobilePublicNav.vue：跨模块基础模块的视图组件。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、vue-router、../viewMode、../../modules/tutorial/api/tutorialApi、../../modules/blog/api/blogApi。
 * 主要响应式状态：route、router、open、panel、categories、tags、loading、error。
 * 阅读入口：active（有效的处理函数）；unlock（unlock的处理函数）；close（关闭当前操作）；show（show的处理函数）；navigate（跳转当前操作）。
 */


import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useViewMode, accountPath } from '../viewMode'
import { listPublicCategories } from '../../modules/tutorial/api/tutorialApi'
import { listPublicTags } from '../../modules/blog/api/blogApi'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), router = useRouter()
const { contentPath } = useViewMode()
/**
 * open；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const open = ref(''), panel = ref(null), categories = ref([]), tags = ref([]), loading = ref(false), error = ref('')
let trigger = null, previousOverflow = '', locked = false, generation = 0
const primary = [
  { key: 'home', path: '/', label: '首页', icon: '⌂' },
  { key: 'tutorials', path: '/tutorials', label: '教程', icon: '册' },
  { key: 'blog', path: '/blog', label: '博客', icon: '记' },
  { key: 'english', path: '/english', label: '英语', icon: 'A' },
]
const more = [
  { path: '/portfolio', label: '作品', description: '查看工程案例与项目复盘' },
  { path: '/messages', label: '留言', description: '交流想法与学习心得' },
  { path: '/about', label: '关于', description: '认识作者与这套知识系统' },
  { path: '/search', label: '搜索', description: '查找站内内容' },
]
const english = [
  { path: '/english/vocabulary', label: '单词' }, { path: '/english/grammar', label: '语法' },
  { path: '/english/reading', label: '阅读' }, { path: '/english/writing', label: '写作' },
]
/**
 * 标题；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const title = computed(() => ({ more: '更多导航', tutorials: '教程导航', blog: '博客导航', english: '英语导航' })[open.value] || '')
/**
 * 有效的处理函数。
 * 主要协作：contentPath、route.path.startsWith。
 * @param path 路径；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function active(path) {
  const target = contentPath(path)
  return path === '/' ? route.path === target : route.path === target || route.path.startsWith(`${target}/`)
}
/**
 * more有效；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const moreActive = computed(() => more.some(item => active(item.path)))
/**
 * unlock的处理函数。
 */
function unlock() { if (locked) { document.body.style.overflow = previousOverflow; locked = false } }
/**
 * 关闭当前操作。
 * 主要协作：unlock、nextTick、trigger.focus。
 * 更新的响应式状态包括 open；渲染由 Vue 的依赖跟踪触发。
 * @param restore = true restoretrue；默认值、解构字段与可选参数见函数签名。
 */
function close(restore = true) {
  open.value = ''; generation++; unlock()
  if (restore) nextTick(() => trigger?.focus())
}
/**
 * show的处理函数。
 * 主要协作：nextTick、.focus、panel.value.querySelector、listPublicCategories、listPublicTags。
 * 更新的响应式状态包括 open、error、loading、categories、tags；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param key 键；默认值、解构字段与可选参数见函数签名。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function show(key, event) {
  trigger = event?.currentTarget || document.activeElement
  if (!locked) { previousOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden'; locked = true }
  open.value = key; error.value = ''; loading.value = key === 'tutorials' || key === 'blog'
  const request = ++generation
  await nextTick(); panel.value?.querySelector('a,button')?.focus()
  try {
    if (key === 'tutorials') { const rows = await listPublicCategories(); if (request === generation) categories.value = rows }
    if (key === 'blog') { const rows = await listPublicTags(); if (request === generation) tags.value = rows }
  } catch { if (request === generation) error.value = '导航加载失败，仍可通过上方入口进入列表。' }
  finally { if (request === generation) loading.value = false }
}
/**
 * 跳转当前操作。
 * 主要协作：active、window.scrollTo、window.matchMedia、show、close。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function navigate(item, event) {
  if (item.key === 'home' && active(item.path)) {
    window.scrollTo({ top: 0, behavior: window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches ? 'auto' : 'smooth' })
    return
  }
  if (item.key !== 'home' && active(item.path)) return show(item.key, event)
  close(false); router.push(contentPath(item.path))
}
/**
 * keydown的处理函数。
 * 主要协作：event.preventDefault、close、panel.value.querySelectorAll、last.focus、first.focus。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function keydown(event) {
  if (!open.value) return
  if (event.key === 'Escape') { event.preventDefault(); close(); return }
  if (event.key !== 'Tab') return
  const items = panel.value?.querySelectorAll('a[href],button:not([disabled])') || []
  const first = items[0], last = items[items.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}
/**
 * resize的处理函数。
 * 主要协作：close。
 */
function resize() { if (window.innerWidth > 768) close(false) }
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.fullPath, () => close(false))
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => { document.addEventListener('keydown', keydown); window.addEventListener('resize', resize) })
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => { generation++; unlock(); document.removeEventListener('keydown', keydown); window.removeEventListener('resize', resize) })
</script>

<template>
  <nav class="mobile-public-nav" aria-label="移动端主导航">
    <button v-for="item in primary" :key="item.key" type="button" :class="{ 'is-active': active(item.path) }"
      :aria-current="active(item.path) ? 'page' : undefined" :aria-expanded="item.key !== 'home' ? open === item.key : undefined"
      :aria-controls="item.key !== 'home' ? 'mobile-navigation-sheet' : undefined" @click="navigate(item, $event)">
      <span aria-hidden="true">{{ item.icon }}</span><small>{{ item.label }}</small>
    </button>
    <button type="button" :class="{ 'is-active': moreActive || open === 'more' }" :aria-expanded="open === 'more'"
      aria-controls="mobile-navigation-sheet" @click="open === 'more' ? close() : show('more', $event)">
      <span aria-hidden="true">•••</span><small>更多</small>
    </button>
  </nav>
  <Teleport to="body">
    <div v-if="open" class="mobile-navigation-overlay" @click.self="close()">
      <section id="mobile-navigation-sheet" ref="panel" class="mobile-navigation-sheet" role="dialog" aria-modal="true" :aria-label="title">
        <header><strong>{{ title }}</strong><button type="button" aria-label="关闭导航" @click="close()">×</button></header>
        <nav v-if="open === 'more'" aria-label="更多页面">
          <RouterLink v-for="item in more" :key="item.path" :to="contentPath(item.path)"><span><strong>{{ item.label }}</strong><small>{{ item.description }}</small></span><span aria-hidden="true">→</span></RouterLink>
          <RouterLink :to="accountPath('/center')"><strong>用户中心</strong><span aria-hidden="true">→</span></RouterLink>
        </nav>
        <nav v-else-if="open === 'english'" aria-label="英语模块">
          <RouterLink :to="contentPath('/english')"><strong>英语中心</strong><span aria-hidden="true">→</span></RouterLink>
          <RouterLink v-for="item in english" :key="item.path" :to="contentPath(item.path)"><strong>{{ item.label }}</strong><span aria-hidden="true">→</span></RouterLink>
        </nav>
        <nav v-else-if="open === 'tutorials'" aria-label="教程分类">
          <RouterLink :to="contentPath('/tutorials')"><strong>全部教程</strong><span aria-hidden="true">→</span></RouterLink>
          <p v-if="loading" role="status">正在加载分类…</p><p v-else-if="error" role="alert">{{ error }}</p>
          <RouterLink v-for="item in categories" v-else :key="item.id" :to="{path:contentPath('/tutorials'),query:{categorySlug:item.slug}}"><strong>{{ item.name }}</strong><span aria-hidden="true">→</span></RouterLink>
        </nav>
        <nav v-else aria-label="博客标签">
          <RouterLink :to="contentPath('/blog')"><strong>全部文章</strong><span aria-hidden="true">→</span></RouterLink>
          <p v-if="loading" role="status">正在加载标签…</p><p v-else-if="error" role="alert">{{ error }}</p>
          <RouterLink v-for="item in tags" v-else :key="item.id" :to="{path:contentPath('/blog'),query:{tag:item.slug}}"><strong>{{ item.name }}</strong><span aria-hidden="true">→</span></RouterLink>
        </nav>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.mobile-public-nav。
 */
.mobile-public-nav{display:none}
/**
 * 样式范围：.mobile-navigation-overlay。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
 * 相对视口定位，用于浮层或固定操作区域；层叠顺序由 z-index 决定。
 */
.mobile-navigation-overlay{position:fixed;z-index:110;inset:0;display:grid;align-items:end;padding:16px 16px max(16px,env(safe-area-inset-bottom));background:rgb(8 15 13 / .48);backdrop-filter:blur(5px)}
/**
 * 样式范围：.mobile-navigation-sheet。
 * 溢出处理：整体 auto。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.mobile-navigation-sheet{width:min(100%,520px);margin-inline:auto;max-height:min(78dvh,640px);overflow:auto;overscroll-behavior:contain;padding:20px;border:1px solid var(--border);border-radius:22px;color:var(--text-primary);background:var(--bg-surface);box-shadow:var(--shadow-floating)}
/**
 * 样式范围：.mobile-navigation-sheet header。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.mobile-navigation-sheet header{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:14px}
  /**
   * 样式范围：.mobile-navigation-sheet header>strong。
   */
  .mobile-navigation-sheet header>strong{font-size:20px}
  /**
   * 样式范围：.mobile-navigation-sheet header button。
   */
  .mobile-navigation-sheet header button{width:44px;min-height:44px;padding:0;font-size:26px;border-radius:10px}
/**
 * 样式范围：.mobile-navigation-sheet nav。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
 */
.mobile-navigation-sheet nav{display:grid;gap:8px}
  /**
   * 样式范围：.mobile-navigation-sheet a。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .mobile-navigation-sheet a{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:52px;padding:12px;border:1px solid var(--border);border-radius:12px;color:var(--text-primary);text-decoration:none}
  /**
   * 样式范围：.mobile-navigation-sheet a:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .mobile-navigation-sheet a:hover{border-color:var(--primary);background:var(--bg-subtle)}
  /**
   * 样式范围：.mobile-navigation-sheet small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .mobile-navigation-sheet small{display:block;margin-top:3px;color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.mobile-navigation-sheet p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .mobile-navigation-sheet p{font-size:14px;color:var(--text-secondary)}
  /**
   * 样式范围：.mobile-navigation-sheet :focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .mobile-navigation-sheet :focus-visible{outline:2px solid var(--primary);outline-offset:2px}
/**
 * 响应式或设备条件：(max-width:768px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:768px){
  /**
   * 样式范围：.mobile-public-nav。
   * 使用网格布局组织内容；列定义为 repeat(5,minmax(0,1fr))，间距由 各行列间距规则 控制。
   * 相对视口定位，用于浮层或固定操作区域；层叠顺序由 z-index 决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .mobile-public-nav{position:fixed;z-index:55;right:10px;bottom:max(10px,env(safe-area-inset-bottom));left:10px;display:grid;grid-template-columns:repeat(5,minmax(0,1fr));min-height:60px;padding:5px;border:1px solid var(--border);border-radius:18px;background:color-mix(in srgb,var(--bg-surface) 94%,transparent);box-shadow:var(--shadow-floating);backdrop-filter:blur(18px)}
  /**
   * 样式范围：.mobile-public-nav button。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 2px 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .mobile-public-nav button{display:grid;place-content:center;gap:2px;min-width:0;min-height:48px;padding:4px;border:0;border-radius:12px;color:var(--text-muted);background:transparent;line-height:1;text-align:center;cursor:pointer}
  /**
   * 样式范围：.mobile-public-nav span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .mobile-public-nav span{font:700 16px/20px var(--font-mono)}
  /**
   * 样式范围：.mobile-public-nav small。
   */
  .mobile-public-nav small{font-size:11px;line-height:16px}
  /**
   * 样式范围：.mobile-public-nav .is-active。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .mobile-public-nav .is-active{color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.mobile-public-nav button:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .mobile-public-nav button:focus-visible{outline:2px solid var(--primary);outline-offset:1px}}
</style>
