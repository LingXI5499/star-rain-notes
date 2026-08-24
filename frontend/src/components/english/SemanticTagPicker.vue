<script setup lang="ts">
import { computed, ref } from 'vue'
import type { TaxonomyDimension, TaxonomyTerm } from '@/api/englishMeta'

const props = withDefaults(
  defineProps<{
    terms: TaxonomyTerm[]
    modelValue: number[]
    dimension?: TaxonomyDimension | 'ALL'
    multiple?: boolean
  }>(),
  { dimension: 'ALL', multiple: true },
)

const emit = defineEmits<{ (e: 'update:modelValue', value: number[]): void }>()

const query = ref('')

const filtered = computed<TaxonomyTerm[]>(() => {
  const q = query.value.trim()
  return props.terms
    .filter((t) => props.dimension === 'ALL' || t.dimension === props.dimension)
    .filter((t) => (t.parentId === null || true) && (q ? t.name.includes(q) || t.slug.includes(q) : true))
})

function toggle(termId: number) {
  if (props.multiple) {
    const next = props.modelValue.includes(termId)
      ? props.modelValue.filter((id) => id !== termId)
      : [...props.modelValue, termId]
    emit('update:modelValue', next)
  } else {
    emit('update:modelValue', props.modelValue[0] === termId ? [] : [termId])
  }
}
</script>

<template>
  <div class="tag-picker">
    <div class="tag-picker__tools">
      <input v-model="query" class="tag-picker__search" placeholder="搜索标签…" type="text" />
      <span class="tag-picker__count">已选 {{ modelValue.length }}</span>
    </div>
    <div class="tag-picker__groups">
      <div v-for="term in filtered" :key="term.id" class="tag-picker__group">
        <button
          type="button"
          class="tag-picker__item"
          :class="{ 'is-active': modelValue.includes(term.id) }"
          @click="toggle(term.id)"
        >
          {{ term.name }}
          <span class="tag-picker__dimension">{{ term.dimension }}</span>
        </button>
        <div v-if="term.children?.length" class="tag-picker__children">
          <button
            v-for="child in term.children"
            :key="child.id"
            type="button"
            class="tag-picker__item tag-picker__item--child"
            :class="{ 'is-active': modelValue.includes(child.id) }"
            @click="toggle(child.id)"
          >
            {{ child.name }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tag-picker__tools { display: flex; align-items: center; gap: var(--space-3); margin-bottom: var(--space-3); }
.tag-picker__search {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-surface);
  color: var(--text-primary);
}
.tag-picker__count { font-size: 12px; color: var(--text-muted); }
.tag-picker__groups { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.tag-picker__group { display: flex; flex-direction: column; gap: 4px; }
.tag-picker__children { display: flex; flex-wrap: wrap; gap: 4px; padding-left: 12px; }
.tag-picker__item {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.tag-picker__item--child { font-size: 11px; }
.tag-picker__item.is-active { background: color-mix(in srgb, var(--primary) 14%, transparent); border-color: var(--primary); color: var(--primary); }
.tag-picker__dimension { font-size: 9px; opacity: 0.6; margin-left: 4px; }
</style>
