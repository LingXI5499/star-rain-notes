<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchPublicBundles, type LearningBundle } from '@/api/englishBundle'
import LearningBundleCards from '@/components/english/LearningBundleCards.vue'

const bundles = ref<LearningBundle[]>([])
const loading = ref(true)
const failed = ref(false)
onMounted(async()=>{try{bundles.value=await fetchPublicBundles()}catch{failed.value=true}finally{loading.value=false}})
</script>

<template>
  <section class="bundles-page">
    <header><p>LEARNING PATHS · 跨模块学习</p><h1>用一条路径，串联阅读、听力与写作</h1><span>学习组合由管理员明确编排，同一主题按输入、理解与输出形成闭环。</span></header>
    <p v-if="loading" class="state">正在加载学习组合…</p>
    <p v-else-if="failed" class="state">暂时无法加载，请稍后重试。</p>
    <LearningBundleCards v-else :bundles="bundles" empty-hint="暂无已发布的学习组合。" />
  </section>
</template>

<style scoped>
.bundles-page{max-width:1100px;margin:auto}.bundles-page>header{padding:24px 0 42px}.bundles-page header p{color:var(--accent);font-size:11px;font-weight:800;letter-spacing:.16em}.bundles-page h1{max-width:820px;margin:10px 0 14px;font-size:clamp(34px,5vw,58px);line-height:1.12}.bundles-page header span{color:var(--text-secondary);line-height:1.7}.state{padding:70px 0;text-align:center;color:var(--text-muted)}
</style>
