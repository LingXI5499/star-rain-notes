/*
 * RobotsController：公开页面与 SEO模块的HTTP 接入文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：RobotsService。
 * 阅读入口：robots（robots处理入口）。
 */

package com.starrainnotes.seo.controller;

import com.starrainnotes.seo.service.RobotsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Robots控制器，属于公开页面与 SEO模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
public class RobotsController {
    /**
     * 业务服务。
     * 协作类型：<code>RobotsService</code>。
     */
    private final RobotsService service;

    /**
     * robots处理入口。
     * HTTP 映射：<code>@GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)</code>。
     * 主要协作调用：<code>service.robotsTxt</code>。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() { return service.robotsTxt(); }
}
