/*
 * SeoHtmlController：公开页面与 SEO模块的HTTP 接入文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：SeoHtmlPageService。
 * 阅读入口：html（HTML处理入口）。
 */

package com.starrainnotes.seo.controller;

import com.starrainnotes.seo.service.SeoHtmlPageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SEOHTML控制器，属于公开页面与 SEO模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
public class SeoHtmlController {
    /**
     * 业务服务。
     * 协作类型：<code>SeoHtmlPageService</code>。
     */
    private final SeoHtmlPageService service;

    /**
     * HTML处理入口。
     * HTTP 映射：<code>@GetMapping(value = {"/", "/blog", "/blog/archive", "/blog/posts/{slug}", "/blog/topics/{slug}", "/tutorials", "/tutorials/{slug}", "/tutorials/{slug}/{chapterSlug}", "/portfolio", "/portfolio/{slug}", "/messages", "/about"}, produces = MediaType.TEXT_HTML_VALUE)</code>。
     * 主要协作调用：<code>service.html</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ResponseEntity&lt;String&gt;</code> 的处理结果
     */
    @GetMapping(value = {"/", "/blog", "/blog/archive", "/blog/posts/{slug}", "/blog/topics/{slug}",
        "/tutorials", "/tutorials/{slug}", "/tutorials/{slug}/{chapterSlug}",
        "/portfolio", "/portfolio/{slug}", "/messages", "/about"},
        produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> html(HttpServletRequest request) {
        return service.html(request.getRequestURI())
            .map(body -> ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
