<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getGrammarCurriculum } from '../../api/englishApi'

const curriculum = ref(null)
const loading = ref(true)
const error = ref('')
const lessonCount = computed(() => curriculum.value?.sections.reduce((sum, section) => sum + section.lessons.length, 0) || 0)
const steps = computed(() => curriculum.value?.course.roadmapMarkdown?.split('\n').map((line) => line.replace(/^\d+\.\s*/, '').trim()).filter(Boolean) || [])

onMounted(async () => {
  try { curriculum.value = await getGrammarCurriculum() }
  catch { error.value = '语法课程暂未开放。' }
  finally { loading.value = false }
})
</script>

<template>
  <main class="grammar-page">
    <p v-if="loading" class="grammar-page__state">课程加载中…</p><p v-else-if="error" class="grammar-page__state" role="alert">{{ error }}</p>
    <template v-else-if="curriculum"><header class="grammar-page__hero"><div><p class="public-eyebrow">ENGLISH GRAMMAR · SYSTEMATIC COURSE</p><h1>{{ curriculum.course.title }}</h1><h2>{{ curriculum.course.subtitle }}</h2><p>{{ curriculum.course.summary }}</p><RouterLink v-if="curriculum.sections[0]?.lessons[0]" :to="`/english/grammar/${curriculum.sections[0].lessons[0].slug}`">从第一课开始 →</RouterLink></div><div class="grammar-page__count"><strong>{{ lessonCount }}</strong><span>课节</span><small>{{ curriculum.sections.length }} CHAPTERS</small></div></header>
      <section v-if="curriculum.course.introduction" class="grammar-page__intro"><div><p class="public-eyebrow">COURSE PURPOSE</p><h2>语法学习目标</h2></div><p>{{ curriculum.course.introduction }}</p></section>
      <section v-if="steps.length" class="grammar-page__roadmap"><p class="public-eyebrow">LEARNING PATH</p><h2>学习路线</h2><ol><li v-for="(step,index) in steps" :key="step"><b>{{ String(index + 1).padStart(2, '0') }}</b>{{ step }}</li></ol></section>
      <section class="grammar-page__curriculum"><header><div><p class="public-eyebrow">CURRICULUM</p><h2>完整课程目录</h2></div><span>{{ curriculum.sections.length }} 章 · {{ lessonCount }} 课</span></header><div class="grammar-page__sections"><article v-for="(section,index) in curriculum.sections" :key="section.id"><h3><span>{{ String(index + 1).padStart(2, '0') }}</span>{{ section.title }}</h3><p>{{ section.lessons.length }} 个课节</p><ol><li v-for="lesson in section.lessons" :key="lesson.id"><RouterLink :to="`/english/grammar/${lesson.slug}`"><span>{{ lesson.slug }}</span><strong>{{ lesson.title }}</strong><span>→</span></RouterLink></li></ol></article></div></section>
    </template>
  </main>
</template>

<style scoped>
.grammar-page{max-width:1240px;margin:auto;padding-bottom:80px}.grammar-page__state{padding:90px 0;text-align:center;color:var(--text-muted)}.grammar-page__hero{display:grid;grid-template-columns:1fr 180px;gap:40px;align-items:center;padding:50px;border:1px solid var(--border);border-radius:24px;background:linear-gradient(140deg,var(--bg-surface),var(--primary-soft))}.grammar-page__hero h1{margin:10px 0;font-size:clamp(40px,5.8vw,68px);line-height:1.08}.grammar-page__hero h2{margin:0 0 15px;color:var(--text-secondary);font-size:20px;font-weight:500}.grammar-page__hero div>p:last-of-type{max-width:650px;color:var(--text-secondary);line-height:1.8}.grammar-page__hero a{display:inline-block;margin-top:23px;padding:12px 18px;border-radius:10px;color:var(--on-primary);background:var(--primary);font-weight:650}.grammar-page__count{display:grid;width:170px;height:170px;place-content:center;border:1px solid var(--border-strong);border-radius:50%;text-align:center}.grammar-page__count strong{color:var(--primary);font-size:54px}.grammar-page__count small{margin-top:6px;color:var(--text-muted);font-size:10px;letter-spacing:.12em}.grammar-page__intro{display:grid;grid-template-columns:1fr 1.4fr;gap:40px;margin:65px 0}.grammar-page__intro h2,.grammar-page__roadmap h2,.grammar-page__curriculum h2{margin:8px 0 22px;font-size:32px}.grammar-page__intro>p{color:var(--text-secondary);font-size:16px;line-height:1.9}.grammar-page__roadmap{margin-bottom:65px}.grammar-page__roadmap ol{display:grid;grid-template-columns:repeat(3,1fr);padding:0;border:1px solid var(--border);border-radius:16px;overflow:hidden;list-style:none}.grammar-page__roadmap li{display:flex;gap:13px;padding:20px;border-right:1px solid var(--border);border-bottom:1px solid var(--border)}.grammar-page__roadmap b{color:var(--accent)}.grammar-page__curriculum>header{display:flex;align-items:end;justify-content:space-between}.grammar-page__curriculum>header>span{color:var(--text-muted)}.grammar-page__sections{display:grid;grid-template-columns:repeat(2,1fr);gap:16px}.grammar-page__sections article{padding:22px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}.grammar-page__sections h3{display:flex;gap:13px;margin:0;font-size:18px}.grammar-page__sections h3 span{color:var(--accent);font-family:monospace}.grammar-page__sections article>p{color:var(--text-muted);font-size:12px}.grammar-page__sections ol{margin:16px 0 0;padding:14px 0 0;border-top:1px solid var(--border);list-style:none}.grammar-page__sections li a{display:grid;grid-template-columns:45px 1fr auto;gap:10px;padding:10px;border-radius:8px;color:var(--text-secondary)}.grammar-page__sections li a:hover{color:var(--primary);background:var(--primary-soft)}.grammar-page__sections li strong{font-weight:550}
@media(max-width:800px){.grammar-page__hero{grid-template-columns:1fr;padding:30px}.grammar-page__count{display:none}.grammar-page__intro{grid-template-columns:1fr;gap:0}.grammar-page__roadmap ol,.grammar-page__sections{grid-template-columns:1fr}}
</style>
