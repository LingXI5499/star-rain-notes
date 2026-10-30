/*
 * GrammarAdminController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、GrammarDto、GrammarService。
 * 阅读入口：curriculum（curriculum处理入口）；lesson（lesson处理入口）；updateCourse（更新Course）；courseStatus（course状态处理入口）。
 */

package com.starrainnotes.english.grammar.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.grammar.dto.GrammarDto;
import com.starrainnotes.english.grammar.service.GrammarService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 语法后台控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/english/grammar")
@PreAuthorize("hasAuthority('english:content-read-admin')")
public class GrammarAdminController {
    /**
     * 业务服务。
     * 协作类型：<code>GrammarService</code>。
     */
    private final GrammarService service;

    @PutMapping("/sections/order")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> reorderSections(@RequestBody GrammarDto.OrderRequest request) {
        service.reorderSections(request == null ? null : request.ids());
        return ApiResponse.ok(null);
    }

    @PutMapping("/sections/{sectionId}/lessons/order")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> reorderLessons(@PathVariable String sectionId, @RequestBody GrammarDto.OrderRequest request) {
        service.reorderLessons(sectionId, request == null ? null : request.ids());
        return ApiResponse.ok(null);
    }

    /**
     * curriculum处理入口。
     * HTTP 映射：<code>@GetMapping</code>。
     * 主要协作调用：<code>service.curriculum</code>。
     * @return 类型为 <code>ApiResponse&lt;GrammarDto.Curriculum&gt;</code> 的处理结果
     */
    @GetMapping
    public ApiResponse<GrammarDto.Curriculum> curriculum() {
        return ApiResponse.ok(service.curriculum(true));
    }

    /**
     * lesson处理入口。
     * HTTP 映射：<code>@GetMapping("/lessons/{lessonId}")</code>。
     * 主要协作调用：<code>service.lesson</code>。
     *
     * @param lessonId lesson标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;GrammarDto.Lesson&gt;</code> 的处理结果
     */
    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<GrammarDto.Lesson> lesson(@PathVariable String lessonId) {
        return ApiResponse.ok(service.lesson(lessonId, true));
    }

    /**
     * 更新Course。
     * HTTP 映射：<code>@PutMapping</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.updateCourse</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>GrammarDto.CourseRequest</code>
     * @return 类型为 <code>ApiResponse&lt;GrammarDto.Course&gt;</code> 的处理结果
     */
    @PutMapping
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Course> updateCourse(@RequestBody GrammarDto.CourseRequest request) {
        return ApiResponse.ok(service.updateCourse(request));
    }

    /**
     * course状态处理入口。
     * HTTP 映射：<code>@PostMapping("/{action:publish|withdraw}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.setCoursePublished</code>。
     *
     * @param action 动作；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{action:publish|withdraw}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> courseStatus(@PathVariable String action) {
        service.setCoursePublished("publish".equals(action));
        return ApiResponse.ok(null);
    }

    /**
     * 创建区块。
     * HTTP 映射：<code>@PostMapping("/sections")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.createSection</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>GrammarDto.SectionRequest</code>
     * @return 类型为 <code>ApiResponse&lt;GrammarDto.Section&gt;</code> 的处理结果
     */
    @PostMapping("/sections")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Section> createSection(@RequestBody GrammarDto.SectionRequest request) {
        return ApiResponse.ok(service.createSection(request));
    }

    /**
     * 更新区块。
     * HTTP 映射：<code>@PutMapping("/sections/{sectionId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.updateSection</code>。
     *
     * @param sectionId 区块标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>GrammarDto.SectionRequest</code>
     * @return 类型为 <code>ApiResponse&lt;GrammarDto.Section&gt;</code> 的处理结果
     */
    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Section> updateSection(@PathVariable String sectionId,
                                                              @RequestBody GrammarDto.SectionRequest request) {
        return ApiResponse.ok(service.updateSection(sectionId, request));
    }

    /**
     * 删除区块。
     * HTTP 映射：<code>@DeleteMapping("/sections/{sectionId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.deleteSection</code>。
     *
     * @param sectionId 区块标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/sections/{sectionId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> deleteSection(@PathVariable String sectionId) {
        service.deleteSection(sectionId);
        return ApiResponse.ok(null);
    }

    /**
     * 创建Lesson。
     * HTTP 映射：<code>@PostMapping("/lessons")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.createLesson</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>GrammarDto.LessonRequest</code>
     * @return 类型为 <code>ApiResponse&lt;GrammarDto.Lesson&gt;</code> 的处理结果
     */
    @PostMapping("/lessons")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Lesson> createLesson(@RequestBody GrammarDto.LessonRequest request) {
        return ApiResponse.ok(service.createLesson(request));
    }

    /**
     * 更新Lesson。
     * HTTP 映射：<code>@PutMapping("/lessons/{lessonId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.updateLesson</code>。
     *
     * @param lessonId lesson标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>GrammarDto.LessonRequest</code>
     * @return 类型为 <code>ApiResponse&lt;GrammarDto.Lesson&gt;</code> 的处理结果
     */
    @PutMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Lesson> updateLesson(@PathVariable String lessonId,
                                                            @RequestBody GrammarDto.LessonRequest request) {
        return ApiResponse.ok(service.updateLesson(lessonId, request));
    }

    /**
     * lesson状态处理入口。
     * HTTP 映射：<code>@PostMapping("/lessons/{lessonId}/{action:publish|withdraw}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.setLessonPublished</code>。
     *
     * @param lessonId lesson标识；类型为 <code>String</code>
     * @param action 动作；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;GrammarDto.Lesson&gt;</code> 的处理结果
     */
    @PostMapping("/lessons/{lessonId}/{action:publish|withdraw}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<GrammarDto.Lesson> lessonStatus(@PathVariable String lessonId,
                                                            @PathVariable String action) {
        return ApiResponse.ok(service.setLessonPublished(lessonId, "publish".equals(action)));
    }

    /**
     * 删除Lesson。
     * HTTP 映射：<code>@DeleteMapping("/lessons/{lessonId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'english:content-edit\')")</code>。
     * 主要协作调用：<code>service.deleteLesson</code>。
     *
     * @param lessonId lesson标识；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @DeleteMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Void> deleteLesson(@PathVariable String lessonId) {
        service.deleteLesson(lessonId);
        return ApiResponse.ok(null);
    }
}
