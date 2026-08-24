<script setup lang="ts">
import { ref } from 'vue'
import type { CategoryNode } from '@/api/tutorial'

export type TreeDropPosition = 'before' | 'after' | 'inside'
export interface CategoryTreeDrop {
  target: CategoryNode
  position: TreeDropPosition
}

const props = defineProps<{
  node: CategoryNode
}>()

const emit = defineEmits<{
  (e: 'create-child', node: CategoryNode): void
  (e: 'edit', node: CategoryNode): void
  (e: 'delete', node: CategoryNode): void
  (e: 'drag-start', node: CategoryNode): void
  (e: 'drop', payload: CategoryTreeDrop): void
}>()

const dropPosition = ref<TreeDropPosition | null>(null)

function resolveDropPosition(event: DragEvent): TreeDropPosition {
  const row = event.currentTarget as HTMLElement
  const bounds = row.getBoundingClientRect()
  const relativeY = (event.clientY - bounds.top) / Math.max(bounds.height, 1)
  if (relativeY < 0.28) return 'before'
  if (relativeY > 0.72) return 'after'
  return 'inside'
}

function startDrag(event: DragEvent) {
  event.dataTransfer?.setData('text/plain', String(props.node.id))
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
  emit('drag-start', props.node)
}

function onDragOver(event: DragEvent) {
  dropPosition.value = resolveDropPosition(event)
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
}

function onDrop(event: DragEvent) {
  const position = resolveDropPosition(event)
  dropPosition.value = null
  emit('drop', { target: props.node, position })
}
</script>

<template>
  <li class="category-node">
    <div
      class="category-node__row"
      :class="dropPosition ? `category-node__row--drop-${dropPosition}` : undefined"
      draggable="true"
      @dragstart="startDrag"
      @dragend="dropPosition = null"
      @dragenter.prevent="onDragOver"
      @dragover.prevent="onDragOver"
      @dragleave="dropPosition = null"
      @drop.prevent.stop="onDrop"
    >
      <span class="category-node__grip" aria-hidden="true" title="拖拽排序">⠿</span>
      <span class="category-node__name">{{ node.name }}</span>
      <span class="category-node__slug">{{ node.slug }}</span>
      <span class="category-node__actions">
        <el-button link type="primary" @click="emit('create-child', node)">添加子分类</el-button>
        <el-button link type="primary" @click="emit('edit', node)">编辑</el-button>
        <el-button link type="danger" @click="emit('delete', node)">删除</el-button>
      </span>
    </div>
    <ul v-if="node.children.length" class="category-node__children">
      <CategoryTreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        @create-child="emit('create-child', $event)"
        @edit="emit('edit', $event)"
        @delete="emit('delete', $event)"
        @drag-start="emit('drag-start', $event)"
        @drop="emit('drop', $event)"
      />
    </ul>
  </li>
</template>

<style scoped>
.category-node,
.category-node__children {
  list-style: none;
}

.category-node__children {
  padding-left: var(--space-7);
}

.category-node__row {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-height: 48px;
  padding: var(--space-2) var(--space-3);
  border-bottom: 1px solid var(--border);
  border-radius: var(--radius-sm);
  transition: background-color 120ms ease, box-shadow 120ms ease;
}

.category-node__row[draggable='true'] {
  cursor: grab;
}

.category-node__row:active {
  cursor: grabbing;
}

.category-node__row--drop-before {
  box-shadow: inset 0 3px 0 var(--primary);
  background: color-mix(in srgb, var(--primary) 7%, transparent);
}

.category-node__row--drop-after {
  box-shadow: inset 0 -3px 0 var(--primary);
  background: color-mix(in srgb, var(--primary) 7%, transparent);
}

.category-node__row--drop-inside {
  background: color-mix(in srgb, var(--primary) 12%, var(--bg-elevated));
  outline: 1px dashed var(--primary);
}

.category-node__grip {
  color: var(--text-muted);
  font-size: 18px;
  letter-spacing: -3px;
  user-select: none;
}

.category-node__name {
  font-weight: 600;
}

.category-node__slug {
  color: var(--text-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}

.category-node__actions {
  margin-left: auto;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

@media (max-width: 680px) {
  .category-node__children {
    padding-left: var(--space-4);
  }

  .category-node__row {
    flex-wrap: wrap;
  }

  .category-node__actions {
    width: 100%;
    margin-left: 0;
  }
}
</style>
