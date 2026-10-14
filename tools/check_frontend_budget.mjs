/** Controlled build budgets; these are artifact sizes, not production Web Vitals. */
import { readFile, readdir } from 'node:fs/promises'
import { gzipSync } from 'node:zlib'
const html = await readFile('dist/index.html', 'utf8')
const entry = html.match(/<script[^>]*src="(\/assets\/[^\"]+\.js)"/)?.[1]
if (!entry) throw new Error('Missing built entry script')
const results = []
async function check(file, maxDecoded, maxGzip) {
  const bytes = await readFile('dist' + file), compressed = gzipSync(bytes).length
  results.push({ file, decoded: bytes.length, gzip: compressed })
  if (bytes.length > maxDecoded || compressed > maxGzip) throw new Error('Frontend budget exceeded: ' + file)
}
await check(entry, 280 * 1024, 100 * 1024)
for (const file of await readdir('dist/assets')) if (/^BlogProse-.*\.js$/.test(file)) await check('/assets/' + file, 260 * 1024, 110 * 1024)
console.log(JSON.stringify(results, null, 2))
