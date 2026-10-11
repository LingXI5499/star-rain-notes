/*
 * AccountPersistenceConfig：账号模块的装配配置文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.account.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/* Registers account persistence components when assembled by the boot module. */
/**
 * 账号持久化配置，属于账号模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan("com.starrainnotes.account.mapper")
public class AccountPersistenceConfig {
}
