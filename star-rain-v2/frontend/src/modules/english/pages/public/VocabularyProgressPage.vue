<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { useViewMode } from '../../../../shared/viewMode'
import {
  getVocabularyProgress,
  importLocalVocabularyProgress,
  resolveVocabularyAccount,
  vocabularyStudyStorage,
} from '../../api/englishApi'
import { directionLabel, intervalLabel } from '../../lib/vocabularyDisplay'
const REVIEW_INTERVAL_SECONDS = [300, 1200, 3600, 14400, 43200, 86400, 172800, 259200, 432000, 604800, 864000, 1209600, 1814400, 2419200]

/*
 * 学习进度页。
 *
 * 只呈现两件事实：计划里的固定复习间隔，以及真实发生过的复习记录。
 * 刻意不画「记忆保持率」——固定间隔表推不出保持率，画出来就是编数据（V1 的原话）。
 *
 * 本机数据区有三件事：导出 / 导入 JSON（换设备搬进度），以及登录后把本机进度合并到账号。
 */
const { contentPath } = useViewMode()

const progress = ref(null)
const loading = ref(true)
const error = ref('')
const account = ref(false)
const transferMessage = ref('')
const transferError = ref('')
const importing = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    account.value = await resolveVocabularyAccount()
    progress.value = await getVocabularyProgress()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function exportData() {
  transferError.value = ''
  try {
    const blob = new Blob([await vocabularyStudyStorage.exportJson()], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `star-rain-vocabulary-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    URL.revokeObjectURL(url)
    transferMessage.value = '本机学习数据已导出。'
  } catch (cause) {
    transferError.value = errorMessage(cause)
  }
}

async function importData(event) {
  const input = event.target
  const file = input.files?.[0]
  if (!file) return
  transferError.value = ''
  try {
    await vocabularyStudyStorage.importJson(await file.text())
    transferMessage.value = '本机学习数据已导入。'
    await load()
  } catch (cause) {
    transferError.value = errorMessage(cause)
  } finally {
    input.value = ''
  }
}

/* 合并本机游客进度到账号：后端只增不减，重复点也不会把账号进度改小 */
async function mergeIntoAccount() {
  if (importing.value) return
  importing.value = true
  transferError.value = ''
  transferMessage.value = ''
  try {
    const payload = await vocabularyStudyStorage.exportForAccountImport()
    await importLocalVocabularyProgress(payload)
    transferMessage.value = `已把本机 ${payload.memory.length} 条记忆与 ${payload.reviewLog.length} 条复习记录合并到账号。`
    await load()
  } catch (cause) {
    transferError.value = errorMessage(cause)
  } finally {
    importing.value = false
  }
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : ''
}

onMounted(load)
</script>

<template>
  <main class="progress">
    <header class="progress__header">
      <div>
        <p class="public-eyebrow">VOCABULARY PROGRESS</p>
        <h1>单词学习进度</h1>
        <span>这里展示真实评价历史。学习计划与永久记忆独立保存；旧记录不补造自评成绩。</span>
      </div>
      <div class="progress__nav">
        <RouterLink :to="contentPath('/english/vocabulary/study')">今日背单词</RouterLink>
        <RouterLink :to="contentPath('/english/vocabulary')">词汇总览</RouterLink>
      </div>
    </header>

    <p v-if="loading" class="progress__state">正在统计…</p>
    <p v-else-if="error" class="progress__state" role="alert">{{ error }} <button type="button" @click="load">重试</button></p>
    <template v-else-if="progress">
      <div class="progress__stats">
        <article><strong>{{ progress.activeWords }}</strong><span>记忆档案单词</span></article>
        <article><strong>{{ progress.dueWords }}</strong><span>当前到期</span></article>
        <article><strong>{{ progress.completedToday }}</strong><span>今日完成</span></article>
        <article><strong>{{ progress.totalReviews }}</strong><span>累计复习</span></article>
      </div>

      <section class="progress__curve">
        <div>
          <h2>动态强化复习间隔</h2>
          <p>每个方向独立演进：忘记回到 5 分钟，模糊回退两级。未毕业最长 28 天；跨日达到出神入化后固定 35 天。具体间隔为产品经验参数。</p>
        </div>
        <div class="progress__intervals">
          <span v-for="(seconds, index) in REVIEW_INTERVAL_SECONDS" :key="`i-${seconds}`">{{ index + 1 }} · {{ intervalLabel(seconds) }}</span>
        </div>
      </section>

      <section class="progress__history">
        <h2>最近真实复习</h2>
        <p v-if="!progress.recentReviews.length" class="progress__state">完成一次复习后，这里会出现真实时间点。</p>
        <article v-for="item in progress.recentReviews" :key="item.id ?? item.reviewSessionId" class="progress__row">
          <div>
            <strong>{{ item.word ?? `单词 #${item.wordId}` }}</strong>
            <span>第 {{ item.reviewNumber }} 次 · {{ directionLabel(item.direction) }} · {{ { FORGOT: '忘记', UNCERTAIN: '模糊', KNOW: '掌握' }[item.rating] || '历史记录（无评分）' }}</span>
          </div>
          <div>
            <time>{{ formatTime(item.reviewedAt) }}</time>
            <span :class="String(item.timingStatus).toLowerCase()">{{ item.timingStatus }} · 间隔 {{ intervalLabel(item.intervalSeconds) }}</span>
          </div>
        </article>
      </section>

      <section class="progress__data">
        <div>
          <h2>本机与账号</h2>
          <p v-if="account">当前已登录：记忆进度保存在账号里，可以把本机（游客时期）的进度合并上来。</p>
          <p v-else>当前未登录：记忆进度保存在这台浏览器里，登录后可在同一页面合并到账号。</p>
        </div>
        <div class="progress__data-actions">
          <button type="button" @click="exportData">导出本机数据</button>
          <label>
            导入本机数据
            <input type="file" accept="application/json" @change="importData">
          </label>
          <button v-if="account" type="button" class="primary" :disabled="importing" @click="mergeIntoAccount">
            {{ importing ? '正在合并…' : '合并本机进度到账号' }}
          </button>
        </div>
        <p v-if="transferError" class="progress__message progress__message--error" role="alert">{{ transferError }}</p>
        <p v-else-if="transferMessage" class="progress__message" role="status">{{ transferMessage }}</p>
      </section>
    </template>
  </main>
</template>

<style scoped>
.progress{max-width:1100px;margin:auto;padding-bottom:60px}
.progress__header{display:flex;justify-content:space-between;align-items:flex-end;gap:24px;margin-bottom:30px}
.progress__header h1{margin:6px 0;font-size:clamp(30px,5vw,40px)}
.progress__header span,.progress__curve p,.progress__data p{color:var(--text-secondary)}
.progress__nav{display:flex;gap:10px}
.progress__nav a,.progress__data-actions button,.progress__data-actions label{padding:9px 14px;border:1px solid var(--border);border-radius:9px;color:var(--text-secondary);background:var(--bg-surface);cursor:pointer}
.progress__data-actions label{position:relative;overflow:hidden}
.progress__data-actions label input{position:absolute;inset:0;opacity:0;cursor:pointer}
.progress__state{padding:40px 0;color:var(--text-secondary)}
.progress__state button{margin-left:8px;padding:6px 12px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer}
.progress__stats{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}
.progress__stats article{display:grid;gap:5px;padding:20px;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface)}
.progress__stats strong{font-size:30px;color:var(--primary)}
.progress__stats span{color:var(--text-muted)}
.progress__curve,.progress__history,.progress__data{margin-top:24px;padding:24px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}
.progress h2{margin:0 0 6px;font-size:22px}
.progress__curve svg{width:100%;max-height:260px;margin-top:16px}
.progress__curve line{stroke:var(--border)}
.progress__curve polyline{fill:none;stroke:var(--primary);stroke-width:3}
.progress__curve circle{fill:var(--accent)}
.progress__curve text{fill:var(--text-muted);font-size:10px;text-anchor:middle}
.progress__intervals{display:flex;flex-wrap:wrap;gap:8px}
.progress__intervals span{padding:4px 8px;border:1px solid var(--border);border-radius:999px;font-size:12px;color:var(--text-muted)}
.progress__row{display:flex;justify-content:space-between;gap:18px;padding:14px 0;border-bottom:1px solid var(--border)}
.progress__row>div{display:flex;gap:10px}
.progress__row span,.progress__row time{font-size:13px;color:var(--text-muted)}
.progress__row .early{color:var(--accent)}
.progress__row .on_time{color:var(--primary)}
.progress__row .overdue{color:var(--accent)}
.progress__data{display:grid;gap:14px}
.progress__data-actions{display:flex;flex-wrap:wrap;gap:10px}
.progress__data-actions button.primary{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}
.progress__data-actions button:disabled{cursor:wait;opacity:.7}
.progress__message{margin:0;font-size:13px;color:var(--primary)}
.progress__message--error{color:var(--accent)}
@media (max-width:700px){
  .progress__header{align-items:flex-start;flex-direction:column}
  .progress__stats{grid-template-columns:repeat(2,1fr)}
  .progress__row,.progress__row>div{flex-direction:column}
  .progress__curve,.progress__history,.progress__data{padding:18px}
}
</style>
