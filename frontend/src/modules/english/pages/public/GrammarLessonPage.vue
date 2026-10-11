<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import { getGrammarCurriculum, getGrammarLesson } from '../../api/englishApi'

const route = useRoute()
const lesson = ref(null)
const curriculum = ref(null)
const neighbors = ref([])
const loading = ref(true)
const error = ref('')
const index = computed(() => neighbors.value.findIndex((item) => item.slug === lesson.value?.slug))
const previous = computed(() => neighbors.value[index.value - 1])
const next = computed(() => neighbors.value[index.value + 1])
const currentSection = computed(() => curriculum.value?.sections.find((section) => section.lessons.some((item) => item.slug === lesson.value?.slug)))
const coursePosition = computed(() => index.value < 0 ? 0 : index.value + 1)

watch(() => route.params.slug, async (slug) => {
  loading.value = true
  error.value = ''
  try {
    const [item, course] = await Promise.all([getGrammarLesson(slug), getGrammarCurriculum()])
    lesson.value = item
    curriculum.value = course
    neighbors.value = course.sections.flatMap((section) => section.lessons)
  } catch { error.value = '课时不存在或尚未公开。' }
  finally { loading.value = false }
}, { immediate: true })
</script>

<template>
  <main class="grammar-lesson">
    <p v-if="loading" class="grammar-lesson__state">正在读取课时…</p><p v-else-if="error" class="grammar-lesson__state" role="alert">{{ error }}</p>
    <div v-else-if="lesson && curriculum" class="grammar-lesson__layout">
      <aside class="grammar-lesson__course"><RouterLink to="/english/grammar" class="grammar-lesson__course-title">{{ curriculum.course.title }}</RouterLink><p>第 {{ coursePosition }} / {{ neighbors.length }} 课</p><section v-for="section in curriculum.sections" :key="section.id"><h2>{{ section.title }}</h2><RouterLink v-for="item in section.lessons" :key="item.id" :to="'/english/grammar/' + item.slug" :class="{ active: item.slug === lesson.slug }"><span>{{ item.slug }}</span>{{ item.title }}</RouterLink></section></aside>
      <article><nav class="grammar-lesson__crumb"><RouterLink to="/english">英语</RouterLink><span>/</span><RouterLink to="/english/grammar">语法教程</RouterLink><span>/</span>{{ currentSection?.title }}</nav><details class="grammar-lesson__mobile-course"><summary>课程目录</summary><section v-for="section in curriculum.sections" :key="section.id"><h2>{{ section.title }}</h2><RouterLink v-for="item in section.lessons" :key="item.id" :to="'/english/grammar/' + item.slug">{{ item.slug }} · {{ item.title }}</RouterLink></section></details><header><p class="public-eyebrow">GRAMMAR LESSON · {{ lesson.slug }}</p><h1>{{ lesson.title }}</h1><p>{{ lesson.summary }}</p><small>{{ currentSection?.title }} · 第 {{ coursePosition }} / {{ neighbors.length }} 课</small></header><BlogProse :markdown="lesson.bodyMarkdown" /><nav class="grammar-lesson__prev-next"><RouterLink v-if="previous" :to="'/english/grammar/' + previous.slug"><small>上一篇</small><strong>← {{ previous.title }}</strong></RouterLink><span v-else></span><RouterLink v-if="next" :to="'/english/grammar/' + next.slug"><small>下一篇</small><strong>{{ next.title }} →</strong></RouterLink></nav></article>
      <aside class="grammar-lesson__aside"><p>当前章节</p><strong>{{ currentSection?.title }}</strong><RouterLink to="/english/grammar">查看完整路线 →</RouterLink></aside>
    </div>
  </main>
</template>
<style scoped>
.grammar-lesson{max-width:1400px;margin:auto;padding:25px 0 80px}.grammar-lesson__state{padding:80px 0;text-align:center;color:var(--text-muted)}.grammar-lesson__layout{display:grid;grid-template-columns:250px minmax(0,1fr) 185px;gap:clamp(22px,3vw,44px);align-items:start}.grammar-lesson__course{position:sticky;top:90px;max-height:calc(100vh - 110px);overflow:auto;padding-right:15px;border-right:1px solid var(--border);scrollbar-width:thin}.grammar-lesson__course-title{display:block;margin:0 0 8px;color:var(--text-primary);font-weight:750}.grammar-lesson__course>p{margin:0 0 20px;color:var(--text-muted);font-size:12px}.grammar-lesson__course section{margin:0 0 17px}.grammar-lesson__course h2,.grammar-lesson__mobile-course h2{margin:0 0 7px;font-size:12px}.grammar-lesson__course section a{display:flex;gap:8px;padding:7px 8px;border-left:2px solid transparent;color:var(--text-secondary);font-size:12px;line-height:1.5}.grammar-lesson__course section a span{color:var(--text-muted);font-family:monospace}.grammar-lesson__course section a.active{border-left-color:var(--primary);background:var(--primary-soft);color:var(--primary);font-weight:650}.grammar-lesson__layout>article{min-width:0}.grammar-lesson__crumb{display:flex;flex-wrap:wrap;gap:8px;margin:0 0 25px;color:var(--text-muted);font-size:13px}.grammar-lesson__crumb a{color:var(--primary)}.grammar-lesson article>header{padding:0 0 25px;border-bottom:1px solid var(--border)}.grammar-lesson h1{margin:9px 0 12px;font-size:clamp(34px,4vw,48px);line-height:1.18}.grammar-lesson article>header>p:not(.public-eyebrow){margin:0;color:var(--text-secondary);line-height:1.8}.grammar-lesson article>header small{display:block;margin-top:13px;color:var(--text-muted)}.grammar-lesson :deep(.markdown-body){padding:32px 0;line-height:1.9}.grammar-lesson__prev-next{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-top:35px;padding-top:22px;border-top:1px solid var(--border)}.grammar-lesson__prev-next a{display:grid;gap:5px;color:var(--text-primary)}.grammar-lesson__prev-next a:last-child{text-align:right}.grammar-lesson__prev-next small{color:var(--text-muted)}.grammar-lesson__aside{position:sticky;top:90px;padding-left:16px;border-left:1px solid var(--border)}.grammar-lesson__aside p{margin:0 0 9px;color:var(--text-muted);font-size:12px}.grammar-lesson__aside strong{display:block;margin-bottom:15px;font-size:13px}.grammar-lesson__aside a{color:var(--primary);font-size:12px}.grammar-lesson__mobile-course{display:none}@media(max-width:1100px){.grammar-lesson__layout{grid-template-columns:220px minmax(0,1fr)}.grammar-lesson__aside{display:none}}@media(max-width:750px){.grammar-lesson__layout{display:block}.grammar-lesson__course{display:none}.grammar-lesson__mobile-course{display:block;margin-bottom:22px;padding:12px;border:1px solid var(--border);border-radius:10px;background:var(--bg-surface)}.grammar-lesson__mobile-course summary{cursor:pointer;color:var(--primary)}.grammar-lesson__mobile-course section{margin-top:15px}.grammar-lesson__mobile-course section a{display:block;padding:5px 0;color:var(--text-secondary)}}
</style>
