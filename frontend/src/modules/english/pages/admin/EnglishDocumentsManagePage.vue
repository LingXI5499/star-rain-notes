<script setup>
/**
 * EnglishDocumentsManagePage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/composables/paginationScroll、vue、vue-router、../../../../shared/http、../../../../shared/viewMode、../../api/englishApi。
 * 主要响应式状态：router、page、search、currentPage、loading、busy、error、notice。
 * 阅读入口：load（加载当前操作）；changeStatus（change状态的处理函数）；remove（移除当前操作）。
 */


import { usePaginationScroll } from '../../../../shared/composables/paginationScroll'
import { ref, onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import { deleteReading, listReading, setReadingPublished } from '../../api/englishApi'

const heading='阅读管理', introduction='管理真实文章、双语对齐、人工精读与版本历史。', englishLabel='READING LIBRARY'
/**
 * 分页页码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const page = ref({ items: [], total: 0 })
/**
 * 搜索；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const search = ref('')
/**
 * 当前分页；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const currentPage = ref(1)
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * busy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busy = ref(false)
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
 * 加载当前操作。
 * 主要协作：listReading、errorMessage。
 * 更新的响应式状态包括 loading、error、page；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  error.value = ''
  try { page.value = await listReading( { page: currentPage.value, size: 20, search: search.value }, true) }
  catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

/**
 * change状态的处理函数。
 * 主要协作：setReadingPublished、load、errorMessage。
 * 更新的响应式状态包括 busy、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function changeStatus(item) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    await setReadingPublished( item.id, item.publishStatus !== 'PUBLISHED')
    notice.value = item.publishStatus === 'PUBLISHED' ? '内容已撤回。' : '内容已发布。'
    await load()
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

/**
 * 移除当前操作。
 * 主要协作：window.confirm、deleteReading、load、errorMessage。
 * 更新的响应式状态包括 busy、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function remove(item) {
  if (!window.confirm(`确定删除「${item.title}」？此操作无法撤销。`)) return
  busy.value = true
  error.value = ''
  try { await deleteReading( item.id); notice.value = '内容已删除。'; await load() }
  catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
usePaginationScroll(() => currentPage.value, () => loading.value)
</script>

<template>
  <main class="english-docs-admin">
    <nav class="english-docs-admin__crumb"><RouterLink :to="accountPath('/english/manage')">英语工作台</RouterLink> / {{ heading }}</nav>
    <header class="english-docs-admin__hero"><div><p class="public-eyebrow">{{ englishLabel }} · CONTENT MANAGEMENT</p><h1>{{ heading }}</h1><p>{{ introduction }}</p></div><div class="english-docs-admin__hero-side"><div class="english-docs-admin__count"><strong>{{ page.total }}</strong><span>项内容</span></div><RouterLink :to="accountPath('/english/manage/editor/reading/new')" class="english-docs-admin__create">＋ 新建内容</RouterLink></div></header>
    <section class="english-docs-admin__catalog"><div class="english-docs-admin__toolbar"><div><p class="public-eyebrow">CONTENT CATALOG</p><h2>{{ heading }}</h2></div><form class="english-docs-admin__search" @submit.prevent="currentPage = 1; load()"><input v-model="search" type="search" placeholder="搜索标题" aria-label="搜索标题"><button type="submit">查询</button></form></div>
      <p v-if="error" class="english-docs-admin__error" role="alert">{{ error }}</p><p v-if="notice" class="english-docs-admin__notice" role="status">{{ notice }}</p><p v-if="loading" class="english-docs-admin__empty">正在读取内容…</p>
      <div v-else class="english-docs-admin__grid"><article v-for="item in page.items" :key="item.id" class="english-docs-admin__card" tabindex="0" :aria-label="`编辑文章：${item.title}`" @click="router.push(accountPath('/english/manage/editor/reading/' + item.id))" @keydown.enter.self="router.push(accountPath('/english/manage/editor/reading/' + item.id))"><div class="english-docs-admin__card-top"><span class="english-docs-admin__kind">READING</span><span class="english-docs-admin__status" :class="{ published: item.publishStatus === 'PUBLISHED' }">{{ item.publishStatus === 'PUBLISHED' ? '已发布' : item.publishStatus === 'WITHDRAWN' ? '已撤回' : '草稿' }}</span></div><h3>{{ item.title }}</h3><p>{{ item.summary || '暂无摘要' }}</p><div class="english-docs-admin__actions" @click.stop><RouterLink :to="accountPath('/english/manage/editor/reading/' + item.id)">编辑</RouterLink><button type="button" :disabled="busy" @click="changeStatus(item)">{{ item.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button><button type="button" :disabled="busy" class="english-docs-admin__delete" @click="remove(item)">删除</button></div></article><p v-if="!page.items.length" class="english-docs-admin__empty">暂无内容。</p></div>
      <nav v-if="page.total > 20" class="english-docs-admin__pager" aria-label="分页"><button type="button" :disabled="currentPage <= 1" @click="currentPage--; load()">上一页</button><span>{{ currentPage }} / {{ Math.ceil(page.total / 20) }}</span><button type="button" :disabled="currentPage >= Math.ceil(page.total / 20)" @click="currentPage++; load()">下一页</button></nav>
    </section>
  </main>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.english-docs-admin。
 */
.english-docs-admin{max-width:1450px;margin:auto;padding:25px 0 80px}
  /**
   * 样式范围：.english-docs-admin__crumb。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__crumb{margin-bottom:22px;color:var(--text-muted);font-size:13px}
  /**
   * 样式范围：.english-docs-admin__crumb a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__crumb a{color:var(--primary)}
  /**
   * 样式范围：.english-docs-admin__hero。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .english-docs-admin__hero{display:flex;justify-content:space-between;align-items:end;gap:30px;margin-bottom:30px}
  /**
   * 样式范围：.english-docs-admin__hero h1。
   */
  .english-docs-admin__hero h1{margin:8px 0;font-size:clamp(34px,4vw,46px)}
  /**
   * 样式范围：.english-docs-admin__hero p:last-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__hero p:last-child{margin:0;color:var(--text-secondary);line-height:1.7}
  /**
   * 样式范围：.english-docs-admin__hero-side。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .english-docs-admin__hero-side{display:flex;align-items:end;gap:22px;flex-shrink:0}
  /**
   * 样式范围：.english-docs-admin__count。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__count{display:grid;min-width:95px;padding:12px 19px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);text-align:center}
  /**
   * 样式范围：.english-docs-admin__count strong。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__count strong{color:var(--primary);font-size:30px;line-height:1.2}
  /**
   * 样式范围：.english-docs-admin__count span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__count span{color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.english-docs-admin__create。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__create{padding:11px 18px;border-radius:9px;background:var(--primary);color:var(--on-primary);white-space:nowrap}
  /**
   * 样式范围：.english-docs-admin__tabs。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__tabs{display:flex;gap:8px;margin-bottom:28px;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.english-docs-admin__tabs a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__tabs a{padding:12px 17px;border-bottom:2px solid transparent;color:var(--text-secondary)}
  /**
   * 样式范围：.english-docs-admin__tabs a.active。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__tabs a.active{border-bottom-color:var(--primary);color:var(--primary);font-weight:650}
  /**
   * 样式范围：.english-docs-admin__catalog。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__catalog{padding-top:24px;border-top:1px solid var(--border)}
  /**
   * 样式范围：.english-docs-admin__tabs+.english-docs-admin__catalog。
   */
  .english-docs-admin__tabs+.english-docs-admin__catalog{border-top:0;padding-top:0}
  /**
   * 样式范围：.english-docs-admin__toolbar。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .english-docs-admin__toolbar{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:19px}
  /**
   * 样式范围：.english-docs-admin__toolbar h2。
   */
  .english-docs-admin__toolbar h2{margin:5px 0 0;font-size:23px}
  /**
   * 样式范围：.english-docs-admin__search。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .english-docs-admin__search{display:flex;width:min(100%,390px);gap:8px}
  /**
   * 样式范围：.english-docs-admin__search input。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__search input{flex:1;min-width:0;padding:10px 12px;border:1px solid var(--border-strong);border-radius:9px;background:var(--bg-surface);color:var(--text-primary);font:inherit}
  /**
   * 样式范围：.english-docs-admin button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin button{padding:9px 13px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);font:inherit;cursor:pointer}
  /**
   * 样式范围：.english-docs-admin button:disabled。
   */
  .english-docs-admin button:disabled{opacity:.45;cursor:default}
  /**
   * 样式范围：.english-docs-admin__grid。
   * 使用网格布局组织内容；列定义为 repeat(3,minmax(0,1fr))，间距由 14px 控制。
   */
  .english-docs-admin__grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}
  /**
   * 样式范围：.english-docs-admin__card。
   * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__card{display:flex;min-height:265px;flex-direction:column;padding:21px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface);box-shadow:0 2px 8px rgb(0 0 0/.025)}
  /**
   * 样式范围：.english-docs-admin__card-top。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .english-docs-admin__card-top{display:flex;justify-content:space-between;align-items:center;gap:8px}
  /**
   * 样式范围：.english-docs-admin__kind。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__kind{padding:5px 9px;border:1px solid var(--border-strong);border-radius:999px;color:var(--accent);font-size:11px;font-weight:700}
  /**
   * 样式范围：.english-docs-admin__status。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__status{color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.english-docs-admin__status.published。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__status.published{color:var(--primary)}
  /**
   * 样式范围：.english-docs-admin__card h3。
   */
  .english-docs-admin__card h3{margin:17px 0 8px;font-size:19px;line-height:1.35}
  /**
   * 样式范围：.english-docs-admin__card>p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__card>p{margin:0;color:var(--text-secondary);font-size:13px;line-height:1.7}
  /**
   * 样式范围：.english-docs-admin__meta。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__meta{display:flex;gap:10px;margin:13px 0;color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.english-docs-admin__actions。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__actions{display:flex;gap:7px;margin-top:auto;padding-top:15px;border-top:1px solid var(--border)}
  /**
   * 样式范围：.english-docs-admin__actions a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__actions a{padding:9px 13px;border:1px solid var(--border-strong);border-radius:8px;color:var(--primary);font-size:13px}
  /**
   * 样式范围：.english-docs-admin__actions button。
   */
  .english-docs-admin__actions button{font-size:13px}
  /**
   * 样式范围：.english-docs-admin__actions .english-docs-admin__delete。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__actions .english-docs-admin__delete{margin-left:auto;color:var(--accent)}
  /**
   * 样式范围：.english-docs-admin__empty。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__empty{grid-column:1/-1;padding:55px 10px;color:var(--text-muted);text-align:center}
  /**
   * 样式范围：.english-docs-admin__error,.english-docs-admin__notice。
   */
  .english-docs-admin__error,.english-docs-admin__notice{padding:12px;border-radius:9px}
  /**
   * 样式范围：.english-docs-admin__error。
   */
  .english-docs-admin__error{color:#a13b2b;background:#fff0e8}
  /**
   * 样式范围：.english-docs-admin__notice。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-docs-admin__notice{color:var(--primary);background:var(--primary-soft)}
  /**
   * 样式范围：.english-docs-admin__pager。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .english-docs-admin__pager{display:flex;justify-content:center;align-items:center;gap:15px;margin-top:25px}
  /**
   * 响应式或设备条件：(max-width:1050px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:1050px){
  /**
   * 样式范围：.english-docs-admin__grid。
   */
  .english-docs-admin__grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
  /**
   * 响应式或设备条件：(max-width:700px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:700px){
  /**
   * 样式范围：.english-docs-admin__hero,.english-docs-admin__toolbar。
   */
  .english-docs-admin__hero,.english-docs-admin__toolbar{align-items:start;flex-direction:column}
  /**
   * 样式范围：.english-docs-admin__hero-side。
   */
  .english-docs-admin__hero-side{width:100%;justify-content:space-between}
  /**
   * 样式范围：.english-docs-admin__search。
   */
  .english-docs-admin__search{width:100%}
  /**
   * 样式范围：.english-docs-admin__grid。
   */
  .english-docs-admin__grid{grid-template-columns:1fr}}
</style>
