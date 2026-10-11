/*
 * TutorialLearningReviewController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、MasterySelfRatingDTO、ReviewCompleteDTO、LearningReviewService、MasteryVO、ReviewResultVO、ReviewTaskVO。
 * 阅读入口：today（today处理入口）；task（任务处理入口）；back（back处理入口）；complete（完成教程学习审核）。
 */

package com.starrainnotes.tutorial.learning.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.tutorial.learning.dto.MasterySelfRatingDTO;
import com.starrainnotes.tutorial.learning.dto.ReviewCompleteDTO;
import com.starrainnotes.tutorial.learning.service.LearningReviewService;
import com.starrainnotes.tutorial.learning.vo.MasteryVO;
import com.starrainnotes.tutorial.learning.vo.ReviewResultVO;
import com.starrainnotes.tutorial.learning.vo.ReviewTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Legacy controller retired; the evidence controller owns the public API.
/**
 * 教程学习审核控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RequiredArgsConstructor
@RequestMapping("/api/account/learning")
@PreAuthorize("isAuthenticated()")
public class TutorialLearningReviewController {
    /**
     * reviews。
     * 协作类型：<code>LearningReviewService</code>。
     */
    private final LearningReviewService reviews;

    /**
     * today处理入口。
     * HTTP 映射：<code>@GetMapping("/reviews/today")</code>。
     * 主要协作调用：<code>reviews.today</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;ReviewTaskVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/reviews/today")
    public ApiResponse<List<ReviewTaskVO>> today() {
        return ApiResponse.ok(reviews.today());
    }

    /**
     * 任务处理入口。
     * HTTP 映射：<code>@GetMapping("/reviews/{taskId}")</code>。
     * 主要协作调用：<code>reviews.task</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;ReviewTaskVO&gt;</code> 的处理结果
     */
    @GetMapping("/reviews/{taskId}")
    public ApiResponse<ReviewTaskVO> task(@PathVariable Long taskId) {
        return ApiResponse.ok(reviews.task(taskId));
    }

    /**
     * back处理入口。
     * HTTP 映射：<code>@GetMapping("/reviews/{taskId}/back")</code>。
     * 主要协作调用：<code>reviews.back</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Map&lt;String, String&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/reviews/{taskId}/back")
    public ApiResponse<Map<String, String>> back(@PathVariable Long taskId) {
        return ApiResponse.ok(Map.of("backMarkdown", reviews.back(taskId)));
    }

    /**
     * 完成教程学习审核。
     * HTTP 映射：<code>@PostMapping("/reviews/{taskId}/complete")</code>。
     * 主要协作调用：<code>reviews.complete</code>。
     *
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ReviewCompleteDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;ReviewResultVO&gt;</code> 的处理结果
     */
    @PostMapping("/reviews/{taskId}/complete")
    public ApiResponse<ReviewResultVO> complete(@PathVariable Long taskId,
            @Valid @RequestBody ReviewCompleteDTO request) {
        return ApiResponse.ok(reviews.complete(taskId, request.getRating()));
    }

    /**
     * 掌握处理入口。
     * HTTP 映射：<code>@GetMapping("/mastery/cards")</code>。
     * 主要协作调用：<code>reviews.mastery</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;MasteryVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/mastery/cards")
    public ApiResponse<List<MasteryVO>> mastery() {
        return ApiResponse.ok(reviews.mastery());
    }

    /**
     * self评分处理入口。
     * HTTP 映射：<code>@PutMapping("/mastery/cards/{cardId}/self-rating")</code>。
     * 主要协作调用：<code>reviews.selfRating</code>。
     *
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>MasterySelfRatingDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;MasteryVO&gt;</code> 的处理结果
     */
    @PutMapping("/mastery/cards/{cardId}/self-rating")
    public ApiResponse<MasteryVO> selfRating(@PathVariable Long cardId,
            @Valid @RequestBody MasterySelfRatingDTO request) {
        return ApiResponse.ok(reviews.selfRating(cardId, request.getLevel()));
    }
}
