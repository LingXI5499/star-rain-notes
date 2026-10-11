/*
 * MediaConfiguration：媒体模块的装配配置文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 * 主要协作类型：MediaProperties。
 */

package com.starrainnotes.media.config;

import com.starrainnotes.media.properties.MediaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * Media 模块的装配入口。
 * 开启 @ConfigurationProperties 绑定与定时任务（孤儿文件清理）。
 */
/**
 * 媒体Configuration，属于媒体模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@EnableConfigurationProperties(MediaProperties.class)
@EnableScheduling
public class MediaConfiguration {
}
