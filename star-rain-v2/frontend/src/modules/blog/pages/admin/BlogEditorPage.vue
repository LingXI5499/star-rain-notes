<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createPost, createTag, deletePost, getAdminPost, listAdminTags, previewPost,
  publishPost, restorePost, updatePost, updatePostBody, withdrawPost,
} from '../../api/blogApi'
import { errorMessage } from '../../../../shared/http'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'
import BlogPreview from '../../components/BlogPreview.vue'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog.vue'
import BlogStatusPill from '../../components/admin/BlogStatusPill.vue'
import BlogTagPicker from '../../components/admin/BlogTagPicker.vue'
import MarkdownEditor from '../../components/admin/MarkdownEditor.vue'
import { derivedSlug } from '../../components/admin/tagSlug'
import { canDelete, canPublish, canRestore, canWithdraw, dateLabel } from '../../support/display'

/*
 * 后台文章编辑器（对齐 V1 views/admin/BlogEditView.vue + components/MarkdownEditor.vue）。
 *
 * 页面结构：顶部条（返回 / 状态 / 保存）→ 写作卡（独立标题行 + Markdown 编辑器）
 * → 文章信息（标签、slug、摘要、封面）→ 底部动作。
 *
 * 三件事刻意拆开（沿用 V2 既有约定）：
 * 1. 元数据（标题 / slug / 摘要 / 封面 / 标签）走 PATCH；
 * 2. 正文走 PUT /body —— 正文可能很大，改个标题不该重传整篇 Markdown；
 * 3. 状态只通过 publish / withdraw / restore 改变，前端不能直接写 status。
 *
 * 「输入新标签，保存时自动创建」：V2 的 PATCH 只有一个 tagIds 字段（没有 V1 的 tagNames），
 * 所以要按顺序做两步——先 POST /admin/blog/tags 建标签，再用返回的 id 保存文章。
 */
const route = useRoute()
const router = useRouter()

const isCreate = computed(() => route.params.postId === 'new')
const postId = computed(() => (isCreate.value ? null : Number(route.params.postId)))

const form = reactive({ title: '', slug: '', summary: '', tagValues: [] })
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
const editorKey = ref(0)
const confirmDialog = ref(null)

/*
 * 未保存判断：拿「当前表单快照」和「上一次成功保存时的快照」比字符串。
 * V1 用 useUnsavedGuard 做同一件事；这里刻意不用 watch + 脏标记，
 * 因为 loadPost() 会程序化地写入 form / body，脏标记必须在写入之后才允许生效，
 * 快照对比天然没有这个时序问题。
 */
const savedSnapshot = ref('')
function snapshot() {
  return JSON.stringify({
    title: form.title,
    slug: form.slug,
    summary: form.summary,
    tagValues: [...form.tagValues],
    body: body.value,
  })
}
const dirty = computed(() => savedSnapshot.value !== '' && savedSnapshot.value !== snapshot())
function markSaved() {
  savedSnapshot.value = snapshot()
}

const { pickerOpen, pickerType, pick, settle } = useMediaPicker()

const selectedTags = computed(() => tagOptions.value.filter((tag) => form.tagValues.includes(tag.id)))
const pendingTagNames = computed(() => form.tagValues.filter((value) => typeof value === 'string'))
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
    markSaved()
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
    form.tagValues = (post.tags || []).map((tag) => tag.id)
    body.value = post.bodyMarkdown || ''
    coverAsset.value = null
    coverRemoved.value = false
    previewHtml.value = null
    markSaved()
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

// 编辑器里的图片按钮复用同一个媒体选择器，插入的是媒体内容地址
async function pickBodyImage() {
  const asset = await pick('IMAGE')
  return asset ? { url: asset.contentUrl, name: asset.originalName } : null
}

function removeCover() {
  coverAsset.value = null
  coverRemoved.value = true
}

// 中文标题推导不出合法 slug，此时给出时间戳兜底，避免用户面对一个必填却又无从下手的字段
function suggestSlug() {
  const base = form.title
    .normalize('NFKD')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 120)
  form.slug = base || `post-${Date.now().toString(36)}`
}

// 离开编辑器：有未保存改动时先问一次（对应 V1 的 useUnsavedGuard 路由离开提醒）
async function leaveEditor() {
  if (dirty.value && !await confirmDialog.value.ask('正文或文章信息还没保存，离开后改动会丢失。确定返回列表吗？')) return
  await router.push('/admin/blog')
}


/*
 * 把「待创建的新标签名」先建成真实标签，再返回完整的 tagIds。
 * 已存在同名标签时直接复用，不重复建。
 */
async function resolveTagIds() {
  const ids = form.tagValues.filter((value) => typeof value === 'number')
  for (const name of pendingTagNames.value) {
    const existing = tagOptions.value.find(
      (tag) => tag.name.normalize('NFKC').trim().toLowerCase() === name.normalize('NFKC').trim().toLowerCase(),
    )
    if (existing) {
      ids.push(existing.id)
      continue
    }
    const created = await createTag({
      // 编号按名称推导；纯中文名用稳定散列，避免每次保存都生成不同的 slug
      slug: derivedSlug(name, 'tag', 100),
      name,
    })
    ids.push(created.id)
    tagOptions.value = [...tagOptions.value, created]
  }
  return Array.from(new Set(ids))
}

async function saveMeta({ silent = false } = {}) {
  if (!form.title.trim()) {
    error.value = '请先填写文章标题。'
    return false
  }
  if (!isCreate.value && !form.slug.trim()) {
    error.value = '请填写 slug（正文地址的一部分）。'
    return false
  }
  saving.value = true
  error.value = ''
  if (!silent) notice.value = ''
  try {
    const tagIds = await resolveTagIds()
    // 新建时允许只填标题：slug 由标题推导（中文标题推导不出时用时间戳兜底）
    if (isCreate.value && !form.slug.trim()) suggestSlug()
    const payload = {
      title: form.title,
      slug: form.slug.trim() || undefined,
      summary: form.summary,
      tagIds,
    }
    if (coverAsset.value) payload.coverMediaAssetId = coverAsset.value.id
    // 只有真的移除了封面才发 clearCover，避免每次保存都把「未提供封面字段」误解成取消封面
    if (coverRemoved.value) payload.clearCover = true

    if (isCreate.value) {
      const created = await createPost(payload)
      markSaved()
      notice.value = '草稿已创建，可以继续写正文了。'
      await router.replace(`/admin/blog/posts/${created.id}`)
      return true
    }
    const updated = await updatePost(postId.value, payload)
    // PATCH 返回的是列表项 VO（没有正文与正文媒体引用），合并而不是整体替换，
    // 否则正在编辑的正文与引用面板会被清空
    detail.value = { ...detail.value, ...updated }
    form.tagValues = (updated.tags || []).map((tag) => tag.id)
    coverAsset.value = null
    coverRemoved.value = false
    markSaved()
    notice.value = '文章信息已保存。'
    return true
  } catch (cause) {
    error.value = errorMessage(cause)
    return false
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
    markSaved()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

// 顶部「保存文章」：一次把元数据与正文都落库，新文章先建草稿再写正文
async function saveAll() {
  const ok = await saveMeta({ silent: true })
  if (!ok) return
  if (isCreate.value) {
    // 新建后路由已切到 /posts/{id}，由路由监听重新加载，交给用户继续写正文
    notice.value = '草稿已创建，可以继续写正文并再次保存。'
    return
  }
  await saveBody()
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
    notice.value = '已按服务端保存的正文刷新预览。'
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

async function act(action) {
  if (!postId.value) return
  if (action === 'delete') {
    const accepted = await confirmDialog.value.ask(`确认删除《${form.title}》？标签、专题关系与媒体引用都会被解除。`)
    if (!accepted) return
  }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    if (action === 'publish') await publishPost(postId.value)
    if (action === 'withdraw') await withdrawPost(postId.value)
    if (action === 'restore') await restorePost(postId.value)
    if (action === 'delete') {
      await deletePost(postId.value)
      await router.replace('/admin/blog')
      return
    }
    notice.value = '状态已更新。'
    // 只刷新详情里的状态与时间戳，不碰 form / body——
    // 改状态之前可能有还没保存的正文，重新 loadPost() 会把它悄悄冲掉
    detail.value = await getAdminPost(postId.value)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

// 未保存提醒：正文与元数据都算改动（V1 由 useUnsavedGuard 负责，这里用等价的最小实现）
function onBeforeUnload(event) {
  if (!dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

onMounted(() => {
  loadTags()
  loadPost()
  window.addEventListener('beforeunload', onBeforeUnload)
})

onBeforeUnmount(() => window.removeEventListener('beforeunload', onBeforeUnload))

watch(() => route.params.postId, () => {
  editorKey.value += 1
  loadPost()
})
</script>

<template>
  <section class="blog-edit">
    <div class="blog-edit__topbar">
      <div>
        <h1 class="blog-edit__title">{{ isCreate ? '新建文章' : '编辑文章' }}</h1>
        <p>
          <template v-if="detail">
            <BlogStatusPill :status="detail.status" />
            · 首次发布 {{ dateLabel(detail.publishedAt) }}
            <template v-if="detail.hasDisabledTags"> · 含已停用标签（历史绑定保留，但不再接受新绑定）</template>
          </template>
          <template v-else>正文、标签与摘要在同一工作流中完成。</template>
        </p>
      </div>
      <div class="blog-edit__topbar-actions">
        <button type="button" :disabled="saving" @click="leaveEditor">返回列表</button>
        <button class="primary-button" type="button" :disabled="saving || loading" @click="saveAll">
          {{ saving ? '保存中…' : '保存文章' }}
        </button>
      </div>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载文章…</p>

    <section class="blog-edit__writing-card">
      <input
        v-model.trim="form.title"
        class="blog-edit__title-input"
        maxlength="255"
        placeholder="输入文章标题"
        aria-label="文章标题"
      />
      <p class="blog-edit__outline-note">页面标题独立展示；正文可从 H1 开始，左侧大纲收录 H1–H6。</p>

      <MarkdownEditor
        :key="editorKey"
        v-model="body"
        placeholder="从 H1 开始撰写正文…"
        :pick-image="pickBodyImage"
      />

      <div class="blog-edit__actions">
        <button class="primary-button" type="button" :disabled="saving || isCreate" @click="saveBody">
          {{ saving ? '保存中…' : '保存正文' }}
        </button>
        <button type="button" :disabled="saving" @click="refreshPreview">刷新预览</button>
        <span v-if="isCreate" class="muted">先创建草稿，正文才能保存。</span>
      </div>
    </section>

    <section class="blog-edit__meta">
      <div class="blog-edit__section-head">
        <h2>文章信息</h2>
        <span>保存时自动创建不存在的标签</span>
      </div>

      <div class="blog-edit__panel">
        <h3>发布信息</h3>
        <div class="field-grid">
          <label class="field-grid__wide">标签
            <BlogTagPicker v-model="form.tagValues" :tags="tagOptions" :disabled="saving" />
          </label>
          <label>slug（正文地址）
            <span class="blog-edit__slug-field">
              <input v-model.trim="form.slug" maxlength="180" placeholder="first-post（小写字母、数字、中划线）" />
              <button type="button" @click="suggestSlug">由标题生成</button>
            </span>
          </label>
          <label class="field-grid__wide">摘要
            <textarea
              v-model="form.summary"
              rows="3"
              maxlength="1000"
              placeholder="留空时发布会自动按正文生成"
            />
          </label>
        </div>

        <h3 class="blog-edit__subheading">封面</h3>
        <div class="blog-cover-editor">
          <img v-if="coverUrl" :src="coverUrl" alt="封面预览" />
          <p v-else class="muted">未设置封面。封面会被登记为 blog.cover 引用，归档该媒体前会先被拒绝。</p>
          <div class="blog-cover-editor__actions">
            <button type="button" @click="openPicker">从媒体库选择</button>
            <button v-if="coverUrl" type="button" @click="removeCover">移除封面</button>
          </div>
        </div>

        <p v-if="detail?.contentMediaAssetIds?.length" class="muted">
          正文媒体引用：<span v-for="assetId in detail.contentMediaAssetIds" :key="assetId" class="blog-ref-chip">#{{ assetId }}</span>
        </p>
      </div>

      <div class="blog-edit__actions blog-edit__actions--split">
        <div class="blog-edit__actions-group">
          <template v-if="!isCreate">
            <button v-if="canPublish(detail?.status)" class="primary-button" type="button" :disabled="saving" @click="act('publish')">发布</button>
            <button v-if="canWithdraw(detail?.status)" type="button" :disabled="saving" @click="act('withdraw')">撤回</button>
            <button v-if="canRestore(detail?.status)" type="button" :disabled="saving" @click="act('restore')">恢复</button>
            <button v-if="canDelete(detail?.status)" class="blog-danger" type="button" :disabled="saving" @click="act('delete')">删除</button>
          </template>
        </div>
        <div class="blog-edit__actions-group">
          <button class="primary-button" type="button" :disabled="saving || loading" @click="saveMeta()">
            {{ isCreate ? '创建草稿' : '保存文章信息' }}
          </button>
        </div>
      </div>
    </section>

    <section class="blog-edit__meta">
      <div class="blog-edit__section-head">
        <h2>预览</h2>
        <span>按前台阅读页的样式渲染</span>
      </div>
      <BlogPreview :post="previewModel" preview />
    </section>

    <AdminConfirmDialog ref="confirmDialog" />
    <MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
  </section>
</template>
