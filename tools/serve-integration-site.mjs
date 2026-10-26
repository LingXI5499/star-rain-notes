/** Nginx-like loopback host for real integration tests: built assets + actual backend HTML/APIs. */
import http from 'node:http'
import { resolve, sep, extname } from 'node:path'
import { readFile } from 'node:fs/promises'
import { createGzip, gzipSync } from 'node:zlib'
const target = new URL(process.env.STAR_RAIN_API_TARGET || 'http://127.0.0.1:18092')
if (target.protocol !== 'http:' || !['127.0.0.1', 'localhost', '[::1]'].includes(target.hostname)) throw new Error('Integration backend must be loopback HTTP')
const root = resolve('dist'), index = await readFile(resolve(root, 'index.html'))
const types = { '.js': 'application/javascript', '.css': 'text/css', '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png', '.webp': 'image/webp', '.ico': 'image/x-icon', '.html': 'text/html' }
const port = Number(process.env.STAR_RAIN_E2E_PORT || 5189)
// Use the deployment template's management rule so the integration host cannot hide overlap bugs.
const nginx = await readFile(resolve('../backend/deploy/account-v3/nginx-site.example.conf'), 'utf8')
const privateRule = nginx.split('\n').find(line => line.includes('location ~ ^/(?:(?:login'))
if (!privateRule) throw new Error('Management SPA rule missing from deployment template')
const privateSpa = new RegExp(privateRule.match(/location ~ (.+) \{/)[1])
http.createServer(async (req, res) => {
  const path = new URL(req.url, 'http://localhost').pathname
  if (/^\/useradmin(?:\/|$)/.test(path)) {
    res.writeHead(308, { Location: req.url.replace(/^\/useradmin(?=\/|\?|$)/, '').replace(/^\?/, '/?') || '/' }); res.end(); return
  }
  if (privateSpa.test(path) || /^\/portfolio\/[^/]+\/live\/?$/.test(path)) {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store', 'X-Robots-Tag': 'noindex, nofollow' }); res.end(index); return
  }
  const staticAsset = /\.(?:js|mjs|css|map|png|jpg|jpeg|gif|svg|webp|ico|woff2?|ttf|wasm|html)$/i.test(path)
  if (path.startsWith('/api/') || (!staticAsset && (/^\/(?:blog(?:\/|$)|tutorials(?:\/|$)|portfolio(?:\/|$)|messages$|about$|robots\.txt$|sitemap\.xml$)/.test(path) || path === '/'))) {
    const upstream = http.request(new URL(req.url, target), { method: req.method, headers: { ...req.headers, 'accept-encoding': 'identity' } }, reply => {
      const headers = { ...reply.headers }, compress = /gzip/.test(req.headers['accept-encoding'] || '') && /text|json|javascript/.test(headers['content-type'] || '')
      if (compress) { headers['content-encoding'] = 'gzip'; delete headers['content-length']; headers.vary = 'Accept-Encoding' }
      res.writeHead(reply.statusCode, headers)
      if (compress) reply.pipe(createGzip()).pipe(res); else reply.pipe(res)
    })
    upstream.on('error', () => { if (!res.headersSent) res.writeHead(503); res.end('Isolated backend unavailable') })
    req.pipe(upstream); return
  }
  const file = resolve(root, '.' + decodeURIComponent(path))
  if (!file.startsWith(root + sep)) { res.writeHead(404).end(); return }
  try {
    const bytes = await readFile(file), type = types[extname(file)] || 'application/octet-stream'
    const compress = /gzip/.test(req.headers['accept-encoding'] || '') && /text|json|javascript|svg/.test(type)
    res.writeHead(200, { 'Content-Type': type, 'Cache-Control': path.startsWith('/assets/') ? 'public,max-age=31536000,immutable' : 'no-cache', ...(compress ? { 'Content-Encoding': 'gzip' } : {}) })
    res.end(compress ? gzipSync(bytes) : bytes)
  } catch {
    const knownSpa = !staticAsset && /^\/(?:english|terms|privacy|search)(?:\/|$)/.test(path)
    res.writeHead(knownSpa ? 200 : 404, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' }); res.end(index)
  }
}).listen(port, '127.0.0.1', () => console.log('Isolated integration site ready on ' + port))
