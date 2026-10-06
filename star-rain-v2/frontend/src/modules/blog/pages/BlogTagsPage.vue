<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listPublicTags } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'

/*
 * 标签总览页 —— 首页右栏标签块「更多」的落点。
 *
 * 首页右栏只放得下一部分标签（按实测行数裁剪），完整清单在这里。
 * 点击标签回到博客列表并按该标签筛选（`?tag=`），筛选状态仍然只存在 URL 里，
 * 与首页、归档页保持同一套约定。
 */
const { contentPath } = useViewMode()
const router = useRouter()
const tags = ref([])
const loading = ref(true)
const error = ref('')

const total = computed(() => tags.value.reduce((sum, tag) => sum + (tag.postCount || 0), 0))

onMounted(async () => {
  try {
    tags.value = await listPublicTags()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
})

function openTag(slug) {
  router.push({ path: contentPath('/blog'), query: { tag: slug } })
}
</script>

<template>
  <section class="page-container tag-index">
    <header class="tag-index__hero">
      <p class="tag-index__eyebrow">TAGS · 标签总览</p>
      <h1>全部标签</h1>
      <p>共 {{ tags.length }} 个标签、{{ total }} 次文章引用。点击任意标签回到博客列表并按它筛选。</p>
    </header>

    <p v-if="loading" class="tag-index__empty">正在加载标签…</p>
    <p v-else-if="error" class="tag-index__empty" role="alert">{{ error }}</p>
    <p v-else-if="!tags.length" class="tag-index__empty">还没有可用标签。</p>
    <div v-else class="tag-index__grid">
      <button v-for="tag in tags" :key="tag.id || tag.slug" type="button" @click="openTag(tag.slug)">
        <span>{{ tag.name }}</span>
        <em>{{ tag.postCount }} 篇</em>
      </button>
    </div>
  </section>
</template>

<style scoped>
.tag-index__hero {
  margin-bottom: var(--space-7);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--border);
}

.tag-index__eyebrow {
  margin-bottom: 10px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.tag-index__hero h1 {
  margin-bottom: 8px;
  font-size: clamp(30px, 3.6vw, 44px);
  letter-spacing: -0.03em;
}

.tag-index__hero p:last-child {
  color: var(--text-secondary);
  line-height: 1.8;
}

.tag-index__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(180px, 100%), 1fr));
  gap: 12px;
}

.tag-index__grid button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 14px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: transform 170ms ease, border-color 170ms ease, color 170ms ease;
}

.tag-index__grid button:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--primary) 55%, var(--border));
  color: var(--primary);
}

.tag-index__grid em {
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
  white-space: nowrap;
}

.tag-index__empty {
  padding: var(--space-10);
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-md);
  color: var(--text-muted);
  text-align: center;
}

@media (prefers-reduced-motion: reduce) {
  .tag-index__grid button { transition: none; }
}

@media (max-width: 620px) {
  .tag-index__grid { grid-template-columns: 1fr; }
}
</style>
