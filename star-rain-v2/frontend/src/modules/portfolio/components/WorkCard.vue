<script setup>
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import { stageLabels } from '../support/workBlocks'

defineProps({
 work: { type: Object, required: true },
 prominent: { type: Boolean, default: false },
 headingLevel: { type: String, default: 'h3' },
})
const { contentPath } = useViewMode()
</script>

<template>
 <RouterLink :to="contentPath(`/portfolio/${work.slug}`)" class="works-card" :class="{ 'works-card--prominent': prominent }">
  <div class="works-card__cover">
   <img v-if="work.coverUrl" :src="work.coverUrl" :alt="work.title" :loading="prominent ? 'eager' : 'lazy'" />
   <span v-else aria-hidden="true">{{ work.title.slice(0, 1) }}</span>
   <small v-if="work.featured">精选作品</small>
  </div>
  <div class="works-card__body">
   <p class="works-card__kind">{{ work.category?.name || '作品' }} <span v-if="work.format">/ {{ work.format.name }}</span></p>
   <component :is="headingLevel" class="works-card__title">{{ work.title }}</component>
   <p v-if="work.subtitle" class="works-card__subtitle">{{ work.subtitle }}</p>
   <p class="works-card__summary">{{ work.summary }}</p>
   <div v-if="work.tags?.length" class="works-card__tags"><span v-for="tag in work.tags.slice(0, 4)" :key="tag.id">{{ tag.name }}</span></div>
   <footer><span>{{ stageLabels[work.projectStatus] || '作品记录' }}</span><strong>探索作品 →</strong></footer>
  </div>
 </RouterLink>
</template>

<style scoped>
.works-card{display:flex;flex-direction:column;overflow:hidden;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface);color:var(--text-primary);text-decoration:none;transition:transform .2s,border-color .2s}.works-card:hover{transform:translateY(-4px);border-color:var(--primary)}.works-card__cover{position:relative;height:240px;display:grid;place-items:center;background:linear-gradient(135deg,color-mix(in srgb,var(--primary) 18%,var(--bg-surface)),var(--bg-subtle))}.works-card__cover>span{font:90px Georgia,serif;color:var(--primary);opacity:.7}.works-card__cover img{width:100%;height:100%;object-fit:cover}.works-card__cover small{position:absolute;top:18px;left:18px;padding:7px 12px;border-radius:999px;background:var(--primary);color:var(--on-primary);font-size:11px}.works-card__body{padding:26px;display:flex;flex-direction:column;flex:1;min-width:0}.works-card__kind{color:var(--accent);font-size:12px;margin:0 0 10px}.works-card__kind span{color:var(--text-muted)}.works-card__title{font-size:25px;line-height:1.4;margin:0 0 12px;overflow-wrap:anywhere}.works-card__subtitle{color:var(--text-muted);margin:0 0 8px}.works-card__summary{color:var(--text-secondary);font-size:14px;line-height:1.8;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden;margin:0 0 24px}.works-card__tags{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:24px}.works-card__tags span{font-size:11px;padding:5px 10px;border:1px solid var(--border);border-radius:999px;color:var(--primary)}.works-card footer{display:flex;justify-content:space-between;gap:12px;border-top:1px solid var(--border);padding-top:18px;margin-top:auto;font-size:12px;color:var(--text-muted)}.works-card footer strong{color:var(--primary)}.works-card--prominent{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);border-color:color-mix(in srgb,var(--primary) 50%,var(--border))}.works-card--prominent .works-card__cover{height:100%;min-height:350px}.works-card--prominent .works-card__cover>span{font-size:128px}.works-card--prominent .works-card__body{padding:40px}.works-card--prominent .works-card__title{font-size:clamp(28px,3vw,36px)}.works-card--prominent .works-card__summary{-webkit-line-clamp:5;font-size:15px}
@media(max-width:700px){.works-card--prominent{grid-template-columns:1fr}.works-card__cover,.works-card--prominent .works-card__cover{height:220px;min-height:0}.works-card--prominent .works-card__body{padding:26px}.works-card--prominent .works-card__cover>span{font-size:90px}}
@media(prefers-reduced-motion:reduce){.works-card{transition:none}.works-card:hover{transform:none}}
</style>
