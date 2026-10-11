/*
 * GrammarPublicController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、GrammarDto、GrammarService。
 * 阅读入口：curriculum（curriculum处理入口）；lesson（lesson处理入口）。
 */

package com.starrainnotes.english.grammar.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.grammar.dto.GrammarDto;
import com.starrainnotes.english.grammar.service.GrammarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 语法公开控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/english/grammar")
public class GrammarPublicController {
    /**
     * 业务服务。
     * 协作类型：<code>GrammarService</code>。
     */
    private final GrammarService service;

    /**
     * curriculum处理入口。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>service.curriculum</code>。
     * @return 类型为 <code>ApiResponse&lt;GrammarDto.Curriculum&gt;</code> 的处理结果
     */
    @GetMapping
    public ApiResponse<GrammarDto.Curriculum> curriculum() {
        return ApiResponse.ok(service.curriculum(false));
    }

    /**
     * lesson处理入口。
     * HTTP 映射：<code>@GetMapping("/lessons/{slug}")</code>。
     * 主要协作调用：<code>service.lesson</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;GrammarDto.Lesson&gt;</code> 的处理结果
     */
    @GetMapping("/lessons/{slug}")
    public ApiResponse<GrammarDto.Lesson> lesson(@PathVariable String slug) {
        return ApiResponse.ok(service.lesson(slug, false));
    }
}
