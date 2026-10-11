/*
 * SearchRebuildController：搜索模块的HTTP 接入文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、SearchRebuildService。
 * 阅读入口：rebuild（重建搜索Rebuild）。
 */

package com.starrainnotes.search.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.search.service.SearchRebuildService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索Rebuild控制器，属于搜索模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/search")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('search:rebuild')")
public class SearchRebuildController {
    /**
     * 业务服务。
     * 协作类型：<code>SearchRebuildService</code>。
     */
    private final SearchRebuildService service;

    /**
     * 重建搜索Rebuild。
     * HTTP 映射：<code>@PostMapping("/rebuild")</code>。
     * 主要协作调用：<code>service.rebuild</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Map&lt;String, Object&gt;&gt;</code> 的处理结果
     */
    @PostMapping("/rebuild")
    public ApiResponse<Map<String, Object>> rebuild(@RequestParam(required = false) String type) {
        return ApiResponse.ok(Map.of("activeDocuments", service.rebuild(type)));
    }
}
