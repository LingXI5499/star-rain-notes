/** Case Study markdown skeleton — README-style chapters for all portfolio projects. */
export const CASE_STUDY_BODY_TEMPLATE = `## 01 一句话与背景

用 2–4 段说明：这个项目是什么、服务谁、要解决什么真实问题、当前状态。

## 02 页面导览

按站点（或产品）主要页面 / 模块逐一说明：

| 页面或模块 | 职责 | 关键交互 |
| --- | --- | --- |
| 例：首页 | … | … |

可在本节引用下方「项目预览」中的截图。

## 03 核心用户流程

分角色写清主路径（步骤化）：

1. **访客 / 使用者**：从进入到完成目标的路径
2. **作者 / 运营**（如有）：发布与维护路径
3. **管理端**（如有）：审核、配置、发布路径

## 04 功能清单

列出核心能力与明确非目标，避免把愿望写成已交付功能。

- 已具备：…
- 不做 / 非目标：…

## 05 技术架构

按 前端 / 后端 / 数据 / 基础设施 分层说明职责边界与关键依赖。页面上方的「技术栈」速览可与本节对应，正文补充「为什么这样分层」。
`

export function isBlankMarkdown(source: string | null | undefined): boolean {
  return !source || !source.replace(/[#>\-\s*`_]/g, '').trim()
}
