<script setup>
import { onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, RouterLink, useRoute } from 'vue-router'
import { getAdminChapter, getAdminTutorial } from '../../api/tutorialApi'
import ChapterExercises from '../../components/ChapterExercises.vue'
import ChapterAuthorNav from '../../components/ChapterAuthorNav.vue'
import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
const props = defineProps({ kind: { type: String, required: true } })
const route = useRoute()
const chapter = ref(null), tutorial = ref(null), loading = ref(true), error = ref('')
const exercises = ref(null), confirmDialog = ref(null)
async function load() {
  loading.value = true; error.value = ''; chapter.value = null; tutorial.value = null
  try {
    const [chapterRow, tutorialRow] = await Promise.all([getAdminChapter(route.params.chapterId), getAdminTutorial(route.params.tutorialId)])
    if (String(chapterRow.tutorialId) !== String(tutorialRow.id)) { error.value = '章节不属于此教程'; return }
    chapter.value = chapterRow; tutorial.value = tutorialRow
  } catch (e) { error.value = errorMessage(e) } finally { loading.value = false }
}
function confirmLeave() { return !exercises.value?.isDirty?.() || confirmDialog.value.ask('卡片或问题尚未保存，离开后改动会丢失。确定离开吗？') }
onMounted(load)
watch(() => `${route.params.tutorialId}:${route.params.chapterId}`, load)
onBeforeRouteLeave(confirmLeave)
onBeforeRouteUpdate(confirmLeave)
</script>
<template>
 <main class="page-container chapter-exercises-page">
  <header><div><p>CHAPTER CONTENT · 学习内容</p><h1>{{ kind === 'cards' ? '知识卡片' : '章节问题' }}</h1><p v-if="chapter">{{ tutorial.title }} / {{ chapter.title }}</p></div><nav aria-label="课程导航"><RouterLink :to="{ path: accountPath(`/tutorials/${route.params.tutorialId}/curriculum`), query: { group: chapter?.groupId } }">返回课程结构</RouterLink></nav></header>
  <ChapterAuthorNav :tutorial-id="route.params.tutorialId" :chapter-id="route.params.chapterId" :active="kind" />
  <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="loading" role="status">正在读取章节…</p>
  <ChapterExercises v-else-if="chapter" :key="`${chapter.id}:${kind}`" ref="exercises" :chapter-id="chapter.id" :kind="kind" />
  <AdminConfirmDialog ref="confirmDialog" />
 </main>
</template>
<style scoped>
.chapter-exercises-page{max-width:1200px}.chapter-exercises-page>header{display:flex;align-items:flex-start;justify-content:space-between;gap:24px;margin-bottom:24px}.chapter-exercises-page h1{font-size:30px;margin:8px 0}.chapter-exercises-page p{color:var(--text-secondary);line-height:1.7}.chapter-exercises-page header>div>p:first-child{font-size:11px;letter-spacing:.12em;color:var(--accent)}.chapter-exercises-page nav{display:flex;flex-wrap:wrap;gap:16px;padding-top:12px}.chapter-exercises-page nav a{color:var(--primary);white-space:nowrap}@media(max-width:760px){.chapter-exercises-page>header{flex-direction:column;gap:8px}}
</style>
