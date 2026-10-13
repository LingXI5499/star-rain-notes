<script setup>
/** Shared mobile reading directory: native modal handles Escape, focus trapping and inert background. */
import { onBeforeUnmount, ref, useId } from 'vue'
import ArticleOutline from './ArticleOutline.vue'
defineProps({ items: { type: Array, default: () => [] } })
const trigger = ref(null), dialog = ref(null), open = ref(false), id = useId()
function show() { dialog.value.showModal(); open.value = true }
function close() { dialog.value?.close(); open.value = false; trigger.value?.focus() }
onBeforeUnmount(() => dialog.value?.close())
</script>
<template>
  <button v-if="items.length" ref="trigger" class="reading-drawer-trigger" type="button"
    :aria-expanded="open" :aria-controls="id" @click="show">本页目录 · {{ items.length }}</button>
  <dialog :id="id" ref="dialog" class="reading-drawer" aria-label="本页目录"
    @cancel.prevent="close" @close="open = false" @click.self="close">
    <header><strong>本页目录</strong><button type="button" aria-label="关闭目录" autofocus @click="close">×</button></header>
    <ArticleOutline :items="items" navigation-style="curriculum" hide-title />
  </dialog>
</template>
<style scoped>
.reading-drawer-trigger { display: none; }
.reading-drawer { position: fixed; inset: auto 0 0; width: 100%; max-width: none; max-height: 75vh;
  margin: 0; padding: var(--space-5); border: 0; border-radius: 20px 20px 0 0;
  background: var(--bg-surface); color: var(--text-primary); }
.reading-drawer::backdrop { background: rgb(0 0 0 / .45); }
.reading-drawer header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-4); }
.reading-drawer header button { border: 0; background: transparent; color: inherit; font-size: 24px; cursor: pointer; }
@media (max-width: 720px) {
  .reading-drawer-trigger { display: inline-flex; margin-bottom: var(--space-5); padding: 9px 14px;
    border: 1px solid var(--border-strong); border-radius: 999px; color: var(--primary); background: var(--bg-surface); cursor: pointer; }
}
</style>
