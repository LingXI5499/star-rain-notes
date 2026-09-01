<script setup lang="ts">
export interface FilterOption {
  value: string
  label: string
}
export interface FilterGroup {
  key: string
  label: string
  options: FilterOption[]
  multiple?: boolean
}

const props = withDefaults(
  defineProps<{
    groups: FilterGroup[]
    modelValue: Record<string, string[]>
  }>(),
  {},
)

const emit = defineEmits<{ (e: 'update:modelValue', value: Record<string, string[]>): void; (e: 'change'): void }>()

function toggle(group: FilterGroup, value: string) {
  const current = new Set(props.modelValue[group.key] ?? [])
  if (group.multiple) {
    if (current.has(value)) current.delete(value)
    else current.add(value)
  } else {
    current.clear()
    if (!props.modelValue[group.key]?.includes(value)) current.add(value)
  }
  const next = { ...props.modelValue, [group.key]: [...current] }
  emit('update:modelValue', next)
  emit('change')
}

function active(group: FilterGroup, value: string): boolean {
  return (props.modelValue[group.key] ?? []).includes(value)
}
</script>

<template>
  <div class="filter-bar">
    <div v-for="group in props.groups" :key="group.key" class="filter-bar__group">
      <span class="filter-bar__label">{{ group.label }}</span>
      <div class="filter-bar__chips">
        <button
          v-for="option in group.options"
          :key="option.value"
          type="button"
          class="filter-bar__chip"
          :class="{ 'is-active': active(group, option.value) }"
          @click="toggle(group, option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.filter-bar { display: flex; flex-direction: column; gap: var(--space-3); }
.filter-bar__group { display: flex; align-items: center; gap: var(--space-3); }
.filter-bar__label { font-size: 12px; color: var(--text-muted); min-width: 52px; }
.filter-bar__chips { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.filter-bar__chip {
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-surface);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.filter-bar__chip.is-active { background: color-mix(in srgb, var(--primary) 14%, transparent); border-color: var(--primary); color: var(--primary); }
</style>
