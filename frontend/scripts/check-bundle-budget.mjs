import { gzipSync } from 'node:zlib'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'

const root = join(process.cwd(), 'dist', 'assets')
const files = readdirSync(root).filter((name) => name.endsWith('.js'))
const initial = files.filter((name) => name.startsWith('index-') || name.startsWith('vue-vendor-'))
const bytes = initial.reduce((total, name) => total + gzipSync(readFileSync(join(root, name))).length, 0)
const limit = 110 * 1024
if (bytes > limit) throw new Error(`Public critical JavaScript is ${(bytes / 1024).toFixed(1)}KB gzip; budget is 110KB.`)
const element = files.find((name) => name.startsWith('element-plus-'))
if (element && gzipSync(readFileSync(join(root, element))).length > 160 * 1024) throw new Error('Element Plus lazy chunk exceeds 160KB gzip.')
console.log(`Bundle budget passed: ${(bytes / 1024).toFixed(1)}KB gzip public critical JS.`)
