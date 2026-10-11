/*
 * MessagePersistenceConfig：留言模块的装配配置文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.message.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 留言持久化配置，属于留言模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan("com.starrainnotes.message.mapper")
public class MessagePersistenceConfig {
}
