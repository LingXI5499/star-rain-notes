package com.starrainnotes.site.config;

import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.starrainnotes.site", annotationClass = Mapper.class)
public class SiteConfigPersistenceConfig {
}
