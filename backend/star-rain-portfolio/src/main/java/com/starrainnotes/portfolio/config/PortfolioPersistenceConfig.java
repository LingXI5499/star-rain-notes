package com.starrainnotes.portfolio.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.starrainnotes.portfolio.mapper")
public class PortfolioPersistenceConfig {
}
