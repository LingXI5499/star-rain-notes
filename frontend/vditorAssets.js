import { readFileSync, readdirSync, statSync } from 'node:fs'
import { resolve, sep, extname } from 'node:path'

const root = resolve('node_modules/vditor/dist')
const prefix = '/vendor/vditor-3.11.3/dist/'
const entries = ['js/lute', 'js/i18n/zh_CN.js', 'js/icons/ant.js', 'js/highlight.js', 'css/content-theme']
export function vditorAssets() {
  function allowed(path) { return entries.some(entry => path === entry || path.startsWith(entry + '/')) }
  return {
    name: 'local-vditor-runtime',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (!req.url?.startsWith(prefix)) return next()
        let path
        try { path = decodeURIComponent(req.url.split('?')[0].slice(prefix.length)) } catch { res.statusCode = 400; res.end(); return }
        const file = resolve(root, path)
        if (!file.startsWith(root + sep) || !allowed(path)) { res.statusCode = 404; res.end(); return }
        try { res.setHeader('Content-Type', extname(file) === '.js' ? 'application/javascript' : extname(file) === '.css' ? 'text/css' : 'application/octet-stream'); res.end(readFileSync(file)) }
        catch { res.statusCode = 404; res.end() }
      })
    },
    generateBundle() {
      const emit = path => {
        const file = resolve(root, path)
        if (statSync(file).isDirectory()) for (const name of readdirSync(file)) emit(path + '/' + name)
        else this.emitFile({ type: 'asset', fileName: prefix.slice(1) + path, source: readFileSync(file) })
      }
      entries.forEach(emit)
    },
  }
}
