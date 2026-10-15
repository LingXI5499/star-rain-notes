<script setup>
/**
 * AccountAuditsPage.vue：账号模块的页面组件。
 * 职责范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../shared/composables/paginationScroll、vue、../api/accountApi、../api/http、../support/display、../../../shared/dateTimePicker。
 * 主要响应式状态：filters、page、data、loading、error、totalPages。
 * 阅读入口：load（加载当前操作）；search（搜索的处理函数）；changePage（change分页的处理函数）。
 */


import { useLatestAccountQuery } from '../support/useLatestAccountQuery'
import { usePaginationScroll } from '../../../shared/composables/paginationScroll'
import { computed, onMounted, reactive, ref } from 'vue'
import { listAudits } from '../api/accountApi'
import { errorMessage } from '../api/http'
import { dateLabel, statusLabel, utcQueryTime, auditActions, actionLabel } from '../support/display'
import { openDateTimePicker } from '../../../shared/dateTimePicker'

/**
 * 筛选条件；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const filters = reactive({ actorAccountId: '', targetAccountId: '', actionCode: '', result: '', startTime: '', endTime: '' })
/**
 * 分页页码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const page = ref(1)
/**
 * 数据；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const data = ref({ items: [], total: 0, page: 1, pageSize: 20 })
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * 总数Pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / data.value.pageSize)))

/**
 * 加载当前操作。
 * 主要协作：utcQueryTime、listAudits、Object.fromEntries、.filter、Object.entries。
 * 更新的响应式状态包括 loading、error、data；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
const latestQuery = useLatestAccountQuery({ data, error, loading })
async function load() {
  return latestQuery(() => {
    const query = { ...filters, startTime: utcQueryTime(filters.startTime), endTime: utcQueryTime(filters.endTime) }
    return listAudits({ ...Object.fromEntries(Object.entries(query).filter(([, value]) => value)), page: page.value, pageSize: 20 })
  })
}
/**
 * 搜索的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 */
function search() { page.value = 1; load() }
/**
 * change分页的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function changePage(next) { page.value = next; load() }
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
usePaginationScroll(() => page.value, () => loading.value)
</script>

<template>
  <main class="page-container">
    <div class="page-heading"><p class="eyebrow">ACCOUNT AUDIT</p><h1>账户审计</h1><p>查看账户操作记录与结果。</p></div>
    <section class="surface-card">
      <form class="toolbar toolbar--wrap" @submit.prevent="search">
        <label>操作人 ID<input v-model.trim="filters.actorAccountId" inputmode="numeric" placeholder="全部" /></label>
        <label>目标账户 ID<input v-model.trim="filters.targetAccountId" inputmode="numeric" placeholder="全部" /></label>
        <label>动作<select v-model="filters.actionCode"><option value="">全部</option><option v-for="(label, code) in auditActions" :key="code" :value="code">{{ label }}</option></select></label>
        <label>结果<select v-model="filters.result"><option value="">全部</option><option value="SUCCESS">成功</option><option value="FAILED">失败</option></select></label>
        <label>开始时间<input v-model="filters.startTime" type="datetime-local" @click="openDateTimePicker" /></label>
        <label>结束时间<input v-model="filters.endTime" type="datetime-local" @click="openDateTimePicker" /></label>
        <button class="primary-button" type="submit">查询记录</button>
      </form>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <p v-if="loading" class="loading" role="status">正在加载审计记录…</p>
      <div class="table-scroll"><table><thead><tr><th>时间</th><th>动作</th><th>操作人</th><th>目标账户</th><th>结果</th><th>IP 地址</th></tr></thead>
        <tbody><tr v-for="item in data.items" :key="item.id"><td>{{ dateLabel(item.createdAt) }}</td><td><strong>{{ actionLabel(item.actionCode) }}</strong><small>{{ item.actionCode }} · #{{ item.id }}</small></td><td>{{ item.actorAccountId || '—' }}</td><td>{{ item.targetAccountId || '—' }}</td><td><span :class="['status-chip', item.result === 'FAILED' && 'status-chip--danger']">{{ statusLabel(item.result) }}</span></td><td>{{ item.ipAddress || '—' }}</td></tr><tr v-if="!loading && !data.items.length"><td colspan="6" class="empty-state">没有符合条件的记录。</td></tr></tbody></table></div>
      <div class="pagination"><span>共 {{ data.total }} 条</span><div><button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ totalPages }}</span><button type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button></div></div>
    </section>
  </main>
</template>
