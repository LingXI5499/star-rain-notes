<script setup>
/**
 * WorkCard.vue：作品模块的视图组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue-router、../../../shared/viewMode、../support/workBlocks。
 * 组件输入：work、prominent、headingLevel；默认值与类型见 defineProps。
 */


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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.works-card。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.works-card{display:flex;flex-direction:column;overflow:hidden;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface);color:var(--text-primary);text-decoration:none;transition:transform .2s,border-color .2s}
  /**
   * 样式范围：.works-card:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card:hover{transform:translateY(-4px);border-color:var(--primary)}
  /**
   * 样式范围：.works-card__cover。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card__cover{position:relative;height:240px;display:grid;place-items:center;background:linear-gradient(135deg,color-mix(in srgb,var(--primary) 18%,var(--bg-surface)),var(--bg-subtle))}
  /**
   * 样式范围：.works-card__cover>span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card__cover>span{font:90px Georgia,serif;color:var(--primary);opacity:.7}
  /**
   * 样式范围：.works-card__cover img。
   */
  .works-card__cover img{width:100%;height:100%;object-fit:cover}
  /**
   * 样式范围：.works-card__cover small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card__cover small{position:absolute;top:18px;left:18px;padding:7px 12px;border-radius:999px;background:var(--primary);color:var(--on-primary);font-size:11px}
  /**
   * 样式范围：.works-card__body。
   * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
   */
  .works-card__body{padding:26px;display:flex;flex-direction:column;flex:1;min-width:0}
  /**
   * 样式范围：.works-card__kind。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card__kind{color:var(--accent);font-size:12px;margin:0 0 10px}
  /**
   * 样式范围：.works-card__kind span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card__kind span{color:var(--text-muted)}
  /**
   * 样式范围：.works-card__title。
   */
  .works-card__title{font-size:25px;line-height:1.4;margin:0 0 12px;overflow-wrap:anywhere}
  /**
   * 样式范围：.works-card__subtitle。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card__subtitle{color:var(--text-muted);margin:0 0 8px}
  /**
   * 样式范围：.works-card__summary。
   * 溢出处理：整体 hidden。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card__summary{color:var(--text-secondary);font-size:14px;line-height:1.8;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden;margin:0 0 24px}
  /**
   * 样式范围：.works-card__tags。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .works-card__tags{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:24px}
  /**
   * 样式范围：.works-card__tags span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card__tags span{font-size:11px;padding:5px 10px;border:1px solid var(--border);border-radius:999px;color:var(--primary)}
  /**
   * 样式范围：.works-card footer。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card footer{display:flex;justify-content:space-between;gap:12px;border-top:1px solid var(--border);padding-top:18px;margin-top:auto;font-size:12px;color:var(--text-muted)}
  /**
   * 样式范围：.works-card footer strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card footer strong{color:var(--primary)}
  /**
   * 样式范围：.works-card--prominent。
   * 使用网格布局组织内容；列定义为 minmax(0,1fr) minmax(0,1fr)，间距由 各行列间距规则 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .works-card--prominent{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);border-color:color-mix(in srgb,var(--primary) 50%,var(--border))}
  /**
   * 样式范围：.works-card--prominent .works-card__cover。
   */
  .works-card--prominent .works-card__cover{height:100%;min-height:350px}
  /**
   * 样式范围：.works-card--prominent .works-card__cover>span。
   */
  .works-card--prominent .works-card__cover>span{font-size:128px}
  /**
   * 样式范围：.works-card--prominent .works-card__body。
   */
  .works-card--prominent .works-card__body{padding:40px}
  /**
   * 样式范围：.works-card--prominent .works-card__title。
   */
  .works-card--prominent .works-card__title{font-size:clamp(28px,3vw,36px)}
  /**
   * 样式范围：.works-card--prominent .works-card__summary。
   */
  .works-card--prominent .works-card__summary{-webkit-line-clamp:5;font-size:15px}
/**
 * 响应式或设备条件：(max-width:700px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:700px){
  /**
   * 样式范围：.works-card--prominent。
   */
  .works-card--prominent{grid-template-columns:1fr}
  /**
   * 样式范围：.works-card__cover,.works-card--prominent .works-card__cover。
   */
  .works-card__cover,.works-card--prominent .works-card__cover{height:220px;min-height:0}
  /**
   * 样式范围：.works-card--prominent .works-card__body。
   */
  .works-card--prominent .works-card__body{padding:26px}
  /**
   * 样式范围：.works-card--prominent .works-card__cover>span。
   */
  .works-card--prominent .works-card__cover>span{font-size:90px}}
/**
 * 响应式或设备条件：(prefers-reduced-motion:reduce)；仅在该条件成立时覆盖基础样式。
 */
@media(prefers-reduced-motion:reduce){
  /**
   * 样式范围：.works-card。
   */
  .works-card{transition:none}
  /**
   * 样式范围：.works-card:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .works-card:hover{transform:none}}
</style>
