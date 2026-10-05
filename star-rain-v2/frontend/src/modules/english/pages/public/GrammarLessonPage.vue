<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import { getGrammarCurriculum, getGrammarLesson } from '../../api/englishApi'

const route = useRoute()
const lesson = ref(null)
const neighbors = ref([])
const loading = ref(true)
const error = ref('')
const index = computed(() => neighbors.value.findIndex((item) => item.slug === lesson.value?.slug))
const previous = computed(() => neighbors.value[index.value - 1])
const next = computed(() => neighbors.value[index.value + 1])

watch(() => route.params.slug, async (slug) => {
  loading.value = true
  error.value = ''
  try {
    const [item, curriculum] = await Promise.all([getGrammarLesson(slug), getGrammarCurriculum()])
    lesson.value = item
    neighbors.value = curriculum.sections.flatMap((section) => section.lessons)
  } catch { error.value = '课时不存在或尚未公开。' }
  finally { loading.value = false }
}, { immediate: true })
</script>

<template>
  <main class="grammar-lesson"><RouterLink to="/english/grammar">← 返回语法目录</RouterLink><p v-if="loading">正在读取课时…</p><p v-else-if="error" role="alert">{{ error }}</p><article v-else-if="lesson"><header><p class="public-eyebrow">GRAMMAR LESSON · {{ lesson.slug }}</p><h1>{{ lesson.title }}</h1><p>{{ lesson.summary }}</p></header><BlogProse :markdown="lesson.bodyMarkdown" /><nav><RouterLink v-if="previous" :to="`/english/grammar/${previous.slug}`">← {{ previous.title }}</RouterLink><span v-else></span><RouterLink v-if="next" :to="`/english/grammar/${next.slug}`">{{ next.title }} →</RouterLink></nav></article></main>
</template>

<style scoped>
.grammar-lesson{max-width:950px;margin:auto;padding-bottom:70px}.grammar-lesson>a{color:var(--primary);font-size:13px}.grammar-lesson article>header{padding:40px 0;border-bottom:1px solid var(--border)}.grammar-lesson h1{margin:8px 0;font-size:clamp(36px,5vw,55px)}.grammar-lesson article>header>p:last-child{color:var(--text-secondary);line-height:1.8}.grammar-lesson :deep(.markdown-body){padding:32px 0}.grammar-lesson nav{display:flex;justify-content:space-between;gap:20px;padding-top:28px;border-top:1px solid var(--border)}.grammar-lesson nav a{color:var(--primary)}
</style>
