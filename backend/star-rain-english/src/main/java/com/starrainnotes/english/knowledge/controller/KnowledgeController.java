/*
 * KnowledgeController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、Page、KnowledgeService。
 * 阅读入口：list（列出知识）。
 */

package com.starrainnotes.english.knowledge.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.knowledge.dto.KnowledgeDto.Page;
import com.starrainnotes.english.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
/**
 * 知识控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController @RequiredArgsConstructor @RequestMapping("/api/public/english/knowledge/articles")
public class KnowledgeController {
    /**
     * 业务服务。
     * 协作类型：<code>KnowledgeService</code>。
     */
    private final KnowledgeService service;
    /**
     * 列出知识。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>service.list</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param genreId genre标识；类型为 <code>Long</code>
     * @param purposeId purpose标识；类型为 <code>Long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;Page&gt;</code> 的处理结果
     */
    @GetMapping public ApiResponse<Page> list(@RequestParam(required=false) String type,@RequestParam(required=false) String search,@RequestParam(required=false) Long topicId,@RequestParam(required=false) Long genreId,@RequestParam(required=false) Long purposeId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int size) { return ApiResponse.ok(service.list(type,search,topicId,genreId,purposeId,page,size)); }
}
