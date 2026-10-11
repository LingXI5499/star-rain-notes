/*
 * SiteDashboardController：站点模块的HTTP 接入文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：SiteDashboardVO、DashboardAggregationService、ApiResponse。
 * 阅读入口：dashboard（概览处理入口）。
 */

package com.starrainnotes.site.controller;

import com.starrainnotes.site.vo.SiteDashboardVO;
import com.starrainnotes.site.service.DashboardAggregationService;

import com.starrainnotes.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点概览控制器，属于站点模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/site")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('site:dashboard-read')")
public class SiteDashboardController {
    /**
     * 业务服务。
     * 协作类型：<code>DashboardAggregationService</code>。
     */
    private final DashboardAggregationService service;

    /**
     * 概览处理入口。
     * HTTP 映射：<code>@GetMapping("/dashboard")</code>。
     * 主要协作调用：<code>service.dashboard</code>。
     * @return 类型为 <code>ApiResponse&lt;SiteDashboardVO&gt;</code> 的处理结果
     */
    @GetMapping("/dashboard")
    public ApiResponse<SiteDashboardVO> dashboard() { return ApiResponse.ok(service.dashboard()); }
}
