# 两级分类筛选与主题过渡改进报告

> 随笔界面的后续改造与最新验证见 [英文随笔博客界面实施报告](english-essays-ui-report.md)。下文保留本阶段当时的结果。

日期：2026-10-22。分支：LingXI/static-site。此前英语模块升级的数据、旧路径和未提交成果继续保留。本次根据用户反馈，把分类的作用调整为内容筛选，并补齐原站的圆形明暗主题过渡。

## 最终交互

- 桌面显示紧凑的“一级分类 / 二级主题”两行筛选，取消大分类卡片；选择后直接更新下方词汇或文章。
- 手机使用两个有明确标签的下拉框；选择一级分类后，只提供该分类的二级主题，避免把全部主题堆在屏幕中。
- 总览直接展示全部已确认分类词汇；一级分类展示该分类的词汇，二级主题进一步过滤。旧资料和整理说明放在可展开区域，来源数据不变。
- 分类切换只读取本地生成的静态 HTML，更新正文、网址、标题、canonical、description、OG 和 JSON-LD；无后端服务或新依赖。
- 保留真实分类路径，刷新和直接访问仍可静态生成；未启用脚本时筛选链接仍能导航。
- 搜索词、每页数量和词卡语言选择在分类切换时保留；页码重置为 1。选择其他一级分类不会残留旧二级主题。
- 支持浏览器后退恢复筛选；键盘选择和手机下拉更新后恢复相应控件焦点。
- 词汇默认每页 24 条、文章默认每页 12 篇；每页数量可选，翻页后回到结果区域。
- 主题切换使用原站同样的圆形展开体验：鼠标点击位置或键盘触发时按钮中心作为圆心，半径覆盖四角，0.52 秒完成；按钮图标同时切换。
- 防止动画期间连续点击造成双重切换；减少动态效果偏好时直接切换；不支持 View Transition 时淡入降级，存储受限仍可在当前页面切换。

## 本次修改文件

|文件|职责|
|---|---|
|src/components/astro/EnglishTaxonomyFilters.astro|共享两级分类链接、真实数量及手机下拉框|
|src/components/astro/OriginalVocabulary.astro|取消分类卡片，所有分类层级直接输出词汇结果和分页|
|src/components/astro/OriginalWorld.astro|取消分类卡片，直接输出筛选后的文章列表|
|src/components/astro/EnglishWordList.astro|语言显示由共享交互统一接管，支持动态切换后继续使用|
|src/components/astro/OriginalPagination.astro|初始紧凑页码、正确的零条范围，避免大量页码挤占页面|
|src/utils/english-browse.ts|本地静态页渐进更新、后退恢复、焦点恢复、加载失败正常导航|
|src/utils/english-browse-url.ts|保留搜索/每页数量/语言，分类切换重置分页|
|src/utils/filter.ts|可重复初始化新内容，恢复每页数量，词汇/文章结果翻页定位|
|src/utils/theme-toggle.ts|圆形主题切换、中心/半径、持久化、降级、连续点击与失败收尾|
|src/styles/english-upgrade.css|移除分类卡片样式，添加紧凑筛选、手机版下拉和结果对齐|
|src/styles/theme-transition.css|整页圆形揭示、图标过渡及低动态效果兼容|
|src/layouts/BaseLayout.astro|装配共享主题与英语筛选交互|
|tests/english-interaction.test.mjs|新增 6 项有状态交互/异常路径测试|
|scripts/verify-english-fixtures.mjs|扩充隔离文章样例，验证筛选列表与 12 条分页|
|docs/english-ui-verification.json|更新为本轮 44 项布局与交互的实际结果|
|docs/build-verification.json、content-validation.json、runtime-verification.json、english-fixture-verification.json|本轮命令、HTTP 与隔离文章验证结果|

## 验证结果

|检查|实际结果|
|---|---|
|npm run validate|PASS，保留原有数据待确认警告|
|npm run check|PASS，73 个文件，0 errors / 0 warnings / 0 hints|
|npm test|PASS，18 项全部通过，0 跳过|
|npm run build|PASS，3275 页；444794 个链接/资源引用，3166 项搜索索引|
|独立静态预览|PASS，477 项页面、资源、重定向与 404 检查|
|响应式布局|PASS，11 个关键页面 × 320/390/768/1440 宽，共 44 项，无横向溢出，每页一个 h1|
|隔离文章验收|PASS，16 篇发布样例、2 篇草稿；正式索引未写入测试内容|
|手机两级筛选|PASS，食物 61 条 → 谷物 17 条；切换分类与页码重置正确|
|文章筛选与分页|PASS，14 篇阅读按 12/2 翻页，科技分类 1 篇，空主题真实空态|
|圆形主题过渡|PASS，捕捉到实际 circle clip-path 动画和展开中的截图，结束后清理 busy/phase|
|Git 与源数据|git diff --check 通过；原词汇/语法数据未改；没有暂存、提交、推送、合并或创建 PR|

本轮构建曾出现一次 content-validation.json 写入 UNKNOWN 异常；文件内容与属性正常，随后完整重跑成功。没有删除报告、跳过校验或把失败记为成功；具体外部原因未能复现。浏览器验收连接也曾短暂超时，恢复连接后完成了正式静态构建的手机、动画与布局回归。

当前用户已打开的词汇预览已刷新到新界面：分类大卡片数量为零，初始展示 24 条结果。

## 仍保留的内容待决项

这些不是本轮交互改动所解决的数据问题：词汇参考图 168/可辨识 167 的差异仍需确认；1163 条待复核与 4140 条未分类记录仍需人工整理；世界与随笔的正式文章清单仍为空。此前数据保留和复核清单继续有效，没有为了界面填充制造内容。

当前验收仅针对本地静态站，没有线上部署或远程发布。

## 实际截图

![桌面两级筛选](../.cache/english-filter-desktop.jpg)

![手机圆形主题切换中](../.cache/english-theme-reveal.jpg)
