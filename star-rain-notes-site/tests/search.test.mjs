import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createSearchEngine } from '../src/utils/site-search.ts';
const catalog = JSON.parse(fs.readFileSync(new URL('../src/content-data/catalog.json', import.meta.url), 'utf8'));
const grammar = JSON.parse(fs.readFileSync(new URL('../src/content-data/english/grammar.json', import.meta.url), 'utf8'));
const entries = [
  ...catalog.blogs.map(p => ({ title: p.title, description: p.description, path: p.path, type: '博客', keywords: p.tags.map(t => t.name).join(' ') })),
  ...catalog.courses.flatMap(c => [{ title: c.title, description: c.description, path: c.path, type: '教程', keywords: c.category }, ...c.groups.flatMap(g => g.chapters.map(ch => ({ title: ch.title, description: ch.description, path: ch.path, type: '教程', keywords: `${c.title} ${g.title}` })))]),
  ...grammar.sections.flatMap(s => s.lessons.map(l => ({ title: l.title, description: l.description, path: l.path, type: '英语', keywords: `英语 语法 ${s.title}` }))),
];

test('global search finds prefixes, misspellings and keyword matches in the actual local index', async () => {
  const engine = await createSearchEngine(entries);
  assert.ok(engine.search('jav', { limit: 8 }).some(hit => /Java/i.test(hit.item.title)));
  assert.ok(engine.search('StrngBuilder', { limit: 8 }).some(hit => /StringBuilder/.test(hit.item.title)));
  assert.ok(engine.search('语法', { limit: 8 }).length > 0);
  assert.equal(engine.search('zzzzzzzzzzzznotfound').length, 0);
});
