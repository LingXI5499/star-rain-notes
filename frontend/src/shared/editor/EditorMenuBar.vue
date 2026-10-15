<script setup>
import { computed, nextTick, onBeforeUnmount, ref, useId } from 'vue'
const props=defineProps({ready:Boolean,readOnly:Boolean,canUpload:Boolean,canPick:Boolean,busy:Boolean,fullscreen:Boolean,mode:String,theme:String,headingKeys:Object})
const emit=defineEmits(['action','capture'])
const root=ref(null),popup=ref(null),opened=ref(null),position=ref({}),uid=useId()
const item=(id,label,shortcut='',extra={})=>({id,label,shortcut,...extra})
const groups=computed(()=>[
  {id:'file',label:'文件',items:[item('import','导入 Markdown…','',{edit:true}),item('export-md','导出 Markdown'),item('export-pdf','导出 PDF'),item('export-word','导出 Word'),item('copy-md','复制 Markdown'),item('recovery','本地恢复点')]},
  {id:'edit',label:'编辑',items:[item('native:undo','撤销','Ctrl+Z',{edit:true}),item('native:redo','重做','Ctrl+Y',{edit:true}),item('copy-selection','复制选区 Markdown'),item('diagnostics','质量检查')]},
  {id:'paragraph',label:'段落',items:[...Array.from({length:7},(_,level)=>item(`heading:${level}`,level?`标题 ${level}`:'正文',props.headingKeys?.[`heading${level}`] || `Alt+${level}`,{edit:true})),...['list','ordered-list','check','quote'].map((name,index)=>item(`native:${name}`,['无序列表','有序列表','任务清单','引用'][index],'',{edit:true}))]},
  {id:'format',label:'格式',items:[...['bold','italic','strike','inline-code','link','code','line'].map((name,index)=>item(`native:${name}`,['加粗','斜体','删除线','行内代码','链接','代码块','分隔线'][index],'',{edit:true})),item('math','插入公式','',{edit:true}),item('table','插入表格…','',{edit:true}),item('edit-table','编辑表格…','',{edit:true}),item('code-group','插入代码组…','',{edit:true}),...(props.canUpload?[item('upload','上传图片…','',{edit:true})]:[]),...(props.canPick?[item('library','从媒体库选择图片…','',{edit:true})]:[]),item('image','图片属性…','',{edit:true}),item('media','素材公开检查')]},
  {id:'view',label:'视图',items:[...['ir','sv','wysiwyg'].map((mode,index)=>item(`mode:${mode}`,['即时渲染','源码','所见即所得'][index],'',{checked:props.mode===mode,edit:true})),item('preview','阅读预览'),item('outline','章节管理'),item('fullscreen',props.fullscreen?'退出全屏':'全屏',props.fullscreen?'Esc':'Ctrl+`',{checked:props.fullscreen})]},
  {id:'theme',label:'主题',items:[item('theme:light','浅色主题','',{checked:props.theme!=='dark'}),item('theme:dark','深色主题','',{checked:props.theme==='dark'})]},
  {id:'help',label:'帮助',items:[item('shortcuts','快捷键设置与帮助'),item('export-help','导出说明')]},
])
const active=computed(()=>groups.value.find(group=>group.id===opened.value))
function disabled(value){return !props.ready || (value.edit&&props.readOnly) || (props.busy&&['import','export-md','export-pdf','export-word','copy-md','upload','library'].includes(value.id))}
function groupButton(id){return root.value?.querySelector(`[data-group="${id}"]`)}
function close(focus=false){const id=opened.value;opened.value=null;if(focus)groupButton(id)?.focus({preventScroll:true})}
async function open(id,keyboard=false,last=false){
  emit('capture');opened.value=id
  const rect=groupButton(id).getBoundingClientRect(),height=Math.min((groups.value.find(group=>group.id===id)?.items.length || 0)*32+12,window.innerHeight-16)
  const below=window.innerHeight-rect.bottom-8,top=below>=Math.min(height,240)?rect.bottom+2:Math.max(8,rect.top-height)
  position.value={left:`${Math.max(8,Math.min(rect.left,window.innerWidth-268))}px`,top:`${top}px`,maxHeight:`${Math.max(80,window.innerHeight-top-8)}px`}
  await nextTick()
  if(keyboard){const items=popup.value?.querySelectorAll('button:not(:disabled)');items?.[last?items.length-1:0]?.focus({preventScroll:true})}
}
function toggle(id){if(opened.value===id)close();else void open(id)}
function choose(value){if(disabled(value))return;close();emit('action',value.id)}
function groupKey(event,index){
  if(['ArrowLeft','ArrowRight','Home','End'].includes(event.key)){
    event.preventDefault();const target=event.key==='Home'?0:event.key==='End'?groups.value.length-1:(index+(event.key==='ArrowRight'?1:-1)+groups.value.length)%groups.value.length
    groupButton(groups.value[target].id)?.focus();if(opened.value)void open(groups.value[target].id)
  }else if(['ArrowDown','ArrowUp'].includes(event.key)){event.preventDefault();void open(groups.value[index].id,true,event.key==='ArrowUp')}
  else if(event.key==='Escape' && opened.value){event.preventDefault();event.stopPropagation();close(true)}
}
function menuKey(event){
  const items=[...(popup.value?.querySelectorAll('button:not(:disabled)') || [])],index=items.indexOf(document.activeElement)
  if(['ArrowDown','ArrowUp','Home','End'].includes(event.key)){event.preventDefault();items[event.key==='Home'?0:event.key==='End'?items.length-1:(index+(event.key==='ArrowDown'?1:-1)+items.length)%items.length]?.focus()}
  else if(['ArrowLeft','ArrowRight'].includes(event.key)){event.preventDefault();const index=groups.value.findIndex(group=>group.id===opened.value);void open(groups.value[(index+(event.key==='ArrowRight'?1:-1)+groups.value.length)%groups.value.length].id,true)}
  else if(event.key==='Escape'){event.preventDefault();event.stopPropagation();close(true)}
  else if(event.key==='Tab')close()
}
function outside(event){if(!root.value?.contains(event.target)&&!popup.value?.contains(event.target))close()}
function resized(){close()}
document.addEventListener('pointerdown',outside);window.addEventListener('resize',resized)
onBeforeUnmount(()=>{document.removeEventListener('pointerdown',outside);window.removeEventListener('resize',resized)})
defineExpose({close})
</script>
<template>
  <nav ref="root" class="editor-menubar" role="menubar" aria-label="文档菜单">
    <div class="editor-menubar__groups" @scroll="close()">
      <button v-for="(group,index) in groups" :key="group.id" :data-group="group.id" type="button" role="menuitem" aria-haspopup="menu" :aria-expanded="opened===group.id" :aria-controls="`${uid}-${group.id}`" :disabled="!ready" @pointerdown="emit('capture')" @click="toggle(group.id)" @pointerenter="opened && opened!==group.id && open(group.id)" @keydown="groupKey($event,index)">{{group.label}}</button>
    </div>
    <button type="button" class="editor-menubar__fullscreen" :disabled="!ready" :aria-pressed="fullscreen" @pointerdown.prevent="emit('capture')" @click="emit('action','fullscreen')">{{fullscreen?'退出全屏':'全屏'}}</button>
    <Teleport to="body"><div v-if="active" :id="`${uid}-${active.id}`" ref="popup" class="editor-menubar__menu" role="menu" :aria-label="`${active.label}菜单`" :style="position" @keydown="menuKey"><button v-for="value in active.items" :key="value.id" type="button" :role="value.checked!==undefined?'menuitemradio':'menuitem'" :aria-checked="value.checked" :disabled="disabled(value)" @pointerdown.prevent @click="choose(value)"><span class="editor-menubar__check" aria-hidden="true">{{value.checked?'✓':''}}</span><span>{{value.label}}</span><kbd v-if="value.shortcut">{{value.shortcut}}</kbd></button></div></Teleport>
  </nav>
</template>
<style scoped>
.editor-menubar{display:flex;align-items:center;justify-content:space-between;gap:8px;min-height:36px;border:1px solid var(--border);border-radius:8px 8px 0 0;background:var(--bg-surface);color:var(--text-primary);font:13px/1.4 var(--font-family,system-ui)}
.editor-menubar__groups{display:flex;min-width:0;overflow-x:auto;scrollbar-width:none}.editor-menubar__groups::-webkit-scrollbar{display:none}
button{font:inherit;color:inherit;cursor:pointer;border:0;border-radius:3px;background:transparent;padding:7px 10px;min-height:30px;white-space:nowrap;box-shadow:none;transition:none;transform:none}
button:hover,button[aria-expanded=true]{background:var(--bg-subtle)}button:focus-visible{outline:2px solid var(--primary);outline-offset:-2px}button:disabled{opacity:.45;cursor:default}.editor-menubar__fullscreen{flex-shrink:0;margin-right:4px;color:var(--text-secondary)}
.editor-menubar__menu{position:fixed;z-index:11050;width:min(260px,calc(100vw - 16px));box-sizing:border-box;overflow:auto;padding:5px;border:1px solid var(--border);border-radius:6px;background:var(--bg-surface);color:var(--text-primary);box-shadow:0 8px 24px #0002;font:13px/1.4 var(--font-family,system-ui)}.editor-menubar__menu button{display:flex;align-items:center;gap:6px;width:100%;text-align:left;min-height:32px}.editor-menubar__check{width:14px;flex-shrink:0;color:var(--primary)}kbd{margin-left:auto;color:var(--text-muted);font:11px/1.4 var(--font-family,system-ui)}
@media(max-width:480px){.editor-menubar{gap:2px;font-size:12px}.editor-menubar__groups button{padding-inline:7px}.editor-menubar__fullscreen{padding-inline:6px}}
</style>
