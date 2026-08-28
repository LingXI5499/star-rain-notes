<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import { createPost, fetchAdminPost, fetchAdminTags, updatePost, type AdminBlogTag } from '@/api/blog'
import type { MediaAsset } from '@/api/media'
import BlogTagPicker from '@/components/BlogTagPicker.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MediaPicker from '@/components/MediaPicker.vue'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => typeof route.params.id === 'string')

const loading = ref(true)
const saving = ref(false)
const tags = ref<AdminBlogTag[]>([])
const mediaPickerOpen = ref(false)
const coverUrl = ref<string | null>(null)

const form = reactive({
  title: '',
  summary: '',
  bodyMarkdown: '',
  coverMediaId: null as number | null,
  tagValues: [] as Array<number | string>,
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
        summary: detail.summary,
        bodyMarkdown: detail.bodyMarkdown,
        coverMediaId: detail.coverMediaId,
        tagValues: detail.tags.map((t) => t.id),
      })
      coverUrl.value = detail.coverUrl
    }
  } catch {
    ElMessage.error('加载失败。')
  } finally {
    loading.value = false
    capture()
  }
})

async function save() {
  if (!form.title.trim() || !form.summary.trim() || !form.bodyMarkdown.trim()) {
    ElMessage.warning('请填写标题、摘要与正文。')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title,
      summary: form.summary,
      bodyMarkdown: form.bodyMarkdown,
      coverMediaId: form.coverMediaId,
      tagIds: form.tagValues.filter((value): value is number => typeof value === 'number'),
      tagNames: form.tagValues.filter((value): value is string => typeof value === 'string'),
    }
    if (isEdit.value) {
      const result = await updatePost(Number(route.params.id), payload)
      if ('contentType' in result && result.status === 'PENDING') {
        capture()
        ElMessage.success('已提交审核，超级管理员批准后会应用到线上文章。')
        await router.push({ name: 'admin-blog' })
        return
      }
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

function selectCover(asset: MediaAsset) {
  if (asset.assetType !== 'IMAGE') {
    ElMessage.warning('封面只能选择图片。')
    return
  }
  form.coverMediaId = asset.id
  coverUrl.value = asset.publicUrl
}

function clearCover() {
  form.coverMediaId = null
  coverUrl.value = null
}
</script>

<template>
  <section class="blog-edit">
    <div class="blog-edit__topbar">
      <div>
        <p class="blog-edit__eyebrow">EDITORIAL WORKSPACE · 博客</p>
        <h1 class="blog-edit__title">{{ isEdit ? '编辑文章' : '新建文章' }}</h1>
        <p>正文、标签、封面与搜索信息在同一工作流中完成。</p>
      </div>
      <div class="blog-edit__topbar-actions">
        <el-button @click="router.push({ name: 'admin-blog' })">取消</el-button>
        <el-button :loading="saving" type="primary" @click="save">保存文章</el-button>
      </div>
    </div>

    <el-form v-loading="loading" label-position="top" class="blog-edit__form" @submit.prevent="save">
      <section class="blog-edit__writing-card">
        <el-input
          v-model="form.title"
          class="blog-edit__title-input"
          placeholder="输入文章标题"
          maxlength="200"
        />
        <p class="blog-edit__outline-note">正文请从 H2 开始；前台右侧目录固定收录 H2–H4。</p>
        <MarkdownEditor v-model="form.bodyMarkdown" placeholder="从 H2 开始撰写正文…" />
      </section>

      <section class="blog-edit__meta">
        <div class="blog-edit__section-head">
          <div><small>ARTICLE SETTINGS</small><h2>文章信息</h2></div>
          <span>保存时自动解析新标签</span>
        </div>
        <div class="blog-edit__meta-grid">
          <div class="blog-edit__panel">
            <h3>发布信息</h3>
          <el-form-item label="标签">
              <BlogTagPicker v-model="form.tagValues" :tags="tags" />
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="1000" />
          </el-form-item>
          </div>

          <div class="blog-edit__panel">
            <h3>文章封面</h3>
            <div class="blog-edit__cover" :class="{ 'blog-edit__cover--empty': !coverUrl }">
              <img v-if="coverUrl" :src="coverUrl" alt="文章封面预览" />
              <div v-else><strong>封</strong><span>从媒体库选择 16:9 图片</span></div>
            </div>
            <div class="blog-edit__cover-actions">
              <el-button @click="mediaPickerOpen = true">选择封面</el-button>
              <el-button v-if="form.coverMediaId" type="danger" plain @click="clearCover">移除</el-button>
            </div>
          </div>

        </div>
      </section>

      <div class="blog-edit__actions">
        <el-button @click="router.push({ name: 'admin-blog' })">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存文章</el-button>
      </div>
    </el-form>
    <MediaPicker v-model="mediaPickerOpen" @select="selectCover" />
  </section>
</template>

<style scoped>
.blog-edit__topbar {
  position: sticky;
  top: 0;
  z-index: 12;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-5);
  margin: -8px -12px var(--space-6);
  padding: 14px 12px;
  border-bottom: 1px solid color-mix(in srgb,var(--border) 78%,transparent);
  background: color-mix(in srgb,var(--bg-page) 88%,transparent);
  backdrop-filter: blur(16px);
}

.blog-edit__eyebrow { margin-bottom: 3px; color: var(--accent); font-size: 10px; font-weight: 750; letter-spacing: .15em; }
.blog-edit__title { font-size: 26px; line-height: 32px; }
.blog-edit__topbar p:last-child { color: var(--text-muted); font-size: 12px; }
.blog-edit__topbar-actions { display: flex; flex-shrink: 0; gap: var(--space-2); }

.blog-edit__writing-card,
.blog-edit__meta { border: 1px solid var(--border); border-radius: 20px; background: var(--bg-surface); box-shadow: 0 18px 45px rgb(0 0 0 / .035); }
.blog-edit__writing-card { padding: var(--space-5); }

.blog-edit__title-input {
  margin-bottom: var(--space-2);
}

.blog-edit__title-input :deep(.el-input__inner) {
  font-size: 26px;
  font-weight: 600;
  height: 52px;
  line-height: 52px;
}

.blog-edit__outline-note { margin-bottom: var(--space-4); color: var(--text-muted); font-size: 12px; }

.blog-edit__meta {
  margin-top: var(--space-8);
  padding: var(--space-6);
}

.blog-edit__section-head { display: flex; align-items: end; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-5); }
.blog-edit__section-head small { color: var(--accent); font-size: 10px; font-weight: 750; letter-spacing: .14em; }
.blog-edit__section-head h2 { font-size: 22px; }
.blog-edit__section-head > span { color: var(--text-muted); font-size: 12px; }

.blog-edit__meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

.blog-edit__panel { min-width: 0; padding: var(--space-5); border: 1px solid var(--border); border-radius: 16px; background: color-mix(in srgb,var(--bg-subtle) 55%,var(--bg-surface)); }
.blog-edit__panel h3 { margin-bottom: var(--space-4); color: var(--text-primary); font-size: 15px; }
.blog-edit__cover { aspect-ratio: 16/9; display: grid; overflow: hidden; place-items: center; border: 1px solid var(--border); border-radius: 14px; background: var(--bg-subtle); }
.blog-edit__cover img { width: 100%; height: 100%; object-fit: cover; }
.blog-edit__cover > div { display: grid; place-items: center; color: var(--text-muted); }
.blog-edit__cover strong { color: var(--primary); font-size: 42px; font-family: Georgia,serif; }
.blog-edit__cover span { font-size: 12px; }
.blog-edit__cover-actions { display: flex; gap: var(--space-2); margin-top: var(--space-3); }

.blog-edit__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  margin-top: var(--space-6);
}

@media (max-width: 1100px) {
  .blog-edit__meta-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) { .blog-edit__topbar { align-items: flex-start; } .blog-edit__topbar p:last-child,.blog-edit__section-head > span { display: none; } .blog-edit__topbar-actions { flex-direction: column-reverse; } .blog-edit__writing-card,.blog-edit__meta { padding: var(--space-4); border-radius: 15px; } }
</style>
