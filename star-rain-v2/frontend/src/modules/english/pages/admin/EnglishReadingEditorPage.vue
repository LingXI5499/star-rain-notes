<script setup>
import { computed,nextTick,ref,watch } from 'vue'
import { RouterLink,useRoute,useRouter } from 'vue-router'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import PublicSelect from '../../../../shared/ui/PublicSelect.vue'
import PublicFilterTabs from '../../../../shared/ui/PublicFilterTabs.vue'
import { accountPath } from '../../../../shared/viewMode'
import { errorMessage } from '../../../../shared/http'
import { createReading,getReading,updateReading } from '../../api/englishApi'
import { post } from '../../../../shared/http'
import { getEnhancements,saveEnhancements } from '../../api/englishRwApi'
import EnglishTaxonomyFields from '../../components/EnglishTaxonomyFields.vue'
import EnglishBilingualProse from '../../components/EnglishBilingualProse.vue'
import EnglishRevisionPanel from '../../components/EnglishRevisionPanel.vue'
import '../../styles/englishRw.css'
const englishEditor=ref(null),chineseEditor=ref(null)
const route=useRoute(),router=useRouter(),article=ref(null),form=ref({}),tab=ref('body'),loading=ref(true),busy=ref(false),error=ref(''),notice=ref(''),dirty=ref(false),selected=ref([]),noteSelection=ref(null),analysis=ref(''),editingNote=ref(null)
const rows=ref({alignments:[],annotations:[],vocabulary:[]}),word=ref({word:'',partOfSpeech:'NOUN',meaningZh:''}),editingWord=ref(null)
const tabs=computed(()=>[{value:'body',label:'正文与分类'},{value:'alignments',label:'双语对齐',disabled:!article.value},{value:'annotations',label:'精选精读',disabled:!article.value},{value:'vocabulary',label:'本文词汇',disabled:!article.value},{value:'history',label:'历史版本',disabled:!article.value}])
const groups=computed(()=>Object.entries(Object.groupBy(rows.value.alignments,r=>r.groupKey)))
const pos=['NOUN','VERB','ADJECTIVE','ADVERB','PREPOSITION','PRONOUN','CONJUNCTION','PHRASE','OTHER']
let initializing=false,generation=0
function blank(){return{title:'',summary:'',bodyMarkdown:'',translationZhMarkdown:'',primaryTopicId:null,otherTopicIds:[],genreIds:[],purposeIds:[],contentOrigin:'EXTERNAL',sourceName:'',sourceUrl:'',rights:{rightsStatus:'PENDING',rightsBasis:'',originalAuthor:'',licenseNotice:''}}}
async function loadRows(){if(!article.value)return;const [a,b,c]=await Promise.all(['alignments','annotations','vocabulary'].map(k=>getEnhancements(article.value.id,k,true)));rows.value={alignments:a,annotations:b,vocabulary:c}}
async function accept(value){initializing=true;article.value=value;form.value={...blank(),...value,rights:{...blank().rights,...value.rights}};await nextTick();initializing=false;dirty.value=false}
watch(()=>route.params.documentId,async id=>{const n=++generation;loading.value=true;error.value='';tab.value='body';article.value=null;rows.value={alignments:[],annotations:[],vocabulary:[]};initializing=true;form.value=blank();await nextTick();initializing=false;dirty.value=false
 try { if(id!=='new'){const value=await getReading(id,true);if(n!==generation)return;await accept(value);await loadRows()} }catch(cause){error.value=errorMessage(cause)}finally{if(n===generation)loading.value=false}
},{immediate:true})
watch(form,()=>{if(!initializing)dirty.value=true},{deep:true})
watch(()=>[form.value.bodyMarkdown,form.value.sourceUrl],()=>{if(!initializing && form.value.contentOrigin==='EXTERNAL')form.value.rights.rightsStatus='PENDING'})
async function save(){if(busy.value)return false;if(englishEditor.value)form.value.bodyMarkdown=englishEditor.value.getMarkdown();if(chineseEditor.value)form.value.translationZhMarkdown=chineseEditor.value.getMarkdown();await nextTick();busy.value=true;error.value='';notice.value=''
 try { const payload={...form.value,rowVersion:article.value?.rowVersion};const saved=article.value?await updateReading(article.value.id,payload):await createReading(payload);await accept(saved);await loadRows();notice.value='正文与分类已保存。'
  if(route.params.documentId==='new')await router.replace(accountPath('/english/manage/editor/reading/'+saved.id));return true
 }catch(cause){error.value=errorMessage(cause);return false}finally{busy.value=false}}
async function status(){if(!article.value || busy.value)return;busy.value=true;error.value=''
 try{const saved=await post('/admin/english/content/reading/'+article.value.id+'/'+(article.value.publishStatus==='PUBLISHED'?'withdraw':'publish'),{rowVersion:article.value.rowVersion});article.value=saved;notice.value=saved.publishStatus==='PUBLISHED'?'文章已发布。':'文章已撤回，可以修改版权待核查的正文。'}catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}}
function select(anchor){if(tab.value==='alignments')selected.value.push(anchor);else if(anchor.language==='EN')noteSelection.value=anchor;else error.value='精选精读请选择英文片段。'}
function addGroup(){if(!selected.value.some(r=>r.language==='EN') || !selected.value.some(r=>r.language==='ZH')){error.value='请至少选择一段英文和一段中文，可以添加多个片段。';return}
 const groupKey=crypto.randomUUID();rows.value.alignments.push(...selected.value.map(r=>({...r,groupKey,status:'ACTIVE',sortOrder:0})));selected.value=[]}
function addNote(){if(!noteSelection.value || !analysis.value.trim()){error.value='请选择英文片段并填写人工解析。';return}
 const value={...noteSelection.value,status:'ACTIVE',analysisMarkdown:analysis.value,sortOrder:0}
 if(editingNote.value!==null)rows.value.annotations.splice(editingNote.value,1,value);else rows.value.annotations.push(value);noteSelection.value=null;analysis.value='';editingNote.value=null}
function addWord(){if(!word.value.word.trim() || !word.value.meaningZh.trim()){error.value='请填写单词与本文释义。';return}
 const value={...word.value,sortOrder:0};if(editingWord.value!==null)rows.value.vocabulary.splice(editingWord.value,1,value);else rows.value.vocabulary.push(value);word.value={word:'',partOfSpeech:'NOUN',meaningZh:''};editingWord.value=null}
async function saveRows(kind){if(busy.value || dirty.value)return;busy.value=true;error.value=''
 try{const saved=await saveEnhancements(article.value.id,kind,article.value.rowVersion,rows.value[kind]);article.value.rowVersion=saved.rowVersion;rows.value[kind]=saved.items;notice.value='条目已保存。';dirty.value=false}catch(cause){error.value=errorMessage(cause)}finally{busy.value=false}}
async function restored(value){await accept(value);await loadRows();notice.value='已恢复正文、分类与增强数据，并新增历史版本。'}
</script>
<template><main class="english-rw"><RouterLink :to="accountPath('/english/manage/reading')">← 阅读管理</RouterLink><header class="english-rw__hero"><div><p class="public-eyebrow">READING STUDIO</p><h1>{{article?'编辑阅读文章':'新建阅读文章'}}</h1><p>{{article?.publishStatus==='PUBLISHED'?'已发布':article?.publishStatus==='WITHDRAWN'?'已撤回':'草稿'}}{{dirty?' · 有未保存修改':''}}</p></div><div class="english-rw__actions"><RouterLink class="english-rw__button" :to="accountPath('/english/manage/taxonomy')">管理分类</RouterLink><button v-if="article" class="english-rw__button" :disabled="busy || (dirty && article.publishStatus!=='PUBLISHED')" @click="status">{{article.publishStatus==='PUBLISHED'?'撤回':'发布'}}</button><button class="english-rw__button english-rw__button--primary" :disabled="busy" @click="save">保存正文</button></div></header>
 <p v-if="error" class="english-rw__error" role="alert">{{error}}</p><p v-if="notice" class="english-rw__notice" role="status">{{notice}}</p><p v-if="loading">正在读取…</p><template v-else>
 <PublicFilterTabs v-model="tab" :options="tabs" label="阅读编辑工作区" />
 <section v-show="tab==='body'"><div class="english-rw__panel"><div class="english-rw__fields"><label>标题 · 可选<input v-model="form.title" maxlength="200" placeholder="Untitled Article"></label><label>摘要 · 可选<textarea v-model="form.summary" maxlength="1000" rows="2"></textarea></label></div><EnglishTaxonomyFields v-model="form" /></div>
 <div class="english-rw__panel"><h2>来源与公开权限</h2><p class="english-rw__muted">外部作品需要先确认公开依据。正文或来源变化后请重新核查。</p><div class="english-rw__fields"><PublicSelect v-model="form.contentOrigin" label="内容来源" :options="[{value:'EXTERNAL',label:'外部作品'},{value:'ORIGINAL',label:'本人原创'}]" /><PublicSelect v-model="form.rights.rightsStatus" label="版权核查状态" :options="[{value:'PENDING',label:'待核查'},{value:'CLEARED',label:'已核查可公开'},{value:'BLOCKED',label:'禁止公开'}]" /><label>来源名称<input v-model="form.sourceName" maxlength="200"></label><label>来源地址<input v-model="form.sourceUrl" type="url" maxlength="500"></label><label>原作者<input v-model="form.rights.originalAuthor" maxlength="200"></label><label>公开依据 · 仅管理端可见<textarea v-model="form.rights.rightsBasis" maxlength="1000" rows="3"></textarea></label><label>读者可见的许可说明<textarea v-model="form.rights.licenseNotice" maxlength="10000" rows="3"></textarea></label></div>
 </div>
 <section class="english-rw__panel"><h2>完整英文正文</h2><MarkdownEditor ref="englishEditor" v-model="form.bodyMarkdown" /></section><section class="english-rw__panel"><h2>完整中文译文 · 可选</h2><MarkdownEditor ref="chineseEditor" v-model="form.translationZhMarkdown" /></section></section>
 <template v-if="tab==='alignments' || tab==='annotations'"><p v-if="dirty" class="english-rw__notice">请先保存正文，再选择片段。正文修改会使已有标注失效。</p><p class="english-rw__muted">{{tab==='alignments'?'在下方预览中依次选择英文、中文片段，可按一对多加入同组。':'在英文预览中选择少量值得精读的片段，填写人工解析。'}}</p>
 <EnglishBilingualProse :english="article.bodyMarkdown || ''" :chinese="article.translationZhMarkdown || ''" :mode="tab==='alignments'?'BOTH':'EN'" :selecting="!dirty" @selection="select" @invalid="error=$event" />
 <div v-if="tab==='alignments'" class="english-rw__panel"><h2>待建立的对齐组</h2><div v-for="(r,i) in selected" :key="i" class="english-rw__row"><span>{{r.language}} · {{r.expectedText}}</span><button class="english-rw__button" @click="selected.splice(i,1)">移除片段</button></div><button class="english-rw__button" :disabled="dirty" @click="addGroup">添加为一个对齐组</button><div v-for="[key,items] in groups" :key="key" class="english-rw__row"><div><p v-for="(r,i) in items" :key="i">{{r.language}} · {{r.expectedText}} <span v-if="r.status==='STALE'" class="english-rw__badge">已失效，请重新选择</span></p></div><button class="english-rw__button" @click="rows.alignments=rows.alignments.filter(r=>r.groupKey!==key)">移除整组</button></div><button class="english-rw__button english-rw__button--primary" :disabled="busy || dirty" @click="saveRows('alignments')">保存所有对齐组</button></div>
 <div v-else class="english-rw__panel"><h2>{{editingNote===null?'添加精选精读':'编辑精选精读'}}</h2><blockquote v-if="noteSelection">{{noteSelection.expectedText}}</blockquote><label>人工解析 · Markdown<textarea v-model="analysis" rows="7" maxlength="200000"></textarea></label><button class="english-rw__button" :disabled="dirty" @click="addNote">确认此条解析</button><div v-for="(r,i) in rows.annotations" :key="i" class="english-rw__row"><div><strong>{{r.expectedText}}</strong><p>{{r.status==='STALE'?'已失效，请重新选择':r.analysisMarkdown}}</p></div><div class="english-rw__actions"><button class="english-rw__button" @click="noteSelection=r;analysis=r.analysisMarkdown;editingNote=i">编辑</button><button class="english-rw__button" @click="rows.annotations.splice(i,1)">移除</button></div></div><button class="english-rw__button english-rw__button--primary" :disabled="busy || dirty" @click="saveRows('annotations')">保存全部精读解析</button></div></template>
 <section v-if="tab==='vocabulary'" class="english-rw__panel"><h2>本文词汇</h2><p class="english-rw__muted">只记录本文的单词、词性和中文释义。同一单词可以添加不同词性。</p><div class="english-rw__fields"><label>单词<input v-model="word.word" maxlength="200"></label><PublicSelect v-model="word.partOfSpeech" label="词性" :options="pos.map(v=>({value:v,label:v}))" /><label>本文中文释义<input v-model="word.meaningZh" maxlength="500"></label></div><button class="english-rw__button" @click="addWord">确认词汇条目</button><div v-for="(r,i) in rows.vocabulary" :key="i" class="english-rw__row"><span><strong>{{r.word}}</strong> · {{r.partOfSpeech}} · {{r.meaningZh}}</span><div class="english-rw__actions"><button class="english-rw__button" @click="word={...r};editingWord=i">编辑</button><button class="english-rw__button" @click="rows.vocabulary.splice(i,1)">移除</button></div></div><button class="english-rw__button english-rw__button--primary" :disabled="busy || dirty" @click="saveRows('vocabulary')">保存全部词汇</button></section>
 <EnglishRevisionPanel v-if="tab==='history'" kind="reading" :id="String(article.id)" :row-version="article.rowVersion" :before-action="async()=>!dirty || await save()" @restored="restored" />
 </template></main></template>
