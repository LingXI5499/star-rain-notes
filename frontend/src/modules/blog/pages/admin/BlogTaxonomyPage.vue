<script setup>
/**
 * BlogTaxonomyPage.vue：博客模块的页面组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/composables/paginationScroll、vue、vue-router、../../api/blogApi、../../../../shared/http、../../../../shared/composables/useListQuery。
 * 主要响应式状态：route、result、loading、saving、error、notice、tagDialog、tagForm。
 * 阅读入口：load（加载当前操作）；search（搜索的处理函数）；changePage（change分页的处理函数）；applyPageSize（应用分页大小）；choosePageSize（choose分页大小的处理函数）。
 */


import { usePaginationScroll } from '../../../../shared/composables/paginationScroll'
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
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
const { filters, page, pageSize, read: readQuery, write: writeQuery, reset: resetQuery } = useListQuery({
  defaults: { keyword: '', status: '' },
  defaultPageSize: 24,
  pageSizes: [12, 24, 48],
  // 允许自定义每页条数（1..100），与后端 BlogQueryRules.PAGE_SIZE_MAX 对齐
  maxPageSize: 100,
})

/**
 * 结果；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const result = ref({ items: [], total: 0, page: 1, pageSize: 24 })
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
/**
 * 标签弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tagDialog = ref(null)
/**
 * 标签Form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tagForm = reactive({ id: null, name: '' })
let loadVersion = 0

/*
 * 每页条数：常用档位之外还允许自己填（用户要求「可以来个自定义的」）。
 * 后端 BlogQueryRules.PAGE_SIZE_MAX = 100，超出会被拒绝，所以这里先把输入夹到 1..100。
 */
const PAGE_SIZE_OPTIONS = [12, 24, 48]
const PAGE_SIZE_MAX = 100
/**
 * customOpen；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const customOpen = ref(false)
/**
 * custom值；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const customValue = ref('')
// URL 里带着一个非档位的 pageSize（例如别人分享的 ?pageSize=30）时，直接进自定义态
/**
 * 是否Custom大小；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const isCustomSize = computed(() => !PAGE_SIZE_OPTIONS.includes(pageSize.value))

/**
 * 标签列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const tags = computed(() => result.value.items || [])
/**
 * 总数Pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const totalPages = computed(() => Math.max(1, Math.ceil(result.value.total / pageSize.value)))
// 「文章关联」只统计当前这一页：分页之后它不再是全站合计，标签就把口径写清楚
/**
 * 分页Relations；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pageRelations = computed(() => tags.value.reduce((sum, tag) => sum + (tag.postCount || 0), 0))
/**
 * 是否Filtered；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const isFiltered = computed(() => Boolean(filters.keyword.trim() || filters.status))

/**
 * 加载当前操作。
 * 主要协作：listAdminTags、filters.keyword.trim、errorMessage。
 * 更新的响应式状态包括 loading、error、result；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function changePage(next) {
  if (next < 1 || next > totalPages.value || next === page.value) return
  page.value = next
  writeQuery()
}

/*
 * 每页条数：既给常用档位，也允许自己填。
 * 后端 BlogQueryRules 的硬边界是 1..100，这里先夹住再写 URL ——
 * 让用户看到「输入被收拢到合法值」，而不是提交后收到一个 BLOG_QUERY_INVALID。
 */
function applyPageSize(raw) {
  const size = Math.min(PAGE_SIZE_MAX, Math.max(1, Math.floor(Number(raw) || pageSize.value)))
  customValue.value = String(size)
  if (size === pageSize.value) return
  pageSize.value = size
  page.value = 1
  writeQuery()
}

/**
 * choose分页大小的处理函数。
 * 主要协作：applyPageSize。
 * 更新的响应式状态包括 customOpen、customValue；渲染由 Vue 的依赖跟踪触发。
 * @param event 事件；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function choosePageSize(event) {
  const value = event.target.value
  if (value === 'custom') {
    customOpen.value = true
    customValue.value = String(pageSize.value)
    return
  }
  customOpen.value = false
  applyPageSize(Number(value))
}

// 自定义输入允许边输边改，回车或失焦时才收拢成合法值并查询
/**
 * 提交Custom分页大小。
 * 主要协作：applyPageSize。
 */
function commitCustomPageSize() {
  applyPageSize(customValue.value)
}

/**
 * 打开标签弹窗。
 * 主要协作：Object.assign、nextTick、tagDialog.value.showModal。
 * 更新的响应式状态包括 error；渲染由 Vue 的依赖跟踪触发。
 * @param tag = null 标签空值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function openTagDialog(tag = null) {
  error.value = ''
  Object.assign(tagForm, tag ? { id: tag.id, name: tag.name } : { id: null, name: '' })
  await nextTick()
  tagDialog.value?.showModal()
}

/**
 * 提交标签。
 * 主要协作：tagForm.name.trim、updateTag、createTaxonomy、tagDialog.value.close、load。
 * 更新的响应式状态包括 error、saving、notice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 切换标签状态。
 * 主要协作：enableTag、disableTag、load、errorMessage。
 * 更新的响应式状态包括 saving、error、notice；渲染由 Vue 的依赖跟踪触发。
 * @param tag 标签；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.fullPath, () => {
  readQuery()
  customOpen.value = !PAGE_SIZE_OPTIONS.includes(pageSize.value)
  customValue.value = String(pageSize.value)
  load()
}, { immediate: true })
usePaginationScroll(() => `${page.value}:${pageSize.value}`, () => loading.value)
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
      <article v-for="tag in tags" :key="tag.id" class="tag-card" tabindex="0" :aria-label="`编辑标签：${tag.name}`" @click="openTagDialog(tag)" @keydown.enter.self="openTagDialog(tag)">
        <div class="tag-card__top">
          <span>#</span>
          <strong>{{ tag.name }}</strong>
          <em>{{ tag.postCount || 0 }}</em>
        </div>
        <footer @click.stop>
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
      <div class="tag-admin__page-controls">
        <label class="tag-admin__page-size">每页
          <select :value="customOpen || isCustomSize ? 'custom' : pageSize" @change="choosePageSize">
            <option v-for="size in PAGE_SIZE_OPTIONS" :key="size" :value="size">{{ size }}</option>
            <option value="custom">自定义…</option>
          </select>
        </label>
        <span v-if="customOpen || isCustomSize" class="tag-admin__page-custom">
          <input
            v-model="customValue"
            type="number"
            min="1"
            :max="PAGE_SIZE_MAX"
            aria-label="每页条数，1 到 100"
            @keyup.enter="commitCustomPageSize"
            @blur="commitCustomPageSize"
          />
          <button type="button" @click="commitCustomPageSize">应用</button>
        </span>
      </div>
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/* 第三格改成整行可换行的筛选条：全局那套是给单个搜索框定的宽度 */
.tag-admin__stats--tags {
  grid-template-columns: 150px 150px minmax(0, 1fr);
  align-items: end;
}

/**
 * 样式范围：.tag-admin__filters。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tag-admin__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 10px;
}

/**
 * 样式范围：.tag-admin__filters label。
 */
.tag-admin__filters label { flex: 1 1 160px; }

/**
 * 样式范围：.tag-admin__filters label:last-of-type。
 */
.tag-admin__filters label:last-of-type { flex: 0 1 130px; }

/**
 * 样式范围：.tag-admin__filters button。
 */
.tag-admin__filters button { white-space: nowrap; }

/**
 * 样式范围：.tag-admin__pages。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
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

/**
 * 样式范围：.tag-admin__page-jump。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tag-admin__page-jump { display: flex; align-items: center; gap: 12px; }

/**
 * 样式范围：.tag-admin__pages button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-admin__pages button {
  padding: 7px 12px;
  border: 1px solid var(--border);
  border-radius: 9px;
  color: var(--primary);
  background: var(--bg-surface);
  cursor: pointer;
}

/**
 * 样式范围：.tag-admin__pages button:disabled。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-admin__pages button:disabled { color: var(--text-muted); cursor: not-allowed; }

/**
 * 样式范围：.tag-admin__page-size。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tag-admin__page-size { display: flex; align-items: center; gap: 8px; }

/**
 * 样式范围：.tag-admin__page-size select。
 */
.tag-admin__page-size select { width: auto; min-height: 34px; padding: 4px 8px; }

/**
 * 样式范围：.tag-admin__page-controls。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tag-admin__page-controls { display: flex; align-items: center; gap: 10px; }

/**
 * 样式范围：.tag-admin__page-custom。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.tag-admin__page-custom { display: flex; align-items: center; gap: 6px; }

/**
 * 样式范围：.tag-admin__page-custom input。
 */
.tag-admin__page-custom input {
  width: 84px;
  min-height: 34px;
  padding: 4px 8px;
  text-align: center;
}

/**
 * 样式范围：.tag-admin__page-custom button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.tag-admin__page-custom button {
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  color: var(--primary);
  background: var(--bg-surface);
  font-size: 12px;
  cursor: pointer;
}

/**
 * 响应式或设备条件：(max-width: 900px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 900px) {
  /**
   * 样式范围：.tag-admin__stats--tags。
   */
  .tag-admin__stats--tags { grid-template-columns: 1fr 1fr; }

  /**
   * 样式范围：.tag-admin__filters。
   */
  .tag-admin__filters { grid-column: 1 / -1; }
}

/**
 * 响应式或设备条件：(max-width: 720px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 720px) {
  /**
   * 样式范围：.tag-admin__pages。
   */
  .tag-admin__pages { align-items: flex-start; flex-direction: column; }
}
</style>
