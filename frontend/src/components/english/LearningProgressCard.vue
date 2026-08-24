<script setup lang="ts">
import { computed } from 'vue'
import type { LearningRecord } from '@/api/englishLearning'

const props = withDefaults(defineProps<{ record: LearningRecord; compact?: boolean }>(), { compact: false })

const percent = computed(() => {
  if (props.record.mastery == null) return 0
  return Math.max(0, Math.min(100, Math.round(props.record.mastery)))
})
</script>

<template>
  <div class="learning-progress-card" :class="{ 'is-compact': props.compact }">
    <div class="learning-progress-card__head">
      <span class="learning-progress-card__status">{{ props.record.status }}</span>
      <span v-if="props.record.score != null" class="learning-progress-card__score">得分 {{ props.record.score }}</span>
    </div>
    <div class="learning-progress-card__bar">
      <span :style="{ width: `${percent}%` }" />
    </div>
    <div class="learning-progress-card__meta">
      <span>掌握度 {{ percent }}%</span>
      <span>尝试 {{ props.record.attemptCount }} 次</span>
    </div>
    <p v-if="props.record.nextReviewAt" class="learning-progress-card__review">下次复习 {{ props.record.nextReviewAt }}</p>
  </div>
</template>

<style scoped>
.learning-progress-card {
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-surface);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.learning-progress-card__head { display: flex; justify-content: space-between; align-items: center; }
.learning-progress-card__status { font-size: 12px; font-weight: 700; color: var(--primary); text-transform: uppercase; letter-spacing: 0.04em; }
.learning-progress-card__score { font-size: 12px; color: var(--text-secondary); }
.learning-progress-card__bar { height: 6px; border-radius: 999px; background: var(--bg-subtle); overflow: hidden; }
.learning-progress-card__bar span { display: block; height: 100%; background: linear-gradient(90deg, var(--primary), var(--accent)); border-radius: 999px; }
.learning-progress-card__meta { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-muted); }
.learning-progress-card__review { font-size: 12px; color: var(--text-secondary); margin: 0; }
.is-compact { padding: 12px; }
</style>
