<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { AxiosError } from 'axios'
import { fetchPublicGrammar, fetchPublicGrammarLesson, type GrammarCurriculum, type GrammarLessonDetail } from '@/api/grammar'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ReadingAside from '@/components/ReadingAside.vue'
import type { OutlineItem } from '@/types'
import { applyPageMeta } from '@/lib/seo'
import { createFetchCache } from '@/lib/fetch-cache'
import { estimateReadingStats } from '@/lib/readingStats'
import { fetchLearningRecord, saveLearningRecord, type LearningRecord } from '@/api/englishLearning'

/** Course tree cache — switching lessons must not refetch / remount the sidebar (导航稳定性). */
const curriculumCache = createFetchCache<GrammarCurriculum>(() => fetchPublicGrammar())

const route=useRoute();const curriculum=ref<GrammarCurriculum|null>(null);const lesson=ref<GrammarLessonDetail|null>(null);const outline=ref<OutlineItem[]>([]);const loading=ref(true);const notFound=ref(false);const courseOpen=ref(false);const outlineOpen=ref(false)
/** True while a sibling lesson is being swapped in with the layout kept mounted. */
const lessonLoading=ref(false)
let loadVersion=0
const flat=computed(()=>curriculum.value?.sections.flatMap(s=>s.lessons)??[])
const progress=computed(()=>{const i=flat.value.findIndex(x=>x.slug===lesson.value?.slug);return i<0?0:Math.round((i+1)/flat.value.length*100)})
const readingStats=computed(()=>estimateReadingStats(lesson.value?.bodyMarkdown))
const charCount=computed(()=>readingStats.value.charCount)
const readMinutes=computed(()=>readingStats.value.readMinutes)
const learningRecord=ref<LearningRecord|null>(null)
const courseNavRef=ref<HTMLElement|null>(null)
/** Keep the active lesson visible inside the scrollable course sidebar (no-op when already visible). */
async function revealActive(){await nextTick();courseNavRef.value?.querySelector('a.active')?.scrollIntoView({block:'nearest'})}
async function load(){
  const version=++loadVersion
  notFound.value=false;courseOpen.value=false;outlineOpen.value=false;lessonLoading.value=true
  try{
    const slug=String(route.params.lessonSlug)
    const [cur,les]=await Promise.all([curriculumCache.load('grammar'),fetchPublicGrammarLesson(slug)])
    if(version!==loadVersion)return
    outline.value=[];curriculum.value=cur;lesson.value=les
    applyPageMeta({title:les.title,description:les.summary||undefined})
    void revealActive()
    learningRecord.value=await fetchLearningRecord('GRAMMAR',les.id).catch(()=>null)
  }catch(e){
    if(version!==loadVersion)return
    notFound.value=e instanceof AxiosError&&e.response?.status===404;lesson.value=null;outline.value=[]
    applyPageMeta({title:notFound.value?'课节不存在':'加载失败',robots:'noindex,nofollow'})
  }finally{
    if(version===loadVersion){loading.value=false;lessonLoading.value=false}
  }
}
async function completeLesson(){if(!lesson.value)return;learningRecord.value=await saveLearningRecord('GRAMMAR',lesson.value.id,{status:'COMPLETED',timeSpentSeconds:readMinutes.value*60,mastery:learningRecord.value?.mastery??.7,weakPoints:learningRecord.value?.weakPoints??[]})}
onMounted(load);watch(()=>route.params.lessonSlug,load)
</script>

<template>
  <section v-if="loading&&!lesson" class="reader-state">课节加载中…</section><section v-else-if="!lesson||!curriculum" class="reader-state">{{ notFound?'课节不存在或尚未公开。':'加载失败，请稍后重试。' }}</section>
  <section v-else class="grammar-reader">
    <div class="mobile-tools"><button @click="courseOpen=!courseOpen">{{ courseOpen?'关闭':'课程目录' }}</button><button :disabled="!outline.length" @click="outlineOpen=!outlineOpen">页内大纲</button></div>
    <aside ref="courseNavRef" class="course-nav" :class="{open:courseOpen}"><RouterLink to="/english/grammar" class="course-nav__title">{{ curriculum.course.title }}</RouterLink><div class="progress"><i :style="{width:`${progress}%`}"/></div><small>课程进度 {{ progress }}%</small><section v-for="section in curriculum.sections" :key="section.id"><h2>{{ section.title }}</h2><RouterLink v-for="item in section.lessons" :key="item.id" :to="`/english/grammar/${item.slug}`" :class="{active:item.slug===lesson.slug}" @click="courseOpen=false"><span>{{ item.slug }}</span>{{ item.title }}</RouterLink></section></aside>
    <article :class="{'is-loading':lessonLoading}" :aria-busy="lessonLoading"><nav class="breadcrumb"><RouterLink to="/english">英语</RouterLink><span>/</span><RouterLink to="/english/grammar">语法教程</RouterLink><span>/</span><span>{{ lesson.sectionTitle }}</span></nav><header><p>GRAMMAR LESSON · {{ lesson.slug }}</p><h1>{{ lesson.title }}</h1><span>{{ lesson.summary }}</span><small>字数 {{ charCount }} · 预计阅读 {{ readMinutes }} 分钟 · 课程进度 {{ progress }}%</small></header><MarkdownRenderer :source="lesson.bodyMarkdown" @outline="outline=$event"/><section class="lesson-complete"><div><small>LEARNING RECORD</small><strong>{{ learningRecord?.status==='COMPLETED'?'课节已完成':'完成本课并更新进度' }}</strong><span>完成后会加入学习趋势和复习计划。</span></div><button type="button" :disabled="learningRecord?.status==='COMPLETED'" @click="completeLesson">{{ learningRecord?.status==='COMPLETED'?'已完成 ✓':'标记完成' }}</button></section><nav class="prev-next"><RouterLink v-if="lesson.previous" :to="`/english/grammar/${lesson.previous.slug}`"><small>上一篇</small><strong>← {{ lesson.previous.title }}</strong></RouterLink><span/><RouterLink v-if="lesson.next" :to="`/english/grammar/${lesson.next.slug}`" class="next"><small>下一篇</small><strong>{{ lesson.next.title }} →</strong></RouterLink></nav></article>
    <ReadingAside
      class="page-outline"
      :class="{ open: outlineOpen }"
      :items="outline"
      :char-count="charCount"
      :read-minutes="readMinutes"
      :extra-info="[{ label: '课程进度', value: `${progress}%` }]"
    >
      <div class="study-card">
        <p>当前章节</p>
        <strong>{{ lesson.sectionTitle }}</strong>
        <RouterLink to="/english/grammar">查看完整路线 →</RouterLink>
      </div>
    </ReadingAside>
  </section>
</template>

<style scoped>
.reader-state{padding:80px;text-align:center;color:var(--text-muted)}.grammar-reader{display:grid;grid-template-columns:minmax(230px,275px) minmax(0,780px) minmax(170px,220px);justify-content:center;gap:clamp(20px,3vw,44px);align-items:start}.course-nav{position:sticky;top:calc(var(--header-height) + 22px);max-height:calc(100vh - var(--header-height) - 44px);overflow:auto;padding:6px 16px 24px 0;border-right:1px solid var(--border);scrollbar-width:thin;scrollbar-color:var(--border-strong) transparent}.course-nav__title{display:block;color:var(--text-primary);font-weight:750;margin-bottom:14px}.progress{height:5px;border-radius:99px;background:var(--bg-subtle);overflow:hidden}.progress i{display:block;height:100%;background:var(--primary);transition:width .16s ease}.course-nav>small{display:block;color:var(--text-muted);font-size:11px;margin:6px 0 18px}.course-nav section{margin-bottom:18px}.course-nav h2{font-size:12px;color:var(--text-primary);font-weight:700;letter-spacing:.04em;margin-bottom:6px;padding-left:10px}.course-nav section>div,.course-nav section a{min-width:0}.course-nav section a{display:flex;gap:8px;margin-left:0;padding:6px 8px 6px 12px;border-left:2px solid transparent;color:var(--text-secondary);font-size:12px;line-height:1.45;transition:color 150ms ease,background-color 150ms ease,border-color 150ms ease}.course-nav section{border-left:1px solid var(--border)}.course-nav section a span{font-family:monospace;color:var(--text-muted)}.course-nav section a:hover{background:color-mix(in srgb,var(--primary) 5%,transparent);color:var(--primary)}.course-nav section a.active{border-left-color:var(--primary);color:var(--primary);background:color-mix(in srgb,var(--primary) 8%,transparent);font-weight:650}.grammar-reader>article{min-width:0}.breadcrumb{display:flex;gap:8px;font-size:13px;color:var(--text-muted);margin-bottom:24px}.breadcrumb a{color:var(--primary)}article>header{padding-bottom:25px;margin-bottom:30px;border-bottom:1px solid var(--border)}article>header p{color:var(--accent);font-size:11px;font-weight:750;letter-spacing:.13em}article>header h1{font-size:clamp(34px,4vw,48px);line-height:1.15;margin:9px 0 13px}article>header>span{display:block;color:var(--text-secondary);line-height:1.7}article>header small{display:block;margin-top:12px;color:var(--text-muted)}article :deep(.markdown-body){line-height:1.9}.page-outline{/* sticky handled by ReadingAside */}.study-card{margin-top:8px;padding:15px 0 0;border-top:1px solid var(--border)}.study-card p{color:var(--text-muted);font-size:11px}.study-card strong{display:block;font-size:13px;margin:7px 0 12px}.study-card a{font-size:12px;color:var(--primary)}.prev-next{display:grid;grid-template-columns:1fr auto 1fr;gap:15px;margin-top:48px;padding-top:22px;border-top:1px solid var(--border)}.prev-next a{display:flex;flex-direction:column;gap:5px;color:var(--text-primary)}.prev-next small{color:var(--text-muted)}.prev-next .next{text-align:right}.mobile-tools{display:none}@media(max-width:1050px){.grammar-reader{grid-template-columns:230px minmax(0,1fr)}.page-outline{display:none}}@media(max-width:720px){.grammar-reader{display:block}.mobile-tools{position:sticky;top:var(--header-height);z-index:8;display:flex;gap:8px;padding:8px 0;background:var(--bg-page)}.mobile-tools button{padding:8px 12px;border:1px solid var(--border);border-radius:9px;background:var(--bg-surface);color:var(--text-primary)}.course-nav{display:none;position:fixed;z-index:10;inset:calc(var(--header-height) + 52px) 14px 18px;max-height:none;padding:18px;border:1px solid var(--border);border-radius:15px;background:var(--bg-surface)}.course-nav.open{display:block}.page-outline.open{display:flex!important;position:fixed;z-index:10;inset:calc(var(--header-height) + 52px) 14px auto;padding:18px;border:1px solid var(--border);border-radius:15px;background:var(--bg-surface)}.study-card{display:none}}@media(prefers-reduced-motion:reduce){.progress i{transition:none}.course-nav section a{transition:none}}
.mobile-tools{z-index:20}
.grammar-reader>article{transition:opacity 160ms ease}
/* Sibling-lesson swap: dim only the article; course nav + outline stay put. */
.grammar-reader>article.is-loading{opacity:.45;pointer-events:none}
@media(prefers-reduced-motion:reduce){.grammar-reader>article{transition:none}}
.lesson-complete{display:flex;align-items:center;justify-content:space-between;gap:18px;margin-top:34px;padding:18px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.lesson-complete small,.lesson-complete span{display:block;color:var(--text-muted);font-size:10px}.lesson-complete strong{display:block;margin:5px 0}.lesson-complete button{padding:9px 15px;border:0;border-radius:10px;background:var(--primary);color:white}.lesson-complete button:disabled{background:var(--bg-subtle);color:var(--text-muted)}@media(max-width:720px){.lesson-complete{align-items:flex-start;flex-direction:column}}
</style>
