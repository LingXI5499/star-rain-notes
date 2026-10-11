/*
 * BlogPersistenceConfig：博客模块的装配配置文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.blog.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/*
 * Blog 模块的持久层注册。
 *
 * 每个业务模块自己声明 @MapperScan，boot 只负责装配：
 * 这样模块可以独立编译与测试，也不会互相影响对方的 mapper 扫描范围。
 * 漏掉这一步会在启动时报 “No qualifying bean of type '...BlogPostMapper' available”。
 */
/**
 * 博客持久化配置，属于博客模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan("com.starrainnotes.blog.mapper")
public class BlogPersistenceConfig {
}
