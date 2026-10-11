<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { accountPath } from '../../../shared/viewMode'
import { chapterPlanRestriction, selectedScopeChapters } from '../support/studyPlanScope'
const props = defineProps({ groups: { type: Array, default: () => [] }, entireTutorial: Boolean, groupIds: { type: Array, default: () => [] }, chapterIds: { type: Array, default: () => [] }, restrictions: { type: Object, default: () => ({}) }, tutorialSlug: { type: String, default: '' }, disabled: Boolean })
const emit = defineEmits(['update:entireTutorial', 'update:groupIds', 'update:chapterIds'])
const activeGroupId = ref(''), search = ref('')
const contains = (ids, id) => ids.some(value => String(value) === String(id))
const activeGroup = computed(() => props.groups.find(group => String(group.id) === activeGroupId.value))
const visibleChapters = computed(() => { const rows = activeGroup.value?.chapters || []; const term = search.value.trim().toLocaleLowerCase(); return term ? rows.filter(chapter => chapter.title.toLocaleLowerCase().includes(term)) : rows })
const isGroupSelected = group => props.entireTutorial || contains(props.groupIds, group.id)
const isChapterSelected = (group, chapter) => isGroupSelected(group) || contains(props.chapterIds, chapter.id)
const selectedChapters = computed(() => selectedScopeChapters(props.groups, props))
const allChapters = computed(() => props.groups.flatMap(group => group.chapters || []))
const restriction = chapter => props.restrictions[String(chapter.id)] || chapterPlanRestriction(chapter)
const availableChapters = computed(() => allChapters.value.filter(chapter => !restriction(chapter)))
const unavailableCount = computed(() => allChapters.value.length - availableChapters.value.length)
const availableInGroup = group => (group.chapters || []).filter(chapter => !restriction(chapter))
const groupButtonSelected = group => isGroupSelected(group) || (availableInGroup(group).length > 0 && availableInGroup(group).every(chapter => isChapterSelected(group, chapter)))
const selectedCardCount = computed(() => selectedChapters.value.reduce((total, chapter) => total + (Number(chapter.cardCount) || 0), 0))
const groupSelectedCount = group => (group.chapters || []).filter(chapter => isChapterSelected(group, chapter)).length
function selectGroup(group) { activeGroupId.value = String(group.id); search.value = '' }
function toggleGroup(group) {
 if (props.disabled || props.entireTutorial) return
 if (contains(props.groupIds, group.id)) { emit('update:groupIds', props.groupIds.filter(id => String(id) !== String(group.id))); return }
 const available = availableInGroup(group)
 if (!available.length) return
 if (groupButtonSelected(group)) { emit('update:chapterIds', props.chapterIds.filter(id => !(group.chapters || []).some(chapter => String(chapter.id) === String(id)))); return }
 if (available.length === group.chapters.length) emit('update:groupIds', [...props.groupIds, group.id])
 else emit('update:chapterIds', [...props.chapterIds, ...available.map(chapter => chapter.id).filter(id => !contains(props.chapterIds, id))])
}
function toggleChapter(chapter) { if (props.disabled || isGroupSelected(activeGroup.value) || (restriction(chapter) && !contains(props.chapterIds, chapter.id))) return; emit('update:chapterIds', contains(props.chapterIds, chapter.id) ? props.chapterIds.filter(id => String(id) !== String(chapter.id)) : [...props.chapterIds, chapter.id]) }
function selectAvailable() { if (props.disabled) return; emit('update:entireTutorial', false); emit('update:groupIds', []); emit('update:chapterIds', availableChapters.value.map(chapter => chapter.id)) }
function clear() { emit('update:entireTutorial', false); emit('update:groupIds', []); emit('update:chapterIds', []) }
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
.scope-picker{min-width:0;border:1px solid var(--border);border-radius:14px;background:var(--bg-surface);overflow:hidden}.scope-picker__toolbar{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:16px 18px;border-bottom:1px solid var(--border);background:var(--bg-subtle)}.scope-picker small{display:block;color:var(--text-muted);font-size:12px;line-height:1.6;margin-top:4px}.scope-picker__actions{display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end}.scope-picker__layout{display:grid;grid-template-columns:minmax(220px,30%) minmax(0,1fr)}.scope-picker__groups,.scope-picker__chapters{display:flex;flex-direction:column;min-width:0;height:400px}.scope-picker__groups{border-right:1px solid var(--border)}.scope-picker h3{margin:0;font-size:15px;line-height:1.5}.scope-picker__groups>h3{display:flex;justify-content:space-between;align-items:center;min-height:80px;padding:16px;border-bottom:1px solid var(--border)}.scope-picker__scroll{min-height:0;flex:1;overflow-y:auto;overscroll-behavior:contain;padding:10px;scrollbar-gutter:stable}.scope-picker__group{margin-bottom:8px;padding:10px;border:1px solid var(--border);border-radius:10px}.scope-picker__group.is-active{border-color:var(--primary);background:color-mix(in srgb,var(--primary) 8%,var(--bg-surface))}.scope-picker__group-open{display:block;width:100%;padding:0;border:0;background:transparent;text-align:left;color:var(--text-primary);font:inherit;cursor:pointer}.scope-picker__group-open strong{font-size:13px;line-height:1.6}.scope-picker__group-open small{display:flex;justify-content:space-between}.scope-picker__select{padding:6px 10px;border:1px solid var(--border);border-radius:8px;background:var(--bg-surface);color:var(--primary);font:inherit;font-size:12px;white-space:nowrap;cursor:pointer}.scope-picker__select[aria-pressed="true"]{border-color:var(--primary);background:var(--bg-subtle)}.scope-picker__select:disabled{cursor:default;opacity:.65}.scope-picker__group>.scope-picker__select{margin-top:8px}.scope-picker__chapters>header{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:80px;padding:16px;border-bottom:1px solid var(--border)}.scope-picker__chapters>header>div{min-width:0}.scope-picker__chapters>header label{width:42%;flex-shrink:0}.scope-picker__chapters input{min-width:0}.scope-picker__chapter{display:flex;align-items:center;gap:10px;padding:12px;margin-bottom:8px;border:1px solid var(--border);border-radius:10px}.scope-picker__chapter>div{flex:1;min-width:0}.scope-picker__chapter strong{font-size:13px;line-height:1.6;overflow-wrap:anywhere}.scope-picker__chapter.is-selected{border-color:color-mix(in srgb,var(--primary) 45%,var(--border));background:color-mix(in srgb,var(--primary) 5%,var(--bg-surface))}.scope-picker__index{color:var(--accent);font-size:11px}.scope-picker__empty{padding:24px 12px;text-align:center;color:var(--text-muted);font-size:13px;line-height:1.8}.scope-picker__sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%)}
.scope-picker__hint{margin:0;padding:12px 18px;border-bottom:1px solid var(--border);color:var(--text-secondary);font-size:13px;line-height:1.8}.scope-picker__chapter.is-unavailable{background:var(--bg-subtle)}.scope-picker__reason{color:var(--text-secondary)!important}.scope-picker__links{display:flex;gap:12px;flex-wrap:wrap;margin-top:5px}.scope-picker__links a{font-size:12px;color:var(--primary)}
@media(max-width:760px){.scope-picker__toolbar{align-items:flex-start;flex-direction:column}.scope-picker__layout{grid-template-columns:minmax(0,1fr)}.scope-picker__groups{height:220px;border-right:0;border-bottom:1px solid var(--border)}.scope-picker__groups>h3{min-height:50px;padding:10px 14px}.scope-picker__chapters{height:360px}.scope-picker__chapters>header{align-items:flex-start;flex-direction:column}.scope-picker__chapters>header label{width:100%}.scope-picker__chapter{flex-wrap:wrap}.scope-picker__chapter>button{margin-left:auto}}
</style>
