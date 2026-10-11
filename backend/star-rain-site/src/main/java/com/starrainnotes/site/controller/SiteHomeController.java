/*
 * SiteHomeController：站点模块的HTTP 接入文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：SiteHomeVO、HomeAggregationService、ApiResponse。
 * 阅读入口：home（首页处理入口）。
 */

package com.starrainnotes.site.controller;

import com.starrainnotes.site.vo.SiteHomeVO;
import com.starrainnotes.site.service.HomeAggregationService;

import com.starrainnotes.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点首页控制器，属于站点模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/site")
public class SiteHomeController {
    /**
     * 业务服务。
     * 协作类型：<code>HomeAggregationService</code>。
     */
    private final HomeAggregationService service;

    /**
     * 首页处理入口。
     * HTTP 映射：<code>@GetMapping("/home")</code>。
     * 主要协作调用：<code>service.home</code>。
     * @return 类型为 <code>ApiResponse&lt;SiteHomeVO&gt;</code> 的处理结果
     */
    @GetMapping("/home")
    public ApiResponse<SiteHomeVO> home() { return ApiResponse.ok(service.home()); }
}
