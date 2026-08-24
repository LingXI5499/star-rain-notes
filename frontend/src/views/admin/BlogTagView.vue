<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import { createTag, deleteTag, fetchAdminTags, updateTag, type AdminBlogTag } from '@/api/blog'

const tags = ref<AdminBlogTag[]>([])
const loading = ref(true)
const search = ref('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', slug: '' })
const filtered = computed(() => { const q=search.value.trim().toLocaleLowerCase(); return q?tags.value.filter(t=>t.name.toLocaleLowerCase().includes(q)||t.slug.includes(q)):tags.value })
const totalRelations = computed(()=>tags.value.reduce((sum,tag)=>sum+tag.postCount,0))

async function load(){loading.value=true;try{tags.value=await fetchAdminTags()}catch{ElMessage.error('加载标签失败。')}finally{loading.value=false}}
function slugSuggestion(name:string){return name.normalize('NFKC').toLocaleLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-+|-+$/g,'').slice(0,60)}
watch(()=>form.name,(name)=>{if(editingId.value===null&&!form.slug)form.slug=slugSuggestion(name)})
function openCreate(){editingId.value=null;form.name='';form.slug='';dialogVisible.value=true}
function openEdit(tag:AdminBlogTag){editingId.value=tag.id;form.name=tag.name;form.slug=tag.slug;dialogVisible.value=true}
async function save(){if(!form.name.trim()||!form.slug.trim()){ElMessage.warning('请填写名称与 slug。');return}try{if(editingId.value===null){await createTag({name:form.name,slug:form.slug});ElMessage.success('标签已创建。')}else{await updateTag(editingId.value,{name:form.name,slug:form.slug});ElMessage.success('标签已保存。')}dialogVisible.value=false;await load()}catch(error){const problem=error instanceof AxiosError?(error.response?.data as ProblemDetail|undefined):undefined;ElMessage.error(problem?.detail??'保存失败。')}}
async function remove(tag:AdminBlogTag){try{const inUse=tag.postCount>0;await ElMessageBox.confirm(inUse?`标签「${tag.name}」正在被 ${tag.postCount} 篇文章使用。继续将解除这些关联并删除标签，确定吗？`:`确定删除标签「${tag.name}」？`,inUse?'删除使用中的标签':'删除确认',{type:'warning',confirmButtonText:inUse?'解除关联并删除':'删除'});await deleteTag(tag.id,inUse);ElMessage.success('标签已删除。');await load()}catch{/* cancel or failure */}}
onMounted(load)
</script>

<template>
  <section class="tag-admin">
    <header class="tag-admin__hero"><div><p>TAG LIBRARY · 内容索引</p><h1>博客标签</h1><span>统一维护标签命名、链接与文章关联。</span></div><el-button type="primary" @click="openCreate">＋ 新建标签</el-button></header>
    <div class="tag-admin__stats"><div><strong>{{tags.length}}</strong><span>标签总数</span></div><div><strong>{{totalRelations}}</strong><span>文章关联</span></div><el-input v-model="search" placeholder="搜索名称或 slug" clearable /></div>
    <div v-loading="loading" class="tag-admin__grid">
      <article v-for="tag in filtered" :key="tag.id" class="tag-card"><div class="tag-card__top"><span>#</span><strong>{{tag.name}}</strong><em>{{tag.postCount}}</em></div><code>{{tag.slug}}</code><footer><span>{{tag.postCount?'已关联文章':'暂未使用'}}</span><div><button @click="openEdit(tag)">编辑</button><button class="danger" @click="remove(tag)">删除</button></div></footer></article>
      <div v-if="!loading&&!filtered.length" class="tag-admin__empty">{{search?'没有匹配的标签':'暂无标签'}}</div>
    </div>
    <el-dialog v-model="dialogVisible" :title="editingId===null?'新建标签':'编辑标签'" width="440px"><el-form label-position="top" @submit.prevent="save"><el-form-item label="名称"><el-input v-model="form.name" maxlength="50" placeholder="例如：Spring Boot"/></el-form-item><el-form-item label="Slug（小写 kebab-case）"><el-input v-model="form.slug" maxlength="60" placeholder="spring-boot"/></el-form-item></el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog>
  </section>
</template>

<style scoped>
.tag-admin__hero{display:flex;align-items:end;justify-content:space-between;gap:var(--space-5);margin-bottom:var(--space-6)}.tag-admin__hero p{margin-bottom:8px;color:var(--accent);font-size:10px;font-weight:800;letter-spacing:.16em}.tag-admin__hero h1{margin-bottom:5px;font-size:34px}.tag-admin__hero span{color:var(--text-muted);font-size:13px}.tag-admin__stats{display:grid;grid-template-columns:150px 150px minmax(220px,1fr);gap:var(--space-3);margin-bottom:var(--space-5)}.tag-admin__stats>div{display:flex;align-items:baseline;gap:10px;padding:13px 16px;border:1px solid var(--border);border-radius:15px;background:var(--bg-surface)}.tag-admin__stats strong{color:var(--primary);font-size:24px}.tag-admin__stats span{color:var(--text-muted);font-size:11px}.tag-admin__grid{min-height:150px;display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:var(--space-4)}.tag-card{padding:var(--space-5);border:1px solid var(--border);border-radius:17px;background:var(--bg-surface);transition:transform 170ms ease,border-color 170ms ease,box-shadow 170ms ease}.tag-card:hover{border-color:color-mix(in srgb,var(--primary) 38%,var(--border));transform:translateY(-2px);box-shadow:0 13px 28px rgb(0 0 0/.055)}.tag-card__top{display:grid;grid-template-columns:28px minmax(0,1fr) auto;align-items:center;gap:8px}.tag-card__top>span{display:grid;width:28px;height:28px;place-items:center;border-radius:9px;color:var(--primary);background:color-mix(in srgb,var(--primary) 11%,transparent);font-weight:800}.tag-card__top strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.tag-card__top em{min-width:28px;padding:4px 7px;border-radius:999px;color:var(--text-muted);background:var(--bg-subtle);font-size:10px;font-style:normal;text-align:center}.tag-card code{display:block;overflow:hidden;margin:13px 0;padding:8px 10px;border-radius:9px;color:var(--accent);background:var(--bg-subtle);font-size:11px;text-overflow:ellipsis;white-space:nowrap}.tag-card footer{display:flex;align-items:center;justify-content:space-between;gap:var(--space-3);color:var(--text-muted);font-size:11px}.tag-card footer div{display:flex;gap:5px}.tag-card button{padding:5px 9px;border:1px solid var(--border);border-radius:999px;color:var(--text-secondary);background:transparent;cursor:pointer}.tag-card button:hover{border-color:var(--primary);color:var(--primary)}.tag-card button.danger:hover{border-color:var(--danger);color:var(--danger)}.tag-admin__empty{grid-column:1/-1;padding:var(--space-9);border:1px dashed var(--border-strong);border-radius:16px;color:var(--text-muted);text-align:center}@media(prefers-reduced-motion:reduce){.tag-card{transition:none}}@media(max-width:650px){.tag-admin__hero{align-items:flex-start}.tag-admin__stats{grid-template-columns:1fr 1fr}.tag-admin__stats>.el-input{grid-column:1/-1}}
</style>
