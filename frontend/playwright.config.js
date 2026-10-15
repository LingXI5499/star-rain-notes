/** Browser regressions run only against an automatically managed loopback Vite server. */
import { defineConfig, devices } from '@playwright/test'
export default defineConfig({
  testDir: './e2e', timeout: 45000, fullyParallel: true,
  testIgnore: process.env.STAR_RAIN_E2E_REAL ? ['**/reliability.spec.js', '**/editor-v3.spec.js', '**/editor-selection.spec.js'] : ['**/real-backend.spec.js', '**/account-real.spec.js'],
  forbidOnly: Boolean(process.env.CI), retries: process.env.CI ? 1 : 0, workers: 2,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: { baseURL: 'http://127.0.0.1:5189', trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], channel: process.env.PLAYWRIGHT_CHANNEL || 'chromium' } }],
  webServer: { command: process.env.STAR_RAIN_E2E_REAL ? 'node ../tools/serve-integration-site.mjs' : 'npm run dev -- --port 5189', url: 'http://127.0.0.1:5189',
    reuseExistingServer: false, timeout: 60000 },
})
