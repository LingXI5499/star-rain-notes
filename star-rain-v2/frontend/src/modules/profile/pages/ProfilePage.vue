<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BlogProse from '../../blog/components/BlogProse.vue'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import ThemeHero from '../../site/components/ThemeHero.vue'
import { getPublicProfile } from '../api/profileApi'
import CareerSnapshot from '../components/CareerSnapshot.vue'
import EvidenceGrid from '../components/EvidenceGrid.vue'
import TechFlow from '../components/TechFlow.vue'

/*
 * 公开「关于」页 —— 逐块对齐 V1 `views/about/AboutView.vue` 的信息架构：
 *
 *   主视觉（主题插画 + 圆形羽化头像 + 眉标/姓名/headline/bio 摘要/标语/操作按钮）
 *   → CURRENT / DIRECTION / FEATURED / STATUS 四格速览
 *   → 此刻关注（CURRENT FOCUS 标签流）
 *   → sticky 章节胶囊导航（01…07）
 *   → 代表作品（首项跨栏大卡）
 *   → 工程证据（SKILL = EVIDENCE）
 *   → 双栏阅读区（左 ON THIS PAGE 锚点 + 右 技术方向 / 学习与实践 / 知识内容）
 *   → 个人说明（PERSONAL NOTE）
 *   → 保持联系（LET'S KEEP IN TOUCH）
 *   → 编辑原则引言块（EDITORIAL PRINCIPLE）
 *
 * 与 V1 的差别只有数据来源，V1 一次 `fetchPublicAbout()` 拿到的是一份**为关于页定制的聚合**
 * （selectedProjects / selectedTutorials / selectedBlogs / technicalDirectionMarkdown /
 * journeyMarkdown / currentFocus / publicEmail / githubUrl / resumeUrl）；
 * V2 的 `/public/profile` 只给「作者档案」本身：
 * displayName / headline / bioMarkdown / avatarUrl / locationText / resumeUrl /
 * experiences / skills / socialLinks / featuredContents。
 * V2 没有的字段一律不编造，降级方式见下面各处中文注释。
 *
 * 链接一律走 `contentPath`：账号树（/useradmin）下同一页要落在账号树前缀里。
 */
const { contentPath } = useViewMode()
const profile = ref(null)
const loading = ref(true)
const error = ref('')

/* V1 用的是 hero 里的「人像插画」，固定资源；V2 作者上传了头像就用它，否则退回同一张品牌图 */

/* 眉标与说明文案 —— 与 V1 逐字一致，按章节 id 取用 */
const eyebrows = {
  direction: 'TECHNICAL MAP',
  journey: 'JOURNEY',
  knowledge: 'SELECTED KNOWLEDGE',
}
const sectionNotes = {
  work: '用真实项目说明我如何理解问题、组织工程并持续复盘。',
  evidence: '不罗列“精通”，只把已经完成并能够查看的内容放在这里。',
  direction: '不是技能清单，而是正在建立的能力结构。',
  journey: '在理解、动手和复盘之间缓慢积累。',
  knowledge: '系统教程与阶段性思考，共同构成可以回看的学习坐标。',
}

/* ---------- 阅读区：把 bioMarkdown 切回 V1 的三个章节 ---------- */

/*
 * 从长文里切出「`## 标题` 到 `## 结束标题`」之间的一段。
 *
 * 为什么用「结束标题」而不是「下一个 h2」：V2 的 bioMarkdown 里所有层级标记都是 `##`
 * （见 ProfileServiceImpl 里作者档案的成文方式），而 `## 技术方向`、`## 学习与实践`
 * 这两节本身只有一行标题、正文全在紧随其后的同级别小标题下 —— 例如
 * 「## 技术方向 / ## Java 全栈开发 / ## AI 大模型与 Agent 智能体」。
 * 若按「遇到下一个 h2 就停」来切，这两节都会切出空串，整块内容凭空消失
 * （实测：`## 技术方向` 的下一行就是 `## Java 全栈开发`）。
 * 因此边界由调用方按文档结构显式给出，`endTitle` 省略时切到文末。
 */
function sectionBetween(markdown, title, endTitle) {
  const lines = String(markdown || '').split(/\r?\n/)
  const start = lines.findIndex((line) => new RegExp(`^##\\s+${title}\\s*$`).test(line))
  if (start < 0) return ''
  const end = endTitle
    ? lines.findIndex((line, index) => index > start && new RegExp(`^##\\s+${endTitle}\\s*$`).test(line))
    : -1
  return lines.slice(start + 1, end < 0 ? lines.length : end).join('\n').trim()
}

/* 切出 `## 标题` 之前的部分，作为个人说明（V1 用完整 bio，这里同样给完整叙述） */
function sectionBefore(markdown, title) {
  const source = String(markdown || '')
  const found = new RegExp(`^##\\s+${title}\\s*$`, 'm').exec(source)
  return (found && found.index > 0 ? source.slice(0, found.index) : source).trim()
}

/*
 * 把这一段里恰好两个 # 的小标题降成 ###。
 * 段落本身已经渲染在页面的「## 技术方向」章节标题之下（V1 同构），
 * 若原样保留同级的 h2，视觉上会和章节标题平级、把层级读平。
 */
function nestHeadings(markdown) {
  return String(markdown || '').replace(/^##(?!#)/gm, '###')
}

/*
 * V2 的 bioMarkdown 是一份「自我介绍 + 技术方向 + 学习与实践」的长文
 * （后端把作者档案里的几段合在一个字段里，见 ProfileServiceImpl）。
 * V1 把这几块拆成独立字段分别渲染在「技术方向 / 学习与实践 / 个人说明」，
 * 这里按同样的章节标题把长文切回三块，视觉与信息架构才与 V1 一致。
 * 「技术方向」到「学习与实践」之前的全部小标题都归属技术方向那一节。
 */
const reading = computed(() => {
  const markdown = profile.value?.bioMarkdown || ''
  return {
    direction: nestHeadings(sectionBetween(markdown, '技术方向', '学习与实践')),
    journey: nestHeadings(sectionBetween(markdown, '学习与实践')),
    story: sectionBefore(markdown, '技术方向'),
  }
})

/* ---------- hero ---------- */

/*
 * bio 摘要：V1 取 bio 前 150 字（去换行、超长加省略号）。
 * V2 的 bioMarkdown 里含 `## …` 小标题，直接截断会把「## 技术方向」这类标记
 * 一起截进摘要，因此先只取第一个小标题之前的自然段，再按 150 字截断。
 */
const bioExcerpt = computed(() => {
  const firstSection = (profile.value?.bioMarkdown || '').split(/\n(?=##\s)/)[0] || ''
  const text = firstSection
    .replace(/^#+\s+/gm, '')
    .replace(/[*_`>]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length > 150 ? `${text.slice(0, 150).trim()}…` : text
})

/* 此刻关注 / DIRECTION 格：skills 里 category = DIRECTION 的名称（V1 是 currentFocus） */
const focusItems = computed(() => (profile.value?.skills || [])
  .filter((item) => item.category === 'DIRECTION')
  .sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0))
  .map((item) => item.name)
  .filter(Boolean))

/* 代表作品：V1 是 selectedProjects，V2 用 featuredContents（后端已按 sortOrder 排序） */
const featuredWork = computed(() => (profile.value?.featuredContents || []).filter((item) => item.available !== false))

/*
 * 知识内容：V2 没有单独的「精选教程 / 精选博客」字段，只能从 featuredContents 里按类型挑。
 * 教程归「系统教程」，博客归「思考记录」；PORTFOLIO 不重复列在这里（它在代表作品里）。
 * 两个类型都没有时整块不渲染，也不留空标题。
 */
const knowledgeColumns = computed(() => {
  const items = featuredWork.value
  return {
    tutorials: items.filter((item) => item.contentType === 'TUTORIAL'),
    posts: items.filter((item) => item.contentType === 'BLOG'),
  }
})

/*
 * 工程证据：V1 把「作品 + 教程 + 博客」各取 2 项拼成证据墙。
 * V2 只有 featuredContents 一类来源，于是按内容类型分组后各组最多取 2 项，
 * kind / note 文案沿用 V1 的措辞。
 */
const evidence = computed(() => {
  const items = featuredWork.value
  const notes = {
    PORTFOLIO: { kind: '工程实践', note: '真实项目与工程复盘' },
    TUTORIAL: { kind: '知识组织', note: '结构化知识与学习路径' },
    BLOG: { kind: '技术写作', note: '实践记录、判断与反思' },
  }
  return ['PORTFOLIO', 'TUTORIAL', 'BLOG'].flatMap((type) => items
    .filter((item) => item.contentType === type)
    .slice(0, 2)
    .map((item) => ({
      title: item.title,
      to: contentPath(item.url),
      note: notes[type].note,
      kind: notes[type].kind,
    })))
})

/* 四格速览：CURRENT 取 headline，DIRECTION 取前三个关注点，FEATURED 取首个代表作品 */
const career = computed(() => ({
  headline: profile.value?.headline || null,
  focus: focusItems.value.slice(0, 3),
  featured: featuredWork.value[0]?.title || null,
  status: '持续学习与构建中',
}))

/* ---------- 联系 ---------- */

/* 邮箱优先取作者自己填的 publicEmail（ProfileVO 目前没有这个字段，取到才会显示） */
const emailAddress = computed(() => profile.value?.publicEmail || '')
const isMailto = (url) => String(url || '').startsWith('mailto:')
function mailAddress(url) {
  return String(url || '').replace(/^mailto:/, '').split('?')[0]
}

const heroLinks = computed(() => (profile.value?.socialLinks || []).filter((link) => (
  link.platformCode === 'GITHUB' || link.platformCode === 'EMAIL_PAGE'
)))

/* hero 按钮文案：V1 是固定的「GitHub / 写邮件」，这里按平台代码取，标签缺失也有兜底 */
const heroLinkLabels = { GITHUB: 'GitHub', EMAIL_PAGE: '写邮件' }
function heroLinkLabel(link) {
  return heroLinkLabels[link.platformCode] || link.label || '主页'
}

/*
 * 保持联系三张卡（V1：邮箱 / 代码主页 / 个人简历）。
 * V2 的 socialLinks 里 EMAIL_PAGE 实际是 `mailto:`，不能当内容页链接走 contentPath，
 * 也不该在新标签页打开；GITHUB 等外链保持 target=_blank。
 * 简历只在作者上传了 media 资产（resumeUrl 非空）时才出现 —— 不编造按钮。
 */
const contactLinks = computed(() => {
  const cards = []
  if (emailAddress.value) {
    cards.push({ key: 'email', label: '邮箱', value: emailAddress.value, href: `mailto:${emailAddress.value}`, external: false, icon: '↗' })
  }
  for (const link of profile.value?.socialLinks || []) {
    if (link.platformCode === 'EMAIL_PAGE' && isMailto(link.url)) {
      cards.push({ key: link.id, label: '邮箱', value: mailAddress(link.url), href: link.url, external: false, icon: '↗' })
      continue
    }
    if (link.platformCode === 'GITHUB') {
      cards.push({ key: link.id, label: '代码主页', value: '查看公开项目', href: link.url, external: true, icon: '↗' })
      continue
    }
    if (!isMailto(link.url)) {
      cards.push({ key: link.id, label: link.label || '公开主页', value: link.url.replace(/^https?:\/\//, ''), href: link.url, external: true, icon: '↗' })
    }
  }
  return cards
})

/*
 * sticky 章节胶囊导航 —— 顺序照抄 V1：
 * 01 代表作品 → 02 工程证据 → 03 技术地图 → 04 学习经历 → 05 知识内容
 * → 06 个人说明 → 07 保持联系，只保留真正有内容的章节。
 * 「学习经历」同时承载 V2 的 experiences 列表与 bio 里的「学习与实践」段落，
 * 因此两块都有内容时也只有一个锚点（见模板里的 #journey）。
 */
const sections = computed(() => [
  { id: 'work', label: '代表作品', visible: !!featuredWork.value.length },
  { id: 'evidence', label: '工程证据', visible: !!evidence.value.length },
  { id: 'direction', label: '技术地图', visible: !!reading.value.direction },
  { id: 'journey', label: '学习与实践', visible: !!reading.value.journey },
  {
    id: 'knowledge',
    label: '知识内容',
    // V2 的 /public/profile 没有 selectedTutorials / selectedBlogs 这类字段，
    // 拿不到独立的教学/博客列表，因此知识内容整体降级为 featuredContents 里
    // contentType 为 TUTORIAL / BLOG 的条目（见 knowledgeColumns）。
    visible: !!knowledgeColumns.value.tutorials.length || !!knowledgeColumns.value.posts.length,
  },
  { id: 'story', label: '个人说明', visible: !!reading.value.story },
  { id: 'contact', label: '保持联系', visible: !!contactLinks.value.length },
].filter((item) => item.visible))

async function load() {
  loading.value = true
  error.value = ''
  try {
    profile.value = await getPublicProfile()
  } catch (cause) {
    // 404 是「作者还没公开档案」，与网络/服务异常分开说，错误不静默
    error.value = cause?.response?.status === 404 ? '作者资料暂未公开。' : errorMessage(cause)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="profile-page">
    <p v-if="loading" class="profile-page__state" role="status">正在读取作者资料…</p>
    <div v-else-if="error" class="profile-page__state" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="load">重新读取</button>
    </div>
    <p v-else-if="!profile" class="profile-page__state">暂时没有可展示的作者资料。</p>

    <template v-else>
      <!-- ============ hero：主题插画 + 羽化头像 + 身份 + 摘要 + 操作 ============ -->
      <header class="profile-page__hero">
        <ThemeHero src="/brand/themes/about-hero.webp" alt="关于页世界观视觉" />
        <div class="profile-page__identity">
          <div>
            <p class="public-eyebrow">ABOUT · STAR RAIN NOTES</p>
            <h1>{{ profile.displayName || '个人开发者' }}</h1>
            <h2>{{ profile.headline || '在持续学习中遇见更好的自己' }}</h2>
          </div>
          <p v-if="bioExcerpt" class="profile-page__excerpt">{{ bioExcerpt }}</p>
          <p class="profile-page__tagline">A wider world, a brighter you.</p>
          <p v-if="profile.locationText" class="profile-page__location">{{ profile.locationText }}</p>
          <div class="profile-page__hero-actions">
            <a
              v-for="link in heroLinks"
              :key="link.id"
              :href="link.url"
              :target="isMailto(link.url) ? undefined : '_blank'"
              :rel="isMailto(link.url) ? undefined : 'noopener noreferrer'"
            >{{ heroLinkLabel(link) }} <span aria-hidden="true">↗</span></a>
            <RouterLink :to="contentPath('/messages')">留言 <span aria-hidden="true">→</span></RouterLink>
          </div>
        </div>
      </header>

      <!-- ============ CURRENT / DIRECTION / FEATURED / STATUS ============ -->
      <section class="profile-page__career">
        <CareerSnapshot
          :headline="career.headline"
          :focus="career.focus"
          :featured="career.featured"
          :status="career.status"
        />
      </section>

      <!-- ============ 此刻关注（CURRENT FOCUS） ============ -->
      <section v-if="focusItems.length" class="profile-page__techflow">
        <div>
          <p class="public-eyebrow">CURRENT FOCUS</p>
          <h2>此刻关注</h2>
        </div>
        <TechFlow :items="focusItems" />
      </section>

      <!-- ============ sticky 章节胶囊导航（只列有内容的章节） ============ -->
      <nav v-if="sections.length" class="profile-page__nav" aria-label="关于页章节">
        <a v-for="(item, index) in sections" :key="item.id" :href="`#${item.id}`">
          <span>{{ String(index + 1).padStart(2, '0') }}</span>{{ item.label }}
        </a>
      </nav>

      <!-- ============ 代表作品：首项跨栏大卡，其余两列 ============ -->
      <section v-if="featuredWork.length" id="work" class="profile-page__section">
        <header>
          <div>
            <p class="public-eyebrow">SELECTED WORK</p>
            <h2 class="public-section-title">代表作品</h2>
          </div>
          <p>{{ sectionNotes.work }}</p>
        </header>
        <div class="profile-page__work">
          <RouterLink
            v-for="(item, index) in featuredWork"
            :key="item.id"
            :to="contentPath(item.url)"
            class="profile-page__work-card editorial-card public-interactive"
            :class="{ 'is-primary': index === 0 }"
          >
            <!-- V2 的 featuredContents 多数没有封面，用首字占位块补住视觉（V1 是 EditorialMotif） -->
            <div class="profile-page__work-visual">
              <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" :loading="index ? 'lazy' : 'eager'" />
              <span v-else aria-hidden="true">{{ (item.title || '作品').slice(0, 1) }}</span>
              <small>{{ { PORTFOLIO: 'PORTFOLIO', TUTORIAL: 'TUTORIAL', BLOG: 'JOURNAL' }[item.contentType] || 'WORK' }}</small>
            </div>
            <article>
              <small>CASE STUDY · {{ String(index + 1).padStart(2, '0') }}</small>
              <h3>{{ item.title }}</h3>
              <p v-if="item.summary" class="profile-page__work-summary">{{ item.summary }}</p>
              <span>查看项目复盘 →</span>
            </article>
          </RouterLink>
        </div>
      </section>

      <!-- ============ 工程证据（SKILL = EVIDENCE） ============ -->
      <section v-if="evidence.length" id="evidence" class="profile-page__section">
        <header>
          <div>
            <p class="public-eyebrow">SKILL = EVIDENCE</p>
            <h2 class="public-section-title">工程证据</h2>
          </div>
          <p>{{ sectionNotes.evidence }}</p>
        </header>
        <EvidenceGrid :items="evidence" />
      </section>

      <!-- ============ 双栏阅读区：左 ON THIS PAGE，右 技术方向 / 学习与实践 / 知识内容 ============ -->
      <div class="profile-page__reading">
        <aside>
          <p class="public-eyebrow">ON THIS PAGE</p>
          <a v-for="item in sections" :key="item.id" :href="`#${item.id}`">{{ item.label }}</a>
        </aside>
        <div>
          <!-- 技术方向：正文取自 bioMarkdown 里的「## 技术方向」段落 -->
          <section v-if="reading.direction" id="direction" class="profile-page__section profile-page__prose-section">
            <header>
              <div>
                <p class="public-eyebrow">{{ eyebrows.direction }}</p>
                <h2 class="public-section-title">技术方向</h2>
              </div>
              <p>{{ sectionNotes.direction }}</p>
            </header>
            <BlogProse :markdown="reading.direction" />
          </section>

          <!-- 学习经历：V1 的一段 prose；V2 另有 experiences 列表，两者共用 #journey 锚点 -->
          <section v-if="reading.journey" id="journey" class="profile-page__section">
            <header>
              <div>
                <p class="public-eyebrow">JOURNEY</p>
                <h2 class="public-section-title">学习与实践</h2>
              </div>
              <p>{{ sectionNotes.journey }}</p>
            </header>
            <!-- 正文取自 bioMarkdown 里的「## 学习与实践」段落 -->
            <BlogProse v-if="reading.journey" :markdown="reading.journey" class="profile-page__prose-section" />
          </section>

          <!-- 知识内容：教程 / 思考两列（来源见 knowledgeColumns 的降级说明） -->
          <section v-if="knowledgeColumns.tutorials.length || knowledgeColumns.posts.length" id="knowledge" class="profile-page__section">
            <header>
              <div>
                <p class="public-eyebrow">{{ eyebrows.knowledge }}</p>
                <h2 class="public-section-title">知识内容</h2>
              </div>
              <p>{{ sectionNotes.knowledge }}</p>
            </header>
            <div class="profile-page__knowledge">
              <div v-if="knowledgeColumns.tutorials.length">
                <h3>系统教程</h3>
                <RouterLink v-for="item in knowledgeColumns.tutorials" :key="item.id" :to="contentPath(item.url)">
                  <span>{{ item.title }}</span><i aria-hidden="true">→</i>
                </RouterLink>
              </div>
              <div v-if="knowledgeColumns.posts.length">
                <h3>思考记录</h3>
                <RouterLink v-for="item in knowledgeColumns.posts" :key="item.id" :to="contentPath(item.url)">
                  <span>{{ item.title }}</span><i aria-hidden="true">→</i>
                </RouterLink>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- ============ 个人说明（PERSONAL NOTE）：V1 用完整 bio，这里同源 ============ -->
      <section v-if="reading.story" id="story" class="profile-page__story">
        <div>
          <p class="public-eyebrow">PERSONAL NOTE</p>
          <h2>关于学习、实践与这套笔录</h2>
        </div>
        <BlogProse :markdown="reading.story" />
      </section>

      <!-- ============ 保持联系（LET'S KEEP IN TOUCH） ============ -->
      <section v-if="contactLinks.length" id="contact" class="profile-page__contact">
        <p class="public-eyebrow">LET'S KEEP IN TOUCH</p>
        <h2>如果你也在构建自己的知识世界，欢迎交流。</h2>
        <div>
          <a
            v-for="card in contactLinks"
            :key="card.key"
            :href="card.href"
            :target="card.external ? '_blank' : undefined"
            :rel="card.external ? 'noopener noreferrer' : undefined"
          >
            <span>{{ card.label }}</span>
            <b>{{ card.value }}</b>
            <i aria-hidden="true">{{ card.icon }}</i>
          </a>
        </div>
      </section>

      <!-- ============ 编辑原则引言块（EDITORIAL PRINCIPLE） ============ -->
      <blockquote class="profile-page__note">
        <span>EDITORIAL PRINCIPLE</span>
        <p>把复杂的知识梳理成路径，把每一次实践沉淀成可以再次抵达的坐标。</p>
      </blockquote>
    </template>
  </main>
</template>

<style scoped>
/*
 * 数值、层级、断点逐条对照 V1 `views/about/AboutView.vue`：
 *   宽度交给公开站外壳的 .layout-shell（--layout-max-width: 1400px + --page-padding-x），
 *   与 V2 的博客/作品列表页一致，因此这里不再重复 max-width 与左右内边距。
 */
.profile-page { padding-bottom: 100px; }

.profile-page__state {
  display: grid;
  place-content: center;
  gap: 14px;
  min-height: 420px;
  color: var(--text-muted);
  text-align: center;
}

.profile-page__state button {
  justify-self: center;
  padding: 9px 16px;
  border: 1px solid var(--border-strong);
  border-radius: 10px;
  color: var(--text-primary);
  background: var(--bg-surface);
  cursor: pointer;
  font-size: 13px;
}

/* ---------------- hero ---------------- */
.profile-page__hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: clamp(28px, 5vw, 72px);
  align-items: center;
  min-height: clamp(480px, 68vh, 620px);
  margin: 0 0 8px;
  padding: 48px 0 58px;
  overflow: hidden;
  border-bottom: 1px solid var(--border);
}

.profile-page__identity {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px 22px;
  align-items: center;
  max-width: 720px;
}

.profile-page__portrait {
  position: relative;
  isolation: isolate;
  width: 196px;
  height: 196px;
  margin: 0;
  padding: 0;
  overflow: visible;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  /* 头像不是硬边框卡片，而是一块落在雾气里的柔光底板 */
  filter: drop-shadow(0 18px 34px color-mix(in srgb, var(--primary) 16%, transparent));
}

.profile-page__portrait img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 12%;
  /* 依赖素材自带的透明通道 + 一层轻羽化，绝不做圆角矩形相框 */
  border-radius: 50%;
  background: transparent;
  -webkit-mask-image: radial-gradient(circle at 50% 42%, #000 46%, rgba(0, 0, 0, 0.55) 68%, transparent 82%);
  mask-image: radial-gradient(circle at 50% 42%, #000 46%, rgba(0, 0, 0, 0.55) 68%, transparent 82%);
}

.profile-page__hero h1 {
  margin: 9px 0 5px;
  font-size: clamp(42px, 5.6vw, 72px);
  line-height: 1.02;
  letter-spacing: -0.06em;
}

.profile-page__hero h2 {
  color: var(--primary);
  font-size: clamp(18px, 2.2vw, 28px);
  line-height: 1.4;
}

.profile-page__excerpt {
  grid-column: 1 / -1;
  max-width: 620px;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.9;
  white-space: pre-line;
}

.profile-page__tagline {
  grid-column: 1 / -1;
  margin: 0;
  color: var(--text-muted);
  font: italic 14px/1.5 Georgia, serif;
}

.profile-page__location {
  grid-column: 1 / -1;
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.profile-page__hero-actions {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.profile-page__hero-actions a {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  min-width: 118px;
  padding: 10px 13px;
  border: 1px solid var(--border-strong);
  border-radius: 10px;
  color: var(--text-primary);
  background: color-mix(in srgb, var(--bg-surface) 88%, transparent);
  backdrop-filter: blur(10px);
  font-size: 12px;
  font-weight: 700;
}

.profile-page__hero-actions a.primary {
  border-color: var(--primary);
  color: var(--on-primary);
  background: var(--primary);
}

/* ---------------- 四格速览 / 此刻关注 ---------------- */
.profile-page__career { padding: 24px 0; }

.profile-page__techflow {
  display: grid;
  grid-template-columns: 190px 1fr;
  gap: 30px;
  align-items: center;
  padding: 24px 0 34px;
  border-top: 1px solid var(--border);
}

.profile-page__techflow h2 {
  margin-top: 5px;
  font-size: 24px;
}

/* ---------------- sticky 章节胶囊导航 ---------------- */
.profile-page__nav {
  position: sticky;
  z-index: 12;
  top: 56px;
  display: flex;
  gap: 7px;
  overflow-x: auto;
  margin: 0 -10px;
  padding: 11px 10px;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg-page) 92%, transparent);
  backdrop-filter: blur(16px);
}

.profile-page__nav a {
  display: flex;
  flex: none;
  gap: 7px;
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  font-size: 11px;
}

.profile-page__nav a:hover { color: var(--primary); border-color: var(--primary); }

.profile-page__nav span {
  color: var(--accent);
  font: 650 9px var(--font-mono);
}

/* ---------------- 章节通用 ---------------- */
.profile-page__section {
  scroll-margin-top: 120px;
  padding: 72px 0;
  border-top: 1px solid var(--border);
}

/* 首屏 hero 自带下边框：紧随其后的那一块（关注点带 / 第一章）不再叠一条上边框。
   用 `>` 限定父级，避免命中双栏阅读区里的章节。 */
.profile-page__nav { border-top: 1px solid var(--border); }
.profile-page > .profile-page__hero + .profile-page__techflow,
.profile-page > .profile-page__hero + .profile-page__section,
.profile-page > .profile-page__career + .profile-page__section { border-top: 0; }

.profile-page__section > header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(250px, 0.62fr);
  gap: 32px;
  align-items: end;
  margin-bottom: 30px;
}

.profile-page__section > header h2 { margin-top: 8px; }

.profile-page__section > header > p {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.75;
}

/* ---------------- 代表作品 ---------------- */
.profile-page__work {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.profile-page__work-card {
  display: grid;
  grid-template-columns: 42% 1fr;
  min-height: 230px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  color: var(--text-primary);
  background: var(--bg-surface);
}

.profile-page__work-card.is-primary {
  grid-column: 1 / -1;
  grid-template-columns: 48% 1fr;
  min-height: 330px;
}

.profile-page__work-card > div { min-height: 200px; }

.profile-page__work-card article {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 24px;
}

.profile-page__work-card small {
  color: var(--accent);
  font: 700 9px var(--font-mono);
  letter-spacing: 0.12em;
}

.profile-page__work-card h3 {
  margin: 13px 0 28px;
  font-size: clamp(20px, 2.6vw, 32px);
  line-height: 1.25;
}

/* V1 的 h3 用 28px 下外边距把「查看项目复盘 →」压到卡底；有摘要时改由摘要承接间距，
   否则标题会与摘要之间出现一大段空白 */
.profile-page__work-card h3:has(+ .profile-page__work-summary) { margin-bottom: 14px; }

.profile-page__work-summary {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.75;
}

.profile-page__work-card article span {
  margin-top: auto;
  padding-top: 22px;
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.profile-page__work-card:hover {
  transform: translateY(-3px);
  border-color: var(--primary);
  box-shadow: var(--shadow-md);
}

/* 封面位：有图用图，没图用首字占位块（V2 的 featuredContents 常无 coverUrl） */
.profile-page__work-visual {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: radial-gradient(110% 85% at 15% 8%, color-mix(in srgb, var(--primary) 22%, transparent), transparent 56%),
    linear-gradient(155deg, var(--bg-subtle), var(--bg-surface));
}

.profile-page__work-visual img { width: 100%; height: 100%; object-fit: cover; }

.profile-page__work-visual > span {
  color: color-mix(in srgb, var(--primary) 72%, var(--bg-surface));
  font: 700 clamp(44px, 6vw, 84px)/1 Georgia, serif;
}

.profile-page__work-visual small {
  position: absolute;
  right: 14px;
  bottom: 12px;
  color: var(--text-muted);
  font: 700 9px var(--font-mono);
  letter-spacing: 0.16em;
}

/* ---------------- 双栏阅读区 ---------------- */
.profile-page__reading {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 55px;
}

.profile-page__reading > aside {
  position: sticky;
  top: 132px;
  align-self: start;
  display: grid;
  gap: 11px;
  padding-top: 74px;
}

.profile-page__reading > aside a {
  color: var(--text-muted);
  font-size: 12px;
}

.profile-page__reading > aside a:hover { color: var(--primary); }

/* 正文块：左侧一条主色发丝线，V1 同款（BlogProse 是子组件，必须用 :deep 穿透作用域） */
.profile-page__prose-section :deep(.markdown-body) {
  padding-left: 26px;
  border-left: 2px solid color-mix(in srgb, var(--primary) 30%, var(--border));
  font-size: 15px;
  line-height: 1.95;
}

.profile-page__prose-section :deep(.markdown-body h2:first-child) { margin-top: 0; }

/* 学习经历时间线（V2 原有结构保留，间距对齐 V1 章节节奏） */
.profile-page__timeline { margin: 0; padding: 0; list-style: none; }

.profile-page__timeline li {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 28px;
  padding: 25px 0;
  border-bottom: 1px solid var(--border);
}

.profile-page__period,
.profile-page__type { color: var(--text-muted); font-size: 12px; }

.profile-page__timeline h3 {
  margin: 8px 0;
  color: var(--text-primary);
  font-size: 21px;
}

.profile-page__timeline li > div:last-child > p { color: var(--text-secondary); }
.profile-page__timeline li > div:last-child :deep(.markdown-body) { max-width: 640px; font-size: 14px; }

/* ---------------- 知识内容 ---------------- */
.profile-page__knowledge {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 36px;
}

.profile-page__knowledge h3 {
  padding-bottom: 11px;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}

.profile-page__knowledge a {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 2px;
  border-bottom: 1px solid var(--border);
  color: var(--text-primary);
  font-size: 13px;
}

.profile-page__knowledge i { color: var(--primary); font-style: normal; }

/* ---------------- 个人说明 ---------------- */
.profile-page__story {
  scroll-margin-top: 120px;
  display: grid;
  grid-template-columns: minmax(220px, 0.55fr) minmax(0, 1.45fr);
  gap: clamp(30px, 6vw, 84px);
  padding: 74px 0;
  border-top: 1px solid var(--border);
}

.profile-page__story h2 {
  margin-top: 10px;
  font-size: clamp(25px, 3vw, 38px);
  line-height: 1.25;
}

.profile-page__story > :deep(.markdown-body) {
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.95;
}

/* ---------------- 保持联系 ---------------- */
.profile-page__contact {
  scroll-margin-top: 120px;
  padding: 80px 0 0;
}

.profile-page__contact h2 {
  max-width: 760px;
  margin: 12px 0 32px;
  font-size: clamp(30px, 4vw, 50px);
  line-height: 1.22;
  letter-spacing: -0.045em;
}

.profile-page__contact > div {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
}

.profile-page__contact a {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 7px;
  padding: 20px;
  border-right: 1px solid var(--border);
  color: var(--text-primary);
}

.profile-page__contact a:last-child { border-right: 0; }

.profile-page__contact a > span {
  grid-column: 1 / -1;
  color: var(--text-muted);
  font-size: 10px;
}

.profile-page__contact b {
  overflow-wrap: anywhere;
  font-size: 12px;
}

.profile-page__contact i { color: var(--primary); font-style: normal; }

/* ---------------- 编辑原则引言块 ---------------- */
.profile-page__note {
  max-width: 780px;
  margin: 80px auto 0;
  padding: 28px;
  border: 1px solid var(--border);
  border-left: 3px solid var(--accent);
  border-radius: 0 18px 18px 0;
  background: var(--bg-surface);
}

.profile-page__note span {
  color: var(--accent);
  font: 700 9px var(--font-mono);
  letter-spacing: 0.15em;
}

.profile-page__note p {
  margin-top: 18px;
  font-family: Georgia, 'Songti SC', serif;
  font-size: 21px;
  line-height: 1.75;
}

/* ---------------- 断点（V1：980 / 650，另加 760 收窄章节头） ---------------- */
@media (max-width: 980px) {
  .profile-page__hero { grid-template-columns: 1fr; min-height: 420px; }
  .profile-page__techflow { grid-template-columns: 1fr; }
  .profile-page__reading { grid-template-columns: 1fr; }
  .profile-page__reading > aside { display: none; }
}

@media (max-width: 760px) {
  .profile-page__section > header { grid-template-columns: 1fr; gap: 10px; }
}

@media (max-width: 650px) {
  .profile-page { padding-bottom: 70px; }
  .profile-page__hero { min-height: 360px; padding: 28px 0 46px; }
  .profile-page__identity { grid-template-columns: minmax(0, 1fr); gap: 12px; }
  .profile-page__portrait { width: 132px; height: 132px; }
  .profile-page__hero h1 { font-size: 42px; }
  /* 四格在窄屏收成两列（CareerSnapshot 自己的 768px 规则已覆盖，这里再压一层内边距） */
  .profile-page__career :deep(.career-snapshot) { grid-template-columns: 1fr 1fr; gap: 8px; }
  .profile-page__career :deep(.career-snapshot__cell) { padding: 14px; }
  .profile-page__nav { top: 55px; margin-inline: -20px; padding-inline: 20px; }
  .profile-page__section { padding: 52px 0; }
  .profile-page__work { grid-template-columns: 1fr; }
  .profile-page__work-card,
  .profile-page__work-card.is-primary { grid-column: auto; grid-template-columns: 1fr; min-height: 0; }
  .profile-page__work-card > div { height: 180px; min-height: 180px; }
  .profile-page__knowledge,
  .profile-page__contact > div { grid-template-columns: 1fr; }
  .profile-page__contact a { border-right: 0; border-bottom: 1px solid var(--border); }
  .profile-page__contact a:last-child { border-bottom: 0; }
  .profile-page__prose-section :deep(.markdown-body) { padding-left: 15px; }
  .profile-page__story { grid-template-columns: 1fr; gap: 16px; padding: 52px 0; }
  .profile-page__timeline li { grid-template-columns: 1fr; gap: 8px; }
  .profile-page__note { margin-top: 54px; }
  .profile-page__note p { font-size: 18px; }
}

@media (prefers-reduced-motion: reduce) {
  .profile-page__work-card { transition: none; }
}
</style>
