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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account/english/vocabulary")
public class VocabularyLearningController {
    private final CurrentActorApi actors;
    private final VocabularyLearningService learning;
    private long accountId() { return actors.current().getAccountId(); }

    @GetMapping("/learning/words")
    public ApiResponse<VocabularyDto.Page<VocabularyDto.Word>> words(@Valid @ModelAttribute Filter filter,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "24") int size) {
        return ApiResponse.ok(learning.words(accountId(), filter, page, size));
    }
    @GetMapping("/learning/states")
    public ApiResponse<List<State>> states(@RequestParam List<Long> wordIds) { return ApiResponse.ok(learning.states(accountId(), wordIds)); }
    @PostMapping("/plan/preview")
    public ApiResponse<Preview> preview(@Valid @RequestBody Selection selection,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "24") int size) {
        return ApiResponse.ok(learning.preview(accountId(), selection, page, size));
    }
    @PutMapping("/plan")
    public ApiResponse<Plan> confirm(@Valid @RequestBody Selection selection) { return ApiResponse.ok(learning.confirm(accountId(), selection)); }
    @GetMapping("/plan")
    public ApiResponse<Plan> plan() { return ApiResponse.ok(learning.plan(accountId())); }
    @GetMapping("/plan/items")
    public ApiResponse<List<PlanItem>> items(@RequestParam long revision, @RequestParam(required = false) Integer groupNo,
            @RequestParam(defaultValue = "0") int after, @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(learning.items(accountId(), revision, groupNo, after, limit));
    }
    @GetMapping("/plan/groups")
    public ApiResponse<List<GroupProgress>> groups(@RequestParam long revision, @RequestParam(defaultValue = "0") int after,
            @RequestParam(defaultValue = "50") int limit) { return ApiResponse.ok(learning.groups(accountId(), revision, after, limit)); }
    @PatchMapping("/plan/batch-size")
    public ApiResponse<Plan> resize(@Valid @RequestBody RevisionRequest request) { return ApiResponse.ok(learning.resize(accountId(), request)); }
    @DeleteMapping("/plan")
    public ApiResponse<Plan> cancel(@RequestParam long expectedRevision) { return ApiResponse.ok(learning.cancel(accountId(), expectedRevision)); }
    @PostMapping("/plan/items/{wordId}/skip")
    public ApiResponse<Plan> skip(@PathVariable long wordId, @Valid @RequestBody RevisionRequest request) {
        return ApiResponse.ok(learning.skipMissing(accountId(), wordId, request.getExpectedRevision()));
    }
    @GetMapping("/review-summary")
    public ApiResponse<ReviewSummary> summary() { return ApiResponse.ok(learning.reviewSummary(accountId())); }
}
