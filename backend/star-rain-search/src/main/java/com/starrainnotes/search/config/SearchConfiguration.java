/*
 * SearchConfiguration：搜索模块的装配配置文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.search.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 搜索Configuration，属于搜索模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@MapperScan("com.starrainnotes.search.mapper")
public class SearchConfiguration {
}
