/*
 * TutorialLearningDashboardController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、PageResult、LearningDashboardService、LearningHistoryVO、LearningStatisticsVO。
 * 阅读入口：history（历史处理入口）；statistics（statistics处理入口）。
 */

package com.starrainnotes.tutorial.learning.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.learning.service.LearningDashboardService;
import com.starrainnotes.tutorial.learning.vo.LearningHistoryVO;
import com.starrainnotes.tutorial.learning.vo.LearningStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Legacy controller retired; the evidence controller owns the public API.
/**
 * 教程学习概览控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RequiredArgsConstructor
@RequestMapping("/api/account/learning")
@PreAuthorize("isAuthenticated()")
public class TutorialLearningDashboardController {
    /**
     * 概览。
     * 协作类型：<code>LearningDashboardService</code>。
     */
    private final LearningDashboardService dashboard;

    /**
     * 历史处理入口。
     * HTTP 映射：<code>@GetMapping("/history")</code>。
     * 主要协作调用：<code>dashboard.history</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;LearningHistoryVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/history")
    public ApiResponse<PageResult<LearningHistoryVO>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(dashboard.history(page, pageSize));
    }

    /**
     * statistics处理入口。
     * HTTP 映射：<code>@GetMapping("/statistics")</code>。
     * 主要协作调用：<code>dashboard.statistics</code>。
     * @return 类型为 <code>ApiResponse&lt;LearningStatisticsVO&gt;</code> 的处理结果
     */
    @GetMapping("/statistics")
    public ApiResponse<LearningStatisticsVO> statistics() {
        return ApiResponse.ok(dashboard.statistics());
    }
}
