<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { listAudits } from '../api/accountApi'
import { errorMessage } from '../api/http'
import { dateLabel, statusLabel, utcQueryTime, auditActions, actionLabel } from '../support/display'
import { openDateTimePicker } from '../../../shared/dateTimePicker'

const filters = reactive({ actorAccountId: '', targetAccountId: '', actionCode: '', result: '', startTime: '', endTime: '' })
const page = ref(1)
const data = ref({ items: [], total: 0, page: 1, pageSize: 20 })
const loading = ref(false)
const error = ref('')
const totalPages = computed(() => Math.max(1, Math.ceil(data.value.total / data.value.pageSize)))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const query = { ...filters, startTime: utcQueryTime(filters.startTime), endTime: utcQueryTime(filters.endTime) }
    data.value = await listAudits({
      ...Object.fromEntries(Object.entries(query).filter(([, value]) => value)),
      page: page.value, pageSize: 20,
    })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}
function search() { page.value = 1; load() }
function changePage(next) { page.value = next; load() }
onMounted(load)
</script>

<template>
  <main class="page-container">
    <div class="page-heading"><p class="eyebrow">ACCOUNT AUDIT</p><h1>账户审计</h1><p>查看账户操作记录与结果。</p></div>
    <section class="surface-card">
      <form class="toolbar toolbar--wrap" @submit.prevent="search">
        <label>操作人 ID<input v-model.trim="filters.actorAccountId" inputmode="numeric" placeholder="全部" /></label>
        <label>目标账户 ID<input v-model.trim="filters.targetAccountId" inputmode="numeric" placeholder="全部" /></label>
        <label>动作<select v-model="filters.actionCode"><option value="">全部</option><option v-for="(label, code) in auditActions" :key="code" :value="code">{{ label }}</option></select></label>
        <label>结果<select v-model="filters.result"><option value="">全部</option><option value="SUCCESS">成功</option><option value="FAILED">失败</option></select></label>
        <label>开始时间<input v-model="filters.startTime" type="datetime-local" @click="openDateTimePicker" /></label>
        <label>结束时间<input v-model="filters.endTime" type="datetime-local" @click="openDateTimePicker" /></label>
        <button class="primary-button" type="submit">查询记录</button>
      </form>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <p v-if="loading" class="loading" role="status">正在加载审计记录…</p>
      <div class="table-scroll"><table><thead><tr><th>时间</th><th>动作</th><th>操作人</th><th>目标账户</th><th>结果</th><th>IP 地址</th></tr></thead>
        <tbody><tr v-for="item in data.items" :key="item.id"><td>{{ dateLabel(item.createdAt) }}</td><td><strong>{{ actionLabel(item.actionCode) }}</strong><small>{{ item.actionCode }} · #{{ item.id }}</small></td><td>{{ item.actorAccountId || '—' }}</td><td>{{ item.targetAccountId || '—' }}</td><td><span :class="['status-chip', item.result === 'FAILED' && 'status-chip--danger']">{{ statusLabel(item.result) }}</span></td><td>{{ item.ipAddress || '—' }}</td></tr><tr v-if="!loading && !data.items.length"><td colspan="6" class="empty-state">没有符合条件的记录。</td></tr></tbody></table></div>
      <div class="pagination"><span>共 {{ data.total }} 条</span><div><button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ totalPages }}</span><button type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button></div></div>
    </section>
  </main>
</template>
