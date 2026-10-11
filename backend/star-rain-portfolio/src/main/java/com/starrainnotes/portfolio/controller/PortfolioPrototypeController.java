/*
 * PortfolioPrototypeController：作品模块的HTTP 接入文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、WorkPrototypeDTO、PortfolioPrototypeService。
 * 阅读入口：bind（绑定作品Prototype）；read（读取作品Prototype）。
 */

package com.starrainnotes.portfolio.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.portfolio.dto.WorkPrototypeDTO;
import com.starrainnotes.portfolio.service.PortfolioPrototypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 作品Prototype控制器，属于作品模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
public class PortfolioPrototypeController {
    /**
     * prototypes。
     * 协作类型：<code>PortfolioPrototypeService</code>。
     */
    private final PortfolioPrototypeService prototypes;
    /**
     * 绑定作品Prototype。
     * HTTP 映射：<code>@PutMapping("/api/admin/portfolio/works/{workId}/prototype")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'portfolio:edit\')")</code>。
     * 主要协作调用：<code>prototypes.bind</code>。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkPrototypeDTO</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PutMapping("/api/admin/portfolio/works/{workId}/prototype")
    @PreAuthorize("hasAuthority('portfolio:edit')")
    public ApiResponse<Void> bind(@PathVariable Long workId, @RequestBody WorkPrototypeDTO request) {
        prototypes.bind(workId, request); return ApiResponse.ok(null);
    }
    /**
     * 读取作品Prototype。
     * HTTP 映射：<code>@GetMapping("/api/public/portfolio/works/{slug}/live/{*path}")</code>。
     * 主要协作调用：<code>prototypes.read</code>、<code>prototypes.contentType</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param path 路径；类型为 <code>String</code>
     * @return 类型为 <code>ResponseEntity&lt;byte[]&gt;</code> 的处理结果
     */
    @GetMapping("/api/public/portfolio/works/{slug}/live/{*path}")
    public ResponseEntity<byte[]> read(@PathVariable String slug, @PathVariable String path) {
        String relative = path.startsWith("/") ? path.substring(1) : path;
        byte[] bytes = prototypes.read(slug, relative);
        return ResponseEntity.ok().header("Content-Type", prototypes.contentType(relative))
                .header("X-Content-Type-Options", "nosniff").header("Cache-Control", "no-store")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Frame-Options", "SAMEORIGIN")
                .header("Access-Control-Allow-Origin", "*")
                .header("Content-Security-Policy", "sandbox allow-scripts; default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'none'; frame-src 'none'; base-uri 'none'; form-action 'none'")
                .body(bytes);
    }
}
