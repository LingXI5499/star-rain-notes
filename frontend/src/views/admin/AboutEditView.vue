<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import {
  fetchAdminAbout,
  updateAbout,
  updateSelectedContent,
  type AdminAbout,
} from '@/api/about'
import { fetchAdminTutorials } from '@/api/tutorial'
import { fetchAdminPosts } from '@/api/blog'
import { fetchAdminProjects } from '@/api/portfolio'
import MediaField from '@/components/ui/MediaField.vue'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'

interface Option {
  id: number
  label: string
}

const loading = ref(true)
const saving = ref(false)
const stageSaving = ref(false)

const form = reactive({
  displayName: '',
  headline: '',
  bio: '',
  avatarMediaId: null as number | null,
  githubUrl: '',
  publicEmail: '',
  resumeMediaId: null as number | null,
  currentFocus: [] as string[],
  technicalDirectionMarkdown: '',
  journeyMarkdown: '',
})

const tutorialOptions = ref<Option[]>([])
const blogOptions = ref<Option[]>([])
const projectOptions = ref<Option[]>([])

const selectedTutorialIds = ref<number[]>([])
const selectedBlogPostIds = ref<number[]>([])
const selectedPortfolioProjectIds = ref<number[]>([])

// Unsaved-changes guard + Ctrl/Cmd+S (TASK-011). Two independent forms:
// the profile form and the selected-content form.
const profileGuard = useUnsavedGuard(() => form, saveProfile)
const selectedGuard = useUnsavedGuard(
  () => [selectedTutorialIds.value, selectedBlogPostIds.value, selectedPortfolioProjectIds.value],
  saveSelected,
)

function addFocus() {
  form.currentFocus.push('')
}

function removeFocus(index: number) {
  form.currentFocus.splice(index, 1)
}

async function loadCandidates() {
  const [tutorials, blogs, projects] = await Promise.all([
    fetchAdminTutorials({ page: 1, pageSize: 50 }),
    fetchAdminPosts({ page: 1, pageSize: 50 }),
    fetchAdminProjects({ page: 1, pageSize: 50 }),
  ])
  tutorialOptions.value = tutorials.items.map((t) => ({ id: t.id, label: t.title }))
  blogOptions.value = blogs.items.map((b) => ({ id: b.id, label: b.title }))
  projectOptions.value = projects.items.map((p) => ({ id: p.id, label: p.title }))
}

onMounted(async () => {
  try {
    const [data] = await Promise.all([fetchAdminAbout(), loadCandidates()])
    form.displayName = data.displayName ?? ''
    form.headline = data.headline ?? ''
    form.bio = data.bio ?? ''
    form.avatarMediaId = data.avatarMediaId
    form.githubUrl = data.githubUrl ?? ''
    form.publicEmail = data.publicEmail ?? ''
    form.resumeMediaId = data.resumeMediaId
    form.currentFocus = [...data.currentFocus]
    form.technicalDirectionMarkdown = data.technicalDirectionMarkdown ?? ''
    form.journeyMarkdown = data.journeyMarkdown ?? ''
    selectedTutorialIds.value = [...data.selectedTutorialIds]
    selectedBlogPostIds.value = [...data.selectedBlogPostIds]
    selectedPortfolioProjectIds.value = [...data.selectedPortfolioProjectIds]
  } catch {
    ElMessage.error('加载失败。')
  } finally {
    loading.value = false
    profileGuard.capture()
    selectedGuard.capture()
  }
})

async function saveProfile() {
  saving.value = true
  try {
    await updateAbout({
      displayName: form.displayName || null,
      headline: form.headline || null,
      bio: form.bio || null,
      avatarMediaId: form.avatarMediaId,
      githubUrl: form.githubUrl || null,
      publicEmail: form.publicEmail || null,
      resumeMediaId: form.resumeMediaId,
      currentFocus: form.currentFocus.filter((f) => f.trim()),
      technicalDirectionMarkdown: form.technicalDirectionMarkdown || null,
      journeyMarkdown: form.journeyMarkdown || null,
    })
    profileGuard.capture()
    ElMessage.success('已保存。')
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function saveSelected() {
  stageSaving.value = true
  try {
    await updateSelectedContent({
      tutorialIds: selectedTutorialIds.value,
      blogPostIds: selectedBlogPostIds.value,
      portfolioProjectIds: selectedPortfolioProjectIds.value,
    })
    selectedGuard.capture()
    ElMessage.success('精选内容已保存。')
  } catch (error) {
    showError(error)
  } finally {
    stageSaving.value = false
  }
}

function showError(error: unknown) {
  const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
  ElMessage.error(problem?.detail ?? '保存失败。')
}
</script>

<template>
  <section class="about-admin">
    <h1 class="about-admin__title">关于管理</h1>

    <el-form v-loading="loading" label-position="top" class="about-admin__form" @submit.prevent="saveProfile">
      <h2 class="about-admin__section">个人资料</h2>
      <div class="about-admin__grid">
        <div>
          <el-form-item label="显示名称">
            <el-input v-model="form.displayName" maxlength="100" />
          </el-form-item>
          <el-form-item label="一句话介绍（Headline）">
            <el-input v-model="form.headline" maxlength="255" />
          </el-form-item>
          <el-form-item label="头像（从媒体库选择或上传，仅限图片）">
            <MediaField v-model="form.avatarMediaId" asset-type="IMAGE" empty-text="未设置头像" />
          </el-form-item>
          <el-form-item label="GitHub URL">
            <el-input v-model="form.githubUrl" maxlength="500" />
          </el-form-item>
        </div>
        <div>
          <el-form-item label="公开邮箱">
            <el-input v-model="form.publicEmail" maxlength="255" />
          </el-form-item>
          <el-form-item label="简历媒体 ID（必须 PDF DOCUMENT）">
            <el-input-number v-model="form.resumeMediaId" :min="1" :controls="false" placeholder="媒体库将在后续任务提供" />
          </el-form-item>
          <el-form-item label="当前关注">
            <div class="about-admin__focus">
              <div v-for="(_, index) in form.currentFocus" :key="index" class="about-admin__focus-row">
                <el-input v-model="form.currentFocus[index]" placeholder="例如：Java 后端" />
                <el-button @click="removeFocus(index)">删除</el-button>
              </div>
              <el-button @click="addFocus">添加关注</el-button>
            </div>
          </el-form-item>
        </div>
      </div>
      <el-form-item label="简介（Bio）">
        <el-input v-model="form.bio" type="textarea" :rows="3" maxlength="1000" />
      </el-form-item>
      <el-form-item label="技术方向（Markdown）">
        <el-input v-model="form.technicalDirectionMarkdown" type="textarea" :rows="6" />
      </el-form-item>
      <el-form-item label="经历（Markdown）">
        <el-input v-model="form.journeyMarkdown" type="textarea" :rows="6" />
      </el-form-item>
      <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
    </el-form>

    <el-form label-position="top" class="about-admin__form about-admin__selected" @submit.prevent="saveSelected">
      <h2 class="about-admin__section">精选内容（每种最多 3 个，顺序即展示顺序）</h2>
      <el-form-item label="精选教程">
        <el-select v-model="selectedTutorialIds" multiple style="width: 100%" placeholder="选择教程">
          <el-option v-for="option in tutorialOptions" :key="option.id" :label="option.label" :value="option.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="精选博客">
        <el-select v-model="selectedBlogPostIds" multiple style="width: 100%" placeholder="选择博客文章">
          <el-option v-for="option in blogOptions" :key="option.id" :label="option.label" :value="option.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="精选作品">
        <el-select v-model="selectedPortfolioProjectIds" multiple style="width: 100%" placeholder="选择作品">
          <el-option v-for="option in projectOptions" :key="option.id" :label="option.label" :value="option.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" :loading="stageSaving" @click="saveSelected">保存精选内容</el-button>
    </el-form>
  </section>
</template>

<style scoped>
.about-admin__title {
  font-size: 28px;
  line-height: 36px;
  margin-bottom: var(--space-8);
}

.about-admin__section {
  font-size: 20px;
  line-height: 28px;
  margin: var(--space-6) 0 var(--space-4);
}

.about-admin__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 var(--space-8);
}

.about-admin__form {
  max-width: 900px;
}

.about-admin__selected {
  margin-top: var(--space-10);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border);
}

.about-admin__focus {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.about-admin__focus-row {
  display: flex;
  gap: var(--space-2);
}

@media (max-width: 900px) {
  .about-admin__grid {
    grid-template-columns: 1fr;
  }
}
</style>
