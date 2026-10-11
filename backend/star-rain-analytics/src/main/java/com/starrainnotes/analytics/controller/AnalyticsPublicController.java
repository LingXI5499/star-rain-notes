/*
 * AnalyticsPublicController：访问统计模块的HTTP 接入文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：PageViewDTO、AnalyticsRouteInvalidException、AnalyticsSubmissionLimiter、AnalyticsService、ApiResponse。
 * 阅读入口：pageView（分页读取View）。
 */

package com.starrainnotes.analytics.controller;

import com.starrainnotes.analytics.dto.PageViewDTO;
import com.starrainnotes.analytics.exception.AnalyticsRouteInvalidException;
import com.starrainnotes.analytics.security.AnalyticsSubmissionLimiter;
import com.starrainnotes.analytics.service.AnalyticsService;
import com.starrainnotes.common.result.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计公开控制器，属于访问统计模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/analytics")
public class AnalyticsPublicController {
    /**
     * 业务服务。
     * 协作类型：<code>AnalyticsService</code>。
     */
    private final AnalyticsService service;
    /**
     * limiter。
     */
    private final AnalyticsSubmissionLimiter limiter;

    /**
     * 分页读取View。
     * HTTP 映射：<code>@PostMapping("/page-view")</code>。
     * 主要协作调用：<code>limiter.check</code>、<code>service.recordPageView</code>。
     * 实现中的显式失败类型包括 <code>AnalyticsRouteInvalidException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>PageViewDTO</code>
     * @param http HTTP；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/page-view")
    public ApiResponse<Void> pageView(@RequestBody PageViewDTO request, HttpServletRequest http) {
        if (request == null) throw new AnalyticsRouteInvalidException();
        limiter.check(http.getRemoteAddr());
        service.recordPageView(request.getRouteKey(), request.getReferrer());
        return ApiResponse.ok(null);
    }
}
