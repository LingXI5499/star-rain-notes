import { load } from 'cheerio';
export const isDate = value => typeof value==='string' && /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(value)) && new Date(value).toISOString().slice(0,10)===value;
export function validateMeta(meta, label){
  const errors=[];
  for(const key of ['title','description','slug','path'])if(typeof meta[key]!=='string'||!meta[key].trim())errors.push(`${label}: ${key} must be a nonempty string`);
  if(meta.slug&&!/^[a-zA-Z0-9][a-zA-Z0-9_-]*$/.test(meta.slug))errors.push(`${label}: unsafe slug`);
  if(meta.path&&(!meta.path.startsWith('/')||!meta.path.endsWith('/')||meta.path.includes('..')||meta.path.includes('//')))errors.push(`${label}: invalid public path`);
  for(const key of ['publishedAt','updatedAt','startedOn','endedOn'])if(meta[key]&&!isDate(meta[key]))errors.push(`${label}: invalid ${key} (${meta[key]})`);
  return errors;
}
export function validateHTML(html,label){
  const errors=[];const $=load(html,null,false);
  if($('script,iframe,object,embed,form,base').length)errors.push(`${label}: executable or unsupported HTML`);
  $('*').each((_,node)=>{for(const [key,value]of Object.entries(node.attribs||{})){if(/^on/i.test(key))errors.push(`${label}: inline handler ${key}`);if(['href','src','action','xlink:href'].includes(key)&&/^\s*(javascript|vbscript|data):/i.test(value))errors.push(`${label}: unsafe ${key}`)}});
  $('img').each((_,node)=>{const src=$(node).attr('src')||'';if(!src.startsWith('/'))errors.push(`${label}: image must be a local absolute URL (${src})`);if(!$(node).attr('alt'))errors.push(`${label}: image requires alt text (${src})`)});
  const voidTags=new Set(['area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr']);
  for(const match of html.matchAll(/<\/([a-z][a-z0-9]*)\s*>/gi))if(voidTags.has(match[1].toLowerCase()))errors.push(`${label}: invalid closing void element ${match[1]}`);
  return errors;
}
