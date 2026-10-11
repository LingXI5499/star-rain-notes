/*
 * AnalyticsAdminController：访问统计模块的HTTP 接入文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：AnalyticsService、ApiResponse。
 * 阅读入口：summary（摘要处理入口）；trend（trend处理入口）；hot（hot处理入口）；referrers（referrers处理入口）。
 */

package com.starrainnotes.analytics.controller;

import com.starrainnotes.analytics.service.AnalyticsService;
import com.starrainnotes.common.result.ApiResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计后台控制器，属于访问统计模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('analytics:read')")
public class AnalyticsAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>AnalyticsService</code>。
     */
    private final AnalyticsService service;

    /**
     * 摘要处理入口。
     * HTTP 映射：<code>@GetMapping("/summary")</code>。
     * 主要协作调用：<code>service.siteSummary</code>。
     * @return 类型为 <code>ApiResponse&lt;Map&lt;String, Object&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() { return ApiResponse.ok(service.siteSummary()); }

    /**
     * trend处理入口。
     * HTTP 映射：<code>@GetMapping("/trend")</code>。
     * 主要协作调用：<code>service.trend</code>。
     *
     * @param startDate 开始日期；类型为 <code>LocalDate</code>
     * @param endDate 结束日期；类型为 <code>LocalDate</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;Map&lt;String, Object&gt;&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> trend(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(service.trend(start(startDate), end(endDate)));
    }

    /**
     * hot处理入口。
     * HTTP 映射：<code>@GetMapping("/hot-content")</code>。
     * 主要协作调用：<code>service.hotContent</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param startDate 开始日期；类型为 <code>LocalDate</code>
     * @param endDate 结束日期；类型为 <code>LocalDate</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;Map&lt;String, Object&gt;&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/hot-content")
    public ApiResponse<List<Map<String, Object>>> hot(
        @RequestParam(required = false) String type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(service.hotContent(type, start(startDate), end(endDate), limit));
    }

    /**
     * referrers处理入口。
     * HTTP 映射：<code>@GetMapping("/referrers")</code>。
     * 主要协作调用：<code>service.referrers</code>。
     *
     * @param startDate 开始日期；类型为 <code>LocalDate</code>
     * @param endDate 结束日期；类型为 <code>LocalDate</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;Map&lt;String, Object&gt;&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/referrers")
    public ApiResponse<List<Map<String, Object>>> referrers(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(service.referrers(start(startDate), end(endDate)));
    }

    /**
     * 结束处理入口。
     * 关键处理步骤调用 <code>LocalDate.now</code>。
     *
     * @param value 值；类型为 <code>LocalDate</code>
     * @return 类型为 <code>LocalDate</code> 的处理结果
     */
    private LocalDate end(LocalDate value) { return value == null ? LocalDate.now(ZoneOffset.UTC) : value; }
    /**
     * 开始处理入口。
     * 关键处理步骤调用 <code>LocalDate.now</code>。
     *
     * @param value 值；类型为 <code>LocalDate</code>
     * @return 类型为 <code>LocalDate</code> 的处理结果
     */
    private LocalDate start(LocalDate value) { return value == null ? LocalDate.now(ZoneOffset.UTC).minusDays(29) : value; }
}
