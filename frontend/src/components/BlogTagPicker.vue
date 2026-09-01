<script setup lang="ts">
import { computed } from 'vue'
import type { AdminBlogTag } from '@/api/blog'

const props = defineProps<{
  modelValue: Array<number | string>
  tags: AdminBlogTag[]
  disabled?: boolean
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: Array<number | string>): void
}>()

function normalizeName(value: string) {
  return value.normalize('NFKC').trim().replace(/\s+/g, ' ')
}

function normalizedKey(value: string) {
  return normalizeName(value).toLocaleLowerCase()
}

function update(values: Array<number | string>) {
  const result: Array<number | string> = []
  const seen = new Set<string>()
  for (const raw of values) {
    const value = typeof raw === 'string' ? normalizeName(raw) : raw
    if (typeof value === 'string' && !value) continue
    const existing = typeof value === 'string'
      ? props.tags.find((tag) => normalizedKey(tag.name) === normalizedKey(value))
      : undefined
    const resolved = existing?.id ?? value
    const key = typeof resolved === 'number' ? `id:${resolved}` : `name:${normalizedKey(resolved)}`
    if (!seen.has(key)) {
      seen.add(key)
      result.push(resolved)
    }
  }
  emit('update:modelValue', result)
}

const pendingNames = computed(() => props.modelValue.filter((value): value is string => typeof value === 'string'))
</script>

<template>
  <div class="tag-picker">
    <el-select
      :model-value="modelValue"
      :disabled="disabled"
      multiple
      filterable
      allow-create
      default-first-option
      clearable
      collapse-tags
      collapse-tags-tooltip
      placeholder="输入或选择标签，回车添加"
      style="width: 100%"
      @update:model-value="update($event as Array<number | string>)"
    >
      <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id">
        <span class="tag-picker__option-name">{{ tag.name }}</span>
        <small>{{ tag.postCount }} 篇</small>
      </el-option>
    </el-select>
    <p class="tag-picker__hint">
      <template v-if="pendingNames.length">
        保存文章时将新建：<strong>{{ pendingNames.join('、') }}</strong>
      </template>
      <template v-else>可直接输入新标签；不存在的标签会在保存文章时自动创建。</template>
    </p>
  </div>
</template>

<style scoped>
.tag-picker { width: 100%; }
.tag-picker__option-name { float: left; }
.tag-picker small { float: right; color: var(--text-muted); }
.tag-picker__hint { margin-top: 7px; color: var(--text-muted); font-size: 12px; line-height: 1.5; }
.tag-picker__hint strong { color: var(--accent); font-weight: 650; }
</style>
