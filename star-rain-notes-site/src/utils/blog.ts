import { blogFilterURL, matchesBlogFilters, readBlogFilters } from './blog-filter-state';

document.querySelectorAll<HTMLElement>('[data-blog-filter]').forEach(scope => {
  const topicLinks = [...scope.querySelectorAll<HTMLAnchorElement>('[data-blog-topic]')];
  const tagLinks = [...scope.querySelectorAll<HTMLAnchorElement>('[data-blog-tag]')];
  const topicNames = new Map(topicLinks.map(a => [a.dataset.blogTopic!, a.textContent!.trim()]));
  const tagNames = new Map(tagLinks.map(a => [a.dataset.blogTag!, a.dataset.tagName!]));
  const posts = [...scope.querySelectorAll<HTMLElement>('[data-blog-post]')].map(element => ({
    element, tags: element.dataset.tags!.split(','), topics: element.dataset.topics!.split(','), publishedAt: element.dataset.publishedAt!,
  }));
  const readState = () => readBlogFilters(new URL(location.href), [...topicNames.keys()], [...tagNames.keys()]);
  let state = readState();
  const pagination = scope.querySelector<HTMLElement>('.public-pagination')!;
  const pageButtons = pagination.querySelector<HTMLElement>('.public-pagination__pages')!;
  const previous = pagination.querySelector<HTMLButtonElement>('[data-page-previous]')!;
  const next = pagination.querySelector<HTMLButtonElement>('[data-page-next]')!;
  const sizeSelect = pagination.querySelector<HTMLSelectElement>('[data-page-size]')!;
  const activePanel = scope.querySelector<HTMLElement>('[data-blog-active]')!;
  const activeChips = scope.querySelector<HTMLElement>('[data-blog-active-chips]')!;
  const columnNav = scope.querySelector<HTMLElement>('[data-blog-columns]')!;
  const row = columnNav.querySelector<HTMLElement>('.column-nav__row')!;
  const rowLinks = [...row.querySelectorAll<HTMLAnchorElement>(':scope > [data-blog-topic]')];
  const more = columnNav.querySelector<HTMLElement>('.column-nav__more')!;
  const moreButton = columnNav.querySelector<HTMLButtonElement>('[data-column-more]')!;
  const menu = columnNav.querySelector<HTMLElement>('.column-nav__menu')!;
  const pill = columnNav.querySelector<HTMLElement>('.column-nav__pill')!;

  function closeMenu() { menu.hidden = true; moreButton.setAttribute('aria-expanded', 'false'); moreButton.classList.remove('column-nav__more-trigger--open'); }
  function placePill() {
    const active = rowLinks.find(a => !a.hidden && a.dataset.blogTopic === state.topic);
    pill.hidden = !active;
    if (active) Object.assign(pill.style, { width: `${active.offsetWidth}px`, height: `${active.offsetHeight}px`, transform: `translate(${active.offsetLeft}px, ${active.offsetTop}px)` });
    moreButton.classList.toggle('column-nav__more-trigger--active', !active);
  }
  function measureColumns() {
    if (columnNav.hidden) return;
    const widths = [...columnNav.querySelectorAll<HTMLElement>('[data-probe-item]')].map(el => el.getBoundingClientRect().width);
    const available = row.getBoundingClientRect().width;
    const moreWidth = columnNav.querySelector<HTMLElement>('[data-probe-more]')!.getBoundingClientRect().width;
    const fitsAll = widths.reduce((sum, width) => sum + width, 0) + Math.max(0, widths.length - 1) * 8 <= available;
    let used = 0, count = 0;
    rowLinks.forEach((a, index) => {
      const needed = used + widths[index] + (count ? 8 : 0);
      const visible = fitsAll || needed + moreWidth + 8 <= available || index === 0;
      a.hidden = !visible;
      if (visible) { used = needed; count++; }
      const item = [...menu.querySelectorAll<HTMLElement>('[data-column-menu-item]')].find(el => el.dataset.columnMenuItem === a.dataset.blogTopic);
      if (item) item.hidden = visible;
    });
    more.hidden = fitsAll;
    if (fitsAll) closeMenu();
    columnNav.dataset.ready = 'true';
    placePill();
  }

  function chip(label: string, remove: () => void) {
    const button = document.createElement('button'); button.type = 'button'; button.textContent = `${label} ×`;
    button.setAttribute('aria-label', `移除筛选：${label}`); button.addEventListener('click', () => { remove(); state.page = 1; commit(); }); activeChips.append(button);
  }
  function render() {
    const matches = posts.filter(post => matchesBlogFilters(post, state));
    const pages = Math.max(1, Math.ceil(matches.length / state.size));
    state.page = Math.min(pages, Math.max(1, state.page));
    const visible = new Set(matches.slice((state.page - 1) * state.size, state.page * state.size));
    let previousYear = '';
    posts.forEach(post => {
      post.element.hidden = !visible.has(post);
      if (!post.element.hidden) { const year = post.publishedAt.slice(0, 4); post.element.querySelector<HTMLElement>('[data-blog-year]')!.hidden = year === previousYear; previousYear = year; }
    });
    topicLinks.forEach(a => { const active = a.dataset.blogTopic === state.topic; a.classList.toggle(a.closest('.column-nav__menu') ? 'column-nav__menu-link--active' : 'column-nav__item--active', active); if (active) a.setAttribute('aria-current', 'true'); else a.removeAttribute('aria-current'); });
    tagLinks.forEach(a => { const active = state.tags.includes(a.dataset.blogTag!); a.classList.toggle('active', active); if (active) a.setAttribute('aria-current', 'true'); else a.removeAttribute('aria-current'); });
    scope.querySelectorAll<HTMLAnchorElement>('[data-blog-month]').forEach(a => a.classList.toggle('active', state.month.startsWith(a.dataset.blogMonth!)));
    activePanel.hidden = !state.topic && !state.tags.length && !state.month;
    activeChips.replaceChildren();
    if (state.topic) chip(topicNames.get(state.topic)!, () => state.topic = '');
    state.tags.forEach(tag => chip(tagNames.get(tag)!, () => state.tags = state.tags.filter(t => t !== tag)));
    if (state.month) chip(state.month, () => state.month = '');
    scope.querySelector('[data-blog-result-count]')!.textContent = `${matches.length} 篇文章`;
    scope.querySelector<HTMLElement>('[data-blog-empty]')!.hidden = matches.length > 0;
    scope.querySelector('[data-blog-empty-text]')!.textContent = state.topic && !state.tags.length && !state.month ? '该专栏暂无公开文章。' : '没有符合当前筛选条件的文章，可以移除部分条件后重试。';
    pagination.querySelector('[data-page-range]')!.textContent = `${matches.length ? (state.page - 1) * state.size + 1 : 0}–${Math.min(state.page * state.size, matches.length)} / 共 ${matches.length} 篇`;
    previous.disabled = state.page <= 1; next.disabled = state.page >= pages; sizeSelect.value = String(state.size);
    pageButtons.querySelectorAll('[data-page-number],.public-pagination__gap').forEach(el => el.remove());
    const numbers = [...new Set([1, state.page - 1, state.page, state.page + 1, pages])].filter(n => n > 0 && n <= pages).sort((a, b) => a - b);
    let last = 0;
    numbers.forEach(n => {
      if (n - last > 1) { const gap = document.createElement('span'); gap.className = 'public-pagination__gap'; gap.textContent = '…'; pageButtons.insertBefore(gap, next); }
      const button = document.createElement('button'); button.type = 'button'; button.dataset.pageNumber = String(n); button.textContent = String(n); button.setAttribute('aria-label', `第 ${n} 页`); if (n === state.page) button.setAttribute('aria-current', 'page'); pageButtons.insertBefore(button, next); last = n;
    });
    syncCalendar(); placePill(); syncHeading();
  }
  function syncHeading() {
    const heading = scope.querySelector('h1')!;
    if (location.pathname.startsWith('/blog/topics/') && state.topic) heading.textContent = topicNames.get(state.topic)!;
    else if (location.pathname.startsWith('/blog/tags/') && state.tags.length === 1) heading.textContent = tagNames.get(state.tags[0])!;
    else if (location.pathname.startsWith('/blog/archive')) heading.textContent = '归档浏览';
    else heading.replaceChildren(document.createTextNode('沿时间沉淀思考，'), document.createElement('br'), document.createTextNode('让经验持续生长。'));
    document.title = `${state.topic ? topicNames.get(state.topic) + '专栏' : state.tags.length ? state.tags.map(tag => tagNames.get(tag)).join('、') + '相关文章' : location.pathname.startsWith('/blog/archive') ? '文章时间归档' : '技术博客'} · 星雨笔录`;
    const canonical = document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (canonical) { const target = new URL(canonical.href); target.pathname = location.pathname; target.search = ''; canonical.href = target.href; }
  }
  function commit() {
    render();
    const url = blogFilterURL(new URL(location.href), state);
    if (url.href !== location.href) history.pushState(null, '', url);
    syncHeading();
  }
  function intercept(event: MouseEvent) { return !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey && event.button === 0; }
  topicLinks.forEach(a => a.addEventListener('click', event => { if (!intercept(event)) return; event.preventDefault(); state.topic = a.dataset.blogTopic!; state.page = 1; closeMenu(); commit(); }));
  tagLinks.forEach(a => a.addEventListener('click', event => { if (!intercept(event)) return; event.preventDefault(); const tag = a.dataset.blogTag!; state.tags = state.tags.length === 1 && state.tags[0] === tag ? [] : [tag]; state.page = 1; commit(); }));
  scope.querySelectorAll<HTMLAnchorElement>('[data-blog-month]').forEach(a => a.addEventListener('click', event => { if (!intercept(event)) return; event.preventDefault(); state.month = state.month === a.dataset.blogMonth ? '' : a.dataset.blogMonth!; state.page = 1; commit(); }));
  scope.querySelectorAll('[data-blog-reset]').forEach(button => button.addEventListener('click', () => { state = { topic: '', tags: [], month: '', page: 1, size: state.size }; commit(); }));
  pagination.addEventListener('click', event => { const b = (event.target as Element).closest<HTMLButtonElement>('button'); if (!b || b.disabled) return; if (b.hasAttribute('data-page-next')) state.page++; else if (b.hasAttribute('data-page-previous')) state.page--; else if (b.dataset.pageNumber) state.page = Number(b.dataset.pageNumber); else return; commit(); });
  sizeSelect.addEventListener('change', () => { state.size = Number(sizeSelect.value); state.page = 1; commit(); });
  moreButton.addEventListener('click', () => { menu.hidden = !menu.hidden; moreButton.setAttribute('aria-expanded', String(!menu.hidden)); moreButton.classList.toggle('column-nav__more-trigger--open', !menu.hidden); });
  document.addEventListener('click', event => { if (!columnNav.contains(event.target as Node)) closeMenu(); });
  document.addEventListener('keydown', event => { if (event.key === 'Escape' && !menu.hidden) { closeMenu(); moreButton.focus(); } });
  window.addEventListener('popstate', () => { state = readState(); render(); });
  scope.addEventListener('static-date-filter', event => { state.month = String((event as CustomEvent<string>).detail || ''); state.page = 1; commit(); });
  function syncCalendar() {
    const month = state.month.slice(0, 7);
    const select = scope.querySelector<HTMLSelectElement>('[data-calendar-select]');
    if (select && month && [...select.options].some(o => o.value === month)) {
      select.value = month;
      scope.querySelectorAll<HTMLElement>('[data-calendar-grid]').forEach(grid => grid.hidden = grid.dataset.calendarGrid !== month);
      scope.querySelector('[data-calendar-title]')!.textContent = `${month.slice(0, 4)} 年 ${Number(month.slice(5))} 月`;
    }
    scope.querySelectorAll<HTMLButtonElement>('[data-calendar-day]').forEach(b => { const selected = b.dataset.calendarDay === state.month; b.classList.toggle('is-selected', selected); b.setAttribute('aria-pressed', String(selected)); });
  }

  const overlay = scope.querySelector<HTMLElement>('[data-tag-overlay]')!;
  const search = overlay.querySelector<HTMLInputElement>('[data-tag-search]')!;
  const options = [...overlay.querySelectorAll<HTMLButtonElement>('[data-tag-option]')];
  const modes = [...overlay.querySelectorAll<HTMLButtonElement>('[data-tag-mode]')];
  let picked: string[] = [], mode = 'single', returnFocus: HTMLElement | null = null, previousOverflow = '';
  function renderOverlay() {
    const term = search.value.trim().toLocaleLowerCase(); let matched = 0;
    options.forEach(button => {
      button.hidden = !button.dataset.tagName!.toLocaleLowerCase().includes(term); if (!button.hidden) matched++;
      const active = picked.includes(button.dataset.tagOption!); button.classList.toggle('is-picked', active); button.setAttribute('aria-pressed', String(active)); button.querySelector('.tag-overlay__mark')!.textContent = active ? '✓' : '';
    });
    modes.forEach(button => { const active = button.dataset.tagMode === mode; button.classList.toggle('is-active', active); button.setAttribute('aria-pressed', String(active)); });
    overlay.querySelector('[data-tag-search-count]')!.textContent = `${matched} / ${options.length}`;
    overlay.querySelector<HTMLElement>('[data-tag-search-empty]')!.hidden = matched > 0;
    const pickedNode = overlay.querySelector('[data-tag-picked]')!; pickedNode.replaceChildren();
    const label = document.createElement('span'); label.textContent = picked.length ? `已选 ${picked.length} 个：` : '未选择标签，应用后取消标签筛选。'; pickedNode.append(label);
    picked.forEach(tag => { const button = document.createElement('button'); button.type = 'button'; button.className = 'tag-overlay__chip'; button.textContent = `${tagNames.get(tag)} ×`; button.setAttribute('aria-label', `取消选择 ${tagNames.get(tag)}`); button.addEventListener('click', () => { picked = picked.filter(t => t !== tag); renderOverlay(); }); pickedNode.append(button); });
  }
  function closeOverlay() { overlay.hidden = true; document.body.style.overflow = previousOverflow; returnFocus?.focus({ preventScroll: true }); }
  scope.querySelector('[data-open-tag-list]')!.addEventListener('click', () => { picked = [...state.tags]; mode = picked.length > 1 ? 'multi' : 'single'; search.value = ''; returnFocus = document.activeElement as HTMLElement; previousOverflow = document.body.style.overflow; overlay.hidden = false; document.body.style.overflow = 'hidden'; renderOverlay(); search.focus({ preventScroll: true }); });
  options.forEach(button => button.addEventListener('click', () => { const slug = button.dataset.tagOption!; picked = mode === 'single' ? picked.includes(slug) ? [] : [slug] : picked.includes(slug) ? picked.filter(t => t !== slug) : [...picked, slug]; renderOverlay(); }));
  modes.forEach(button => button.addEventListener('click', () => { mode = button.dataset.tagMode!; if (mode === 'single') picked = picked.slice(0, 1); renderOverlay(); }));
  search.addEventListener('input', renderOverlay);
  overlay.querySelector('[data-tag-clear]')!.addEventListener('click', () => { picked = []; renderOverlay(); });
  overlay.querySelector('[data-tag-apply]')!.addEventListener('click', () => { state.tags = [...picked]; state.page = 1; commit(); closeOverlay(); });
  overlay.querySelector('[data-tag-close]')!.addEventListener('click', closeOverlay);
  overlay.addEventListener('click', event => { if (event.target === overlay) closeOverlay(); });
  document.addEventListener('keydown', event => {
    if (overlay.hidden) return;
    if (event.key === 'Escape') { event.preventDefault(); closeOverlay(); }
    if (event.key === 'Tab') {
      const focusable = [...overlay.querySelectorAll<HTMLElement>('button,input,[tabindex="0"]')].filter(el => el.getClientRects().length > 0);
      const index = focusable.indexOf(document.activeElement as HTMLElement);
      if (event.shiftKey && index <= 0) { event.preventDefault(); focusable.at(-1)?.focus(); }
      else if (!event.shiftKey && (index === focusable.length - 1 || index < 0)) { event.preventDefault(); focusable[0]?.focus(); }
    }
  });
  render(); measureColumns();
  new ResizeObserver(measureColumns).observe(row);
  void document.fonts.ready.then(measureColumns);
});
