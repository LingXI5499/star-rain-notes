/*
 * SeoConfiguration：公开页面与 SEO模块的装配配置文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.seo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SEOConfiguration，属于公开页面与 SEO模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(SeoProperties.class)
@MapperScan("com.starrainnotes.seo.mapper")
public class SeoConfiguration { }
