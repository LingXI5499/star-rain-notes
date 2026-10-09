<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { cancelReview, getReview } from '../api/reviewApi'
import { errorMessage } from '../../../shared/http'
import ReviewApproveDialog from '../components/ReviewApproveDialog.vue'
import ReviewHistoryTimeline from '../components/ReviewHistoryTimeline.vue'
import ReviewRejectDialog from '../components/ReviewRejectDialog.vue'
import ReviewStatusTag from '../components/ReviewStatusTag.vue'
import ReviewTargetPreview from '../components/ReviewTargetPreview.vue'
import { dateLabel, targetLabel } from '../support/display'

/*
 * 审核详情（REV-003）。
 *
 * 三段结构：审核请求元数据 → 目标冻结版本预览 → 动作历史 → 决策按钮。
 *
 * 关键约束：可执行动作不在这里重算。后端 ReviewDetailVO 已经按当前主体的权限与身份
 * 算好 canApprove / canReject / canCancel，前端只照着渲染。
 * 否则前后端各写一套授权规则，迟早会漂移出「前端给了按钮后端 403」这种体验。
 */
const route = useRoute()

const reviewId = computed(() => route.params.reviewId)
const detail = ref(null)
const loading = ref(false)
const error = ref('')
const notice = ref('')
const busy = ref(false)
const approveDialog = ref(null)
const rejectDialog = ref(null)

const isPending = computed(() => detail.value?.status === 'PENDING')

async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await getReview(reviewId.value)
  } catch (cause) {
    error.value = errorMessage(cause)
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function afterDecision(message) {
  notice.value = message
  await load()
}

async function cancelPending() {
  if (!window.confirm('确认取消这条待审核请求？取消后需要重新提交。')) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await cancelReview(reviewId.value)
    notice.value = '已取消该待审核请求。'
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="page-container">
    <div class="page-heading">
      <p class="eyebrow">REVIEW DETAIL</p>
      <h1>{{ detail ? targetLabel(detail) : '审核详情' }}</h1>
      <p><RouterLink to="/useradmin/reviews">← 返回审核中心</RouterLink></p>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载审核详情…</p>

    <template v-if="detail">
      <section class="surface-card">
        <div class="section-heading">
          <div><p class="eyebrow">REQUEST</p><h2>审核请求 #{{ detail.reviewId }}</h2></div>
          <ReviewStatusTag :status="detail.status" />
        </div>

        <dl class="detail-list">
          <div><dt>审核类型</dt><dd>{{ detail.reviewType }}</dd></div>
          <div><dt>目标</dt><dd>{{ detail.targetModule }} / {{ detail.targetType }} #{{ detail.targetId }}</dd></div>
          <div><dt>显示名快照</dt><dd>{{ detail.targetDisplayName }}</dd></div>
          <div><dt>版本引用</dt><dd>{{ detail.targetRevisionRef }}</dd></div>
          <div><dt>申请人</dt><dd>{{ detail.applicantDisplayName || '—' }}（#{{ detail.applicantAccountId }}）</dd></div>
          <div><dt>提交时间</dt><dd>{{ dateLabel(detail.submittedAt) }}</dd></div>
          <div v-if="detail.submissionNote"><dt>申请说明</dt><dd>{{ detail.submissionNote }}</dd></div>
          <div v-if="detail.reviewerAccountId">
            <dt>决策人</dt><dd>审核员 #{{ detail.reviewerAccountId }}</dd>
          </div>
          <div v-if="detail.decidedAt"><dt>决策时间</dt><dd>{{ dateLabel(detail.decidedAt) }}</dd></div>
          <div v-if="detail.canceledAt"><dt>取消时间</dt><dd>{{ dateLabel(detail.canceledAt) }}</dd></div>
          <div v-if="detail.decisionReason"><dt>决定原因</dt><dd>{{ detail.decisionReason }}</dd></div>
        </dl>

        <div class="review-actions">
          <button
            v-if="detail.canApprove"
            class="primary-button"
            type="button"
            :disabled="busy"
            @click="approveDialog.open(detail)"
          >审核通过</button>
          <button
            v-if="detail.canReject"
            class="review-danger-button"
            type="button"
            :disabled="busy"
            @click="rejectDialog.open(detail)"
          >审核拒绝</button>
          <button
            v-if="detail.canCancel"
            type="button"
            :disabled="busy"
            @click="cancelPending"
          >取消待审请求</button>
          <span v-if="isPending && !detail.canApprove && !detail.canReject && !detail.canCancel" class="form-hint">
            当前账户可以查看这条请求，但没有审批或取消它的权限。
          </span>
          <span v-if="!isPending" class="form-hint">
            该请求已是终态（{{ detail.status }}），不再提供可执行动作。
          </span>
        </div>
      </section>

      <section class="surface-card detail-panel">
        <div class="section-heading">
          <div><p class="eyebrow">TARGET SNAPSHOT</p><h2>审核内容（提交时冻结的版本）</h2></div>
          <span class="status-chip">{{ detail.targetRevisionRef }}</span>
        </div>
        <ReviewTargetPreview :view="detail.targetView" :revision-ref="detail.targetRevisionRef" />
      </section>

      <section class="surface-card detail-panel">
        <div class="section-heading">
          <div><p class="eyebrow">HISTORY</p><h2>审核轨迹</h2></div>
          <span class="status-chip">{{ detail.history.length }} 条动作</span>
        </div>
        <ReviewHistoryTimeline :history="detail.history" />
      </section>
    </template>

    <ReviewApproveDialog ref="approveDialog" @approved="afterDecision('审核已通过，目标模块将按审核结论推进自身状态。')" />
    <ReviewRejectDialog ref="rejectDialog" @rejected="afterDecision('审核已拒绝，申请人可修改后重新提交。')" />
  </main>
</template>
