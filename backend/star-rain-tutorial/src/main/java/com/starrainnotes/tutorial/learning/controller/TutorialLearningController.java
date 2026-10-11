/*
 * TutorialLearningController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、LearningProgressDTO、LearningProgressService、LearningProgressVO、LearningTutorialProgressVO。
 * 阅读入口：saveProgress（保存进度）；completeChapter（完成章节）；chapterProgress（章节进度处理入口）；tutorialProgress（教程进度处理入口）。
 */

package com.starrainnotes.tutorial.learning.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.learning.dto.LearningProgressDTO;
import com.starrainnotes.tutorial.learning.service.LearningProgressService;
import com.starrainnotes.tutorial.learning.vo.LearningProgressVO;
import com.starrainnotes.tutorial.learning.vo.LearningTutorialProgressVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教程学习控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account/learning")
@PreAuthorize("isAuthenticated()")
public class TutorialLearningController {
    /**
     * 进度业务服务。
     * 协作类型：<code>LearningProgressService</code>。
     */
    private final LearningProgressService progressService;

    /**
     * 保存进度。
     * HTTP 映射：<code>@PutMapping("/progress/chapters/{chapterId}")</code>。
     * 主要协作调用：<code>progressService.save</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>LearningProgressDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;LearningProgressVO&gt;</code> 的处理结果
     */
    @PutMapping("/progress/chapters/{chapterId}")
    public ApiResponse<LearningProgressVO> saveProgress(@PathVariable Long chapterId,
            @Valid @RequestBody LearningProgressDTO request) {
        return ApiResponse.ok(progressService.save(chapterId, request));
    }

    /**
     * 完成章节。
     * HTTP 映射：<code>@PostMapping("/progress/chapters/{chapterId}/complete")</code>。
     * 主要协作调用：<code>progressService.complete</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;LearningProgressVO&gt;</code> 的处理结果
     */
    @PostMapping("/progress/chapters/{chapterId}/complete")
    public ApiResponse<LearningProgressVO> completeChapter(@PathVariable Long chapterId) {
        return ApiResponse.ok(progressService.complete(chapterId));
    }

    /**
     * 章节进度处理入口。
     * HTTP 映射：<code>@GetMapping("/progress/chapters/{chapterId}")</code>。
     * 主要协作调用：<code>progressService.chapter</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;LearningProgressVO&gt;</code> 的处理结果
     */
    @GetMapping("/progress/chapters/{chapterId}")
    public ApiResponse<LearningProgressVO> chapterProgress(@PathVariable Long chapterId) {
        return ApiResponse.ok(progressService.chapter(chapterId));
    }

    /**
     * 教程进度处理入口。
     * HTTP 映射：<code>@GetMapping("/progress/tutorials/{tutorialId}")</code>。
     * 主要协作调用：<code>progressService.tutorial</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;LearningTutorialProgressVO&gt;</code> 的处理结果
     */
    @GetMapping("/progress/tutorials/{tutorialId}")
    public ApiResponse<LearningTutorialProgressVO> tutorialProgress(@PathVariable Long tutorialId) {
        return ApiResponse.ok(progressService.tutorial(tutorialId));
    }

    /**
     * recent处理入口。
     * HTTP 映射：<code>@GetMapping("/recent")</code>。
     * 主要协作调用：<code>progressService.recent</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;LearningProgressVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/recent")
    public ApiResponse<List<LearningProgressVO>> recent() {
        return ApiResponse.ok(progressService.recent());
    }

}
