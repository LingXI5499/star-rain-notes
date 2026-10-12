# 知识点准入与来源规则

## 1. 每个知识点回答两个问题

### 为什么要教

至少满足下列两项之一；核心主线优先满足两项以上：

- 是后续 Java 核心知识的直接前置；
- 在当前秋招、实习或初级 Java 面试中高频出现；
- 在真实 Java 项目中经常使用或直接影响正确性；
- 用户现有代码和题库反复使用。

### 凭什么这样讲

技术结论必须回到 JDK 21 官方资料或实际编译运行结果：

- 语言语法与语义：JLS；
- JVM 行为：JVMS；
- API 契约：Java SE API；
- 工具使用：JDK Docs / Dev.java；
- 版本演进：OpenJDK / JEP。

## 2. 来源角色

| 来源 | 只能用于什么 |
|---|---|
| 官方规范/API | 确定技术事实 |
| 当前岗位 JD、面经、招聘题 | 判断需求频率，不作为事实终点 |
| [`C-Resource`](https://github.com/Blitzer207/C-Resource) | 参考目录节奏、最小示例和表达密度 |
| 用户旧教程/题库/代码 | 覆盖检查、案例和练习线索 |
| 社区文章 | 发现线索，必须上溯验证 |

## 3. 优先级

- **CORE**：基础依赖、高频面试或高频工作知识，进入主线并充分讲清。
- **COMMON**：经常使用但不是当前章核心，可进入后续对应章节。
- **OPTIONAL**：便利语法、工具或版本特性，只放补充资料或按需解释。
- **OMIT**：低频、无明确收益、会打断认知顺序的内容，不进入教程。

`var`、JShell 默认归入 `OPTIONAL`，不进入当前核心示例。

## 4. 内部记录格式

每个知识点记录：

```yaml
id:
name:
priority: CORE|COMMON|OPTIONAL|OMIT
need_evidence:
  prerequisites: []
  interview_signals: []
  work_signals: []
technical_sources: []
code_verified: true|false
decision_reason:
```

公开正文不显示这份记录。
