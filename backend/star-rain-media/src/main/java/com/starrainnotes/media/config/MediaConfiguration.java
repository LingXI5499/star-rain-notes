package com.starrainnotes.media.config;

import com.starrainnotes.media.properties.MediaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * Media 模块的装配入口。
 * 开启 @ConfigurationProperties 绑定与定时任务（孤儿文件清理）。
 */
@Configuration
@EnableConfigurationProperties(MediaProperties.class)
@EnableScheduling
public class MediaConfiguration {
}
