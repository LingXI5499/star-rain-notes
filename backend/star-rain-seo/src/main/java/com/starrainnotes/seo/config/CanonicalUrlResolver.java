/*
 * CanonicalUrlResolver：公开页面与 SEO模块的装配配置文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 * 主要协作类型：SeoProperties、SeoRouteInvalidException。
 * 阅读入口：baseUrl（基础地址处理入口）；canonical（规范处理入口）。
 */

package com.starrainnotes.seo.config;

import com.starrainnotes.seo.config.SeoProperties;
import com.starrainnotes.seo.exception.SeoRouteInvalidException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 规范地址Resolver，属于公开页面与 SEO模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Component
@RequiredArgsConstructor
public class CanonicalUrlResolver {
    /**
     * 本模块的外部配置属性。
     * 协作类型：<code>SeoProperties</code>。
     */
    private final SeoProperties properties;

    /**
     * 基础地址处理入口。
     * 主要协作调用：<code>properties.getPublicBaseUrl</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String baseUrl() {
        String raw = properties.getPublicBaseUrl();
        if (raw == null || raw.isBlank()) throw new IllegalStateException("SEO public base URL is required");
        URI uri = URI.create(raw.strip());
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
            || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
            || uri.getFragment() != null || (uri.getPath() != null && !uri.getPath().isBlank()
            && !"/".equals(uri.getPath()))) {
            throw new IllegalStateException("SEO public base URL is invalid");
        }
        return raw.strip().replaceAll("/+$", "");
    }

    /**
     * 规范处理入口。
     * 关键处理步骤调用 <code>routePath.startsWith</code>、<code>routePath.contains</code>、<code>routePath.length</code>、<code>routePath.chars</code>。
     * 实现中的显式失败类型包括 <code>SeoRouteInvalidException</code>；具体触发条件见对应分支。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String canonical(String routePath) {
        if (routePath == null || !routePath.startsWith("/") || routePath.startsWith("//")
            || routePath.contains("?") || routePath.contains("#") || routePath.contains("\\")
            || routePath.contains("..") || routePath.length() > 500
            || routePath.chars().anyMatch(ch -> ch < 32 || ch == 127)) {
            throw new SeoRouteInvalidException();
        }
        return baseUrl() + routePath;
    }
}
