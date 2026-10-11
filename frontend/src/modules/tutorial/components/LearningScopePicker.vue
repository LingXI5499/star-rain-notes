<script setup>
/**
 * LearningScopePicker.vue：教程与学习模块的视图组件。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、vue-router、../../../shared/viewMode、../support/studyPlanScope。
 * 组件输入：groups、entireTutorial、groupIds、chapterIds、restrictions、tutorialSlug、disabled；默认值与类型见 defineProps。
 * 组件输出事件：update:entireTutorial、update:groupIds、update:chapterIds；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、activeGroupId、search、activeGroup、visibleChapters、selectedChapters、allChapters。
 * 阅读入口：contains（contains的处理函数）；isGroupSelected（判断是否分组选中）；isChapterSelected（判断是否章节选中）；restriction（restriction的处理函数）；availableInGroup（available内部分组的处理函数）。
 */


import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { accountPath } from '../../../shared/viewMode'
import { chapterPlanRestriction, selectedScopeChapters } from '../support/studyPlanScope'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ groups: { type: Array, default: () => [] }, entireTutorial: Boolean, groupIds: { type: Array, default: () => [] }, chapterIds: { type: Array, default: () => [] }, restrictions: { type: Object, default: () => ({}) }, tutorialSlug: { type: String, default: '' }, disabled: Boolean })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['update:entireTutorial', 'update:groupIds', 'update:chapterIds'])
/**
 * 有效分组标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const activeGroupId = ref(''), search = ref('')
/**
 * contains的处理函数。
 * 主要协作：ids.some。
 * @param ids 标识集合；默认值、解构字段与可选参数见函数签名。
 * @param id 当前业务对象的数据库标识；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const contains = (ids, id) => ids.some(value => String(value) === String(id))
/**
 * 有效分组；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const activeGroup = computed(() => props.groups.find(group => String(group.id) === activeGroupId.value))
/**
 * 可见章节列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const visibleChapters = computed(() => { const rows = activeGroup.value?.chapters || []; const term = search.value.trim().toLocaleLowerCase(); return term ? rows.filter(chapter => chapter.title.toLocaleLowerCase().includes(term)) : rows })
/**
 * 判断是否分组选中。
 * 主要协作：contains。
 * @param group 分组；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const isGroupSelected = group => props.entireTutorial || contains(props.groupIds, group.id)
/**
 * 判断是否章节选中。
 * 主要协作：isGroupSelected、contains。
 * @param group 分组；默认值、解构字段与可选参数见函数签名。
 * @param chapter 章节；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const isChapterSelected = (group, chapter) => isGroupSelected(group) || contains(props.chapterIds, chapter.id)
/**
 * 选中章节列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const selectedChapters = computed(() => selectedScopeChapters(props.groups, props))
/**
 * 全部章节列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const allChapters = computed(() => props.groups.flatMap(group => group.chapters || []))
/**
 * restriction的处理函数。
 * 主要协作：chapterPlanRestriction。
 * @param chapter 章节；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const restriction = chapter => props.restrictions[String(chapter.id)] || chapterPlanRestriction(chapter)
/**
 * available章节列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const availableChapters = computed(() => allChapters.value.filter(chapter => !restriction(chapter)))
/**
 * unavailable数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const unavailableCount = computed(() => allChapters.value.length - availableChapters.value.length)
/**
 * available内部分组的处理函数。
 * 主要协作：.filter、restriction。
 * @param group 分组；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const availableInGroup = group => (group.chapters || []).filter(chapter => !restriction(chapter))
/**
 * 分组Button选中的处理函数。
 * 主要协作：isGroupSelected、availableInGroup、.every、isChapterSelected。
 * @param group 分组；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const groupButtonSelected = group => isGroupSelected(group) || (availableInGroup(group).length > 0 && availableInGroup(group).every(chapter => isChapterSelected(group, chapter)))
/**
 * 选中卡片数量；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const selectedCardCount = computed(() => selectedChapters.value.reduce((total, chapter) => total + (Number(chapter.cardCount) || 0), 0))
/**
 * 分组选中数量的处理函数。
 * 主要协作：.filter、isChapterSelected。
 * @param group 分组；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
const groupSelectedCount = group => (group.chapters || []).filter(chapter => isChapterSelected(group, chapter)).length
/**
 * 查询分组的处理函数。
 * 更新的响应式状态包括 activeGroupId、search；渲染由 Vue 的依赖跟踪触发。
 * @param group 分组；默认值、解构字段与可选参数见函数签名。
 */
function selectGroup(group) { activeGroupId.value = String(group.id); search.value = '' }
/**
 * 切换分组。
 * 主要协作：contains、emit、props.groupIds.filter、availableInGroup、groupButtonSelected。
 * @param group 分组；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function toggleGroup(group) {
 if (props.disabled || props.entireTutorial) return
 if (contains(props.groupIds, group.id)) { emit('update:groupIds', props.groupIds.filter(id => String(id) !== String(group.id))); return }
 const available = availableInGroup(group)
 if (!available.length) return
 if (groupButtonSelected(group)) { emit('update:chapterIds', props.chapterIds.filter(id => !(group.chapters || []).some(chapter => String(chapter.id) === String(id)))); return }
 if (available.length === group.chapters.length) emit('update:groupIds', [...props.groupIds, group.id])
 else emit('update:chapterIds', [...props.chapterIds, ...available.map(chapter => chapter.id).filter(id => !contains(props.chapterIds, id))])
}
/**
 * 切换章节。
 * 主要协作：isGroupSelected、restriction、contains、emit、props.chapterIds.filter。
 * @param chapter 章节；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function toggleChapter(chapter) { if (props.disabled || isGroupSelected(activeGroup.value) || (restriction(chapter) && !contains(props.chapterIds, chapter.id))) return; emit('update:chapterIds', contains(props.chapterIds, chapter.id) ? props.chapterIds.filter(id => String(id) !== String(chapter.id)) : [...props.chapterIds, chapter.id]) }
/**
 * 查询Available的处理函数。
 * 主要协作：emit、availableChapters.value.map。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function selectAvailable() { if (props.disabled) return; emit('update:entireTutorial', false); emit('update:groupIds', []); emit('update:chapterIds', availableChapters.value.map(chapter => chapter.id)) }
/**
 * 清理当前操作。
 * 主要协作：emit。
 */
function clear() { emit('update:entireTutorial', false); emit('update:groupIds', []); emit('update:chapterIds', []) }
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => props.groups, groups => { if (!groups.some(group => String(group.id) === activeGroupId.value)) activeGroupId.value = String(groups[0]?.id || ''); search.value = '' }, { immediate: true })
</script>
<template>
 <section class="scope-picker" aria-label="学习范围选择">
  <header class="scope-picker__toolbar"><div><strong>已选 {{ selectedChapters.length }} 个章节</strong><small>{{ selectedCardCount }} 个知识点 · {{ availableChapters.length }} 章可加入计划</small></div><div class="scope-picker__actions"><button type="button" class="learning-button" :disabled="disabled || (!entireTutorial && (!allChapters.length || unavailableCount > 0))" :aria-pressed="entireTutorial" @click="emit('update:entireTutorial', !entireTutorial)">{{ entireTutorial ? '取消整套' : '选中整套' }}</button><button v-if="unavailableCount && availableChapters.length" type="button" class="learning-button" :disabled="disabled" @click="selectAvailable">选中所有可加入章节</button><button type="button" class="learning-button" :disabled="disabled || (!entireTutorial && !groupIds.length && !chapterIds.length)" @click="clear">清空选择</button></div></header>
  <p v-if="allChapters.length && !availableChapters.length" class="scope-picker__hint" role="status">本教程当前没有可加入新计划的章节。尚无启用且已发布的知识卡片，或章节已在其他未结束计划中；可以先阅读章节、继续已有计划，或选择其他教程。</p>
  <p v-else-if="unavailableCount" class="scope-picker__hint" role="status">{{ unavailableCount }} 章暂不可加入新计划，原因见章节列表。“选中所有可加入章节”仅选择符合条件的章节，不等于整套教程。</p>
  <p v-else class="scope-picker__hint">章节至少有一张启用且已发布的知识卡片才能加入计划；问题可为零。选择整套、整组或单章后预览任务。</p>
  <div class="scope-picker__layout">
   <aside class="scope-picker__groups" aria-label="课程分组"><h3>课程分组 <small>{{ groups.length }} 组</small></h3><div class="scope-picker__scroll">
    <article v-for="group in groups" :key="group.id" class="scope-picker__group" :class="{ 'is-active': String(group.id) === activeGroupId }">
     <button type="button" class="scope-picker__group-open" :aria-pressed="String(group.id) === activeGroupId" @click="selectGroup(group)"><strong>{{ group.title }}</strong><small>{{ groupSelectedCount(group) }} / {{ group.chapters?.length || 0 }} 章已选 · {{ availableInGroup(group).length }} 章可加入 <span aria-hidden="true">›</span></small></button>
     <button type="button" class="scope-picker__select" :disabled="disabled || entireTutorial || (!contains(groupIds, group.id) && !availableInGroup(group).length)" :aria-label="`${groupButtonSelected(group) ? '取消' : '选中'}分组 ${group.title}`" :aria-pressed="groupButtonSelected(group)" @click="toggleGroup(group)">{{ entireTutorial ? '整套已选' : groupButtonSelected(group) ? '取消选中' : !availableInGroup(group).length ? '暂无可加入章节' : availableInGroup(group).length === group.chapters.length ? '选中整组' : '选中可加入章节' }}</button>
    </article>
    <p v-if="!groups.length" class="scope-picker__empty">选择教程后，在这里查看课程分组。</p>
   </div></aside>
   <section class="scope-picker__chapters" aria-label="分组章节"><header><div><h3>{{ activeGroup?.title || '章节列表' }}</h3><small>当前分组 {{ activeGroup?.chapters?.length || 0 }} 章</small></div><label><span class="scope-picker__sr-only">搜索当前分组章节</span><input v-model="search" type="search" :disabled="!activeGroup" placeholder="搜索当前分组章节" /></label></header><div class="scope-picker__scroll">
    <article v-for="(chapter,index) in visibleChapters" :key="chapter.id" class="scope-picker__chapter" :class="{ 'is-selected': isChapterSelected(activeGroup, chapter), 'is-unavailable': restriction(chapter) }"><span class="scope-picker__index">{{ String(index + 1).padStart(2, '0') }}</span><div><strong>{{ chapter.title }}</strong><small>{{ chapter.cardCount ?? '—' }} 个知识点 · {{ chapter.questionCount ?? '—' }} 道问题</small><small v-if="restriction(chapter)" class="scope-picker__reason">{{ restriction(chapter).reason }}</small><div class="scope-picker__links"><RouterLink v-if="tutorialSlug && chapter.slug" :to="accountPath(`/tutorials/${tutorialSlug}/${chapter.slug}`)">阅读章节 ↗</RouterLink><RouterLink v-if="restriction(chapter)?.planId" :to="accountPath(`/learning/plans/${restriction(chapter).planId}`)">查看已有计划 ↗</RouterLink></div></div><button type="button" class="scope-picker__select" :disabled="disabled || isGroupSelected(activeGroup) || (!!restriction(chapter) && !contains(chapterIds, chapter.id))" :aria-label="`${isChapterSelected(activeGroup, chapter) ? '取消' : '选中'}章节 ${chapter.title}`" :aria-pressed="isChapterSelected(activeGroup, chapter)" @click="toggleChapter(chapter)">{{ isGroupSelected(activeGroup) ? '范围已包含' : isChapterSelected(activeGroup, chapter) ? '取消选中' : restriction(chapter) ? '暂不可加入' : '选中' }}</button></article>
    <p v-if="!visibleChapters.length" class="scope-picker__empty">{{ search ? '没有匹配的章节，请更换关键词。' : activeGroup ? '当前分组暂无公开章节。' : '先从左侧选择一个课程分组。' }}</p>
   </div></section>
  </div>
 </section>
</template>
<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.scope-picker。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.scope-picker{min-width:0;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface);overflow:hidden}
  /**
   * 样式范围：.scope-picker__toolbar。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__toolbar{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:16px 18px;border-bottom:1px solid var(--border);background:var(--bg-subtle)}
  /**
   * 样式范围：.scope-picker small。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker small{display:block;color:var(--text-muted);font-size:12px;line-height:1.6;margin-top:4px}
  /**
   * 样式范围：.scope-picker__actions。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .scope-picker__actions{display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end}
  /**
   * 样式范围：.scope-picker__layout。
   * 使用网格布局组织内容；列定义为 minmax(220px,30%) minmax(0,1fr)，间距由 各行列间距规则 控制。
   */
  .scope-picker__layout{display:grid;grid-template-columns:minmax(220px,30%) minmax(0,1fr)}
  /**
   * 样式范围：.scope-picker__groups,.scope-picker__chapters。
   * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
   */
  .scope-picker__groups,.scope-picker__chapters{display:flex;flex-direction:column;min-width:0;height:400px}
  /**
   * 样式范围：.scope-picker__groups。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__groups{border-right:1px solid var(--border)}
  /**
   * 样式范围：.scope-picker h3。
   */
  .scope-picker h3{margin:0;font-size:15px;line-height:1.5}
  /**
   * 样式范围：.scope-picker__groups>h3。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__groups>h3{display:flex;justify-content:space-between;align-items:center;min-height:80px;padding:16px;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.scope-picker__scroll。
   * 溢出处理：纵向 auto。
   */
  .scope-picker__scroll{min-height:0;flex:1;overflow-y:auto;overscroll-behavior:contain;padding:10px;scrollbar-gutter:stable}
  /**
   * 样式范围：.scope-picker__group。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__group{margin-bottom:8px;padding:10px;border:1px solid var(--border);border-radius:10px}
  /**
   * 样式范围：.scope-picker__group.is-active。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__group.is-active{border-color:var(--primary);background:color-mix(in srgb,var(--primary) 8%,var(--bg-surface))}
  /**
   * 样式范围：.scope-picker__group-open。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__group-open{display:block;width:100%;padding:0;border:0;background:transparent;text-align:left;color:var(--text-primary);font:inherit;cursor:pointer}
  /**
   * 样式范围：.scope-picker__group-open strong。
   */
  .scope-picker__group-open strong{font-size:13px;line-height:1.6}
  /**
   * 样式范围：.scope-picker__group-open small。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .scope-picker__group-open small{display:flex;justify-content:space-between}
  /**
   * 样式范围：.scope-picker__select。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__select{padding:6px 10px;border:1px solid var(--border);border-radius:8px;background:var(--bg-surface);color:var(--primary);font:inherit;font-size:12px;white-space:nowrap;cursor:pointer}
  /**
   * 样式范围：.scope-picker__select[aria-pressed="true"]。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__select[aria-pressed="true"]{border-color:var(--primary);background:var(--bg-subtle)}
  /**
   * 样式范围：.scope-picker__select:disabled。
   */
  .scope-picker__select:disabled{cursor:default;opacity:.65}
  /**
   * 样式范围：.scope-picker__group>.scope-picker__select。
   */
  .scope-picker__group>.scope-picker__select{margin-top:8px}
  /**
   * 样式范围：.scope-picker__chapters>header。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__chapters>header{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:80px;padding:16px;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.scope-picker__chapters>header>div。
   */
  .scope-picker__chapters>header>div{min-width:0}
  /**
   * 样式范围：.scope-picker__chapters>header label。
   */
  .scope-picker__chapters>header label{width:42%;flex-shrink:0}
  /**
   * 样式范围：.scope-picker__chapters input。
   */
  .scope-picker__chapters input{min-width:0}
  /**
   * 样式范围：.scope-picker__chapter。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__chapter{display:flex;align-items:center;gap:10px;padding:12px;margin-bottom:8px;border:1px solid var(--border);border-radius:10px}
  /**
   * 样式范围：.scope-picker__chapter>div。
   */
  .scope-picker__chapter>div{flex:1;min-width:0}
  /**
   * 样式范围：.scope-picker__chapter strong。
   */
  .scope-picker__chapter strong{font-size:13px;line-height:1.6;overflow-wrap:anywhere}
  /**
   * 样式范围：.scope-picker__chapter.is-selected。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__chapter.is-selected{border-color:color-mix(in srgb,var(--primary) 45%,var(--border));background:color-mix(in srgb,var(--primary) 5%,var(--bg-surface))}
  /**
   * 样式范围：.scope-picker__index。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__index{color:var(--accent);font-size:11px}
  /**
   * 样式范围：.scope-picker__empty。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__empty{padding:24px 12px;text-align:center;color:var(--text-muted);font-size:13px;line-height:1.8}
  /**
   * 样式范围：.scope-picker__sr-only。
   * 溢出处理：整体 hidden。
   */
  .scope-picker__sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%)}
/**
 * 样式范围：.scope-picker__hint。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.scope-picker__hint{margin:0;padding:12px 18px;border-bottom:1px solid var(--border);color:var(--text-secondary);font-size:13px;line-height:1.8}
  /**
   * 样式范围：.scope-picker__chapter.is-unavailable。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__chapter.is-unavailable{background:var(--bg-subtle)}
  /**
   * 样式范围：.scope-picker__reason。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__reason{color:var(--text-secondary)!important}
  /**
   * 样式范围：.scope-picker__links。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .scope-picker__links{display:flex;gap:12px;flex-wrap:wrap;margin-top:5px}
  /**
   * 样式范围：.scope-picker__links a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__links a{font-size:12px;color:var(--primary)}
/**
 * 响应式或设备条件：(max-width:760px)；仅在该条件成立时覆盖基础样式。
 */
@media(max-width:760px){
  /**
   * 样式范围：.scope-picker__toolbar。
   */
  .scope-picker__toolbar{align-items:flex-start;flex-direction:column}
  /**
   * 样式范围：.scope-picker__layout。
   */
  .scope-picker__layout{grid-template-columns:minmax(0,1fr)}
  /**
   * 样式范围：.scope-picker__groups。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .scope-picker__groups{height:220px;border-right:0;border-bottom:1px solid var(--border)}
  /**
   * 样式范围：.scope-picker__groups>h3。
   */
  .scope-picker__groups>h3{min-height:50px;padding:10px 14px}
  /**
   * 样式范围：.scope-picker__chapters。
   */
  .scope-picker__chapters{height:360px}
  /**
   * 样式范围：.scope-picker__chapters>header。
   */
  .scope-picker__chapters>header{align-items:flex-start;flex-direction:column}
  /**
   * 样式范围：.scope-picker__chapters>header label。
   */
  .scope-picker__chapters>header label{width:100%}
  /**
   * 样式范围：.scope-picker__chapter。
   */
  .scope-picker__chapter{flex-wrap:wrap}
  /**
   * 样式范围：.scope-picker__chapter>button。
   */
  .scope-picker__chapter>button{margin-left:auto}}
</style>
