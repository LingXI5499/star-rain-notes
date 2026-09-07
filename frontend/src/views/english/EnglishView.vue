<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { type EnglishView } from '@/api/english'
import { fetchLearningSummary, type LearningSummary } from '@/api/englishLearning'
import { fetchPublicBundles, type LearningBundle } from '@/api/englishBundle'
import { importLocalVocabularyProgress } from '@/api/vocabulary'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import LearningBundleCards from '@/components/english/LearningBundleCards.vue'
import EnglishLearningModeHint from '@/components/english/EnglishLearningModeHint.vue'
import StarfallScene from '@/components/visual/StarfallScene.vue'
import { useAuthStore } from '@/stores/auth'
import { vocabularyStudyStorage } from '@/lib/vocabulary-study-storage'
import { englishHubCache } from '@/lib/publicContentCache'

const auth = useAuthStore()
const isAuthenticated = computed(() => auth.isAuthenticated)
const hasGuestData = ref(false)
const english = ref<EnglishView | null>(null)
const loading = ref(true)
const error = ref(false)
const progress = ref<LearningSummary | null>(null)
const bundles = ref<LearningBundle[]>([])
const levels = ['A1','A2','B1','B2','C1','C2']
const stageLabels: Record<string,string> = { FOUNDATION:'基础阶段',WORD_MEMORY:'词汇记忆',READING:'阅读训练',ANALYTICS:'高阶分析' }
const directions = [
  { icon:'词',name:'单词',en:'VOCABULARY',kind:'flash',description:'主题词库、发音与固定间隔复习，建立可长期维护的词汇网络。',to:'/english/vocabulary',cta:'进入词库' },
  { icon:'语',name:'语法',en:'GRAMMAR',kind:'document',description:'沿章节和课程目录，从词法走向复杂句法与真实表达。',to:'/english/grammar',cta:'开始课程' },
  { icon:'读',name:'阅读',en:'READING',kind:'magazine',description:'通过分级材料训练信息提取、结构理解和语言观察。',to:'/english/reading',cta:'开始阅读' },
  { icon:'听',name:'听力',en:'LISTENING',kind:'audio',description:'围绕完整音频、时间片段、练习与语音规则进行精听。',to:'/english/listening',cta:'开始训练' },
  { icon:'写',name:'写作',en:'WRITING',kind:'lab',description:'从素材、范文和任务中练习结构清晰、意思准确的表达。',to:'/english/writing',cta:'开始写作' },
]

async function importGuestProgress() {
  try {
    const [memory,reviewLog] = await Promise.all([vocabularyStudyStorage.memories(),vocabularyStudyStorage.reviews()])
    await importLocalVocabularyProgress({ memory,reviewLog })
    hasGuestData.value = false
    ElMessage.success('本机单词进度已合并到账号；本机备份仍然保留。')
  } catch { ElMessage.error('导入失败，请稍后重试。') }
}

onMounted(async () => {
  hasGuestData.value = (await vocabularyStudyStorage.memories().catch(() => [])).length > 0
  try {
    const [content,learning,paths] = await Promise.all([englishHubCache.load('hub'),fetchLearningSummary().catch(() => null),fetchPublicBundles().catch(() => [])])
    english.value = content; progress.value = learning; bundles.value = paths
  } catch { error.value = true }
  finally { loading.value = false }
})
</script>

<template>
  <section class="english-hub">
    <header class="english-hero">
      <div class="english-hero__copy"><p class="public-eyebrow">ENGLISH · LONG-TERM SKILL</p><h1>{{ english?.title ?? 'English' }}</h1><p>{{ english?.subtitle ?? 'Build English as a long-term skill.' }}</p><div class="english-hero__actions"><RouterLink to="/english/vocabulary/study">今日单词 <span>→</span></RouterLink><RouterLink to="/english/bundles">学习组合 <span>↗</span></RouterLink></div></div>
      <div class="english-hero__visual"><StarfallScene compact label="英语长期学习路径"/><div><small>CURRENT STAGE</small><strong>{{ english ? (stageLabels[english.currentStage] ?? english.currentStage) : '读取中' }}</strong></div></div>
    </header>

    <div class="cefr-path" aria-label="CEFR 长期能力路径"><div><span v-for="level in levels" :key="level">{{ level }}</span></div><p>长期能力路径 · 不标记未经可靠映射的当前等级</p></div>
    <EnglishLearningModeHint :is-authenticated="isAuthenticated" :email="auth.username" />
    <button v-if="isAuthenticated && hasGuestData" type="button" class="english-import" @click="importGuestProgress">检测到本机游客英语进度，导入到账号 →</button>

    <div v-if="loading" class="english-state">正在整理英语学习路径…</div>
    <div v-else-if="error" class="english-state">暂时无法读取英语内容，请稍后重试。</div>
    <template v-else>
      <section class="english-overview">
        <div><p class="public-eyebrow">WHY ENGLISH</p><h2 class="public-section-title">为什么学英语</h2><p>{{ english?.introduction ?? '技术世界以英语为主，长期投资英语是职业与认知的复利。' }}</p></div>
        <dl><div><dt>已完成</dt><dd>{{ progress?.completed ?? 0 }}</dd></div><div><dt>学习中</dt><dd>{{ progress?.inProgress ?? 0 }}</dd></div><div><dt>待复习</dt><dd>{{ progress?.dueForReview ?? 0 }}</dd></div><div><dt>累计记录</dt><dd>{{ progress?.total ?? 0 }}</dd></div></dl>
      </section>

      <section class="english-section"><header><div><p class="public-eyebrow">FIVE DIRECTIONS</p><h2 class="public-section-title">五条学习方向</h2></div><RouterLink to="/english/progress">查看学习洞察 →</RouterLink></header><div class="direction-grid"><RouterLink v-for="direction in directions" :key="direction.name" :to="direction.to" class="direction-card public-interactive" :data-kind="direction.kind"><div><span>{{ direction.icon }}</span><small>{{ direction.en }}</small></div><h3>{{ direction.name }}</h3><p>{{ direction.description }}</p><strong>{{ direction.cta }} →</strong></RouterLink></div></section>

      <section class="english-section"><header><div><p class="public-eyebrow">CONNECTED LEARNING</p><h2 class="public-section-title">跨模块学习组合</h2></div><RouterLink to="/english/bundles">查看全部 →</RouterLink></header><LearningBundleCards :bundles="bundles.slice(0,3)" empty-hint="学习组合正在编排中。" /></section>

      <section class="vocab-gateway"><div><p class="public-eyebrow">VOCABULARY MEMORY</p><h2>把单词放进可以持续复习的系统</h2><p>按主题浏览词汇，通过英译中、中译英与随机混合完成固定间隔复习。</p><div><RouterLink to="/english/vocabulary">浏览主题</RouterLink><RouterLink to="/english/vocabulary/study">开始今日学习 →</RouterLink></div></div><span aria-hidden="true">Aa</span></section>

      <section v-if="english?.roadmapMarkdown" class="english-section english-roadmap"><header><div><p class="public-eyebrow">ROADMAP</p><h2 class="public-section-title">长期学习路线</h2></div></header><MarkdownRenderer :source="english.roadmapMarkdown" /></section>
    </template>
  </section>
</template>

<style scoped>
.english-hub{max-width:1240px;margin:auto;padding:20px 0 90px}.english-hero{display:grid;grid-template-columns:minmax(0,.86fr) minmax(430px,1.14fr);gap:clamp(40px,6vw,82px);align-items:center;padding:38px 0 58px}.english-hero h1{margin:12px 0 6px;font-size:clamp(58px,8vw,104px);line-height:.94;letter-spacing:-.07em}.english-hero__copy>p:last-of-type{color:var(--primary);font:500 clamp(18px,2.3vw,28px)/1.5 Georgia,serif}.english-hero__actions{display:flex;gap:9px;margin-top:28px}.english-hero__actions a{display:flex;justify-content:space-between;gap:25px;padding:11px 14px;border:1px solid var(--border);border-radius:10px;color:var(--text-primary);background:var(--bg-surface);font-size:12px;font-weight:700}.english-hero__actions a:first-child{border-color:var(--primary);color:var(--on-primary);background:var(--primary)}.english-hero__visual{position:relative;padding-bottom:26px}.english-hero__visual>div{position:absolute;right:18px;bottom:0;display:grid;min-width:190px;gap:3px;padding:14px;border:1px solid var(--border);border-radius:13px;background:var(--bg-surface);box-shadow:var(--shadow-md)}.english-hero__visual small{color:var(--accent);font:700 8px var(--font-mono);letter-spacing:.13em}.english-hero__visual strong{font-size:14px}.cefr-path{padding:20px 22px;border-block:1px solid var(--border)}.cefr-path>div{position:relative;display:grid;grid-template-columns:repeat(6,1fr);gap:8px}.cefr-path>div::before{position:absolute;z-index:0;top:50%;right:4%;left:4%;height:1px;background:var(--border-strong);content:''}.cefr-path span{position:relative;z-index:1;display:grid;width:42px;height:42px;margin:auto;place-items:center;border:1px solid var(--border-strong);border-radius:50%;color:var(--primary);background:var(--bg-page);font:750 11px var(--font-mono)}.cefr-path p{margin:9px 0 0;color:var(--text-muted);font-size:10px;text-align:center}.english-import{width:100%;margin-top:10px;padding:11px;border:1px solid var(--primary);border-radius:10px;color:var(--primary);background:var(--primary-soft);cursor:pointer}.english-state{min-height:300px;display:grid;place-content:center;color:var(--text-muted)}.english-overview{display:grid;grid-template-columns:1fr 1fr;gap:50px;padding:70px 0;border-bottom:1px solid var(--border)}.english-overview h2{margin:8px 0 14px}.english-overview>div>p:last-child{max-width:580px;color:var(--text-secondary);font-size:15px;line-height:1.9}.english-overview dl{display:grid;grid-template-columns:repeat(2,1fr);border:1px solid var(--border);border-radius:18px;overflow:hidden;background:var(--bg-surface)}.english-overview dl>div{padding:18px;border-right:1px solid var(--border);border-bottom:1px solid var(--border)}.english-overview dl>div:nth-child(even){border-right:0}.english-overview dl>div:nth-child(n+3){border-bottom:0}.english-overview dt{color:var(--text-muted);font-size:10px}.english-overview dd{margin-top:5px;color:var(--primary);font-size:28px;font-weight:750}.english-section{padding:70px 0;border-bottom:1px solid var(--border)}.english-section>header{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:28px}.english-section h2{margin-top:8px}.english-section header>a{color:var(--primary);font-size:12px}.direction-grid{display:grid;grid-template-columns:repeat(6,1fr);gap:12px}.direction-card{position:relative;display:flex;min-height:270px;flex-direction:column;overflow:hidden;padding:18px;border:1px solid var(--border);border-radius:18px;color:var(--text-primary);background:var(--bg-surface)}.direction-card:first-child{grid-column:span 2}.direction-card:nth-child(2){grid-column:span 2}.direction-card:nth-child(3){grid-column:span 2}.direction-card:nth-child(4),.direction-card:nth-child(5){grid-column:span 3}.direction-card::after{position:absolute;right:-48px;bottom:-60px;width:130px;height:130px;border:1px solid color-mix(in srgb,var(--card-accent,var(--primary)) 25%,transparent);border-radius:50%;content:''}.direction-card[data-kind='magazine'],.direction-card[data-kind='lab']{--card-accent:var(--accent)}.direction-card>div{display:flex;align-items:center;justify-content:space-between}.direction-card>div>span{display:grid;width:46px;height:46px;place-items:center;border-radius:14px;color:var(--card-accent,var(--primary));background:color-mix(in srgb,var(--card-accent,var(--primary)) 12%,transparent);font-size:22px;font-weight:800}.direction-card small{color:var(--accent);font:700 8px var(--font-mono);letter-spacing:.12em}.direction-card h3{margin:30px 0 8px;font-size:22px}.direction-card p{color:var(--text-secondary);font-size:12px;line-height:1.75}.direction-card strong{margin-top:auto;color:var(--primary);font-size:12px}.direction-card:hover{transform:translateY(-3px);border-color:var(--card-accent,var(--primary));box-shadow:var(--shadow-sm)}.vocab-gateway{display:grid;grid-template-columns:1fr 230px;gap:40px;align-items:center;margin:70px 0 0;padding:38px;border:1px solid var(--border);border-radius:var(--radius-hero);background:linear-gradient(135deg,var(--bg-surface),var(--primary-soft));overflow:hidden}.vocab-gateway h2{max-width:650px;margin:9px 0 12px;font-size:clamp(28px,4vw,46px);line-height:1.15}.vocab-gateway p:not(.public-eyebrow){color:var(--text-secondary);line-height:1.8}.vocab-gateway>span{color:color-mix(in srgb,var(--primary) 72%,transparent);font:800 110px/1 Georgia,serif;text-align:center}.vocab-gateway>div>div{display:flex;gap:9px;margin-top:24px}.vocab-gateway a{padding:9px 12px;border:1px solid var(--border-strong);border-radius:9px;color:var(--text-primary);background:var(--bg-surface);font-size:12px;font-weight:700}.vocab-gateway a:last-child{border-color:var(--primary);color:var(--on-primary);background:var(--primary)}.english-roadmap :deep(.markdown-body){padding:24px;border-left:2px solid var(--primary);background:color-mix(in srgb,var(--bg-surface) 55%,transparent)}@media(max-width:920px){.english-hero{grid-template-columns:1fr}.english-hero__visual{max-width:720px}.english-overview{grid-template-columns:1fr}.direction-grid{grid-template-columns:repeat(2,1fr)}.direction-card,.direction-card:first-child,.direction-card:nth-child(2),.direction-card:nth-child(3),.direction-card:nth-child(4),.direction-card:nth-child(5){grid-column:auto}.vocab-gateway{grid-template-columns:1fr}.vocab-gateway>span{display:none}}@media(max-width:600px){.english-hub{padding-top:0}.english-hero{padding-top:34px}.english-hero h1{font-size:62px}.english-hero__visual :deep(.starfall-scene){min-height:250px}.cefr-path{margin-inline:-20px;padding-inline:12px}.cefr-path span{width:35px;height:35px}.english-overview,.english-section{padding:50px 0}.direction-grid{grid-template-columns:1fr}.direction-card{min-height:220px}.vocab-gateway{margin-top:50px;padding:25px;border-radius:20px}.english-section>header{align-items:flex-start;flex-direction:column}.english-overview dl{grid-template-columns:1fr 1fr}}
@media(max-width:600px){.english-hero h1{font-size:clamp(46px,13vw,58px)}}
</style>
