<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import { createPost, fetchAdminPost, fetchAdminTags, updatePost, type BlogTag } from '@/api/blog'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => typeof route.params.id === 'string')

const loading = ref(true)
const saving = ref(false)
const tags = ref<BlogTag[]>([])

const form = reactive({
  title: '',
  slug: '',
  summary: '',
  bodyMarkdown: '',
  seoTitle: '',
  seoDescription: '',
  tagIds: [] as number[],
})

// Unsaved-changes guard + Ctrl/Cmd+S (TASK-011). Dirty until the form
// matches the snapshot taken after load / after a successful save.
const { capture } = useUnsavedGuard(() => form, save)

onMounted(async () => {
  try {
    tags.value = await fetchAdminTags()
    if (isEdit.value) {
      const detail = await fetchAdminPost(Number(route.params.id))
      Object.assign(form, {
        title: detail.title,
        slug: detail.slug,
        summary: detail.summary,
        bodyMarkdown: detail.bodyMarkdown,
        seoTitle: detail.seoTitle ?? '',
        seoDescription: detail.seoDescription ?? '',
        tagIds: detail.tags.map((t) => t.id),
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
  if (!form.title.trim() || !form.slug.trim() || !form.summary.trim() || !form.bodyMarkdown.trim()) {
    ElMessage.warning('请填写标题、slug、摘要与正文。')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title,
      slug: form.slug,
      summary: form.summary,
      bodyMarkdown: form.bodyMarkdown,
      seoTitle: form.seoTitle || null,
      seoDescription: form.seoDescription || null,
      tagIds: form.tagIds,
    }
    if (isEdit.value) {
      await updatePost(Number(route.params.id), payload)
    } else {
      await createPost(payload)
    }
    capture()
    ElMessage.success('已保存。')
    await router.push({ name: 'admin-blog' })
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="blog-edit">
    <!-- top action bar -->
    <div class="blog-edit__topbar">
      <h1 class="blog-edit__title">{{ isEdit ? '编辑文章' : '新建文章' }}</h1>
      <div class="blog-edit__topbar-actions">
        <el-button :loading="saving" type="primary" @click="save">保存</el-button>
        <el-button @click="router.push({ name: 'admin-blog' })">取消</el-button>
      </div>
    </div>

    <el-form v-loading="loading" label-position="top" class="blog-edit__form" @submit.prevent="save">
      <!-- center: big title + body editor (CSDN-style) -->
      <el-input
        v-model="form.title"
        class="blog-edit__title-input"
        placeholder="输入文章标题"
        maxlength="200"
      />

      <MarkdownEditor v-model="form.bodyMarkdown" placeholder="从 H2 开始撰写正文…" />

      <!-- bottom: other meta info (SEO / tags / summary) -->
      <div class="blog-edit__meta">
        <h2 class="blog-edit__meta-title">文章信息</h2>
        <div class="blog-edit__meta-grid">
          <el-form-item label="Slug（小写 kebab-case）">
            <el-input v-model="form.slug" maxlength="150" />
          </el-form-item>
          <el-form-item label="标签">
            <el-select v-model="form.tagIds" multiple clearable placeholder="选择标签" style="width: 100%">
              <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="1000" />
          </el-form-item>
          <el-form-item label="SEO 标题">
            <el-input v-model="form.seoTitle" maxlength="200" />
          </el-form-item>
          <el-form-item label="SEO 描述">
            <el-input v-model="form.seoDescription" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
        </div>
      </div>

      <div class="blog-edit__actions">
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button @click="router.push({ name: 'admin-blog' })">取消</el-button>
      </div>
    </el-form>
  </section>
</template>

<style scoped>
.blog-edit__topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.blog-edit__title {
  font-size: 28px;
  line-height: 36px;
}

.blog-edit__title-input {
  margin-bottom: var(--space-5);
}

.blog-edit__title-input :deep(.el-input__inner) {
  font-size: 26px;
  font-weight: 600;
  height: 52px;
  line-height: 52px;
}

/* bottom meta section */
.blog-edit__meta {
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border);
}

.blog-edit__meta-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: var(--space-5);
  color: var(--text-secondary);
}

.blog-edit__meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0 var(--space-6);
}

.blog-edit__actions {
  margin-top: var(--space-6);
}

@media (max-width: 1100px) {
  .blog-edit__meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
