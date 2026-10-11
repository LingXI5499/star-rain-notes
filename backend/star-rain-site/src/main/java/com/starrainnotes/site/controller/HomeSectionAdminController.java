/*
 * HomeSectionAdminController：站点模块的HTTP 接入文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：HomeSectionOrderDTO、HomeSectionPatchDTO、HomeSectionService、HomeSectionSettingVO、ApiResponse。
 * 阅读入口：all（全部处理入口）；order（顺序处理入口）；patch（patch处理入口）。
 */

package com.starrainnotes.site.controller;

import com.starrainnotes.site.dto.HomeSectionOrderDTO;
import com.starrainnotes.site.dto.HomeSectionPatchDTO;
import com.starrainnotes.site.service.HomeSectionService;
import com.starrainnotes.site.vo.HomeSectionSettingVO;

import com.starrainnotes.common.result.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页区块后台控制器，属于站点模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/site/home-sections")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('site:section-manage')")
public class HomeSectionAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>HomeSectionService</code>。
     */
    private final HomeSectionService service;

    /**
     * 全部处理入口。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>service.all</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;HomeSectionSettingVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    public ApiResponse<List<HomeSectionSettingVO>> all() { return ApiResponse.ok(service.all()); }

    /**
     * 顺序处理入口。
     * HTTP 映射：<code>@PutMapping("/order")</code>。
     * 主要协作调用：<code>service.reorder</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>HomeSectionOrderDTO</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;HomeSectionSettingVO&gt;&gt;</code> 的处理结果
     */
    @PutMapping("/order")
    public ApiResponse<List<HomeSectionSettingVO>> order(@RequestBody HomeSectionOrderDTO request) {
        return ApiResponse.ok(service.reorder(request));
    }

    /**
     * patch处理入口。
     * HTTP 映射：<code>@PatchMapping("/{sectionCode}")</code>。
     * 主要协作调用：<code>service.patch</code>。
     *
     * @param sectionCode 区块代码；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>HomeSectionPatchDTO</code>
     * @return 类型为 <code>ApiResponse&lt;HomeSectionSettingVO&gt;</code> 的处理结果
     */
    @PatchMapping("/{sectionCode}")
    public ApiResponse<HomeSectionSettingVO> patch(@PathVariable String sectionCode, @RequestBody HomeSectionPatchDTO request) {
        return ApiResponse.ok(service.patch(sectionCode, request));
    }
}
