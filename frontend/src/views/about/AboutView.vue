<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchPublicAbout, type PublicAbout } from '@/api/about'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import TechFlow from '@/components/about/TechFlow.vue'
import CareerSnapshot from '@/components/about/CareerSnapshot.vue'
import EvidenceGrid from '@/components/about/EvidenceGrid.vue'
import EditorialMotif from '@/components/visual/EditorialMotif.vue'
import StarfallScene from '@/components/visual/StarfallScene.vue'
import { useReveal } from '@/composables/useReveal'

const about = ref<PublicAbout | null>(null)
const loading = ref(true)
const error = ref(false)
const pageRoot = ref<HTMLElement | null>(null)
const { refresh: refreshReveal } = useReveal(pageRoot)

const initials = computed(() => about.value?.displayName?.slice(0,1) || '星')
const bioExcerpt = computed(() => {
  const bio = about.value?.bio?.replace(/\s+/g, ' ').trim() ?? ''
  return bio.length > 150 ? `${bio.slice(0, 150).trim()}…` : bio
})
const career = computed(() => ({
  headline: about.value?.headline ?? null,
  focus: (about.value?.currentFocus ?? []).slice(0,3),
  featured: about.value?.selectedProjects?.[0]?.title ?? null,
  status: '持续学习与构建中',
}))
const evidence = computed(() => [
  ...(about.value?.selectedProjects ?? []).slice(0,2).map((item) => ({ title:item.title,to:`/portfolio/${item.slug}`,note:'真实项目与工程复盘',kind:'工程实践' })),
  ...(about.value?.selectedTutorials ?? []).slice(0,2).map((item) => ({ title:item.title,to:`/tutorials/${item.slug}`,note:'结构化知识与学习路径',kind:'知识组织' })),
  ...(about.value?.selectedBlogs ?? []).slice(0,2).map((item) => ({ title:item.title,to:`/blog/${item.slug}`,note:'实践记录、判断与反思',kind:'技术写作' })),
])
const sections = computed(() => [
  { id:'work',label:'代表作品',visible:!!about.value?.selectedProjects.length },
  { id:'evidence',label:'工程证据',visible:!!evidence.value.length },
  { id:'direction',label:'技术地图',visible:!!about.value?.technicalDirectionMarkdown },
  { id:'journey',label:'学习经历',visible:!!about.value?.journeyMarkdown },
  { id:'knowledge',label:'知识内容',visible:!!(about.value?.selectedTutorials.length || about.value?.selectedBlogs.length) },
  { id:'story',label:'个人说明',visible:!!about.value?.bio },
  { id:'contact',label:'保持联系',visible:!!(about.value?.publicEmail || about.value?.githubUrl || about.value?.resumeUrl) },
].filter((item) => item.visible))

onMounted(async () => {
  try { about.value = await fetchPublicAbout() }
  catch { error.value = true }
  finally { loading.value = false; void refreshReveal() }
})
</script>

<template>
  <main ref="pageRoot" class="about-page">
    <div v-if="loading" class="about-state">正在读取作者资料…</div>
    <div v-else-if="error" class="about-state">暂时无法读取关于资料，请稍后再试。</div>
    <template v-else-if="about">
      <header class="about-hero">
        <div class="about-hero__identity">
          <div class="about-hero__portrait"><img v-if="about.avatarUrl" :src="about.avatarUrl" :alt="`${about.displayName || '作者'}头像`"><span v-else>{{ initials }}</span></div>
          <div><p class="public-eyebrow">ABOUT · STAR RAIN NOTES</p><h1>{{ about.displayName || '个人开发者' }}</h1><h2>{{ about.headline || '在学习、实践与记录之间持续前进' }}</h2></div>
          <p class="about-hero__bio">{{ bioExcerpt }}</p>
          <div class="about-hero__actions">
            <a v-if="about.resumeUrl" class="primary" :href="about.resumeUrl" target="_blank" rel="noopener noreferrer">查看简历 <span aria-hidden="true">↓</span></a>
            <a v-if="about.githubUrl" :href="about.githubUrl" target="_blank" rel="noopener noreferrer">GitHub <span aria-hidden="true">↗</span></a>
            <a v-if="about.publicEmail" :href="`mailto:${about.publicEmail}`">写邮件 <span aria-hidden="true">↗</span></a>
          </div>
        </div>
        <StarfallScene compact label="作者持续学习与构建的星轨意象" />
      </header>

      <section class="about-career" data-reveal><CareerSnapshot v-bind="career" /></section>
      <section v-if="about.currentFocus.length" class="about-techflow" data-reveal><div><p class="public-eyebrow">CURRENT FOCUS</p><h2>此刻关注</h2></div><TechFlow :items="about.currentFocus" /></section>

      <nav v-if="sections.length" class="about-nav" aria-label="关于页章节">
        <a v-for="(section,index) in sections" :key="section.id" :href="`#${section.id}`"><span>{{ String(index + 1).padStart(2,'0') }}</span>{{ section.label }}</a>
      </nav>

      <section v-if="about.selectedProjects.length" id="work" class="about-section" data-reveal>
        <header><div><p class="public-eyebrow">SELECTED WORK</p><h2 class="public-section-title">代表作品</h2></div><p>用真实项目说明我如何理解问题、组织工程并持续复盘。</p></header>
        <div class="about-work">
          <RouterLink v-for="(item,index) in about.selectedProjects" :key="item.id" :to="`/portfolio/${item.slug}`" class="about-work__card editorial-card public-interactive" :class="{ 'is-primary': index === 0 }">
            <div><EditorialMotif kind="portfolio" :seed="item.title" :label="item.title" /></div><article><small>CASE STUDY · {{ String(index + 1).padStart(2,'0') }}</small><h3>{{ item.title }}</h3><span>查看项目复盘 →</span></article>
          </RouterLink>
        </div>
      </section>

      <section v-if="evidence.length" id="evidence" class="about-section" data-reveal>
        <header><div><p class="public-eyebrow">SKILL = EVIDENCE</p><h2 class="public-section-title">工程证据</h2></div><p>不罗列“精通”，只把已经完成并能够查看的内容放在这里。</p></header>
        <EvidenceGrid :items="evidence" />
      </section>

      <div class="about-reading">
        <aside><p class="public-eyebrow">ON THIS PAGE</p><a v-for="section in sections" :key="section.id" :href="`#${section.id}`">{{ section.label }}</a></aside>
        <div>
          <section v-if="about.technicalDirectionMarkdown" id="direction" class="about-section about-prose-section" data-reveal><header><div><p class="public-eyebrow">TECHNICAL MAP</p><h2 class="public-section-title">技术方向</h2></div><p>不是技能清单，而是正在建立的能力结构。</p></header><MarkdownRenderer :source="about.technicalDirectionMarkdown" /></section>
          <section v-if="about.journeyMarkdown" id="journey" class="about-section about-prose-section" data-reveal><header><div><p class="public-eyebrow">JOURNEY</p><h2 class="public-section-title">学习与实践</h2></div><p>在理解、动手和复盘之间缓慢积累。</p></header><MarkdownRenderer :source="about.journeyMarkdown" /></section>
          <section v-if="about.selectedTutorials.length || about.selectedBlogs.length" id="knowledge" class="about-section" data-reveal><header><div><p class="public-eyebrow">SELECTED KNOWLEDGE</p><h2 class="public-section-title">知识内容</h2></div><p>系统教程与阶段性思考，共同构成可以回看的学习坐标。</p></header><div class="about-knowledge"><div v-if="about.selectedTutorials.length"><h3>系统教程</h3><RouterLink v-for="item in about.selectedTutorials" :key="item.id" :to="`/tutorials/${item.slug}`"><span>{{ item.title }}</span><i>→</i></RouterLink></div><div v-if="about.selectedBlogs.length"><h3>思考记录</h3><RouterLink v-for="item in about.selectedBlogs" :key="item.id" :to="`/blog/${item.slug}`"><span>{{ item.title }}</span><i>→</i></RouterLink></div></div></section>
        </div>
      </div>

      <section v-if="about.bio" id="story" class="about-story" data-reveal><div><p class="public-eyebrow">PERSONAL NOTE</p><h2>关于学习、实践与这套笔录</h2></div><p>{{ about.bio }}</p></section>
      <section v-if="about.publicEmail || about.githubUrl || about.resumeUrl" id="contact" class="about-contact" data-reveal><p class="public-eyebrow">LET'S KEEP IN TOUCH</p><h2>如果你也在构建自己的知识世界，欢迎交流。</h2><div><a v-if="about.publicEmail" :href="`mailto:${about.publicEmail}`"><span>邮箱</span><b>{{ about.publicEmail }}</b><i>↗</i></a><a v-if="about.githubUrl" :href="about.githubUrl" target="_blank" rel="noopener noreferrer"><span>代码主页</span><b>查看公开项目</b><i>↗</i></a><a v-if="about.resumeUrl" :href="about.resumeUrl" target="_blank" rel="noopener noreferrer"><span>个人简历</span><b>打开 PDF 文档</b><i>↓</i></a></div></section>
      <blockquote class="about-note" data-reveal><span>EDITORIAL PRINCIPLE</span><p>把复杂的知识梳理成路径，把每一次实践沉淀成可以再次抵达的坐标。</p></blockquote>
    </template>
  </main>
</template>

<style scoped>
.about-page{max-width:1320px;margin:auto;padding:24px 24px 100px}.about-state{min-height:420px;display:grid;place-content:center;color:var(--text-muted)}.about-hero{display:grid;grid-template-columns:minmax(0,1.06fr) minmax(390px,.94fr);gap:clamp(42px,6vw,86px);align-items:center;padding:48px 0 58px;border-bottom:1px solid var(--border)}.about-hero__identity{display:grid;grid-template-columns:112px 1fr;gap:22px;align-items:center}.about-hero__portrait{width:104px;height:126px;overflow:hidden;border:1px solid var(--border);border-radius:48px 48px 16px 16px;background:linear-gradient(155deg,var(--primary-soft),var(--accent-soft));box-shadow:var(--shadow-sm)}.about-hero__portrait img{width:100%;height:100%;object-fit:cover}.about-hero__portrait>span{display:grid;width:100%;height:100%;place-items:center;color:var(--primary);font:800 44px var(--font-mono)}.about-hero h1{margin:9px 0 5px;font-size:clamp(48px,6vw,76px);line-height:.98;letter-spacing:-.06em}.about-hero h2{color:var(--primary);font-size:clamp(18px,2.2vw,27px);line-height:1.4}.about-hero__bio{grid-column:1/-1;max-width:690px;color:var(--text-secondary);font-size:15px;line-height:1.9;white-space:pre-line}.about-hero__actions{grid-column:1/-1;display:flex;flex-wrap:wrap;gap:9px}.about-hero__actions a{display:flex;justify-content:space-between;gap:24px;min-width:118px;padding:10px 13px;border:1px solid var(--border-strong);border-radius:10px;color:var(--text-primary);background:var(--bg-surface);font-size:12px;font-weight:700}.about-hero__actions a.primary{border-color:var(--primary);color:var(--on-primary);background:var(--primary)}.about-career{padding:24px 0}.about-techflow{display:grid;grid-template-columns:190px 1fr;gap:30px;align-items:center;padding:24px 0 34px;border-bottom:1px solid var(--border)}.about-techflow h2{margin-top:5px;font-size:24px}.about-nav{position:sticky;z-index:12;top:56px;display:flex;gap:7px;overflow-x:auto;margin:0 -10px;padding:11px 10px;border-bottom:1px solid var(--border);background:color-mix(in srgb,var(--bg-page) 92%,transparent);backdrop-filter:blur(16px)}.about-nav a{display:flex;flex:none;gap:7px;padding:7px 10px;border:1px solid var(--border);border-radius:999px;color:var(--text-secondary);background:var(--bg-surface);font-size:11px}.about-nav span{color:var(--accent);font:650 9px var(--font-mono)}.about-section{scroll-margin-top:120px;padding:72px 0;border-bottom:1px solid var(--border)}.about-section>header{display:grid;grid-template-columns:minmax(0,1fr) minmax(250px,.62fr);gap:32px;align-items:end;margin-bottom:30px}.about-section>header h2{margin-top:8px}.about-section>header>p{color:var(--text-muted);font-size:13px;line-height:1.75}.about-work{display:grid;grid-template-columns:repeat(2,1fr);gap:14px}.about-work__card{display:grid;grid-template-columns:42% 1fr;min-height:230px;overflow:hidden;border:1px solid var(--border);border-radius:var(--radius-card);color:var(--text-primary);background:var(--bg-surface)}.about-work__card.is-primary{grid-column:1/-1;grid-template-columns:48% 1fr;min-height:330px}.about-work__card>div{min-height:200px}.about-work__card article{display:flex;flex-direction:column;justify-content:center;padding:24px}.about-work__card small{color:var(--accent);font:700 9px var(--font-mono);letter-spacing:.12em}.about-work__card h3{margin:13px 0 28px;font-size:clamp(20px,2.6vw,32px);line-height:1.25}.about-work__card article span{margin-top:auto;color:var(--primary);font-size:12px;font-weight:700}.about-work__card:hover{transform:translateY(-3px);border-color:var(--primary);box-shadow:var(--shadow-md)}.about-reading{display:grid;grid-template-columns:180px minmax(0,1fr);gap:55px}.about-reading>aside{position:sticky;top:132px;align-self:start;display:grid;gap:11px;padding-top:74px}.about-reading>aside a{color:var(--text-muted);font-size:12px}.about-reading>aside a:hover{color:var(--primary)}.about-prose-section :deep(.markdown-body){padding-left:26px;border-left:2px solid color-mix(in srgb,var(--primary) 30%,var(--border));font-size:15px;line-height:1.95}.about-prose-section :deep(.markdown-body h2:first-child){margin-top:0}.about-knowledge{display:grid;grid-template-columns:repeat(2,1fr);gap:36px}.about-knowledge h3{padding-bottom:11px;border-bottom:1px solid var(--border);font-size:14px}.about-knowledge a{display:flex;justify-content:space-between;gap:16px;padding:13px 2px;border-bottom:1px solid var(--border);color:var(--text-primary);font-size:13px}.about-knowledge i{color:var(--primary);font-style:normal}.about-contact{scroll-margin-top:120px;padding:80px 0 0}.about-contact h2{max-width:760px;margin:12px 0 32px;font-size:clamp(30px,4vw,50px);line-height:1.22;letter-spacing:-.045em}.about-contact>div{display:grid;grid-template-columns:repeat(3,1fr);border-block:1px solid var(--border)}.about-contact a{display:grid;grid-template-columns:1fr auto;gap:7px;padding:20px;border-right:1px solid var(--border);color:var(--text-primary)}.about-contact a:last-child{border-right:0}.about-contact a>span{grid-column:1/-1;color:var(--text-muted);font-size:10px}.about-contact b{overflow-wrap:anywhere;font-size:12px}.about-contact i{color:var(--primary);font-style:normal}.about-note{max-width:780px;margin:80px auto 0;padding:28px;border:1px solid var(--border);border-left:3px solid var(--accent);border-radius:0 18px 18px 0;background:var(--bg-surface)}.about-note span{color:var(--accent);font:700 9px var(--font-mono);letter-spacing:.15em}.about-note p{margin-top:18px;font-family:Georgia,'Songti SC',serif;font-size:21px;line-height:1.75}@media(max-width:980px){.about-hero{grid-template-columns:1fr}.about-hero :deep(.starfall-scene){min-height:290px}.about-techflow{grid-template-columns:1fr}.about-reading{grid-template-columns:1fr}.about-reading>aside{display:none}}@media(max-width:650px){.about-page{padding:0 0 70px}.about-hero{padding:32px 0 46px}.about-hero__identity{grid-template-columns:82px 1fr;gap:15px}.about-hero__portrait{width:78px;height:94px;border-radius:34px 34px 12px 12px}.about-hero h1{font-size:46px}.about-hero__bio,.about-hero__actions{grid-column:1/-1}.about-career :deep(.career-snapshot){grid-template-columns:1fr 1fr;gap:8px}.about-career :deep(.career-snapshot__cell){padding:14px}.about-nav{top:55px;margin-inline:-20px;padding-inline:20px}.about-section{padding:52px 0}.about-section>header{grid-template-columns:1fr;gap:10px}.about-work{grid-template-columns:1fr}.about-work__card,.about-work__card.is-primary{grid-column:auto;grid-template-columns:1fr;min-height:0}.about-work__card>div{height:180px}.about-knowledge,.about-contact>div{grid-template-columns:1fr}.about-contact a{border-right:0;border-bottom:1px solid var(--border)}.about-contact a:last-child{border-bottom:0}.about-prose-section :deep(.markdown-body){padding-left:15px}.about-note{margin-top:54px}.about-note p{font-size:18px}}@media(prefers-reduced-motion:reduce){.about-work__card{transition:none}}
.about-story{scroll-margin-top:120px;display:grid;grid-template-columns:minmax(220px,.55fr) minmax(0,1.45fr);gap:clamp(30px,6vw,84px);padding:74px 0;border-bottom:1px solid var(--border)}
.about-story h2{margin-top:10px;font-size:clamp(25px,3vw,38px);line-height:1.25}
.about-story>p{color:var(--text-secondary);font-size:15px;line-height:1.95;white-space:pre-line}
@media(max-width:650px){.about-story{grid-template-columns:1fr;gap:16px;padding:52px 0}}
</style>
