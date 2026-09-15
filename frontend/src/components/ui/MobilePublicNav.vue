<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { fetchPublicCategoryTree, type PublicCategoryNode } from '@/api/tutorial'
import { fetchPublicTags, type PublicTagWithCount } from '@/api/blog'
import { ENGLISH_MODULE_LINKS } from '@/lib/englishNav'

type SectionKey = 'tutorials' | 'blog' | 'english'

const route = useRoute()
const router = useRouter()

const moreOpen = ref(false)
const sectionOpen = ref<SectionKey | null>(null)
const moreButton = ref<HTMLButtonElement | null>(null)
const sectionTriggers = ref<Partial<Record<SectionKey, HTMLButtonElement | null>>>({})
const sheet = ref<HTMLElement | null>(null)
const firstSheetLink = ref<HTMLElement | null>(null)

const categories = ref<PublicCategoryNode[]>([])
const tags = ref<PublicTagWithCount[]>([])
const panelError = ref('')
const panelLoading = ref(false)

let previousBodyOverflow = ''

const primary = [
  { key: 'home' as const, to: '/', label: '首页', icon: '⌂' },
  { key: 'tutorials' as const, to: '/tutorials', label: '教程', icon: '册' },
  { key: 'blog' as const, to: '/blog', label: '博客', icon: '记' },
  { key: 'english' as const, to: '/english', label: '英语', icon: 'A' },
]

const secondary = [
  { to: '/portfolio', label: '作品', description: '查看工程案例与项目复盘' },
  { to: '/about', label: '关于', description: '认识作者与这套知识系统' },
  { to: '/search', label: '搜索', description: '查找教程、文章、作品和英语内容' },
]

const englishLinks = ENGLISH_MODULE_LINKS

const anySheetOpen = computed(() => moreOpen.value || sectionOpen.value !== null)

const sectionTitle = computed(() => {
  if (sectionOpen.value === 'tutorials') return '教程导航'
  if (sectionOpen.value === 'blog') return '博客导航'
  if (sectionOpen.value === 'english') return '英语导航'
  return ''
})

function setFirstSheetLink(element: unknown, index: number) {
  if (index !== 0) return
  if (element instanceof HTMLElement) firstSheetLink.value = element
  else if (element && typeof element === 'object' && '$el' in element && (element as { $el?: unknown }).$el instanceof HTMLElement) {
    firstSheetLink.value = (element as { $el: HTMLElement }).$el
  }
}

function setSectionTrigger(key: SectionKey, el: unknown) {
  sectionTriggers.value[key] = el instanceof HTMLButtonElement ? el : null
}

const secondaryActive = computed(() => secondary.some((item) => route.path === item.to || route.path.startsWith(`${item.to}/`)))

function active(path: string) {
  return path === '/' ? route.path === '/' : route.path === path || route.path.startsWith(`${path}/`)
}

function closeAll(restore: SectionKey | 'more' | false = false) {
  const wasMore = moreOpen.value
  const wasSection = sectionOpen.value
  moreOpen.value = false
  sectionOpen.value = null
  panelError.value = ''
  if (restore === 'more' && wasMore) nextTick(() => moreButton.value?.focus())
  else if (restore && restore !== 'more' && wasSection === restore) nextTick(() => sectionTriggers.value[restore]?.focus())
}

async function focusSheet() {
  await nextTick()
  firstSheetLink.value?.focus()
}

async function showMore() {
  sectionOpen.value = null
  moreOpen.value = true
  await focusSheet()
}

async function loadTutorialsPanel() {
  panelLoading.value = true
  panelError.value = ''
  try {
    categories.value = await fetchPublicCategoryTree()
  } catch {
    categories.value = []
    panelError.value = '分类加载失败，仍可进入教程中心。'
  } finally {
    panelLoading.value = false
  }
}

async function loadBlogPanel() {
  panelLoading.value = true
  panelError.value = ''
  try {
    tags.value = await fetchPublicTags()
  } catch {
    tags.value = []
    panelError.value = '标签加载失败，仍可进入博客中心。'
  } finally {
    panelLoading.value = false
  }
}

async function openSection(key: SectionKey) {
  moreOpen.value = false
  sectionOpen.value = key
  firstSheetLink.value = null
  if (key === 'tutorials') await loadTutorialsPanel()
  else if (key === 'blog') await loadBlogPanel()
  else {
    panelError.value = ''
    panelLoading.value = false
  }
  await focusSheet()
}

async function onPrimaryClick(item: (typeof primary)[number]) {
  if (item.key === 'home') {
    if (route.path === '/') {
      window.scrollTo({ top: 0, behavior: 'smooth' })
      return
    }
    closeAll()
    await router.push('/')
    return
  }

  const inSection = active(item.to)
  if (inSection) {
    await openSection(item.key)
    return
  }
  closeAll()
  await router.push(item.to)
}

function onKeydown(event: KeyboardEvent) {
  if (!anySheetOpen.value) return
  if (event.key === 'Escape') {
    const restore = moreOpen.value ? 'more' as const : sectionOpen.value
    closeAll(restore || false)
    return
  }
  if (event.key !== 'Tab' || !sheet.value) return
  const focusable = Array.from(sheet.value.querySelectorAll<HTMLElement>('a[href],button:not([disabled])'))
  if (!focusable.length) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

watch(() => route.fullPath, () => closeAll())
watch(anySheetOpen, (value) => {
  if (value) {
    previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = previousBodyOverflow
  }
})
onMounted(() => document.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = previousBodyOverflow
})
</script>

<template>
  <nav class="mobile-public-nav" aria-label="移动端主导航" data-testid="mobile-public-nav">
    <button
      v-for="item in primary"
      :key="item.to"
      type="button"
      :ref="item.key !== 'home' ? (el) => setSectionTrigger(item.key, el) : undefined"
      :class="{ 'is-active': active(item.to) || (item.key !== 'home' && sectionOpen === item.key) }"
      :aria-expanded="item.key !== 'home' ? sectionOpen === item.key : undefined"
      :aria-controls="item.key !== 'home' ? 'mobile-section-sheet' : undefined"
      @click="onPrimaryClick(item)"
    >
      <span aria-hidden="true">{{ item.icon }}</span><small>{{ item.label }}</small>
    </button>
    <button
      ref="moreButton"
      type="button"
      :class="{ 'is-active': secondaryActive || moreOpen }"
      :aria-expanded="moreOpen"
      aria-controls="mobile-more-sheet"
      @click="showMore"
    >
      <span aria-hidden="true">•••</span><small>更多</small>
    </button>
  </nav>

  <Teleport to="body">
    <Transition name="mobile-sheet">
      <div v-if="moreOpen" class="mobile-more" role="presentation" @click.self="closeAll('more')">
        <section id="mobile-more-sheet" ref="sheet" role="dialog" aria-modal="true" aria-label="更多导航">
          <header>
            <div><small>STAR RAIN NOTES</small><strong>继续探索</strong></div>
            <button type="button" aria-label="关闭更多导航" @click="closeAll('more')">×</button>
          </header>
          <nav aria-label="更多页面">
            <RouterLink
              v-for="(item, index) in secondary"
              :key="item.to"
              :ref="(element) => setFirstSheetLink(element, index)"
              :to="item.to"
            >
              <span><strong>{{ item.label }}</strong><small>{{ item.description }}</small></span><i aria-hidden="true">→</i>
            </RouterLink>
          </nav>
        </section>
      </div>
    </Transition>

    <Transition name="mobile-sheet">
      <div v-if="sectionOpen" class="mobile-more" role="presentation" @click.self="closeAll(sectionOpen)">
        <section
          id="mobile-section-sheet"
          ref="sheet"
          role="dialog"
          aria-modal="true"
          :aria-label="sectionTitle"
        >
          <header>
            <div>
              <small>STAR RAIN NOTES</small>
              <strong>{{ sectionOpen === 'tutorials' ? '教程' : sectionOpen === 'blog' ? '博客' : '英语' }}</strong>
            </div>
            <button type="button" :aria-label="`关闭${sectionTitle}`" @click="closeAll(sectionOpen)">×</button>
          </header>

          <nav v-if="sectionOpen === 'english'" aria-label="英语模块">
            <RouterLink
              :ref="(el) => setFirstSheetLink(el, 0)"
              to="/english"
            >
              <span><strong>英语中心</strong><small>回到英语总览</small></span><i aria-hidden="true">→</i>
            </RouterLink>
            <RouterLink v-for="item in englishLinks" :key="item.to" :to="item.to">
              <span><strong>{{ item.label }}</strong></span><i aria-hidden="true">→</i>
            </RouterLink>
          </nav>

          <nav v-else-if="sectionOpen === 'tutorials'" aria-label="教程分类">
            <RouterLink :ref="(el) => setFirstSheetLink(el, 0)" to="/tutorials">
              <span><strong>教程中心</strong><small>查看全部教程</small></span><i aria-hidden="true">→</i>
            </RouterLink>
            <p v-if="panelLoading" class="mobile-more__hint">正在加载分类…</p>
            <p v-else-if="panelError" class="mobile-more__hint">{{ panelError }}</p>
            <p v-else-if="!categories.length" class="mobile-more__hint">暂无分类</p>
            <RouterLink
              v-for="node in categories"
              :key="node.id"
              :to="{ path: '/tutorials', query: { categorySlug: node.slug } }"
            >
              <span><strong>{{ node.name }}</strong></span><i aria-hidden="true">→</i>
            </RouterLink>
          </nav>

          <nav v-else-if="sectionOpen === 'blog'" aria-label="博客标签">
            <RouterLink :ref="(el) => setFirstSheetLink(el, 0)" to="/blog">
              <span><strong>博客中心</strong><small>查看全部文章</small></span><i aria-hidden="true">→</i>
            </RouterLink>
            <p v-if="panelLoading" class="mobile-more__hint">正在加载标签…</p>
            <p v-else-if="panelError" class="mobile-more__hint">{{ panelError }}</p>
            <p v-else-if="!tags.length" class="mobile-more__hint">暂无标签</p>
            <RouterLink
              v-for="tag in tags"
              :key="tag.id"
              :to="{ path: '/blog', query: { tag: tag.slug } }"
            >
              <span><strong>{{ tag.name }}</strong><small>{{ tag.postCount }} 篇</small></span><i aria-hidden="true">→</i>
            </RouterLink>
          </nav>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.mobile-public-nav { display: none; }
@media (max-width: 768px) {
  .mobile-public-nav { position: fixed; z-index: 55; right: 10px; bottom: max(10px, env(safe-area-inset-bottom)); left: 10px; display: grid; grid-template-columns: repeat(5,1fr); min-height: 58px; padding: 5px; border: 1px solid var(--border); border-radius: 18px; background: color-mix(in srgb,var(--bg-surface) 92%,transparent); box-shadow: var(--shadow-floating); backdrop-filter: blur(18px); }
  .mobile-public-nav a,.mobile-public-nav button { display: grid; place-content: center; gap: 1px; min-width: 0; border: 0; border-radius: 13px; color: var(--text-muted); background: transparent; text-align: center; cursor: pointer; }
  .mobile-public-nav span { height: 20px; font: 700 15px/20px var(--font-mono); }
  .mobile-public-nav small { font-size: 10px; line-height: 14px; }
  .mobile-public-nav .is-active { color: var(--primary); background: var(--primary-soft); }
}

.mobile-more { position: fixed; z-index: 90; inset: 0; display: grid; align-items: end; padding: 16px; background: rgb(8 15 13 / .48); backdrop-filter: blur(5px); }
.mobile-more section { width: min(100%,520px); margin-inline: auto; max-height: min(78vh,640px); overflow: auto; padding: 20px; border: 1px solid var(--border); border-radius: 24px; color: var(--text-primary); background: var(--bg-surface); box-shadow: 0 30px 90px rgb(0 0 0 / .28); }
.mobile-more header { display: flex; align-items: start; justify-content: space-between; gap: 18px; padding-bottom: 16px; border-bottom: 1px solid var(--border); }
.mobile-more header div { display: grid; gap: 3px; }
.mobile-more header small { color: var(--accent); font: 700 9px/1.4 var(--font-mono); letter-spacing: .17em; }
.mobile-more header strong { font-size: 22px; }
.mobile-more header button { width: 36px; height: 36px; border: 1px solid var(--border); border-radius: 50%; color: var(--text-primary); background: transparent; font-size: 22px; cursor: pointer; }
.mobile-more nav { display: grid; gap: 8px; padding-top: 14px; }
.mobile-more nav a { display: flex; align-items: center; justify-content: space-between; gap: 18px; min-height: 56px; padding: 12px 14px; border: 1px solid transparent; border-radius: 14px; color: var(--text-primary); background: var(--bg-subtle); }
.mobile-more nav a span { display: grid; gap: 2px; }
.mobile-more nav a small { color: var(--text-muted); font-size: 11px; }
.mobile-more nav i { color: var(--primary); font-style: normal; }
.mobile-more__hint { margin: 4px 0 0; color: var(--text-muted); font-size: 13px; }
.mobile-sheet-enter-active,.mobile-sheet-leave-active { transition: opacity 180ms ease; }
.mobile-sheet-enter-active section,.mobile-sheet-leave-active section { transition: transform 200ms cubic-bezier(.16,1,.3,1); }
.mobile-sheet-enter-from,.mobile-sheet-leave-to { opacity: 0; }
.mobile-sheet-enter-from section,.mobile-sheet-leave-to section { transform: translateY(20px); }
</style>
