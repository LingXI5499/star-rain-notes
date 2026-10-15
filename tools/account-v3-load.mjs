/** Modest, opt-in login/read rehearsal against the explicitly isolated loopback acceptance app. */
import { request } from '../frontend/node_modules/@playwright/test/index.mjs'
import { writeFile } from 'node:fs/promises'
import { performance } from 'node:perf_hooks'

const baseURL = process.env.STAR_RAIN_API_TARGET
if (process.env.STAR_RAIN_ACCOUNT_LOAD_ENABLED !== 'true' || !/^http:\/\/127\.0\.0\.1:\d+$/.test(baseURL || '')
    || !/^jdbc:mysql:\/\/127\.0\.0\.1:13316\/star_rain_ci\?/.test(process.env.STAR_RAIN_DB_URL || '')
    || !process.env.STAR_RAIN_E2E_PASSWORD) {
  throw new Error('Use only the opt-in isolated account acceptance instance and generated fixture credentials')
}
const results = [], started = performance.now()
let next = 1
async function worker() {
  while (next <= 100) {
    const index = next++, client = await request.newContext({ baseURL })
    try {
      const token = (await (await client.get('/api/auth/csrf')).json()).data.token
      const loginStart = performance.now()
      const login = await client.post('/api/auth/login', { headers: { 'X-XSRF-TOKEN': token },
        data: { identifier: `account_v3_load_${String(index).padStart(3, '0')}`, password: process.env.STAR_RAIN_E2E_PASSWORD } })
      results.push({ operation: 'login', status: login.status(), ms: performance.now() - loginStart })
      for (const path of ['/api/account/me', '/api/account/learning/plans', '/api/account/english/vocabulary/memory']) {
        const begin = performance.now(), response = await client.get(path)
        results.push({ operation: path, status: response.status(), ms: performance.now() - begin })
      }
    } finally { await client.dispose() }
  }
}
await Promise.all(Array.from({ length: 4 }, worker))
const summarize = rows => {
  const sorted = rows.map(row => row.ms).sort((a, b) => a - b)
  return { requests: rows.length, errors: rows.filter(row => row.status >= 400).length,
    rateLimited: rows.filter(row => row.status === 429).length,
    medianMs: Math.round(sorted[Math.floor(sorted.length * 0.5)]), p95Ms: Math.round(sorted[Math.ceil(sorted.length * 0.95) - 1]) }
}
const report = { accounts: 100, concurrency: 4, seconds: +(performance.now() - started).toFixed(0) / 1000,
  all: summarize(results), login: summarize(results.filter(row => row.operation === 'login')),
  reads: summarize(results.filter(row => row.operation !== 'login')) }
await writeFile(new URL('../.local/account-v3/load.json', import.meta.url), JSON.stringify(report, null, 2))
console.log(JSON.stringify(report))
if (report.all.errors) process.exitCode = 1
