<script setup>
import PublicPagination from '../../../shared/ui/PublicPagination.vue'
defineProps({ page: { type: Number, default: 1 }, totalPages: Number, total: { type: Number, default: 0 }, pageSize: { type: Number, default: 10 }, loading: Boolean })
defineEmits(['change', 'page-size'])
</script>
<template><PublicPagination :page="page" :page-size="pageSize" :total="total" :page-sizes="[10, 20, 30, 50]" :loading="loading" label="博客分页" unit="篇" @change="$emit('change', $event)" @page-size="$emit('page-size', $event)" /></template>
