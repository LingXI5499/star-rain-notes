<script setup lang="ts">
import { ref } from 'vue'
import type { ExerciseConfig, ExerciseModule } from '@/api/englishExercise'

const props = withDefaults(
  defineProps<{
    moduleType: ExerciseModule
    questionTypes: string[]
    model: ExerciseConfig
  }>(),
  {},
)

const emit = defineEmits<{ (e: 'update:model', value: ExerciseConfig): void; (e: 'change'): void }>()

const answer = ref<string>(typeof props.model.answer === 'string' ? props.model.answer : '')

function commit() {
  const next: ExerciseConfig = { ...props.model, answer: answer.value }
  emit('update:model', next)
  emit('change')
}
</script>

<template>
  <div class="exercise-builder">
    <div class="exercise-builder__field">
      <label class="exercise-builder__label">标准答案（按当前题型填入）</label>
      <el-input v-model="answer" @change="commit" />
    </div>
    <div class="exercise-builder__field">
      <label class="exercise-builder__label">配置 JSON 预览</label>
      <pre class="exercise-builder__preview">{{ JSON.stringify(props.model, null, 2) }}</pre>
    </div>
    <p class="exercise-builder__hint">
      题型结构由后端 EnglishExerciseService 按题型校验；此处仅提供编辑与预览入口。
    </p>
  </div>
</template>

<style scoped>
.exercise-builder { display: flex; flex-direction: column; gap: 14px; }
.exercise-builder__field { display: flex; flex-direction: column; gap: 6px; }
.exercise-builder__label { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.exercise-builder__preview {
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-subtle);
  overflow: auto;
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}
.exercise-builder__hint { font-size: 12px; color: var(--text-muted); margin: 0; }
</style>
