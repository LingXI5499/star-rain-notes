/*
 * TutorialPublicController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、PageResult、TutorialPublicationService、TutorialPublicReadService。
 * 阅读入口：categories（categories处理入口）；tutorials（tutorials处理入口）；tutorial（教程处理入口）；chapter（章节处理入口）。
 */

package com.starrainnotes.tutorial.content.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import com.starrainnotes.tutorial.content.service.TutorialPublicReadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教程公开控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class TutorialPublicController {
    /**
     * 发布业务服务。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    private final TutorialPublicationService publicationService;
    /**
     * 阅读。
     * 协作类型：<code>TutorialPublicReadService</code>。
     */
    private final TutorialPublicReadService reading;

    /**
     * categories处理入口。
     * HTTP 映射：<code>@GetMapping("/tutorial-categories/tree")</code>。
     * 主要协作调用：<code>publicationService.publicCategories</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;JsonNode&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/tutorial-categories/tree")
    public ApiResponse<List<JsonNode>> categories() {
        return ApiResponse.ok(publicationService.publicCategories());
    }

    /**
     * tutorials处理入口。
     * HTTP 映射：<code>@GetMapping("/tutorials")</code>。
     * 主要协作调用：<code>publicationService.publicTutorials</code>。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;JsonNode&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/tutorials")
    public ApiResponse<PageResult<JsonNode>> tutorials(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(publicationService.publicTutorials(categoryId, search, page, pageSize));
    }

    /**
     * 教程处理入口。
     * HTTP 映射：<code>@GetMapping("/tutorials/{slug}")</code>。
     * 主要协作调用：<code>reading.tutorial</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;JsonNode&gt;</code> 的处理结果
     */
    @GetMapping("/tutorials/{slug}")
    public ApiResponse<JsonNode> tutorial(@PathVariable String slug) {
        return ApiResponse.ok(reading.tutorial(slug));
    }

    /**
     * 章节处理入口。
     * HTTP 映射：<code>@GetMapping("/tutorials/{slug}/chapters/{chapterSlug}")</code>。
     * 主要协作调用：<code>reading.chapter</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param chapterSlug 章节固定链接标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;JsonNode&gt;</code> 的处理结果
     */
    @GetMapping("/tutorials/{slug}/chapters/{chapterSlug}")
    public ApiResponse<JsonNode> chapter(@PathVariable String slug, @PathVariable String chapterSlug) {
        return ApiResponse.ok(reading.chapter(slug, chapterSlug));
    }

}
