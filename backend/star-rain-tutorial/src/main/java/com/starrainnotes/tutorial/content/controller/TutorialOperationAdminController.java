/*
 * TutorialOperationAdminController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、TutorialOperationService。
 * 阅读入口：pending（待处理处理入口）。
 */

package com.starrainnotes.tutorial.content.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教程操作后台控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
public class TutorialOperationAdminController {
    /**
     * operations。
     * 协作类型：<code>TutorialOperationService</code>。
     */
    private final TutorialOperationService operations;

    /**
     * 待处理处理入口。
     * HTTP 映射：<code>@GetMapping("/api/admin/tutorial-operations/pending")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'tutorial:read-admin\')")</code>。
     * 主要协作调用：<code>operations.pendingOperations</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;Map&lt;String, Object&gt;&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/api/admin/tutorial-operations/pending")
    @PreAuthorize("hasAuthority('tutorial:read-admin')")
    public ApiResponse<List<Map<String, Object>>> pending() {
        return ApiResponse.ok(operations.pendingOperations());
    }
}
