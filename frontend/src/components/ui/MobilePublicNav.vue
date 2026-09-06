<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()
const open = ref(false)
const moreButton = ref<HTMLButtonElement | null>(null)
const firstSheetLink = ref<HTMLElement | null>(null)
const sheet = ref<HTMLElement | null>(null)
let previousBodyOverflow = ''

const primary = [
  { to: '/', label: '首页', icon: '⌂' },
  { to: '/tutorials', label: '教程', icon: '册' },
  { to: '/blog', label: '博客', icon: '记' },
  { to: '/english', label: '英语', icon: 'A' },
]
const secondary = [
  { to: '/portfolio', label: '作品', description: '查看工程案例与项目复盘' },
  { to: '/about', label: '关于', description: '认识作者与这套知识系统' },
  { to: '/search', label: '搜索', description: '查找教程、文章、作品和英语内容' },
]

function setFirstSheetLink(element: unknown, index: number) {
  if (index !== 0) return
  if (element instanceof HTMLElement) firstSheetLink.value = element
  else if (element && typeof element === 'object' && '$el' in element && (element as { $el?: unknown }).$el instanceof HTMLElement) {
    firstSheetLink.value = (element as { $el: HTMLElement }).$el
  }
}

const secondaryActive = computed(() => secondary.some((item) => route.path === item.to || route.path.startsWith(`${item.to}/`)))
function active(path: string) { return path === '/' ? route.path === '/' : route.path === path || route.path.startsWith(`${path}/`) }
async function showMore() { open.value = true; await nextTick(); firstSheetLink.value?.focus() }
function close(restore = false) { open.value = false; if (restore) nextTick(() => moreButton.value?.focus()) }
function onKeydown(event: KeyboardEvent) {
  if (!open.value) return
  if (event.key === 'Escape') { close(true); return }
  if (event.key !== 'Tab' || !sheet.value) return
  const focusable = Array.from(sheet.value.querySelectorAll<HTMLElement>('a[href],button:not([disabled])'))
  if (!focusable.length) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
}

watch(() => route.fullPath, () => close())
watch(open, (value) => {
  if (value) { previousBodyOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden' }
  else document.body.style.overflow = previousBodyOverflow
})
onMounted(() => document.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => { document.removeEventListener('keydown', onKeydown); document.body.style.overflow = previousBodyOverflow })
</script>

<template>
  <nav class="mobile-public-nav" aria-label="移动端主导航">
    <RouterLink v-for="item in primary" :key="item.to" :to="item.to" :class="{ 'is-active': active(item.to) }">
      <span aria-hidden="true">{{ item.icon }}</span><small>{{ item.label }}</small>
    </RouterLink>
    <button ref="moreButton" type="button" :class="{ 'is-active': secondaryActive || open }" :aria-expanded="open" aria-controls="mobile-more-sheet" @click="showMore">
      <span aria-hidden="true">•••</span><small>更多</small>
    </button>
  </nav>

  <Teleport to="body">
    <Transition name="mobile-sheet">
      <div v-if="open" class="mobile-more" role="presentation" @click.self="close(true)">
        <section id="mobile-more-sheet" ref="sheet" role="dialog" aria-modal="true" aria-label="更多导航">
          <header><div><small>STAR RAIN NOTES</small><strong>继续探索</strong></div><button type="button" aria-label="关闭更多导航" @click="close(true)">×</button></header>
          <nav aria-label="更多页面">
            <RouterLink v-for="(item,index) in secondary" :key="item.to" :ref="(element) => setFirstSheetLink(element, index)" :to="item.to">
              <span><strong>{{ item.label }}</strong><small>{{ item.description }}</small></span><i aria-hidden="true">→</i>
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
.mobile-more section { width: min(100%,520px); margin-inline: auto; padding: 20px; border: 1px solid var(--border); border-radius: 24px; color: var(--text-primary); background: var(--bg-surface); box-shadow: 0 30px 90px rgb(0 0 0 / .28); }
.mobile-more header { display: flex; align-items: start; justify-content: space-between; gap: 18px; padding-bottom: 16px; border-bottom: 1px solid var(--border); }
.mobile-more header div { display: grid; gap: 3px; }
.mobile-more header small { color: var(--accent); font: 700 9px/1.4 var(--font-mono); letter-spacing: .17em; }
.mobile-more header strong { font-size: 22px; }
.mobile-more header button { width: 36px; height: 36px; border: 1px solid var(--border); border-radius: 50%; color: var(--text-primary); background: transparent; font-size: 22px; cursor: pointer; }
.mobile-more nav { display: grid; gap: 8px; padding-top: 14px; }
.mobile-more nav a { display: flex; align-items: center; justify-content: space-between; gap: 18px; min-height: 68px; padding: 12px 14px; border: 1px solid transparent; border-radius: 14px; color: var(--text-primary); background: var(--bg-subtle); }
.mobile-more nav a span { display: grid; gap: 2px; }
.mobile-more nav a small { color: var(--text-muted); font-size: 11px; }
.mobile-more nav i { color: var(--primary); font-style: normal; }
.mobile-sheet-enter-active,.mobile-sheet-leave-active { transition: opacity 180ms ease; }
.mobile-sheet-enter-active section,.mobile-sheet-leave-active section { transition: transform 200ms cubic-bezier(.16,1,.3,1); }
.mobile-sheet-enter-from,.mobile-sheet-leave-to { opacity: 0; }
.mobile-sheet-enter-from section,.mobile-sheet-leave-to section { transform: translateY(20px); }
</style>
