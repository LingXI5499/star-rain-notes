---
name: java-songshan-guidelines
description: 基于《Java开发手册（嵩山版）》为 Java 服务端项目的编码、接口、异常日志、测试、安全、MySQL 和设计提供实现或审查指引。适用于相关代码新增、修改与评审；不用于非 Java 项目，也不替代通用教程写作规范。
---

# Java 开发手册（嵩山版）实践

来源：用户提供的《Java开发手册（嵩山版）》，1.7.0，2020-08-03。下列参考文件是按主题提炼的工作指引，不是手册全文；本技能包不分发原 PDF。页码均为手册印刷页码；PDF 文件页码比印刷页码大 3。

## 适用与取舍

- 在 Java 服务端代码、相关 API、MySQL 表与 SQL、工程结构或设计任务中，按需读取相应参考文件。仅讨论非 Java 项目时不加载这些规则；教程创作优先使用 [通用教程写作技能](../star-rain-tutorial-authoring/SKILL.md)，涉及 JavaSE 时再读取其领域上下文。
- 先看用户要求、项目 `AGENTS.md`、现有代码风格、JDK/框架/数据库版本和测试约定，再决定具体做法。手册的“强制 / 推荐 / 参考”是**原文的分级**，不是对当前项目自动生效的更高层指令；用户要求和项目约定优先。
- 将这份 2020 年手册当作开发规范来源。版本相关、框架相关、性能数字及安全实现细节若可能过时，核对项目所用版本与官方文档。遇到冲突时说明取舍依据，不擅自大范围改造现有项目。
- 只在当前任务触及的代码与接口应用规则。新增代码优先符合规范；审查存量代码时报告具体风险和位置，不为统一风格批量修改无关文件。

## 按任务读取

- Java 命名、格式、OOP、日期、集合、并发、控制流、注释、前后端交互：读 [references/java-and-api.md](references/java-and-api.md)。
- 错误码、异常、日志、单元测试、安全：读 [references/quality-and-security.md](references/quality-and-security.md)。
- MySQL、ORM、依赖与分层、设计文档：读 [references/persistence-and-design.md](references/persistence-and-design.md)。

实施或审查后，报告与当前任务有关的规范、验证结果及未解决的版本或项目约定冲突；没有必要逐条复述手册。
