// Read-only checks against the standalone static preview (not the Astro dev server).
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { load } from 'cheerio';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const base = new URL(process.argv[2] || 'http://127.0.0.1:4321/');
const json = relative => JSON.parse(fs.readFileSync(path.join(root, relative), 'utf8'));
const catalog = json('src/content-data/catalog.json');
const themes = json('src/content-data/english/themes.json');
const paths = ['/', '/tutorials/', catalog.courses[0].path, '/tutorials/java-se/chapter-4-3/', '/blog/', '/blog/page/2/', '/blog/topics/java-web/', '/blog/tags/java/', '/blog/archive/', catalog.blogs[0].path, '/portfolio/', catalog.works[0].path, '/english/', '/english/vocabulary/', themes[0].path, '/english/grammar/', '/english/grammar/1-1/', '/about/', '/search/?q=Java', '/privacy/', '/terms/'];
const checks = [], errors = [];
let home = '';
for (const pathname of paths) {
  const response = await fetch(new URL(pathname, base));
  const html = await response.text();
  const ok = response.status === 200 && html.includes('<h1') && !html.includes('/@vite/client');
  checks.push({ path: pathname, status: response.status, staticHTML: ok });
  if (!ok) errors.push(`Not an independent static page: ${pathname} (${response.status})`);
  if (pathname === '/') home = html;
}
const $ = load(home);
const resources = [...new Set([...$('script[src]').map((_, el) => $(el).attr('src')).get(), ...$('link[rel=stylesheet]').map((_, el) => $(el).attr('href')).get(), $('img').eq(1).attr('src')])].filter(Boolean);
for (const resource of resources) {
  const response = await fetch(new URL(resource, base));
  const type = response.headers.get('content-type') || '';
  const ok = response.status === 200 && (resource.endsWith('.js') ? type.includes('javascript') : resource.endsWith('.css') ? type.includes('text/css') : type.startsWith('image/'));
  checks.push({ path: resource, status: response.status, contentType: type, compression: response.headers.get('content-encoding'), ok });
  if (!ok) errors.push(`Invalid static resource: ${resource}`);
  await response.arrayBuffer();
}
for (const pathname of ['/robots.txt', '/sitemap-index.xml', '/search-index.json']) {
  const response = await fetch(new URL(pathname, base));
  checks.push({ path: pathname, status: response.status });
  if (response.status !== 200) errors.push(`Missing ${pathname}`);
  await response.arrayBuffer();
}
const missing = await fetch(new URL('/__static-verification-missing__/', base));
checks.push({ path: '/__static-verification-missing__/', status: missing.status });
if (missing.status !== 404 || !(await missing.text()).includes('404')) errors.push('Custom 404 failed');
const redirect = await fetch(new URL('/blog?tag=java', base), { redirect: 'manual' });
checks.push({ path: '/blog?tag=java', status: redirect.status, location: redirect.headers.get('location') });
if (redirect.status !== 301 || redirect.headers.get('location') !== '/blog/?tag=java') errors.push('Trailing slash redirect failed');
const report = { status: errors.length ? 'FAIL' : 'PASS', checkedAt: new Date().toISOString(), base: base.href, checks, errors };
fs.writeFileSync(path.join(root, 'docs/runtime-verification.json'), JSON.stringify(report, null, 2));
console.log(`Runtime ${report.status}: ${checks.length} page, resource, redirect and 404 checks; no dev server or backend.`);
if (errors.length) { console.error(errors.join('\n')); process.exitCode = 1; }
