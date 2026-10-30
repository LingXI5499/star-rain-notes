<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'
import { errorMessage } from '../../../../shared/http'
import { getTaxonomy, createTerm, updateTerm, disableTerm } from '../../api/englishRwApi'
import '../../styles/englishRw.css'
const route = useRoute(), terms = ref([]), loading = ref(true), error = ref(''), notice = ref(''), busy = ref(false), search = ref(''), editing = ref(null), confirm = ref(null)
const module = computed(() => route.query.module === 'world' ? 'world' : 'vocabulary')
const title = computed(() => module.value === 'vocabulary' ? '词汇分类' : '世界分类')
const roots = computed(() => terms.value.filter(term => term.libraryModule === module.value))
const form = ref({ parentId: '', name: '', nameEn: '' })
const visible = computed(() => roots.value.map(parent => ({ ...parent, children: parent.children || [] })).filter(parent => !search.value || [parent, ...parent.children].some(term => (term.name+' '+term.nameEn).toLowerCase().includes(search.value.toLowerCase()))))
const parents = computed(() => roots.value.filter(parent => parent.enabled && parent.id !== editing.value?.id))
let generation = 0
async function load() { const current=++generation; loading.value=true; error.value=''; try { const result=await getTaxonomy(true); if(current===generation)terms.value=result } catch(cause) { if(current===generation)error.value=errorMessage(cause) } finally { if(current===generation)loading.value=false } }
function reset() { editing.value=null; form.value={ parentId: String(route.query.parent || ''), name:'', nameEn:'' } }
function edit(term) { editing.value=term; form.value={parentId:term.parentId ? String(term.parentId) : '',name:term.name,nameEn:term.nameEn} }
async function save() {
  if(busy.value)return; busy.value=true; error.value='';notice.value=''
  try {
    const value={name:form.value.name.trim(),nameEn:form.value.nameEn.trim()}
    if(editing.value)await updateTerm(editing.value.id,value)
    else await createTerm({...value,parentId:form.value.parentId || null,dimension:module.value==='vocabulary'?'VOCABULARY':'TOPIC',libraryModule:module.value})
    reset();await load();notice.value='分类已保存。'
  }catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}
}
async function disable(term) {
  if(busy.value || !await confirm.value.ask(`停用「${term.name}」${term.parentId?'':'及其下全部主题'}？已有词条和历史学习记录保留。`,{confirmText:'确认停用',danger:true}))return
  busy.value=true;error.value=''
  try{await disableTerm(term.id);await load();notice.value='分类已停用，历史引用已保留。'}catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}
}
watch(()=>route.query.module,()=>{reset();load()},{immediate:true})
</script>
<template>
 <main class="english-rw taxonomy-manage">
  <RouterLink :to="module==='vocabulary'?'/english/manage/vocabulary':'/english/manage/world'">← 返回{{module==='vocabulary'?'词汇':'世界'}}管理</RouterLink>
  <header class="english-rw__hero"><div><h1>{{title}}管理</h1><p>一级分类下添加二级主题，只需维护中英文名称。停用分类保留历史引用。</p></div></header>
  <nav class="taxonomy-manage__tabs" aria-label="分类范围"><RouterLink :to="{query:{module:'vocabulary'}}" :aria-current="module==='vocabulary'?'page':undefined">词汇分类</RouterLink><RouterLink :to="{query:{module:'world'}}" :aria-current="module==='world'?'page':undefined">世界分类</RouterLink></nav>
  <p v-if="error" class="english-rw__error" role="alert">{{error}}</p><p v-if="notice" role="status">{{notice}}</p>
  <section class="english-rw__panel"><h2>{{editing?'编辑':'新增'}}{{form.parentId?'主题':'一级分类'}}</h2><form @submit.prevent="save"><div class="english-rw__fields">
   <label>父分类<select aria-label="父分类" v-model="form.parentId" :disabled="busy || !!editing"><option value="">无 · 新建一级分类</option><option v-for="parent in parents" :key="parent.id" :value="String(parent.id)">{{parent.name}}</option><option v-if="editing?.parentId && !parents.some(parent=>String(parent.id)===form.parentId)" :value="form.parentId">当前父分类（已停用）</option></select></label>
   <label>中文名称<input v-model="form.name" required maxlength="100" :disabled="busy"></label><label>英文名称<input v-model="form.nameEn" required maxlength="150" :disabled="busy"></label>
  </div><div class="english-rw__actions"><button class="english-rw__button english-rw__button--primary" type="submit" :disabled="busy">保存{{form.parentId?'主题':'分类'}}</button><button type="button" class="english-rw__button" :disabled="busy" @click="reset">取消编辑</button></div></form></section>
  <PublicSearch v-model="search" label="查找分类或主题" />
  <p v-if="loading" role="status">正在读取分类…</p>
  <section v-for="parent in visible" v-else :key="parent.id" class="taxonomy-manage__group"><header><div><h2>{{parent.name}}</h2><p>{{parent.nameEn}} <span v-if="!parent.enabled">· 已停用</span></p></div><div><button type="button" :disabled="busy" @click="edit(parent)">编辑</button><button type="button" :disabled="busy || !parent.enabled" @click="disable(parent)">停用</button><button type="button" :disabled="busy || !parent.enabled" @click="editing=null;form={parentId:String(parent.id),name:'',nameEn:''}">新建主题</button></div></header>
   <ul><li v-for="term in parent.children" :key="term.id"><div><strong>{{term.name}}</strong><span>{{term.nameEn}}{{!term.enabled?' · 已停用':''}}</span></div><div><button type="button" :disabled="busy" @click="edit(term)">编辑</button><button type="button" :disabled="busy || !term.enabled" @click="disable(term)">停用</button></div></li></ul><p v-if="!parent.children.length">暂无主题，可点击“新建主题”添加。</p>
  </section><AppConfirmDialog ref="confirm" />
 </main>
</template>
<style scoped>
.taxonomy-manage__tabs{display:flex;gap:8px;margin-bottom:24px}.taxonomy-manage__tabs a{padding:8px 12px;border:1px solid var(--border);border-radius:8px;color:var(--text-secondary)}.taxonomy-manage__tabs a[aria-current]{color:var(--primary);background:var(--primary-soft)}.taxonomy-manage__group{margin:24px 0;padding:24px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.taxonomy-manage__group header{display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap}.taxonomy-manage__group h2{margin:0;font-size:20px;line-height:28px}.taxonomy-manage__group p{font-size:12px;line-height:16px;color:var(--text-muted)}.taxonomy-manage__group ul{list-style:none;padding:0}.taxonomy-manage__group li{display:flex;justify-content:space-between;align-items:center;gap:16px;padding:12px 0;border-bottom:1px solid var(--border)}.taxonomy-manage__group li span{display:block;font-size:12px;line-height:16px;color:var(--text-muted);margin-top:4px}.taxonomy-manage__group button{padding:8px 12px;border:1px solid var(--border);border-radius:8px;background:var(--bg-surface);color:var(--text-primary);cursor:pointer;font:inherit;font-size:12px;line-height:16px}.taxonomy-manage__group :is(header,li)>div:last-child{display:flex;gap:8px;flex-wrap:wrap}button:hover{background:var(--primary-soft);color:var(--primary)}button:disabled{opacity:.5;cursor:not-allowed}:is(button,a,input,select):focus-visible{outline:2px solid var(--primary);outline-offset:2px}@media(max-width:640px){.taxonomy-manage__group{padding:16px}.taxonomy-manage__group li{flex-wrap:wrap}}
</style>
