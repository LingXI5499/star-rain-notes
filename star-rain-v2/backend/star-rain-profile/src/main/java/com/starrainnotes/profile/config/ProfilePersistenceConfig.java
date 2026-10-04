package com.starrainnotes.profile.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.starrainnotes.profile.mapper")
public class ProfilePersistenceConfig {
}
