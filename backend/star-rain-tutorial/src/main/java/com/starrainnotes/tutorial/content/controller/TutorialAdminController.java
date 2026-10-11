/*
 * TutorialAdminController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、TutorialOperationService、PageResult、TutorialPermissions、GroupNameDTO、IdOrderDTO、TutorialCreateDTO。
 * 阅读入口：tutorials（tutorials处理入口）；orderItems（顺序条目集合处理入口）；create（创建教程后台）；tutorial（教程处理入口）。
 */

package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.dto.GroupNameDTO;
import com.starrainnotes.tutorial.content.dto.IdOrderDTO;
import com.starrainnotes.tutorial.content.dto.TutorialCreateDTO;
import com.starrainnotes.tutorial.content.dto.TutorialUpdateDTO;
import com.starrainnotes.tutorial.content.service.TutorialContentService;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import com.starrainnotes.tutorial.content.vo.TutorialCurriculumVO;
import com.starrainnotes.tutorial.content.vo.TutorialGroupVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教程后台控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tutorials")
public class TutorialAdminController {
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
     * tutorials处理入口。
     * HTTP 映射：<code>@GetMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>contentService.tutorials</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;TutorialAdminVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<PageResult<TutorialAdminVO>> tutorials(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(contentService.tutorials(categoryId, keyword, page, pageSize));
    }

    /**
     * 顺序条目集合处理入口。
     * HTTP 映射：<code>@GetMapping("/order-items")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>contentService.tutorialOrder</code>。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;java.util.List&lt;com.starrainnotes.tutorial.content.vo.TutorialOrderItemVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/order-items")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<java.util.List<com.starrainnotes.tutorial.content.vo.TutorialOrderItemVO>> orderItems(@RequestParam Long categoryId) {
        return ApiResponse.ok(contentService.tutorialOrder(categoryId));
    }

    /**
     * 创建教程后台。
     * HTTP 映射：<code>@PostMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>TutorialCreateDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> create(@Valid @RequestBody TutorialCreateDTO request) {
        return ApiResponse.ok(operations.request("createTutorial", new Object[]{request}));
    }

    /**
     * 教程处理入口。
     * HTTP 映射：<code>@GetMapping("/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>contentService.tutorial</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;TutorialAdminVO&gt;</code> 的处理结果
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<TutorialAdminVO> tutorial(@PathVariable Long id) {
        return ApiResponse.ok(contentService.tutorial(id));
    }

    /**
     * 更新教程后台。
     * HTTP 映射：<code>@PatchMapping("/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>TutorialUpdateDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> update(@PathVariable Long id,
                                                @Valid @RequestBody TutorialUpdateDTO request) {
        return ApiResponse.ok(operations.request("updateTutorial", new Object[]{id, request}));
    }

    /**
     * 删除教程后台。
     * HTTP 映射：<code>@DeleteMapping("/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        return ApiResponse.ok(operations.request("deleteTutorial", new Object[]{id}));
    }

    /**
     * curriculum处理入口。
     * HTTP 映射：<code>@GetMapping("/{id}/curriculum")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>contentService.curriculum</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;TutorialCurriculumVO&gt;</code> 的处理结果
     */
    @GetMapping("/{id}/curriculum")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<TutorialCurriculumVO> curriculum(@PathVariable Long id) {
        return ApiResponse.ok(contentService.curriculum(id));
    }

    /**
     * 创建分组。
     * HTTP 映射：<code>@PostMapping("/{id}/groups")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>GroupNameDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping("/{id}/groups")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> createGroup(@PathVariable Long id,
                                                     @Valid @RequestBody GroupNameDTO request) {
        return ApiResponse.ok(operations.request("createGroup", new Object[]{id, request.getTitle()}));
    }

    /**
     * 调整顺序Groups。
     * HTTP 映射：<code>@PutMapping("/{id}/groups/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PutMapping("/{id}/groups/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorderGroups(@PathVariable Long id,
                                            @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderGroups", new Object[]{id, request.getIds()}));
    }
}
