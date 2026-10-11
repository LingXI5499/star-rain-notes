/*
 * TaxonomyAdminController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、Node、TaxonomyService。
 * 阅读入口：tree（tree处理入口）；create（创建Taxonomy后台）；update（更新Taxonomy后台）；disable（禁用Taxonomy后台）。
 */

package com.starrainnotes.english.taxonomy.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 * Taxonomy后台控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin/english/taxonomy")
@PreAuthorize("hasAuthority('english:content-edit')")
public class TaxonomyAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>TaxonomyService</code>。
     */
    private final TaxonomyService service;
    /**
     * tree处理入口。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>service.tree</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;Node&gt;&gt;</code> 的处理结果
     */
    @GetMapping public ApiResponse<List<Node>> tree() { return ApiResponse.ok(service.tree(true)); }
    /**
     * 创建Taxonomy后台。
     * HTTP 映射：<code>@PostMapping</code>。
     * 主要协作调用：<code>service.create</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>Node</code>
     * @return 类型为 <code>ApiResponse&lt;Node&gt;</code> 的处理结果
     */
    @PostMapping public ApiResponse<Node> create(@RequestBody Node request) { return ApiResponse.ok(service.create(request)); }
    /**
     * 更新Taxonomy后台。
     * HTTP 映射：<code>@PutMapping("/{id}")</code>。
     * 主要协作调用：<code>service.update</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Node</code>
     * @return 类型为 <code>ApiResponse&lt;Node&gt;</code> 的处理结果
     */
    @PutMapping("/{id}") public ApiResponse<Node> update(@PathVariable long id,@RequestBody Node request) { return ApiResponse.ok(service.update(id,request)); }
    /**
     * 禁用Taxonomy后台。
     * HTTP 映射：<code>@PostMapping("/{id}/disable")</code>。
     * 主要协作调用：<code>service.disable</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/disable") public ApiResponse<Void> disable(@PathVariable long id) { service.disable(id); return ApiResponse.ok(null); }
}
