<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useViewMode, accountPath } from '../viewMode'
import { listPublicCategories } from '../../modules/tutorial/api/tutorialApi'
import { listPublicTags } from '../../modules/blog/api/blogApi'

const route = useRoute(), router = useRouter()
const { contentPath, isAccount } = useViewMode()
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
const title = computed(() => ({ more: '更多导航', tutorials: '教程导航', blog: '博客导航', english: '英语导航' })[open.value] || '')
function active(path) {
  const target = contentPath(path)
  return path === '/' ? route.path === target : route.path === target || route.path.startsWith(`${target}/`)
}
const moreActive = computed(() => more.some(item => active(item.path)))
function unlock() { if (locked) { document.body.style.overflow = previousOverflow; locked = false } }
function close(restore = true) {
  open.value = ''; generation++; unlock()
  if (restore) nextTick(() => trigger?.focus())
}
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
function navigate(item, event) {
  if (item.key === 'home' && active(item.path)) {
    window.scrollTo({ top: 0, behavior: window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches ? 'auto' : 'smooth' })
    return
  }
  if (item.key !== 'home' && active(item.path)) return show(item.key, event)
  close(false); router.push(contentPath(item.path))
}
function keydown(event) {
  if (!open.value) return
  if (event.key === 'Escape') { event.preventDefault(); close(); return }
  if (event.key !== 'Tab') return
  const items = panel.value?.querySelectorAll('a[href],button:not([disabled])') || []
  const first = items[0], last = items[items.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}
function resize() { if (window.innerWidth > 768) close(false) }
watch(() => route.fullPath, () => close(false))
onMounted(() => { document.addEventListener('keydown', keydown); window.addEventListener('resize', resize) })
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
          <RouterLink v-if="isAccount" :to="accountPath('/center')"><strong>用户中心</strong><span aria-hidden="true">→</span></RouterLink>
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
.mobile-public-nav{display:none}
.mobile-navigation-overlay{position:fixed;z-index:110;inset:0;display:grid;align-items:end;padding:16px 16px max(16px,env(safe-area-inset-bottom));background:rgb(8 15 13 / .48);backdrop-filter:blur(5px)}
.mobile-navigation-sheet{width:min(100%,520px);margin-inline:auto;max-height:min(78dvh,640px);overflow:auto;overscroll-behavior:contain;padding:20px;border:1px solid var(--border);border-radius:22px;color:var(--text-primary);background:var(--bg-surface);box-shadow:var(--shadow-floating)}
.mobile-navigation-sheet header{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:14px}.mobile-navigation-sheet header>strong{font-size:20px}.mobile-navigation-sheet header button{width:44px;min-height:44px;padding:0;font-size:26px;border-radius:10px}
.mobile-navigation-sheet nav{display:grid;gap:8px}.mobile-navigation-sheet a{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:52px;padding:12px;border:1px solid var(--border);border-radius:12px;color:var(--text-primary);text-decoration:none}.mobile-navigation-sheet a:hover{border-color:var(--primary);background:var(--bg-subtle)}.mobile-navigation-sheet small{display:block;margin-top:3px;color:var(--text-muted);font-size:12px}.mobile-navigation-sheet p{font-size:14px;color:var(--text-secondary)}.mobile-navigation-sheet :focus-visible{outline:2px solid var(--primary);outline-offset:2px}
@media(max-width:768px){.mobile-public-nav{position:fixed;z-index:55;right:10px;bottom:max(10px,env(safe-area-inset-bottom));left:10px;display:grid;grid-template-columns:repeat(5,minmax(0,1fr));min-height:60px;padding:5px;border:1px solid var(--border);border-radius:18px;background:color-mix(in srgb,var(--bg-surface) 94%,transparent);box-shadow:var(--shadow-floating);backdrop-filter:blur(18px)}.mobile-public-nav button{display:grid;place-content:center;gap:2px;min-width:0;min-height:48px;padding:4px;border:0;border-radius:12px;color:var(--text-muted);background:transparent;line-height:1;text-align:center;cursor:pointer}.mobile-public-nav span{font:700 16px/20px var(--font-mono)}.mobile-public-nav small{font-size:11px;line-height:16px}.mobile-public-nav .is-active{color:var(--primary);background:var(--primary-soft)}.mobile-public-nav button:focus-visible{outline:2px solid var(--primary);outline-offset:1px}}
</style>
