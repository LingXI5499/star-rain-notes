import { renderMarkdownDocument } from '../../../shared/markdown/markdown'
const segmenters=new Map()

// Render each full document through the shared sanitized Markdown pipeline first.
// Split prose after sanitizing the whole document, preserving links and inline formatting.
// Structured blocks (headings, lists, code, tables and math) stay intact.
function paragraphSentences(node, locale) {
  if(node.tagName!=='P' || node.querySelector('code,img,svg,math,.math-source,.katex,mjx-container,audio,video') || typeof Intl.Segmenter!=='function')return [node.outerHTML]
  const text=node.textContent
  const segments=[]
  if(!segmenters.has(locale))segmenters.set(locale,new Intl.Segmenter(locale,{granularity:'sentence'}))
  for(const part of segmenters.get(locale).segment(text)){
    const previous=segments.at(-1)
    // Sentence segmentation treats some English titles and initials as sentence endings.
    if(previous && locale==='en' && (/(?:\b(?:Mr|Mrs|Ms|Dr|Prof|Sr|Jr|St|vs)|\b[A-Z]|\b(?:e\.g|i\.e))\.$/i.test(previous.text.trim()) || (/[?!][”’"']$/.test(previous.text.trim()) && /^[a-z]/.test(part.segment))))previous.text+=part.segment
    else segments.push({start:part.index,text:part.segment})
  }
  if(segments.length<2)return [node.outerHTML]
  const walker=document.createTreeWalker(node,NodeFilter.SHOW_TEXT),texts=[]
  while(walker.nextNode())texts.push(walker.currentNode)
  function point(offset){
    for(const child of texts){if(offset<=child.length)return [child,offset];offset-=child.length}
    return [texts.at(-1),texts.at(-1).length]
  }
  const ids=new Set()
  return segments.filter(part=>part.text.trim()).map(part=>{
    const range=document.createRange(),paragraph=node.cloneNode(false)
    range.setStart(...point(part.start));range.setEnd(...point(part.start+part.text.length))
    let fragment=range.cloneContents(),ancestor=range.commonAncestorContainer
    if(ancestor.nodeType===Node.TEXT_NODE)ancestor=ancestor.parentNode
    while(ancestor!==node){const wrapper=ancestor.cloneNode(false);wrapper.append(fragment);fragment=wrapper;ancestor=ancestor.parentNode}
    paragraph.append(fragment)
    for(const element of [paragraph,...paragraph.querySelectorAll('[id]')]){
      if(!element.id)continue
      if(ids.has(element.id))element.removeAttribute('id');else ids.add(element.id)
    }
    return paragraph.outerHTML
  })
}
export function bilingualDocument(source, namespace) {
  if(!String(source ?? '').trim())return {blocks:[],outline:[]}
  const rendered=renderMarkdownDocument(String(source ?? ''))
  const host=document.createElement('div');host.innerHTML=rendered.html
  host.querySelectorAll('[id]').forEach(node=>{node.id=namespace+node.id})
  host.querySelectorAll('a[href^="#"]').forEach(node=>node.setAttribute('href','#'+namespace+node.getAttribute('href').slice(1)))
  const sandbox=host.firstElementChild?.matches('[data-markdown-sandbox]')?host.firstElementChild:null
  const blocks=Array.from((sandbox || host).children).flatMap(node=>paragraphSentences(node,namespace==='zh-'?'zh':'en'))
  // Keep the renderer's sandbox around every displayed block; never split the wrapper itself.
  return {blocks:sandbox?blocks.map(html=>{const wrapper=sandbox.cloneNode(false);wrapper.innerHTML=html;return wrapper.outerHTML}):blocks,outline:rendered.outline.map(item=>({...item,id:namespace+item.id}))}
}
export function bilingualRows(chinese, english) {
  const zh=bilingualDocument(chinese,'zh-'),en=bilingualDocument(english,'en-')
  return {outline:en.outline,rows:Array.from({length:Math.max(zh.blocks.length,en.blocks.length)},(_,index)=>({chinese:zh.blocks[index] || '',english:en.blocks[index] || ''}))}
}
