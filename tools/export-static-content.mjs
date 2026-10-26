// Export reviewed public tutorial/blog content; the static site's application stays separate.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const site = path.join(root, 'star-rain-notes-site');
const require = createRequire(path.join(site, 'package.json'));
const { load } = require('cheerio');
const MarkdownIt = require('markdown-it');
const sanitize = require('sanitize-html');
const { katex } = require('@mdit/plugin-katex');
const md = new MarkdownIt({ html: true, linkify: true }).use(katex, { throwOnError: false });
const source = path.join(site, '.cache/source');
const data = path.join(site, 'src/content-data');
const read = file => JSON.parse(fs.readFileSync(file, 'utf8'));
const useImportCache = !process.argv.includes('--no-import-cache');
const assetFile = path.join(source, 'asset-map.json');
const assetMap = useImportCache && fs.existsSync(assetFile) ? read(assetFile) : {};
const bundleFile = path.join(root, 'backend/content/static-site-content.json');
const preservedBodies = new Map();
if (fs.existsSync(bundleFile)) {
  const previous = read(bundleFile);
  for (const blog of previous.blogs || []) preservedBodies.set(`blogs/${blog.slug}/body.html`, blog);
  for (const course of previous.courses || []) for (const group of course.groups)
    for (const chapter of group.chapters) preservedBodies.set(`tutorials/${course.slug}/chapters/${chapter.slug}.html`, chapter);
}
const localize = text => {
  for (const [from, to] of Object.entries(assetMap)) text = text.replaceAll(from, to);
  return text.replaceAll('https://yulanlin.cn/blog/', '/blog/').replaceAll('https://yulanlin.cn/tutorials/', '/tutorials/');
};
function renderOriginal(text) {
  return localize(sanitize(md.render(text || ''), {
    allowedTags: sanitize.defaults.allowedTags.concat(['img','figure','figcaption','details','summary','math','semantics','mrow','mi','mo','mn','msup','msub','mfrac','msqrt','mtext','annotation']),
    allowedAttributes: { '*': ['class','id','aria-hidden'], a: ['href','title'], img: ['src','alt','width','height','title'], code: ['class'], span: ['class','style'], math: ['xmlns','display'], annotation: ['encoding'] },
    allowedSchemes: ['http','https','mailto'],
    allowedStyles: { span: { '*': [/^[\w\s.:%#(),+\-]+$/] } },
  }));
}
const normal = html => load(html, null, false).html().trim();
const plain = html => load(html, null, false).text().replace(/\s+/g, '');
const stats = { originalMarkdown: 0, preservedMarkdown: 0, convertedHtml: 0, placeholders: 0, images: [] };

// Convert only changed static HTML. Original Markdown is retained verbatim when its
// rendered body matches, preserving code fences, formulas and editor source syntax.
export function htmlToMarkdown(html) {
  const $ = load(html, null, false);
  const inline = node => (node.children || []).map(convert).join('');
  function convert(node) {
    if (node.type === 'text') return node.data.replace(/[\\`*_\[\]<>]/g, '\\$&');
    if (node.type === 'comment') return '';
    const tag = node.name, el = $(node), text = () => inline(node);
    if (/^h[1-6]$/.test(tag)) return `\n\n${'#'.repeat(Number(tag[1]))} ${text().trim()}\n\n`;
    if (tag === 'pre') {
      const code = el.find('code').first();
      const language = (code.attr('class') || '').match(/language-([\w+-]+)/)?.[1] || '';
      const body = code.length ? code.text() : el.text();
      const ticks = '`'.repeat(Math.max(3, ...[...body.matchAll(/`+/g)].map(m => m[0].length + 1)));
      return `\n\n${ticks}${language}\n${body.replace(/\n$/, '')}\n${ticks}\n\n`;
    }
    if (tag === 'code') {
      const value = el.text(), ticks = '`'.repeat(Math.max(1, ...[...value.matchAll(/`+/g)].map(m => m[0].length + 1)));
      const pad = /^[` ]|[` ]$/.test(value) ? ' ' : '';
      return `${ticks}${pad}${value}${pad}${ticks}`;
    }
    if (tag === 'strong' || tag === 'b') return `**${text()}**`;
    if (tag === 'em' || tag === 'i') return `*${text()}*`;
    if (tag === 'a') return `[${text()}](${el.attr('href') || ''})`;
    if (tag === 'img') return `![${(el.attr('alt') || '').replace(/\]/g,'\\]')}](${el.attr('src') || ''})`;
    if (tag === 'br') return '  \n';
    if (tag === 'hr') return '\n\n---\n\n';
    if (tag === 'blockquote') return '\n\n' + text().trim().split('\n').map(l => '> ' + l).join('\n') + '\n\n';
    if (tag === 'ul' || tag === 'ol') {
      const items = el.children('li').toArray().map((li, i) => {
        const marker = tag === 'ol' ? `${i + Number(el.attr('start') || 1)}. ` : '- ';
        return marker + inline(li).trim().split('\n').map((l,j) => j ? ' '.repeat(marker.length) + l : l).join('\n');
      });
      return '\n\n' + items.join('\n') + '\n\n';
    }
    if (tag === 'table') {
      const rows = el.find('tr').toArray().map(tr => $(tr).children('th,td').toArray().map(td => inline(td).trim().replace(/\|/g,'\\|').replace(/\n+/g,' ')));
      if (!rows.length) return '';
      return '\n\n' + [rows[0], rows[0].map(() => '---'), ...rows.slice(1)].map(r => '| ' + r.join(' | ') + ' |').join('\n') + '\n\n';
    }
    if (tag === 'math' || el.hasClass('katex')) {
      const tex = el.find('annotation[encoding="application/x-tex"]').text();
      if (!tex) throw new Error('Formula has no original TeX');
      return el.attr('display') === 'block' ? `\n\n$$\n${tex}\n$$\n\n` : `$${tex}$`;
    }
    if (tag === 'p' || tag === 'div' || tag === 'figure' || tag === 'figcaption') return '\n\n' + text().trim() + '\n\n';
    if (tag === 'span') return text();
    throw new Error(`Unsupported content tag: ${tag}`);
  }
  return $.root().contents().toArray().map(convert).join('').trim() + '\n';
}
function body(file, cachedFile) {
  const html = fs.readFileSync(file, 'utf8');
  let markdown;
  if (useImportCache && fs.existsSync(cachedFile)) {
    const original = read(cachedFile).bodyMarkdown || '';
    if (normal(renderOriginal(original)) === normal(html)) { markdown = localize(original); stats.originalMarkdown++; }
  }
  const hash = crypto.createHash('sha256').update(html).digest('hex');
  const previous = preservedBodies.get(path.relative(data, file).replaceAll('\\', '/'));
  const sameSource = previous && [html, html.replaceAll('\r\n', '\n'), html.replace(/\r?\n/g, '\r\n')]
    .some(value => crypto.createHash('sha256').update(value).digest('hex') === previous.sourceSha256);
  if (markdown === undefined && sameSource) {
    markdown = previous.bodyMarkdown;
    stats.preservedMarkdown++;
  }
  if (markdown === undefined) {
    markdown = htmlToMarkdown(html);
    // All visible text must survive the conversion, including tables and code.
    const rendered = md.render(markdown);
    if (plain(rendered) !== plain(html)) throw new Error(`Text changed during conversion: ${path.relative(data, file)}`);
    const signature = value => {
      const $ = load(value, null, false);
      return JSON.stringify({
        code: $('pre code').toArray().map(el => $(el).text()),
        links: $('a').toArray().map(el => $(el).attr('href')),
        images: $('img').toArray().map(el => [$(el).attr('src'), $(el).attr('alt') || '']),
        tables: $('table').toArray().map(el => $(el).find('tr').toArray().map(tr => $(tr).children('th,td').toArray().map(td => $(td).text().replace(/\s+/g,'')))),
      });
    };
    if (signature(rendered) !== signature(html)) throw new Error(`Code, link, image or table changed: ${path.relative(data, file)}`);
    stats.convertedHtml++;
  }
  if (!markdown.trim()) throw new Error(`Empty source: ${file}`);
  if (plain(html) === '待编写') stats.placeholders++;
  return { bodyMarkdown: markdown, sourceSha256: hash };
}
const courses = fs.readdirSync(path.join(data, 'tutorials')).sort().map(slug => {
  const course = read(path.join(data, 'tutorials', slug, 'course.json'));
  return { ...course, groups: course.groups.map(group => ({ ...group, chapters: group.chapters.map(chapter => ({
    ...chapter, ...body(path.join(data, 'tutorials', slug, 'chapters', `${chapter.slug}.html`), path.join(source, 'chapters', slug, `${chapter.slug}.json`)),
  })) })) };
});
const blogs = fs.readdirSync(path.join(data, 'blogs')).sort().map(slug => ({
  ...read(path.join(data, 'blogs', slug, 'meta.json')),
  ...body(path.join(data, 'blogs', slug, 'body.html'), path.join(source, 'blogs', `${slug}.json`)),
}));
const bundle = { schemaVersion: 1, categories: read(path.join(data, 'categories.json')), courses, blogs };
const serialized = JSON.stringify(bundle, null, 2) + '\n';
const images = [...new Set(serialized.match(/\/images\/content\/[\w.-]+/g) || [])].sort();
for (const image of images) {
  const src = path.join(site, 'public', image), dest = path.join(root, 'frontend/public', image);
  if (!fs.existsSync(src)) throw new Error(`Missing image: ${image}`);
  if (fs.existsSync(dest) && !fs.readFileSync(dest).equals(fs.readFileSync(src))) throw new Error(`Conflicting image: ${image}`);
  fs.mkdirSync(path.dirname(dest), { recursive: true }); fs.copyFileSync(src, dest);
}
const dest = path.join(root, 'backend/content/static-site-content.json');
fs.mkdirSync(path.dirname(dest), { recursive: true }); fs.writeFileSync(dest, serialized);
stats.images = images;
console.log(JSON.stringify({ courses: courses.length, chapters: courses.reduce((n,c) => n + c.groups.reduce((s,g) => s + g.chapters.length, 0),0), blogs: blogs.length, ...stats }, null, 2));
