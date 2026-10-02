<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  deletePost, listAdminPosts, listAdminTags, listAdminTopics,
  publishPost, restorePost, withdrawPost,
} from '../../api/blogApi'
import { errorMessage } from '../../../../shared/http'
import BlogTagList from '../../components/BlogTagList.vue'
import BlogTopicPanel from '../../components/BlogTopicPanel.vue'
import {
  POST_STATUSES, canDelete, canPublish, canRestore, canWithdraw, dateLabel, postStatusLabel,
} from '../../support/display'

/*
 * 后台文章列表（BLOG-003 / BLOG-008 / BLOG-009 的入口）。
 *
 * 每个状态只显示它允许的操作：按钮的可用性直接对应后端状态机，
 * 让「先撤回再删除」这类规则在界面上就能看出来，而不是靠一次失败的请求来教用户。
 */
const router = useRouter()

const filters = reactive({ keyword: '', status: '', tagId: '', topicId: '' })
const page = ref(1)
const pageSize = 20
const data = ref({ items: [], total: 0, page: 1, pageSize })
const tags = ref([])
const topics = ref([])
const loading = ref(false)
const error = ref('')
const notice = ref('')
const busyId = ref(null)

const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / pageSize)))

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await listAdminPosts({
      page: page.value,
      pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      tagId: filters.tagId || undefined,
      topicId: filters.topicId || undefined,
    })
    data.value.pageSize = pageSize
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [tagPage, topicPage] = await Promise.all([
      listAdminTags({ page: 1, pageSize: 100 }),
      listAdminTopics({ page: 1, pageSize: 100 }),
    ])
    tags.value = tagPage.items
    topics.value = topicPage.items
  } catch {
    tags.value = []
    topics.value = []
  }
}

function search() {
  page.value = 1
  load()
}

function changePage(next) {
  page.value = next
  load()
}

async function act(post, action) {
  busyId.value = post.id
  error.value = ''
  notice.value = ''
  try {
    if (action === 'publish') await publishPost(post.id)
    if (action === 'withdraw') await withdrawPost(post.id)
    if (action === 'restore') await restorePost(post.id)
    if (action === 'delete') {
      // 二次确认：物理删除不可撤销，且会一并解除该文章的媒体引用
      if (!window.confirm(`确认删除《${post.title}》？该文章的标签、专题关系与媒体引用都会被解除。`)) return
      await deletePost(post.id)
    }
    notice.value = `操作成功：${post.title}`
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busyId.value = null
  }
}

onMounted(() => {
  loadOptions()
  load()
})
</script>

<template>
  <main class="page-container">
    <div class="page-heading">
      <p class="eyebrow">BLOG ADMIN</p>
      <h1>博客文章</h1>
      <p>草稿 → 发布 ↔ 撤回，不需要走通用审批：V2 的博客由超级管理员直接维护。</p>
    </div>

    <section class="surface-card">
      <form class="toolbar toolbar--wrap" @submit.prevent="search">
        <label>关键词<input v-model.trim="filters.keyword" maxlength="100" placeholder="标题或 slug" /></label>
        <label>状态
          <select v-model="filters.status" @change="search">
            <option value="">全部</option>
            <option v-for="status in POST_STATUSES" :key="status.value" :value="status.value">{{ status.label }}</option>
          </select>
        </label>
        <label>标签
          <select v-model="filters.tagId" @change="search">
            <option value="">全部</option>
            <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
          </select>
        </label>
        <label>专题
          <select v-model="filters.topicId" @change="search">
            <option value="">全部</option>
            <option v-for="topic in topics" :key="topic.id" :value="topic.id">{{ topic.name }}</option>
          </select>
        </label>
        <button class="primary-button" type="submit">查询</button>
        <RouterLink class="button-link" to="/admin/blog/posts/new">新建文章</RouterLink>
        <RouterLink class="button-link" to="/admin/blog/taxonomy">分类与专题</RouterLink>
      </form>

      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <p v-if="notice" class="notice" role="status">{{ notice }}</p>
      <p v-if="loading" class="loading" role="status">正在加载文章…</p>

      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>文章</th><th>状态</th><th>标签</th><th>专题</th>
              <th>发布时间</th><th>更新时间</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="post in data.items" :key="post.id">
              <td>
                <strong>{{ post.title }}</strong>
                <small>/blog/posts/{{ post.slug }} · 正文{{ post.hasBody ? '已填写' : '为空' }}</small>
              </td>
              <td>
                <span :class="['status-chip', post.status === 'DRAFT' && 'status-chip--muted',
                  post.status === 'WITHDRAWN' && 'status-chip--danger']">{{ postStatusLabel(post.status) }}</span>
              </td>
              <td><BlogTagList :tags="post.tags" empty-text="—" /></td>
              <td><BlogTopicPanel :topics="post.topics" empty-text="—" /></td>
              <td>{{ dateLabel(post.publishedAt) }}</td>
              <td>{{ dateLabel(post.updatedAt) }}</td>
              <td class="table-actions">
                <RouterLink class="link-button" :to="`/admin/blog/posts/${post.id}`">编辑</RouterLink>
                <RouterLink v-if="post.status === 'PUBLISHED'" class="link-button" :to="`/blog/posts/${post.slug}`" target="_blank">前台</RouterLink>
                <button v-if="canPublish(post.status)" class="link-button" type="button" :disabled="busyId === post.id" @click="act(post, 'publish')">发布</button>
                <button v-if="canWithdraw(post.status)" class="link-button" type="button" :disabled="busyId === post.id" @click="act(post, 'withdraw')">撤回</button>
                <button v-if="canRestore(post.status)" class="link-button" type="button" :disabled="busyId === post.id" @click="act(post, 'restore')">恢复</button>
                <button v-if="canDelete(post.status)" class="link-button blog-danger" type="button" :disabled="busyId === post.id" @click="act(post, 'delete')">删除</button>
              </td>
            </tr>
            <tr v-if="!loading && !data.items.length"><td colspan="7" class="empty-state">还没有文章，先新建一篇草稿。</td></tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span>共 {{ data.total }} 篇</span>
        <div>
          <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <span>{{ page }} / {{ totalPages }}</span>
          <button type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </div>
    </section>
  </main>
</template>
