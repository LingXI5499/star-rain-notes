package com.starrainnotes.english.config;

import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {
    "com.starrainnotes.english.overview.mapper",
    "com.starrainnotes.english.vocabulary.mapper",
    "com.starrainnotes.english.grammar.mapper",
    "com.starrainnotes.english.reading.mapper",
    "com.starrainnotes.english.listening.mapper",
    "com.starrainnotes.english.writing.mapper"
}, annotationClass = Mapper.class)
public class EnglishPersistenceConfig {
}
