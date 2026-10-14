# Markdown 编辑器 V3 实施记录

需求基准：用户提供的《md文档编辑.md》，基于 main c6ed632；用户补充优先：Markdown 导入/导出，PDF 与 Word 只导出。分支 codex/markdown-editor-v3。没有生产部署或数据库迁移。

## 阶段 A

已实现共享 Alt+0–6、普通块语义保护、行首斜杠菜单、选区浮动工具栏、只读快捷键帮助、媒体保护文案和稳定表格弹层监听。编辑器核心保持 Vditor 3.11.3，原 getMarkdown/v-model/保存接口保持兼容。Vditor Lute/语言/图标/代码高亮与主题资源在构建时从锁定依赖打包到本地，不依赖 CDN。

技术验证：IR/WYSIWYG 复用原生标题按钮，源码转换按 Markdown token 的普通块范围修改；复杂块拒绝转换，多行段落/Setext 不做有损转换。兼容选择器集中在 vditorAdapter；没有调用未经验证的 setHeadingLevel/setMode。Vditor 原生撤销使用 undoDelay=0，使格式命令独立形成恢复点，普通输入的撤销粒度也随之变细；长文档性能需在后续阶段实测。

测试：完整前端 336 项通过；原有 10 个浏览器回归、新增标题/撤销、斜杠/取消、IME 与输入框隔离通过；浮动工具栏单独回归通过。表格弹层 100 次开关验证监听引用释放。生产构建与资源预算通过。Chrome/Edge 物理键盘、真实 Windows 中文输入法人工验收未执行，不能以模拟 composition 事件声称完成真实 IME 验收。

后续 B/C/D 继续实施。当前不是全部功能完成报告。
