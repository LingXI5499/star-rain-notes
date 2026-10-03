package com.starrainnotes.tutorial.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.starrainnotes.tutorial.mapper")
public class TutorialPersistenceConfig {
}
