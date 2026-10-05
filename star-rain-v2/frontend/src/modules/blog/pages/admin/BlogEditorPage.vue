<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createPost, createTag, deletePost, getAdminPost, listAdminTags,
  publishPost, restorePost, updatePost, updatePostBody, withdrawPost,
} from '../../api/blogApi'
import { errorMessage } from '../../../../shared/http'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog.vue'
import BlogStatusPill from '../../components/admin/BlogStatusPill.vue'
import BlogTagPicker from '../../components/admin/BlogTagPicker.vue'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import { createTaxonomy } from '../../components/admin/tagSlug'
import { canDelete, canPublish, canRestore, canWithdraw, dateLabel } from '../../support/display'

/*
 * 后台文章编辑器（字段集严格对齐 V1 `views/admin/BlogEditView.vue`）。
 *
 * 表单只有四项：标题（独立一行）、正文（MarkdownEditor）、标签、摘要。
 * V1 的模板里**没有** slug、封面与独立预览区，V2 也不再提供：
 *   - slug：创建时由服务端从标题派生（BlogPostCreateDTO.slug 可空），
 *     更新时保持已有值不变（改标题不会悄悄改地址，已经发出去的链接不会失效）；
 *   - 封面：后端仍支持 coverMediaAssetId / clearCover / blog.cover 媒体引用，
 *     只是编辑页不提供输入项（与 V1 的字段集一致）；
 *   - 独立预览区：IR 模式本身就是所见即所得，再放一个预览是重复的第二个真相来源。
 *
 * 三件事刻意拆开（沿用 V2 既有约定）：
 * 1. 元数据（标题 / 摘要 / 标签）走 PATCH；
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

const form = reactive({ title: '', summary: '', tagValues: [] })
const body = ref('')
const detail = ref(null)
const tagOptions = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
/* 加载文章时递增，用来强制重建编辑器（Vditor 需要拿到新的初始值，而不是被 setValue 追着改） */
const editorKey = ref(0)
const editorRef = ref(null)
let keepEditorOnNextRoute = false
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
    summary: form.summary,
    tagValues: [...form.tagValues],
    body: body.value,
  })
}
const dirty = computed(() => savedSnapshot.value !== '' && savedSnapshot.value !== snapshot())
function markSaved() {
  savedSnapshot.value = snapshot()
}
function markMetaSaved(savedBody) {
  savedSnapshot.value = JSON.stringify({
    title: form.title, summary: form.summary, tagValues: [...form.tagValues], body: savedBody ?? '',
  })
}
function syncEditorBody() {
  // Vditor 的 input 回调可能晚于紧接着的保存点击；提交前直接读取当前正文。
  const current = editorRef.value?.getMarkdown?.()
  if (typeof current === 'string') body.value = current
}

const { pickerOpen, pickerType, pick, settle } = useMediaPicker()

const selectedTags = computed(() => tagOptions.value.filter((tag) => form.tagValues.includes(tag.id)))
const pendingTagNames = computed(() => form.tagValues.filter((value) => typeof value === 'string'))

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
    form.title = ''
    form.summary = ''
    form.tagValues = []
    body.value = ''
    markSaved()
    return
  }
  loading.value = true
  error.value = ''
  try {
    const post = await getAdminPost(postId.value)
    detail.value = post
    form.title = post.title
    form.summary = post.summary || ''
    form.tagValues = (post.tags || []).map((tag) => tag.id)
    body.value = post.bodyMarkdown || ''
    markSaved()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

/* 编辑器里的图片按钮复用媒体库选择器，插入的是媒体内容地址 */
async function pickBodyImage() {
  const asset = await pick('IMAGE')
  return asset ? { url: asset.contentUrl, name: asset.originalName } : null
}

// 离开编辑器：有未保存改动时先问一次（对应 V1 的 useUnsavedGuard 路由离开提醒）
async function leaveEditor() {
  syncEditorBody()
  if (dirty.value && !await confirmDialog.value.ask('正文或文章信息还没保存，离开后改动会丢失。确定返回列表吗？')) return
  await router.push('/useradmin/blog/manage')
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
    const created = await createTaxonomy(createTag, name, 'tag', 100)
    ids.push(created.id)
    tagOptions.value = [...tagOptions.value, created]
  }
  return Array.from(new Set(ids))
}

async function saveMeta({ silent = false } = {}) {
  syncEditorBody()
  if (!form.title.trim()) {
    error.value = '请先填写文章标题。'
    return false
  }
  saving.value = true
  error.value = ''
  if (!silent) notice.value = ''
  try {
    const tagIds = await resolveTagIds()
    /*
     * 刻意不传 slug：
     *   创建时后端从标题派生（纯中文标题也有确定性回退）；
     *   更新时字段缺省 = 不改，已发布的地址因此不会因为改标题而变化。
     */
    const payload = {
      title: form.title,
      summary: form.summary,
      tagIds,
    }

    if (isCreate.value) {
      const created = await createPost(payload)
      detail.value = { ...created, bodyMarkdown: '' }
      // 创建草稿只保存了元数据；正文还在编辑器里，必须保持为未保存状态。
      markMetaSaved('')
      notice.value = `草稿已创建（地址 ${created.slug}），可以继续写正文了。`
      keepEditorOnNextRoute = true
      await router.replace(`/useradmin/blog/editor/${created.id}`)
      return true
    }
    const updated = await updatePost(postId.value, payload)
    // PATCH 返回的是列表项 VO（没有正文与正文媒体引用），合并而不是整体替换，
    // 否则正在编辑的正文与引用面板会被清空
    detail.value = { ...detail.value, ...updated }
    form.tagValues = (updated.tags || []).map((tag) => tag.id)
    markMetaSaved(detail.value?.bodyMarkdown)
    notice.value = '文章信息已保存。'
    return true
  } catch (cause) {
    keepEditorOnNextRoute = false
    error.value = errorMessage(cause)
    return false
  } finally {
    saving.value = false
  }
}

async function saveBody() {
  syncEditorBody()
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
  const wasCreate = isCreate.value
  const ok = await saveMeta({ silent: true })
  if (!ok) return
  if (wasCreate && !body.value.trim()) {
    notice.value = '草稿已创建，可以继续写正文。'
    return
  }
  await saveBody()
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
      await router.replace('/useradmin/blog/manage')
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
  syncEditorBody()
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
  if (keepEditorOnNextRoute) {
    keepEditorOnNextRoute = false
    return
  }
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
      <p class="blog-edit__outline-note">
        标题独立展示，页面地址在创建时按标题自动生成；正文可从 H1 开始，左侧大纲收录 H1–H6。
      </p>

      <MarkdownEditor
        ref="editorRef"
        :key="editorKey"
        v-model="body"
        placeholder="从 H1 开始撰写正文…"
        :pick-image="pickBodyImage"
      />

      <div class="blog-edit__actions">
        <button class="primary-button" type="button" :disabled="saving || isCreate" @click="saveBody">
          {{ saving ? '保存中…' : '保存正文' }}
        </button>
        <span v-if="isCreate" class="muted">先创建草稿，正文才能保存。</span>
      </div>
    </section>

    <section class="blog-edit__meta">
      <div class="blog-edit__section-head">
        <h2>文章信息</h2>
        <span>保存时自动创建不存在的标签</span>
      </div>

      <div class="blog-edit__panel">
        <div class="field-grid">
          <label class="field-grid__wide">标签
            <BlogTagPicker v-model="form.tagValues" :tags="tagOptions" :disabled="saving" />
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

        <p v-if="detail?.contentMediaAssetIds?.length" class="muted">
          正文媒体引用：<span v-for="assetId in detail.contentMediaAssetIds" :key="assetId" class="blog-ref-chip">#{{ assetId }}</span>
        </p>
      </div>

      <div class="blog-edit__actions blog-edit__actions--split">
        <div class="blog-edit__actions-group">
          <template v-if="!isCreate">
            <button v-if="canPublish(detail?.status)" class="primary-button" type="button" :disabled="saving" @click="act('publish')">发布</button>
            <button v-if="canWithdraw(detail?.status)" type="button" :disabled="saving" @click="act('withdraw')">撤回</button>
            <button v-if="canRestore(detail?.status)" type="button" :disabled="saving" @click="act('restore')">重新公开</button>
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

    <AdminConfirmDialog ref="confirmDialog" />
    <MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
  </section>
</template>
