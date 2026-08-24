<script setup lang="ts">
import { RouterLink } from 'vue-router'

const modules = [
  { key: '词', title: '单词', en: 'VOCABULARY', desc: '主题词库、词形、例句与记忆状态。', to: '/admin/english/vocabulary', status: '可管理' },
  { key: '语', title: '语法', en: 'GRAMMAR', desc: '独立的英语语法完整教程与42个课节。', to: '/admin/english/grammar', status: '可管理' },
  { key: '读', title: '阅读', en: 'READING', desc: '分级阅读材料与精读训练。', to: '/admin/english/reading', status: '可管理' },
  { key: '写', title: '写作', en: 'WRITING', desc: '结构化写作与表达训练。', status: '待开发' },
  { key: '听', title: '听力', en: 'LISTENING', desc: '听力素材与训练计划。', status: '待开发' },
]
</script>

<template>
  <section class="english-workspace">
    <header class="english-workspace__hero">
      <div><p>ENGLISH SYSTEMS · 独立子系统</p><h1>英语管理工作台</h1><span>五个学习方向各自维护数据与流程，在同一入口清晰协作。</span></div>
      <RouterLink to="/admin/english/overview" class="english-workspace__settings">总览设置</RouterLink>
    </header>
    <div class="english-workspace__grid">
      <component :is="item.to ? RouterLink : 'article'" v-for="item in modules" :key="item.title" :to="item.to" class="module-card" :class="{ 'is-disabled': !item.to }">
        <div class="module-card__top"><span class="module-card__glyph">{{ item.key }}</span><em>{{ item.status }}</em></div>
        <p>{{ item.en }}</p><h2>{{ item.title }}</h2><span>{{ item.desc }}</span>
        <strong v-if="item.to">进入管理 →</strong><strong v-else>建设中</strong>
      </component>
    </div>

    <section class="english-workspace__shared">
      <div class="english-workspace__shared-head">
        <h2>共享底座</h2>
        <span>统一标签、CEFR、练习与学习组合 —— 阅读、听力、写作在此基础上构建。</span>
      </div>
      <div class="english-workspace__shared-grid">
        <RouterLink to="/admin/english/taxonomy" class="shared-card">
          <p>TAXONOMY</p><h3>标签管理</h3><span>六类语义标签，两级同维、无循环。</span>
        </RouterLink>
        <RouterLink to="/admin/english/bundles" class="shared-card">
          <p>LEARNING BUNDLES</p><h3>学习组合</h3><span>跨阅读、听力、写作的显式学习路径。</span>
        </RouterLink>
        <article class="shared-card is-future">
          <p>CEFR / EXERCISE</p><h3>CEFR 与练习</h3><span>六级标准与题型已在公共 meta 提供。</span>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.english-workspace{max-width:1380px;margin:auto}.english-workspace__hero{display:flex;justify-content:space-between;align-items:end;margin-bottom:32px;padding:26px 30px;border:1px solid var(--border);border-radius:20px;background:linear-gradient(135deg,color-mix(in srgb,var(--primary) 10%,var(--bg-surface)),var(--bg-surface))}.english-workspace__hero p,.module-card>p{color:var(--accent);font-size:11px;font-weight:750;letter-spacing:.14em}.english-workspace__hero h1{font-size:34px;margin:7px 0}.english-workspace__hero span,.module-card>span{color:var(--text-secondary)}.english-workspace__settings{padding:10px 16px;border:1px solid var(--border-strong);border-radius:10px;color:var(--text-primary)}.english-workspace__grid{display:grid;grid-template-columns:repeat(5,minmax(180px,1fr));gap:16px}.module-card{min-height:255px;display:flex;flex-direction:column;padding:22px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);color:inherit;transition:transform .16s ease,border-color .16s ease,box-shadow .16s ease}.module-card:not(.is-disabled):hover{transform:translateY(-3px);border-color:var(--primary);box-shadow:0 16px 36px rgb(16 49 39/.09)}.module-card__top{display:flex;justify-content:space-between;align-items:start;margin-bottom:28px}.module-card__glyph{display:grid;place-items:center;width:48px;height:48px;border-radius:15px;background:color-mix(in srgb,var(--primary) 12%,var(--bg-subtle));color:var(--primary);font-size:23px;font-weight:800}.module-card em{font-size:11px;font-style:normal;color:var(--primary)}.module-card h2{font-size:25px;margin:5px 0 12px}.module-card>span{font-size:14px;line-height:1.7}.module-card strong{margin-top:auto;color:var(--primary);font-size:13px}.module-card.is-disabled{opacity:.58}@media(max-width:1100px){.english-workspace__grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:620px){.english-workspace__hero{align-items:flex-start;flex-direction:column;gap:18px}.english-workspace__grid{grid-template-columns:1fr}}@media(prefers-reduced-motion:reduce){.module-card{transition:none}}
.english-workspace__shared{margin-top:36px;padding:22px;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface)}
.english-workspace__shared-head{display:flex;align-items:baseline;gap:12px;margin-bottom:18px}
.english-workspace__shared-head h2{font-size:18px;margin:0}
.english-workspace__shared-head span{font-size:13px;color:var(--text-secondary)}
.english-workspace__shared-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:14px}
.shared-card{display:flex;flex-direction:column;gap:6px;padding:18px;border:1px solid var(--border);border-radius:16px;background:var(--bg-subtle);color:inherit;transition:transform .16s ease,border-color .16s ease}
.shared-card:not(.is-future):hover{transform:translateY(-2px);border-color:var(--primary)}
.shared-card p{color:var(--accent);font-size:10px;font-weight:750;letter-spacing:.14em;margin:0}
.shared-card h3{font-size:17px;margin:0}
.shared-card span{font-size:13px;color:var(--text-secondary)}
.shared-card.is-future{opacity:.6}
</style>
