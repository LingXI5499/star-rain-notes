<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  fetchPublicCategoryTree,
  fetchPublicTutorials,
  type PublicCategoryNode,
  type PublicTutorialSummary,
} from '@/api/tutorial'

/**
 * Tutorial center (V3 polish): top category chips + featured highlight card +
 * tutorial card grid with live sort. Uses real data (real chapter counts).
 */
const route = useRoute()
const router = useRouter()

const categories = ref<PublicCategoryNode[]>([])
const flatCategories = ref<PublicCategoryNode[]>([])
const tutorials = ref<PublicTutorialSummary[]>([])
const loading = ref(true)
const error = ref(false)
const activeSlug = ref('')
const sortBy = ref<'default' | 'chapters'>('default')

function flatten(nodes: PublicCategoryNode[]): PublicCategoryNode[] {
  const out: PublicCategoryNode[] = []
  for (const node of nodes) {
    out.push(node)
    out.push(...flatten(node.children))
  }
  return out
}

const displayed = computed(() => {
  const list = [...tutorials.value]
  if (sortBy.value === 'chapters') {
    list.sort((a, b) => b.publishedChapterCount - a.publishedChapterCount)
  }
  return list
})

const activeCategory = computed(
  () => flatCategories.value.find((c) => c.slug === activeSlug.value) ?? null,
)
const featured = computed(() =>
  activeCategory.value
    ? activeCategory.value
    : flatCategories.value.find((c) => c.slug === 'java-fullstack') ?? flatCategories.value[0] ?? null,
)

async function load() {
  loading.value = true
  error.value = false
  try {
    tutorials.value = await fetchPublicTutorials(activeSlug.value || undefined)
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function selectCategory(slug: string) {
  activeSlug.value = slug
  router.replace({ query: slug ? { categorySlug: slug } : {} })
  void load()
}

onMounted(async () => {
  try {
    categories.value = await fetchPublicCategoryTree()
    flatCategories.value = flatten(categories.value)
  } catch {
    // Category filters are secondary; a failure only hides the navigation.
  }
  if (typeof route.query.categorySlug === 'string') {
    activeSlug.value = route.query.categorySlug
  }
  await load()
})

watch(
  () => route.query.categorySlug,
  (value) => {
    const slug = typeof value === 'string' ? value : ''
    if (slug !== activeSlug.value) {
      activeSlug.value = slug
      void load()
    }
  },
)
</script>

<template>
  <section class="tutorials">
    <header class="tutorials__hero">
      <p class="tutorials__eyebrow">TUTORIALS · KNOWLEDGE SYSTEM</p>
      <h1 class="tutorials__title">教程</h1>
      <p class="tutorials__subtitle">
        系统化技术课程与知识体系，持续学习、长期成长。从基础到进阶，构建完整的知识体系。
      </p>
      <nav v-if="flatCategories.length" class="tutorials__chips" role="group" aria-label="分类筛选">
        <button
          type="button"
          class="tutorials__chip"
          :class="{ 'is-active': activeSlug === '' }"
          @click="selectCategory('')"
        >
          全部
        </button>
        <button
          v-for="category in flatCategories"
          :key="category.id"
          type="button"
          class="tutorials__chip"
          :class="{ 'is-active': activeSlug === category.slug }"
          @click="selectCategory(category.slug)"
        >
          {{ category.name }}
        </button>
      </nav>
    </header>

    <!-- featured highlight card -->
    <RouterLink
      v-if="featured"
      :to="featured.slug ? `/tutorials?categorySlug=${featured.slug}` : '/tutorials'"
      class="tutorials__featured"
    >
      <span class="tutorials__featured-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
      </span>
      <div class="tutorials__featured-body">
        <p class="tutorials__featured-tag">精选推荐 · {{ featured.name }}</p>
        <h2 class="tutorials__featured-title">{{ featured.name }}</h2>
        <p class="tutorials__featured-desc">
          {{ featured.name }}知识体系：覆盖核心基础到进阶应用，系统化学习路径。
        </p>
        <p class="tutorials__featured-meta">{{ tutorials.length }} 个教程</p>
      </div>
      <span class="tutorials__featured-cta">查看教程 →</span>
    </RouterLink>

    <div class="tutorials__toolbar">
      <p class="tutorials__count">共 {{ tutorials.length }} 个教程</p>
      <select v-model="sortBy" class="tutorials__sort" aria-label="排序">
        <option value="default">综合排序</option>
        <option value="chapters">章节数</option>
      </select>
    </div>

    <div v-if="loading" class="tutorials__empty">加载中…</div>
    <div v-else-if="error" class="tutorials__empty">加载失败，请稍后重试。</div>
    <div v-else-if="!displayed.length" class="tutorials__empty">暂无教程</div>
    <div v-else class="tutorials__grid">
      <RouterLink
        v-for="tutorial in displayed"
        :key="tutorial.id"
        :to="`/tutorials/${tutorial.slug}`"
        class="tutorial-card"
      >
        <p class="tutorial-card__category">{{ tutorial.categoryName }}</p>
        <h2 class="tutorial-card__title">{{ tutorial.title }}</h2>
        <p class="tutorial-card__summary">{{ tutorial.summary }}</p>
        <p class="tutorial-card__meta">{{ tutorial.publishedChapterCount }} 个章节</p>
        <span class="tutorial-card__more">查看 →</span>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.tutorials__hero {
  margin-bottom: var(--space-8);
}

.tutorials__eyebrow {
  font-size: 14px;
  letter-spacing: 0.18em;
  color: var(--accent);
  margin-bottom: var(--space-3);
}

.tutorials__title {
  font-size: 42px;
  line-height: 50px;
  margin-bottom: var(--space-3);
}

.tutorials__subtitle {
  font-size: 16px;
  line-height: 28px;
  color: var(--text-secondary);
  max-width: 560px;
  margin-bottom: var(--space-6);
}

/* ---------- category chips ---------- */
.tutorials__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.tutorials__chip {
  padding: var(--space-2) var(--space-5);
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition:
    color 0.15s ease,
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.tutorials__chip:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.tutorials__chip.is-active {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--on-primary);
}

/* ---------- featured highlight ---------- */
.tutorials__featured {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  padding: var(--space-6) var(--space-7);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(120deg, color-mix(in srgb, var(--primary) 10%, transparent), transparent 60%),
    var(--bg-surface);
  margin-bottom: var(--space-8);
  color: var(--text-primary);
  transition:
    border-color 0.15s ease,
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.tutorials__featured:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgb(0 0 0 / 0.08);
}

.tutorials__featured-icon {
  color: var(--accent);
  flex-shrink: 0;
}

.tutorials__featured-body {
  flex: 1;
  min-width: 0;
}

.tutorials__featured-tag {
  font-size: 13px;
  color: var(--accent);
  margin-bottom: var(--space-2);
}

.tutorials__featured-title {
  font-size: 26px;
  line-height: 34px;
  margin-bottom: var(--space-2);
}

.tutorials__featured-desc {
  font-size: 14px;
  line-height: 22px;
  color: var(--text-secondary);
  margin-bottom: var(--space-3);
}

.tutorials__featured-meta {
  font-size: 14px;
  color: var(--primary);
  font-weight: 500;
}

.tutorials__featured-cta {
  flex-shrink: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--primary);
  white-space: nowrap;
}

@media (max-width: 768px) {
  .tutorials__featured {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* ---------- toolbar ---------- */
.tutorials__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-5);
}

.tutorials__count {
  font-size: 14px;
  color: var(--text-muted);
}

.tutorials__sort {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--bg-surface);
  color: var(--text-secondary);
  font-size: 13px;
}

/* ---------- tutorial cards ---------- */
.tutorials__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-5);
}

.tutorial-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: var(--space-6);
  color: var(--text-primary);
  transition:
    border-color 0.15s ease,
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.tutorial-card:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgb(0 0 0 / 0.08);
}

.tutorial-card__category {
  font-size: 13px;
  color: var(--accent);
}

.tutorial-card__title {
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
}

.tutorial-card__summary {
  font-size: 14px;
  line-height: 22px;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tutorial-card__meta {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: auto;
}

.tutorial-card__more {
  font-size: 14px;
  color: var(--primary);
}

.tutorials__empty {
  color: var(--text-muted);
  padding: var(--space-8) 0;
}
</style>
