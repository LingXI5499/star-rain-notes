/** Case Study markdown skeleton for new portfolio projects. */
export const CASE_STUDY_BODY_TEMPLATE = `## 01 项目概述

用 2–4 段说明这个产品是什么、服务谁、现在处于什么状态。

## 02 为什么开发

写清楚动机与要解决的真实问题，而不是功能清单。

## 03 产品设计

说明信息架构、关键页面与交互取舍。可配截图说明。

## 04 核心功能

挑选 3–5 个真正体现工程判断的功能，每个功能写：

- 用户价值
- 实现方式
- 取舍原因

## 05 技术架构

按 Frontend / Backend / Data / Deployment 分层说明职责边界与关键依赖。

## 06 关键技术实现

挑 1–2 个最能体现工程深度的点（例如稳定内容 URL、权限、搜索、缓存、部署策略）。

## 07 部署架构

说明线上拓扑、发布流程与运维约定。

## 08 项目演进

用时间线记录关键里程碑：从 0→1、重构、上线后的迭代与下一步计划。
`

export function isBlankMarkdown(source: string | null | undefined): boolean {
  return !source || !source.replace(/[#>\-\s*`_]/g, '').trim()
}
