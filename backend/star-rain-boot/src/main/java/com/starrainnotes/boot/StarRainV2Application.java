/*
 * StarRainV2Application：应用启动模块的模块支持文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 阅读入口：main（main处理入口）。
 */

package com.starrainnotes.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StarRain2应用，属于应用启动模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@SpringBootApplication(scanBasePackages = "com.starrainnotes")
public class StarRainV2Application {

    /**
     * main处理入口。
     * 关键处理步骤调用 <code>SpringApplication.run</code>。
     *
     * @param args args；类型为 <code>String[]</code>
     */
    public static void main(String[] args) {
        SpringApplication.run(StarRainV2Application.class, args);
    }
}
