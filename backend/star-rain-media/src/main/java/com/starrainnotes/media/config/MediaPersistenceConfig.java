/*
 * MediaPersistenceConfig：媒体模块的装配配置文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.media.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/*
 * Media 模块的持久层注册。
 *
 * 每个业务模块自己声明 @MapperScan，boot 只负责装配：
 * 这样模块可以独立编译与测试，也不会互相影响对方的 mapper 扫描范围。
 * 新增业务模块时这是必做的一步，漏掉会在启动时报
 * “No qualifying bean of type ...Mapper available”。
 */
/**
 * 媒体持久化配置，属于媒体模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan("com.starrainnotes.media.mapper")
public class MediaPersistenceConfig {
}
