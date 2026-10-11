/*
 * PortfolioPersistenceConfig：作品模块的装配配置文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.portfolio.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 作品持久化配置，属于作品模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan("com.starrainnotes.portfolio.mapper")
public class PortfolioPersistenceConfig {
}
