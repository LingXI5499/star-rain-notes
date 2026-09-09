<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { deletePost, fetchAdminPosts, fetchAdminTags, publishPost, withdrawPost, type AdminBlogTag, type AdminPostPage, type AdminPostSummary } from '@/api/blog'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const page = ref<AdminPostPage | null>(null)
const tags = ref<AdminBlogTag[]>([])
const filters = reactive({ page: 1, pageSize: 10, status: '', tag: '', q: '' })
const statusLabels: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }

async function load() {
  loading.value = true
  try {
    page.value = await fetchAdminPosts({ page: filters.page, pageSize: filters.pageSize, status: filters.status || undefined, tag: filters.tag || undefined, q: filters.q || undefined })
  } catch { ElMessage.error('加载文章列表失败。') } finally { loading.value = false }
}
function search() { filters.page = 1; void load() }
function formatTime(iso: string | null) { if (!iso) return '尚未发布'; const date = new Date(iso); return Number.isNaN(date.getTime()) ? iso : date.toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) }
async function togglePublish(row: AdminPostSummary) { try { if (row.publishStatus === 'PUBLISHED') { await withdrawPost(row.id); ElMessage.success('文章已撤回。') } else { await publishPost(row.id); ElMessage.success('文章已发布。') } await load() } catch { ElMessage.error('操作失败。') } }
function preview(row: AdminPostSummary) { if (row.publishStatus !== 'PUBLISHED') { ElMessage.info('文章发布后才能在前台预览。'); return } window.open(`/blog/${row.slug}`, '_blank', 'noopener,noreferrer') }
async function remove(row: AdminPostSummary) { try { await ElMessageBox.confirm(`确定删除文章「${row.title}」？此操作不可恢复。`, '删除确认', { type: 'warning' }); await deletePost(row.id); ElMessage.success('文章已删除。'); await load() } catch { /* cancel or request failure */ } }
onMounted(async () => { const result = await Promise.allSettled([fetchAdminTags(), load()]); if (result[0].status === 'fulfilled') tags.value = result[0].value })
</script>

<template>
  <section class="content-admin">
    <header class="content-admin__hero">
      <div><h1>博客管理</h1><p>管理文章生命周期、标签与前台时间线展示。</p></div>
      <el-button type="primary" @click="router.push({ name: 'admin-blog-new' })">＋ 新建文章</el-button>
    </header>
    <div class="content-admin__toolbar">
      <el-input v-model="filters.q" placeholder="搜索标题 / 编号" clearable @keyup.enter="search" @clear="search" />
      <el-select v-model="filters.status" placeholder="发布状态" clearable @change="search"><el-option label="草稿" value="DRAFT" /><el-option label="已发布" value="PUBLISHED" /><el-option label="已撤回" value="WITHDRAWN" /></el-select>
      <el-select v-model="filters.tag" placeholder="标签" filterable clearable @change="search"><el-option v-for="tag in tags" :key="tag.id" :label="`${tag.name} · ${tag.postCount}`" :value="tag.slug" /></el-select>
      <el-button @click="search">筛选</el-button><span class="content-admin__total">{{ page?.total ?? 0 }} 篇内容</span>
    </div>
    <div v-loading="loading" class="content-admin__list">
      <article v-for="row in page?.items ?? []" :key="row.id" class="content-card">
        <div class="content-card__body">
          <div class="content-card__heading"><div><p class="content-card__slug">编号 {{ row.slug }}</p><h2>{{ row.title }}</h2></div><span class="status-pill" :class="`status-pill--${row.publishStatus.toLowerCase()}`">{{ statusLabels[row.publishStatus] ?? row.publishStatus }}</span></div>
          <p class="content-card__summary">{{ row.summary }}</p>
          <div class="content-card__tags"><span v-for="tag in row.tags" :key="tag.id"># {{ tag.name }}</span><small v-if="!row.tags.length">暂无标签</small></div>
          <div class="content-card__foot"><div><span>发布 {{ formatTime(row.publishedAt) }}</span><span>更新 {{ formatTime(row.updatedAt) }}</span></div><div class="content-card__actions"><button type="button" @click="preview(row)">预览</button><button type="button" @click="router.push(`/admin/blog/${row.id}/edit`)">编辑</button><button v-if="auth.isSuperAdmin" type="button" @click="togglePublish(row)">{{ row.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button><button v-if="auth.isSuperAdmin" type="button" class="is-danger" @click="remove(row)">删除</button></div></div>
        </div>
      </article>
      <div v-if="!loading && !(page?.items.length)" class="content-admin__empty"><strong>暂无文章</strong><span>新建第一篇内容，开始记录你的技术时间线。</span></div>
    </div>
    <el-pagination v-if="page && page.total > 0" v-model:current-page="filters.page" v-model:page-size="filters.pageSize" :total="page.total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" class="content-admin__pagination" @change="load" />
  </section>
</template>

<style scoped>
.content-admin__hero{display:flex;align-items:end;justify-content:space-between;gap:var(--space-6);margin-bottom:var(--space-6)}.content-admin__hero h1{margin-bottom:5px;font-size:34px;letter-spacing:-.03em}.content-admin__hero p:last-child{color:var(--text-muted);font-size:13px}.content-admin__toolbar{display:grid;grid-template-columns:minmax(220px,1fr) 150px 170px auto auto;align-items:center;gap:var(--space-3);margin-bottom:var(--space-5);padding:14px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.content-admin__total{justify-self:end;color:var(--text-muted);font-size:12px}.content-admin__list{min-height:180px;display:grid;gap:var(--space-4)}.content-card{display:grid;grid-template-columns:minmax(0,1fr);overflow:hidden;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);transition:transform 170ms ease,border-color 170ms ease,box-shadow 170ms ease}.content-card:hover{border-color:color-mix(in srgb,var(--primary) 38%,var(--border));transform:translateY(-2px);box-shadow:0 15px 34px rgb(0 0 0/.065)}.content-card__body{min-width:0;padding:var(--space-5)}.content-card__heading{display:flex;align-items:flex-start;justify-content:space-between;gap:var(--space-4)}.content-card__slug{margin-bottom:4px;color:var(--accent);font:600 10px/1.4 ui-monospace,SFMono-Regular,Menlo,monospace}.content-card h2{font-size:20px;line-height:1.4}.content-card__summary{display:-webkit-box;overflow:hidden;margin:10px 0;color:var(--text-secondary);font-size:13px;line-height:1.7;-webkit-box-orient:vertical;-webkit-line-clamp:2}.content-card__tags{display:flex;min-height:24px;flex-wrap:wrap;gap:6px}.content-card__tags span{padding:3px 9px;border-radius:999px;color:var(--primary);background:color-mix(in srgb,var(--primary) 9%,transparent);font-size:11px}.content-card__tags small{color:var(--text-muted)}.content-card__foot{display:flex;align-items:end;justify-content:space-between;gap:var(--space-4);margin-top:12px;padding-top:12px;border-top:1px solid var(--border)}.content-card__foot>div:first-child{display:flex;flex-wrap:wrap;gap:5px 14px;color:var(--text-muted);font-size:11px}.content-card__actions{display:flex;flex-shrink:0;gap:5px}.content-card__actions button{padding:6px 10px;border:1px solid var(--border);border-radius:999px;color:var(--text-secondary);background:var(--bg-page);cursor:pointer;font-size:12px;transition:color 160ms ease,border-color 160ms ease,background-color 160ms ease}.content-card__actions button:hover{border-color:var(--primary);color:var(--primary)}.content-card__actions .is-danger:hover{border-color:var(--danger);color:var(--danger)}.status-pill{flex-shrink:0;padding:5px 10px;border-radius:999px;font-size:11px;font-weight:700}.status-pill--published{color:#159362;background:color-mix(in srgb,#25b879 14%,transparent)}.status-pill--draft{color:#b47a17;background:color-mix(in srgb,#d79a2b 14%,transparent)}.status-pill--withdrawn{color:var(--text-muted);background:var(--bg-subtle)}.content-admin__empty{display:grid;min-height:220px;place-content:center;gap:6px;border:1px dashed var(--border-strong);border-radius:18px;color:var(--text-muted);text-align:center}.content-admin__empty strong{color:var(--text-primary)}.content-admin__pagination{margin-top:var(--space-6)}@media(prefers-reduced-motion:reduce){.content-card,.content-card__actions button{transition:none}}@media(max-width:900px){.content-admin__toolbar{grid-template-columns:1fr 1fr}.content-admin__total{justify-self:start}.content-card__foot{align-items:flex-start;flex-direction:column}}@media(max-width:620px){.content-admin__hero{align-items:flex-start}.content-admin__toolbar{grid-template-columns:1fr}.content-card__actions{width:100%;flex-wrap:wrap}}
</style>
