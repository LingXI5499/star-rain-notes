package com.starrainnotes.english.vocabulary.controller;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.vocabulary.dto.VocabularyDisplayRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyLocalProgressRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyStudySettingsRequest;
import com.starrainnotes.english.vocabulary.service.VocabularyProgressImportService;
import com.starrainnotes.english.vocabulary.service.VocabularyStudyCommandService;
import com.starrainnotes.english.vocabulary.service.VocabularyStudyQueryService;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryEntryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyMemoryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyProgressVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyQueueVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyReviewResultVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;
import com.starrainnotes.english.vocabulary.vo.VocabularySummaryVO;
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
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account/english/vocabulary")
public class VocabularyStudyAccountController {

    private final VocabularyStudyQueryService queryService;
    private final VocabularyStudyCommandService commandService;
    private final VocabularyProgressImportService importService;
    private final CurrentActorApi currentActorApi;

    private long accountId() {
        return currentActorApi.current().getAccountId();
    }

    @GetMapping("/settings")
    public ApiResponse<VocabularyStudySettingsVO> settings() {
        return ApiResponse.ok(queryService.settings(accountId()));
    }

    @PutMapping("/settings")
    public ApiResponse<VocabularyStudySettingsVO> updateSettings(
            @Valid @RequestBody VocabularyStudySettingsRequest request) {
        return ApiResponse.ok(commandService.updateSettings(accountId(), request));
    }

    /* 记忆集合（wordId + memoryCount + lastMemoryAt），供「已加入计划」筛选 */
    @GetMapping("/memory")
    public ApiResponse<List<VocabularyMemoryEntryVO>> memory() {
        return ApiResponse.ok(queryService.memorySnapshot(accountId()));
    }

    /* 批量状态；wordId 可重复传参，也兼容 ?wordIds=1,2,3 的单个逗号串 */
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

    @GetMapping("/review-queue")
    public ApiResponse<VocabularyQueueVO> reviewQueue(@RequestParam(required = false) Long themeId) {
        return ApiResponse.ok(queryService.queue(accountId(), themeId));
    }

    @PostMapping("/words/{wordId}/start")
    public ApiResponse<VocabularyMemoryVO> start(@PathVariable long wordId) {
        return ApiResponse.ok(commandService.start(accountId(), wordId));
    }

    @PostMapping("/words/{wordId}/reviews")
    public ApiResponse<VocabularyReviewResultVO> review(@PathVariable long wordId,
                                                       @Valid @RequestBody VocabularyReviewRequest request) {
        return ApiResponse.ok(commandService.completeReview(accountId(), wordId, request));
    }

    @DeleteMapping("/words/{wordId}/progress")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@PathVariable long wordId) {
        commandService.reset(accountId(), wordId);
    }

    @PutMapping("/words/{wordId}/display")
    public ApiResponse<VocabularyMemoryVO> setDisplay(@PathVariable long wordId,
                                                     @Valid @RequestBody VocabularyDisplayRequest request) {
        return ApiResponse.ok(commandService.setDisplay(accountId(), wordId, request));
    }

    @DeleteMapping("/words/{wordId}/display")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDisplay(@PathVariable long wordId) {
        commandService.clearDisplay(accountId(), wordId);
    }

    @GetMapping("/progress")
    public ApiResponse<VocabularyProgressVO> progress() {
        return ApiResponse.ok(queryService.progress(accountId()));
    }

    /* 英语首页四格统计：completed / inProgress / dueForReview / total */
    @GetMapping("/summary")
    public ApiResponse<VocabularySummaryVO> summary() {
        return ApiResponse.ok(queryService.summary(accountId()));
    }

    /* 登录后把浏览器本地进度合并到账号；合并只增不减 */
    @PostMapping("/import-local")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importLocal(@RequestBody VocabularyLocalProgressRequest request) {
        importService.importLocal(accountId(), request);
    }
}
