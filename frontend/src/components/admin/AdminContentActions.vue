<script setup lang="ts">
import { computed } from 'vue'
import { useSlots } from 'vue'
const props = defineProps<{ permissionNote?: string; showMore?: boolean }>()
const slots = useSlots()
const hasMoreActions = computed(() => props.showMore === true || Boolean(slots.more))
</script>
<template><div class="admin-content-actions"><div class="admin-content-actions__primary"><slot /></div><el-dropdown v-if="hasMoreActions" trigger="click"><el-button class="admin-content-actions__more" aria-label="打开更多操作">更多 ···</el-button><template #dropdown><el-dropdown-menu><slot name="more" /></el-dropdown-menu></template></el-dropdown><small v-if="permissionNote" class="admin-content-actions__note">{{ permissionNote }}</small></div></template>
<style scoped>.admin-content-actions{display:flex;align-items:center;flex-wrap:wrap;gap:8px}.admin-content-actions__primary{display:flex;align-items:center;flex-wrap:wrap;gap:8px}.admin-content-actions :deep(.el-button){min-height:36px;margin-left:0;border-radius:9px}.admin-content-actions__more{color:var(--text-secondary)}.admin-content-actions__note{flex-basis:100%;color:var(--text-muted);font-size:11px}.admin-content-actions :deep(.el-dropdown-menu__item.is-danger){color:var(--danger,#d84a4a)}@media(max-width:520px){.admin-content-actions{align-items:stretch}.admin-content-actions__primary{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));flex:1}.admin-content-actions__primary :deep(.el-button){width:100%}}</style>
