<script setup lang="ts">
import type { AdminTreeNode } from '@/api/tutorial'

const props = defineProps<{
  node: AdminTreeNode
}>()

const emit = defineEmits<{
  (e: 'rename', node: AdminTreeNode): void
  (e: 'delete', node: AdminTreeNode): void
  (e: 'publish', node: AdminTreeNode): void
  (e: 'move', node: AdminTreeNode, delta: number): void
}>()

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
</script>

<template>
  <li class="chapter-node">
    <div class="chapter-node__row">
      <span class="chapter-node__type">{{ node.type === 'GROUP' ? '分组' : '章节' }}</span>
      <span class="chapter-node__title">{{ node.title }}</span>
      <el-tag v-if="node.type === 'CHAPTER'" size="small" :type="statusType(node.publishStatus)">
        {{ statusLabel(node.publishStatus) }}
      </el-tag>
      <span class="chapter-node__actions">
        <el-button v-if="node.type === 'GROUP'" link type="primary" @click="emit('rename', node)">重命名</el-button>
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
      />
    </ul>
  </li>
</template>

<style scoped>
.chapter-node {
  list-style: none;
}

.chapter-node__row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--border);
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
