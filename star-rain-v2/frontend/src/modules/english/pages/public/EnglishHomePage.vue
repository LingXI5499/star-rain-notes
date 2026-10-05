<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import ThemeHero from '../../../site/components/ThemeHero.vue'
import { useViewMode } from '../../../../shared/viewMode'
import { useAuthStore } from '../../../account/stores/authStore'
import { errorMessage } from '../../../../shared/http'
import { getEnglishOverview } from '../../api/englishApi'
import { loadVocabularySummary } from '../../support/vocabularySummary'

/*
 * 英语首页 —— 按 V1 `views/english/EnglishView.vue` 的信息架构逐块对齐：
 *
 *   整幅主题主视觉（左文案 / 右插画，插画自带 CURRENT STAGE 卡）
 *   → 四个方向胶囊（阅读 / 听力 / 写作 / 持续成长）
 *   → A1–C2 长期能力轨道
 *   → 游客提示
 *   → 为什么学英语（含四格学习统计）
 *   → 五条学习方向
 *   → 词汇记忆入口
 *   → 长期学习路线
 *
 * 上一版把主视觉做成了「左文案 + 右一张带边框的图」，还缺了方向胶囊、
 * 当前阶段卡与 CEFR 轨道 —— 与 V1 差得最明显的就在这里，因此本轮按 V1 重排。
 */
const { contentPath } = useViewMode()
const auth = useAuthStore()

const overview = ref(null)
const summary = ref({ completed: 0, inProgress: 0, dueForReview: 0, total: 0 })
const loading = ref(true)
const error = ref('')

const levels = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']
const stageLabels = {
  FOUNDATION: '基础阶段',
  WORD_MEMORY: '词汇记忆',
  READING: '阅读训练',
  ANALYTICS: '高阶分析',
}

/* 主视觉下方的四个方向胶囊，与 V1 同名同序 */
const heroTraits = [
  { to: '/english/reading', zh: '阅读', en: 'Reading' },
  { to: '/english/listening', zh: '听力', en: 'Listening' },
  { to: '/english/writing', zh: '写作', en: 'Writing' },
  { to: '/english/progress', zh: '持续成长', en: 'A Better Me' },
]

const directions = [
  { glyph: '词', name: '单词', en: 'VOCABULARY', to: '/english/vocabulary', description: '主题词库、发音与固定间隔复习，建立可长期维护的词汇网络。', cta: '进入词库' },
  { glyph: '语', name: '语法', en: 'GRAMMAR', to: '/english/grammar', description: '沿章节和课程目录，从词法走向复杂句法与真实表达。', cta: '开始课程' },
  { glyph: '读', name: '阅读', en: 'READING', to: '/english/reading', description: '通过分级材料训练信息提取、结构理解和语言观察。', cta: '开始阅读' },
  { glyph: '听', name: '听力', en: 'LISTENING', to: '/english/listening', description: '围绕完整音频、时间片段、练习与语音规则进行精听。', cta: '开始训练' },
  { glyph: '写', name: '写作', en: 'WRITING', to: '/english/writing', description: '从素材、范文和任务中练习结构清晰、意思准确的表达。', cta: '开始写作' },
]

const stageText = () => {
  const stage = overview.value?.currentStage
  if (!stage) return '读取中'
  return stageLabels[stage] || stage
}

onMounted(async () => {
  try {
    /*
     * 未登录时不打学习汇总接口（账号树接口对匿名返回 401）：
     * 直接跳过，四格统计保持 0，等账号树里登录后再进来就会有数。
     */
    const [content, learning] = await Promise.all([
      getEnglishOverview(),
      auth.currentUser ? loadVocabularySummary() : Promise.resolve(null),
    ])
    overview.value = content
    if (learning) summary.value = learning
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="english-home">
    <header class="english-hero">
      <ThemeHero src="/brand/themes/english-hero.webp" alt="英语模块世界观视觉" />
      <div class="english-hero__copy">
        <p class="public-eyebrow">STAR RAIN NOTES · ENGLISH</p>
        <h1>{{ overview?.title || '英语能力成长路径' }}</h1>
        <p class="english-hero__lead">{{ overview?.subtitle || '从可理解输入到清晰表达，按 CEFR 建立阅读、听力与写作的长期学习闭环。' }}</p>
        <div class="english-hero__actions">
          <RouterLink class="is-primary" :to="contentPath('/english/vocabulary/study')">今日背单词 <span aria-hidden="true">→</span></RouterLink>
          <RouterLink :to="contentPath('/english/vocabulary')">浏览词库 <span aria-hidden="true">↗</span></RouterLink>
        </div>
        <p class="english-hero__quote">Knowledge leaves a trail.</p>
        <ul class="english-hero__traits" aria-label="英语学习方向">
          <li v-for="trait in heroTraits" :key="trait.to">
            <RouterLink :to="contentPath(trait.to)"><strong>{{ trait.zh }}</strong><span>{{ trait.en }}</span></RouterLink>
          </li>
        </ul>
      </div>
      <aside class="english-hero__stage" aria-label="当前阶段">
        <small>CURRENT STAGE</small>
        <strong>{{ stageText() }}</strong>
      </aside>
    </header>

    <div class="cefr-path" aria-label="CEFR 长期能力路径">
      <div><span v-for="level in levels" :key="level">{{ level }}</span></div>
      <p>长期能力路径 · 不标记未经可靠映射的当前等级</p>
    </div>

    <p class="english-guest-hint">
      {{ auth.currentUser ? `当前登录：${auth.currentUser.displayName || auth.currentUser.username}，学习进度会同步到账户。` : '当前为游客，英语进度仅保存在此浏览器。' }}
    </p>

    <p v-if="loading" class="english-state">正在整理英语学习路径…</p>
    <p v-else-if="error" class="english-state" role="alert">{{ error }}</p>

    <template v-else>
      <section class="english-overview">
        <div>
          <p class="public-eyebrow">WHY ENGLISH</p>
          <h2 class="public-section-title">为什么学英语</h2>
          <p>{{ overview?.introduction || '技术世界以英语为主，长期投资英语是职业与认知的复利。' }}</p>
        </div>
        <dl>
          <div><dt>已完成</dt><dd>{{ summary.completed }}</dd></div>
          <div><dt>学习中</dt><dd>{{ summary.inProgress }}</dd></div>
          <div><dt>待复习</dt><dd>{{ summary.dueForReview }}</dd></div>
          <div><dt>累计记录</dt><dd>{{ summary.total }}</dd></div>
        </dl>
      </section>

      <section class="english-section">
        <header>
          <div><p class="public-eyebrow">FIVE DIRECTIONS</p><h2 class="public-section-title">五条学习方向</h2></div>
          <RouterLink :to="contentPath('/english/progress')">查看学习洞察 <span aria-hidden="true">→</span></RouterLink>
        </header>
        <div class="direction-grid">
          <RouterLink v-for="direction in directions" :key="direction.name" :to="contentPath(direction.to)" class="direction-card public-interactive">
            <div><span>{{ direction.glyph }}</span><small>{{ direction.en }}</small></div>
            <h3>{{ direction.name }}</h3>
            <p>{{ direction.description }}</p>
            <strong>{{ direction.cta }} →</strong>
          </RouterLink>
        </div>
      </section>

      <section class="vocab-gateway">
        <div>
          <p class="public-eyebrow">VOCABULARY MEMORY</p>
          <h2>把单词放进可以持续复习的系统</h2>
          <p>按主题浏览词汇，通过英译中、中译英与随机混合完成固定间隔复习。</p>
          <div>
            <RouterLink :to="contentPath('/english/vocabulary')">浏览主题</RouterLink>
            <RouterLink :to="contentPath('/english/vocabulary/study')">开始今日学习 →</RouterLink>
          </div>
        </div>
        <span aria-hidden="true">Aa</span>
      </section>

      <section v-if="overview?.roadmapMarkdown" class="english-section english-roadmap">
        <header><div><p class="public-eyebrow">ROADMAP</p><h2 class="public-section-title">长期学习路线</h2></div></header>
        <BlogProse :markdown="overview.roadmapMarkdown" />
      </section>
    </template>
  </div>
</template>

<style scoped>
.english-home { padding-bottom: var(--space-12); }

/* ---------------- 主视觉 ---------------- */
.english-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: clamp(28px, 5vw, 64px);
  align-items: end;
  min-height: clamp(460px, 68vh, 640px);
  margin: 0 -8px 8px;
  padding: 48px 8px 42px;
  overflow: hidden;
  border-bottom: 1px solid var(--border);
}

.english-hero__copy { position: relative; z-index: 2; max-width: 520px; padding-bottom: 8px; }
.english-hero h1 { margin: 14px 0; font-size: clamp(42px, 6.4vw, 78px); line-height: 1.05; letter-spacing: -0.05em; text-wrap: balance; }
.english-hero__lead { max-width: 440px; color: var(--text-secondary); font-size: 15px; line-height: 1.85; }
.english-hero__actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 26px; }

.english-hero__actions a {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-width: 148px;
  padding: 12px 15px;
  border: 1px solid var(--border-strong);
  border-radius: 11px;
  color: var(--text-primary);
  background: color-mix(in srgb, var(--bg-surface) 88%, transparent);
  backdrop-filter: blur(10px);
  font-size: 12px;
  font-weight: 700;
}

.english-hero__actions a.is-primary { border-color: var(--primary); color: var(--on-primary); background: var(--primary); }
.english-hero__actions a:hover { transform: translateY(-2px); }
.english-hero__quote { margin: 18px 0 0; color: var(--text-muted); font: italic 13px/1.5 Georgia, serif; }

.english-hero__traits { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin: 28px 0 0; padding: 0; list-style: none; }

.english-hero__traits a {
  display: grid;
  gap: 4px;
  padding: 12px 10px;
  border: 1px solid color-mix(in srgb, var(--border) 80%, transparent);
  border-radius: 12px;
  color: var(--text-primary);
  background: color-mix(in srgb, var(--bg-surface) 72%, transparent);
  backdrop-filter: blur(8px);
}

.english-hero__traits strong { font-size: 13px; }
.english-hero__traits span { color: var(--text-muted); font: 700 9px/1 var(--font-mono); letter-spacing: 0.08em; }
.english-hero__traits a:hover { border-color: var(--primary); }

/*
 * 当前阶段卡是**网格的第二个流内子项**（第 1 列放文案、第 2 列放它），
 * 靠 align-items:end + justify-self:end 落到右下角。
 * 上一版把它写成 position:absolute，视觉上差不多，但一旦 hero 变窄就会压到插画上。
 */
.english-hero__stage {
  position: relative;
  z-index: 2;
  justify-self: end;
  align-self: end;
  display: grid;
  gap: 4px;
  min-width: 168px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: color-mix(in srgb, var(--bg-surface) 90%, transparent);
  backdrop-filter: blur(12px);
  box-shadow: var(--shadow-md);
}

.english-hero__stage small { color: var(--accent); font: 700 8px var(--font-mono); letter-spacing: 0.13em; }
.english-hero__stage strong { color: var(--text-primary); font-size: 15px; }

/* ---------------- CEFR 轨道 ---------------- */
.cefr-path { padding: 26px 0 8px; }
.cefr-path > div { position: relative; display: grid; grid-template-columns: repeat(6, 1fr); align-items: center; }
.cefr-path > div::before { position: absolute; left: 0; right: 0; height: 1px; background: var(--border-strong); content: ''; }

.cefr-path span {
  position: relative;
  z-index: 1;
  justify-self: center;
  padding: 6px 12px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-page);
  font: 650 11px var(--font-mono);
}

.cefr-path p { margin: 12px 0 0; color: var(--text-muted); font-size: 11px; text-align: center; }
.english-guest-hint { margin: 0; padding: 12px 0; border-top: 1px solid var(--border); color: var(--text-muted); font-size: 12px; }
.english-state { padding: 34px 0; color: var(--text-muted); }

/* ---------------- 为什么学英语 ---------------- */
.english-overview {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 1fr);
  gap: clamp(30px, 6vw, 80px);
  align-items: start;
  padding: 58px 0;
  border-bottom: 1px solid var(--border);
}

.english-overview > div > p:last-child { max-width: 640px; margin-top: 14px; color: var(--text-secondary); line-height: 1.9; }
.english-overview dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1px; margin: 0; overflow: hidden; border: 1px solid var(--border); border-radius: var(--radius-card); background: var(--border); }
.english-overview dl > div { display: grid; gap: 6px; padding: 22px; background: var(--bg-surface); }
.english-overview dt { color: var(--text-muted); font-size: 12px; }
.english-overview dd { margin: 0; color: var(--primary); font-size: 30px; font-weight: 700; line-height: 1; }

/* ---------------- 通用区块 ---------------- */
.english-section { padding: 58px 0; border-bottom: 1px solid var(--border); }
.english-section > header { display: flex; align-items: end; justify-content: space-between; gap: 20px; margin-bottom: 30px; }
.english-section > header h2 { margin-top: 8px; }
.english-section > header > a { color: var(--text-secondary); font-size: 12px; }
.english-section > header > a:hover { color: var(--primary); }

/* ---------------- 五条学习方向 ---------------- */
.direction-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; }

.direction-card {
  display: flex;
  min-height: 250px;
  flex-direction: column;
  padding: 22px;
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  color: var(--text-primary);
  background: var(--bg-surface);
}

.direction-card:hover { transform: translateY(-3px); border-color: var(--primary); box-shadow: var(--shadow-sm); }
.direction-card > div { display: flex; align-items: center; justify-content: space-between; gap: 10px; }

.direction-card > div span {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: var(--primary);
  background: var(--primary-soft);
  font-size: 20px;
  font-weight: 750;
}

.direction-card small { color: var(--accent); font: 700 9px var(--font-mono); letter-spacing: 0.12em; }
.direction-card h3 { margin: 20px 0 10px; font-size: 21px; }
.direction-card p { color: var(--text-secondary); font-size: 13px; line-height: 1.78; }
.direction-card strong { margin-top: auto; padding-top: 18px; color: var(--primary); font-size: 12px; }

/* ---------------- 词汇记忆入口 ---------------- */
.vocab-gateway {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  margin: 58px 0;
  padding: clamp(28px, 4vw, 48px);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background:
    radial-gradient(60% 120% at 92% 10%, color-mix(in srgb, var(--accent) 14%, transparent), transparent 70%),
    var(--bg-surface);
}

.vocab-gateway h2 { margin: 10px 0 12px; font-size: clamp(24px, 3vw, 36px); line-height: 1.24; }
.vocab-gateway p { max-width: 620px; color: var(--text-secondary); line-height: 1.85; }
.vocab-gateway > div > div { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 22px; }
.vocab-gateway a { padding: 10px 15px; border: 1px solid var(--border-strong); border-radius: 10px; color: var(--text-primary); font-size: 13px; font-weight: 700; }
.vocab-gateway a:last-child { border-color: var(--primary); color: var(--on-primary); background: var(--primary); }
.vocab-gateway > span { color: color-mix(in srgb, var(--primary) 26%, transparent); font: 700 clamp(60px, 9vw, 120px)/1 Georgia, serif; }

/* ---------------- 长期路线 ---------------- */
.english-roadmap :deep(.markdown-body) { max-width: 820px; }

@media (max-width: 1100px) {
  .direction-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 900px) {
  .english-hero { grid-template-columns: 1fr; min-height: 0; padding-top: 24px; align-items: start; }
  .english-hero__stage { justify-self: start; margin-top: 24px; }
  .english-hero__traits { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .english-overview { grid-template-columns: 1fr; gap: 26px; }
}

@media (max-width: 640px) {
  .english-hero h1 { font-size: 42px; }
  .english-hero__traits { grid-template-columns: 1fr 1fr; }
  .direction-grid { grid-template-columns: 1fr; }
  .direction-card { min-height: 0; }
  .vocab-gateway { flex-direction: column; align-items: flex-start; }
  .vocab-gateway > span { display: none; }
  .cefr-path > div { grid-template-columns: repeat(3, 1fr); gap: 10px; }
  .cefr-path > div::before { display: none; }
}
</style>
