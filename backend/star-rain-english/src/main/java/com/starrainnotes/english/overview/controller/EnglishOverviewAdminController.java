/*
 * EnglishOverviewAdminController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：EnglishOverviewVO、EnglishOverviewService、EnglishOverviewRequestDTO、ApiResponse。
 * 阅读入口：get（读取英语Overview后台）；update（更新英语Overview后台）。
 */

package com.starrainnotes.english.overview.controller;

import com.starrainnotes.english.overview.vo.EnglishOverviewVO;

import com.starrainnotes.english.overview.service.EnglishOverviewService;

import com.starrainnotes.english.overview.dto.EnglishOverviewRequestDTO;

import com.starrainnotes.common.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 英语Overview后台控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english")
@PreAuthorize("hasAuthority('english:overview-manage')")
public class EnglishOverviewAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>EnglishOverviewService</code>。
     */
    private final EnglishOverviewService service;

    /**
     * 读取英语Overview后台。
     * HTTP 映射：<code>@GetMapping("/overview")</code>。
     * 主要协作调用：<code>service.get</code>。
     * @return 类型为 <code>ApiResponse&lt;EnglishOverviewVO&gt;</code> 的处理结果
     */
    @GetMapping("/overview")
    public ApiResponse<EnglishOverviewVO> get() { return ApiResponse.ok(service.get()); }

    /**
     * 更新英语Overview后台。
     * HTTP 映射：<code>@PutMapping("/overview")</code>。
     * 主要协作调用：<code>service.update</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>EnglishOverviewRequestDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;EnglishOverviewVO&gt;</code> 的处理结果
     */
    @PutMapping("/overview")
    public ApiResponse<EnglishOverviewVO> update(@Valid @RequestBody EnglishOverviewRequestDTO request) {
        return ApiResponse.ok(service.update(request));
    }
}
