package com.starrainnotes.analytics.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@MapperScan("com.starrainnotes.analytics.mapper")
public class AnalyticsConfiguration {
}
