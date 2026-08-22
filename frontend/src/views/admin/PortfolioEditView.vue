<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import { createProject, fetchAdminProject, updateProject } from '@/api/portfolio'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => typeof route.params.id === 'string')

const loading = ref(true)
const saving = ref(false)

const form = reactive({
  title: '',
  slug: '',
  summary: '',
  role: '',
  techStack: [] as string[],
  bodyMarkdown: '',
  repositoryUrl: '',
  demoUrl: '',
  projectStatus: 'DEVELOPING',
  featured: false,
  sortOrder: 0,
  startedAt: null as string | null,
  completedAt: null as string | null,
  seoTitle: '',
  seoDescription: '',
})

// Unsaved-changes guard + Ctrl/Cmd+S (TASK-011).
const { capture } = useUnsavedGuard(() => form, save)

function addTech() {
  form.techStack.push('')
}

function removeTech(index: number) {
  form.techStack.splice(index, 1)
}

onMounted(async () => {
  try {
    if (isEdit.value) {
      const detail = await fetchAdminProject(Number(route.params.id))
      Object.assign(form, {
        title: detail.title,
        slug: detail.slug,
        summary: detail.summary,
        role: detail.role ?? '',
        techStack: [...detail.techStack],
        bodyMarkdown: detail.bodyMarkdown,
        repositoryUrl: detail.repositoryUrl ?? '',
        demoUrl: detail.demoUrl ?? '',
        projectStatus: detail.projectStatus,
        featured: detail.featured,
        sortOrder: detail.sortOrder,
        startedAt: detail.startedAt,
        completedAt: detail.completedAt,
        seoTitle: detail.seoTitle ?? '',
        seoDescription: detail.seoDescription ?? '',
      })
    } else {
      form.techStack = []
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
      role: form.role || null,
      techStack: form.techStack.filter((t) => t.trim()),
      bodyMarkdown: form.bodyMarkdown,
      repositoryUrl: form.repositoryUrl || null,
      demoUrl: form.demoUrl || null,
      projectStatus: form.projectStatus,
      featured: form.featured,
      sortOrder: form.sortOrder,
      startedAt: form.startedAt,
      completedAt: form.completedAt,
      seoTitle: form.seoTitle || null,
      seoDescription: form.seoDescription || null,
    }
    if (isEdit.value) {
      await updateProject(Number(route.params.id), payload)
    } else {
      await createProject(payload)
    }
    capture()
    ElMessage.success('已保存。')
    await router.push({ name: 'admin-portfolio' })
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="portfolio-edit">
    <!-- top action bar -->
    <div class="portfolio-edit__topbar">
      <h1 class="portfolio-edit__title">{{ isEdit ? '编辑作品' : '新建作品' }}</h1>
      <div class="portfolio-edit__topbar-actions">
        <el-button :loading="saving" type="primary" @click="save">保存</el-button>
        <el-button @click="router.push({ name: 'admin-portfolio' })">取消</el-button>
      </div>
    </div>

    <el-form v-loading="loading" label-position="top" class="portfolio-edit__form" @submit.prevent="save">
      <!-- center: big title + body editor (CSDN-style) -->
      <el-input
        v-model="form.title"
        class="portfolio-edit__title-input"
        placeholder="输入作品标题"
        maxlength="200"
      />

      <MarkdownEditor v-model="form.bodyMarkdown" placeholder="Background / Goals / Architecture / Challenges / Results…" />

      <!-- bottom: other meta info (status / links / stack / SEO) -->
      <div class="portfolio-edit__meta">
        <h2 class="portfolio-edit__meta-title">作品信息</h2>
        <div class="portfolio-edit__meta-grid">
          <el-form-item label="Slug（小写 kebab-case）">
            <el-input v-model="form.slug" maxlength="150" />
          </el-form-item>
          <el-form-item label="项目状态">
            <el-select v-model="form.projectStatus" style="width: 100%">
              <el-option label="开发中" value="DEVELOPING" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已上线" value="ONLINE" />
            </el-select>
          </el-form-item>
          <el-form-item label="角色（Role）">
            <el-input v-model="form.role" maxlength="200" />
          </el-form-item>
          <el-form-item label="精选（最多 3 个）">
            <el-switch v-model="form.featured" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :controls="false" />
          </el-form-item>
          <el-form-item label="开始日期">
            <el-date-picker v-model="form.startedAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="完成日期">
            <el-date-picker v-model="form.completedAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="代码仓库 URL">
            <el-input v-model="form.repositoryUrl" maxlength="500" />
          </el-form-item>
          <el-form-item label="在线演示 URL（ONLINE 必填）">
            <el-input v-model="form.demoUrl" maxlength="500" />
          </el-form-item>
          <el-form-item label="技术栈（≤20 项）">
            <div class="portfolio-edit__stack">
              <div v-for="(_, index) in form.techStack" :key="index" class="portfolio-edit__stack-row">
                <el-input v-model="form.techStack[index]" placeholder="例如：Java" />
                <el-button @click="removeTech(index)">删除</el-button>
              </div>
              <el-button @click="addTech">添加技术</el-button>
            </div>
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

      <div class="portfolio-edit__actions">
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button @click="router.push({ name: 'admin-portfolio' })">取消</el-button>
      </div>
    </el-form>
  </section>
</template>

<style scoped>
.portfolio-edit__topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.portfolio-edit__title {
  font-size: 28px;
  line-height: 36px;
}

.portfolio-edit__title-input {
  margin-bottom: var(--space-5);
}

.portfolio-edit__title-input :deep(.el-input__inner) {
  font-size: 26px;
  font-weight: 600;
  height: 52px;
  line-height: 52px;
}

/* bottom meta section */
.portfolio-edit__meta {
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border);
}

.portfolio-edit__meta-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: var(--space-5);
  color: var(--text-secondary);
}

.portfolio-edit__meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0 var(--space-6);
}

.portfolio-edit__stack {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.portfolio-edit__stack-row {
  display: flex;
  gap: var(--space-2);
}

.portfolio-edit__actions {
  margin-top: var(--space-6);
}

@media (max-width: 1100px) {
  .portfolio-edit__meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
