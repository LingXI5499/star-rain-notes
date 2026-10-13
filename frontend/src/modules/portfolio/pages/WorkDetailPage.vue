<script setup>
/**
 * WorkDetailPage.vue：作品模块的页面组件。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 根据路由区分公开详情与后台预览；有可见区块时渲染区块，否则渲染 Markdown 正文。
 * 详情不展示封面和在线访问入口，仅显示启用且 URL 安全的代码库链接；左栏目录跟随正文结构。
 * 主要依赖：vue、vue-router、../api/portfolioApi、../../../shared/http、../../../shared/viewMode、../../blog/components/BlogProse.vue。
 * 主要响应式状态：route、work、outline、error、loading、links、navigation、stats。
 * 阅读入口：load（加载当前操作）。
 */


import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getPublicWork, getAdminWork } from '../api/portfolioApi'
import { errorMessage } from '../../../shared/http'
import { useViewMode } from '../../../shared/viewMode'
import BlogProse from '../../blog/components/BlogProse.vue'
import ReadingAside from '../../blog/components/ReadingAside.vue'
import ReadingDrawer from '../../blog/components/ReadingDrawer.vue'
import { estimateReadingStats } from '../../blog/support/display'
import WorkSectionRenderer from '../components/WorkSectionRenderer.vue'
import { stageLabels, safeWorkUrl } from '../support/workBlocks'
import '../../../styles/reading-layout.css'

/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
const { contentPath } = useViewMode()
/**
 * 作品；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const work = ref(null)
/**
 * 正文目录；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const outline = ref([])
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 链接列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const links = computed(() => work.value?.links?.filter(item => item.enabled && item.linkType === 'GITHUB' && safeWorkUrl(item.url)) || [])
/**
 * navigation；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const navigation = computed(() => work.value?.hasSections
  ? (work.value.sections || []).filter(item => item.visible !== false && item.title).map(item => ({ id: `work-section-${item.id}`, text: item.title, level: 2 }))
  : outline.value)
/**
 * 统计字段；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const stats = computed(() => estimateReadingStats(work.value?.hasSections
  ? (work.value.sections || []).filter(item => item.visible !== false).map(item => `${item.title || ''}\n${item.content || ''}`).join('\n')
  : work.value?.bodyMarkdown))
/**
 * 预览；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const preview = computed(() => !!route.params.workId)
/**
 * 类型标签；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const typeLabel = computed(() => ({ SOFTWARE: '软件', VIDEO: '视频', MUSIC: '音乐', WRITING: '写作', OTHER: '作品' }[work.value?.workType] || '作品'))

/**
 * 加载当前操作。
 * 主要协作：getAdminWork、getPublicWork、errorMessage。
 * 更新的响应式状态包括 loading、error、work、outline；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
let requestGeneration = 0
onBeforeUnmount(() => { requestGeneration += 1 })
async function load() {
  const generation = ++requestGeneration
  const { slug, workId } = route.params
  loading.value = true
  error.value = ''
  work.value = null
  outline.value = []
  try {
    const result = await (workId ? getAdminWork(workId) : getPublicWork(slug))
    if (generation !== requestGeneration) return
    work.value = result
  } catch (cause) {
    if (generation !== requestGeneration) return
    error.value = cause?.response?.status === 404 ? '作品不存在或尚未公开。' : errorMessage(cause)
  } finally {
    if (generation === requestGeneration) loading.value = false
  }
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => [route.params.slug, route.params.workId], load, { immediate: true })
</script>

<template>
  <article class="work-detail page-container">
    <p v-if="loading" class="work-detail__state">正在加载作品…</p>
    <p v-else-if="error" class="work-detail__state" role="alert">{{ error }} <RouterLink :to="contentPath('/portfolio')">返回作品列表</RouterLink></p>
    <template v-else-if="work">
      <p v-if="preview" class="work-preview">作品预览 · 仅展示可见区块，不改变发布状态</p>
      <ReadingDrawer :key="work.id || work.slug" :items="navigation" />
      <div class="work-detail__reading reading-layout">
        <ReadingAside class="work-detail__navigation reading-layout__sidebar" navigation-style="curriculum" :items="navigation" :char-count="stats.charCount" :read-minutes="stats.readMinutes" />
        <div class="work-detail__body">
      <header class="work-detail__hero">
        <RouterLink :to="contentPath('/portfolio')">← 全部作品</RouterLink>
        <p class="public-eyebrow">SELECTED WORK · {{ typeLabel }}</p>
        <h1 class="public-display">{{ work.title }}</h1>
        <p v-if="work.subtitle" class="work-detail__subtitle">{{ work.subtitle }}</p><div class="work-detail__meta"><span v-if="work.category">{{ work.category.name }}</span><span v-if="work.format">{{ work.format.name }}</span><span>{{ stageLabels[work.projectStatus] }}</span><span v-if="work.role">{{ work.role }}</span></div><div class="work-detail__tags"><span v-for="tag in work.tags || []" :key="tag.id">{{ tag.name }}</span></div><p class="work-detail__summary">{{ work.summary }}</p>
        <div v-if="links.length" class="work-detail__links">
          <a v-for="link in links" :key="link.id" :href="link.url" target="_blank" rel="noopener noreferrer">代码库{{ links.length > 1 && link.label ? ` · ${link.label}` : '' }} ↗</a>
        </div>
      </header>
          <WorkSectionRenderer v-if="work.hasSections" :sections="work.sections" /><template v-else><BlogProse :markdown="work.bodyMarkdown || ''" @outline="outline = $event" /><div class="work-detail__gallery"><figure v-for="item in work.media?.filter(m => m.usageType === 'SCREENSHOT') || []" :key="item.id"><img :src="item.url" :alt="item.caption || work.title" /><figcaption>{{ item.caption }}</figcaption></figure></div><audio v-for="item in work.media?.filter(m => m.usageType === 'AUDIO') || []" :key="item.id" :src="item.url" controls /></template>
      <nav v-if="!preview" class="work-neighbors" aria-label="前后作品"><RouterLink v-if="work.previousWork" :to="contentPath(`/portfolio/${work.previousWork.slug}`)">← {{ work.previousWork.title }}</RouterLink><RouterLink v-if="work.nextWork" :to="contentPath(`/portfolio/${work.nextWork.slug}`)">{{ work.nextWork.title }} →</RouterLink></nav>
        </div>
      </div>
    </template>
  </article>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.work-detail__mobile-navigation。
 */
.work-detail__mobile-navigation{display:none}
  /**
   * 样式范围：.work-detail__mobile-navigation summary。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-detail__mobile-navigation summary{cursor:pointer;color:var(--primary);font-weight:600}
/**
 * 样式范围：.work-detail。
 */
.work-detail{width:100%;padding-block:34px 90px}
  /**
   * 样式范围：.work-detail__hero。
   */
  .work-detail__hero{padding:20px 0 40px;text-align:center}
  /**
   * 样式范围：.work-detail__hero>a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-detail__hero>a{display:inline-block;margin-bottom:45px;color:var(--text-muted);text-decoration:none}
  /**
   * 样式范围：.work-detail__hero .public-eyebrow。
   */
  .work-detail__hero .public-eyebrow{margin-bottom:15px}
  /**
   * 样式范围：.work-detail__hero h1。
   */
  .work-detail__hero h1{margin:0}
  /**
   * 样式范围：.work-detail__summary。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-detail__summary{margin:20px auto 0;max-width:680px;color:var(--text-secondary);font-size:18px;line-height:1.8}
  /**
   * 样式范围：.work-detail__links。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .work-detail__links{display:flex;justify-content:center;gap:12px;flex-wrap:wrap;margin-top:30px}
  /**
   * 样式范围：.work-detail__links a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-detail__links a{padding:10px 17px;border:1px solid var(--border);border-radius:999px;color:var(--primary);text-decoration:none}
  /**
   * 样式范围：.work-detail__gallery。
   * 使用网格布局组织内容；列定义为 repeat(2,1fr)，间距由 18px 控制。
   */
  .work-detail__gallery{display:grid;grid-template-columns:repeat(2,1fr);gap:18px;margin-top:24px}
  /**
   * 样式范围：.work-detail__gallery figure。
   */
  .work-detail__gallery figure{margin:0}
  /**
   * 样式范围：.work-detail__gallery img。
   */
  .work-detail__gallery img{width:100%;border-radius:14px}
  /**
   * 样式范围：.work-detail__gallery figcaption。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-detail__gallery figcaption{padding:7px;color:var(--text-muted);font-size:12px}
  /**
   * 样式范围：.work-detail__reading main。
   */
  .work-detail__body{min-width:0}
  /**
   * 样式范围：.work-detail__reading aside。
   * 在正常文档流中保留位置，吸附偏移由 top=100px 决定；父级高度限制吸附范围。
   */
  /**
   * 样式范围：.work-detail__state。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-detail__state{padding:100px 20px;color:var(--text-muted);text-align:center}
  /**
   * 样式范围：.work-detail__state a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-detail__state a{margin-left:8px;color:var(--primary)}
  /**
   * 响应式或设备条件：(max-width:720px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:620px){
  /**
   * 样式范围：.work-detail__gallery。
   */
  .work-detail__gallery{grid-template-columns:1fr}}
/**
 * 样式范围：.work-detail__meta。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.work-detail__meta{display:flex;justify-content:center;gap:12px;flex-wrap:wrap;margin:20px auto 0;color:var(--text-muted);font-size:13px}
  /**
   * 样式范围：.work-detail__meta span+span::before。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-detail__meta span+span::before{content:'·';margin-right:12px;color:var(--border-strong)}
/**
 * 样式范围：.work-section-outline。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 16px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.work-section-outline{display:grid;gap:16px;border-left:1px solid var(--border);padding-left:18px}
  /**
   * 样式范围：.work-section-outline a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-section-outline a{color:var(--text-secondary);font-size:13px;line-height:1.7}
  /**
   * 样式范围：.work-detail__subtitle。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-detail__subtitle{font-size:22px;color:var(--text-secondary)}
  /**
   * 样式范围：.work-detail__tags。
   * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
   */
  .work-detail__tags{display:flex;gap:8px;justify-content:center;flex-wrap:wrap;margin-top:18px}
  /**
   * 样式范围：.work-detail__tags span。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-detail__tags span{font-size:12px;border:1px solid var(--border);border-radius:999px;padding:5px 12px;color:var(--primary)}
  /**
   * 样式范围：.work-preview。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-preview{padding:14px 18px;border:1px solid var(--border);background:var(--bg-subtle);border-radius:12px;color:var(--primary)}
/**
 * 样式范围：.work-neighbors。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.work-neighbors{display:flex;justify-content:space-between;flex-wrap:wrap;gap:20px;border-top:1px solid var(--border);margin-top:56px;padding-top:24px}
  /**
   * 样式范围：.work-neighbors a。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .work-neighbors a{color:var(--primary);font-size:14px;line-height:1.8;max-width:45%;text-decoration:none}
</style>
