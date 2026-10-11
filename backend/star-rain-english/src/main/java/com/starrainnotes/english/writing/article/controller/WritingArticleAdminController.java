/*
 * WritingArticleAdminController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、Article、Request、WritingArticleService。
 * 阅读入口：publish（发布写作Article后台）。
 */

package com.starrainnotes.english.writing.article.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.Article;
import com.starrainnotes.english.knowledge.dto.RevisionDto.Request;
import com.starrainnotes.english.writing.article.service.WritingArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 * 写作Article后台控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin/english/content/writing-articles")
@PreAuthorize("hasAuthority('english:content-publish')")
public class WritingArticleAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>WritingArticleService</code>。
     */
    private final WritingArticleService service;
    /**
     * 发布写作Article后台。
     * HTTP 映射：<code>@PostMapping("/{id}/{action:publish|withdraw}")</code>。
     * 主要协作调用：<code>service.publish</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param action 动作；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>ApiResponse&lt;Article&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/{action:publish|withdraw}") public ApiResponse<Article> publish(@PathVariable String id,@PathVariable String action,@RequestBody Request request) { return ApiResponse.ok(service.publish(id,request.getRowVersion(),"publish".equals(action))); }
}
