# 教程模块包结构

`star-rain-tutorial` 是一个 Maven 子模块，内部按业务域分为两个顶层 Java 包：

```text
com.starrainnotes.tutorial
├── content   教程分类、课程、章节、卡片、问题、发布与公开读取
└── learning  个人进度、答案、学习计划、任务、复习、掌握度与历史
```

每个业务域在自己的包下组织 `controller`、`service`、`mapper`、`entity`、`dto` 等层。学习域通过 `LearningContentAccess` 读取内容域的已发布修订；内容业务实现不依赖学习域。两个域分别声明自己的安全路径，各自的配置类扫描本域 Mapper。

`service` 目录仅放服务接口，实现类在 `service.impl`。教程内容的媒体引用组件放在 `content.media`，不与服务接口混放。

HTTP 路径保持为 `/api/public/tutorials/**`、`/api/admin/tutorials/**` 与 `/api/account/learning/**`。数据库迁移仍由 `star-rain-boot` 统一执行。
