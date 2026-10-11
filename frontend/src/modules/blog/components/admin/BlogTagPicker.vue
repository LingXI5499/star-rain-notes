<script setup>
/**
 * BlogTagPicker.vue：博客模块的视图组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue。
 * 组件输入：modelValue、tags、disabled；默认值与类型见 defineProps。
 * 组件输出事件：update:modelValue；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、draftName、focused、inputError、selectedIds、pendingNames、selectedTags。
 * 阅读入口：normalizeName（规范化名称）；normalizedKey（规范化键的处理函数）；commit（提交当前操作）；toggle（切换当前操作）；addDraft（添加草稿）。
 */


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
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
})

/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update:modelValue'])

/**
 * 草稿名称；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const draftName = ref('')
/**
 * focused；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const focused = ref(false)
/**
 * input错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const inputError = ref('')

/**
 * 规范化名称。
 * 主要协作：.replace、.trim、.normalize。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function normalizeName(value) {
  return String(value ?? '').normalize('NFKC').trim().replace(/\s+/g, ' ')
}

/**
 * 规范化键的处理函数。
 * 主要协作：.toLocaleLowerCase、normalizeName。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function normalizedKey(value) {
  return normalizeName(value).toLocaleLowerCase()
}

/**
 * 提交当前操作。
 * 主要协作：normalizeName、props.tags.find、normalizedKey、seen.has、seen.add。
 * @param values 值列表；默认值、解构字段与可选参数见函数签名。
 */
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

/**
 * 选中标识集合；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const selectedIds = computed(() => props.modelValue.filter((value) => typeof value === 'number'))
/**
 * 待处理Names；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const pendingNames = computed(() => props.modelValue.filter((value) => typeof value === 'string'))
/**
 * 选中标签列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const selectedTags = computed(() => props.tags.filter((tag) => selectedIds.value.includes(tag.id)))
/**
 * suggestions；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const suggestions = computed(() => {
  const term = normalizedKey(draftName.value.split(/[|｜,，、;；\n]/).at(-1))
  return props.tags.filter((tag) => tag.status !== 'DISABLED'
    && (!term || normalizedKey(tag.name).includes(term))
    && !selectedIds.value.includes(tag.id))
    .sort((a, b) => (b.postCount || 0) - (a.postCount || 0))
    .slice(0, 8)
})

/**
 * 切换当前操作。
 * 主要协作：commit、selectedIds.value.includes、props.modelValue.filter。
 * @param tag 标签；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function toggle(tag) {
  if (props.disabled) return
  commit(selectedIds.value.includes(tag.id)
    ? props.modelValue.filter((value) => value !== tag.id)
    : [...props.modelValue, tag.id])
}

/**
 * 添加草稿。
 * 主要协作：.filter、.map、draftName.value.split、names.some、commit。
 * 更新的响应式状态包括 inputError、draftName；渲染由 Vue 的依赖跟踪触发。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function addDraft() {
  const names = draftName.value.split(/[|｜,，、;；\n]/).map(normalizeName).filter(Boolean)
  if (!names.length) return
  if (names.some((name) => name.length > 100)) {
    inputError.value = '每个标签最多 100 个字符。'
    return
  }
  inputError.value = ''
  commit([...props.modelValue, ...names])
  draftName.value = ''
}

/**
 * 添加Suggestion。
 * 主要协作：commit。
 * 更新的响应式状态包括 draftName；渲染由 Vue 的依赖跟踪触发。
 * @param tag 标签；默认值、解构字段与可选参数见函数签名。
 */
function addSuggestion(tag) {
  commit([...props.modelValue, tag.id])
  draftName.value = ''
}

/**
 * 移除值。
 * 主要协作：commit、props.modelValue.filter。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function removeValue(value) {
  commit(props.modelValue.filter((item) => item !== value))
}
</script>

<template>
  <div class="blog-tag-picker">
    <div class="blog-tag-picker__chips">
      <button
        v-for="tag in selectedTags"
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
      <p v-if="!selectedTags.length && !pendingNames.length" class="muted">暂未选择标签，可搜索已有标签或直接输入新名称。</p>
    </div>

    <div class="blog-tag-picker__new">
      <textarea
        v-model="draftName"
        maxlength="500"
        rows="1"
        placeholder="搜索已有标签；用 | 分隔多个新标签"
        :disabled="disabled"
        @focus="focused = true"
        @blur="focused = false"
        @input="inputError = ''"
        @keydown.enter.exact.prevent="addDraft"
      />
      <button type="button" :disabled="disabled || !draftName.trim()" @click="addDraft">添加标签</button>
    </div>
    <p v-if="inputError" class="error" role="alert">{{ inputError }}</p>
    <div v-if="focused && suggestions.length" class="blog-tag-picker__suggestions" aria-label="匹配的已有标签">
      <p>{{ draftName.trim() ? '匹配的已有标签' : '常用标签' }}</p>
      <button v-for="tag in suggestions" :key="tag.id" type="button" :disabled="disabled"
        @mousedown.prevent @click="addSuggestion(tag)"># {{ tag.name }}</button>
    </div>

    <p class="blog-tag-picker__hint">
      <template v-if="pendingNames.length">
        保存文章时将新建：<strong>{{ pendingNames.join('、') }}</strong>
      </template>
      <template v-else>输入时匹配已有标签；多个标签可用 |、逗号、分号或换行分隔，回车添加，Shift+Enter 换行。新标签在保存文章时创建。</template>
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
