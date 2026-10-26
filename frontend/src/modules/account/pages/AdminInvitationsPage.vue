<script setup>
/**
 * AdminInvitationsPage.vue：账号模块的页面组件。
 * 职责范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../shared/composables/paginationScroll、vue、vue-router、../api/accountApi、../api/http、../support/display。
 * 主要响应式状态：data、page、loading、busy、error、notice、latestLink、confirm。
 * 阅读入口：statusLabel（状态标签的处理函数）；load（加载当前操作）；changePage（change分页的处理函数）；act（act的处理函数）。
 */


import { useLatestAccountQuery } from '../support/useLatestAccountQuery'
import { usePaginationScroll } from '../../../shared/composables/paginationScroll'
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import * as api from '../api/accountApi'
import { errorMessage } from '../api/http'
import { dateLabel } from '../support/display'
import ConfirmDialog from '../components/ConfirmDialog.vue'

/**
 * 数据；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const data = ref({ items: [], total: 0, pageSize: 20 })
/**
 * 分页页码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const page = ref(1)
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
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
 * 最新链接；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const latestLink = ref('')
/**
 * 确认；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirm = ref(null)
/**
 * 总数Pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / 20)))
/**
 * 状态标签的处理函数。
 * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const statusLabel = (status) => ({ PENDING: '待接受', ACCEPTED: '已接受', REVOKED: '已撤销', EXPIRED: '已过期' })[status] || status

/**
 * 加载当前操作。
 * 主要协作：api.listInvitations、errorMessage。
 * 更新的响应式状态包括 loading、error、data；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
const latestQuery = useLatestAccountQuery({ data, error, loading })
async function load() {
  return latestQuery(() => api.listInvitations({ page: page.value, pageSize: 20 }))
}

/**
 * change分页的处理函数。
 * 主要协作：load。
 * 更新的响应式状态包括 page；渲染由 Vue 的依赖跟踪触发。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function changePage(value) { page.value = value; await load() }

/**
 * act的处理函数。
 * 主要协作：confirm.value.ask、api.resendInvitation、api.revokeInvitation、load、errorMessage。
 * 更新的响应式状态包括 busy、error、notice、latestLink；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function act(item, action) {
  if (busy.value) return
  const message = action === 'resend'
    ? `向 ${item.targetEmail} 重发邀请通知？重发成功后旧链接失效，新的邀请有效期为 48 小时。`
    : `撤销 ${item.targetEmail} 的邀请？撤销后该邀请不能再被接受。`
  if (!await confirm.value.ask(message)) return
  busy.value = true
  error.value = ''; notice.value = ''; latestLink.value = ''
  try {
    if (action === 'resend') {
      const result = await api.resendInvitation(item.id)
      latestLink.value = result.invitationUrl
      notice.value = '邀请通知已重新提交发送。若未收到邮件，受邀用户仍可在“我的账户 → 待处理邀请”中接受。'
    } else {
      await api.revokeInvitation(item.id)
      notice.value = '邀请已撤销。'
    }
    await load()
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
usePaginationScroll(() => page.value, () => loading.value)
</script>

<template>
  <main class="page-container">
    <div class="page-heading"><p class="eyebrow">ADMIN INVITATIONS</p><h1>管理员邀请</h1><p>查看邀请状态，重发通知或撤销待处理邀请。</p><RouterLink to="/accounts">到“账户管理”选择普通用户并发出邀请 →</RouterLink></div>
    <section class="surface-card">
      <div class="section-heading"><h2>邀请记录</h2><button class="secondary-button" type="button" :disabled="loading || busy" @click="load">刷新</button></div>
      <p class="muted">邮件状态表示是否已提交发送，不能证明收件箱已收到。受邀用户可直接登录“我的账户”处理邀请。</p>
      <p v-if="notice" class="notice" role="status">{{ notice }}</p><p v-if="error" class="error" role="alert">{{ error }}</p>
      <label v-if="latestLink" class="invitation-link">备用邀请链接（点击可全选复制）<input :value="latestLink" readonly @focus="$event.target.select()" /></label>
      <p v-if="loading" class="loading" role="status">正在加载邀请…</p>
      <div class="table-scroll"><table><thead><tr><th>邀请</th><th>受邀邮箱</th><th>状态</th><th>邮件状态</th><th>到期时间</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in data.items" :key="item.id"><td>#{{ item.id }}</td><td>{{ item.targetEmail }}<small>账户 #{{ item.targetAccountId }}</small></td><td>{{ statusLabel(item.status) }}</td><td>{{ item.mailSubmissionStatus === 'SUBMITTED' ? '已提交发送，送达未确认' : '未记录投递结果' }}<small>{{ dateLabel(item.lastSentAt) }}</small></td><td>{{ dateLabel(item.expiresAt) }}</td><td class="table-actions"><template v-if="['PENDING', 'EXPIRED'].includes(item.status)"><button class="link-button" type="button" :disabled="busy" @click="act(item, 'resend')">重发通知</button><button class="link-button" type="button" :disabled="busy" @click="act(item, 'revoke')">撤销邀请</button></template><span v-else>已处理</span></td></tr>
          <tr v-if="!loading && !data.items.length"><td colspan="6" class="empty-state">暂无管理员邀请。</td></tr></tbody></table></div>
      <div class="pagination"><span>共 {{ data.total }} 条</span><div><button type="button" :disabled="page <= 1 || loading || busy" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ totalPages }}</span><button type="button" :disabled="page >= totalPages || loading || busy" @click="changePage(page + 1)">下一页</button></div></div>
    </section>
  </main>
  <ConfirmDialog ref="confirm" />
</template>
