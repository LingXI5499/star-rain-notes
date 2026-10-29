// Export the standalone site's reviewed portfolio into an application content bundle.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const site=path.join(root,'star-rain-notes-site');
const require=createRequire(path.join(site,'package.json'));
const {load}=require('cheerio');
const MarkdownIt=require('markdown-it');
const md=new MarkdownIt({html:true});
const plain=html=>load(html,null,false).text().replace(/\s+/g,'');
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

const source=path.join(site,'src/content-data/portfolio');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const convert=html=>{const text=htmlToMarkdown(html);if(plain(md.render(text))!==plain(html))throw new Error('Portfolio text changed during conversion');return text;};
const works=fs.readdirSync(source).sort().map(slug=>{
 const directory=path.join(source,slug),meta=read(path.join(directory,'meta.json'));
 return {...meta,bodyMarkdown:convert(fs.readFileSync(path.join(directory,'body.html'),'utf8')),sections:read(path.join(directory,'sections.json')).map(s=>({...s,content:convert(s.html || '')}))};
});
const bundle={schemaVersion:1,works};
const images=[...new Set(JSON.stringify(bundle).match(/\/images\/content\/[\w.-]+/g)||[])].sort();
for(const image of images){const src=path.join(site,'public',image),dest=path.join(root,'frontend/public',image);if(!fs.existsSync(src))throw new Error('Missing portfolio image');if(fs.existsSync(dest)&&!fs.readFileSync(dest).equals(fs.readFileSync(src)))throw new Error('Conflicting portfolio image');fs.mkdirSync(path.dirname(dest),{recursive:true});fs.copyFileSync(src,dest);}
fs.writeFileSync(path.join(root,'backend/content/static-portfolio-content.json'),JSON.stringify(bundle,null,2)+'\n');
console.log(JSON.stringify({works:works.length,sections:works.reduce((n,w)=>n+w.sections.length,0),images:images.length}));
