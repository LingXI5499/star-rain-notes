<script setup lang="ts">
import { ref } from 'vue'
import type { AdminTreeNode } from '@/api/tutorial'

export type NodeDropPosition = 'before' | 'after' | 'inside'
export interface NodeTreeDrop {
  target: AdminTreeNode
  position: NodeDropPosition
}

const props = defineProps<{
  node: AdminTreeNode
}>()

const emit = defineEmits<{
  (e: 'rename', node: AdminTreeNode): void
  (e: 'delete', node: AdminTreeNode): void
  (e: 'publish', node: AdminTreeNode): void
  (e: 'move', node: AdminTreeNode, delta: number): void
  (e: 'drag-start', node: AdminTreeNode): void
  (e: 'drop', payload: NodeTreeDrop): void
  (e: 'create-group', node: AdminTreeNode): void
  (e: 'create-chapter', node: AdminTreeNode): void
}>()

const dropPosition = ref<NodeDropPosition | null>(null)

const statusLabels: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }
const statusTypes: Record<string, 'warning' | 'success' | 'info'> = {
  DRAFT: 'warning',
  PUBLISHED: 'success',
  WITHDRAWN: 'info',
}

function statusLabel(status: string | null): string {
  return status ? (statusLabels[status] ?? status) : '—'
}

function statusType(status: string | null): 'warning' | 'success' | 'info' {
  return status ? (statusTypes[status] ?? 'info') : 'info'
}

function resolveDropPosition(event: DragEvent): NodeDropPosition {
  const row = event.currentTarget as HTMLElement
  const bounds = row.getBoundingClientRect()
  const relativeY = (event.clientY - bounds.top) / Math.max(bounds.height, 1)
  if (relativeY < 0.28) return 'before'
  if (relativeY > 0.72) return 'after'
  return props.node.type === 'GROUP' ? 'inside' : 'after'
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
  <li class="chapter-node">
    <div
      class="chapter-node__row"
      :class="dropPosition ? `chapter-node__row--drop-${dropPosition}` : undefined"
      draggable="true"
      @dragstart="startDrag"
      @dragend="dropPosition = null"
      @dragenter.prevent="onDragOver"
      @dragover.prevent="onDragOver"
      @dragleave="dropPosition = null"
      @drop.prevent.stop="onDrop"
    >
      <span class="chapter-node__grip" aria-hidden="true" title="拖拽排序">⠿</span>
      <span class="chapter-node__type">{{ node.type === 'GROUP' ? '分组' : '章节' }}</span>
      <span class="chapter-node__title">{{ node.title }}</span>
      <el-tag v-if="node.type === 'CHAPTER'" size="small" :type="statusType(node.publishStatus)">
        {{ statusLabel(node.publishStatus) }}
      </el-tag>
      <span class="chapter-node__actions">
        <template v-if="node.type === 'GROUP'">
          <el-button link type="primary" @click="emit('create-group', node)">子分组</el-button>
          <el-button link type="primary" @click="emit('create-chapter', node)">加章节</el-button>
          <el-button link type="primary" @click="emit('rename', node)">重命名</el-button>
        </template>
        <template v-else>
          <el-button link type="primary" @click="emit('rename', node)">编辑</el-button>
          <el-button
            link
            :type="node.publishStatus === 'PUBLISHED' ? 'warning' : 'success'"
            @click="emit('publish', node)"
          >
            {{ node.publishStatus === 'PUBLISHED' ? '撤回' : node.publishStatus === 'WITHDRAWN' ? '重新发布' : '发布' }}
          </el-button>
        </template>
        <el-button link @click="emit('move', node, -1)">↑</el-button>
        <el-button link @click="emit('move', node, 1)">↓</el-button>
        <el-button link type="danger" @click="emit('delete', node)">删除</el-button>
      </span>
    </div>
    <ul v-if="node.children.length" class="chapter-node__children">
      <ChapterTreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        @rename="emit('rename', $event)"
        @delete="emit('delete', $event)"
        @publish="emit('publish', $event)"
        @move="(childNode, delta) => emit('move', childNode, delta)"
        @drag-start="emit('drag-start', $event)"
        @drop="emit('drop', $event)"
        @create-group="emit('create-group', $event)"
        @create-chapter="emit('create-chapter', $event)"
      />
    </ul>
  </li>
</template>

<style scoped>
.chapter-node {
  list-style: none;
}

.chapter-node__row {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--border);
  border-radius: var(--radius-sm);
  transition: background-color 120ms ease, box-shadow 120ms ease;
}

.chapter-node__row[draggable='true'] {
  cursor: grab;
}

.chapter-node__row:active {
  cursor: grabbing;
}

.chapter-node__row--drop-before {
  box-shadow: inset 0 3px 0 var(--primary);
  background: color-mix(in srgb, var(--primary) 7%, transparent);
}

.chapter-node__row--drop-after {
  box-shadow: inset 0 -3px 0 var(--primary);
  background: color-mix(in srgb, var(--primary) 7%, transparent);
}

.chapter-node__row--drop-inside {
  background: color-mix(in srgb, var(--primary) 12%, var(--bg-elevated));
  outline: 1px dashed var(--primary);
}

.chapter-node__grip {
  color: var(--text-muted);
  font-size: 18px;
  letter-spacing: -3px;
  user-select: none;
}

.chapter-node__type {
  font-size: 12px;
  color: var(--text-muted);
  width: 36px;
  flex-shrink: 0;
}

.chapter-node__title {
  font-weight: 500;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chapter-node__actions {
  margin-left: auto;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.chapter-node__children {
  list-style: none;
  padding-left: var(--space-8);
}
</style>
