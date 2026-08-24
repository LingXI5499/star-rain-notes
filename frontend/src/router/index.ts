import { createRouter, createWebHistory } from 'vue-router'

import WideLayout from '@/layouts/WideLayout.vue'
import ProseLayout from '@/layouts/ProseLayout.vue'
import DocumentationLayout from '@/layouts/DocumentationLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { hasUnsavedChanges } from '@/composables/useUnsavedGuard'
import { applyPageMeta } from '@/lib/seo'

/**
 * V1 SPA routing (TASK-001 skeleton + TASK-003 admin auth pages).
 *
 * - public pages share WideLayout (shell 1400, unified layout tokens)
 * - blog/portfolio article detail uses WideLayout/ProseLayout (narrow prose)
 * - tutorial routes use DocumentationLayout (Sidebar | Content | TOC)
 * - /admin/** uses AdminLayout; /admin/setup and /admin/login are
 *   standalone full-screen pages protected by the auth guard below
 *
 * Business pages are placeholders until their vertical slices land
 * (TASK-005 tutorials, TASK-006 blog, TASK-007 portfolio, TASK-008 english/about).
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: WideLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
        },
        {
          path: 'tutorials',
          name: 'tutorials',
          component: () => import('@/views/tutorials/TutorialsView.vue'),
          meta: { title: '教程', description: '系统化技术教程：按主题组织，可跟随的学习路径。' },
        },
        {
          path: 'blog',
          name: 'blog',
          component: () => import('@/views/blog/BlogView.vue'),
          meta: {
            title: '博客',
            description: '个人技术博客：按时间线记录思考、经验与技术笔记。',
            elementPlus: true,
          },
        },
        {
          path: 'portfolio',
          name: 'portfolio',
          component: () => import('@/views/portfolio/PortfolioView.vue'),
          meta: { title: '作品', description: '真实项目案例研究：从问题、方案到上线复盘。' },
        },
        {
          path: 'english',
          name: 'english',
          component: () => import('@/views/english/EnglishView.vue'),
          meta: { title: '英语', description: '英语学习路线与资料整理。' },
        },
        {
          path: 'english/vocabulary',
          name: 'english-vocabulary',
          component: () => import('@/views/english/VocabularyView.vue'),
          meta: { title: '词汇', description: '按主题分类的英语词汇库。' },
        },
        {
          path: 'english/vocabulary/:themeId',
          name: 'english-vocabulary-theme',
          component: () => import('@/views/english/ThemeWordsView.vue'),
          meta: { title: '词汇', description: '主题词汇卡片。' },
        },
        {
          path: 'about',
          name: 'about',
          component: () => import('@/views/about/AboutView.vue'),
          meta: { title: '关于', description: '关于作者与星雨笔录这个知识站点。' },
        },
        {
          path: 'search',
          name: 'search',
          component: () => import('@/views/search/SearchView.vue'),
          meta: {
            title: '搜索',
            description: '站内搜索教程、博客与作品。',
            robots: 'noindex,follow',
            elementPlus: true,
          },
        },
      ],
    },
    {
      path: '/blog/:slug',
      component: WideLayout,
      children: [
        {
          path: '',
          name: 'blog-detail',
          component: () => import('@/views/blog/BlogDetailView.vue'),
          meta: { title: '博客文章' },
        },
      ],
    },
    {
      path: '/portfolio/:slug',
      component: ProseLayout,
      children: [
        {
          path: '',
          name: 'portfolio-detail',
          component: () => import('@/views/portfolio/PortfolioDetailView.vue'),
          meta: { title: '作品' },
        },
      ],
    },
    {
      path: '/tutorials/:tutorialSlug',
      component: DocumentationLayout,
      children: [
        {
          path: '',
          name: 'tutorial-detail',
          component: () => import('@/views/tutorials/TutorialDetailView.vue'),
          meta: { title: '教程' },
        },
        {
          path: ':chapterSlug',
          name: 'tutorial-chapter',
          component: () => import('@/views/tutorials/ChapterView.vue'),
          meta: { title: '教程' },
        },
      ],
    },
    {
      path: '/english/grammar',
      component: DocumentationLayout,
      children: [
        {
          path: '',
          name: 'english-grammar',
          component: () => import('@/views/english/GrammarView.vue'),
          meta: { title: '英语语法完整教程', description: '从词法到复杂句法的系统英语语法课程。' },
        },
        {
          path: ':lessonSlug',
          name: 'english-grammar-lesson',
          component: () => import('@/views/english/GrammarLessonView.vue'),
          meta: { title: '英语语法课程' },
        },
      ],
    },
    {
      path: '/admin/setup',
      name: 'admin-setup',
      component: () => import('@/views/admin/SetupView.vue'),
      meta: { title: '初始化', robots: 'noindex,nofollow', elementPlus: true },
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/LoginView.vue'),
      meta: { title: '登录', robots: 'noindex,nofollow', elementPlus: true },
    },
    {
      path: '/admin',
      // Lazy so the Admin shell (and its Element Plus import) stays out of
      // the public initial bundle.
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { title: '管理后台', robots: 'noindex,nofollow', elementPlus: true },
      children: [
        {
          path: '',
          name: 'admin',
          component: () => import('@/views/admin/DashboardView.vue'),
        },
        {
          path: 'tutorials',
          name: 'admin-tutorials',
          component: () => import('@/views/admin/TutorialListView.vue'),
        },
        {
          path: 'tutorials/categories',
          name: 'admin-tutorial-categories',
          redirect: { name: 'admin-tutorials' },
        },
        {
          path: 'tutorials/new',
          name: 'admin-tutorial-new',
          component: () => import('@/views/admin/TutorialEditView.vue'),
        },
        {
          path: 'tutorials/:id/edit',
          name: 'admin-tutorial-edit',
          component: () => import('@/views/admin/TutorialEditView.vue'),
        },
        {
          path: 'tutorials/:id/chapters',
          name: 'admin-tutorial-chapters',
          component: () => import('@/views/admin/ChapterManageView.vue'),
        },
        {
          path: 'tutorials/:id/chapters/new',
          name: 'admin-chapter-new',
          component: () => import('@/views/admin/ChapterEditView.vue'),
        },
        {
          path: 'tutorials/:id/chapters/:chapterId/edit',
          name: 'admin-chapter-edit',
          component: () => import('@/views/admin/ChapterEditView.vue'),
        },
        {
          path: 'blog',
          name: 'admin-blog',
          component: () => import('@/views/admin/BlogListView.vue'),
        },
        {
          path: 'blog/new',
          name: 'admin-blog-new',
          component: () => import('@/views/admin/BlogEditView.vue'),
        },
        {
          path: 'blog/:id/edit',
          name: 'admin-blog-edit',
          component: () => import('@/views/admin/BlogEditView.vue'),
        },
        {
          path: 'blog/tags',
          name: 'admin-blog-tags',
          component: () => import('@/views/admin/BlogTagView.vue'),
        },
        {
          path: 'portfolio',
          name: 'admin-portfolio',
          component: () => import('@/views/admin/PortfolioListView.vue'),
        },
        {
          path: 'portfolio/new',
          name: 'admin-portfolio-new',
          component: () => import('@/views/admin/PortfolioEditView.vue'),
        },
        {
          path: 'portfolio/:id/edit',
          name: 'admin-portfolio-edit',
          component: () => import('@/views/admin/PortfolioEditView.vue'),
        },
        {
          path: 'english',
          name: 'admin-english',
          component: () => import('@/views/admin/EnglishWorkspaceView.vue'),
        },
        {
          path: 'english/overview',
          name: 'admin-english-overview',
          component: () => import('@/views/admin/EnglishEditView.vue'),
        },
        {
          path: 'english/vocabulary',
          name: 'admin-english-vocabulary',
          component: () => import('@/views/admin/VocabularyAdminView.vue'),
        },
        {
          path: 'english/grammar',
          name: 'admin-english-grammar',
          component: () => import('@/views/admin/GrammarManageView.vue'),
        },
        {
          path: 'english/taxonomy',
          name: 'admin-english-taxonomy',
          component: () => import('@/views/admin/TaxonomyManagerView.vue'),
        },
        {
          path: 'english/bundles',
          name: 'admin-english-bundles',
          component: () => import('@/views/admin/BundleManagerView.vue'),
        },
        {
          path: 'english/grammar/lessons/new',
          name: 'admin-english-grammar-lesson-new',
          component: () => import('@/views/admin/GrammarLessonEditView.vue'),
        },
        {
          path: 'english/grammar/lessons/:lessonId/edit',
          name: 'admin-english-grammar-lesson-edit',
          component: () => import('@/views/admin/GrammarLessonEditView.vue'),
        },
        {
          path: 'vocabulary',
          name: 'admin-vocabulary',
          redirect: { name: 'admin-english-vocabulary' },
        },
        {
          path: 'about',
          name: 'admin-about',
          component: () => import('@/views/admin/AboutEditView.vue'),
        },
        {
          path: 'media',
          name: 'admin-media',
          component: () => import('@/views/admin/MediaLibraryView.vue'),
        },
        {
          path: 'settings',
          name: 'admin-settings',
          component: () => import('@/views/admin/SiteSettingsView.vue'),
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面未找到', robots: 'noindex,nofollow' },
    },
  ],
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition ?? { top: 0 }
  },
})

/**
 * Global guards (TASK-011):
 * 1. unsaved-changes confirmation for admin editors
 * 2. lazy Element Plus registration for routes that use EP components
 * 3. admin auth guard (setup/login are anonymous; the rest needs a session)
 */
router.beforeEach(async (to) => {
  if (hasUnsavedChanges()) {
    // eslint-disable-next-line no-alert
    if (!window.confirm('当前页面有未保存的修改，确定要离开吗？')) {
      return false
    }
  }
  if (to.meta.elementPlus) {
    const { registerElementPlus } = await import('@/plugins/element-plus')
    await registerElementPlus()
  }
  if (!to.path.startsWith('/admin')) {
    return true
  }
  const auth = useAuthStore()
  if (to.name === 'admin-setup' || to.name === 'admin-login') {
    if (auth.isAuthenticated) {
      return { name: 'admin' }
    }
    return true
  }
  if (!auth.isAuthenticated) {
    if (auth.status === 'unknown') {
      await auth.fetchSession()
    }
    if (!auth.isAuthenticated) {
      return { name: 'admin-login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

/**
 * Basic SEO (01 §10): apply per-route title / description / canonical /
 * Open Graph / robots on every navigation. Detail views call applyPageMeta
 * again after their content loads.
 */
router.afterEach((to) => {
  applyPageMeta(
    {
      title: to.meta.title as string | undefined,
      description: to.meta.description as string | undefined,
      robots: to.meta.robots as string | undefined,
    },
    to.path,
  )
})

export default router
