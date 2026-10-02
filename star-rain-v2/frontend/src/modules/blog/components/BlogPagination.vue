<script setup>
import { computed } from 'vue'

/*
 * 前台分页 —— 用原生实现替代 V1 的 `el-pagination`（V2 没有 Element Plus）。
 *
 * 页码窗口策略：始终给出首页与末页，当前页左右各一页，中间用省略号连接。
 * 页数少（≤7）时全部列出，不做无意义的省略。
 * 上一页 / 下一页在边界处 disabled，而不是隐藏 —— 按钮位置固定，点起来不会跳。
 */
const props = defineProps({
  page: { type: Number, default: 1 },
  totalPages: { type: Number, default: 1 },
  total: { type: Number, default: 0 },
})

const emit = defineEmits(['change'])

const pages = computed(() => {
  const last = Math.max(1, props.totalPages)
  const current = Math.min(Math.max(1, props.page), last)
  if (last <= 7) return Array.from({ length: last }, (unused, index) => index + 1)
  const result = new Set([1, last, current, current - 1, current + 1])
  const sorted = [...result].filter((value) => value >= 1 && value <= last).sort((a, b) => a - b)
  const withGaps = []
  sorted.forEach((value, index) => {
    if (index > 0 && value - sorted[index - 1] > 1) withGaps.push('…')
    withGaps.push(value)
  })
  return withGaps
})

function go(target) {
  if (target === '…' || target === props.page) return
  if (target < 1 || target > props.totalPages) return
  emit('change', target)
}
</script>

<template>
  <nav v-if="total > 0" class="blog-pagination" aria-label="分页">
    <span class="blog-pagination__total">共 {{ total }} 篇 · 第 {{ page }} / {{ Math.max(1, totalPages) }} 页</span>
    <div class="blog-pagination__pages">
      <button type="button" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
      <template v-for="(item, index) in pages" :key="`${item}-${index}`">
        <span v-if="item === '…'" class="blog-pagination__gap">…</span>
        <button
          v-else
          type="button"
          :class="{ 'is-active': item === page }"
          :aria-current="item === page ? 'page' : undefined"
          @click="go(item)"
        >{{ item }}</button>
      </template>
      <button type="button" :disabled="page >= totalPages" @click="go(page + 1)">下一页</button>
    </div>
  </nav>
</template>

<style scoped>
.blog-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin: var(--space-8) 0 0 112px;
}

.blog-pagination__total {
  color: var(--text-muted);
  font-size: 12px;
}

.blog-pagination__pages {
  display: flex;
  align-items: center;
  gap: 6px;
}

.blog-pagination__pages button {
  min-width: 34px;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-secondary);
  background: var(--bg-surface);
  cursor: pointer;
  font-size: 12px;
}

.blog-pagination__pages button:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.blog-pagination__pages button.is-active {
  border-color: var(--primary);
  color: var(--on-primary);
  background: var(--primary);
  font-weight: 650;
}

.blog-pagination__pages button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.blog-pagination__gap {
  color: var(--text-muted);
  font-size: 12px;
}

@media (max-width: 680px) {
  .blog-pagination {
    margin-left: 26px;
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
