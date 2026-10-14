<script setup>
/**
 * ProfileEditorPage.vue：作者资料模块的页面组件。
 * 职责范围：个人介绍、社交链接、经历技能和精选内容。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../../shared/editor/MarkdownEditor.vue、../../../tutorial/api/tutorialApi、../../../blog/api/blogApi、../../../portfolio/api/portfolioApi。
 * 主要响应式状态：profile、loading、busy、error、notice、editorKey、basic、skill。
 * 阅读入口：fetchAll（fetch全部的处理函数）；load（加载当前操作）；run（运行当前操作）；resetSkill（重置Skill）；editSkill（editSkill的处理函数）。
 */


import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import { useEditorContexts } from '../../../../shared/editor/recovery/useEditorContexts'
const editorContext = useEditorContexts('profile')

import { listPublicTutorials } from '../../../tutorial/api/tutorialApi'
import { listPublicPosts } from '../../../blog/api/blogApi'
import { listPublicWorks } from '../../../portfolio/api/portfolioApi'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import {
  addFeatured, getAdminProfile, orderFeatured,
  orderSkills, orderSocial, removeFeatured, removeSkill, removeSocial,
  saveSkill, saveSocial, updateProfile,
} from '../../api/profileApi'

/**
 * 资料；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const profile = ref(null)
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
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
 * editor键；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const editorKey = ref(0)
/**
 * basic；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const basic = reactive({ displayName: '', headline: '', bioMarkdown: '', locationText: '' })
/**
 * skill；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const skill = reactive({ id: '', category: 'SKILL', name: '', description: '', proficiency: '' })
/**
 * social；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const social = reactive({ id: '', platformCode: 'GITHUB', label: '', url: '' })
/**
 * 功能；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const feature = reactive({ contentType: 'TUTORIAL', contentId: '', titleOverride: '' })
/**
 * candidates；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const candidates = reactive({ TUTORIAL: [], BLOG: [], PORTFOLIO: [] })
/**
 * availableCandidates；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const availableCandidates = computed(() => candidates[feature.contentType] || [])

/**
 * fetch全部的处理函数。
 * 主要协作：api、items.push、items.map。
 * @param api 接口；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function fetchAll(api) {
  const first = await api({ page: 1, pageSize: 100 })
  const items = [...(first.items || [])]
  for (let page = 2; page <= Math.ceil((first.total || 0) / 100); page += 1) {
    const next = await api({ page, pageSize: 100 })
    items.push(...(next.items || []))
  }
  return items.map((item) => ({ id: String(item.id), title: item.title }))
}

/**
 * 加载当前操作。
 * 主要协作：getAdminProfile、Promise.all、fetchAll、errorMessage。
 * 更新的响应式状态包括 loading、profile、editorKey、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true
  try {
    profile.value = await getAdminProfile()
    basic.displayName = profile.value.displayName || ''
    basic.headline = profile.value.headline || ''
    basic.bioMarkdown = profile.value.bioMarkdown || ''
    basic.locationText = profile.value.locationText || ''
    editorKey.value += 1
    const [tutorials, blogs, works] = await Promise.all([
      fetchAll(listPublicTutorials), fetchAll(listPublicPosts), fetchAll(listPublicWorks),
    ])
    candidates.TUTORIAL = tutorials
    candidates.BLOG = blogs
    candidates.PORTFOLIO = works
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

/**
 * 运行当前操作。
 * 主要协作：action、errorMessage。
 * 更新的响应式状态包括 busy、error、notice、profile；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @param success = '已保存' success；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function run(action, success = '已保存') {
  if (busy.value) return false
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    profile.value = await action()
    notice.value = success
    return true
  } catch (cause) {
    error.value = errorMessage(cause)
    return false
  } finally {
    busy.value = false
  }
}

/**
 * 重置Skill。
 * 主要协作：Object.assign。
 */
function resetSkill() { Object.assign(skill, { id: '', category: 'SKILL', name: '', description: '', proficiency: '' }) }
/**
 * editSkill的处理函数。
 * 主要协作：Object.assign、.scrollIntoView、document.getElementById。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 */
function editSkill(item) { Object.assign(skill, item); document.getElementById('profile-skill-form')?.scrollIntoView({ behavior: 'smooth' }) }
/**
 * 提交Skill。
 * 主要协作：run、saveSkill、resetSkill。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function submitSkill() { const payload = { ...skill }; delete payload.id; if (await run(() => saveSkill(skill.id, payload))) resetSkill() }
/**
 * 删除Skill。
 * 主要协作：window.confirm、run、removeSkill。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function deleteSkill(id) { if (window.confirm('删除这个条目？')) await run(() => removeSkill(id), '条目已删除') }

/**
 * 重置Social。
 * 主要协作：Object.assign。
 */
function resetSocial() { Object.assign(social, { id: '', platformCode: 'GITHUB', label: '', url: '' }) }
/**
 * editSocial的处理函数。
 * 主要协作：Object.assign、.scrollIntoView、document.getElementById。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 */
function editSocial(item) { Object.assign(social, item); document.getElementById('profile-social-form')?.scrollIntoView({ behavior: 'smooth' }) }
/**
 * 提交Social。
 * 主要协作：run、saveSocial、resetSocial。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function submitSocial() { const payload = { ...social }; delete payload.id; if (await run(() => saveSocial(social.id, payload))) resetSocial() }
/**
 * 删除Social。
 * 主要协作：window.confirm、run、removeSocial。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function deleteSocial(id) { if (window.confirm('删除这个链接？')) await run(() => removeSocial(id), '链接已删除') }

/**
 * 提交精选。
 * 主要协作：run、addFeatured。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function submitFeatured() {
  if (!feature.contentId) return
  if (await run(() => addFeatured({ ...feature }), '精选内容已添加')) {
    feature.contentId = ''
    feature.titleOverride = ''
  }
}
/**
 * 删除精选。
 * 主要协作：window.confirm、run、removeFeatured。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function deleteFeatured(id) { if (window.confirm('移除这条精选内容？')) await run(() => removeFeatured(id), '精选已移除') }

/**
 * move的处理函数。
 * 主要协作：items.map、run、save。
 * @param section 区块；默认值、解构字段与可选参数见函数签名。
 * @param index 索引；默认值、解构字段与可选参数见函数签名。
 * @param delta delta；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function move(section, index, delta) {
  const items = profile.value?.[section] || []
  const target = index + delta
  if (target < 0 || target >= items.length) return
  const ids = items.map((item) => item.id)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  const save = { skills: orderSkills, socialLinks: orderSocial, featuredContents: orderFeatured }[section]
  await run(() => save(ids), '顺序已更新')
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
</script>

<template>
  <main class="profile-editor">
    <header class="profile-editor__header"><p>AUTHOR PROFILE · 内容编辑</p><h1>作者资料</h1><span>管理公开介绍、技能、社交链接与精选内容。</span><RouterLink :to="accountPath('/about')">查看公开页 ↗</RouterLink></header>
    <p v-if="loading" class="profile-editor__state">正在加载作者资料…</p>
    <template v-else-if="profile">
      <p v-if="error" class="profile-editor__error" role="alert">{{ error }}</p>
      <p v-if="notice" class="profile-editor__notice" role="status">{{ notice }}</p>

      <section class="profile-editor__panel"><h2>基础介绍</h2>
        <form @submit.prevent="run(() => updateProfile({ ...basic }))">
          <div class="profile-editor__grid"><label>显示名称<input v-model.trim="basic.displayName" required maxlength="100" /></label><label>一句话介绍<input v-model.trim="basic.headline" maxlength="255" /></label><label>所在地<input v-model.trim="basic.locationText" maxlength="120" /></label></div>
          <label>个人介绍</label><MarkdownEditor :editor-context="loading ? null : editorContext(profile?.id || 'site-profile', 'bioMarkdown', profile?.rowVersion, profile?.bioMarkdown || '')" :key="editorKey" v-model="basic.bioMarkdown" /><button type="submit" :disabled="busy">保存基础介绍</button>
        </form>
      </section>

      <section class="profile-editor__panel"><h2>技能、方向与兴趣</h2>
        <ol class="profile-editor__items"><li v-for="(item, index) in profile.skills" :key="item.id"><div><strong>{{ item.name }}</strong><small>{{ item.category }} · {{ item.proficiency || '未标注' }}</small></div><div class="profile-editor__actions"><button type="button" :disabled="index === 0 || busy" @click="move('skills', index, -1)">↑</button><button type="button" :disabled="index === profile.skills.length - 1 || busy" @click="move('skills', index, 1)">↓</button><button type="button" @click="editSkill(item)">编辑</button><button type="button" @click="deleteSkill(item.id)">删除</button></div></li></ol>
        <form id="profile-skill-form" @submit.prevent="submitSkill"><h3>{{ skill.id ? '编辑条目' : '添加条目' }}</h3><div class="profile-editor__grid"><label>类别<select v-model="skill.category"><option value="SKILL">技能</option><option value="DIRECTION">方向</option><option value="INTEREST">兴趣</option></select></label><label>名称<input v-model.trim="skill.name" required maxlength="120" /></label><label>熟悉程度（展示文字）<input v-model.trim="skill.proficiency" maxlength="30" /></label></div><label>说明<input v-model.trim="skill.description" maxlength="500" /></label><button type="submit" :disabled="busy">{{ skill.id ? '保存条目' : '添加条目' }}</button><button v-if="skill.id" type="button" @click="resetSkill">取消编辑</button></form>
      </section>

      <section class="profile-editor__panel"><h2>社交链接</h2>
        <ol class="profile-editor__items"><li v-for="(item, index) in profile.socialLinks" :key="item.id"><div><strong>{{ item.label }}</strong><small>{{ item.platformCode }} · {{ item.url }}</small></div><div class="profile-editor__actions"><button type="button" :disabled="index === 0 || busy" @click="move('socialLinks', index, -1)">↑</button><button type="button" :disabled="index === profile.socialLinks.length - 1 || busy" @click="move('socialLinks', index, 1)">↓</button><button type="button" @click="editSocial(item)">编辑</button><button type="button" @click="deleteSocial(item.id)">删除</button></div></li></ol>
        <form id="profile-social-form" @submit.prevent="submitSocial"><h3>{{ social.id ? '编辑链接' : '添加链接' }}</h3><div class="profile-editor__grid"><label>平台<select v-model="social.platformCode"><option v-for="platform in ['GITHUB','BILIBILI','GITEE','LINKEDIN','EMAIL_PAGE','OTHER']" :key="platform">{{ platform }}</option></select></label><label>显示名称<input v-model.trim="social.label" required maxlength="100" /></label><label>链接地址<input v-model.trim="social.url" type="url" required maxlength="1000" placeholder="https://" /></label></div><button type="submit" :disabled="busy">{{ social.id ? '保存链接' : '添加链接' }}</button><button v-if="social.id" type="button" @click="resetSocial">取消编辑</button></form>
      </section>

      <section class="profile-editor__panel"><h2>精选内容</h2><p class="profile-editor__hint">只能选择已公开内容；撤回后会自动从公开页隐藏，并在此提示。</p>
        <ol class="profile-editor__items"><li v-for="(item, index) in profile.featuredContents" :key="item.id"><div><strong>{{ item.title }}</strong><small>{{ item.contentType }} · {{ item.available ? '公开可用' : '引用对象不可公开' }}</small></div><div class="profile-editor__actions"><button type="button" :disabled="index === 0 || busy" @click="move('featuredContents', index, -1)">↑</button><button type="button" :disabled="index === profile.featuredContents.length - 1 || busy" @click="move('featuredContents', index, 1)">↓</button><button type="button" @click="deleteFeatured(item.id)">移除</button></div></li></ol>
        <form @submit.prevent="submitFeatured"><h3>添加精选</h3><div class="profile-editor__grid"><label>类型<select v-model="feature.contentType" @change="feature.contentId = ''"><option value="TUTORIAL">教程</option><option value="BLOG">博客</option><option value="PORTFOLIO">作品</option></select></label><label>已公开内容<select v-model="feature.contentId" required><option value="">请选择</option><option v-for="item in availableCandidates" :key="item.id" :value="item.id">{{ item.title }}</option></select></label><label>展示标题（选填）<input v-model.trim="feature.titleOverride" maxlength="255" /></label></div><button type="submit" :disabled="busy || !feature.contentId">添加精选</button></form>
      </section>
    </template>
    <p v-else-if="error" class="profile-editor__error" role="alert">{{ error }}</p>
  </main>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.profile-editor。
 */
.profile-editor { max-width: 1180px; margin: auto; padding: 36px 36px 90px; }
/**
 * 样式范围：.profile-editor__header。
 */
.profile-editor__header { margin-bottom: 32px; }
/**
 * 样式范围：.profile-editor__header p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor__header p { color: var(--accent); font-size: 11px; font-weight: 700; letter-spacing: .14em; }
/**
 * 样式范围：.profile-editor__header h1。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor__header h1 { color: var(--text-primary); font-size: 36px; margin: 10px 0; }
/**
 * 样式范围：.profile-editor__header span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor__header span { color: var(--text-secondary); }
/**
 * 样式范围：.profile-editor__header a。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor__header a { display: inline-block; margin-left: 20px; color: var(--primary); }
/**
 * 样式范围：.profile-editor__state。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor__state { padding: 60px; text-align: center; color: var(--text-muted); }
/**
 * 样式范围：.profile-editor__error。
 */
.profile-editor__error { color: #bd4e39; margin-bottom: 16px; }
  /**
   * 样式范围：.profile-editor__notice。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .profile-editor__notice { color: var(--primary); margin-bottom: 16px; }
/**
 * 样式范围：.profile-editor__panel。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor__panel { border: 1px solid var(--border); border-radius: 18px; padding: 27px; background: var(--bg-surface); margin-bottom: 22px; }
/**
 * 样式范围：.profile-editor__panel h2。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor__panel h2 { color: var(--text-primary); font-size: 24px; margin-bottom: 18px; }
/**
 * 样式范围：.profile-editor__panel h3。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor__panel h3 { font-size: 17px; color: var(--text-primary); margin: 22px 0 12px; }
/**
 * 样式范围：.profile-editor__hint。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor__hint { color: var(--text-muted); font-size: 13px; margin: -5px 0 18px; }
/**
 * 样式范围：.profile-editor__grid。
 * 使用网格布局组织内容；列定义为 repeat(auto-fit, minmax(200px, 1fr))，间距由 14px 控制。
 */
.profile-editor__grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; }
/**
 * 样式范围：.profile-editor label。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor label { display: block; color: var(--text-secondary); font-size: 13px; font-weight: 600; margin: 12px 0; }
/**
 * 样式范围：.profile-editor input:not([type=checkbox]), .profile-editor select, .profile-editor textarea。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor input:not([type=checkbox]), .profile-editor select, .profile-editor textarea { display: block; width: 100%; box-sizing: border-box; margin-top: 7px; border: 1px solid var(--border); border-radius: 8px; padding: 10px 12px; background: var(--bg-page); color: var(--text-primary); font: inherit; }
/**
 * 样式范围：.profile-editor textarea。
 */
.profile-editor textarea { resize: vertical; }
  /**
   * 样式范围：.profile-editor input:focus, .profile-editor select:focus, .profile-editor textarea:focus。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .profile-editor input:focus, .profile-editor select:focus, .profile-editor textarea:focus { outline: 2px solid var(--primary); outline-offset: 2px; }
/**
 * 样式范围：.profile-editor button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor button { border: 1px solid var(--border); border-radius: 8px; padding: 9px 14px; margin: 8px 8px 0 0; background: var(--bg-page); color: var(--text-primary); cursor: pointer; }
/**
 * 样式范围：.profile-editor button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor button:hover { color: var(--primary); border-color: var(--primary); }
  /**
   * 样式范围：.profile-editor button:disabled。
   */
  .profile-editor button:disabled { opacity: .45; cursor: default; }
/**
 * 样式范围：.profile-editor__checkbox。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.profile-editor__checkbox { display: flex !important; align-items: center; gap: 8px; align-self: end; }
/**
 * 样式范围：.profile-editor__items。
 */
.profile-editor__items { list-style: none; padding: 0; margin: 0; }
  /**
   * 样式范围：.profile-editor__items li。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .profile-editor__items li { display: flex; justify-content: space-between; align-items: center; gap: 18px; border-top: 1px solid var(--border); padding: 12px 0; }
/**
 * 样式范围：.profile-editor__items strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.profile-editor__items strong { display: block; color: var(--text-primary); }
  /**
   * 样式范围：.profile-editor__items small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .profile-editor__items small { display: block; color: var(--text-muted); margin-top: 4px; overflow-wrap: anywhere; }
/**
 * 样式范围：.profile-editor__actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.profile-editor__actions { display: flex; flex-wrap: wrap; justify-content: flex-end; }
  /**
   * 样式范围：.profile-editor__actions button。
   */
  .profile-editor__actions button { margin: 0 0 4px 6px; }
/**
 * 样式范围：.profile-editor__media。
 * 使用网格布局组织内容；列定义为 1fr 1fr，间距由 24px 控制。
 */
.profile-editor__media { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
  /**
   * 样式范围：.profile-editor__media > div。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .profile-editor__media > div { border: 1px dashed var(--border); padding: 18px; border-radius: 10px; }
  /**
   * 样式范围：.profile-editor__media img。
   */
  .profile-editor__media img { max-width: 120px; aspect-ratio: 1; object-fit: cover; display: block; margin-bottom: 10px; }
  /**
   * 样式范围：.profile-editor__media p。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .profile-editor__media p { color: var(--text-muted); }
  /**
   * 样式范围：.profile-editor__media a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .profile-editor__media a { color: var(--primary); }
/**
 * 响应式或设备条件：(max-width: 700px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width: 700px) {
  /**
   * 样式范围：.profile-editor。
   */
  .profile-editor { padding-inline: 16px; }
  /**
   * 样式范围：.profile-editor__panel。
   */
  .profile-editor__panel { padding: 18px; }
  /**
   * 样式范围：.profile-editor__media。
   */
  .profile-editor__media { grid-template-columns: 1fr; }
  /**
   * 样式范围：.profile-editor__items li。
   */
  .profile-editor__items li { align-items: flex-start; flex-direction: column; } }
</style>
