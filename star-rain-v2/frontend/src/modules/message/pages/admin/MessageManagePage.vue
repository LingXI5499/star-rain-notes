<script setup>
import { onMounted, ref } from 'vue'
import { errorMessage } from '../../../../shared/http'
import { approveMessage, deleteMessage, hideMessage, listAdminMessages, listMessageActions, rejectMessage, restoreMessage } from '../../api/messageApi'

const statuses = [
  { value: 'PENDING', label: '待审核' }, { value: 'PUBLIC', label: '已公开' },
  { value: 'HIDDEN', label: '已隐藏' }, { value: 'REJECTED', label: '已拒绝' },
  { value: 'DELETED', label: '已删除' },
]
const status = ref('PENDING')
const page = ref(1)
const total = ref(0)
const items = ref([])
const loading = ref(false)
const actingId = ref(null)
const rejectingId = ref(null)
const rejectReason = ref('')
const error = ref('')
const expandedId = ref(null)
const history = ref([])
const pageSize = 20

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await listAdminMessages({ page: page.value, pageSize, status: status.value })
    items.value = result.items || []
    total.value = result.total || 0
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function selectStatus(value) {
  status.value = value
  page.value = 1
  expandedId.value = null
  await load()
}

async function act(item, action) {
  if (actingId.value) return
  let reason = ''
  if (action === 'reject') {
    reason = rejectReason.value.trim()
    if (!reason) return
  }
  if (action === 'delete' && !window.confirm('确定软删除这条留言？删除后不能恢复。')) return
  actingId.value = item.id
  error.value = ''
  try {
    if (action === 'approve') await approveMessage(item.id)
    if (action === 'reject') await rejectMessage(item.id, reason)
    if (action === 'hide') await hideMessage(item.id)
    if (action === 'restore') await restoreMessage(item.id)
    if (action === 'delete') await deleteMessage(item.id)
    rejectingId.value = null
    rejectReason.value = ''
    await load()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    actingId.value = null
  }
}

async function toggleHistory(item) {
  if (expandedId.value === item.id) { expandedId.value = null; return }
  expandedId.value = item.id
  try {
    history.value = await listMessageActions(item.id)
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

function date(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '—'
}

onMounted(load)
</script>

<template>
  <main class="message-admin">
    <header class="message-admin__header">
      <p>MESSAGE MODERATION · 协作</p>
      <h1>留言管理</h1>
      <span>审核新留言，管理已公开内容。联系邮箱仅在后台显示。</span>
    </header>

    <nav class="message-admin__tabs" aria-label="留言状态">
      <button v-for="option in statuses" :key="option.value" type="button"
        :class="{ active: status === option.value }" @click="selectStatus(option.value)">{{ option.label }}</button>
    </nav>
    <p v-if="error" class="message-admin__error" role="alert">{{ error }}</p>
    <section class="message-admin__panel">
      <div class="message-admin__count">{{ statuses.find((option) => option.value === status)?.label }} · 共 {{ total }} 条</div>
      <p v-if="loading" class="message-admin__empty">正在加载…</p>
      <p v-else-if="!items.length" class="message-admin__empty">当前没有此状态的留言。</p>
      <ol v-else class="message-admin__items">
        <li v-for="item in items" :key="item.id" class="message-admin__item">
          <div class="message-admin__meta"><strong>{{ item.authorDisplayName }}</strong><time :datetime="item.submittedAt">{{ date(item.submittedAt) }}</time></div>
          <p class="message-admin__content">{{ item.content }}</p>
          <p v-if="item.contactEmail" class="message-admin__private">联系邮箱（不公开）：{{ item.contactEmail }}</p>
          <p v-if="item.rejectReason" class="message-admin__reason">拒绝原因：{{ item.rejectReason }}</p>
          <div class="message-admin__actions">
            <button v-if="status === 'PENDING'" type="button" :disabled="!!actingId" @click="act(item, 'approve')">通过</button>
            <button v-if="status === 'PENDING'" type="button" :disabled="!!actingId" @click="rejectingId = rejectingId === item.id ? null : item.id; rejectReason = ''">拒绝</button>
            <button v-if="status === 'PUBLIC'" type="button" :disabled="!!actingId" @click="act(item, 'hide')">隐藏</button>
            <button v-if="status === 'HIDDEN'" type="button" :disabled="!!actingId" @click="act(item, 'restore')">恢复公开</button>
            <button v-if="status === 'PUBLIC' || status === 'HIDDEN'" type="button" :disabled="!!actingId" @click="act(item, 'delete')">删除</button>
            <button type="button" @click="toggleHistory(item)">{{ expandedId === item.id ? '收起记录' : '操作记录' }}</button>
          </div>
          <form v-if="rejectingId === item.id" class="message-admin__reject" @submit.prevent="act(item, 'reject')">
            <label :for="`reject-${item.id}`">拒绝原因</label>
            <textarea :id="`reject-${item.id}`" v-model.trim="rejectReason" required maxlength="1000" rows="3" placeholder="请说明拒绝原因" />
            <button type="submit" :disabled="!!actingId || !rejectReason.trim()">确认拒绝</button>
          </form>
          <ol v-if="expandedId === item.id" class="message-admin__history">
            <li v-for="(entry, index) in history" :key="index">{{ date(entry.createdAt) }} · {{ entry.actionType }}<span v-if="entry.note"> · {{ entry.note }}</span></li>
          </ol>
        </li>
      </ol>
      <div v-if="total > pageSize" class="message-admin__pager">
        <button type="button" :disabled="page <= 1 || loading" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
        <button type="button" :disabled="page * pageSize >= total || loading" @click="page++; load()">下一页</button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.message-admin { max-width: 1180px; margin: 0 auto; padding: 36px 36px 90px; }
.message-admin__header > p { color: var(--accent); font-size: 11px; font-weight: 700; letter-spacing: .14em; }
.message-admin__header h1 { font-size: 36px; margin: 12px 0 10px; color: var(--text-primary); }
.message-admin__header > span { color: var(--text-secondary); }
.message-admin__tabs { display: flex; gap: 10px; flex-wrap: wrap; margin: 34px 0 22px; }
.message-admin button { border: 1px solid var(--border); border-radius: 9px; padding: 9px 14px; background: var(--bg-surface); color: var(--text-primary); cursor: pointer; }
.message-admin button:hover, .message-admin button.active { border-color: var(--primary); color: var(--primary); }
.message-admin button:disabled { opacity: .45; cursor: default; }
.message-admin__panel { border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); overflow: hidden; }
.message-admin__count { padding: 20px 26px; background: color-mix(in srgb, var(--primary) 5%, var(--bg-surface)); font-weight: 700; }
.message-admin__items { list-style: none; padding: 0; margin: 0; }
.message-admin__item { padding: 25px 26px; border-top: 1px solid var(--border); }
.message-admin__meta { display: flex; justify-content: space-between; gap: 20px; color: var(--text-primary); }
.message-admin__meta time { font-size: 12px; color: var(--text-muted); }
.message-admin__content { color: var(--text-secondary); white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.75; margin: 12px 0; }
.message-admin__private, .message-admin__reason { font-size: 13px; color: var(--text-muted); margin: 8px 0; }
.message-admin__actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 17px; }
.message-admin__reject { margin-top: 16px; display: grid; gap: 8px; max-width: 540px; color: var(--text-secondary); font-size: 13px; }
.message-admin__reject textarea { border: 1px solid var(--border); border-radius: 8px; background: var(--bg-page); color: var(--text-primary); padding: 10px; font: inherit; resize: vertical; }
.message-admin__reject button { justify-self: start; }
.message-admin__history { margin: 18px 0 0; padding: 14px 20px 14px 35px; border-radius: 10px; background: var(--bg-page); color: var(--text-secondary); font-size: 12px; line-height: 2; }
.message-admin__error { color: #bc4d37; margin-bottom: 18px; }
.message-admin__empty { text-align: center; color: var(--text-muted); padding: 54px 15px; }
.message-admin__pager { display: flex; justify-content: center; gap: 16px; align-items: center; padding: 20px; border-top: 1px solid var(--border); }
@media(max-width: 700px) { .message-admin { padding-inline: 18px; } .message-admin__item { padding-inline: 18px; } }
</style>
