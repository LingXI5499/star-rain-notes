package com.starrainnotes.tutorial.learning.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.starrainnotes.tutorial.learning.mapper")
public class TutorialLearningPersistenceConfig {
}
