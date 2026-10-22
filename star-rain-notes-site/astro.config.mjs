import { defineConfig } from 'astro/config';
import vue from '@astrojs/vue';
import sitemap from '@astrojs/sitemap';
import fs from 'node:fs';
import { emptyEnglishPaths } from './src/utils/english-model.ts';
const site = JSON.parse(fs.readFileSync(new URL('./src/content-data/site.json', import.meta.url), 'utf8'));
const english = name => JSON.parse(fs.readFileSync(new URL(`./src/content-data/english/${name}.json`, import.meta.url),'utf8'));
const vocabularyEntries=english('vocabulary-entries').entries;
const excludedEnglish = new Set(emptyEnglishPaths(english('vocabulary-taxonomy'),vocabularyEntries,english('world-taxonomy'),english('world-articles').articles,english('essays').articles));
for(const theme of english('themes'))if(!vocabularyEntries.some(e=>e.source.legacyThemeId===theme.id))excludedEnglish.add(theme.path);
const siteUrl = process.env.SITE_URL || site.url;
export default defineConfig({
  site: siteUrl,
  output: 'static',
  trailingSlash: 'always',
  integrations: [vue(), sitemap({ filter: url => !excludedEnglish.has(new URL(url).pathname)&&!['/404/', '/404.html', '/search/', '/privacy/', '/terms/'].some(p => new URL(url).pathname === p) })],
  vite: { build: { chunkSizeWarningLimit: 650 } },
  devToolbar: { enabled: false },
});
