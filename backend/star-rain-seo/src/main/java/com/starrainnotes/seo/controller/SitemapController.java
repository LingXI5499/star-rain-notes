/*
 * SitemapController：公开页面与 SEO模块的HTTP 接入文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：SitemapService。
 * 阅读入口：sitemap（sitemap处理入口）。
 */

package com.starrainnotes.seo.controller;

import com.starrainnotes.seo.service.SitemapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sitemap控制器，属于公开页面与 SEO模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
public class SitemapController {
    /**
     * 业务服务。
     * 协作类型：<code>SitemapService</code>。
     */
    private final SitemapService service;

    /**
     * sitemap处理入口。
     * HTTP 映射：<code>@GetMapping(value = "/sitemap.xml", produces = "application/xml")</code>。
     * 主要协作调用：<code>service.xml</code>。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public String sitemap() { return service.xml(); }
}
