export function initStaticFilters(root:ParentNode=document){
root.querySelectorAll<HTMLElement>('[data-filter-scope]').forEach(scope=>{
  if(scope.dataset.filterReady)return;
  scope.dataset.filterReady='true';
  const input=scope.querySelector<HTMLInputElement>('[data-filter-query]');
  const buttons=[...scope.querySelectorAll<HTMLButtonElement>('[data-filter]')];
  const cards=[...scope.querySelectorAll<HTMLElement>('[data-filter-item]')];
  const tagButtons=[...scope.querySelectorAll<HTMLButtonElement>('[data-filter-tag]')];
  const monthButtons=[...scope.querySelectorAll<HTMLButtonElement>('[data-filter-month]')];
  const tagsByCard=new Map(cards.map(card=>[card,JSON.parse(card.dataset.filterTags||'[]') as string[]]));
  const paginated=scope.hasAttribute('data-static-pagination');
  const nav=scope.querySelector<HTMLElement>('.public-pagination');
  const en=nav?.dataset.pageLocale==='en';
  const sizeSelect=scope.querySelector<HTMLSelectElement>('[data-page-size]');
  const params=new URLSearchParams(location.search);
  let category=buttons.find(b=>b.dataset.categorySlug===params.get('categorySlug'))?.dataset.filter||'全部';
  const defaultSize=Number(scope.dataset.paginationSize||12);
  const sizes=sizeSelect?[...sizeSelect.options].map(o=>Number(o.value)):[defaultSize];
  let size=sizes.includes(Number(params.get('size')))?Number(params.get('size')):defaultSize;
  const requestedPage=Number(params.get('page')||scope.dataset.paginationPage||1);
  let page=Number.isSafeInteger(requestedPage)&&requestedPage>0?requestedPage:1;
  if(sizeSelect)sizeSelect.value=String(size);
  let dateFilter=params.get('month')||location.hash.match(/^#month-(\d{4}-\d{2})$/)?.[1]||'';
  let tagFilter=tagButtons.length?params.get('tag')||'':'';
  if(input)input.value=params.get('q')||'';
  function updateURL(){const url=new URL(location.href);const selected=buttons.find(b=>b.dataset.filter===category);if(selected?.dataset.categorySlug)url.searchParams.set('categorySlug',selected.dataset.categorySlug);else url.searchParams.delete('categorySlug');if(input?.value)url.searchParams.set('q',input.value);else url.searchParams.delete('q');if(page>1)url.searchParams.set('page',String(page));else url.searchParams.delete('page');if(size!==defaultSize)url.searchParams.set('size',String(size));else url.searchParams.delete('size');if(tagFilter)url.searchParams.set('tag',tagFilter);else url.searchParams.delete('tag');if(dateFilter)url.searchParams.set('month',dateFilter);else url.searchParams.delete('month');history.replaceState(null,'',url)}
  function render(){
    const matches=cards.filter(card=>(!tagFilter||tagsByCard.get(card)?.includes(tagFilter))&&(!dateFilter||card.dataset.publishedAt?.startsWith(dateFilter))&&(category==='全部'||card.dataset.category===category)&&(!input?.value||(card.dataset.filterText??card.textContent)?.toLowerCase().includes(input.value.toLowerCase())));
    const pages=Math.max(1,Math.ceil(matches.length/size));page=Math.min(pages,Math.max(1,page));
    const visible=new Set(paginated?matches.slice((page-1)*size,page*size):matches);
    cards.forEach(card=>card.hidden=!visible.has(card));
    let previousYear='';
    cards.filter(card=>visible.has(card)).forEach(card=>{const year=card.querySelector<HTMLElement>('[data-timeline-year]');if(year){year.hidden=year.textContent===previousYear;previousYear=year.textContent||'';}});
    tagButtons.forEach(button=>{const active=button.dataset.filterTag===tagFilter;button.classList.toggle('is-active',active);button.setAttribute('aria-pressed',String(active));});
    monthButtons.forEach(button=>{const active=button.dataset.filterMonth===dateFilter;button.classList.toggle('is-active',active);button.setAttribute('aria-pressed',String(active));});
    buttons.forEach(b=>{const active=b.dataset.filter===category;b.setAttribute('aria-pressed',String(active));b.classList.toggle('tutorial-catalog__all--active',active&&b.classList.contains('tutorial-catalog__all'));b.classList.toggle('tutorial-catalog__category--active',active&&b.classList.contains('tutorial-catalog__category'));b.classList.toggle('is-active',active)});
    scope.querySelectorAll('[data-category-heading]').forEach(el=>el.textContent=category==='全部'?'全部教程':category);
    scope.querySelectorAll('[data-category-total]').forEach(el=>el.textContent=String(cards.filter(card=>category==='全部'||card.dataset.category===category).length));
    const unit=en&&scope.dataset.paginationUnit==='essays'&&matches.length===1?'essay':scope.dataset.paginationUnit;
    const status=scope.querySelector('[data-filter-status]');if(status)status.textContent=`${matches.length} ${scope.classList.contains('tutorial-catalog')?'门教程':unit||'项内容'}`;
    const empty=scope.querySelector<HTMLElement>('[data-filter-empty]');if(empty)empty.hidden=matches.length>0;
    const reset=scope.querySelector<HTMLElement>('[data-filter-reset]');if(reset)reset.hidden=category==='全部'&&!input?.value&&!tagFilter&&!dateFilter;
    scope.querySelectorAll<HTMLElement>('[data-filter-group]').forEach(group=>group.hidden=![...group.querySelectorAll<HTMLElement>('[data-filter-item]')].some(card=>!card.hidden));
    if(nav){const range=nav.querySelector('[data-page-range]');if(range)range.textContent=`${matches.length?(page-1)*size+1:0}–${Math.min(page*size,matches.length)} ${en?'of':'/ 共'} ${matches.length.toLocaleString()} ${unit||'条'}`;
      const prev=nav.querySelector<HTMLButtonElement>('[data-page-previous]'),next=nav.querySelector<HTMLButtonElement>('[data-page-next]');if(prev)prev.disabled=page<=1;if(next)next.disabled=page>=pages;
      nav.querySelectorAll('[data-page-number],.public-pagination__gap').forEach(n=>n.remove());const pagesNode=nav.querySelector('.public-pagination__pages');
      const numbers=[...new Set([1,page-1,page,page+1,pages])].filter(n=>n>0&&n<=pages).sort((a,b)=>a-b);let last=0;
      numbers.forEach(n=>{if(n-last>1){const gap=document.createElement('span');gap.className='public-pagination__gap';gap.textContent='…';pagesNode?.insertBefore(gap,next||null)}const b=document.createElement('button');b.type='button';b.dataset.pageNumber=String(n);b.textContent=String(n);b.setAttribute('aria-label',en?`Page ${n}`:`第 ${n} 页`);if(n===page)b.setAttribute('aria-current','page');pagesNode?.insertBefore(b,next||null);last=n});
    }
  }
  buttons.forEach(b=>b.addEventListener('click',()=>{category=b.dataset.filter||'全部';page=1;render();updateURL()}));
  tagButtons.forEach(button=>button.addEventListener('click',()=>{tagFilter=tagFilter===button.dataset.filterTag?'':button.dataset.filterTag||'';page=1;render();updateURL()}));
  monthButtons.forEach(button=>button.addEventListener('click',()=>{dateFilter=dateFilter===button.dataset.filterMonth?'':button.dataset.filterMonth||'';page=1;render();updateURL()}));
  input?.addEventListener('input',()=>{page=1;render();updateURL()});
  scope.querySelector('[data-filter-reset]')?.addEventListener('click',()=>{category='全部';tagFilter='';dateFilter='';page=1;if(input)input.value='';render();updateURL()});
  nav?.addEventListener('click',event=>{const target=(event.target as Element).closest<HTMLButtonElement>('button');if(!target||target.disabled)return;if(target.hasAttribute('data-page-next'))page++;else if(target.hasAttribute('data-page-previous'))page--;else if(target.dataset.pageNumber)page=Number(target.dataset.pageNumber);else return;render();updateURL();if(scope.closest('[data-english-page]')||scope.classList.contains('essays-page'))scope.querySelector('[data-filter-status]')?.scrollIntoView({block:'start',behavior:'instant'});});
  sizeSelect?.addEventListener('change',()=>{size=Number(sizeSelect.value);page=1;render();updateURL()});
  scope.addEventListener('static-date-filter',event=>{dateFilter=String((event as CustomEvent<string>).detail||'');page=1;render();updateURL()});
  render();
});
}
initStaticFilters();
