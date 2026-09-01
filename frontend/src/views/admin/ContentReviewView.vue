<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import {
  approveContentReview,
  fetchContentReviews,
  rejectContentReview,
  type ContentReview,
} from '@/api/account'

const reviews = ref<ContentReview[]>([])
const loading = ref(false)
const filters = reactive({ status: 'PENDING', page: 1, pageSize: 20 })

function label(review: ContentReview) {
  if (review.contentType === 'BLOG_POST') return '博客文章'
  return review.contentType
}

function actionLabel(review: ContentReview) {
  if (review.actionType === 'UPDATE') return '内容修改'
  return review.actionType
}

function statusLabel(status: string) {
  return status === 'PENDING' ? '待审核' : status === 'APPROVED' ? '已通过' : '已拒绝'
}

async function load() {
  loading.value = true
  try {
    reviews.value = await fetchContentReviews({
      status: filters.status || 'ALL',
      page: filters.page,
      pageSize: filters.pageSize,
    })
  } catch {
    ElMessage.error('加载审核列表失败。')
  } finally {
    loading.value = false
  }
}

async function approve(review: ContentReview) {
  try {
    const { value } = await ElMessageBox.prompt(`确认通过「${review.title}」的${actionLabel(review)}？`, '批准审核', {
      inputType: 'textarea',
      inputPlaceholder: '可选：填写审核备注',
      confirmButtonText: '批准并应用',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await approveContentReview(review.id, value)
    ElMessage.success('已批准并应用内容。')
    await load()
  } catch {
    // cancelled
  }
}

async function reject(review: ContentReview) {
  try {
    const { value } = await ElMessageBox.prompt(`拒绝「${review.title}」的${actionLabel(review)}？`, '拒绝审核', {
      inputType: 'textarea',
      inputPlaceholder: '建议填写原因，便于协作者修改',
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await rejectContentReview(review.id, value)
    ElMessage.success('已拒绝该审核。')
    await load()
  } catch {
    // cancelled
  }
}

onMounted(load)
</script>

<template>
  <section class="review-admin">
    <header class="review-admin__hero">
      <div>
        <p>CONTENT REVIEW · 协作发布</p>
        <h1>审核中心</h1>
        <span>普通管理员对已发布内容的修改会先进入这里，批准后才应用到线上内容。</span>
      </div>
      <el-select v-model="filters.status" style="width: 150px" @change="load">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已拒绝" value="REJECTED" />
        <el-option label="全部" value="ALL" />
      </el-select>
    </header>

    <div v-loading="loading" class="review-list">
      <article v-for="review in reviews" :key="review.id" class="review-card">
        <div>
          <span>{{ label(review) }} · {{ actionLabel(review) }}</span>
          <h2>{{ review.title }}</h2>
          <p>提交人 #{{ review.submittedBy || '—' }} · {{ review.createdAt }}</p>
        </div>
        <em :class="review.status.toLowerCase()">{{ statusLabel(review.status) }}</em>
        <nav v-if="review.status === 'PENDING'">
          <el-button type="success" @click="approve(review)">批准</el-button>
          <el-button type="danger" plain @click="reject(review)">拒绝</el-button>
        </nav>
        <p v-else class="review-card__note">审核备注：{{ review.reviewNote || '—' }}</p>
      </article>
      <div v-if="!loading && !reviews.length" class="review-empty">暂无符合条件的审核记录。</div>
    </div>
  </section>
</template>

<style scoped>
.review-admin__hero{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:24px}
.review-admin__hero p{margin:0 0 7px;color:var(--accent);font-size:10px;font-weight:800;letter-spacing:.15em}
.review-admin__hero h1{margin:0 0 6px;font-size:34px}
.review-admin__hero span{color:var(--text-muted);font-size:13px}
.review-list{display:flex;min-height:240px;flex-direction:column;gap:12px}
.review-card{display:grid;grid-template-columns:minmax(0,1fr) auto auto;gap:18px;align-items:center;padding:18px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface)}
.review-card span{color:var(--accent);font-size:10px;font-weight:800;letter-spacing:.12em}
.review-card h2{margin:6px 0;font-size:18px}
.review-card p{margin:0;color:var(--text-muted);font-size:12px}
.review-card em{padding:7px 10px;border-radius:999px;background:var(--bg-subtle);font-size:11px;font-style:normal}
.review-card em.pending{color:var(--warning)}
.review-card em.approved{color:var(--success)}
.review-card em.rejected{color:var(--danger)}
.review-card nav{display:flex;gap:8px}
.review-card__note{max-width:260px}
.review-empty{display:grid;min-height:220px;place-items:center;border:1px dashed var(--border);border-radius:18px;color:var(--text-muted)}
@media(max-width:720px){.review-admin__hero{align-items:flex-start;flex-direction:column}.review-card{grid-template-columns:1fr}.review-card nav{justify-content:flex-start}}
</style>
