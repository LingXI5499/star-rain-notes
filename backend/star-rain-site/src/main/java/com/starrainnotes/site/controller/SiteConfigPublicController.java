/*
 * SiteConfigPublicController：站点模块的HTTP 接入文件。
 * 业务范围：站点品牌、首页内容和站点设置。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：SitePublicConfigVO、ApiResponse、SitePublicApi。
 * 阅读入口：config（配置处理入口）。
 */

package com.starrainnotes.site.controller;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.site.api.SitePublicApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点配置公开控制器，属于站点模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/site")
public class SiteConfigPublicController {
    /**
     * 站点。
     * 协作类型：<code>SitePublicApi</code>。
     */
    private final SitePublicApi site;

    /**
     * 配置处理入口。
     * HTTP 映射：<code>@GetMapping("/config")</code>。
     * 主要协作调用：<code>site.config</code>。
     * @return 类型为 <code>ApiResponse&lt;SitePublicConfigVO&gt;</code> 的处理结果
     */
    @GetMapping("/config")
    public ApiResponse<SitePublicConfigVO> config() { return ApiResponse.ok(site.config()); }
}
