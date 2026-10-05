<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import { errorMessage } from '../../../../shared/http'
import { getEnglishOverview } from '../../api/englishApi'

const overview = ref(null)
const loading = ref(true)
const error = ref('')
const directions = [
  { glyph: '词', name: '单词', en: 'VOCABULARY', to: '/english/vocabulary', description: '按主题浏览词汇，查看音标、释义和例句。' },
  { glyph: '语', name: '语法', en: 'GRAMMAR', to: '/english/grammar', description: '按课程章节阅读语法内容。' },
  { glyph: '读', name: '阅读', en: 'READING', to: '/english/reading', description: '通过分级文章练习理解和表达。' },
  { glyph: '听', name: '听力', en: 'LISTENING', to: '/english/listening', description: '使用音频和逐句文本练习精听。' },
  { glyph: '写', name: '写作', en: 'WRITING', to: '/english/writing', description: '阅读写作素材并完成写作任务。' },
]

onMounted(async () => {
  try { overview.value = await getEnglishOverview() }
  catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
})
</script>

<template>
  <main class="english-home">
    <header class="english-home__hero">
      <div class="english-home__copy">
        <p class="public-eyebrow">STAR RAIN NOTES · ENGLISH</p>
        <h1>{{ overview?.title || '英语能力成长路径' }}</h1>
        <p class="english-home__lead">{{ overview?.subtitle || '按自己的节奏，持续积累英语能力。' }}</p>
        <div class="english-home__actions"><RouterLink to="/english/vocabulary">浏览词库 →</RouterLink><RouterLink to="/english/grammar">开始语法课程 ↗</RouterLink></div>
        <p class="english-home__quote">Knowledge leaves a trail.</p>
      </div>
      <img src="/brand/themes/english-hero.webp" alt="英语学习主题视觉">
    </header>
    <p v-if="loading" class="english-home__state">正在整理英语内容…</p>
    <p v-if="error" class="english-home__state" role="alert">{{ error }}</p>
    <section v-if="overview?.introduction" class="english-home__intro"><p class="public-eyebrow">WHY ENGLISH</p><h2>为什么学英语</h2><p>{{ overview.introduction }}</p></section>
    <section class="english-home__directions"><header><p class="public-eyebrow">FIVE DIRECTIONS</p><h2>五个学习方向</h2></header><div><RouterLink v-for="item in directions" :key="item.name" :to="item.to" class="english-home__card"><span>{{ item.glyph }}</span><small>{{ item.en }}</small><h3>{{ item.name }}</h3><p>{{ item.description }}</p><strong>进入学习 →</strong></RouterLink></div></section>
    <section v-if="overview?.roadmapMarkdown" class="english-home__roadmap"><p class="public-eyebrow">ROADMAP</p><h2>长期学习路线</h2><BlogProse :markdown="overview.roadmapMarkdown" /></section>
  </main>
</template>

<style scoped>
.english-home{max-width:1240px;margin:auto;padding-bottom:90px}.english-home__hero{display:grid;grid-template-columns:1fr 1fr;gap:36px;align-items:center;min-height:480px;padding:36px 0 52px;border-bottom:1px solid var(--border)}.english-home__copy{position:relative;z-index:1}.english-home h1{margin:15px 0;font-size:clamp(42px,6vw,78px);line-height:1.05;letter-spacing:-.05em}.english-home__lead{max-width:520px;color:var(--text-secondary);font-size:17px;line-height:1.8}.english-home__hero img{width:100%;max-height:560px;object-fit:contain}.english-home__actions{display:flex;gap:12px;margin-top:26px}.english-home__actions a{padding:12px 18px;border:1px solid var(--border-strong);border-radius:10px;color:var(--text-primary);font-weight:700}.english-home__actions a:first-child{color:var(--on-primary);background:var(--primary);border-color:var(--primary)}.english-home__quote{margin-top:24px;color:var(--text-muted);font-style:italic}.english-home__state{padding:25px;color:var(--text-secondary)}.english-home__intro,.english-home__directions,.english-home__roadmap{padding:62px 0;border-bottom:1px solid var(--border)}.english-home h2{margin:8px 0 25px;font-size:clamp(28px,4vw,42px)}.english-home__intro>p:last-child{max-width:700px;color:var(--text-secondary);line-height:1.9}.english-home__directions>div{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:14px}.english-home__card{display:flex;min-height:260px;flex-direction:column;padding:20px;border:1px solid var(--border);border-radius:18px;color:var(--text-primary);background:var(--bg-surface);transition:transform .18s,border-color .18s}.english-home__card:hover{transform:translateY(-3px);border-color:var(--primary)}.english-home__card>span{display:grid;width:45px;height:45px;place-items:center;border-radius:12px;color:var(--primary);background:var(--primary-soft);font-size:22px;font-weight:750}.english-home__card small{margin-top:22px;color:var(--accent);font-size:10px;letter-spacing:.13em}.english-home__card h3{margin:6px 0;font-size:24px}.english-home__card p{color:var(--text-secondary);font-size:13px;line-height:1.7}.english-home__card strong{margin-top:auto;color:var(--primary);font-size:13px}.english-home__roadmap :deep(.markdown-body){max-width:780px}
@media(max-width:1000px){.english-home__directions>div{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:700px){.english-home__hero{grid-template-columns:1fr}.english-home__hero img{max-height:340px}.english-home__directions>div{grid-template-columns:1fr}.english-home__card{min-height:205px}}
</style>
