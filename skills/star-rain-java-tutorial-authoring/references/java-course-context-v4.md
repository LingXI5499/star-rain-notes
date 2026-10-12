# Java 课程上下文 V4 评审稿

## 课程目标

课程首先服务于三件事：

1. 建立完整、可运行的 JavaSE 基础；
2. 覆盖秋招、实习和初级 Java 岗位的核心考点；
3. 为集合、并发、JVM、网络和后续 Java 后端学习提供真实前置能力。

“JDK 21 有”不等于“核心课程必须教”。版本便利功能只有在高频使用、明显改善主线理解或被真实项目依赖时才进入核心章。

## 结构

- Tutorial：Java 程序设计。
- Group：课程分组，只在左侧课程目录和 Course Manifest 中编号。
- Chapter：网站独立章节，`title` 和 `summary` 字段渲染为页面 H1 与标题下摘要。
- Section：正文必须且只能使用四个 H2：学习目标、核心知识、问题、练习题。
- Knowledge Point：正文 H3，在所属 Section 下编号，例如 `2.1`、`2.2`。
- Subtopic：仅在确有独立子知识时使用 H4，例如 `2.1.1`。

每篇章节的正文编号重新开始，不把课程分组号和章号继续叠加到正文。定义、作用、语法、示例和注意事项属于知识点正文，不是固定标题层级。

## 章节 2.7 的修订边界

- 标题：字面量、变量与作用域。
- 保留：常见字面量、变量、局部变量读取前赋值、块作用域。
- 移出核心：`var`。
- 后续章节负责：基本类型与引用类型、默认值、类型转换、运算符、数组。

## 章节 2.3 的修订方向

- 核心保留：JDK 安装、`JAVA_HOME`、`Path`、`java`、`javac`、版本检查、命令行编译运行。
- 移出核心：JShell。

## 事实来源

- JLS 21：<https://docs.oracle.com/javase/specs/jls/se21/html/>
- JVMS 21：<https://docs.oracle.com/javase/specs/jvms/se21/html/>
- Java SE 21 API：<https://docs.oracle.com/en/java/javase/21/docs/api/>
- JDK 21 Docs：<https://docs.oracle.com/en/java/javase/21/>
- Dev.java：<https://dev.java/>
- OpenJDK：<https://openjdk.org/>

完整课程目录在正式定稿时重新做相关性审计，不沿用“已冻结所以不能删”的逻辑阻止用户纠偏。
