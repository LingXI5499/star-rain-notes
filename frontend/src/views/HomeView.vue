<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { fetchHome, type PublicHome } from '@/api/site'
import EditorialMotif from '@/components/visual/EditorialMotif.vue'
import ThemeHero from '@/components/visual/ThemeHero.vue'
import { useReveal } from '@/composables/useReveal'

const appStore = useAppStore()
const { name, tagline } = storeToRefs(appStore)
const home = ref<PublicHome | null>(null)
const leadProject = computed(() => home.value?.featuredProjects[0])
const otherProjects = computed(() => home.value?.featuredProjects.slice(1) ?? [])
const loading = ref(true)
const error = ref(false)
const pageRoot = ref<HTMLElement | null>(null)
const { refresh: refreshReveal } = useReveal(pageRoot)

const modules = [
  { to: '/tutorials', index: '01', label: '教程', en: 'LEARN', desc: '从知识体系进入系统课程' },
  { to: '/blog', index: '02', label: '博客', en: 'THINK', desc: '记录判断、方法与复盘' },
  { to: '/portfolio', index: '03', label: '作品', en: 'BUILD', desc: '用真实项目验证学习' },
  { to: '/english', index: '04', label: '英语', en: 'GROW', desc: '长期积累语言能力' },
]
const systems = [
  { to: '/tutorials?categorySlug=java-fullstack', index: '01', title: 'Java 全栈知识体系', desc: '从语言基础到后端工程、数据库与前端协作，构建可落地的开发能力。' },
  { to: '/tutorials?categorySlug=computer-science', index: '02', title: '计算机基础知识体系', desc: '围绕算法、操作系统、网络与组成原理，建立稳定的技术底座。' },
  { to: '/tutorials?categorySlug=agent-development', index: '03', title: '智能体开发知识体系', desc: '理解大模型、工具调用与 Agent 架构，持续跟进智能应用开发。' },
]
const typeLabels: Record<string,string> = { TUTORIAL: '教程章节', BLOG: '博客记录', PORTFOLIO: '项目作品' }
const statusLabels: Record<string,string> = { DEVELOPING: '开发中', COMPLETED: '已完成', ONLINE: '已上线' }

function linkOf(item: { type:string; tutorialSlug:string|null; chapterSlug:string|null; slug:string|null }) {
  if (item.type === 'TUTORIAL') return `/tutorials/${item.tutorialSlug ?? ''}/${item.chapterSlug ?? ''}`
  if (item.type === 'BLOG') return `/blog/${item.slug ?? ''}`
  return `/portfolio/${item.slug ?? ''}`
}
function dateOf(value:string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('zh-CN',{ month:'2-digit',day:'2-digit',year:'numeric' }).format(date)
}

onMounted(async () => {
  try { home.value = await fetchHome() }
  catch { error.value = true }
  finally { loading.value = false; void refreshReveal() }
})
</script>

<template>
  <div ref="pageRoot" class="home-page">
    <section class="home-hero" aria-labelledby="home-title">
      <ThemeHero src="/brand/themes/home-hero.webp" alt="星雨笔录世界观主视觉" />
      <div class="home-hero__copy">
        <p class="public-eyebrow home-hero__kicker">STAR RAIN NOTES</p>
        <h1 id="home-title" class="public-display">{{ name }}</h1>
        <h2>{{ tagline || '用文字记录星光，用知识照亮远方' }}</h2>
        <p class="home-hero__intro">系统整理技术，认真记录思考，在真实项目中验证学习，也为下一次出发留下清晰的路径。</p>
        <div class="home-hero__actions">
          <RouterLink class="public-button primary" to="/tutorials">开始学习 <span aria-hidden="true">↗</span></RouterLink>
          <RouterLink class="public-button" to="/portfolio">查看作品 <span aria-hidden="true">→</span></RouterLink>
        </div>
      </div>
    </section>

    <nav class="home-rail" aria-label="站点主要内容">
      <RouterLink v-for="item in modules" :key="item.to" :to="item.to" class="public-interactive">
        <span>{{ item.index }}</span><div><small>{{ item.en }}</small><strong>{{ item.label }}</strong><p>{{ item.desc }}</p></div><i aria-hidden="true">→</i>
      </RouterLink>
    </nav>

    <main class="home-content">
      <section class="home-section home-updates">
        <header><div><p class="public-eyebrow">LATEST NOTES</p><h2 class="public-section-title">最近更新</h2></div><RouterLink to="/search">浏览全部 <span aria-hidden="true">→</span></RouterLink></header>
        <div v-if="loading" class="home-state">正在整理最新内容…</div>
        <div v-else-if="error" class="home-state">暂时无法读取更新，内容仍可从顶部导航访问。</div>
        <div v-else-if="!home?.latestUpdates.length" class="home-state">第一条更新正在路上。</div>
        <ol v-else>
          <li v-for="(item,index) in home.latestUpdates" :key="`${item.type}-${item.id}`">
            <RouterLink :to="linkOf(item)" class="public-interactive">
              <span class="home-updates__index">{{ String(index + 1).padStart(2,'0') }}</span>
              <div><small>{{ typeLabels[item.type] ?? item.type }}</small><h3>{{ item.title }}</h3></div>
              <time :datetime="item.activityAt">{{ dateOf(item.activityAt) }}</time><i aria-hidden="true">↗</i>
            </RouterLink>
          </li>
        </ol>
      </section>

      <section class="home-section home-systems">
        <header><div><p class="public-eyebrow">LEARNING MAP</p><h2 class="public-section-title">精选知识体系</h2></div><RouterLink to="/tutorials">进入教程中心 <span aria-hidden="true">→</span></RouterLink></header>
        <div class="home-systems__grid">
          <RouterLink v-for="system in systems" :key="system.to" :to="system.to" class="editorial-card public-interactive">
            <div><span>{{ system.index }}</span><i aria-hidden="true">✦</i></div><h3>{{ system.title }}</h3><p>{{ system.desc }}</p><strong>探索体系 →</strong>
          </RouterLink>
        </div>
      </section>

      <section class="home-section home-projects">
        <header><div><p class="public-eyebrow">SELECTED WORK</p><h2 class="public-section-title">把学习做成作品</h2></div><RouterLink to="/portfolio">全部作品 <span aria-hidden="true">→</span></RouterLink></header>
        <div v-if="otherProjects.length" class="home-projects__grid">
          <RouterLink v-for="(project,index) in otherProjects" :key="project.id" :to="`/portfolio/${project.slug}`" class="home-project editorial-card public-interactive" :class="{ 'is-featured': index === 0 }">
            <div class="home-project__cover"><img v-if="project.coverUrl" :src="project.coverUrl" :alt="project.title" :loading="index ? 'lazy' : 'eager'"><EditorialMotif v-else kind="portfolio" :seed="project.title" :label="project.title" /></div>
            <div class="home-project__body"><small>{{ statusLabels[project.projectStatus] ?? project.projectStatus }} · PROJECT {{ String(index + 1).padStart(2,'0') }}</small><h3>{{ project.title }}</h3><p>{{ project.summary }}</p><strong>阅读项目复盘 →</strong></div>
          </RouterLink>
        </div>
        <RouterLink v-else-if="leadProject" class="home-project-index" to="/portfolio"><span>从问题出发，到实现与复盘。</span><strong>打开作品档案 ↗</strong></RouterLink>
        <div v-else class="home-state">作品资料正在整理，可先从教程与博客了解项目脉络。</div>
      </section>

      <section v-if="home?.aboutPreview" class="home-author">
        <div><p class="public-eyebrow">ABOUT THE AUTHOR</p><span aria-hidden="true">✦</span></div>
        <div><h2>{{ home.aboutPreview.displayName || '站点作者' }}</h2><p>{{ home.aboutPreview.headline || home.aboutPreview.bio }}</p><RouterLink to="/about">认识作者与这套知识系统 →</RouterLink></div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.home-page{max-width:1320px;margin:auto;padding:0 24px 96px}
.home-hero{
  position:relative;
  display:grid;
  grid-template-columns:minmax(0,.88fr) minmax(0,1.12fr);
  gap:clamp(28px,5vw,72px);
  align-items:center;
  min-height:clamp(520px,72vh,680px);
  margin:0 -12px 8px;
  padding:48px 12px 56px;
  overflow:hidden;
}
.home-hero__copy{position:relative;z-index:2;max-width:560px}
.home-hero__kicker{margin-bottom:22px}
.home-hero h1{margin:0;font-size:clamp(52px,6.8vw,94px);line-height:1.08;letter-spacing:-.07em;text-wrap:balance}
.home-hero__english{margin:10px 0 28px;color:var(--text-muted);font:600 12px/1.4 var(--font-mono);letter-spacing:.24em;text-transform:uppercase}
.home-hero h2{max-width:520px;margin:0 0 16px;color:var(--primary);font-size:clamp(22px,2.6vw,34px);line-height:1.35;letter-spacing:-.035em}
.home-hero__intro{max-width:420px;color:var(--text-secondary);font-size:16px;line-height:1.85}
.home-hero__actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:28px}
.public-button{display:inline-flex;align-items:center;justify-content:space-between;gap:30px;min-width:150px;padding:12px 15px;border:1px solid var(--border-strong);border-radius:11px;color:var(--text-primary);background:color-mix(in srgb,var(--bg-surface) 88%,transparent);backdrop-filter:blur(10px);font-size:13px;font-weight:700}
.public-button.primary{border-color:var(--primary);color:var(--on-primary);background:var(--primary)}
.public-button:hover{transform:translateY(-2px);box-shadow:var(--shadow-sm)}
.home-rail{display:grid;grid-template-columns:repeat(4,1fr);border-block:1px solid var(--border)}
.home-rail>a{display:grid;grid-template-columns:auto 1fr auto;gap:14px;align-items:center;min-height:88px;padding:18px;border-right:0;color:var(--text-primary)}
.home-rail>a+a{border-left:1px solid var(--border)}
.home-rail>a>span{color:var(--accent);font:700 10px var(--font-mono)}
.home-rail small{color:var(--text-muted);font:700 9px var(--font-mono);letter-spacing:.16em}
.home-rail strong{display:block;margin:2px 0;font-size:16px}
.home-rail p{color:var(--text-muted);font-size:12px;line-height:1.45}
.home-rail i{color:var(--primary);font-style:normal}
.home-rail a:hover{background:var(--bg-surface)}
.home-content{padding-top:8px}.home-section{padding:72px 0;border-bottom:1px solid var(--border)}.home-section>header{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:30px}.home-section>header h2{margin-top:8px}.home-section>header>a{color:var(--text-secondary);font-size:12px}.home-section>header>a:hover{color:var(--primary)}.home-state{padding:34px;border:1px dashed var(--border-strong);border-radius:16px;color:var(--text-muted);background:color-mix(in srgb,var(--bg-surface) 45%,transparent)}.home-updates ol{margin:0;padding:0;list-style:none;border-top:1px solid var(--border)}.home-updates li{border-bottom:1px solid var(--border)}.home-updates a{display:grid;grid-template-columns:50px 1fr auto 20px;gap:18px;align-items:center;padding:19px 4px;color:var(--text-primary)}.home-updates a:hover{padding-inline:10px;background:var(--bg-surface)}.home-updates__index{color:var(--text-muted);font:650 10px var(--font-mono)}.home-updates small{color:var(--accent);font-size:9px}.home-updates h3{margin-top:3px;font-size:17px}.home-updates time{color:var(--text-muted);font-size:10px}.home-updates i{color:var(--primary);font-style:normal}.home-systems__grid{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}.home-systems__grid>a{position:relative;display:flex;min-height:285px;flex-direction:column;padding:22px;overflow:hidden;border:1px solid var(--border);border-radius:var(--radius-card);color:var(--text-primary);background:linear-gradient(145deg,var(--bg-surface),color-mix(in srgb,var(--primary-soft) 42%,var(--bg-surface)))}.home-systems__grid>a::after{position:absolute;right:-65px;bottom:-65px;width:170px;height:170px;border:1px solid color-mix(in srgb,var(--primary) 23%,transparent);border-radius:50%;content:''}.home-systems__grid>a>div{display:flex;justify-content:space-between;color:var(--accent);font:700 10px var(--font-mono)}.home-systems h3{max-width:260px;margin:54px 0 13px;font-size:22px;line-height:1.34}.home-systems p{max-width:300px;color:var(--text-secondary);font-size:13px;line-height:1.72}.home-systems strong{margin-top:auto;color:var(--primary);font-size:12px}.home-systems__grid>a:hover{transform:translateY(-3px);border-color:var(--primary);box-shadow:var(--shadow-sm)}.home-projects__grid{display:grid;grid-template-columns:repeat(2,1fr);gap:16px}.home-project{display:grid;grid-template-columns:42% 1fr;min-height:300px;overflow:hidden;border:1px solid var(--border);border-radius:var(--radius-card);color:var(--text-primary);background:var(--bg-surface)}.home-project.is-featured{grid-column:1/-1;grid-template-columns:48% 1fr;min-height:380px}.home-project:hover{transform:translateY(-3px);border-color:var(--primary);box-shadow:var(--shadow-md)}.home-project__cover{min-height:230px;overflow:hidden}.home-project__cover img{width:100%;height:100%;object-fit:cover;transition:transform var(--motion-slow) var(--ease-out)}.home-project:hover img{transform:scale(1.015)}.home-project__body{display:flex;flex-direction:column;justify-content:center;padding:clamp(22px,3vw,38px)}.home-project__body small{color:var(--accent);font:700 9px var(--font-mono);letter-spacing:.08em}.home-project__body h3{margin:13px 0 10px;font-size:clamp(22px,2.7vw,36px);line-height:1.2}.home-project__body p{color:var(--text-secondary);font-size:13px;line-height:1.78}.home-project__body strong{margin-top:auto;padding-top:20px;color:var(--primary);font-size:12px}.home-author{display:grid;grid-template-columns:220px 1fr;gap:55px;padding:75px 0 0}.home-author>div:first-child{display:flex;align-items:start;justify-content:space-between;border-top:1px solid var(--border);padding-top:14px}.home-author>div:first-child>span{color:var(--accent)}.home-author h2{font-size:32px}.home-author div>p{max-width:680px;margin:8px 0;color:var(--text-secondary);line-height:1.8}.home-author a{color:var(--primary);font-size:13px;font-weight:700}.home-hero__copy>*{animation:home-enter 520ms var(--ease-out) both}
.home-hero__copy>*:nth-child(2){animation-delay:70ms}
.home-hero__copy>*:nth-child(3){animation-delay:120ms}
.home-hero__copy>*:nth-child(4){animation-delay:170ms}
.home-hero__copy>*:nth-child(5){animation-delay:220ms}
.home-hero__copy>*:nth-child(6){animation-delay:280ms}
@keyframes home-enter{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:none}}
@media(max-width:980px){
  .home-hero{grid-template-columns:1fr;min-height:420px;padding-top:36px}
  .home-rail{grid-template-columns:repeat(2,1fr)}
  .home-rail>a:nth-child(n+3){border-top:1px solid var(--border)}
  .home-systems__grid{grid-template-columns:1fr}
  .home-systems__grid>a{min-height:220px}
  .home-project,.home-project.is-featured{grid-column:auto;grid-template-columns:1fr}
  .home-project__cover{min-height:220px}
  .home-author{grid-template-columns:1fr;gap:20px}
}
@media(max-width:600px){
  .home-page{padding:0 0 64px}
  .home-hero{margin:0;padding:28px 0 40px;min-height:380px}
  .home-hero h1{font-size:48px}
  .home-hero__actions{display:grid;grid-template-columns:1fr 1fr}
  .public-button{min-width:0;gap:10px}
  .home-rail{margin-inline:-20px}
  .home-rail>a{min-height:102px;padding:14px}
  .home-rail p{display:none}
  .home-content{padding-top:12px}
  .home-section{padding:50px 0}
  .home-section>header{align-items:flex-start;flex-direction:column;margin-bottom:22px}
  .home-updates a{grid-template-columns:32px 1fr 18px}
  .home-updates time{display:none}
  .home-projects__grid{grid-template-columns:1fr}
  .home-author{padding-top:50px}
}
@media(prefers-reduced-motion:reduce){
  .home-hero__copy>*{animation:none}
  .home-project__cover img{transition:none}
}
.home-section{padding:48px 0}
.home-updates a{padding-block:24px}
.home-updates h3{font-size:clamp(17px,2vw,22px);line-height:1.5}
.home-systems__grid{gap:0}
.home-systems__grid>a{min-height:240px;border:0;border-radius:0;box-shadow:none;background:transparent;padding:20px 26px}
.home-systems__grid>a+a{border-left:1px solid var(--border)}
.home-systems__grid>a::after{display:none}
.home-systems__grid>a:hover{transform:none;box-shadow:none;background:var(--bg-surface)}
.home-systems h3{margin-top:30px}
.home-systems strong{padding-top:22px}
.home-project{border-radius:6px}
.home-project-index{display:flex;justify-content:space-between;gap:20px;align-items:center;padding:28px 0;color:var(--text-secondary)}
.home-project-index strong{color:var(--primary)}
@media(max-width:1024px){.home-hero{gap:24px}.home-page{padding-inline:8px}.home-rail>a{padding:14px 8px;gap:8px}.home-rail p{display:none}}
@media(max-width:768px){
  .home-hero{grid-template-columns:1fr;padding-top:12px;gap:24px}
  .home-rail{grid-template-columns:repeat(2,1fr)}
  .home-rail>a:nth-child(3){border-left:0}
  .home-systems__grid{grid-template-columns:1fr}
  .home-systems__grid>a{min-height:0;padding:24px 0}
  .home-systems__grid>a+a{border-left:0;border-top:1px solid var(--border)}
  .home-section{padding:36px 0}
  .home-project-index{align-items:start;flex-direction:column}
  .home-updates a{grid-template-columns:1fr auto;gap:10px}
  .home-updates__index,.home-updates i{display:none}
}
</style>
