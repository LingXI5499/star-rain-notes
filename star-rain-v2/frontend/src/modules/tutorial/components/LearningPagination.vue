<script setup>
import PublicPagination from '../../../shared/ui/PublicPagination.vue'
defineProps({ page: { type: Number, default: 1 }, pageSize: { type: Number, default: 20 }, total: { type: Number, default: 0 }, loading: Boolean })
defineEmits(['page', 'pageSize'])
</script>
<template><PublicPagination :page="page" :page-size="pageSize" :total="total" :loading="loading" :page-sizes="[10, 20, 50]" label="学习记录分页" unit="条" @change="$emit('page', $event)" @page-size="$emit('pageSize', $event)" /></template>
