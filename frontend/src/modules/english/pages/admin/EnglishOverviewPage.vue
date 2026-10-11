<script setup>
/**
 * EnglishOverviewPage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../../shared/editor/MarkdownEditor.vue、../../../../shared/http、../../../../shared/viewMode、../../api/englishApi。
 * 主要响应式状态：form、editor、loading、saving、error、notice。
 * 阅读入口：load（加载当前操作）；save（保存当前操作）。
 */


import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import { getAdminEnglishOverview, updateEnglishOverview } from '../../api/englishApi'

/**
 * form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const form = reactive({ title: '', subtitle: '', introduction: '', roadmapMarkdown: '' })
/**
 * editor；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const editor = ref(null)
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * saving；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const saving = ref(false)
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
 * 加载当前操作。
 * 主要协作：getAdminEnglishOverview、errorMessage。
 * 更新的响应式状态包括 loading、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await getAdminEnglishOverview()
    for (const key of Object.keys(form)) form[key] = data[key] || ''
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

/**
 * 保存当前操作。
 * 主要协作：form.title.trim、updateEnglishOverview、editor.value.getMarkdown、errorMessage。
 * 更新的响应式状态包括 error、saving、notice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function save() {
  if (saving.value) return
  if (!form.title.trim()) { error.value = '请填写标题。'; return }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const data = await updateEnglishOverview({
      ...form,
      title: form.title.trim(),
      roadmapMarkdown: editor.value?.getMarkdown?.() ?? form.roadmapMarkdown,
    })
    for (const key of Object.keys(form)) form[key] = data[key] || ''
    notice.value = '英语总览已保存。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { saving.value = false }
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
</script>

<template>
  <main class="english-overview-admin">
    <header class="english-overview-admin__head">
      <div><p class="public-eyebrow">ENGLISH · 内容管理</p><h1>英语总览</h1><p>维护英语首页的介绍和学习路线。</p></div>
      <RouterLink :to="accountPath('/english/manage')">返回英语工作台</RouterLink>
    </header>
    <p v-if="error" class="english-overview-admin__error" role="alert">{{ error }}</p>
    <p v-if="notice" class="english-overview-admin__notice" role="status">{{ notice }}</p>
    <p v-if="loading">正在读取英语总览…</p>
    <form v-else class="english-overview-admin__form" @submit.prevent="save">
      <label>标题<input v-model="form.title" maxlength="200" required></label>
      <label>副标题<input v-model="form.subtitle" maxlength="500"></label>
      <label>介绍<textarea v-model="form.introduction" rows="5" maxlength="10000"></textarea></label>
      <div class="english-overview-admin__editor"><span>长期学习路线</span><MarkdownEditor ref="editor" v-model="form.roadmapMarkdown" /></div>
      <button type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存总览' }}</button>
    </form>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.english-overview-admin。
 */
.english-overview-admin{max-width:1100px;margin:0 auto;padding:28px 0 64px}
/**
 * 样式范围：.english-overview-admin__head。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.english-overview-admin__head{display:flex;align-items:end;justify-content:space-between;gap:20px;margin-bottom:28px}
/**
 * 样式范围：.english-overview-admin__head h1。
 */
.english-overview-admin__head h1{margin:8px 0;font-size:34px}
  /**
   * 样式范围：.english-overview-admin__head p:last-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-overview-admin__head p:last-child{color:var(--text-secondary)}
/**
 * 样式范围：.english-overview-admin__head a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-overview-admin__head a{padding:10px 14px;border:1px solid var(--border);border-radius:10px;color:var(--primary)}
/**
 * 样式范围：.english-overview-admin__form。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 22px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.english-overview-admin__form{display:grid;gap:22px;padding:28px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface)}
/**
 * 样式范围：.english-overview-admin__form label。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
 */
.english-overview-admin__form label{display:grid;gap:8px;font-weight:650}
  /**
   * 样式范围：.english-overview-admin__form input,.english-overview-admin__form textarea。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-overview-admin__form input,.english-overview-admin__form textarea{width:100%;padding:12px 14px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-page);color:var(--text-primary);font:inherit}
/**
 * 样式范围：.english-overview-admin__editor。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 8px 控制。
 */
.english-overview-admin__editor{display:grid;gap:8px;font-weight:650}
  /**
   * 样式范围：.english-overview-admin__form button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-overview-admin__form button{justify-self:start;padding:11px 22px;border:0;border-radius:10px;background:var(--primary);color:var(--on-primary);font:inherit;cursor:pointer}
/**
 * 样式范围：.english-overview-admin__form button:disabled。
 */
.english-overview-admin__form button:disabled{opacity:.5;cursor:wait}
  /**
   * 样式范围：.english-overview-admin__error,.english-overview-admin__notice。
   */
  .english-overview-admin__error,.english-overview-admin__notice{padding:12px 16px;border-radius:10px}
  /**
   * 样式范围：.english-overview-admin__error。
   */
  .english-overview-admin__error{color:#a13b2b;background:#fff0e8}
  /**
   * 样式范围：.english-overview-admin__notice。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .english-overview-admin__notice{color:var(--primary);background:var(--primary-soft)}
/**
 * 响应式或设备条件：(max-width:700px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:700px){
  /**
   * 样式范围：.english-overview-admin__head。
   */
  .english-overview-admin__head{align-items:start;flex-direction:column}
  /**
   * 样式范围：.english-overview-admin__form。
   */
  .english-overview-admin__form{padding:18px}}
</style>
