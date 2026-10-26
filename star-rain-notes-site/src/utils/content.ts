import sanitizeHtml from 'sanitize-html';
import { load } from 'cheerio';
import { createHighlighter } from 'shiki';
import catalogData from '../content-data/catalog.json';
import site from '../content-data/site.json';
import profile from '../content-data/profile.json';
import themes from '../content-data/english/themes.json';
import grammar from '../content-data/english/grammar.json';
import topics from '../content-data/topics.json';
import homeData from '../content-data/home.json';
import vocabularyTaxonomy from '../content-data/english/vocabulary-taxonomy.json';
import worldTaxonomy from '../content-data/english/world-taxonomy.json';
import vocabularyData from '../content-data/english/vocabulary-entries.json';
import worldData from '../content-data/english/world-articles.json';
import essaysData from '../content-data/english/essays.json';
import type { VocabularyEntry, EnglishArticle } from './english-model';

export { vocabularyTaxonomy, worldTaxonomy };
export * from './english-model';
export const vocabularyEntries = vocabularyData.entries as VocabularyEntry[];
export const worldArticles = worldData.articles as EnglishArticle[];
export const essays = essaysData.articles as EnglishArticle[];

export { site, profile, themes, grammar, topics };
export const home = homeData as { code:string; title:string; items:{ slug?:string; title:string; path?:string; publishedAt:string; type?:string }[] }[];
export const catalog = catalogData;
export type Blog = typeof catalog.blogs[number];
export type Course = typeof catalog.courses[number];
export type Work = typeof catalog.works[number];
export type Chapter = Course['groups'][number]['chapters'][number];
// Eager raw imports survive Astro's relocation of prerender chunks into dist/.
// These are build-only modules; article bodies never become client JavaScript.
const htmlFiles = import.meta.glob<string>('../content-data/**/*.html', { query:'?raw', import:'default', eager:true });
const jsonFiles = import.meta.glob('../content-data/**/*.json', { import:'default', eager:true });
export function readBody(relative:string):string {
  const body=htmlFiles[`../content-data/${relative}`];
  if(body===undefined)throw new Error(`Missing local HTML content: ${relative}`);
  return body;
}
export function readJSON(relative:string) {
  const data=jsonFiles[`../content-data/${relative}`];
  if(data===undefined)throw new Error(`Missing local JSON content: ${relative}`);
  return JSON.parse(JSON.stringify(data));
}
export const dateLabel = (date?: string) => date?.replaceAll('-', '/') || '';
export const plainText = (html: string) => load(html).text().replace(/\s+/g, ' ').trim();
export const allTags = [...new Map(catalog.blogs.flatMap(b => b.tags).map(t => [t.slug, t])).values()];
const highlighter = createHighlighter({ themes: ['github-light', 'github-dark'], langs: ['javascript', 'typescript', 'java', 'python', 'html', 'css', 'json', 'bash', 'sql', 'xml', 'cpp', 'c', 'yaml', 'markdown', 'powershell', 'vue'] });
const rendered = new Map<string, Promise<RenderedBody>>();
export interface RenderedBody { html: string; toc: { id: string; title: string; depth: number }[]; text: string; minutes: number }
export function renderBody(relative: string): Promise<RenderedBody> {
  if (!rendered.has(relative)) rendered.set(relative, prepareBody(readBody(relative)));
  return rendered.get(relative)!;
}
export async function prepareBody(source: string): Promise<RenderedBody> {
  const safe = sanitizeHtml(source, {
    allowedTags: sanitizeHtml.defaults.allowedTags.concat(['img','figure','figcaption','details','summary','math','semantics','mrow','mi','mo','mn','msup','msub','mfrac','msqrt','mtext','annotation']),
    allowedAttributes: { '*': ['class','id','aria-hidden'], a: ['href','title','rel'], img: ['src','alt','width','height','title'], code: ['class'], span: ['class','style'], math: ['xmlns','display'], annotation: ['encoding'] },
    allowedSchemes: ['http','https','mailto'],
    allowedStyles: { span: { '*': [/^[\w\s.:%#(),+\-]+$/] } },
  });
  const $ = load(safe, null, false);
  const toc: RenderedBody['toc'] = [];
  $('h1,h2,h3,h4,h5,h6').each((i, node) => {
    const element = $(node), original = Number(node.tagName[1]);
    const depth = original === 1 ? 2 : original;
    const id = element.attr('id') || `section-${i + 1}`;
    const title = element.text();
    element.attr('id', id);
    if (original === 1) { element.replaceWith(`<h2 id="${id}">${element.html()}</h2>`); }
    if (depth <= 3) toc.push({ id, title, depth });
  });
  $('a').each((_, node) => {
    const el = $(node); const href = el.attr('href') || '';
    if (/^https?:/.test(href)) el.attr('target', '_blank').attr('rel', 'noopener noreferrer');
    else if (href.startsWith('/') && !href.includes('.') && !href.includes('#') && !href.endsWith('/')) el.attr('href', href + '/');
  });
  $('img').each((_, node) => { const el = $(node); el.attr('loading','lazy').attr('decoding','async'); if (!el.attr('alt')) el.attr('alt', '文章配图'); });
  const engine = await highlighter;
  $('pre').each((_, node) => {
    const el = $(node), code = el.find('code');
    const text = code.length ? code.text() : el.text();
    const requested = (code.attr('class') || '').match(/language-([^\s]+)/)?.[1] || 'text';
    const aliases: Record<string, string> = { shell:'bash', sh:'bash', js:'javascript', ts:'typescript', yml:'yaml', plaintext:'text', console:'text' };
    const lang = aliases[requested] || requested;
    const supported = engine.getLoadedLanguages().includes(lang);
    const output = engine.codeToHtml(text, { lang: supported ? lang : 'text', themes: { light:'github-light', dark:'github-dark' } });
    el.replaceWith(`<div class="code-block"><div class="code-block__head"><span class="code-block__label">${supported ? lang : 'text'}</span><button type="button" class="code-block__copy" data-copy-code aria-label="复制代码">复制</button></div>${output}</div>`);
  });
  const text = $.text();
  return { html: $.html(), toc, text, minutes: Math.max(1, Math.ceil(text.length / 600)) };
}
