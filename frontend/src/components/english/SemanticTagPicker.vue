<script setup lang="ts">
import { computed, ref } from 'vue'
import type { TaxonomyDimension, TaxonomyTerm } from '@/api/englishMeta'

const props = withDefaults(
  defineProps<{
    terms: TaxonomyTerm[]
    modelValue: number[]
    dimension?: TaxonomyDimension | 'ALL'
    multiple?: boolean
    placeholder?: string
  }>(),
  { dimension: 'ALL', multiple: true, placeholder: '搜索并选择标签…' },
)

const emit = defineEmits<{ (e: 'update:modelValue', value: number[]): void }>()
const query = ref('')

interface FlatTerm {
  term: TaxonomyTerm
  depth: number
  parentName: string | null
}

function flatten(terms: TaxonomyTerm[], depth = 0, parentName: string | null = null): FlatTerm[] {
  return terms.flatMap((term) => [
    { term, depth, parentName },
    ...flatten(term.children ?? [], depth + 1, term.name),
  ])
}

const allTerms = computed(() => flatten(props.terms)
  .filter(({ term }) => props.dimension === 'ALL' || term.dimension === props.dimension))

const selectedTerms = computed(() => {
  const byId = new Map(allTerms.value.map(({ term }) => [term.id, term]))
  return [...new Set(props.modelValue)].map((id) => byId.get(id)).filter((term): term is TaxonomyTerm => !!term)
})

const filtered = computed(() => {
  const q = query.value.trim().toLocaleLowerCase()
  const selected = new Set(props.modelValue)
  return allTerms.value.filter(({ term }) => {
    if (selected.has(term.id) || !term.enabled) return false
    if (!q) return true
    return term.name.toLocaleLowerCase().includes(q) || term.slug.toLocaleLowerCase().includes(q)
  })
})

function select(termId: number) {
  if (props.multiple) {
    emit('update:modelValue', [...new Set([...props.modelValue, termId])])
  } else {
    emit('update:modelValue', [termId])
  }
  query.value = ''
}

function remove(termId: number) {
  emit('update:modelValue', props.modelValue.filter((id) => id !== termId))
}

function selectFirst() {
  const first = filtered.value[0]
  if (first) select(first.term.id)
}
</script>

<template>
  <div class="tag-picker">
    <div v-if="selectedTerms.length" class="tag-picker__selected" aria-label="已选择标签">
      <button
        v-for="term in selectedTerms"
        :key="term.id"
        type="button"
        class="tag-picker__chip"
        :aria-label="`移除标签 ${term.name}`"
        @click="remove(term.id)"
      >
        <span>{{ term.name }}</span>
        <small>{{ term.dimension }}</small>
        <b aria-hidden="true">×</b>
      </button>
    </div>

    <div class="tag-picker__tools">
      <input
        v-model="query"
        class="tag-picker__search"
        :placeholder="placeholder"
        type="search"
        autocomplete="off"
        @keydown.enter.prevent="selectFirst"
      />
      <span class="tag-picker__count">已选 {{ selectedTerms.length }}</span>
    </div>

    <div v-if="filtered.length" class="tag-picker__results">
      <button
        v-for="entry in filtered"
        :key="entry.term.id"
        type="button"
        class="tag-picker__item"
        :class="{ 'tag-picker__item--child': entry.depth > 0 }"
        :style="{ '--tag-depth': String(Math.min(entry.depth, 3)) }"
        @click="select(entry.term.id)"
      >
        <span>{{ entry.term.name }}</span>
        <small v-if="entry.parentName">{{ entry.parentName }}</small>
        <em>{{ entry.term.dimension }}</em>
      </button>
    </div>
    <p v-else class="tag-picker__empty">{{ query.trim() ? '没有匹配的可选标签' : '所有可用标签均已选择' }}</p>
  </div>
</template>

<style scoped>
.tag-picker { display: flex; flex-direction: column; gap: var(--space-3); }
.tag-picker__selected { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-picker__chip {
  display: inline-flex; min-height: 34px; align-items: center; gap: 7px; padding: 5px 9px 5px 12px;
  border: 1px solid var(--primary); border-radius: 999px;
  background: color-mix(in srgb, var(--primary) 12%, var(--bg-surface)); color: var(--primary); cursor: pointer;
}
.tag-picker__chip small { font-size: 9px; opacity: .65; }
.tag-picker__chip b { display: grid; width: 18px; height: 18px; place-items: center; border-radius: 50%; background: color-mix(in srgb, var(--primary) 14%, transparent); }
.tag-picker__tools { display: flex; align-items: center; gap: var(--space-3); }
.tag-picker__search {
  min-width: 0; flex: 1; padding: 9px 12px; border: 1px solid var(--border); border-radius: 8px;
  background: var(--bg-surface); color: var(--text-primary);
}
.tag-picker__search:focus { border-color: var(--primary); outline: 2px solid color-mix(in srgb, var(--primary) 14%, transparent); }
.tag-picker__count { flex: none; font-size: 12px; color: var(--text-muted); }
.tag-picker__results { display: flex; max-height: 230px; flex-wrap: wrap; gap: var(--space-2); overflow: auto; padding: 2px; }
.tag-picker__item {
  display: inline-flex; min-height: 34px; align-items: center; gap: 7px;
  margin-left: calc(var(--tag-depth, 0) * 10px); padding: 5px 10px;
  border: 1px solid var(--border); border-radius: 999px; background: var(--bg-subtle);
  color: var(--text-secondary); font-size: 12px; cursor: pointer; transition: border-color .15s ease, color .15s ease;
}
.tag-picker__item:hover { border-color: var(--primary); color: var(--primary); }
.tag-picker__item--child::before { content: '↳'; color: var(--text-muted); }
.tag-picker__item small { color: var(--text-muted); font-size: 10px; }
.tag-picker__item em { font-size: 9px; font-style: normal; opacity: .6; }
.tag-picker__empty { margin: 0; color: var(--text-muted); font-size: 12px; }
@media (max-width: 640px) { .tag-picker__tools { align-items: stretch; flex-direction: column; gap: 6px; } }
</style>
