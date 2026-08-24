<script setup lang="ts">
import { computed, ref } from 'vue'
import { isChoiceConfig, type Exercise } from '@/api/englishExercise'

const props = defineProps<{ exercise: Exercise }>()
const emit = defineEmits<{ (e: 'submit', answer: unknown): void }>()

const selected = ref<string>('')
const textAnswer = ref('')
const orderItems = ref<unknown[]>(Array.isArray(props.exercise.config.items) ? [...props.exercise.config.items] : [])
const matchAnswers = ref<Record<string, unknown>>({})
const structureAnswers = ref<Record<string, string>>({})

const isChoice = computed(() => isChoiceConfig(props.exercise.config))
const isTrueFalse = computed(() => props.exercise.questionType === 'TRUE_FALSE')
const isOrder = computed(() => props.exercise.questionType === 'ORDERING')
const isMatch = computed(() => ['SENTENCE_MATCH', 'PARAGRAPH_MATCH'].includes(props.exercise.questionType))
const isStructure = computed(() => props.exercise.questionType === 'STRUCTURE_FILL')
const leftItems = computed(() => Array.isArray(props.exercise.config.leftItems) ? props.exercise.config.leftItems : [])
const rightItems = computed(() => Array.isArray(props.exercise.config.rightItems) ? props.exercise.config.rightItems : [])
const structure = computed(() => Array.isArray(props.exercise.config.structure) ? props.exercise.config.structure as Array<Record<string, unknown>> : [])
const canSubmit = computed(() => {
  if (isChoice.value || isTrueFalse.value) return selected.value !== ''
  if (isOrder.value) return orderItems.value.length > 0
  if (isMatch.value) return leftItems.value.length > 0 && leftItems.value.every((item) => matchAnswers.value[String(item)] !== undefined)
  if (isStructure.value) return structure.value.length > 0 && structure.value.every((item) => structureAnswers.value[String(item.label)]?.trim())
  return !!textAnswer.value.trim()
})

function display(value: unknown): string {
  return typeof value === 'string' ? value : JSON.stringify(value)
}

function move(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= orderItems.value.length) return
  const next = [...orderItems.value]
  ;[next[index], next[target]] = [next[target], next[index]]
  orderItems.value = next
}

function submit() {
  let answer: unknown = textAnswer.value
  if (isChoice.value) answer = selected.value
  else if (isTrueFalse.value) answer = selected.value === 'true'
  else if (isOrder.value) answer = orderItems.value
  else if (isMatch.value) answer = leftItems.value.map((left) => [left, matchAnswers.value[String(left)]])
  else if (isStructure.value) answer = structureAnswers.value
  emit('submit', answer)
}
</script>

<template>
  <div class="exercise-runner">
    <p class="exercise-runner__prompt">{{ props.exercise.promptMarkdown }}</p>

    <div v-if="isChoice" class="exercise-runner__options">
      <button
        v-for="option in props.exercise.config.options"
        :key="option.key"
        type="button"
        class="exercise-runner__option"
        :class="{ 'is-selected': selected === option.key }"
        @click="selected = option.key"
      >
        {{ option.text }}
      </button>
    </div>

    <div v-else-if="isTrueFalse" class="exercise-runner__options">
      <button v-for="option in [{ key: 'true', text: '正确' }, { key: 'false', text: '错误' }]" :key="option.key" type="button" class="exercise-runner__option" :class="{ 'is-selected': selected === option.key }" @click="selected = option.key">{{ option.text }}</button>
    </div>

    <div v-else-if="isOrder" class="exercise-runner__ordering">
      <div v-for="(item, index) in orderItems" :key="index" class="exercise-runner__order-item">
        <span>{{ index + 1 }}. {{ display(item) }}</span>
        <span><button type="button" :disabled="index === 0" @click="move(index, -1)">↑</button><button type="button" :disabled="index === orderItems.length - 1" @click="move(index, 1)">↓</button></span>
      </div>
    </div>

    <div v-else-if="isMatch" class="exercise-runner__matching">
      <label v-for="left in leftItems" :key="String(left)">
        <span>{{ display(left) }}</span>
        <select v-model="matchAnswers[String(left)]">
          <option :value="undefined" disabled>请选择匹配项</option>
          <option v-for="right in rightItems" :key="String(right)" :value="right">{{ display(right) }}</option>
        </select>
      </label>
    </div>

    <div v-else-if="isStructure" class="exercise-runner__structure">
      <label v-for="item in structure" :key="String(item.label)"><span>{{ item.label }}</span><input v-model="structureAnswers[String(item.label)]" type="text" /></label>
    </div>

    <textarea
      v-else
      v-model="textAnswer"
      class="exercise-runner__textarea"
      rows="4"
      placeholder="输入你的作答…"
    />

    <div class="exercise-runner__actions">
      <el-button type="primary" @click="submit" :disabled="!canSubmit">
        提交
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.exercise-runner__prompt { font-size: 15px; line-height: 1.7; margin: 0 0 16px; }
.exercise-runner__options { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
.exercise-runner__option {
  text-align: left;
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-surface);
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.15s ease;
}
.exercise-runner__option.is-selected { border-color: var(--primary); background: color-mix(in srgb, var(--primary) 8%, transparent); color: var(--primary); }
.exercise-runner__textarea { width: 100%; padding: 12px; border: 1px solid var(--border); border-radius: 12px; background: var(--bg-surface); color: var(--text-primary); resize: vertical; }
.exercise-runner__ordering, .exercise-runner__matching, .exercise-runner__structure { display: grid; gap: 10px; }
.exercise-runner__order-item, .exercise-runner__matching label, .exercise-runner__structure label { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 10px; background: var(--bg-surface); }
.exercise-runner__order-item button { margin-left: 6px; border: 0; background: var(--bg-subtle); color: var(--text-primary); border-radius: 6px; cursor: pointer; }
.exercise-runner__matching select, .exercise-runner__structure input { min-width: 180px; padding: 8px 10px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-surface); color: var(--text-primary); }
.exercise-runner__actions { margin-top: 12px; }
</style>
