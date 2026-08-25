<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { fetchAuditLogs, type AuditLog } from '@/api/account'

const logs = ref<AuditLog[]>([])
const loading = ref(false)
const filters = reactive({ action: '', page: 1, pageSize: 20 })

async function load() {
  loading.value = true
  try {
    logs.value = await fetchAuditLogs({ ...filters, action: filters.action || undefined })
  } finally {
    loading.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <section class="audit-admin">
    <header>
      <p>SUPER ADMIN · 安全审计</p>
      <h1>审计日志</h1>
      <span>记录账号和协作相关事件，不记录密码、验证码和正文内容。</span>
    </header>
    <div class="audit-admin__filters">
      <el-input v-model="filters.action" placeholder="按事件类型筛选" clearable @keyup.enter="load" @clear="load" />
      <el-button @click="load">筛选</el-button>
    </div>
    <div v-loading="loading" class="audit-admin__list">
      <article v-for="log in logs" :key="log.id">
        <strong>{{ log.action }}</strong>
        <span>{{ log.result }} · {{ log.targetType || '-' }} #{{ log.targetId || '-' }} · {{ log.createdAt }}</span>
        <small>操作者：{{ log.actorId || '系统/匿名' }}</small>
      </article>
      <p v-if="!loading && !logs.length" class="empty">暂无审计日志。</p>
    </div>
  </section>
</template>

<style scoped>
.audit-admin header { margin-bottom: 18px; }
.audit-admin header p { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .16em; }
.audit-admin header h1 { margin: 6px 0; font-size: 32px; line-height: 1.18; }
.audit-admin header span, .audit-admin small, .audit-admin article span, .empty { color: var(--text-secondary); }
.audit-admin__filters { display: flex; gap: 10px; max-width: 520px; margin-bottom: 16px; }
.audit-admin__list { display: grid; gap: 10px; max-width: 980px; }
.audit-admin article { padding: 14px 16px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-surface); }
.audit-admin article strong, .audit-admin article span, .audit-admin article small { display: block; }
.audit-admin article span, .audit-admin article small { margin-top: 6px; }
</style>
