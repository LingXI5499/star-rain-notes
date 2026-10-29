<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import PublicPagination from '../../../../shared/ui/PublicPagination.vue'
import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
import { publicPage, publicPageSize } from '../../../../shared/composables/publicListState'
import EnglishLibraryFilters from '../../components/EnglishLibraryFilters.vue'
import EnglishLibraryWord from '../../components/EnglishLibraryWord.vue'
import { getLibraryTaxonomy, getLibraryCounts, getLibraryOverview, getLibraryWords } from '../../api/englishLibraryApi'
import { listReading, getVocabularyThemes } from '../../api/englishApi'
import { libraryPath, libraryQuery } from '../../lib/libraryNavigation'
import { getTopic } from '../../api/englishRwApi'
import '../../styles/englishLibrary.css'

const props = defineProps({ module: { type: String, default: 'vocabulary' } })
const route = useRoute(), router = useRouter()
const categories = ref([]), counts = ref({}), overview = ref({}), data = ref({ items: [], total: 0 }), error = ref(''), loading = ref(true), query = ref('')
const vocabulary = computed(() => props.module === 'vocabulary')
const title = computed(() => vocabulary.value ? '词汇' : '世界')
const category = computed(() => categories.value.find(c => c.code?.toLowerCase() === String(route.params.category || '').toLowerCase()))
const topic = computed(() => category.value?.children.find(t => t.code?.toLowerCase() === String(route.params.topic || '').toLowerCase()))
const page = computed(() => publicPage(route.query.page))
const size = computed(() => publicPageSize(route.query.size || route.query.pageSize, vocabulary.value ? 24 : 12, [12, 24, 48]))
const display = computed(() => ['en', 'zh'].includes(route.query.display) ? route.query.display : 'all')
const total = computed(() => Number(vocabulary.value ? overview.value.confirmedRecords : overview.value.worldArticles) || 0)
const legacyTheme=ref(null)
const sourceView=computed(()=>Boolean(route.params.themeId)||Boolean(route.query.status && route.query.status!=='confirmed'))
const resultsTitle=computed(()=>legacyTheme.value?.name || (route.query.status==='all'?'全部来源记录':route.query.status==='needs-review'?'分类待复核':route.query.status==='unclassified'?'未分类记录':topic.value?.name || category.value?.name || (vocabulary.value?'全部词汇':'全部文章')))
let generation = 0, loadedModule = ''
async function load() {
  const current = ++generation
  loading.value = true; error.value = ''; query.value = String(route.query.q || route.query.search || '')
  try {
    if(route.params.slug && route.path.startsWith('/english/topics/')){
      const node=await getTopic(route.params.slug)
      if(current!==generation)return
      if(node.libraryModule!=='world'||!node.code)throw Object.assign(new Error('该主题已停止公开。'),{libraryValidation:true})
      await router.replace({path:libraryPath('world',node.code.split('-')[0],node.parentId?node.code:''),query:route.query,hash:route.hash})
      return
    }
    try { libraryPath(props.module, String(route.params.category || ''), String(route.params.topic || '')) }
    catch { throw Object.assign(new Error('分类路径无效。'), { libraryValidation: true }) }
    if (loadedModule !== props.module) {
      const [tree, totals, stats] = await Promise.all([getLibraryTaxonomy(props.module), getLibraryCounts(props.module), getLibraryOverview()])
      if (current !== generation) return
      categories.value = tree; counts.value = totals; overview.value = stats; loadedModule = props.module
    }
    if ((route.params.category && !category.value) || (route.params.topic && !topic.value)) throw Object.assign(new Error('分类或主题不存在。'), { libraryValidation: true })
    if(route.params.themeId){const themes=await getVocabularyThemes();if(current!==generation)return;legacyTheme.value=themes.find(t=>String(t.id)===String(route.params.themeId));if(!legacyTheme.value)throw Object.assign(new Error('来源主题不存在。'),{libraryValidation:true})}else legacyTheme.value=null
    const result = vocabulary.value
      ? await getLibraryWords({ q: query.value, category: category.value?.code, topic: topic.value?.code, status: route.params.themeId || route.query.status==='all' ? '' : String(route.query.status || 'confirmed'), theme: route.params.themeId || undefined, page: page.value, size: size.value })
      : await listReading({ search: query.value, topicId: (topic.value || category.value)?.id, page: page.value, size: size.value })
    if (current === generation) {
      data.value = result
      const last = Math.max(1, Math.ceil(result.total / size.value))
      if (page.value > last) await router.replace({ query: { ...route.query, page: last > 1 ? String(last) : undefined } })
    }
  } catch (cause) { if (current === generation) { data.value = { items: [], total: 0 }; error.value = cause.libraryValidation ? cause.message : errorMessage(cause) } }
  finally { if (current === generation) loading.value = false }
}
watch([() => props.module, () => route.path, ...['q', 'search', 'status', 'page', 'size', 'pageSize'].map(key => () => route.query[key])], load, { immediate: true })
function search(value) { router.push({ query: libraryQuery(route.query, { q: value || undefined, search: undefined }) }) }
function setDisplay(value) { router.replace({ query: { ...route.query, display: value === 'all' ? undefined : value } }) }
</script>
<template>
  <section class="english-library" :aria-busy="loading">
    <nav class="library-crumbs" aria-label="面包屑"><RouterLink to="/english">英语</RouterLink><span>/</span><RouterLink :to="libraryPath(module)">{{ title }}</RouterLink><template v-if="category"><span>/</span><RouterLink :to="libraryPath(module, category.code)">{{ category.name }}</RouterLink></template><template v-if="topic"><span>/</span><span>{{ topic.name }}</span></template></nav>
    <header class="library-heading"><p class="public-eyebrow">{{ vocabulary ? 'VOCABULARY' : 'WORLD · LANGUAGE & KNOWLEDGE' }}</p><h1>{{ title }}</h1><p>{{ vocabulary ? '按分类与主题筛选，直接浏览单词、音标和释义。' : '通过英语阅读科学、技术、社会与生活，让语言连接更大的世界。' }}</p></header>
    <EnglishLibraryFilters :module="module" :categories="categories" :counts="counts" :total="total" />
    <h2 class="library-results-title">{{ resultsTitle }}</h2>
    <p v-if="sourceView" class="library-count">来源记录完整保留，未确认分类的词条仍可在这里浏览。</p>
    <div class="library-tools"><PublicSearch v-model="query" :label="vocabulary ? '搜索单词或释义' : '搜索文章标题'" :placeholder="vocabulary ? '单词、中文释义或语境…' : '输入文章标题…'" @search="search" /><div v-if="vocabulary" class="library-display" role="group" aria-label="词卡显示语言"><button v-for="option in [{value:'all',label:'全部'},{value:'en',label:'英文'},{value:'zh',label:'中文'}]" :key="option.value" type="button" :aria-pressed="display === option.value" @click="setDisplay(option.value)">{{ option.label }}</button></div></div>
    <p class="library-count" role="status" aria-live="polite">{{ data.total.toLocaleString() }} {{ vocabulary ? '条记录' : '篇已发布文章' }}</p>
    <p v-if="error" class="library-empty" role="alert">{{ error }} <button type="button" @click="load">重新加载</button></p>
    <div v-else-if="loading" class="library-word-grid" aria-label="正在加载"><div v-for="n in 6" :key="n" class="library-skeleton" /></div>
    <template v-else>
      <div v-if="vocabulary" class="library-word-grid"><EnglishLibraryWord v-for="word in data.items" :key="word.id" :word="word" :display="display" /></div>
      <div v-else class="library-article-grid"><article v-for="article in data.items" :key="article.id"><time v-if="article.publishedAt">{{ article.publishedAt.slice(0,10) }}</time><h3><RouterLink :to="'/english/world/articles/' + article.slug">{{ article.title }}</RouterLink></h3><p>{{ article.summary }}</p></article></div>
      <p v-if="!data.items.length" class="library-empty" role="status">{{ query ? (vocabulary ? '没有符合条件的词汇。' : '没有匹配的文章。') : vocabulary ? '词条整理中，你可以切换分类或浏览保留的来源记录。' : '该主题正在整理内容。文章发布后会出现在这里。' }}</p>
      <PublicPagination v-if="data.total > 0" :page="page" :page-size="size" :total="data.total" :loading="loading" :unit="vocabulary ? '条记录' : '篇文章'" @change="router.push({query:{...route.query,page:$event > 1 ? String($event) : undefined}})" @page-size="router.push({query:libraryQuery(route.query,{size:String($event),pageSize:undefined})})" />
    </template>
    <details v-if="vocabulary" class="library-legacy"><summary>来源记录与学习入口</summary><p>{{ categories.length }} 个一级分类，{{ categories.reduce((n,c) => n + c.children.length,0) }} 个二级主题。来源记录共 {{ Number(overview.vocabularyRecords || 0).toLocaleString() }} 条，未确认的词条保留原记录，不会自动进入主题目录。</p><div class="library-links"><RouterLink :to="{path:'/english/vocabulary',query:{status:'all'}}">全部来源记录</RouterLink><RouterLink :to="{path:'/english/vocabulary',query:{status:'needs-review'}}">分类待复核（{{ overview.reviewRecords || 0 }}）</RouterLink><RouterLink :to="{path:'/english/vocabulary',query:{status:'unclassified'}}">未分类（{{ overview.unclassifiedRecords || 0 }}）</RouterLink><RouterLink to="/english/vocabulary/study">今日背单词</RouterLink><RouterLink to="/english/vocabulary/plan">学习计划</RouterLink><RouterLink to="/english/vocabulary/progress">学习进度</RouterLink></div></details>
  </section>
</template>
