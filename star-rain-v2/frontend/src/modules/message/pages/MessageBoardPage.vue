<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '../../account/stores/authStore'
import { errorMessage } from '../../../shared/http'
import { listPublicMessages, submitMessage } from '../api/messageApi'

const auth = useAuthStore()
const form = reactive({ authorDisplayName: '', contactEmail: '', content: '' })
const items = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const notice = ref('')
const pageSize = 12

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await listPublicMessages({ page: page.value, pageSize })
    items.value = result.items || []
    total.value = result.total || 0
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (submitting.value) return
  submitting.value = true
  error.value = ''
  notice.value = ''
  try {
    await submitMessage({ ...form })
    form.content = ''
    notice.value = '留言已提交，审核通过后会显示在这里。'
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    submitting.value = false
  }
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) : ''
}

async function changePage(next) {
  page.value = next
  await load()
}

onMounted(load)
</script>

<template>
  <main class="message-board">
    <header class="message-board__hero">
      <p class="message-board__eyebrow">MESSAGE BOARD · 留言</p>
      <h1>留下你的想法</h1>
      <p>关于文章、作品，或只是想打个招呼，都欢迎写在这里。</p>
    </header>

    <div class="message-board__columns">
      <section class="message-board__form-card" aria-labelledby="message-form-title">
        <p class="message-board__eyebrow">WRITE A MESSAGE</p>
        <h2 id="message-form-title">写一条留言</h2>
        <p class="message-board__hint">提交后先由管理员审核。联系邮箱仅供回复使用，不会公开。</p>
        <form @submit.prevent="submit">
          <label v-if="!auth.currentUser">署名
            <input v-model.trim="form.authorDisplayName" type="text" required maxlength="100" autocomplete="name" placeholder="怎么称呼你？" />
          </label>
          <p v-else class="message-board__identity">将以当前账号显示名署名</p>
          <label>联系邮箱 <span>选填 · 不公开</span>
            <input v-model.trim="form.contactEmail" type="email" maxlength="128" autocomplete="email" placeholder="方便我们私下回复" />
          </label>
          <label>留言内容
            <textarea v-model.trim="form.content" required maxlength="3000" rows="7" placeholder="写下你想说的话……" />
          </label>
          <div class="message-board__form-bottom">
            <span>{{ form.content.length }} / 3000</span>
            <button type="submit" :disabled="submitting">{{ submitting ? '提交中…' : '提交留言' }}</button>
          </div>
          <p v-if="notice" class="message-board__notice" role="status">{{ notice }}</p>
          <p v-if="error" class="message-board__error" role="alert">{{ error }}</p>
        </form>
      </section>

      <section class="message-board__list" aria-labelledby="message-list-title">
        <div class="message-board__list-head">
          <div>
            <p class="message-board__eyebrow">COMMUNITY</p>
            <h2 id="message-list-title">公开留言 <small>{{ total }}</small></h2>
          </div>
        </div>
        <p v-if="loading" class="message-board__empty">正在加载留言…</p>
        <p v-else-if="!items.length" class="message-board__empty">还没有公开留言，欢迎写下第一条。</p>
        <ol v-else class="message-board__items">
          <li v-for="item in items" :key="item.id" class="message-board__item">
            <div class="message-board__item-top"><strong>{{ item.authorDisplayName }}</strong><time :datetime="item.submittedAt">{{ formatDate(item.submittedAt) }}</time></div>
            <p>{{ item.content }}</p>
          </li>
        </ol>
        <div v-if="total > pageSize" class="message-board__pager">
          <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <span>{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
          <button type="button" :disabled="page * pageSize >= total || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.message-board { max-width: var(--layout-max-width); margin: 0 auto; padding: clamp(54px, 7vw, 108px) var(--page-padding-x) 110px; }
.message-board__hero { max-width: 680px; margin-bottom: 54px; }
.message-board__eyebrow { color: var(--accent); font-size: 11px; font-weight: 750; letter-spacing: .18em; }
.message-board__hero h1 { font-size: clamp(42px, 6vw, 72px); line-height: 1.12; margin: 15px 0 20px; color: var(--text-primary); }
.message-board__hero > p:last-child, .message-board__hint { color: var(--text-secondary); line-height: 1.8; }
.message-board__columns { display: grid; grid-template-columns: minmax(300px, 420px) minmax(0, 1fr); gap: clamp(36px, 6vw, 88px); align-items: start; }
.message-board__form-card { padding: 32px; border: 1px solid var(--border); border-radius: 20px; background: var(--bg-surface); }
.message-board h2 { color: var(--text-primary); font-size: 25px; margin: 8px 0 10px; }
.message-board__hint { font-size: 14px; margin-bottom: 27px; }
.message-board label { display: block; color: var(--text-secondary); font-size: 14px; font-weight: 600; margin: 20px 0; }
.message-board label span { font-size: 12px; color: var(--text-muted); font-weight: 400; }
.message-board input, .message-board textarea { display: block; width: 100%; box-sizing: border-box; margin-top: 9px; border: 1px solid var(--border); border-radius: 10px; padding: 13px 14px; background: var(--bg-page); color: var(--text-primary); font: inherit; resize: vertical; }
.message-board input:focus, .message-board textarea:focus { outline: 2px solid var(--primary); outline-offset: 2px; }
.message-board__identity { color: var(--text-muted); font-size: 13px; }
.message-board__form-bottom { display: flex; align-items: center; justify-content: space-between; color: var(--text-muted); font-size: 12px; }
.message-board__form-bottom button { border: 0; border-radius: 10px; background: var(--primary); color: white; padding: 12px 22px; font-weight: 700; cursor: pointer; }
.message-board__form-bottom button:disabled { opacity: .6; cursor: wait; }
.message-board__notice { color: var(--primary); margin-top: 17px; }
.message-board__error { color: #bb4c36; margin-top: 17px; }
.message-board__list-head { border-bottom: 1px solid var(--border); padding-bottom: 16px; }
.message-board__list-head small { color: var(--text-muted); font-size: 15px; font-weight: 400; }
.message-board__empty { color: var(--text-muted); padding: 52px 0; }
.message-board__items { list-style: none; padding: 0; }
.message-board__item { padding: 27px 0; border-bottom: 1px solid var(--border); }
.message-board__item-top { display: flex; justify-content: space-between; gap: 14px; margin-bottom: 12px; }
.message-board__item-top strong { color: var(--text-primary); }
.message-board__item-top time { color: var(--text-muted); font-size: 12px; white-space: nowrap; }
.message-board__item > p { color: var(--text-secondary); line-height: 1.9; white-space: pre-wrap; overflow-wrap: anywhere; }
.message-board__pager { display: flex; justify-content: center; align-items: center; gap: 22px; margin-top: 28px; }
.message-board__pager button { border: 1px solid var(--border); border-radius: 8px; background: var(--bg-surface); color: var(--text-primary); padding: 8px 16px; cursor: pointer; }
.message-board__pager button:disabled { opacity: .45; cursor: default; }
@media(max-width: 820px) { .message-board__columns { grid-template-columns: 1fr; } .message-board__form-card { padding: 24px; } }
</style>
