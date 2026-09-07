<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import { createProject, fetchAdminProject, updateProject, type ProjectMediaItem } from '@/api/portfolio'
import type { MediaAsset } from '@/api/media'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MediaPicker from '@/components/MediaPicker.vue'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'

type GalleryDraft = {
  id: number | null
  mediaAssetId: number
  url: string
  title: string
  description: string
  altText: string
  deviceType: string
  sortOrder: number
}

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => typeof route.params.id === 'string')

const loading = ref(true)
const saving = ref(false)
const mediaPickerOpen = ref(false)
const pickerMode = ref<'cover' | 'gallery'>('cover')
const coverUrl = ref<string | null>(null)
const gallery = ref<GalleryDraft[]>([])

const form = reactive({
  title: '',
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
  coverMediaId: null as number | null,
})

const { capture } = useUnsavedGuard(() => ({ ...form, gallery: gallery.value }), save)

function toDraft(item: ProjectMediaItem): GalleryDraft {
  return {
    id: item.id,
    mediaAssetId: item.mediaAssetId,
    url: item.url,
    title: item.title ?? '',
    description: item.description ?? '',
    altText: item.altText ?? '',
    deviceType: item.deviceType || 'DESKTOP',
    sortOrder: item.sortOrder ?? 0,
  }
}

onMounted(async () => {
  try {
    if (isEdit.value) {
      const detail = await fetchAdminProject(Number(route.params.id))
      Object.assign(form, {
        title: detail.title,
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
        coverMediaId: detail.coverMediaId,
      })
      coverUrl.value = detail.coverUrl
      gallery.value = (detail.gallery ?? []).map(toDraft)
    } else {
      form.techStack = []
      gallery.value = []
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
      coverMediaId: form.coverMediaId,
      gallery: gallery.value.map((item, index) => ({
        id: item.id,
        mediaAssetId: item.mediaAssetId,
        title: item.title || null,
        description: item.description || null,
        altText: item.altText || null,
        deviceType: item.deviceType || 'DESKTOP',
        sortOrder: index * 10,
      })),
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

function openCoverPicker() {
  pickerMode.value = 'cover'
  mediaPickerOpen.value = true
}

function openGalleryPicker() {
  pickerMode.value = 'gallery'
  mediaPickerOpen.value = true
}

function selectMedia(asset: MediaAsset) {
  if (asset.assetType !== 'IMAGE') {
    ElMessage.warning('只能选择图片。')
    return
  }
  if (pickerMode.value === 'cover') {
    form.coverMediaId = asset.id
    coverUrl.value = asset.publicUrl
    return
  }
  gallery.value.push({
    id: null,
    mediaAssetId: asset.id,
    url: asset.publicUrl,
    title: '',
    description: '',
    altText: '',
    deviceType: 'DESKTOP',
    sortOrder: gallery.value.length * 10,
  })
}

function clearCover() {
  form.coverMediaId = null
  coverUrl.value = null
}

function removeGalleryItem(index: number) {
  gallery.value.splice(index, 1)
}

function moveGalleryItem(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= gallery.value.length) return
  const copy = [...gallery.value]
  const [row] = copy.splice(index, 1)
  copy.splice(target, 0, row)
  gallery.value = copy
}
</script>

<template>
  <section class="portfolio-edit">
    <!-- top action bar -->
    <div class="portfolio-edit__topbar">
      <div>
        <p class="portfolio-edit__eyebrow">CASE STUDY WORKSPACE · 作品</p>
        <h1 class="portfolio-edit__title">{{ isEdit ? '编辑作品' : '新建作品' }}</h1>
        <p>组织项目叙事、技术栈、封面与上线信息。</p>
      </div>
      <div class="portfolio-edit__topbar-actions">
        <el-button @click="router.push({ name: 'admin-portfolio' })">取消</el-button>
        <el-button :loading="saving" type="primary" @click="save">保存作品</el-button>
      </div>
    </div>

    <el-form v-loading="loading" label-position="top" class="portfolio-edit__form" @submit.prevent="save">
      <section class="portfolio-edit__writing-card">
        <el-input
          v-model="form.title"
          class="portfolio-edit__title-input"
          placeholder="输入作品标题"
          maxlength="200"
        />
        <p class="portfolio-edit__outline-note">正文请从 H2 开始；前台右侧目录固定收录 H2–H4。</p>
        <MarkdownEditor v-model="form.bodyMarkdown" placeholder="Background / Goals / Architecture / Challenges / Results…" />
      </section>

      <!-- bottom: project details -->
      <div class="portfolio-edit__meta">
        <div class="portfolio-edit__section-head"><div><small>PROJECT SETTINGS</small><h2>作品信息</h2></div><span>用案例叙事呈现完整工程过程</span></div>
        <div class="portfolio-edit__meta-grid">
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
            <el-select v-model="form.techStack" multiple filterable allow-create default-first-option placeholder="输入技术名称后回车" style="width:100%" />
          </el-form-item>
          <el-form-item label="作品封面" class="portfolio-edit__cover-field">
            <div class="portfolio-edit__cover" :class="{ 'portfolio-edit__cover--empty': !coverUrl }">
              <img v-if="coverUrl" :src="coverUrl" alt="作品封面预览" />
              <div v-else><strong>作</strong><span>建议使用 16:9 项目截图</span></div>
            </div>
            <div class="portfolio-edit__cover-actions">
              <el-button @click="openCoverPicker">选择封面</el-button>
              <el-button v-if="form.coverMediaId" type="danger" plain @click="clearCover">移除</el-button>
            </div>
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="1000" />
          </el-form-item>
        </div>
      </div>

      <div class="portfolio-edit__meta portfolio-edit__gallery">
        <div class="portfolio-edit__section-head">
          <div><small>PROJECT GALLERY</small><h2>项目截图</h2></div>
          <el-button type="primary" plain @click="openGalleryPicker">添加截图</el-button>
        </div>
        <p class="portfolio-edit__outline-note">手动切换、不自动播放。可为每张图填写标题与说明。</p>
        <div v-if="!gallery.length" class="portfolio-edit__gallery-empty">尚未添加截图。</div>
        <div v-for="(item, index) in gallery" :key="`${item.mediaAssetId}-${index}`" class="portfolio-edit__gallery-item">
          <img :src="item.url" :alt="item.altText || item.title || `截图 ${index + 1}`" />
          <div class="portfolio-edit__gallery-fields">
            <el-input v-model="item.title" placeholder="标题，如：教程阅读页" maxlength="200" />
            <el-input v-model="item.description" type="textarea" :rows="2" placeholder="说明" maxlength="1000" />
            <el-input v-model="item.altText" placeholder="ALT 文本" maxlength="300" />
            <div class="portfolio-edit__gallery-actions">
              <el-select v-model="item.deviceType" style="width: 140px">
                <el-option label="Desktop" value="DESKTOP" />
                <el-option label="Mobile" value="MOBILE" />
                <el-option label="Tablet" value="TABLET" />
              </el-select>
              <el-button @click="moveGalleryItem(index, -1)" :disabled="index === 0">上移</el-button>
              <el-button @click="moveGalleryItem(index, 1)" :disabled="index === gallery.length - 1">下移</el-button>
              <el-button type="danger" plain @click="removeGalleryItem(index)">删除</el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="portfolio-edit__actions">
        <el-button @click="router.push({ name: 'admin-portfolio' })">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存作品</el-button>
      </div>
    </el-form>
    <MediaPicker v-model="mediaPickerOpen" @select="selectMedia" />
  </section>
</template>

<style scoped>
.portfolio-edit__topbar {
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

.portfolio-edit__eyebrow { margin-bottom: 3px; color: var(--accent); font-size: 10px; font-weight: 750; letter-spacing: .15em; }
.portfolio-edit__title { font-size: 26px; line-height: 32px; }
.portfolio-edit__topbar p:last-child { color: var(--text-muted); font-size: 12px; }
.portfolio-edit__topbar-actions { display:flex; flex-shrink:0; gap:var(--space-2); }
.portfolio-edit__writing-card,.portfolio-edit__meta { border:1px solid var(--border); border-radius:20px; background:var(--bg-surface); box-shadow:0 18px 45px rgb(0 0 0/.035); }
.portfolio-edit__writing-card { padding:var(--space-5); }

.portfolio-edit__title-input {
  margin-bottom: var(--space-2);
}

.portfolio-edit__outline-note { margin-bottom:var(--space-4); color:var(--text-muted); font-size:12px; }

.portfolio-edit__title-input :deep(.el-input__inner) {
  font-size: 26px;
  font-weight: 600;
  height: 52px;
  line-height: 52px;
}

/* bottom meta section */
.portfolio-edit__meta {
  margin-top: var(--space-8);
  padding: var(--space-6);
}

.portfolio-edit__section-head { display:flex; align-items:end; justify-content:space-between; gap:var(--space-4); margin-bottom:var(--space-5); }
.portfolio-edit__section-head small { color:var(--accent); font-size:10px; font-weight:750; letter-spacing:.14em; }
.portfolio-edit__section-head h2 { font-size:22px; }
.portfolio-edit__section-head > span { color:var(--text-muted); font-size:12px; }

.portfolio-edit__meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0 var(--space-6);
}

.portfolio-edit__cover-field { grid-column:span 2; }
.portfolio-edit__cover { width:100%; aspect-ratio:16/9; display:grid; overflow:hidden; place-items:center; border:1px solid var(--border); border-radius:14px; background:var(--bg-subtle); }
.portfolio-edit__cover img { width:100%; height:100%; object-fit:cover; }
.portfolio-edit__cover > div { display:grid; place-items:center; color:var(--text-muted); }
.portfolio-edit__cover strong { color:var(--primary); font:700 42px/1 Georgia,serif; }
.portfolio-edit__cover span { font-size:12px; }
.portfolio-edit__cover-actions { display:flex; gap:var(--space-2); margin-top:var(--space-3); }

.portfolio-edit__gallery { margin-top: var(--space-6); }
.portfolio-edit__gallery-empty { color: var(--text-muted); font-size: 13px; padding: 12px 0; }
.portfolio-edit__gallery-item {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: var(--space-4);
  padding: 14px 0;
  border-top: 1px solid var(--border);
}
.portfolio-edit__gallery-item img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid var(--border);
}
.portfolio-edit__gallery-fields { display: grid; gap: 8px; }
.portfolio-edit__gallery-actions { display: flex; flex-wrap: wrap; gap: 8px; }

.portfolio-edit__actions {
  display:flex;
  justify-content:flex-end;
  gap:var(--space-2);
  margin-top: var(--space-6);
}

@media (max-width: 1100px) {
  .portfolio-edit__meta-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) { .portfolio-edit__topbar { align-items:flex-start; } .portfolio-edit__topbar p:last-child,.portfolio-edit__section-head > span { display:none; } .portfolio-edit__topbar-actions { flex-direction:column-reverse; } .portfolio-edit__writing-card,.portfolio-edit__meta { padding:var(--space-4); border-radius:15px; } .portfolio-edit__cover-field { grid-column:auto; } }
</style>
