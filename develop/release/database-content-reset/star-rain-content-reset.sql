-- 星雨笔录：教程与作品内容重建脚本
-- 适用结构：Flyway V25 / MySQL 8.0、8.4
-- 作用范围：重建 tutorial_category、tutorial、tutorial_node、portfolio_project。
-- 保留范围：博客、英语、用户、媒体与 flyway_schema_history 均不修改。
-- 重要：执行前必须完成数据库备份；不要使用 mysql --force。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET time_zone = "+00:00";

-- 保存非目标内容数量，事务提交前会再次校验。
SET @before_blog_post = (SELECT COUNT(*) FROM blog_post);
SET @before_vocabulary_theme = (SELECT COUNT(*) FROM vocabulary_theme);
SET @before_vocabulary_word = (SELECT COUNT(*) FROM vocabulary_word);
SET @before_grammar_lesson = (SELECT COUNT(*) FROM english_grammar_lesson);
SET @before_reading_article = (SELECT COUNT(*) FROM english_reading_article);
SET @before_listening_item = (SELECT COUNT(*) FROM english_listening_item);
SET @before_writing_prompt = (SELECT COUNT(*) FROM english_writing_prompt);

START TRANSACTION;

-- 清理仅与教程/作品相关的引用；博客和英语审核记录保留。
DELETE FROM content_review_request WHERE content_type = 'TUTORIAL_CHAPTER';
DELETE FROM profile_selected_content
WHERE tutorial_id IS NOT NULL OR portfolio_project_id IS NOT NULL;

-- 按外键依赖顺序清空教程体系与作品。
DELETE FROM tutorial_node WHERE node_type = 'CHAPTER';
DELETE FROM tutorial_node WHERE node_type = 'GROUP';
DELETE FROM tutorial;
DELETE FROM tutorial_category;
DELETE FROM portfolio_project;

-- 五大知识体系。
INSERT INTO tutorial_category (parent_id, name, slug, sort_order)
VALUES (NULL, 'Java 全栈知识体系', 'java-fullstack', 100);
SET @category_java_fullstack = LAST_INSERT_ID();

INSERT INTO tutorial_category (parent_id, name, slug, sort_order)
VALUES (NULL, '计算机基础知识体系', 'computer-science', 200);
SET @category_computer_science = LAST_INSERT_ID();

INSERT INTO tutorial_category (parent_id, name, slug, sort_order)
VALUES (NULL, '通用编程与开发工具体系', 'programming-tooling', 300);
SET @category_programming_tooling = LAST_INSERT_ID();

INSERT INTO tutorial_category (parent_id, name, slug, sort_order)
VALUES (NULL, '智能体开发知识体系', 'agent-development', 400);
SET @category_agent_development = LAST_INSERT_ID();

INSERT INTO tutorial_category (parent_id, name, slug, sort_order)
VALUES (NULL, '软件工程方法体系', 'software-engineering', 500);
SET @category_software_engineering = LAST_INSERT_ID();

-- 八门公开教程。
INSERT INTO tutorial (
    category_id, title, slug, summary, cover_media_id, publish_status, sort_order,
    seo_title, seo_description, published_at
) VALUES (
    @category_java_fullstack, 'JavaSE 基础', 'javase',
    '以 JDK 21 为实践基线，系统学习 Java 语法、面向对象、集合、IO、并发、网络和常用开发能力。', NULL, 'PUBLISHED', 100,
    'JavaSE 基础 | 星雨笔录', '以 JDK 21 为实践基线，系统学习 Java 语法、面向对象、集合、IO、并发、网络和常用开发能力。', UTC_TIMESTAMP(3)
);
SET @tutorial_javase = LAST_INSERT_ID();

INSERT INTO tutorial (
    category_id, title, slug, summary, cover_media_id, publish_status, sort_order,
    seo_title, seo_description, published_at
) VALUES (
    @category_computer_science, '数据结构与算法', 'data-structures-algorithms',
    '从复杂度、基础数据结构到常用算法范式，建立可分析、可实现、可验证的算法基础。', NULL, 'PUBLISHED', 100,
    '数据结构与算法 | 星雨笔录', '从复杂度、基础数据结构到常用算法范式，建立可分析、可实现、可验证的算法基础。', UTC_TIMESTAMP(3)
);
SET @tutorial_data_structures_algorithms = LAST_INSERT_ID();

INSERT INTO tutorial (
    category_id, title, slug, summary, cover_media_id, publish_status, sort_order,
    seo_title, seo_description, published_at
) VALUES (
    @category_computer_science, '计算机组成原理', 'computer-organization',
    '从数据表示、处理器、指令到存储和输入输出，理解程序在硬件中的执行过程。', NULL, 'PUBLISHED', 200,
    '计算机组成原理 | 星雨笔录', '从数据表示、处理器、指令到存储和输入输出，理解程序在硬件中的执行过程。', UTC_TIMESTAMP(3)
);
SET @tutorial_computer_organization = LAST_INSERT_ID();

INSERT INTO tutorial (
    category_id, title, slug, summary, cover_media_id, publish_status, sort_order,
    seo_title, seo_description, published_at
) VALUES (
    @category_computer_science, '操作系统', 'operating-systems',
    '围绕进程、线程、内存、文件和设备管理，理解操作系统提供抽象与资源隔离的方式。', NULL, 'PUBLISHED', 300,
    '操作系统 | 星雨笔录', '围绕进程、线程、内存、文件和设备管理，理解操作系统提供抽象与资源隔离的方式。', UTC_TIMESTAMP(3)
);
SET @tutorial_operating_systems = LAST_INSERT_ID();

INSERT INTO tutorial (
    category_id, title, slug, summary, cover_media_id, publish_status, sort_order,
    seo_title, seo_description, published_at
) VALUES (
    @category_computer_science, '计算机网络', 'computer-networks',
    '从分层协议到 HTTP、DNS 和 Socket，建立端到端通信与网络排障基础。', NULL, 'PUBLISHED', 400,
    '计算机网络 | 星雨笔录', '从分层协议到 HTTP、DNS 和 Socket，建立端到端通信与网络排障基础。', UTC_TIMESTAMP(3)
);
SET @tutorial_computer_networks = LAST_INSERT_ID();

INSERT INTO tutorial (
    category_id, title, slug, summary, cover_media_id, publish_status, sort_order,
    seo_title, seo_description, published_at
) VALUES (
    @category_programming_tooling, 'Git 与代码协作', 'git-collaboration',
    '掌握版本记录、分支协作、历史整理、冲突解决和代码评审的基础工作流。', NULL, 'PUBLISHED', 100,
    'Git 与代码协作 | 星雨笔录', '掌握版本记录、分支协作、历史整理、冲突解决和代码评审的基础工作流。', UTC_TIMESTAMP(3)
);
SET @tutorial_git_collaboration = LAST_INSERT_ID();

INSERT INTO tutorial (
    category_id, title, slug, summary, cover_media_id, publish_status, sort_order,
    seo_title, seo_description, published_at
) VALUES (
    @category_agent_development, 'Agent 架构与开发', 'agent-architecture-development',
    '从模型、工具、状态与工作流出发，理解可控、可评测、可观测的智能体系统。', NULL, 'PUBLISHED', 100,
    'Agent 架构与开发 | 星雨笔录', '从模型、工具、状态与工作流出发，理解可控、可评测、可观测的智能体系统。', UTC_TIMESTAMP(3)
);
SET @tutorial_agent_architecture_development = LAST_INSERT_ID();

INSERT INTO tutorial (
    category_id, title, slug, summary, cover_media_id, publish_status, sort_order,
    seo_title, seo_description, published_at
) VALUES (
    @category_software_engineering, '软件工程基础与生命周期', 'software-engineering-lifecycle',
    '从生命周期、开发模型和敏捷实践理解软件如何被持续定义、构建、验证和改进。', NULL, 'PUBLISHED', 100,
    '软件工程基础与生命周期 | 星雨笔录', '从生命周期、开发模型和敏捷实践理解软件如何被持续定义、构建、验证和改进。', UTC_TIMESTAMP(3)
);
SET @tutorial_software_engineering_lifecycle = LAST_INSERT_ID();

-- JavaSE：沿用 V2 权威目录中的 19 组、163 个章节标题与 slug。
INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', 'Java 与开发环境', NULL, NULL, NULL,
    NULL, 10, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Java 是什么',
    'what-is-java', '学习Java 是什么的核心概念、使用边界与实践方法。', '# Java 是什么

## 学习目标

- 理解“Java 是什么”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 与开发环境”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Java 是什么”梳理定义、语法或运行机制，并明确它在“Java 与开发环境”中的位置。
- 区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。
- 掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。
- 以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Java 技术平台',
    'java-technology-platforms', '学习Java 技术平台的核心概念、使用边界与实践方法。', '# Java 技术平台

## 学习目标

- 理解“Java 技术平台”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 与开发环境”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Java 技术平台”梳理定义、语法或运行机制，并明确它在“Java 与开发环境”中的位置。
- 区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。
- 掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。
- 以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'JDK、JRE 与 JVM',
    'jdk-jre-jvm', '学习JDK、JRE 与 JVM的核心概念、使用边界与实践方法。', '# JDK、JRE 与 JVM

## 学习目标

- 理解“JDK、JRE 与 JVM”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 与开发环境”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“JDK、JRE 与 JVM”梳理定义、语法或运行机制，并明确它在“Java 与开发环境”中的位置。
- 区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。
- 掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。
- 以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'JDK 版本与 LTS',
    'jdk-versions-lts', '学习JDK 版本与 LTS的核心概念、使用边界与实践方法。', '# JDK 版本与 LTS

## 学习目标

- 理解“JDK 版本与 LTS”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 与开发环境”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“JDK 版本与 LTS”梳理定义、语法或运行机制，并明确它在“Java 与开发环境”中的位置。
- 区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。
- 掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。
- 以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'JDK 环境变量',
    'jdk-environment-variables', '学习JDK 环境变量的核心概念、使用边界与实践方法。', '# JDK 环境变量

## 学习目标

- 理解“JDK 环境变量”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 与开发环境”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“JDK 环境变量”梳理定义、语法或运行机制，并明确它在“Java 与开发环境”中的位置。
- 区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。
- 掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。
- 以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'javac 与 java',
    'javac-java-commands', '学习javac 与 java的核心概念、使用边界与实践方法。', '# javac 与 java

## 学习目标

- 理解“javac 与 java”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 与开发环境”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“javac 与 java”梳理定义、语法或运行机制，并明确它在“Java 与开发环境”中的位置。
- 区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。
- 掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。
- 以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Java 程序开发流程',
    'java-program-development-flow', '学习Java 程序开发流程的核心概念、使用边界与实践方法。', '# Java 程序开发流程

## 学习目标

- 理解“Java 程序开发流程”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 与开发环境”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Java 程序开发流程”梳理定义、语法或运行机制，并明确它在“Java 与开发环境”中的位置。
- 区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。
- 掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。
- 以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Java 跨平台原理',
    'java-cross-platform', '学习Java 跨平台原理的核心概念、使用边界与实践方法。', '# Java 跨平台原理

## 学习目标

- 理解“Java 跨平台原理”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 与开发环境”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Java 跨平台原理”梳理定义、语法或运行机制，并明确它在“Java 与开发环境”中的位置。
- 区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。
- 掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。
- 以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'IntelliJ IDEA',
    'intellij-idea', '学习IntelliJ IDEA的核心概念、使用边界与实践方法。', '# IntelliJ IDEA

## 学习目标

- 理解“IntelliJ IDEA”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 与开发环境”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“IntelliJ IDEA”梳理定义、语法或运行机制，并明确它在“Java 与开发环境”中的位置。
- 区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。
- 掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。
- 以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Project、Module、Package 与 Class',
    'project-module-package-class', '学习Project、Module、Package 与 Class的核心概念、使用边界与实践方法。', '# Project、Module、Package 与 Class

## 学习目标

- 理解“Project、Module、Package 与 Class”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 与开发环境”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Project、Module、Package 与 Class”梳理定义、语法或运行机制，并明确它在“Java 与开发环境”中的位置。
- 区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。
- 掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。
- 以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', 'Java 基本语法', NULL, NULL, NULL,
    NULL, 20, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '注释',
    'comments', '学习注释的核心概念、使用边界与实践方法。', '# 注释

## 学习目标

- 理解“注释”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 基本语法”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“注释”梳理定义、语法或运行机制，并明确它在“Java 基本语法”中的位置。
- 掌握变量、字面量、基本类型、引用类型、作用域和命名规则。
- 理解源代码中的语法结构如何影响编译检查与运行结果。
- 关注数值范围、精度、空引用和输入输出等常见边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '字面量',
    'literals', '学习字面量的核心概念、使用边界与实践方法。', '# 字面量

## 学习目标

- 理解“字面量”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 基本语法”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“字面量”梳理定义、语法或运行机制，并明确它在“Java 基本语法”中的位置。
- 掌握变量、字面量、基本类型、引用类型、作用域和命名规则。
- 理解源代码中的语法结构如何影响编译检查与运行结果。
- 关注数值范围、精度、空引用和输入输出等常见边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '变量',
    'variables', '学习变量的核心概念、使用边界与实践方法。', '# 变量

## 学习目标

- 理解“变量”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 基本语法”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“变量”梳理定义、语法或运行机制，并明确它在“Java 基本语法”中的位置。
- 掌握变量、字面量、基本类型、引用类型、作用域和命名规则。
- 理解源代码中的语法结构如何影响编译检查与运行结果。
- 关注数值范围、精度、空引用和输入输出等常见边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '进制与数据存储',
    'number-systems-data-storage', '学习进制与数据存储的核心概念、使用边界与实践方法。', '# 进制与数据存储

## 学习目标

- 理解“进制与数据存储”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 基本语法”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“进制与数据存储”梳理定义、语法或运行机制，并明确它在“Java 基本语法”中的位置。
- 掌握变量、字面量、基本类型、引用类型、作用域和命名规则。
- 理解源代码中的语法结构如何影响编译检查与运行结果。
- 关注数值范围、精度、空引用和输入输出等常见边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '基本数据类型',
    'primitive-data-types', '学习基本数据类型的核心概念、使用边界与实践方法。', '# 基本数据类型

## 学习目标

- 理解“基本数据类型”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 基本语法”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“基本数据类型”梳理定义、语法或运行机制，并明确它在“Java 基本语法”中的位置。
- 掌握变量、字面量、基本类型、引用类型、作用域和命名规则。
- 理解源代码中的语法结构如何影响编译检查与运行结果。
- 关注数值范围、精度、空引用和输入输出等常见边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '关键字',
    'keywords', '学习关键字的核心概念、使用边界与实践方法。', '# 关键字

## 学习目标

- 理解“关键字”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 基本语法”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“关键字”梳理定义、语法或运行机制，并明确它在“Java 基本语法”中的位置。
- 掌握变量、字面量、基本类型、引用类型、作用域和命名规则。
- 理解源代码中的语法结构如何影响编译检查与运行结果。
- 关注数值范围、精度、空引用和输入输出等常见边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '标识符与命名规范',
    'identifiers-naming-conventions', '学习标识符与命名规范的核心概念、使用边界与实践方法。', '# 标识符与命名规范

## 学习目标

- 理解“标识符与命名规范”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 基本语法”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“标识符与命名规范”梳理定义、语法或运行机制，并明确它在“Java 基本语法”中的位置。
- 掌握变量、字面量、基本类型、引用类型、作用域和命名规则。
- 理解源代码中的语法结构如何影响编译检查与运行结果。
- 关注数值范围、精度、空引用和输入输出等常见边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '输入与输出',
    'input-output', '学习输入与输出的核心概念、使用边界与实践方法。', '# 输入与输出

## 学习目标

- 理解“输入与输出”的核心概念、适用场景与使用边界。
- 能将该知识点与“Java 基本语法”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“输入与输出”梳理定义、语法或运行机制，并明确它在“Java 基本语法”中的位置。
- 掌握变量、字面量、基本类型、引用类型、作用域和命名规则。
- 理解源代码中的语法结构如何影响编译检查与运行结果。
- 关注数值范围、精度、空引用和输入输出等常见边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', '方法、类型转换与运算符', NULL, NULL, NULL,
    NULL, 30, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '方法定义与调用',
    'method-definition-invocation', '学习方法定义与调用的核心概念、使用边界与实践方法。', '# 方法定义与调用

## 学习目标

- 理解“方法定义与调用”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“方法定义与调用”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '方法参数与返回值',
    'method-parameters-return-values', '学习方法参数与返回值的核心概念、使用边界与实践方法。', '# 方法参数与返回值

## 学习目标

- 理解“方法参数与返回值”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“方法参数与返回值”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '方法重载',
    'method-overloading', '学习方法重载的核心概念、使用边界与实践方法。', '# 方法重载

## 学习目标

- 理解“方法重载”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“方法重载”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'return',
    'return-keyword', '学习return的核心概念、使用边界与实践方法。', '# return

## 学习目标

- 理解“return”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“return”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '自动类型转换',
    'implicit-type-conversion', '学习自动类型转换的核心概念、使用边界与实践方法。', '# 自动类型转换

## 学习目标

- 理解“自动类型转换”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“自动类型转换”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '强制类型转换',
    'explicit-type-casting', '学习强制类型转换的核心概念、使用边界与实践方法。', '# 强制类型转换

## 学习目标

- 理解“强制类型转换”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“强制类型转换”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '表达式类型提升',
    'expression-type-promotion', '学习表达式类型提升的核心概念、使用边界与实践方法。', '# 表达式类型提升

## 学习目标

- 理解“表达式类型提升”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“表达式类型提升”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '算术运算符',
    'arithmetic-operators', '学习算术运算符的核心概念、使用边界与实践方法。', '# 算术运算符

## 学习目标

- 理解“算术运算符”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“算术运算符”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '赋值运算符',
    'assignment-operators', '学习赋值运算符的核心概念、使用边界与实践方法。', '# 赋值运算符

## 学习目标

- 理解“赋值运算符”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“赋值运算符”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '关系运算符',
    'relational-operators', '学习关系运算符的核心概念、使用边界与实践方法。', '# 关系运算符

## 学习目标

- 理解“关系运算符”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“关系运算符”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '逻辑运算符',
    'logical-operators', '学习逻辑运算符的核心概念、使用边界与实践方法。', '# 逻辑运算符

## 学习目标

- 理解“逻辑运算符”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“逻辑运算符”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 110, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '自增与自减',
    'increment-decrement', '学习自增与自减的核心概念、使用边界与实践方法。', '# 自增与自减

## 学习目标

- 理解“自增与自减”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“自增与自减”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 120, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '三元运算符',
    'ternary-operator', '学习三元运算符的核心概念、使用边界与实践方法。', '# 三元运算符

## 学习目标

- 理解“三元运算符”的核心概念、适用场景与使用边界。
- 能将该知识点与“方法、类型转换与运算符”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“三元运算符”梳理定义、语法或运行机制，并明确它在“方法、类型转换与运算符”中的位置。
- 理解方法签名、参数传递、返回值和重载规则。
- 区分自动转换与显式转换，避免精度丢失和溢出被忽略。
- 根据优先级、短路规则和表达式类型判断运算结果。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 130, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', '程序流程控制', NULL, NULL, NULL,
    NULL, 40, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '顺序结构',
    'sequential-structure', '学习顺序结构的核心概念、使用边界与实践方法。', '# 顺序结构

## 学习目标

- 理解“顺序结构”的核心概念、适用场景与使用边界。
- 能将该知识点与“程序流程控制”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“顺序结构”梳理定义、语法或运行机制，并明确它在“程序流程控制”中的位置。
- 使用条件、选择和循环结构表达可读、可终止的控制流程。
- 理解 break、continue 与嵌套结构对执行路径的影响。
- 通过边界输入和分支覆盖检查遗漏路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'if',
    'if-statement', '学习if的核心概念、使用边界与实践方法。', '# if

## 学习目标

- 理解“if”的核心概念、适用场景与使用边界。
- 能将该知识点与“程序流程控制”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“if”梳理定义、语法或运行机制，并明确它在“程序流程控制”中的位置。
- 使用条件、选择和循环结构表达可读、可终止的控制流程。
- 理解 break、continue 与嵌套结构对执行路径的影响。
- 通过边界输入和分支覆盖检查遗漏路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'if-else',
    'if-else-statement', '学习if-else的核心概念、使用边界与实践方法。', '# if-else

## 学习目标

- 理解“if-else”的核心概念、适用场景与使用边界。
- 能将该知识点与“程序流程控制”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“if-else”梳理定义、语法或运行机制，并明确它在“程序流程控制”中的位置。
- 使用条件、选择和循环结构表达可读、可终止的控制流程。
- 理解 break、continue 与嵌套结构对执行路径的影响。
- 通过边界输入和分支覆盖检查遗漏路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'switch',
    'switch-statement', '学习switch的核心概念、使用边界与实践方法。', '# switch

## 学习目标

- 理解“switch”的核心概念、适用场景与使用边界。
- 能将该知识点与“程序流程控制”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“switch”梳理定义、语法或运行机制，并明确它在“程序流程控制”中的位置。
- 使用条件、选择和循环结构表达可读、可终止的控制流程。
- 理解 break、continue 与嵌套结构对执行路径的影响。
- 通过边界输入和分支覆盖检查遗漏路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'for',
    'for-loop', '学习for的核心概念、使用边界与实践方法。', '# for

## 学习目标

- 理解“for”的核心概念、适用场景与使用边界。
- 能将该知识点与“程序流程控制”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“for”梳理定义、语法或运行机制，并明确它在“程序流程控制”中的位置。
- 使用条件、选择和循环结构表达可读、可终止的控制流程。
- 理解 break、continue 与嵌套结构对执行路径的影响。
- 通过边界输入和分支覆盖检查遗漏路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'while',
    'while-loop', '学习while的核心概念、使用边界与实践方法。', '# while

## 学习目标

- 理解“while”的核心概念、适用场景与使用边界。
- 能将该知识点与“程序流程控制”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“while”梳理定义、语法或运行机制，并明确它在“程序流程控制”中的位置。
- 使用条件、选择和循环结构表达可读、可终止的控制流程。
- 理解 break、continue 与嵌套结构对执行路径的影响。
- 通过边界输入和分支覆盖检查遗漏路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'do-while',
    'do-while-loop', '学习do-while的核心概念、使用边界与实践方法。', '# do-while

## 学习目标

- 理解“do-while”的核心概念、适用场景与使用边界。
- 能将该知识点与“程序流程控制”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“do-while”梳理定义、语法或运行机制，并明确它在“程序流程控制”中的位置。
- 使用条件、选择和循环结构表达可读、可终止的控制流程。
- 理解 break、continue 与嵌套结构对执行路径的影响。
- 通过边界输入和分支覆盖检查遗漏路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '循环嵌套',
    'nested-loops', '学习循环嵌套的核心概念、使用边界与实践方法。', '# 循环嵌套

## 学习目标

- 理解“循环嵌套”的核心概念、适用场景与使用边界。
- 能将该知识点与“程序流程控制”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“循环嵌套”梳理定义、语法或运行机制，并明确它在“程序流程控制”中的位置。
- 使用条件、选择和循环结构表达可读、可终止的控制流程。
- 理解 break、continue 与嵌套结构对执行路径的影响。
- 通过边界输入和分支覆盖检查遗漏路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'break',
    'break-statement', '学习break的核心概念、使用边界与实践方法。', '# break

## 学习目标

- 理解“break”的核心概念、适用场景与使用边界。
- 能将该知识点与“程序流程控制”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“break”梳理定义、语法或运行机制，并明确它在“程序流程控制”中的位置。
- 使用条件、选择和循环结构表达可读、可终止的控制流程。
- 理解 break、continue 与嵌套结构对执行路径的影响。
- 通过边界输入和分支覆盖检查遗漏路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'continue',
    'continue-statement', '学习continue的核心概念、使用边界与实践方法。', '# continue

## 学习目标

- 理解“continue”的核心概念、适用场景与使用边界。
- 能将该知识点与“程序流程控制”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“continue”梳理定义、语法或运行机制，并明确它在“程序流程控制”中的位置。
- 使用条件、选择和循环结构表达可读、可终止的控制流程。
- 理解 break、continue 与嵌套结构对执行路径的影响。
- 通过边界输入和分支覆盖检查遗漏路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', '数组与二维数组', NULL, NULL, NULL,
    NULL, 50, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '一维数组',
    'one-dimensional-array', '学习一维数组的核心概念、使用边界与实践方法。', '# 一维数组

## 学习目标

- 理解“一维数组”的核心概念、适用场景与使用边界。
- 能将该知识点与“数组与二维数组”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“一维数组”梳理定义、语法或运行机制，并明确它在“数组与二维数组”中的位置。
- 理解数组定长、元素同类型、索引从零开始等基本约束。
- 掌握初始化、遍历、参数传递和二维数组的数组嵌套模型。
- 重点检查越界、空引用、默认值和复制语义。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '数组初始化',
    'array-initialization', '学习数组初始化的核心概念、使用边界与实践方法。', '# 数组初始化

## 学习目标

- 理解“数组初始化”的核心概念、适用场景与使用边界。
- 能将该知识点与“数组与二维数组”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“数组初始化”梳理定义、语法或运行机制，并明确它在“数组与二维数组”中的位置。
- 理解数组定长、元素同类型、索引从零开始等基本约束。
- 掌握初始化、遍历、参数传递和二维数组的数组嵌套模型。
- 重点检查越界、空引用、默认值和复制语义。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '数组访问与遍历',
    'array-access-traversal', '学习数组访问与遍历的核心概念、使用边界与实践方法。', '# 数组访问与遍历

## 学习目标

- 理解“数组访问与遍历”的核心概念、适用场景与使用边界。
- 能将该知识点与“数组与二维数组”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“数组访问与遍历”梳理定义、语法或运行机制，并明确它在“数组与二维数组”中的位置。
- 理解数组定长、元素同类型、索引从零开始等基本约束。
- 掌握初始化、遍历、参数传递和二维数组的数组嵌套模型。
- 重点检查越界、空引用、默认值和复制语义。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '数组常见操作',
    'array-common-operations', '学习数组常见操作的核心概念、使用边界与实践方法。', '# 数组常见操作

## 学习目标

- 理解“数组常见操作”的核心概念、适用场景与使用边界。
- 能将该知识点与“数组与二维数组”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“数组常见操作”梳理定义、语法或运行机制，并明确它在“数组与二维数组”中的位置。
- 理解数组定长、元素同类型、索引从零开始等基本约束。
- 掌握初始化、遍历、参数传递和二维数组的数组嵌套模型。
- 重点检查越界、空引用、默认值和复制语义。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '数组作为方法参数',
    'array-as-method-parameter', '学习数组作为方法参数的核心概念、使用边界与实践方法。', '# 数组作为方法参数

## 学习目标

- 理解“数组作为方法参数”的核心概念、适用场景与使用边界。
- 能将该知识点与“数组与二维数组”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“数组作为方法参数”梳理定义、语法或运行机制，并明确它在“数组与二维数组”中的位置。
- 理解数组定长、元素同类型、索引从零开始等基本约束。
- 掌握初始化、遍历、参数传递和二维数组的数组嵌套模型。
- 重点检查越界、空引用、默认值和复制语义。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '二维数组',
    'two-dimensional-array', '学习二维数组的核心概念、使用边界与实践方法。', '# 二维数组

## 学习目标

- 理解“二维数组”的核心概念、适用场景与使用边界。
- 能将该知识点与“数组与二维数组”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“二维数组”梳理定义、语法或运行机制，并明确它在“数组与二维数组”中的位置。
- 理解数组定长、元素同类型、索引从零开始等基本约束。
- 掌握初始化、遍历、参数传递和二维数组的数组嵌套模型。
- 重点检查越界、空引用、默认值和复制语义。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', '面向对象基础', NULL, NULL, NULL,
    NULL, 60, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '面向对象思想',
    'oop-thinking', '学习面向对象思想的核心概念、使用边界与实践方法。', '# 面向对象思想

## 学习目标

- 理解“面向对象思想”的核心概念、适用场景与使用边界。
- 能将该知识点与“面向对象基础”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“面向对象思想”梳理定义、语法或运行机制，并明确它在“面向对象基础”中的位置。
- 用类描述状态和行为，用对象承载具体实例数据。
- 理解封装、构造过程、this 引用和对象之间的协作。
- 区分对象引用与对象本身，关注生命周期和可变状态。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '类与对象',
    'class-and-object', '学习类与对象的核心概念、使用边界与实践方法。', '# 类与对象

## 学习目标

- 理解“类与对象”的核心概念、适用场景与使用边界。
- 能将该知识点与“面向对象基础”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“类与对象”梳理定义、语法或运行机制，并明确它在“面向对象基础”中的位置。
- 用类描述状态和行为，用对象承载具体实例数据。
- 理解封装、构造过程、this 引用和对象之间的协作。
- 区分对象引用与对象本身，关注生命周期和可变状态。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '成员变量',
    'member-variables', '学习成员变量的核心概念、使用边界与实践方法。', '# 成员变量

## 学习目标

- 理解“成员变量”的核心概念、适用场景与使用边界。
- 能将该知识点与“面向对象基础”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“成员变量”梳理定义、语法或运行机制，并明确它在“面向对象基础”中的位置。
- 用类描述状态和行为，用对象承载具体实例数据。
- 理解封装、构造过程、this 引用和对象之间的协作。
- 区分对象引用与对象本身，关注生命周期和可变状态。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '成员方法',
    'member-methods', '学习成员方法的核心概念、使用边界与实践方法。', '# 成员方法

## 学习目标

- 理解“成员方法”的核心概念、适用场景与使用边界。
- 能将该知识点与“面向对象基础”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“成员方法”梳理定义、语法或运行机制，并明确它在“面向对象基础”中的位置。
- 用类描述状态和行为，用对象承载具体实例数据。
- 理解封装、构造过程、this 引用和对象之间的协作。
- 区分对象引用与对象本身，关注生命周期和可变状态。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '构造器',
    'constructors', '学习构造器的核心概念、使用边界与实践方法。', '# 构造器

## 学习目标

- 理解“构造器”的核心概念、适用场景与使用边界。
- 能将该知识点与“面向对象基础”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“构造器”梳理定义、语法或运行机制，并明确它在“面向对象基础”中的位置。
- 用类描述状态和行为，用对象承载具体实例数据。
- 理解封装、构造过程、this 引用和对象之间的协作。
- 区分对象引用与对象本身，关注生命周期和可变状态。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'this',
    'this-keyword', '学习this的核心概念、使用边界与实践方法。', '# this

## 学习目标

- 理解“this”的核心概念、适用场景与使用边界。
- 能将该知识点与“面向对象基础”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“this”梳理定义、语法或运行机制，并明确它在“面向对象基础”中的位置。
- 用类描述状态和行为，用对象承载具体实例数据。
- 理解封装、构造过程、this 引用和对象之间的协作。
- 区分对象引用与对象本身，关注生命周期和可变状态。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '封装',
    'encapsulation', '学习封装的核心概念、使用边界与实践方法。', '# 封装

## 学习目标

- 理解“封装”的核心概念、适用场景与使用边界。
- 能将该知识点与“面向对象基础”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“封装”梳理定义、语法或运行机制，并明确它在“面向对象基础”中的位置。
- 用类描述状态和行为，用对象承载具体实例数据。
- 理解封装、构造过程、this 引用和对象之间的协作。
- 区分对象引用与对象本身，关注生命周期和可变状态。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'private',
    'private-access-modifier', '学习private的核心概念、使用边界与实践方法。', '# private

## 学习目标

- 理解“private”的核心概念、适用场景与使用边界。
- 能将该知识点与“面向对象基础”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“private”梳理定义、语法或运行机制，并明确它在“面向对象基础”中的位置。
- 用类描述状态和行为，用对象承载具体实例数据。
- 理解封装、构造过程、this 引用和对象之间的协作。
- 区分对象引用与对象本身，关注生命周期和可变状态。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'JavaBean',
    'javabean', '学习JavaBean的核心概念、使用边界与实践方法。', '# JavaBean

## 学习目标

- 理解“JavaBean”的核心概念、适用场景与使用边界。
- 能将该知识点与“面向对象基础”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“JavaBean”梳理定义、语法或运行机制，并明确它在“面向对象基础”中的位置。
- 用类描述状态和行为，用对象承载具体实例数据。
- 理解封装、构造过程、this 引用和对象之间的协作。
- 区分对象引用与对象本身，关注生命周期和可变状态。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '对象数组',
    'object-array', '学习对象数组的核心概念、使用边界与实践方法。', '# 对象数组

## 学习目标

- 理解“对象数组”的核心概念、适用场景与使用边界。
- 能将该知识点与“面向对象基础”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“对象数组”梳理定义、语法或运行机制，并明确它在“面向对象基础”中的位置。
- 用类描述状态和行为，用对象承载具体实例数据。
- 理解封装、构造过程、this 引用和对象之间的协作。
- 区分对象引用与对象本身，关注生命周期和可变状态。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', '继承与多态', NULL, NULL, NULL,
    NULL, 70, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'static',
    'static-keyword', '学习static的核心概念、使用边界与实践方法。', '# static

## 学习目标

- 理解“static”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“static”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '继承',
    'inheritance', '学习继承的核心概念、使用边界与实践方法。', '# 继承

## 学习目标

- 理解“继承”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“继承”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '权限修饰符',
    'access-modifiers', '学习权限修饰符的核心概念、使用边界与实践方法。', '# 权限修饰符

## 学习目标

- 理解“权限修饰符”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“权限修饰符”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '方法重写',
    'method-overriding', '学习方法重写的核心概念、使用边界与实践方法。', '# 方法重写

## 学习目标

- 理解“方法重写”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“方法重写”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'super',
    'super-keyword', '学习super的核心概念、使用边界与实践方法。', '# super

## 学习目标

- 理解“super”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“super”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '多态',
    'polymorphism', '学习多态的核心概念、使用边界与实践方法。', '# 多态

## 学习目标

- 理解“多态”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“多态”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '对象类型转换',
    'object-type-casting', '学习对象类型转换的核心概念、使用边界与实践方法。', '# 对象类型转换

## 学习目标

- 理解“对象类型转换”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“对象类型转换”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'instanceof',
    'instanceof-operator', '学习instanceof的核心概念、使用边界与实践方法。', '# instanceof

## 学习目标

- 理解“instanceof”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“instanceof”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'final',
    'final-keyword', '学习final的核心概念、使用边界与实践方法。', '# final

## 学习目标

- 理解“final”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“final”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '常量',
    'constants', '学习常量的核心概念、使用边界与实践方法。', '# 常量

## 学习目标

- 理解“常量”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“常量”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '枚举',
    'enums', '学习枚举的核心概念、使用边界与实践方法。', '# 枚举

## 学习目标

- 理解“枚举”的核心概念、适用场景与使用边界。
- 能将该知识点与“继承与多态”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“枚举”梳理定义、语法或运行机制，并明确它在“继承与多态”中的位置。
- 理解继承表达的 is-a 关系以及方法重写形成的动态分派。
- 掌握访问控制、super、final、类型转换和 instanceof 的边界。
- 优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 110, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', '抽象类与接口', NULL, NULL, NULL,
    NULL, 80, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '抽象类',
    'abstract-classes', '学习抽象类的核心概念、使用边界与实践方法。', '# 抽象类

## 学习目标

- 理解“抽象类”的核心概念、适用场景与使用边界。
- 能将该知识点与“抽象类与接口”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“抽象类”梳理定义、语法或运行机制，并明确它在“抽象类与接口”中的位置。
- 使用抽象类表达共享状态与部分实现，使用接口表达能力契约。
- 理解抽象方法、接口实现、默认方法和静态方法的职责。
- 通过依赖抽象降低调用方与具体实现之间的耦合。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '抽象方法',
    'abstract-methods', '学习抽象方法的核心概念、使用边界与实践方法。', '# 抽象方法

## 学习目标

- 理解“抽象方法”的核心概念、适用场景与使用边界。
- 能将该知识点与“抽象类与接口”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“抽象方法”梳理定义、语法或运行机制，并明确它在“抽象类与接口”中的位置。
- 使用抽象类表达共享状态与部分实现，使用接口表达能力契约。
- 理解抽象方法、接口实现、默认方法和静态方法的职责。
- 通过依赖抽象降低调用方与具体实现之间的耦合。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '接口',
    'interfaces', '学习接口的核心概念、使用边界与实践方法。', '# 接口

## 学习目标

- 理解“接口”的核心概念、适用场景与使用边界。
- 能将该知识点与“抽象类与接口”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“接口”梳理定义、语法或运行机制，并明确它在“抽象类与接口”中的位置。
- 使用抽象类表达共享状态与部分实现，使用接口表达能力契约。
- 理解抽象方法、接口实现、默认方法和静态方法的职责。
- 通过依赖抽象降低调用方与具体实现之间的耦合。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '接口实现',
    'interface-implementation', '学习接口实现的核心概念、使用边界与实践方法。', '# 接口实现

## 学习目标

- 理解“接口实现”的核心概念、适用场景与使用边界。
- 能将该知识点与“抽象类与接口”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“接口实现”梳理定义、语法或运行机制，并明确它在“抽象类与接口”中的位置。
- 使用抽象类表达共享状态与部分实现，使用接口表达能力契约。
- 理解抽象方法、接口实现、默认方法和静态方法的职责。
- 通过依赖抽象降低调用方与具体实现之间的耦合。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '接口与多态',
    'interface-polymorphism', '学习接口与多态的核心概念、使用边界与实践方法。', '# 接口与多态

## 学习目标

- 理解“接口与多态”的核心概念、适用场景与使用边界。
- 能将该知识点与“抽象类与接口”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“接口与多态”梳理定义、语法或运行机制，并明确它在“抽象类与接口”中的位置。
- 使用抽象类表达共享状态与部分实现，使用接口表达能力契约。
- 理解抽象方法、接口实现、默认方法和静态方法的职责。
- 通过依赖抽象降低调用方与具体实现之间的耦合。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '接口应用设计',
    'interface-design-patterns', '学习接口应用设计的核心概念、使用边界与实践方法。', '# 接口应用设计

## 学习目标

- 理解“接口应用设计”的核心概念、适用场景与使用边界。
- 能将该知识点与“抽象类与接口”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“接口应用设计”梳理定义、语法或运行机制，并明确它在“抽象类与接口”中的位置。
- 使用抽象类表达共享状态与部分实现，使用接口表达能力契约。
- 理解抽象方法、接口实现、默认方法和静态方法的职责。
- 通过依赖抽象降低调用方与具体实现之间的耦合。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', '代码块与内部类', NULL, NULL, NULL,
    NULL, 90, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '代码块',
    'code-blocks', '学习代码块的核心概念、使用边界与实践方法。', '# 代码块

## 学习目标

- 理解“代码块”的核心概念、适用场景与使用边界。
- 能将该知识点与“代码块与内部类”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“代码块”梳理定义、语法或运行机制，并明确它在“代码块与内部类”中的位置。
- 区分静态初始化、实例初始化和构造器的执行时机。
- 理解成员内部类、静态嵌套类、局部类和匿名类的使用边界。
- 关注外部实例引用、变量捕获和初始化顺序。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '成员内部类',
    'member-inner-class', '学习成员内部类的核心概念、使用边界与实践方法。', '# 成员内部类

## 学习目标

- 理解“成员内部类”的核心概念、适用场景与使用边界。
- 能将该知识点与“代码块与内部类”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“成员内部类”梳理定义、语法或运行机制，并明确它在“代码块与内部类”中的位置。
- 区分静态初始化、实例初始化和构造器的执行时机。
- 理解成员内部类、静态嵌套类、局部类和匿名类的使用边界。
- 关注外部实例引用、变量捕获和初始化顺序。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '静态内部类',
    'static-inner-class', '学习静态内部类的核心概念、使用边界与实践方法。', '# 静态内部类

## 学习目标

- 理解“静态内部类”的核心概念、适用场景与使用边界。
- 能将该知识点与“代码块与内部类”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“静态内部类”梳理定义、语法或运行机制，并明确它在“代码块与内部类”中的位置。
- 区分静态初始化、实例初始化和构造器的执行时机。
- 理解成员内部类、静态嵌套类、局部类和匿名类的使用边界。
- 关注外部实例引用、变量捕获和初始化顺序。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '局部内部类',
    'local-inner-class', '学习局部内部类的核心概念、使用边界与实践方法。', '# 局部内部类

## 学习目标

- 理解“局部内部类”的核心概念、适用场景与使用边界。
- 能将该知识点与“代码块与内部类”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“局部内部类”梳理定义、语法或运行机制，并明确它在“代码块与内部类”中的位置。
- 区分静态初始化、实例初始化和构造器的执行时机。
- 理解成员内部类、静态嵌套类、局部类和匿名类的使用边界。
- 关注外部实例引用、变量捕获和初始化顺序。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '匿名内部类',
    'anonymous-inner-class', '学习匿名内部类的核心概念、使用边界与实践方法。', '# 匿名内部类

## 学习目标

- 理解“匿名内部类”的核心概念、适用场景与使用边界。
- 能将该知识点与“代码块与内部类”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“匿名内部类”梳理定义、语法或运行机制，并明确它在“代码块与内部类”中的位置。
- 区分静态初始化、实例初始化和构造器的执行时机。
- 理解成员内部类、静态嵌套类、局部类和匿名类的使用边界。
- 关注外部实例引用、变量捕获和初始化顺序。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', 'Lambda 与方法引用', NULL, NULL, NULL,
    NULL, 100, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '函数式编程思想',
    'functional-programming-thinking', '学习函数式编程思想的核心概念、使用边界与实践方法。', '# 函数式编程思想

## 学习目标

- 理解“函数式编程思想”的核心概念、适用场景与使用边界。
- 能将该知识点与“Lambda 与方法引用”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“函数式编程思想”梳理定义、语法或运行机制，并明确它在“Lambda 与方法引用”中的位置。
- 理解函数式接口是 Lambda 和方法引用的目标类型。
- 掌握参数、返回值、变量捕获与 effectively final 约束。
- 用行为参数化减少样板代码，同时保持命名和副作用清晰。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Lambda 表达式',
    'lambda-expressions', '学习Lambda 表达式的核心概念、使用边界与实践方法。', '# Lambda 表达式

## 学习目标

- 理解“Lambda 表达式”的核心概念、适用场景与使用边界。
- 能将该知识点与“Lambda 与方法引用”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Lambda 表达式”梳理定义、语法或运行机制，并明确它在“Lambda 与方法引用”中的位置。
- 理解函数式接口是 Lambda 和方法引用的目标类型。
- 掌握参数、返回值、变量捕获与 effectively final 约束。
- 用行为参数化减少样板代码，同时保持命名和副作用清晰。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '函数式接口',
    'functional-interfaces', '学习函数式接口的核心概念、使用边界与实践方法。', '# 函数式接口

## 学习目标

- 理解“函数式接口”的核心概念、适用场景与使用边界。
- 能将该知识点与“Lambda 与方法引用”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“函数式接口”梳理定义、语法或运行机制，并明确它在“Lambda 与方法引用”中的位置。
- 理解函数式接口是 Lambda 和方法引用的目标类型。
- 掌握参数、返回值、变量捕获与 effectively final 约束。
- 用行为参数化减少样板代码，同时保持命名和副作用清晰。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '方法引用',
    'method-references', '学习方法引用的核心概念、使用边界与实践方法。', '# 方法引用

## 学习目标

- 理解“方法引用”的核心概念、适用场景与使用边界。
- 能将该知识点与“Lambda 与方法引用”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“方法引用”梳理定义、语法或运行机制，并明确它在“Lambda 与方法引用”中的位置。
- 理解函数式接口是 Lambda 和方法引用的目标类型。
- 掌握参数、返回值、变量捕获与 effectively final 约束。
- 用行为参数化减少样板代码，同时保持命名和副作用清晰。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', 'Java 常用 API', NULL, NULL, NULL,
    NULL, 110, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', '异常与泛型', NULL, NULL, NULL,
    NULL, 120, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '异常体系',
    'exception-hierarchy', '学习异常体系的核心概念、使用边界与实践方法。', '# 异常体系

## 学习目标

- 理解“异常体系”的核心概念、适用场景与使用边界。
- 能将该知识点与“异常与泛型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“异常体系”梳理定义、语法或运行机制，并明确它在“异常与泛型”中的位置。
- 理解异常层次、受检异常与非受检异常的处理责任。
- 使用泛型在编译期表达类型约束，并理解通配符和类型擦除。
- 避免吞掉异常、过度捕获和不安全的强制类型转换。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '异常处理',
    'exception-handling', '学习异常处理的核心概念、使用边界与实践方法。', '# 异常处理

## 学习目标

- 理解“异常处理”的核心概念、适用场景与使用边界。
- 能将该知识点与“异常与泛型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“异常处理”梳理定义、语法或运行机制，并明确它在“异常与泛型”中的位置。
- 理解异常层次、受检异常与非受检异常的处理责任。
- 使用泛型在编译期表达类型约束，并理解通配符和类型擦除。
- 避免吞掉异常、过度捕获和不安全的强制类型转换。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '异常捕获',
    'exception-catching', '学习异常捕获的核心概念、使用边界与实践方法。', '# 异常捕获

## 学习目标

- 理解“异常捕获”的核心概念、适用场景与使用边界。
- 能将该知识点与“异常与泛型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“异常捕获”梳理定义、语法或运行机制，并明确它在“异常与泛型”中的位置。
- 理解异常层次、受检异常与非受检异常的处理责任。
- 使用泛型在编译期表达类型约束，并理解通配符和类型擦除。
- 避免吞掉异常、过度捕获和不安全的强制类型转换。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '异常抛出',
    'exception-throwing', '学习异常抛出的核心概念、使用边界与实践方法。', '# 异常抛出

## 学习目标

- 理解“异常抛出”的核心概念、适用场景与使用边界。
- 能将该知识点与“异常与泛型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“异常抛出”梳理定义、语法或运行机制，并明确它在“异常与泛型”中的位置。
- 理解异常层次、受检异常与非受检异常的处理责任。
- 使用泛型在编译期表达类型约束，并理解通配符和类型擦除。
- 避免吞掉异常、过度捕获和不安全的强制类型转换。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '自定义异常',
    'custom-exceptions', '学习自定义异常的核心概念、使用边界与实践方法。', '# 自定义异常

## 学习目标

- 理解“自定义异常”的核心概念、适用场景与使用边界。
- 能将该知识点与“异常与泛型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“自定义异常”梳理定义、语法或运行机制，并明确它在“异常与泛型”中的位置。
- 理解异常层次、受检异常与非受检异常的处理责任。
- 使用泛型在编译期表达类型约束，并理解通配符和类型擦除。
- 避免吞掉异常、过度捕获和不安全的强制类型转换。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '泛型',
    'generics', '学习泛型的核心概念、使用边界与实践方法。', '# 泛型

## 学习目标

- 理解“泛型”的核心概念、适用场景与使用边界。
- 能将该知识点与“异常与泛型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“泛型”梳理定义、语法或运行机制，并明确它在“异常与泛型”中的位置。
- 理解异常层次、受检异常与非受检异常的处理责任。
- 使用泛型在编译期表达类型约束，并理解通配符和类型擦除。
- 避免吞掉异常、过度捕获和不安全的强制类型转换。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '泛型类',
    'generic-classes', '学习泛型类的核心概念、使用边界与实践方法。', '# 泛型类

## 学习目标

- 理解“泛型类”的核心概念、适用场景与使用边界。
- 能将该知识点与“异常与泛型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“泛型类”梳理定义、语法或运行机制，并明确它在“异常与泛型”中的位置。
- 理解异常层次、受检异常与非受检异常的处理责任。
- 使用泛型在编译期表达类型约束，并理解通配符和类型擦除。
- 避免吞掉异常、过度捕获和不安全的强制类型转换。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '泛型方法',
    'generic-methods', '学习泛型方法的核心概念、使用边界与实践方法。', '# 泛型方法

## 学习目标

- 理解“泛型方法”的核心概念、适用场景与使用边界。
- 能将该知识点与“异常与泛型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“泛型方法”梳理定义、语法或运行机制，并明确它在“异常与泛型”中的位置。
- 理解异常层次、受检异常与非受检异常的处理责任。
- 使用泛型在编译期表达类型约束，并理解通配符和类型擦除。
- 避免吞掉异常、过度捕获和不安全的强制类型转换。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '泛型接口',
    'generic-interfaces', '学习泛型接口的核心概念、使用边界与实践方法。', '# 泛型接口

## 学习目标

- 理解“泛型接口”的核心概念、适用场景与使用边界。
- 能将该知识点与“异常与泛型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“泛型接口”梳理定义、语法或运行机制，并明确它在“异常与泛型”中的位置。
- 理解异常层次、受检异常与非受检异常的处理责任。
- 使用泛型在编译期表达类型约束，并理解通配符和类型擦除。
- 避免吞掉异常、过度捕获和不安全的强制类型转换。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '泛型通配符',
    'generic-wildcards', '学习泛型通配符的核心概念、使用边界与实践方法。', '# 泛型通配符

## 学习目标

- 理解“泛型通配符”的核心概念、适用场景与使用边界。
- 能将该知识点与“异常与泛型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“泛型通配符”梳理定义、语法或运行机制，并明确它在“异常与泛型”中的位置。
- 理解异常层次、受检异常与非受检异常的处理责任。
- 使用泛型在编译期表达类型约束，并理解通配符和类型擦除。
- 避免吞掉异常、过度捕获和不安全的强制类型转换。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', 'List / Set / Map 集合体系', NULL, NULL, NULL,
    NULL, 130, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Collection',
    'collection-framework', '学习Collection的核心概念、使用边界与实践方法。', '# Collection

## 学习目标

- 理解“Collection”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Collection”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'List',
    'list', '学习List的核心概念、使用边界与实践方法。', '# List

## 学习目标

- 理解“List”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“List”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'ArrayList',
    'arraylist', '学习ArrayList的核心概念、使用边界与实践方法。', '# ArrayList

## 学习目标

- 理解“ArrayList”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“ArrayList”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'LinkedList',
    'linkedlist', '学习LinkedList的核心概念、使用边界与实践方法。', '# LinkedList

## 学习目标

- 理解“LinkedList”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“LinkedList”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Set',
    'set', '学习Set的核心概念、使用边界与实践方法。', '# Set

## 学习目标

- 理解“Set”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Set”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'HashSet',
    'hashset', '学习HashSet的核心概念、使用边界与实践方法。', '# HashSet

## 学习目标

- 理解“HashSet”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“HashSet”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'TreeSet',
    'treeset', '学习TreeSet的核心概念、使用边界与实践方法。', '# TreeSet

## 学习目标

- 理解“TreeSet”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“TreeSet”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Map',
    'map', '学习Map的核心概念、使用边界与实践方法。', '# Map

## 学习目标

- 理解“Map”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Map”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'HashMap',
    'hashmap', '学习HashMap的核心概念、使用边界与实践方法。', '# HashMap

## 学习目标

- 理解“HashMap”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“HashMap”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'TreeMap',
    'treemap', '学习TreeMap的核心概念、使用边界与实践方法。', '# TreeMap

## 学习目标

- 理解“TreeMap”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“TreeMap”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Iterator',
    'iterator', '学习Iterator的核心概念、使用边界与实践方法。', '# Iterator

## 学习目标

- 理解“Iterator”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Iterator”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 110, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Collections',
    'collections-utility', '学习Collections的核心概念、使用边界与实践方法。', '# Collections

## 学习目标

- 理解“Collections”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Collections”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 120, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '集合嵌套',
    'nested-collections', '学习集合嵌套的核心概念、使用边界与实践方法。', '# 集合嵌套

## 学习目标

- 理解“集合嵌套”的核心概念、适用场景与使用边界。
- 能将该知识点与“List / Set / Map 集合体系”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“集合嵌套”梳理定义、语法或运行机制，并明确它在“List / Set / Map 集合体系”中的位置。
- 根据有序性、重复性、键值关系和访问模式选择集合。
- 理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。
- 结合时间复杂度、内存成本和并发要求评估实现。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 130, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', 'Stream API', NULL, NULL, NULL,
    NULL, 140, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Stream 概述',
    'stream-overview', '学习Stream 概述的核心概念、使用边界与实践方法。', '# Stream 概述

## 学习目标

- 理解“Stream 概述”的核心概念、适用场景与使用边界。
- 能将该知识点与“Stream API”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Stream 概述”梳理定义、语法或运行机制，并明确它在“Stream API”中的位置。
- 理解数据源、中间操作和终止操作组成的惰性处理流水线。
- 掌握映射、过滤、归约、收集和分组等常见组合。
- 避免复用流、依赖外部可变状态或盲目并行化。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Stream 创建',
    'stream-creation', '学习Stream 创建的核心概念、使用边界与实践方法。', '# Stream 创建

## 学习目标

- 理解“Stream 创建”的核心概念、适用场景与使用边界。
- 能将该知识点与“Stream API”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Stream 创建”梳理定义、语法或运行机制，并明确它在“Stream API”中的位置。
- 理解数据源、中间操作和终止操作组成的惰性处理流水线。
- 掌握映射、过滤、归约、收集和分组等常见组合。
- 避免复用流、依赖外部可变状态或盲目并行化。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '中间操作',
    'stream-intermediate-operations', '学习中间操作的核心概念、使用边界与实践方法。', '# 中间操作

## 学习目标

- 理解“中间操作”的核心概念、适用场景与使用边界。
- 能将该知识点与“Stream API”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“中间操作”梳理定义、语法或运行机制，并明确它在“Stream API”中的位置。
- 理解数据源、中间操作和终止操作组成的惰性处理流水线。
- 掌握映射、过滤、归约、收集和分组等常见组合。
- 避免复用流、依赖外部可变状态或盲目并行化。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '终结操作',
    'stream-terminal-operations', '学习终结操作的核心概念、使用边界与实践方法。', '# 终结操作

## 学习目标

- 理解“终结操作”的核心概念、适用场景与使用边界。
- 能将该知识点与“Stream API”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“终结操作”梳理定义、语法或运行机制，并明确它在“Stream API”中的位置。
- 理解数据源、中间操作和终止操作组成的惰性处理流水线。
- 掌握映射、过滤、归约、收集和分组等常见组合。
- 避免复用流、依赖外部可变状态或盲目并行化。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '收集结果',
    'stream-collecting-results', '学习收集结果的核心概念、使用边界与实践方法。', '# 收集结果

## 学习目标

- 理解“收集结果”的核心概念、适用场景与使用边界。
- 能将该知识点与“Stream API”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“收集结果”梳理定义、语法或运行机制，并明确它在“Stream API”中的位置。
- 理解数据源、中间操作和终止操作组成的惰性处理流水线。
- 掌握映射、过滤、归约、收集和分组等常见组合。
- 避免复用流、依赖外部可变状态或盲目并行化。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', 'File、字符集与 IO', NULL, NULL, NULL,
    NULL, 150, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'File',
    'file-class', '学习File的核心概念、使用边界与实践方法。', '# File

## 学习目标

- 理解“File”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“File”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '字符集',
    'charsets', '学习字符集的核心概念、使用边界与实践方法。', '# 字符集

## 学习目标

- 理解“字符集”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“字符集”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '编码与解码',
    'encoding-decoding', '学习编码与解码的核心概念、使用边界与实践方法。', '# 编码与解码

## 学习目标

- 理解“编码与解码”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“编码与解码”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'IO 流体系',
    'io-stream-hierarchy', '学习IO 流体系的核心概念、使用边界与实践方法。', '# IO 流体系

## 学习目标

- 理解“IO 流体系”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“IO 流体系”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'FileInputStream',
    'fileinputstream', '学习FileInputStream的核心概念、使用边界与实践方法。', '# FileInputStream

## 学习目标

- 理解“FileInputStream”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“FileInputStream”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'FileOutputStream',
    'fileoutputstream', '学习FileOutputStream的核心概念、使用边界与实践方法。', '# FileOutputStream

## 学习目标

- 理解“FileOutputStream”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“FileOutputStream”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'FileReader',
    'filereader', '学习FileReader的核心概念、使用边界与实践方法。', '# FileReader

## 学习目标

- 理解“FileReader”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“FileReader”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'FileWriter',
    'filewriter', '学习FileWriter的核心概念、使用边界与实践方法。', '# FileWriter

## 学习目标

- 理解“FileWriter”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“FileWriter”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'BufferedInputStream',
    'bufferedinputstream', '学习BufferedInputStream的核心概念、使用边界与实践方法。', '# BufferedInputStream

## 学习目标

- 理解“BufferedInputStream”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“BufferedInputStream”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'BufferedOutputStream',
    'bufferedoutputstream', '学习BufferedOutputStream的核心概念、使用边界与实践方法。', '# BufferedOutputStream

## 学习目标

- 理解“BufferedOutputStream”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“BufferedOutputStream”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'BufferedReader',
    'bufferedreader', '学习BufferedReader的核心概念、使用边界与实践方法。', '# BufferedReader

## 学习目标

- 理解“BufferedReader”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“BufferedReader”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 110, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'BufferedWriter',
    'bufferedwriter', '学习BufferedWriter的核心概念、使用边界与实践方法。', '# BufferedWriter

## 学习目标

- 理解“BufferedWriter”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“BufferedWriter”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 120, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '转换流',
    'conversion-streams', '学习转换流的核心概念、使用边界与实践方法。', '# 转换流

## 学习目标

- 理解“转换流”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“转换流”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 130, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '数据流',
    'data-streams', '学习数据流的核心概念、使用边界与实践方法。', '# 数据流

## 学习目标

- 理解“数据流”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“数据流”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 140, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '打印流',
    'print-streams', '学习打印流的核心概念、使用边界与实践方法。', '# 打印流

## 学习目标

- 理解“打印流”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“打印流”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 150, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '对象序列化',
    'object-serialization', '学习对象序列化的核心概念、使用边界与实践方法。', '# 对象序列化

## 学习目标

- 理解“对象序列化”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“对象序列化”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 160, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '文件操作综合案例',
    'file-operations-case', '学习文件操作综合案例的核心概念、使用边界与实践方法。', '# 文件操作综合案例

## 学习目标

- 理解“文件操作综合案例”的核心概念、适用场景与使用边界。
- 能将该知识点与“File、字符集与 IO”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“文件操作综合案例”梳理定义、语法或运行机制，并明确它在“File、字符集与 IO”中的位置。
- 区分字节流与字符流，理解字符集参与编码和解码。
- 掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。
- 处理部分读取、异常、权限、路径差异和大文件边界。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 170, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', '多线程', NULL, NULL, NULL,
    NULL, 160, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '线程与进程',
    'threads-and-processes', '学习线程与进程的核心概念、使用边界与实践方法。', '# 线程与进程

## 学习目标

- 理解“线程与进程”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“线程与进程”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Thread',
    'thread-class', '学习Thread的核心概念、使用边界与实践方法。', '# Thread

## 学习目标

- 理解“Thread”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Thread”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Runnable',
    'runnable-interface', '学习Runnable的核心概念、使用边界与实践方法。', '# Runnable

## 学习目标

- 理解“Runnable”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Runnable”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Callable',
    'callable-interface', '学习Callable的核心概念、使用边界与实践方法。', '# Callable

## 学习目标

- 理解“Callable”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Callable”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '线程生命周期',
    'thread-lifecycle', '学习线程生命周期的核心概念、使用边界与实践方法。', '# 线程生命周期

## 学习目标

- 理解“线程生命周期”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“线程生命周期”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '线程常用方法',
    'thread-common-methods', '学习线程常用方法的核心概念、使用边界与实践方法。', '# 线程常用方法

## 学习目标

- 理解“线程常用方法”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“线程常用方法”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '线程安全',
    'thread-safety', '学习线程安全的核心概念、使用边界与实践方法。', '# 线程安全

## 学习目标

- 理解“线程安全”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“线程安全”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'synchronized',
    'synchronized-keyword', '学习synchronized的核心概念、使用边界与实践方法。', '# synchronized

## 学习目标

- 理解“synchronized”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“synchronized”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Lock',
    'lock', '学习Lock的核心概念、使用边界与实践方法。', '# Lock

## 学习目标

- 理解“Lock”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Lock”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '线程通信',
    'thread-communication', '学习线程通信的核心概念、使用边界与实践方法。', '# 线程通信

## 学习目标

- 理解“线程通信”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“线程通信”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '线程池',
    'thread-pool', '学习线程池的核心概念、使用边界与实践方法。', '# 线程池

## 学习目标

- 理解“线程池”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“线程池”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 110, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '并发与并行',
    'concurrency-and-parallelism', '学习并发与并行的核心概念、使用边界与实践方法。', '# 并发与并行

## 学习目标

- 理解“并发与并行”的核心概念、适用场景与使用边界。
- 能将该知识点与“多线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“并发与并行”梳理定义、语法或运行机制，并明确它在“多线程”中的位置。
- 理解线程生命周期、共享状态、原子性、可见性和有序性。
- 掌握同步、锁、等待通知、并发工具和线程池的适用场景。
- 通过限制共享、设置超时和正确关闭执行器降低并发风险。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 120, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', '网络编程', NULL, NULL, NULL,
    NULL, 170, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '网络通信基本概念',
    'network-communication-basics', '学习网络通信基本概念的核心概念、使用边界与实践方法。', '# 网络通信基本概念

## 学习目标

- 理解“网络通信基本概念”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络编程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“网络通信基本概念”梳理定义、语法或运行机制，并明确它在“网络编程”中的位置。
- 理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。
- 明确协议边界、消息 framing、超时、重试和连接释放。
- 使用本地客户端与服务端验证正常、断连和异常输入路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'IP 地址',
    'ip-address', '学习IP 地址的核心概念、使用边界与实践方法。', '# IP 地址

## 学习目标

- 理解“IP 地址”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络编程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“IP 地址”梳理定义、语法或运行机制，并明确它在“网络编程”中的位置。
- 理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。
- 明确协议边界、消息 framing、超时、重试和连接释放。
- 使用本地客户端与服务端验证正常、断连和异常输入路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '端口',
    'port', '学习端口的核心概念、使用边界与实践方法。', '# 端口

## 学习目标

- 理解“端口”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络编程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“端口”梳理定义、语法或运行机制，并明确它在“网络编程”中的位置。
- 理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。
- 明确协议边界、消息 framing、超时、重试和连接释放。
- 使用本地客户端与服务端验证正常、断连和异常输入路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '协议',
    'protocol', '学习协议的核心概念、使用边界与实践方法。', '# 协议

## 学习目标

- 理解“协议”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络编程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“协议”梳理定义、语法或运行机制，并明确它在“网络编程”中的位置。
- 理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。
- 明确协议边界、消息 framing、超时、重试和连接释放。
- 使用本地客户端与服务端验证正常、断连和异常输入路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'InetAddress',
    'inetaddress', '学习InetAddress的核心概念、使用边界与实践方法。', '# InetAddress

## 学习目标

- 理解“InetAddress”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络编程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“InetAddress”梳理定义、语法或运行机制，并明确它在“网络编程”中的位置。
- 理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。
- 明确协议边界、消息 framing、超时、重试和连接释放。
- 使用本地客户端与服务端验证正常、断连和异常输入路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'UDP',
    'udp', '学习UDP的核心概念、使用边界与实践方法。', '# UDP

## 学习目标

- 理解“UDP”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络编程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“UDP”梳理定义、语法或运行机制，并明确它在“网络编程”中的位置。
- 理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。
- 明确协议边界、消息 framing、超时、重试和连接释放。
- 使用本地客户端与服务端验证正常、断连和异常输入路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'TCP',
    'tcp', '学习TCP的核心概念、使用边界与实践方法。', '# TCP

## 学习目标

- 理解“TCP”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络编程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“TCP”梳理定义、语法或运行机制，并明确它在“网络编程”中的位置。
- 理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。
- 明确协议边界、消息 framing、超时、重试和连接释放。
- 使用本地客户端与服务端验证正常、断连和异常输入路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Socket',
    'socket', '学习Socket的核心概念、使用边界与实践方法。', '# Socket

## 学习目标

- 理解“Socket”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络编程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Socket”梳理定义、语法或运行机制，并明确它在“网络编程”中的位置。
- 理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。
- 明确协议边界、消息 framing、超时、重试和连接释放。
- 使用本地客户端与服务端验证正常、断连和异常输入路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'ServerSocket',
    'serversocket', '学习ServerSocket的核心概念、使用边界与实践方法。', '# ServerSocket

## 学习目标

- 理解“ServerSocket”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络编程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“ServerSocket”梳理定义、语法或运行机制，并明确它在“网络编程”中的位置。
- 理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。
- 明确协议边界、消息 framing、超时、重试和连接释放。
- 使用本地客户端与服务端验证正常、断连和异常输入路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '多客户端通信',
    'multi-client-communication', '学习多客户端通信的核心概念、使用边界与实践方法。', '# 多客户端通信

## 学习目标

- 理解“多客户端通信”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络编程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“多客户端通信”梳理定义、语法或运行机制，并明确它在“网络编程”中的位置。
- 理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。
- 明确协议边界、消息 framing、超时、重试和连接释放。
- 使用本地客户端与服务端验证正常、断连和异常输入路径。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', 'JUnit、反射、注解与动态代理', NULL, NULL, NULL,
    NULL, 180, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'JUnit',
    'junit', '学习JUnit的核心概念、使用边界与实践方法。', '# JUnit

## 学习目标

- 理解“JUnit”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“JUnit”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '反射',
    'reflection', '学习反射的核心概念、使用边界与实践方法。', '# 反射

## 学习目标

- 理解“反射”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“反射”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Class',
    'class-object', '学习Class的核心概念、使用边界与实践方法。', '# Class

## 学习目标

- 理解“Class”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Class”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 30, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Constructor',
    'constructor-reflection', '学习Constructor的核心概念、使用边界与实践方法。', '# Constructor

## 学习目标

- 理解“Constructor”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Constructor”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 40, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Field',
    'field-reflection', '学习Field的核心概念、使用边界与实践方法。', '# Field

## 学习目标

- 理解“Field”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Field”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 50, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Method',
    'method-reflection', '学习Method的核心概念、使用边界与实践方法。', '# Method

## 学习目标

- 理解“Method”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Method”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 60, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '注解',
    'annotations', '学习注解的核心概念、使用边界与实践方法。', '# 注解

## 学习目标

- 理解“注解”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“注解”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 70, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '元注解',
    'meta-annotations', '学习元注解的核心概念、使用边界与实践方法。', '# 元注解

## 学习目标

- 理解“元注解”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“元注解”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 80, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '注解解析',
    'annotation-processing', '学习注解解析的核心概念、使用边界与实践方法。', '# 注解解析

## 学习目标

- 理解“注解解析”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“注解解析”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 90, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '动态代理',
    'dynamic-proxy', '学习动态代理的核心概念、使用边界与实践方法。', '# 动态代理

## 学习目标

- 理解“动态代理”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“动态代理”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 100, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', 'Proxy',
    'proxy-class', '学习Proxy的核心概念、使用边界与实践方法。', '# Proxy

## 学习目标

- 理解“Proxy”的核心概念、适用场景与使用边界。
- 能将该知识点与“JUnit、反射、注解与动态代理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“Proxy”梳理定义、语法或运行机制，并明确它在“JUnit、反射、注解与动态代理”中的位置。
- 使用测试组织前置条件、执行步骤和可重复断言。
- 理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。
- 控制反射与代理的使用范围，保留类型安全和错误诊断能力。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 110, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, NULL, 'GROUP', 'JavaSE 综合案例', NULL, NULL, NULL,
    NULL, 190, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '石头迷阵',
    'stone-labyrinth-game', '学习石头迷阵的核心概念、使用边界与实践方法。', '# 石头迷阵

## 学习目标

- 理解“石头迷阵”的核心概念、适用场景与使用边界。
- 能将该知识点与“JavaSE 综合案例”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“石头迷阵”梳理定义、语法或运行机制，并明确它在“JavaSE 综合案例”中的位置。
- 从需求拆分领域对象、服务边界、数据输入输出和异常策略。
- 综合使用集合、IO、面向对象与测试完成可运行的小型应用。
- 通过分层、重构和自动化测试保持代码可维护。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_javase, @curriculum_group_id, 'CHAPTER', '局域网即时通信',
    'lan-im-chat', '学习局域网即时通信的核心概念、使用边界与实践方法。', '# 局域网即时通信

## 学习目标

- 理解“局域网即时通信”的核心概念、适用场景与使用边界。
- 能将该知识点与“JavaSE 综合案例”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕“局域网即时通信”梳理定义、语法或运行机制，并明确它在“JavaSE 综合案例”中的位置。
- 从需求拆分领域对象、服务边界、数据输入输出和异常策略。
- 综合使用集合、IO、面向对象与测试完成可运行的小型应用。
- 通过分层、重构和自动化测试保持代码可维护。

## 实践建议

- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 20, UTC_TIMESTAMP(3)
);

-- 其余教程：每个真实目录组包含一篇公开提纲章节。
INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '复杂度分析', NULL, NULL, NULL,
    NULL, 10, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '复杂度分析：核心概念与实践',
    'complexity-analysis', '学习复杂度分析：核心概念与实践的核心概念、使用边界与实践方法。', '# 复杂度分析：核心概念与实践

## 学习目标

- 理解“复杂度分析：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“复杂度分析”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 掌握大 O 表示法，区分最好、平均、最坏情况以及时间和空间成本。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '数组与字符串', NULL, NULL, NULL,
    NULL, 20, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '数组与字符串：核心概念与实践',
    'arrays-strings', '学习数组与字符串：核心概念与实践的核心概念、使用边界与实践方法。', '# 数组与字符串：核心概念与实践

## 学习目标

- 理解“数组与字符串：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“数组与字符串”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解连续索引访问、原地修改、双指针、滑动窗口和字符串处理边界。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '链表', NULL, NULL, NULL,
    NULL, 30, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '链表：核心概念与实践',
    'linked-lists', '学习链表：核心概念与实践的核心概念、使用边界与实践方法。', '# 链表：核心概念与实践

## 学习目标

- 理解“链表：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“链表”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 掌握节点链接、哑节点、快慢指针以及插入、删除、反转的指针变化。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '栈与队列', NULL, NULL, NULL,
    NULL, 40, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '栈与队列：核心概念与实践',
    'stacks-queues', '学习栈与队列：核心概念与实践的核心概念、使用边界与实践方法。', '# 栈与队列：核心概念与实践

## 学习目标

- 理解“栈与队列：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“栈与队列”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解后进先出与先进先出语义，以及单调栈、双端队列等常见扩展。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '哈希表', NULL, NULL, NULL,
    NULL, 50, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '哈希表：核心概念与实践',
    'hash-tables', '学习哈希表：核心概念与实践的核心概念、使用边界与实践方法。', '# 哈希表：核心概念与实践

## 学习目标

- 理解“哈希表：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“哈希表”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解散列、冲突、装载因子和键相等约定，识别以空间换时间的场景。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '树与二叉树', NULL, NULL, NULL,
    NULL, 60, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '树与二叉树：核心概念与实践',
    'trees-binary-trees', '学习树与二叉树：核心概念与实践的核心概念、使用边界与实践方法。', '# 树与二叉树：核心概念与实践

## 学习目标

- 理解“树与二叉树：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“树与二叉树”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 掌握树的层级结构、递归定义以及前序、中序、后序和层序遍历。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '堆与优先队列', NULL, NULL, NULL,
    NULL, 70, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '堆与优先队列：核心概念与实践',
    'heaps-priority-queues', '学习堆与优先队列：核心概念与实践的核心概念、使用边界与实践方法。', '# 堆与优先队列：核心概念与实践

## 学习目标

- 理解“堆与优先队列：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“堆与优先队列”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解堆序性质、上浮下沉和 Top-K、动态最值等典型应用。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '图', NULL, NULL, NULL,
    NULL, 80, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '图：核心概念与实践',
    'graphs', '学习图：核心概念与实践的核心概念、使用边界与实践方法。', '# 图：核心概念与实践

## 学习目标

- 理解“图：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“图”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 使用邻接表或邻接矩阵表示图，掌握 BFS、DFS 与连通性分析。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '排序与查找', NULL, NULL, NULL,
    NULL, 90, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '排序与查找：核心概念与实践',
    'sorting-searching', '学习排序与查找：核心概念与实践的核心概念、使用边界与实践方法。', '# 排序与查找：核心概念与实践

## 学习目标

- 理解“排序与查找：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“排序与查找”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 比较常见排序的稳定性和复杂度，并掌握二分查找的循环不变量。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '递归与分治', NULL, NULL, NULL,
    NULL, 100, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '递归与分治：核心概念与实践',
    'recursion-divide-conquer', '学习递归与分治：核心概念与实践的核心概念、使用边界与实践方法。', '# 递归与分治：核心概念与实践

## 学习目标

- 理解“递归与分治：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“递归与分治”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 明确递归终止条件、子问题定义和合并过程，控制调用深度。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '回溯', NULL, NULL, NULL,
    NULL, 110, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '回溯：核心概念与实践',
    'backtracking', '学习回溯：核心概念与实践的核心概念、使用边界与实践方法。', '# 回溯：核心概念与实践

## 学习目标

- 理解“回溯：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“回溯”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 用选择、约束、撤销描述搜索树，并通过剪枝减少无效分支。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '贪心算法', NULL, NULL, NULL,
    NULL, 120, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '贪心算法：核心概念与实践',
    'greedy-algorithms', '学习贪心算法：核心概念与实践的核心概念、使用边界与实践方法。', '# 贪心算法：核心概念与实践

## 学习目标

- 理解“贪心算法：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“贪心算法”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 识别局部最优选择，并用交换论证或结构性质说明正确性。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '动态规划', NULL, NULL, NULL,
    NULL, 130, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '动态规划：核心概念与实践',
    'dynamic-programming', '学习动态规划：核心概念与实践的核心概念、使用边界与实践方法。', '# 动态规划：核心概念与实践

## 学习目标

- 理解“动态规划：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“动态规划”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 定义状态、转移、初值与遍历顺序，并评估状态压缩的可行性。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '字符串算法', NULL, NULL, NULL,
    NULL, 140, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '字符串算法：核心概念与实践',
    'string-algorithms', '学习字符串算法：核心概念与实践的核心概念、使用边界与实践方法。', '# 字符串算法：核心概念与实践

## 学习目标

- 理解“字符串算法：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“字符串算法”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 围绕匹配、前缀、回文和哈希理解字符串问题的状态与边界。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, NULL, 'GROUP', '算法题复盘', NULL, NULL, NULL,
    NULL, 150, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_data_structures_algorithms, @curriculum_group_id, 'CHAPTER', '算法题复盘：核心概念与实践',
    'algorithm-review', '学习算法题复盘：核心概念与实践的核心概念、使用边界与实践方法。', '# 算法题复盘：核心概念与实践

## 学习目标

- 理解“算法题复盘：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“算法题复盘”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 记录题意建模、错误假设、复杂度和可迁移模式，形成稳定解题流程。
- 用时间复杂度和空间复杂度分析方案随输入规模增长的成本。
- 理解数据结构的操作、不变量与适用场景，而不是只记接口。
- 使用边界样例、反例和复杂度复盘验证算法正确性。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, NULL, 'GROUP', '数据表示', NULL, NULL, NULL,
    NULL, 10, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, @curriculum_group_id, 'CHAPTER', '数据表示：核心概念与实践',
    'data-representation', '学习数据表示：核心概念与实践的核心概念、使用边界与实践方法。', '# 数据表示：核心概念与实践

## 学习目标

- 理解“数据表示：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“数据表示”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解二进制、补码、定点数、浮点数和字符编码的表示范围与误差。
- 从指令执行和数据流动角度连接处理器、存储器与外部设备。
- 区分功能原理与具体产品实现，使用层次结构理解性能权衡。
- 通过位级表示、时序和访存过程解释程序行为。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, NULL, 'GROUP', 'CPU', NULL, NULL, NULL,
    NULL, 20, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, @curriculum_group_id, 'CHAPTER', 'CPU：核心概念与实践',
    'cpu', '学习CPU：核心概念与实践的核心概念、使用边界与实践方法。', '# CPU：核心概念与实践

## 学习目标

- 理解“CPU：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“CPU”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解运算器、控制器、寄存器和时钟如何协同完成取指、译码与执行。
- 从指令执行和数据流动角度连接处理器、存储器与外部设备。
- 区分功能原理与具体产品实现，使用层次结构理解性能权衡。
- 通过位级表示、时序和访存过程解释程序行为。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, NULL, 'GROUP', '指令系统', NULL, NULL, NULL,
    NULL, 30, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, @curriculum_group_id, 'CHAPTER', '指令系统：核心概念与实践',
    'instruction-set', '学习指令系统：核心概念与实践的核心概念、使用边界与实践方法。', '# 指令系统：核心概念与实践

## 学习目标

- 理解“指令系统：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“指令系统”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 掌握指令格式、寻址方式、指令周期以及软件与硬件之间的接口。
- 从指令执行和数据流动角度连接处理器、存储器与外部设备。
- 区分功能原理与具体产品实现，使用层次结构理解性能权衡。
- 通过位级表示、时序和访存过程解释程序行为。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, NULL, 'GROUP', '存储系统', NULL, NULL, NULL,
    NULL, 40, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, @curriculum_group_id, 'CHAPTER', '存储系统：核心概念与实践',
    'memory-system', '学习存储系统：核心概念与实践的核心概念、使用边界与实践方法。', '# 存储系统：核心概念与实践

## 学习目标

- 理解“存储系统：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“存储系统”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解寄存器、缓存、主存和外存构成的层次结构及局部性原理。
- 从指令执行和数据流动角度连接处理器、存储器与外部设备。
- 区分功能原理与具体产品实现，使用层次结构理解性能权衡。
- 通过位级表示、时序和访存过程解释程序行为。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, NULL, 'GROUP', 'Cache', NULL, NULL, NULL,
    NULL, 50, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, @curriculum_group_id, 'CHAPTER', 'Cache：核心概念与实践',
    'cache', '学习Cache：核心概念与实践的核心概念、使用边界与实践方法。', '# Cache：核心概念与实践

## 学习目标

- 理解“Cache：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Cache”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解缓存行、映射、替换和写策略，以及命中率对访问成本的影响。
- 从指令执行和数据流动角度连接处理器、存储器与外部设备。
- 区分功能原理与具体产品实现，使用层次结构理解性能权衡。
- 通过位级表示、时序和访存过程解释程序行为。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, NULL, 'GROUP', '总线', NULL, NULL, NULL,
    NULL, 60, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, @curriculum_group_id, 'CHAPTER', '总线：核心概念与实践',
    'bus', '学习总线：核心概念与实践的核心概念、使用边界与实践方法。', '# 总线：核心概念与实践

## 学习目标

- 理解“总线：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“总线”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解地址、数据和控制信息的传递，以及总线仲裁与同步方式。
- 从指令执行和数据流动角度连接处理器、存储器与外部设备。
- 区分功能原理与具体产品实现，使用层次结构理解性能权衡。
- 通过位级表示、时序和访存过程解释程序行为。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, NULL, 'GROUP', '输入输出系统', NULL, NULL, NULL,
    NULL, 70, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_organization, @curriculum_group_id, 'CHAPTER', '输入输出系统：核心概念与实践',
    'io-system', '学习输入输出系统：核心概念与实践的核心概念、使用边界与实践方法。', '# 输入输出系统：核心概念与实践

## 学习目标

- 理解“输入输出系统：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“输入输出系统”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 比较程序查询、中断和 DMA 等 I/O 控制方式及其处理流程。
- 从指令执行和数据流动角度连接处理器、存储器与外部设备。
- 区分功能原理与具体产品实现，使用层次结构理解性能权衡。
- 通过位级表示、时序和访存过程解释程序行为。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, NULL, 'GROUP', '进程与线程', NULL, NULL, NULL,
    NULL, 10, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, @curriculum_group_id, 'CHAPTER', '进程与线程：核心概念与实践',
    'processes-threads', '学习进程与线程：核心概念与实践的核心概念、使用边界与实践方法。', '# 进程与线程：核心概念与实践

## 学习目标

- 理解“进程与线程：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“进程与线程”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 区分进程资源边界与线程执行单元，理解状态转换和上下文切换。
- 理解操作系统在硬件与应用之间提供的资源管理和抽象。
- 用状态、队列、地址空间和并发关系分析系统行为。
- 结合系统调用与观测工具验证理论模型。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, NULL, 'GROUP', 'CPU 调度', NULL, NULL, NULL,
    NULL, 20, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, @curriculum_group_id, 'CHAPTER', 'CPU 调度：核心概念与实践',
    'cpu-scheduling', '学习CPU 调度：核心概念与实践的核心概念、使用边界与实践方法。', '# CPU 调度：核心概念与实践

## 学习目标

- 理解“CPU 调度：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“CPU 调度”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 比较先来先服务、时间片、优先级等调度思想及响应、公平与吞吐权衡。
- 理解操作系统在硬件与应用之间提供的资源管理和抽象。
- 用状态、队列、地址空间和并发关系分析系统行为。
- 结合系统调用与观测工具验证理论模型。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, NULL, 'GROUP', '内存管理', NULL, NULL, NULL,
    NULL, 30, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, @curriculum_group_id, 'CHAPTER', '内存管理：核心概念与实践',
    'memory-management', '学习内存管理：核心概念与实践的核心概念、使用边界与实践方法。', '# 内存管理：核心概念与实践

## 学习目标

- 理解“内存管理：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“内存管理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解地址转换、分配、分页、分段和内外碎片等基础问题。
- 理解操作系统在硬件与应用之间提供的资源管理和抽象。
- 用状态、队列、地址空间和并发关系分析系统行为。
- 结合系统调用与观测工具验证理论模型。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, NULL, 'GROUP', '虚拟内存', NULL, NULL, NULL,
    NULL, 40, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, @curriculum_group_id, 'CHAPTER', '虚拟内存：核心概念与实践',
    'virtual-memory', '学习虚拟内存：核心概念与实践的核心概念、使用边界与实践方法。', '# 虚拟内存：核心概念与实践

## 学习目标

- 理解“虚拟内存：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“虚拟内存”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解页表、缺页、页面置换和工作集如何提供大于物理内存的地址空间。
- 理解操作系统在硬件与应用之间提供的资源管理和抽象。
- 用状态、队列、地址空间和并发关系分析系统行为。
- 结合系统调用与观测工具验证理论模型。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, NULL, 'GROUP', '文件系统', NULL, NULL, NULL,
    NULL, 50, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, @curriculum_group_id, 'CHAPTER', '文件系统：核心概念与实践',
    'file-systems', '学习文件系统：核心概念与实践的核心概念、使用边界与实践方法。', '# 文件系统：核心概念与实践

## 学习目标

- 理解“文件系统：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“文件系统”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解文件、目录、元数据、块分配、缓存和一致性之间的关系。
- 理解操作系统在硬件与应用之间提供的资源管理和抽象。
- 用状态、队列、地址空间和并发关系分析系统行为。
- 结合系统调用与观测工具验证理论模型。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, NULL, 'GROUP', 'I/O 模型', NULL, NULL, NULL,
    NULL, 60, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, @curriculum_group_id, 'CHAPTER', 'I/O 模型：核心概念与实践',
    'io-models', '学习I/O 模型：核心概念与实践的核心概念、使用边界与实践方法。', '# I/O 模型：核心概念与实践

## 学习目标

- 理解“I/O 模型：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“I/O 模型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 比较阻塞、非阻塞、复用和异步 I/O 的等待与通知方式。
- 理解操作系统在硬件与应用之间提供的资源管理和抽象。
- 用状态、队列、地址空间和并发关系分析系统行为。
- 结合系统调用与观测工具验证理论模型。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, NULL, 'GROUP', '同步与互斥', NULL, NULL, NULL,
    NULL, 70, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, @curriculum_group_id, 'CHAPTER', '同步与互斥：核心概念与实践',
    'synchronization-mutual-exclusion', '学习同步与互斥：核心概念与实践的核心概念、使用边界与实践方法。', '# 同步与互斥：核心概念与实践

## 学习目标

- 理解“同步与互斥：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“同步与互斥”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解临界区、锁、信号量、条件变量以及竞态条件。
- 理解操作系统在硬件与应用之间提供的资源管理和抽象。
- 用状态、队列、地址空间和并发关系分析系统行为。
- 结合系统调用与观测工具验证理论模型。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, NULL, 'GROUP', '死锁', NULL, NULL, NULL,
    NULL, 80, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_operating_systems, @curriculum_group_id, 'CHAPTER', '死锁：核心概念与实践',
    'deadlocks', '学习死锁：核心概念与实践的核心概念、使用边界与实践方法。', '# 死锁：核心概念与实践

## 学习目标

- 理解“死锁：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“死锁”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 掌握死锁必要条件以及预防、避免、检测和恢复的基本策略。
- 理解操作系统在硬件与应用之间提供的资源管理和抽象。
- 用状态、队列、地址空间和并发关系分析系统行为。
- 结合系统调用与观测工具验证理论模型。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, NULL, 'GROUP', '网络分层', NULL, NULL, NULL,
    NULL, 10, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, @curriculum_group_id, 'CHAPTER', '网络分层：核心概念与实践',
    'network-layering', '学习网络分层：核心概念与实践的核心概念、使用边界与实践方法。', '# 网络分层：核心概念与实践

## 学习目标

- 理解“网络分层：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络分层”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解 OSI 与 TCP/IP 模型、封装解封装以及各层的职责边界。
- 使用分层和封装理解数据从应用到链路再返回的过程。
- 区分协议提供的语义、可靠性边界和安全责任。
- 结合抓包、连通性与服务端日志定位网络问题。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, NULL, 'GROUP', 'TCP/IP', NULL, NULL, NULL,
    NULL, 20, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, @curriculum_group_id, 'CHAPTER', 'TCP/IP：核心概念与实践',
    'tcp-ip', '学习TCP/IP：核心概念与实践的核心概念、使用边界与实践方法。', '# TCP/IP：核心概念与实践

## 学习目标

- 理解“TCP/IP：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“TCP/IP”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解 IP 寻址与路由、TCP 连接和可靠传输如何协同提供端到端通信。
- 使用分层和封装理解数据从应用到链路再返回的过程。
- 区分协议提供的语义、可靠性边界和安全责任。
- 结合抓包、连通性与服务端日志定位网络问题。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, NULL, 'GROUP', 'UDP', NULL, NULL, NULL,
    NULL, 30, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, @curriculum_group_id, 'CHAPTER', 'UDP：核心概念与实践',
    'udp', '学习UDP：核心概念与实践的核心概念、使用边界与实践方法。', '# UDP：核心概念与实践

## 学习目标

- 理解“UDP：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“UDP”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解无连接数据报、消息边界、丢包可能性及低开销场景。
- 使用分层和封装理解数据从应用到链路再返回的过程。
- 区分协议提供的语义、可靠性边界和安全责任。
- 结合抓包、连通性与服务端日志定位网络问题。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, NULL, 'GROUP', 'HTTP 与 HTTPS', NULL, NULL, NULL,
    NULL, 40, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, @curriculum_group_id, 'CHAPTER', 'HTTP 与 HTTPS：核心概念与实践',
    'http-https', '学习HTTP 与 HTTPS：核心概念与实践的核心概念、使用边界与实践方法。', '# HTTP 与 HTTPS：核心概念与实践

## 学习目标

- 理解“HTTP 与 HTTPS：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“HTTP 与 HTTPS”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 掌握请求响应、状态码、缓存、连接复用以及 TLS 提供的机密性与身份校验。
- 使用分层和封装理解数据从应用到链路再返回的过程。
- 区分协议提供的语义、可靠性边界和安全责任。
- 结合抓包、连通性与服务端日志定位网络问题。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, NULL, 'GROUP', 'DNS', NULL, NULL, NULL,
    NULL, 50, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, @curriculum_group_id, 'CHAPTER', 'DNS：核心概念与实践',
    'dns', '学习DNS：核心概念与实践的核心概念、使用边界与实践方法。', '# DNS：核心概念与实践

## 学习目标

- 理解“DNS：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“DNS”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解域名层次、递归与迭代查询、记录类型、缓存和 TTL。
- 使用分层和封装理解数据从应用到链路再返回的过程。
- 区分协议提供的语义、可靠性边界和安全责任。
- 结合抓包、连通性与服务端日志定位网络问题。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, NULL, 'GROUP', 'Socket', NULL, NULL, NULL,
    NULL, 60, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, @curriculum_group_id, 'CHAPTER', 'Socket：核心概念与实践',
    'sockets', '学习Socket：核心概念与实践的核心概念、使用边界与实践方法。', '# Socket：核心概念与实践

## 学习目标

- 理解“Socket：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Socket”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解套接字地址、监听、连接、读写、超时和关闭流程。
- 使用分层和封装理解数据从应用到链路再返回的过程。
- 区分协议提供的语义、可靠性边界和安全责任。
- 结合抓包、连通性与服务端日志定位网络问题。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, NULL, 'GROUP', '网络安全', NULL, NULL, NULL,
    NULL, 70, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, @curriculum_group_id, 'CHAPTER', '网络安全：核心概念与实践',
    'network-security', '学习网络安全：核心概念与实践的核心概念、使用边界与实践方法。', '# 网络安全：核心概念与实践

## 学习目标

- 理解“网络安全：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络安全”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 认识窃听、篡改、伪造和拒绝服务风险，并采用加密、认证和最小暴露原则。
- 使用分层和封装理解数据从应用到链路再返回的过程。
- 区分协议提供的语义、可靠性边界和安全责任。
- 结合抓包、连通性与服务端日志定位网络问题。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, NULL, 'GROUP', '网络故障排查', NULL, NULL, NULL,
    NULL, 80, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_computer_networks, @curriculum_group_id, 'CHAPTER', '网络故障排查：核心概念与实践',
    'network-troubleshooting', '学习网络故障排查：核心概念与实践的核心概念、使用边界与实践方法。', '# 网络故障排查：核心概念与实践

## 学习目标

- 理解“网络故障排查：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“网络故障排查”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 按 DNS、路由、端口、TLS、代理和应用日志分层定位问题。
- 使用分层和封装理解数据从应用到链路再返回的过程。
- 区分协议提供的语义、可靠性边界和安全责任。
- 结合抓包、连通性与服务端日志定位网络问题。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, NULL, 'GROUP', '基本操作', NULL, NULL, NULL,
    NULL, 10, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, @curriculum_group_id, 'CHAPTER', '基本操作：核心概念与实践',
    'basic-operations', '学习基本操作：核心概念与实践的核心概念、使用边界与实践方法。', '# 基本操作：核心概念与实践

## 学习目标

- 理解“基本操作：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“基本操作”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 掌握 init、clone、status、add、commit、log、diff、fetch、pull 和 push 的作用。
- 理解工作区、暂存区、本地仓库和远程仓库之间的数据流。
- 用小而清晰的提交保存可审查、可回退的历史。
- 在共享分支操作前明确影响范围，避免破坏他人历史。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, NULL, 'GROUP', '分支模型', NULL, NULL, NULL,
    NULL, 20, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, @curriculum_group_id, 'CHAPTER', '分支模型：核心概念与实践',
    'branching-models', '学习分支模型：核心概念与实践的核心概念、使用边界与实践方法。', '# 分支模型：核心概念与实践

## 学习目标

- 理解“分支模型：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“分支模型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解分支指针和合并策略，并按团队规模选择短分支或主干开发。
- 理解工作区、暂存区、本地仓库和远程仓库之间的数据流。
- 用小而清晰的提交保存可审查、可回退的历史。
- 在共享分支操作前明确影响范围，避免破坏他人历史。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, NULL, 'GROUP', 'Commit 规范', NULL, NULL, NULL,
    NULL, 30, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, @curriculum_group_id, 'CHAPTER', 'Commit 规范：核心概念与实践',
    'commit-conventions', '学习Commit 规范：核心概念与实践的核心概念、使用边界与实践方法。', '# Commit 规范：核心概念与实践

## 学习目标

- 理解“Commit 规范：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Commit 规范”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 让一次提交表达单一意图，并用准确标题和正文说明原因与影响。
- 理解工作区、暂存区、本地仓库和远程仓库之间的数据流。
- 用小而清晰的提交保存可审查、可回退的历史。
- 在共享分支操作前明确影响范围，避免破坏他人历史。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, NULL, 'GROUP', 'Merge 与 Rebase', NULL, NULL, NULL,
    NULL, 40, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, @curriculum_group_id, 'CHAPTER', 'Merge 与 Rebase：核心概念与实践',
    'merge-rebase', '学习Merge 与 Rebase：核心概念与实践的核心概念、使用边界与实践方法。', '# Merge 与 Rebase：核心概念与实践

## 学习目标

- 理解“Merge 与 Rebase：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Merge 与 Rebase”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 区分保留分叉历史的合并与重放提交的变基，避免改写已共享历史。
- 理解工作区、暂存区、本地仓库和远程仓库之间的数据流。
- 用小而清晰的提交保存可审查、可回退的历史。
- 在共享分支操作前明确影响范围，避免破坏他人历史。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, NULL, 'GROUP', '冲突解决', NULL, NULL, NULL,
    NULL, 50, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, @curriculum_group_id, 'CHAPTER', '冲突解决：核心概念与实践',
    'conflict-resolution', '学习冲突解决：核心概念与实践的核心概念、使用边界与实践方法。', '# 冲突解决：核心概念与实践

## 学习目标

- 理解“冲突解决：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“冲突解决”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 逐段确认双方意图，完成构建和测试后再提交冲突解决结果。
- 理解工作区、暂存区、本地仓库和远程仓库之间的数据流。
- 用小而清晰的提交保存可审查、可回退的历史。
- 在共享分支操作前明确影响范围，避免破坏他人历史。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, NULL, 'GROUP', 'GitHub 协作', NULL, NULL, NULL,
    NULL, 60, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_git_collaboration, @curriculum_group_id, 'CHAPTER', 'GitHub 协作：核心概念与实践',
    'github-collaboration', '学习GitHub 协作：核心概念与实践的核心概念、使用边界与实践方法。', '# GitHub 协作：核心概念与实践

## 学习目标

- 理解“GitHub 协作：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“GitHub 协作”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 通过 Fork、分支、Pull Request、评审和保护规则组织协作。
- 理解工作区、暂存区、本地仓库和远程仓库之间的数据流。
- 用小而清晰的提交保存可审查、可回退的历史。
- 在共享分支操作前明确影响范围，避免破坏他人历史。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', 'Agent 概念', NULL, NULL, NULL,
    NULL, 10, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', 'Agent 概念：核心概念与实践',
    'agent-concepts', '学习Agent 概念：核心概念与实践的核心概念、使用边界与实践方法。', '# Agent 概念：核心概念与实践

## 学习目标

- 理解“Agent 概念：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Agent 概念”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解智能体目标、环境、观察、行动与反馈循环，以及它和普通聊天调用的差别。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', 'Tool Calling', NULL, NULL, NULL,
    NULL, 20, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', 'Tool Calling：核心概念与实践',
    'tool-calling', '学习Tool Calling：核心概念与实践的核心概念、使用边界与实践方法。', '# Tool Calling：核心概念与实践

## 学习目标

- 理解“Tool Calling：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Tool Calling”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 使用结构化参数描述工具，校验模型输出，并隔离工具执行错误与模型错误。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', 'Memory', NULL, NULL, NULL,
    NULL, 30, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', 'Memory：核心概念与实践',
    'memory', '学习Memory：核心概念与实践的核心概念、使用边界与实践方法。', '# Memory：核心概念与实践

## 学习目标

- 理解“Memory：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Memory”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 区分会话上下文、持久记忆与检索知识，并控制写入、更新和遗忘策略。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', 'Planning', NULL, NULL, NULL,
    NULL, 40, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', 'Planning：核心概念与实践',
    'planning', '学习Planning：核心概念与实践的核心概念、使用边界与实践方法。', '# Planning：核心概念与实践

## 学习目标

- 理解“Planning：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Planning”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 将复杂目标拆分为可验证步骤，并在新信息和失败后重新规划。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', 'Workflow', NULL, NULL, NULL,
    NULL, 50, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', 'Workflow：核心概念与实践',
    'workflow', '学习Workflow：核心概念与实践的核心概念、使用边界与实践方法。', '# Workflow：核心概念与实践

## 学习目标

- 理解“Workflow：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Workflow”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 用确定性流程编排稳定步骤，仅在需要判断的节点引入模型决策。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', '状态管理', NULL, NULL, NULL,
    NULL, 60, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', '状态管理：核心概念与实践',
    'state-management', '学习状态管理：核心概念与实践的核心概念、使用边界与实践方法。', '# 状态管理：核心概念与实践

## 学习目标

- 理解“状态管理：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“状态管理”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 明确状态结构、生命周期、并发更新和恢复点，避免依赖隐式上下文。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', 'Human-in-the-Loop', NULL, NULL, NULL,
    NULL, 70, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', 'Human-in-the-Loop：核心概念与实践',
    'human-in-the-loop', '学习Human-in-the-Loop：核心概念与实践的核心概念、使用边界与实践方法。', '# Human-in-the-Loop：核心概念与实践

## 学习目标

- 理解“Human-in-the-Loop：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Human-in-the-Loop”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 在高风险、不可逆或信息不足的操作前引入人工确认和修正。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', '多智能体协作', NULL, NULL, NULL,
    NULL, 80, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', '多智能体协作：核心概念与实践',
    'multi-agent-collaboration', '学习多智能体协作：核心概念与实践的核心概念、使用边界与实践方法。', '# 多智能体协作：核心概念与实践

## 学习目标

- 理解“多智能体协作：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“多智能体协作”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 按职责拆分代理，并明确任务分派、共享上下文、冲突处理和终止条件。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', 'Agent 评测', NULL, NULL, NULL,
    NULL, 90, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', 'Agent 评测：核心概念与实践',
    'agent-evaluation', '学习Agent 评测：核心概念与实践的核心概念、使用边界与实践方法。', '# Agent 评测：核心概念与实践

## 学习目标

- 理解“Agent 评测：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Agent 评测”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 覆盖任务成功率、工具正确性、步骤效率、鲁棒性、延迟和成本。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', 'Agent 安全', NULL, NULL, NULL,
    NULL, 100, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', 'Agent 安全：核心概念与实践',
    'agent-security', '学习Agent 安全：核心概念与实践的核心概念、使用边界与实践方法。', '# Agent 安全：核心概念与实践

## 学习目标

- 理解“Agent 安全：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Agent 安全”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 防范提示注入、越权工具调用、敏感数据泄露和不可信输出传播。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, NULL, 'GROUP', 'Agent 可观测性', NULL, NULL, NULL,
    NULL, 110, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_agent_architecture_development, @curriculum_group_id, 'CHAPTER', 'Agent 可观测性：核心概念与实践',
    'agent-observability', '学习Agent 可观测性：核心概念与实践的核心概念、使用边界与实践方法。', '# Agent 可观测性：核心概念与实践

## 学习目标

- 理解“Agent 可观测性：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Agent 可观测性”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 记录模型请求、工具调用、状态变化、错误和耗时，同时对敏感字段脱敏。
- 将智能体视为模型、指令、工具、状态和控制循环组成的系统。
- 为外部操作设置明确输入输出、权限、超时、重试和人工确认。
- 用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_software_engineering_lifecycle, NULL, 'GROUP', '软件生命周期', NULL, NULL, NULL,
    NULL, 10, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_software_engineering_lifecycle, @curriculum_group_id, 'CHAPTER', '软件生命周期：核心概念与实践',
    'software-lifecycle', '学习软件生命周期：核心概念与实践的核心概念、使用边界与实践方法。', '# 软件生命周期：核心概念与实践

## 学习目标

- 理解“软件生命周期：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“软件生命周期”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解从构想到退役的阶段、交付物、参与角色和反馈关系。
- 把需求、设计、实现、测试、交付和运维看作可追踪的连续活动。
- 依据风险、反馈速度和变更成本选择过程，而不是机械套用模型。
- 通过明确完成标准、评审、度量和复盘持续改进交付。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_software_engineering_lifecycle, NULL, 'GROUP', '开发模型', NULL, NULL, NULL,
    NULL, 20, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_software_engineering_lifecycle, @curriculum_group_id, 'CHAPTER', '开发模型：核心概念与实践',
    'development-models', '学习开发模型：核心概念与实践的核心概念、使用边界与实践方法。', '# 开发模型：核心概念与实践

## 学习目标

- 理解“开发模型：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“开发模型”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 比较瀑布、迭代、增量等模型对计划、风险和变更的处理方式。
- 把需求、设计、实现、测试、交付和运维看作可追踪的连续活动。
- 依据风险、反馈速度和变更成本选择过程，而不是机械套用模型。
- 通过明确完成标准、评审、度量和复盘持续改进交付。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_software_engineering_lifecycle, NULL, 'GROUP', '敏捷开发', NULL, NULL, NULL,
    NULL, 30, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_software_engineering_lifecycle, @curriculum_group_id, 'CHAPTER', '敏捷开发：核心概念与实践',
    'agile-development', '学习敏捷开发：核心概念与实践的核心概念、使用边界与实践方法。', '# 敏捷开发：核心概念与实践

## 学习目标

- 理解“敏捷开发：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“敏捷开发”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 理解价值优先、短反馈周期、持续交付和拥抱变化等核心思想。
- 把需求、设计、实现、测试、交付和运维看作可追踪的连续活动。
- 依据风险、反馈速度和变更成本选择过程，而不是机械套用模型。
- 通过明确完成标准、评审、度量和复盘持续改进交付。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_software_engineering_lifecycle, NULL, 'GROUP', 'Scrum', NULL, NULL, NULL,
    NULL, 40, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_software_engineering_lifecycle, @curriculum_group_id, 'CHAPTER', 'Scrum：核心概念与实践',
    'scrum', '学习Scrum：核心概念与实践的核心概念、使用边界与实践方法。', '# Scrum：核心概念与实践

## 学习目标

- 理解“Scrum：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“Scrum”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 掌握角色、事件和工件，并用透明、检查、适应推动迭代。
- 把需求、设计、实现、测试、交付和运维看作可追踪的连续活动。
- 依据风险、反馈速度和变更成本选择过程，而不是机械套用模型。
- 通过明确完成标准、评审、度量和复盘持续改进交付。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_software_engineering_lifecycle, NULL, 'GROUP', '软件过程改进', NULL, NULL, NULL,
    NULL, 50, NULL
);
SET @curriculum_group_id = LAST_INSERT_ID();

INSERT INTO tutorial_node (
    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at
) VALUES (
    @tutorial_software_engineering_lifecycle, @curriculum_group_id, 'CHAPTER', '软件过程改进：核心概念与实践',
    'process-improvement', '学习软件过程改进：核心概念与实践的核心概念、使用边界与实践方法。', '# 软件过程改进：核心概念与实践

## 学习目标

- 理解“软件过程改进：核心概念与实践”的核心概念、适用场景与使用边界。
- 能将该知识点与“软件过程改进”中的相邻内容建立联系。
- 能通过最小示例或可重复实验验证结论，而不是只记术语。

## 核心知识

- 从现状和瓶颈出发设定指标，通过小步实验验证改进效果。
- 把需求、设计、实现、测试、交付和运维看作可追踪的连续活动。
- 依据风险、反馈速度和变更成本选择过程，而不是机械套用模型。
- 通过明确完成标准、评审、度量和复盘持续改进交付。

## 实践建议

- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。
- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。
- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
    'PUBLISHED', 10, UTC_TIMESTAMP(3)
);

-- 唯一公开、上线并精选的作品：星雨笔录。
INSERT INTO portfolio_project (
    title, slug, summary, role, tech_stack, body_markdown, cover_media_id,
    repository_url, demo_url, publish_status, project_status, featured, sort_order,
    started_at, completed_at, seo_title, seo_description, published_at
) VALUES (
    '星雨笔录', 'star-rain-notes', '面向个人知识沉淀与学习管理的一体化系统，覆盖教程、博客、作品、英语学习、搜索与后台内容管理。', '独立设计与开发',
    CAST('["Java 21","Spring Boot 3.5","MyBatis-Plus","MySQL 8","Flyway","Vue 3","TypeScript","Vite","Nginx"]' AS JSON), '# 星雨笔录

星雨笔录是一个前后端分离的个人知识系统，用统一的信息架构管理教程、博客、作品、英语学习内容与个人主页。

## 已实现能力

- 教程分类、课程目录、章节发布与前台阅读。
- 博客、作品、媒体资源、个人资料和站内搜索。
- 英语词汇、语法、阅读、听力、写作与学习记录。
- 管理后台、账号权限、内容审核与发布状态管理。

## 技术实现

- 后端使用 Java 21、Spring Boot 3.5、Spring Security、MyBatis-Plus、MySQL 8 与 Flyway。
- 前端使用 Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios 与 Element Plus。
- 生产环境由 Nginx 提供 HTTPS 和静态资源服务，Spring Boot JAR 提供 API，MySQL 保存业务数据。

## 工程实践

- 数据库结构由 Flyway 按版本迁移，部署配置与业务代码分离。
- 健康检查、日志轮转、备份脚本和反向代理配置随项目维护。
- 公开内容与后台管理使用独立接口，并通过发布状态控制可见范围。', NULL,
    'https://gitee.com/linglingxixixi/yulanlin-blog', 'https://yulanlin.cn',
    'PUBLISHED', 'ONLINE', 1, 100, NULL, NULL,
    '星雨笔录 | 个人知识与英语学习系统', '面向个人知识沉淀与学习管理的一体化系统，覆盖教程、博客、作品、英语学习、搜索与后台内容管理。', UTC_TIMESTAMP(3)
);

-- 事务内硬校验：任何一项不满足都会触发 CHECK 错误，连接关闭后自动回滚。
SET @content_reset_valid = (
    (SELECT COUNT(*) FROM tutorial_category) = 5
    AND (SELECT COUNT(*) FROM tutorial) = 8
    AND (SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'GROUP') = 79
    AND (SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'CHAPTER') = 223
    AND (SELECT COUNT(*) FROM portfolio_project WHERE slug = 'star-rain-notes') = 1
    AND (SELECT COUNT(*) FROM tutorial_node chapter
         LEFT JOIN tutorial_node parent
           ON parent.tutorial_id = chapter.tutorial_id AND parent.id = chapter.parent_id
         WHERE chapter.node_type = 'CHAPTER'
           AND (parent.id IS NULL OR parent.node_type <> 'GROUP')) = 0
    AND (SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'CHAPTER'
         AND (body_markdown IS NULL OR body_markdown = ''
              OR body_markdown REGEXP '待编写|待补充')) = 0
    AND (SELECT COUNT(*) FROM blog_post) = @before_blog_post
    AND (SELECT COUNT(*) FROM vocabulary_theme) = @before_vocabulary_theme
    AND (SELECT COUNT(*) FROM vocabulary_word) = @before_vocabulary_word
    AND (SELECT COUNT(*) FROM english_grammar_lesson) = @before_grammar_lesson
    AND (SELECT COUNT(*) FROM english_reading_article) = @before_reading_article
    AND (SELECT COUNT(*) FROM english_listening_item) = @before_listening_item
    AND (SELECT COUNT(*) FROM english_writing_prompt) = @before_writing_prompt
);

CREATE TEMPORARY TABLE star_rain_content_reset_assert (
    ok TINYINT NOT NULL,
    CONSTRAINT ck_star_rain_content_reset_assert CHECK (ok = 1)
);
INSERT INTO star_rain_content_reset_assert (ok) VALUES (@content_reset_valid);

COMMIT;
DROP TEMPORARY TABLE star_rain_content_reset_assert;

-- 预期结果：5 / 8 / 79 / 223 / 1，且 invalid_chapters=0。
SELECT
    (SELECT COUNT(*) FROM tutorial_category) AS categories,
    (SELECT COUNT(*) FROM tutorial) AS tutorials,
    (SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'GROUP') AS curriculum_groups,
    (SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'CHAPTER') AS published_chapters,
    (SELECT COUNT(*) FROM portfolio_project) AS portfolio_projects;

SELECT COUNT(*) AS invalid_chapters
FROM tutorial_node chapter
LEFT JOIN tutorial_node parent
  ON parent.tutorial_id = chapter.tutorial_id AND parent.id = chapter.parent_id
WHERE chapter.node_type = 'CHAPTER'
  AND (chapter.publish_status <> 'PUBLISHED'
       OR chapter.published_at IS NULL
       OR chapter.body_markdown IS NULL OR chapter.body_markdown = ''
       OR chapter.body_markdown REGEXP '待编写|待补充'
       OR parent.id IS NULL OR parent.node_type <> 'GROUP');

SELECT c.name AS category_name, COUNT(t.id) AS published_tutorials
FROM tutorial_category c
LEFT JOIN tutorial t ON t.category_id = c.id AND t.publish_status = 'PUBLISHED'
GROUP BY c.id, c.name, c.sort_order
ORDER BY c.sort_order, c.id;
