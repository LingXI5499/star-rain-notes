/*
 * AnalyticsConfiguration：访问统计模块的装配配置文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.analytics.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 统计Configuration，属于访问统计模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@EnableScheduling
@MapperScan("com.starrainnotes.analytics.mapper")
public class AnalyticsConfiguration {
}
