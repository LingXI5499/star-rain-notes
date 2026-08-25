<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchPublicEnglish, type EnglishView } from '@/api/english'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { fetchLearningSummary, type LearningSummary } from '@/api/englishLearning'

const english = ref<EnglishView | null>(null)
const loading = ref(true)
const error = ref(false)
const progress = ref<LearningSummary | null>(null)

const stageLabels: Record<string, string> = {
  FOUNDATION: '基础阶段（FOUNDATION）',
  WORD_MEMORY: '词汇记忆',
  READING: '阅读',
  ANALYTICS: '高阶分析',
}

const directions = [
  { icon: '词', name: '单词', en: 'VOCABULARY', description: '按主题积累常用词与高频表达，建立可检索的词汇网络。', to: '/english/vocabulary', cta: '进入词库', status: '' },
  { icon: '语', name: '语法', en: 'GRAMMAR', description: '10章42课，从词法到复杂句法建立完整语法框架。', to: '/english/grammar', cta: '开始课程', status: '' },
  { icon: '读', name: '阅读', en: 'READING', description: '分级精读材料，能力×主题×文体×CEFR 组织。', to: '/english/reading', cta: '开始阅读', status: '' },
  { icon: '写', name: '写作', en: 'WRITING', description: '从句子、段落到风格，用课程、模板与任务训练清晰表达。', to: '/english/writing', cta: '开始写作', status: '' },
  { icon: '听', name: '听力', en: 'LISTENING', description: '分层听力训练，逐句精听与语音规则。', to: '/english/listening', cta: '开始训练', status: '' },
]

onMounted(async () => {
  try {
    const [content, learning] = await Promise.all([fetchPublicEnglish(), fetchLearningSummary().catch(() => null)])
    english.value = content
    progress.value = learning
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="english">
    <div class="english__layout">
      <!-- main: hero + learning content -->
      <div class="english__main">
        <header class="english__hero">
          <p class="english__eyebrow">ENGLISH · LONG-TERM SKILL</p>
          <h1 class="english__title">{{ english?.title ?? 'English' }}</h1>
          <p class="english__subtitle">{{ english?.subtitle ?? 'Build English as a long-term skill.' }}</p>
        </header>

        <div v-if="loading" class="english__empty">加载中…</div>
        <div v-else-if="error" class="english__empty">加载失败，请稍后重试。</div>
        <template v-else>
          <section class="english__section">
            <h2 class="english__section-title">为什么学英语</h2>
            <p class="english__intro">{{ english?.introduction ?? '技术世界以英语为主，长期投资英语是职业与认知的复利。' }}</p>
          </section>

          <section class="english__section">
            <h2 class="english__section-title">学习方向</h2>
            <div class="english__directions">
              <template v-for="direction in directions" :key="direction.name">
                <RouterLink v-if="direction.to" :to="direction.to" class="english__direction english__direction--link">
                  <div class="english__direction-head"><span>{{ direction.icon }}</span><small>{{ direction.en }}</small></div>
                  <h3 class="english__direction-name">{{ direction.name }}</h3>
                  <p class="english__direction-desc">{{ direction.description }}</p>
                  <span class="english__direction-cta">{{ direction.cta }} →</span>
                </RouterLink>
                <div v-else class="english__direction">
                  <div class="english__direction-head"><span>{{ direction.icon }}</span><small>{{ direction.en }}</small></div>
                  <h3 class="english__direction-name">{{ direction.name }}</h3>
                  <p class="english__direction-desc">{{ direction.description }}</p>
                  <span class="english__direction-status">{{ direction.status }}</span>
                </div>
              </template>
            </div>
          </section>

          <section class="english__section">
            <h2 class="english__section-title">主题词汇库</h2>
            <RouterLink to="/english/vocabulary" class="english__vocab-banner">
              <div>
                <p class="english__vocab-banner-title">按主题卡片学习，边学边记</p>
                <p class="english__vocab-banner-desc">
                  15 个词层 · 60 个主题 · 8500+ 词条：词性 / 美式音标 / 中文 / 词形变化，支持例句与记忆打卡。
                </p>
              </div>
              <span class="english__vocab-banner-cta">开始学习 →</span>
            </RouterLink>
          </section>

          <section v-if="english?.roadmapMarkdown" class="english__section">
            <h2 class="english__section-title">路线图</h2>
            <MarkdownRenderer :source="english.roadmapMarkdown" />
          </section>
        </template>
      </div>

      <!-- aside: learning status dashboard -->
      <aside class="english__aside" aria-label="学习概览">
        <div class="english__card">
          <p class="english__card-title">当前阶段</p>
          <p class="english__stage">
            {{ english ? (stageLabels[english.currentStage] ?? english.currentStage) : '—' }}
          </p>
        </div>

        <div class="english__card">
          <p class="english__card-title">学习模块</p>
          <ul class="english__modules">
            <li v-for="direction in directions" :key="direction.name">{{ direction.name }}</li>
          </ul>
        </div>

        <div class="english__card">
          <p class="english__card-title">我的学习进度</p>
          <p class="english__stage">{{ progress?.completed ?? 0 }} 项已完成</p>
          <ul class="english__modules">
            <li>学习中 {{ progress?.inProgress ?? 0 }}</li>
            <li>待复习 {{ progress?.dueForReview ?? 0 }}</li>
            <li>累计记录 {{ progress?.total ?? 0 }}</li>
          </ul>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.english__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--aside-width);
  gap: var(--layout-gap);
  align-items: start;
}

.english__main {
  min-width: 0;
  max-width: 820px;
  margin-inline: auto;
  width: 100%;
}

.english__eyebrow {
  font-size: 14px;
  letter-spacing: 0.18em;
  color: var(--accent);
  margin-bottom: var(--space-4);
}

.english__title {
  font-size: 42px;
  line-height: 50px;
  margin-bottom: var(--space-3);
}

.english__subtitle {
  font-size: 18px;
  color: var(--text-secondary);
  margin-bottom: var(--space-10);
}

.english__section {
  margin-bottom: var(--space-12);
}

.english__section-title {
  font-size: 24px;
  line-height: 32px;
  margin-bottom: var(--space-5);
}

.english__intro {
  font-size: 16px;
  line-height: 28px;
  color: var(--text-secondary);
}

.english__directions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--layout-gap);
}

.english__direction {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: var(--space-6);
  min-height: 210px;
  background: linear-gradient(145deg,var(--bg-surface),color-mix(in srgb,var(--primary) 3%,var(--bg-surface)));
  color: inherit;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    border-color 0.15s ease;
}
.english__direction-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px}.english__direction-head>span{display:grid;place-items:center;width:42px;height:42px;border-radius:13px;background:color-mix(in srgb,var(--primary) 12%,var(--bg-subtle));color:var(--primary);font-size:20px;font-weight:800}.english__direction-head small{color:var(--accent);font-size:10px;letter-spacing:.12em}.english__direction-status{margin-top:auto;color:var(--text-muted);font-size:12px}

.english__direction--link:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgb(0 0 0 / 0.08);
  border-color: var(--primary);
}

.english__direction-name {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: var(--space-2);
}

.english__direction-desc {
  font-size: 14px;
  line-height: 22px;
  color: var(--text-secondary);
}

.english__direction-cta {
  margin-top: auto;
  font-size: 13px;
  color: var(--primary);
}

/* vocabulary banner entry */
.english__vocab-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
  padding: var(--space-6) var(--space-7);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background:
    linear-gradient(120deg, color-mix(in srgb, var(--primary) 8%, transparent), transparent 60%),
    var(--bg-surface);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    border-color 0.15s ease;
}

.english__vocab-banner:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgb(0 0 0 / 0.08);
  border-color: var(--primary);
}

.english__vocab-banner-title {
  font-size: 19px;
  font-weight: 600;
  margin-bottom: var(--space-2);
}

.english__vocab-banner-desc {
  font-size: 14px;
  line-height: 22px;
  color: var(--text-secondary);
}

.english__vocab-banner-cta {
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary);
}

.english__empty {
  color: var(--text-muted);
  padding: var(--space-8) 0;
}

/* ---------- aside dashboard ---------- */
.english__aside {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.english__card {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  padding: var(--space-5);
}

.english__card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: var(--space-4);
}

.english__stage {
  display: inline-block;
  border: 1px solid var(--primary);
  color: var(--primary);
  border-radius: 999px;
  padding: var(--space-2) var(--space-5);
  font-weight: 600;
}

.english__modules {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.english__modules li {
  font-size: 14px;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border);
  padding-bottom: var(--space-2);
}

/* ---------- responsive ---------- */
@media (max-width: 1100px) {
  .english__layout {
    grid-template-columns: 1fr;
  }

  .english__aside {
    position: static;
  }
}
</style>
