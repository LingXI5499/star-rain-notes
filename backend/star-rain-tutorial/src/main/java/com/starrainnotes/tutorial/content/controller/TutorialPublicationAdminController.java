/*
 * TutorialPublicationAdminController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、TutorialOperationService、TutorialPermissions、TutorialPublicationService、TutorialAdminVO。
 * 阅读入口：submit（提交教程发布后台）；preview（预览教程发布后台）；publish（发布教程发布后台）；withdraw（撤回教程发布后台）。
 */

package com.starrainnotes.tutorial.content.controller;

import org.springframework.web.bind.annotation.RequestParam;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教程发布后台控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tutorials/{id}")
public class TutorialPublicationAdminController {
    /**
     * operations。
     * 协作类型：<code>TutorialOperationService</code>。
     */
    private final TutorialOperationService operations;
    /**
     * 发布业务服务。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    private final TutorialPublicationService publicationService;

    /**
     * 提交教程发布后台。
     * HTTP 映射：<code>@PostMapping("/submit-review")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.SUBMIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping("/submit-review")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.SUBMIT + "')")
    public ApiResponse<Object> submit(@PathVariable Long id,
            @RequestParam(required = false) Long chapterId) {
        return ApiResponse.ok(operations.request(chapterId == null ? "publish" : "publishChapter",
                new Object[]{chapterId == null ? id : chapterId}));
    }

    /**
     * 预览教程发布后台。
     * HTTP 映射：<code>@GetMapping("/preview")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>publicationService.previewTutorial</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;JsonNode&gt;</code> 的处理结果
     */
    @GetMapping("/preview")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<JsonNode> preview(@PathVariable Long id) {
        return ApiResponse.ok(publicationService.previewTutorial(id));
    }

    /**
     * 发布教程发布后台。
     * HTTP 映射：<code>@PostMapping("/publish")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> publish(@PathVariable Long id) {
        return ApiResponse.ok(operations.request("publish", new Object[]{id}));
    }

    /**
     * 撤回教程发布后台。
     * HTTP 映射：<code>@PostMapping("/withdraw")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping("/withdraw")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> withdraw(@PathVariable Long id) {
        return ApiResponse.ok(operations.request("withdraw", new Object[]{id}));
    }

}
