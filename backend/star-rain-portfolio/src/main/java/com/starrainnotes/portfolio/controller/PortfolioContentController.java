/*
 * PortfolioContentController：作品模块的HTTP 接入文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、IdOrderDTO、WorkSectionDTO、PortfolioContentService。
 * 阅读入口：taxonomy（taxonomy处理入口）；templates（templates处理入口）；create（创建作品内容）；update（更新作品内容）。
 */

package com.starrainnotes.portfolio.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.portfolio.dto.IdOrderDTO;
import com.starrainnotes.portfolio.dto.WorkSectionDTO;
import com.starrainnotes.portfolio.service.PortfolioContentService;
import com.starrainnotes.portfolio.vo.*;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 作品内容控制器，属于作品模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
public class PortfolioContentController {
    /**
     * 内容。
     * 协作类型：<code>PortfolioContentService</code>。
     */
    private final PortfolioContentService content;
    /**
     * taxonomy处理入口。
     * HTTP 映射：<code>@GetMapping("/api/public/portfolio/taxonomy")</code>。
     * 主要协作调用：<code>content.taxonomy</code>。
     * @return 类型为 <code>ApiResponse&lt;Map&lt;String, List&lt;WorkTaxonomyVO&gt;&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/api/public/portfolio/taxonomy")
    public ApiResponse<Map<String, List<WorkTaxonomyVO>>> taxonomy() { return ApiResponse.ok(content.taxonomy()); }
    /**
     * templates处理入口。
     * HTTP 映射：<code>@GetMapping("/api/admin/portfolio/templates")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'portfolio:read-admin\')")</code>。
     * 主要协作调用：<code>content.templates</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;WorkTemplateVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/api/admin/portfolio/templates")
    @PreAuthorize("hasAuthority('portfolio:read-admin')")
    public ApiResponse<List<WorkTemplateVO>> templates() { return ApiResponse.ok(content.templates()); }
    /**
     * 创建作品内容。
     * HTTP 映射：<code>@PostMapping("/api/admin/portfolio/works/{workId}/sections")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'portfolio:edit\')")</code>。
     * 主要协作调用：<code>content.create</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkSectionDTO</code>
     * @return 类型为 <code>ApiResponse&lt;WorkSectionVO&gt;</code> 的处理结果
     */
    @PostMapping("/api/admin/portfolio/works/{workId}/sections")
    @PreAuthorize("hasAuthority('portfolio:edit')")
    public ApiResponse<WorkSectionVO> create(@PathVariable Long workId, @RequestBody WorkSectionDTO request) {
        return ApiResponse.ok(content.create(workId, request));
    }
    /**
     * 更新作品内容。
     * HTTP 映射：<code>@PutMapping("/api/admin/portfolio/works/{workId}/sections/{sectionId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'portfolio:edit\')")</code>。
     * 主要协作调用：<code>content.update</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param sectionId 区块标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkSectionDTO</code>
     * @return 类型为 <code>ApiResponse&lt;WorkSectionVO&gt;</code> 的处理结果
     */
    @PutMapping("/api/admin/portfolio/works/{workId}/sections/{sectionId}")
    @PreAuthorize("hasAuthority('portfolio:edit')")
    public ApiResponse<WorkSectionVO> update(@PathVariable Long workId, @PathVariable Long sectionId, @RequestBody WorkSectionDTO request) {
        return ApiResponse.ok(content.update(workId, sectionId, request));
    }
    /**
     * 删除作品内容。
     * HTTP 映射：<code>@DeleteMapping("/api/admin/portfolio/works/{workId}/sections/{sectionId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'portfolio:edit\')")</code>。
     * 主要协作调用：<code>content.delete</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param sectionId 区块标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/api/admin/portfolio/works/{workId}/sections/{sectionId}")
    @PreAuthorize("hasAuthority('portfolio:edit')")
    public ApiResponse<Void> delete(@PathVariable Long workId, @PathVariable Long sectionId) {
        content.delete(workId, sectionId); return ApiResponse.ok(null);
    }
    /**
     * 顺序处理入口。
     * HTTP 映射：<code>@PutMapping("/api/admin/portfolio/works/{workId}/sections/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'portfolio:edit\')")</code>。
     * 主要协作调用：<code>content.reorder</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PutMapping("/api/admin/portfolio/works/{workId}/sections/order")
    @PreAuthorize("hasAuthority('portfolio:edit')")
    public ApiResponse<Void> order(@PathVariable Long workId, @RequestBody IdOrderDTO request) {
        content.reorder(workId, request.getIds()); return ApiResponse.ok(null);
    }
}
