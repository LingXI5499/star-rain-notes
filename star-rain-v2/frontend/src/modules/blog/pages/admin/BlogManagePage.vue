<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  deletePost, listAdminPosts, listAdminTags, publishPost, restorePost, withdrawPost,
} from '../../api/blogApi'
import { errorMessage } from '../../../../shared/http'
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
 * 预览跳到的是公开站的文章地址（/blog 属公开站入口，在管理站域名下会被守卫挡回），
 * 因此这里用公开站的绝对地址，只对已发布文章开放。
 * 该地址由 AdminShell 通过 provide 给出（管理站域名去掉 admin. 前缀就是公开站）。
 */
const publicSiteUrl = inject('adminPublicSiteUrl', '')

const router = useRouter()

const filters = reactive({ keyword: '', status: '', tagId: '' })
const page = ref(1)
const pageSize = ref(10)
const data = ref({ items: [], total: 0, page: 1, pageSize: 10 })
const tags = ref([])
const loading = ref(false)
const error = ref('')
const notice = ref('')
const busyId = ref(null)
const confirmDialog = ref(null)

const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / pageSize.value)))
const activeTag = computed(() => tags.value.find((tag) => tag.id === Number(filters.tagId)) || null)

// V1 的 el-pagination 是完整分页器；这里用同样的窗口策略（当前页两侧各两页）
const pageNumbers = computed(() => {
  const total = totalPages.value
  const start = Math.max(1, Math.min(page.value - 2, total - 4))
  const end = Math.min(total, start + 4)
  return Array.from({ length: Math.max(0, end - start + 1) }, (_, index) => start + index)
})

function previewUrl(post) {
  const origin = typeof publicSiteUrl === 'string' ? publicSiteUrl : publicSiteUrl.value
  if (!origin || !post.slug) return ''
  return `${origin.replace(/\/$/, '')}/blog/posts/${post.slug}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await listAdminPosts({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      tagId: filters.tagId || undefined,
    })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  try {
    const result = await listAdminTags({ page: 1, pageSize: 100 })
    tags.value = result.items
  } catch {
    tags.value = []
  }
}

function search() {
  page.value = 1
  load()
}

function changePage(next) {
  if (next < 1 || next > totalPages.value || next === page.value) return
  page.value = next
  load()
}

function changePageSize() {
  page.value = 1
  load()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  filters.tagId = ''
  search()
}

function preview(post) {
  if (post.status !== 'PUBLISHED') {
    notice.value = '文章发布后才能在前台预览。'
    return
  }
  const url = previewUrl(post)
  if (!url) {
    error.value = '无法推导公开站地址，请在环境变量 VITE_PUBLIC_SITE_ORIGIN 里显式指定。'
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

async function act(post, action) {
  if (action === 'delete') {
    const accepted = await confirmDialog.value.ask(
      `确认删除《${post.title}》？该文章的标签、专题关系与媒体引用都会被解除，且不可恢复。`,
    )
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

onMounted(() => {
  loadTags()
  load()
})
</script>

<template>
  <section class="content-admin">
    <header class="content-admin__hero">
      <div>
        <h1>博客管理</h1>
        <p>管理文章生命周期、标签与前台时间线展示。</p>
      </div>
      <div class="content-admin__hero-actions">
        <RouterLink to="/admin/blog/taxonomy">分类与专题</RouterLink>
        <RouterLink to="/admin/blog/posts/new" class="is-primary">＋ 新建文章</RouterLink>
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
      <article v-for="post in data.items" :key="post.id" class="content-card">
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
            <div class="content-card__actions">
              <button type="button" @click="preview(post)">预览</button>
              <button type="button" @click="router.push(`/admin/blog/posts/${post.id}`)">编辑</button>
              <button v-if="canPublish(post.status)" type="button" :disabled="busyId === post.id" @click="act(post, 'publish')">发布</button>
              <button v-if="canWithdraw(post.status)" type="button" :disabled="busyId === post.id" @click="act(post, 'withdraw')">撤回</button>
              <button v-if="canRestore(post.status)" type="button" :disabled="busyId === post.id" @click="act(post, 'restore')">恢复</button>
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
