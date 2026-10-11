<script setup>
/**
 * ReviewCenterPage.vue：审核模块的页面组件。
 * 职责范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../shared/composables/paginationScroll、vue、vue-router、../../account/stores/authStore、../api/reviewApi、../../../shared/http。
 * 主要响应式状态：router、auth、tab、loading、error、notice、pendingFilters、pendingPage。
 * 阅读入口：blank（blank的处理函数）；loadPending（加载待处理）；loadHistory（加载历史）；load（加载当前操作）；switchTab（switchTab的处理函数）。
 */


import { usePaginationScroll } from '../../../shared/composables/paginationScroll'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../account/stores/authStore'
import { cancelReview, listPendingReviews, listReviewHistory } from '../api/reviewApi'
import { errorMessage } from '../../../shared/http'
import { openDateTimePicker } from '../../../shared/dateTimePicker'
import ReviewPendingTable from '../components/ReviewPendingTable.vue'
import { REVIEW_STATUSES, dateLabel, targetLabel } from '../support/display'

/*
 * 审核中心（REV-002 待办 + REV-007 历史）。
 *
 * 两个标签页合并在一个页面里，因为它们的筛选维度高度重叠，
 * Reviewer 的典型动作是「看完待办顺手翻一下这个目标之前被审过几次」。
 *
 * 历史标签页比待办多出状态、申请人/决策人与时间范围；
 * 待办永远只查 PENDING（由后端固定），前端不提供状态筛选，
 * 避免出现「待办里筛状态」这种自相矛盾的交互。
 */
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()

/**
 * tab；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const tab = ref('pending')
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
 * notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notice = ref('')

/**
 * 待处理筛选条件；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pendingFilters = reactive({ targetModule: '', reviewType: '', keyword: '' })
/**
 * 待处理分页；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pendingPage = ref(1)
const pendingSize = 20
/**
 * 待处理；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pending = ref({ items: [], total: 0, page: 1, pageSize: pendingSize })

/**
 * 历史筛选条件；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const historyFilters = reactive({
  targetModule: '', targetType: '', targetId: '', reviewType: '',
  status: '', applicantAccountId: '', reviewerAccountId: '', startTime: '', endTime: '',
})
/**
 * 历史分页；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const historyPage = ref(1)
const historySize = 20
/**
 * 历史；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const history = ref({ items: [], total: 0, page: 1, pageSize: historySize })


/**
 * can读取；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const canRead = computed(() => auth.hasPermission('review:read'))
/**
 * can历史；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const canHistory = computed(() => auth.hasPermission('review:history-read'))
/**
 * 待处理Pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pendingPages = computed(() => Math.max(1, Math.ceil(pending.value.total / pendingSize)))
/**
 * 历史Pages；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const historyPages = computed(() => Math.max(1, Math.ceil(history.value.total / historySize)))

/**
 * blank的处理函数。
 * 主要协作：.trim。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function blank(value) {
  if (value === undefined || value === null) return undefined
  const trimmed = String(value).trim()
  return trimmed === '' ? undefined : trimmed
}

/**
 * 加载待处理。
 * 主要协作：listPendingReviews、blank、errorMessage。
 * 更新的响应式状态包括 loading、error、pending；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadPending() {
  loading.value = true
  error.value = ''
  try {
    pending.value = await listPendingReviews({
      page: pendingPage.value,
      pageSize: pendingSize,
      targetModule: blank(pendingFilters.targetModule),
      reviewType: blank(pendingFilters.reviewType),
      keyword: blank(pendingFilters.keyword),
    })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

/**
 * 加载历史。
 * 主要协作：listReviewHistory、blank、errorMessage。
 * 更新的响应式状态包括 loading、error、history；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadHistory() {
  loading.value = true
  error.value = ''
  try {
    history.value = await listReviewHistory({
      page: historyPage.value,
      pageSize: historySize,
      targetModule: blank(historyFilters.targetModule),
      targetType: blank(historyFilters.targetType),
      targetId: blank(historyFilters.targetId),
      reviewType: blank(historyFilters.reviewType),
      status: blank(historyFilters.status),
      applicantAccountId: blank(historyFilters.applicantAccountId),
      reviewerAccountId: blank(historyFilters.reviewerAccountId),
      // 时间范围用 datetime-local，后端按闭区间过滤 submitted_at
      startTime: blank(historyFilters.startTime),
      endTime: blank(historyFilters.endTime),
    })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

/**
 * 加载当前操作。
 * 主要协作：loadPending、loadHistory。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function load() {
  return tab.value === 'pending' ? loadPending() : loadHistory()
}

/**
 * switchTab的处理函数。
 * 主要协作：loadPending、loadHistory。
 * 更新的响应式状态包括 tab、notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param next 下一项；默认值、解构字段与可选参数见函数签名。
 */
function switchTab(next) {
  tab.value = next
  notice.value = ''
  error.value = ''
  if (next === 'pending' && canRead.value) loadPending()
  if (next === 'history' && canHistory.value) loadHistory()
}

/**
 * 搜索待处理的处理函数。
 * 主要协作：loadPending。
 * 更新的响应式状态包括 pendingPage；渲染由 Vue 的依赖跟踪触发。
 */
function searchPending() {
  pendingPage.value = 1
  loadPending()
}

/**
 * 搜索历史的处理函数。
 * 主要协作：loadHistory。
 * 更新的响应式状态包括 historyPage；渲染由 Vue 的依赖跟踪触发。
 */
function searchHistory() {
  historyPage.value = 1
  loadHistory()
}

/**
 * 重置历史。
 * 主要协作：Object.assign、searchHistory。
 */
function resetHistory() {
  Object.assign(historyFilters, {
    targetModule: '', targetType: '', targetId: '', reviewType: '',
    status: '', applicantAccountId: '', reviewerAccountId: '', startTime: '', endTime: '',
  })
  searchHistory()
}

/**
 * 打开详情。
 * 主要协作：router.push。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 */
function openDetail(item) {
  router.push(`/useradmin/reviews/${item.reviewId}`)
}

/*
 * REV-006 取消：只有申请人本人能取消自己提交的待审请求。
 * 列表里没有 applicantAccountId 与当前账户的比对结果，
 * 所以这里只在「确实是当前账户提交的」时候显示取消按钮，
 * 真正的授权仍由后端 cancelByApplicant 判定。
 */
async function cancel(item) {
  if (!window.confirm(`确认取消「${targetLabel(item)}」的待审核请求？`)) return
  error.value = ''
  notice.value = ''
  try {
    await cancelReview(item.reviewId)
    notice.value = '已取消该待审核请求。'
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

/*
 * 是否是当前账户提交的请求。
 *
 * 注意类型：Account 的 CurrentAccountVO.id 是字符串（避免 JS 精度问题），
 * 而 Review 列表里的 applicantAccountId 是后端 Long 序列化出来的数字。
 * 所以这里统一转成字符串比较，否则恒为 false，取消按钮永远不会出现。
 */
function isMine(item) {
  const currentId = auth.currentUser?.id
  if (currentId === undefined || currentId === null) return false
  return item.applicantAccountId != null && String(item.applicantAccountId) === String(currentId)
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  if (canRead.value) loadPending()
  else if (canHistory.value) switchTab('history')
})
usePaginationScroll(() => `${pendingPage.value}:${historyPage.value}`, () => loading.value)
</script>

<template>
  <main class="page-container">
    <div class="page-heading">
      <p class="eyebrow">REVIEW CENTER</p>
      <h1>审核中心</h1>
      <p>处理待审核请求并查询历史结论。审核只记录决定，目标对象的状态由对应业务模块自己推进。</p>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>

    <nav class="review-tabs" aria-label="审核视图切换">
      <button
        v-if="canRead"
        type="button"
        :class="['review-tab', tab === 'pending' && 'is-active']"
        @click="switchTab('pending')"
      >待审核<span v-if="pending.total">{{ pending.total }}</span></button>
      <button
        v-if="canHistory"
        type="button"
        :class="['review-tab', tab === 'history' && 'is-active']"
        @click="switchTab('history')"
      >审核历史</button>
    </nav>

    <section v-if="tab === 'pending' && canRead" class="surface-card">
      <form class="toolbar toolbar--wrap" @submit.prevent="searchPending">
        <label>目标模块<input v-model.trim="pendingFilters.targetModule" maxlength="50" placeholder="例如 TUTORIAL" /></label>
        <label>审核类型<input v-model.trim="pendingFilters.reviewType" maxlength="100" placeholder="例如 tutorial.publish" /></label>
        <label>关键字<input v-model.trim="pendingFilters.keyword" maxlength="100" placeholder="目标名称或申请人" /></label>
        <button class="primary-button" type="submit">查询</button>
      </form>

      <p v-if="loading" class="loading" role="status">正在加载待审核请求…</p>
      <ReviewPendingTable
        :items="pending.items"
        :loading="loading"
        :can-cancel="isMine"
        empty-text="当前没有待审核请求。"
        @open="openDetail"
        @cancel="cancel"
      >
      </ReviewPendingTable>

      <div class="pagination">
        <span>共 {{ pending.total }} 条</span>
        <div>
          <button type="button" :disabled="pendingPage <= 1 || loading" @click="pendingPage -= 1; loadPending()">上一页</button>
          <span>{{ pendingPage }} / {{ pendingPages }}</span>
          <button type="button" :disabled="pendingPage >= pendingPages || loading" @click="pendingPage += 1; loadPending()">下一页</button>
        </div>
      </div>
    </section>

    <section v-if="tab === 'history' && canHistory" class="surface-card">
      <form class="toolbar toolbar--wrap" @submit.prevent="searchHistory">
        <label>目标模块<input v-model.trim="historyFilters.targetModule" maxlength="50" /></label>
        <label>目标类型<input v-model.trim="historyFilters.targetType" maxlength="50" /></label>
        <label>目标ID<input v-model="historyFilters.targetId" inputmode="numeric" /></label>
        <label>审核类型<input v-model.trim="historyFilters.reviewType" maxlength="100" /></label>
        <label>状态
          <select v-model="historyFilters.status">
            <option value="">全部</option>
            <option v-for="status in REVIEW_STATUSES" :key="status.value" :value="status.value">{{ status.label }}</option>
          </select>
        </label>
        <label>申请人ID<input v-model="historyFilters.applicantAccountId" inputmode="numeric" /></label>
        <label>决策人ID<input v-model="historyFilters.reviewerAccountId" inputmode="numeric" /></label>
        <label>起始时间<input v-model="historyFilters.startTime" type="datetime-local" @click="openDateTimePicker" /></label>
        <label>结束时间<input v-model="historyFilters.endTime" type="datetime-local" @click="openDateTimePicker" /></label>
        <button class="primary-button" type="submit">查询</button>
        <button type="button" @click="resetHistory">重置</button>
      </form>

      <p v-if="loading" class="loading" role="status">正在加载审核历史…</p>
      <ReviewPendingTable
        :items="history.items"
        :loading="loading"
        :show-status="true"
        :show-reviewer="true"
        empty-text="没有符合条件的审核历史。"
        @open="openDetail"
      >
      </ReviewPendingTable>

      <div class="pagination">
        <span>共 {{ history.total }} 条</span>
        <div>
          <button type="button" :disabled="historyPage <= 1 || loading" @click="historyPage -= 1; loadHistory()">上一页</button>
          <span>{{ historyPage }} / {{ historyPages }}</span>
          <button type="button" :disabled="historyPage >= historyPages || loading" @click="historyPage += 1; loadHistory()">下一页</button>
        </div>
      </div>
    </section>

    <p v-if="!canRead && !canHistory" class="empty-state">
      当前账户没有审核查看权限（review:read / review:history-read）。
    </p>
  </main>
</template>
