<script setup>
/**
 * SiteSettingsPage.vue：站点模块的页面组件。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、../../../media/components/MediaPicker.vue、../../../media/support/useMediaPicker、../../../../shared/http、../../support/siteBranding、../../../tutorial/api/tutorialApi。
 * 主要响应式状态：loading、busy、error、notice、config、sections、selectionOptions、form。
 * 阅读入口：fillConfig（fill配置的处理函数）；fillSections（fillSections的处理函数）；load（加载当前操作）；run（运行当前操作）；saveConfig（保存配置）。
 */


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

/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(true)
/**
 * busy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busy = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notice = ref('')
/**
 * 配置；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const config = ref(null)
/**
 * sections；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const sections = ref([])
/**
 * 选择结果选项；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const selectionOptions = reactive({ TUTORIALS: [], PORTFOLIO: [] })
/**
 * form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const form = reactive({ siteName: '', siteTitle: '', tagline: '', siteDescription: '', homeIntro: '', footerText: '' })
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()

/**
 * fill配置的处理函数。
 * 主要协作：applySiteBranding。
 * 更新的响应式状态包括 config；渲染由 Vue 的依赖跟踪触发。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 */
function fillConfig(value) {
  config.value = value
  applySiteBranding(value)
  for (const key of Object.keys(form)) form[key] = value[key] || ''
}

/**
 * fillSections的处理函数。
 * 主要协作：value.map、JSON.parse、.includes、display.selectedIds.map。
 * 更新的响应式状态包括 sections；渲染由 Vue 的依赖跟踪触发。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function fillSections(value) {
  sections.value = value.map((section) => {
    let display = {}
    try { display = JSON.parse(section.configJson || '{}') } catch { /* 后端校验负责拦截无效配置 */ }
    return { ...section, limit: ['TUTORIALS', 'PORTFOLIO'].includes(section.sectionCode) ? 3 : display.limit || 6,
      selectedIds: Array.isArray(display.selectedIds) ? display.selectedIds.map(String) : ['', '', ''],
      layout: display.layout || (section.sectionCode === 'HERO' ? 'hero' : ['BLOG', 'LATEST'].includes(section.sectionCode) ? 'list' : 'cards') }
  })
}

/**
 * 加载当前操作。
 * 主要协作：Promise.all、getPublicSiteConfig、listHomeSections、listPublicTutorials、listPublicWorks。
 * 更新的响应式状态包括 loading、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 运行当前操作。
 * 主要协作：action、errorMessage。
 * 更新的响应式状态包括 busy、error、notice；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param action 动作；默认值、解构字段与可选参数见函数签名。
 * @param success success；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function run(action, success) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try { await action(); notice.value = success }
  catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}

/**
 * 保存配置。
 * 主要协作：run、fillConfig、patchSiteConfig。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function saveConfig() {
  await run(async () => { fillConfig(await patchSiteConfig({ ...form })) }, '站点配置已保存')
}

/**
 * choose媒体的处理函数。
 * 主要协作：pick、run、fillConfig、setSiteMedia。
 * 更新的响应式状态包括 error；渲染由 Vue 的依赖跟踪触发。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function chooseMedia(kind) {
  const asset = await pick('IMAGE')
  if (!asset) return
  if (asset.accessLevel !== 'PUBLIC') {
    error.value = '站点图片必须设为公开。请先到媒体库调整访问级别。'
    return
  }
  await run(async () => { fillConfig(await setSiteMedia(kind, asset.id)) }, kind === 'logo' ? 'Logo 已更新' : '网站图标已更新')
}

/**
 * 移除媒体。
 * 主要协作：run、fillConfig、clearSiteMedia。
 * @param kind kind；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeMedia(kind) {
  await run(async () => { fillConfig(await clearSiteMedia(kind)) }, '媒体引用已移除')
}

/**
 * move的处理函数。
 * 主要协作：sections.value.map、run、fillSections、orderHomeSections。
 * @param index 索引；默认值、解构字段与可选参数见函数签名。
 * @param delta delta；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function move(index, delta) {
  const target = index + delta
  if (target < 0 || target >= sections.value.length) return
  const codes = sections.value.map((section) => section.sectionCode)
  ;[codes[index], codes[target]] = [codes[target], codes[index]]
  await run(async () => { fillSections(await orderHomeSections(codes)) }, '首页顺序已更新')
}

/**
 * 切换当前操作。
 * 主要协作：run、patchHomeSection、fillSections、listHomeSections。
 * @param section 区块；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function toggle(section) {
  await run(async () => {
    await patchHomeSection(section.sectionCode, { enabled: !section.enabled })
    fillSections(await listHomeSections())
  }, '区块状态已更新')
}

/**
 * 保存区块。
 * 主要协作：.includes、.map、selectedIds.some、Number.isSafeInteger、Number.isInteger。
 * 更新的响应式状态包括 error；渲染由 Vue 的依赖跟踪触发。
 * @param section 区块；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
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
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.site-settings。
 */
.site-settings { max-width: 1100px; padding: 12px 0 72px; }
/**
 * 样式范围：.site-settings h1。
 */
.site-settings h1 { margin: 10px 0; font-size: clamp(28px, 3vw, 40px); }
/**
 * 样式范围：.site-settings__lead。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings__lead { color: var(--text-secondary); margin-bottom: 26px; }
/**
 * 样式范围：.site-settings__notice。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings__notice { color: var(--primary); margin: 12px 0; }
/**
 * 样式范围：.site-settings__panel。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings__panel { margin: 20px 0; padding: clamp(20px, 3vw, 32px); border: 1px solid var(--border); border-radius: 18px; background: var(--bg-surface); }
/**
 * 样式范围：.site-settings__panel h2。
 */
.site-settings__panel h2 { margin: 0 0 18px; font-size: 22px; }
/**
 * 样式范围：.site-settings__panel > p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings__panel > p { color: var(--text-secondary); margin-bottom: 18px; }
/**
 * 样式范围：.site-settings__fields, .site-settings__section-fields。
 * 使用网格布局组织内容；列定义为 repeat(2, minmax(0, 1fr))，间距由 14px 控制。
 */
.site-settings__fields, .site-settings__section-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
/**
 * 样式范围：.site-settings__wide。
 */
.site-settings__wide { grid-column: 1 / -1; }
/**
 * 样式范围：.site-settings label。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings label { display: grid; gap: 6px; color: var(--text-secondary); font-size: 13px; }
/**
 * 样式范围：.site-settings input, .site-settings textarea, .site-settings select。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings input, .site-settings textarea, .site-settings select { width: 100%; padding: 11px 12px; border: 1px solid var(--border); border-radius: 9px; color: var(--text-primary); background: var(--bg-page); font: inherit; }
/**
 * 样式范围：.site-settings button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings button { padding: 9px 14px; border: 1px solid var(--border); border-radius: 9px; color: var(--text-primary); background: var(--bg-surface); cursor: pointer; }
/**
 * 样式范围：.site-settings button:hover:not(:disabled)。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings button:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
/**
 * 样式范围：.site-settings button:disabled。
 */
.site-settings button:disabled { opacity: .5; cursor: not-allowed; }
/**
 * 样式范围：.site-settings__fields + button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings__fields + button { margin-top: 20px; background: var(--primary); color: white; }
/**
 * 样式范围：.site-settings__media。
 * 使用网格布局组织内容；列定义为 repeat(2, 1fr)，间距由 20px 控制。
 */
.site-settings__media { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
/**
 * 样式范围：.site-settings__media > div。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings__media > div { padding: 18px; border: 1px solid var(--border); border-radius: 12px; }
/**
 * 样式范围：.site-settings__media img。
 */
.site-settings__media img { display: block; width: 64px; height: 64px; object-fit: contain; margin: 14px 0; }
/**
 * 样式范围：.site-settings__media p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings__media p { margin: 14px 0; color: var(--text-muted); }
/**
 * 样式范围：.site-settings__media button + button。
 */
.site-settings__media button + button { margin-left: 8px; }
/**
 * 样式范围：.site-settings__sections。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 12px 控制。
 */
.site-settings__sections { display: grid; gap: 12px; margin: 0; padding: 0; list-style: none; }
/**
 * 样式范围：.site-settings__sections > li。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings__sections > li { padding: 16px; border: 1px solid var(--border); border-radius: 12px; }
/**
 * 样式范围：.site-settings__section-head。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.site-settings__section-head { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
/**
 * 样式范围：.site-settings__section-head strong。
 */
.site-settings__section-head strong { margin-right: auto; }
/**
 * 样式范围：.site-settings__section-head span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings__section-head span { color: var(--text-muted); font-size: 12px; }
/**
 * 样式范围：.site-settings__section-fields。
 */
.site-settings__section-fields { grid-template-columns: 2fr 1fr 1fr auto; align-items: end; }
/**
 * 样式范围：.site-settings__curated。
 * 使用网格布局组织内容；列定义为 repeat(3, minmax(0, 1fr))，间距由 10px 控制。
 */
.site-settings__curated { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
/**
 * 样式范围：.site-settings__curated p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-settings__curated p { grid-column: 1 / -1; margin: 0; color: var(--text-secondary); }
/**
 * 响应式或设备条件：(max-width: 720px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 720px) {
  /**
   * 样式范围：.site-settings__fields, .site-settings__media, .site-settings__section-fields。
   */
  .site-settings__fields, .site-settings__media, .site-settings__section-fields { grid-template-columns: 1fr; } }
</style>
