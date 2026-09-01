<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { fetchPublicBundle, fetchPublicBundleItems, type BundleItem, type LearningBundle } from '@/api/englishBundle'
import { fetchLearningRecords, type LearningRecord } from '@/api/englishLearning'
import CefrBadge from '@/components/english/CefrBadge.vue'
import { applyPageMeta } from '@/lib/seo'

const route=useRoute();const bundle=ref<LearningBundle|null>(null);const items=ref<BundleItem[]>([]);const records=ref<Record<string,LearningRecord|null>>({});const loading=ref(true);const failed=ref(false)
const labels={READING:'阅读输入',LISTENING:'听力理解',WRITING:'写作输出'} as const
const completeCount=computed(()=>items.value.filter(item=>records.value[key(item)]?.status==='COMPLETED').length)
const progress=computed(()=>items.value.length?Math.round(completeCount.value/items.value.length*100):0)
const nextItem=computed(()=>items.value.find(item=>records.value[key(item)]?.status!=='COMPLETED')??null)
const moduleCounts=computed(()=>items.value.reduce((counts,item)=>{counts[item.contentType]=(counts[item.contentType]??0)+1;return counts},{READING:0,LISTENING:0,WRITING:0} as Record<BundleItem['contentType'],number>))
function key(item:BundleItem){return `${item.contentType}-${item.contentId}`}
function target(item:BundleItem){return item.contentType==='READING'?`/english/reading/${item.slug}`:item.contentType==='LISTENING'?`/english/listening/${item.slug}`:`/english/writing/practice/${item.slug}`}
async function load(){loading.value=true;failed.value=false;try{const slug=String(route.params.slug);const [meta,list]=await Promise.all([fetchPublicBundle(slug),fetchPublicBundleItems(slug)]);bundle.value=meta;items.value=list;applyPageMeta({title:meta.title,description:meta.summary||undefined});const batch=await fetchLearningRecords(list.map(item=>({contentType:item.contentType,contentId:item.contentId}))).catch(()=>({} as Record<string,LearningRecord>));records.value=Object.fromEntries(list.map(item=>[key(item),batch[`${item.contentType}:${item.contentId}`]??null]))}catch{failed.value=true}finally{loading.value=false}}
watch(()=>route.params.slug,load,{immediate:true})
</script>

<template>
  <section class="bundle-detail">
    <p v-if="loading" class="state">正在准备学习路径…</p><p v-else-if="failed||!bundle" class="state">该学习组合暂不可用。</p>
    <template v-else>
      <header class="hero" :style="bundle.coverUrl?{backgroundImage:`linear-gradient(90deg,var(--bg-surface) 30%,transparent),url(${bundle.coverUrl})`}:{}">
        <div><RouterLink to="/english/bundles" class="breadcrumb">学习组合 /</RouterLink><CefrBadge :level="bundle.primaryCefr"/><h1>{{ bundle.title }}</h1><p>{{ bundle.summary || '跨阅读、听力与写作完成一次主题学习闭环。' }}</p></div>
        <aside><strong>{{ progress }}%</strong><span>{{ completeCount }} / {{ items.length }} 已完成</span><div><i :style="{width:`${progress}%`}"/></div><RouterLink v-if="nextItem" :to="target(nextItem)">{{ completeCount ? '继续下一步' : '开始这条路径' }} →</RouterLink><b v-else>路径已完成</b></aside>
      </header>
      <section class="path-summary"><span v-for="(label,type) in labels" :key="type"><b>{{ moduleCounts[type] }}</b>{{ label }}</span><em>{{ items.length }} 个连续步骤</em></section>
      <section class="path"><div class="path-line"/><article v-for="(item,index) in items" :key="key(item)" :class="{complete:records[key(item)]?.status==='COMPLETED',current:nextItem&&key(nextItem)===key(item)}"><span class="node">{{ records[key(item)]?.status==='COMPLETED'?'✓':index+1 }}</span><div class="content"><small>STEP {{ String(index+1).padStart(2,'0') }} · {{ labels[item.contentType] }}<template v-if="item.cefrLevel"> · {{ item.cefrLevel }}</template></small><h2>{{ item.title }}</h2><p>{{ item.summary || '进入内容开始本阶段学习。' }}</p><div class="meta"><span>{{ records[key(item)]?.status==='COMPLETED'?'已完成':records[key(item)]?.status==='IN_PROGRESS'?'学习中':nextItem&&key(nextItem)===key(item)?'建议下一步':'未开始' }}</span><RouterLink :to="target(item)">{{ records[key(item)]?'继续学习':'开始学习' }} →</RouterLink></div></div></article><p v-if="!items.length" class="state">该组合暂未编排公开内容。</p></section>
    </template>
  </section>
</template>

<style scoped>
.bundle-detail{max-width:1040px;margin:auto}.hero{display:grid;grid-template-columns:minmax(0,1fr) 220px;gap:40px;align-items:end;padding:40px;border:1px solid var(--border);border-radius:26px;background-color:var(--bg-surface);background-position:right center;background-size:cover}.breadcrumb{display:inline-block;margin-right:10px;color:var(--text-muted);font-size:12px}.hero h1{margin:16px 0 12px;font-size:clamp(34px,5vw,56px);line-height:1.1}.hero p{max-width:650px;color:var(--text-secondary);line-height:1.7}.hero aside{padding:18px;border-radius:18px;background:color-mix(in srgb,var(--bg-surface) 88%,transparent);backdrop-filter:blur(12px)}.hero aside strong{display:block;font-size:34px}.hero aside span{color:var(--text-muted);font-size:12px}.hero aside div{height:7px;margin-top:14px;border-radius:999px;background:var(--border);overflow:hidden}.hero aside i{display:block;height:100%;background:linear-gradient(90deg,var(--primary),var(--accent))}.hero aside>a,.hero aside>b{display:block;margin-top:16px;color:var(--primary);font-size:12px}.path-summary{display:flex;max-width:780px;gap:9px;align-items:center;margin:28px auto -12px}.path-summary span{padding:9px 12px;border-radius:999px;background:var(--bg-subtle);color:var(--text-muted);font-size:11px}.path-summary b{margin-right:5px;color:var(--text-primary)}.path-summary em{margin-left:auto;color:var(--text-muted);font-size:11px;font-style:normal}.path{position:relative;max-width:780px;margin:44px auto}.path-line{position:absolute;top:0;bottom:0;left:25px;width:1px;background:linear-gradient(var(--primary),var(--border))}.path article{position:relative;display:grid;grid-template-columns:52px 1fr;gap:18px;margin-bottom:18px}.node{z-index:1;display:grid;width:52px;height:52px;place-items:center;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface);color:var(--text-muted);font-weight:800}.complete .node{border-color:var(--primary);background:var(--primary);color:white}.current .node{border-color:var(--accent);box-shadow:0 0 0 5px color-mix(in srgb,var(--accent) 12%,transparent);color:var(--accent)}.content{padding:21px;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface)}.current .content{border-color:color-mix(in srgb,var(--accent) 50%,var(--border))}.content small{color:var(--accent);font-size:10px;font-weight:800;letter-spacing:.12em}.content h2{margin:8px 0;font-size:20px}.content p{color:var(--text-secondary);line-height:1.6}.meta{display:flex;justify-content:space-between;margin-top:18px;padding-top:14px;border-top:1px solid var(--border);font-size:12px}.meta span{color:var(--text-muted)}.meta a{color:var(--primary)}.state{padding:90px 0;text-align:center;color:var(--text-muted)}@media(max-width:700px){.hero{grid-template-columns:1fr;padding:24px}.path-summary{align-items:flex-start;flex-wrap:wrap}.path-summary em{width:100%;margin-left:0}.path article{grid-template-columns:42px 1fr;gap:10px}.node{width:42px;height:42px;border-radius:13px}.path-line{left:20px}.content{padding:16px}}
</style>
