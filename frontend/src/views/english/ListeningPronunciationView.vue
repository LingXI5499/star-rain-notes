<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchPublicPronunciationRules, type PronunciationRule } from '@/api/listening'
import EnglishModuleHero from '@/components/english/EnglishModuleHero.vue'

const rules = ref<PronunciationRule[]>([]); const loading = ref(true); const error = ref(false)
const ruleTypes = ['LINKING','WEAK_FORM','ASSIMILATION','ELISION','STRESS','INTONATION']
const ruleLabel: Record<string,string> = { LINKING:'连读', WEAK_FORM:'弱读', ASSIMILATION:'同化', ELISION:'省音', STRESS:'重音', INTONATION:'语调' }
const grouped = computed(() => ruleTypes.map((t) => ({ type: t, label: ruleLabel[t], items: rules.value.filter((r) => r.ruleType === t) })))
onMounted(async () => { try { rules.value = await fetchPublicPronunciationRules() } catch { error.value = true } finally { loading.value = false } })
</script>

<template>
  <section>
    <EnglishModuleHero tag="PRONUNCIATION · 语音规则" title="语音规则" subtitle="连读、弱读、同化、省音、重音、语调。" description="独立语音规则教学支线，含示例音频与讲解。"/>
    <div v-if="error" class="pr-empty">加载失败，请稍后重试。</div>
    <div v-else v-loading="loading" class="pr-list">
      <section v-for="g in grouped" :key="g.type" class="pr-public-group"><h2>{{ g.label }} <small>{{ g.type }}</small></h2>
        <p v-if="!g.items.length" class="pr-empty">暂无规则。</p>
        <div class="pr-public-grid"><RouterLink v-for="r in g.items" :key="r.id" :to="`/english/listening/pronunciation/${r.slug}`" class="pr-public-card"><h3>{{ r.title }}</h3><p>{{ r.summary }}</p><span>开始学习 →</span></RouterLink></div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.pr-list{display:flex;flex-direction:column;gap:var(--space-8)}.pr-public-group h2{font-size:20px;margin:0 0 12px}.pr-public-group h2 small{font-size:11px;color:var(--text-muted);font-weight:400}.pr-public-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:var(--space-4)}
.pr-public-card{display:flex;flex-direction:column;gap:8px;padding:18px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);color:inherit;transition:transform .16s ease,border-color .16s ease}.pr-public-card:hover{transform:translateY(-3px);border-color:var(--primary)}
.pr-public-card h3{font-size:17px;margin:0}.pr-public-card p{font-size:13px;color:var(--text-secondary);margin:0}.pr-public-card span{color:var(--primary);font-size:13px;margin-top:auto}.pr-empty{color:var(--text-muted);padding:var(--space-4) 0}
</style>
