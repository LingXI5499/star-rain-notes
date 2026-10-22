import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { readBlogFilters, matchesBlogFilters, blogFilterURL } from '../src/utils/blog-filter-state.ts';
const catalog = JSON.parse(fs.readFileSync(new URL('../src/content-data/catalog.json', import.meta.url), 'utf8'));
const topics = JSON.parse(fs.readFileSync(new URL('../src/content-data/topics.json', import.meta.url), 'utf8'));
const tags = JSON.parse(fs.readFileSync(new URL('../src/content-data/tags.json', import.meta.url), 'utf8'));
const parse = path => readBlogFilters(new URL(path, 'https://static.test'), topics.map(t => t.slug), tags.map(t => t.slug));
const count = state => catalog.blogs.filter(post => matchesBlogFilters({ topics: post.topics.map(t => t.slug), tags: post.tags.map(t => t.slug), publishedAt: post.publishedAt }, state)).length;

test('all eight public columns retain their real membership, including empty columns', () => {
  assert.equal(topics.length, 8);
  for (const topic of topics) assert.equal(count(parse(`/blog/?topic=${topic.slug}`)), catalog.blogs.filter(p => p.topics.some(t => t.slug === topic.slug)).length);
  assert.equal(count(parse('/blog/?topic=java-web')), 2);
  assert.equal(count(parse('/blog/topics/web/')), 0);
  assert.equal(count(parse('/blog/')), 16);
});
test('single tag, any-of multiple tags and combined topic/month filters use real content', () => {
  assert.equal(count(parse('/blog/?tag=java')), 8);
  const multi = parse('/blog/?tags=java,spring-boot');
  assert.equal(count(multi), catalog.blogs.filter(p => p.tags.some(t => ['java', 'spring-boot'].includes(t.slug))).length);
  const combined = parse('/blog/?topic=java-web&tags=java,spring-boot&month=2026-09');
  assert.equal(count(combined), catalog.blogs.filter(p => p.topics.some(t => t.slug === 'java-web') && p.tags.some(t => ['java', 'spring-boot'].includes(t.slug)) && p.publishedAt.startsWith('2026-09')).length);
});
test('shared URLs, static tag/column paths and pagination round-trip without losing filters', () => {
  const state = parse('/blog/?topic=java-web&tags=java,spring-boot&month=2026-09&page=2&size=24');
  assert.deepEqual(parse(blogFilterURL(new URL('https://static.test/blog/'), state).href), state);
  assert.deepEqual(parse('/blog/tags/java/').tags, ['java']);
  assert.equal(parse('/blog/page/2/').page, 2);
  assert.equal(parse('/blog/?page=NaN&size=999&month=2026-99&topic=missing&tags=java,java,missing').page, 1);
  assert.deepEqual(parse('/blog/?tags=java,java,missing').tags, ['java']);
  assert.equal(parse('/blog/archive/#month-2026-08').month, '2026-08');
});
