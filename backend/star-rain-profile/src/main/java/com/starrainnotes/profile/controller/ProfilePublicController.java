/*
 * ProfilePublicController：作者资料模块的HTTP 接入文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、ProfileService、ProfileVO。
 * 阅读入口：get（读取资料公开）。
 */

package com.starrainnotes.profile.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.profile.service.ProfileService;
import com.starrainnotes.profile.api.vo.ProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资料公开控制器，属于作者资料模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/profile")
public class ProfilePublicController {
    /**
     * 业务服务。
     * 协作类型：<code>ProfileService</code>。
     */
    private final ProfileService service;

    /**
     * 读取资料公开。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>service.publicProfile</code>。
     * @return 类型为 <code>ApiResponse&lt;ProfileVO&gt;</code> 的处理结果
     */
    @GetMapping
    public ApiResponse<ProfileVO> get() {
        return ApiResponse.ok(service.publicProfile());
    }
}
