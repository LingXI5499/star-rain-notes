package com.starrainnotes.account.english.controller;

import com.starrainnotes.account.english.AccountEnglishService;
import com.starrainnotes.account.english.vocabulary.VocabularyDisplayRequest;
import com.starrainnotes.account.english.vocabulary.VocabularyMemoryView;
import com.starrainnotes.account.english.vocabulary.VocabularyProgressView;
import com.starrainnotes.account.english.vocabulary.VocabularyQueueView;
import com.starrainnotes.account.english.vocabulary.VocabularyReviewRequest;
import com.starrainnotes.account.english.vocabulary.VocabularyReviewResultView;
import com.starrainnotes.account.english.vocabulary.VocabularyStudyService;
import com.starrainnotes.account.english.vocabulary.VocabularyStudySettingsRequest;
import com.starrainnotes.account.english.vocabulary.VocabularyStudySettingsView;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.learning.dto.WritingSubmissionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/account/english")
public class AccountEnglishController {

    private final AccountEnglishService service;
    private final VocabularyStudyService vocabularyStudyService;

    public AccountEnglishController(AccountEnglishService service, VocabularyStudyService vocabularyStudyService) {
        this.service = service;
        this.vocabularyStudyService = vocabularyStudyService;
    }

    private long accountId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object p = auth == null ? null : auth.getPrincipal();
        if (p instanceof com.starrainnotes.account.security.AccountPrincipal ap) {
            return ap.getId();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Not authenticated", "Account login required.");
    }

    @GetMapping("/learning/records")
    public List<Map<String, Object>> records(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        return service.records(accountId(), page, pageSize);
    }

    @GetMapping("/learning/records/batch")
    public java.util.Map<String, com.starrainnotes.english.shared.learning.dto.LearningRecordView> batchRecords(
            @RequestParam("ref") java.util.List<String> refs) {
        return service.batchRecords(accountId(), refs);
    }

    @GetMapping("/learning/summary")
    public com.starrainnotes.english.shared.learning.dto.LearningSummaryView summary() {
        return service.summary(accountId());
    }

    @GetMapping("/learning/insights")
    public com.starrainnotes.english.shared.learning.dto.LearningInsightsView insights() {
        return service.insights(accountId());
    }

    @GetMapping("/learning/records/{type}/{contentId}")
    public Map<String, Object> record(@PathVariable String type, @PathVariable Long contentId) {
        return service.record(accountId(), type, contentId);
    }

    @PutMapping("/learning/records/{type}/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putRecord(@PathVariable String type, @PathVariable Long contentId,
                          @RequestBody Map<String, Object> body) {
        service.putRecord(accountId(), type, contentId,
                body.get("completionStatus") == null ? "IN_PROGRESS" : String.valueOf(body.get("completionStatus")),
                body.get("timeSpentSeconds") == null ? null : ((Number) body.get("timeSpentSeconds")).intValue());
    }

    @GetMapping("/vocabulary/memory")
    public List<Map<String, Object>> vocabularyMemory() {
        return service.vocabularyMemory(accountId());
    }

    @PutMapping("/vocabulary/words/{wordId}/memory")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putMemory(@PathVariable Long wordId, @RequestBody Map<String, Object> body) {
        service.putVocabularyMemory(accountId(), wordId,
                body.get("memoryCount") == null ? 1 : ((Number) body.get("memoryCount")).intValue());
    }

    @GetMapping("/vocabulary/settings")
    public VocabularyStudySettingsView vocabularySettings() {
        return vocabularyStudyService.settings(accountId());
    }

    @PutMapping("/vocabulary/settings")
    public VocabularyStudySettingsView updateVocabularySettings(
            @Valid @RequestBody VocabularyStudySettingsRequest request) {
        return vocabularyStudyService.updateSettings(accountId(), request);
    }

    @GetMapping("/vocabulary/states")
    public List<VocabularyMemoryView> vocabularyStates(@RequestParam("wordId") List<Long> wordIds) {
        return vocabularyStudyService.memories(accountId(), wordIds);
    }

    @GetMapping("/vocabulary/review-queue")
    public VocabularyQueueView vocabularyReviewQueue(@RequestParam(required = false) Long themeId) {
        return vocabularyStudyService.queue(accountId(), themeId);
    }

    @PostMapping("/vocabulary/words/{wordId}/start")
    public VocabularyMemoryView startVocabularyWord(@PathVariable long wordId) {
        return vocabularyStudyService.start(accountId(), wordId);
    }

    @PostMapping("/vocabulary/words/{wordId}/reviews")
    public VocabularyReviewResultView reviewVocabularyWord(
            @PathVariable long wordId, @Valid @RequestBody VocabularyReviewRequest request) {
        return vocabularyStudyService.completeReview(accountId(), wordId, request);
    }

    @DeleteMapping("/vocabulary/words/{wordId}/progress")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetVocabularyWord(@PathVariable long wordId) {
        vocabularyStudyService.reset(accountId(), wordId);
    }

    @PutMapping("/vocabulary/words/{wordId}/display")
    public VocabularyMemoryView setVocabularyDisplay(
            @PathVariable long wordId, @Valid @RequestBody VocabularyDisplayRequest request) {
        return vocabularyStudyService.setDisplay(accountId(), wordId, request);
    }

    @DeleteMapping("/vocabulary/words/{wordId}/display")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearVocabularyDisplay(@PathVariable long wordId) {
        vocabularyStudyService.clearDisplay(accountId(), wordId);
    }

    @GetMapping("/vocabulary/statistics")
    public VocabularyProgressView vocabularyStatistics() {
        return vocabularyStudyService.progress(accountId());
    }

    @PostMapping("/vocabulary/import-local")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importLocalVocabulary(@RequestBody Map<String, Object> body) {
        vocabularyStudyService.importLocal(accountId(), body);
    }

    @GetMapping("/writing-submissions/{promptId}")
    public Map<String, Object> writingSubmission(@PathVariable Long promptId) {
        return service.writingSubmission(accountId(), promptId);
    }

    @PutMapping("/writing-submissions/{promptId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveWritingSubmission(@PathVariable Long promptId,
                                      @Valid @RequestBody WritingSubmissionRequest request) {
        service.saveSubmission(accountId(), promptId, request);
    }

    @PostMapping("/import-local-progress")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importLocal(@RequestBody Map<String, Object> body) {
        service.importLocalProgress(accountId(), body);
    }

    @PostMapping("/claim-legacy-progress")
    public void claimLegacy(@RequestBody Map<String, Object> body) {
        service.claimLegacyProgress(accountId(), String.valueOf(body.get("learnerKeyHash")));
    }
}
