/*
 * ProfilePersistenceConfig：作者资料模块的装配配置文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.profile.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 资料持久化配置，属于作者资料模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan("com.starrainnotes.profile.mapper")
public class ProfilePersistenceConfig {
}
