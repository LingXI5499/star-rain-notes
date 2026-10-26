import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import MarkdownIt from 'markdown-it';
import sanitizeHtml from 'sanitize-html';
import { katex } from '@mdit/plugin-katex';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = path.join(root, '.cache/source');
const dest = path.join(root, 'src/content-data');
const partial = process.argv.includes('--partial');
const read = name => JSON.parse(fs.readFileSync(path.join(source, `${name}.json`), 'utf8'));
const write = (name, value) => {
  const file = path.join(dest, name);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, typeof value === 'string' ? value : JSON.stringify(value, null, 2) + '\n');
};
const assetMap = fs.existsSync(path.join(source, 'asset-map.json')) ? read('asset-map') : {};
const md = new MarkdownIt({ html: true, linkify: true, typographer: false }).use(katex, { throwOnError: false });
const cleanName = s => String(s || '').replaceAll('`', '').trim().replace(/^[‘’“”'\"]+|[‘’“”'\"]+$/g, '');
const date = s => s ? s.slice(0, 10) : undefined;
const localize = s => {
  let result = s;
  for (const [from, to] of Object.entries(assetMap)) result = result.replaceAll(from, to);
  return result.replaceAll('https://yulanlin.cn/blog/', '/blog/').replaceAll('https://yulanlin.cn/tutorials/', '/tutorials/');
};
function render(markdown) {
  return localize(sanitizeHtml(md.render(markdown || ''), {
    allowedTags: sanitizeHtml.defaults.allowedTags.concat(['img','figure','figcaption','details','summary','math','semantics','mrow','mi','mo','mn','msup','msub','mfrac','msqrt','mtext','annotation']),
    allowedAttributes: { '*': ['class', 'id', 'aria-hidden'], a: ['href','title'], img: ['src','alt','width','height','title'], code: ['class'], span: ['class','style'], math: ['xmlns','display'], annotation: ['encoding'] },
    allowedSchemes: ['http', 'https', 'mailto'],
    allowedStyles: { span: { '*': [/^[\w\s.:%#(),+\-]+$/] } },
  }));
}
const report = { date: '2026-10-21', source: 'https://yulanlin.cn/api/public/', counts: {}, emptyCourses: [], emptyChapters: [], missing: [], notes: ['原站已发布的提纲按真实内容保留，不补写为完整教程。', '作品中关于原平台业务的说明作为原作品描述保留；本站不提供对应动态业务。'] };
const blogs = [];
for (const item of read('blogs').items) {
  const data = read(`blogs/${item.slug}`);
  const meta = { slug: data.slug, title: data.title, description: data.summary, publishedAt: date(data.publishedAt), updatedAt: date(data.updatedAt), wordCount: data.bodyMarkdown.replace(/\s/g, '').length, tags: data.tags.map(t => ({ slug: t.slug, name: cleanName(t.name) })), topics: data.topics.map(t => ({ slug: t.slug, name: t.name })), cover: assetMap[data.coverUrl] || undefined, path: `/blog/posts/${data.slug}/` };
  write(`blogs/${meta.slug}/meta.json`, meta);
  write(`blogs/${meta.slug}/body.html`, render(data.bodyMarkdown));
  blogs.push(meta);
}
let chapterCount = 0;
const courses = [];
for (const item of read('tutorials').items) {
  const data = read(`courses/${item.slug}`);
  const groups = data.groups.map(group => ({ title: group.title, chapters: group.chapters.filter(chapter => !partial || fs.existsSync(path.join(source, `chapters/${item.slug}/${chapter.slug}.json`))).map(chapter => {
    const detail = read(`chapters/${item.slug}/${chapter.slug}`);
    const body = render(detail.bodyMarkdown);
    if (!body.trim()) report.emptyChapters.push({ course: item.slug, slug: chapter.slug, title: chapter.title });
    write(`tutorials/${item.slug}/chapters/${chapter.slug}.html`, body);
    chapterCount++;
    return { slug: chapter.slug, title: chapter.title, description: detail.summary || chapter.summary || `${data.title} · ${chapter.title}`, path: `/tutorials/${item.slug}/${chapter.slug}/`, wordCount: (detail.bodyMarkdown || '').replace(/\s/g,'').length, hasBody: !!body.trim() };
  }) }));
  const count = groups.reduce((n, g) => n + g.chapters.length, 0);
  const meta = { slug: data.slug, title: data.title, description: data.summary || `${data.title}课程与章节目录`, category: data.categoryName, categorySlug: data.categorySlug, publishedAt: date(data.publishedAt), chapterCount: count, wordCount: item.wordCount || 0, categoryId: item.categoryId, groups, path: `/tutorials/${item.slug}/` };
  if (!count) report.emptyCourses.push({ slug: item.slug, title: data.title });
  write(`tutorials/${item.slug}/course.json`, meta);
  courses.push(meta);
}
const works = [];
for (const item of read('works').items) {
  const data = read(`works/${item.slug}`);
  let techStack = data.typeDetail?.techStack;
  if (!Array.isArray(techStack)) { try { techStack = JSON.parse(data.techStack || '[]'); } catch { techStack = []; } }
  const screenshots = (data.media || []).filter(m => m.usageType !== 'COVER' && assetMap[m.url]).map(m => ({ src: assetMap[m.url], alt: m.caption || data.title }));
  const meta = { slug: data.slug, title: data.title, description: data.summary, category: data.category?.name || '项目作品', format: data.format?.name || '', techStack, status: data.projectStatus, publishedAt: date(data.publishedAt), updatedAt: date(data.updatedAt), startedOn: data.startedOn, endedOn: data.endedOn, featured: data.featured, cover: assetMap[data.coverUrl], screenshots, links: (data.links || []).filter(l => l.enabled && /^https?:/.test(l.url)).map(l => ({ label: l.label, url: l.url, type: l.linkType })), path: `/portfolio/${data.slug}/` };
  let body = data.bodyMarkdown || '';
  if (data.hasSections && data.sections?.length) body = data.sections.map(s => `## ${s.title || ''}\n\n${s.bodyMarkdown || s.contentMarkdown || s.content || ''}`).join('\n\n');
  write(`portfolio/${data.slug}/meta.json`, meta);
  write(`portfolio/${data.slug}/body.html`, render(body));
  write(`portfolio/${data.slug}/sections.json`, (data.sections || []).filter(s=>s.visible!==false).map(s=>({ id:s.id,title:s.title,type:s.sectionType,html:render(s.content || ''),data:s.data || {},media:(s.media||[]).filter(m=>assetMap[m.url]).map(m=>({src:assetMap[m.url],caption:m.caption || s.title})) })));
  works.push(meta);
}
const themes = [];
let wordCount = 0;
for (const item of read('themes')) {
  const first = read(`words/${item.id}-1`);
  if (partial && Array.from({length:first.totalPages},(_,i)=>i+1).some(page=>!fs.existsSync(path.join(source,`words/${item.id}-${page}.json`)))) continue;
  let words = [];
  for (let page = 1; page <= first.totalPages; page++) words.push(...read(`words/${item.id}-${page}`).items);
  const unique = [...new Map(words.map(w => [w.id, w])).values()];
  if (unique.length !== first.total) throw new Error(`Theme ${item.id}: ${unique.length}/${first.total} words; pagination incomplete`);
  words = unique.map(w => ({ word: w.word, partOfSpeech: w.partOfSpeech, phoneticUs: w.phoneticUs, phoneticUk: w.phoneticUk, translation: w.translation, sceneMeaning: w.sceneMeaning, examples: w.examples }));
  const theme = { id: item.id, title: item.name, layer: item.layer, wordCount: words.length, path: `/english/vocabulary/${item.id}/` };
  write(`english/vocabulary/${item.id}.json`, { ...theme, words });
  themes.push(theme); wordCount += words.length;
}
write('english/themes.json', themes);
const grammar = read('grammar');
const grammarSections = grammar.sections.map(section => ({ title: section.title, lessons: section.lessons.filter(l => l.publishStatus === 'PUBLISHED').map(lesson => {
  const meta = { slug: lesson.slug, title: lesson.title, description: lesson.summary || `${section.title} · ${lesson.title}`, section: section.title, path: `/english/grammar/${lesson.slug}/` };
  write(`english/grammar/${lesson.slug}/meta.json`, meta);
  write(`english/grammar/${lesson.slug}/body.html`, render(lesson.bodyMarkdown));
  return meta;
}) }));
write('english/grammar.json', { ...grammar.course, sections: grammarSections });
const profile = read('profile');
write('profile.json', { displayName: profile.displayName, headline: profile.headline, avatar: '/brand/author-avatar.webp', skills: profile.skills.map(s => ({ category: s.category, name: s.name })), socialLinks: profile.socialLinks.map(l => ({ label: l.label, url: l.url })), experiences: profile.experiences });
write('profile.html', render(profile.bioMarkdown));
const profileMarkdown=profile.bioMarkdown || '';
const firstHeading=profileMarkdown.search(/^##\s+技术方向\s*$/m);
const journeyHeading=profileMarkdown.search(/^##\s+学习与实践\s*$/m);
write('profile/story.html', render(firstHeading>=0?profileMarkdown.slice(0,firstHeading):profileMarkdown));
write('profile/direction.html', render(firstHeading>=0?profileMarkdown.slice(firstHeading,journeyHeading>=0?journeyHeading:undefined).replace(/^##\s+技术方向\s*\n/,'').replace(/^##(?!#)/gm,'###'):''));
write('profile/journey.html', render(journeyHeading>=0?profileMarkdown.slice(journeyHeading).replace(/^##\s+学习与实践\s*\n/,'').replace(/^##(?!#)/gm,'###'):''));
write('profile-view.json', { excerpt:profileMarkdown.split(/\n(?=##\s)/)[0].replace(/^#+\s+/gm,'').replace(/[*_`>]/g,'').replace(/\s+/g,' ').trim().slice(0,150)+'…',locationText:profile.locationText || '',featuredContents:(profile.featuredContents||[]).filter(c=>c.available!==false).map(c=>({...c,coverUrl:assetMap[c.coverUrl],url:c.url?.replace(/\/$/,'')+'/'})) });
write('messages.json', read('messages').items.map(m => ({ id: String(m.id), author: m.authorDisplayName, content: m.content, publishedAt: date(m.submittedAt), approved: true, source: '原站已公开留言' })));
const config = read('site');
write('site.json', { name: config.siteName, tagline: config.tagline, description: config.siteDescription, intro: config.homeIntro, url: 'https://yulanlin.cn', language: 'zh-CN', ogImage: '/brand/og-default.png' });
write('home.json', read('home').sections.map(s => ({ code: s.code, title: s.title, items: Array.isArray(s.data) ? s.data.map(i => ({ slug: i.slug, title: i.title, path: i.routePath, publishedAt: date(i.publishedAt), type: i.type })) : [] })));
write('tags.json', read('tags').map(t => ({ ...t, name: cleanName(t.name) })));
write('categories.json', read('categories'));
write('blog-stats.json', read('blog-stats'));
write('topics.json', read('topics').map(t => ({ slug: t.slug, name: t.name, description: t.description || `${t.name}相关技术文章与学习笔记` })));
write('catalog.json', { blogs, courses, works });
report.counts = { blogs: blogs.length, courses: courses.length, chapters: chapterCount, works: works.length, vocabularyThemes: themes.length, words: wordCount, grammarLessons: grammarSections.reduce((n,s) => n+s.lessons.length,0), messages: read('messages').total, localizedAssets: Object.keys(assetMap).length };
fs.writeFileSync(path.join(root, 'docs/import-report.json'), JSON.stringify(report, null, 2));
console.log(JSON.stringify(report.counts, null, 2));
