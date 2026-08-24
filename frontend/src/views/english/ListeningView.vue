<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPublicListeningHome, fetchPublicListenings, type ListeningHome, type ListeningPage } from '@/api/listening'
import { fetchPublicMeta, type TaxonomyTerm } from '@/api/englishMeta'
import CefrBadge from '@/components/english/CefrBadge.vue'
import EnglishModuleHero from '@/components/english/EnglishModuleHero.vue'

const route = useRoute(); const router = useRouter()
const page = ref<ListeningPage | null>(null); const home = ref<ListeningHome | null>(null)
const scenes = ref<TaxonomyTerm[]>([]); const formats = ref<TaxonomyTerm[]>([]); const topics = ref<TaxonomyTerm[]>([])
const loading = ref(true); const error = ref(false)
const filters = reactive({ q:'', level:'', cefr:'', topic:'', scene:'', format:'' })
const levelLabels: Record<number,string> = {1:'语音识别',2:'信息捕获',3:'逻辑理解'}
const fmt = (s:number)=>`${Math.floor(s/60)}:${String(s%60).padStart(2,'0')}`
function sync(){ filters.q=String(route.query.q??'');filters.level=String(route.query.level??'');filters.cefr=String(route.query.cefr??'');filters.topic=String(route.query.topic??'');filters.scene=String(route.query.scene??'');filters.format=String(route.query.format??'') }
async function load(){ loading.value=true;error.value=false; try{ page.value=await fetchPublicListenings({page:Number(route.query.page??1),pageSize:20,q:filters.q||undefined,level:filters.level?Number(filters.level):undefined,cefr:filters.cefr||undefined,topic:filters.topic?Number(filters.topic):undefined,scene:filters.scene?Number(filters.scene):undefined,format:filters.format?Number(filters.format):undefined}) }catch{error.value=true}finally{loading.value=false} }
function apply(){ router.push({ query:{ q:filters.q||undefined,level:filters.level||undefined,cefr:filters.cefr||undefined,topic:filters.topic||undefined,scene:filters.scene||undefined,format:filters.format||undefined} }) }
function goPage(p:number){ router.push({ query:{...route.query,page:String(p)} }) }
watch(()=>route.query,()=>{sync();void load()})
onMounted(async()=>{ sync(); try{ const meta=await fetchPublicMeta(); topics.value=meta.taxonomy.filter(t=>t.dimension==='TOPIC'&&t.parentId===null); scenes.value=meta.taxonomy.filter(t=>t.dimension==='SCENE'&&t.parentId===null); formats.value=meta.taxonomy.filter(t=>t.dimension==='FORMAT'&&t.parentId===null); home.value=await fetchPublicListeningHome() }catch{}; await load() })
</script>

<template>
  <section class="l-center">
    <EnglishModuleHero tag="ENGLISH LISTENING · 场景×形式×能力" title="听力中心" subtitle="三段能力路线，逐句时间片段与安全练习。" :description="home ? `${home.total} 篇已发布材料` : '加载中…'">
      <div v-if="home" class="l-center__stat"><b>{{ home.total }}</b><span>篇精听</span></div>
    </EnglishModuleHero>

    <div v-if="home" class="l-center__route">
      <RouterLink v-for="(count,lv) in home.byLevel" :key="lv" :to="{query:{level:String(lv)}}" class="route-pill"><b>{{count}}</b><span>{{levelLabels[Number(lv)]}}</span></RouterLink>
    </div>

    <div class="l-center__filters">
      <el-input v-model="filters.q" placeholder="搜索标题/摘要" clearable style="width:190px" @keyup.enter="apply" @clear="apply"/>
      <el-select v-model="filters.scene" placeholder="场景" clearable style="width:120px" @change="apply"><el-option v-for="s in scenes" :key="s.id" :label="s.name" :value="String(s.id)"/></el-select>
      <el-select v-model="filters.format" placeholder="形式" clearable style="width:120px" @change="apply"><el-option v-for="f in formats" :key="f.id" :label="f.name" :value="String(f.id)"/></el-select>
      <el-select v-model="filters.topic" placeholder="主题" clearable style="width:120px" @change="apply"><el-option v-for="t in topics" :key="t.id" :label="t.name" :value="String(t.id)"/></el-select>
      <el-select v-model="filters.cefr" placeholder="CEFR" clearable style="width:100px" @change="apply"><el-option v-for="lv in ['A1','A2','B1','B2','C1','C2']" :key="lv" :label="lv" :value="lv"/></el-select>
    </div>

    <div v-if="error" class="l-center__empty">加载失败，请稍后重试。</div>
    <div v-else v-loading="loading" class="l-center__grid">
      <p v-if="!loading && !page?.items.length" class="l-center__empty">暂无材料。</p>
      <RouterLink v-for="item in page?.items" :key="item.id" :to="`/english/listening/${item.slug}`" class="lc-card">
        <div class="lc-card__cover"><img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" loading="lazy"/><span v-else class="lc-card__fallback">♪</span></div>
        <div class="lc-card__body"><div class="lc-card__meta"><CefrBadge :level="item.cefrLevel"/><span class="lc-card__level">{{levelLabels[item.listeningLevel]}}</span></div>
          <h2 class="lc-card__title">{{item.title}}</h2><p class="lc-card__summary">{{item.summary}}</p>
          <div class="lc-card__tags"><span v-for="t in item.tags" :key="t.id" class="lc-card__tag">{{t.name}}</span></div>
          <span class="lc-card__cta">{{fmt(item.durationSeconds)}} · {{item.exerciseCount}} 练习 · 开始训练 →</span>
        </div>
      </RouterLink>
    </div>
    <el-pagination v-if="page && page.total>0" :current-page="Number(route.query.page??1)" :total="page.total" :page-size="20" layout="prev, pager, next" style="margin:24px 0" @current-change="goPage"/>
  </section>
</template>

<style scoped>
.l-center__stat{text-align:center;padding:14px 22px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.l-center__stat b{display:block;font-size:30px;color:var(--primary)}.l-center__stat span{font-size:12px;color:var(--text-muted)}
.l-center__route{display:flex;flex-wrap:wrap;gap:10px;margin-bottom:20px}.route-pill{display:flex;align-items:center;gap:8px;padding:8px 14px;border:1px solid var(--border);border-radius:999px;color:var(--text-secondary);transition:all .15s ease}.route-pill:hover{border-color:var(--primary);color:var(--primary)}.route-pill b{color:var(--primary)}
.l-center__filters{display:flex;flex-wrap:wrap;gap:var(--space-3);margin-bottom:20px}.l-center__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:var(--space-4);min-height:100px}.l-center__empty{color:var(--text-muted);padding:var(--space-6) 0;grid-column:1/-1}
.lc-card{display:flex;flex-direction:column;border:1px solid var(--border);border-radius:18px;overflow:hidden;background:var(--bg-surface);color:inherit;transition:transform .16s ease,border-color .16s ease}.lc-card:hover{transform:translateY(-3px);border-color:var(--primary)}
.lc-card__cover{height:120px;background:var(--bg-subtle);display:grid;place-items:center}.lc-card__cover img{width:100%;height:100%;object-fit:cover}.lc-card__fallback{font-size:40px;color:var(--primary)}
.lc-card__body{padding:16px}.lc-card__meta{display:flex;align-items:center;gap:8px;margin-bottom:8px}.lc-card__level{font-size:12px;color:var(--text-secondary)}.lc-card__title{font-size:18px;margin:0 0 6px}
.lc-card__summary{font-size:13px;color:var(--text-secondary);margin:0 0 8px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.lc-card__tags{display:flex;flex-wrap:wrap;gap:4px;margin-bottom:10px}.lc-card__tag{font-size:11px;padding:2px 8px;border-radius:999px;background:var(--bg-subtle);border:1px solid var(--border);color:var(--text-secondary)}.lc-card__cta{font-size:13px;color:var(--primary)}
@media(max-width:600px){.l-center__filters>*{width:calc(50% - 6px)!important}.l-center__filters>*:first-child{width:100%!important}.l-center__grid{grid-template-columns:1fr}.l-center__route{display:grid;grid-template-columns:1fr}.route-pill{justify-content:space-between}}
@media(prefers-reduced-motion:reduce){.route-pill,.lc-card{transition:none}.lc-card:hover{transform:none}}
</style>
