<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../account/stores/authStore'
import { cancelReview, listPendingReviews, listReviewHistory, submitDemoReview } from '../api/reviewApi'
import { errorMessage } from '../../../shared/http'
import ReviewPendingTable from '../components/ReviewPendingTable.vue'
import { REVIEW_STATUSES, dateLabel, demoSubmissionPayload, targetLabel } from '../support/display'

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
const router = useRouter()
const auth = useAuthStore()

const tab = ref('pending')
const loading = ref(false)
const error = ref('')
const notice = ref('')

const pendingFilters = reactive({ targetModule: '', reviewType: '', keyword: '' })
const pendingPage = ref(1)
const pendingSize = 20
const pending = ref({ items: [], total: 0, page: 1, pageSize: pendingSize })

const historyFilters = reactive({
  targetModule: '', targetType: '', targetId: '', reviewType: '',
  status: '', applicantAccountId: '', reviewerAccountId: '', startTime: '', endTime: '',
})
const historyPage = ref(1)
const historySize = 20
const history = ref({ items: [], total: 0, page: 1, pageSize: historySize })

const demoOpen = ref(false)
const demoBusy = ref(false)
const demoForm = reactive({ targetId: '', displayName: '', note: '' })

const canRead = computed(() => auth.hasPermission('review:read'))
const canHistory = computed(() => auth.hasPermission('review:history-read'))
const pendingPages = computed(() => Math.max(1, Math.ceil(pending.value.total / pendingSize)))
const historyPages = computed(() => Math.max(1, Math.ceil(history.value.total / historySize)))

function blank(value) {
  if (value === undefined || value === null) return undefined
  const trimmed = String(value).trim()
  return trimmed === '' ? undefined : trimmed
}

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

function load() {
  return tab.value === 'pending' ? loadPending() : loadHistory()
}

function switchTab(next) {
  tab.value = next
  notice.value = ''
  error.value = ''
  if (next === 'pending' && canRead.value) loadPending()
  if (next === 'history' && canHistory.value) loadHistory()
}

function searchPending() {
  pendingPage.value = 1
  loadPending()
}

function searchHistory() {
  historyPage.value = 1
  loadHistory()
}

function resetHistory() {
  Object.assign(historyFilters, {
    targetModule: '', targetType: '', targetId: '', reviewType: '',
    status: '', applicantAccountId: '', reviewerAccountId: '', startTime: '', endTime: '',
  })
  searchHistory()
}

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

/*
 * 演示提交（脚手架）：Tutorial 模块还没有实现，
 * 这里让「提交审核 → 待办出现 → 同一目标重复提交被拒」这条链路可以被手工验证。
 * 目标ID 与版本引用都由这里生成，浏览器不参与 targetModule 的选择。
 */
async function submitDemo() {
  if (demoBusy.value) return
  const targetId = Number(demoForm.targetId)
  if (!Number.isInteger(targetId) || targetId <= 0) {
    error.value = '演示目标ID 必须是正整数。'
    return
  }
  if (!demoForm.displayName.trim()) {
    error.value = '请填写演示目标名称。'
    return
  }
  demoBusy.value = true
  error.value = ''
  notice.value = ''
  try {
    const result = await submitDemoReview(
      demoSubmissionPayload(targetId, demoForm.displayName.trim(), demoForm.note.trim()))
    notice.value = `已提交审核请求 #${result.reviewRequestId}（${result.status}）。`
    demoOpen.value = false
    demoForm.targetId = ''
    demoForm.displayName = ''
    demoForm.note = ''
    tab.value = 'pending'
    pendingPage.value = 1
    await loadPending()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    demoBusy.value = false
  }
}

onMounted(() => {
  if (canRead.value) loadPending()
  else if (canHistory.value) switchTab('history')
})
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
      <button v-if="canRead" class="review-demo-toggle" type="button" @click="demoOpen = !demoOpen">
        {{ demoOpen ? '收起演示提交' : '演示提交（脚手架）' }}
      </button>
    </nav>

    <section v-if="demoOpen && canRead" class="surface-card review-demo">
      <div class="section-heading">
        <div>
          <p class="eyebrow">DEMO SUBMISSION</p>
          <h2>演示提交审核</h2>
        </div>
      </div>
      <p class="form-hint">
        Tutorial 模块尚未实现，REV-001 在这里用演示目标验证：提交后出现在待办，
        同一目标重复提交会被后端以 REVIEW_ALREADY_PENDING 拒绝。
        申请人取自当前登录账户，浏览器无法伪造。
      </p>
      <form class="toolbar toolbar--wrap" @submit.prevent="submitDemo">
        <label>演示目标ID<input v-model="demoForm.targetId" inputmode="numeric" placeholder="例如 1001" /></label>
        <label>目标名称<input v-model.trim="demoForm.displayName" maxlength="255" placeholder="例如 《Java 程序设计》" /></label>
        <label>申请说明<input v-model.trim="demoForm.note" maxlength="1000" placeholder="可选" /></label>
        <button class="primary-button" type="submit" :disabled="demoBusy">{{ demoBusy ? '提交中…' : '提交审核' }}</button>
      </form>
    </section>

    <section v-if="tab === 'pending' && canRead" class="surface-card">
      <form class="toolbar toolbar--wrap" @submit.prevent="searchPending">
        <label>目标模块<input v-model.trim="pendingFilters.targetModule" maxlength="50" placeholder="例如 DEMO / TUTORIAL" /></label>
        <label>审核类型<input v-model.trim="pendingFilters.reviewType" maxlength="100" placeholder="例如 demo.publish" /></label>
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
        <label>起始时间<input v-model="historyFilters.startTime" type="datetime-local" /></label>
        <label>结束时间<input v-model="historyFilters.endTime" type="datetime-local" /></label>
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
