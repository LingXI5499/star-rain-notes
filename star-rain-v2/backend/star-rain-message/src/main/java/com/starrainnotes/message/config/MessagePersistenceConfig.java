package com.starrainnotes.message.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.starrainnotes.message.mapper")
public class MessagePersistenceConfig {
}
