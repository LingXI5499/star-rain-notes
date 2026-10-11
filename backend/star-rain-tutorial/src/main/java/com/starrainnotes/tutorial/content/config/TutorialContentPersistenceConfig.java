/*
 * TutorialContentPersistenceConfig：教程与学习模块的装配配置文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.tutorial.content.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 教程内容持久化配置，属于教程与学习模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan("com.starrainnotes.tutorial.content.mapper")
public class TutorialContentPersistenceConfig {
}
