/*
 * TutorialCategoryAdminController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、TutorialOperationService、TutorialPermissions、CategoryNameDTO、IdOrderDTO、TutorialContentService、TutorialCategoryVO。
 * 阅读入口：categories（categories处理入口）；create（创建教程分类后台）；update（更新教程分类后台）；delete（删除教程分类后台）。
 */

package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import com.starrainnotes.tutorial.content.constant.TutorialPermissions;
import com.starrainnotes.tutorial.content.dto.CategoryNameDTO;
import com.starrainnotes.tutorial.content.dto.IdOrderDTO;
import com.starrainnotes.tutorial.content.service.TutorialContentService;
import com.starrainnotes.tutorial.content.vo.TutorialCategoryVO;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 教程分类后台控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tutorial-categories")
public class TutorialCategoryAdminController {
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
     * categories处理入口。
     * HTTP 映射：<code>@GetMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.READ_ADMIN + "\')")</code>。
     * 主要协作调用：<code>contentService.categories</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;TutorialCategoryVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping
    @PreAuthorize("hasAuthority('" + TutorialPermissions.READ_ADMIN + "')")
    public ApiResponse<List<TutorialCategoryVO>> categories() {
        return ApiResponse.ok(contentService.categories());
    }

    /**
     * 创建教程分类后台。
     * HTTP 映射：<code>@PostMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>CategoryNameDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PostMapping
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> create(@Valid @RequestBody CategoryNameDTO request) {
        return ApiResponse.ok(operations.request("createCategory", new Object[]{request.getName()}));
    }

    /**
     * 更新教程分类后台。
     * HTTP 映射：<code>@PatchMapping("/{id}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>CategoryNameDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> update(@PathVariable Long id,
                                                   @Valid @RequestBody CategoryNameDTO request) {
        return ApiResponse.ok(operations.request("updateCategory", new Object[]{id, request.getName()}));
    }

    /**
     * 删除教程分类后台。
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
        return ApiResponse.ok(operations.request("deleteCategory", new Object[]{id}));
    }

    /**
     * 调整顺序教程分类后台。
     * HTTP 映射：<code>@PutMapping("/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PutMapping("/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorder(@Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderCategories", new Object[]{request.getIds()}));
    }

    /**
     * 调整顺序Tutorials。
     * HTTP 映射：<code>@PutMapping("/{categoryId}/tutorials/order")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'" + TutorialPermissions.EDIT + "\')")</code>。
     * 主要协作调用：<code>operations.request</code>。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>IdOrderDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Object&gt;</code> 的处理结果
     */
    @PutMapping("/{categoryId}/tutorials/order")
    @PreAuthorize("hasAuthority('" + TutorialPermissions.EDIT + "')")
    public ApiResponse<Object> reorderTutorials(@PathVariable Long categoryId,
                                               @Valid @RequestBody IdOrderDTO request) {
        return ApiResponse.ok(operations.request("reorderTutorials", new Object[]{categoryId, request.getIds()}));
    }
}
