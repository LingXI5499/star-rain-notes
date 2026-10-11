/*
 * SeoProperties：公开页面与 SEO模块的装配配置文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */

package com.starrainnotes.seo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SEO配置属性，属于公开页面与 SEO模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Data
@ConfigurationProperties(prefix = "star-rain.seo")
public class SeoProperties {
    /**
     * 公开基础地址。
     */
    private String publicBaseUrl;
    /**
     * frontend索引路径。
     */
    private String frontendIndexPath;
    /**
     * indexnow。
     */
    private IndexNow indexnow = new IndexNow();

    /**
     * 索引Now，属于公开页面与 SEO模块。
     * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
     */
    @Data
    public static class IndexNow {
        /**
         * 已启用。
         */
        private boolean enabled;
        /**
         * endpoint。
         */
        private String endpoint;
        /**
         * 键。
         */
        private String key;
    }
}
