/*
 * TaxonomyPublicController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、Node、TaxonomyService。
 * 阅读入口：tree（tree处理入口）；get（读取Taxonomy公开）。
 */

package com.starrainnotes.english.taxonomy.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
/**
 * Taxonomy公开控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController @RequiredArgsConstructor @RequestMapping("/api/public/english/taxonomy")
public class TaxonomyPublicController {
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
    @GetMapping public ApiResponse<List<Node>> tree() { return ApiResponse.ok(service.tree(false)); }
    /**
     * 读取Taxonomy公开。
     * HTTP 映射：<code>@GetMapping("/{slug}")</code>。
     * 主要协作调用：<code>service.get</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Node&gt;</code> 的处理结果
     */
    @GetMapping("/{slug}") public ApiResponse<Node> get(@PathVariable String slug) { return ApiResponse.ok(service.get(slug)); }
}
