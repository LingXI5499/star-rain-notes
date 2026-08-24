<script setup lang="ts">
import { computed, ref } from 'vue'
import { isChoiceConfig, type Exercise } from '@/api/englishExercise'

const props = defineProps<{ exercise: Exercise }>()
const emit = defineEmits<{ (e: 'submit', answer: unknown): void }>()

const selected = ref<string>('')
const textAnswer = ref('')
const submitted = ref(false)

const isChoice = computed(() => isChoiceConfig(props.exercise.config))

function submit() {
  submitted.value = true
  const answer: unknown = isChoice.value ? selected.value : textAnswer.value
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

    <textarea
      v-else
      v-model="textAnswer"
      class="exercise-runner__textarea"
      rows="4"
      placeholder="输入你的作答…"
    />

    <div class="exercise-runner__actions">
      <el-button type="primary" @click="submit" :disabled="isChoice ? !selected : !textAnswer.trim()">
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
.exercise-runner__actions { margin-top: 12px; }
</style>
