<script setup lang="ts">
import { ref } from 'vue'
import type { CategoryNode } from '@/api/tutorial'

export type WorkspaceDropPosition = 'before' | 'after' | 'inside'
export interface WorkspaceCategoryDrop {
  target: CategoryNode
  position: WorkspaceDropPosition
}

const props = defineProps<{
  node: CategoryNode
  activeId: number | null
}>()

const emit = defineEmits<{
  (e: 'select', node: CategoryNode): void
  (e: 'create-child', node: CategoryNode): void
  (e: 'edit', node: CategoryNode): void
  (e: 'delete', node: CategoryNode): void
  (e: 'drag-start', node: CategoryNode): void
  (e: 'drop', payload: WorkspaceCategoryDrop): void
}>()

const dropPosition = ref<WorkspaceDropPosition | null>(null)

function resolveDropPosition(event: DragEvent): WorkspaceDropPosition {
  const row = event.currentTarget as HTMLElement
  const bounds = row.getBoundingClientRect()
  const ratio = (event.clientY - bounds.top) / Math.max(bounds.height, 1)
  if (ratio < 0.25) return 'before'
  if (ratio > 0.75) return 'after'
  return 'inside'
}

function startDrag(event: DragEvent) {
  event.dataTransfer?.setData('text/plain', String(props.node.id))
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
  emit('drag-start', props.node)
}

function dragOver(event: DragEvent) {
  dropPosition.value = resolveDropPosition(event)
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
}

function drop(event: DragEvent) {
  const position = resolveDropPosition(event)
  dropPosition.value = null
  emit('drop', { target: props.node, position })
}
</script>

<template>
  <li class="workspace-category">
    <div
      class="workspace-category__row"
      :class="[
        { 'workspace-category__row--active': activeId === node.id },
        dropPosition ? `workspace-category__row--drop-${dropPosition}` : undefined,
      ]"
      draggable="true"
      @click="emit('select', node)"
      @dragstart="startDrag"
      @dragend="dropPosition = null"
      @dragenter.prevent="dragOver"
      @dragover.prevent="dragOver"
      @dragleave="dropPosition = null"
      @drop.prevent.stop="drop"
    >
      <span class="workspace-category__grip" title="拖拽排序" aria-hidden="true">⠿</span>
      <span class="workspace-category__name" :title="node.name">{{ node.name }}</span>
      <span class="workspace-category__actions">
        <button type="button" title="添加子分类" @click.stop="emit('create-child', node)">＋</button>
        <button type="button" title="编辑分类" @click.stop="emit('edit', node)">编</button>
        <button type="button" title="删除分类" @click.stop="emit('delete', node)">删</button>
      </span>
    </div>

    <ul v-if="node.children.length" class="workspace-category__children">
      <TutorialWorkspaceCategoryNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :active-id="activeId"
        @select="emit('select', $event)"
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
.workspace-category,
.workspace-category__children {
  list-style: none;
}

.workspace-category__children {
  padding-left: 14px;
  border-left: 1px solid var(--border);
  margin-left: 14px;
}

.workspace-category__row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 38px;
  padding: 5px 7px;
  border-radius: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 120ms ease, background-color 120ms ease, box-shadow 120ms ease;
}

.workspace-category__row:hover {
  color: var(--text-primary);
  background: var(--bg-subtle);
}

.workspace-category__row--active {
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 11%, transparent);
  font-weight: 600;
}

.workspace-category__row--drop-before { box-shadow: inset 0 3px 0 var(--primary); }
.workspace-category__row--drop-after { box-shadow: inset 0 -3px 0 var(--primary); }
.workspace-category__row--drop-inside { outline: 1px dashed var(--primary); }

.workspace-category__grip {
  color: var(--text-muted);
  font-size: 15px;
  letter-spacing: -3px;
  cursor: grab;
  user-select: none;
}

.workspace-category__name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.workspace-category__actions {
  display: none;
  margin-left: auto;
  gap: 2px;
}

.workspace-category__row:hover .workspace-category__actions,
.workspace-category__row:focus-within .workspace-category__actions {
  display: flex;
}

.workspace-category__actions button {
  width: 24px;
  height: 24px;
  padding: 0;
  border: 0;
  border-radius: 5px;
  color: var(--text-muted);
  background: var(--bg-elevated);
  font-size: 11px;
  cursor: pointer;
}

.workspace-category__actions button:hover {
  color: var(--primary);
}
</style>
