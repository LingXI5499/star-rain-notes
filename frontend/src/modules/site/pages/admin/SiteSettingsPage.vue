<script setup>
import { onMounted, reactive, ref } from 'vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'
import { errorMessage } from '../../../../shared/http'
import { applySiteBranding } from '../../support/siteBranding'
import { listPublicTutorials } from '../../../tutorial/api/tutorialApi'
import { listPublicWorks } from '../../../portfolio/api/portfolioApi'
import {
  clearSiteMedia, getPublicSiteConfig, listHomeSections, orderHomeSections,
  patchHomeSection, patchSiteConfig, setSiteMedia,
} from '../../api/siteApi'

const loading = ref(true)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const config = ref(null)
const sections = ref([])
const selectionOptions = reactive({ TUTORIALS: [], PORTFOLIO: [] })
const form = reactive({ siteName: '', siteTitle: '', tagline: '', siteDescription: '', homeIntro: '', footerText: '' })
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()

function fillConfig(value) {
  config.value = value
  applySiteBranding(value)
  for (const key of Object.keys(form)) form[key] = value[key] || ''
}

function fillSections(value) {
  sections.value = value.map((section) => {
    let display = {}
    try { display = JSON.parse(section.configJson || '{}') } catch { /* 后端校验负责拦截无效配置 */ }
    return { ...section, limit: ['TUTORIALS', 'PORTFOLIO'].includes(section.sectionCode) ? 3 : display.limit || 6,
      selectedIds: Array.isArray(display.selectedIds) ? display.selectedIds.map(String) : ['', '', ''],
      layout: display.layout || (section.sectionCode === 'HERO' ? 'hero' : ['BLOG', 'LATEST'].includes(section.sectionCode) ? 'list' : 'cards') }
  })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [site, order, tutorialPage, workPage] = await Promise.all([
      getPublicSiteConfig(), listHomeSections(),
      listPublicTutorials({ page: 1, pageSize: 100 }), listPublicWorks({ page: 1, pageSize: 100 }),
    ])
    fillConfig(site)
    fillSections(order)
    selectionOptions.TUTORIALS = tutorialPage.items || []
    selectionOptions.PORTFOLIO = workPage.items || []
  } catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}

async function run(action, success) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try { await action(); notice.value = success }
  catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

async function saveConfig() {
  await run(async () => { fillConfig(await patchSiteConfig({ ...form })) }, '站点配置已保存')
}

async function chooseMedia(kind) {
  const asset = await pick('IMAGE')
  if (!asset) return
  if (asset.accessLevel !== 'PUBLIC') {
    error.value = '站点图片必须设为公开。请先到媒体库调整访问级别。'
    return
  }
  await run(async () => { fillConfig(await setSiteMedia(kind, asset.id)) }, kind === 'logo' ? 'Logo 已更新' : '网站图标已更新')
}

async function removeMedia(kind) {
  await run(async () => { fillConfig(await clearSiteMedia(kind)) }, '媒体引用已移除')
}

async function move(index, delta) {
  const target = index + delta
  if (target < 0 || target >= sections.value.length) return
  const codes = sections.value.map((section) => section.sectionCode)
  ;[codes[index], codes[target]] = [codes[target], codes[index]]
  await run(async () => { fillSections(await orderHomeSections(codes)) }, '首页顺序已更新')
}

async function toggle(section) {
  await run(async () => {
    await patchHomeSection(section.sectionCode, { enabled: !section.enabled })
    fillSections(await listHomeSections())
  }, '区块状态已更新')
}

async function saveSection(section) {
  const curated = ['TUTORIALS', 'PORTFOLIO'].includes(section.sectionCode)
  const selectedIds = (section.selectedIds || []).map(Number)
  if (curated && (selectedIds.length !== 3 || selectedIds.some((id) => !Number.isSafeInteger(id) || id <= 0)
    || new Set(selectedIds).size !== 3)) {
    error.value = '请为该区块选择三个不同的公开内容。'
    return
  }
  const limit = Number(section.limit)
  if (!Number.isInteger(limit) || limit < 1 || limit > 12) {
    error.value = '展示数量须在 1 到 12 之间。'
    return
  }
  const config = ['HERO', 'PROFILE'].includes(section.sectionCode) ? {}
    : curated ? { limit: 3, layout: section.layout, selectedIds }
      : section.sectionCode === 'BLOG' ? { limit } : { limit, layout: section.layout }
  await run(async () => {
    await patchHomeSection(section.sectionCode, {
      displayName: section.displayName,
      config,
    })
    fillSections(await listHomeSections())
  }, '区块配置已保存')
}

onMounted(load)
</script>

<template>
  <main class="site-settings">
    <p class="public-eyebrow">SITE SETTINGS · 站点治理</p>
    <h1>站点设置</h1>
    <p class="site-settings__lead">管理公开站点名称、首页介绍和首页区块的显示顺序。</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="site-settings__notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在读取站点设置…</p>

    <template v-if="!loading && config">
      <form class="site-settings__panel" @submit.prevent="saveConfig">
        <h2>基本信息</h2>
        <div class="site-settings__fields">
          <label>站点名称<input v-model.trim="form.siteName" required maxlength="120" /></label>
          <label>首页主标题<input v-model.trim="form.siteTitle" required maxlength="255" /></label>
          <label>首页副标题<input v-model.trim="form.tagline" maxlength="255" placeholder="显示在主标题下方的一行绿色标语" /></label>
          <label class="site-settings__wide">站点描述<textarea v-model.trim="form.siteDescription" maxlength="1000" rows="2" /></label>
          <label class="site-settings__wide">首页介绍<textarea v-model.trim="form.homeIntro" maxlength="2000" rows="3" /></label>
          <label class="site-settings__wide">页脚文字<textarea v-model.trim="form.footerText" maxlength="1000" rows="2" /></label>
        </div>
        <button type="submit" :disabled="busy">保存基本信息</button>
      </form>

      <section class="site-settings__panel" aria-labelledby="site-media-title">
        <h2 id="site-media-title">站点图片</h2>
        <div class="site-settings__media">
          <div>
            <h3>Logo</h3>
            <img v-if="config.logoUrl" :src="config.logoUrl" alt="当前 Logo" />
            <p v-else>当前使用默认标识。</p>
            <button type="button" :disabled="busy" @click="chooseMedia('logo')">选择图片</button>
            <button v-if="config.logoUrl" type="button" :disabled="busy" @click="removeMedia('logo')">恢复默认</button>
          </div>
          <div>
            <h3>网站图标</h3>
            <img v-if="config.faviconUrl" :src="config.faviconUrl" alt="当前网站图标" />
            <p v-else>当前使用默认图标。</p>
            <button type="button" :disabled="busy" @click="chooseMedia('favicon')">选择图片</button>
            <button v-if="config.faviconUrl" type="button" :disabled="busy" @click="removeMedia('favicon')">恢复默认</button>
          </div>
        </div>
      </section>

      <section class="site-settings__panel" aria-labelledby="site-sections-title">
        <h2 id="site-sections-title">首页区块</h2>
        <p>从上到下排列。关闭后该区块不会出现在公开首页。</p>
        <ol class="site-settings__sections">
          <li v-for="(section, index) in sections" :key="section.sectionCode">
            <div class="site-settings__section-head">
              <strong>{{ section.sectionCode }}</strong>
              <span>{{ section.enabled ? '展示中' : '已隐藏' }}</span>
              <button type="button" :disabled="busy || index === 0" @click="move(index, -1)">上移</button>
              <button type="button" :disabled="busy || index === sections.length - 1" @click="move(index, 1)">下移</button>
              <button type="button" :disabled="busy" @click="toggle(section)">{{ section.enabled ? '隐藏' : '显示' }}</button>
            </div>
            <form class="site-settings__section-fields" @submit.prevent="saveSection(section)">
              <label>标题<input v-model.trim="section.displayName" maxlength="120" required /></label>
              <label v-if="!['HERO', 'PROFILE', 'TUTORIALS', 'PORTFOLIO'].includes(section.sectionCode)">展示数量<input v-model.number="section.limit" type="number" min="1" max="12" /></label>
              <label v-if="['TUTORIALS', 'PORTFOLIO', 'HOT_CONTENT'].includes(section.sectionCode)">布局<select v-model="section.layout"><option value="cards">卡片</option><option value="list">列表</option></select></label>
              <div v-if="['TUTORIALS', 'PORTFOLIO'].includes(section.sectionCode)" class="site-settings__curated">
                <p>按展示顺序选择三个已公开内容</p>
                <label v-for="slot in 3" :key="slot">第 {{ slot }} 项
                  <select v-model="section.selectedIds[slot - 1]" required>
                    <option value="">请选择</option>
                    <option v-for="item in selectionOptions[section.sectionCode]" :key="item.id" :value="String(item.id)">{{ item.title }}</option>
                  </select>
                </label>
              </div>
              <button type="submit" :disabled="busy">保存区块</button>
            </form>
          </li>
        </ol>
      </section>
    </template>
    <MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
  </main>
</template>

<style scoped>
.site-settings { max-width: 1100px; padding: 12px 0 72px; }
.site-settings h1 { margin: 10px 0; font-size: clamp(28px, 3vw, 40px); }
.site-settings__lead { color: var(--text-secondary); margin-bottom: 26px; }
.site-settings__notice { color: var(--primary); margin: 12px 0; }
.site-settings__panel { margin: 20px 0; padding: clamp(20px, 3vw, 32px); border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); }
.site-settings__panel h2 { margin: 0 0 18px; font-size: 22px; }
.site-settings__panel > p { color: var(--text-secondary); margin-bottom: 18px; }
.site-settings__fields, .site-settings__section-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.site-settings__wide { grid-column: 1 / -1; }
.site-settings label { display: grid; gap: 6px; color: var(--text-secondary); font-size: 13px; }
.site-settings input, .site-settings textarea, .site-settings select { width: 100%; padding: 11px 12px; border: 1px solid var(--border); border-radius: 9px; color: var(--text-primary); background: var(--bg-page); font: inherit; }
.site-settings button { padding: 9px 14px; border: 1px solid var(--border); border-radius: 9px; color: var(--text-primary); background: var(--bg-surface); cursor: pointer; }
.site-settings button:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.site-settings button:disabled { opacity: .5; cursor: not-allowed; }
.site-settings__fields + button { margin-top: 20px; background: var(--primary); color: white; }
.site-settings__media { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.site-settings__media > div { padding: 18px; border: 1px solid var(--border); border-radius: 12px; }
.site-settings__media img { display: block; width: 64px; height: 64px; object-fit: contain; margin: 14px 0; }
.site-settings__media p { margin: 14px 0; color: var(--text-muted); }
.site-settings__media button + button { margin-left: 8px; }
.site-settings__sections { display: grid; gap: 12px; margin: 0; padding: 0; list-style: none; }
.site-settings__sections > li { padding: 16px; border: 1px solid var(--border); border-radius: 12px; }
.site-settings__section-head { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.site-settings__section-head strong { margin-right: auto; }
.site-settings__section-head span { color: var(--text-muted); font-size: 12px; }
.site-settings__section-fields { grid-template-columns: 2fr 1fr 1fr auto; align-items: end; }
.site-settings__curated { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.site-settings__curated p { grid-column: 1 / -1; margin: 0; color: var(--text-secondary); }
@media (max-width: 720px) { .site-settings__fields, .site-settings__media, .site-settings__section-fields { grid-template-columns: 1fr; } }
</style>
