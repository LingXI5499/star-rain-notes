package com.starrainnotes.account.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** Registers account persistence components when assembled by the boot module. */
@Configuration
@MapperScan("com.starrainnotes.account.mapper")
public class AccountPersistenceConfig {
}
