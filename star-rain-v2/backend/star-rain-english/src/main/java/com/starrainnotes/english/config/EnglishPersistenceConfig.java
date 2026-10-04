package com.starrainnotes.english.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.starrainnotes.english", annotationClass = Mapper.class)
public class EnglishPersistenceConfig {
}
