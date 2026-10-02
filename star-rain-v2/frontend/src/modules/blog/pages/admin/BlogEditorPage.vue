<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createPost, deletePost, getAdminPost, listAdminTags, previewPost,
  publishPost, restorePost, updatePost, updatePostBody, withdrawPost,
} from '../../api/blogApi'
import { errorMessage } from '../../../../shared/http'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'
import BlogPreview from '../../components/BlogPreview.vue'
import BlogTagList from '../../components/BlogTagList.vue'
import BlogTopicPanel from '../../components/BlogTopicPanel.vue'
import { canDelete, canPublish, canRestore, canWithdraw, dateLabel, postStatusLabel } from '../../support/display'

/*
 * 后台文章编辑器（BLOG-003 / BLOG-008 / BLOG-009）。
 *
 * 三件事刻意拆开：
 * 1. 元数据（标题/slug/摘要/封面/标签）走 PATCH；
 * 2. 正文走 PUT /body —— 正文可能很大，改个标题不该重传整篇 Markdown；
 * 3. 状态只通过 publish / withdraw / restore 三个动作改变 ——
 *    前端没有任何地方可以把 status 字段直接写进请求体。
 *
 * 封面通过媒体模块提供的 MediaPicker 选择：博客只提交 mediaAssetId，
 * 引用登记由后端在同一个业务事务里完成。
 */
const route = useRoute()
const router = useRouter()

const isCreate = computed(() => route.params.postId === 'new')
const postId = computed(() => (isCreate.value ? null : Number(route.params.postId)))

const form = reactive({ title: '', slug: '', summary: '', tagIds: [] })
const body = ref('')
const detail = ref(null)
const coverAsset = ref(null)
const coverRemoved = ref(false)
const tagOptions = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const previewHtml = ref(null)

const { pickerOpen, pickerType, pick, settle } = useMediaPicker()

const selectedTags = computed(() => tagOptions.value.filter((tag) => form.tagIds.includes(tag.id)))
const coverUrl = computed(() => {
  if (coverRemoved.value) return null
  if (coverAsset.value) return coverAsset.value.contentUrl
  return detail.value?.coverUrl || null
})
// 预览用的数据模型：把「表单里的当前内容」组装成 BlogPreview 需要的形状。
// 刻意不叫 previewPost —— 那是本模块的接口名，同名会让 api 导入被遮蔽。
const previewModel = computed(() => ({
  title: form.title || '（未命名草稿）',
  slug: form.slug,
  summary: form.summary,
  bodyMarkdown: previewHtml.value ?? body.value,
  coverUrl: coverUrl.value,
  tags: selectedTags.value,
  topics: detail.value?.topics || [],
  publishedAt: detail.value?.publishedAt,
  updatedAt: detail.value?.updatedAt,
}))

async function loadTags() {
  try {
    const page = await listAdminTags({ page: 1, pageSize: 100 })
    tagOptions.value = page.items
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function loadPost() {
  if (isCreate.value) {
    detail.value = null
    previewHtml.value = null
    return
  }
  loading.value = true
  error.value = ''
  try {
    const post = await getAdminPost(postId.value)
    detail.value = post
    form.title = post.title
    form.slug = post.slug
    form.summary = post.summary || ''
    form.tagIds = (post.tags || []).map((tag) => tag.id)
    body.value = post.bodyMarkdown || ''
    coverAsset.value = null
    coverRemoved.value = false
    previewHtml.value = null
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function openPicker() {
  const asset = await pick('IMAGE')
  if (asset) {
    coverAsset.value = asset
    coverRemoved.value = false
  }
}

function removeCover() {
  coverAsset.value = null
  coverRemoved.value = true
}

function toggleTag(tagId) {
  const index = form.tagIds.indexOf(tagId)
  if (index >= 0) form.tagIds.splice(index, 1)
  else form.tagIds.push(tagId)
}

async function saveMeta() {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const payload = {
      title: form.title,
      slug: form.slug,
      summary: form.summary,
      tagIds: form.tagIds,
    }
    if (coverAsset.value) payload.coverMediaAssetId = coverAsset.value.id
    // 只有真的移除了封面才发 clearCover，避免每次保存都把“未提供封面字段”误解成取消封面
    if (coverRemoved.value) payload.clearCover = true

    if (isCreate.value) {
      const created = await createPost(payload)
      notice.value = '草稿已创建，可以继续写正文了。'
      await router.replace(`/admin/blog/posts/${created.id}`)
      return
    }
    await updatePost(postId.value, payload)
    notice.value = '文章信息已保存。'
    await loadPost()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

async function saveBody() {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const updated = await updatePostBody(postId.value, body.value)
    detail.value = updated
    // 正文里的媒体引用由后端从正文解析后重建，这里把返回的引用集合显示出来便于核对
    notice.value = `正文已保存，识别到 ${updated.contentMediaAssetIds?.length || 0} 个正文媒体引用。`
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

async function refreshPreview() {
  if (isCreate.value) {
    // 草稿还没入库：直接用当前编辑器内容渲染，走的是与前台相同的 Markdown 逻辑
    previewHtml.value = body.value
    return
  }
  error.value = ''
  try {
    const post = await previewPost(postId.value)
    previewHtml.value = post.bodyMarkdown
    detail.value = post
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function act(action) {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    if (action === 'publish') await publishPost(postId.value)
    if (action === 'withdraw') await withdrawPost(postId.value)
    if (action === 'restore') await restorePost(postId.value)
    if (action === 'delete') {
      if (!window.confirm(`确认删除《${form.title}》？标签、专题关系与媒体引用都会被解除。`)) return
      await deletePost(postId.value)
      await router.replace('/admin/blog')
      return
    }
    notice.value = '状态已更新。'
    await loadPost()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadTags()
  loadPost()
})
watch(() => route.params.postId, () => {
  loadPost()
})
</script>

<template>
  <main class="page-container">
    <div class="page-heading">
      <p class="eyebrow">BLOG EDITOR</p>
      <h1>{{ isCreate ? '新建文章' : '编辑文章' }}</h1>
      <p v-if="detail">
        当前状态：<strong>{{ postStatusLabel(detail.status) }}</strong>
        · 首次发布 {{ dateLabel(detail.publishedAt) }}
        <template v-if="detail.hasDisabledTags"> · 含已停用标签（历史绑定保留，但不再接受新绑定）</template>
      </p>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载文章…</p>

    <div class="blog-editor-layout">
      <section class="surface-card">
        <h2 class="section-heading">文章信息</h2>
        <div class="field-grid">
          <label>标题<input v-model.trim="form.title" maxlength="255" placeholder="文章标题" /></label>
          <label>slug<input v-model.trim="form.slug" maxlength="180" placeholder="first-post（小写字母、数字、中划线）" /></label>
          <label class="field-grid__wide">摘要
            <textarea v-model.trim="form.summary" rows="3" maxlength="1000" placeholder="留空时发布会自动按正文生成"></textarea>
          </label>
        </div>

        <h3 class="blog-editor__subheading">封面</h3>
        <div class="blog-cover-editor">
          <img v-if="coverUrl" :src="coverUrl" alt="封面预览" />
          <p v-else class="muted">未设置封面。封面会被登记为 blog.cover 引用，归档该媒体前会先被拒绝。</p>
          <div class="blog-cover-editor__actions">
            <button type="button" @click="openPicker">从媒体库选择</button>
            <button v-if="coverUrl" type="button" @click="removeCover">移除封面</button>
          </div>
        </div>

        <h3 class="blog-editor__subheading">标签（多维分类，可多选）</h3>
        <div class="blog-tag-picker">
          <label v-for="tag in tagOptions" :key="tag.id" :class="['blog-tag-option', tag.status === 'DISABLED' && 'is-disabled']">
            <input
              type="checkbox"
              :checked="form.tagIds.includes(tag.id)"
              @change="toggleTag(tag.id)"
            />
            {{ tag.name }}<small>{{ tag.status === 'DISABLED' ? '已停用' : `${tag.postCount || 0} 篇` }}</small>
          </label>
          <p v-if="!tagOptions.length" class="muted">还没有标签，先到「分类与专题」创建。</p>
        </div>

        <div class="blog-editor__actions">
          <button class="primary-button" type="button" :disabled="saving" @click="saveMeta">
            {{ isCreate ? '创建草稿' : '保存文章信息' }}
          </button>
          <template v-if="!isCreate">
            <button v-if="canPublish(detail?.status)" type="button" :disabled="saving" @click="act('publish')">发布</button>
            <button v-if="canWithdraw(detail?.status)" type="button" :disabled="saving" @click="act('withdraw')">撤回</button>
            <button v-if="canRestore(detail?.status)" type="button" :disabled="saving" @click="act('restore')">恢复</button>
            <button v-if="canDelete(detail?.status)" class="blog-danger" type="button" :disabled="saving" @click="act('delete')">删除</button>
          </template>
        </div>
      </section>

      <section class="surface-card">
        <h2 class="section-heading">正文（Markdown）</h2>
        <p class="muted">
          正文里插入媒体时使用媒体模块给出的内容地址（形如 /api/media/assets/12/content），
          后端会据此登记 blog.content 引用，删除文章时一并解除。
        </p>
        <textarea v-model="body" class="blog-body-input" rows="18" placeholder="# 标题&#10;&#10;正文…"></textarea>
        <div class="blog-editor__actions">
          <button class="primary-button" type="button" :disabled="saving || isCreate" @click="saveBody">保存正文</button>
          <button type="button" :disabled="saving" @click="refreshPreview">刷新预览</button>
          <span v-if="isCreate" class="muted">先创建草稿，正文才能保存。</span>
        </div>

        <h3 class="blog-editor__subheading">预览</h3>
        <BlogPreview :post="previewModel" preview />
      </section>

      <aside v-if="detail" class="blog-editor-aside">
        <section class="surface-card">
          <h2 class="blog-aside__title">分类现状</h2>
          <p class="muted">标签</p>
          <BlogTagList :tags="detail.tags" empty-text="未打标签" />
          <p class="muted">专题（顺序由专题管理决定）</p>
          <BlogTopicPanel :topics="detail.topics" empty-text="未加入专题" />
          <p class="muted">正文媒体引用</p>
          <p v-if="!detail.contentMediaAssetIds?.length" class="muted">正文里还没有引用任何媒体。</p>
          <ul v-else class="blog-ref-list">
            <li v-for="assetId in detail.contentMediaAssetIds" :key="assetId">#{{ assetId }}</li>
          </ul>
        </section>
      </aside>
    </div>

    <MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
  </main>
</template>
