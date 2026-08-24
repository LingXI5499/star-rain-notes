<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import {
  createChapter,
  fetchAdminTutorial,
  fetchChapter,
  fetchTutorialNodes,
  updateChapter,
  type AdminTreeNode,
} from '@/api/tutorial'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'

/**
 * Dedicated full-page chapter editor (tutorial teaching content), mirroring
 * the blog and portfolio editors: meta fields, summary and a split body
 * editor with live MarkdownRenderer preview + image insertion.
 *
 * Routes: /admin/tutorials/:id/chapters/new  (create)
 *         /admin/tutorials/:id/chapters/:chapterId/edit (edit)
 * Parent group is chosen at creation only; moving chapters between groups
 * is handled by the chapter tree page (↑/↓ + tree structure).
 */
const route = useRoute()
const router = useRouter()

const tutorialId = Number(route.params.id)
const chapterId = typeof route.params.chapterId === 'string' ? Number(route.params.chapterId) : null
const isEdit = computed(() => chapterId !== null)

const loading = ref(true)
const saving = ref(false)
const tutorialTitle = ref('')
const groups = ref<AdminTreeNode[]>([])

const form = reactive({
  title: '',
  slug: '',
  parentId: null as number | null,
  summary: '',
  bodyMarkdown: '',
})

// Unsaved-changes guard + Ctrl/Cmd+S (same as blog / portfolio editors).
const { capture } = useUnsavedGuard(() => form, save)

const groupOptions = computed(() => {
  const out: { id: number; label: string }[] = []
  const walk = (nodes: AdminTreeNode[], depth: number) => {
    for (const node of nodes) {
      if (node.type === 'GROUP') {
        out.push({ id: node.id, label: `${'　'.repeat(depth)}${node.title}` })
        walk(node.children, depth + 1)
      } else {
        walk(node.children, depth + 1)
      }
    }
  }
  walk(groups.value, 0)
  return out
})

onMounted(async () => {
  try {
    const [detail, nodes] = await Promise.all([fetchAdminTutorial(tutorialId), fetchTutorialNodes(tutorialId)])
    tutorialTitle.value = detail.title
    groups.value = nodes
    if (isEdit.value && chapterId !== null) {
      const chapter = await fetchChapter(tutorialId, chapterId)
      form.title = chapter.title
      form.slug = chapter.slug
      form.parentId = chapter.parentId
      form.summary = chapter.summary ?? ''
      form.bodyMarkdown = chapter.bodyMarkdown
    } else {
      const requestedParentId = Number(route.query.parentId)
      if (Number.isInteger(requestedParentId)
        && groupOptions.value.some((group) => group.id === requestedParentId)) {
        form.parentId = requestedParentId
      }
    }
  } catch {
    ElMessage.error('加载失败。')
  } finally {
    loading.value = false
    capture()
  }
})

async function save() {
  if (!form.title.trim() || !form.slug.trim() || !form.bodyMarkdown.trim()) {
    ElMessage.warning('请填写标题、slug 与正文。')
    return
  }
  saving.value = true
  try {
    if (isEdit.value && chapterId !== null) {
      // parentId is only chosen at creation; tree moves handle regrouping.
      await updateChapter(tutorialId, chapterId, {
        title: form.title,
        slug: form.slug,
        summary: form.summary || null,
        bodyMarkdown: form.bodyMarkdown,
      })
    } else {
      await createChapter(tutorialId, {
        title: form.title,
        slug: form.slug,
        parentId: form.parentId,
        summary: form.summary || null,
        bodyMarkdown: form.bodyMarkdown,
      })
    }
    capture()
    ElMessage.success('已保存。')
    await backToWorkspace()
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}

async function backToWorkspace() {
  await router.push({ name: 'admin-tutorials', query: { tutorial: String(tutorialId) } })
}
</script>

<template>
  <section class="chapter-edit">
    <!-- top action bar -->
    <div class="chapter-edit__topbar">
      <div>
        <h1 class="chapter-edit__title">{{ isEdit ? '编辑章节' : '新建章节' }}</h1>
        <p class="chapter-edit__subtitle">
          <el-button
            link
            type="primary"
            @click="backToWorkspace"
          >
            {{ tutorialTitle }} › 章节管理
          </el-button>
        </p>
      </div>
      <div class="chapter-edit__topbar-actions">
        <el-button :loading="saving" type="primary" @click="save">保存</el-button>
        <el-button @click="backToWorkspace">
          取消
        </el-button>
      </div>
    </div>

    <el-form v-loading="loading" label-position="top" class="chapter-edit__form" @submit.prevent="save">
      <!-- center: big title + body editor (CSDN-style) -->
      <el-input
        v-model="form.title"
        class="chapter-edit__title-input"
        placeholder="输入章节标题"
        maxlength="200"
      />

      <MarkdownEditor v-model="form.bodyMarkdown" placeholder="从 H2 开始撰写正文…" />

      <!-- bottom: other meta info (slug / parent / summary) -->
      <div class="chapter-edit__meta">
        <h2 class="chapter-edit__meta-title">章节信息</h2>
        <div class="chapter-edit__meta-grid">
          <el-form-item label="Slug（小写 kebab-case）">
            <el-input v-model="form.slug" maxlength="150" />
          </el-form-item>
          <el-form-item v-if="!isEdit" label="父分组">
            <el-select v-model="form.parentId" placeholder="无（根级）" clearable style="width: 100%">
              <el-option v-for="option in groupOptions" :key="option.id" :label="option.label" :value="option.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="1000" />
          </el-form-item>
        </div>
      </div>

      <div class="chapter-edit__actions">
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button @click="backToWorkspace">
          取消
        </el-button>
      </div>
    </el-form>
  </section>
</template>

<style scoped>
.chapter-edit__topbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-6);
}

.chapter-edit__title {
  font-size: 28px;
  line-height: 36px;
}

.chapter-edit__subtitle {
  margin-top: var(--space-1);
}

.chapter-edit__title-input {
  margin-bottom: var(--space-5);
}

.chapter-edit__title-input :deep(.el-input__inner) {
  font-size: 26px;
  font-weight: 600;
  height: 52px;
  line-height: 52px;
}

/* bottom meta section */
.chapter-edit__meta {
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border);
}

.chapter-edit__meta-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: var(--space-5);
  color: var(--text-secondary);
}

.chapter-edit__meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0 var(--space-6);
}

.chapter-edit__actions {
  margin-top: var(--space-6);
}

@media (max-width: 1100px) {
  .chapter-edit__meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
