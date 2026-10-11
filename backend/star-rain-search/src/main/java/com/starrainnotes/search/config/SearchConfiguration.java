package com.starrainnotes.search.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.starrainnotes.search.mapper")
public class SearchConfiguration {
}
