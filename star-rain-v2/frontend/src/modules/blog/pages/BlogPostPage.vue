<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getPublicPost } from '../api/blogApi'
import { errorMessage } from '../../../shared/http'
import BlogPreview from '../components/BlogPreview.vue'

/*
 * BLOG-002 阅读文章。
 *
 * 按 slug 读取：后端对草稿、已撤回与不存在的 slug 一律返回 BLOG_POST_NOT_FOUND，
 * 因此这里只需要区分「404」与「其它错误」，不需要也不应该去猜文章的状态。
 */
const route = useRoute()
const router = useRouter()

const post = ref(null)
const loading = ref(false)
const error = ref('')
const notFound = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  notFound.value = false
  post.value = null
  try {
    post.value = await getPublicPost(route.params.slug)
  } catch (cause) {
    if (cause?.response?.status === 404) {
      notFound.value = true
    } else {
      error.value = errorMessage(cause)
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<template>
  <main class="page-container blog-public">
    <p v-if="loading" class="loading" role="status">正在加载文章…</p>
    <p v-else-if="error" class="error" role="alert">{{ error }}</p>

    <template v-else-if="notFound">
      <div class="surface-card blog-notfound">
        <h1>文章不可读</h1>
        <p>这篇文章不存在，或者已经被作者撤回。撤回的文章对外与「不存在」完全一致，因此无法区分。</p>
        <RouterLink class="primary-button" to="/blog">返回博客首页</RouterLink>
      </div>
    </template>

    <template v-else-if="post">
      <nav class="blog-breadcrumb">
        <RouterLink to="/blog">← 返回博客列表</RouterLink>
      </nav>
      <BlogPreview :post="post" />
      <p class="blog-article__footer">
        <button class="text-button" type="button" @click="router.back()">回到上一页</button>
      </p>
    </template>
  </main>
</template>
