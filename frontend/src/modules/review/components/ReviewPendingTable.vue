<script setup>
import { computed } from 'vue'
import ReviewStatusTag from './ReviewStatusTag.vue'
import { dateLabel, targetLabel } from '../support/display'

/*
 * 待审核列表。
 *
 * 一个受控组件：筛选、分页、数据都由父页面持有，这里只负责渲染与把「点击行」
 * 交给父级跳详情。这样「待办」与「历史」两个列表可以共用同一套表格骨架。
 *
 * 列表所有字段都来自提交时冻结的快照，因此渲染不需要任何跨模块查询。
 */
const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  emptyText: { type: String, default: '没有符合条件的审核请求。' },
  // 历史列表需要多显示状态与决策人，待办列表则不必
  showStatus: { type: Boolean, default: false },
  showReviewer: { type: Boolean, default: false },
  // 该行是否显示「取消」按钮。是否为申请人本人由父级用 isMine 判定后传进来，
  // 表格本身不接触认证状态，方便「历史」标签页复用同一组件。
  canCancel: { type: Function, default: () => false },
})

const emit = defineEmits(['open', 'cancel'])

const columnCount = computed(() => 5 + (props.showStatus ? 1 : 0) + (props.showReviewer ? 1 : 0))
</script>

<template>
  <div class="table-scroll">
    <table>
      <thead>
        <tr>
          <th>审核对象</th>
          <th>审核类型</th>
          <th>申请人</th>
          <th v-if="showStatus">状态</th>
          <th v-if="showReviewer">决策人</th>
          <th>提交时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.reviewId">
          <td>
            <strong>{{ targetLabel(item) }}</strong>
            <small>#{{ item.reviewId }} · {{ item.targetModule }}/{{ item.targetType }} #{{ item.targetId }}</small>
          </td>
          <td>{{ item.reviewType }}</td>
          <td>
            {{ item.applicantDisplayName || '—' }}
            <small>#{{ item.applicantAccountId }}</small>
          </td>
          <td v-if="showStatus"><ReviewStatusTag :status="item.status" /></td>
          <td v-if="showReviewer">
            {{ item.reviewerAccountId ? `审核员 #${item.reviewerAccountId}` : '—' }}
          </td>
          <td>{{ dateLabel(item.submittedAt) }}</td>
          <td class="table-actions">
            <button class="link-button" type="button" @click="emit('open', item)">详情</button>
            <button
              v-if="canCancel(item)"
              class="link-button review-cancel-button"
              type="button"
              @click="emit('cancel', item)"
            >取消申请</button>
          </td>
        </tr>
        <tr v-if="!loading && !items.length">
          <td :colspan="columnCount" class="empty-state">{{ emptyText }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
