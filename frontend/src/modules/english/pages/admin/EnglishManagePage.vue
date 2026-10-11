<script setup>
/**
 * EnglishManagePage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue-router、../../../../shared/viewMode。
 */


import { RouterLink } from 'vue-router'
import { accountPath } from '../../../../shared/viewMode'

const domains = [
  { glyph: '词', title: '单词', en: 'VOCABULARY', description: '主题词库、音标、释义与例句。', path: '/english/manage/vocabulary' },
  { glyph: '语', title: '语法', en: 'GRAMMAR', description: '课程章节、课时正文与顺序。', path: '/english/manage/grammar' },
  { glyph: '读', title: '阅读', en: 'READING', description: '真实文章、双语对照与人工精读。', path: '/english/manage/reading' },
  { glyph: '写', title: '写作', en: 'WRITING', description: '本人英文创作、草稿与历史版本。', path: '/english/writing' },
]
</script>

<template>
  <main class="english-manage">
    <header class="english-manage__hero"><div><p class="public-eyebrow">ENGLISH SYSTEMS · 内容管理</p><h1>英语管理工作台</h1><p>集中管理单词、语法、阅读与写作。</p></div><RouterLink :to="accountPath('/english/manage/overview')">总览设置</RouterLink></header>
    <div class="english-manage__grid"><RouterLink v-for="item in domains" :key="item.title" :to="accountPath(item.path)" class="english-manage__card"><span>{{ item.glyph }}</span><small>{{ item.en }}</small><h2>{{ item.title }}</h2><p>{{ item.description }}</p><strong>进入管理 →</strong></RouterLink></div>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.english-manage。
 */
.english-manage{max-width:1380px;margin:auto;padding:28px 0 70px}
  /**
   * 样式范围：.english-manage__hero。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-manage__hero{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:28px;padding:28px;border:1px solid var(--border);border-radius:20px;background:linear-gradient(135deg,var(--primary-soft),var(--bg-surface))}
  /**
   * 样式范围：.english-manage__hero h1。
   */
  .english-manage__hero h1{margin:8px 0;font-size:34px}
  /**
   * 样式范围：.english-manage__hero p:last-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-manage__hero p:last-child{color:var(--text-secondary)}
  /**
   * 样式范围：.english-manage__hero a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-manage__hero a{padding:10px 16px;border:1px solid var(--border-strong);border-radius:10px;color:var(--primary);background:var(--bg-surface)}
  /**
   * 样式范围：.english-manage__grid。
   * 使用网格布局组织内容；列定义为 repeat(2,minmax(0,1fr))，间距由 15px 控制。
   */
  .english-manage__grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:15px}
  /**
   * 样式范围：.english-manage__card。
   * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-manage__card{display:flex;min-height:220px;flex-direction:column;padding:22px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);color:var(--text-primary);transition:transform .18s,border-color .18s}
  /**
   * 样式范围：.english-manage__card:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-manage__card:hover{transform:translateY(-3px);border-color:var(--primary)}
  /**
   * 样式范围：.english-manage__card>span。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-manage__card>span{display:grid;width:46px;height:46px;place-items:center;border-radius:13px;background:var(--primary-soft);color:var(--primary);font-size:23px;font-weight:750}
  /**
   * 样式范围：.english-manage__card small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-manage__card small{margin-top:22px;color:var(--accent);font-size:10px;letter-spacing:.13em}
  /**
   * 样式范围：.english-manage__card h2。
   */
  .english-manage__card h2{margin:6px 0;font-size:25px}
  /**
   * 样式范围：.english-manage__card p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-manage__card p{color:var(--text-secondary);font-size:13px;line-height:1.7}
  /**
   * 样式范围：.english-manage__card strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-manage__card strong{margin-top:auto;color:var(--primary);font-size:13px}
  /**
   * 响应式或设备条件：(max-width:650px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:650px){
  /**
   * 样式范围：.english-manage__hero。
   */
  .english-manage__hero{align-items:start;flex-direction:column}
  /**
   * 样式范围：.english-manage__grid。
   */
  .english-manage__grid{grid-template-columns:1fr}}
</style>
