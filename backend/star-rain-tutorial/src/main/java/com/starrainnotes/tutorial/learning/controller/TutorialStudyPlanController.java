/*
 * TutorialStudyPlanController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、StudyPlanDTO、StudyPlanService、StudyPlanVO、StudyTaskVO。
 * 阅读入口：create（创建教程学习计划）；list（列出教程学习计划）；detail（读取详情教程学习计划）；update（更新教程学习计划）。
 */

package com.starrainnotes.tutorial.learning.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.learning.dto.StudyPlanDTO;
import com.starrainnotes.tutorial.learning.service.StudyPlanService;
import com.starrainnotes.tutorial.learning.vo.StudyPlanVO;
import com.starrainnotes.tutorial.learning.vo.StudyTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Legacy controller retired; the evidence controller owns the public API.
/**
 * 教程学习计划控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RequiredArgsConstructor
@RequestMapping("/api/account/learning")
@PreAuthorize("isAuthenticated()")
public class TutorialStudyPlanController {
    /**
     * plans。
     * 协作类型：<code>StudyPlanService</code>。
     */
    private final StudyPlanService plans;

    /**
     * 创建教程学习计划。
     * HTTP 映射：<code>@PostMapping("/plans")</code>。
     * 主要协作调用：<code>plans.create</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>StudyPlanDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;StudyPlanVO&gt;</code> 的处理结果
     */
    @PostMapping("/plans")
    public ApiResponse<StudyPlanVO> create(@Valid @RequestBody StudyPlanDTO request) {
        return ApiResponse.ok(plans.create(request));
    }

    /**
     * 列出教程学习计划。
     * HTTP 映射：<code>@GetMapping("/plans")</code>。
     * 主要协作调用：<code>plans.plans</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;StudyPlanVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/plans")
    public ApiResponse<List<StudyPlanVO>> list() {
        return ApiResponse.ok(plans.plans());
    }

    /**
     * 读取详情教程学习计划。
     * HTTP 映射：<code>@GetMapping("/plans/{planId}")</code>。
     * 主要协作调用：<code>plans.plan</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;StudyPlanVO&gt;</code> 的处理结果
     */
    @GetMapping("/plans/{planId}")
    public ApiResponse<StudyPlanVO> detail(@PathVariable Long planId) {
        return ApiResponse.ok(plans.plan(planId));
    }

    /**
     * 更新教程学习计划。
     * HTTP 映射：<code>@PatchMapping("/plans/{planId}")</code>。
     * 主要协作调用：<code>plans.update</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>StudyPlanDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;StudyPlanVO&gt;</code> 的处理结果
     */
    @PatchMapping("/plans/{planId}")
    public ApiResponse<StudyPlanVO> update(@PathVariable Long planId, @Valid @RequestBody StudyPlanDTO request) {
        return ApiResponse.ok(plans.update(planId, request));
    }

    /**
     * transition处理入口。
     * HTTP 映射：<code>@PostMapping("/plans/{planId}/{action:activate|pause|resume|finish|cancel}")</code>。
     * 主要协作调用：<code>plans.transition</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;StudyPlanVO&gt;</code> 的处理结果
     */
    @PostMapping("/plans/{planId}/{action:activate|pause|resume|finish|cancel}")
    public ApiResponse<StudyPlanVO> transition(@PathVariable Long planId, @PathVariable String action) {
        return ApiResponse.ok(plans.transition(planId, action));
    }

    /**
     * 计划Tasks处理入口。
     * HTTP 映射：<code>@GetMapping("/plans/{planId}/tasks")</code>。
     * 主要协作调用：<code>plans.planTasks</code>。
     *
     * @param planId 计划标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;StudyTaskVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/plans/{planId}/tasks")
    public ApiResponse<List<StudyTaskVO>> planTasks(@PathVariable Long planId) {
        return ApiResponse.ok(plans.planTasks(planId));
    }

    /**
     * today处理入口。
     * HTTP 映射：<code>@GetMapping("/tasks/today")</code>。
     * 主要协作调用：<code>plans.today</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;StudyTaskVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/tasks/today")
    public ApiResponse<List<StudyTaskVO>> today() {
        return ApiResponse.ok(plans.today());
    }

    /**
     * 开始处理入口。
     * HTTP 映射：<code>@PostMapping("/tasks/{taskId}/start")</code>。
     * 主要协作调用：<code>plans.start</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;StudyTaskVO&gt;</code> 的处理结果
     */
    @PostMapping("/tasks/{taskId}/start")
    public ApiResponse<StudyTaskVO> start(@PathVariable Long taskId) {
        return ApiResponse.ok(plans.start(taskId));
    }

    /**
     * 完成教程学习计划。
     * HTTP 映射：<code>@PostMapping("/tasks/{taskId}/complete")</code>。
     * 主要协作调用：<code>plans.complete</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;StudyTaskVO&gt;</code> 的处理结果
     */
    @PostMapping("/tasks/{taskId}/complete")
    public ApiResponse<StudyTaskVO> complete(@PathVariable Long taskId) {
        return ApiResponse.ok(plans.complete(taskId));
    }

    /**
     * skip处理入口。
     * HTTP 映射：<code>@PostMapping("/tasks/{taskId}/skip")</code>。
     * 主要协作调用：<code>plans.skip</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;StudyTaskVO&gt;</code> 的处理结果
     */
    @PostMapping("/tasks/{taskId}/skip")
    public ApiResponse<StudyTaskVO> skip(@PathVariable Long taskId) {
        return ApiResponse.ok(plans.skip(taskId));
    }
}
