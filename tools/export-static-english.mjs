import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const require=createRequire(path.join(root,'star-rain-notes-site/package.json'));
const {load}=require('cheerio');
const data=path.join(root,'star-rain-notes-site/src/content-data/english');
const read=name=>JSON.parse(fs.readFileSync(path.join(data,name+'.json'),'utf8'));
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
const grammar=read('grammar');
const grammarLessons=grammar.sections.flatMap(section=>section.lessons.map(lesson=>{
 const html=fs.readFileSync(path.join(data,'grammar',lesson.slug,'body.html'),'utf8');
 return {...lesson,sectionTitle:section.title,bodyMarkdown:htmlToMarkdown(html),sourceSha256:crypto.createHash('sha256').update(html.replaceAll('\r\n','\n')).digest('hex')};
}));
const homeSource=fs.readFileSync(path.join(root,'star-rain-notes-site/src/components/astro/EnglishHome.astro'),'utf8');
const overview={title:homeSource.match(/<h1>([^<]+)<\/h1>/)?.[1],introduction:homeSource.match(/class="english-hero__lead">([^<]+)<\/p>/)?.[1]};
if(!overview.title||!overview.introduction)throw new Error('English homepage content requires review');
const bundle={schemaVersion:1,overview,vocabularyTaxonomy:read('vocabulary-taxonomy'),worldTaxonomy:read('world-taxonomy'),entries:read('vocabulary-entries').entries,grammar:{...grammar,lessons:grammarLessons},world:read('world-articles').articles,essays:read('essays').articles};
for(const article of [...bundle.world,...bundle.essays]){
 article.bodyMarkdown=htmlToMarkdown(fs.readFileSync(path.join(data,article.bodyPath.replace(/^english\//,'')),'utf8'));
 if(article.translationBodyPath)article.translationZhMarkdown=htmlToMarkdown(fs.readFileSync(path.join(data,article.translationBodyPath.replace(/^english\//,'')),'utf8'));
}
fs.writeFileSync(path.join(root,'backend/content/static-english-content.json'),JSON.stringify(bundle,null,2)+'\n');
console.log(JSON.stringify({records:bundle.entries.length,vocabularyCategories:bundle.vocabularyTaxonomy.categories.length,vocabularyTopics:bundle.vocabularyTaxonomy.categories.flatMap(c=>c.topics).length,worldCategories:bundle.worldTaxonomy.categories.length,worldTopics:bundle.worldTaxonomy.categories.flatMap(c=>c.topics).length,grammarLessons:grammarLessons.length,worldArticles:bundle.world.length,essays:bundle.essays.length}));
