/*
 * TutorialStructureAdminController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：TutorialPublicationService、ApiResponse、TutorialOperationService、TutorialPermissions、ChapterBodyDTO、ChapterCreateDTO、ChapterUpdateDTO。
 * 阅读入口：updateGroup（更新分组）；deleteGroup（删除分组）；createChapter（创建章节）；reorderChapters（调整顺序章节列表）。
 */

package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.dto.ChapterBodyDTO;
import com.starrainnotes.tutorial.content.dto.ChapterCreateDTO;
import com.starrainnotes.tutorial.content.dto.ChapterUpdateDTO;
import com.starrainnotes.tutorial.content.dto.GroupNameDTO;
import com.starrainnotes.tutorial.content.dto.IdOrderDTO;
import com.starrainnotes.tutorial.content.service.TutorialContentService;
import com.starrainnotes.tutorial.content.vo.TutorialChapterVO;
import com.starrainnotes.tutorial.content.vo.TutorialGroupVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教程Structure后台控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class TutorialStructureAdminController {
    /**
     * operations。
     * 协作类型：<code>TutorialOperationService</code>。
     */
    private final TutorialOperationService operations;
    /**
     * 内容业务服务。
     * 协作类型：<code>TutorialContentService</code>。
     */
    private final TutorialContentService contentService;
    /**
     * 发布业务服务。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    private final TutorialPublicationService publicationService;

    /**
     * 更新分组。
     * HTTP 映射：<code>@PatchMapping("/tutorial-groups/{groupId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>GroupNameDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PatchMapping("/tutorial-groups/{groupId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> updateGroup(@PathVariable Long groupId,
                                                     @Valid @RequestBody GroupNameDTO request) {
        return ApiResponse.ok(operations.request("updateGroup", new Object[]{groupId, request.getTitle()}));
    }

    /**
     * 删除分组。
     * HTTP 映射：<code>@DeleteMapping("/tutorial-groups/{groupId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @DeleteMapping("/tutorial-groups/{groupId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> deleteGroup(@PathVariable Long groupId) {
        return ApiResponse.ok(operations.request("deleteGroup", new Object[]{groupId}));
    }

    /**
     * 创建章节。
     * HTTP 映射：<code>@PostMapping("/tutorial-groups/{groupId}/chapters")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterCreateDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping("/tutorial-groups/{groupId}/chapters")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> createChapter(@PathVariable Long groupId,
                                                         @Valid @RequestBody ChapterCreateDTO request) {
        return ApiResponse.ok(operations.request("createChapter", new Object[]{groupId, request}));
    }

    /**
     * 调整顺序章节列表。
     * HTTP 映射：<code>@PutMapping("/tutorial-groups/{groupId}/chapters/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PutMapping("/tutorial-groups/{groupId}/chapters/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorderChapters(@PathVariable Long groupId,
                                              @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderChapters", new Object[]{groupId, request.getIds()}));
    }

    /**
     * 章节处理入口。
     * HTTP 映射：<code>@GetMapping("/tutorial-chapters/{chapterId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>contentService.chapter</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;TutorialChapterVO&gt;</code> 的处理结果
     */
    @GetMapping("/tutorial-chapters/{chapterId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<TutorialChapterVO> chapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(contentService.chapter(chapterId));
    }

    /**
     * 更新章节。
     * HTTP 映射：<code>@PatchMapping("/tutorial-chapters/{chapterId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterUpdateDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PatchMapping("/tutorial-chapters/{chapterId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> updateChapter(@PathVariable Long chapterId,
                                                         @Valid @RequestBody ChapterUpdateDTO request) {
        return ApiResponse.ok(operations.request("updateChapter", new Object[]{chapterId, request}));
    }

    /**
     * 更新正文。
     * HTTP 映射：<code>@PutMapping("/tutorial-chapters/{chapterId}/body")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ChapterBodyDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PutMapping("/tutorial-chapters/{chapterId}/body")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> updateBody(@PathVariable Long chapterId,
                                                      @Valid @RequestBody ChapterBodyDTO request) {
        return ApiResponse.ok(operations.request("updateChapterBody", new Object[]{chapterId, request}));
    }

    /**
     * 删除章节。
     * HTTP 映射：<code>@DeleteMapping("/tutorial-chapters/{chapterId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @DeleteMapping("/tutorial-chapters/{chapterId}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> deleteChapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(operations.request("deleteChapter", new Object[]{chapterId}));
    }

    /**
     * 发布章节。
     * HTTP 映射：<code>@PostMapping("/tutorial-chapters/{chapterId}/publish")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping("/tutorial-chapters/{chapterId}/publish")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> publishChapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(operations.request("publishChapter", new Object[]{chapterId}));
    }

    /**
     * 撤回章节。
     * HTTP 映射：<code>@PostMapping("/tutorial-chapters/{chapterId}/withdraw")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping("/tutorial-chapters/{chapterId}/withdraw")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> withdrawChapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(operations.request("withdrawChapter", new Object[]{chapterId}));
    }

}
