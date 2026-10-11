<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getPublicWork } from '../api/portfolioApi'
import { useViewMode } from '../../../shared/viewMode'
import { errorMessage } from '../../../shared/http'
const route = useRoute(), work = ref(null), error = ref('')
const { contentPath } = useViewMode()
onMounted(async () => { try { work.value = await getPublicWork(route.params.slug); if (!work.value.prototypeUrl) error.value = '这个作品暂无可体验的静态原型。' } catch (cause) { error.value = errorMessage(cause) } })
</script>
<template><section class="work-live"><header><RouterLink :to="contentPath(`/portfolio/${route.params.slug}`)">← 返回作品</RouterLink><h1>{{ work?.title || '作品原型' }}</h1><span>静态原型体验</span></header><p v-if="error" role="alert">{{ error }}</p><iframe v-else-if="work?.prototypeUrl" :src="work.prototypeUrl" :title="`${work.title}静态原型`" sandbox="allow-scripts" referrerpolicy="no-referrer" /><p v-else>正在加载原型…</p></section></template>
<style scoped>.work-live{padding:28px 0 50px}.work-live header{display:flex;align-items:center;gap:24px;flex-wrap:wrap;margin-bottom:22px}.work-live h1{margin:0;font-size:24px}.work-live a{color:var(--primary)}.work-live span{margin-left:auto;font-size:12px;color:var(--text-muted)}.work-live iframe{width:100%;height:calc(100vh - 230px);min-height:500px;border:1px solid var(--border);border-radius:16px;background:white}.work-live p{padding:50px;text-align:center;color:var(--text-muted)}</style>
