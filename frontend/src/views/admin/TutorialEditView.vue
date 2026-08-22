<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import {
  createTutorial,
  fetchAdminTutorial,
  fetchCategoryTree,
  updateTutorial,
  type CategoryNode,
} from '@/api/tutorial'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => typeof route.params.id === 'string')

const categories = ref<CategoryNode[]>([])
const loading = ref(true)
const saving = ref(false)

const form = reactive({
  categoryId: null as number | null,
  title: '',
  slug: '',
  summary: '',
  coverMediaId: null as number | null,
  sortOrder: 0,
  seoTitle: '',
  seoDescription: '',
})

// Unsaved-changes guard + Ctrl/Cmd+S (TASK-011).
const { capture } = useUnsavedGuard(() => form, save)

function flatten(nodes: CategoryNode[]): CategoryNode[] {
  const out: CategoryNode[] = []
  for (const node of nodes) {
    out.push(node)
    out.push(...flatten(node.children))
  }
  return out
}

onMounted(async () => {
  try {
    categories.value = flatten(await fetchCategoryTree())
    if (isEdit.value) {
      const detail = await fetchAdminTutorial(Number(route.params.id))
      Object.assign(form, {
        categoryId: detail.categoryId,
        title: detail.title,
        slug: detail.slug,
        summary: detail.summary,
        coverMediaId: detail.coverMediaId,
        sortOrder: detail.sortOrder,
        seoTitle: detail.seoTitle ?? '',
        seoDescription: detail.seoDescription ?? '',
      })
    }
  } catch {
    ElMessage.error('加载失败。')
  } finally {
    loading.value = false
    capture()
  }
})

async function save() {
  if (!form.categoryId || !form.title.trim() || !form.slug.trim() || !form.summary.trim()) {
    ElMessage.warning('请填写分类、标题、slug 与摘要。')
    return
  }
  saving.value = true
  try {
    const payload = {
      categoryId: form.categoryId,
      title: form.title,
      slug: form.slug,
      summary: form.summary,
      coverMediaId: form.coverMediaId,
      sortOrder: form.sortOrder,
      seoTitle: form.seoTitle || null,
      seoDescription: form.seoDescription || null,
    }
    if (isEdit.value) {
      await updateTutorial(Number(route.params.id), payload)
    } else {
      await createTutorial(payload)
    }
    capture()
    ElMessage.success('已保存。')
    await router.push({ name: 'admin-tutorials' })
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="tutorial-edit">
    <h1 class="tutorial-edit__title">{{ isEdit ? '编辑教程' : '新建教程' }}</h1>

    <el-form v-loading="loading" label-position="top" class="tutorial-edit__form" @submit.prevent="save">
      <el-form-item label="分类" required>
        <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
          <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="form.title" maxlength="200" />
      </el-form-item>
      <el-form-item label="Slug（小写 kebab-case）">
        <el-input v-model="form.slug" maxlength="150" />
      </el-form-item>
      <el-form-item label="摘要">
        <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="1000" />
      </el-form-item>
      <el-form-item label="排序（数字，越小越靠前）">
        <el-input-number v-model="form.sortOrder" :controls="false" />
      </el-form-item>
      <el-form-item label="SEO 标题">
        <el-input v-model="form.seoTitle" maxlength="200" />
      </el-form-item>
      <el-form-item label="SEO 描述">
        <el-input v-model="form.seoDescription" type="textarea" :rows="2" maxlength="500" />
      </el-form-item>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      <el-button @click="router.push({ name: 'admin-tutorials' })">取消</el-button>
    </el-form>
  </section>
</template>

<style scoped>
.tutorial-edit__title {
  font-size: 28px;
  line-height: 36px;
  margin-bottom: var(--space-8);
}

.tutorial-edit__form {
  max-width: 560px;
}
</style>
