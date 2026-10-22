import {initStaticFilters} from './filter';
import {browseURL} from './english-browse-url';

export function initWordDisplays(root:ParentNode=document){
  const requested=new URLSearchParams(location.search).get('display');
  const display=requested==='en'||requested==='zh'?requested:'all';
  root.querySelectorAll<HTMLElement>('.english-word-list').forEach(scope=>{
    scope.querySelectorAll<HTMLButtonElement>('[data-word-display]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.wordDisplay===display)));
    scope.querySelectorAll<HTMLElement>('.vocabulary-card__english').forEach(el=>el.hidden=display==='zh');
    scope.querySelectorAll<HTMLElement>('.vocabulary-card__chinese').forEach(el=>el.hidden=display==='en');
  });
}

export function installEnglishBrowsing(){
  const cache=new Map<string,string>();
  let pending:AbortController|undefined;
  const enhance=(root:ParentNode=document)=>{
    root.querySelectorAll<HTMLElement>('[data-english-filters]').forEach(el=>el.dataset.enhanced='true');
    initStaticFilters(root);initWordDisplays(root);
  };
  async function navigate(url:URL,push=true,focus?:'link'|'primary'|'secondary'){
    const current=document.querySelector<HTMLElement>('[data-english-page]');
    if(!current)return;
    pending?.abort();const controller=new AbortController();pending=controller;
    current.setAttribute('aria-busy','true');
    const status=current.querySelector('[data-english-navigation-status]');if(status)status.textContent='正在更新筛选结果…';
    try{
      let html=cache.get(url.pathname);
      if(!html){
        const response=await fetch(url.pathname,{signal:controller.signal});
        if(!response.ok)throw new Error('Static page unavailable');
        html=await response.text();cache.set(url.pathname,html);
        if(cache.size>6)cache.delete(cache.keys().next().value!);
      }
      if(controller.signal.aborted)return;
      const parsed=new DOMParser().parseFromString(html,'text/html');
      const next=parsed.querySelector<HTMLElement>('[data-english-page]');
      if(!next||next.dataset.englishPage!==current.dataset.englishPage)throw new Error('Unexpected category page');
      const filterTop=current.querySelector('[data-english-filters]')?.getBoundingClientRect().top;
      if(push)history.pushState(null,'',url);
      current.replaceWith(next);
      document.title=parsed.title;
      for(const selector of ['meta[name="description"]','meta[name="robots"]','link[rel="canonical"]','meta[property="og:title"]','meta[property="og:description"]','meta[property="og:url"]']){
        const fresh=parsed.head.querySelector(selector),existing=document.head.querySelector(selector);if(fresh&&existing)existing.replaceWith(fresh);
      }
      document.head.querySelectorAll('script[type="application/ld+json"]').forEach(el=>el.remove());
      parsed.head.querySelectorAll('script[type="application/ld+json"]').forEach(el=>document.head.append(el));
      enhance(next);
      if(focus==='link'){
        [...next.querySelectorAll<HTMLAnchorElement>('[data-english-filter]')].find(a=>new URL(a.href).pathname===url.pathname)?.focus({preventScroll:true});
      }else if(focus)next.querySelector<HTMLSelectElement>(`[data-english-${focus}]`)?.focus({preventScroll:true});
      if(push&&filterTop!==undefined&&filterTop<70){
        next.querySelector('[data-english-filters]')?.scrollIntoView({block:'start',behavior:'instant'});
      }
      const announcement=next.querySelector('[data-english-navigation-status]');
      if(announcement)announcement.textContent=`已显示${next.querySelector('.english-results-title')?.textContent||'筛选结果'}`;
    }catch{
      if(!controller.signal.aborted)location.assign(url.href);
    }finally{
      if(!controller.signal.aborted)document.querySelector('[data-english-page]')?.removeAttribute('aria-busy');
    }
  }
  document.addEventListener('click',event=>{
    const target=event.target instanceof Element?event.target:null;
    const display=target?.closest<HTMLButtonElement>('[data-word-display]');
    if(display){
      const url=new URL(location.href);if(display.dataset.wordDisplay==='all')url.searchParams.delete('display');else url.searchParams.set('display',display.dataset.wordDisplay!);
      history.replaceState(null,'',url);initWordDisplays();return;
    }
    const link=target?.closest<HTMLAnchorElement>('a[data-english-filter]');
    if(!link||event.button!==0||event.ctrlKey||event.metaKey||event.shiftKey||event.altKey)return;
    const url=browseURL(link.href,location.href);if(url.origin!==location.origin)return;
    event.preventDefault();void navigate(url,true,event.detail===0?'link':undefined);
  });
  document.addEventListener('change',event=>{
    const select=event.target;
    if(select instanceof HTMLSelectElement&&select.matches('[data-english-primary],[data-english-secondary]'))void navigate(browseURL(select.value,location.href),true,select.hasAttribute('data-english-primary')?'primary':'secondary');
  });
  window.addEventListener('popstate',()=>{
    const page=document.querySelector<HTMLElement>('[data-english-page]');
    if(page&&location.pathname.startsWith(`/english/${page.dataset.englishPage}/`))void navigate(new URL(location.href),false);
  });
  enhance();
}
