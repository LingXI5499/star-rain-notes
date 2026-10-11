/*
 * SiteConfigAdminController：站点模块的HTTP 接入文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：SiteConfigPatchDTO、SiteMediaRequestDTO、SiteConfigService、SitePublicConfigVO、ApiResponse。
 * 阅读入口：patch（patch处理入口）；setMedia（设置媒体）；clearMedia（清理媒体）。
 */

package com.starrainnotes.site.controller;

import com.starrainnotes.site.dto.SiteConfigPatchDTO;
import com.starrainnotes.site.dto.SiteMediaRequestDTO;
import com.starrainnotes.site.service.SiteConfigService;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;

import com.starrainnotes.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点配置后台控制器，属于站点模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/site")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('site:config-manage')")
public class SiteConfigAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>SiteConfigService</code>。
     */
    private final SiteConfigService service;

    /**
     * patch处理入口。
     * HTTP 映射：<code>@PatchMapping("/config")</code>。
     * 主要协作调用：<code>service.patch</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>SiteConfigPatchDTO</code>
     * @return 类型为 <code>ApiResponse&lt;SitePublicConfigVO&gt;</code> 的处理结果
     */
    @PatchMapping("/config")
    public ApiResponse<SitePublicConfigVO> patch(@RequestBody SiteConfigPatchDTO request) {
        return ApiResponse.ok(service.patch(request));
    }

    /**
     * 设置媒体。
     * HTTP 映射：<code>@PutMapping("/{kind:logo|favicon}")</code>。
     * 主要协作调用：<code>service.setMediaRequired</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>SiteMediaRequestDTO</code>
     * @return 类型为 <code>ApiResponse&lt;SitePublicConfigVO&gt;</code> 的处理结果
     */
    @PutMapping("/{kind:logo|favicon}")
    public ApiResponse<SitePublicConfigVO> setMedia(@PathVariable String kind, @RequestBody SiteMediaRequestDTO request) {
        return ApiResponse.ok(service.setMediaRequired(kind, request));
    }

    /**
     * 清理媒体。
     * HTTP 映射：<code>@DeleteMapping("/{kind:logo|favicon}")</code>。
     * 主要协作调用：<code>service.setMedia</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;SitePublicConfigVO&gt;</code> 的处理结果
     */
    @DeleteMapping("/{kind:logo|favicon}")
    public ApiResponse<SitePublicConfigVO> clearMedia(@PathVariable String kind) {
        return ApiResponse.ok(service.setMedia(kind, null));
    }
}
