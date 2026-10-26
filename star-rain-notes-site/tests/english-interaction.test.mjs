import test from 'node:test';
import assert from 'node:assert/strict';
import {browseURL} from '../src/utils/english-browse-url.ts';
import {installThemeToggle,themeRevealGeometry} from '../src/utils/theme-toggle.ts';

test('changing either category resets pagination while keeping search, page size and language',()=>{
  const previous='https://example.test/english/vocabulary/t02/t02-01/?q=眼睛&page=4&size=12&display=en';
  const primary=browseURL('/english/vocabulary/t01/',previous);
  assert.equal(primary.pathname,'/english/vocabulary/t01/');assert.equal(primary.searchParams.get('page'),null);
  assert.equal(primary.searchParams.get('q'),'眼睛');assert.equal(primary.searchParams.get('size'),'12');assert.equal(primary.searchParams.get('display'),'en');
  const secondary=browseURL('/english/vocabulary/t02/t02-03/',previous);
  assert.equal(secondary.pathname,'/english/vocabulary/t02/t02-03/');assert.equal(secondary.searchParams.get('page'),null);
});

test('theme reveal geometry covers every viewport corner and supports keyboard origin',()=>{
  for(const [width,height,x,y] of [[1440,900,1380,30],[390,844,330,28],[320,700,160,350]]){
    const geometry=themeRevealGeometry(x,y,width,height);
    const radius=parseFloat(geometry.radius)/100*Math.hypot(width,height)/Math.SQRT2;
    for(const [cx,cy] of [[0,0],[width,0],[0,height],[width,height]])assert.ok(radius>Math.hypot(cx-x,cy-y));
    assert.equal(geometry.center,`${(x/width*100).toFixed(4)}% ${(y/height*100).toFixed(4)}%`);
  }
});

async function withThemeEnvironment(options,run){
  const keys=['document','localStorage','matchMedia','innerWidth','innerHeight'];
  const saved=Object.fromEntries(keys.map(k=>[k,Object.getOwnPropertyDescriptor(globalThis,k)]));
  const attrs=new Map(),classes=new Set(),properties=new Map(),listeners={};let persisted,transitionCalls=0;
  const button={title:'',setAttribute:(k,v)=>attrs.set(k,v),removeAttribute:k=>attrs.delete(k),getBoundingClientRect:()=>({left:300,top:8,width:40,height:40}),addEventListener:(name,fn)=>listeners[name]=fn};
  const root={dataset:{theme:'light'},style:{setProperty:(k,v)=>properties.set(k,v),removeProperty:k=>properties.delete(k)},classList:{add:(...xs)=>xs.forEach(x=>classes.add(x)),remove:(...xs)=>xs.forEach(x=>classes.delete(x))}};
  const doc={documentElement:root,querySelector:selector=>selector==='#theme-toggle'?button:{setAttribute(){}}};
  if(options.transition)doc.startViewTransition=callback=>{transitionCalls++;return options.transition(callback);};
  Object.assign(globalThis,{document:doc,localStorage:{setItem:(_,v)=>{if(options.storageError)throw new Error('blocked');persisted=v;}},matchMedia:()=>({matches:!!options.reduced}),innerWidth:390,innerHeight:844});
  try{installThemeToggle();await run({click:event=>listeners.click(event||{detail:0}),root,attrs,classes,properties,get persisted(){return persisted},get transitionCalls(){return transitionCalls}});}
  finally{for(const k of keys){if(saved[k])Object.defineProperty(globalThis,k,saved[k]);else delete globalThis[k];}}
}

test('reduced motion switches immediately and never starts a transition',async()=>{
  await withThemeEnvironment({reduced:true,transition:()=>{throw new Error('must not run');}},async env=>{
    await env.click();assert.equal(env.transitionCalls,0);assert.equal(env.root.dataset.theme,'dark');assert.equal(env.persisted,'dark');assert.equal(env.attrs.get('aria-pressed'),'true');assert.equal(env.classes.size,0);
  });
});
test('unsupported transitions and blocked storage still apply the requested theme',async()=>{
  await withThemeEnvironment({storageError:true},async env=>{await env.click();assert.equal(env.root.dataset.theme,'dark');assert.equal(env.attrs.has('aria-busy'),false);assert.equal(env.classes.size,0);});
});
test('snapshot failures commit only once and clear transition state',async()=>{
  await withThemeEnvironment({transition:callback=>{callback();return {ready:Promise.reject(new Error('snapshot skipped')),finished:Promise.resolve()};}},async env=>{
    await env.click();assert.equal(env.root.dataset.theme,'dark');assert.equal(env.persisted,'dark');assert.equal(env.classes.size,0);assert.equal(env.properties.size,0);
  });
});
test('rapid repeated clicks cannot double-toggle while a reveal is in progress',async()=>{
  let finish;
  await withThemeEnvironment({transition:callback=>{callback();return {ready:Promise.resolve(),finished:new Promise(resolve=>finish=resolve)};}},async env=>{
    const first=env.click({detail:1,clientX:320,clientY:28});await env.click({detail:1,clientX:320,clientY:28});
    assert.equal(env.transitionCalls,1);assert.equal(env.root.dataset.theme,'dark');assert.ok(env.classes.has('is-theme-revealing'));
    finish();await first;assert.equal(env.classes.size,0);assert.equal(env.attrs.has('aria-busy'),false);
    const second=env.click();finish();await second;assert.equal(env.root.dataset.theme,'light');
  });
});
