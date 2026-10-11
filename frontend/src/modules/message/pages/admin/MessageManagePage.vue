<script setup>
/**
 * MessageManagePage.vue：留言模块的页面组件。
 * 职责范围：留言提交、审核展示和后台管理。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/composables/paginationScroll、vue、../../../../shared/http、../../api/messageApi。
 * 主要响应式状态：status、page、total、items、loading、actingId、rejectingId、rejectReason。
 * 阅读入口：load（加载当前操作）；selectStatus（查询状态的处理函数）；act（act的处理函数）；toggleHistory（切换历史）；date（日期的处理函数）。
 */


import { usePaginationScroll } from '../../../../shared/composables/paginationScroll'
import { onMounted, ref } from 'vue'
import { errorMessage } from '../../../../shared/http'
import { approveMessage, deleteMessage, hideMessage, listAdminMessages, listMessageActions, rejectMessage, restoreMessage } from '../../api/messageApi'

const statuses = [
  { value: 'PENDING', label: '待审核' }, { value: 'PUBLIC', label: '已公开' },
  { value: 'HIDDEN', label: '已隐藏' }, { value: 'REJECTED', label: '已拒绝' },
  { value: 'DELETED', label: '已删除' },
]
/**
 * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const status = ref('PENDING')
/**
 * 分页页码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const page = ref(1)
/**
 * 符合查询条件的总记录数；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const total = ref(0)
/**
 * 条目列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const items = ref([])
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * acting标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const actingId = ref(null)
/**
 * rejecting标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const rejectingId = ref(null)
/**
 * reject原因；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const rejectReason = ref('')
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * expanded标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const expandedId = ref(null)
/**
 * 历史；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const history = ref([])
const pageSize = 20

/**
 * 加载当前操作。
 * 主要协作：listAdminMessages、errorMessage。
 * 更新的响应式状态包括 loading、error、items、total；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await listAdminMessages({ page: page.value, pageSize, status: status.value })
    items.value = result.items || []
    total.value = result.total || 0
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

/**
 * 查询状态的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 status、page、expandedId；渲染由 Vue 的依赖跟踪触发。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function selectStatus(value) {
  status.value = value
  page.value = 1
  expandedId.value = null
  await load()
}

/**
 * act的处理函数。
 * 主要协作：rejectReason.value.trim、window.confirm、approveMessage、rejectMessage、hideMessage。
 * 更新的响应式状态包括 actingId、error、rejectingId、rejectReason；渲染由 Vue 的依赖跟踪触发。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function act(item, action) {
  if (actingId.value) return
  let reason = ''
  if (action === 'reject') {
    reason = rejectReason.value.trim()
    if (!reason) return
  }
  if (action === 'delete' && !window.confirm('确定软删除这条留言？删除后不能恢复。')) return
  actingId.value = item.id
  error.value = ''
  try {
    if (action === 'approve') await approveMessage(item.id)
    if (action === 'reject') await rejectMessage(item.id, reason)
    if (action === 'hide') await hideMessage(item.id)
    if (action === 'restore') await restoreMessage(item.id)
    if (action === 'delete') await deleteMessage(item.id)
    rejectingId.value = null
    rejectReason.value = ''
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    actingId.value = null
  }
}

/**
 * 切换历史。
 * 主要协作：listMessageActions、errorMessage。
 * 更新的响应式状态包括 expandedId、history、error；渲染由 Vue 的依赖跟踪触发。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function toggleHistory(item) {
  if (expandedId.value === item.id) { expandedId.value = null; return }
  expandedId.value = item.id
  try {
    history.value = await listMessageActions(item.id)
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

/**
 * 日期的处理函数。
 * 主要协作：.toLocaleString。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function date(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '—'
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
usePaginationScroll(() => page.value, () => loading.value)
</script>

<template>
  <main class="message-admin">
    <header class="message-admin__header">
      <p>MESSAGE MODERATION · 协作</p>
      <h1>留言管理</h1>
      <span>审核新留言，管理已公开内容。联系邮箱仅在后台显示。</span>
    </header>

    <nav class="message-admin__tabs" aria-label="留言状态">
      <button v-for="option in statuses" :key="option.value" type="button"
        :class="{ active: status === option.value }" @click="selectStatus(option.value)">{{ option.label }}</button>
    </nav>
    <p v-if="error" class="message-admin__error" role="alert">{{ error }}</p>
    <section class="message-admin__panel">
      <div class="message-admin__count">{{ statuses.find((option) => option.value === status)?.label }} · 共 {{ total }} 条</div>
      <p v-if="loading" class="message-admin__empty">正在加载…</p>
      <p v-else-if="!items.length" class="message-admin__empty">当前没有此状态的留言。</p>
      <ol v-else class="message-admin__items">
        <li v-for="item in items" :key="item.id" class="message-admin__item">
          <div class="message-admin__meta"><strong>{{ item.authorDisplayName }}</strong><time :datetime="item.submittedAt">{{ date(item.submittedAt) }}</time></div>
          <p class="message-admin__content">{{ item.content }}</p>
          <p v-if="item.contactEmail" class="message-admin__private">联系邮箱（不公开）：{{ item.contactEmail }}</p>
          <p v-if="item.rejectReason" class="message-admin__reason">拒绝原因：{{ item.rejectReason }}</p>
          <div class="message-admin__actions">
            <button v-if="status === 'PENDING'" type="button" :disabled="!!actingId" @click="act(item, 'approve')">通过</button>
            <button v-if="status === 'PENDING'" type="button" :disabled="!!actingId" @click="rejectingId = rejectingId === item.id ? null : item.id; rejectReason = ''">拒绝</button>
            <button v-if="status === 'PUBLIC'" type="button" :disabled="!!actingId" @click="act(item, 'hide')">隐藏</button>
            <button v-if="status === 'HIDDEN'" type="button" :disabled="!!actingId" @click="act(item, 'restore')">恢复公开</button>
            <button v-if="status === 'PUBLIC' || status === 'HIDDEN'" type="button" :disabled="!!actingId" @click="act(item, 'delete')">删除</button>
            <button type="button" @click="toggleHistory(item)">{{ expandedId === item.id ? '收起记录' : '操作记录' }}</button>
          </div>
          <form v-if="rejectingId === item.id" class="message-admin__reject" @submit.prevent="act(item, 'reject')">
            <label :for="`reject-${item.id}`">拒绝原因</label>
            <textarea :id="`reject-${item.id}`" v-model.trim="rejectReason" required maxlength="1000" rows="3" placeholder="请说明拒绝原因" />
            <button type="submit" :disabled="!!actingId || !rejectReason.trim()">确认拒绝</button>
          </form>
          <ol v-if="expandedId === item.id" class="message-admin__history">
            <li v-for="(entry, index) in history" :key="index">{{ date(entry.createdAt) }} · {{ entry.actionType }}<span v-if="entry.note"> · {{ entry.note }}</span></li>
          </ol>
        </li>
      </ol>
      <div v-if="total > pageSize" class="message-admin__pager">
        <button type="button" :disabled="page <= 1 || loading" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
        <button type="button" :disabled="page * pageSize >= total || loading" @click="page++; load()">下一页</button>
      </div>
    </section>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.message-admin。
 */
.message-admin { max-width: 1180px; margin: 0 auto; padding: 36px 36px 90px; }
/**
 * 样式范围：.message-admin__header > p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__header > p { color: var(--accent); font-size: 11px; font-weight: 700; letter-spacing: .14em; }
/**
 * 样式范围：.message-admin__header h1。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__header h1 { font-size: 36px; margin: 12px 0 10px; color: var(--text-primary); }
/**
 * 样式范围：.message-admin__header > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__header > span { color: var(--text-secondary); }
/**
 * 样式范围：.message-admin__tabs。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.message-admin__tabs { display: flex; gap: 10px; flex-wrap: wrap; margin: 34px 0 22px; }
/**
 * 样式范围：.message-admin button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin button { border: 1px solid var(--border); border-radius: 9px; padding: 9px 14px; background: var(--bg-surface); color: var(--text-primary); cursor: pointer; }
/**
 * 样式范围：.message-admin button:hover, .message-admin button.active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin button:hover, .message-admin button.active { border-color: var(--primary); color: var(--primary); }
/**
 * 样式范围：.message-admin button:disabled。
 */
.message-admin button:disabled { opacity: .45; cursor: default; }
/**
 * 样式范围：.message-admin__panel。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__panel { border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); overflow: hidden; }
/**
 * 样式范围：.message-admin__count。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__count { padding: 20px 26px; background: color-mix(in srgb, var(--primary) 5%, var(--bg-surface)); font-weight: 700; }
/**
 * 样式范围：.message-admin__items。
 */
.message-admin__items { list-style: none; padding: 0; margin: 0; }
/**
 * 样式范围：.message-admin__item。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__item { padding: 25px 26px; border-top: 1px solid var(--border); }
/**
 * 样式范围：.message-admin__meta。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__meta { display: flex; justify-content: space-between; gap: 20px; color: var(--text-primary); }
/**
 * 样式范围：.message-admin__meta time。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__meta time { font-size: 12px; color: var(--text-muted); }
/**
 * 样式范围：.message-admin__content。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__content { color: var(--text-secondary); white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.75; margin: 12px 0; }
/**
 * 样式范围：.message-admin__private, .message-admin__reason。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__private, .message-admin__reason { font-size: 13px; color: var(--text-muted); margin: 8px 0; }
/**
 * 样式范围：.message-admin__actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.message-admin__actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 17px; }
/**
 * 样式范围：.message-admin__reject。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__reject { margin-top: 16px; display: grid; gap: 8px; max-width: 540px; color: var(--text-secondary); font-size: 13px; }
/**
 * 样式范围：.message-admin__reject textarea。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__reject textarea { border: 1px solid var(--border); border-radius: 8px; background: var(--bg-page); color: var(--text-primary); padding: 10px; font: inherit; resize: vertical; }
/**
 * 样式范围：.message-admin__reject button。
 */
.message-admin__reject button { justify-self: start; }
/**
 * 样式范围：.message-admin__history。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__history { margin: 18px 0 0; padding: 14px 20px 14px 35px; border-radius: 10px; background: var(--bg-page); color: var(--text-secondary); font-size: 12px; line-height: 2; }
/**
 * 样式范围：.message-admin__error。
 */
.message-admin__error { color: #bc4d37; margin-bottom: 18px; }
/**
 * 样式范围：.message-admin__empty。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__empty { text-align: center; color: var(--text-muted); padding: 54px 15px; }
/**
 * 样式范围：.message-admin__pager。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.message-admin__pager { display: flex; justify-content: center; gap: 16px; align-items: center; padding: 20px; border-top: 1px solid var(--border); }
/**
 * 响应式或设备条件：(max-width: 700px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width: 700px) {
  /**
   * 样式范围：.message-admin。
   */
  .message-admin { padding-inline: 18px; }
  /**
   * 样式范围：.message-admin__item。
   */
  .message-admin__item { padding-inline: 18px; } }
</style>
