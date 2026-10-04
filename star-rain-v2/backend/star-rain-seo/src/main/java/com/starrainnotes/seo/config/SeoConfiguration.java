package com.starrainnotes.seo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SeoProperties.class)
@MapperScan("com.starrainnotes.seo.mapper")
public class SeoConfiguration { }
