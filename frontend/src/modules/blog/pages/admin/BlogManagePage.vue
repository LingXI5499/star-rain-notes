<script setup>
/**
 * BlogManagePage.vue：博客模块的页面组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/composables/paginationScroll、vue、vue-router、../../api/blogApi、../../../../shared/http、../../../../shared/composables/useListQuery。
 * 主要响应式状态：router、route、data、tags、loading、error、notice、busyId。
 * 阅读入口：previewUrl（预览地址）；load（加载当前操作）；loadTags（加载标签列表）；search（搜索的处理函数）；changePage（change分页的处理函数）。
 */


import { usePaginationScroll } from '../../../../shared/composables/paginationScroll'
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  deletePost, listAdminPosts, listAdminTags, publishPost, restorePost, withdrawPost,
} from '../../api/blogApi'
import { errorMessage } from '../../../../shared/http'
import { useListQuery } from '../../../../shared/composables/useListQuery'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog.vue'
import BlogStatusPill from '../../components/admin/BlogStatusPill.vue'
import {
  POST_STATUSES, canDelete, canPublish, canRestore, canWithdraw, dateLabel,
} from '../../support/display'

/*
 * 后台博客列表（对齐 V1 views/admin/BlogListView.vue）。
 *
 * 结构与 V1 一致：标题区 + 筛选工具条 + 卡片式列表 + 分页器。
 * 行内操作按后端状态机给出：每个状态只显示它能做的事，
 * 让「先撤回再删除」这类规则在界面上就能看出来。
 *
 * 预览打开的是**公开树**的文章地址（/blog/posts/:slug，同域同源）。
 * 为什么不用账号树的镜像地址：预览要看的就是匿名访客看到的那一页，
 * 账号树那份带着登录态与账号外壳，不是公开发布后的样子。
 * 地址直接用当前 origin 拼：入口改成路径方案后不再有域名推导，后台与公开站同一个来源。
 */
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()

const { filters, page, pageSize, read: readQuery, write: writeQuery, reset: resetQuery } = useListQuery({
  defaults: { keyword: '', status: '', tagId: '' },
  defaultPageSize: 10,
  pageSizes: [10, 20, 50],
})
/**
 * 数据；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const data = ref({ items: [], total: 0, page: 1, pageSize: 10 })
/**
 * 标签列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tags = ref([])
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
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
/**
 * busy标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busyId = ref(null)
/**
 * 确认弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirmDialog = ref(null)
let loadVersion = 0

/**
 * 总数Pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / pageSize.value)))
/**
 * 有效标签；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const activeTag = computed(() => tags.value.find((tag) => tag.id === Number(filters.tagId)) || null)

// V1 的 el-pagination 是完整分页器；这里用同样的窗口策略（当前页两侧各两页）
/**
 * 分页Numbers；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pageNumbers = computed(() => {
  const total = totalPages.value
  const start = Math.max(1, Math.min(page.value - 2, total - 4))
  const end = Math.min(total, start + 4)
  return Array.from({ length: Math.max(0, end - start + 1) }, (_, index) => start + index)
})

/**
 * 预览地址。
 * @param post 文章；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function previewUrl(post) {
  if (!post.slug) return ''
  return `${window.location.origin}/blog/posts/${post.slug}`
}

/**
 * 加载当前操作。
 * 主要协作：listAdminPosts、errorMessage。
 * 更新的响应式状态包括 loading、error、data；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  const version = ++loadVersion
  loading.value = true
  error.value = ''
  try {
    const result = await listAdminPosts({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      tagId: filters.tagId || undefined,
    })
    if (version === loadVersion) data.value = result
  } catch (cause) {
    if (version === loadVersion) error.value = errorMessage(cause)
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

/**
 * 加载标签列表。
 * 主要协作：listAdminTags。
 * 更新的响应式状态包括 tags；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadTags() {
  try {
    const result = await listAdminTags({ page: 1, pageSize: 100 })
    tags.value = result.items
  } catch {
    tags.value = []
  }
}

/**
 * 搜索的处理函数。
 * 主要协作：writeQuery、load。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function search() {
  page.value = 1
  const previous = route.fullPath
  await writeQuery()
  if (route.fullPath === previous) await load()
}

/**
 * change分页的处理函数。
 * 主要协作：writeQuery。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function changePage(next) {
  if (next < 1 || next > totalPages.value || next === page.value) return
  page.value = next
  await writeQuery()
}

/**
 * change分页大小的处理函数。
 * 主要协作：writeQuery。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function changePageSize() {
  page.value = 1
  await writeQuery()
}

/**
 * 重置筛选条件。
 * 主要协作：resetQuery。
 */
function resetFilters() {
  resetQuery()
}

/**
 * 预览当前操作。
 * 主要协作：previewUrl、window.open。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param post 文章；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function preview(post) {
  if (post.status !== 'PUBLISHED') {
    notice.value = '文章发布后才能在前台预览。'
    return
  }
  const url = previewUrl(post)
  if (!url) {
    error.value = '这篇文章还没有 slug，无法生成公开地址。'
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

/**
 * act的处理函数。
 * 主要协作：confirmDialog.value.ask、publishPost、withdrawPost、restorePost、deletePost。
 * 更新的响应式状态包括 busyId、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param post 文章；默认值、解构字段与可选参数见函数签名。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function act(post, action) {
  if (action === 'delete') {
    const accepted = await confirmDialog.value.ask({
      title: '删除文章',
      message: `删除《${post.title}》会解除标签、专题关系与媒体引用，且无法恢复。`,
      confirmText: '删除',
      danger: true,
      requireName: post.title,
    })
    if (!accepted) return
  }
  busyId.value = post.id
  error.value = ''
  notice.value = ''
  try {
    if (action === 'publish') await publishPost(post.id)
    if (action === 'withdraw') await withdrawPost(post.id)
    if (action === 'restore') await restorePost(post.id)
    if (action === 'delete') await deletePost(post.id)
    notice.value = `操作成功：${post.title}`
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busyId.value = null
  }
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  loadTags()
})

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.fullPath, () => {
  readQuery()
  load()
}, { immediate: true })
usePaginationScroll(() => `${page.value}:${pageSize.value}`, () => loading.value)
</script>

<template>
  <section class="content-admin">
    <header class="content-admin__hero">
      <div>
        <h1>博客管理</h1>
        <p>管理文章生命周期、标签与前台时间线展示。</p>
      </div>
      <div class="content-admin__hero-actions">
        <RouterLink to="/useradmin/blog/taxonomy">分类与专题</RouterLink>
        <RouterLink to="/useradmin/blog/editor/new" class="is-primary">＋ 新建文章</RouterLink>
      </div>
    </header>

    <form class="content-admin__toolbar" @submit.prevent="search">
      <label>搜索标题 / 编号
        <input v-model.trim="filters.keyword" maxlength="100" placeholder="输入标题或 slug" />
      </label>
      <label>发布状态
        <select v-model="filters.status" @change="search">
          <option value="">全部状态</option>
          <option v-for="status in POST_STATUSES" :key="status.value" :value="status.value">{{ status.label }}</option>
        </select>
      </label>
      <label>标签
        <select v-model="filters.tagId" @change="search">
          <option value="">全部标签</option>
          <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }} · {{ tag.postCount || 0 }}</option>
        </select>
      </label>
      <button type="submit">筛选</button>
      <span class="content-admin__total">共 {{ data.total }} 篇内容</span>
    </form>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载文章…</p>

    <div class="content-admin__list">
      <article v-for="post in data.items" :key="post.id" class="content-card" tabindex="0" :aria-label="`编辑文章：${post.title}`" @click="router.push(`/useradmin/blog/editor/${post.id}`)" @keydown.enter.self="router.push(`/useradmin/blog/editor/${post.id}`)">
        <div class="content-card__body">
          <div class="content-card__heading">
            <div>
              <p class="content-card__slug">编号 {{ post.id }} · {{ post.slug }}</p>
              <h2>{{ post.title }}</h2>
            </div>
            <BlogStatusPill :status="post.status" />
          </div>

          <p class="content-card__summary">{{ post.summary || '（暂无摘要，发布时会按正文自动生成）' }}</p>

          <div class="content-card__tags">
            <span v-for="tag in post.tags" :key="tag.id"># {{ tag.name }}</span>
            <small v-if="!post.tags?.length">暂无标签</small>
          </div>

          <div class="content-card__foot">
            <div>
              <span>发布 {{ post.publishedAt ? dateLabel(post.publishedAt) : '尚未发布' }}</span>
              <span>更新 {{ dateLabel(post.updatedAt) }}</span>
              <span>正文{{ post.hasBody ? '已填写' : '为空' }}</span>
            </div>
            <div class="content-card__actions" @click.stop>
              <button type="button" @click="preview(post)">预览</button>
              <button type="button" @click="router.push(`/useradmin/blog/editor/${post.id}`)">编辑</button>
              <button v-if="canPublish(post.status)" type="button" :disabled="busyId === post.id" @click="act(post, 'publish')">发布</button>
              <button v-if="canWithdraw(post.status)" type="button" :disabled="busyId === post.id" @click="act(post, 'withdraw')">撤回</button>
              <button v-if="canRestore(post.status)" type="button" :disabled="busyId === post.id" @click="act(post, 'restore')">重新公开</button>
              <button v-if="canDelete(post.status)" type="button" class="is-danger" :disabled="busyId === post.id" @click="act(post, 'delete')">删除</button>
            </div>
          </div>
        </div>
      </article>

      <div v-if="!loading && !data.items.length" class="content-admin__empty">
        <strong>{{ filters.keyword || filters.status || filters.tagId ? '没有符合条件的文章' : '暂无文章' }}</strong>
        <span v-if="filters.keyword || filters.status || filters.tagId">
          <button class="text-button" type="button" @click="resetFilters">清除筛选条件</button>
        </span>
        <span v-else>新建第一篇内容，开始记录你的技术时间线。</span>
      </div>
    </div>

    <div v-if="data.total > 0" class="pagination content-admin__pagination">
      <span>
        共 {{ data.total }} 篇
        <template v-if="activeTag">· 标签「{{ activeTag.name }}」</template>
      </span>
      <div>
        <label class="content-admin__page-size">
          每页
          <select v-model.number="pageSize" @change="changePageSize">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>
        <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
        <button
          v-for="number in pageNumbers"
          :key="number"
          type="button"
          :class="{ 'is-active': number === page }"
          :disabled="loading"
          @click="changePage(number)"
        >{{ number }}</button>
        <button type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
      </div>
    </div>

    <AdminConfirmDialog ref="confirmDialog" />
  </section>
</template>
