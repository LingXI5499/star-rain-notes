/*
 * TutorialChapterPreviewAdminController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、TutorialPermissions、TutorialPublicationService。
 * 阅读入口：preview（预览教程章节预览后台）。
 */

package com.starrainnotes.tutorial.content.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教程章节预览后台控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tutorial-chapters")
public class TutorialChapterPreviewAdminController {
    /**
     * 发布业务服务。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    private final TutorialPublicationService publicationService;

    /**
     * 预览教程章节预览后台。
     * HTTP 映射：<code>@GetMapping("/{chapterId}/preview")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>publicationService.previewChapter</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;JsonNode&gt;</code> 的处理结果
     */
    @GetMapping("/{chapterId}/preview")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<JsonNode> preview(@PathVariable Long chapterId) {
        return ApiResponse.ok(publicationService.previewChapter(chapterId));
    }
}
