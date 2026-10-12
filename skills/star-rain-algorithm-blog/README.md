# 星雨笔录算法博客 Skill

将算法题、旧题解或实际问题的算法建模整理为可发布的 Markdown 博客。来源可以是 LeetCode、牛客、洛谷、AtCoder、Codeforces、竞赛笔试、课程练习或自拟问题；不强制平台题号、难度或 `Solution` 签名。

核心价值是把**题意 → 测试 → 思想与步骤 → 正确性依据 → 实现与复杂度 → 选型**连成完整阅读过程。代码按解法组织，默认提供五语言页签，也支持用户指定组合。仅审查或生成发布信息时保持对应范围。

## 文件与用途

| 文件 | 用途 |
| --- | --- |
| [SKILL.md](SKILL.md) | 触发范围、写作流程、契约及交付要求 |
| [agents/openai.yaml](agents/openai.yaml) | 技能名称、简述和调用提示 |
| [references/writing-standard.md](references/writing-standard.md) | 题意、测试、推导、步骤、证明与选型 |
| [references/star-rain-markdown.md](references/star-rain-markdown.md) | 星雨笔录页签、公式、图片和发布字段约定 |
| [references/verification-standard.md](references/verification-standard.md) | 静态结构、实际运行与正确性验证 |
| [assets/ARTICLE_TEMPLATE.md](assets/ARTICLE_TEMPLATE.md) | 正文骨架，提示和代码占位必须替换 |
| [assets/METADATA_TEMPLATE.md](assets/METADATA_TEMPLATE.md) | 平台无关的标题、摘要、标签示例 |
| [scripts/validate_article.py](scripts/validate_article.py) | Python 标准库结构检查器，不执行文章代码 |
| [scripts/test_validate_article.py](scripts/test_validate_article.py) | 检查器的有效输入及错误格式回归验证 |

## 使用示例

> 使用 $star-rain-algorithm-blog 为这道洛谷题写博客，保留标准输入输出格式，解释基础方法与优化，给出测试、逐步推导和发布信息。

> 使用 $star-rain-algorithm-blog 整理自拟的“区间任务调度”问题，先明确输入输出与约束，提供 C++ 和 Python 实现，并验证反例和边界。

> 使用 $star-rain-algorithm-blog 重构旧题解，保留正确题意、示例及已有图片地址；不要凑解法数量，指出事实性修订。

> 使用 $star-rain-algorithm-blog 只生成本文标题、摘要和逗号分隔标签，不改正文。

使用完整目录，保留参考文档、模板和脚本；工具是否自动发现技能取决于其加载机制，仓库中的 `skills/` 是合集与维护位置。合集说明见[上级 README](../README.md)。

## 校验

```bash
python scripts/test_validate_article.py
python scripts/validate_article.py /path/to/article.md --strict-comments
python scripts/validate_article.py /path/to/article.md --languages cpp,python
```

默认检查五语言组合；`--languages` 接受围栏语言标识，顺序也作为页签顺序。静态检查不能判断算法正确或测量覆盖率，运行记录必须另行提供。模板和测试中的示例不代表已经提交到任何题目平台。

本技能按仓库 [MIT 协议](../../LICENSE) 维护。题目原文及用户引用的第三方图像、代码或资料保留其自身权利与使用条件。技能不会自动发布文章、部署站点或推送代码。
