/** Exercise the actual deployment template with a disposable loopback Nginx and SEO upstream. */
import assert from 'node:assert/strict'
import http from 'node:http'
import { mkdtemp, mkdir, readFile, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { resolve, join } from 'node:path'
import { spawn, spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
const nginx = process.env.NGINX_BIN
assert(nginx, 'Set NGINX_BIN to a local Nginx executable')
const source = await readFile('backend/deploy/account-v3/nginx-site.example.conf', 'utf8')
const routes = await readFile('frontend/src/router/index.js', 'utf8')
const temporary = await mkdtemp(join(tmpdir(), 'star-rain-nginx-routes-'))
const dist = join(temporary, 'dist')
await mkdir(join(temporary, 'logs')); await mkdir(join(temporary, 'temp')); await mkdir(dist)
await writeFile(join(dist, 'index.html'), '<!doctype html><html><body>ROUTE_SPA</body></html>')
const upstream = http.createServer((req, res) => {
  const path = new URL(req.url, 'http://localhost').pathname
  const status = path.includes('missing') || path === '/unknown' ? 404 : 200
  res.writeHead(status, { 'Content-Type': 'text/html', 'X-Test-Upstream': 'seo' })
  res.end(`<html><head><link rel="canonical" href="https://example.test${path}"></head><body>SEO_HTML</body></html>`)
})
await new Promise(resolve => upstream.listen(0, '127.0.0.1', resolve))
const probe = http.createServer()
await new Promise(resolve => probe.listen(0, '127.0.0.1', resolve))
const port = probe.address().port
await new Promise(resolve => probe.close(resolve))
const secondServer = source.indexOf('server {', source.indexOf('server {') + 1)
let server = source.slice(secondServer)
  .replace('listen 443 ssl;', `listen 127.0.0.1:${port};`)
  .replace(/\s*ssl_certificate(?:_key)? [^;]+;/g, '')
  .replace(/root [^;]+;/, `root "${dist.replaceAll('\\', '/')}";`)
  .replace(/access_log [^;]+;/g, 'access_log off;')
  .replaceAll('127.0.0.1:8088', `127.0.0.1:${upstream.address().port}`)
await writeFile(join(temporary, 'nginx.conf'), `pid nginx.pid;\nerror_log logs/error.log;\nevents {}\nhttp { default_type text/html; ${server} }`)
const args = ['-p', temporary.replaceAll('\\', '/') + '/', '-c', 'nginx.conf']
const syntax = spawnSync(nginx, [...args, '-t'], { encoding: 'utf8', windowsHide: true })
assert.equal(syntax.status, 0, syntax.stderr)
const processHandle = spawn(nginx, [...args, '-g', 'daemon off;'], { windowsHide: true, stdio: 'ignore' })
const origin = `http://127.0.0.1:${port}`
let checks = 0
try {
  let ready = false
  for (let attempt = 0; attempt < 100; attempt++) {
    try { await fetch(origin); ready = true; break } catch { await new Promise(resolve => setTimeout(resolve, 50)) }
  }
  assert(ready, 'Disposable Nginx did not start')
  const privatePaths = []
  for (const match of routes.matchAll(/\{ path: '([^']+)'([^\n]*)(?:\n\s+meta: \{([^\n]+)\})?/g)) {
    const metadata = match[2] + (match[3] || '')
    if (/requiresAuth: true|console: true|authPage: true|permission:|superAdminOnly: true/.test(metadata)) {
      privatePaths.push(match[1].replace(/:[A-Za-z]+/g, '12'))
    }
  }
  assert(privatePaths.length > 40, 'Route matrix was not parsed')
  for (const path of [...new Set([...privatePaths, '/permissions', '/tutorials/exercises', '/tutorials/12/chapters/34/exercises', '/portfolio/demo/live'])]) {
    const response = await fetch(origin + path)
    assert.equal(response.status, 200, path)
    assert.match(await response.text(), /ROUTE_SPA/, path)
    assert.match(response.headers.get('x-robots-tag') || '', /noindex/, path)
    assert.equal(response.headers.get('x-test-upstream'), null, path)
    checks++
  }
  for (const path of ['/', '/blog', '/blog/archive', '/blog/posts/hello', '/blog/topics/java', '/tutorials', '/tutorials/java', '/tutorials/java/first', '/portfolio', '/portfolio/demo', '/about', '/messages', '/robots.txt', '/sitemap.xml']) {
    const response = await fetch(origin + path)
    assert.equal(response.status, 200, path)
    assert.equal(response.headers.get('x-test-upstream'), 'seo', path)
    assert.equal(response.headers.get('x-robots-tag'), null, path)
    checks++
  }
  for (const path of ['/blog/posts/missing', '/tutorials/missing/first', '/portfolio/missing', '/unknown', '/assets/missing.js', '/english/missing.js', '/blog/missing.css', '/missing.png']) {
    assert.equal((await fetch(origin + path)).status, 404, path); checks++
  }
  for (const path of ['/english', '/english/grammar/first', '/terms', '/privacy']) {
    const response = await fetch(origin + path)
    assert.match(await response.text(), /ROUTE_SPA/, path)
    assert.equal(response.headers.get('x-robots-tag'), null, path); checks++
  }
  for (const [oldPath, expected] of [['/useradmin', '/'], ['/useradmin/', '/'], ['/useradmin/center?tab=profile', '/center?tab=profile'], ['/useradmin/reset-password?token=a%2Bb%3D&redirect=%2Fcenter', '/reset-password?token=a%2Bb%3D&redirect=%2Fcenter']]) {
    const response = await fetch(origin + oldPath, { redirect: 'manual' })
    assert.equal(response.status, 308)
    const location = new URL(response.headers.get('location'), origin)
    assert.equal(location.pathname + location.search, expected); checks++
  }
  // A real browser must inherit the fragment across the server's 308 redirect.
  const require = createRequire(resolve('frontend/package.json'))
  const browser = await require('@playwright/test').chromium.launch({ headless: true })
  try {
    const page = await browser.newPage()
    await page.goto(origin + '/useradmin/reset-password?token=a%2Bb%3D#reset')
    assert.equal(new URL(page.url()).pathname, '/reset-password')
    assert.equal(new URL(page.url()).searchParams.get('token'), 'a+b=')
    assert.equal(new URL(page.url()).hash, '#reset'); checks++
  } finally { await browser.close() }
  console.log(JSON.stringify({ checks, privatePaths: privatePaths.length, result: 'passed', production: false }))
} finally {
  spawnSync(nginx, [...args, '-s', 'quit'], { windowsHide: true })
  processHandle.kill()
  await new Promise(resolve => upstream.close(resolve))
}
