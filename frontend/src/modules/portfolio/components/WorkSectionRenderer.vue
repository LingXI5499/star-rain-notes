<script setup>
import WorkMarkdownBlock from './blocks/WorkMarkdownBlock.vue'
import WorkImageBlock from './blocks/WorkImageBlock.vue'
import WorkGalleryBlock from './blocks/WorkGalleryBlock.vue'
import WorkAudioBlock from './blocks/WorkAudioBlock.vue'
import WorkCodeBlock from './blocks/WorkCodeBlock.vue'
import WorkTimelineBlock from './blocks/WorkTimelineBlock.vue'
import WorkLinksBlock from './blocks/WorkLinksBlock.vue'
import WorkQuoteBlock from './blocks/WorkQuoteBlock.vue'
import WorkFeatureBlock from './blocks/WorkFeatureBlock.vue'
import WorkTechBlock from './blocks/WorkTechBlock.vue'
import WorkStatsBlock from './blocks/WorkStatsBlock.vue'
defineProps({ sections: { type: Array, default: () => [] } })
const renderers = { MARKDOWN: WorkMarkdownBlock, CUSTOM: WorkMarkdownBlock, IMAGE: WorkImageBlock,
 GALLERY: WorkGalleryBlock, AUDIO: WorkAudioBlock, CODE: WorkCodeBlock, TIMELINE: WorkTimelineBlock,
 LINKS: WorkLinksBlock, QUOTE: WorkQuoteBlock, FEATURE_LIST: WorkFeatureBlock, TECH_STACK: WorkTechBlock, STATS: WorkStatsBlock }
</script>
<template>
 <div class="work-sections">
  <section v-for="block in sections.filter(item => item.visible !== false)" :id="`work-section-${block.id}`" :key="block.id" class="work-block">
   <h2 v-if="block.title">{{ block.title }}</h2>
   <component :is="renderers[block.sectionType]" v-if="renderers[block.sectionType]" :block="block" />
  </section>
 </div>
</template>
<style>
.work-block{scroll-margin-top:110px;margin:0 0 48px}.work-block>h2{margin:0 0 24px;padding-bottom:16px;border-bottom:1px solid var(--border);font-size:clamp(24px,3vw,32px)}.work-block figure{margin:0}.work-block img{display:block;width:100%;height:auto;border-radius:14px}.work-block figcaption{margin-top:10px;color:var(--text-muted);font-size:13px;line-height:1.6}.work-block__gallery{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px}.work-block__audio{padding:24px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.work-block audio{width:100%}.work-block__code{overflow:auto;padding:24px;border-radius:14px;background:var(--bg-subtle);border:1px solid var(--border);font:14px/1.8 Consolas,monospace;white-space:pre}.work-block__timeline{list-style:none;margin:0;padding:0 0 0 24px;border-left:2px solid var(--border)}.work-block__timeline li{position:relative;padding:0 0 28px}.work-block__timeline li::before{content:'';position:absolute;left:-30px;top:5px;width:10px;height:10px;border-radius:50%;background:var(--primary)}.work-block time{color:var(--accent);font-size:12px}.work-block h3{margin:8px 0;font-size:18px}.work-block p{color:var(--text-secondary);line-height:1.8}.work-block__grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.work-block__tile{padding:22px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}.work-block__tile p{margin-bottom:0}.work-block__links a{display:block;color:var(--primary);font-weight:700}.work-block blockquote{margin:0;padding:26px 30px;border-left:4px solid var(--primary);background:var(--bg-subtle);border-radius:0 14px 14px 0}.work-block blockquote p{white-space:pre-wrap;font-size:20px}.work-block cite{color:var(--text-muted);font-size:13px}.work-block__tech{display:flex;gap:10px;flex-wrap:wrap}.work-block__tech span{padding:10px 16px;border:1px solid var(--border);border-radius:999px;color:var(--primary);background:var(--bg-surface)}.work-block__tech small{margin-left:8px;color:var(--text-muted)}.work-block__stat strong{display:block;color:var(--primary);font-size:32px}.work-block__stat span{color:var(--text-muted)}@media(max-width:600px){.work-block__gallery,.work-block__grid{grid-template-columns:1fr}}
</style>
