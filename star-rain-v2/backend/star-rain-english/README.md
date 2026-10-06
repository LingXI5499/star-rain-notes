# 英语模块

V2 英语域重新建模。V1 只用于核对已有内容和用户流程，不复制 V1 的后端包结构或表结构。

## 当前范围

- 词汇：主题、单词和例句。
- 语法：课程、章节和课时。
- 阅读：文章。
- 写作：素材和任务。

不引入学习组合、词族关系、词性转化图或相应表。后台名称生成内部 slug，前端不填写 slug。英语内容仍使用全站统一的 Markdown 编辑器。

## 模块内分层

`overview`、`vocabulary`、`grammar`、`reading`、`writing` 是业务子域。每个子域根据实际职责分别放置 `controller`、`service`、`mapper`、`dto`；概览还含 `entity` 和 `exception`。

HTTP 入口只做参数接收、权限声明和响应封装；Service 负责校验、状态转换和事务；Mapper 负责持久化，手写 SQL 放在 `src/main/resources/mapper/english/` 的 MyBatis XML。表结构与权限的 Flyway 版本在 `star-rain-boot/src/main/resources/db/migration/`，不放在本模块根目录。

## V1 内容导入

项目级一次性导入工具位于 `star-rain-v2/tools/data-migration/english/`。它不是启动迁移，不能在生产库上自动运行；需指定来源库和目标库，并核对数据与媒体引用。此前在隔离库核对到 V1 本机快照：60 个主题、7053 个单词、42 个语法课时、6 篇阅读、6 份写作素材和 6 项写作任务。数据量只代表这次本机快照。
