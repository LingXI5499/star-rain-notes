/*
 * VocabularyLearningController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：CurrentActorApi、ApiResponse、VocabularyDto、VocabularyLearningService。
 * 阅读入口：words（words处理入口）；states（states处理入口）；preview（预览词汇学习）；confirm（确认词汇学习）。
 */

package com.starrainnotes.english.vocabulary.controller;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.*;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 词汇学习控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account/english/vocabulary")
public class VocabularyLearningController {
    /**
     * actors。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi actors;
    /**
     * 学习。
     * 协作类型：<code>VocabularyLearningService</code>。
     */
    private final VocabularyLearningService learning;
    /**
     * 账号标识处理入口。
     * 主要协作调用：<code>actors.current</code>。
     * @return 类型为 <code>long</code> 的处理结果
     */
    private long accountId() { return actors.current().getAccountId(); }

    /**
     * words处理入口。
     * HTTP 映射：<code>@GetMapping("/learning/words")</code>。
     * 主要协作调用：<code>learning.words</code>。
     *
     * @param filter 筛选；类型为 <code>Filter</code>；嵌套字段继续执行校验
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;VocabularyDto.Page&lt;VocabularyDto.Word&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/learning/words")
    public ApiResponse<VocabularyDto.Page<VocabularyDto.Word>> words(@Valid @ModelAttribute Filter filter,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "24") int size) {
        return ApiResponse.ok(learning.words(accountId(), filter, page, size));
    }
    /**
     * states处理入口。
     * HTTP 映射：<code>@GetMapping("/learning/states")</code>。
     * 主要协作调用：<code>learning.states</code>。
     *
     * @param wordIds 单词标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;State&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/learning/states")
    public ApiResponse<List<State>> states(@RequestParam List<Long> wordIds) { return ApiResponse.ok(learning.states(accountId(), wordIds)); }
    /**
     * 预览词汇学习。
     * HTTP 映射：<code>@PostMapping("/plan/preview")</code>。
     * 主要协作调用：<code>learning.preview</code>。
     *
     * @param selection selection；类型为 <code>Selection</code>；嵌套字段继续执行校验
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;Preview&gt;</code> 的处理结果
     */
    @PostMapping("/plan/preview")
    public ApiResponse<Preview> preview(@Valid @RequestBody Selection selection,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "24") int size) {
        return ApiResponse.ok(learning.preview(accountId(), selection, page, size));
    }
    /**
     * 确认词汇学习。
     * HTTP 映射：<code>@PutMapping("/plan")</code>。
     * 主要协作调用：<code>learning.confirm</code>。
     *
     * @param selection selection；类型为 <code>Selection</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Plan&gt;</code> 的处理结果
     */
    @PutMapping("/plan")
    public ApiResponse<Plan> confirm(@Valid @RequestBody Selection selection) { return ApiResponse.ok(learning.confirm(accountId(), selection)); }
    /**
     * 计划处理入口。
     * HTTP 映射：<code>@GetMapping("/plan")</code>。
     * 主要协作调用：<code>learning.plan</code>。
     * @return 类型为 <code>ApiResponse&lt;Plan&gt;</code> 的处理结果
     */
    @GetMapping("/plan")
    public ApiResponse<Plan> plan() { return ApiResponse.ok(learning.plan(accountId())); }
    /**
     * 条目集合处理入口。
     * HTTP 映射：<code>@GetMapping("/plan/items")</code>。
     * 主要协作调用：<code>learning.items</code>。
     *
     * @param revision 修订；类型为 <code>long</code>
     * @param groupNo 分组不含；类型为 <code>Integer</code>
     * @param after 之后；类型为 <code>int</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;PlanItem&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/plan/items")
    public ApiResponse<List<PlanItem>> items(@RequestParam long revision, @RequestParam(required = false) Integer groupNo,
            @RequestParam(defaultValue = "0") int after, @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(learning.items(accountId(), revision, groupNo, after, limit));
    }
    /**
     * groups处理入口。
     * HTTP 映射：<code>@GetMapping("/plan/groups")</code>。
     * 主要协作调用：<code>learning.groups</code>。
     *
     * @param revision 修订；类型为 <code>long</code>
     * @param after 之后；类型为 <code>int</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;GroupProgress&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/plan/groups")
    public ApiResponse<List<GroupProgress>> groups(@RequestParam long revision, @RequestParam(defaultValue = "0") int after,
            @RequestParam(defaultValue = "50") int limit) { return ApiResponse.ok(learning.groups(accountId(), revision, after, limit)); }
    /**
     * resize处理入口。
     * HTTP 映射：<code>@PatchMapping("/plan/batch-size")</code>。
     * 主要协作调用：<code>learning.resize</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>RevisionRequest</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Plan&gt;</code> 的处理结果
     */
    @PatchMapping("/plan/batch-size")
    public ApiResponse<Plan> resize(@Valid @RequestBody RevisionRequest request) { return ApiResponse.ok(learning.resize(accountId(), request)); }
    /**
     * 取消词汇学习。
     * HTTP 映射：<code>@DeleteMapping("/plan")</code>。
     * 主要协作调用：<code>learning.cancel</code>。
     *
     * @param expectedRevision expected修订；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;Plan&gt;</code> 的处理结果
     */
    @DeleteMapping("/plan")
    public ApiResponse<Plan> cancel(@RequestParam long expectedRevision) { return ApiResponse.ok(learning.cancel(accountId(), expectedRevision)); }
    /**
     * skip处理入口。
     * HTTP 映射：<code>@PostMapping("/plan/items/{wordId}/skip")</code>。
     * 主要协作调用：<code>learning.skipMissing</code>。
     *
     * @param wordId 单词标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>RevisionRequest</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Plan&gt;</code> 的处理结果
     */
    @PostMapping("/plan/items/{wordId}/skip")
    public ApiResponse<Plan> skip(@PathVariable long wordId, @Valid @RequestBody RevisionRequest request) {
        return ApiResponse.ok(learning.skipMissing(accountId(), wordId, request.getExpectedRevision()));
    }
    /**
     * 摘要处理入口。
     * HTTP 映射：<code>@GetMapping("/review-summary")</code>。
     * 主要协作调用：<code>learning.reviewSummary</code>。
     * @return 类型为 <code>ApiResponse&lt;ReviewSummary&gt;</code> 的处理结果
     */
    @GetMapping("/review-summary")
    public ApiResponse<ReviewSummary> summary() { return ApiResponse.ok(learning.reviewSummary(accountId())); }
}
