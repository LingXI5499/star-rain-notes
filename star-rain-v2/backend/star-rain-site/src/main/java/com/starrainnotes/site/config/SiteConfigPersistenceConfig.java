package com.starrainnotes.site.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.starrainnotes.site.mapper")
public class SiteConfigPersistenceConfig {
}
