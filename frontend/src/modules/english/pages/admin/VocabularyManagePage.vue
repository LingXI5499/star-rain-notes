<script setup>
/**
 * VocabularyManagePage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：../../../../shared/composables/paginationScroll、vue、vue-router、../../../../shared/http、../../../../shared/viewMode、../../api/englishApi。
 * 主要响应式状态：themes、words、activeTheme、activeLayer、page、search、loading、wordLoading。
 * 阅读入口：loadThemes（加载Themes）；loadWords（加载Words）；load（加载当前操作）；selectLayer（查询Layer的处理函数）；selectTheme（查询主题的处理函数）。
 */


import { usePaginationScroll } from '../../../../shared/composables/paginationScroll'
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import {
  createVocabularyTheme, createVocabularyWord, deleteVocabularyTheme, deleteVocabularyWord,
  getVocabularyThemes, getVocabularyWords, updateVocabularyTheme, updateVocabularyWord,
} from '../../api/englishApi'

/**
 * themes；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const themes = ref([])
/**
 * words；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const words = ref({ items: [], total: 0 })
/**
 * 有效主题；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const activeTheme = ref('')
/**
 * 有效Layer；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const activeLayer = ref('')
/**
 * 分页页码；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const page = ref(1)
/**
 * 搜索；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const search = ref('')
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * 单词Loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const wordLoading = ref(false)
let wordRequest = 0
/**
 * busy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busy = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notice = ref('')
/**
 * 主题FormOpen；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const themeFormOpen = ref(false)
/**
 * 单词FormOpen；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const wordFormOpen = ref(false)
/**
 * 主题Form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const themeForm = reactive({ id: '', layer: '', layerOrder: 0, name: '', sortOrder: 0 })
/**
 * 单词Form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const wordForm = reactive({ id: '', themeId: '', word: '', partOfSpeech: '', phoneticUs: '', phoneticUk: '', translation: '', sceneMeaning: '', inflections: '', examples: '', sortOrder: 0 })
/**
 * groupedThemes；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const groupedThemes = computed(() => {
  const map = new Map()
  for (const theme of themes.value) {
    if (!map.has(theme.layer)) map.set(theme.layer, [])
    map.get(theme.layer).push(theme)
  }
  return [...map].map(([name, items]) => ({ name, items }))
})

/**
 * 可见Themes；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const visibleThemes = computed(() => activeLayer.value
  ? groupedThemes.value.find((group) => group.name === activeLayer.value)?.items || []
  : groupedThemes.value.flatMap((group) => group.items))

/**
 * 加载Themes。
 * 主要协作：getVocabularyThemes。
 * 更新的响应式状态包括 themes；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadThemes() { themes.value = await getVocabularyThemes() }
/**
 * 加载Words。
 * 主要协作：getVocabularyWords。
 * 更新的响应式状态包括 wordLoading、words；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadWords() {
  const request = ++wordRequest
  wordLoading.value = true
  try {
    const result = await getVocabularyWords({ themeId: activeTheme.value || undefined, search: search.value || undefined, page: page.value, size: 20 })
    if (request === wordRequest) words.value = result
  } finally { if (request === wordRequest) wordLoading.value = false }
}
/**
 * 加载当前操作。
 * 主要协作：Promise.all、loadThemes、loadWords、errorMessage。
 * 更新的响应式状态包括 loading、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  error.value = ''
  try { await Promise.all([loadThemes(), loadWords()]) }
  catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

/**
 * 查询Layer的处理函数。
 * 主要协作：selectTheme。
 * 更新的响应式状态包括 activeLayer；渲染由 Vue 的依赖跟踪触发。
 * @param name 名称；默认值、解构字段与可选参数见函数签名。
 */
function selectLayer(name) { activeLayer.value = name; selectTheme('') }
/**
 * 查询主题的处理函数。
 * 主要协作：.catch、loadWords、errorMessage。
 * 更新的响应式状态包括 activeTheme、page、error；渲染由 Vue 的依赖跟踪触发。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 */
function selectTheme(id) { activeTheme.value = id; page.value = 1; loadWords().catch((cause) => { error.value = errorMessage(cause) }) }
/**
 * 打开主题。
 * 主要协作：Object.assign。
 * 更新的响应式状态包括 themeFormOpen；渲染由 Vue 的依赖跟踪触发。
 * @param theme = null 主题空值；默认值、解构字段与可选参数见函数签名。
 */
function openTheme(theme = null) { Object.assign(themeForm, theme || { id: '', layer: activeLayer.value || groupedThemes.value[0]?.name || '', layerOrder: 0, name: '', sortOrder: 0 }); themeFormOpen.value = true }
/**
 * 打开单词。
 * 主要协作：Object.assign、JSON.parse、.join、parsed.map。
 * 更新的响应式状态包括 wordFormOpen；渲染由 Vue 的依赖跟踪触发。
 * @param word = null 单词空值；默认值、解构字段与可选参数见函数签名。
 */
function openWord(word = null) {
  Object.assign(wordForm, word || { id: '', themeId: activeTheme.value || themes.value[0]?.id || '', word: '', partOfSpeech: '', phoneticUs: '', phoneticUk: '', translation: '', sceneMeaning: '', inflections: '', examples: '[]', sortOrder: 0 })
  let parsed = []
  try { parsed = JSON.parse(wordForm.examples || '[]') } catch { /* keep empty editor */ }
  wordForm.examples = parsed.map((item) => typeof item === 'string' ? item : `${item.sentence || item.text || ''} | ${item.translation || ''}`).join('\n')
  wordFormOpen.value = true
}

/**
 * 保存主题。
 * 主要协作：updateVocabularyTheme、createVocabularyTheme、loadThemes、errorMessage。
 * 更新的响应式状态包括 busy、error、notice、themeFormOpen；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 保存单词。
 * 主要协作：.map、.filter、wordForm.examples.split、line.trim、line.split。
 * 更新的响应式状态包括 busy、error、notice、wordFormOpen；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 移除主题。
 * 主要协作：window.confirm、deleteVocabularyTheme、load、errorMessage。
 * 更新的响应式状态包括 activeTheme、notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param theme 主题；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeTheme(theme) {
  if (!window.confirm(`删除空主题「${theme.name}」？`)) return
  try { await deleteVocabularyTheme(theme.id); if (activeTheme.value === theme.id) activeTheme.value = ''; await load(); notice.value = '主题已删除。' }
  catch (cause) { error.value = errorMessage(cause) }
}
/**
 * 移除单词。
 * 主要协作：window.confirm、deleteVocabularyWord、load、errorMessage。
 * 更新的响应式状态包括 notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param word 单词；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeWord(word) {
  if (!window.confirm(`删除单词「${word.word}」？`)) return
  try { await deleteVocabularyWord(word.id); await load(); notice.value = '单词已删除。' }
  catch (cause) { error.value = errorMessage(cause) }
}

/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(page, () => loadWords().catch((cause) => { error.value = errorMessage(cause) }))
load()
usePaginationScroll(() => page.value, () => wordLoading.value)
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
        <article v-for="word in words.items" :key="word.id" class="content-card" tabindex="0" :aria-label="`编辑单词 ${word.word}`" @click="openWord(word)" @keydown.enter.self="openWord(word)"><div><h3>{{ word.word }} <small>{{ word.phoneticUs }}</small></h3><p>{{ word.partOfSpeech }} {{ word.translation }}</p></div><div class="vocab-admin__word-actions" @click.stop><button type="button" @click="openWord(word)">编辑</button><button type="button" @click="removeWord(word)">删除</button></div></article>
        <p v-if="!words.items.length" class="vocab-admin__empty">没有符合条件的单词。</p>
      </div>
      <footer v-if="words.total > 20"><button :disabled="wordLoading || page <= 1" @click="page--">上一页</button><span>{{ page }} / {{ Math.ceil(words.total / 20) }}</span><button :disabled="wordLoading || page >= Math.ceil(words.total / 20)" @click="page++">下一页</button></footer>
    </section>
    <div v-if="themeFormOpen" class="vocab-admin__overlay" role="presentation" @click.self="themeFormOpen = false"><form class="vocab-admin__dialog" role="dialog" aria-modal="true" aria-label="编辑词汇主题" @submit.prevent="saveTheme"><h2>{{ themeForm.id ? '编辑主题' : '新建主题' }}</h2><label>词层<input v-model="themeForm.layer" required maxlength="100" placeholder="例如：基础词汇"></label><label>主题名称<input v-model="themeForm.name" required maxlength="200"></label><label>词层顺序<input v-model.number="themeForm.layerOrder" type="number"></label><label>主题顺序<input v-model.number="themeForm.sortOrder" type="number"></label><div><button type="button" @click="themeFormOpen = false">取消</button><button type="submit" :disabled="busy">保存</button></div></form></div>
    <div v-if="wordFormOpen" class="vocab-admin__overlay" role="presentation" @click.self="wordFormOpen = false"><form class="vocab-admin__dialog vocab-admin__dialog--wide" role="dialog" aria-modal="true" aria-label="编辑单词" @submit.prevent="saveWord"><h2>{{ wordForm.id ? '编辑单词' : '新建单词' }}</h2><label>主题<select v-model="wordForm.themeId" required><option v-for="theme in themes" :key="theme.id" :value="theme.id">{{ theme.layer }} › {{ theme.name }}</option></select></label><label>单词<input v-model="wordForm.word" required maxlength="200"></label><label>词性<input v-model="wordForm.partOfSpeech" maxlength="50" placeholder="例如：n."></label><label>美式音标<input v-model="wordForm.phoneticUs" maxlength="100"></label><label>英式音标<input v-model="wordForm.phoneticUk" maxlength="100"></label><label>释义<textarea v-model="wordForm.translation" required rows="3" maxlength="1000"></textarea></label><label>本主题用法<textarea v-model="wordForm.sceneMeaning" rows="2" maxlength="1000" placeholder="该词在本主题语境下的用法（词卡「本主题用法」一行）"></textarea></label><label>词形变化<input v-model="wordForm.inflections" maxlength="1000" placeholder="例如：复数 abilities；过去式 abandoned"></label><label>例句（每行：英文 | 中文）<textarea v-model="wordForm.examples" rows="4" placeholder="This is an example. | 这是一个例句。"></textarea></label><label>排序<input v-model.number="wordForm.sortOrder" type="number"></label><div><button type="button" @click="wordFormOpen = false">取消</button><button type="submit" :disabled="busy">保存</button></div></form></div>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.vocab-admin。
 */
.vocab-admin{max-width:1450px;margin:auto;padding:25px 0 70px}
/**
 * 样式范围：.vocab-admin__breadcrumb。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocab-admin__breadcrumb{display:flex;gap:9px;margin-bottom:20px;color:var(--text-muted);font-size:13px}
  /**
   * 样式范围：.vocab-admin__breadcrumb a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__breadcrumb a{color:var(--primary)}
/**
 * 样式范围：.vocab-admin__hero,.vocab-admin__section-heading,.vocab-admin__words-heading。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.vocab-admin__hero,.vocab-admin__section-heading,.vocab-admin__words-heading{display:flex;justify-content:space-between;align-items:end;gap:20px}
/**
 * 样式范围：.vocab-admin__hero。
 */
.vocab-admin__hero{margin-bottom:24px}
  /**
   * 样式范围：.vocab-admin__hero h1。
   */
  .vocab-admin__hero h1{margin:7px 0;font-size:clamp(30px,4vw,38px)}
  /**
   * 样式范围：.vocab-admin__hero p:last-child,.vocab-admin__section-heading p,.vocab-admin__words-heading p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__hero p:last-child,.vocab-admin__section-heading p,.vocab-admin__words-heading p{margin:6px 0 0;color:var(--text-muted);font-size:13px}
/**
 * 样式范围：.vocab-admin__hero-actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.vocab-admin__hero-actions{display:flex;gap:9px;flex-shrink:0}
  /**
   * 样式范围：.vocab-admin button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin button{padding:8px 13px;border:1px solid var(--border-strong);border-radius:9px;color:var(--text-primary);background:var(--bg-surface);font:inherit;cursor:pointer}
  /**
   * 样式范围：.vocab-admin button:disabled。
   */
  .vocab-admin button:disabled{opacity:.45;cursor:default}
  /**
   * 样式范围：.vocab-admin__hero-actions button:last-child,.vocab-admin__dialog>div:last-child button:last-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__hero-actions button:last-child,.vocab-admin__dialog>div:last-child button:last-child{border-color:var(--primary);color:var(--on-primary);background:var(--primary)}
/**
 * 样式范围：.vocab-admin__error,.vocab-admin__notice。
 */
.vocab-admin__error,.vocab-admin__notice{padding:12px;border-radius:9px}
  /**
   * 样式范围：.vocab-admin__error。
   */
  .vocab-admin__error{color:#a13b2b;background:#fff0e8}
  /**
   * 样式范围：.vocab-admin__notice。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__notice{color:var(--primary);background:var(--primary-soft)}
/**
 * 样式范围：.vocab-admin__layers。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 溢出处理：横向 auto。
 */
.vocab-admin__layers{display:flex;gap:8px;overflow-x:auto;padding:2px 2px 12px;margin-bottom:22px}
  /**
   * 样式范围：.vocab-admin__layers button。
   */
  .vocab-admin__layers button{flex-shrink:0;border-radius:999px;padding:9px 16px}
  /**
   * 样式范围：.vocab-admin__layers button.active。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__layers button.active{border-color:var(--primary);color:var(--on-primary);background:var(--primary)}
/**
 * 样式范围：.vocab-admin__taxonomy。
 */
.vocab-admin__taxonomy{margin-bottom:35px}
  /**
   * 样式范围：.vocab-admin__section-heading。
   */
  .vocab-admin__section-heading{margin-bottom:15px}
  /**
   * 样式范围：.vocab-admin__section-heading h2,.vocab-admin__words-heading h2。
   */
  .vocab-admin__section-heading h2,.vocab-admin__words-heading h2{margin:0;font-size:20px}
  /**
   * 样式范围：.vocab-admin__section-heading>button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__section-heading>button{color:var(--primary)}
/**
 * 样式范围：.vocab-admin__theme-grid。
 * 使用网格布局组织内容；列定义为 repeat(auto-fill,minmax(210px,1fr))，间距由 12px 控制。
 */
.vocab-admin__theme-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:12px}
  /**
   * 样式范围：.vocab-admin__theme-card。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__theme-card{position:relative;min-height:145px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface);box-shadow:0 2px 6px rgb(0 0 0/.025);transition:border-color .16s,transform .16s,box-shadow .16s}
  /**
   * 样式范围：.vocab-admin__theme-card:hover,.vocab-admin__theme-card.selected。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__theme-card:hover,.vocab-admin__theme-card.selected{border-color:var(--primary);box-shadow:0 7px 22px rgb(0 0 0/.06);transform:translateY(-1px)}
  /**
   * 样式范围：.vocab-admin__theme-card.selected。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__theme-card.selected{outline:1px solid color-mix(in srgb,var(--primary) 34%,transparent)}
  /**
   * 样式范围：.vocab-admin__theme-actions。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .vocab-admin__theme-actions{position:absolute;z-index:1;top:10px;right:10px;display:flex;gap:4px}
  /**
   * 样式范围：.vocab-admin__theme-actions button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__theme-actions button{padding:3px 5px;border:0;background:transparent;color:var(--primary);font-size:12px}
  /**
   * 样式范围：.vocab-admin__theme-actions button.danger。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__theme-actions button.danger{color:var(--accent)}
  /**
   * 样式范围：.vocab-admin__theme-select。
   * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
   */
  .vocab-admin__theme-select{display:flex;width:100%;height:100%;min-height:145px;flex-direction:column;align-items:start;justify-content:space-between;padding:40px 18px 17px!important;border:0!important;border-radius:16px!important;background:transparent!important;text-align:left}
  /**
   * 样式范围：.vocab-admin__theme-select strong。
   */
  .vocab-admin__theme-select strong{font-size:16px;line-height:1.5}
  /**
   * 样式范围：.vocab-admin__theme-select span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__theme-select span{color:var(--text-muted);font-size:13px}
/**
 * 样式范围：.vocab-admin__words。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocab-admin__words{padding:20px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}
  /**
   * 样式范围：.vocab-admin__words-heading。
   */
  .vocab-admin__words-heading{margin-bottom:15px}
  /**
   * 样式范围：.vocab-admin__words-heading form。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .vocab-admin__words-heading form{display:flex;width:min(100%,520px);gap:8px}
  /**
   * 样式范围：.vocab-admin__words-heading input。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__words-heading input{flex:1;min-width:0;padding:9px 12px;border:1px solid var(--border-strong);border-radius:9px;color:var(--text-primary);background:var(--bg-page);font:inherit}
  /**
   * 样式范围：.vocab-admin__word-list article。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__word-list article{display:flex;justify-content:space-between;align-items:center;gap:15px;padding:14px 10px;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.vocab-admin__word-list article:last-child。
   */
  .vocab-admin__word-list article:last-child{border-bottom:0}
  /**
   * 样式范围：.vocab-admin__word-list h3。
   */
  .vocab-admin__word-list h3{margin:0;font-size:17px}
  /**
   * 样式范围：.vocab-admin__word-list h3 small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__word-list h3 small{color:var(--text-muted);font-size:12px;font-weight:400}
  /**
   * 样式范围：.vocab-admin__word-list p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__word-list p{margin:4px 0 0;color:var(--text-secondary);font-size:13px}
  /**
   * 样式范围：.vocab-admin__word-list article>div:last-child。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .vocab-admin__word-list article>div:last-child{display:flex;gap:6px}
  /**
   * 样式范围：.vocab-admin__words footer。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .vocab-admin__words footer{display:flex;justify-content:center;align-items:center;gap:15px;margin-top:20px}
  /**
   * 样式范围：.vocab-admin__empty。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__empty{grid-column:1/-1;padding:30px;color:var(--text-muted);text-align:center}
/**
 * 样式范围：.vocab-admin__overlay。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 各行列间距规则 控制。
 * 相对视口定位，用于浮层或固定操作区域；层叠顺序由 z-index 决定。
 */
.vocab-admin__overlay{position:fixed;z-index:90;inset:0;display:grid;place-items:center;padding:20px;background:#0008}
  /**
   * 样式范围：.vocab-admin__dialog。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 13px 控制。
   * 溢出处理：整体 auto。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__dialog{display:grid;gap:13px;width:min(100%,430px);max-height:90vh;overflow:auto;padding:25px;border:1px solid var(--border);border-radius:17px;background:var(--bg-surface)}
  /**
   * 样式范围：.vocab-admin__dialog--wide。
   */
  .vocab-admin__dialog--wide{width:min(100%,620px)}
  /**
   * 样式范围：.vocab-admin__dialog h2。
   */
  .vocab-admin__dialog h2{margin:0 0 5px}
  /**
   * 样式范围：.vocab-admin__dialog label。
   * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
   */
  .vocab-admin__dialog label{display:grid;gap:6px;font-weight:650}
  /**
   * 样式范围：.vocab-admin__dialog input,.vocab-admin__dialog select,.vocab-admin__dialog textarea。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocab-admin__dialog input,.vocab-admin__dialog select,.vocab-admin__dialog textarea{width:100%;padding:9px 11px;border:1px solid var(--border-strong);border-radius:8px;background:var(--bg-page);color:var(--text-primary);font:inherit}
  /**
   * 样式范围：.vocab-admin__dialog>div:last-child。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .vocab-admin__dialog>div:last-child{display:flex;justify-content:end;gap:8px;margin-top:8px}
/**
 * 响应式或设备条件：(max-width:700px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:700px){
  /**
   * 样式范围：.vocab-admin__hero,.vocab-admin__section-heading,.vocab-admin__words-heading。
   */
  .vocab-admin__hero,.vocab-admin__section-heading,.vocab-admin__words-heading{align-items:start;flex-direction:column}
  /**
   * 样式范围：.vocab-admin__hero-actions,.vocab-admin__words-heading form。
   */
  .vocab-admin__hero-actions,.vocab-admin__words-heading form{width:100%}
  /**
   * 样式范围：.vocab-admin__theme-grid。
   */
  .vocab-admin__theme-grid{grid-template-columns:repeat(2,minmax(0,1fr))}
  /**
   * 样式范围：.vocab-admin__word-list article。
   */
  .vocab-admin__word-list article{align-items:start;flex-direction:column}}
/**
 * 响应式或设备条件：(max-width:460px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:460px){
  /**
   * 样式范围：.vocab-admin__theme-grid。
   */
  .vocab-admin__theme-grid{grid-template-columns:1fr}}
/**
 * 响应式或设备条件：(prefers-reduced-motion:reduce)；仅在该条件成立时覆盖基础样式。
 */
@media(prefers-reduced-motion:reduce){
  /**
   * 样式范围：.vocab-admin__theme-card。
   */
  .vocab-admin__theme-card{transition:none}
  /**
   * 样式范围：.vocab-admin__theme-card:hover。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   */
  .vocab-admin__theme-card:hover{transform:none}}
</style>
