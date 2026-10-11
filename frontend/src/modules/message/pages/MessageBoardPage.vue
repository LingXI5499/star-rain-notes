<script setup>
/**
 * MessageBoardPage.vue：留言模块的页面组件。
 * 职责范围：留言提交、审核展示和后台管理。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../shared/ui/PublicPagination.vue、../../../shared/composables/publicListState、vue、../../account/stores/authStore、../../../shared/http、../api/messageApi。
 * 主要响应式状态：route、router、auth、form、items、page、total、loading。
 * 阅读入口：load（加载当前操作）；submit（提交当前操作）；formatDate（格式化日期）；changePage（change分页的处理函数）；changeSize（change大小的处理函数）。
 */


import PublicPagination from '../../../shared/ui/PublicPagination.vue'
import { publicPage, publicPageSize, sizeQuery } from '../../../shared/composables/publicListState'

import { computed, reactive, ref, watch } from 'vue'
import { useAuthStore } from '../../account/stores/authStore'
import { errorMessage } from '../../../shared/http'
import { listPublicMessages, submitMessage } from '../api/messageApi'

import { useRoute, useRouter } from 'vue-router'
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute(), router = useRouter()
/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()
/**
 * form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const form = reactive({ authorDisplayName: '', contactEmail: '', content: '' })
/**
 * 条目列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const items = ref([])
/**
 * 分页页码；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const page = computed(() => publicPage(route.query.page))
/**
 * 符合查询条件的总记录数；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const total = ref(0)
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * submitting；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const submitting = ref(false)
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
 * 单页返回条目数；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pageSize = computed(() => publicPageSize(route.query.pageSize))
let requestVersion = 0

/**
 * 加载当前操作。
 * 主要协作：listPublicMessages、errorMessage。
 * 更新的响应式状态包括 loading、error、items、total；渲染由 Vue 的依赖跟踪触发。
 * 通过版本或 epoch 判断放弃过期结果，避免异步请求覆盖后来的界面状态。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  const version = ++requestVersion
  loading.value = true
  error.value = ''
  try {
    const result = await listPublicMessages({ page: page.value, pageSize: pageSize.value })
    if (version !== requestVersion) return
    items.value = result.items || []
    total.value = result.total || 0
  } catch (cause) {
    if (version === requestVersion) error.value = errorMessage(cause)
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

/**
 * 提交当前操作。
 * 主要协作：submitMessage、errorMessage。
 * 更新的响应式状态包括 submitting、error、notice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function submit() {
  if (submitting.value) return
  submitting.value = true
  error.value = ''
  notice.value = ''
  try {
    await submitMessage({ ...form })
    form.content = ''
    notice.value = '留言已提交，审核通过后会显示在这里。'
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    submitting.value = false
  }
}

/**
 * 格式化日期。
 * 主要协作：.toLocaleDateString。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function formatDate(value) {
  return value ? new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) : ''
}

/**
 * change分页的处理函数。
 * 主要协作：router.push。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function changePage(next) { router.push({ query: { ...route.query, page: next > 1 ? String(next) : undefined } }) }
/**
 * change大小的处理函数。
 * 主要协作：router.push、sizeQuery。
 * @param size 大小；默认值、解构字段与可选参数见函数签名。
 */
function changeSize(size) { router.push({ query: sizeQuery(route.query, size, 12) }) }
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => route.query, load, { immediate: true })
</script>

<template>
  <main class="message-board">
    <header class="message-board__hero">
      <p class="message-board__eyebrow">MESSAGE BOARD · 留言</p>
      <h1>留下你的想法</h1>
      <p>关于文章、作品，或只是想打个招呼，都欢迎写在这里。</p>
    </header>

    <div class="message-board__columns">
      <section class="message-board__form-card" aria-labelledby="message-form-title">
        <p class="message-board__eyebrow">WRITE A MESSAGE</p>
        <h2 id="message-form-title">写一条留言</h2>
        <p class="message-board__hint">提交后先由管理员审核。联系邮箱仅供回复使用，不会公开。</p>
        <form @submit.prevent="submit">
          <label v-if="!auth.currentUser">署名
            <input v-model.trim="form.authorDisplayName" type="text" required maxlength="100" autocomplete="name" placeholder="怎么称呼你？" />
          </label>
          <p v-else class="message-board__identity">将以当前账号显示名署名</p>
          <label>联系邮箱 <span>选填 · 不公开</span>
            <input v-model.trim="form.contactEmail" type="email" maxlength="128" autocomplete="email" placeholder="方便我们私下回复" />
          </label>
          <label>留言内容
            <textarea v-model.trim="form.content" required maxlength="3000" rows="7" placeholder="写下你想说的话……" />
          </label>
          <div class="message-board__form-bottom">
            <span>{{ form.content.length }} / 3000</span>
            <button type="submit" :disabled="submitting">{{ submitting ? '提交中…' : '提交留言' }}</button>
          </div>
          <p v-if="notice" class="message-board__notice" role="status">{{ notice }}</p>
          <p v-if="error" class="message-board__error" role="alert">{{ error }}</p>
        </form>
      </section>

      <section class="message-board__list" aria-labelledby="message-list-title">
        <div class="message-board__list-head">
          <div>
            <p class="message-board__eyebrow">COMMUNITY</p>
            <h2 id="message-list-title">公开留言 <small>{{ total }}</small></h2>
          </div>
        </div>
        <p v-if="loading" class="message-board__empty">正在加载留言…</p>
        <p v-else-if="!items.length" class="message-board__empty">还没有公开留言，欢迎写下第一条。</p>
        <ol v-else class="message-board__items">
          <li v-for="item in items" :key="item.id" class="message-board__item">
            <div class="message-board__item-top"><strong>{{ item.authorDisplayName }}</strong><time :datetime="item.submittedAt">{{ formatDate(item.submittedAt) }}</time></div>
            <p>{{ item.content }}</p>
          </li>
        </ol>
        <PublicPagination :page="page" :page-size="pageSize" :total="total" :loading="loading" label="留言分页" unit="条" @change="changePage" @page-size="changeSize" />
      </section>
    </div>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.message-board。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board { max-width: var(--layout-max-width); margin: 0 auto; padding: clamp(54px, 7vw, 108px) var(--page-padding-x) 110px; }
/**
 * 样式范围：.message-board__hero。
 */
.message-board__hero { max-width: 680px; margin-bottom: 54px; }
/**
 * 样式范围：.message-board__eyebrow。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__eyebrow { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .18em; }
/**
 * 样式范围：.message-board__hero h1。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__hero h1 { font-size: clamp(42px, 6vw, 72px); line-height: 1.12; margin: 15px 0 20px; color: var(--text-primary); }
/**
 * 样式范围：.message-board__hero > p:last-child, .message-board__hint。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__hero > p:last-child, .message-board__hint { color: var(--text-secondary); line-height: 1.8; }
/**
 * 样式范围：.message-board__columns。
 * 使用网格布局组织内容；列定义为 minmax(300px, 420px) minmax(0, 1fr)，间距由 clamp(36px, 6vw, 88px) 控制。
 */
.message-board__columns { display: grid; grid-template-columns: minmax(300px, 420px) minmax(0, 1fr); gap: clamp(36px, 6vw, 88px); align-items: start; }
/**
 * 样式范围：.message-board__form-card。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__form-card { padding: 32px; border: 1px solid var(--border); border-radius: 20px; background: var(--bg-surface); }
/**
 * 样式范围：.message-board h2。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board h2 { color: var(--text-primary); font-size: 25px; margin: 8px 0 10px; }
/**
 * 样式范围：.message-board__hint。
 */
.message-board__hint { font-size: 14px; margin-bottom: 27px; }
/**
 * 样式范围：.message-board label。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board label { display: block; color: var(--text-secondary); font-size: 14px; font-weight: 600; margin: 20px 0; }
/**
 * 样式范围：.message-board label span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board label span { font-size: 12px; color: var(--text-muted); font-weight: 400; }
/**
 * 样式范围：.message-board input, .message-board textarea。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board input, .message-board textarea { display: block; width: 100%; box-sizing: border-box; margin-top: 9px; border: 1px solid var(--border); border-radius: 10px; padding: 13px 14px; background: var(--bg-page); color: var(--text-primary); font: inherit; resize: vertical; }
/**
 * 样式范围：.message-board input:focus, .message-board textarea:focus。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board input:focus, .message-board textarea:focus { outline: 2px solid var(--primary); outline-offset: 2px; }
/**
 * 样式范围：.message-board__identity。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__identity { color: var(--text-muted); font-size: 13px; }
/**
 * 样式范围：.message-board__form-bottom。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__form-bottom { display: flex; align-items: center; justify-content: space-between; color: var(--text-muted); font-size: 12px; }
/**
 * 样式范围：.message-board__form-bottom button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__form-bottom button { border: 0; border-radius: 10px; background: var(--primary); color: white; padding: 12px 22px; font-weight: 700; cursor: pointer; }
/**
 * 样式范围：.message-board__form-bottom button:disabled。
 */
.message-board__form-bottom button:disabled { opacity: .6; cursor: wait; }
/**
 * 样式范围：.message-board__notice。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__notice { color: var(--primary); margin-top: 17px; }
/**
 * 样式范围：.message-board__error。
 */
.message-board__error { color: #bb4c36; margin-top: 17px; }
/**
 * 样式范围：.message-board__list-head。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__list-head { border-bottom: 1px solid var(--border); padding-bottom: 16px; }
/**
 * 样式范围：.message-board__list-head small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__list-head small { color: var(--text-muted); font-size: 15px; font-weight: 400; }
/**
 * 样式范围：.message-board__empty。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__empty { color: var(--text-muted); padding: 52px 0; }
/**
 * 样式范围：.message-board__items。
 */
.message-board__items { list-style: none; padding: 0; }
/**
 * 样式范围：.message-board__item。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__item { padding: 27px 0; border-bottom: 1px solid var(--border); }
/**
 * 样式范围：.message-board__item-top。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.message-board__item-top { display: flex; justify-content: space-between; gap: 14px; margin-bottom: 12px; }
/**
 * 样式范围：.message-board__item-top strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__item-top strong { color: var(--text-primary); }
/**
 * 样式范围：.message-board__item-top time。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__item-top time { color: var(--text-muted); font-size: 12px; white-space: nowrap; }
/**
 * 样式范围：.message-board__item > p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__item > p { color: var(--text-secondary); line-height: 1.9; white-space: pre-wrap; overflow-wrap: anywhere; }
/**
 * 样式范围：.message-board__pager。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.message-board__pager { display: flex; justify-content: center; align-items: center; gap: 22px; margin-top: 28px; }
/**
 * 样式范围：.message-board__pager button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-board__pager button { border: 1px solid var(--border); border-radius: 8px; background: var(--bg-surface); color: var(--text-primary); padding: 8px 16px; cursor: pointer; }
/**
 * 样式范围：.message-board__pager button:disabled。
 */
.message-board__pager button:disabled { opacity: .45; cursor: default; }
/**
 * 响应式或设备条件：(max-width: 820px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width: 820px) {
  /**
   * 样式范围：.message-board__columns。
   */
  .message-board__columns { grid-template-columns: 1fr; }
  /**
   * 样式范围：.message-board__form-card。
   */
  .message-board__form-card { padding: 24px; } }
</style>
