<script setup>
/** Shared mobile reading directory: native modal handles Escape, focus trapping and inert background. */
import { onBeforeUnmount, onMounted, ref, useId } from 'vue'
import { RouterLink } from 'vue-router'
import ArticleOutline from './ArticleOutline.vue'
const props = defineProps({ items: { type: Array, default: () => [] }, courses: { type: Array, default: () => [] } })
const trigger = ref(null), dialog = ref(null), open = ref(false), id = useId()
const tab = ref('page')
let previousOverflow = '', locked = false, media
function unlock() { if (locked) { document.body.style.overflow = previousOverflow; locked = false } }
function show() {
  tab.value = props.items.length ? 'page' : 'course'
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'; locked = true
  dialog.value.showModal(); open.value = true
}
function closed() { open.value = false; unlock() }
function close(restoreFocus = true) {
  dialog.value?.close(); closed()
  if (restoreFocus) trigger.value?.focus({ preventScroll: true })
}
function select(id) {
  close(false)
  const heading = document.getElementById(id)
  heading?.setAttribute('tabindex', '-1')
  heading?.focus({ preventScroll: true })
}
function resize(event) { if (!event.matches && open.value) close(false) }
onMounted(() => { media = matchMedia('(max-width: 768px)'); media.addEventListener('change', resize) })
onBeforeUnmount(() => { dialog.value?.close(); unlock(); media?.removeEventListener('change', resize) })
</script>
<template>
  <button ref="trigger" class="reading-drawer-trigger" type="button"
    :aria-expanded="open" :aria-controls="id" @click="show">本页目录 · {{ items.length }}</button>
  <dialog :id="id" ref="dialog" class="reading-drawer" aria-label="本页目录"
    @cancel.prevent="close()" @close="closed" @click.self="close()">
    <header><strong>阅读目录</strong><button type="button" aria-label="关闭目录" autofocus @click="close()">×</button></header>
    <nav v-if="courses.length" class="reading-drawer__tabs" aria-label="目录类型">
      <button v-if="items.length" type="button" :aria-pressed="tab === 'page'" @click="tab = 'page'">本页目录</button>
      <button type="button" :aria-pressed="tab === 'course'" @click="tab = 'course'">课程目录</button>
    </nav>
    <div class="reading-drawer__content">
      <ArticleOutline v-if="tab === 'page'" :items="items" navigation-style="curriculum" hide-title embedded @select="select" />
      <nav v-else aria-label="课程章节">
        <section v-for="(group, index) in courses" :key="index" class="reading-drawer__course">
          <h2>{{ group.title }}</h2>
          <RouterLink v-for="link in group.links" :key="link.to" :to="link.to" :aria-current="link.current ? 'page' : undefined" @click="close(false)">{{ link.text }}</RouterLink>
        </section>
      </nav>
    </div>
  </dialog>
</template>
<style scoped>
.reading-drawer-trigger { display: none; }
.reading-drawer { position: fixed; inset: auto 0 0; width: 100%; max-width: none; max-height: 75dvh;
  margin: 0; padding: var(--space-5); padding-bottom: max(var(--space-5), env(safe-area-inset-bottom)); border: 0; border-radius: 20px 20px 0 0;
  background: var(--bg-surface); color: var(--text-primary); }
.reading-drawer[open] { display: flex; flex-direction: column; overflow: hidden; }
.reading-drawer::backdrop { background: rgb(0 0 0 / .45); }
.reading-drawer header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-4); }
.reading-drawer header button { border: 0; background: transparent; color: inherit; font-size: 24px; cursor: pointer; }
.reading-drawer header, .reading-drawer__tabs { flex-shrink: 0; }
.reading-drawer__content { min-height: 0; overflow-y: auto; overscroll-behavior: contain; }
.reading-drawer__content :deep(.article-outline--embedded) { max-height: none; overflow: visible; }
.reading-drawer__tabs { display: flex; gap: 8px; padding-bottom: 12px; }
.reading-drawer__tabs button { min-height: 44px; padding: 8px 14px; border: 1px solid var(--border); border-radius: 999px; background: var(--bg-surface); color: var(--text-primary); cursor: pointer; }
.reading-drawer__tabs button[aria-pressed="true"] { background: var(--primary); color: var(--on-primary); }
.reading-drawer__course { margin-bottom: 20px; }
.reading-drawer__course h2 { font-size: 14px; margin-bottom: 8px; }
.reading-drawer__course a { display: block; padding: 10px 12px; color: var(--text-secondary); border-left: 2px solid transparent; }
.reading-drawer__course a[aria-current="page"] { border-left-color: var(--primary); background: var(--bg-subtle); color: var(--primary); }
@media (max-width: 768px) {
  .reading-drawer-trigger { position: fixed; z-index: 60; right: 16px; bottom: calc(84px + env(safe-area-inset-bottom));
    display: inline-flex; align-items: center; min-height: 44px; max-width: calc(100vw - 32px); padding: 9px 16px;
    border: 1px solid var(--border-strong); border-radius: 999px; color: var(--primary); background: var(--bg-surface); box-shadow: var(--shadow-floating); cursor: pointer; }
}
</style>
