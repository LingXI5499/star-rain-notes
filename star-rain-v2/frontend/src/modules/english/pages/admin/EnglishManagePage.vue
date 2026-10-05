<script setup>
import { RouterLink } from 'vue-router'
import { accountPath } from '../../../../shared/viewMode'

const domains = [
  { glyph: '词', title: '单词', en: 'VOCABULARY', description: '主题词库、音标、释义与例句。', path: '/english/manage/vocabulary' },
  { glyph: '语', title: '语法', en: 'GRAMMAR', description: '课程章节、课时正文与顺序。', path: '/english/manage/grammar' },
  { glyph: '读', title: '阅读', en: 'READING', description: '分级阅读文章与正文。', path: '/english/manage/reading' },
  { glyph: '听', title: '听力', en: 'LISTENING', description: '音频材料、文本与逐句片段。', path: '/english/manage/listening' },
  { glyph: '写', title: '写作', en: 'WRITING', description: '写作素材和练习任务。', path: '/english/manage/writing' },
]
</script>

<template>
  <main class="english-manage">
    <header class="english-manage__hero"><div><p class="public-eyebrow">ENGLISH SYSTEMS · 内容管理</p><h1>英语管理工作台</h1><p>按五个方向管理英语内容。</p></div><RouterLink :to="accountPath('/english/manage/overview')">总览设置</RouterLink></header>
    <div class="english-manage__grid"><RouterLink v-for="item in domains" :key="item.title" :to="accountPath(item.path)" class="english-manage__card"><span>{{ item.glyph }}</span><small>{{ item.en }}</small><h2>{{ item.title }}</h2><p>{{ item.description }}</p><strong>进入管理 →</strong></RouterLink></div>
  </main>
</template>

<style scoped>
.english-manage{max-width:1380px;margin:auto;padding:28px 0 70px}.english-manage__hero{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:28px;padding:28px;border:1px solid var(--border);border-radius:20px;background:linear-gradient(135deg,var(--primary-soft),var(--bg-surface))}.english-manage__hero h1{margin:8px 0;font-size:34px}.english-manage__hero p:last-child{color:var(--text-secondary)}.english-manage__hero a{padding:10px 16px;border:1px solid var(--border-strong);border-radius:10px;color:var(--primary);background:var(--bg-surface)}.english-manage__grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:15px}.english-manage__card{display:flex;min-height:250px;flex-direction:column;padding:22px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);color:var(--text-primary);transition:transform .18s,border-color .18s}.english-manage__card:hover{transform:translateY(-3px);border-color:var(--primary)}.english-manage__card>span{display:grid;width:46px;height:46px;place-items:center;border-radius:13px;background:var(--primary-soft);color:var(--primary);font-size:23px;font-weight:750}.english-manage__card small{margin-top:22px;color:var(--accent);font-size:10px;letter-spacing:.13em}.english-manage__card h2{margin:6px 0;font-size:25px}.english-manage__card p{color:var(--text-secondary);font-size:13px;line-height:1.7}.english-manage__card strong{margin-top:auto;color:var(--primary);font-size:13px}@media(max-width:1100px){.english-manage__grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:650px){.english-manage__hero{align-items:start;flex-direction:column}.english-manage__grid{grid-template-columns:1fr}}
</style>
