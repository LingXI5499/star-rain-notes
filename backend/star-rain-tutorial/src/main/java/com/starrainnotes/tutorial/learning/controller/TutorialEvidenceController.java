/*
 * TutorialEvidenceController：教程与学习模块的HTTP 接入文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、PageResult、EvidenceLearningService、EvidenceStudyPlanService。
 * 阅读入口：plans（plans处理入口）；planHistory（计划历史处理入口）；preview（预览教程证据）；create（创建教程证据）。
 */

package com.starrainnotes.tutorial.learning.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.learning.dto.*;
import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import com.starrainnotes.tutorial.learning.service.impl.EvidenceLearningService;
import com.starrainnotes.tutorial.learning.service.impl.EvidenceStudyPlanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 教程证据控制器，属于教程与学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/account/learning")
public class TutorialEvidenceController {
    /**
     * plans。
     * 协作类型：<code>EvidenceStudyPlanService</code>。
     */
    private final EvidenceStudyPlanService plans;
    /**
     * 学习。
     * 协作类型：<code>EvidenceLearningService</code>。
     */
    private final EvidenceLearningService learning;

    /**
     * plans处理入口。
     * HTTP 映射：<code>@GetMapping("/plans")</code>。
     * 主要协作调用：<code>plans.list</code>。
     *
     * @param currentOnly 当前Only；类型为 <code>boolean</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;Plan&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/plans") public ApiResponse<List<Plan>> plans(@RequestParam(defaultValue="false") boolean currentOnly) { return ApiResponse.ok(plans.list(currentOnly)); }
    /**
     * 计划历史处理入口。
     * HTTP 映射：<code>@GetMapping("/plans/history")</code>。
     * 主要协作调用：<code>plans.history</code>。
     *
     * @param query 查询参数；类型为 <code>LearningPageQueryDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;Plan&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/plans/history") public ApiResponse<PageResult<Plan>> planHistory(@Valid @ModelAttribute LearningPageQueryDTO query) { return ApiResponse.ok(plans.history(query)); }
    /**
     * 预览教程证据。
     * HTTP 映射：<code>@PostMapping("/plans/preview")</code>。
     * 主要协作调用：<code>plans.preview</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>EvidencePlanDTO</code>；嵌套字段继续执行校验
     * @param excludePlanId exclude计划标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Plan&gt;</code> 的处理结果
     */
    @PostMapping("/plans/preview") public ApiResponse<Plan> preview(@Valid @RequestBody EvidencePlanDTO request,@RequestParam(required=false) Long excludePlanId) { return ApiResponse.ok(plans.preview(request,excludePlanId)); }
    /**
     * 创建教程证据。
     * HTTP 映射：<code>@PostMapping("/plans")</code>。
     * 主要协作调用：<code>plans.create</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>EvidencePlanDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Plan&gt;</code> 的处理结果
     */
    @PostMapping("/plans") public ApiResponse<Plan> create(@Valid @RequestBody EvidencePlanDTO request) { return ApiResponse.ok(plans.create(request)); }
    /**
     * 计划处理入口。
     * HTTP 映射：<code>@GetMapping("/plans/{id}")</code>。
     * 主要协作调用：<code>plans.get</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Plan&gt;</code> 的处理结果
     */
    @GetMapping("/plans/{id}") public ApiResponse<Plan> plan(@PathVariable Long id) { return ApiResponse.ok(plans.get(id)); }
    /**
     * 更新教程证据。
     * HTTP 映射：<code>@PatchMapping("/plans/{id}")</code>。
     * 主要协作调用：<code>plans.update</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>EvidencePlanDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Plan&gt;</code> 的处理结果
     */
    @PatchMapping("/plans/{id}") public ApiResponse<Plan> update(@PathVariable Long id,@Valid @RequestBody EvidencePlanDTO request) { return ApiResponse.ok(plans.update(id,request)); }
    /**
     * transition处理入口。
     * HTTP 映射：<code>@PostMapping("/plans/{id}/{action:activate|pause|resume|cancel|restart}")</code>。
     * 主要协作调用：<code>plans.transition</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;Plan&gt;</code> 的处理结果
     */
    @PostMapping("/plans/{id}/{action:activate|pause|resume|cancel|restart}") public ApiResponse<Plan> transition(@PathVariable Long id,@PathVariable String action) { return ApiResponse.ok(plans.transition(id,action)); }
    /**
     * tasks处理入口。
     * HTTP 映射：<code>@GetMapping("/plans/{id}/tasks")</code>。
     * 主要协作调用：<code>plans.tasks</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;Task&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/plans/{id}/tasks") public ApiResponse<List<Task>> tasks(@PathVariable Long id) { return ApiResponse.ok(plans.tasks(id)); }
    /**
     * 任务处理入口。
     * HTTP 映射：<code>@GetMapping("/tasks/{id}")</code>。
     * 主要协作调用：<code>plans.task</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Task&gt;</code> 的处理结果
     */
    @GetMapping("/tasks/{id}") public ApiResponse<Task> task(@PathVariable Long id) { return ApiResponse.ok(plans.task(id)); }
    /**
     * 任务章节列表处理入口。
     * HTTP 映射：<code>@GetMapping("/tasks/{id}/chapters")</code>。
     * 主要协作调用：<code>plans.task</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;Chapter&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/tasks/{id}/chapters") public ApiResponse<List<Chapter>> taskChapters(@PathVariable Long id) { return ApiResponse.ok(plans.task(id).getChapters()); }
    /**
     * 开始处理入口。
     * HTTP 映射：<code>@PostMapping("/tasks/{id}/start")</code>。
     * 主要协作调用：<code>plans.start</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Task&gt;</code> 的处理结果
     */
    @PostMapping("/tasks/{id}/start") public ApiResponse<Task> start(@PathVariable Long id) { return ApiResponse.ok(plans.start(id)); }
    /**
     * initial处理入口。
     * HTTP 映射：<code>@PostMapping("/chapters/{id}/sessions")</code>。
     * 主要协作调用：<code>learning.initial</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param taskId 任务标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Session&gt;</code> 的处理结果
     */
    @PostMapping("/chapters/{id}/sessions") public ApiResponse<Session> initial(@PathVariable Long id,@RequestParam(required=false) Long taskId) { return ApiResponse.ok(learning.initial(id,taskId)); }
    /**
     * 学习状态处理入口。
     * HTTP 映射：<code>@GetMapping("/chapters/{id}/study-state")</code>。
     * 主要协作调用：<code>learning.studyState</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param planId 计划标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;StudyState&gt;</code> 的处理结果
     */
    @GetMapping("/chapters/{id}/study-state") public ApiResponse<StudyState> studyState(@PathVariable Long id,@RequestParam(required=false) Long planId) { return ApiResponse.ok(learning.studyState(id,planId)); }
    /**
     * 当前业务使用的会话对象处理入口。
     * HTTP 映射：<code>@GetMapping("/sessions/{id}")</code>。
     * 主要协作调用：<code>learning.session</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Session&gt;</code> 的处理结果
     */
    @GetMapping("/sessions/{id}") public ApiResponse<Session> session(@PathVariable Long id) { return ApiResponse.ok(learning.session(id)); }
    /**
     * reveal处理入口。
     * HTTP 映射：<code>@PostMapping("/sessions/{id}/cards/{cardId}/reveal")</code>。
     * 主要协作调用：<code>learning.reveal</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Session&gt;</code> 的处理结果
     */
    @PostMapping("/sessions/{id}/cards/{cardId}/reveal") public ApiResponse<Session> reveal(@PathVariable Long id,@PathVariable Long cardId) { return ApiResponse.ok(learning.reveal(id,cardId,false)); }
    /**
     * rate处理入口。
     * HTTP 映射：<code>@PostMapping("/sessions/{id}/cards/{cardId}/rate")</code>。
     * 主要协作调用：<code>learning.rate</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>EvidenceRatingDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Session&gt;</code> 的处理结果
     */
    @PostMapping("/sessions/{id}/cards/{cardId}/rate") public ApiResponse<Session> rate(@PathVariable Long id,@PathVariable Long cardId,@Valid @RequestBody EvidenceRatingDTO request) { return ApiResponse.ok(learning.rate(id,cardId,request,false)); }
    /**
     * 答案处理入口。
     * HTTP 映射：<code>@GetMapping("/questions/{id}/answer")</code>。
     * 主要协作调用：<code>learning.answer</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Answer&gt;</code> 的处理结果
     */
    @GetMapping("/questions/{id}/answer") public ApiResponse<Answer> answer(@PathVariable Long id) { return ApiResponse.ok(learning.answer(id)); }
    /**
     * 保存教程证据。
     * HTTP 映射：<code>@PostMapping("/questions/{id}/versions")</code>。
     * 主要协作调用：<code>learning.saveVersion</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>AnswerVersionDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Answer&gt;</code> 的处理结果
     */
    @PostMapping("/questions/{id}/versions") public ApiResponse<Answer> save(@PathVariable Long id,@Valid @RequestBody AnswerVersionDTO request) { return ApiResponse.ok(learning.saveVersion(id,request)); }
    /**
     * versions处理入口。
     * HTTP 映射：<code>@GetMapping("/questions/{id}/versions")</code>。
     * 主要协作调用：<code>learning.versions</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;AnswerVersion&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/questions/{id}/versions") public ApiResponse<List<AnswerVersion>> versions(@PathVariable Long id) { return ApiResponse.ok(learning.versions(id)); }
    /**
     * unlock处理入口。
     * HTTP 映射：<code>@PostMapping("/questions/{id}/unlock-reference")</code>。
     * 主要协作调用：<code>learning.reference</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Map&lt;String, Boolean&gt;&gt;</code> 的处理结果
     */
    @PostMapping("/questions/{id}/unlock-reference") public ApiResponse<Map<String,Boolean>> unlock(@PathVariable Long id) { learning.reference(id);return ApiResponse.ok(Map.of("unlocked",true)); }
    /**
     * 引用处理入口。
     * HTTP 映射：<code>@GetMapping("/questions/{id}/reference-answer")</code>。
     * 主要协作调用：<code>learning.reference</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Map&lt;String, String&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/questions/{id}/reference-answer") public ApiResponse<Map<String,String>> reference(@PathVariable Long id) { return ApiResponse.ok(Map.of("referenceAnswer",learning.reference(id))); }
    /**
     * 掌握处理入口。
     * HTTP 映射：<code>@GetMapping("/mastery")</code>。
     * 主要协作调用：<code>learning.mastery</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;CardMastery&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/mastery") public ApiResponse<List<CardMastery>> mastery() { return ApiResponse.ok(learning.mastery(null,null)); }
    /**
     * 掌握分页处理入口。
     * HTTP 映射：<code>@GetMapping("/mastery/page")</code>。
     * 主要协作调用：<code>learning.masteryPage</code>。
     *
     * @param query 查询参数；类型为 <code>LearningPageQueryDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;CardMastery&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/mastery/page") public ApiResponse<PageResult<CardMastery>> masteryPage(@Valid @ModelAttribute LearningPageQueryDTO query) { return ApiResponse.ok(learning.masteryPage(query)); }
    /**
     * 掌握Options处理入口。
     * HTTP 映射：<code>@GetMapping("/mastery/options")</code>。
     * 主要协作调用：<code>learning.masteryOptions</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;TutorialOption&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/mastery/options") public ApiResponse<List<TutorialOption>> masteryOptions() { return ApiResponse.ok(learning.masteryOptions()); }
    /**
     * 教程掌握处理入口。
     * HTTP 映射：<code>@GetMapping("/mastery/tutorials/{id}")</code>。
     * 主要协作调用：<code>learning.mastery</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;CardMastery&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/mastery/tutorials/{id}") public ApiResponse<List<CardMastery>> tutorialMastery(@PathVariable Long id) { return ApiResponse.ok(learning.mastery(id,null)); }
    /**
     * 章节掌握处理入口。
     * HTTP 映射：<code>@GetMapping("/mastery/chapters/{id}")</code>。
     * 主要协作调用：<code>learning.mastery</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;CardMastery&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/mastery/chapters/{id}") public ApiResponse<List<CardMastery>> chapterMastery(@PathVariable Long id) { return ApiResponse.ok(learning.mastery(null,id)); }
    /**
     * 摘要处理入口。
     * HTTP 映射：<code>@GetMapping("/reviews/summary")</code>。
     * 主要协作调用：<code>learning.reviewSummary</code>。
     * @return 类型为 <code>ApiResponse&lt;ReviewSummary&gt;</code> 的处理结果
     */
    @GetMapping("/reviews/summary") public ApiResponse<ReviewSummary> summary() { return ApiResponse.ok(learning.reviewSummary()); }
    /**
     * 审核处理入口。
     * HTTP 映射：<code>@PostMapping("/reviews/sessions")</code>。
     * 主要协作调用：<code>learning.review</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ReviewSessionDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Session&gt;</code> 的处理结果
     */
    @PostMapping("/reviews/sessions") public ApiResponse<Session> review(@Valid @RequestBody ReviewSessionDTO request) { return ApiResponse.ok(learning.review(request)); }
    /**
     * 当前处理入口。
     * HTTP 映射：<code>@GetMapping("/reviews/sessions/current")</code>。
     * 主要协作调用：<code>learning.currentReview</code>。
     * @return 类型为 <code>ApiResponse&lt;Session&gt;</code> 的处理结果
     */
    @GetMapping("/reviews/sessions/current") public ApiResponse<Session> current() { return ApiResponse.ok(learning.currentReview()); }
    /**
     * 审核会话处理入口。
     * HTTP 映射：<code>@GetMapping("/reviews/sessions/{id}")</code>。
     * 主要协作调用：<code>learning.reviewSession</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Session&gt;</code> 的处理结果
     */
    @GetMapping("/reviews/sessions/{id}") public ApiResponse<Session> reviewSession(@PathVariable Long id) { return ApiResponse.ok(learning.reviewSession(id)); }
    /**
     * 审核Reveal处理入口。
     * HTTP 映射：<code>@PostMapping("/reviews/sessions/{id}/cards/{cardId}/reveal")</code>。
     * 主要协作调用：<code>learning.reveal</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Session&gt;</code> 的处理结果
     */
    @PostMapping("/reviews/sessions/{id}/cards/{cardId}/reveal") public ApiResponse<Session> reviewReveal(@PathVariable Long id,@PathVariable Long cardId) { return ApiResponse.ok(learning.reveal(id,cardId,true)); }
    /**
     * 审核Rate处理入口。
     * HTTP 映射：<code>@PostMapping("/reviews/sessions/{id}/cards/{cardId}/rate")</code>。
     * 主要协作调用：<code>learning.rate</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>EvidenceRatingDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Session&gt;</code> 的处理结果
     */
    @PostMapping("/reviews/sessions/{id}/cards/{cardId}/rate") public ApiResponse<Session> reviewRate(@PathVariable Long id,@PathVariable Long cardId,@Valid @RequestBody EvidenceRatingDTO request) { return ApiResponse.ok(learning.rate(id,cardId,request,true)); }
    /**
     * abandon处理入口。
     * HTTP 映射：<code>@PostMapping("/reviews/sessions/{id}/abandon")</code>。
     * 主要协作调用：<code>learning.abandon</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Session&gt;</code> 的处理结果
     */
    @PostMapping("/reviews/sessions/{id}/abandon") public ApiResponse<Session> abandon(@PathVariable Long id) { return ApiResponse.ok(learning.abandon(id)); }
    /**
     * 历史处理入口。
     * HTTP 映射：<code>@GetMapping("/history")</code>。
     * 主要协作调用：<code>learning.history</code>。
     *
     * @param query 查询参数；类型为 <code>LearningPageQueryDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;Session&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/history") public ApiResponse<PageResult<Session>> history(@Valid @ModelAttribute LearningPageQueryDTO query) { return ApiResponse.ok(learning.history(query)); }
    /**
     * 历史Options处理入口。
     * HTTP 映射：<code>@GetMapping("/history/options")</code>。
     * 主要协作调用：<code>learning.historyOptions</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;TutorialOption&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/history/options") public ApiResponse<List<TutorialOption>> historyOptions() { return ApiResponse.ok(learning.historyOptions()); }
    /**
     * statistics处理入口。
     * HTTP 映射：<code>@GetMapping("/statistics")</code>。
     * 主要协作调用：<code>learning.statistics</code>。
     * @return 类型为 <code>ApiResponse&lt;Statistics&gt;</code> 的处理结果
     */
    @GetMapping("/statistics") public ApiResponse<Statistics> statistics() { return ApiResponse.ok(learning.statistics()); }
}
