# 路由清单

以下保留原公开路由及实际 slug，统一使用尾部 `/`，构建生成独立 HTML。

| 路由 | 来源 |
| --- | --- |
| `/` | 原首页 |
| `/tutorials/` | 原课程目录 |
| `/tutorials/:tutorialSlug/` | 原课程 slug |
| `/tutorials/:tutorialSlug/:chapterSlug/` | 原章节 slug |
| `/blog/` | 原博客 |
| `/blog/page/:page/` | 静态分页 |
| `/blog/archive/` | 原时间归档 |
| `/blog/posts/:slug/` | 原文章 slug |
| `/blog/topics/:slug/` | 原专题 slug |
| `/blog/tags/:slug/` | 静态标签页 |
| `/portfolio/`、`/portfolio/:slug/` | 原作品 |
| `/english/`、`/english/vocabulary/` | 原英语首页与词汇目录 |
| `/english/vocabulary/:themeId/` | 原主题 ID |
| `/english/grammar/`、`/english/grammar/:slug/` | 原语法课程与课文 slug |
| `/about/`、`/messages/` | 原个人介绍与留言 |
| `/search/`、`/privacy/`、`/terms/` | 原辅助路由 |
| `/404.html` | 返回状态 404；不进入 Sitemap |

不创建 `/useradmin/`、`/auth/`、学习任务、词汇 study/plan/progress、英语 reading/writing。搜索、隐私、条款不进入 Sitemap。不存在的 URL 由静态服务器返回 404，禁止 SPA 回退。
