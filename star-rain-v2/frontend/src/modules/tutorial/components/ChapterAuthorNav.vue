<script setup>
import { RouterLink } from 'vue-router'
import { accountPath } from '../../../shared/viewMode'
defineProps({ tutorialId: [String, Number], chapterId: [String, Number], active: { type: String, required: true } })
const sections = [{ key: 'body', label: '章节正文', suffix: '' }, { key: 'cards', label: '知识卡片', suffix: '/cards' }, { key: 'questions', label: '章节问题', suffix: '/questions' }]
</script>
<template><nav class="chapter-author-nav" aria-label="章节内容导航"><template v-for="section in sections" :key="section.key"><RouterLink v-if="chapterId && chapterId !== 'new'" :to="accountPath(`/tutorials/${tutorialId}/chapters/${chapterId}${section.suffix}`)" :aria-current="active === section.key ? 'page' : undefined">{{ section.label }}</RouterLink><span v-else class="chapter-author-nav__pending" :aria-current="active === section.key ? 'page' : undefined">{{ section.label }}</span></template></nav></template>
<style scoped>
.chapter-author-nav{display:flex;flex-wrap:wrap;align-items:center;gap:12px;margin:0 0 28px;padding:10px;border:1px solid var(--border);border-radius:12px;background:var(--bg-surface)}.chapter-author-nav a,.chapter-author-nav__pending{padding:9px 16px;border-radius:8px;color:var(--text-secondary)}.chapter-author-nav [aria-current="page"]{color:var(--primary);font-weight:700;background:color-mix(in srgb,var(--primary) 10%,var(--bg-surface))}@media(max-width:600px){.chapter-author-nav{gap:4px;padding:8px}.chapter-author-nav a,.chapter-author-nav__pending{font-size:12px;padding:8px}}
</style>
