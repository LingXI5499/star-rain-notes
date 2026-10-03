<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { createTutorial, getAdminTutorial, listAdminCategories, updateTutorial } from '../../api/tutorialApi'
import { errorMessage } from '../../../../shared/http'
import AdminConfirmDialog from '../../../blog/components/admin/AdminConfirmDialog.vue'

const route = useRoute()
const router = useRouter()
const isCreate = computed(() => route.params.tutorialId === 'new')
const form = reactive({ categoryId: '', title: '', summary: '' })
const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const confirmDialog = ref(null)
let savedSnapshot = ''
let savedAndLeaving = false

const snapshot = () => JSON.stringify(form)
const dirty = () => savedSnapshot !== '' && savedSnapshot !== snapshot()

async function load() {
  loading.value = true
  error.value = ''
  try {
    categories.value = await listAdminCategories()
    if (isCreate.value) {
      const requested = String(route.query.categoryId || '')
      form.categoryId = categories.value.some((item) => String(item.id) === requested)
        ? requested : String(categories.value[0]?.id || '')
    } else {
      const tutorial = await getAdminTutorial(route.params.tutorialId)
      form.categoryId = String(tutorial.categoryId)
      form.title = tutorial.title
      form.summary = tutorial.summary || ''
    }
    savedSnapshot = snapshot()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.categoryId || !form.title.trim() || !form.summary.trim()) {
    error.value = '请填写知识体系、标题和摘要。'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const payload = {
      categoryId: form.categoryId,
      title: form.title.trim(),
      summary: form.summary.trim(),
    }
    const tutorial = isCreate.value
      ? await createTutorial(payload)
      : await updateTutorial(route.params.tutorialId, payload)
    savedSnapshot = snapshot()
    savedAndLeaving = true
    await router.push({ path: '/useradmin/tutorials/manage', query: { category: tutorial.categoryId } })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

async function leave() {
  await router.push('/useradmin/tutorials/manage')
}

function onKeydown(event) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault()
    if (!saving.value) save()
  }
}

onBeforeRouteLeave(async () => {
  if (savedAndLeaving || !dirty()) return true
  return confirmDialog.value.ask('教程信息尚未保存，离开后改动会丢失。确定离开吗？')
})
onMounted(() => { load(); window.addEventListener('keydown', onKeydown) })
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <main class="page-container tutorial-edit">
    <header class="tutorial-edit__heading">
      <h1>{{ isCreate ? '新建教程' : '编辑教程' }}</h1>
      <p>教程顺序请在教程工作台中直接拖动调整。</p>
    </header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading" class="loading" role="status">正在加载教程…</p>
    <form v-else class="surface-card tutorial-edit__form" @submit.prevent="save">
      <label>知识体系
        <select v-model="form.categoryId" required>
          <option value="" disabled>选择知识体系</option>
          <option v-for="category in categories" :key="category.id" :value="String(category.id)">{{ category.name }}</option>
        </select>
      </label>
      <label>标题<input v-model="form.title" maxlength="200" required placeholder="输入教程标题" /></label>
      <label>摘要<textarea v-model="form.summary" rows="3" maxlength="1000" required placeholder="概述这门教程的学习内容" /></label>
      <div class="tutorial-edit__actions">
        <button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存' }}</button>
        <button type="button" @click="leave">取消</button>
      </div>
    </form>
    <AdminConfirmDialog ref="confirmDialog" />
  </main>
</template>

<style scoped>
.tutorial-edit__heading h1 { font-size: 28px; line-height: 36px; }
.tutorial-edit__heading p { color: var(--text-muted); }
.tutorial-edit__form { display: grid; gap: 20px; max-width: 620px; }
.tutorial-edit__form textarea { width: 100%; padding: 12px; border: 1px solid var(--border-strong); border-radius: 10px; color: var(--text-primary); background: var(--bg-surface); font: inherit; resize: vertical; }
.tutorial-edit__actions { display: flex; gap: 10px; }
</style>
