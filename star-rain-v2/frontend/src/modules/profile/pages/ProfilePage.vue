<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BlogProse from '../../blog/components/BlogProse.vue'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import { getPublicProfile } from '../api/profileApi'

const { contentPath } = useViewMode()
const profile = ref(null)
const loading = ref(true)
const error = ref('')
const skillGroups = computed(() => [
  { key: 'SKILL', label: '技术技能', eyebrow: 'SKILLS' },
  { key: 'DIRECTION', label: '探索方向', eyebrow: 'DIRECTIONS' },
  { key: 'INTEREST', label: '兴趣领域', eyebrow: 'INTERESTS' },
].map((group) => ({ ...group, items: profile.value?.skills?.filter((item) => item.category === group.key) || [] }))
  .filter((group) => group.items.length))

async function load() {
  loading.value = true
  error.value = ''
  try {
    profile.value = await getPublicProfile()
  } catch (cause) {
    error.value = cause?.response?.status === 404 ? '作者资料暂未公开。' : errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function date(value) {
  if (!value) return ''
  return new Date(`${value}T00:00:00`).toLocaleDateString('zh-CN', { year: 'numeric', month: 'short' })
}

onMounted(load)
</script>

<template>
  <main class="profile-page">
    <p v-if="loading" class="profile-page__state">正在读取作者资料…</p>
    <p v-else-if="error" class="profile-page__state" role="alert">{{ error }}</p>
    <template v-else-if="profile">
      <header class="profile-page__hero">
        <div class="profile-page__portrait"><img :src="profile.avatarUrl || '/brand/mark.svg'" :alt="`${profile.displayName}的头像`" /></div>
        <div class="profile-page__intro">
          <p class="profile-page__eyebrow">ABOUT THE AUTHOR · 关于</p>
          <h1>{{ profile.displayName }}</h1>
          <h2 v-if="profile.headline">{{ profile.headline }}</h2>
          <p v-if="profile.locationText" class="profile-page__location">{{ profile.locationText }}</p>
          <div class="profile-page__hero-actions">
            <a v-if="profile.resumeUrl" :href="profile.resumeUrl" target="_blank" rel="noopener noreferrer">查看公开简历 ↗</a>
            <RouterLink :to="contentPath('/messages')">留言交流 →</RouterLink>
          </div>
        </div>
      </header>

      <section v-if="profile.bioMarkdown" class="profile-page__section profile-page__story">
        <div class="profile-page__section-heading"><p class="profile-page__eyebrow">PERSONAL NOTE</p><h2>关于我</h2></div>
        <BlogProse :markdown="profile.bioMarkdown" />
      </section>

      <section v-if="profile.featuredContents?.length" class="profile-page__section">
        <div class="profile-page__section-heading"><p class="profile-page__eyebrow">SELECTED WORK</p><h2>精选内容</h2></div>
        <div class="profile-page__featured">
          <RouterLink v-for="item in profile.featuredContents" :key="item.id" :to="contentPath(item.url)" class="profile-page__feature">
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" loading="lazy" />
            <span>{{ { TUTORIAL: '教程', BLOG: '博客', PORTFOLIO: '作品' }[item.contentType] }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.summary }}</p>
            <b>阅读详情 →</b>
          </RouterLink>
        </div>
      </section>

      <section v-if="profile.experiences?.length" class="profile-page__section">
        <div class="profile-page__section-heading"><p class="profile-page__eyebrow">JOURNEY</p><h2>经历与成长</h2></div>
        <ol class="profile-page__timeline">
          <li v-for="item in profile.experiences" :key="item.id">
            <div class="profile-page__period">{{ date(item.startDate) || '过去' }} — {{ item.isCurrent ? '现在' : date(item.endDate) || '持续中' }}</div>
            <div><span class="profile-page__type">{{ { EDUCATION: '教育', PROJECT: '项目', CAREER: '职业', GROWTH: '成长', OTHER: '其他' }[item.experienceType] }}</span><h3>{{ item.title }}</h3><p v-if="item.organization">{{ item.organization }}</p><BlogProse v-if="item.descriptionMd" :markdown="item.descriptionMd" /></div>
          </li>
        </ol>
      </section>

      <section v-if="skillGroups.length" class="profile-page__section">
        <div class="profile-page__section-heading"><p class="profile-page__eyebrow">TECHNICAL MAP</p><h2>技术地图</h2></div>
        <div class="profile-page__skill-groups">
          <div v-for="group in skillGroups" :key="group.key"><p class="profile-page__eyebrow">{{ group.eyebrow }}</p><h3>{{ group.label }}</h3>
            <ul><li v-for="item in group.items" :key="item.id"><strong>{{ item.name }}</strong><span v-if="item.proficiency">{{ item.proficiency }}</span><p v-if="item.description">{{ item.description }}</p></li></ul>
          </div>
        </div>
      </section>

      <section v-if="profile.socialLinks?.length" class="profile-page__section profile-page__contact">
        <div class="profile-page__section-heading"><p class="profile-page__eyebrow">CONNECT</p><h2>保持联系</h2></div>
        <div><a v-for="item in profile.socialLinks" :key="item.id" :href="item.url" target="_blank" rel="noopener noreferrer">{{ item.label }} <span>↗</span></a></div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.profile-page { max-width: var(--layout-max-width); margin: 0 auto; padding: 0 var(--page-padding-x) 110px; }
.profile-page__state { min-height: 420px; display: grid; place-content: center; color: var(--text-muted); }
.profile-page__hero { display: grid; grid-template-columns: minmax(160px, 240px) 1fr; align-items: center; gap: clamp(28px, 6vw, 84px); margin: 36px 0 62px; padding: clamp(36px, 6vw, 70px); border-radius: 28px; background: radial-gradient(circle at 5% 10%, color-mix(in srgb, var(--accent) 16%, transparent), transparent 45%), linear-gradient(140deg, color-mix(in srgb, var(--primary) 12%, var(--bg-surface)), var(--bg-surface)); border: 1px solid var(--border); }
.profile-page__portrait { aspect-ratio: 1; border-radius: 28px; overflow: hidden; background: color-mix(in srgb, var(--primary) 12%, var(--bg-page)); display: grid; place-items: center; }
.profile-page__portrait img { width: 100%; height: 100%; object-fit: cover; }
.profile-page__eyebrow { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .16em; }
.profile-page__intro h1 { color: var(--text-primary); font-size: clamp(42px, 6vw, 74px); line-height: 1.08; margin: 15px 0 12px; }
.profile-page__intro h2 { font-size: clamp(18px, 2.4vw, 27px); color: var(--primary); font-weight: 550; }
.profile-page__location { color: var(--text-muted); margin-top: 13px; }
.profile-page__hero-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 30px; }
.profile-page__hero-actions a { border: 1px solid var(--border); border-radius: 9px; padding: 11px 16px; background: var(--bg-page); color: var(--text-primary); }
.profile-page__hero-actions a:hover { color: var(--primary); border-color: var(--primary); }
.profile-page__section { padding: 55px 0; border-top: 1px solid var(--border); }
.profile-page__section-heading { margin-bottom: 28px; }
.profile-page__section-heading h2 { font-size: clamp(29px, 4vw, 44px); color: var(--text-primary); margin-top: 8px; }
.profile-page__story :deep(.markdown-body) { max-width: 820px; }
.profile-page__featured { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; }
.profile-page__feature { display: block; color: var(--text-primary); border: 1px solid var(--border); border-radius: 18px; padding: 22px; background: var(--bg-surface); }
.profile-page__feature:hover { border-color: var(--primary); transform: translateY(-2px); }
.profile-page__feature img { width: 100%; aspect-ratio: 16/9; object-fit: cover; border-radius: 10px; margin-bottom: 18px; }
.profile-page__feature > span { color: var(--accent); font-size: 11px; }
.profile-page__feature h3 { font-size: 20px; margin: 9px 0; }
.profile-page__feature p { color: var(--text-secondary); line-height: 1.7; }
.profile-page__feature b { display: inline-block; margin-top: 20px; font-size: 13px; color: var(--primary); }
.profile-page__timeline { list-style: none; padding: 0; }
.profile-page__timeline li { display: grid; grid-template-columns: 180px 1fr; gap: 28px; padding: 25px 0; border-bottom: 1px solid var(--border); }
.profile-page__period, .profile-page__type { color: var(--text-muted); font-size: 12px; }
.profile-page__timeline h3 { color: var(--text-primary); font-size: 21px; margin: 8px 0; }
.profile-page__timeline li > div:last-child > p { color: var(--text-secondary); }
.profile-page__skill-groups { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 24px; }
.profile-page__skill-groups > div { border: 1px solid var(--border); border-radius: 16px; padding: 25px; background: var(--bg-surface); }
.profile-page__skill-groups h3 { color: var(--text-primary); font-size: 21px; margin: 7px 0 20px; }
.profile-page__skill-groups ul { list-style: none; padding: 0; }
.profile-page__skill-groups li { border-top: 1px solid var(--border); padding: 12px 0; color: var(--text-primary); }
.profile-page__skill-groups li span { margin-left: 10px; font-size: 12px; color: var(--text-muted); }
.profile-page__skill-groups li p { color: var(--text-secondary); font-size: 13px; line-height: 1.7; margin-top: 5px; }
.profile-page__contact > div:last-child { display: flex; flex-wrap: wrap; gap: 12px; }
.profile-page__contact a { min-width: 150px; display: flex; justify-content: space-between; gap: 20px; padding: 18px; border: 1px solid var(--border); border-radius: 10px; color: var(--text-primary); }
.profile-page__contact a:hover { border-color: var(--primary); color: var(--primary); }
@media(max-width: 680px) { .profile-page__hero { grid-template-columns: 1fr; } .profile-page__portrait { max-width: 180px; } .profile-page__timeline li { grid-template-columns: 1fr; gap: 8px; } }
</style>
