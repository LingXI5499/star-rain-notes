<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'
import { workTechnologies } from '../support/workPresentation'

const props = defineProps({
 work: { type: Object, required: true },
 prominent: { type: Boolean, default: false },
 reverse: { type: Boolean, default: false },
 headingLevel: { type: String, default: 'h3' },
})
const { contentPath } = useViewMode()
const technologies = computed(() => workTechnologies(props.work).slice(0, 3))
</script>

<template>
 <RouterLink :to="contentPath(`/portfolio/${work.slug}`)" class="works-card" :class="{ 'works-card--prominent': prominent, 'works-card--reverse': reverse }">
  <div class="works-card__cover">
   <img v-if="work.coverUrl" :src="work.coverUrl" :alt="work.title" :loading="prominent ? 'eager' : 'lazy'" />
   <span v-else aria-hidden="true">{{ work.title.slice(0, 1) }}</span>
  </div>
  <div class="works-card__body">
   <p class="works-card__kind">{{ work.category?.name || '作品' }} <span v-if="work.format">/ {{ work.format.name }}</span></p>
   <component :is="headingLevel" class="works-card__title">{{ work.title }}</component>
   <p class="works-card__summary">{{ work.summary }}</p>
   <div v-if="technologies.length" class="works-card__tags"><span v-for="tag in technologies" :key="tag">{{ tag }}</span></div>
   <span class="works-card__explore">探索作品 <span aria-hidden="true">↗</span></span>
  </div>
 </RouterLink>
</template>

<style scoped>
.works-card{display:flex;flex-direction:column;min-width:0;overflow:hidden;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);color:var(--text-primary);text-decoration:none;transition:transform .2s,border-color .2s}
.works-card:hover{transform:translateY(-3px);border-color:var(--primary)}
.works-card:focus-visible{outline:3px solid var(--primary);outline-offset:5px}
.works-card__cover{position:relative;aspect-ratio:16/9;overflow:hidden;display:grid;place-items:center;background:linear-gradient(135deg,color-mix(in srgb,var(--primary) 18%,var(--bg-surface)),var(--bg-subtle))}
.works-card__cover>span{font:90px Georgia,serif;color:var(--primary);opacity:.7}
.works-card__cover img{position:absolute;width:100%;height:100%;object-fit:cover;transition:transform .2s}
.works-card:hover img{transform:scale(1.025)}
.works-card__body{padding:26px;display:flex;flex-direction:column;flex:1;min-width:0;justify-content:center}
.works-card__kind{color:var(--accent);font-size:12px;margin:0 0 12px}
.works-card__kind span{color:var(--text-muted)}
.works-card__title{font-size:25px;line-height:1.4;margin:0 0 12px;overflow-wrap:anywhere}
.works-card__summary{color:var(--text-secondary);font-size:14px;line-height:1.9;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden;overflow-wrap:anywhere;margin:0 0 22px}
.works-card__tags{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:24px}
.works-card__tags span{font-size:11px;padding:5px 10px;border:1px solid var(--border);border-radius:999px;color:var(--primary);overflow-wrap:anywhere}
.works-card__explore{margin-top:auto;font-size:13px;color:var(--primary)}
.works-card--prominent{display:grid;grid-template-columns:repeat(2,minmax(0,1fr))}
.works-card--prominent .works-card__cover{align-self:center;width:100%}
.works-card--prominent .works-card__body{padding:clamp(24px,4vw,46px)}
.works-card--prominent .works-card__title{font-size:clamp(28px,3vw,36px)}
.works-card--reverse .works-card__cover{grid-column:2;grid-row:1}
.works-card--reverse .works-card__body{grid-column:1;grid-row:1}
@media(max-width:700px){
 .works-card--prominent{grid-template-columns:1fr}
 .works-card--reverse .works-card__cover,.works-card--reverse .works-card__body{grid-column:auto;grid-row:auto}
}
@media(prefers-reduced-motion:reduce){.works-card,.works-card__cover img{transition:none}.works-card:hover,.works-card:hover img{transform:none}}
</style>
