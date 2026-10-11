<script setup>
/**
 * ReviewHistoryTimeline.vue：审核模块的视图组件。
 * 职责范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：../support/display。
 * 组件输入：history；默认值与类型见 defineProps。
 */


import { actionLabel, actorLabel, dateLabel } from '../support/display'

/*
 * 审核动作时间线。
 *
 * 后端保证轨迹从 SUBMITTED 开始、只追加不修改，因此这里就是一条真实的历史线：
 * 提交 → 通过 / 拒绝 / 取消。
 *
 * 决策类的执行者只有账户ID（Account 模块未暴露账户摘要 API），
 * 显示为「审核员 #12」而不是留空——留空会让人以为是系统自动决定。
 */
defineProps({
  history: { type: Array, default: () => [] },
})
</script>

<template>
  <ol class="review-timeline">
    <li v-for="action in history" :key="action.actionId" :class="['review-timeline__item', `is-${action.actionType.toLowerCase()}`]">
      <div class="review-timeline__marker" aria-hidden="true"></div>
      <div class="review-timeline__body">
        <p class="review-timeline__head">
          <strong>{{ actionLabel(action.actionType) }}</strong>
          <span class="review-timeline__actor">{{ actorLabel(action) }}</span>
          <time>{{ dateLabel(action.createdAt) }}</time>
        </p>
        <p v-if="action.note" class="review-timeline__note">{{ action.note }}</p>
      </div>
    </li>
    <li v-if="!history.length" class="empty-state">暂无审核动作记录。</li>
  </ol>
</template>
