<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus/es/components/index.mjs'
import type { ProblemDetail } from '@/api/http'
import { deletePronunciationRule, fetchPronunciationRules, publishPronunciationRule, withdrawPronunciationRule, type PronunciationRule } from '@/api/listening'
import AdminContentActions from '@/components/admin/AdminContentActions.vue'
import PublishStatusBadge from '@/components/admin/PublishStatusBadge.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter(); const auth = useAuthStore(); const rules = ref<PronunciationRule[]>([]); const loading = ref(true)
const ruleTypes = ['LINKING','WEAK_FORM','ASSIMILATION','ELISION','STRESS','INTONATION']
const ruleLabel: Record<string,string> = { LINKING:'连读', WEAK_FORM:'弱读', ASSIMILATION:'同化', ELISION:'省音', STRESS:'重音', INTONATION:'语调' }
const grouped = computed(() => ruleTypes.map((type) => ({ type, items: rules.value.filter((rule) => rule.ruleType === type) })))
function detailOf(error: unknown, fallback: string) { return (error as {response?:{data?:ProblemDetail}}).response?.data?.detail ?? fallback }
async function load() { loading.value = true; try { rules.value = await fetchPronunciationRules() } catch (error) { ElMessage.error(detailOf(error, '加载语音规则失败。')) } finally { loading.value = false } }
function openCreate() { void router.push({ name: 'admin-listening-pronunciation-new' }) }
function openEdit(rule: PronunciationRule) { void router.push({ name: 'admin-listening-pronunciation-edit', params: { id: rule.id } }) }
async function setPublished(rule: PronunciationRule, publish: boolean) { try { publish ? await publishPronunciationRule(rule.id) : await withdrawPronunciationRule(rule.id); ElMessage.success(publish ? '规则已发布。' : '规则已撤回。'); await load() } catch (error) { ElMessage.error(detailOf(error, '操作失败。')) } }
async function remove(rule: PronunciationRule) { try { await ElMessageBox.confirm(`确定删除「${rule.title}」？`, '删除语音规则', { type: 'warning' }); await deletePronunciationRule(rule.id); ElMessage.success('规则已删除。'); await load() } catch (error) { const detail = (error as {response?:{data?:ProblemDetail}}).response?.data?.detail; if (detail) ElMessage.error(detail) } }
onMounted(load)
</script>

<template>
  <section class="pronunciation-manage"><header class="pronunciation-manage__hero"><div><p>PRONUNCIATION · 语音规则</p><h1>语音规则</h1><span>连读、弱读、同化、省音、重音与语调，按真实语流组织课程。</span></div><div><el-button @click="router.push({ name: 'admin-listening' })">听力材料</el-button><el-button type="primary" @click="openCreate">新建规则</el-button></div></header>
    <div v-loading="loading" class="pronunciation-manage__groups"><section v-for="group in grouped" :key="group.type" class="rule-group"><header><span>{{ group.type }}</span><h2>{{ ruleLabel[group.type] }}</h2><b>{{ group.items.length }}</b></header><p v-if="!group.items.length" class="rule-group__empty">暂无{{ ruleLabel[group.type] }}课程。</p><article v-for="rule in group.items" :key="rule.id" class="rule-card"><div class="rule-card__head"><h3>{{ rule.title }}</h3><PublishStatusBadge :status="rule.publishStatus" /></div><p>{{ rule.summary }}</p><AdminContentActions :permission-note="auth.isSuperAdmin ? '' : '发布和删除由超级管理员操作'"><el-button type="primary" plain @click="openEdit(rule)">编辑</el-button><el-button v-if="rule.publishStatus === 'PUBLISHED'" @click="router.push(`/english/listening/pronunciation/${rule.slug}`)">预览</el-button><el-button v-if="auth.isSuperAdmin && rule.publishStatus !== 'PUBLISHED'" type="success" plain @click="setPublished(rule, true)">{{ rule.publishStatus === 'WITHDRAWN' ? '重新发布' : '发布' }}</el-button><el-button v-else-if="auth.isSuperAdmin" type="warning" plain @click="setPublished(rule, false)">撤回</el-button><template v-if="auth.isSuperAdmin" #more><el-dropdown-item class="is-danger" @click="remove(rule)">删除</el-dropdown-item></template></AdminContentActions></article></section></div>
  </section>
</template>

<style scoped>
.pronunciation-manage{max-width:1240px;margin:0 auto}.pronunciation-manage__hero{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:24px}.pronunciation-manage__hero>div:last-child{display:flex;gap:8px}.pronunciation-manage__hero p{margin:0;color:var(--accent);font-size:11px;font-weight:750;letter-spacing:.15em}.pronunciation-manage__hero h1{margin:6px 0;font-size:clamp(28px,3vw,40px)}.pronunciation-manage__hero span{color:var(--text-secondary);font-size:13px}.pronunciation-manage__groups{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.rule-group{padding:18px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface)}.rule-group>header{display:grid;grid-template-columns:1fr auto;align-items:end;margin-bottom:14px}.rule-group>header span{grid-column:1/-1;color:var(--accent);font-size:10px;letter-spacing:.13em}.rule-group h2{margin:4px 0 0;font-size:18px}.rule-group>header b{color:var(--text-muted);font:600 13px var(--font-mono)}.rule-group__empty{color:var(--text-muted);font-size:13px}.rule-card{padding:15px 0;border-top:1px solid var(--border)}.rule-card:first-of-type{border-top:0}.rule-card__head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.rule-card h3{margin:0;font-size:16px}.rule-card>p{margin:7px 0 13px;color:var(--text-secondary);font-size:13px;line-height:1.65}@media(max-width:860px){.pronunciation-manage__groups{grid-template-columns:1fr}}@media(max-width:620px){.pronunciation-manage__hero{align-items:flex-start;flex-direction:column}.pronunciation-manage__hero>div:last-child{width:100%}.pronunciation-manage__hero :deep(.el-button){flex:1}}
</style>
