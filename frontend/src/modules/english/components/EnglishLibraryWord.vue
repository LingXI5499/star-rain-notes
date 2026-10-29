<script setup>
import { computed } from 'vue'
import { wordExamples } from '../lib/libraryNavigation'
const props = defineProps({ word: Object, display: { type: String, default: 'all' } })
const examples = computed(() => wordExamples(props.word.examples))
const statuses = { confirmed: '已映射', 'needs-review': '分类待复核', unclassified: '分类整理中' }
</script>
<template>
  <article class="library-word" :data-word-id="word.id">
    <div class="library-word__status">{{ statuses[word.migrationStatus] || '词汇' }}</div>
    <section v-if="display !== 'zh'" class="library-word__english" aria-label="英文信息" lang="en"><h3>{{ word.word }}</h3><p>{{ word.partOfSpeech }}</p><p v-if="word.phoneticUk">英 /{{ word.phoneticUk }}/</p><p v-if="word.phoneticUs">美 /{{ word.phoneticUs }}/</p></section>
    <section v-if="display !== 'en'" class="library-word__chinese" aria-label="中文信息"><p>{{ word.translation }}</p><p v-if="word.sceneMeaning && word.sceneMeaning !== word.translation" class="library-word__scene"><span>本主题用法</span>{{ word.sceneMeaning }}</p></section>
    <div v-if="examples.length && display === 'all'" class="library-word__examples"><p v-for="(example, index) in examples" :key="index">{{ example }}</p></div>
    <small v-if="word.entryId?.startsWith('manual-')">来源：人工维护</small>
    <small v-else>来源：旧主题 {{ word.themeId }} · 记录 {{ Number(word.legacyIndex) + 1 }}</small>
    <div v-if="$slots.actions" class="library-word__actions"><slot name="actions" /></div>
  </article>
</template>
