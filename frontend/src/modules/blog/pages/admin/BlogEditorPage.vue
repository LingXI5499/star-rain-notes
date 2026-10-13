<script setup>
/**
 * BlogEditorPage.vue：博客模块的页面组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../api/blogApi、../../../../shared/http、../../../media/components/MediaPicker.vue、../../../media/support/useMediaPicker。
 * 主要响应式状态：route、router、isCreate、postId、form、body、detail、tagOptions。
 * 阅读入口：snapshot（快照的处理函数）；markSaved（markSaved的处理函数）；markMetaSaved（markMetaSaved的处理函数）；syncEditorBody（同步Editor正文）；loadTags（加载标签列表）。
 */


import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
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
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()

/**
 * 是否创建；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const isCreate = computed(() => route.params.postId === 'new')
/**
 * 文章标识；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const postId = computed(() => (isCreate.value ? null : Number(route.params.postId)))

/**
 * form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const form = reactive({ title: '', summary: '', tagValues: [] })
/**
 * 正文；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const body = ref('')
/**
 * 详情；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const detail = ref(null)
/**
 * 标签选项；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tagOptions = ref([])
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * saving；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const saving = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notice = ref('')
/* 加载文章时递增，用来强制重建编辑器（Vditor 需要拿到新的初始值，而不是被 setValue 追着改） */
/**
 * editor键；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const editorKey = ref(0)
/**
 * editorRef；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const editorRef = ref(null)
let draftTransition = null
let active = true
let generation = 0
let leaveConfirmation = null
const uncertainSave = ref(false)
/**
 * 确认弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirmDialog = ref(null)

/*
 * 未保存判断：拿「当前表单快照」和「上一次成功保存时的快照」比字符串。
 * V1 用 useUnsavedGuard 做同一件事；这里刻意不用 watch + 脏标记，
 * 因为 loadPost() 会程序化地写入 form / body，脏标记必须在写入之后才允许生效，
 * 快照对比天然没有这个时序问题。
 */
/**
 * saved快照；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const savedMeta = ref(null)
const savedBody = ref('')
/**
 * 快照的处理函数。
 * 主要协作：JSON.stringify。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function snapshot() {
  return {
    title: form.title,
    summary: form.summary,
    tagValues: [...form.tagValues],
    body: body.value,
  }
}
/**
 * dirty；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const metaOf = value => JSON.stringify({ title: value.title, summary: value.summary, tagValues: value.tagValues })
const dirty = computed(() => savedMeta.value !== null
  && (savedMeta.value !== metaOf(snapshot()) || savedBody.value !== body.value))
/**
 * markSaved的处理函数。
 * 主要协作：snapshot。
 * 更新的响应式状态包括 savedSnapshot；渲染由 Vue 的依赖跟踪触发。
 */
function markSaved() {
  savedMeta.value = metaOf(snapshot())
  savedBody.value = body.value
}
/**
 * 同步Editor正文。
 * 主要协作：editorRef.value.getMarkdown。
 * 更新的响应式状态包括 body；渲染由 Vue 的依赖跟踪触发。
 */
function syncEditorBody() {
  // Vditor 的 input 回调可能晚于紧接着的保存点击；提交前直接读取当前正文。
  const current = editorRef.value?.getMarkdown?.()
  if (typeof current === 'string') body.value = current
}

const { pickerOpen, pickerType, pick, settle } = useMediaPicker()

/**
 * 选中标签列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const selectedTags = computed(() => tagOptions.value.filter((tag) => form.tagValues.includes(tag.id)))
/**
 * 待处理标签Names；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pendingTagNames = computed(() => form.tagValues.filter((value) => typeof value === 'string'))

/**
 * 加载标签列表。
 * 主要协作：listAdminTags、rows.push、errorMessage。
 * 更新的响应式状态包括 tagOptions、error；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadTags() {
  try {
    const first = await listAdminTags({ page: 1, pageSize: 100 })
    const rows = [...(first.items || [])]
    for (let page = 2; page <= Math.ceil((first.total || 0) / 100); page += 1) {
      const next = await listAdminTags({ page, pageSize: 100 })
      rows.push(...(next.items || []))
    }
    if (active) tagOptions.value = rows
  } catch (cause) {
    if (active) error.value = errorMessage(cause)
  }
}

/**
 * 加载文章。
 * 主要协作：markSaved、getAdminPost、.map、errorMessage。
 * 更新的响应式状态包括 detail、body、loading、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadPost() {
  const version = ++generation
  uncertainSave.value = false
  const id = postId.value
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
    const post = await getAdminPost(id)
    if (!active || version !== generation) return
    detail.value = post
    form.title = post.title
    form.summary = post.summary || ''
    form.tagValues = (post.tags || []).map((tag) => tag.id)
    body.value = post.bodyMarkdown || ''
    markSaved()
  } catch (cause) {
    if (active && version === generation) error.value = errorMessage(cause)
  } finally {
    if (active && version === generation) loading.value = false
  }
}

/* 编辑器里的图片按钮复用媒体库选择器，插入的是媒体内容地址 */
/**
 * pick正文图片的处理函数。
 * 主要协作：pick。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function pickBodyImage() {
  const asset = await pick('IMAGE')
  return asset ? { url: asset.contentUrl, name: asset.originalName } : null
}

// 离开编辑器：有未保存改动时先问一次（对应 V1 的 useUnsavedGuard 路由离开提醒）
/**
 * 离开Editor。
 * 主要协作：syncEditorBody、confirmDialog.value.ask、router.push。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function leaveEditor() {
  await router.push('/useradmin/blog/manage')
}

/** One guard covers buttons, navigation, history and changes to the article parameter. */
async function guardNavigation(to, from) {
  if (draftTransition?.from === from.fullPath && draftTransition?.to === to.fullPath) return true
  if (saving.value) {
    error.value = '请求尚未结束，请等待结果后再离开编辑器。'
    return false
  }
  syncEditorBody()
  if (!dirty.value && !uncertainSave.value) return true
  if (!leaveConfirmation) {
    leaveConfirmation = confirmDialog.value.ask('正文或文章信息还没保存，或保存结果尚未核实。离开后改动会丢失，确定离开吗？')
      .finally(() => { leaveConfirmation = null })
  }
  return active && await leaveConfirmation
}
onBeforeRouteLeave(guardNavigation)
onBeforeRouteUpdate((to, from) => to.params.postId === from.params.postId ? true : guardNavigation(to, from))

/*
 * 把「待创建的新标签名」先建成真实标签，再返回完整的 tagIds。
 * 已存在同名标签时直接复用，不重复建。
 */
async function resolveTagIds(values) {
  const ids = values.filter((value) => typeof value === 'number')
  for (const name of values.filter(value => typeof value === 'string')) {
    const existing = tagOptions.value.find(
      (tag) => tag.name.normalize('NFKC').trim().toLowerCase() === name.normalize('NFKC').trim().toLowerCase(),
    )
    if (existing) {
      ids.push(existing.id)
      continue
    }
    const created = await createTaxonomy(createTag, name, 'tag', 100)
    ids.push(created.id)
    if (active) tagOptions.value = [...tagOptions.value, created]
  }
  return Array.from(new Set(ids))
}

/**
 * 保存Meta。
 * 主要协作：syncEditorBody、form.title.trim、resolveTagIds、createPost、markMetaSaved。
 * 更新的响应式状态包括 error、saving、notice、detail；渲染由 Vue 的依赖跟踪触发。
 * @param { silent = false } = {} silentfalse；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
/** Capture everything before tag creation or HTTP requests can yield. */
async function runSave(operation) {
  if (saving.value || loading.value || uncertainSave.value) return
  syncEditorBody()
  const submitted = snapshot()
  const context = { id: postId.value, version: generation, submitted }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    await operation(context)
  } catch (cause) {
    if (!active || context.version !== generation) return
    // A transport error cannot tell whether the server committed the request.
    uncertainSave.value = !cause?.response || cause.response.status >= 500
    error.value = uncertainSave.value
      ? '保存结果尚未确认，输入已保留。请核实服务端内容后再重试；创建结果请在文章列表核实。'
      : errorMessage(cause)
  } finally {
    draftTransition = null
    if (active && context.version === generation) saving.value = false
  }
}

const validContext = context => active && context.version === generation

/** Save only submitted metadata; the returned body cannot acknowledge editor input. */
async function persistMeta(context) {
  const submitted = context.submitted
  if (!submitted.title.trim()) throw { response: { status: 400, data: { message: '请先填写文章标题。' } } }
  const tagIds = await resolveTagIds(submitted.tagValues)
  if (!validContext(context)) return false
  const payload = { title: submitted.title, summary: submitted.summary, tagIds }
  const created = context.id === null
  const updated = await (created ? createPost(payload) : updatePost(context.id, payload))
  if (!validContext(context)) return false
  detail.value = created ? { ...updated, bodyMarkdown: '' } : { ...detail.value, ...updated }
  const tags = (updated.tags || []).map(tag => tag.id)
  // Convert submitted tag names to IDs only when the selection has not changed.
  if (JSON.stringify(form.tagValues) === JSON.stringify(submitted.tagValues)) form.tagValues = tags
  if (form.title === submitted.title) form.title = updated.title ?? submitted.title
  if (form.summary === submitted.summary) form.summary = updated.summary || ''
  savedMeta.value = metaOf({ title: updated.title ?? submitted.title, summary: updated.summary || '', tagValues: tags })
  notice.value = created ? `草稿已创建（地址 ${updated.slug}），正文尚未保存。` : '文章信息已保存，正文需单独保存。'
  if (created) {
    savedBody.value = ''
    context.id = updated.id
    draftTransition = { from: route.fullPath, to: `/useradmin/blog/editor/${updated.id}` }
    try {
      const failure = await router.replace(draftTransition.to)
      if (failure) throw new Error('Draft navigation failed')
    } finally { draftTransition = null }
  }
  return true
}

/** A body response acknowledges exactly the Markdown submitted at request start. */
async function persistBody(context) {
  if (!context.id || !validContext(context)) return
  const updated = await updatePostBody(context.id, context.submitted.body)
  if (!validContext(context)) return
  detail.value = updated
  savedBody.value = context.submitted.body
  notice.value = `正文已保存，识别到 ${updated.contentMediaAssetIds?.length || 0} 个正文媒体引用。`
}

async function saveMeta() { await runSave(persistMeta) }
async function saveBody() { await runSave(persistBody) }
async function saveAll() {
  await runSave(async context => {
    if (!await persistMeta(context)) return
    if (!context.submitted.body.trim() && !savedBody.value) return
    await persistBody(context)
  })
}

/** Read back uncertain writes without replacing any current input or replaying a write. */
async function verifySavedContent() {
  if (saving.value || !postId.value) return
  const context = { id: postId.value, version: generation }
  saving.value = true
  try {
    const persisted = await getAdminPost(context.id)
    if (!validContext(context)) return
    detail.value = persisted
    savedMeta.value = metaOf({ title: persisted.title, summary: persisted.summary || '', tagValues: (persisted.tags || []).map(tag => tag.id) })
    savedBody.value = persisted.bodyMarkdown || ''
    uncertainSave.value = false
    error.value = ''
    syncEditorBody()
    notice.value = dirty.value ? '服务端内容已核实，当前输入仍有未保存改动，可继续保存。' : '服务端内容已核实，与当前输入一致。'
  } catch (cause) {
    if (validContext(context)) error.value = errorMessage(cause)
  } finally { if (validContext(context)) saving.value = false }
}

/**
 * act的处理函数。
 * 主要协作：confirmDialog.value.ask、publishPost、withdrawPost、restorePost、deletePost。
 * 更新的响应式状态包括 saving、error、notice、detail；渲染由 Vue 的依赖跟踪触发。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function act(action) {
  if (!postId.value || saving.value || loading.value || uncertainSave.value) return
  const context = { id: postId.value, version: generation }
  if (action === 'delete') {
    const accepted = await confirmDialog.value.ask(`确认删除《${form.title}》？标签、专题关系与媒体引用都会被解除。`)
    if (!accepted) return
    if (!validContext(context) || saving.value) return
  }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    if (action === 'publish') await publishPost(context.id)
    if (action === 'withdraw') await withdrawPost(context.id)
    if (action === 'restore') await restorePost(context.id)
    if (action === 'delete') {
      await deletePost(context.id)
      if (!validContext(context)) return
      savedMeta.value = metaOf(snapshot())
      savedBody.value = body.value
      saving.value = false
      await router.replace('/useradmin/blog/manage')
      return
    }
    if (!validContext(context)) return
    notice.value = '状态已更新。'
    // 只刷新详情里的状态与时间戳，不碰 form / body——
    // 改状态之前可能有还没保存的正文，重新 loadPost() 会把它悄悄冲掉
    const updated = await getAdminPost(context.id)
    if (validContext(context)) detail.value = updated
  } catch (cause) {
    if (validContext(context)) error.value = errorMessage(cause)
  } finally {
    if (validContext(context)) saving.value = false
  }
}

// 未保存提醒：正文与元数据都算改动（V1 由 useUnsavedGuard 负责，这里用等价的最小实现）
/**
 * 响应之前Unload。
 * 主要协作：syncEditorBody、event.preventDefault。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function onBeforeUnload(event) {
  syncEditorBody()
  if (!dirty.value && !saving.value && !uncertainSave.value) return
  event.preventDefault()
  event.returnValue = ''
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  loadTags()
  loadPost()
  window.addEventListener('beforeunload', onBeforeUnload)
})

/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => {
  active = false
  generation += 1
  window.removeEventListener('beforeunload', onBeforeUnload)
})

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.params.postId, () => {
  if (draftTransition?.to === route.fullPath) {
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
    <button v-if="uncertainSave && postId" type="button" :disabled="saving" @click="verifySavedContent">核实服务端保存结果</button>
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
