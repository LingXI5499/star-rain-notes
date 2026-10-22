# 视觉与资源清单

基准：原 `HomePage.vue`、`PublicHeader.vue`、`PublicFooter.vue`、`ThemeHero.vue`、`styles/tokens.css`、`styles/public-theme.css`。主题实际为墨绿 `#24584d`、米白 `#f4f3ed`、铜色 `#a85831`，并提供深色模式。

复用 `frontend/public/brand/` 中的品牌标志、favicon、OG 图、作者头像与三幅 WebP 主题插画。首页保留左文右图的渐变幕布构图、四模块导轨、编号更新列表、三列课程和作品布局。公共导航顺序维持教程、博客、作品、英语、留言、关于。字体使用原有系统字体栈，不依赖远程字体。

原图无需重新生成；不加入粒子、3D 或新的复杂动效。支持 reduced-motion、键盘、移动端导航与移动阅读目录。图片、正文插图和附件迁移到本项目静态资源。
