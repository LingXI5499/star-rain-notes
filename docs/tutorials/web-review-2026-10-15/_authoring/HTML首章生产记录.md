# HTML 首章生产记录

日期：2026-10-15。状态：READY_FOR_HUMAN_REVIEW。此记录只覆盖第一章，不代表 HTML 43 章或三套教程已经完成。

## 读者与边界

读者是开始学习 HTML 的初学者，能够新建、保存文件并打开浏览器。目标是分清标签、元素、属性，选择少量常见标签，读写属性，区分普通元素、空元素与注释，并能根据修改内容核对页面结果。默认使用 UTF-8 的 HTML 文档，不依赖 CSS、JavaScript、构建工具或互联网资源。

目录定位按《web教程总纲.md》的 `HTML → 一.先认识 HTML 的组成 → 1.标签、元素、属性：先读懂一行 HTML` 保留。正文元数据省略网站目录显示的章序号；数据中的 outlineChapterTitle 保留原三级标题。下一章负责嵌套和结构树，第三章负责文档组成；本章完整页面的外壳只供复制运行，不提前展开。

## 内容与知识价值

| 核心知识 | 进入本章的原因 | 对应训练 |
| --- | --- | --- |
| 标签、元素与属性的组成 | 后续所有 HTML 阅读的直接前置 | 问题 1、2，练习 1、2 |
| 按内容用途选择常见标签 | 避免按默认字号误用标题或给段落添加链接属性 | 问题 4、7，练习 2 |
| 属性位置、取值与 class 的边界 | 避免把属性当正文、把类名当内置样式 | 问题 2、3、4，练习 1、2 |
| 空元素与注释 | 修正给 img 添加结束标签的常见错误 | 问题 5、6，练习 1、2 |
| 单一变量修改与页面对照 | 将术语转化为能观察、能说明的操作结果 | 运行对照表，练习 2 |

标题只承担内容导航。正文采用四个 H2、五个核心知识 H3 与两个练习 H3，保留 title、summary 两项元数据。问题答案与卡片数据独立保存，公开正文不直接展示答案；验收页提供可展开的答案与卡片背面。

## 来源

- 用户总纲：`E:/star-rain/web教程总纲.md`，用于确定课程层级与本章边界。
- 用户代码示例：`F:/code/web/HTML/01-先认识HTML的组成/01-html-syntax-标签、元素、属性：先读懂一行HTML/`，阅读了 README.md 与 index.html。保留标题和两个段落的学习内容，移除原例的样式、脚本引用，生成单文件最小示例。本次未修改用户代码库。
- [WHATWG HTML 元素语法](https://html.spec.whatwg.org/multipage/syntax.html#elements-2)：核对空元素、标签和属性的语法边界。
- [MDN HTML 基本语法](https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Structuring_content/Basic_HTML_syntax)：交叉核对标签、元素、属性和注释的教学概念。
- [MDN class 属性](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Global_attributes/class)：核对类名与样式规则的关系。
- [MDN img](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/img)：核对 src 与 alt 的职责。

使用仓库 `skills/star-rain-tutorial-authoring/SKILL.md` 的通用教程结构。文字沿用用户总纲先解释术语、再读代码的表达方式，未检索其他应用中的个人写作样本。

## 实际验证

结构检查：`python -X utf8 skills/star-rain-tutorial-authoring/scripts/validate_chapter_structure.py docs/tutorials/web-review-2026-10-15/HTML/01-先认识HTML的组成/01-标签元素属性.md`，0 错误、0 警告，5104 字符。

浏览器执行：运行 `_authoring/verify_html_sample.cjs`，使用本机 Chrome 156.0.8078.12，九组检查通过。完整检查结果保存在 `HTML首章校验.json`。覆盖：

- 卡片、问题、练习本地键的唯一性和关联完整性；正文七道问题与数据逐项一致。
- 正文完整示例代码与交付 index.html 一致。
- 离线打开验收页，四块正文、卡片与答案数量正确，未捕获脚本异常为零。
- 卡片和问题答案可展开；目录锚点及本地链接有实际目标。
- 320px 与 390px 窄屏没有整页水平溢出。
- 主例显示一个标题与两个段落；没有 CSS 或 JS 依赖。
- 修改文字仅影响第一段；修改 class 不改变计算后的颜色、字号和字重；把 p 改为 h2 后元素类型与默认字号改变。
- 练习参考页面符合要求的文字、顺序、属性和注释条件；两段文字计算颜色一致。

尚未进行人工验收。未对不同浏览器的悬停提示外观作一致性承诺；title 的说明明确为可能出现的补充提示。阅读题中的 cover.png 与 notes.html 是用于解释属性的片段，不是承诺有配套资源的可运行示例；实际完整示例不依赖这些资源。

本次没有生成 SQL，没有写入或覆盖网站的教程数据。
