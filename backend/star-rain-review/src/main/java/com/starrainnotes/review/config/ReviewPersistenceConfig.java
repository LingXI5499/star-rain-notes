/*
 * ReviewPersistenceConfig：审核模块的装配配置文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

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
/**
 * 审核持久化配置，属于审核模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan("com.starrainnotes.review.mapper")
public class ReviewPersistenceConfig {
}
