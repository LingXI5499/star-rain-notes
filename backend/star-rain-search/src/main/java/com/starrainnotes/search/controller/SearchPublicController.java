/*
 * SearchPublicController：搜索模块的HTTP 接入文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、PageResult、SearchRateLimitService、SearchQueryService、SearchHitVO、SearchSuggestionVO。
 * 阅读入口：search（搜索处理入口）；quick（quick处理入口）；suggestions（suggestions处理入口）。
 */

package com.starrainnotes.search.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.search.service.SearchRateLimitService;
import com.starrainnotes.search.service.SearchQueryService;
import com.starrainnotes.search.vo.SearchHitVO;
import com.starrainnotes.search.vo.SearchSuggestionVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索公开控制器，属于搜索模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/search")
public class SearchPublicController {
    /**
     * 业务服务。
     * 协作类型：<code>SearchQueryService</code>。
     */
    private final SearchQueryService service;
    /**
     * rate上限。
     * 协作类型：<code>SearchRateLimitService</code>。
     */
    private final SearchRateLimitService rateLimit;

    /**
     * 搜索处理入口。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>rateLimit.check</code>、<code>service.search</code>。
     *
     * @param q q；类型为 <code>String</code>
     * @param type 类型；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param http HTTP；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;SearchHitVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    public ApiResponse<PageResult<SearchHitVO>> search(@RequestParam(required = false) String q,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest http) {
        rateLimit.check(http.getRemoteAddr());
        return ApiResponse.ok(service.search(q, type, page, pageSize));
    }

    /**
     * quick处理入口。
     * HTTP 映射：<code>@GetMapping("/quick")</code>。
     * 主要协作调用：<code>rateLimit.check</code>、<code>service.quick</code>。
     *
     * @param q q；类型为 <code>String</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @param http HTTP；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;SearchHitVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/quick")
    public ApiResponse<List<SearchHitVO>> quick(@RequestParam(required = false) String q,
        @RequestParam(defaultValue = "8") int limit, HttpServletRequest http) {
        rateLimit.check(http.getRemoteAddr());
        return ApiResponse.ok(service.quick(q, limit));
    }

    /**
     * suggestions处理入口。
     * HTTP 映射：<code>@GetMapping("/suggestions")</code>。
     * 主要协作调用：<code>rateLimit.check</code>、<code>service.suggestions</code>。
     *
     * @param q q；类型为 <code>String</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @param http HTTP；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;SearchSuggestionVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/suggestions")
    public ApiResponse<List<SearchSuggestionVO>> suggestions(@RequestParam(required = false) String q,
        @RequestParam(defaultValue = "8") int limit, HttpServletRequest http) {
        rateLimit.check(http.getRemoteAddr());
        return ApiResponse.ok(service.suggestions(q, limit));
    }

}
