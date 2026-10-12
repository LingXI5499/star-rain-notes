# 星雨笔录 Skills 合集

这里维护星雨笔录目前创建的 4 项自建 skills，把写作、开发和工程分析中的稳定方法整理为可复用的任务规范。每项技能保留完整入口、按需参考资料和实际使用的模板或校验脚本，避免只保存一段提示词后丢失验证流程。

本合集收录本机自建的三项技能与提供的算法博客技能包；工具自带、系统及第三方插件 skills 不属于自建内容，不纳入此目录。

## 技能与意义

| 技能 | 用途与意义 | 典型产物 |
| --- | --- | --- |
| [star-rain-algorithm-blog](star-rain-algorithm-blog/SKILL.md) | 将任意来源算法题组织为“题意、测试、推导、步骤、实现、复杂度、选型”，帮助读者理解和复现算法；支持函数提交、标准输入输出和自拟契约 | 博客 Markdown、发布信息、真实验证记录 |
| [star-rain-java-tutorial-authoring](star-rain-java-tutorial-authoring/SKILL.md) | 为 JavaSE 课程控制知识价值、章节边界、标题编号、示例和练习，避免堆砌低价值特性或重复内容 | 目录审计、章节正文、内部生产记录、结构检查结果 |
| [java-songshan-guidelines](java-songshan-guidelines/SKILL.md) | 将嵩山版手册提炼为 Java 服务端实践，结合项目版本与约定检查编码、API、异常日志、安全、测试、持久化和设计 | 实现指引、具体审查问题、验证结果 |
| [project-engineering-report](project-engineering-report/SKILL.md) | 从当前源码、数据库迁移、路由、接口和测试等证据生成工程文档，使需求、设计和部署说明可以回溯到真实实现 | 独立 HTML 工程报告、页面与接口清单、ER 和类设计资料 |

算法博客不局限于 LeetCode；JavaSE 教程和 Java 服务端规范分别处理教学与工程约定；工程报告用于实际系统现状分析。按照请求选择需要的技能，不把所有规范同时加载到无关任务。

## 使用

在支持技能调用的环境中，可用 `$star-rain-algorithm-blog` 等名称明确调用；也可以要求助手读取对应 `SKILL.md`，再按入口链接加载本次需要的资源。使用或安装时保留整个技能目录，不能只复制入口文件。`skills/` 是版本维护目录，提交到仓库不等于在每个工具或会话中自动启用。

示例：

```text
使用 $star-rain-algorithm-blog，把这道自拟算法题写成星雨笔录博客，提供测试和 C++ / Python 实现。
使用 $star-rain-java-tutorial-authoring，审查这章 JavaSE 教程的边界、编号、示例和练习。
使用 $java-songshan-guidelines，检查当前 Java API 改动并说明具体风险和验证结果。
使用 $project-engineering-report，为当前仓库生成可回溯源码的 HTML 工程报告。
```

## 维护与验证

- 一项技能对应一个同名目录，入口 `SKILL.md` 使用 `name` 和 `description` 前置信息。
- `references/` 放条件性细则，`assets/` 放输出模板，`scripts/` 放可执行辅助工具，`agents/openai.yaml` 放显示与调用信息。仅保留确实需要的资源。
- 更新时同时核对触发范围、引用链接、模板、脚本和示例；用户请求和项目约定优先，不把技能规范作为额外外部操作授权。
- 校验算法技能可运行 `python skills/star-rain-algorithm-blog/scripts/test_validate_article.py`。其他脚本按各自入口参数使用；结构通过、代码运行和人工审查是不同结论。
- 不上传本机绝对路径、个人邮箱、密钥、实际环境配置、运行输出及缓存；部署包不包含本合集，网站前后端不依赖它运行。

## 开源协议与来源

本仓库维护的技能入口、参考指引、模板和脚本沿用 [MIT 协议](../LICENSE)。`java-songshan-guidelines` 是对用户提供的《Java开发手册（嵩山版）》的主题实践指引，不包含原 PDF，也不改变原手册的版权。题面及引用的第三方代码、图像、规范和资料仍按其来源的条件使用。
