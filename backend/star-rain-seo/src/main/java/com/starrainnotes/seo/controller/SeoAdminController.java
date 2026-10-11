/*
 * SeoAdminController：公开页面与 SEO模块的HTTP 接入文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、SeoRebuildService。
 * 阅读入口：rebuild（重建SEO后台）。
 */

package com.starrainnotes.seo.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.seo.service.SeoRebuildService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SEO后台控制器，属于公开页面与 SEO模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/seo")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('seo:rebuild')")
public class SeoAdminController {
    /**
     * rebuild。
     * 协作类型：<code>SeoRebuildService</code>。
     */
    private final SeoRebuildService rebuild;

    /**
     * 重建SEO后台。
     * HTTP 映射：<code>@PostMapping("/rebuild")</code>。
     * 主要协作调用：<code>rebuild.rebuild</code>。
     *
     * @param route 路由；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Map&lt;String, Object&gt;&gt;</code> 的处理结果
     */
    @PostMapping("/rebuild")
    public ApiResponse<Map<String, Object>> rebuild(@RequestParam(required = false) String route) {
        return ApiResponse.ok(Map.of("activePages", rebuild.rebuild(route)));
    }
}
