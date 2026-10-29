<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { libraryPath, libraryQuery } from '../lib/libraryNavigation'
const props = defineProps({ module: String, categories: { type: Array, default: () => [] }, counts: { type: Object, default: () => ({}) }, total: Number, basePath: String, showCounts: { type: Boolean, default: true } })
const route = useRoute(), router = useRouter()
const category = computed(() => props.categories.find(c => c.code?.toLowerCase() === String((props.basePath ? route.query.category : route.params.category) || '').toLowerCase()))
const topicCode = computed(() => String((props.basePath ? route.query.topic : route.params.topic) || '').toUpperCase())
const target = (parent = '', topic = '') => props.basePath
  ? { path: props.basePath, query: libraryQuery(route.query, { category: parent || undefined, topic: topic || undefined }) }
  : { path: libraryPath(props.module, parent, topic), query: libraryQuery(route.query, {status: undefined}) }
</script>
<template>
  <section class="library-filters" aria-label="两级分类筛选">
    <div class="library-filter-selects">
      <label>一级分类<select aria-label="一级分类" :value="category?.code || ''" @change="router.push(target($event.target.value))"><option value="">全部分类{{ showCounts ? `（${total}）` : '' }}</option><option v-for="c in categories" :key="c.id" :value="c.code">{{ c.name }}{{ showCounts ? `（${counts[c.code] || 0}）` : '' }}</option></select></label>
      <label>二级主题<select aria-label="二级主题" :disabled="!category" :value="topicCode" @change="router.push(target(category.code, $event.target.value))"><option value="">全部主题</option><option v-for="t in category?.children || []" :key="t.id" :value="t.code">{{ t.name }}{{ showCounts ? `（${counts[t.code] || 0}）` : '' }}</option></select></label>
    </div>
    <div class="library-filter-row"><span>一级分类</span><nav aria-label="一级分类"><RouterLink :to="target()" :aria-current="!category ? 'true' : undefined">全部<small v-if="showCounts">{{ total }}</small></RouterLink><RouterLink v-for="c in categories" :key="c.id" :to="target(c.code)" :aria-current="category?.code === c.code ? 'true' : undefined">{{ c.name }}<small v-if="showCounts">{{ counts[c.code] || 0 }}</small></RouterLink></nav></div>
    <div class="library-filter-row"><span>二级主题</span><nav aria-label="二级主题"><RouterLink :to="target(category?.code)" :aria-current="!topicCode ? 'true' : undefined">全部主题</RouterLink><RouterLink v-for="t in category?.children || []" :key="t.id" :to="target(category.code, t.code)" :aria-current="topicCode === t.code ? 'true' : undefined">{{ t.name }}<small v-if="showCounts">{{ counts[t.code] || 0 }}</small></RouterLink><span v-if="!category" class="library-hint">选择一级分类后，可继续筛选主题</span></nav></div>
  </section>
</template>
