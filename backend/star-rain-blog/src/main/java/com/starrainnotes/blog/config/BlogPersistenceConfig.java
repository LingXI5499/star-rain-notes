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
@Configuration
@MapperScan("com.starrainnotes.blog.mapper")
public class BlogPersistenceConfig {
}
