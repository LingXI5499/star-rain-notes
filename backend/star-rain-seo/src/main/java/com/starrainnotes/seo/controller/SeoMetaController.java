/*
 * SeoMetaController：公开页面与 SEO模块的HTTP 接入文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、SeoPageApi。
 * 阅读入口：meta（meta处理入口）。
 */

package com.starrainnotes.seo.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.seo.api.SeoPageApi;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SEOMeta控制器，属于公开页面与 SEO模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
public class SeoMetaController {
    /**
     * pages。
     * 协作类型：<code>SeoPageApi</code>。
     */
    private final SeoPageApi pages;

    /**
     * meta处理入口。
     * HTTP 映射：<code>@GetMapping("/api/public/seo/meta")</code>。
     * 主要协作调用：<code>pages.getMetaByRoute</code>。
     *
     * @param route 路由；类型为 <code>String</code>
     * @return 类型为 <code>ResponseEntity&lt;ApiResponse&lt;Map&lt;String, String&gt;&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/api/public/seo/meta")
    public ResponseEntity<ApiResponse<Map<String, String>>> meta(@RequestParam String route) {
        return pages.getMetaByRoute(route)
            .map(page -> ResponseEntity.ok(ApiResponse.ok(Map.of(
                "title", page.getTitle(), "description", page.getDescription(),
                "canonicalUrl", page.getCanonicalUrl(), "robots", page.getRobotsDirective()))))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
