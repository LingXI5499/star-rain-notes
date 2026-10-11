/*
 * SiteConfigPersistenceConfig：站点模块的装配配置文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.site.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 站点配置持久化配置，属于站点模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan("com.starrainnotes.site.mapper")
public class SiteConfigPersistenceConfig {
}
