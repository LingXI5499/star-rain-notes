<script setup>
/**
 * EnglishTaxonomyManagePage.vue：英语学习模块的页面组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../../../shared/ui/PublicSelect.vue、../../../../shared/ui/PublicSearch.vue、../../../../shared/viewMode、../../../../shared/http。
 * 主要响应式状态：terms、error、busy、search、editing、pending、form、flat。
 * 阅读入口：load（加载当前操作）；edit（edit的处理函数）；reset（重置当前操作）；save（保存当前操作）；disable（禁用当前操作）。
 */


import { computed,onMounted,ref } from 'vue'
import { RouterLink } from 'vue-router'
import PublicSelect from '../../../../shared/ui/PublicSelect.vue'
import PublicSearch from '../../../../shared/ui/PublicSearch.vue'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import { getTaxonomy,createTerm,updateTerm,disableTerm } from '../../api/englishRwApi'
import '../../styles/englishRw.css'
/**
 * terms；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const terms=ref([]),error=ref(''),busy=ref(false),search=ref(''),editing=ref(null),pending=ref(null),form=ref({dimension:'TOPIC',parentId:null,name:'',nameEn:'',slug:'',description:'',sortOrder:0})
/**
 * flat；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const flat=computed(()=>terms.value.flatMap(t=>[t,...(t.children||[])])),visible=computed(()=>flat.value.filter(t=>(t.name+' '+t.nameEn+' '+t.slug).toLowerCase().includes(search.value.toLowerCase())))
/**
 * 祖先节点；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const parents=computed(()=>[{value:'',label:'一级分类'},...terms.value.filter(t=>t.dimension===form.value.dimension && t.enabled).map(t=>({value:String(t.id),label:t.name}))])
/**
 * 加载当前操作。
 * 主要协作：getTaxonomy、errorMessage。
 * 更新的响应式状态包括 terms、error；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load(){try{terms.value=await getTaxonomy(true)}catch(cause){error.value=errorMessage(cause)}}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
/**
 * edit的处理函数。
 * 更新的响应式状态包括 editing、form；渲染由 Vue 的依赖跟踪触发。
 * @param t t；默认值、解构字段与可选参数见函数签名。
 */
function edit(t){editing.value=t.id;form.value={...t}}
/**
 * 重置当前操作。
 * 更新的响应式状态包括 editing、form；渲染由 Vue 的依赖跟踪触发。
 */
function reset(){editing.value=null;form.value={dimension:'TOPIC',parentId:null,name:'',nameEn:'',slug:'',description:'',sortOrder:0}}
/**
 * 保存当前操作。
 * 主要协作：updateTerm、createTerm、reset、load、errorMessage。
 * 更新的响应式状态包括 busy、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function save(){busy.value=true;error.value='';try{await(editing.value?updateTerm(editing.value,form.value):createTerm({...form.value,parentId:form.value.parentId||null}));reset();await load()}catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}}
/**
 * 禁用当前操作。
 * 主要协作：disableTerm、load、errorMessage。
 * 更新的响应式状态包括 busy、pending、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function disable(){busy.value=true;try{await disableTerm(pending.value.id);pending.value=null;await load()}catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}}
</script>
<template><main class="english-rw"><RouterLink :to="accountPath('/english/manage/reading')">← 阅读管理</RouterLink><header class="english-rw__hero"><div><p class="public-eyebrow">SHARED TAXONOMY</p><h1>读写分类</h1><p>主题、文体与用途由阅读和写作共享。停用分类会保留历史引用。</p></div></header><p v-if="error" class="english-rw__error" role="alert">{{error}}</p><section class="english-rw__panel"><h2>{{editing?'修改分类':'新增分类'}}</h2><div class="english-rw__fields"><PublicSelect v-model="form.dimension" label="维度" :disabled="Boolean(editing)" :options="[{value:'TOPIC',label:'主题'},{value:'GENRE',label:'文体'},{value:'PURPOSE',label:'用途'}]" /><PublicSelect v-if="form.dimension!=='PURPOSE'" v-model="form.parentId" label="父分类" :disabled="Boolean(editing)" :options="parents" /><label>中文名称<input v-model="form.name" maxlength="100"></label><label>英文名称<input v-model="form.nameEn" maxlength="150"></label><label>稳定标识<input v-model="form.slug" :disabled="Boolean(editing)" placeholder="例如：digital-life" maxlength="100"></label><label>说明<textarea v-model="form.description" rows="2" maxlength="500"></textarea></label></div><div class="english-rw__actions"><button class="english-rw__button english-rw__button--primary" :disabled="busy" @click="save">保存分类</button><button class="english-rw__button" @click="reset">清空编辑</button></div></section><PublicSearch v-model="search" label="查找分类" @search="search=$event" /><div v-for="t in visible" :key="t.id" class="english-rw__row"><span>{{t.dimension}} · {{t.parentId?'↳ ':''}}{{t.name}} <small class="english-rw__muted">{{t.slug}} · {{t.enabled?'启用':'停用'}}</small></span><div class="english-rw__actions"><button class="english-rw__button" @click="edit(t)">编辑</button><button class="english-rw__button" :disabled="!t.enabled" @click="pending=t">停用</button></div></div><section v-if="pending" class="english-rw__notice"><p>确认停用“{{pending.name}}”？其子分类也将停止出现在新文章的分类选项中。</p><button class="english-rw__button" @click="pending=null">取消</button><button class="english-rw__button" :disabled="busy" @click="disable">确认停用</button></section></main></template>
