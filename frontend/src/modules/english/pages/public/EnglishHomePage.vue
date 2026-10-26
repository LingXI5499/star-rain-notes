<script setup>
/**
 * EnglishHomePage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../blog/components/BlogProse.vue、../../../site/components/ThemeHero.vue、../../../../shared/viewMode、../../../account/stores/authStore。
 * 主要响应式状态：auth、overview、summary、loading、error。
 * 阅读入口：stageText（stage文本的处理函数）。
 */


import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BlogProse from '../../../blog/components/BlogProse.vue'
import ThemeHero from '../../../site/components/ThemeHero.vue'
import { useViewMode } from '../../../../shared/viewMode'
import { useAuthStore } from '../../../account/stores/authStore'
import { errorMessage } from '../../../../shared/http'
import { getEnglishOverview } from '../../api/englishApi'
import { loadVocabularySummary } from '../../support/vocabularySummary'

/* 英语首页保留单词、语法、真实阅读与自主创作四个方向。 */
const { contentPath } = useViewMode()
/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()

/**
 * overview；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const overview = ref(null)
/**
 * 摘要；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const summary = ref({ completed: 0, inProgress: 0, dueForReview: 0, total: 0 })
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')

const stageLabels = {
  FOUNDATION: '基础阶段',
  WORD_MEMORY: '词汇记忆',
  READING: '阅读训练',
  ANALYTICS: '高阶分析',
}

/* 主视觉下方的四个核心方向入口 */
const heroTraits = [
  { to: '/english/vocabulary', zh: '单词', en: 'Vocabulary' },
  { to: '/english/grammar', zh: '语法', en: 'Grammar' },
  { to: '/english/reading', zh: '阅读', en: 'Reading' },
  { to: '/english/writing', zh: '写作', en: 'Writing' },
]

const directions = [
  { glyph: '词', name: '单词', en: 'VOCABULARY', to: '/english/vocabulary', description: '主题词库、发音与固定间隔复习，建立可长期维护的词汇网络。', cta: '进入词库' },
  { glyph: '语', name: '语法', en: 'GRAMMAR', to: '/english/grammar', description: '沿章节和课程目录，从词法走向复杂句法与真实表达。', cta: '开始课程' },
  { glyph: '读', name: '阅读', en: 'READING', to: '/english/reading', description: '通过真实文章阅读，练习信息提取、结构理解和语言观察。', cta: '开始阅读' },
  { glyph: '写', name: '写作', en: 'WRITING', to: '/english/writing', description: '通过自主英文创作，记录思考，练习结构清晰、意思准确的表达。', cta: '开始写作' },
]

/**
 * stage文本的处理函数。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const stageText = () => {
  const stage = overview.value?.currentStage
  if (!stage) return '读取中'
  return stageLabels[stage] || stage
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(async () => {
  try {
    // 导航统一使用规范路径；账号能力仍由登录状态和权限决定。
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
        <p class="english-hero__lead">{{ overview?.subtitle || '从单词和语法打好基础，通过阅读积累输入，再用写作练习清晰表达。' }}</p>
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
          <div><p class="public-eyebrow">FOUR DIRECTIONS</p><h2 class="public-section-title">四条学习方向</h2></div>
          <RouterLink :to="contentPath('/english/progress')">查看学习洞察 <span aria-hidden="true">→</span></RouterLink>
        </header>
        <div class="direction-grid">
          <RouterLink v-for="direction in directions" :key="direction.name" :to="contentPath(direction.to)" class="direction-card public-interactive" :data-kind="direction.en.toLowerCase()">
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.english-home。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.english-hero__copy。
 */
.english-hero__copy { position: relative; z-index: 2; max-width: 520px; padding-bottom: 8px; }
/**
 * 样式范围：.english-hero h1。
 */
.english-hero h1 { margin: 14px 0; font-size: clamp(42px, 6.4vw, 78px); line-height: 1.05; letter-spacing: -0.05em; text-wrap: balance; }
/**
 * 样式范围：.english-hero__lead。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-hero__lead { max-width: 440px; color: var(--text-secondary); font-size: 15px; line-height: 1.85; }
/**
 * 样式范围：.english-hero__actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.english-hero__actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 26px; }

/**
 * 样式范围：.english-hero__actions a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.english-hero__actions a.is-primary。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-hero__actions a.is-primary { border-color: var(--primary); color: var(--on-primary); background: var(--primary); }
/**
 * 样式范围：.english-hero__actions a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.english-hero__actions a:hover { transform: translateY(-2px); }
/**
 * 样式范围：.english-hero__quote。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-hero__quote { margin: 18px 0 0; color: var(--text-muted); font: italic 13px/1.5 Georgia, serif; }

/**
 * 样式范围：.english-hero__traits。
 * 使用网格布局组织内容；列定义为 repeat(4, minmax(0, 1fr))，间距由 10px 控制。
 */
.english-hero__traits { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin: 28px 0 0; padding: 0; list-style: none; }

/**
 * 样式范围：.english-hero__traits a。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 4px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.english-hero__traits strong。
 */
.english-hero__traits strong { font-size: 13px; }
/**
 * 样式范围：.english-hero__traits span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-hero__traits span { color: var(--text-muted); font: 700 9px/1 var(--font-mono); letter-spacing: 0.08em; }
/**
 * 样式范围：.english-hero__traits a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.english-hero__stage small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-hero__stage small { color: var(--accent); font: 700 8px var(--font-mono); letter-spacing: 0.13em; }
/**
 * 样式范围：.english-hero__stage strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-hero__stage strong { color: var(--text-primary); font-size: 15px; }

/**
 * 样式范围：.english-guest-hint。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-guest-hint { margin: 0; padding: 12px 0; border-top: 1px solid var(--border); color: var(--text-muted); font-size: 12px; }
/**
 * 样式范围：.english-state。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.english-overview > div > p:last-child。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-overview > div > p:last-child { max-width: 640px; margin-top: 14px; color: var(--text-secondary); line-height: 1.9; }
/**
 * 样式范围：.english-overview dl。
 * 使用网格布局组织内容；列定义为 repeat(2, minmax(0, 1fr))，间距由 1px 控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-overview dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1px; margin: 0; overflow: hidden; border: 1px solid var(--border); border-radius: var(--radius-card); background: var(--border); }
/**
 * 样式范围：.english-overview dl > div。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-overview dl > div { display: grid; gap: 6px; padding: 22px; background: var(--bg-surface); }
/**
 * 样式范围：.english-overview dt。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-overview dt { color: var(--text-muted); font-size: 12px; }
/**
 * 样式范围：.english-overview dd。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-overview dd { margin: 0; color: var(--primary); font-size: 30px; font-weight: 700; line-height: 1; }

/* ---------------- 通用区块 ---------------- */
.english-section { padding: 58px 0; border-bottom: 1px solid var(--border); }
/**
 * 样式范围：.english-section > header。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.english-section > header { display: flex; align-items: end; justify-content: space-between; gap: 20px; margin-bottom: 30px; }
/**
 * 样式范围：.english-section > header h2。
 */
.english-section > header h2 { margin-top: 8px; }
/**
 * 样式范围：.english-section > header > a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-section > header > a { color: var(--text-secondary); font-size: 12px; }
/**
 * 样式范围：.english-section > header > a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-section > header > a:hover { color: var(--primary); }

/* ---------------- 四条学习方向 ---------------- */
.direction-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }

/**
 * 样式范围：.direction-card。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.direction-card {
  position: relative;
  display: flex;
  min-height: 270px;
  flex-direction: column;
  overflow: hidden;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 18px;
  color: var(--text-primary);
  background: var(--bg-surface);
}

/**
 * 样式范围：.direction-card::after。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.direction-card::after { position: absolute; right: -48px; bottom: -60px; width: 130px; height: 130px; border: 1px solid color-mix(in srgb, var(--card-accent, var(--primary)) 25%, transparent); border-radius: 50%; content: ''; }
/**
 * 样式范围：.direction-card[data-kind='reading'], .direction-card[data-kind='writing']。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.direction-card[data-kind='reading'], .direction-card[data-kind='writing'] { --card-accent: var(--accent); }
/**
 * 样式范围：.direction-card:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.direction-card:hover { transform: translateY(-3px); border-color: var(--primary); box-shadow: var(--shadow-sm); }
/**
 * 样式范围：.direction-card > div。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.direction-card > div { display: flex; align-items: center; justify-content: space-between; gap: 10px; }

/**
 * 样式范围：.direction-card > div span。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.direction-card > div span {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 14px;
  color: var(--card-accent, var(--primary));
  background: color-mix(in srgb, var(--card-accent, var(--primary)) 12%, transparent);
  font-size: 22px;
  font-weight: 800;
}

/**
 * 样式范围：.direction-card small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.direction-card small { color: var(--accent); font: 700 9px var(--font-mono); letter-spacing: 0.12em; }
/**
 * 样式范围：.direction-card h3。
 */
.direction-card h3 { margin: 30px 0 8px; font-size: 22px; }
/**
 * 样式范围：.direction-card p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.direction-card p { color: var(--text-secondary); font-size: 13px; line-height: 1.78; }
/**
 * 样式范围：.direction-card strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.vocab-gateway h2。
 */
.vocab-gateway h2 { margin: 10px 0 12px; font-size: clamp(24px, 3vw, 36px); line-height: 1.24; }
/**
 * 样式范围：.vocab-gateway p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocab-gateway p { max-width: 620px; color: var(--text-secondary); line-height: 1.85; }
/**
 * 样式范围：.vocab-gateway > div > div。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.vocab-gateway > div > div { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 22px; }
/**
 * 样式范围：.vocab-gateway a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocab-gateway a { padding: 10px 15px; border: 1px solid var(--border-strong); border-radius: 10px; color: var(--text-primary); font-size: 13px; font-weight: 700; }
/**
 * 样式范围：.vocab-gateway a:last-child。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocab-gateway a:last-child { border-color: var(--primary); color: var(--on-primary); background: var(--primary); }
/**
 * 样式范围：.vocab-gateway > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocab-gateway > span { color: color-mix(in srgb, var(--primary) 26%, transparent); font: 700 clamp(60px, 9vw, 120px)/1 Georgia, serif; }

/* ---------------- 长期路线 ---------------- */
.english-roadmap :deep(.markdown-body) { max-width: 820px; }

/**
 * 响应式或设备条件：(max-width: 1100px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 1100px) {
  /**
   * 样式范围：.direction-grid。
   */
  .direction-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

/**
 * 响应式或设备条件：(max-width: 900px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 900px) {
  /**
   * 样式范围：.english-hero。
   */
  .english-hero { grid-template-columns: 1fr; min-height: 0; padding-top: 24px; align-items: start; }
  /**
   * 样式范围：.english-hero__stage。
   */
  .english-hero__stage { justify-self: start; margin-top: 24px; }
  /**
   * 样式范围：.english-hero__traits。
   */
  .english-hero__traits { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  /**
   * 样式范围：.english-overview。
   */
  .english-overview { grid-template-columns: 1fr; gap: 26px; }
}

/**
 * 响应式或设备条件：(max-width: 640px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 640px) {
  /**
   * 样式范围：.english-hero h1。
   */
  .english-hero h1 { font-size: 42px; }
  /**
   * 样式范围：.english-hero__traits。
   */
  .english-hero__traits { grid-template-columns: 1fr 1fr; }
  /**
   * 样式范围：.direction-grid。
   */
  .direction-grid { grid-template-columns: 1fr; }
  /**
   * 样式范围：.direction-card。
   */
  .direction-card { min-height: 0; }
  /**
   * 样式范围：.vocab-gateway。
   */
  .vocab-gateway { flex-direction: column; align-items: flex-start; }
  /**
   * 样式范围：.vocab-gateway > span。
   */
  .vocab-gateway > span { display: none; }
}
</style>
