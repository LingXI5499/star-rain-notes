import { defineConfig } from 'astro/config';
import vue from '@astrojs/vue';
import sitemap from '@astrojs/sitemap';
import fs from 'node:fs';
const site = JSON.parse(fs.readFileSync(new URL('./src/content-data/site.json', import.meta.url), 'utf8'));
const siteUrl = process.env.SITE_URL || site.url;
export default defineConfig({
  site: siteUrl,
  output: 'static',
  trailingSlash: 'always',
  integrations: [vue(), sitemap({ filter: url => !['/404/', '/404.html', '/search/', '/privacy/', '/terms/'].some(p => new URL(url).pathname === p) })],
  vite: { build: { chunkSizeWarningLimit: 650 } },
  devToolbar: { enabled: false },
});
