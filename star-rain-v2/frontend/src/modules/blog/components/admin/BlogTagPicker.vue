<script setup>
import { computed, ref } from 'vue'

/*
 * 文章标签选择（对齐 V1 components/BlogTagPicker.vue 的交互）。
 *
 * 值是一个混装数组，和 V1 一致：
 * - number：已有标签的 id；
 * - string：本次新输入的标签名，保存文章时先建标签再绑定。
 *
 * 之所以不在这里直接调创建接口：标签要跟着文章一起保存，
 * 用户取消编辑时不应该在标签库里留下一个孤儿标签。
 * 名称归一化（NFKC + 折叠空白 + 大小写无关去重）与 V1 保持一致：
 * 「Spring  Boot」与「spring boot」视为同一个标签，不会重复添加。
 */
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const draftName = ref('')

function normalizeName(value) {
  return String(value ?? '').normalize('NFKC').trim().replace(/\s+/g, ' ')
}

function normalizedKey(value) {
  return normalizeName(value).toLocaleLowerCase()
}

function commit(values) {
  const result = []
  const seen = new Set()
  for (const raw of values) {
    const value = typeof raw === 'string' ? normalizeName(raw) : raw
    if (typeof value === 'string' && !value) continue
    // 输入的名称如果已经存在同名标签，直接绑定已有标签，避免建出重复项
    const existing = typeof value === 'string'
      ? props.tags.find((tag) => normalizedKey(tag.name) === normalizedKey(value))
      : undefined
    const resolved = existing ? existing.id : value
    const key = typeof resolved === 'number' ? `id:${resolved}` : `name:${normalizedKey(resolved)}`
    if (seen.has(key)) continue
    seen.add(key)
    result.push(resolved)
  }
  emit('update:modelValue', result)
}

const selectedIds = computed(() => props.modelValue.filter((value) => typeof value === 'number'))
const pendingNames = computed(() => props.modelValue.filter((value) => typeof value === 'string'))

function toggle(tag) {
  if (props.disabled) return
  commit(selectedIds.value.includes(tag.id)
    ? props.modelValue.filter((value) => value !== tag.id)
    : [...props.modelValue, tag.id])
}

function addDraft() {
  const name = normalizeName(draftName.value)
  if (!name) return
  commit([...props.modelValue, name])
  draftName.value = ''
}

function removeValue(value) {
  commit(props.modelValue.filter((item) => item !== value))
}
</script>

<template>
  <div class="blog-tag-picker">
    <div class="blog-tag-picker__chips">
      <button
        v-for="tag in tags"
        :key="tag.id"
        type="button"
        :class="['blog-tag-picker__chip', selectedIds.includes(tag.id) && 'is-on', tag.status === 'DISABLED' && 'is-disabled']"
        :disabled="disabled"
        :aria-pressed="selectedIds.includes(tag.id)"
        :title="tag.description || tag.name"
        @click="toggle(tag)"
      >
        # {{ tag.name }}
        <small>{{ tag.status === 'DISABLED' ? '已停用' : `${tag.postCount || 0} 篇` }}</small>
      </button>
      <p v-if="!tags.length" class="muted">标签库还是空的，直接在下面输入新标签名即可。</p>
    </div>

    <div class="blog-tag-picker__new">
      <input
        v-model="draftName"
        maxlength="100"
        placeholder="输入新标签名，回车添加"
        :disabled="disabled"
        @keydown.enter.prevent="addDraft"
      />
      <button type="button" :disabled="disabled || !draftName.trim()" @click="addDraft">添加标签</button>
    </div>

    <p class="blog-tag-picker__hint">
      <template v-if="pendingNames.length">
        保存文章时将新建：<strong>{{ pendingNames.join('、') }}</strong>
      </template>
      <template v-else>不存在的标签会在保存文章时自动创建；已停用的标签不能新增绑定。</template>
    </p>

    <p v-if="pendingNames.length" class="blog-tag-picker__hint">
      待创建
      <button
        v-for="name in pendingNames"
        :key="name"
        class="blog-tag-picker__chip is-on"
        type="button"
        :disabled="disabled"
        title="点击移除"
        @click="removeValue(name)"
      >{{ name }} ×</button>
    </p>
  </div>
</template>
