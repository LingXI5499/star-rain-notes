package com.starrainnotes.review.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/*
 * Review 模块的持久层注册。
 *
 * 每个业务模块自己声明 @MapperScan，boot 只负责装配：
 * 这样模块可以独立编译与测试，也不会互相影响对方的 mapper 扫描范围。
 * 新增业务模块时这是必做的一步，漏掉会在启动时报
 * “No qualifying bean of type ...Mapper available”。
 */
@Configuration
@MapperScan("com.starrainnotes.review.mapper")
public class ReviewPersistenceConfig {
}
