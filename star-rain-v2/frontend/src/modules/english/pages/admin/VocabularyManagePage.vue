<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import {
  createVocabularyTheme, createVocabularyWord, deleteVocabularyTheme, deleteVocabularyWord,
  getVocabularyThemes, getVocabularyWords, updateVocabularyTheme, updateVocabularyWord,
} from '../../api/englishApi'

const themes = ref([])
const words = ref({ items: [], total: 0 })
const activeTheme = ref('')
const activeLayer = ref('')
const page = ref(1)
const search = ref('')
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const themeFormOpen = ref(false)
const wordFormOpen = ref(false)
const themeForm = reactive({ id: '', layer: '', layerOrder: 0, name: '', sortOrder: 0 })
const wordForm = reactive({ id: '', themeId: '', word: '', partOfSpeech: '', phoneticUs: '', phoneticUk: '', translation: '', sceneMeaning: '', inflections: '', examples: '', sortOrder: 0 })
const groupedThemes = computed(() => {
  const map = new Map()
  for (const theme of themes.value) {
    if (!map.has(theme.layer)) map.set(theme.layer, [])
    map.get(theme.layer).push(theme)
  }
  return [...map].map(([name, items]) => ({ name, items }))
})

const visibleThemes = computed(() => activeLayer.value
  ? groupedThemes.value.find((group) => group.name === activeLayer.value)?.items || []
  : groupedThemes.value.flatMap((group) => group.items))

async function loadThemes() { themes.value = await getVocabularyThemes() }
async function loadWords() { words.value = await getVocabularyWords({ themeId: activeTheme.value || undefined, search: search.value || undefined, page: page.value, size: 20 }) }
async function load() {
  loading.value = true
  error.value = ''
  try { await Promise.all([loadThemes(), loadWords()]) }
  catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

function selectLayer(name) { activeLayer.value = name; selectTheme('') }
function selectTheme(id) { activeTheme.value = id; page.value = 1; loadWords().catch((cause) => { error.value = errorMessage(cause) }) }
function openTheme(theme = null) { Object.assign(themeForm, theme || { id: '', layer: activeLayer.value || groupedThemes.value[0]?.name || '', layerOrder: 0, name: '', sortOrder: 0 }); themeFormOpen.value = true }
function openWord(word = null) {
  Object.assign(wordForm, word || { id: '', themeId: activeTheme.value || themes.value[0]?.id || '', word: '', partOfSpeech: '', phoneticUs: '', phoneticUk: '', translation: '', sceneMeaning: '', inflections: '', examples: '[]', sortOrder: 0 })
  let parsed = []
  try { parsed = JSON.parse(wordForm.examples || '[]') } catch { /* keep empty editor */ }
  wordForm.examples = parsed.map((item) => typeof item === 'string' ? item : `${item.sentence || item.text || ''} | ${item.translation || ''}`).join('\n')
  wordFormOpen.value = true
}

async function saveTheme() {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    const payload = { layer: themeForm.layer, layerOrder: Number(themeForm.layerOrder), name: themeForm.name, sortOrder: Number(themeForm.sortOrder) }
    if (themeForm.id) await updateVocabularyTheme(themeForm.id, payload)
    else await createVocabularyTheme(payload)
    themeFormOpen.value = false; await loadThemes(); notice.value = '主题已保存。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

async function saveWord() {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    const examples = wordForm.examples.split('\n').map((line) => line.trim()).filter(Boolean).map((line) => {
      const [sentence, translation = ''] = line.split(' | ')
      return { sentence, translation }
    })
    const payload = {
      themeId: wordForm.themeId, word: wordForm.word, partOfSpeech: wordForm.partOfSpeech,
      phoneticUs: wordForm.phoneticUs, phoneticUk: wordForm.phoneticUk,
      translation: wordForm.translation, sceneMeaning: wordForm.sceneMeaning, inflections: wordForm.inflections,
      examples: JSON.stringify(examples),
      sortOrder: Number(wordForm.sortOrder),
    }
    if (wordForm.id) await updateVocabularyWord(wordForm.id, payload)
    else await createVocabularyWord(payload)
    wordFormOpen.value = false; await Promise.all([loadWords(), loadThemes()]); notice.value = '单词已保存。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

async function removeTheme(theme) {
  if (!window.confirm(`删除空主题「${theme.name}」？`)) return
  try { await deleteVocabularyTheme(theme.id); if (activeTheme.value === theme.id) activeTheme.value = ''; await load(); notice.value = '主题已删除。' }
  catch (cause) { error.value = errorMessage(cause) }
}
async function removeWord(word) {
  if (!window.confirm(`删除单词「${word.word}」？`)) return
  try { await deleteVocabularyWord(word.id); await load(); notice.value = '单词已删除。' }
  catch (cause) { error.value = errorMessage(cause) }
}

watch(page, () => loadWords().catch((cause) => { error.value = errorMessage(cause) }))
load()
</script>

<template>
  <main class="vocab-admin">
    <nav class="vocab-admin__breadcrumb"><RouterLink :to="accountPath('/english/manage')">英语工作台</RouterLink><span>/</span><strong>单词管理</strong></nav>
    <header class="vocab-admin__hero">
      <div>
        <p class="public-eyebrow">VOCABULARY TAXONOMY · 内容管理</p>
        <h1>词汇管理</h1>
        <p>先按分类定位主题，再在列表中维护主题词条。</p>
      </div>
      <div class="vocab-admin__hero-actions"><button type="button" @click="openTheme()">新建主题</button><button type="button" @click="openWord()">添加词条</button></div>
    </header>
    <p v-if="error" class="vocab-admin__error" role="alert">{{ error }}</p>
    <p v-if="notice" class="vocab-admin__notice" role="status">{{ notice }}</p>
    <nav class="vocab-admin__layers" aria-label="按词层筛选主题">
      <button type="button" :class="{ active: !activeLayer }" @click="selectLayer('')">全部分类</button>
      <button v-for="group in groupedThemes" :key="group.name" type="button" :class="{ active: activeLayer === group.name }" @click="selectLayer(group.name)">{{ group.name }}</button>
    </nav>
    <section class="vocab-admin__taxonomy" aria-label="主题分类">
      <header class="vocab-admin__section-heading">
        <div><h2>{{ activeLayer || '全部主题' }}</h2><p>选择主题查看词条；空主题可以删除，含词条主题需先清空内容。</p></div>
        <button v-if="activeTheme" type="button" @click="selectTheme('')">显示全部词条</button>
      </header>
      <div class="vocab-admin__theme-grid">
        <article v-for="theme in visibleThemes" :key="theme.id" class="vocab-admin__theme-card" :class="{ selected: activeTheme === theme.id }">
          <div class="vocab-admin__theme-actions"><button type="button" :aria-label="`编辑${theme.name}`" @click="openTheme(theme)">编辑</button><button v-if="theme.wordCount === 0" type="button" class="danger" :aria-label="`删除${theme.name}`" @click="removeTheme(theme)">删除</button></div>
          <button type="button" class="vocab-admin__theme-select" :aria-pressed="activeTheme === theme.id" @click="selectTheme(theme.id)"><strong>{{ theme.name }}</strong><span>{{ theme.wordCount }} 词</span></button>
        </article>
        <p v-if="!visibleThemes.length" class="vocab-admin__empty">当前分类暂无主题。</p>
      </div>
    </section>
    <section class="vocab-admin__words" aria-label="主题词条">
      <header class="vocab-admin__words-heading"><div><h2>{{ activeTheme ? themes.find((theme) => theme.id === activeTheme)?.name || '主题词条' : '全部词条' }}</h2><p>{{ words.total }} 条当前结果</p></div>
        <form @submit.prevent="page = 1; loadWords()"><input v-model="search" type="search" placeholder="搜索单词或释义" aria-label="搜索单词或释义"><button type="submit">查询</button></form>
      </header>
      <p v-if="loading" class="vocab-admin__empty">正在读取词汇…</p>
      <div v-else class="vocab-admin__word-list">
        <article v-for="word in words.items" :key="word.id"><div><h3>{{ word.word }} <small>{{ word.phoneticUs }}</small></h3><p>{{ word.partOfSpeech }} {{ word.translation }}</p></div><div><button type="button" @click="openWord(word)">编辑</button><button type="button" @click="removeWord(word)">删除</button></div></article>
        <p v-if="!words.items.length" class="vocab-admin__empty">没有符合条件的单词。</p>
      </div>
      <footer v-if="words.total > 20"><button :disabled="page <= 1" @click="page--">上一页</button><span>{{ page }} / {{ Math.ceil(words.total / 20) }}</span><button :disabled="page >= Math.ceil(words.total / 20)" @click="page++">下一页</button></footer>
    </section>
    <div v-if="themeFormOpen" class="vocab-admin__overlay" role="presentation" @click.self="themeFormOpen = false"><form class="vocab-admin__dialog" role="dialog" aria-modal="true" aria-label="编辑词汇主题" @submit.prevent="saveTheme"><h2>{{ themeForm.id ? '编辑主题' : '新建主题' }}</h2><label>词层<input v-model="themeForm.layer" required maxlength="100" placeholder="例如：基础词汇"></label><label>主题名称<input v-model="themeForm.name" required maxlength="200"></label><label>词层顺序<input v-model.number="themeForm.layerOrder" type="number"></label><label>主题顺序<input v-model.number="themeForm.sortOrder" type="number"></label><div><button type="button" @click="themeFormOpen = false">取消</button><button type="submit" :disabled="busy">保存</button></div></form></div>
    <div v-if="wordFormOpen" class="vocab-admin__overlay" role="presentation" @click.self="wordFormOpen = false"><form class="vocab-admin__dialog vocab-admin__dialog--wide" role="dialog" aria-modal="true" aria-label="编辑单词" @submit.prevent="saveWord"><h2>{{ wordForm.id ? '编辑单词' : '新建单词' }}</h2><label>主题<select v-model="wordForm.themeId" required><option v-for="theme in themes" :key="theme.id" :value="theme.id">{{ theme.layer }} › {{ theme.name }}</option></select></label><label>单词<input v-model="wordForm.word" required maxlength="200"></label><label>词性<input v-model="wordForm.partOfSpeech" maxlength="50" placeholder="例如：n."></label><label>美式音标<input v-model="wordForm.phoneticUs" maxlength="100"></label><label>英式音标<input v-model="wordForm.phoneticUk" maxlength="100"></label><label>释义<textarea v-model="wordForm.translation" required rows="3" maxlength="1000"></textarea></label><label>本主题用法<textarea v-model="wordForm.sceneMeaning" rows="2" maxlength="1000" placeholder="该词在本主题语境下的用法（词卡「本主题用法」一行）"></textarea></label><label>词形变化<input v-model="wordForm.inflections" maxlength="1000" placeholder="例如：复数 abilities；过去式 abandoned"></label><label>例句（每行：英文 | 中文）<textarea v-model="wordForm.examples" rows="4" placeholder="This is an example. | 这是一个例句。"></textarea></label><label>排序<input v-model.number="wordForm.sortOrder" type="number"></label><div><button type="button" @click="wordFormOpen = false">取消</button><button type="submit" :disabled="busy">保存</button></div></form></div>
  </main>
</template>

<style scoped>
.vocab-admin{max-width:1450px;margin:auto;padding:25px 0 70px}
.vocab-admin__breadcrumb{display:flex;gap:9px;margin-bottom:20px;color:var(--text-muted);font-size:13px}.vocab-admin__breadcrumb a{color:var(--primary)}
.vocab-admin__hero,.vocab-admin__section-heading,.vocab-admin__words-heading{display:flex;justify-content:space-between;align-items:end;gap:20px}
.vocab-admin__hero{margin-bottom:24px}.vocab-admin__hero h1{margin:7px 0;font-size:clamp(30px,4vw,38px)}.vocab-admin__hero p:last-child,.vocab-admin__section-heading p,.vocab-admin__words-heading p{margin:6px 0 0;color:var(--text-muted);font-size:13px}
.vocab-admin__hero-actions{display:flex;gap:9px;flex-shrink:0}.vocab-admin button{padding:8px 13px;border:1px solid var(--border-strong);border-radius:9px;color:var(--text-primary);background:var(--bg-surface);font:inherit;cursor:pointer}.vocab-admin button:disabled{opacity:.45;cursor:default}.vocab-admin__hero-actions button:last-child,.vocab-admin__dialog>div:last-child button:last-child{border-color:var(--primary);color:var(--on-primary);background:var(--primary)}
.vocab-admin__error,.vocab-admin__notice{padding:12px;border-radius:9px}.vocab-admin__error{color:#a13b2b;background:#fff0e8}.vocab-admin__notice{color:var(--primary);background:var(--primary-soft)}
.vocab-admin__layers{display:flex;gap:8px;overflow-x:auto;padding:2px 2px 12px;margin-bottom:22px}.vocab-admin__layers button{flex-shrink:0;border-radius:999px;padding:9px 16px}.vocab-admin__layers button.active{border-color:var(--primary);color:var(--on-primary);background:var(--primary)}
.vocab-admin__taxonomy{margin-bottom:35px}.vocab-admin__section-heading{margin-bottom:15px}.vocab-admin__section-heading h2,.vocab-admin__words-heading h2{margin:0;font-size:20px}.vocab-admin__section-heading>button{color:var(--primary)}
.vocab-admin__theme-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:12px}.vocab-admin__theme-card{position:relative;min-height:145px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);box-shadow:0 2px 6px rgb(0 0 0/.025);transition:border-color .16s,transform .16s,box-shadow .16s}.vocab-admin__theme-card:hover,.vocab-admin__theme-card.selected{border-color:var(--primary);box-shadow:0 7px 22px rgb(0 0 0/.06);transform:translateY(-1px)}.vocab-admin__theme-card.selected{outline:1px solid color-mix(in srgb,var(--primary) 34%,transparent)}.vocab-admin__theme-actions{position:absolute;z-index:1;top:10px;right:10px;display:flex;gap:4px}.vocab-admin__theme-actions button{padding:3px 5px;border:0;background:transparent;color:var(--primary);font-size:12px}.vocab-admin__theme-actions button.danger{color:var(--accent)}.vocab-admin__theme-select{display:flex;width:100%;height:100%;min-height:145px;flex-direction:column;align-items:start;justify-content:space-between;padding:40px 18px 17px!important;border:0!important;border-radius:16px!important;background:transparent!important;text-align:left}.vocab-admin__theme-select strong{font-size:16px;line-height:1.5}.vocab-admin__theme-select span{color:var(--text-muted);font-size:13px}
.vocab-admin__words{padding:20px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}.vocab-admin__words-heading{margin-bottom:15px}.vocab-admin__words-heading form{display:flex;width:min(100%,520px);gap:8px}.vocab-admin__words-heading input{flex:1;min-width:0;padding:9px 12px;border:1px solid var(--border-strong);border-radius:9px;color:var(--text-primary);background:var(--bg-page);font:inherit}.vocab-admin__word-list article{display:flex;justify-content:space-between;align-items:center;gap:15px;padding:14px 10px;border-bottom:1px solid var(--border)}.vocab-admin__word-list article:last-child{border-bottom:0}.vocab-admin__word-list h3{margin:0;font-size:17px}.vocab-admin__word-list h3 small{color:var(--text-muted);font-size:12px;font-weight:400}.vocab-admin__word-list p{margin:4px 0 0;color:var(--text-secondary);font-size:13px}.vocab-admin__word-list article>div:last-child{display:flex;gap:6px}.vocab-admin__words footer{display:flex;justify-content:center;align-items:center;gap:15px;margin-top:20px}.vocab-admin__empty{grid-column:1/-1;padding:30px;color:var(--text-muted);text-align:center}
.vocab-admin__overlay{position:fixed;z-index:90;inset:0;display:grid;place-items:center;padding:20px;background:#0008}.vocab-admin__dialog{display:grid;gap:13px;width:min(100%,430px);max-height:90vh;overflow:auto;padding:25px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}.vocab-admin__dialog--wide{width:min(100%,620px)}.vocab-admin__dialog h2{margin:0 0 5px}.vocab-admin__dialog label{display:grid;gap:6px;font-weight:650}.vocab-admin__dialog input,.vocab-admin__dialog select,.vocab-admin__dialog textarea{width:100%;padding:9px 11px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-page);color:var(--text-primary);font:inherit}.vocab-admin__dialog>div:last-child{display:flex;justify-content:end;gap:8px;margin-top:8px}
@media(max-width:700px){.vocab-admin__hero,.vocab-admin__section-heading,.vocab-admin__words-heading{align-items:start;flex-direction:column}.vocab-admin__hero-actions,.vocab-admin__words-heading form{width:100%}.vocab-admin__theme-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.vocab-admin__word-list article{align-items:start;flex-direction:column}}
@media(max-width:460px){.vocab-admin__theme-grid{grid-template-columns:1fr}}
@media(prefers-reduced-motion:reduce){.vocab-admin__theme-card{transition:none}.vocab-admin__theme-card:hover{transform:none}}
</style>
