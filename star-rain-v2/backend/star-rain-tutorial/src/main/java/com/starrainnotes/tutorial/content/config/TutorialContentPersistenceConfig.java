package com.starrainnotes.tutorial.content.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.starrainnotes.tutorial.content.mapper")
public class TutorialContentPersistenceConfig {
}
