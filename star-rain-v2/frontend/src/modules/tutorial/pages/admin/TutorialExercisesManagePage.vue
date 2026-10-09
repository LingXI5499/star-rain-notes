<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getAdminCurriculum, listAdminTutorials } from '../../api/tutorialApi'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
const props = defineProps({ kind: { type: String, required: true } })
const route = useRoute(), router = useRouter()
const tutorials = ref([]), tutorialId = ref(String(route.query.tutorial || '')), curriculum = ref(null)
const groupId = ref(''), search = ref(''), loading = ref(true), error = ref('')
let loadVersion = 0
const chapters = computed(() => (curriculum.value?.groups || []).filter(g => !groupId.value || String(g.id) === groupId.value).flatMap(g => g.chapters.map(c => ({ ...c, groupTitle: g.title }))).filter(c => c.title.toLocaleLowerCase().includes(search.value.trim().toLocaleLowerCase())))
async function selectTutorial() {
 const version = ++loadVersion; curriculum.value = null; groupId.value = ''; error.value = ''
 if (!tutorialId.value) return
 loading.value = true
 try { const row = await getAdminCurriculum(tutorialId.value); if (version === loadVersion) { curriculum.value = row; await router.replace({ query: { tutorial: tutorialId.value } }) } }
 catch (e) { if (version === loadVersion) error.value = errorMessage(e) }
 finally { if (version === loadVersion) loading.value = false }
}
onMounted(async () => {
 try {
  let page = 1, total = 0
  do { const result = await listAdminTutorials({ page: page++, pageSize: 100 }); tutorials.value.push(...result.items); total = result.total } while (tutorials.value.length < total)
  if (!tutorials.value.some(t => String(t.id) === tutorialId.value)) tutorialId.value = ''
  if (tutorialId.value) await selectTutorial()
 } catch (e) { error.value = errorMessage(e) } finally { loading.value = false }
})
</script>
<template>
 <main class="page-container tutorial-exercises-manage">
  <header><p>LEARNING CONTENT WORKSPACE</p><h1>{{ kind === 'cards' ? '知识卡片管理' : '章节问题管理' }}</h1><p>{{ kind === 'cards' ? '选择章节，维护主动回忆的知识卡片。' : '选择章节，维护问题、参考答案和关联知识卡片。' }}</p></header>
  <div class="tutorial-exercises-manage__filters"><label>教程<select v-model="tutorialId" @change="selectTutorial"><option value="">请选择教程</option><option v-for="t in tutorials" :key="t.id" :value="String(t.id)">{{ t.title }}</option></select></label><label>分组<select v-model="groupId" :disabled="!curriculum"><option value="">所有分组</option><option v-for="g in curriculum?.groups || []" :key="g.id" :value="String(g.id)">{{ g.title }}</option></select></label><label>章节搜索<input v-model="search" type="search" placeholder="搜索章节标题" :disabled="!curriculum" /></label></div>
  <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在读取教程…</p>
  <p v-else-if="!tutorialId" class="tutorial-exercises-manage__empty">先选择一套教程。</p>
  <template v-else-if="curriculum"><div class="tutorial-exercises-manage__heading"><h2>{{ curriculum.tutorial.title }} · {{ chapters.length }} 章</h2><RouterLink :to="accountPath(`/tutorials/${tutorialId}/curriculum`)">管理课程结构 →</RouterLink></div><p v-if="!chapters.length" class="tutorial-exercises-manage__empty">暂无匹配章节。</p><article v-for="c in chapters" :key="c.id"><div><small>{{ c.groupTitle }}</small><h3>{{ c.title }}</h3><p>{{ c.status === 'PUBLISHED' ? '已公开' : c.status === 'WITHDRAWN' ? '已撤回' : '草稿' }}</p></div><nav><RouterLink :to="accountPath(`/tutorials/${tutorialId}/chapters/${c.id}`)">章节正文</RouterLink><RouterLink class="tutorial-exercises-manage__edit" :to="accountPath(`/tutorials/${tutorialId}/chapters/${c.id}/${kind}`)">{{ kind === 'cards' ? '编写知识卡片 →' : '编写章节问题 →' }}</RouterLink></nav></article></template>
 </main>
</template>
<style scoped>
.tutorial-exercises-manage{max-width:1200px}.tutorial-exercises-manage h1{font-size:32px;margin:8px 0}.tutorial-exercises-manage header p,.tutorial-exercises-manage article p,.tutorial-exercises-manage small{color:var(--text-secondary)}.tutorial-exercises-manage header>p:first-child{font-size:11px;letter-spacing:.12em;color:var(--accent)}.tutorial-exercises-manage__filters{display:grid;grid-template-columns:2fr 1fr 1fr;gap:20px;margin:28px 0}.tutorial-exercises-manage label{display:grid;gap:8px}.tutorial-exercises-manage__heading{display:flex;justify-content:space-between;gap:16px;margin:24px 0}.tutorial-exercises-manage article{display:flex;align-items:center;justify-content:space-between;gap:24px;padding:22px;margin:12px 0;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}.tutorial-exercises-manage article h3{margin:5px 0}.tutorial-exercises-manage article nav{display:flex;gap:18px;white-space:nowrap}.tutorial-exercises-manage a{color:var(--primary)}.tutorial-exercises-manage__edit{font-weight:650}.tutorial-exercises-manage__empty{padding:48px 24px;text-align:center;color:var(--text-muted)}@media(max-width:760px){.tutorial-exercises-manage__filters{grid-template-columns:1fr}.tutorial-exercises-manage article{align-items:flex-start;flex-direction:column}}
</style>
