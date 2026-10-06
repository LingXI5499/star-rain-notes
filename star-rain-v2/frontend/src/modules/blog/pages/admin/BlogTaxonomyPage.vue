<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { createTag, disableTag, enableTag, listAdminTags, updateTag } from '../../api/blogApi'
import { errorMessage } from '../../../../shared/http'
import { useListQuery } from '../../../../shared/composables/useListQuery'
import { createTaxonomy } from '../../components/admin/tagSlug'
import { taxonomyStatusLabel } from '../../support/display'

/*
 * 标签管理（BLOG-004）。
 *
 * 原先标签与专题挤在同一页，用户要求拆开：
 *   本页只做标签（`/useradmin/blog/taxonomy`，保留原路径，后台导航不用改），
 *   专题搬到 `/useradmin/blog/topics`（BlogTopicManagePage），两页互相给入口。
 *
 * 标签不像专题那样有成员与顺序，只有「名称 + 被引用次数 + 启用状态」，
 * 所以这里是一张卡片网格 + 分页：50 多个标签一屏铺满要滚很久，翻页比滚动好找。
 *
 * 分页、搜索、状态筛选都走服务端（标签列表接口本来就支持 page/pageSize/keyword/status），
 * 条件写回 URL，刷新与前进后退都不会丢状态。
 *
 * 视觉沿用 admin.css 里既有的 .tag-admin__* / .tag-card（那一套是按 V1 BlogTagView 定的），
 * 本文件只补分页条与筛选行，避免同一套样式在全局与页面里各写一份后悄悄分叉。
 *
 * 仍然没有物理删除：后端 BlogAdminTagController 明确不提供 DELETE
 * （已被文章使用的标签删掉，历史文章就会指向不存在的标签），下架请用停用。
 */
const route = useRoute()
const { filters, page, pageSize, read: readQuery, write: writeQuery, reset: resetQuery } = useListQuery({
  defaults: { keyword: '', status: '' },
  defaultPageSize: 24,
  pageSizes: [12, 24, 48],
})

const result = ref({ items: [], total: 0, page: 1, pageSize: 24 })
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const tagDialog = ref(null)
const tagForm = reactive({ id: null, name: '' })
let loadVersion = 0

const tags = computed(() => result.value.items || [])
const totalPages = computed(() => Math.max(1, Math.ceil(result.value.total / pageSize.value)))
// 「文章关联」只统计当前这一页：分页之后它不再是全站合计，标签就把口径写清楚
const pageRelations = computed(() => tags.value.reduce((sum, tag) => sum + (tag.postCount || 0), 0))
const isFiltered = computed(() => Boolean(filters.keyword.trim() || filters.status))

async function load() {
  const version = ++loadVersion
  loading.value = true
  error.value = ''
  try {
    const response = await listAdminTags({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
    })
    if (version === loadVersion) result.value = response
  } catch (cause) {
    if (version === loadVersion) error.value = errorMessage(cause)
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

async function search() {
  page.value = 1
  const previous = route.fullPath
  await writeQuery()
  if (route.fullPath === previous) await load()
}

function changePage(next) {
  if (next < 1 || next > totalPages.value || next === page.value) return
  page.value = next
  writeQuery()
}

function changePageSize(event) {
  pageSize.value = Number(event.target.value)
  page.value = 1
  writeQuery()
}

async function openTagDialog(tag = null) {
  error.value = ''
  Object.assign(tagForm, tag ? { id: tag.id, name: tag.name } : { id: null, name: '' })
  await nextTick()
  tagDialog.value?.showModal()
}

async function submitTag() {
  if (!tagForm.name.trim()) {
    error.value = '请填写标签名称。'
    return
  }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    // 创建时由服务端派生唯一地址；改名时不传 slug，已有公开链接保持不变。
    const payload = { name: tagForm.name.trim() }
    if (tagForm.id) {
      await updateTag(tagForm.id, payload)
      notice.value = `标签「${tagForm.name}」已更新。`
    } else {
      await createTaxonomy(createTag, payload.name, 'tag', 100)
      notice.value = `标签「${tagForm.name}」已创建。`
    }
    tagDialog.value?.close()
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

async function toggleTagStatus(tag) {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    if (tag.status === 'DISABLED') {
      await enableTag(tag.id)
      notice.value = `标签「${tag.name}」已启用。`
    } else {
      await disableTag(tag.id)
      notice.value = `标签「${tag.name}」已停用：历史绑定保留，但不能绑定到新文章。`
    }
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

// URL 是筛选与分页的唯一出口：手改地址栏、前进后退都会走到这里
watch(() => route.fullPath, () => {
  readQuery()
  load()
}, { immediate: true })
</script>

<template>
  <section class="tag-admin">
    <header class="tag-admin__hero">
      <div>
        <p>TAG LIBRARY · 内容索引</p>
        <h1>标签管理</h1>
        <span>标签只有名称与被引用次数，没有顺序；下架用停用，历史绑定会保留。</span>
      </div>
      <div class="content-admin__hero-actions">
        <RouterLink to="/useradmin/blog/manage">← 返回博客管理</RouterLink>
        <RouterLink to="/useradmin/blog/topics">专题管理 →</RouterLink>
        <button class="primary-button" type="button" @click="openTagDialog()">＋ 新建标签</button>
      </div>
    </header>

    <div class="tag-admin__stats tag-admin__stats--tags">
      <div><strong>{{ result.total }}</strong><span>{{ isFiltered ? '筛选结果' : '标签总数' }}</span></div>
      <div><strong>{{ pageRelations }}</strong><span>本页文章关联</span></div>
      <form class="tag-admin__filters" @submit.prevent="search">
        <label>搜索标签<input v-model.trim="filters.keyword" maxlength="100" placeholder="输入标签名称或编号" /></label>
        <label>状态
          <select v-model="filters.status" @change="search">
            <option value="">全部</option>
            <option value="ENABLED">启用中</option>
            <option value="DISABLED">已停用</option>
          </select>
        </label>
        <button class="primary-button" type="submit">查询</button>
        <button v-if="isFiltered" type="button" @click="resetQuery">清除筛选</button>
      </form>
    </div>

    <p v-if="error && !tagDialog?.open" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载标签…</p>

    <div class="tag-grid">
      <article v-for="tag in tags" :key="tag.id" class="tag-card">
        <div class="tag-card__top">
          <span>#</span>
          <strong>{{ tag.name }}</strong>
          <em>{{ tag.postCount || 0 }}</em>
        </div>
        <footer>
          <span :class="['status-chip', tag.status === 'DISABLED' && 'status-chip--danger']">{{ taxonomyStatusLabel(tag.status) }}</span>
          <div>
            <button type="button" @click="openTagDialog(tag)">编辑</button>
            <button type="button" :disabled="saving" @click="toggleTagStatus(tag)">
              {{ tag.status === 'DISABLED' ? '启用' : '停用' }}
            </button>
          </div>
        </footer>
      </article>
      <div v-if="!loading && !tags.length" class="tag-admin__empty">
        {{ isFiltered ? '没有匹配的标签' : '暂无标签' }}
      </div>
    </div>

    <nav v-if="result.total > 0" class="tag-admin__pages" aria-label="标签分页">
      <span>共 {{ result.total }} 个标签</span>
      <label class="tag-admin__page-size">每页
        <select :value="pageSize" @change="changePageSize">
          <option v-for="size in [12, 24, 48]" :key="size" :value="size">{{ size }}</option>
        </select>
      </label>
      <div class="tag-admin__page-jump">
        <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
      </div>
    </nav>

    <p class="tag-admin__note">
      标签不提供物理删除：已被文章使用的标签一旦删除，历史文章就会指向不存在的标签。
      需要下架时用「停用」，历史绑定保留。专题在「专题管理」页，规则不同（专题有成员与顺序，空专题可以删除）。
    </p>

    <dialog ref="tagDialog" aria-labelledby="tag-dialog-title" @cancel.prevent="tagDialog?.close()">
      <h2 id="tag-dialog-title">{{ tagForm.id ? '编辑标签' : '新建标签' }}</h2>
      <form class="form-stack" @submit.prevent="submitTag">
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <label>名称<input v-model="tagForm.name" maxlength="100" placeholder="例如：Spring Boot" /></label>
        <div class="dialog-actions">
          <button type="button" @click="tagDialog?.close()">取消</button>
          <button class="primary-button" type="submit" :disabled="saving">{{ tagForm.id ? '保存' : '创建' }}</button>
        </div>
      </form>
    </dialog>
  </section>
</template>

<style scoped>
/* 第三格改成整行可换行的筛选条：全局那套是给单个搜索框定的宽度 */
.tag-admin__stats--tags {
  grid-template-columns: 150px 150px minmax(0, 1fr);
  align-items: end;
}

.tag-admin__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 10px;
}

.tag-admin__filters label { flex: 1 1 160px; }

.tag-admin__filters label:last-of-type { flex: 0 1 130px; }

.tag-admin__filters button { white-space: nowrap; }

.tag-admin__pages {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: var(--space-5);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 12px;
}

.tag-admin__page-jump { display: flex; align-items: center; gap: 12px; }

.tag-admin__pages button {
  padding: 7px 12px;
  border: 1px solid var(--border);
  border-radius: 9px;
  color: var(--primary);
  background: var(--bg-surface);
  cursor: pointer;
}

.tag-admin__pages button:disabled { color: var(--text-muted); cursor: not-allowed; }

.tag-admin__page-size { display: flex; align-items: center; gap: 8px; }

.tag-admin__page-size select { width: auto; min-height: 34px; padding: 4px 8px; }

@media (max-width: 900px) {
  .tag-admin__stats--tags { grid-template-columns: 1fr 1fr; }

  .tag-admin__filters { grid-column: 1 / -1; }
}

@media (max-width: 720px) {
  .tag-admin__pages { align-items: flex-start; flex-direction: column; }
}
</style>
