<script setup>
/**
 * WorkSectionRenderer.vue：作品模块的视图组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：./blocks/WorkMarkdownBlock.vue、./blocks/WorkImageBlock.vue、./blocks/WorkGalleryBlock.vue、./blocks/WorkAudioBlock.vue、./blocks/WorkCodeBlock.vue、./blocks/WorkTimelineBlock.vue。
 * 组件输入：sections；默认值与类型见 defineProps。
 */


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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.work-block。
 */
.work-block{scroll-margin-top:110px;margin:0 0 48px}
  /**
   * 样式范围：.work-block>h2。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block>h2{margin:0 0 24px;padding-bottom:16px;border-bottom:1px solid var(--border);font-size:clamp(24px,3vw,32px)}
  /**
   * 样式范围：.work-block figure。
   */
  .work-block figure{margin:0}
  /**
   * 样式范围：.work-block img。
   */
  .work-block img{display:block;width:100%;height:auto;border-radius:14px}
  /**
   * 样式范围：.work-block figcaption。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block figcaption{margin-top:10px;color:var(--text-muted);font-size:13px;line-height:1.6}
  /**
   * 样式范围：.work-block__gallery。
   * 使用网格布局组织内容；列定义为 repeat(2,minmax(0,1fr))，间距由 20px 控制。
   */
  .work-block__gallery{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px}
  /**
   * 样式范围：.work-block__audio。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block__audio{padding:24px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}
  /**
   * 样式范围：.work-block audio。
   */
  .work-block audio{width:100%}
  /**
   * 样式范围：.work-block__code。
   * 溢出处理：整体 auto。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block__code{overflow:auto;padding:24px;border-radius:14px;background:var(--bg-subtle);border:1px solid var(--border);font:14px/1.8 Consolas,monospace;white-space:pre}
  /**
   * 样式范围：.work-block__timeline。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block__timeline{list-style:none;margin:0;padding:0 0 0 24px;border-left:2px solid var(--border)}
  /**
   * 样式范围：.work-block__timeline li。
   */
  .work-block__timeline li{position:relative;padding:0 0 28px}
  /**
   * 样式范围：.work-block__timeline li::before。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block__timeline li::before{content:'';position:absolute;left:-30px;top:5px;width:10px;height:10px;border-radius:50%;background:var(--primary)}
  /**
   * 样式范围：.work-block time。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block time{color:var(--accent);font-size:12px}
  /**
   * 样式范围：.work-block h3。
   */
  .work-block h3{margin:8px 0;font-size:18px}
  /**
   * 样式范围：.work-block p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block p{color:var(--text-secondary);line-height:1.8}
  /**
   * 样式范围：.work-block__grid。
   * 使用网格布局组织内容；列定义为 repeat(2,minmax(0,1fr))，间距由 16px 控制。
   */
  .work-block__grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}
  /**
   * 样式范围：.work-block__tile。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block__tile{padding:22px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}
  /**
   * 样式范围：.work-block__tile p。
   */
  .work-block__tile p{margin-bottom:0}
  /**
   * 样式范围：.work-block__links a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block__links a{display:block;color:var(--primary);font-weight:700}
  /**
   * 样式范围：.work-block blockquote。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block blockquote{margin:0;padding:26px 30px;border-left:4px solid var(--primary);background:var(--bg-subtle);border-radius:0 14px 14px 0}
  /**
   * 样式范围：.work-block blockquote p。
   */
  .work-block blockquote p{white-space:pre-wrap;font-size:20px}
  /**
   * 样式范围：.work-block cite。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block cite{color:var(--text-muted);font-size:13px}
  /**
   * 样式范围：.work-block__tech。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .work-block__tech{display:flex;gap:10px;flex-wrap:wrap}
  /**
   * 样式范围：.work-block__tech span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block__tech span{padding:10px 16px;border:1px solid var(--border);border-radius:999px;color:var(--primary);background:var(--bg-surface)}
  /**
   * 样式范围：.work-block__tech small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block__tech small{margin-left:8px;color:var(--text-muted)}
  /**
   * 样式范围：.work-block__stat strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block__stat strong{display:block;color:var(--primary);font-size:32px}
  /**
   * 样式范围：.work-block__stat span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-block__stat span{color:var(--text-muted)}
  /**
   * 响应式或设备条件：(max-width:600px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:600px){
  /**
   * 样式范围：.work-block__gallery,.work-block__grid。
   */
  .work-block__gallery,.work-block__grid{grid-template-columns:1fr}}
</style>
