/*
 * VocabularyStudyAccountController：英语学习模块的HTTP 接入文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：CurrentActorApi、ApiResponse、VocabularyDisplayRequestDTO、VocabularyLocalProgressRequestDTO、VocabularyStudySettingsRequestDTO、VocabularyProgressImportService、VocabularyStudyCommandService。
 * 阅读入口：settings（settings处理入口）；updateSettings（更新Settings）；memory（记忆处理入口）；states（states处理入口）。
 */

package com.starrainnotes.english.vocabulary.controller;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.vocabulary.dto.VocabularyDisplayRequestDTO;
import com.starrainnotes.english.vocabulary.dto.VocabularyLocalProgressRequestDTO;
import com.starrainnotes.english.vocabulary.dto.VocabularyStudySettingsRequestDTO;
import com.starrainnotes.english.vocabulary.service.VocabularyProgressImportService;
import com.starrainnotes.english.vocabulary.service.VocabularyStudyCommandService;
import com.starrainnotes.english.vocabulary.service.VocabularyStudyQueryService;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryEntryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyProgressVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;
import com.starrainnotes.english.vocabulary.vo.VocabularySummaryVO;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningService;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels;
import com.starrainnotes.common.exception.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/*
 * 词汇记忆体系的账户接口。
 *
 * 账户一律取自当前登录主体（CurrentActorApi），不接受客户端传入的 accountId——
 * 否则任何人都能读写别人的记忆进度。未登录时 CurrentActorApi.current() 抛 UNAUTHORIZED，
 * 由全局异常处理器转成 401，前端据此回退到浏览器本地进度。
 * URL 落在 /api/account/** 下，已由 Account 模块的认证边界覆盖。
 */
/**
 * 词汇学习账号控制器，属于英语学习模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account/english/vocabulary")
public class VocabularyStudyAccountController {

    /**
     * 查询参数业务服务。
     * 协作类型：<code>VocabularyStudyQueryService</code>。
     */
    private final VocabularyStudyQueryService queryService;
    /**
     * command业务服务。
     * 协作类型：<code>VocabularyStudyCommandService</code>。
     */
    private final VocabularyStudyCommandService commandService;
    /**
     * import业务服务。
     * 协作类型：<code>VocabularyProgressImportService</code>。
     */
    private final VocabularyProgressImportService importService;
    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;
    /**
     * 学习业务服务。
     * 协作类型：<code>VocabularyLearningService</code>。
     */
    private final VocabularyLearningService learningService;

    /**
     * 账号标识处理入口。
     * 主要协作调用：<code>currentActorApi.current</code>。
     * @return 类型为 <code>long</code> 的处理结果
     */
    private long accountId() {
        return currentActorApi.current().getAccountId();
    }

    /**
     * settings处理入口。
     * HTTP 映射：<code>@GetMapping("/settings")</code>。
     * 主要协作调用：<code>queryService.settings</code>。
     * @return 类型为 <code>ApiResponse&lt;VocabularyStudySettingsVO&gt;</code> 的处理结果
     */
    @GetMapping("/settings")
    public ApiResponse<VocabularyStudySettingsVO> settings() {
        return ApiResponse.ok(queryService.settings(accountId()));
    }

    /**
     * 更新Settings。
     * HTTP 映射：<code>@PutMapping("/settings")</code>。
     * 主要协作调用：<code>commandService.updateSettings</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyStudySettingsRequestDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;VocabularyStudySettingsVO&gt;</code> 的处理结果
     */
    @PutMapping("/settings")
    public ApiResponse<VocabularyStudySettingsVO> updateSettings(
            @Valid @RequestBody VocabularyStudySettingsRequestDTO request) {
        return ApiResponse.ok(commandService.updateSettings(accountId(), request));
    }

    /* 记忆集合（wordId + memoryCount + lastMemoryAt），供「已加入计划」筛选 */
    /**
     * 记忆处理入口。
     * HTTP 映射：<code>@GetMapping("/memory")</code>。
     * 主要协作调用：<code>queryService.memorySnapshot</code>。
     * @return 类型为 <code>ApiResponse&lt;List&lt;VocabularyMemoryEntryVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/memory")
    public ApiResponse<List<VocabularyMemoryEntryVO>> memory() {
        return ApiResponse.ok(queryService.memorySnapshot(accountId()));
    }

    /* 批量状态；wordId 可重复传参，也兼容 ?wordIds=1,2,3 的单个逗号串 */
    /**
     * states处理入口。
     * HTTP 映射：<code>@GetMapping("/states")</code>。
     * 主要协作调用：<code>queryService.memories</code>。
     *
     * @param wordIds 单词标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param wordIdsParam 单词标识集合Param；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;List&lt;VocabularyMemoryVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/states")
    public ApiResponse<List<VocabularyMemoryVO>> states(
            @RequestParam(value = "wordId", required = false) List<Long> wordIds,
            @RequestParam(value = "wordIds", required = false) String wordIdsParam) {
        List<Long> ids = wordIds == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(wordIds);
        if (wordIdsParam != null && !wordIdsParam.isBlank()) {
            for (String token : wordIdsParam.split(",")) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    ids.add(Long.valueOf(trimmed));
                } catch (NumberFormatException ignored) {
                    /* 单个坏编号不该让整次状态查询失败，跳过即可 */
                }
            }
        }
        return ApiResponse.ok(queryService.memories(accountId(), ids));
    }

    /**
     * 审核Queue处理入口。
     * HTTP 映射：<code>@GetMapping("/review-queue")</code>。
     * 主要协作调用：<code>learningService.queue</code>。
     *
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @param cursor cursor；类型为 <code>String</code>
     * @return 类型为 <code>ApiResponse&lt;VocabularyLearningModels.Queue&gt;</code> 的处理结果
     */
    @GetMapping("/review-queue")
    public ApiResponse<VocabularyLearningModels.Queue> reviewQueue(@RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        return ApiResponse.ok(learningService.queue(accountId(), limit, cursor));
    }

    /**
     * 开始处理入口。
     * HTTP 映射：<code>@PostMapping("/words/{wordId}/start")</code>。
     * 主要协作调用：<code>commandService.start</code>。
     *
     * @param wordId 单词标识；类型为 <code>long</code>
     * @return 类型为 <code>ApiResponse&lt;VocabularyMemoryVO&gt;</code> 的处理结果
     */
    @PostMapping("/words/{wordId}/start")
    public ApiResponse<VocabularyMemoryVO> start(@PathVariable long wordId) {
        return ApiResponse.ok(commandService.start(accountId(), wordId));
    }

    /**
     * 审核处理入口。
     * HTTP 映射：<code>@PostMapping("/words/{wordId}/reviews")</code>。
     * 主要协作调用：<code>learningService.rate</code>。
     *
     * @param wordId 单词标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyLearningModels.RatingRequest</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;VocabularyLearningModels.RatingResult&gt;</code> 的处理结果
     */
    @PostMapping("/words/{wordId}/reviews")
    public ApiResponse<VocabularyLearningModels.RatingResult> review(@PathVariable long wordId,
            @Valid @RequestBody VocabularyLearningModels.RatingRequest request) {
        return ApiResponse.ok(learningService.rate(accountId(), wordId, request));
    }

    /**
     * 重置词汇学习账号。
     * HTTP 映射：<code>@DeleteMapping("/words/{wordId}/progress")</code>。
     * 关键处理步骤调用 <code>accountId</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param wordId 单词标识；类型为 <code>long</code>
     */
    @DeleteMapping("/words/{wordId}/progress")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@PathVariable long wordId) {
        accountId();
        throw new ApiException("ENGLISH_VOCABULARY_LEARNING_CONFLICT", "记忆档案永久保留；重新练习请创建学习计划", 409);
    }

    /**
     * 设置显示。
     * HTTP 映射：<code>@PutMapping("/words/{wordId}/display")</code>。
     * 主要协作调用：<code>commandService.setDisplay</code>。
     *
     * @param wordId 单词标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyDisplayRequestDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;VocabularyMemoryVO&gt;</code> 的处理结果
     */
    @PutMapping("/words/{wordId}/display")
    public ApiResponse<VocabularyMemoryVO> setDisplay(@PathVariable long wordId,
                                                     @Valid @RequestBody VocabularyDisplayRequestDTO request) {
        return ApiResponse.ok(commandService.setDisplay(accountId(), wordId, request));
    }

    /**
     * 清理显示。
     * HTTP 映射：<code>@DeleteMapping("/words/{wordId}/display")</code>。
     * 主要协作调用：<code>commandService.clearDisplay</code>。
     *
     * @param wordId 单词标识；类型为 <code>long</code>
     */
    @DeleteMapping("/words/{wordId}/display")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDisplay(@PathVariable long wordId) {
        commandService.clearDisplay(accountId(), wordId);
    }

    /**
     * 进度处理入口。
     * HTTP 映射：<code>@GetMapping("/progress")</code>。
     * 主要协作调用：<code>queryService.progress</code>。
     * @return 类型为 <code>ApiResponse&lt;VocabularyProgressVO&gt;</code> 的处理结果
     */
    @GetMapping("/progress")
    public ApiResponse<VocabularyProgressVO> progress() {
        return ApiResponse.ok(queryService.progress(accountId()));
    }

    /* 英语首页四格统计：completed / inProgress / dueForReview / total */
    /**
     * 摘要处理入口。
     * HTTP 映射：<code>@GetMapping("/summary")</code>。
     * 主要协作调用：<code>queryService.summary</code>。
     * @return 类型为 <code>ApiResponse&lt;VocabularySummaryVO&gt;</code> 的处理结果
     */
    @GetMapping("/summary")
    public ApiResponse<VocabularySummaryVO> summary() {
        return ApiResponse.ok(queryService.summary(accountId()));
    }

    /* 登录后把浏览器本地进度合并到账号；合并只增不减 */
    /**
     * importLocal处理入口。
     * HTTP 映射：<code>@PostMapping("/import-local")</code>。
     * 主要协作调用：<code>importService.importLocal</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>VocabularyLocalProgressRequestDTO</code>
     */
    @PostMapping("/import-local")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importLocal(@RequestBody VocabularyLocalProgressRequestDTO request) {
        importService.importLocal(accountId(), request);
    }
}
