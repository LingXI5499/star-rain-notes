<script setup>
/**
 * PortfolioManagePage.vue：作品模块的页面组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/composables/paginationScroll、vue、vue-router、../../api/portfolioApi、../../../../shared/http、../../../../shared/composables/useListQuery。
 * 主要响应式状态：route、categories、result、error、notice、loading、busyId、confirmDialog。
 * 阅读入口：techStackOf（技术技术栈所属的处理函数）；updatedLabel（更新标签的处理函数）；load（加载当前操作）；search（搜索的处理函数）；changePage（change分页的处理函数）。
 */


import { usePaginationScroll } from '../../../../shared/composables/paginationScroll'
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { deleteWork, getWorkTaxonomy, listAdminWorks, publishWork, restoreWork, withdrawWork } from '../../api/portfolioApi'
import { errorMessage } from '../../../../shared/http'
import { useListQuery } from '../../../../shared/composables/useListQuery'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'
import { accountPath } from '../../../../shared/viewMode'
import { stageLabels } from '../../support/workBlocks'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
const { filters, page, pageSize, read: readQuery, write: writeQuery, reset: resetQuery } = useListQuery({
  defaults: { type: '', categoryId: '', status: '', q: '' },
})
/**
 * categories；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const categories = ref([])
getWorkTaxonomy().then(data => { categories.value = data.categories }).catch(cause => { error.value = errorMessage(cause) })
/**
 * 结果；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const result = ref({ items: [], total: 0, pageSize: 20 })
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
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * busy标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busyId = ref('')
/**
 * 确认弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirmDialog = ref(null)
let loadVersion = 0
/**
 * 分页数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pageCount = computed(() => Math.max(1, Math.ceil(result.value.total / pageSize.value)))
const typeLabels = { SOFTWARE: '软件', VIDEO: '视频', MUSIC: '音乐', WRITING: '写作', OTHER: '其他' }
const statusLabels = { DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }
/**
 * 技术技术栈所属的处理函数。
 * 主要协作：.filter、.map、work.techStack.split、item.trim。
 * @param work 作品；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function techStackOf(work) {
  const stack = work.techStack ? work.techStack.split(/[,，、]/).map(item => item.trim()).filter(Boolean) : work.typeDetail?.techStack
  return Array.isArray(stack) ? stack : []
}

/**
 * 更新标签的处理函数。
 * 主要协作：.replace、work.updatedAt.slice。
 * @param work 作品；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function updatedLabel(work) {
  return work.updatedAt ? work.updatedAt.slice(0, 16).replace('T', ' ') : '—'
}

/**
 * 加载当前操作。
 * 主要协作：listAdminWorks、filters.q.trim、errorMessage。
 * 更新的响应式状态包括 loading、error、result；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  const version = ++loadVersion
  loading.value = true
  error.value = ''
  try {
    const response = await listAdminWorks({ page: page.value, pageSize: pageSize.value,
      categoryId: filters.categoryId || undefined, type: filters.type || undefined, status: filters.status || undefined, q: filters.q.trim() || undefined })
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
  if (next < 1 || next > pageCount.value || next === page.value) return
  page.value = next
  writeQuery()
}

/**
 * change状态的处理函数。
 * 主要协作：withdrawWork、restoreWork、publishWork、load、errorMessage。
 * 更新的响应式状态包括 busyId、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param work 作品；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function changeStatus(work) {
  busyId.value = work.id
  error.value = ''
  notice.value = ''
  try {
    if (work.status === 'PUBLISHED') await withdrawWork(work.id)
    else if (work.status === 'WITHDRAWN') await restoreWork(work.id)
    else await publishWork(work.id)
    notice.value = '作品状态已更新。'
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busyId.value = ''
  }
}

/**
 * 移除当前操作。
 * 主要协作：confirmDialog.value.ask、deleteWork、load、errorMessage。
 * 更新的响应式状态包括 busyId、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param work 作品；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function remove(work) {
  if (!await confirmDialog.value.ask({
    title: '删除作品', message: `删除「${work.title}」后无法恢复。`,
    confirmText: '删除', danger: true, requireName: work.title,
  })) return
  busyId.value = work.id
  error.value = ''
  try { await deleteWork(work.id); notice.value = '作品已删除。'; await load() }
  catch (cause) { error.value = errorMessage(cause) }
  finally { busyId.value = '' }
}

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.fullPath, () => {
  readQuery()
  load()
}, { immediate: true })
usePaginationScroll(() => `${page.value}:${pageSize.value}`, () => loading.value)
</script>

<!--
  作品后台：视觉按 V1 `views/admin/PortfolioListView.vue` 对齐。
  接口、筛选写回 URL、分页、发布状态流转、删除确认全部保持 V2 原样，只重排呈现层：
  筛选栏（筛选改为次要按钮、计数右对齐）/ 卡片（编号等宽小字 + 类型·状态·阶段三枚胶囊 /
  摘要两行截断 / 角色 + 技术栈胶囊 / 底部分隔线上的更新时间与圆角操作按钮）/
  封面位改成 V1 的主色到强调色渐变 + 衬线首字 / 卡片悬停上浮 / V1 那套虚线空态。
  数据全部取自列表接口已有的字段（typeDetail.role、typeDetail.techStack、typeDetail.projectStage）。
-->
<template>
  <section class="portfolio-admin">
    <header class="portfolio-admin__hero">
      <div>
        <p>CASE STUDY LIBRARY · 项目案例</p>
        <h1>作品管理</h1>
        <span>通过模板与内容区块管理作品、分类和发布状态。</span>
      </div>
      <RouterLink :to="accountPath('/portfolio/editor/new')">＋ 新建作品</RouterLink>
    </header>

    <form class="portfolio-admin__filters" @submit.prevent="search">
      <input v-model="filters.q" type="search" placeholder="搜索作品标题" aria-label="搜索作品标题" />
      <select v-model="filters.categoryId" aria-label="作品分类"><option value="">全部分类</option><option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option></select>
      <select v-model="filters.type" aria-label="作品类型">
        <option value="">全部类型</option>
        <option v-for="(label, value) in typeLabels" :key="value" :value="value">{{ label }}</option>
      </select>
      <select v-model="filters.status" aria-label="发布状态">
        <option value="">全部状态</option>
        <option v-for="(label, value) in statusLabels" :key="value" :value="value">{{ label }}</option>
      </select>
      <button type="submit">筛选</button>
      <span>共 {{ result.total }} 件</span>
    </form>

    <p v-if="error" class="portfolio-admin__error" role="alert">{{ error }}</p>
    <p v-if="notice" class="portfolio-admin__notice" role="status">{{ notice }}</p>

    <p v-if="loading" class="portfolio-admin__empty">正在加载…</p>
    <div v-else-if="!result.items.length" class="portfolio-admin__empty">
      <template v-if="filters.categoryId || filters.type || filters.status || filters.q">
        <strong>没有符合条件的作品</strong>
        <span>换一组筛选条件，或清除筛选查看全部作品。</span>
        <button type="button" @click="resetQuery">清除筛选</button>
      </template>
      <template v-else>
        <strong>暂无作品</strong>
        <span>创建项目案例，展示完整的工程过程。</span>
      </template>
    </div>

    <div v-else class="portfolio-admin__list">
      <article v-for="work in result.items" :key="work.id" class="portfolio-card" tabindex="0" :aria-label="`编辑作品：${work.title}`" @click="router.push(accountPath(`/portfolio/editor/${work.id}`))" @keydown.enter.self="router.push(accountPath(`/portfolio/editor/${work.id}`))">
        <div class="portfolio-card__visual">
          <img v-if="work.coverUrl" :src="work.coverUrl" :alt="work.title" loading="lazy" />
          <span v-else aria-hidden="true">{{ work.title.slice(0, 1) }}</span>
        </div>
        <div class="portfolio-card__body">
          <div class="portfolio-card__top">
            <div>
              <p>编号 {{ work.slug }}</p>
              <h2>{{ work.title }}</h2>
            </div>
            <div class="portfolio-card__pills">
              <span>{{ typeLabels[work.workType] || work.workType }}</span>
              <span>{{ statusLabels[work.status] || work.status }}</span>
              <span v-if="stageLabels[work.projectStatus || work.typeDetail?.projectStage]">{{ stageLabels[work.projectStatus || work.typeDetail?.projectStage] }}</span>
            </div>
          </div>

          <p class="portfolio-card__summary">{{ work.summary || '摘要待填写' }}</p>
          <p v-if="(work.role || work.typeDetail?.role)" class="portfolio-card__role">角色 · {{ work.role || work.typeDetail?.role }}</p>

          <div class="portfolio-card__stack">
            <span v-for="tech in techStackOf(work)" :key="tech">{{ tech }}</span>
            <small v-if="!techStackOf(work).length">技术栈待补充</small>
          </div>

          <footer @click.stop>
            <time :datetime="work.updatedAt || undefined">更新 {{ updatedLabel(work) }}</time>
            <div>
              <RouterLink :to="accountPath(`/portfolio/editor/${work.id}`)">编辑</RouterLink>
              <RouterLink v-if="work.status === 'PUBLISHED'" :to="`/portfolio/${work.slug}`" target="_blank">查看前台 ↗</RouterLink>
              <button type="button" :disabled="busyId === work.id" @click="changeStatus(work)">{{ work.status === 'PUBLISHED' ? '撤回' : work.status === 'WITHDRAWN' ? '恢复' : '发布' }}</button>
              <button v-if="work.status !== 'PUBLISHED'" type="button" class="danger" :disabled="busyId === work.id" @click="remove(work)">删除</button>
            </div>
          </footer>
        </div>
      </article>
    </div>

    <nav v-if="pageCount > 1" class="portfolio-admin__pages">
      <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span>{{ page }} / {{ pageCount }}</span>
      <button type="button" :disabled="page >= pageCount" @click="changePage(page + 1)">下一页</button>
    </nav>

    <AppConfirmDialog ref="confirmDialog" />
  </section>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.portfolio-admin。
 */
.portfolio-admin { max-width: 1160px; margin: auto; }

/**
 * 样式范围：.portfolio-admin__hero。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-6);
}

/**
 * 样式范围：.portfolio-admin__hero p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__hero p {
  margin-bottom: 8px;
  color: var(--accent);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

/**
 * 样式范围：.portfolio-admin__hero h1。
 */
.portfolio-admin__hero h1 { margin-bottom: 5px; font-size: 34px; letter-spacing: -0.03em; }

/**
 * 样式范围：.portfolio-admin__hero span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__hero span { color: var(--text-muted); font-size: 13px; }

/**
 * 样式范围：.portfolio-admin__hero > a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__hero > a {
  padding: 11px 18px;
  border-radius: 10px;
  color: var(--on-primary);
  background: var(--primary);
  font-weight: 650;
  text-decoration: none;
  white-space: nowrap;
}

/**
 * 样式范围：.portfolio-admin__hero > a:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__hero > a:hover { background: var(--primary-hover); }

/**
 * 样式范围：.portfolio-admin__filters。
 * 使用网格布局组织内容；列定义为 minmax(220px, 1fr) 150px 150px auto auto，间距由 var(--space-3) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__filters {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 150px 150px auto auto;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-surface);
}

/**
 * 样式范围：.portfolio-admin__filters input, .portfolio-admin__filters select。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__filters input,
.portfolio-admin__filters select {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  color: var(--text-primary);
  background: var(--bg-page);
}

/* 筛选是次要动作，主按钮留给「新建作品」——V1 这里也是默认按钮 */
.portfolio-admin__filters button {
  padding: 11px 18px;
  border: 1px solid var(--border-strong);
  border-radius: 10px;
  color: var(--text-primary);
  background: var(--bg-surface);
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

/**
 * 样式范围：.portfolio-admin__filters button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__filters button:hover { border-color: var(--primary); color: var(--primary); }

/**
 * 样式范围：.portfolio-admin__filters > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__filters > span { justify-self: end; color: var(--text-muted); font-size: 12px; }

/**
 * 样式范围：.portfolio-admin__list。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 var(--space-4) 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__list { min-height: 180px; display: grid; gap: var(--space-4); }

/**
 * 样式范围：.portfolio-card。
 * 使用网格布局组织内容；列定义为 230px minmax(0, 1fr)，间距由 各行列间距规则 控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--bg-surface);
  transition: transform 170ms ease, border-color 170ms ease, box-shadow 170ms ease;
}

/**
 * 样式范围：.portfolio-card:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card:hover {
  border-color: color-mix(in srgb, var(--primary) 38%, var(--border));
  transform: translateY(-2px);
  box-shadow: 0 15px 34px rgb(0 0 0 / 0.065);
}

/**
 * 样式范围：.portfolio-card__visual。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card__visual {
  position: relative;
  display: grid;
  min-height: 205px;
  overflow: hidden;
  place-items: center;
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--primary) 18%, var(--bg-subtle)),
    color-mix(in srgb, var(--accent) 13%, var(--bg-surface))
  );
}

/**
 * 样式范围：.portfolio-card__visual img。
 */
.portfolio-card__visual img { width: 100%; height: 100%; object-fit: cover; }

/**
 * 样式范围：.portfolio-card__visual > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card__visual > span {
  color: color-mix(in srgb, var(--primary) 58%, var(--text-muted));
  font: 700 62px/1 Georgia, 'Songti SC', serif;
}

/**
 * 样式范围：.portfolio-card__body。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card__body { min-width: 0; padding: var(--space-5); }

/**
 * 样式范围：.portfolio-card__top。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

/**
 * 样式范围：.portfolio-card__top p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card__top p {
  margin-bottom: 4px;
  color: var(--accent);
  font: 600 10px/1.4 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
}

/**
 * 样式范围：.portfolio-card__top h2。
 */
.portfolio-card__top h2 { font-size: 21px; line-height: 1.4; }

/**
 * 样式范围：.portfolio-card__pills。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.portfolio-card__pills { display: flex; flex-wrap: wrap; gap: 5px; }

/**
 * 样式范围：.portfolio-card__pills span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card__pills span {
  padding: 5px 9px;
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-subtle);
  font-size: 10px;
  white-space: nowrap;
}

/**
 * 样式范围：.portfolio-card__summary。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card__summary {
  display: -webkit-box;
  overflow: hidden;
  margin: 10px 0 5px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

/**
 * 样式范围：.portfolio-card__role。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card__role { color: var(--text-muted); font-size: 11px; }

/**
 * 样式范围：.portfolio-card__stack。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.portfolio-card__stack {
  display: flex;
  min-height: 26px;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

/**
 * 样式范围：.portfolio-card__stack span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card__stack span {
  padding: 3px 9px;
  border-radius: 999px;
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 9%, transparent);
  font-size: 11px;
}

/**
 * 样式范围：.portfolio-card__stack small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card__stack small { color: var(--text-muted); }

/**
 * 样式范围：.portfolio-card footer。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

/**
 * 样式范围：.portfolio-card time。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card time { color: var(--text-muted); font-size: 11px; }

/**
 * 样式范围：.portfolio-card footer > div。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.portfolio-card footer > div { display: flex; flex-wrap: wrap; gap: 5px; }

/**
 * 样式范围：.portfolio-card footer a, .portfolio-card footer button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card footer a,
.portfolio-card footer button {
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-secondary);
  background: var(--bg-page);
  font-size: 12px;
  text-decoration: none;
  cursor: pointer;
}

/**
 * 样式范围：.portfolio-card footer a:hover, .portfolio-card footer button:hover:not(:disabled)。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card footer a:hover,
.portfolio-card footer button:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }

/**
 * 样式范围：.portfolio-card footer .danger:hover:not(:disabled)。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-card footer .danger:hover:not(:disabled) { border-color: var(--danger); color: var(--danger); }

/**
 * 样式范围：.portfolio-admin__empty。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__empty {
  display: grid;
  min-height: 220px;
  place-content: center;
  gap: 6px;
  padding: 60px 24px;
  border: 1px dashed var(--border);
  border-radius: 16px;
  color: var(--text-muted);
  text-align: center;
}

/**
 * 样式范围：.portfolio-admin__empty strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__empty strong { color: var(--text-primary); font-size: 15px; }

/**
 * 样式范围：.portfolio-admin__empty span。
 */
.portfolio-admin__empty span { font-size: 13px; }

/**
 * 样式范围：.portfolio-admin__empty button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__empty button {
  justify-self: center;
  margin-top: 6px;
  padding: 7px 14px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  color: var(--text-primary);
  background: var(--bg-surface);
  font-size: 12px;
  cursor: pointer;
}

/**
 * 样式范围：.portfolio-admin__empty button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__empty button:hover { border-color: var(--primary); color: var(--primary); }

/**
 * 样式范围：.portfolio-admin__error。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__error { color: var(--danger); }
/**
 * 样式范围：.portfolio-admin__notice。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__notice { color: var(--primary); }

/**
 * 样式范围：.portfolio-admin__pages。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__pages {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: var(--space-6);
}

/**
 * 样式范围：.portfolio-admin__pages button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__pages button {
  padding: 7px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--primary);
  background: var(--bg-page);
  cursor: pointer;
}

/**
 * 样式范围：.portfolio-admin__pages span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.portfolio-admin__pages span { color: var(--text-muted); font-size: 12px; }

/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.portfolio-card。
   */
  .portfolio-card { transition: none; }
  /**
   * 样式范围：.portfolio-card:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .portfolio-card:hover { transform: none; }
}

/**
 * 响应式或设备条件：(max-width: 1000px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 1000px) {
  /**
   * 样式范围：.portfolio-admin__filters。
   */
  .portfolio-admin__filters { grid-template-columns: 1fr 1fr; }
  /**
   * 样式范围：.portfolio-card。
   */
  .portfolio-card { grid-template-columns: 160px minmax(0, 1fr); }
  /**
   * 样式范围：.portfolio-card footer。
   */
  .portfolio-card footer { align-items: flex-start; flex-direction: column; }
}

/**
 * 响应式或设备条件：(max-width: 750px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 750px) {
  /**
   * 样式范围：.portfolio-admin__filters。
   */
  .portfolio-admin__filters { grid-template-columns: 1fr 1fr; }
  /**
   * 样式范围：.portfolio-admin__hero。
   */
  .portfolio-admin__hero { align-items: flex-start; flex-direction: column; }
  /**
   * 样式范围：.portfolio-card。
   */
  .portfolio-card { grid-template-columns: 1fr; }
  /**
   * 样式范围：.portfolio-card__visual。
   */
  .portfolio-card__visual { min-height: 150px; aspect-ratio: 16 / 7; }
  /**
   * 样式范围：.portfolio-card__top。
   */
  .portfolio-card__top { flex-direction: column; }
}

/**
 * 响应式或设备条件：(max-width: 480px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 480px) {
  /**
   * 样式范围：.portfolio-admin__filters。
   */
  .portfolio-admin__filters { grid-template-columns: 1fr; }
}
</style>
