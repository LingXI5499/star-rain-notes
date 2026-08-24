<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  fetchPublicCategoryTree,
  fetchPublicTutorials,
  type PublicCategoryNode,
  type PublicTutorialSummary,
} from '@/api/tutorial'
import TutorialCategoryNav from '@/components/TutorialCategoryNav.vue'

const route = useRoute()
const router = useRouter()
const categories = ref<PublicCategoryNode[]>([])
const tutorials = ref<PublicTutorialSummary[]>([])
const loading = ref(true)
const error = ref(false)
const activeSlug = ref('')
const query = ref('')

function flatten(nodes: PublicCategoryNode[]): PublicCategoryNode[] {
  return nodes.flatMap((node) => [node, ...flatten(node.children)])
}

const flatCategories = computed(() => flatten(categories.value))
const activeCategory = computed(() => flatCategories.value.find((item) => item.slug === activeSlug.value) ?? null)
const displayed = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return keyword
    ? tutorials.value.filter((item) => item.title.toLowerCase().includes(keyword) || item.summary.toLowerCase().includes(keyword))
    : tutorials.value
})

async function loadTutorials() {
  loading.value = true
  error.value = false
  try { tutorials.value = await fetchPublicTutorials(activeSlug.value || undefined) }
  catch { error.value = true }
  finally { loading.value = false }
}

async function selectCategory(slug: string) {
  if (slug === activeSlug.value) return
  activeSlug.value = slug
  query.value = ''
  await router.replace({ query: slug ? { categorySlug: slug } : {} })
  await loadTutorials()
}

onMounted(async () => {
  try { categories.value = await fetchPublicCategoryTree() }
  catch { /* Tutorial cards remain usable when category navigation fails. */ }
  activeSlug.value = typeof route.query.categorySlug === 'string' ? route.query.categorySlug : ''
  await loadTutorials()
})

watch(() => route.query.categorySlug, (value) => {
  const slug = typeof value === 'string' ? value : ''
  if (slug !== activeSlug.value) { activeSlug.value = slug; void loadTutorials() }
})
</script>

<template>
  <section class="tutorial-catalog">
    <header class="tutorial-catalog__hero">
      <div>
        <p class="tutorial-catalog__eyebrow">LEARNING PATHS · 教程中心</p>
        <h1>选择一门教程，开始系统学习</h1>
        <p>左侧按知识体系浏览，右侧从教程卡片进入；课程内提供完整目录、上下篇与页内导航。</p>
      </div>
      <div class="tutorial-catalog__summary">
        <strong>{{ tutorials.length }}</strong>
        <span>{{ activeCategory ? '当前分类教程' : '公开教程' }}</span>
      </div>
    </header>

    <div class="tutorial-catalog__layout">
      <aside class="tutorial-catalog__sidebar">
        <div class="tutorial-catalog__sidebar-head">
          <span>教程目录</span>
          <small>{{ flatCategories.length }} 类</small>
        </div>
        <nav aria-label="教程分类">
          <button
            type="button"
            class="tutorial-catalog__all"
            :class="{ 'tutorial-catalog__all--active': !activeSlug }"
            @click="selectCategory('')"
          >
            <span>全部教程</span><span>›</span>
          </button>
          <ul class="tutorial-catalog__category-tree">
            <TutorialCategoryNav
              v-for="category in categories"
              :key="category.id"
              :node="category"
              :active-slug="activeSlug"
              @select="selectCategory"
            />
          </ul>
        </nav>
      </aside>

      <main class="tutorial-catalog__content">
        <div class="tutorial-catalog__content-head">
          <div>
            <p class="tutorial-catalog__path">教程 / {{ activeCategory?.name || '全部教程' }}</p>
            <h2>{{ activeCategory?.name || '全部教程' }}</h2>
            <p>共 {{ tutorials.length }} 门可学习教程</p>
          </div>
          <label class="tutorial-catalog__search">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
            <input v-model="query" type="search" placeholder="筛选当前教程" aria-label="筛选当前教程" />
          </label>
        </div>

        <div v-if="loading" class="tutorial-catalog__empty">正在加载教程…</div>
        <div v-else-if="error" class="tutorial-catalog__empty">加载失败，请稍后重试。</div>
        <div v-else-if="!displayed.length" class="tutorial-catalog__empty">当前分类暂无匹配教程。</div>
        <div v-else class="tutorial-catalog__grid">
          <RouterLink
            v-for="tutorial in displayed"
            :key="tutorial.id"
            :to="tutorial.firstChapterSlug ? `/tutorials/${tutorial.slug}/${tutorial.firstChapterSlug}` : `/tutorials/${tutorial.slug}`"
            class="tutorial-card"
          >
            <div v-if="tutorial.coverUrl" class="tutorial-card__cover">
              <img :src="tutorial.coverUrl" :alt="tutorial.title" loading="lazy" />
            </div>
            <div v-else class="tutorial-card__cover tutorial-card__cover--placeholder" aria-hidden="true">
              <span>{{ tutorial.title.slice(0, 1) }}</span>
            </div>
            <div class="tutorial-card__body">
              <p class="tutorial-card__category">{{ tutorial.categoryName }}</p>
              <h3>{{ tutorial.title }}</h3>
              <p class="tutorial-card__summary">{{ tutorial.summary }}</p>
              <div class="tutorial-card__footer">
                <span>{{ tutorial.publishedChapterCount }} 个章节</span>
                <strong>开始学习 →</strong>
              </div>
            </div>
          </RouterLink>
        </div>
      </main>
    </div>
  </section>
</template>

<style scoped>
.tutorial-catalog__hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-8);
  margin-bottom: var(--space-8);
  padding: var(--space-7) 0;
  border-bottom: 1px solid var(--border);
}
.tutorial-catalog__eyebrow { margin-bottom: var(--space-3); color: var(--accent); font-size: 12px; font-weight: 700; letter-spacing: .16em; }
.tutorial-catalog__hero h1 { max-width: 760px; margin-bottom: var(--space-3); font-size: clamp(32px,4vw,48px); line-height: 1.16; letter-spacing: -.03em; }
.tutorial-catalog__hero p:last-child { max-width: 720px; color: var(--text-secondary); font-size: 15px; line-height: 1.8; }
.tutorial-catalog__summary { min-width: 130px; padding: var(--space-4) var(--space-5); border: 1px solid var(--border); border-radius: var(--radius-md); background: var(--bg-surface); text-align: center; }
.tutorial-catalog__summary strong { display: block; color: var(--primary); font-size: 28px; }
.tutorial-catalog__summary span { color: var(--text-muted); font-size: 12px; }
.tutorial-catalog__layout { display: grid; grid-template-columns: 260px minmax(0,1fr); gap: var(--space-8); align-items: start; }
.tutorial-catalog__sidebar { position: sticky; top: calc(var(--header-height) + var(--space-5)); max-height: calc(100vh - var(--header-height) - var(--space-10)); overflow: auto; border: 1px solid var(--border); border-radius: var(--radius-md); background: var(--bg-surface); }
.tutorial-catalog__sidebar-head { display: flex; align-items: center; justify-content: space-between; padding: var(--space-4); border-bottom: 1px solid var(--border); font-weight: 700; }
.tutorial-catalog__sidebar-head small { color: var(--text-muted); font-weight: 400; }
.tutorial-catalog__sidebar nav { padding: var(--space-3); }
.tutorial-catalog__all { width: 100%; min-height: 40px; display: flex; align-items: center; justify-content: space-between; padding: 7px 10px; border: 0; border-radius: 7px; color: var(--text-secondary); background: none; font-size: 14px; text-align: left; cursor: pointer; }
.tutorial-catalog__all:hover { background: var(--bg-subtle); color: var(--text-primary); }
.tutorial-catalog__all--active { color: var(--primary); background: color-mix(in srgb,var(--primary) 11%,transparent); font-weight: 600; }
.tutorial-catalog__category-tree { margin: 3px 0 0; padding: 0; list-style: none; }
.tutorial-catalog__content { min-width: 0; }
.tutorial-catalog__content-head { display: flex; align-items: flex-end; justify-content: space-between; gap: var(--space-5); margin-bottom: var(--space-6); }
.tutorial-catalog__path { margin-bottom: var(--space-2); color: var(--text-muted); font-size: 12px; }
.tutorial-catalog__content-head h2 { margin-bottom: 4px; font-size: 26px; line-height: 1.3; }
.tutorial-catalog__content-head > div > p:last-child { color: var(--text-muted); font-size: 13px; }
.tutorial-catalog__search { width: min(280px,38vw); height: 40px; display: flex; align-items: center; gap: var(--space-2); padding: 0 var(--space-3); border: 1px solid var(--border-strong); border-radius: 8px; color: var(--text-muted); background: var(--bg-surface); }
.tutorial-catalog__search:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px color-mix(in srgb,var(--primary) 12%,transparent); }
.tutorial-catalog__search input { min-width: 0; flex: 1; border: 0; outline: 0; color: var(--text-primary); background: none; }
.tutorial-catalog__grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(290px,1fr)); gap: var(--space-5); }
.tutorial-card { overflow: hidden; display: flex; flex-direction: column; min-height: 330px; border: 1px solid var(--border); border-radius: var(--radius-md); color: var(--text-primary); background: var(--bg-surface); transition: transform 160ms ease,border-color 160ms ease,box-shadow 160ms ease; }
.tutorial-card:hover { transform: translateY(-3px); border-color: color-mix(in srgb,var(--primary) 55%,var(--border)); box-shadow: 0 14px 35px rgb(0 0 0/.09); }
.tutorial-card__cover { height: 128px; overflow: hidden; background: var(--bg-subtle); }
.tutorial-card__cover img { width: 100%; height: 100%; display: block; object-fit: cover; transition: transform 240ms ease; }
.tutorial-card:hover .tutorial-card__cover img { transform: scale(1.03); }
.tutorial-card__cover--placeholder { display: grid; place-items: center; background: linear-gradient(135deg,color-mix(in srgb,var(--primary) 18%,var(--bg-surface)),color-mix(in srgb,var(--accent) 11%,var(--bg-surface))); }
.tutorial-card__cover--placeholder span { color: color-mix(in srgb,var(--primary) 80%,var(--text-primary)); font-size: 42px; font-weight: 800; opacity: .75; }
.tutorial-card__body { flex: 1; display: flex; flex-direction: column; padding: var(--space-5); }
.tutorial-card__category { margin-bottom: var(--space-2); color: var(--accent); font-size: 12px; }
.tutorial-card h3 { margin-bottom: var(--space-3); font-size: 19px; line-height: 1.45; }
.tutorial-card__summary { display: -webkit-box; overflow: hidden; margin-bottom: var(--space-5); color: var(--text-secondary); font-size: 14px; line-height: 1.7; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
.tutorial-card__footer { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); margin-top: auto; padding-top: var(--space-4); border-top: 1px solid var(--border); color: var(--text-muted); font-size: 12px; }
.tutorial-card__footer strong { color: var(--primary); font-size: 13px; }
.tutorial-catalog__empty { padding: var(--space-10); border: 1px dashed var(--border-strong); border-radius: var(--radius-md); color: var(--text-muted); text-align: center; }
@media (max-width: 900px) { .tutorial-catalog__hero { align-items: flex-start; } .tutorial-catalog__summary { display: none; } .tutorial-catalog__layout { grid-template-columns: 1fr; } .tutorial-catalog__sidebar { position: static; max-height: none; } }
@media (max-width: 620px) { .tutorial-catalog__content-head { align-items: stretch; flex-direction: column; } .tutorial-catalog__search { width: 100%; } .tutorial-catalog__grid { grid-template-columns: 1fr; } }
</style>
