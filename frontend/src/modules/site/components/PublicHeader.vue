<script setup>
/**
 * PublicHeader.vue：站点模块的视图组件。
 * 职责范围：站点品牌、首页内容和站点设置。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、vue-router、./SearchLauncher.vue、./ThemeControl.vue、../../../shared/viewMode、../support/siteBranding。
 * 主要响应式状态：route、scrolled、navItems、brandPath。
 * 阅读入口：isActive（判断是否有效）；onScroll（响应滚动）。
 */


import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import SearchLauncher from './SearchLauncher.vue'
import ThemeControl from './ThemeControl.vue'
import { useViewMode } from '../../../shared/viewMode'
import { useSiteBranding } from '../support/siteBranding'

/*
 * 前台顶部导航 —— 对齐 V1 `components/PublicHeader.vue`：
 * 粘性、滚动后收窄并加下边框，品牌 + 内容导航 + 搜索 + 主题 + 账号区。
 *
 * 品牌名与 Logo 来自 Site 公共配置；尚未配置时显示本地默认标识。
 * 导航目标按**当前路径树**生成：公开树指向 /blog，账号树指向 /useradmin/blog。
 *      写死 /blog 的话，在 /useradmin 里点「博客」会掉回公开树、把账号外壳丢掉。
 *
 * 账号区不在这里实现：它由 PublicShell 通过 `account` 插槽传进来，且只在账号模式挂载。
 * 本文件因此不出现任何账号相关的链接或状态，也不引用 auth store。
 */
/**
 * 路由；通过 useRoute 建立。
 */
const route = useRoute()
const { contentPath } = useViewMode()
/**
 * scrolled；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const scrolled = ref(false)
let ticking = false

const branding = useSiteBranding()
/*
 * 导航顺序是所有者指定的一级信息架构：
 *   教程 → 博客 → 作品 → 英语 → 留言 → 关于
 * 页脚 EXPLORE 保持同一顺序，两处必须一起改，否则同一个站点会出现两种导航心智。
 */
/**
 * nav条目列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const navItems = computed(() => [
  { label: '教程', to: contentPath('/tutorials') },
  { label: '博客', to: contentPath('/blog') },
  { label: '作品', to: contentPath('/portfolio') },
  { label: '英语', to: contentPath('/english') },
  { label: '留言', to: contentPath('/messages') },
  { label: '关于', to: contentPath('/about') },
])
/**
 * brand路径；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const brandPath = computed(() => contentPath('/'))

/**
 * 判断是否有效。
 * 主要协作：route.path.startsWith。
 * @param path 路径；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function isActive(path) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

/**
 * 响应滚动。
 * 主要协作：requestAnimationFrame。
 * 更新的响应式状态包括 scrolled；渲染由 Vue 的依赖跟踪触发。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    scrolled.value = window.scrollY > 16
    ticking = false
  })
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})

/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <header class="site-header" :class="{ 'site-header--scrolled': scrolled }">
    <div class="site-header__inner">
      <RouterLink :to="brandPath" class="site-header__brand">
        <img class="site-header__logo" :src="branding.logoUrl || '/brand/mark.svg'" alt="" aria-hidden="true" width="26" height="26" />
        <span>{{ branding.siteName }}</span>
      </RouterLink>

      <nav class="site-header__nav" aria-label="主导航">
        <template v-for="item in navItems" :key="item.label">
          <RouterLink
            v-if="item.to"
            :to="item.to"
            class="site-header__link"
            :class="{ 'site-header__link--active': isActive(item.to) }"
          >{{ item.label }}</RouterLink>
          <span v-else class="site-header__link site-header__link--pending" :title="item.pending" aria-disabled="true">{{ item.label }}</span>
        </template>
      </nav>

      <div class="site-header__actions">
        <SearchLauncher class="site-header__search" />
        <ThemeControl class="site-header__theme" />
        <!-- 账号区：公开模式下插槽为空，右上角不可能出现账号入口 -->
        <slot name="account" />
      </div>
    </div>
  </header>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.site-header。
 * 在正常文档流中保留位置，吸附偏移由 top=0 决定；父级高度限制吸附范围。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-header {
  position: sticky;
  top: 0;
  z-index: var(--z-header, 20);
  height: var(--header-height);
  background: color-mix(in srgb, var(--bg-page) 72%, transparent);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid transparent;
  transition:
    height var(--motion-base) var(--ease-standard),
    background-color var(--motion-base) var(--ease-standard),
    border-color var(--motion-base) var(--ease-standard),
    backdrop-filter var(--motion-base) var(--ease-standard);
}

/**
 * 样式范围：.site-header--scrolled。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-header--scrolled {
  height: 56px;
  background: color-mix(in srgb, var(--bg-page) 88%, transparent);
  backdrop-filter: blur(16px);
  border-bottom-color: var(--border);
}

/**
 * 样式范围：.site-header__inner。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-header__inner {
  max-width: var(--layout-max-width);
  margin-inline: auto;
  padding-inline: var(--page-padding-x);
  height: 100%;
  display: flex;
  align-items: center;
  gap: var(--space-8);
}

/**
 * 样式范围：.site-header__brand。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-header__brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
}

/**
 * 样式范围：.site-header__logo。
 */
.site-header__logo {
  width: 26px;
  height: 26px;
  display: block;
  object-fit: contain;
}

/**
 * 样式范围：.site-header__nav。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-header__nav {
  display: flex;
  gap: var(--space-6);
}

/**
 * 样式范围：.site-header__link。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-header__link {
  font-size: 15px;
  color: var(--text-secondary);
  padding-block: var(--space-1);
  border-bottom: 2px solid transparent;
}

/**
 * 样式范围：.site-header__link:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-header__link:hover {
  color: var(--primary);
}

/**
 * 样式范围：.site-header__link--active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-header__link--active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

/* 尚未实现的模块：明确不可点，避免产生死链，也避免“点了没反应”的错觉 */
.site-header__link--pending {
  color: var(--text-muted);
  cursor: not-allowed;
}

/**
 * 样式范围：.site-header__link--pending:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-header__link--pending:hover {
  color: var(--text-muted);
}

/**
 * 样式范围：.site-header__actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.site-header__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-left: auto;
}

/**
 * 响应式或设备条件：(max-width: 768px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 768px) {
  /**
   * 样式范围：.site-header。
   */
  .site-header {
    height: auto;
    padding-block: 8px;
  }

  /**
   * 样式范围：.site-header--scrolled。
   */
  .site-header--scrolled {
    height: auto;
  }

  /**
   * 样式范围：.site-header__inner。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .site-header__inner {
    flex-wrap: wrap;
    gap: var(--space-3);
    row-gap: 6px;
  }

  /**
   * 样式范围：.site-header__brand。
   */
  .site-header__brand {
    font-size: 17px;
  }

  /* 导航换行排在第二行，横向可滚动，不藏入口也不挤爆头部 */
  .site-header__nav {
    display: none;
  }

  /**
   * 样式范围：.site-header__nav::-webkit-scrollbar。
   */
  .site-header__nav::-webkit-scrollbar {
    display: none;
  }

  /**
   * 样式范围：.site-header__link。
   */
  .site-header__link {
    font-size: 14px;
  }
}
</style>
