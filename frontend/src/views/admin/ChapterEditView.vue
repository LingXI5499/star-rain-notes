<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import type { ContentReview } from '@/api/account'
import {
  createChapter,
  fetchAdminCurriculum,
  fetchChapter,
  updateChapter,
  type AdminCurriculumGroup,
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
 * The group is chosen at creation only; moving chapters between groups is an
 * explicit, confirmed action on the course structure page.
 */
const route = useRoute()
const router = useRouter()

const tutorialId = Number(route.params.id)
const chapterId = typeof route.params.chapterId === 'string' ? Number(route.params.chapterId) : null
const isEdit = computed(() => chapterId !== null)

const loading = ref(true)
const saving = ref(false)
const tutorialTitle = ref('')
const groups = ref<AdminCurriculumGroup[]>([])

const form = reactive({
  title: '',
  groupId: null as number | null,
  summary: '',
  bodyMarkdown: '',
})

// Unsaved-changes guard + Ctrl/Cmd+S (same as blog / portfolio editors).
const { capture } = useUnsavedGuard(() => form, save)

const groupOptions = computed(() => groups.value.map((group) => ({
  id: group.id,
  label: `${group.title}（${group.chapterCount} 章）`,
})))

function isContentReview(value: unknown): value is ContentReview {
  return typeof value === 'object' && value !== null && 'contentType' in value && 'status' in value
}

onMounted(async () => {
  try {
    const curriculum = await fetchAdminCurriculum(tutorialId)
    tutorialTitle.value = curriculum.tutorial.title
    groups.value = curriculum.groups
    if (isEdit.value && chapterId !== null) {
      const chapter = await fetchChapter(tutorialId, chapterId)
      form.title = chapter.title
      form.groupId = chapter.groupId
      form.summary = chapter.summary ?? ''
      form.bodyMarkdown = chapter.bodyMarkdown
    } else {
      const requestedGroupId = Number(route.query.group)
      if (Number.isInteger(requestedGroupId)
        && groupOptions.value.some((group) => group.id === requestedGroupId)) {
        form.groupId = requestedGroupId
      } else {
        form.groupId = groupOptions.value[0]?.id ?? null
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
  if (!form.title.trim() || !form.bodyMarkdown.trim() || !form.groupId) {
    ElMessage.warning('请填写标题、所属分组与正文。')
    return
  }
  saving.value = true
  try {
    if (isEdit.value && chapterId !== null) {
      // Group changes are handled by the explicit move action on the structure page.
      const result = await updateChapter(tutorialId, chapterId, {
        title: form.title,
        summary: form.summary || null,
        bodyMarkdown: form.bodyMarkdown,
      })
      if (isContentReview(result)) {
        capture()
        ElMessage.success('已提交审核，超级管理员批准后会应用到线上章节。')
        await backToWorkspace()
        return
      }
    } else {
      await createChapter(tutorialId, {
        title: form.title,
        groupId: form.groupId,
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
  await router.push({
    name: 'admin-tutorial-chapters',
    params: { id: String(tutorialId) },
    query: form.groupId ? { group: String(form.groupId) } : undefined,
  })
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
            教程工作台 › {{ tutorialTitle }} › 课程结构
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

      <!-- bottom: other meta info (parent / summary) -->
      <div class="chapter-edit__meta">
        <h2 class="chapter-edit__meta-title">章节信息</h2>
        <div class="chapter-edit__meta-grid">
          <el-form-item label="所属分组">
            <el-select v-model="form.groupId" placeholder="选择分组" style="width: 100%" :disabled="isEdit">
              <el-option v-for="option in groupOptions" :key="option.id" :label="option.label" :value="option.id" />
            </el-select>
            <p v-if="isEdit" class="chapter-edit__field-hint">如需换组，请返回课程结构页使用“移动到分组”。</p>
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

.chapter-edit__field-hint { margin-top: 7px; color: var(--text-muted); font-size: 11px; }

@media (max-width: 1100px) {
  .chapter-edit__meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
