<script lang="ts">
export interface PublishCheck {
  key: string
  label: string
  passed: boolean
  detail?: string
}
</script>

<script setup lang="ts">
defineProps<{ open: boolean; checks: PublishCheck[] }>()

defineEmits<{ (e: 'close'): void }>()
</script>

<template>
  <el-drawer v-model="$props.open" title="发布检查" size="360px" @close="$emit('close')">
    <p class="publish-checklist__hint">发布前需逐项满足；不满足项会在下方列出，请补齐后再发布。</p>
    <ul class="publish-checklist__list">
      <li
        v-for="check in checks"
        :key="check.key"
        class="publish-checklist__item"
        :class="{ 'is-failed': !check.passed }"
      >
        <span class="publish-checklist__mark">{{ check.passed ? '✓' : '✗' }}</span>
        <div>
          <p class="publish-checklist__label">{{ check.label }}</p>
          <p v-if="!check.passed && check.detail" class="publish-checklist__detail">{{ check.detail }}</p>
        </div>
      </li>
    </ul>
    <el-button type="primary" class="publish-checklist__action" :disabled="checks.some((c) => !c.passed)" @click="$emit('close')">
      准备就绪，可以发布
    </el-button>
  </el-drawer>
</template>

<style scoped>
.publish-checklist__hint { color: var(--text-secondary); font-size: 13px; margin: 0 0 16px; }
.publish-checklist__list { display: flex; flex-direction: column; gap: 10px; }
.publish-checklist__item { display: flex; align-items: flex-start; gap: 10px; }
.publish-checklist__mark { width: 20px; height: 20px; border-radius: 50%; display: grid; place-items: center; font-size: 12px; background: color-mix(in srgb, var(--primary) 14%, transparent); color: var(--primary); flex-shrink: 0; }
.publish-checklist__item.is-failed .publish-checklist__mark { background: color-mix(in srgb, var(--danger) 14%, transparent); color: var(--danger); }
.publish-checklist__label { margin: 0; font-weight: 600; font-size: 13px; }
.publish-checklist__detail { margin: 2px 0 0; font-size: 12px; color: var(--danger); }
.publish-checklist__action { width: 100%; margin-top: 20px; }
</style>
